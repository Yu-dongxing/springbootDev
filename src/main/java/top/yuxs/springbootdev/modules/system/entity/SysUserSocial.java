/*
 * Copyright © 2026 YuDongXing. All rights reserved.
 *
 * @author YuDongXing
 * @since 2026/05/31
 */

package top.yuxs.springbootdev.modules.system.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;
import top.yuxs.springbootdev.core.common.BaseEntity;
import top.yuxs.springbootdev.core.db.annotation.*;
import top.yuxs.springbootdev.core.enums.db.IndexType;

/**
 * 第三方社交账号绑定表 (一本地账号多社交平台)
 *
 * @author YuDongXing
 * @since 2026/05/31
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_social")
@TableComment("用户第三方社交账号绑定表")
@Index(name = "idx_social_uid_source", columns = {"uuid", "source"}, type = IndexType.UNIQUE)
@Index(name = "idx_social_user_id", columns = {"user_id"})
public class SysUserSocial extends BaseEntity {

    /**
     * 本地系统用户ID
     */
    @TableField("user_id")
    @ColumnComment("本地系统用户ID")
    private Long userId;

    /**
     * 第三方平台源 (如 GITHUB/WECHAT/QQ)
     */
    @TableField("source")
    @ColumnComment("第三方平台源 (大写，如 GITHUB, WECHAT, QQ)")
    private String source;

    /**
     * 第三方用户唯一标识 (openid/uuid)
     */
    @TableField("uuid")
    @ColumnComment("第三方平台用户唯一识别码 (如 OpenID/UID)")
    private String uuid;

    /**
     * 社交账号昵称
     */
    @TableField("nickname")
    @ColumnComment("社交账号昵称")
    private String nickname;

    /**
     * 社交账号头像 URL
     */
    @TableField("avatar")
    @ColumnComment("社交账号头像 URL")
    private String avatar;

    /**
     * 第三方返回的原始数据 JSON
     */
    @TableField("raw_info")
    @ColumnType("text")
    @ColumnComment("第三方返回的原始数据 JSON 格式元数据")
    private String rawInfo;
}
