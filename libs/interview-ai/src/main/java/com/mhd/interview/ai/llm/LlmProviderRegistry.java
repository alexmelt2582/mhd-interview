package com.mhd.interview.ai.llm;

import com.mhd.interview.common.enums.ErrorCodeEnum;
import com.mhd.interview.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * LLM Provider 动态路由注册中心
 * <p>
 * 管理多个 Provider 的 ChatClient 缓存，支持按名称动态创建和路由。
 * Provider 配置从数据库（{@code t_llm_provider}）读取，支持运行时刷新缓存。
 * <p>
 * 使用方式：
 * <pre>
 * ChatClient client = registry.getChatClientOrDefault(providerId);
 * </pre>
 *
 * @author zhao-hao-dong
 */
@Component
@Slf4j
public class LlmProviderRegistry {

    /**
     * ChatClient 缓存（Key = Provider 名称）
     */
    private final Map<String, ChatClient> clientCache = new ConcurrentHashMap<>();

    /**
     * Provider 配置加载器（由 interview-business 模块注入，避免循环依赖）
     */
    private volatile Function<String, ProviderConfig> configLoader;

    /**
     * 注入 Provider 配置加载器（在 interview-business 启动时由 LlmProviderServiceImpl 调用）
     *
     * @param loader 根据 Provider 名称加载配置的函数
     */
    public void setConfigLoader(Function<String, ProviderConfig> loader) {
        this.configLoader = loader;
    }

    /**
     * 获取指定 Provider 的 ChatClient（优先从缓存取，缓存未命中则动态创建）
     *
     * @param providerName Provider 名称（对应 t_llm_provider.name）
     * @return ChatClient 实例
     * @throws BusinessException 当 Provider 不存在或配置加载失败时
     */
    public ChatClient getChatClient(String providerName) {
        return clientCache.computeIfAbsent(providerName, name -> {
            log.info("[LlmProviderRegistry] Creating ChatClient for provider: {}", name);
            return createChatClient(name);
        });
    }

    /**
     * 获取指定 Provider 的 ChatClient，providerName 为 null/空白时使用默认 Provider
     *
     * @param providerName Provider 名称（可为 null）
     * @return ChatClient 实例
     */
    public ChatClient getChatClientOrDefault(String providerName) {
        if (providerName != null && !providerName.isBlank()) {
            return getChatClient(providerName);
        }
        // 加载器注册后通过 DEFAULT_KEY 查找默认 Provider
        return getChatClient(DEFAULT_KEY);
    }

    /**
     * 使指定 Provider 的缓存失效（Provider 配置变更后调用）
     *
     * @param providerName Provider 名称
     */
    public void evict(String providerName) {
        clientCache.remove(providerName);
        log.info("[LlmProviderRegistry] Evicted cache for provider: {}", providerName);
    }

    /**
     * 清空全部 Provider 缓存（批量重载时使用）
     */
    public void reload() {
        int size = clientCache.size();
        clientCache.clear();
        log.info("[LlmProviderRegistry] All cache cleared ({} entries)", size);
    }

    // ==================== 内部实现 ====================

    /** 默认 Provider 的缓存 Key */
    private static final String DEFAULT_KEY = "__default__";

    /**
     * 根据 Provider 名称动态构建 ChatClient
     *
     * @param providerName Provider 名称
     * @return 新建的 ChatClient
     */
    private ChatClient createChatClient(String providerName) {
        if (configLoader == null) {
            throw new BusinessException(ErrorCodeEnum.AI_SERVICE_UNAVAILABLE,
                    "LlmProviderRegistry 尚未初始化，请检查 LlmProviderServiceImpl 是否正确启动");
        }

        // 加载 Provider 配置（从数据库查询）
        ProviderConfig config = configLoader.apply(providerName);
        if (config == null) {
            throw new BusinessException(ErrorCodeEnum.PROVIDER_NOT_FOUND,
                    "LLM Provider 不存在或已被删除: " + providerName);
        }

        log.info("[LlmProviderRegistry] Building ChatClient - provider={}, baseUrl={}, model={}",
                providerName, config.baseUrl(), config.model());

        // 构建 OpenAI 兼容 API（支持 DashScope、GLM 等任何 OpenAI 协议 Provider）
        OpenAiApi openAiApi = OpenAiApi.builder()
                .baseUrl(config.baseUrl())
                .apiKey(config.apiKey())
                .build();

        // 构建 ChatModel
        //OpenAiChatModel chatModel = new OpenAiChatModel(
        //        openAiApi,
        //        OpenAiChatOptions.builder()
        //                .model(config.model())
        //                .temperature(0.2)
        //                .build()
        //);
        //
        //return ChatClient.builder(chatModel).build();
        return null;
    }

    // ==================== Provider 配置数据类 ====================

    /**
     * Provider 配置快照（用于传递解密后的 API Key 等信息）
     *
     * @param baseUrl API 基础 URL
     * @param apiKey  解密后的 API 密钥
     * @param model   默认模型名称
     */
    public record ProviderConfig(String baseUrl, String apiKey, String model) {
    }
}
