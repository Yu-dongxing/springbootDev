/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import top.yuxs.springbootdev.modules.system.entity.SysApi;
import top.yuxs.springbootdev.modules.system.entity.SysUserRole;
import top.yuxs.springbootdev.modules.system.entity.SysRoleApi;
import top.yuxs.springbootdev.modules.system.mapper.SysApiMapper;
import top.yuxs.springbootdev.modules.system.mapper.SysUserRoleMapper;
import top.yuxs.springbootdev.modules.system.mapper.SysRoleApiMapper;
import top.yuxs.springbootdev.modules.system.service.SysApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 物理 API 接口资源 服务实现类
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Service
@Slf4j
public class SysApiServiceImpl extends ServiceImpl<SysApiMapper, SysApi> implements SysApiService {

    private static final String API_CACHE_KEY_PREFIX = "auth:api:admin:";

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    @Autowired
    private SysUserRoleMapper sysUserRoleMapper;

    @Autowired
    private SysRoleApiMapper sysRoleApiMapper;

    @Override
    public Set<String> getApiPermissionsByUserId(Long userId) {
        if (userId == null) {
            return Set.of();
        }
        String cacheKey = API_CACHE_KEY_PREFIX + userId;
        // 1. 尝试从 Redis 极速读取
        Set<String> apiPerms = redisTemplate.opsForSet().members(cacheKey);
        
        // 2. 缓存未命中，进行单表分步查询，并载入缓存
        if (CollectionUtils.isEmpty(apiPerms)) {
            // Step 1: 根据 userId 查找拥有的角色 ID 集合
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>()
                            .eq(SysUserRole::getUserId, userId)
            );
            if (CollectionUtils.isEmpty(userRoles)) {
                return Set.of();
            }
            List<Long> roleIds = userRoles.stream()
                    .map(SysUserRole::getRoleId)
                    .collect(Collectors.toList());

            // Step 2: 根据角色 ID 集合查找关联的 API ID 集合
            List<SysRoleApi> roleApis = sysRoleApiMapper.selectList(
                    new LambdaQueryWrapper<SysRoleApi>()
                            .in(SysRoleApi::getRoleId, roleIds)
            );
            if (CollectionUtils.isEmpty(roleApis)) {
                return Set.of();
            }
            List<Long> apiIds = roleApis.stream()
                    .map(SysRoleApi::getApiId)
                    .distinct()
                    .collect(Collectors.toList());

            // Step 3: 根据 API ID 集合查找可用的 API 记录并抽取权限标识
            List<SysApi> apis = this.list(
                    new LambdaQueryWrapper<SysApi>()
                            .in(SysApi::getId, apiIds)
                            .eq(SysApi::getStatus, 0)
            );
            if (CollectionUtils.isEmpty(apis)) {
                return Set.of();
            }

            apiPerms = apis.stream()
                    .map(api -> api.getMethod() + ":" + api.getPath())
                    .collect(Collectors.toSet());

            if (!CollectionUtils.isEmpty(apiPerms)) {
                redisTemplate.opsForSet().add(cacheKey, apiPerms.toArray(new String[0]));
                // 缓存设置 2 小时随机过期，防止缓存雪崩
                long timeout = 120 + (long) (Math.random() * 10);
                redisTemplate.expire(cacheKey, timeout, TimeUnit.MINUTES);
            }
        }
        return apiPerms;
    }

    @Override
    public void clearUserApiCache(Long userId) {
        if (userId != null) {
            redisTemplate.delete(API_CACHE_KEY_PREFIX + userId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncApis(List<SysApi> apis) {
        if (CollectionUtils.isEmpty(apis)) {
            return;
        }

        // 1. 查出数据库中现有的所有 API 规则记录
        List<SysApi> existApis = this.list();
        Map<String, SysApi> existMap = existApis.stream()
                .collect(Collectors.toMap(
                        api -> api.getMethod().toUpperCase() + ":" + api.getPath(),
                        api -> api,
                        (k1, k2) -> k1
                ));

        List<SysApi> toInsert = new ArrayList<>();
        List<SysApi> toUpdate = new ArrayList<>();

        for (SysApi api : apis) {
            String key = api.getMethod().toUpperCase() + ":" + api.getPath();
            SysApi existApi = existMap.get(key);
            
            if (existApi == null) {
                // 数据库中没有：则直接增量插入，初始状态启用 (0:正常)
                api.setStatus(0);
                toInsert.add(api);
            } else {
                // 数据库中已有：比对并更新（若数据库中没有友好描述，而扫描到了自定义的接口说明，则更新描述）
                boolean needUpdate = false;
                // 判断是否是自动生成的默认名称（以 .java 方法结尾），若是，且本次扫描出更好的名字，则更新
                if (existApi.getApiName() == null || existApi.getApiName().contains("Controller.") || existApi.getApiName().equals(existApi.getPath())) {
                    if (api.getApiName() != null && !api.getApiName().contains("Controller.")) {
                        existApi.setApiName(api.getApiName());
                        needUpdate = true;
                    }
                }
                if (needUpdate) {
                    toUpdate.add(existApi);
                }
            }
        }

        if (!toInsert.isEmpty()) {
            this.saveBatch(toInsert);
            log.info(">>>>>> [物理 API 同步] 新增物理接口 {} 个", toInsert.size());
        }
        if (!toUpdate.isEmpty()) {
            this.updateBatchById(toUpdate);
            log.info(">>>>>> [物理 API 同步] 丰富或纠正物理接口中文注释 {} 个", toUpdate.size());
        }
    }
}
