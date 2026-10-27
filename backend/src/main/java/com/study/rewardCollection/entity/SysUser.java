package com.study.rewardCollection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_user")
@TableName(value = "sys_user")
@Data
public class SysUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Column(name = "org_id", nullable = false)
    @TableField("org_id")
    private Long orgId;

    @Column(name = "class_id")
    @TableField("class_id")
    private Long classId;

    @Column(length = 50)
    private String username;

    @Column(name = "real_name", length = 50)
    @TableField("real_name")
    private String realName;

    @Column(length = 20)
    private String role;

    @Column(name = "user_no", length = 50)
    @TableField("user_no")
    private String userNo;

    @JsonIgnore
    @ToString.Exclude
    @Column(name = "password_hash", length = 255)
    @TableField("password_hash")
    private String passwordHash;

    @JsonIgnore
    @ToString.Exclude
    @Column(name = "password_salt", length = 64)
    @TableField("password_salt")
    private String passwordSalt;

    @JsonIgnore
    @Column(name = "password_algorithm", length = 40)
    @TableField("password_algorithm")
    private String passwordAlgorithm;

    private Integer status;

    @JsonFormat(timezone = "GMT+8", pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(columnDefinition = "DATETIME", name = "last_login_time")
    @TableField(value = "last_login_time")
    private LocalDateTime lastLoginTime;

    @Column(length = 500)
    private String remark;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("update_time")
    private LocalDateTime updateTime;
}
