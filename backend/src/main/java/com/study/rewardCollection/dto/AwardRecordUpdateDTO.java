package com.study.rewardCollection.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.OptBoolean;
import lombok.Data;

import java.time.LocalDate;

@Data
public class AwardRecordUpdateDTO {

    private String awardName;

    private String activityName;

    private String awardCategory;

    private String awardLevel;

    private String awardGrade;

    private String issuingOrganization;

    @JsonFormat(pattern = "uuuu-MM-dd", lenient = OptBoolean.FALSE)
    private LocalDate awardDate;

    private String certificateNo;

    private String description;
}