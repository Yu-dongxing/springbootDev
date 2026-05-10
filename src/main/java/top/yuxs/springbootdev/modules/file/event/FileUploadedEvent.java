/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/10
 */

package top.yuxs.springbootdev.modules.file.event;

import lombok.Getter;
import org.springframework.context.ApplicationEvent;
import top.yuxs.springbootdev.modules.file.entity.SysFile;

/**
 * 文件上传完成事件
 */
@Getter
public class FileUploadedEvent extends ApplicationEvent {

    private final SysFile sysFile;

    public FileUploadedEvent(Object source, SysFile sysFile) {
        super(source);
        this.sysFile = sysFile;
    }
}
