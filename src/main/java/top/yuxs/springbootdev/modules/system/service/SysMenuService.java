/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.yuxs.springbootdev.modules.system.entity.SysMenu;

import java.util.List;

/**
 * 系统菜单与按钮权限 服务类
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
public interface SysMenuService extends IService<SysMenu> {

    /**
     * 根据用户ID查询按钮级权限标识列表 (前端显示使用)
     */
    List<String> getPermsByUserId(Long userId);
}
