/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import top.yuxs.springbootdev.modules.system.entity.SysRole;
import top.yuxs.springbootdev.modules.system.mapper.SysRoleMapper;
import top.yuxs.springbootdev.modules.system.service.SysRoleService;

import java.util.List;

/**
 * 系统角色 服务实现类
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Service
public class SysRoleServiceImpl extends ServiceImpl<SysRoleMapper, SysRole> implements SysRoleService {

    @Override
    public boolean isSuperAdmin(Long userId) {
        if (userId == null) {
            return false;
        }
        List<String> roleKeys = this.getRoleKeysByUserId(userId);
        return !CollectionUtils.isEmpty(roleKeys) && roleKeys.contains("super_admin");
    }

    @Override
    public List<String> getRoleKeysByUserId(Long userId) {
        return this.baseMapper.selectRoleKeysByUserId(userId);
    }
}
