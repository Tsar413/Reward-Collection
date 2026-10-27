package com.study.rewardCollection.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.study.rewardCollection.entity.AwardImage;
import com.study.rewardCollection.entity.AwardRecord;
import com.study.rewardCollection.mapper.AwardImageMapper;
import com.study.rewardCollection.mapper.AwardRecordMapper;
import com.study.rewardCollection.security.DataScopeService;
import com.study.rewardCollection.service.AwardImageService;
import com.study.rewardCollection.util.AwardImageStorageUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.time.LocalDateTime;

@Slf4j
@Service
public class AwardImageServiceImpl implements AwardImageService {

    @Resource
    private AwardImageMapper awardImageMapper;

    @Resource
    private AwardRecordMapper awardRecordMapper;

    @Resource
    private DataScopeService dataScopeService;

    @Value("${app.award-image.root}")
    private String uploadRoot;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveImage(Long awardId, MultipartFile file) {
        AwardRecord award = lockWritableAward(awardId);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "请选择图片"
            );
        }

        if (file.getSize() > AwardImageStorageUtil.MAX_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(413), "图片不能超过5MB"
            );
        }

        AwardImage image = findImage(awardId);
        String oldKey = image == null ? null : image.getStorageKey();

        byte[] bytes;

        try (InputStream input = file.getInputStream()) {
            bytes = input.readNBytes(
                    AwardImageStorageUtil.MAX_BYTES + 1
            );
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "读取上传文件失败",
                    e
            );
        }

        if (bytes.length > AwardImageStorageUtil.MAX_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.valueOf(413), "图片不能超过5MB"
            );
        }

        AwardImageStorageUtil.StoredImage stored;

        try {
            stored = AwardImageStorageUtil.save(
                    root(),
                    award.getOrgId(),
                    bytes,
                    file.getOriginalFilename()
            );
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, e.getMessage(), e
            );
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "图片保存失败",
                    e
            );
        }

        // 从这里开始，事务回滚时清理新文件；
        // 事务提交后才清理旧文件。
        trackFiles(stored.storageKey(), oldKey);

        boolean inserting = image == null;
        LocalDateTime time = LocalDateTime.now();
        Long uploaderId = dataScopeService.currentUser().getId();

        if (inserting) {
            image = new AwardImage();
            image.setCreateTime(time);
        }

        image.setOrgId(award.getOrgId());
        image.setAwardId(awardId);
        image.setStorageKey(stored.storageKey());
        image.setOriginalFilename(stored.originalFilename());
        image.setMimeType(stored.mimeType());
        image.setFileSize(stored.fileSize());
        image.setImageWidth(stored.width());
        image.setImageHeight(stored.height());
        image.setUpdatedBy(uploaderId);
        image.setUpdateTime(time);

        int rows = inserting
                ? awardImageMapper.insert(image)
                : awardImageMapper.updateById(image);

        if (rows != 1) {
            throw new RuntimeException("图片信息保存失败");
        }

        // 修改图片也属于修改奖状。
        int awardRows = awardRecordMapper.update(
                null,
                new UpdateWrapper<AwardRecord>()
                        .eq("id", awardId)
                        .set("updated_by", uploaderId)
                        .set("update_time", time)
        );

        if (awardRows != 1) {
            throw new RuntimeException("奖状信息更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ImageContent readImage(Long awardId) {
        // 读取期间也锁定父记录，避免文件刚查到就被替换、删除。
        AwardRecord award = awardRecordMapper.selectOne(
                dataScopeService.awardQuery()
                        .eq("id", awardId)
                        .last("FOR UPDATE")
        );

        if (award == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "奖状不存在或无权查看"
            );
        }

        AwardImage image = findImage(awardId);

        if (image == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "该奖状尚未上传图片"
            );
        }

        try {
            return new ImageContent(
                    AwardImageStorageUtil.read(
                            root(), image.getStorageKey()
                    ),
                    image.getMimeType()
            );
        } catch (NoSuchFileException e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "图片文件不存在", e
            );
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "图片读取失败",
                    e
            );
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteForAward(Long awardId) {
        lockWritableAward(awardId);

        AwardImage image = findImage(awardId);
        if (image == null) {
            return;
        }

        if (awardImageMapper.deleteById(image.getId()) != 1) {
            throw new RuntimeException("图片记录删除失败");
        }

        // 数据库提交后再删除文件。
        trackFiles(null, image.getStorageKey());
    }

    private AwardRecord lockWritableAward(Long awardId) {
        if (awardId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "奖状ID不能为空"
            );
        }

        AwardRecord award =
                awardRecordMapper.selectForUpdate(awardId);

        if (award == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "奖状不存在"
            );
        }

        dataScopeService.checkAwardWrite(award);
        return award;
    }

    private AwardImage findImage(Long awardId) {
        return awardImageMapper.selectOne(
                new QueryWrapper<AwardImage>()
                        .eq("award_id", awardId)
        );
    }

    private Path root() {
        return Path.of(uploadRoot);
    }

    private void trackFiles(String newKey, String oldKey) {
        try {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            deleteFileQuietly(oldKey);
                        }

                        @Override
                        public void afterCompletion(int status) {
                            if (status == STATUS_ROLLED_BACK) {
                                deleteFileQuietly(newKey);
                            }
                        }
                    }
            );
        } catch (RuntimeException e) {
            deleteFileQuietly(newKey);
            throw e;
        }
    }

    private void deleteFileQuietly(String key) {
        if (key == null) {
            return;
        }

        try {
            AwardImageStorageUtil.delete(root(), key);
        } catch (Exception e) {
            // 文件清理失败留日志，避免把已成功提交的业务返回成失败。
            log.warn("图片文件清理失败，storageKey={}", key, e);
        }
    }
}