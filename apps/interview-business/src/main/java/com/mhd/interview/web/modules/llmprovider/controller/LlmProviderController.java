package com.mhd.interview.web.modules.llmprovider.controller;

import com.mhd.interview.common.respnsedata.BaseResultUtils;
import com.mhd.interview.common.respnsedata.BaseResponse;
import com.mhd.interview.web.common.annotation.RateLimit;
import com.mhd.interview.web.modules.llmprovider.dto.CreateProviderRequest;
import com.mhd.interview.web.modules.llmprovider.dto.LlmProviderDTO;
import com.mhd.interview.web.modules.llmprovider.dto.ProviderTestResultDTO;
import com.mhd.interview.web.modules.llmprovider.dto.UpdateProviderRequest;
import com.mhd.interview.web.modules.llmprovider.service.ILlmProviderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * LLM Provider 管理接口
 *
 * @author mhd
 */
@Tag(name = "LLM Provider 管理")
@RestController
@RequestMapping("/api/llmprovider")
@RequiredArgsConstructor
public class LlmProviderController {

    private final ILlmProviderService llmProviderService;

    /**
     * 查询所有 LLM Provider 列表
     *
     * @return Provider 列表
     */
    @Operation(summary = "查询 Provider 列表")
    @GetMapping("/list")
    public BaseResponse<List<LlmProviderDTO>> list() {
        return BaseResultUtils.successOfData(llmProviderService.selectLlmProviderList());
    }

    /**
     * 根据 ID 查询 Provider 详情
     *
     * @param id Provider ID
     * @return Provider 详情
     */
    @Operation(summary = "查询 Provider 详情")
    @GetMapping("/{id}")
    public BaseResponse<LlmProviderDTO> getById(@PathVariable Long id) {
        return BaseResultUtils.successOfData(llmProviderService.selectLlmProviderById(id));
    }

    /**
     * 查询默认 Provider
     *
     * @return 默认 Provider，未设置时返回 null
     */
    @Operation(summary = "查询默认 Provider")
    @GetMapping("/default")
    public BaseResponse<LlmProviderDTO> getDefault() {
        return BaseResultUtils.successOfData(llmProviderService.selectDefaultLlmProvider());
    }

    /**
     * 新增 LLM Provider
     *
     * @param request 创建请求体
     * @return 操作结果
     */
    @Operation(summary = "新增 Provider")
    @PostMapping
    public BaseResponse<Void> create(@RequestBody CreateProviderRequest request) {
        llmProviderService.insertLlmProvider(request);
        return BaseResultUtils.success();
    }

    /**
     * 更新 LLM Provider 信息
     *
     * @param id      Provider ID
     * @param request 更新请求体
     * @return 操作结果
     */
    @Operation(summary = "更新 Provider")
    @PutMapping("/{id}")
    public BaseResponse<Void> update(@PathVariable Long id, @RequestBody UpdateProviderRequest request) {
        llmProviderService.updateLlmProvider(id, request);
        return BaseResultUtils.success();
    }

    /**
     * 批量删除 LLM Provider
     *
     * @param ids Provider ID 列表
     * @return 操作结果
     */
    @Operation(summary = "批量删除 Provider")
    @DeleteMapping
    public BaseResponse<Void> delete(@RequestBody List<Long> ids) {
        llmProviderService.deleteLlmProviderByIds(ids);
        return BaseResultUtils.success();
    }

    /**
     * 设置默认 Provider
     *
     * @param id Provider ID
     * @return 操作结果
     */
    @Operation(summary = "设置默认 Provider")
    @PutMapping("/{id}/default")
    public BaseResponse<Void> setDefault(@PathVariable Long id) {
        llmProviderService.updateDefaultLlmProvider(id);
        return BaseResultUtils.success();
    }

    /**
     * 测试 Provider 连通性
     *
     * @param id Provider ID
     * @return 测试结果
     */
    @Operation(summary = "测试 Provider 连通性")
    @RateLimit(dimension = RateLimit.Dimension.IP, count = 5)
    @PostMapping("/{id}/test")
    public BaseResponse<ProviderTestResultDTO> test(@PathVariable Long id) {
        return BaseResultUtils.successOfData(llmProviderService.testLlmProviderConnection(id));
    }
}
