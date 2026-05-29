/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/29
 */

package top.yuxs.springbootdev.modules.system.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import top.yuxs.springbootdev.core.common.BaseController;
import top.yuxs.springbootdev.core.common.Result;
import top.yuxs.springbootdev.core.enums.ResultCode;
import top.yuxs.springbootdev.modules.system.entity.SysLog;
import top.yuxs.springbootdev.modules.system.service.SysLogService;

import java.util.List;

/**
 * 系统操作日志后台管理接口控制器
 * 
 * 继承 BaseController 后，默认获得以下安全 API：
 * 1. 条件分页筛选: GET    /sys/log/page
 * 2. 单条日志删除: DELETE /sys/log/delete/{id}
 * 3. 批量日志删除: DELETE /sys/log/deleteBatch
 * 
 * 安全防御：
 * - 显式重写并强行禁用了日志的新增和更新 API (save, saveBatch, update, updateBatch)，杜绝篡改日志的安全风险。
 *
 * @author YuDongXing
 * @since 2026/05/29
 */
@RestController
@RequestMapping("/sys/log")
public class SysLogController extends BaseController<SysLog, SysLogService> {

    /**
     * 重写查询Wrapper构造钩子，实现全面的多条件组合分页筛选
     */
    @Override
    protected QueryWrapper<SysLog> getQueryWrapper(SysLog queryEntity) {
        QueryWrapper<SysLog> wrapper = new QueryWrapper<>();
        
        // 默认按请求时间倒序排列 (让最新的日志排在第一位)
        wrapper.orderByDesc("request_time");

        if (queryEntity == null) {
            return wrapper;
        }

        // 1. 操作用户名模糊查询
        if (queryEntity.getUsername() != null && !queryEntity.getUsername().trim().isEmpty()) {
            wrapper.like("username", queryEntity.getUsername().trim());
        }

        // 2. 访问IP精确等值匹配
        if (queryEntity.getIp() != null && !queryEntity.getIp().trim().isEmpty()) {
            wrapper.eq("ip", queryEntity.getIp().trim());
        }

        // 3. 请求URL模糊匹配
        if (queryEntity.getUrl() != null && !queryEntity.getUrl().trim().isEmpty()) {
            wrapper.like("url", queryEntity.getUrl().trim());
        }

        // 4. 请求方式 (GET/POST/PUT/DELETE) 精确等值匹配
        if (queryEntity.getMethod() != null && !queryEntity.getMethod().trim().isEmpty()) {
            wrapper.eq("method", queryEntity.getMethod().trim().toUpperCase());
        }

        // 5. 接口注释名称模糊匹配
        if (queryEntity.getTitle() != null && !queryEntity.getTitle().trim().isEmpty()) {
            wrapper.like("title", queryEntity.getTitle().trim());
        }

        // 6. 业务操作类型等值匹配 (INSERT/UPDATE/DELETE/SELECT/OTHER)
        if (queryEntity.getBusinessType() != null && !queryEntity.getBusinessType().trim().isEmpty()) {
            wrapper.eq("business_type", queryEntity.getBusinessType().trim().toUpperCase());
        }

        // 7. 执行状态等值匹配 (1: 成功, 0: 失败)
        if (queryEntity.getStatus() != null) {
            wrapper.eq("status", queryEntity.getStatus());
        }

        // 8. 无感支持操作时间段查询 (提取 HttpServletRequest 里的 beginTime 与 endTime 参数)
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes != null) {
            HttpServletRequest request = attributes.getRequest();
            String beginTime = request.getParameter("beginTime");
            String endTime = request.getParameter("endTime");

            if (beginTime != null && !beginTime.trim().isEmpty()) {
                wrapper.ge("request_time", beginTime.trim());
            }
            if (endTime != null && !endTime.trim().isEmpty()) {
                wrapper.le("request_time", endTime.trim());
            }
        }

        return wrapper;
    }

    // ==================== 安全防御控制：显式重写写接口，强行封禁写入权限 ====================

    @Override
    @PostMapping("/save")
    public Result<?> save(@RequestBody SysLog entity) {
        return Result.error(ResultCode.NO_PERMISSION, "安全防御：系统日志禁止通过 API 手动创建！");
    }

    @Override
    @PostMapping("/saveBatch")
    public Result<?> saveBatch(@RequestBody List<SysLog> list) {
        return Result.error(ResultCode.NO_PERMISSION, "安全防御：系统日志禁止通过 API 手动批量创建！");
    }

    @Override
    @PutMapping("/update")
    public Result<?> update(@RequestBody SysLog entity) {
        return Result.error(ResultCode.NO_PERMISSION, "安全防御：系统操作日志禁止人工篡改与更新！");
    }

    @Override
    @PutMapping("/updateBatch")
    public Result<?> updateBatch(@RequestBody List<SysLog> list) {
        return Result.error(ResultCode.NO_PERMISSION, "安全防御：系统操作日志禁止人工批量篡改与更新！");
    }
}
