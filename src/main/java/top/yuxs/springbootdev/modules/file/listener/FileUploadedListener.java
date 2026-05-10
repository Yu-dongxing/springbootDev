/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/10
 */

package top.yuxs.springbootdev.modules.file.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import top.yuxs.springbootdev.modules.file.event.FileUploadedEvent;
import top.yuxs.springbootdev.modules.file.service.SysFileService;

/**
 * 文件上传完成事件监听器 - 负责落库
 */
@Slf4j
@Component
public class FileUploadedListener {

    @Autowired
    private SysFileService sysFileService;

    @EventListener
    public void onFileUploaded(FileUploadedEvent event) {
        log.info("监听到文件上传完成事件，准备落库: {}", event.getSysFile().getFileName());
        try {
            sysFileService.save(event.getSysFile());
        } catch (Exception e) {
            log.error("文件落库失败: {}", event.getSysFile().getFileName(), e);
            // 这里可以抛出异常以回滚事务（如果发布者开启了事务且监听器是同步的）
            throw e;
        }
    }
}
