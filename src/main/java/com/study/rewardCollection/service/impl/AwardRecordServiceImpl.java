package com.study.rewardCollection.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.study.rewardCollection.dto.AwardRecordAddDTO;
import com.study.rewardCollection.entity.AwardRecord;
import com.study.rewardCollection.entity.SysClass;
import com.study.rewardCollection.entity.SysUser;
import com.study.rewardCollection.mapper.AwardRecordMapper;
import com.study.rewardCollection.mapper.SysClassMapper;
import com.study.rewardCollection.mapper.SysUserMapper;
import com.study.rewardCollection.security.DataScopeService;
import com.study.rewardCollection.service.AwardRecordService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

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

    @Override
    public Integer addAwardRecord(AwardRecordAddDTO recordAddDTO) {
        if(recordAddDTO == null || recordAddDTO.getAwardType() == null || recordAddDTO.getClassId() == null || recordAddDTO.getAwardName() == null || recordAddDTO.getIssuingOrganization() == null || recordAddDTO.getAwardDate() == null){
            return -2;
        }
        String awardType = recordAddDTO.getAwardType().trim();
        Long studentUserId = recordAddDTO.getStudentUserId();
        Long classId = recordAddDTO.getClassId();
        String awardName = recordAddDTO.getAwardName().trim();
        String activityName = recordAddDTO.getActivityName() == null ? "" : recordAddDTO.getActivityName().trim();
        String awardCategory = recordAddDTO.getAwardCategory() == null ? "" : recordAddDTO.getAwardCategory().trim();
        String awardLevel = recordAddDTO.getAwardLevel() == null ? "" : recordAddDTO.getAwardLevel().trim();
        String awardGrade = recordAddDTO.getAwardGrade() == null ? "" : recordAddDTO.getAwardGrade().trim();
        String issuingOrganization = recordAddDTO.getIssuingOrganization().trim();
        LocalDate awardDate = recordAddDTO.getAwardDate();
        String certificateNo = recordAddDTO.getCertificateNo() == null ? "" : recordAddDTO.getCertificateNo().trim();
        String description = recordAddDTO.getDescription() == null ? "" : recordAddDTO.getDescription().trim();
        if(!"PERSONAL".equals(awardType) && !"CLASS".equals(awardType)){
            return -3;
        }
        if(studentUserId != null){
            SysUser sysUser = sysUserMapper.selectById(studentUserId);
            if(sysUser == null){
                return -4;
            }
        }
        SysClass sysClass = sysClassMapper.selectById(classId);
        if(sysClass == null){
            return -5;
        }
        if(awardName.length() > 200 || activityName.length() > 50 || awardCategory.length() > 50 || awardLevel.length() > 50 || awardGrade.length() > 50 || issuingOrganization.length() > 200 || certificateNo.length() > 100 || description.length() > 5000){
            return -6;
        }
        LocalDateTime time = LocalDateTime.now();
        SysUser currentUser = dataScopeService.currentUser();
        AwardRecord awardRecord = new AwardRecord();
        awardRecord.setAwardType(awardType);
        awardRecord.setOrgId(currentUser.getOrgId());
        awardRecord.setStudentUserId(studentUserId);
        awardRecord.setClassId(classId);
        awardRecord.setAwardName(awardName);
        awardRecord.setActivityName(activityName);
        awardRecord.setAwardCategory(awardCategory);
        awardRecord.setAwardLevel(awardLevel);
        awardRecord.setAwardGrade(awardGrade);
        awardRecord.setIssuingOrganization(issuingOrganization);
        awardRecord.setAwardDate(awardDate);
        awardRecord.setCertificateNo(certificateNo);
        awardRecord.setDescription(description);
        awardRecord.setUpdatedBy(currentUser.getId());
        awardRecord.setIsDeleted(0);
        awardRecord.setUpdatedBy(currentUser.getId());
        awardRecord.setCreateTime(time);
        awardRecord.setUpdateTime(time);
        if (baseMapper.insert(awardRecord) != 1) {
            throw new RuntimeException("保存失败");
        }
        return 1;
    }
}
