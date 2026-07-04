CREATE TABLE IF NOT EXISTS `gyt_support_message` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '会话归属会员ID',
    `sender_user_id` BIGINT NOT NULL COMMENT '发送人ID',
    `sender_type` VARCHAR(16) NOT NULL COMMENT 'USER/STAFF',
    `message_type` VARCHAR(16) NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT',
    `content` TEXT NOT NULL COMMENT '文字内容',
    `is_read` TINYINT DEFAULT 0 COMMENT '是否已读',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_support_user_time` (`user_id`, `create_time`),
    INDEX `idx_support_sender_time` (`sender_user_id`, `create_time`),
    INDEX `idx_support_deleted_time` (`deleted`, `create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服消息表';
