CREATE TABLE IF NOT EXISTS `gyt_system_config` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key` VARCHAR(64) NOT NULL COMMENT '配置键',
    `config_value` TEXT COMMENT '配置值',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `version` INT DEFAULT 1 COMMENT '版本号',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sc_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

INSERT INTO `gyt_system_config` (`config_key`, `config_value`, `description`, `version`, `status`) VALUES
('WECHAT_APP_ID', '', '微信 AppID（公众号/小程序）', 1, 1),
('WECHAT_MCH_ID', '', '微信商户号', 1, 1),
('WECHAT_API_KEY', '', '微信支付 API 密钥（v2）', 1, 1),
('WECHAT_API_V3_KEY', '', '微信支付 API v3 密钥', 1, 1),
('WECHAT_NOTIFY_URL', '', '微信支付回调通知地址', 1, 1),
('PAYMENT_CALLBACK_ALLOWED_IPS', '127.0.0.1,::1', '支付回调 IP 白名单，逗号分隔', 1, 1),
('ALIPAY_PUBLIC_KEY', '', '支付宝回调验签公钥', 1, 1)
ON DUPLICATE KEY UPDATE
    description = VALUES(description),
    update_time = CURRENT_TIMESTAMP;
