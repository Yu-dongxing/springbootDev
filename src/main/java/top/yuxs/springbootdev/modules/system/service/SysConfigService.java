/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/06/01
 */

package top.yuxs.springbootdev.modules.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import top.yuxs.springbootdev.modules.system.entity.SysConfig;

/**
 * 系统通用参数配置 服务类
 *
 * @author YuDongXing
 * @since 2026/06/01
 */
public interface SysConfigService extends IService<SysConfig> {

    /**
     * 根据键名获取配置值
     *
     * @param configKey 键名
     * @return 配置值，若未配置或禁用则返回 null
     */
    String getValue(String configKey);

    /**
     * 根据键名获取配置值，支持默认值 fallback
     *
     * @param configKey 键名
     * @param defaultValue 默认值
     * @return 配置值
     */
    String getValue(String configKey, String defaultValue);

    /**
     * 更新或插入配置
     *
     * @param configKey 键名
     * @param configValue 键值
     * @param configName 名称
     * @param remark 备注
     */
    void updateConfig(String configKey, String configValue, String configName, String remark);
}
