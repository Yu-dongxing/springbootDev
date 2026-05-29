/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/28
 */

package top.yuxs.springbootdev.modules.system.event;

import org.springframework.context.ApplicationEvent;
import top.yuxs.springbootdev.modules.system.entity.SysLog;

/**
 * 操作日志记录解耦事件
 *
 * @author YuDongXing
 * @since 2026/05/28
 */
public class AegisLogEvent extends ApplicationEvent {

    private final SysLog sysLog;

    public AegisLogEvent(Object source, SysLog sysLog) {
        super(source);
        this.sysLog = sysLog;
    }

    public SysLog getSysLog() {
        return this.sysLog;
    }
}
