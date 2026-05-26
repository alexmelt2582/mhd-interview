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
                          access_count INTEGER,
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
COMMENT ON COLUMN t_resume.access_count IS '访问次数';
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