/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.yuxs.springbootdev.modules.system.entity.SysMenu;

import java.util.List;

/**
 * 系统菜单与按钮权限 Mapper 接口
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Mapper
public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /**
     * 根据用户ID查询前端按钮权限标识列表
     */
    List<String> selectPermsByUserId(@Param("userId") Long userId);
}
