package com.mhd.interview.web.modules.resume.dto;

import java.time.LocalDateTime;

/**
 * 简历列表项 DTO（对应前端 ResumeListItem 类型）
 *
 * @param id               简历 ID
 * @param filename         原始文件名
 * @param fileSize         文件大小（字节）
 * @param uploadTime       上传时间
 * @param accessCount      访问次数
 * @param analysisStatus   分析状态
 * @param latestScore      上次简历分数
 * @param lastAnalysisTime 上次分析时间
 * @param interviewCount   面试次数
 */
public record ResumeListItemDTO(
        String id,
        String filename,
        Long fileSize,
        LocalDateTime uploadTime,
        Integer accessCount,
        String analysisStatus,
        Integer latestScore,
        LocalDateTime lastAnalysisTime,
        Integer interviewCount
) {
}
