package com.mhd.interview.web.modules.llmprovider.dto;

/**
 * 更新 LLM Provider 请求体（所有字段可选，null 表示不更新）
 *
 * @param name    新 Provider 名称（null 表示不修改）
 * @param baseUrl 新 API 基础 URL（null 表示不修改）
 * @param apiKey  新 API 密钥（null 表示不修改）
 * @param model   新默认模型名（null 表示不修改）
 */
public record UpdateProviderRequest(
        String name,
        String baseUrl,
        String apiKey,
        String model
) {
}
