package com.mhd.interview.web.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 接口限流注解，支持多维度叠加（可重复注解）
 * <p>
 * 示例：同时限制全局 10次/分钟 + 单IP 5次/分钟：
 * <pre>
 * {@literal @}RateLimit(dimension = RateLimit.Dimension.GLOBAL, count = 10)
 * {@literal @}RateLimit(dimension = RateLimit.Dimension.IP, count = 5)
 * public Result<?> api() { ... }
 * </pre>
 *
 * @author mhd
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(RateLimit.Container.class)
public @interface RateLimit {

    /**
     * 限流维度枚举
     */
    enum Dimension {
        /** 全局维度：所有用户共享计数 */
        GLOBAL,
        /** IP 维度：按请求来源 IP 单独计数 */
        IP,
        /** 用户维度：按认证用户 ID 单独计数（需登录） */
        USER
    }

    /**
     * 限流维度，默认全局
     *
     * @return 限流维度
     */
    Dimension dimension() default Dimension.GLOBAL;

    /**
     * 时间窗口内允许的最大请求次数
     *
     * @return 最大请求次数
     */
    long count() default 100;

    /**
     * 时间窗口大小
     *
     * @return 时间窗口数值
     */
    long interval() default 1;

    /**
     * 时间单位，默认分钟
     *
     * @return 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.MINUTES;

    /**
     * 可重复注解容器
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    @interface Container {
        /**
         * 多个限流规则
         *
         * @return 限流注解数组
         */
        RateLimit[] value();
    }
}
