package com.mhd.interview.web.common.exception;

/**
 * 限流异常，当请求触发滑动窗口限流规则时抛出
 *
 * @author mhd
 */
public class RateLimitExceededException extends RuntimeException {
    /**
     * 构造限流异常
     *
     * @param err        异常信息
     */
    public RateLimitExceededException(String err) {
        super(err);
    }
}
