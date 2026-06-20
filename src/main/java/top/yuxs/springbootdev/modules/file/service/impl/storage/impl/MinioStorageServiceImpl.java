/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/23
 */

package top.yuxs.springbootdev.modules.file.service.impl.storage.impl;

import io.minio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.yuxs.springbootdev.core.config.file.FileProperties;
import top.yuxs.springbootdev.core.enums.db.StorageType;
import top.yuxs.springbootdev.core.exception.BusinessException;
import top.yuxs.springbootdev.modules.file.service.impl.storage.StorageService;

import java.io.InputStream;
import java.util.UUID;

/**
 * MinIO 存储服务实现
 *
 * @author YuDongXing
 * @since 2026/05/23 (Updated 2026/06/20)
 */
@Slf4j
@Service
public class MinioStorageServiceImpl implements StorageService {

    @Autowired
    private FileProperties fileProperties;

    private volatile MinioClient minioClient;

    /**
     * 获取 MinioClient (双重校验锁懒加载，防止高并发下重复初始化，且防范未配置时的启动报错)
     */
    private MinioClient getMinioClient() {
        if (minioClient == null) {
            synchronized (this) {
                if (minioClient == null) {
                    FileProperties.MinioConfig minio = fileProperties.getMinio();
                    if (minio == null || minio.getEndpoint() == null || minio.getEndpoint().isBlank()) {
                        throw new BusinessException("MinIO 存储端点(endpoint)未正确配置，无法连接服务器！");
                    }
                    try {
                        log.info("Aegis-File-Hub: 正在延迟初始化 MinIO 客户端... 端点: {}", minio.getEndpoint());
                        MinioClient client = MinioClient.builder()
                                .endpoint(minio.getEndpoint())
                                .credentials(minio.getAccessKey(), minio.getSecretKey())
                                .build();

                        // 自动校验并创建存储桶
                        if (Boolean.TRUE.equals(minio.getAutoCreateBucket())) {
                            boolean found = client.bucketExists(BucketExistsArgs.builder().bucket(minio.getBucketName()).build());
                            if (!found) {
                                log.info("Aegis-File-Hub: MinIO 存储桶 [{}] 不存在，正在自动创建并配置公共只读策略...", minio.getBucketName());
                                client.makeBucket(MakeBucketArgs.builder().bucket(minio.getBucketName()).build());
                                
                                // 配置公共只读 Policy，使得生成的直链可以在不签名的情况下直接公开访问
                                String policyJson = "{\n" +
                                        "  \"Statement\": [\n" +
                                        "    {\n" +
                                        "      \"Action\": [\"s3:GetObject\"],\n" +
                                        "      \"Effect\": \"Allow\",\n" +
                                        "      \"Principal\": \"*\",\n" +
                                        "      \"Resource\": [\"arn:aws:s3:::" + minio.getBucketName() + "/*\"]\n" +
                                        "    }\n" +
                                        "  ],\n" +
                                        "  \"Version\": \"2012-10-17\"\n" +
                                        "}";
                                client.setBucketPolicy(SetBucketPolicyArgs.builder()
                                        .bucket(minio.getBucketName())
                                        .config(policyJson)
                                        .build());
                                log.info("Aegis-File-Hub: MinIO 存储桶 [{}] 创建并成功绑定 Anonymous-Read-Only 策略！", minio.getBucketName());
                            }
                        }
                        this.minioClient = client;
                    } catch (Exception e) {
                        log.error("Aegis-File-Hub: 初始化 MinIO 客户端异常", e);
                        throw new BusinessException("初始化 MinIO 客户端失败: " + e.getMessage());
                    }
                }
            }
        }
        return minioClient;
    }

    @Override
    public String upload(MultipartFile file, String path) {
        MinioClient client = getMinioClient();
        FileProperties.MinioConfig minio = fileProperties.getMinio();

        try {
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String fileName = UUID.randomUUID().toString() + extension;

            // 获取当前年月做规范化目录划分
            java.time.LocalDate now = java.time.LocalDate.now();
            String year = String.valueOf(now.getYear());
            String month = String.format("%02d", now.getMonthValue());

            // 构造对象 Key (去除前导斜杠，去除重复的斜杠)
            String objectKey = (path + "/" + year + "/" + month + "/" + fileName)
                    .replaceAll("//+", "/")
                    .replaceAll("^/", "");

            try (InputStream is = file.getInputStream()) {
                client.putObject(
                        PutObjectArgs.builder()
                                .bucket(minio.getBucketName())
                                .object(objectKey)
                                .stream(is, file.getSize(), -1)
                                .contentType(file.getContentType())
                                .build()
                );
            }
            log.info("Aegis-File-Hub: 文件成功上传到 MinIO. Bucket={}, Key={}", minio.getBucketName(), objectKey);
            return objectKey;
        } catch (Exception e) {
            log.error("Aegis-File-Hub: MinIO 文件上传失败", e);
            throw new BusinessException("MinIO 文件上传失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String filePath) {
        MinioClient client = getMinioClient();
        FileProperties.MinioConfig minio = fileProperties.getMinio();
        try {
            client.removeObject(
                    RemoveObjectArgs.builder()
                            .bucket(minio.getBucketName())
                            .object(filePath)
                            .build()
            );
            log.info("Aegis-File-Hub: 成功从 MinIO 中物理删除文件. Bucket={}, Key={}", minio.getBucketName(), filePath);
        } catch (Exception e) {
            log.error("Aegis-File-Hub: MinIO 物理删除文件失败, Key={}", filePath, e);
            throw new BusinessException("MinIO 文件物理删除失败: " + e.getMessage());
        }
    }

    @Override
    public String buildUrl(String filePath) {
        FileProperties.MinioConfig minio = fileProperties.getMinio();

        // 1. 优先使用自定义访问域名
        if (minio.getDomain() != null && !minio.getDomain().isBlank()) {
            String domain = minio.getDomain();
            if (!domain.endsWith("/")) {
                domain += "/";
            }
            return domain + minio.getBucketName() + "/" + filePath;
        }

        // 2. 备用：基于 Endpoint 的直链构造 (需确保 MinIO 设置了桶的公共读策略)
        String endpoint = minio.getEndpoint();
        if (endpoint.endsWith("/")) {
            endpoint = endpoint.substring(0, endpoint.length() - 1);
        }
        return endpoint + "/" + minio.getBucketName() + "/" + filePath;
    }

    @Override
    public StorageType getType() {
        return StorageType.MINIO;
    }
}
