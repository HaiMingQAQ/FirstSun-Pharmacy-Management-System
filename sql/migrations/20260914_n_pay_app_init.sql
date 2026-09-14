SET NAMES utf8mb4;

-- E 统一支付初始化数据：支付应用（app）与模拟渠道（mock）。
-- 说明：
--  1) pay_app.order_notify_url / refund_notify_url 为渠道回调地址，本仓库以本地 18080 端口为例；
--     正式部署或团队统一端口后按实际环境调整（通道回调由 pay 模块 /admin-api/pay/notify/* 接收）。
--  2) 演示环境使用 mock 渠道（code='mock'），无需微信/支付宝商户配置即可体验支付全流程；
--     如需真实渠道，由运维按 pay_channel.config 补充商户参数。

INSERT IGNORE INTO `pay_app`
  (`id`, `name`, `status`, `remark`, `order_notify_url`, `refund_notify_url`, `transfer_notify_url`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`, `app_key`)
VALUES
  (1, 'FirstSun 药店', 0, 'FirstSun 药店管理系统统一支付应用（E 维护）',
   'http://localhost:18080/admin-api/pay/notify/order/{channelId}',
   'http://localhost:18080/admin-api/pay/notify/refund/{channelId}',
   'http://localhost:18080/admin-api/pay/notify/transfer/{channelId}',
   '1', NOW(), '1', NOW(), b'0', 163, 'firstsun');

INSERT IGNORE INTO `pay_channel`
  (`id`, `code`, `status`, `remark`, `fee_rate`, `app_id`, `config`,
   `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
  (1, 'mock', 0, '模拟支付渠道（演示/联调用，无真实资金）', 0, 1, '{}',
   '1', NOW(), '1', NOW(), b'0', 163);
