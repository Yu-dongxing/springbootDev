/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/16
 */

package top.yuxs.springbootdev.service.file.impl.storage.impl;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import top.yuxs.springbootdev.enums.db.StorageType;
import top.yuxs.springbootdev.exception.BusinessException;
import top.yuxs.springbootdev.service.file.impl.storage.StorageService;

/**
 * 阿里云存储服务实现 (占位)
 *
 * @author YuDongXing
 * @since 2026/04/16
 */
@Service
public class AliyunStorageServiceImpl implements StorageService {

    @Override
    public String upload(MultipartFile file, String path) {
        throw new BusinessException("阿里云 OSS 存储暂未集成，请使用本地存储");
    }

    @Override
    public void delete(String filePath) {
        // TODO: 集成阿里云 SDK 后实现
    }

    @Override
    public String buildUrl(String filePath) {
        return "https://oss-placeholder.com/" + filePath;
    }

    @Override
    public StorageType getType() {
        return StorageType.ALIYUN_OSS;
    }
}
