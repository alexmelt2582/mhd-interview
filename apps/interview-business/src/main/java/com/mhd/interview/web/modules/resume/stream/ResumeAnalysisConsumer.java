package com.mhd.interview.web.modules.resume.stream;

import com.mhd.interview.common.enums.ResumeAnalysisStatusEnum;
import com.mhd.interview.common.utils.StringUtils;
import com.mhd.interview.web.common.async.AbstractStreamConsumer;
import com.mhd.interview.web.common.constant.StreamConstants;
import com.mhd.interview.web.infrastructure.redis.RedisStreamService;
import com.mhd.interview.web.modules.resume.entity.ResumeEntity;
import com.mhd.interview.web.modules.resume.mapper.ResumeMapper;
import com.mhd.interview.web.modules.resume.service.IResumeAnalysisService;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.StreamMessageId;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 简历分析任务 Stream 消费者
 * <p>
 * 监听 {@value StreamConstants#RESUME_ANALYZE_STREAM} Stream，
 * 异步调用 {@link IResumeAnalysisService#doAnalyzeResume} 执行 AI 分析
 *
 * @author zhao-hao-dong
 */
@Component
@Slf4j
public class ResumeAnalysisConsumer extends AbstractStreamConsumer<ResumeAnalysisPayload> {

    private final IResumeAnalysisService resumeAnalysisService;
    private final ResumeMapper resumeMapper;

    /**
     * 构造函数
     *
     * @param redisStreamService    Redis Stream 操作服务
     * @param resumeAnalysisService 简历分析服务
     * @param resumeMapper          简历 Mapper（用于状态更新）
     */
    public ResumeAnalysisConsumer(RedisStreamService redisStreamService,
                                  IResumeAnalysisService resumeAnalysisService,
                                  ResumeMapper resumeMapper) {
        super(redisStreamService);
        this.resumeAnalysisService = resumeAnalysisService;
        this.resumeMapper = resumeMapper;
    }

    // ==================== AbstractStreamConsumer 抽象方法实现 ====================

    @Override
    protected String taskDisplayName() {
        return "简历分析";
    }

    @Override
    protected String streamKey() {
        return StreamConstants.RESUME_ANALYZE_STREAM;
    }

    @Override
    protected String groupName() {
        return StreamConstants.RESUME_ANALYZE_GROUP;
    }

    @Override
    protected String consumerPrefix() {
        return StreamConstants.RESUME_ANALYZE_CONSUMER;
    }

    @Override
    protected String threadName() {
        return StreamConstants.RESUME_ANALYSIS_THREAD;
    }

    /**
     * 从 Redis Stream 消息中解析任务载体
     *
     * @param messageId 消息 ID
     * @param fields    消息字段 Map
     * @return 解析后的任务载体
     */
    @Override
    protected ResumeAnalysisPayload parsePayload(StreamMessageId messageId, Map<String, String> fields) {
        Long resumeId = Long.parseLong(fields.get(StreamConstants.FIELD_ID));
        String content = fields.getOrDefault(StreamConstants.FIELD_CONTENT, null);
        if (StringUtils.isBlank(content)) {
            log.warn("消息格式错误，跳过: messageId={}", messageId);
            return null;
        }
        return new ResumeAnalysisPayload(resumeId, content);
    }

    /**
     * 获取任务的唯一标识（用于日志）
     *
     * @param payload 任务载体
     * @return 标识字符串
     */
    @Override
    protected String payloadIdentifier(ResumeAnalysisPayload payload) {
        return "resumeId=" + payload.resumeId();
    }

    /**
     * 标记任务为处理中（更新简历状态为 PROCESSING）
     *
     * @param payload 任务载体
     */
    @Override
    protected void markProcessing(ResumeAnalysisPayload payload) {
        updateResumeStatus(payload.resumeId(), ResumeAnalysisStatusEnum.ANALYZING);
    }

    /**
     * 执行核心业务逻辑（调用 AI 分析）
     *
     * @param payload 任务载体
     */
    @Override
    protected void processBusiness(ResumeAnalysisPayload payload) {
        resumeAnalysisService.doAnalyzeResume(payload.resumeId(), payload.content());
    }

    /**
     * 标记任务为完成（更新简历状态为 COMPLETED）
     *
     * @param payload 任务载体
     */
    @Override
    protected void markCompleted(ResumeAnalysisPayload payload) {
        updateResumeStatus(payload.resumeId(), ResumeAnalysisStatusEnum.COMPLETED);
    }

    /**
     * 标记任务为失败（更新简历状态为 FAILED）
     *
     * @param payload 任务载体
     * @param error   失败异常
     */
    @Override
    protected void markFailed(ResumeAnalysisPayload payload, Exception error) {
        updateResumeStatus(payload.resumeId(), ResumeAnalysisStatusEnum.FAILED, truncateError("简历分析失败: " + error.getMessage()));
        log.error("简历分析失败: resumeId={}, error={}", payload.resumeId(), error.getMessage());
    }

    /**
     * 重试任务（重新将任务投递到 Stream）
     *
     * @param payload    任务载体
     * @param retryCount 当前重试次数
     */
    @Override
    protected void retryMessage(ResumeAnalysisPayload payload, int retryCount) {
        Long resumeId = payload.resumeId();
        String content = payload.content();
        // 递增重试计数后重新投递
        int newRetryCount = retryCount + 1;
        Map<String, String> newFields = new java.util.HashMap<>();
        newFields.put(StreamConstants.FIELD_RESUME_ID, String.valueOf(resumeId));
        newFields.put(StreamConstants.FIELD_CONTENT, content);
        newFields.put(StreamConstants.FIELD_RETRY_COUNT, String.valueOf(newRetryCount));
        redisStreamService().streamAdd(streamKey(), newFields);
        log.warn("简历分析任务重试: resumeId={}, retryCount={}", payload.resumeId(), newRetryCount);
    }

    // ==================== 内部工具方法 ====================

    /**
     * 更新简历分析状态
     *
     * @param resumeId 简历 ID
     * @param status   目标状态
     */
    private void updateResumeStatus(Long resumeId, ResumeAnalysisStatusEnum status) {
        updateResumeStatus(resumeId, status, null);
    }

    /**
     * 更新简历分析状态
     *
     * @param resumeId 简历 ID
     * @param status   目标状态
     * @param error    错误描述
     */
    private void updateResumeStatus(Long resumeId, ResumeAnalysisStatusEnum status, String error) {
        ResumeEntity entity = new ResumeEntity();
        entity.setId(resumeId);
        entity.setAnalysisStatus(status.getCode());
        entity.setAnalysisError(error);
        resumeMapper.updateById(entity);
    }
}
