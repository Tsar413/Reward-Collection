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
@Table(name = "award_image")
@TableName(value = "award_image")
@Data
public class AwardImage {
    @Id
    private Long id;

    @Column(name = "org_code", length = 50)
    @TableField(value = "org_code")
    private String orgCode;

    @Column(name = "award_id", length = 50)
    @TableField(value = "award_id")
    private Long awardId;

    @Column(name = "storage_key", length = 512)
    @TableField(value = "storage_key")
    private String storageKey;

    @Column(name = "original_filename")
    @TableField(value = "original_filename")
    private String originalFilename;

    @Column(name = "mime_type", length = 100)
    @TableField(value = "mime_type")
    private String mimeType;

    @Column(name = "file_size")
    @TableField(value = "file_size")
    private Long fileSize;

    @Column(name = "image_width")
    @TableField(value = "image_width")
    private Integer imageWidth;

    @Column(name = "image_height")
    @TableField(value = "image_height")
    private Integer imageHeight;

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
