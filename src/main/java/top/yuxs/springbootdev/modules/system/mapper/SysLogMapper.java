/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/28
 */

package top.yuxs.springbootdev.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import top.yuxs.springbootdev.modules.system.entity.SysLog;

/**
 * 系统操作日志 Mapper 接口
 *
 * @author YuDongXing
 * @since 2026/05/28
 */
@Mapper
public interface SysLogMapper extends BaseMapper<SysLog> {
}
