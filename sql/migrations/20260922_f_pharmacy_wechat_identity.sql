-- ============================================================
-- F 会员模块：药店微信小程序身份绑定
--
-- 仅保存已由微信实时校验得到的身份标识，不保存 code、session_key、token
-- 或微信原始响应。tenant_id 由框架租户字段维护，业务唯一键防止并发重复绑定。
-- 本文件为幂等结构迁移；已有数据库升级时请先备份，再单独执行本文件。
-- ============================================================

CREATE TABLE IF NOT EXISTS `ph_member_wechat_identity`
(
    `id`          bigint        NOT NULL AUTO_INCREMENT COMMENT '主键',
    `app_id`      varchar(64)   NOT NULL COMMENT '可信微信小程序 AppID',
    `openid`      varchar(128)  NOT NULL COMMENT '微信小程序 openid',
    `member_id`   bigint        NOT NULL COMMENT '药店会员编号',
    `creator`     varchar(64)   DEFAULT '' COMMENT '创建者',
    `create_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)   DEFAULT '' COMMENT '更新者',
    `update_time` datetime      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`   bigint        NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ph_member_wechat_identity` (`tenant_id`, `app_id`, `openid`),
    KEY `idx_ph_member_wechat_member` (`tenant_id`, `member_id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '药店会员微信小程序身份绑定';
