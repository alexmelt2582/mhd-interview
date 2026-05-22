package com.mhd.interview.web.common.constant;

/**
 * Redis Stream 相关常量
 * <p>
 * 所有 Stream Key、消费者组名、消费者前缀必须在此处统一定义，禁止硬编码在业务代码中
 *
 * @author mhd
 */
public final class StreamConstants {

    // ==================== Stream Key ====================

    /** 简历分析 Stream Key */
    public static final String RESUME_ANALYZE_STREAM = "stream:resume:analyze";

    /** 面试答题评估 Stream Key */
    public static final String INTERVIEW_EVAL_STREAM = "stream:interview:evaluate";

    /** 知识库向量化 Stream Key */
    public static final String KNOWLEDGE_VECTOR_STREAM = "stream:knowledge:vectorize";

    // ==================== 消费者组名 ====================

    /** 简历分析消费者组 */
    public static final String RESUME_ANALYZE_GROUP = "group:resume:analyze";

    /** 面试评估消费者组 */
    public static final String INTERVIEW_EVAL_GROUP = "group:interview:evaluate";

    /** 知识库向量化消费者组 */
    public static final String KNOWLEDGE_VECTOR_GROUP = "group:knowledge:vectorize";

    // ==================== 消费者名前缀 ====================

    /** 简历分析消费者名前缀（后接实例标识） */
    public static final String RESUME_ANALYZE_CONSUMER = "consumer:resume:analyze:";

    /** 面试评估消费者名前缀 */
    public static final String INTERVIEW_EVAL_CONSUMER = "consumer:interview:evaluate:";

    /** 知识库向量化消费者名前缀 */
    public static final String KNOWLEDGE_VECTOR_CONSUMER = "consumer:knowledge:vectorize:";

    // ==================== 消费者名线程 ====================

    public static final String RESUME_ANALYSIS_THREAD = "resume-analysis-consumer";

    // ==================== 消息字段名 ====================

    /** 消息中任务 ID 字段名 */
    public static final String FIELD_ID = "id";

    /** 消息中任务内容字段名 */
    public static final String FIELD_CONTENT = "content";

    /** 消息中重试次数字段名 */
    public static final String FIELD_RETRY_COUNT = "retryCount";

    /** 消息中简历 ID 字段名 */
    public static final String FIELD_RESUME_ID = "resumeId";

    // ==================== Stream 运行参数 ====================

    /** Stream 最大消息保留条数（超出后自动裁剪） */
    public static final int STREAM_MAX_LEN = 1000;

    /** 每次 Poll 拉取的最大消息条数 */
    public static final int BATCH_SIZE = 5;

    /** 消费者轮询间隔（毫秒） */
    public static final long POLL_INTERVAL_MS = 2000L;

    /** 消息最大重试次数 */
    public static final int MAX_RETRY = 3;

    private StreamConstants() {
        // 工具类，禁止实例化
    }
}
