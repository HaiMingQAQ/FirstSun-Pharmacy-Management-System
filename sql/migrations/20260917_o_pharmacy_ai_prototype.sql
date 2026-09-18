SET NAMES utf8mb4;

CREATE TABLE IF NOT EXISTS `ai_api_key` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `name` varchar(64) NOT NULL COMMENT '名称',
  `api_key` varchar(512) NOT NULL COMMENT '密钥或环境变量占位符',
  `platform` varchar(32) NOT NULL COMMENT '模型平台',
  `url` varchar(255) NULL COMMENT 'API 基础地址',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0开启 1关闭',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_api_key_platform_status` (`platform`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI API 密钥';

CREATE TABLE IF NOT EXISTS `ai_model` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `key_id` bigint NOT NULL COMMENT 'API 密钥编号',
  `name` varchar(64) NOT NULL COMMENT '模型名称',
  `model` varchar(128) NOT NULL COMMENT '模型标识',
  `platform` varchar(32) NOT NULL COMMENT '模型平台',
  `type` tinyint NOT NULL COMMENT '模型类型：1聊天',
  `sort` int NOT NULL DEFAULT 0 COMMENT '排序',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0开启 1关闭',
  `temperature` double NULL COMMENT '温度参数',
  `max_tokens` int NULL COMMENT '最大输出 Token',
  `max_contexts` int NULL COMMENT '最大上下文消息数',
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  KEY `idx_ai_model_default` (`type`, `status`, `sort`),
  KEY `idx_ai_model_key` (`key_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI 模型';

-- 真实密钥只从后端进程环境变量读取，不落入 Git 或迁移脚本。
INSERT INTO `ai_api_key`
  (`id`, `name`, `api_key`, `platform`, `url`, `status`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (29001, 'FirstSun DeepSeek', '${PHARMACY_AI_API_KEY}', 'DeepSeek',
   'https://api.deepseek.com', 0, 'system', NOW(), 'system', NOW(), b'0')
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `api_key`=VALUES(`api_key`),
  `platform`=VALUES(`platform`), `url`=VALUES(`url`), `status`=VALUES(`status`),
  `updater`='system', `update_time`=NOW(), `deleted`=b'0';

INSERT INTO `ai_model`
  (`id`, `key_id`, `name`, `model`, `platform`, `type`, `sort`, `status`,
   `temperature`, `max_tokens`, `max_contexts`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (29001, 29001, 'DeepSeek Flash（官方）', 'deepseek-flash',
   'DeepSeek', 1, 0, 0, 0.1, 800, 20, 'system', NOW(), 'system', NOW(), b'0')
ON DUPLICATE KEY UPDATE `key_id`=VALUES(`key_id`), `name`=VALUES(`name`),
  `model`=VALUES(`model`), `platform`=VALUES(`platform`), `type`=VALUES(`type`),
  `sort`=VALUES(`sort`), `status`=VALUES(`status`), `temperature`=VALUES(`temperature`),
  `max_tokens`=VALUES(`max_tokens`), `max_contexts`=VALUES(`max_contexts`),
  `updater`='system', `update_time`=NOW(), `deleted`=b'0';

CREATE TABLE IF NOT EXISTS `ph_ai_command` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '命令编号',
  `conversation_id` bigint NULL COMMENT '来源会话',
  `client_message_id` varchar(64) NOT NULL COMMENT '客户端消息幂等标识',
  `user_id` bigint NOT NULL COMMENT '发起用户',
  `employee_id` bigint NULL COMMENT '发起员工',
  `store_id` bigint NULL COMMENT '服务端解析门店',
  `tool_name` varchar(32) NOT NULL COMMENT '白名单写工具',
  `permission` varchar(64) NOT NULL COMMENT '执行权限',
  `request_json` json NOT NULL COMMENT '规范化不可变请求',
  `before_json` json NULL COMMENT '执行前快照',
  `after_json` json NULL COMMENT '预览后快照',
  `request_hash` char(64) NOT NULL COMMENT '命令摘要',
  `token_hash` char(64) NOT NULL COMMENT '一次性凭证摘要',
  `status` varchar(16) NOT NULL COMMENT 'PENDING/EXECUTING/EXECUTED/CANCELLED/FAILED',
  `expires_at` datetime NOT NULL COMMENT '过期时间',
  `executed_at` datetime NULL COMMENT '执行时间',
  `result_json` json NULL COMMENT '脱敏结果',
  `error_message` varchar(500) NULL COMMENT '脱敏错误',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  `creator` varchar(64) NOT NULL DEFAULT '',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) NOT NULL DEFAULT '',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_ph_ai_command_client` (`tenant_id`, `user_id`, `client_message_id`, `tool_name`, `deleted`),
  KEY `idx_ph_ai_command_pending` (`tenant_id`, `user_id`, `status`, `expires_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='药店 AI 待确认命令';

INSERT INTO `system_menu`
  (`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`, `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
  (22300, 'AI 助手使用', 'pharmacy:ai:chat', 3, 90, 22000, '', 'ep:chat-dot-round', '', '', 0, b'0', b'0', b'0', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE `name`=VALUES(`name`), `permission`=VALUES(`permission`), `type`=VALUES(`type`),
  `path`='', `component`='', `component_name`='', `visible`=b'0', `update_time`=NOW(), `deleted`=b'0';

-- 系统超管与 FirstSun 共享演示账号所属角色均授权 AI 入口；业务工具仍逐项检查原有权限。
INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 1, 22300, 1, '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_role_menu` WHERE `role_id`=1 AND `menu_id`=22300 AND `tenant_id`=1 AND `deleted`=b'0');

INSERT INTO `system_role_menu` (`role_id`, `menu_id`, `tenant_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 167, 22300, 163, 'system', NOW(), 'system', NOW(), b'0'
WHERE NOT EXISTS (SELECT 1 FROM `system_role_menu` WHERE `role_id`=167 AND `menu_id`=22300 AND `tenant_id`=163 AND `deleted`=b'0');

-- 刷新 FirstSun 租户使用的药店套餐菜单范围，确保能力权限进入租户可用权限集合。
UPDATE `system_tenant_package`
SET `menu_ids` = (
    SELECT CAST(JSON_ARRAYAGG(`id`) AS CHAR)
    FROM `system_menu`
    WHERE `deleted` = b'0' AND (`id` BETWEEN 22000 AND 22999 OR `id` BETWEEN 61000 AND 61999)
  ),
  `updater` = 'system',
  `update_time` = NOW()
WHERE `id` = 114;
