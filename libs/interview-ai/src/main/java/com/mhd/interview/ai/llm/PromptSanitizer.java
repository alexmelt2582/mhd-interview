package com.mhd.interview.ai.llm;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Prompt 注入攻击防护工具类
 * <p>
 * 过滤用户输入中常见的 Prompt 注入攻击模式，防止恶意用户通过构造特殊输入
 * 绕过系统 Prompt 的约束，执行未授权操作。
 *
 * @author mhd
 */
@Component
public class PromptSanitizer {

    /**
     * 常见 Prompt 注入攻击模式（忽略大小写匹配）
     */
    private static final Pattern[] INJECTION_PATTERNS = {
            // 角色覆盖攻击
            Pattern.compile("ignore\\s+(previous|prior|all|above)\\s+(instructions?|prompts?|directives?|context)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("disregard\\s+(previous|prior|all|above)\\s+(instructions?|prompts?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("forget\\s+(previous|prior|all|above)\\s+(instructions?|prompts?|context)", Pattern.CASE_INSENSITIVE),
            // 角色扮演攻击
            Pattern.compile("you\\s+are\\s+now\\s+(a|an)\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("act\\s+as\\s+(a|an)\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("pretend\\s+(you\\s+are|to\\s+be)\\s+", Pattern.CASE_INSENSITIVE),
            Pattern.compile("roleplay\\s+as\\s+", Pattern.CASE_INSENSITIVE),
            // 系统指令覆盖
            Pattern.compile("system\\s*:\\s*you\\s+are", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\[system\\]", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\[instruction\\]", Pattern.CASE_INSENSITIVE),
            // 越权指令
            Pattern.compile("reveal\\s+(your|the)\\s+(system|hidden|secret)\\s+(prompt|instructions?)", Pattern.CASE_INSENSITIVE),
            Pattern.compile("print\\s+(your|the)\\s+(system|original)\\s+(prompt|instructions?)", Pattern.CASE_INSENSITIVE),
    };

    /**
     * 最大允许的用户输入长度（字符数）
     */
    private static final int MAX_INPUT_LENGTH = 10000;

    /**
     * 检查并清洗用户输入，过滤 Prompt 注入攻击模式
     * <p>
     * 检测到注入攻击时，将对应内容替换为 {@code [filtered]}
     *
     * @param userInput 用户原始输入
     * @return 清洗后的安全输入，null 时返回空字符串
     */
    public String sanitize(String userInput) {
        if (userInput == null) {
            return "";
        }

        // 截断超长输入
        String sanitized = userInput.length() > MAX_INPUT_LENGTH
                ? userInput.substring(0, MAX_INPUT_LENGTH)
                : userInput;

        // 逐一匹配并替换注入攻击模式
        for (Pattern pattern : INJECTION_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("[filtered]");
        }

        return sanitized;
    }

    /**
     * 仅检查输入是否包含注入攻击，不修改原文
     *
     * @param userInput 用户输入
     * @return {@code true} 表示检测到潜在注入攻击
     */
    public boolean containsInjection(String userInput) {
        if (userInput == null || userInput.isBlank()) {
            return false;
        }
        for (Pattern pattern : INJECTION_PATTERNS) {
            if (pattern.matcher(userInput).find()) {
                return true;
            }
        }
        return false;
    }
}
