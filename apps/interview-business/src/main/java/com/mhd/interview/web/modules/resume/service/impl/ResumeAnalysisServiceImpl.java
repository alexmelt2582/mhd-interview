package com.mhd.interview.web.modules.resume.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.mhd.interview.ai.llm.LlmProviderRegistry;
import com.mhd.interview.ai.llm.StructuredOutputInvoker;
import com.mhd.interview.common.enums.ErrorCodeEnum;
import com.mhd.interview.common.exception.BusinessException;
import com.mhd.interview.common.utils.StringUtils;
import com.mhd.interview.common.utils.json.JsonUtils;
import com.mhd.interview.web.infrastructure.export.PdfExportService;
import com.mhd.interview.web.modules.base.ExportResult;
import com.mhd.interview.web.modules.resume.convert.ResumeStructMapper;
import com.mhd.interview.web.modules.resume.dto.ResumeAnalysisDTO;
import com.mhd.interview.web.modules.resume.entity.ResumeAnalysisEntity;
import com.mhd.interview.web.modules.resume.entity.ResumeEntity;
import com.mhd.interview.web.modules.resume.mapper.ResumeAnalysisMapper;
import com.mhd.interview.web.modules.resume.mapper.ResumeMapper;
import com.mhd.interview.web.modules.resume.service.IResumeAnalysisService;
import com.mhd.interview.web.properties.ResumeAnalysisProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 简历分析实现类
 *
 * @author zhao-hao-dong
 */
@Service
@Slf4j
public class ResumeAnalysisServiceImpl implements IResumeAnalysisService {

    private final ResumeAnalysisMapper baseMapper;
    private final ResumeMapper resumeMapper;
    private final LlmProviderRegistry llmProviderRegistry;
    private final StructuredOutputInvoker structuredOutputInvoker;
    private final BeanOutputConverter<ResumeAnalysisResult> outputConverter;
    private final PromptTemplate systemPromptTemplate;
    private final PromptTemplate userPromptTemplate;
    private final PdfExportService pdfExportService;
    private final ResumeStructMapper resumeStructMapper;

    public ResumeAnalysisServiceImpl(ResumeAnalysisMapper baseMapper,
                                     ResumeMapper resumeMapper,
                                     LlmProviderRegistry llmProviderRegistry,
                                     StructuredOutputInvoker structuredOutputInvoker,
                                     ResumeAnalysisProperties properties,
                                     ResourceLoader resourceLoader,
                                     PdfExportService pdfExportService,
                                     ResumeStructMapper resumeStructMapper) {
        this.baseMapper = baseMapper;
        this.resumeMapper = resumeMapper;
        this.llmProviderRegistry = llmProviderRegistry;
        this.structuredOutputInvoker = structuredOutputInvoker;
        this.outputConverter = new BeanOutputConverter<>(ResumeAnalysisResult.class);
        try {
            this.systemPromptTemplate = new PromptTemplate(
                    resourceLoader.getResource(properties.getSystemPromptPath())
                            .getContentAsString(StandardCharsets.UTF_8)
            );
            this.userPromptTemplate = new PromptTemplate(
                    resourceLoader.getResource(properties.getUserPromptPath())
                            .getContentAsString(StandardCharsets.UTF_8)
            );
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load resume analysis prompt templates", e);
        }
        this.pdfExportService = pdfExportService;
        this.resumeStructMapper = resumeStructMapper;
    }

    /**
     * 简历分析 AI 结构化输出 DTO（仅用于解析 LLM 返回结果）
     *
     * @param score       综合评分 0-100
     * @param summary     综合评价（2-3 段）
     * @param strengths   核心优势列表
     * @param suggestions 优化建议列表
     */
    private record ResumeAnalysisResult(
            int score,
            ScoreDetail scoreDetail,
            String summary,
            List<String> strengths,
            List<ResumeAnalysisDTO.SuggestionDTO> suggestions
    ) {
        private record ScoreDetail(
                int projectScore,
                int skillMatchScore,
                int contentScore,
                int structureScore,
                int expressionScore
        ) {
        }
    }

    @Override
    public ResumeAnalysisDTO selectResumeAnalysisByResumeId(Long resumeId) {
        ResumeAnalysisEntity entity = baseMapper.selectOne(
                Wrappers.<ResumeAnalysisEntity>lambdaQuery()
                        .eq(ResumeAnalysisEntity::getResumeId, resumeId)
                        .orderByDesc(ResumeAnalysisEntity::getCreateTime)
                        .last("LIMIT 1"));
        if (entity == null) {
            return null;
        }
        return resumeStructMapper.toResumeAnalysisDTO(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void doAnalyzeResume(Long resumeId, String content) {
        ResumeEntity resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            log.warn("Resume not found, skipping analysis: resumeId={}", resumeId);
            throw new BusinessException(ErrorCodeEnum.RESUME_NOT_FOUND, "Resume not found: " + resumeId);
        }

        String parsedText = resume.getParsedText();
        if (StringUtils.isBlank(parsedText)) {
            throw new BusinessException(ErrorCodeEnum.RESUME_PARSE_FAILED, "Resume text is empty, cannot analyze");
        }
        log.info("Starting AI analysis for resume: resumeId={}, filename={}, length={}", resumeId, resume.getOriginalName(), parsedText.length());
        // 加载系统提示词
        String systemPrompt = systemPromptTemplate.render();
        // 加载用户提示词并填充变量
        Map<String, Object> variables = new HashMap<>();
        variables.put("resumeText", parsedText);
        String userPrompt = userPromptTemplate.render(variables);
        // 添加格式指令到系统提示词
        String systemPromptWithFormat = systemPrompt + "\n\n" + outputConverter.getFormat();

        ResumeAnalysisResult result;
        try {
            ChatClient chatClient = llmProviderRegistry.getChatClientOrDefault(null);
            result = structuredOutputInvoker.invoke(
                    chatClient,
                    systemPromptWithFormat,
                    userPrompt,
                    outputConverter,
                    ErrorCodeEnum.RESUME_ANALYSIS_FAILED,
                    "简历分析失败：",
                    "简历分析",
                    log
            );
        } catch (Exception e) {
            log.error("简历分析AI调用失败: {}", e.getMessage(), e);
            result = new ResumeAnalysisResult(
                    0,
                    new ResumeAnalysisResult.ScoreDetail(0, 0, 0, 0, 0),
                    "分析过程中出现错误: " + e.getMessage(),
                    List.of(),
                    List.of(new ResumeAnalysisDTO.SuggestionDTO(
                            "系统",
                            "高",
                            "AI分析服务暂时不可用",
                            "请稍后重试，或检查AI服务是否正常运行"
                    ))
            );
        }
        saveAnalysisResult(resumeId, result);
        log.info("简历分析完成: resumeId={}, score={}", resumeId, result.score());
    }

    /**
     * Saves analysis result to t_resume_analysis table.
     *
     * @param resumeId the resume ID
     * @param result   the AI analysis result
     */
    private void saveAnalysisResult(Long resumeId, ResumeAnalysisResult result) {
        ResumeAnalysisEntity entity = new ResumeAnalysisEntity();
        entity.setResumeId(resumeId);
        entity.setScore(result.score());
        entity.setSummary(result.summary());

        if (result.scoreDetail() != null) {
            entity.setProjectScore(result.scoreDetail().projectScore());
            entity.setSkillMatchScore(result.scoreDetail().skillMatchScore());
            entity.setContentScore(result.scoreDetail().contentScore());
            entity.setStructureScore(result.scoreDetail().structureScore());
            entity.setExpressionScore(result.scoreDetail().expressionScore());
        }

        try {
            entity.setStrengths(JsonUtils.toJsonString(result.strengths()));
            entity.setSuggestions(JsonUtils.toJsonString(result.suggestions()));
        } catch (Exception e) {
            log.warn("Failed to serialize analysis fields: resumeId={}, error={}", resumeId, e.getMessage());
            entity.setStrengths("[]");
            entity.setSuggestions("[]");
        }
        baseMapper.insert(entity);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ExportResult exportResumeAnalysisPdf(Long resumeId) {
        // 1. 查询简历和分析结果
        ResumeEntity resume = resumeMapper.selectById(resumeId);
        if (resume == null) {
            throw new BusinessException(ErrorCodeEnum.RESUME_NOT_FOUND, "简历不存在: " + resumeId);
        }
        ResumeAnalysisEntity analysis = baseMapper.selectOne(
                Wrappers.<ResumeAnalysisEntity>lambdaQuery()
                        .eq(ResumeAnalysisEntity::getResumeId, resumeId)
                        .orderByDesc(ResumeAnalysisEntity::getCreateTime)
                        .last("LIMIT 1"));
        if (analysis == null) {
            throw new BusinessException(ErrorCodeEnum.RESUME_ANALYSIS_FAILED,
                    "简历尚未完成分析，无法导出 PDF");
        }

        // 2. 生成 PDF 字节数组
        Map<String, String> fields = Map.of(
                "name", resume.getOriginalName(),
                "score", String.valueOf(analysis.getScore()),
                "summary", "",
                "strengths", analysis.getStrengths() != null ? analysis.getStrengths() : "",
                "suggestions", analysis.getSuggestions() != null ? analysis.getSuggestions() : ""
        );
        try {
            byte[] pdfBytes = pdfExportService.exportResumeAnalysisPdf(fields);
            String filename = "简历分析报告_" + resume.getOriginalName() + ".pdf";
            log.info("简历分析 PDF 导出完成: resumeId={}", resumeId);

            return new ExportResult(pdfBytes, filename);
        } catch (Exception e) {
            log.error("简历分析 PDF 导出失败: resumeId={}", resumeId, e);
            throw new BusinessException(ErrorCodeEnum.EXPORT_PDF_FAILED, "导出PDF失败: " + e.getMessage());
        }
    }
}
