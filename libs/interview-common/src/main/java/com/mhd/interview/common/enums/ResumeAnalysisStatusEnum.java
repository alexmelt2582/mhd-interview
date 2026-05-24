package com.mhd.interview.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * AI 简历分析状态枚举
 *
 * @author zhao-hao-dong
 */
@Getter
@AllArgsConstructor
public enum ResumeAnalysisStatusEnum implements PowerfulEnum{
    PENDING(0, "待解析"),
    ANALYZING(1, "解析中"),
    COMPLETED(2, "已完成"),
    FAILED(3, "失败")
    ;
    private final Integer code;
    private final String description;
}
