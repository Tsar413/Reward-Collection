package com.study.rewardCollection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_class")
@TableName(value = "sys_class")
@Data
public class SysClass {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Column(name = "org_id", nullable = false)
    @TableField("org_id")
    private Long orgId;

    @Column(name = "class_code", length = 50)
    @TableField(value = "class_code")
    private String classCode;

    @Column(name = "class_name", length = 100)
    @TableField(value = "class_name")
    private String className;

    @Column(name = "admission_year")
    @TableField(value = "admission_year")
    private Integer admissionYear;

    @Column(name = "teacher_user_id")
    @TableField(value = "teacher_user_id")
    private Long teacherUserId;

    private Integer status;

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
