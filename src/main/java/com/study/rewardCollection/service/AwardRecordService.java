package com.study.rewardCollection.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.study.rewardCollection.dto.AwardRecordAddDTO;
import com.study.rewardCollection.dto.AwardRecordUpdateDTO;
import com.study.rewardCollection.entity.AwardRecord;

public interface AwardRecordService extends IService<AwardRecord> {
    Integer addAwardRecord(AwardRecordAddDTO recordAddDTO);

    Integer updateAwardRecord(Long id, AwardRecordUpdateDTO recordUpdateDTO);

    Integer deleteAwardRecord(Long id);
}
