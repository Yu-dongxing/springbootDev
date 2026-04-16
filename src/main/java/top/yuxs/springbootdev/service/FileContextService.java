/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/16
 */

package top.yuxs.springbootdev.service;

import cn.hutool.crypto.digest.DigestUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;
import top.yuxs.springbootdev.entity.SysFile;
import top.yuxs.springbootdev.enums.db.StorageType;
import top.yuxs.springbootdev.exception.BusinessException;
import top.yuxs.springbootdev.service.storage.StorageFactory;
import top.yuxs.springbootdev.service.storage.StorageService;
import top.yuxs.springbootdev.utils.IpUtils;

import java.io.IOException;

/**
 * 文件上下文服务（业务编排层）
 *
 * @author YuDongXing
 * @since 2026/04/16
 */
@Slf4j
@Service
public class FileContextService {

    @Autowired
    private StorageFactory storageFactory;

    @Autowired
    private SysFileService sysFileService;

    /**
     * 上传文件
     *
     * @param file    文件
     * @param bizId   业务关联 ID
     * @param bizType 业务类型
     * @param path    存储子路径
     * @return 文件信息
     */
    @Transactional(rollbackFor = Exception.class)
    public SysFile upload(MultipartFile file, String bizId, String bizType, String path) {
        StorageService storageService = storageFactory.getActiveService();

        // 1. 计算 MD5
        String md5;
        try {
            md5 = DigestUtil.md5Hex(file.getInputStream());
        } catch (IOException e) {
            log.error("计算文件 MD5 失败", e);
            throw new BusinessException("文件解析失败");
        }

        // 2. 物理上传
        String filePath = storageService.upload(file, path);
        String fileUrl = storageService.buildUrl(filePath);

        // 3. 记录落库
        SysFile sysFile = new SysFile();
        sysFile.setBizId(bizId);
        sysFile.setBizType(bizType);
        sysFile.setOriginalName(file.getOriginalFilename());
        sysFile.setFileName(filePath.substring(filePath.lastIndexOf("/") + 1));
        sysFile.setFileExt(getFileExtension(file.getOriginalFilename()));
        sysFile.setFileSize(file.getSize());
        sysFile.setContentType(file.getContentType());
        sysFile.setMd5(md5);
        sysFile.setStorageType(storageService.getType().name());
        sysFile.setFilePath(filePath);
        sysFile.setFileUrl(fileUrl);
        sysFile.setUploadStatus(1);
        sysFile.setUploadIp(getIpAddress());

        try {
            sysFileService.save(sysFile);
        } catch (Exception e) {
            // 4. 异常补偿：落库失败则删除已上传的物理文件
            log.error("文件记录落库失败，执行补偿删除: {}", filePath, e);
            storageService.delete(filePath);
            throw new BusinessException("文件上传保存记录失败");
        }

        return sysFile;
    }

    /**
     * 删除文件
     *
     * @param id       文件 ID
     * @param physical 是否物理删除
     */
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id, boolean physical) {
        SysFile sysFile = sysFileService.getById(id);
        if (sysFile == null) {
            return;
        }

        if (physical) {
            // 物理删除：先删记录 (调用物理删除方法，绕过 @TableLogic)
            sysFileService.physicalDeleteById(id);
            try {
                StorageService storageService = storageFactory.getService(StorageType.valueOf(sysFile.getStorageType()));
                storageService.delete(sysFile.getFilePath());
            } catch (Exception e) {
                log.error("物理文件删除失败: {}", sysFile.getFilePath(), e);
                // 宽松模式：物理文件删除失败不影响业务流程（后续可通过脚本清理）
            }
        } else {
            // 逻辑删除 (MyBatis-Plus 配置 @TableLogic 后 removeById 即为逻辑删除)
            sysFileService.removeById(id);
        }
    }

    /**
     * 构建最新的访问地址 (防止域名变更)
     */
    public String buildUrl(SysFile sysFile) {
        if (sysFile == null) return null;
        StorageService storageService = storageFactory.getService(StorageType.valueOf(sysFile.getStorageType()));
        return storageService.buildUrl(sysFile.getFilePath());
    }

    private String getFileExtension(String fileName) {
        if (fileName != null && fileName.contains(".")) {
            return fileName.substring(fileName.lastIndexOf("."));
        }
        return "";
    }

    private String getIpAddress() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            return IpUtils.getClientIp(request);
        }
        return "unknown";
    }
}
