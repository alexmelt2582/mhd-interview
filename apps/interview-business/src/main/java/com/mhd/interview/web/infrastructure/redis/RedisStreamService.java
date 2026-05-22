package com.mhd.interview.web.infrastructure.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RStream;
import org.redisson.api.RedissonClient;
import org.redisson.api.StreamMessageId;
import org.redisson.api.stream.StreamAddArgs;
import org.redisson.api.stream.StreamCreateGroupArgs;
import org.redisson.api.stream.StreamReadGroupArgs;
import org.redisson.client.codec.StringCodec;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Map;

/**
 * Redis Stream 操作服务
 * <p>
 * 封装 Redisson 的 Stream API，提供消息发送、消费者组管理、消息消费、ACK 等核心操作。
 * 所有 Key 和组名须通过 {@code StreamConstants} 中的常量传入，禁止硬编码。
 *
 * @author mhd
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RedisStreamService {

    private final RedissonClient redissonClient;

    /**
     * Stream 消息处理器接口
     */
    @FunctionalInterface
    public interface StreamMessageProcessor {
        /**
         * 处理单条 Stream 消息
         *
         * @param messageId 消息 ID
         * @param data      消息数据（字段名→字段值）
         */
        void process(StreamMessageId messageId, Map<String, String> data);
    }

    /**
     * 发送消息到 Redis Stream（不限制长度）
     *
     * @param streamKey Stream 键
     * @param message   消息内容（字段名→字段值）
     * @return 消息 ID 字符串
     */
    public String streamAdd(String streamKey, Map<String, String> message) {
        return streamAdd(streamKey, message, 0);
    }

    /**
     * 发送消息到 Redis Stream（带长度限制）
     *
     * @param streamKey Stream 键
     * @param message   消息内容（字段名→字段值）
     * @param maxLen    最大长度，超过时自动非严格裁剪旧消息，0 表示不限制
     * @return 消息 ID 字符串
     */
    public String streamAdd(String streamKey, Map<String, String> message, int maxLen) {
        RStream<String, String> stream = redissonClient.getStream(streamKey, StringCodec.INSTANCE);
        StreamAddArgs<String, String> args = StreamAddArgs.entries(message);
        if (maxLen > 0) {
            args.trimNonStrict().maxLen(maxLen);
        }
        StreamMessageId messageId = stream.add(args);
        log.debug("发送 Stream 消息: stream={}, messageId={}", streamKey, messageId);
        return messageId.toString();
    }

    /**
     * 创建消费者组（如果不存在则自动创建 Stream 并建组）
     *
     * @param streamKey Stream 键
     * @param groupName 消费者组名
     */
    public void createStreamGroup(String streamKey, String groupName) {
        RStream<String, String> stream = redissonClient.getStream(streamKey, StringCodec.INSTANCE);
        try {
            stream.createGroup(StreamCreateGroupArgs.name(groupName).makeStream());
            log.info("创建 Stream 消费者组: stream={}, group={}", streamKey, groupName);
        } catch (Exception e) {
            // 组已存在时忽略 BUSYGROUP 错误
            if (e instanceof org.redisson.client.RedisException re
                    && re.getMessage() != null
                    && re.getMessage().contains("BUSYGROUP")) {
                return;
            }
            log.warn("创建消费者组失败: stream={}, group={}, error={}", streamKey, groupName, e.getMessage());
        }
    }

    /**
     * 以消费者组模式阻塞读取 Stream 消息并逐条处理
     * <p>
     * 使用 Redis BLOCK 机制，服务端等待消息，比客户端轮询更高效。
     * Redisson 在无消息时可能抛出 ClassCastException（已知 bug），此处已静默处理。
     *
     * @param streamKey      Stream 键
     * @param groupName      消费者组名
     * @param consumerName   消费者名
     * @param count          每次最多读取数量
     * @param blockTimeoutMs 阻塞等待超时（毫秒），0 表示无限等待
     * @param processor      消息处理器
     * @return true 若本次处理了消息；false 若超时无消息
     */
    public boolean streamConsumeMessages(
            String streamKey,
            String groupName,
            String consumerName,
            int count,
            long blockTimeoutMs,
            StreamMessageProcessor processor) {

        RStream<String, String> stream = redissonClient.getStream(streamKey, StringCodec.INSTANCE);
        Map<StreamMessageId, Map<String, String>> messages;
        try {
            messages = stream.readGroup(
                    groupName,
                    consumerName,
                    StreamReadGroupArgs.neverDelivered()
                            .count(count)
                            .timeout(Duration.ofMillis(blockTimeoutMs))
            );
        } catch (ClassCastException e) {
            // Redisson 已知问题：无消息时内部返回空列表导致强转失败，等价于"本次无消息"
            return false;
        }

        if (messages == null || messages.isEmpty()) {
            return false;
        }

        for (Map.Entry<StreamMessageId, Map<String, String>> entry : messages.entrySet()) {
            processor.process(entry.getKey(), entry.getValue());
        }
        return true;
    }

    /**
     * 确认消息已处理（ACK）
     *
     * @param streamKey Stream 键
     * @param groupName 消费者组名
     * @param ids       消息 ID 列表
     */
    public void streamAck(String streamKey, String groupName, StreamMessageId... ids) {
        RStream<String, String> stream = redissonClient.getStream(streamKey, StringCodec.INSTANCE);
        stream.ack(groupName, ids);
    }
}
