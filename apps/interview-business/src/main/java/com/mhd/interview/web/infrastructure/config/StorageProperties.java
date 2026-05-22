package com.mhd.interview.web.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 对象存储（MinIO/S3）配置属性
 * <p>
 * 对应配置前缀 {@code app.storage}，所有值均通过环境变量注入以保证安全
 *
 * @param endpoint  MinIO 服务端点 URL（如 http://127.0.0.1:9000）
 * @param accessKey 访问密钥 ID
 * @param secretKey 访问密钥密文
 * @param bucket    默认存储桶名称
 * @param region    存储区域（如 us-east-1）
 * @author mhd
 */
@ConfigurationProperties(prefix = "app.storage")
public record StorageProperties(
        String endpoint,
        String accessKey,
        String secretKey,
        String bucket,
        String region
) {
}
