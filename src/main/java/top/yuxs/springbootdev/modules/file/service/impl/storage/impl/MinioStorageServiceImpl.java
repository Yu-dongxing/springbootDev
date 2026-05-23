/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/23
 */

package top.yuxs.springbootdev.modules.file.service.impl.storage.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.yuxs.springbootdev.core.enums.db.StorageType;
import top.yuxs.springbootdev.core.exception.BusinessException;
import top.yuxs.springbootdev.modules.file.service.impl.storage.StorageService;

/**
 * MinIO 存储服务实现 (占位)
 *
 * @author YuDongXing
 * @since 2026/05/23
 */
@Service
public class MinioStorageServiceImpl implements StorageService {

    @Override
    public String upload(MultipartFile file, String path) {
        throw new BusinessException("MinIO 存储暂未集成，请使用本地存储");
    }

    @Override
    public void delete(String filePath) {
        // TODO: 集成 MinIO SDK 后实现
    }

    @Override
    public String buildUrl(String filePath) {
        return "https://minio-placeholder.com/" + filePath;
    }

    @Override
    public StorageType getType() {
        return StorageType.MINIO;
    }
}
