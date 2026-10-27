package com.study.rewardCollection.service;

import org.springframework.web.multipart.MultipartFile;

public interface AwardImageService {

    // 第一次上传或替换现有图片。
    void saveImage(Long awardId, MultipartFile file);

    ImageContent readImage(Long awardId);

    // 供删除奖状时调用。
    void deleteForAward(Long awardId);

    record ImageContent(byte[] data, String mimeType) {
    }
}