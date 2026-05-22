package com.mhd.interview.web.common.async;

import com.mhd.interview.web.common.constant.StreamConstants;
import com.mhd.interview.web.infrastructure.redis.RedisStreamService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.StreamMessageId;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Redis Stream 消息消费者抽象基
 * <p>
 * 子类需实现消息解析、状态更新、业务处理和重试逻辑
 * 框架负责消费循环、ACK、重试计数等通用逻辑
 *
 * @param <T> 任务载荷类型
 * @author zhao-hao-dong
 */
@Slf4j
public abstract class AbstractStreamConsumer<T> {

    private final RedisStreamService redisStreamService;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private ExecutorService executorService;
    /** 消费者实例名（组内唯一*/
    private String consumerName;

    /**
     * 构造消费者，注入 RedisStreamService
     *
     * @param redisStreamService Redis Stream 操作服务
     */
    protected AbstractStreamConsumer(RedisStreamService redisStreamService) {
        this.redisStreamService = redisStreamService;
    }

    /**
     * 应用启动后自动初始化消费者线程
     */
    @PostConstruct
    public void init() {
        this.consumerName = consumerPrefix() + UUID.randomUUID().toString().substring(0, 8);
        this.executorService = new ThreadPoolExecutor(
                1, 1, 0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                r -> {
                    Thread t = new Thread(r, threadName());
                    t.setDaemon(true);
                    return t;
                },
                new ThreadPoolExecutor.AbortPolicy()
        );
        running.set(true);
        executorService.submit(this::startConsumer);
        log.info("{} 消费者已启动: consumerName={}", taskDisplayName(), consumerName);
    }

    /**
     * 应用关闭前优雅停止消费者线程
     */
    @PreDestroy
    public void shutdown() {
        running.set(false);
        if (executorService != null) {
            executorService.shutdown();
        }
        log.info("{} 消费者已停止: consumerName={}", taskDisplayName(), consumerName);
    }

    private void startConsumer() {
        try {
            redisStreamService.createStreamGroup(streamKey(), groupName());
            log.info("Redis Stream 消费者组就绪: group={}", groupName());
        } catch (Exception e) {
            log.warn("准备消费者组失败: group={}, error={}", groupName(), e.getMessage());
        }
        consumeLoop();
    }

    private void consumeLoop() {
        while (running.get()) {
            try {
                redisStreamService.streamConsumeMessages(
                        streamKey(), groupName(), consumerName,
                        StreamConstants.BATCH_SIZE, StreamConstants.POLL_INTERVAL_MS,
                        this::handleMessage
                );
            } catch (Exception e) {
                if (Thread.currentThread().isInterrupted()) {
                    log.info("{} 消费线程被中断", taskDisplayName());
                    break;
                }
                log.error("{} 消费消息失败", taskDisplayName(), e);
            }
        }
    }

    private void handleMessage(StreamMessageId messageId, Map<String, String> data) {
        T payload = parsePayload(messageId, data);
        if (payload == null) {
            ackMessage(messageId);
            return;
        }
        int retryCount = parseRetryCount(data);
        log.info("处理 {} 任务: payload={}, retryCount={}", taskDisplayName(), payloadIdentifier(payload), retryCount);
        try {
            markProcessing(payload);
            processBusiness(payload);
            markCompleted(payload);
            ackMessage(messageId);
            log.info("{} 任务处理完成: {}", taskDisplayName(), payloadIdentifier(payload));
        } catch (Exception e) {
            log.error("{} 任务处理失败: {}", taskDisplayName(), payloadIdentifier(payload), e);
            if (retryCount < StreamConstants.MAX_RETRY) {
                retryMessage(payload, retryCount + 1);
            } else {
                markFailed(payload, e);
            }
            ackMessage(messageId);
        }
    }

    /**
     * 从消息字段中解析重试次数
     *
     * @param data 消息数据
     * @return 重试次数
     */
    protected int parseRetryCount(Map<String, String> data) {
        try {
            return Integer.parseInt(data.getOrDefault(StreamConstants.FIELD_RETRY_COUNT, "0"));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * 截断错误信息，最多 500 字符
     *
     * @param error 原始错误信息
     * @return 截断后的错误信息
     */
    protected String truncateError(String error) {
        if (error == null) {
            return null;
        }
        return error.length() > 500 ? error.substring(0, 500) : error;
    }

    private void ackMessage(StreamMessageId messageId) {
        try {
            redisStreamService.streamAck(streamKey(), groupName(), messageId);
        } catch (Exception e) {
            log.error("ACK 消息失败: messageId={}", messageId, e);
        }
    }

    /**
     * 获取 RedisStreamService，供子类retryMessage 中使
     *
     * @return RedisStreamService
     */
    protected RedisStreamService redisStreamService() {
        return redisStreamService;
    }

    /** 任务显示名称，用于日志 */
    protected abstract String taskDisplayName();

    /** Stream Key */
    protected abstract String streamKey();

    /** 消费者组名称 */
    protected abstract String groupName();

    /** 消费者名前缀 */
    protected abstract String consumerPrefix();

    /** 消费线程名称 */
    protected abstract String threadName();

    /**
     * 解析消息为业务载荷，返回 null 则直接 ACK 丢弃
     *
     * @param messageId 消息 ID
     * @param data      消息数据
     * @return 解析后的业务载荷
     */
    protected abstract T parsePayload(StreamMessageId messageId, Map<String, String> data);

    /**
     * 提取载荷唯一标识字符串，用于日志
     *
     * @param payload 业务载荷
     * @return 标识字符
     */
    protected abstract String payloadIdentifier(T payload);

    /**
     * 将任务状态标记为处理
     *
     * @param payload 业务载荷
     */
    protected abstract void markProcessing(T payload);

    /**
     * 执行核心业务逻辑
     *
     * @param payload 业务载荷
     */
    protected abstract void processBusiness(T payload);

    /**
     * 将任务状态标记为已完成
     *
     * @param payload 业务载荷
     */
    protected abstract void markCompleted(T payload);

    /**
     * 将任务状态标记为最终失败
     *
     * @param payload 业务载荷
     * @param error   错误异常
     */
    protected abstract void markFailed(T payload, Exception error);

    /**
     * 重新将消息投Stream 进行重试
     *
     * @param payload    业务载荷
     * @param retryCount 新的重试次数
     */
    protected abstract void retryMessage(T payload, int retryCount);
}
