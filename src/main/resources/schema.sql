-- 药师祖庭数字化系统数据库初始化脚本
-- 版本: 1.0
-- 数据库: yaoshizuting

CREATE DATABASE IF NOT EXISTS yaoshizuting DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE yaoshizuting;

-- 1. 用户基础表
DROP TABLE IF EXISTS `gyt_user`;
CREATE TABLE `gyt_user` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `username` VARCHAR(50) DEFAULT NULL COMMENT '用户名',
    `password` VARCHAR(100) NOT NULL COMMENT '密码',
    `mobile` VARCHAR(20) NOT NULL COMMENT '手机号',
    `nickname` VARCHAR(50) DEFAULT NULL COMMENT '昵称',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像',
    `role` TINYINT NOT NULL DEFAULT 1 COMMENT '角色: 1-店主 2-代理 3-合伙人',
    `parent_id` BIGINT DEFAULT 0 COMMENT '推荐人ID',
    `tree_path` VARCHAR(1000) DEFAULT NULL COMMENT '族谱路径: /0/1/10/',
    `agent_count` INT DEFAULT 0 COMMENT '已招募代理数量',
    `store_count` INT DEFAULT 0 COMMENT '已招募店铺数量',
    `balance` DECIMAL(12,2) DEFAULT 0.00 COMMENT '账户余额',
    `total_earnings` DECIMAL(12,2) DEFAULT 0.00 COMMENT '累计收益',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-冻结 1-正常',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除: 0-否 1-是',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_mobile` (`mobile`),
    INDEX `idx_parent_id` (`parent_id`),
    INDEX `idx_tree_path` (`tree_path`(255)),
    INDEX `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 2. 动态政策配置表
DROP TABLE IF EXISTS `gyt_config_policy`;
CREATE TABLE `gyt_config_policy` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `config_key` VARCHAR(64) NOT NULL COMMENT '配置键',
    `config_value` DECIMAL(12,2) NOT NULL COMMENT '配置值',
    `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
    `version` INT DEFAULT 1 COMMENT '版本号',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-禁用 1-启用',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_config_key` (`config_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='政策配置表';

-- 3. 订单表
DROP TABLE IF EXISTS `gyt_order`;
CREATE TABLE `gyt_order` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_sn` VARCHAR(64) NOT NULL COMMENT '订单编号',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `order_type` TINYINT NOT NULL COMMENT '订单类型: 1-店铺加盟 2-代理加盟 3-合伙人加盟 4-产品补货 5-云仓代发',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '订单金额',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待支付 1-已支付 2-处理中 3-已完成 4-已取消 5-已退款',
    `pay_method` VARCHAR(20) DEFAULT NULL COMMENT '支付方式',
    `pay_time` DATETIME DEFAULT NULL COMMENT '支付时间',
    `transaction_id` VARCHAR(64) DEFAULT NULL COMMENT '微信交易号',
    `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_sn` (`order_sn`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单表';

-- 4. 订单明细表
DROP TABLE IF EXISTS `gyt_order_item`;
CREATE TABLE `gyt_order_item` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_id` BIGINT NOT NULL COMMENT '订单ID',
    `product_id` BIGINT NOT NULL COMMENT '商品ID',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `product_image` VARCHAR(255) DEFAULT NULL COMMENT '商品图片',
    `quantity` INT NOT NULL COMMENT '数量',
    `price` DECIMAL(12,2) NOT NULL COMMENT '单价',
    `total_price` DECIMAL(12,2) NOT NULL COMMENT '总价',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    INDEX `idx_order_id` (`order_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='订单明细表';

-- 5. 分润流水表
DROP TABLE IF EXISTS `gyt_profit_log`;
CREATE TABLE `gyt_profit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `order_sn` VARCHAR(64) NOT NULL COMMENT '来源订单号',
    `receiver_id` BIGINT NOT NULL COMMENT '收益人',
    `contributor_id` BIGINT DEFAULT NULL COMMENT '贡献人',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '金额',
    `type` VARCHAR(32) NOT NULL COMMENT '类型: DIRECT_STORE-直推店铺 INDIRECT_STORE-间推店铺 AGENT_MANAGE-代理商管理培训 PARTNER_DIRECT-直推合伙人 TEAM_MANAGEMENT-团队管理津贴 HEADQUARTER_SUPPORT_FEE-总部培训支持费',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-已冲抵 1-已入账',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_order_type_receiver` (`order_sn`, `type`, `receiver_id`),
    INDEX `idx_receiver_id` (`receiver_id`),
    INDEX `idx_contributor_id` (`contributor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='分润流水表';

-- 6. 产品表
DROP TABLE IF EXISTS `gyt_product`;
CREATE TABLE `gyt_product` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称',
    `product_code` VARCHAR(50) DEFAULT NULL COMMENT '商品编码',
    `product_type` TINYINT DEFAULT 1 COMMENT '类型: 1-仪器 2-套盒 3-单品',
    `market_price` DECIMAL(12,2) NOT NULL COMMENT '市场价',
    `join_price` DECIMAL(12,2) NOT NULL COMMENT '加盟价',
    `agent_price` DECIMAL(12,2) DEFAULT NULL COMMENT '代理价',
    `partner_price` DECIMAL(12,2) DEFAULT NULL COMMENT '合伙人价',
    `stock` INT DEFAULT 0 COMMENT '库存',
    `unit` VARCHAR(20) DEFAULT '套' COMMENT '单位',
    `image` VARCHAR(255) DEFAULT NULL COMMENT '图片',
    `description` TEXT COMMENT '描述',
    `status` TINYINT DEFAULT 1 COMMENT '状态: 0-下架 1-上架',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_product_code` (`product_code`),
    INDEX `idx_product_type` (`product_type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='产品表';

-- 7. 提现记录表
DROP TABLE IF EXISTS `gyt_withdrawal`;
CREATE TABLE `gyt_withdrawal` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `withdraw_sn` VARCHAR(64) NOT NULL COMMENT '提现单号',
    `amount` DECIMAL(12,2) NOT NULL COMMENT '提现金额',
    `fee` DECIMAL(12,2) DEFAULT 0.00 COMMENT '手续费',
    `actual_amount` DECIMAL(12,2) NOT NULL COMMENT '实际到账',
    `withdraw_type` TINYINT NOT NULL COMMENT '类型: 1-微信零钱 2-银行卡',
    `account_no` VARCHAR(50) DEFAULT NULL COMMENT '账号',
    `account_name` VARCHAR(50) DEFAULT NULL COMMENT '户名',
    `bank_name` VARCHAR(50) DEFAULT NULL COMMENT '银行',
    `status` TINYINT DEFAULT 0 COMMENT '状态: 0-待审核 1-审核通过 2-已打款 3-已拒绝',
    `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
    `audit_time` DATETIME DEFAULT NULL COMMENT '审核时间',
    `complete_time` DATETIME DEFAULT NULL COMMENT '完成时间',
    `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_withdraw_sn` (`withdraw_sn`),
    INDEX `idx_user_id` (`user_id`),
    INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='提现记录表';

-- 8. 初始化默认政策配置
INSERT INTO `gyt_config_policy` (`config_key`, `config_value`, `description`, `version`, `status`) VALUES
('STORE_JOIN_FEE', 13960.00, '店铺加盟费', 1, 1),
('AGENT_JOIN_FEE', 39800.00, '代理加盟费', 1, 1),
('PARTNER_JOIN_FEE', 99800.00, '合伙人加盟费', 1, 1),
('STORE_REWARD_DIRECT', 9000.00, '店主直推店铺奖励', 1, 1),
('AGENT_REWARD_DIRECT', 9000.00, '代理直推店铺奖励', 1, 1),
('PARTNER_REWARD_DIRECT', 9980.00, '合伙人直推店铺奖励', 1, 1),
('REWARD_INDIRECT', 6000.00, '间推店铺奖励', 1, 1),
('AGENT_REWARD_DIRECT_AGENT', 16000.00, '直推代理奖励', 1, 1),
('PARTNER_REWARD_DIRECT_PARTNER', 40000.00, '合伙人直推合伙人奖励', 1, 1),
('PARTNER_REWARD_DIRECT_AGENT', 16000.00, '合伙人直推代理奖励', 1, 1),
('PARTNER_TEAM_MANAGEMENT', 998.00, '团队管理津贴/家', 1, 1),
('PARTNER_MANAGE_FEE', 39800.00, '代理商管理培训费', 1, 1),
('HEADQUARTER_SUPPORT_FEE', 9800.00, '总部培训支持费', 1, 1),
('PRODUCT_DISCOUNT', 0.15, '产品进货折扣', 1, 1),
('CLOUD_WAREHOUSE_FEE', 39.80, '云仓代发服务费/套', 1, 1),
('WITHDRAWAL_FEE_RATE', 0.005, '提现手续费率', 1, 1),
('WITHDRAWAL_MIN_AMOUNT', 100.00, '最低提现金额', 1, 1);

-- 9. 初始化基础货品
INSERT INTO `gyt_product` (`product_name`, `product_code`, `product_type`, `market_price`, `join_price`, `agent_price`, `partner_price`, `stock`, `unit`, `description`, `status`) VALUES
('药师祖庭调理仪', 'YST-DEVICE-001', 1, 3980.00, 2980.00, 2680.00, 2380.00, 50, '台', '门店基础调理设备', 1),
('药师祖庭养护套盒', 'YST-KIT-001', 2, 1396.00, 998.00, 898.00, 798.00, 200, '套', '会员养护服务套盒', 1),
('药师祖庭草本单品', 'YST-SKU-001', 3, 398.00, 298.00, 268.00, 238.00, 500, '盒', '日常复购草本单品', 1);

-- 10. 初始化超级管理员 (密码: admin123)
INSERT INTO `gyt_user` (`username`, `password`, `mobile`, `nickname`, `role`, `status`) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', '13800000000', '系统管理员', 3, 1);
