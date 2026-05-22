package com.mhd.interview.web.modules.resume.service;

import com.mhd.interview.web.modules.base.ExportResult;
import com.mhd.interview.web.modules.resume.dto.ResumeAnalysisDTO;

/**
 * 简历 AI 分析服务接口
 *
 * @author zhao-hao-dong
 */
public interface IResumeAnalysisService {

    /**
     * 根据简历 ID 查询最新的分析结果
     *
     * @param resumeId 简历 ID
     * @return 分析结果 DTO，未分析完成时返回 null
     */
    ResumeAnalysisDTO selectResumeAnalysisByResumeId(Long resumeId);

    /**
     * 执行简历 AI 分析（由 Redis Stream 消费者异步调用）
     * <p>
     * 调用 LLM 分析简历文本，将结果以 JSON 结构化存储到 t_resume_analysis 表，
     * 并更新简历状态为 COMPLETED（或 FAILED）
     *
     * @param resumeId 简历 ID
     * @param content  内容
     */
    void doAnalyzeResume(Long resumeId, String content);

    /**
     * 导出简历分析报告 PDF
     *
     * @param resumeId 简历 ID（需已完成分析）
     * @return 导出结果
     */
    ExportResult exportResumeAnalysisPdf(Long resumeId);
}
