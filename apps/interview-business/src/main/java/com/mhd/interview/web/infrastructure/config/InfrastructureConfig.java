package com.mhd.interview.web.infrastructure.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 基础设施层配置类
 * <p>
 * 注册 MinIO 客户端等基础设施 Bean
 *
 * @author mhd
 */
@Configuration
@EnableConfigurationProperties(StorageProperties.class)
@RequiredArgsConstructor
public class InfrastructureConfig {

    private final StorageProperties storageProperties;

    /**
     * 注册 MinIO 客户端 Bean
     * <p>
     * 使用 {@code app.storage.*} 中的配置创建连接
     *
     * @return MinioClient 实例
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(storageProperties.endpoint())
                .credentials(storageProperties.accessKey(), storageProperties.secretKey())
                .build();
    }
}
