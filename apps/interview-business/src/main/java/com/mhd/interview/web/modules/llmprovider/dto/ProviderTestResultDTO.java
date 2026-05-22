package com.mhd.interview.web.modules.llmprovider.dto;

/**
 * Provider 连通性测试结果 DTO
 *
 * @param success  是否连通
 * @param message  测试结果消息（成功或失败原因）
 * @param latencyMs 响应延迟（毫秒），仅成功时有效
 */
public record ProviderTestResultDTO(
        boolean success,
        String message,
        Long latencyMs
) {
}
