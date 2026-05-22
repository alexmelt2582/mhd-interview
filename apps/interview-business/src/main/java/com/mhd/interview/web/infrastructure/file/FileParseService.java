package com.mhd.interview.web.infrastructure.file;

import com.mhd.interview.common.enums.ErrorCodeEnum;
import com.mhd.interview.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.apache.tika.extractor.EmbeddedDocumentExtractor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.pdf.PDFParserConfig;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.Set;

/**
 * 文件解析服务（基于 Apache Tika）
 * <p>
 * 提供文件文本提取、哈希计算、类型检测及安全校验功能。
 * 支持 PDF、Word、Excel、TXT 等常见文档格式。
 *
 * @author mhd
 */
@Service
@Slf4j
public class FileParseService {

    private final Tika tika = new Tika();

    /**
     * 默认文件大小上限：20 MB
     */
    private static final long DEFAULT_MAX_FILE_SIZE = 20 * 1024 * 1024;

    /**
     * 文本提取最大长度：5 MB（超过部分将被截断，避免内存溢出）
     */
    private static final int MAX_TEXT_LENGTH = 5 * 1024 * 1024;

    /**
     * 允许上传的知识库文件 MIME 类型
     */
    private static final Set<String> ALLOWED_KNOWLEDGE_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "text/markdown",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    /**
     * 文件大小上限：20 MB
     */
    private static final long MAX_FILE_SIZE = 20 * 1024 * 1024;

    /**
     * Tika 提取文件的纯文本内容
     *
     * @param inputStream 文件输入流
     * @return 提取出的纯文本字符串
     * @throws BusinessException 当文件解析失败时
     */
    public String tikaParseText(InputStream inputStream) {
        try {
            // 1. 创建自动检测解析器
            AutoDetectParser parser = new AutoDetectParser();
            // 2. 创建内容处理器，只接收正文，限制最大长度为 5MB
            BodyContentHandler handler = new BodyContentHandler(MAX_TEXT_LENGTH);
            // 3. 创建元数据对象
            Metadata metadata = new Metadata();
            // 4. 创建解析上下文
            ParseContext context = new ParseContext();
            // 5. 显式指定 Parser 到 Context（增强健壮性）
            context.set(Parser.class, parser);
            // 6. 禁用嵌入文档解析（关键：避免提取图片引用和临时文件路径）
            context.set(EmbeddedDocumentExtractor.class, new NoOpEmbeddedDocumentExtractor());
            // 7. PDF 专用配置：关闭图片提取，按位置排序文本
            PDFParserConfig pdfConfig = new PDFParserConfig();
            pdfConfig.setExtractInlineImages(false);
            pdfConfig.setSortByPosition(true); // 按 x/y 坐标排序文本，改善多栏布局解析顺序
            // 注意：Tika 2.9.2 中 setExtractAnnotations 方法可能不存在，关闭图片提取已足够
            context.set(PDFParserConfig.class, pdfConfig);
            // 8. 执行解析
            parser.parse(inputStream, handler, metadata, context);
            String text = handler.toString();
            log.debug("Tika文本提取完成，字符数: {}", text.length());
            return text;
        } catch (Exception e) {
            log.error("Tika文件文本提取失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.RESUME_PARSE_FAILED,
                    "文件内容解析失败: " + e.getMessage());
        }
    }

    /**
     * 计算文件的 SHA-256 哈希值（用于简历去重）
     *
     * @param inputStream 文件输入流
     * @return 十六进制 SHA-256 哈希字符串（小写）
     * @throws BusinessException 当哈希计算失败时
     */
    public String calculateHash(InputStream inputStream) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            log.error("文件哈希计算失败: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.STORAGE_UPLOAD_FAILED,
                    "文件哈希计算失败: " + e.getMessage());
        }
    }


    /**
     * 检测文件的真实 MIME 类型（基于文件内容，而非扩展名）
     *
     * @param inputStream 文件输入流
     * @return MIME 类型字符串（如 "application/pdf"）
     */
    public String detectContentType(InputStream inputStream) {
        try {
            return tika.detect(inputStream);
        } catch (Exception e) {
            log.warn("MIME 类型检测失败，返回 application/octet-stream: {}", e.getMessage());
            return "application/octet-stream";
        }
    }

    /**
     * 文件合法性校验，包括大小和类型校验
     *
     * @param file         文件对象
     * @param maxSizeBytes 最大文件大小（字节）
     * @param allowedTypes 允许的 MIME 类型集合
     * @param fileTypeName 文件类型说明（用于错误信息）
     */
    public void validateFile(MultipartFile file, long maxSizeBytes, Set<String> allowedTypes, String fileTypeName) {
        if (file.isEmpty()) {
            throw new BusinessException(ErrorCodeEnum.VALID_FAILED,
                    String.format("请选择要上传的%s文件", fileTypeName));
        }
        if (maxSizeBytes <= 0) {
            maxSizeBytes = DEFAULT_MAX_FILE_SIZE;
            log.warn("未指定{}文件大小上限，使用默认值: {} MB", fileTypeName, maxSizeBytes / (1024 * 1024));
        }
        // 大小校验
        if (file.getSize() > maxSizeBytes) {
            throw new BusinessException(ErrorCodeEnum.RESUME_PARSE_FAILED,
                    fileTypeName + "文件大小超过限制（最大 " + maxSizeBytes / (1024 * 1024) + "MB）");
        }

        // 类型校验（同时检查 Content-Type 和 Tika 探测）
        try {
            String detectedType = tika.detect(file.getInputStream());
            if (!allowedTypes.contains(detectedType)) {
                throw new BusinessException(ErrorCodeEnum.RESUME_PARSE_FAILED,
                        "不支持的" + fileTypeName + "文件类型: " + detectedType);
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCodeEnum.RESUME_PARSE_FAILED,
                    "文件类型校验失败: " + e.getMessage());
        }
    }

    /**
     * 格式化文件大小为人类可读的字符串（如 "1.2MB"）
     *
     * @param bytes 文件大小（字节数）
     * @return 格式化后的字符串
     */
    public String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + "B";
        if (bytes < 1024 * 1024) return String.format("%.1fKB", bytes / 1024.0);
        return String.format("%.1fMB", bytes / (1024.0 * 1024.0));
    }
}
