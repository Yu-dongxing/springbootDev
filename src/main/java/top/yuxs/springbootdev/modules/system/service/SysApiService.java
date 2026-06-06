/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.yuxs.springbootdev.modules.system.entity.SysApi;

import java.util.List;
import java.util.Set;

/**
 * 物理 API 接口资源 服务类
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
public interface SysApiService extends IService<SysApi> {

    /**
     * 获取用户拥有的可用 API 权限规则集 (内置 Redis 二级高速缓存，格式: METHOD:PATH)
     */
    Set<String> getApiPermissionsByUserId(Long userId);

    /**
     * 清理指定用户的 API 权限缓存 (角色权限/绑定变更时调用)
     */
    void clearUserApiCache(Long userId);

    /**
     * 批量同步物理 API (自动扫描器启动时执行增量幂等更新)
     */
     void syncApis(List<SysApi> apis);

    /**
     * 检查系统核心管理员数据与角色自愈初始化
     */
    void checkAndInitDefaultAdminData();
}
