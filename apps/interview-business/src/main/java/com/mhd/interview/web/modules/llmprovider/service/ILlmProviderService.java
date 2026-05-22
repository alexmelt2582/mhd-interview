package com.mhd.interview.web.modules.llmprovider.service;

import com.mhd.interview.web.modules.llmprovider.dto.CreateProviderRequest;
import com.mhd.interview.web.modules.llmprovider.dto.LlmProviderDTO;
import com.mhd.interview.web.modules.llmprovider.dto.ProviderTestResultDTO;
import com.mhd.interview.web.modules.llmprovider.dto.UpdateProviderRequest;

import java.util.List;

/**
 * LLM Provider 服务接口
 *
 * @author mhd
 */
public interface ILlmProviderService {

    /**
     * 查询所有 LLM Provider 列表（不分页）
     *
     * @return Provider 列表
     */
    List<LlmProviderDTO> selectLlmProviderList();

    /**
     * 根据 ID 查询 LLM Provider 详情
     *
     * @param id Provider ID
     * @return Provider 详情，不存在时抛出 BusinessException
     */
    LlmProviderDTO selectLlmProviderById(Long id);

    /**
     * 查询当前默认 LLM Provider
     *
     * @return 默认 Provider 详情，无默认时返回 null
     */
    LlmProviderDTO selectDefaultLlmProvider();

    /**
     * 新增 LLM Provider
     *
     * @param request 创建请求体（包含名称、URL、API Key、模型名）
     */
    void insertLlmProvider(CreateProviderRequest request);

    /**
     * 更新 LLM Provider 信息
     *
     * @param id      Provider ID
     * @param request 更新请求体（null 字段表示不修改）
     */
    void updateLlmProvider(Long id, UpdateProviderRequest request);

    /**
     * 批量删除 LLM Provider
     *
     * @param ids Provider ID 列表
     */
    void deleteLlmProviderByIds(List<Long> ids);

    /**
     * 将指定 Provider 设为默认，并将其他 Provider 的默认标识清除
     *
     * @param id 要设为默认的 Provider ID
     */
    void updateDefaultLlmProvider(Long id);

    /**
     * 测试指定 Provider 的连通性（发送简单提示词，验证 API Key 和 URL 是否有效）
     *
     * @param id Provider ID
     * @return 测试结果（含是否成功、延迟、错误信息）
     */
    ProviderTestResultDTO testLlmProviderConnection(Long id);
}
