package com.mhd.interview.web.modules.resume.stream;

import com.mhd.interview.common.enums.ResumeAnalysisStatusEnum;
import com.mhd.interview.common.web.utils.SpringUtils;
import com.mhd.interview.web.common.async.AbstractStreamProducer;
import com.mhd.interview.web.common.constant.StreamConstants;
import com.mhd.interview.web.infrastructure.redis.RedisStreamService;
import com.mhd.interview.web.modules.resume.entity.ResumeEntity;
import com.mhd.interview.web.modules.resume.service.IResumeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Objects;

/**
 * 简历分析任务 Stream 生产者
 * <p>
 * 将简历分析任务投递到 {@value StreamConstants#RESUME_ANALYZE_STREAM} Stream，
 * 由 {@link ResumeAnalysisConsumer} 异步消费
 *
 * @author zhao-hao-dong
 */
@Component
@Slf4j
public class ResumeAnalysisProducer extends AbstractStreamProducer<ResumeAnalysisPayload> {

    /**
     * 构造函数，注入 RedisStreamService
     *
     * @param redisStreamService Redis Stream 操作服务
     */
    public ResumeAnalysisProducer(RedisStreamService redisStreamService) {
        super(redisStreamService);
    }

    /**
     * 发送分析任务到 Redis Stream
     *
     * @param resumeId 简历ID
     * @param content  简历内容
     */
    public void sendAnalyzeTask(Long resumeId, String content) {
        sendTask(new ResumeAnalysisPayload(resumeId, content));
    }

    @Override
    protected String streamKey() {
        return StreamConstants.RESUME_ANALYZE_STREAM;
    }

    @Override
    protected Map<String, String> buildMessage(ResumeAnalysisPayload payload) {
        return Map.of(
                StreamConstants.FIELD_ID, String.valueOf(payload.resumeId()),
                StreamConstants.FIELD_CONTENT, payload.content()
        );
    }

    @Override
    protected String payloadIdentifier(ResumeAnalysisPayload payload) {
        return "resumeId=" + payload.resumeId();
    }

    @Override
    protected void onSendSuccess(ResumeAnalysisPayload payload) {
        //updateAnalyzeStatus(payload.resumeId(), ResumeAnalysisStatusEnum.FAILED, null);
        log.info("简历分析任务发送成功，resumeId={}", payload.resumeId());

    }

    @Override
    protected void onSendFailed(ResumeAnalysisPayload payload, Exception e) {
        String errMsg = truncateError("任务入队失败: " + e.getMessage());
        updateAnalyzeStatus(payload.resumeId(), ResumeAnalysisStatusEnum.FAILED, truncateError(errMsg));
        log.error("简历分析任务发送失败，resumeId={}, error={}", payload.resumeId(), e.getMessage());
    }

    @Override
    protected String taskDisplayName() {
        return "简历分析";
    }

    /**
     * 更新分析状态
     */
    private void updateAnalyzeStatus(Long resumeId, ResumeAnalysisStatusEnum status, String error) {
        IResumeService resumeService = SpringUtils.getBean(IResumeService.class);
        ResumeEntity dbResume = resumeService.selectResumeById(resumeId);
        if (!Objects.isNull(dbResume)) {
            resumeService.updateResumeAnalysisInfo(resumeId, status, error);
            log.debug("分析状态已更新: resumeId={}, status={}", resumeId, status);
        }
    }
}
