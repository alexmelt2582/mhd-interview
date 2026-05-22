package com.mhd.interview.web.common.async;

import com.mhd.interview.web.common.constant.StreamConstants;
import com.mhd.interview.web.infrastructure.redis.RedisStreamService;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Redis Stream 消息生产者抽象基类
 * <p>
 * 子类只需实现 {@link #streamKey()}、{@link #buildMessage(Object)}、
 * {@link #payloadIdentifier(Object)} 和 {@link #onSendFailed(Object, Exception)} 即可完成消息发送
 *
 * @param <T> 任务载荷类型
 * @author zhao-hao-dong
 */
@Slf4j
public abstract class AbstractStreamProducer<T> {

    /**
     * Redis Stream 操作服务
     */
    private final RedisStreamService redisStreamService;

    /**
     * 构造生产者，注入 RedisStreamService
     *
     * @param redisStreamService Redis Stream 操作服务
     */
    protected AbstractStreamProducer(RedisStreamService redisStreamService) {
        this.redisStreamService = redisStreamService;
    }

    /**
     * 发送任务消息到 Redis Stream
     * <p>
     * 封装了发送逻辑和失败处理，子类通过调用此方法发送消息
     *
     * @param payload 任务载荷
     */
    protected void sendTask(T payload) {
        try {
            // 构建消息体并发送到对应的 Stream
            String messageId = redisStreamService.streamAdd(
                    streamKey(),
                    buildMessage(payload),
                    StreamConstants.STREAM_MAX_LEN
            );
            log.info("{}任务已发送到Stream: {}, messageId={}",
                    taskDisplayName(), payloadIdentifier(payload), messageId);
            onSendSuccess(payload);
        } catch (Exception e) {
            log.error("发送{}任务失败: {}, error={}",
                    taskDisplayName(), payloadIdentifier(payload), e.getMessage(), e);
            // 通知子类处理发送失败的情况（如更新数据库状态为失败）
            onSendFailed(payload, e);
        }
    }

    /**
     * 截断错误信息，防止超出数据库字段长度限制
     *
     * @param error 原始错误信息
     * @return 截断后的错误信息（最大500字符）
     */
    protected String truncateError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > 500 ? error.substring(0, 500) : error;
    }

    /**
     * 任务显示名称，用于日志输出
     *
     * @return 任务名称
     */
    protected abstract String taskDisplayName();

    /**
     * 消息投递目标 Stream Key
     *
     * @return Stream Key
     */
    protected abstract String streamKey();

    /**
     * 将任务载荷转换为 Stream 消息 Map
     *
     * @param payload 任务载荷
     * @return 消息字段 Map
     */
    protected abstract Map<String, String> buildMessage(T payload);

    /**
     * 提取载荷的唯一标识字符串，用于日志输出
     *
     * @param payload 任务载荷
     * @return 标识字符串
     */
    protected abstract String payloadIdentifier(T payload);

    /**
     * 消息发送成功时的处理逻辑
     *
     * @param payload 任务载荷
     */
    protected abstract void onSendSuccess(T payload);

    /**
     * 消息发送失败时的处理逻辑（如将状态更新为失败）
     *
     * @param payload 任务载荷
     * @param error   错误异常
     */
    protected abstract void onSendFailed(T payload, Exception error);
}
