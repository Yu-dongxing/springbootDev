/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/16
 */

package top.yuxs.springbootdev.modules.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.yuxs.springbootdev.modules.file.entity.SysFile;
import top.yuxs.springbootdev.modules.file.mapper.SysFileMapper;
import top.yuxs.springbootdev.modules.file.service.SysFileService;

/**
 * 文件信息服务实现
 *
 * @author YuDongXing
 * @since 2026/04/16
 */
@Service
public class SysFileServiceImpl extends ServiceImpl<SysFileMapper, SysFile> implements SysFileService {

    @Override
    public boolean physicalDeleteById(Long id) {
        return baseMapper.physicalDeleteById(id) > 0;
    }
}
