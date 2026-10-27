package com.study.rewardCollection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.study.rewardCollection.dto.AwardRecordAddDTO;
import com.study.rewardCollection.dto.AwardRecordUpdateDTO;
import com.study.rewardCollection.entity.AwardRecord;
import com.study.rewardCollection.entity.SysClass;
import com.study.rewardCollection.entity.SysUser;
import com.study.rewardCollection.mapper.AwardRecordMapper;
import com.study.rewardCollection.mapper.SysClassMapper;
import com.study.rewardCollection.mapper.SysUserMapper;
import com.study.rewardCollection.security.DataScopeService;
import com.study.rewardCollection.service.AwardImageService;
import com.study.rewardCollection.service.AwardRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class AwardRecordServiceImpl extends ServiceImpl<AwardRecordMapper, AwardRecord> implements AwardRecordService {

    @Resource
    private SysUserMapper sysUserMapper;

    @Resource
    private SysClassMapper sysClassMapper;

    @Resource
    private DataScopeService dataScopeService;

    @Resource
    private AwardImageService awardImageService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Integer addAwardRecord(AwardRecordAddDTO recordAddDTO, MultipartFile file) {
        if (recordAddDTO == null || recordAddDTO.getAwardType() == null || recordAddDTO.getAwardName() == null || recordAddDTO.getIssuingOrganization() == null || recordAddDTO.getAwardDate() == null) {
            return -2;
        }
        String awardType = recordAddDTO.getAwardType().trim();
        String awardName = recordAddDTO.getAwardName().trim();
        String issuingOrganization = recordAddDTO.getIssuingOrganization().trim();
        String activityName = recordAddDTO.getActivityName() == null ? "" : recordAddDTO.getActivityName().trim();
        String awardCategory = recordAddDTO.getAwardCategory() == null ? "" : recordAddDTO.getAwardCategory().trim();
        String awardLevel = recordAddDTO.getAwardLevel() == null ? "" : recordAddDTO.getAwardLevel().trim();
        String awardGrade = recordAddDTO.getAwardGrade() == null ? "" : recordAddDTO.getAwardGrade().trim();
        String certificateNo = recordAddDTO.getCertificateNo() == null ? "" : recordAddDTO.getCertificateNo().trim();
        String description = recordAddDTO.getDescription() == null ? "" : recordAddDTO.getDescription().trim();
        if (awardName.isBlank() || issuingOrganization.isBlank()) {
            return -2;
        }
        if (!"PERSONAL".equals(awardType) && !"CLASS".equals(awardType)) {
            return -3;
        }
        if (awardName.length() > 200 || activityName.length() > 200 || awardCategory.length() > 50 || awardLevel.length() > 50 || awardGrade.length() > 50 || issuingOrganization.length() > 200 || certificateNo.length() > 100 || description.length() > 5000) {
            return -6;
        }
        SysUser currentUser = dataScopeService.currentUser();
        SysUser student = null;
        Long studentUserId = null;
        Long classId;
        if ("PERSONAL".equals(awardType)) {
            // 学生上传时，固定为本人，不使用前端传来的学生 ID。
            studentUserId = "STUDENT".equals(currentUser.getRole()) ? currentUser.getId() : recordAddDTO.getStudentUserId();
            if (studentUserId == null) {
                return -2;
            }
            student = sysUserMapper.selectById(studentUserId);
            if (student == null) {
                return -4;
            }
            if (!"STUDENT".equals(student.getRole())) {
                throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "获奖用户必须是学生");
            }
            // 个人奖状的班级由学生信息确定。
            classId = student.getClassId();
        } else {
            // 班级奖状没有获奖学生。
            classId = recordAddDTO.getClassId();
            if (classId == null) {
                return -2;
            }
        }
        if (classId == null) {
            return -5;
        }
        SysClass targetClass = sysClassMapper.selectById(classId);
        if (targetClass == null) {
            return -5;
        }
        // 检查角色权限、教师负责的班级以及学生归属。
        dataScopeService.checkCreateAward(awardType, student, targetClass);
        LocalDateTime time = LocalDateTime.now();
        AwardRecord awardRecord = new AwardRecord();
        awardRecord.setAwardType(awardType);
        // 使用奖状所属班级的组织，支持管理员跨组织上传。
        awardRecord.setOrgId(targetClass.getOrgId());
        awardRecord.setClassId(targetClass.getId());
        awardRecord.setStudentUserId(studentUserId);
        awardRecord.setAwardName(awardName);
        awardRecord.setActivityName(activityName);
        awardRecord.setAwardCategory(awardCategory);
        awardRecord.setAwardLevel(awardLevel);
        awardRecord.setAwardGrade(awardGrade);
        awardRecord.setIssuingOrganization(issuingOrganization);
        awardRecord.setAwardDate(recordAddDTO.getAwardDate());
        awardRecord.setCertificateNo(certificateNo);
        awardRecord.setDescription(description);
        awardRecord.setUploadedBy(currentUser.getId());
        awardRecord.setUpdatedBy(currentUser.getId());
        awardRecord.setIsDeleted(0);
        awardRecord.setCreateTime(time);
        awardRecord.setUpdateTime(time);
        if (baseMapper.insert(awardRecord) != 1) {
            throw new RuntimeException("保存失败");
        }
        awardImageService.saveImage(awardRecord.getId(), file);
        return 1;
    }

    @Override
    public Integer updateAwardRecord(Long id, AwardRecordUpdateDTO recordUpdateDTO) {
        if (id == null) {
            return -2;
        }
        AwardRecord target = baseMapper.selectById(id);
        if (target == null) {
            return -7;
        }
        dataScopeService.checkAwardWrite(target);
        if (recordUpdateDTO == null || recordUpdateDTO.getAwardName() == null || recordUpdateDTO.getIssuingOrganization() == null || recordUpdateDTO.getAwardDate() == null) {
            return -2;
        }
        String awardName = recordUpdateDTO.getAwardName().trim();
        String issuingOrganization = recordUpdateDTO.getIssuingOrganization().trim();
        String activityName = recordUpdateDTO.getActivityName() == null ? "" : recordUpdateDTO.getActivityName().trim();
        String awardCategory = recordUpdateDTO.getAwardCategory() == null ? "" : recordUpdateDTO.getAwardCategory().trim();
        String awardLevel = recordUpdateDTO.getAwardLevel() == null ? "" : recordUpdateDTO.getAwardLevel().trim();
        String awardGrade = recordUpdateDTO.getAwardGrade() == null ? "" : recordUpdateDTO.getAwardGrade().trim();
        String certificateNo = recordUpdateDTO.getCertificateNo() == null ? "" : recordUpdateDTO.getCertificateNo().trim();
        String description = recordUpdateDTO.getDescription() == null ? "" : recordUpdateDTO.getDescription().trim();
        if (awardName.isBlank() || issuingOrganization.isBlank()) {
            return -2;
        }
        if (awardName.length() > 200 || activityName.length() > 200 || awardCategory.length() > 50 || awardLevel.length() > 50 || awardGrade.length() > 50 || issuingOrganization.length() > 200 || certificateNo.length() > 100 || description.length() > 5000) {
            return -6;
        }
        SysUser currentUser = dataScopeService.currentUser();
        LocalDateTime time = LocalDateTime.now();
        // 明确指定允许修改的字段。
        UpdateWrapper<AwardRecord> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .set("award_name", awardName)
                .set("activity_name", activityName)
                .set("award_category", awardCategory)
                .set("award_level", awardLevel)
                .set("award_grade", awardGrade)
                .set("issuing_organization", issuingOrganization)
                .set("award_date", recordUpdateDTO.getAwardDate())
                .set("certificate_no", certificateNo)
                .set("description", description)
                .set("updated_by", currentUser.getId())
                .set("update_time", time);
        if (baseMapper.update(null, wrapper) != 1) {
            throw new RuntimeException("修改失败");
        }
        return 1;
    }

    @Override
    public Integer deleteAwardRecord(Long id) {
        if (id == null) {
            return -2;
        }
        AwardRecord target = baseMapper.selectById(id);
        if (target == null) {
            return -7;
        }
        dataScopeService.checkAwardWrite(target);
        awardImageService.deleteForAward(id);
        SysUser currentUser = dataScopeService.currentUser();
        LocalDateTime time = LocalDateTime.now();
        // 逻辑删除，同时记录操作人与操作时间。
        UpdateWrapper<AwardRecord> wrapper = new UpdateWrapper<>();
        wrapper.eq("id", id)
                .set("is_deleted", 1)
                .set("updated_by", currentUser.getId())
                .set("update_time", time);
        if (baseMapper.update(null, wrapper) != 1) {
            throw new RuntimeException("删除失败");
        }
        return 1;
    }
}
