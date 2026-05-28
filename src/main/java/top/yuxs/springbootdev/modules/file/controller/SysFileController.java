/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/28
 */

package top.yuxs.springbootdev.modules.file.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.yuxs.springbootdev.core.common.BaseController;
import top.yuxs.springbootdev.modules.file.entity.SysFile;
import top.yuxs.springbootdev.modules.file.service.SysFileService;

/**
 * 系统文件后台管理接口控制器
 * 继承 BaseController 后，自动获得如下标准 CRUD API：
 * 1. 单条新增: POST   /sys/file/save
 * 2. 批量新增: POST   /sys/file/saveBatch
 * 3. 单条删除: DELETE /sys/file/delete/{id}
 * 4. 批量删除: DELETE /sys/file/deleteBatch
 * 5. 局部更新: PUT    /sys/file/update
 * 6. 条件分页: GET    /sys/file/page
 *
 * @author YuDongXing
 * @since 2026/05/28
 */
@RestController
@RequestMapping("/sys/file")
public class SysFileController extends BaseController<SysFile, SysFileService> {

    /**
     * 重写查询钩子，定制更适合文件搜索的查询条件。
     * 支持 originalName 模糊查询，以及等值的 bizType / bizId 匹配。
     */
    @Override
    protected QueryWrapper<SysFile> getQueryWrapper(SysFile queryEntity) {
        QueryWrapper<SysFile> wrapper = new QueryWrapper<>();
        if (queryEntity == null) {
            return wrapper;
        }

        // 1. originalName 模糊查询
        if (queryEntity.getOriginalName() != null && !queryEntity.getOriginalName().trim().isEmpty()) {
            wrapper.like("original_name", queryEntity.getOriginalName().trim());
        }

        // 2. bizType 等值匹配
        if (queryEntity.getBizType() != null && !queryEntity.getBizType().trim().isEmpty()) {
            wrapper.eq("biz_type", queryEntity.getBizType().trim());
        }

        // 3. bizId 等值匹配
        if (queryEntity.getBizId() != null && !queryEntity.getBizId().trim().isEmpty()) {
            wrapper.eq("biz_id", queryEntity.getBizId().trim());
        }

        // 4. storageBucket 等值匹配
        if (queryEntity.getStorageBucket() != null && !queryEntity.getStorageBucket().trim().isEmpty()) {
            wrapper.eq("storage_bucket", queryEntity.getStorageBucket().trim());
        }

        return wrapper;
    }
}
