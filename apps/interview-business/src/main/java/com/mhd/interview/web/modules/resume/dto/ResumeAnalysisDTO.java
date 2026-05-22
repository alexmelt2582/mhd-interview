package com.mhd.interview.web.modules.resume.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历分析结果 DTO
 *
 * @param id              分析记录 ID
 * @param resumeId        关联简历 ID
 * @param score           综合评分（0-100）
 * @param contentScore    内容完整性
 * @param structureScore  结构清晰度
 * @param skillMatchScore 技能匹配度
 * @param expressionScore 表达专业性
 * @param projectScore    项目经验
 * @param summary         AI 分析摘要
 * @param analysisTime    分析完成时间
 * @param strengths       核心优势
 * @param suggestions     优化建议
 */
public record ResumeAnalysisDTO(
        String id,
        String resumeId,
        Integer score,
        Integer contentScore,
        Integer structureScore,
        Integer skillMatchScore,
        Integer expressionScore,
        Integer projectScore,
        String summary,
        LocalDateTime analysisTime,
        List<String> strengths,
        List<SuggestionDTO> suggestions
) {
    public record SuggestionDTO(
            String category,
            String priority,
            String issue,
            String recommendation
    ) {
    }
}
