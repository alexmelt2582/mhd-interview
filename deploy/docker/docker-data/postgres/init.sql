-- 启用 pgvector 扩展（需要 PostgreSQL 16+ 或已安装 pgvector 插件）
CREATE EXTENSION IF NOT EXISTS vector;

-- ========== LLM Provider 模块 ==========

-- LLM Provider 配置表
CREATE TABLE IF NOT EXISTS t_llm_provider (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(100)    NOT NULL,
    base_url    VARCHAR(500)    NOT NULL,
    api_key     VARCHAR(500)    NOT NULL,
    model       VARCHAR(100)    NOT NULL,
    is_default  BOOLEAN         NOT NULL DEFAULT FALSE,
    is_deleted  BOOLEAN         NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  t_llm_provider IS 'LLM Provider 配置表';
COMMENT ON COLUMN t_llm_provider.id          IS '主键ID';
COMMENT ON COLUMN t_llm_provider.name        IS 'Provider 名称（唯一标识，如 dashscope）';
COMMENT ON COLUMN t_llm_provider.base_url    IS 'API 基础 URL';
COMMENT ON COLUMN t_llm_provider.api_key     IS 'API 密钥（AES 加密存储）';
COMMENT ON COLUMN t_llm_provider.model       IS '默认模型名称（如 qwen-plus）';
COMMENT ON COLUMN t_llm_provider.is_default  IS '是否为默认 Provider';
COMMENT ON COLUMN t_llm_provider.is_deleted  IS '逻辑删除标志（true=已删除）';
COMMENT ON COLUMN t_llm_provider.create_time IS '创建时间';
COMMENT ON COLUMN t_llm_provider.update_time IS '最后更新时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_llm_provider_name ON t_llm_provider(name) WHERE is_deleted = FALSE;

-- LLM 全局配置表
CREATE TABLE IF NOT EXISTS t_llm_global_setting (
    id           BIGSERIAL    PRIMARY KEY,
    setting_key  VARCHAR(100) NOT NULL,
    setting_val  TEXT,
    description  VARCHAR(500),
    create_time  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    update_time  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  t_llm_global_setting IS 'LLM 全局配置表（键值对）';
COMMENT ON COLUMN t_llm_global_setting.id          IS '主键ID';
COMMENT ON COLUMN t_llm_global_setting.setting_key IS '配置键（唯一）';
COMMENT ON COLUMN t_llm_global_setting.setting_val IS '配置值';
COMMENT ON COLUMN t_llm_global_setting.description IS '配置说明';
COMMENT ON COLUMN t_llm_global_setting.create_time IS '创建时间';
COMMENT ON COLUMN t_llm_global_setting.update_time IS '最后更新时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_llm_setting_key ON t_llm_global_setting(setting_key);

-- ========== 简历模块 ==========
-- 创建简历表
CREATE TABLE t_resume (
                          id BIGINT PRIMARY KEY,
                          original_name VARCHAR(255) NOT NULL,
                          file_hash VARCHAR(64) NOT NULL,
                          file_size BIGINT,
                          content_type VARCHAR(100),
                          storage_key VARCHAR(255) NOT NULL,
                          storage_url VARCHAR(500),
                          parsed_text TEXT,
                          analysis_status INTEGER DEFAULT 0,
                          analysis_error VARCHAR(500),
                          is_deleted BOOLEAN DEFAULT FALSE,
                          create_time TIMESTAMP,
                          update_time TIMESTAMP
);

-- 表注释
COMMENT ON TABLE t_resume IS '简历表';
-- 字段注释
COMMENT ON COLUMN t_resume.id IS '主键 ID';
COMMENT ON COLUMN t_resume.original_name IS '原始文件名';
COMMENT ON COLUMN t_resume.file_hash IS '文件 SHA-256 哈希（用于去重）';
COMMENT ON COLUMN t_resume.file_size IS '文件大小（字节）';
COMMENT ON COLUMN t_resume.content_type IS '文件类型';
COMMENT ON COLUMN t_resume.storage_key IS '存储文件的 Key';
COMMENT ON COLUMN t_resume.storage_url IS '存储文件的 URL';
COMMENT ON COLUMN t_resume.parsed_text IS '解析后的简历纯文本内容';
COMMENT ON COLUMN t_resume.analysis_status IS '解析/分析状态：0=待解析，1=解析中，2=已完成，3=失败';
COMMENT ON COLUMN t_resume.analysis_error IS 'AI 分析失败时的错误信息（最多500字符）';
COMMENT ON COLUMN t_resume.is_deleted IS '逻辑删除标识（0=正常, 1=已删除）';
COMMENT ON COLUMN t_resume.create_time IS '创建时间（INSERT 自动填充）';
COMMENT ON COLUMN t_resume.update_time IS '更新时间（INSERT/UPDATE 自动填充）';

-- 索引设计
-- 1. 文件哈希唯一索引（用于文件去重，同时自动包含 is_deleted 过滤）
CREATE UNIQUE INDEX idx_resume_file_hash ON t_resume (file_hash) WHERE is_deleted = FALSE;

-- 2. 复合索引（用于查询指定用户的未完成/失败简历，等值条件在前）
CREATE INDEX idx_resume_status_not_deleted ON t_resume (analysis_status, is_deleted);

-- 3. 时间降序索引（用于按创建时间倒序分页查询最新简历）
CREATE INDEX idx_resume_create_time ON t_resume (create_time DESC);

-- 创建简历分析表
CREATE TABLE t_resume_analysis (
                                   id BIGINT PRIMARY KEY,
                                   resume_id BIGINT NOT NULL,
                                   score INTEGER,
                                   content_score INTEGER,
                                   structure_score INTEGER,
                                   skill_match_score INTEGER,
                                   expression_score INTEGER,
                                   project_score INTEGER,
                                   summary TEXT,
                                   strengths TEXT,
                                   suggestions TEXT,
                                   create_time TIMESTAMP
);

-- 表注释
COMMENT ON TABLE t_resume_analysis IS '简历分析结果表';
-- 字段注释
COMMENT ON COLUMN t_resume_analysis.id IS '主键 ID';
COMMENT ON COLUMN t_resume_analysis.resume_id IS '关联的简历 ID';
COMMENT ON COLUMN t_resume_analysis.score IS '综合评分（0-100）';
COMMENT ON COLUMN t_resume_analysis.content_score IS '内容完整性 (0-25)';
COMMENT ON COLUMN t_resume_analysis.structure_score IS '结构清晰度 (0-20)';
COMMENT ON COLUMN t_resume_analysis.skill_match_score IS '技能匹配度 (0-25)';
COMMENT ON COLUMN t_resume_analysis.expression_score IS '表达专业性 (0-15)';
COMMENT ON COLUMN t_resume_analysis.project_score IS '项目经验 (0-15)';
COMMENT ON COLUMN t_resume_analysis.summary IS 'AI 分析摘要';
COMMENT ON COLUMN t_resume_analysis.strengths IS '核心优势（JSON格式）';
COMMENT ON COLUMN t_resume_analysis.suggestions IS '优化建议（JSON格式）';
COMMENT ON COLUMN t_resume_analysis.create_time IS '创建时间（INSERT 自动填充）';

-- 索引设计
-- 1. 关联简历 ID 索引（最核心的查询场景：根据简历ID查分析结果）
CREATE INDEX idx_analysis_resume_id ON t_resume_analysis (resume_id);

-- ========== 模拟面试模块 ==========

-- 面试会话表
CREATE TABLE IF NOT EXISTS t_interview_session (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL DEFAULT 0,
    provider_id      BIGINT       NOT NULL,
    job_title        VARCHAR(200) NOT NULL,
    job_description  TEXT,
    status           SMALLINT     NOT NULL DEFAULT 0,
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    create_time      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    update_time      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  t_interview_session IS '模拟面试会话表';
COMMENT ON COLUMN t_interview_session.id              IS '主键ID';
COMMENT ON COLUMN t_interview_session.user_id         IS '用户ID（预留，当前版本默认0）';
COMMENT ON COLUMN t_interview_session.provider_id     IS '使用的 LLM Provider ID';
COMMENT ON COLUMN t_interview_session.job_title       IS '面试岗位名称';
COMMENT ON COLUMN t_interview_session.job_description IS '岗位描述（JD 内容）';
COMMENT ON COLUMN t_interview_session.status          IS '会话状态：0=进行中，1=已完成，2=已终止';
COMMENT ON COLUMN t_interview_session.is_deleted      IS '逻辑删除标志';
COMMENT ON COLUMN t_interview_session.create_time     IS '创建时间';
COMMENT ON COLUMN t_interview_session.update_time     IS '最后更新时间';

-- 面试答题记录表
CREATE TABLE IF NOT EXISTS t_interview_answer (
    id          BIGSERIAL PRIMARY KEY,
    session_id  BIGINT    NOT NULL,
    question    TEXT      NOT NULL,
    answer      TEXT,
    score       SMALLINT,
    feedback    TEXT,
    round       SMALLINT  NOT NULL DEFAULT 1,
    create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  t_interview_answer IS '面试答题记录表';
COMMENT ON COLUMN t_interview_answer.id          IS '主键ID';
COMMENT ON COLUMN t_interview_answer.session_id  IS '关联面试会话ID';
COMMENT ON COLUMN t_interview_answer.question    IS 'AI 出的面试题目';
COMMENT ON COLUMN t_interview_answer.answer      IS '用户回答内容';
COMMENT ON COLUMN t_interview_answer.score       IS 'AI 评分（0~100，null 表示待评估）';
COMMENT ON COLUMN t_interview_answer.feedback    IS 'AI 评价反馈内容';
COMMENT ON COLUMN t_interview_answer.round       IS '第几轮（从1开始）';
COMMENT ON COLUMN t_interview_answer.create_time IS '创建时间';
COMMENT ON COLUMN t_interview_answer.update_time IS '最后更新时间';
CREATE INDEX IF NOT EXISTS idx_interview_answer_session ON t_interview_answer(session_id);

-- ========== 知识库模块 ==========

-- 知识库文档表
CREATE TABLE IF NOT EXISTS t_knowledge_base (
    id           BIGSERIAL    PRIMARY KEY,
    user_id      BIGINT       NOT NULL DEFAULT 0,
    name         VARCHAR(200) NOT NULL,
    description  TEXT,
    file_name    VARCHAR(255) NOT NULL,
    file_key     VARCHAR(500) NOT NULL,
    file_size    BIGINT       NOT NULL DEFAULT 0,
    file_hash    VARCHAR(64)  NOT NULL,
    status       SMALLINT     NOT NULL DEFAULT 0,
    chunk_count  INT          NOT NULL DEFAULT 0,
    is_deleted   BOOLEAN      NOT NULL DEFAULT FALSE,
    create_time  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    update_time  TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  t_knowledge_base IS '知识库文档表';
COMMENT ON COLUMN t_knowledge_base.id          IS '主键ID';
COMMENT ON COLUMN t_knowledge_base.user_id     IS '用户ID（预留）';
COMMENT ON COLUMN t_knowledge_base.name        IS '知识库名称';
COMMENT ON COLUMN t_knowledge_base.description IS '知识库描述';
COMMENT ON COLUMN t_knowledge_base.file_name   IS '原始文件名';
COMMENT ON COLUMN t_knowledge_base.file_key    IS 'MinIO 对象 Key';
COMMENT ON COLUMN t_knowledge_base.file_size   IS '文件大小（字节）';
COMMENT ON COLUMN t_knowledge_base.file_hash   IS '文件内容 SHA-256 哈希（去重用）';
COMMENT ON COLUMN t_knowledge_base.status      IS '向量化状态：0=待向量化，1=向量化中，2=已完成，3=失败';
COMMENT ON COLUMN t_knowledge_base.chunk_count IS '向量分块数量（向量化完成后更新）';
COMMENT ON COLUMN t_knowledge_base.is_deleted  IS '逻辑删除标志';
COMMENT ON COLUMN t_knowledge_base.create_time IS '创建时间';
COMMENT ON COLUMN t_knowledge_base.update_time IS '最后更新时间';

-- RAG 聊天会话表
CREATE TABLE IF NOT EXISTS t_rag_chat_session (
    id          BIGSERIAL    PRIMARY KEY,
    user_id     BIGINT       NOT NULL DEFAULT 0,
    kb_id       BIGINT       NOT NULL,
    title       VARCHAR(200),
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    update_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  t_rag_chat_session IS 'RAG 聊天会话表';
COMMENT ON COLUMN t_rag_chat_session.id          IS '主键ID';
COMMENT ON COLUMN t_rag_chat_session.user_id     IS '用户ID（预留）';
COMMENT ON COLUMN t_rag_chat_session.kb_id       IS '关联知识库ID';
COMMENT ON COLUMN t_rag_chat_session.title       IS '会话标题（可由用户命名）';
COMMENT ON COLUMN t_rag_chat_session.is_deleted  IS '逻辑删除标志';
COMMENT ON COLUMN t_rag_chat_session.create_time IS '创建时间';
COMMENT ON COLUMN t_rag_chat_session.update_time IS '最后更新时间';

-- RAG 聊天消息表
CREATE TABLE IF NOT EXISTS t_rag_chat_message (
    id          BIGSERIAL    PRIMARY KEY,
    session_id  BIGINT       NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    content     TEXT         NOT NULL,
    create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  t_rag_chat_message IS 'RAG 聊天消息表';
COMMENT ON COLUMN t_rag_chat_message.id          IS '主键ID';
COMMENT ON COLUMN t_rag_chat_message.session_id  IS '关联聊天会话ID';
COMMENT ON COLUMN t_rag_chat_message.role        IS '角色：user（用户）/ assistant（AI 助手）';
COMMENT ON COLUMN t_rag_chat_message.content     IS '消息内容';
COMMENT ON COLUMN t_rag_chat_message.create_time IS '创建时间';
CREATE INDEX IF NOT EXISTS idx_rag_msg_session ON t_rag_chat_message(session_id);

-- ========== 面试日程模块 ==========

-- 面试日程表
CREATE TABLE IF NOT EXISTS t_interview_schedule (
    id              BIGSERIAL    PRIMARY KEY,
    user_id         BIGINT       NOT NULL DEFAULT 0,
    company         VARCHAR(200),
    position        VARCHAR(200),
    interview_time  TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    location        VARCHAR(500),
    online_link     VARCHAR(500),
    notes           TEXT,
    raw_invitation  TEXT,
    status          SMALLINT     NOT NULL DEFAULT 0,
    is_deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    create_time     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    update_time     TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  t_interview_schedule IS '面试日程表';
COMMENT ON COLUMN t_interview_schedule.id             IS '主键ID';
COMMENT ON COLUMN t_interview_schedule.user_id        IS '用户ID（预留）';
COMMENT ON COLUMN t_interview_schedule.company        IS '公司名称';
COMMENT ON COLUMN t_interview_schedule.position       IS '面试岗位名称';
COMMENT ON COLUMN t_interview_schedule.interview_time IS '面试时间';
COMMENT ON COLUMN t_interview_schedule.location       IS '线下面试地点';
COMMENT ON COLUMN t_interview_schedule.online_link    IS '线上面试链接';
COMMENT ON COLUMN t_interview_schedule.notes          IS '备注信息';
COMMENT ON COLUMN t_interview_schedule.raw_invitation IS 'AI 解析来源的原始邀请邮件文本';
COMMENT ON COLUMN t_interview_schedule.status         IS '日程状态：0=待面试，1=已完成，2=已取消';
COMMENT ON COLUMN t_interview_schedule.is_deleted     IS '逻辑删除标志';
COMMENT ON COLUMN t_interview_schedule.create_time    IS '创建时间';
COMMENT ON COLUMN t_interview_schedule.update_time    IS '最后更新时间';

-- ========== 语音面试模块 ==========

-- 语音面试会话表
CREATE TABLE IF NOT EXISTS t_voice_session (
    id               BIGSERIAL    PRIMARY KEY,
    user_id          BIGINT       NOT NULL DEFAULT 0,
    provider_id      BIGINT       NOT NULL,
    job_title        VARCHAR(200) NOT NULL,
    interviewer_role VARCHAR(50)  NOT NULL DEFAULT 'technical',
    status           SMALLINT     NOT NULL DEFAULT 0,
    is_deleted       BOOLEAN      NOT NULL DEFAULT FALSE,
    create_time      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    update_time      TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  t_voice_session IS '语音面试会话表';
COMMENT ON COLUMN t_voice_session.id               IS '主键ID';
COMMENT ON COLUMN t_voice_session.user_id          IS '用户ID（预留）';
COMMENT ON COLUMN t_voice_session.provider_id      IS '使用的 LLM Provider ID';
COMMENT ON COLUMN t_voice_session.job_title        IS '面试岗位名称';
COMMENT ON COLUMN t_voice_session.interviewer_role IS '面试官角色：technical（技术）/ product（产品）/ hr（HR）';
COMMENT ON COLUMN t_voice_session.status           IS '会话状态：0=进行中，1=已完成，2=已终止';
COMMENT ON COLUMN t_voice_session.is_deleted       IS '逻辑删除标志';
COMMENT ON COLUMN t_voice_session.create_time      IS '创建时间';
COMMENT ON COLUMN t_voice_session.update_time      IS '最后更新时间';

-- 语音面试消息记录表
CREATE TABLE IF NOT EXISTS t_voice_message (
    id          BIGSERIAL    PRIMARY KEY,
    session_id  BIGINT       NOT NULL,
    role        VARCHAR(20)  NOT NULL,
    content     TEXT         NOT NULL,
    audio_key   VARCHAR(500),
    duration_ms INT,
    create_time TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  t_voice_message IS '语音面试消息记录表';
COMMENT ON COLUMN t_voice_message.id          IS '主键ID';
COMMENT ON COLUMN t_voice_message.session_id  IS '关联语音面试会话ID';
COMMENT ON COLUMN t_voice_message.role        IS '角色：user（候选人）/ assistant（AI 面试官）';
COMMENT ON COLUMN t_voice_message.content     IS '消息文本内容（ASR 识别结果或 AI 回复）';
COMMENT ON COLUMN t_voice_message.audio_key   IS '对应音频文件的 MinIO Key（可为空）';
COMMENT ON COLUMN t_voice_message.duration_ms IS '音频时长（毫秒）';
COMMENT ON COLUMN t_voice_message.create_time IS '创建时间';
CREATE INDEX IF NOT EXISTS idx_voice_msg_session ON t_voice_message(session_id);

-- 语音面试评估结果表
CREATE TABLE IF NOT EXISTS t_voice_evaluation (
    id            BIGSERIAL PRIMARY KEY,
    session_id    BIGINT    NOT NULL,
    overall_score SMALLINT,
    tech_score    SMALLINT,
    comm_score    SMALLINT,
    logic_score   SMALLINT,
    detail        TEXT,
    create_time   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    update_time   TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);
COMMENT ON TABLE  t_voice_evaluation IS '语音面试评估结果表（一对一关联 t_voice_session）';
COMMENT ON COLUMN t_voice_evaluation.id            IS '主键ID';
COMMENT ON COLUMN t_voice_evaluation.session_id    IS '关联语音面试会话ID（唯一）';
COMMENT ON COLUMN t_voice_evaluation.overall_score IS '综合评分（0~100）';
COMMENT ON COLUMN t_voice_evaluation.tech_score    IS '技术能力评分（0~100）';
COMMENT ON COLUMN t_voice_evaluation.comm_score    IS '表达沟通能力评分（0~100）';
COMMENT ON COLUMN t_voice_evaluation.logic_score   IS '逻辑思维能力评分（0~100）';
COMMENT ON COLUMN t_voice_evaluation.detail        IS '各维度详细评价（JSON 字符串）';
COMMENT ON COLUMN t_voice_evaluation.create_time   IS '创建时间';
COMMENT ON COLUMN t_voice_evaluation.update_time   IS '最后更新时间';
CREATE UNIQUE INDEX IF NOT EXISTS uk_voice_eval_session ON t_voice_evaluation(session_id);

