package com.mhd.interview.web.modules.llmprovider.dto;

/**
 * 创建 LLM Provider 请求体
 *
 * @param name    Provider 显示名称（如 "阿里云百炼"）
 * @param baseUrl API 基础 URL
 * @param apiKey  API 密钥（明文，服务层加密存储）
 * @param model   默认模型名称
 */
public record CreateProviderRequest(
        String name,
        String baseUrl,
        String apiKey,
        String model
) {
}
