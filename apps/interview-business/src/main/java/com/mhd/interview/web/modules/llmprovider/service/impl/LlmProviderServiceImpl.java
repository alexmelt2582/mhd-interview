package com.mhd.interview.web.modules.llmprovider.service.impl;

import cn.hutool.crypto.symmetric.AES;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mhd.interview.ai.llm.LlmProviderRegistry;
import com.mhd.interview.common.enums.ErrorCodeEnum;
import com.mhd.interview.common.exception.BusinessException;
import com.mhd.interview.web.modules.llmprovider.dto.CreateProviderRequest;
import com.mhd.interview.web.modules.llmprovider.dto.LlmProviderDTO;
import com.mhd.interview.web.modules.llmprovider.dto.ProviderTestResultDTO;
import com.mhd.interview.web.modules.llmprovider.dto.UpdateProviderRequest;
import com.mhd.interview.web.modules.llmprovider.entity.LlmProviderEntity;
import com.mhd.interview.web.modules.llmprovider.mapper.LlmProviderMapper;
import com.mhd.interview.web.modules.llmprovider.service.ILlmProviderService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * LLM Provider 服务实现类
 *
 * @author mhd
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LlmProviderServiceImpl implements ILlmProviderService {

    /** LLM Provider Mapper */
    private final LlmProviderMapper baseMapper;
    /** AI Provider 注册中心 */
    private final LlmProviderRegistry llmProviderRegistry;

    /** AES 密钥（从配置文件 app.security.aes-key 注入，16/24/32 字节）*/
    @Value("${app.security.aes-key}")
    private String aesKey;

    // ==================== 初始化 ====================

    /**
     * 应用启动时向 LlmProviderRegistry 注册配置加载器，实现按需创建 ChatClient
     */
    @PostConstruct
    public void registerConfigLoader() {
        llmProviderRegistry.setConfigLoader(providerName -> {
            // 优先按名称精确查找；__default__ 特殊 Key 则查默认 Provider
            LlmProviderEntity entity;
            if ("__default__".equals(providerName)) {
                entity = baseMapper.selectOne(
                        Wrappers.<LlmProviderEntity>lambdaQuery()
                                .eq(LlmProviderEntity::getIsDefault, true)
                                .last("LIMIT 1"));
            } else {
                entity = baseMapper.selectOne(
                        Wrappers.<LlmProviderEntity>lambdaQuery()
                                .eq(LlmProviderEntity::getName, providerName)
                                .last("LIMIT 1"));
            }
            if (entity == null) {
                return null;
            }
            // 解密 API Key 后返回配置快照
            String decryptedKey = decryptApiKey(entity.getApiKey());
            return new LlmProviderRegistry.ProviderConfig(
                    entity.getBaseUrl(), decryptedKey, entity.getModel());
        });
        log.info("[LlmProviderServiceImpl] LlmProviderRegistry 配置加载器注册成功");
    }

    // ==================== 查询 ====================

    /**
     * {@inheritDoc}
     */
    @Override
    public List<LlmProviderDTO> selectLlmProviderList() {
        List<LlmProviderEntity> entities = baseMapper.selectList(
                Wrappers.<LlmProviderEntity>lambdaQuery()
                        .orderByDesc(LlmProviderEntity::getIsDefault)
                        .orderByDesc(LlmProviderEntity::getCreateTime));
        return entities.stream().map(this::toDTO).toList();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LlmProviderDTO selectLlmProviderById(Long id) {
        LlmProviderEntity entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.PROVIDER_NOT_FOUND,
                    "LLM Provider 不存在: " + id);
        }
        return toDTO(entity);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public LlmProviderDTO selectDefaultLlmProvider() {
        LlmProviderEntity entity = baseMapper.selectOne(
                Wrappers.<LlmProviderEntity>lambdaQuery()
                        .eq(LlmProviderEntity::getIsDefault, true)
                        .last("LIMIT 1"));
        return entity == null ? null : toDTO(entity);
    }

    // ==================== 写操作 ====================

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void insertLlmProvider(CreateProviderRequest request) {
        // 检查名称重复
        long count = baseMapper.selectCount(
                Wrappers.<LlmProviderEntity>lambdaQuery()
                        .eq(LlmProviderEntity::getName, request.name()));
        if (count > 0) {
            throw new BusinessException(ErrorCodeEnum.PROVIDER_ALREADY_EXISTS,
                    "Provider 名称已存在: " + request.name());
        }

        LlmProviderEntity entity = new LlmProviderEntity();
        entity.setName(request.name());
        entity.setBaseUrl(request.baseUrl());
        // API Key 加密存储
        entity.setApiKey(encryptApiKey(request.apiKey()));
        entity.setModel(request.model());
        entity.setIsDefault(false);
        entity.setIsDeleted(false);
        baseMapper.insert(entity);
        log.info("新增 LLM Provider: name={}, id={}", entity.getName(), entity.getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateLlmProvider(Long id, UpdateProviderRequest request) {
        LlmProviderEntity entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.PROVIDER_NOT_FOUND,
                    "LLM Provider 不存在: " + id);
        }

        // 只更新不为 null 的字段
        if (request.name() != null) {
            entity.setName(request.name());
        }
        if (request.baseUrl() != null) {
            entity.setBaseUrl(request.baseUrl());
        }
        if (request.apiKey() != null) {
            entity.setApiKey(encryptApiKey(request.apiKey()));
        }
        if (request.model() != null) {
            entity.setModel(request.model());
        }

        baseMapper.updateById(entity);
        // 配置变更后清除缓存，下次访问重建 ChatClient
        llmProviderRegistry.evict(entity.getName());
        log.info("更新 LLM Provider: id={}", id);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteLlmProviderByIds(List<Long> ids) {
        for (Long id : ids) {
            LlmProviderEntity entity = baseMapper.selectById(id);
            if (entity == null) {
                continue;
            }
            // 默认 Provider 禁止删除
            if (Boolean.TRUE.equals(entity.getIsDefault())) {
                throw new BusinessException(ErrorCodeEnum.PROVIDER_DEFAULT_CANNOT_DELETE,
                        "默认 Provider 不可删除，请先更换默认 Provider");
            }
            baseMapper.deleteById(id);
            llmProviderRegistry.evict(entity.getName());
        }
        log.info("批量删除 LLM Provider: ids={}", ids);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDefaultLlmProvider(Long id) {
        // 校验目标 Provider 存在
        LlmProviderEntity target = baseMapper.selectById(id);
        if (target == null) {
            throw new BusinessException(ErrorCodeEnum.PROVIDER_NOT_FOUND,
                    "LLM Provider 不存在: " + id);
        }

        // 先清除全部默认标识
        LambdaUpdateWrapper<LlmProviderEntity> clearWrapper = Wrappers.<LlmProviderEntity>lambdaUpdate()
                .set(LlmProviderEntity::getIsDefault, false)
                .eq(LlmProviderEntity::getIsDefault, true);
        baseMapper.update(clearWrapper);

        // 将目标 Provider 设为默认
        target.setIsDefault(true);
        baseMapper.updateById(target);

        // 刷新注册中心默认 Provider 缓存
        llmProviderRegistry.reload();
        log.info("设置默认 LLM Provider: id={}, name={}", id, target.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ProviderTestResultDTO testLlmProviderConnection(Long id) {
        LlmProviderEntity entity = baseMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException(ErrorCodeEnum.PROVIDER_NOT_FOUND,
                    "LLM Provider 不存在: " + id);
        }

        long start = System.currentTimeMillis();
        try {
            // 通过注册中心获取 ChatClient（或临时创建测试用客户端）
            ChatClient client = llmProviderRegistry.getChatClient(entity.getName());
            // 发送简单提示词验证连通性
            String response = client.prompt()
                    .user("Reply with 'OK' only.")
                    .call()
                    .content();
            long latency = System.currentTimeMillis() - start;
            log.info("Provider 连通性测试成功: id={}, latency={}ms", id, latency);
            return new ProviderTestResultDTO(true, "连接成功，模型响应: " + response, latency);
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - start;
            log.warn("Provider 连通性测试失败: id={}, error={}", id, e.getMessage());
            return new ProviderTestResultDTO(false, "连接失败: " + e.getMessage(), latency);
        }
    }

    // ==================== 内部工具方法 ====================

    /**
     * 将实体转为 DTO（不暴露 apiKey）
     *
     * @param entity Provider 实体
     * @return Provider DTO
     */
    private LlmProviderDTO toDTO(LlmProviderEntity entity) {
        return new LlmProviderDTO(
                entity.getId(),
                entity.getName(),
                entity.getBaseUrl(),
                entity.getModel(),
                entity.getIsDefault(),
                entity.getCreateTime(),
                entity.getUpdateTime()
        );
    }

    /**
     * 使用 AES 对称加密 API Key
     *
     * @param plainKey 明文 API Key
     * @return Base64 编码的密文
     */
    private String encryptApiKey(String plainKey) {
        if (plainKey == null || plainKey.isBlank()) {
            return plainKey;
        }
        try {
            AES aes = new AES(aesKey.getBytes(StandardCharsets.UTF_8));
            return aes.encryptBase64(plainKey);
        } catch (Exception e) {
            throw e;
            //throw new BusinessException(ErrorCodeEnum.PROVIDER_CONFIG_READ_FAILED,
            //        "API Key 加密失败: " + e.getMessage());
        }
    }

    /**
     * 解密 AES 加密的 API Key
     *
     * @param encryptedKey Base64 编码的密文
     * @return 明文 API Key
     */
    private String decryptApiKey(String encryptedKey) {
        if (encryptedKey == null || encryptedKey.isBlank()) {
            return encryptedKey;
        }
        try {
            AES aes = new AES(aesKey.getBytes(StandardCharsets.UTF_8));
            return aes.decryptStr(encryptedKey);
        } catch (Exception e) {
            throw e;
            //throw new BusinessException(ErrorCodeEnum.PROVIDER_CONFIG_READ_FAILED,
            //        "API Key 解密失败，请重新配置 Provider: " + e.getMessage());
        }
    }
}
