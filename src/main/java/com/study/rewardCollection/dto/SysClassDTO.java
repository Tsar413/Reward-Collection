package com.study.rewardCollection.dto;

import lombok.Data;

@Data
public class SysClassDTO {

    private Long orgId;

    private String classCode;

    private String className;

    private Integer admissionYear;

    private Long teacherUserId;

    private String remark;
    
}
