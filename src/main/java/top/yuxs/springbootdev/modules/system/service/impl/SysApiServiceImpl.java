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
import cn.hutool.crypto.digest.BCrypt;
import top.yuxs.springbootdev.core.config.AegisSecurityProperties;
import top.yuxs.springbootdev.modules.system.entity.SysApi;
import top.yuxs.springbootdev.modules.system.entity.SysUser;
import top.yuxs.springbootdev.modules.system.entity.SysRole;
import top.yuxs.springbootdev.modules.system.entity.SysUserRole;
import top.yuxs.springbootdev.modules.system.entity.SysRoleApi;
import top.yuxs.springbootdev.modules.system.mapper.SysApiMapper;
import top.yuxs.springbootdev.modules.system.mapper.SysUserMapper;
import top.yuxs.springbootdev.modules.system.mapper.SysRoleMapper;
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

    @Autowired
    private SysRoleMapper sysRoleMapper;

    @Autowired
    private SysUserMapper sysUserMapper;

    @Autowired
    private AegisSecurityProperties securityProperties;

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

            // 过滤掉被禁用的角色（status != 0 的被过滤，只保留 status == 0 的正常角色）
            List<SysRole> activeRoles = sysRoleMapper.selectList(
                    new LambdaQueryWrapper<SysRole>()
                            .in(SysRole::getId, roleIds)
                            .eq(SysRole::getStatus, 0)
            );
            if (CollectionUtils.isEmpty(activeRoles)) {
                return Set.of();
            }
            List<Long> activeRoleIds = activeRoles.stream()
                    .map(SysRole::getId)
                    .collect(Collectors.toList());

            // Step 2: 根据活跃角色 ID 集合查找关联的 API ID 集合
            List<SysRoleApi> roleApis = sysRoleApiMapper.selectList(
                    new LambdaQueryWrapper<SysRoleApi>()
                            .in(SysRoleApi::getRoleId, activeRoleIds)
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
        List<SysApi> toDelete = new ArrayList<>();

        // 收集此次扫描中依然存在的 API key
        Set<String> scannedKeys = apis.stream()
                .map(api -> api.getMethod().toUpperCase() + ":" + api.getPath())
                .collect(Collectors.toSet());

        // 2. 匹配计算出需要物理删除的废弃 API (数据库中有，但最新代码扫描里已经没有的)
        for (SysApi existApi : existApis) {
            String key = existApi.getMethod().toUpperCase() + ":" + existApi.getPath();
            if (!scannedKeys.contains(key)) {
                toDelete.add(existApi);
            }
        }

        // 3. 计算新增和更新列表
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

        // 4. 执行落库物理写入
        if (!toInsert.isEmpty()) {
            this.saveBatch(toInsert);
            log.info(">>>>>> [物理 API 同步] 新增物理接口 {} 个", toInsert.size());

            // 默认为 role_key = 'admin' 的系统管理员角色自动赋予新增接口
            List<SysRole> adminRoles = sysRoleMapper.selectList(
                    new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, "admin")
            );
            if (!CollectionUtils.isEmpty(adminRoles)) {
                for (SysRole adminRole : adminRoles) {
                    for (SysApi newApi : toInsert) {
                        SysRoleApi sra = new SysRoleApi();
                        sra.setRoleId(adminRole.getId());
                        sra.setApiId(newApi.getId());
                        sysRoleApiMapper.insert(sra);
                    }
                }
                log.info(">>>>>> [物理 API 同步] 已自动为 {} 个系统管理员(admin)角色赋予新增物理接口权限", adminRoles.size());
            }
        }

        if (!toUpdate.isEmpty()) {
            this.updateBatchById(toUpdate);
            log.info(">>>>>> [物理 API 同步] 丰富或纠正物理接口中文注释 {} 个", toUpdate.size());
        }

        if (!toDelete.isEmpty()) {
            List<Long> deleteIds = toDelete.stream().map(SysApi::getId).collect(Collectors.toList());
            this.removeByIds(deleteIds);
            // 物理级联清理 sys_role_api 里的绑定记录
            sysRoleApiMapper.delete(new LambdaQueryWrapper<SysRoleApi>().in(SysRoleApi::getApiId, deleteIds));
            log.info(">>>>>> [物理 API 同步] 物理删除废弃物理接口 {} 个，并同步清理角色 API 关联", deleteIds.size());
        }

        // 5. 极速更新受影响用户的 Redis 鉴权缓存 (具有 admin 或 super_admin 角色的用户)
        List<SysRole> adminRoles = sysRoleMapper.selectList(
                new LambdaQueryWrapper<SysRole>().in(SysRole::getRoleKey, List.of("admin", "super_admin"))
        );
        if (!CollectionUtils.isEmpty(adminRoles)) {
            List<Long> adminRoleIds = adminRoles.stream().map(SysRole::getId).collect(Collectors.toList());
            List<SysUserRole> userRoles = sysUserRoleMapper.selectList(
                    new LambdaQueryWrapper<SysUserRole>().in(SysUserRole::getRoleId, adminRoleIds)
            );
            if (!CollectionUtils.isEmpty(userRoles)) {
                Set<Long> userIds = userRoles.stream().map(SysUserRole::getUserId).collect(Collectors.toSet());
                for (Long userId : userIds) {
                    this.clearUserApiCache(userId);
                }
                log.info(">>>>>> [物理 API 同步] 清空 {} 个受影响管理员用户的 Redis 权限缓存", userIds.size());
            }
        }

        // 6. 执行数据自愈初始化 (若数据库中角色或初始超级管理员账号为空，自动创建并赋权)
        this.checkAndInitDefaultAdminData();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void checkAndInitDefaultAdminData() {
        log.info(">>>>>> 检查系统核心管理员数据与角色自愈初始化...");
        
        // 1. 确保在 sys_role 中存在 super_admin 和 admin 角色
        SysRole superAdminRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, "super_admin")
        );
        if (superAdminRole == null) {
            superAdminRole = new SysRole();
            superAdminRole.setRoleName("超级管理员");
            superAdminRole.setRoleKey("super_admin");
            superAdminRole.setStatus(0);
            sysRoleMapper.insert(superAdminRole);
            log.info(">>>>>> [数据初始化自愈] 创建超级管理员角色 (super_admin)");
        }

        SysRole adminRole = sysRoleMapper.selectOne(
                new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, "admin")
        );
        if (adminRole == null) {
            adminRole = new SysRole();
            adminRole.setRoleName("系统管理员");
            adminRole.setRoleKey("admin");
            adminRole.setStatus(0);
            sysRoleMapper.insert(adminRole);
            log.info(">>>>>> [数据初始化自愈] 创建系统管理员角色 (admin)");
        }

        // 2. 检查系统中是否有用户名是 admin 的管理员账号
        SysUser defaultAdmin = sysUserMapper.selectOne(
                new LambdaQueryWrapper<SysUser>().eq(SysUser::getUsername, "admin")
        );
        
        if (defaultAdmin == null) {
            // 如果不存在，自动创建一个超级管理员，账号为 "admin"，默认密码为 "123456"
            defaultAdmin = new SysUser();
            defaultAdmin.setUsername("admin");
            
            String plainPassword = "123456";
            String encryptedPassword;
            if (securityProperties.isPasswordEncryptEnabled()) {
                encryptedPassword = BCrypt.hashpw(plainPassword, BCrypt.gensalt());
            } else {
                encryptedPassword = plainPassword;
            }
            defaultAdmin.setPassword(encryptedPassword);
            defaultAdmin.setUserType("ADMIN");
            defaultAdmin.setStatus(0);
            
            sysUserMapper.insert(defaultAdmin);
            log.info(">>>>>> [数据初始化自愈] 创建默认超级管理员用户 (admin / 123456)");
            
            // 绑定超级管理员角色 (super_admin) 给默认管理员用户
            SysUserRole userRoleRelation = new SysUserRole();
            userRoleRelation.setUserId(defaultAdmin.getId());
            userRoleRelation.setRoleId(superAdminRole.getId());
            sysUserRoleMapper.insert(userRoleRelation);
            log.info(">>>>>> [数据初始化自愈] 绑定超级管理员用户与 super_admin 角色");
        }

        // 3. 全自动默认赋权：将当前所有的物理 API，全量赋予 `role_key = 'admin'` 的角色
        // 只有当 sys_role_api 表中缺少对应关系时，我们再插入（做去重防守）
        List<SysApi> allApis = this.list();
        if (!CollectionUtils.isEmpty(allApis)) {
            // 找到所有 roleKey = 'admin' 的角色的 ID
            List<SysRole> allAdminRoles = sysRoleMapper.selectList(
                    new LambdaQueryWrapper<SysRole>().eq(SysRole::getRoleKey, "admin")
            );
            if (!CollectionUtils.isEmpty(allAdminRoles)) {
                for (SysRole r : allAdminRoles) {
                    // 查询该角色已绑定的 API
                    List<SysRoleApi> existingBindings = sysRoleApiMapper.selectList(
                            new LambdaQueryWrapper<SysRoleApi>().eq(SysRoleApi::getRoleId, r.getId())
                    );
                    Set<Long> boundApiIds = existingBindings.stream()
                            .map(SysRoleApi::getApiId)
                            .collect(Collectors.toSet());
                    
                    for (SysApi api : allApis) {
                        if (!boundApiIds.contains(api.getId())) {
                            SysRoleApi sra = new SysRoleApi();
                            sra.setRoleId(r.getId());
                            sra.setApiId(api.getId());
                            sysRoleApiMapper.insert(sra);
                        }
                    }
                }
                log.info(">>>>>> [数据初始化自愈] 已自动确保所有系统管理员 (admin) 拥有全量物理接口权限");
            }
        }
    }
}
