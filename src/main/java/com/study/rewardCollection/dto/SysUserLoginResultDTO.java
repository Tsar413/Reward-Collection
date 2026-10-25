package com.study.rewardCollection.dto;

import lombok.Data;

@Data
public class SysUserLoginResultDTO {

    // 原始Token
    private String token;

    // 用户基本资料
    private UserInfo user;

    @Data
    public static class UserInfo {

        private Long id;

        private Long orgId;

        private Long classId;

        private String userNo;

        private String username;

        private String realName;

        private String role;

        private Integer status;
    }
}