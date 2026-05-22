package com.mhd.interview.web.modules.resume.controller;

import com.mhd.interview.common.mybatis.domain.PageParam;
import com.mhd.interview.common.mybatis.domain.PageResponse;
import com.mhd.interview.common.respnsedata.BaseResponse;
import com.mhd.interview.common.respnsedata.BaseResultUtils;
import com.mhd.interview.web.common.annotation.RateLimit;
import com.mhd.interview.web.modules.resume.dto.ResumeAnalysisDTO;
import com.mhd.interview.web.modules.resume.dto.ResumeDetailDTO;
import com.mhd.interview.web.modules.resume.dto.ResumeListItemDTO;
import com.mhd.interview.web.modules.resume.dto.ResumeUploadResultDTO;
import com.mhd.interview.web.modules.resume.service.IResumeAnalysisService;
import com.mhd.interview.web.modules.resume.service.IResumeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/**
 * 简历管理接口
 *
 * @author zhao-hao-dong
 */
@Tag(name = "简历管理")
@RestController
@RequestMapping("/api/resume")
@RequiredArgsConstructor
public class ResumeController {

    private final IResumeService resumeService;
    private final IResumeAnalysisService resumeAnalysisService;

    /**
     * 上传简历（支持 PDF、DOCX、DOC、TXT、MD等），触发异步 AI 分析
     *
     * @param file 简历文件
     * @return 上传结果（含简历 ID、分析状态）
     */
    @Operation(summary = "上传简历")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 5)
    @RateLimit(dimension = RateLimit.Dimension.IP, count = 5)
    public BaseResponse<ResumeUploadResultDTO> upload(@RequestParam("file") MultipartFile file) {
        return BaseResultUtils.successOfData(resumeService.uploadResume(file));
    }

    /**
     * 获取所有简历列表
     */
    @Operation(summary = "获取所有简历列表")
    @GetMapping("")
    public BaseResponse<List<ResumeListItemDTO>> listAll() {
        List<ResumeListItemDTO> resumes = resumeService.selectResumeList();
        return BaseResultUtils.successOfData(resumes);
    }

    /**
     * 分页获取简历列表
     *
     * @param pageParam 分页参数
     * @return 简历分页列表
     */
    @Operation(summary = "分页获取简历列表")
    @GetMapping("/list")
    public BaseResponse<PageResponse<ResumeListItemDTO>> list(PageParam pageParam) {
        return BaseResultUtils.successOfData(resumeService.selectResumePageList(pageParam));
    }

    /**
     * 根据 ID 查询简历详情（包含分析历史）
     *
     * @param id 简历 ID
     * @return 简历详情
     */
    @Operation(summary = "获取简历详情")
    @GetMapping("/{id}/detail")
    public BaseResponse<ResumeDetailDTO> getDetailById(@PathVariable Long id) {
        return BaseResultUtils.successOfData(resumeService.selectResumeDetailById(id));
    }

    /**
     * 删除简历
     *
     * @param id 简历ID
     */
    @Operation(summary = "删除简历")
    @DeleteMapping("/{id}")
    public BaseResponse<Void> delete(@PathVariable Long id) {
        resumeService.deleteResumeByIds(List.of(id));
        return BaseResultUtils.success();
    }

    /**
     * 批量删除简历
     *
     * @param ids 简历 ID 列表
     */
    @Operation(summary = "批量删除简历")
    @DeleteMapping
    public BaseResponse<Void> delete(@RequestBody List<Long> ids) {
        resumeService.deleteResumeByIds(ids);
        return BaseResultUtils.success();
    }

    /**
     * 查询简历分析结果
     *
     * @param id 简历 ID
     * @return 分析结果（含评分、优劣势、建议）
     */
    @Operation(summary = "查询简历分析结果")
    @GetMapping("/{id}/analysis")
    public BaseResponse<ResumeAnalysisDTO> getAnalysis(@PathVariable Long id) {
        return BaseResultUtils.successOfData(resumeAnalysisService.selectResumeAnalysisByResumeId(id));
    }

    /**
     * 重新分析简历（手动重试）
     * 用于分析失败后的重试
     *
     * @param id 简历ID
     * @return 结果
     */
    @Operation(summary = "重新分析简历")
    @PostMapping("/{id}/reanalyze")
    @RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 2)
    @RateLimit(dimension = RateLimit.Dimension.IP, count = 2)
    public BaseResponse<Void> reanalyze(@PathVariable Long id) {
        resumeService.reanalyze(id);
        return BaseResultUtils.success();
    }

    /**
     * 导出简历分析 PDF 报告
     *
     * @param id 简历 ID（需已完成分析）
     */
    @Operation(summary = "导出简历分析 PDF")
    @PostMapping("/{id}/export-pdf")
    public ResponseEntity<byte[]> exportPdf(@PathVariable Long id) {
        try {
            var result = resumeAnalysisService.exportResumeAnalysisPdf(id);
            String filename = URLEncoder.encode(result.filename(), StandardCharsets.UTF_8);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + filename)
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(result.pdfBytes());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public BaseResponse<Map<String, String>> health() {
        return BaseResultUtils.successOfData(Map.of(
                "status", "UP",
                "service", "AI Interview Platform - Resume Service"
        ));
    }
}
