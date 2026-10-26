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
        return 1;
    }
}
