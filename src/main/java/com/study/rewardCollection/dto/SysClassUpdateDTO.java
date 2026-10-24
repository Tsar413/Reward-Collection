package com.study.rewardCollection.dto;

import lombok.Data;

@Data
public class SysClassUpdateDTO {
    private String className;

    private Integer admissionYear;

    private Long teacherUserId;

    private Integer status;

    private String remark;
}
