/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.yuxs.springbootdev.modules.system.entity.SysRoleApi;

/**
 * 角色接口关系 Mapper 接口
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Mapper
public interface SysRoleApiMapper extends BaseMapper<SysRoleApi> {
}
