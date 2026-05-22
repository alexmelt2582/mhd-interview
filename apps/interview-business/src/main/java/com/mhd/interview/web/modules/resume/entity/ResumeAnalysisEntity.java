package com.mhd.interview.web.modules.resume.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 简历 AI 分析结果实体类（对应 t_resume_analysis 表）
 *
 * @author zhao-hao-dong
 */
@Data
@TableName("t_resume_analysis")
public class ResumeAnalysisEntity {

    /**
     * 主键 ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 关联的简历 ID
     */
    private Long resumeId;

    /**
     * 综合评分（0-100）
     */
    private Integer score;

    /**
     * 内容完整性 (0-25)
     */
    private Integer contentScore;

    /**
     * 结构清晰度 (0-20)
     */
    private Integer structureScore;

    /**
     * 技能匹配度 (0-25)
     */
    private Integer skillMatchScore;

    /**
     * 表达专业性 (0-15)
     */
    private Integer expressionScore;

    /**
     * 项目经验 (0-15)
     */
    private Integer projectScore;

    /**
     * AI 分析摘要
     */
    private String summary;

    /**
     * 核心优势（JSON格式）
     */
    private String strengths;

    /**
     * 优化建议（JSON格式）
     */
    private String suggestions;

    /**
     * 创建时间（INSERT 自动填充）
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
