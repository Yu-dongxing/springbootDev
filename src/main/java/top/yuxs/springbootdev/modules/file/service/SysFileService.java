/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/16
 */

package top.yuxs.springbootdev.modules.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.yuxs.springbootdev.modules.file.entity.SysFile;

/**
 * 文件信息服务
 *
 * @author YuDongXing
 * @since 2026/04/16
 */
public interface SysFileService extends IService<SysFile> {

    /**
     * 物理删除记录
     *
     * @param id ID
     * @return 是否成功
     */
    boolean physicalDeleteById(Long id);
}
