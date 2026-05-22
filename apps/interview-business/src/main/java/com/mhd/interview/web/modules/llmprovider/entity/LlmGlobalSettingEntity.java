package com.mhd.interview.web.modules.llmprovider.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * LLM 全局设置实体类（对应 t_llm_global_setting 表）
 * <p>
 * 存储系统级的 AI 配置，如最大并发数、全局超时时间等
 *
 * @author mhd
 */
@Data
@TableName("t_llm_global_setting")
public class LlmGlobalSettingEntity {

    /** 主键 ID */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 配置项 Key */
    private String settingKey;

    /** 配置项值 */
    private String settingValue;

    /** 配置项描述 */
    private String description;

    /** 创建时间（INSERT 自动填充）*/
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间（INSERT/UPDATE 自动填充）*/
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
