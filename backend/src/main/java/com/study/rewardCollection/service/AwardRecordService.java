package com.study.rewardCollection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.rewardCollection.dto.AwardRecordAddDTO;
import com.study.rewardCollection.dto.AwardRecordUpdateDTO;
import com.study.rewardCollection.entity.AwardRecord;
import org.springframework.web.multipart.MultipartFile;

public interface AwardRecordService extends IService<AwardRecord> {
    Integer addAwardRecord(AwardRecordAddDTO recordAddDTO, MultipartFile file);

    Integer updateAwardRecord(Long id, AwardRecordUpdateDTO recordUpdateDTO);

    Integer deleteAwardRecord(Long id);

}
