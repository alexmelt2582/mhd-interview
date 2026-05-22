package com.mhd.interview.web.modules.llmprovider.dto;

import java.time.LocalDateTime;

/**
 * LLM Provider 详情 DTO（返回给前端，不含 apiKey 明文）
 *
 * @param id         Provider ID
 * @param name       显示名称
 * @param baseUrl    API 基础 URL
 * @param model      默认模型名称
 * @param isDefault  是否为默认 Provider
 * @param createTime 创建时间
 * @param updateTime 更新时间
 */
public record LlmProviderDTO(
        Long id,
        String name,
        String baseUrl,
        String model,
        Boolean isDefault,
        LocalDateTime createTime,
        LocalDateTime updateTime
) {
}
