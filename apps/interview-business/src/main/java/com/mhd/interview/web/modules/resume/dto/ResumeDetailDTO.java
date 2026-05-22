package com.mhd.interview.web.modules.resume.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 简历详情 DTO（对应前端 ResumeDetail 类型）
 *
 * @param id             简历 ID
 * @param filename       文件名
 * @param fileSize       文件大小（字节）
 * @param contentType    MIME 类型
 * @param fileUrl        文件下载预签名 URL（有效期1小时）
 * @param uploadTime     上传时间
 * @param accessCount    访问次数
 * @param parseText      解析后的简历文本
 * @param analysisStatus 分析状态
 * @param analysisError  分析错误原因
 * @param analyses       分析历史记录列表
 * @param interviews     面试列表
 */
public record ResumeDetailDTO(
        String id,
        String filename,
        Long fileSize,
        String contentType,
        String fileUrl,
        LocalDateTime uploadTime,
        Integer accessCount,
        String parseText,
        String analysisStatus,
        String analysisError,
        List<ResumeAnalysisDTO> analyses,
        List<Object> interviews
) {
}

