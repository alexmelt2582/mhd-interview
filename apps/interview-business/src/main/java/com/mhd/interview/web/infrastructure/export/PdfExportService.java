package com.mhd.interview.web.infrastructure.export;

import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.mhd.interview.common.enums.ErrorCodeEnum;
import com.mhd.interview.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * PDF 导出服务（基于 iText 8）
 * <p>
 * 提供简历分析报告、面试报告等 PDF 导出功能。
 * 使用内嵌字体支持中文显示。
 *
 * @author mhd
 */
@Service
@Slf4j
public class PdfExportService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 主题色（蓝色）
     */
    private static final DeviceRgb THEME_COLOR = new DeviceRgb(41, 128, 185);
    /**
     * 次要色（深灰）
     */
    private static final DeviceRgb SECONDARY_COLOR = new DeviceRgb(74, 74, 74);
    /**
     * 分隔线颜色（浅灰）
     */
    private static final DeviceRgb BORDER_COLOR = new DeviceRgb(220, 220, 220);

    /**
     * 导出简历分析报告为 PDF 字节数组
     *
     * @param fields 报告字段 Map，Key 为字段名，Value 为字段值
     *               期望 Key：name, position, score, summary, strengths, weaknesses, suggestions
     * @return PDF 文件字节数组
     * @throws BusinessException 当 PDF 生成失败时
     */
    public byte[] exportResumeAnalysisPdf(Map<String, String> fields) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 50, 40, 50);

            // 使用支持中文的字体
            PdfFont font = loadFont();

            // 报告标题
            addTitle(document, font, "简历分析报告");
            addDivider(document);

            // 基本信息区块
            addSectionHeader(document, font, "基本信息");
            addInfoRow(document, font, "姓名", fields.getOrDefault("name", "-"));
            addInfoRow(document, font, "目标岗位", fields.getOrDefault("position", "-"));
            addInfoRow(document, font, "综合评分", fields.getOrDefault("score", "-") + " / 100");

            addSectionHeader(document, font, "综合评价");
            addBodyText(document, font, fields.getOrDefault("summary", "暂无"));

            addSectionHeader(document, font, "核心优势");
            addBodyText(document, font, fields.getOrDefault("strengths", "暂无"));

            addSectionHeader(document, font, "优化建议");
            addBodyText(document, font, fields.getOrDefault("suggestions", "暂无"));

            document.close();
            log.info("简历分析 PDF 导出成功，size={}bytes", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("简历分析 PDF 导出失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.EXPORT_PDF_FAILED,
                    "PDF 生成失败: " + e.getMessage());
        }
        /*
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // 使用支持中文的字体
        PdfFont font = createChineseFont();
        document.setFont(font);

        // 标题
        Paragraph title = new Paragraph("简历分析报告")
            .setFontSize(24)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(HEADER_COLOR);
        document.add(title);

        // 基本信息
        document.add(new Paragraph("\n"));
        document.add(createSectionTitle("基本信息"));
        document.add(new Paragraph("文件名: " + resume.getOriginalFilename()));
        document.add(new Paragraph("上传时间: " +
            (resume.getUploadedAt() != null ? DATE_FORMAT.format(resume.getUploadedAt()) : "未知")));

        // 总分
        document.add(new Paragraph("\n"));
        document.add(createSectionTitle("综合评分"));
        Paragraph scoreP = new Paragraph("总分: " + analysis.overallScore() + " / 100")
            .setFontSize(18)
            .setBold()
            .setFontColor(getScoreColor(analysis.overallScore()));
        document.add(scoreP);

        // 各维度评分
        if (analysis.scoreDetail() != null) {
            document.add(new Paragraph("\n"));
            document.add(createSectionTitle("各维度评分"));

            Table scoreTable = new Table(UnitValue.createPercentArray(new float[]{2, 1}))
                .useAllAvailableWidth();
            addScoreRow(scoreTable, "项目经验", analysis.scoreDetail().projectScore(), 40);
            addScoreRow(scoreTable, "技能匹配度", analysis.scoreDetail().skillMatchScore(), 20);
            addScoreRow(scoreTable, "内容完整性", analysis.scoreDetail().contentScore(), 15);
            addScoreRow(scoreTable, "结构清晰度", analysis.scoreDetail().structureScore(), 15);
            addScoreRow(scoreTable, "表达专业性", analysis.scoreDetail().expressionScore(), 10);
            document.add(scoreTable);
        }

        // 简历摘要
        if (analysis.summary() != null) {
            document.add(new Paragraph("\n"));
            document.add(createSectionTitle("简历摘要"));
            document.add(new Paragraph(sanitizeText(analysis.summary())));
        }

        // 优势亮点
        if (analysis.strengths() != null && !analysis.strengths().isEmpty()) {
            document.add(new Paragraph("\n"));
            document.add(createSectionTitle("优势亮点"));
            for (String strength : analysis.strengths()) {
                document.add(new Paragraph("• " + sanitizeText(strength)));
            }
        }

        // 改进建议
        if (analysis.suggestions() != null && !analysis.suggestions().isEmpty()) {
            document.add(new Paragraph("\n"));
            document.add(createSectionTitle("改进建议"));
            for (ResumeAnalysisResponse.Suggestion suggestion : analysis.suggestions()) {
                document.add(new Paragraph("【" + suggestion.priority() + "】" + sanitizeText(suggestion.category()))
                    .setBold());
                document.add(new Paragraph("问题: " + sanitizeText(suggestion.issue())));
                document.add(new Paragraph("建议: " + sanitizeText(suggestion.recommendation())));
                document.add(new Paragraph("\n"));
            }
        }

        document.close();
        return baos.toByteArray();
         */
    }

    /*
    // 清理文本中可能导致字体问题的字符
    private String sanitizeText(String text) {
        if (text == null) return "";
        // 移除可能导致问题的特殊字符（如 emoji）
        return text.replaceAll("[\\p{So}\\p{Cs}]", "").trim();
    }
    */

    /**
     * 导出面试报告为 PDF 字节数组
     *
     * @param fields 报告字段 Map
     *               期望 Key：candidate, position, duration, totalScore, summary, qaList
     *               qaList 格式为多行文本（"Q: ...\nA: ...\n评分: ...\n"）
     * @return PDF 文件字节数组
     * @throws BusinessException 当 PDF 生成失败时
     */
    public byte[] exportInterviewReportPdf(Map<String, String> fields) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 50, 40, 50);

            PdfFont font = loadFont();

            addTitle(document, font, "模拟面试报告");
            addDivider(document);

            addSectionHeader(document, font, "面试信息");
            addInfoRow(document, font, "候选人", fields.getOrDefault("candidate", "-"));
            addInfoRow(document, font, "面试岗位", fields.getOrDefault("position", "-"));
            addInfoRow(document, font, "面试时长", fields.getOrDefault("duration", "-"));
            addInfoRow(document, font, "综合得分", fields.getOrDefault("totalScore", "-") + " / 100");

            addSectionHeader(document, font, "面试总评");
            addBodyText(document, font, fields.getOrDefault("summary", "暂无"));

            addSectionHeader(document, font, "题目详情");
            addBodyText(document, font, fields.getOrDefault("qaList", "暂无"));

            document.close();
            log.info("面试报告 PDF 导出成功，size={}bytes", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("面试报告 PDF 导出失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.EXPORT_PDF_FAILED,
                    "PDF 生成失败: " + e.getMessage());
        }
    }

    // ==================== 内部排版工具方法 ====================

    /**
     * 加载中文字体（优先使用系统宋体，回退为内嵌基础字体）
     *
     * @return PdfFont 实例
     */
    private PdfFont loadFont() throws Exception {
        try {
            // 1. 优先尝试加载项目内嵌的朱雀仿宋字体
            try (var fontStream = getClass().getClassLoader().getResourceAsStream("fonts/ZhuqueFangsong-Regular.ttf")) {
                if (fontStream == null) {
                    // 如果内嵌字体文件不存在，直接抛出异常触发第一层回退
                    throw new IllegalArgumentException("未找到内嵌字体文件: fonts/ZhuqueFangsong-Regular.ttf");
                }
                byte[] fontBytes = fontStream.readAllBytes();
                log.debug("成功加载项目内嵌字体: ZhuqueFangsong-Regular.ttf");
                return PdfFontFactory.createFont(fontBytes, PdfEncodings.IDENTITY_H, PdfFontFactory.EmbeddingStrategy.FORCE_EMBEDDED);
            }
        } catch (Exception e) {
            // 2. 内嵌字体加载失败（文件缺失或解析异常），回退尝试加载系统宋体
            log.warn("内嵌字体加载失败，尝试回退至系统宋体。原因: {}", e.getMessage());
            try {
                // 尝试加载 iText 自带的或系统环境下的中文字体
                return PdfFontFactory.createFont("STSong-Light", "UniGB-UCS2-H",
                        PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            } catch (Exception ex) {
                // 3. 系统中文字体也加载失败，最终保底回退至 Helvetica
                log.error("系统中文字体加载失败，最终使用回退字体 Helvetica。原因: {}", ex.getMessage());
                return PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA,
                        PdfEncodings.WINANSI, PdfFontFactory.EmbeddingStrategy.PREFER_EMBEDDED);
            }
        }
    }

    /**
     * 添加报告标题
     *
     * @param document PDF 文档
     * @param font     字体
     * @param title    标题文字
     */
    private void addTitle(Document document, PdfFont font, String title) {
        document.add(new Paragraph(title)
                .setFont(font)
                .setFontSize(20)
                .setFontColor(THEME_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10));
    }

    /**
     * 添加分隔线（用 Table 边框模拟）
     *
     * @param document PDF 文档
     */
    private void addDivider(Document document) {
        Table divider = new Table(UnitValue.createPercentArray(new float[]{1}))
                .useAllAvailableWidth()
                .setMarginBottom(16)
                .setBorderBottom(new com.itextpdf.layout.borders.SolidBorder(BORDER_COLOR, 1))
                .setBorderTop(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setBorderLeft(com.itextpdf.layout.borders.Border.NO_BORDER)
                .setBorderRight(com.itextpdf.layout.borders.Border.NO_BORDER);
        document.add(divider);
    }

    /**
     * 添加区块标题（蓝色加粗）
     *
     * @param document PDF 文档
     * @param font     字体
     * @param header   标题文字
     */
    private void addSectionHeader(Document document, PdfFont font, String header) {
        document.add(new Paragraph(header)
                .setFont(font)
                .setFontSize(13)
                .setFontColor(THEME_COLOR)
                .setBold()
                .setMarginTop(16)
                .setMarginBottom(6));
    }

    /**
     * 添加信息行（左侧标签 + 右侧值）
     *
     * @param document PDF 文档
     * @param font     字体
     * @param label    标签文字
     * @param value    值文字
     */
    private void addInfoRow(Document document, PdfFont font, String label, String value) {
        Table table = new Table(UnitValue.createPercentArray(new float[]{25, 75}))
                .useAllAvailableWidth()
                .setMarginBottom(4);
        table.addCell(new Cell().add(new Paragraph(label).setFont(font).setFontSize(10)
                .setFontColor(SECONDARY_COLOR)).setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        table.addCell(new Cell().add(new Paragraph(value).setFont(font).setFontSize(10))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER));
        document.add(table);
    }

    /**
     * 添加正文段落
     *
     * @param document PDF 文档
     * @param font     字体
     * @param text     正文内容
     */
    private void addBodyText(Document document, PdfFont font, String text) {
        document.add(new Paragraph(text)
                .setFont(font)
                .setFontSize(10)
                .setFontColor(SECONDARY_COLOR)
                .setMarginBottom(8)
                .setMultipliedLeading(1.5f));
    }

    /*
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final DeviceRgb HEADER_COLOR = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb SECTION_COLOR = new DeviceRgb(52, 73, 94);
    public byte[] exportInterviewReport(InterviewSessionEntity session) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdfDoc = new PdfDocument(writer);
        Document document = new Document(pdfDoc);

        // 使用支持中文的字体
        PdfFont font = createChineseFont();
        document.setFont(font);

        // 标题
        Paragraph title = new Paragraph("模拟面试报告")
                .setFontSize(24)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(HEADER_COLOR);
        document.add(title);

        // 基本信息
        document.add(new Paragraph("\n"));
        document.add(createSectionTitle("面试信息"));
        document.add(new Paragraph("会话ID: " + session.getSessionId()));
        document.add(new Paragraph("题目数量: " + session.getTotalQuestions()));
        document.add(new Paragraph("面试状态: " + getStatusText(session.getStatus())));
        document.add(new Paragraph("开始时间: " +
                (session.getCreatedAt() != null ? DATE_FORMAT.format(session.getCreatedAt()) : "未知")));
        if (session.getCompletedAt() != null) {
            document.add(new Paragraph("完成时间: " + DATE_FORMAT.format(session.getCompletedAt())));
        }

        // 总分
        if (session.getOverallScore() != null) {
            document.add(new Paragraph("\n"));
            document.add(createSectionTitle("综合评分"));
            Paragraph scoreP = new Paragraph("总分: " + session.getOverallScore() + " / 100")
                    .setFontSize(18)
                    .setBold()
                    .setFontColor(getScoreColor(session.getOverallScore()));
            document.add(scoreP);
        }

        // 总体评价
        if (session.getOverallFeedback() != null) {
            document.add(new Paragraph("\n"));
            document.add(createSectionTitle("总体评价"));
            document.add(new Paragraph(sanitizeText(session.getOverallFeedback())));
        }

        // 优势
        if (session.getStrengthsJson() != null) {
            try {
                List<String> strengths = objectMapper.readValue(session.getStrengthsJson(),
                        new TypeReference<>() {
                        });
                if (!strengths.isEmpty()) {
                    document.add(new Paragraph("\n"));
                    document.add(createSectionTitle("表现优势"));
                    for (String s : strengths) {
                        document.add(new Paragraph("• " + sanitizeText(s)));
                    }
                }
            } catch (Exception e) {
                log.error("解析优势JSON失败: sessionId={}", session.getSessionId(), e);
            }
        }

        // 改进建议
        if (session.getImprovementsJson() != null) {
            try {
                List<String> improvements = objectMapper.readValue(session.getImprovementsJson(),
                        new TypeReference<>() {
                        });
                if (!improvements.isEmpty()) {
                    document.add(new Paragraph("\n"));
                    document.add(createSectionTitle("改进建议"));
                    for (String s : improvements) {
                        document.add(new Paragraph("• " + sanitizeText(s)));
                    }
                }
            } catch (Exception e) {
                log.error("解析改进建议JSON失败: sessionId={}", session.getSessionId(), e);
            }
        }

        // 问答详情
        List<InterviewAnswerEntity> answers = session.getAnswers();
        if (answers != null && !answers.isEmpty()) {
            document.add(new Paragraph("\n"));
            document.add(createSectionTitle("问答详情"));

            for (InterviewAnswerEntity answer : answers) {
                document.add(new Paragraph("\n"));
                document.add(new Paragraph("问题 " + (answer.getQuestionIndex() + 1) +
                        " [" + (answer.getCategory() != null ? answer.getCategory() : "综合") + "]")
                        .setBold()
                        .setFontSize(12));
                document.add(new Paragraph("Q: " + sanitizeText(answer.getQuestion())));
                document.add(new Paragraph("A: " + sanitizeText(answer.getUserAnswer() != null ? answer.getUserAnswer() : "未回答")));
                document.add(new Paragraph("得分: " + answer.getScore() + "/100")
                        .setFontColor(getScoreColor(answer.getScore())));
                if (answer.getFeedback() != null) {
                    document.add(new Paragraph("评价: " + sanitizeText(answer.getFeedback()))
                            .setItalic());
                }
                if (answer.getReferenceAnswer() != null) {
                    document.add(new Paragraph("参考答案: " + sanitizeText(answer.getReferenceAnswer()))
                            .setFontColor(new DeviceRgb(39, 174, 96)));
                }
            }
        }

        document.close();
        return baos.toByteArray();
    }

    private Paragraph createSectionTitle(String title) {
        return new Paragraph(title)
                .setFontSize(14)
                .setBold()
                .setFontColor(SECTION_COLOR)
                .setMarginTop(10);
    }

    private void addScoreRow(Table table, String dimension, int score, int maxScore) {
        table.addCell(new Cell().add(new Paragraph(dimension)));
        table.addCell(new Cell().add(new Paragraph(score + " / " + maxScore)
                .setFontColor(getScoreColor(score * 100 / maxScore))));
    }

    private DeviceRgb getScoreColor(int score) {
        if (score >= 80) return new DeviceRgb(39, 174, 96);   // 绿色
        if (score >= 60) return new DeviceRgb(241, 196, 15);  // 黄色
        return new DeviceRgb(231, 76, 60);                    // 红色
    }

    private String getStatusText(InterviewSessionEntity.SessionStatus status) {
        return switch (status) {
            case CREATED -> "已创建";
            case IN_PROGRESS -> "进行中";
            case COMPLETED -> "已完成";
            case EVALUATED -> "已评估";
        };
    }
     */
}
