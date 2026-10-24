package com.study.rewardCollection.dto;

import lombok.Data;

@Data
public class SysUserUpdateDTO {

    private String realName;

    private Long classId;

    private Integer status;

    private String remark;
}