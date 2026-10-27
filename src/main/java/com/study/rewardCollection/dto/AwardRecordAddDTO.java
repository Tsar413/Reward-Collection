package com.study.rewardCollection.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AwardRecordAddDTO {

    // PERSONAL：个人奖状；CLASS：班级奖状
    private String awardType;

    // 教师、管理员为学生上传个人奖状时填写
    private Long studentUserId;

    // 上传班级奖状时填写
    private Long classId;

    private String awardName;

    private String activityName;

    private String awardCategory;

    private String awardLevel;

    private String awardGrade;

    private String issuingOrganization;

    @JsonFormat(pattern = "yyyy-MM-dd", lenient = OptBoolean.FALSE)
    private LocalDate awardDate;

    private String certificateNo;

    private String description;
}