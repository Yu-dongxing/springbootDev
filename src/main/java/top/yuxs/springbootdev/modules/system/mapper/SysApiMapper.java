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
import top.yuxs.springbootdev.modules.system.entity.SysApi;

import java.util.Set;

/**
 * 物理 API 接口资源 Mapper 接口
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Mapper
public interface SysApiMapper extends BaseMapper<SysApi> {

    /**
     * 根据用户ID三表联查该用户所拥有的所有可用 API 授权规则列表 (格式: METHOD:PATH)
     */
    Set<String> selectApiPermissionsByUserId(@Param("userId") Long userId);
}
