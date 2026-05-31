/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import top.yuxs.springbootdev.modules.system.entity.SysMenu;
import top.yuxs.springbootdev.modules.system.mapper.SysMenuMapper;
import top.yuxs.springbootdev.modules.system.service.SysMenuService;

import java.util.List;

/**
 * 系统菜单权限 服务实现类
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Service
public class SysMenuServiceImpl extends ServiceImpl<SysMenuMapper, SysMenu> implements SysMenuService {

    @Override
    public List<String> getPermsByUserId(Long userId) {
        return this.baseMapper.selectPermsByUserId(userId);
    }
}
