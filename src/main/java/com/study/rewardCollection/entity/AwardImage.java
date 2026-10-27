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
@Table(name = "award_image")
@TableName(value = "award_image")
@Data
public class AwardImage {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    @Column(name = "org_id", nullable = false)
    @TableField("org_id")
    private Long orgId;

    @Column(name = "award_id", nullable = false, unique = true)
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
