/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.yuxs.springbootdev.modules.system.entity.SysUserSocial;

/**
 * 用户第三方社交账号绑定 Mapper 接口
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Mapper
public interface SysUserSocialMapper extends BaseMapper<SysUserSocial> {
}
