/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/20
 */

package top.yuxs.springbootdev.modules.file.service.impl.storage.impl;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import top.yuxs.springbootdev.core.config.file.FileProperties;
import top.yuxs.springbootdev.core.enums.db.StorageType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * MinioStorageServiceImpl 单元测试
 *
 * @author YuDongXing
 * @since 2026/06/20
 */
@SpringBootTest
@ActiveProfiles("test") // 保证不影响正常的开发配置
public class MinioStorageServiceImplTest {

    @Autowired
    private FileProperties fileProperties;

    @Autowired
    private MinioStorageServiceImpl minioStorageService;

    @Test
    public void testFilePropertiesBinding() {
        assertNotNull(fileProperties);
        FileProperties.MinioConfig minio = fileProperties.getMinio();
        assertNotNull(minio);
        
        // 验证默认绑定值
        assertEquals("http://127.0.0.1:9000", minio.getEndpoint());
        assertEquals("dev-bucket", minio.getBucketName());
        assertEquals("admin", minio.getAccessKey());
        assertEquals("admin123", minio.getSecretKey());
        assertTrue(minio.getAutoCreateBucket());
    }

    @Test
    public void testGetType() {
        assertNotNull(minioStorageService);
        assertEquals(StorageType.MINIO, minioStorageService.getType());
    }

    @Test
    public void testBuildUrlWithEndpoint() {
        // 设置临时属性并恢复
        FileProperties.MinioConfig minio = fileProperties.getMinio();
        String originalDomain = minio.getDomain();
        String originalEndpoint = minio.getEndpoint();
        String originalBucket = minio.getBucketName();
        
        try {
            minio.setDomain("");
            minio.setEndpoint("http://minio.test.local:9000/");
            minio.setBucketName("test-bucket");
            
            String fileUrl = minioStorageService.buildUrl("2026/06/test.png");
            assertEquals("http://minio.test.local:9000/test-bucket/2026/06/test.png", fileUrl);
        } finally {
            minio.setDomain(originalDomain);
            minio.setEndpoint(originalEndpoint);
            minio.setBucketName(originalBucket);
        }
    }

    @Test
    public void testBuildUrlWithDomain() {
        FileProperties.MinioConfig minio = fileProperties.getMinio();
        String originalDomain = minio.getDomain();
        String originalEndpoint = minio.getEndpoint();
        String originalBucket = minio.getBucketName();
        
        try {
            minio.setDomain("https://cdn.test.local");
            minio.setEndpoint("http://minio.test.local:9000");
            minio.setBucketName("test-bucket");
            
            String fileUrl = minioStorageService.buildUrl("2026/06/test.png");
            assertEquals("https://cdn.test.local/test-bucket/2026/06/test.png", fileUrl);
        } finally {
            minio.setDomain(originalDomain);
            minio.setEndpoint(originalEndpoint);
            minio.setBucketName(originalBucket);
        }
    }
}
