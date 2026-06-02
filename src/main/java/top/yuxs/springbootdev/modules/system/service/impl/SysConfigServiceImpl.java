/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/01
 */

package top.yuxs.springbootdev.modules.system.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import top.yuxs.springbootdev.modules.system.entity.SysConfig;
import top.yuxs.springbootdev.modules.system.mapper.SysConfigMapper;
import top.yuxs.springbootdev.modules.system.service.SysConfigService;

/**
 * 系统通用参数配置 服务实现类
 *
 * @author YuDongXing
 * @since 2026/06/01
 */
@Service
public class SysConfigServiceImpl extends ServiceImpl<SysConfigMapper, SysConfig> implements SysConfigService {

    @Override
    public String getValue(String configKey) {
        if (!StringUtils.hasText(configKey)) {
            return null;
        }
        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey)
                .eq(SysConfig::getStatus, 0); // 必须是正常启用状态
        SysConfig config = this.getOne(queryWrapper);
        return config != null ? config.getConfigValue() : null;
    }

    @Override
    public String getValue(String configKey, String defaultValue) {
        String val = getValue(configKey);
        return val != null ? val : defaultValue;
    }

    @Override
    public void updateConfig(String configKey, String configValue, String configName, String remark) {
        if (!StringUtils.hasText(configKey)) {
            return;
        }
        LambdaQueryWrapper<SysConfig> queryWrapper = new LambdaQueryWrapper<SysConfig>()
                .eq(SysConfig::getConfigKey, configKey);
        SysConfig exist = this.getOne(queryWrapper);
        if (exist != null) {
            exist.setConfigValue(configValue);
            if (StringUtils.hasText(configName)) {
                exist.setConfigName(configName);
            }
            if (StringUtils.hasText(remark)) {
                exist.setRemark(remark);
            }
            this.updateById(exist);
        } else {
            SysConfig config = new SysConfig();
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigName(configName);
            config.setRemark(remark);
            config.setStatus(0); // 默认启用状态
            this.save(config);
        }
    }
}
