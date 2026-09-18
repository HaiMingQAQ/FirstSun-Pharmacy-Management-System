-- ============================================================
-- F 会员模块：手机号快捷登录验证码表
--
-- 背景：原「手机号快捷登录」只要提交手机号即可登录或自动注册，
--       知道手机号即可冒用会员身份（F-1，P2）。
-- 修复：登录必须提交验证码并由后端校验；验证码与「手机号 + 用途(scene)」绑定，
--       支持过期与一次性消费（重复使用被拒绝）。
--
-- 说明：验证码本身来自环境变量 PHARMACY_DEV_SMS_CODE（仅 local/dev 生效），
--       本脚本不写入、也不包含任何验证码，可重复执行。
-- 执行：docker compose 首次初始化时自动执行；已有库可手工执行本文件。
-- ============================================================

CREATE TABLE IF NOT EXISTS `member_sms_code`
(
    `id`          bigint       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `mobile`      varchar(20)  NOT NULL COMMENT '手机号',
    `scene`       tinyint      NOT NULL COMMENT '使用场景：1=会员手机号快捷登录',
    `code`        varchar(16)  NOT NULL COMMENT '验证码（来源环境变量，禁止在本脚本硬编码）',
    `expire_time` datetime     NOT NULL COMMENT '过期时间',
    `used_time`   datetime     DEFAULT NULL COMMENT '使用时间，非空表示已使用',
    `used_ip`     varchar(64)  DEFAULT NULL COMMENT '使用 IP',
    `creator`     varchar(64)  DEFAULT '' COMMENT '创建者',
    `create_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `updater`     varchar(64)  DEFAULT '' COMMENT '更新者',
    `update_time` datetime     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted`     bit(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
    `tenant_id`   bigint       NOT NULL DEFAULT 0 COMMENT '租户编号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mobile_scene` (`tenant_id`, `mobile`, `scene`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4 COMMENT ='会员手机验证码：手机号+场景唯一，一次性消费';
