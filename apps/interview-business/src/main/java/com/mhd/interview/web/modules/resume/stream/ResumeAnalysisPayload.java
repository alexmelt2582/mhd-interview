package com.mhd.interview.web.modules.resume.stream;

/**
 * 简历分析任务对象
 *
 * @param resumeId 简历 ID
 * @param content  内容
 */
public record ResumeAnalysisPayload(Long resumeId, String content) {
}
