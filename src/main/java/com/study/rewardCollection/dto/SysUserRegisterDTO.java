package com.study.rewardCollection.dto;

import lombok.Data;

@Data
public class SysUserRegisterDTO {
    private Long orgId;
    private String realName;
    private String password;
    private String role;
    private Long classId;
    private String userNo;
    private String remark;
}
