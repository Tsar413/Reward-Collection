package com.study.rewardCollection.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "sys_class")
@TableName(value = "sys_class")
@Data
public class SysClass {
    @Id
    private Long id;

    @Column(name = "org_code", length = 50)
    @TableField(value = "org_code")
    private String orgCode;

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
