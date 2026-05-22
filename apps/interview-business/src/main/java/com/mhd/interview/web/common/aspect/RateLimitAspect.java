package com.mhd.interview.web.common.aspect;

import com.mhd.interview.web.common.annotation.RateLimit;
import com.mhd.interview.web.common.exception.RateLimitExceededException;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * 限流 AOP 切面
 * <p>
 * 支持可重复注�?{@link RateLimit}，逐条执行独立的限流规则，任一规则超限即拒绝请求�?
 * 使用 Redisson + Redis Lua 脚本实现滑动窗口算法�?
 *
 * @author mhd
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private final RedissonClient redissonClient;

    /** Lua 脚本内容（静态加载，应用启动时读取） */
    private static final String LUA_SCRIPT;

    /** Lua 脚本 SHA1（用�?evalSha 避免重复传输脚本内容�?*/
    private String luaScriptSha;

    /** Redisson 脚本操作对象 */
    private RScript rScript;

    static {
        try {
            ClassPathResource resource = new ClassPathResource("scripts/rate_limit_single.lua");
            LUA_SCRIPT = new String(resource.getContentAsByteArray(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("加载限流 Lua 脚本失败: scripts/rate_limit_single.lua", e);
        }
    }

    /**
     * 应用启动后初始化：创�?RScript 实例并预加载 Lua 脚本
     */
    @PostConstruct
    public void init() {
        rScript = redissonClient.getScript(StringCodec.INSTANCE);
        loadScript();
    }

    /**
     * 加载 Lua 脚本�?Redis，返�?SHA1
     */
    private void loadScript() {
        this.luaScriptSha = rScript.scriptLoad(LUA_SCRIPT);
        log.info("限流 Lua 脚本加载完成, SHA1: {}", luaScriptSha);
    }

    /**
     * 环绕通知：拦截带 {@link RateLimit} �?{@link RateLimit.Container} 注解的方�?
     *
     * @param joinPoint 切入�?
     * @return 方法执行结果
     * @throws Throwable 方法执行或限流拒绝异�?
     */
    @Around("@annotation(com.mhd.interview.web.common.annotation.RateLimit) || " +
            "@annotation(com.mhd.interview.web.common.annotation.RateLimit.Container)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String className = method.getDeclaringClass().getSimpleName();
        String methodName = method.getName();

        // 获取方法上所�?@RateLimit 注解（支持可重复注解�?
        RateLimit[] rules = method.getAnnotationsByType(RateLimit.class);
        long nowMs = System.currentTimeMillis();
        String requestId = UUID.randomUUID().toString();

        // 逐条规则执行限流检查，任一超限即拒�?
        for (RateLimit rule : rules) {
            long intervalMs = rule.timeUnit().toMillis(rule.interval());
            String key = generateKey(className, methodName, rule.dimension());

            Long result = executeRateLimitScript(key, nowMs, requestId, intervalMs, rule.count());

            if (result == null || result == 0) {
                log.warn("限流触发，拒绝请�? key={}, count={} per {}ms", key, rule.count(), intervalMs);
                throw new RateLimitExceededException("请求过于频繁，请稍后再试");
            }
        }

        return joinPoint.proceed();
    }

    /**
     * 执行限流 Lua 脚本，支�?NOSCRIPT 错误时自动重新加载并重试
     *
     * @param key        Redis Key
     * @param nowMs      当前时间戳（毫秒�?
     * @param requestId  请求唯一标识
     * @param intervalMs 时间窗口（毫秒）
     * @param count      允许的最大请求次�?
     * @return 脚本返回值（�?表示通过�?或null表示超限�?
     */
    private Long executeRateLimitScript(String key, long nowMs, String requestId, long intervalMs, long count) {
        List<Object> keysList = Collections.singletonList(key);
        Object[] args = {
                String.valueOf(nowMs),
                String.valueOf(1),
                String.valueOf(intervalMs),
                String.valueOf(count),
                requestId
        };

        try {
            Object resultObj = rScript.evalSha(
                    RScript.Mode.READ_WRITE,
                    luaScriptSha,
                    RScript.ReturnType.VALUE,
                    keysList,
                    args
            );
            return convertToLong(resultObj);
        } catch (org.redisson.client.RedisException e) {
            // Redis 重启后脚本缓存丢失，重新加载并重�?
            if (e.getMessage() != null && e.getMessage().contains("NOSCRIPT")) {
                loadScript();
                Object resultObj = rScript.evalSha(
                        RScript.Mode.READ_WRITE,
                        luaScriptSha,
                        RScript.ReturnType.VALUE,
                        keysList,
                        args
                );
                return convertToLong(resultObj);
            }
            throw e;
        }
    }

    /**
     * 生成限流 Redis Key，格式：ratelimit:{ClassName:MethodName}:dimension[:ip/userId]
     *
     * @param className  类名
     * @param methodName 方法�?
     * @param dimension  限流维度
     * @return Redis Key
     */
    private String generateKey(String className, String methodName, RateLimit.Dimension dimension) {
        String hashTag = "{" + className + ":" + methodName + "}";
        String keyPrefix = "ratelimit:" + hashTag;

        return switch (dimension) {
            case GLOBAL -> keyPrefix + ":global";
            case IP -> keyPrefix + ":ip:" + getClientIp();
            case USER -> keyPrefix + ":user:" + getCurrentUserId();
        };
    }

    /**
     * 将脚本返回值转换为 Long
     *
     * @param obj 返回值对�?
     * @return Long 值，无法转换时返�?null
     */
    private Long convertToLong(Object obj) {
        if (obj instanceof Number n) {
            return n.longValue();
        }
        if (obj instanceof String s) {
            try {
                return Long.parseLong(s);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    /**
     * 获取客户端真�?IP（依次从 X-Forwarded-For、X-Real-IP、RemoteAddr 获取�?
     *
     * @return 客户�?IP 字符�?
     */
    private String getClientIp() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "unknown";
        }

        HttpServletRequest request = attributes.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip.split(",")[0].trim();
        }
        ip = request.getHeader("X-Real-IP");
        if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
            return ip;
        }
        String remoteAddr = request.getRemoteAddr();
        return remoteAddr != null ? remoteAddr : "unknown";
    }

    /**
     * 获取当前用户 ID（从请求属性或请求头中读取，未登录时降级为 "anonymous"�?
     *
     * @return 用户 ID 字符�?
     */
    private String getCurrentUserId() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return "anonymous";
        }
        HttpServletRequest request = attributes.getRequest();
        Object userId = request.getAttribute("userId");
        if (userId != null) {
            return userId.toString();
        }
        String headerUserId = request.getHeader("X-User-Id");
        return headerUserId != null ? headerUserId : "anonymous";
    }
}
