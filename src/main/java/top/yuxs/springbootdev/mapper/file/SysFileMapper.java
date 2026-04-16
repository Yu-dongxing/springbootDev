/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/04/16
 */

package top.yuxs.springbootdev.mapper.file;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import top.yuxs.springbootdev.entity.SysFile;

/**
 * 文件信息 Mapper
 *
 * @author YuDongXing
 * @since 2026/04/16
 */
@Mapper
public interface SysFileMapper extends BaseMapper<SysFile> {

    /**
     * 物理删除记录
     *
     * @param id ID
     * @return 影响行数
     */
    @Delete("DELETE FROM sys_file WHERE id = #{id}")
    int physicalDeleteById(@Param("id") Long id);
}
