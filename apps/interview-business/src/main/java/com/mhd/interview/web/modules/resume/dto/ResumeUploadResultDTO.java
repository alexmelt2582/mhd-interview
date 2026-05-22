package com.mhd.interview.web.modules.resume.dto;

import java.time.LocalDateTime;

/**
 * 简历上传结果 DTO
 *
 * @param id             简历 ID
 * @param filename       文件名
 * @param analysisStatus 初始分析状态（固定为 PENDING）
 * @param isDuplicate    是否为重复上传（文件哈希相同）
 * @param uploadTime     上传时间
 */
public record ResumeUploadResultDTO(
        String id,
        String filename,
        String analysisStatus,
        boolean isDuplicate,
        LocalDateTime uploadTime
) {
}
