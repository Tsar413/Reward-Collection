package com.study.rewardCollection.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "award_record")
@TableName(value = "award_record")
@Data
public class AwardRecord {
    @Id
    private Long id;

    @Column(name = "org_code", length = 50)
    @TableField(value = "org_code")
    private String orgCode;

    @Column(name = "award_type", length = 20)
    @TableField(value = "award_type")
    private String awardType;

    @Column(name = "student_no", length = 50)
    @TableField("student_no")
    private String studentNo;

    @Column(name = "class_code", length = 50)
    @TableField(value = "class_code")
    private String classCode;

    @Column(name = "award_name", length = 200)
    @TableField("award_name")
    private String awardName;

    @Column(name = "activity_name", length = 200)
    @TableField("activity_name")
    private String activityName;

    @Column(name = "award_category", length = 50)
    @TableField("award_category")
    private String awardCategory;

    @Column(name = "award_level", length = 50)
    @TableField("award_level")
    private String awardLevel;

    @Column(name = "award_grade", length = 50)
    @TableField("award_grade")
    private String awardGrade;

    @Column(name = "issuing_organization", length = 50)
    @TableField("issuing_organization")
    private String issuingOrganization;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "award_date", columnDefinition = "DATETIME")
    @TableField("award_date")
    private LocalDateTime awardDate;

    @Column(name = "certificate_no", length = 100)
    @TableField("certificate_no")
    private String certificateNo;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "uploaded_by")
    @TableField("uploaded_by")
    private Long uploadedBy;

    @Column(name = "is_deleted")
    @TableField("is_deleted")
    private Integer isDeleted;

    @Column(name = "updated_by")
    @TableField("updated_by")
    private Long updatedBy;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "create_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("create_time")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Column(name = "update_time", nullable = false, columnDefinition = "DATETIME")
    @TableField("update_time")
    private LocalDateTime updateTime;
}
