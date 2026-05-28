/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/28
 */

package top.yuxs.springbootdev.core.common;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import top.yuxs.springbootdev.core.enums.ResultCode;

import java.util.List;

/**
 * 神盾通用安全控制器基类
 *
 * @param <T> 实体类型，必须继承自 BaseEntity 以确保具有 Snowflake 主键 ID
 * @param <S> 对应的 Service 类型，必须继承自 MyBatis-Plus IService
 * @author YuDongXing
 * @since 2026/05/28
 */
@Slf4j
public abstract class BaseController<T extends BaseEntity, S extends IService<T>> {

    /**
     * 单次批量操作的最大数据量限制，防止恶意大批量请求导致内存溢出、数据库锁死或长事务阻塞。
     */
    private static final int MAX_BATCH_SIZE = 100;

    /**
     * 利用 Spring 4+ 的泛型注入机制，自动装配子类对应的 Service
     */
    @Autowired(required = false)
    protected S baseService;

    /**
     * 1. 单条新增 (原子事务控制)
     */
    @PostMapping("/save")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> save(@Validated @RequestBody T entity) {
        log.info("Aegis-Base 通用新增，实体数据: {}", entity);
        if (baseService.save(entity)) {
            return Result.success(entity);
        }
        return Result.error(ResultCode.ERROR, "新增保存数据失败");
    }

    /**
     * 2. 防御型批量新增 (原子事务控制)
     */
    @PostMapping("/saveBatch")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> saveBatch(@RequestBody List<T> list) {
        if (list == null || list.isEmpty()) {
            return Result.error(ResultCode.PARAM_IS_BLANK, "批量新增数据不能为空");
        }
        if (list.size() > MAX_BATCH_SIZE) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "单次批量新增数量不能超过 " + MAX_BATCH_SIZE + " 条");
        }
        log.info("Aegis-Base 通用批量新增，数据量: {}", list.size());
        if (baseService.saveBatch(list)) {
            return Result.success("批量新增成功");
        }
        return Result.error(ResultCode.ERROR, "批量新增数据失败");
    }

    /**
     * 3. 单条物理/逻辑删除 (原子事务控制)
     */
    @DeleteMapping("/delete/{id}")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> delete(@PathVariable Long id) {
        if (id == null) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "待删除的主键ID不能为空");
        }
        log.info("Aegis-Base 通用删除，ID: {}", id);
        if (baseService.removeById(id)) {
            return Result.success("删除成功");
        }
        return Result.error(ResultCode.ERROR, "待删除记录不存在或删除失败");
    }

    /**
     * 4. 防御型批量删除 (原子事务控制)
     */
    @DeleteMapping("/deleteBatch")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> deleteBatch(@RequestBody List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return Result.error(ResultCode.PARAM_IS_BLANK, "待删除ID列表不能为空");
        }
        if (ids.size() > MAX_BATCH_SIZE) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "单次批量删除数量不能超过 " + MAX_BATCH_SIZE + " 条");
        }
        log.info("Aegis-Base 通用批量删除，IDs: {}", ids);
        if (baseService.removeByIds(ids)) {
            return Result.success("批量删除成功");
        }
        return Result.error(ResultCode.ERROR, "批量删除失败");
    }

    /**
     * 5. 局部更新 (传入什么非空字段就更新什么字段，原子事务控制)
     * 类型安全：通过 BaseEntity 泛型直接校验主键是否存在，规避低效的反射。
     */
    @PutMapping("/update")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> update(@Validated @RequestBody T entity) {
        log.info("Aegis-Base 通用局部更新，数据: {}", entity);
        if (entity.getId() == null) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "更新操作主键ID不能为空");
        }
        if (baseService.updateById(entity)) {
            return Result.success(entity);
        }
        return Result.error(ResultCode.ERROR, "更新失败，未找到记录或数据无变更");
    }

    /**
     * 5-2. 防御型批量局部更新 (原子事务控制)
     * 限制单次最大批量上限，且其中任何一条更新失败都将整体回滚。
     */
    @PutMapping("/updateBatch")
    @Transactional(rollbackFor = Exception.class)
    public Result<?> updateBatch(@RequestBody List<T> list) {
        if (list == null || list.isEmpty()) {
            return Result.error(ResultCode.PARAM_IS_BLANK, "批量更新数据不能为空");
        }
        if (list.size() > MAX_BATCH_SIZE) {
            return Result.error(ResultCode.PARAM_IS_INVALID, "单次批量更新数量不能超过 " + MAX_BATCH_SIZE + " 条");
        }
        // 防守校验：每条更新数据必须含有主键 ID
        for (T entity : list) {
            if (entity.getId() == null) {
                return Result.error(ResultCode.PARAM_IS_INVALID, "批量更新操作中存在主键ID为空的数据");
            }
        }
        log.info("Aegis-Base 通用批量局部更新，数据量: {}", list.size());
        if (baseService.updateBatchById(list)) {
            return Result.success("批量更新成功");
        }
        return Result.error(ResultCode.ERROR, "批量更新数据失败");
    }

    /**
     * 6. 多条件分页查询
     * @param current 当前页 (默认1)
     * @param size 页大小 (默认10)
     * @param queryEntity 作为查询条件的实体对象 (由 Spring MVC 自动绑定 Query 属性)
     */
    @GetMapping("/page")
    public Result<?> page(@RequestParam(defaultValue = "1") long current,
                          @RequestParam(defaultValue = "10") long size,
                          T queryEntity) {
        log.info("Aegis-Base 通用分页查询，当前页: {}, 大小: {}, 条件实体: {}", current, size, queryEntity);
        Page<T> page = new Page<>(current, size);
        
        // 调用钩子获取查询条件 Wrapper
        QueryWrapper<T> queryWrapper = getQueryWrapper(queryEntity);
        
        IPage<T> resultPage = baseService.page(page, queryWrapper);
        return Result.success(resultPage);
    }

    /**
     * 查询 Wrapper 构造钩子 (由子类重写以支持更复杂的查询条件，如模糊查询、时间段等)
     * 默认实现：将实体中非空字段作为“等值 (EQ)”查询条件
     */
    protected QueryWrapper<T> getQueryWrapper(T queryEntity) {
        return new QueryWrapper<>(queryEntity);
    }
}
