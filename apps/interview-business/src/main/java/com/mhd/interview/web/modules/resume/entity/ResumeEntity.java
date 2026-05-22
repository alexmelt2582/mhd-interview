package com.mhd.interview.web.modules.resume.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历实体类（对应 t_resume 表）
 *
 * @author zhao-hao-dong
 */
@Data
@TableName("t_resume")
public class ResumeEntity {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 原始文件名
     */
    private String originalName;

    /**
     * 文件 SHA-256 哈希（用于去重）
     */
    private String fileHash;

    /**
     * 文件大小（字节）
     */
    private Long fileSize;

    /**
     * 文件类型
     */
    private String contentType;

    /**
     * 存储文件的 Key
     */
    private String storageKey;

    /**
     * 存储文件的 URL
     */
    private String storageUrl;

    /**
     * 解析后的简历纯文本内容
     */
    private String parsedText;

    /**
     * 解析/分析状态：0=待解析，1=解析中，2=已完成，3=失败
     */
    private Integer analysisStatus;

    /**
     * AI 分析失败时的错误信息（最多500字符）
     */
    private String analysisError;

    /**
     * 访问次数
     */
    private Integer accessCount;

    /**
     * 逻辑删除标识（0=正常, 1=已删除）
     */
    @TableLogic
    private Boolean isDeleted;

    /**
     * 创建时间（INSERT 自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 更新时间（INSERT/UPDATE 自动填充）
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
