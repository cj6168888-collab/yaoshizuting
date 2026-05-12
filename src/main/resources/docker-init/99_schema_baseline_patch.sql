USE yaoshizuting;

ALTER TABLE `gyt_user`
    ADD COLUMN `version` INT DEFAULT 0 COMMENT 'optimistic lock version';

CREATE INDEX `idx_order_user_status`
    ON `gyt_order` (`user_id`, `status`);

CREATE INDEX `idx_profit_receiver_time`
    ON `gyt_profit_log` (`receiver_id`, `create_time`);

CREATE INDEX `idx_withdrawal_user_status`
    ON `gyt_withdrawal` (`user_id`, `status`);

CREATE INDEX `idx_user_role_status_time`
    ON `gyt_user` (`deleted`, `role`, `status`, `create_time`);

CREATE INDEX `idx_product_type_status_time`
    ON `gyt_product` (`deleted`, `product_type`, `status`, `update_time`);

CREATE INDEX `idx_withdrawal_status_time`
    ON `gyt_withdrawal` (`deleted`, `status`, `create_time`);

CREATE INDEX `idx_profit_type_time`
    ON `gyt_profit_log` (`deleted`, `type`, `create_time`);

CREATE TABLE IF NOT EXISTS `gyt_audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT 'primary key',
    `module` VARCHAR(50) DEFAULT NULL COMMENT 'module',
    `operation` VARCHAR(100) DEFAULT NULL COMMENT 'operation',
    `operator_id` BIGINT DEFAULT NULL COMMENT 'operator id',
    `operator_name` VARCHAR(50) DEFAULT NULL COMMENT 'operator name',
    `request_method` VARCHAR(10) DEFAULT NULL COMMENT 'request method',
    `request_url` VARCHAR(255) DEFAULT NULL COMMENT 'request url',
    `request_params` TEXT COMMENT 'request params',
    `response_data` TEXT COMMENT 'response data',
    `response_status` INT DEFAULT NULL COMMENT 'response status',
    `execution_time` BIGINT DEFAULT NULL COMMENT 'execution time ms',
    `client_ip` VARCHAR(50) DEFAULT NULL COMMENT 'client ip',
    `user_agent` VARCHAR(500) DEFAULT NULL COMMENT 'user agent',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT 'create time',
    `deleted` TINYINT DEFAULT 0 COMMENT 'logical delete',
    PRIMARY KEY (`id`),
    INDEX `idx_audit_log_operator` (`operator_id`, `create_time`),
    INDEX `idx_audit_log_module` (`module`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='audit log table';
