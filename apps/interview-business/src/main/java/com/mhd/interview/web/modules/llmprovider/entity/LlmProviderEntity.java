package com.mhd.interview.web.modules.llmprovider.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * LLM Provider 实体类（对应 t_llm_provider 表）
 *
 * @author mhd
 */
@Data
@TableName("t_llm_provider")
public class LlmProviderEntity {

    /** 主键 ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** Provider 显示名称（如 "阿里云百炼"）*/
    private String name;

    /** API 基础 URL（如 https://dashscope.aliyuncs.com/compatible-mode/v1）*/
    private String baseUrl;

    /** API 密钥（AES 加密存储）*/
    private String apiKey;

    /** 默认使用的模型名（如 qwen-max-latest）*/
    private String model;

    /** 是否为默认 Provider（0=否, 1=是）*/
    private Boolean isDefault;

    /** 逻辑删除标识（0=正常, 1=已删除）*/
    @TableLogic
    private Boolean isDeleted;

    /** 创建时间（INSERT 自动填充）*/
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（INSERT/UPDATE 自动填充）*/
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
