package com.mhd.interview.web.common.exception;

/**
 * 限流异常，当请求触发滑动窗口限流规则时抛出
 *
 * @author mhd
 */
public class RateLimitExceededException extends RuntimeException {

    /** 触发限流的 Redis Key */
    private final String key;

    /** 限流阈值（允许的最大请求次数） */
    private final long count;

    /** 时间窗口（毫秒） */
    private final long intervalMs;

    /**
     * 构造限流异常
     *
     * @param key        触发限流的 Redis Key
     * @param count      限流阈值
     * @param intervalMs 时间窗口（毫秒）
     */
    public RateLimitExceededException(String key, long count, long intervalMs) {
        super(String.format("Rate limit exceeded: key=%s, count=%d, interval=%dms", key, count, intervalMs));
        this.key = key;
        this.count = count;
        this.intervalMs = intervalMs;
    }

    /**
     * 获取触发限流的 Redis Key
     *
     * @return Redis Key
     */
    public String getKey() {
        return key;
    }

    /**
     * 获取限流阈值
     *
     * @return 限流阈值
     */
    public long getCount() {
        return count;
    }

    /**
     * 获取时间窗口（毫秒）
     *
     * @return 时间窗口毫秒数
     */
    public long getIntervalMs() {
        return intervalMs;
    }
}
