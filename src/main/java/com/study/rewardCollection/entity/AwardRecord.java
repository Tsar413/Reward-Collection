package com.study.rewardCollection.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "award_record")
@TableName(value = "award_record")
@Data
public class AwardRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Column(name = "award_type", length = 20)
    @TableField(value = "award_type")
    private String awardType;

    @Column(name = "org_id", nullable = false)
    @TableField("org_id")
    private Long orgId;

    @Column(name = "student_user_id")
    @TableField("student_user_id")
    private Long studentUserId;

    @Column(name = "class_id", nullable = false)
    @TableField("class_id")
    private Long classId;

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

    @Column(name = "issuing_organization", length = 200)
    @TableField("issuing_organization")
    private String issuingOrganization;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "award_date", columnDefinition = "DATE")
    @TableField("award_date")
    private LocalDate awardDate;

    @Column(name = "certificate_no", length = 100)
    @TableField("certificate_no")
    private String certificateNo;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String description;

    @Column(name = "uploaded_by")
    @TableField("uploaded_by")
    private Long uploadedBy;

    // 0：未删除；1：已删除
    @TableLogic(value = "0", delval = "1")
    @Column(name = "is_deleted", nullable = false, columnDefinition = "TINYINT DEFAULT 0")
    @TableField("is_deleted")
    private Integer isDeleted = 0;

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
