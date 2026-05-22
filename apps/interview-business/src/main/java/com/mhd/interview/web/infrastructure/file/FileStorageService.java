package com.mhd.interview.web.infrastructure.file;

import com.mhd.interview.common.enums.ErrorCodeEnum;
import com.mhd.interview.common.exception.BusinessException;
import com.mhd.interview.web.infrastructure.config.StorageProperties;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 对象存储服务（MinIO S3）
 * <p>
 * 封装文件上传、下载、删除、URL 生成等操作。
 * 所有存储桶操作均使用 {@code app.storage.bucket} 配置的默认桶。
 *
 * @author mhd
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageService {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;

    /**
     * 应用启动时自动初始化存储桶（不存在则创建）
     */
    @PostConstruct
    public void initBucket() {
        try {
            boolean exists = minioClient.bucketExists(
                    BucketExistsArgs.builder().bucket(storageProperties.bucket()).build());
            if (!exists) {
                minioClient.makeBucket(
                        MakeBucketArgs.builder().bucket(storageProperties.bucket()).build());
                log.info("存储桶已创建: {}", storageProperties.bucket());
            }
        } catch (Exception e) {
            log.warn("初始化存储桶失败，请检查 MinIO 配置: {}", e.getMessage());
        }
    }

    /**
     * 通用文件上传，Key 格式为 {prefix}/{uuid}.{ext}
     *
     * @param file   MultipartFile 文件对象
     * @param prefix 存储路径前缀（如 "resumes"、"knowledge"）
     * @return MinIO 对象 Key
     */
    public String uploadFile(MultipartFile file, String prefix) {
        String originalFilename = file.getOriginalFilename();
        String ext = extractExtension(originalFilename);
        String fileKey = prefix + "/" + UUID.randomUUID() + ext;

        try (InputStream inputStream = file.getInputStream()) {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(storageProperties.bucket())
                    .object(fileKey)
                    .stream(inputStream, file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build());
            log.info("文件上传成功: key={}, size={}bytes", fileKey, file.getSize());
            return fileKey;
        } catch (Exception e) {
            log.error("文件上传失败: prefix={}, error={}", prefix, e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.STORAGE_UPLOAD_FAILED,
                    "文件上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传简历文件（存储到 resumes/ 目录）
     *
     * @param file 简历 MultipartFile
     * @return MinIO 对象 Key
     */
    public String uploadResume(MultipartFile file) {
        return uploadFile(file, "resumes");
    }

    /**
     * 上传知识库文档（存储到 knowledge/ 目录）
     *
     * @param file 知识库文档 MultipartFile
     * @return MinIO 对象 Key
     */
    public String uploadKnowledgeBase(MultipartFile file) {
        return uploadFile(file, "knowledge");
    }

    /**
     * 上传 PDF 报告（存储到 reports/ 目录）
     *
     * @param pdfBytes  PDF 字节数组
     * @param fileKey   目标对象 Key（已包含路径和文件名）
     */
    public void uploadPdf(byte[] pdfBytes, String fileKey) {
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(storageProperties.bucket())
                    .object(fileKey)
                    .stream(new java.io.ByteArrayInputStream(pdfBytes), pdfBytes.length, -1)
                    .contentType("application/pdf")
                    .build());
            log.info("PDF 上传成功: key={}", fileKey);
        } catch (Exception e) {
            log.error("PDF 上传失败: key={}, error={}", fileKey, e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.STORAGE_UPLOAD_FAILED,
                    "PDF 上传失败: " + e.getMessage());
        }
    }

    /**
     * 生成预签名访问 URL（有效期1小时）
     *
     * @param fileKey 对象 Key
     * @return 预签名 URL
     */
    public String getFileUrl(String fileKey) {
        try {
            return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder()
                    .bucket(storageProperties.bucket())
                    .object(fileKey)
                    .method(Method.GET)
                    .expiry(1, TimeUnit.HOURS)
                    .build());
        } catch (Exception e) {
            log.error("生成文件 URL 失败: key={}, error={}", fileKey, e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.STORAGE_DOWNLOAD_FAILED,
                    "生成文件访问地址失败: " + e.getMessage());
        }
    }

    /**
     * 下载文件为输入流（调用方负责关闭流）
     *
     * @param fileKey 对象 Key
     * @return 文件输入流
     */
    public InputStream downloadFile(String fileKey) {
        try {
            return minioClient.getObject(GetObjectArgs.builder()
                    .bucket(storageProperties.bucket())
                    .object(fileKey)
                    .build());
        } catch (Exception e) {
            log.error("文件下载失败: key={}, error={}", fileKey, e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.STORAGE_DOWNLOAD_FAILED,
                    "文件下载失败: " + e.getMessage());
        }
    }

    /**
     * 删除文件（逻辑上不可恢复，请谨慎调用）
     *
     * @param fileKey 对象 Key
     */
    public void deleteFile(String fileKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder()
                    .bucket(storageProperties.bucket())
                    .object(fileKey)
                    .build());
            log.info("文件删除成功: key={}", fileKey);
        } catch (Exception e) {
            log.error("文件删除失败: key={}, error={}", fileKey, e.getMessage(), e);
            throw new BusinessException(ErrorCodeEnum.STORAGE_DELETE_FAILED,
                    "文件删除失败: " + e.getMessage());
        }
    }

    // ==================== 内部工具方法 ====================

    /**
     * 从文件名中提取扩展名（含 "."，如 ".pdf"）
     *
     * @param filename 原始文件名
     * @return 扩展名字符串，若无扩展名返回空字符串
     */
    private String extractExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
