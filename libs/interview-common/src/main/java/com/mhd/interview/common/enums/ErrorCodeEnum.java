package com.mhd.interview.common.enums;

import com.mhd.interview.common.pipeline.ProcessResult;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 错误码枚举类
 *
 * @author zhao-hao-dong
 **/
@Getter
@AllArgsConstructor
public enum ErrorCodeEnum implements ProcessResult {
    /**
     * 错误
     */
    ERROR_500("500", "服务器未知错误"),
    ERROR_400("400", "错误请求"),

    /**
     * OK：操作成功
     */
    SUCCESS("200", "操作成功"),
    FAIL("-1", "操作失败"),

    VALID_FAILED("A0000", "参数校验失败"),

    /**
     * 客户端
     */
    CLIENT_BAD_PARAMETERS("A0001", "客户端参数错误"),
    NO_LOGIN("A0005", "还未登录，请先登录"),
    DUPLICATE_REQUEST("A0007", "重复请求，请勿重复提交"),
    DLQ_RECORD_NOT_FOUND("A0009", "未找到对应的DLQ记录"),

    /**
     * 系统
     */
    SERVICE_ERROR("B0001", "消息服务执行异常"),
    RESOURCE_NOT_FOUND("B0404", "资源不存在"),

    /**
     * pipeline
     */
    CONTEXT_IS_NULL("P0001", "流程上下文为空"),
    BUSINESS_CODE_IS_NULL("P0002", "业务代码为空"),
    PROCESS_TEMPLATE_IS_NULL("P0003", "流程模板配置为空"),
    PROCESS_LIST_IS_NULL("P0004", "业务处理器配置为空"),

    // ==================== 业务模块错误码（来源：interview-guide 集成） ====================

    /**
     * 简历模块 2xxx
     */
    RESUME_NOT_FOUND("2001", "简历不存在"),
    RESUME_PARSE_FAILED("2002", "简历解析失败"),
    RESUME_UPLOAD_FAILED("2003", "简历上传失败"),
    RESUME_DUPLICATE("2004", "简历已存在，已自动关联"),
    RESUME_ANALYSIS_NOT_FOUND("2005", "简历分析结果不存在"),
    RESUME_ANALYSIS_FAILED("2007", "简历分析失败"),

    /**
     * 面试模块 3xxx
     */
    INTERVIEW_SESSION_NOT_FOUND("3001", "面试会话不存在"),
    INTERVIEW_SESSION_ALREADY_ENDED("3002", "面试会话已结束"),
    INTERVIEW_ANSWER_NOT_FOUND("3004", "面试答题记录不存在"),
    INTERVIEW_EVALUATION_FAILED("3005", "面试评估失败"),
    INTERVIEW_QUESTION_GENERATION_FAILED("3006", "面试问题生成失败"),

    /**
     * 对象存储模块 4xxx
     */
    STORAGE_UPLOAD_FAILED("4001", "文件上传失败"),
    STORAGE_DOWNLOAD_FAILED("4002", "文件下载失败"),
    STORAGE_DELETE_FAILED("4003", "文件删除失败"),
    STORAGE_FILE_NOT_FOUND("4004", "存储文件不存在"),

    /**
     * 导出模块 5xxx
     */
    EXPORT_PDF_FAILED("5001", "PDF导出失败"),

    /**
     * 知识库模块 6xxx
     */
    KNOWLEDGE_BASE_NOT_FOUND("6001", "知识库不存在"),
    KNOWLEDGE_BASE_PARSE_FAILED("6002", "知识库文件解析失败"),
    KNOWLEDGE_BASE_ALREADY_PROCESSING("6003", "知识库正在处理中"),
    KNOWLEDGE_BASE_NOT_READY("6004", "知识库尚未向量化完成"),
    RAG_SESSION_NOT_FOUND("6005", "RAG 聊天会话不存在"),
    KNOWLEDGE_BASE_VECTORIZATION_FAILED("6006", "知识库向量化失败"),

    /**
     * AI 服务模块 7xxx
     */
    AI_SERVICE_UNAVAILABLE("7001", "AI服务暂时不可用"),
    AI_SERVICE_TIMEOUT("7002", "AI服务响应超时"),
    AI_STRUCTURED_OUTPUT_FAILED("7003", "AI结构化输出解析失败"),
    AI_API_KEY_INVALID("7004", "AI服务密钥无效"),

    /**
     * 限流模块 8xxx
     */
    RATE_LIMIT_EXCEEDED("8001", "请求过于频繁，请稍后再试"),

    /**
     * 面试日程模块 9xxx
     */
    INTERVIEW_SCHEDULE_NOT_FOUND("9001", "面试日程不存在"),
    INTERVIEW_SCHEDULE_PARSE_FAILED("9002", "面试邀请解析失败"),

    /**
     * 语音面试模块 10xxx
     */
    VOICE_SESSION_NOT_FOUND("10001", "语音面试会话不存在"),
    VOICE_SESSION_ALREADY_ENDED("10002", "语音面试会话已结束"),
    VOICE_ASR_FAILED("10003", "语音识别失败"),
    VOICE_EVALUATION_FAILED("10004", "语音面试评估失败"),
    VOICE_TTS_FAILED("10005", "语音合成失败"),

    /**
     * LLM Provider 模块 11xxx
     */
    PROVIDER_NOT_FOUND("11001", "LLM Provider 不存在"),
    PROVIDER_ALREADY_EXISTS("11002", "Provider 名称已存在"),
    PROVIDER_API_KEY_DECRYPT_FAILED("11003", "API 密钥解密失败"),
    PROVIDER_API_KEY_ENCRYPT_FAILED("11004", "API 密钥加密失败"),
    PROVIDER_DEFAULT_NOT_SET("11005", "尚未设置默认 Provider"),
    PROVIDER_TEST_FAILED("11006", "Provider 连通性测试失败"),
    PROVIDER_DEFAULT_CANNOT_DELETE("11007", "默认 Provider 不可删除"),
    ;
    /**
     * 状态码
     */
    private final String code;

    /**
     * 信息
     */
    private final String message;
}
