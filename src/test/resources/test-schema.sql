DROP TABLE IF EXISTS gyt_audit_log;
DROP TABLE IF EXISTS gyt_user_hierarchy;
DROP TABLE IF EXISTS gyt_withdrawal;
DROP TABLE IF EXISTS gyt_profit_log;
DROP TABLE IF EXISTS gyt_order_item;
DROP TABLE IF EXISTS gyt_order;
DROP TABLE IF EXISTS gyt_product;
DROP TABLE IF EXISTS gyt_config_policy;
DROP TABLE IF EXISTS gyt_user;

CREATE TABLE gyt_user (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(50) DEFAULT NULL,
    password VARCHAR(100) DEFAULT NULL,
    mobile VARCHAR(20) NOT NULL,
    nickname VARCHAR(50) DEFAULT NULL,
    avatar VARCHAR(255) DEFAULT NULL,
    role TINYINT NOT NULL DEFAULT 1,
    parent_id BIGINT DEFAULT 0,
    tree_path VARCHAR(1000) DEFAULT NULL,
    agent_count INT DEFAULT 0,
    store_count INT DEFAULT 0,
    balance DECIMAL(12,2) DEFAULT 0.00,
    total_earnings DECIMAL(12,2) DEFAULT 0.00,
    status TINYINT DEFAULT 1,
    version INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_mobile (mobile),
    KEY idx_user_parent_id (parent_id),
    KEY idx_user_tree_path (tree_path),
    KEY idx_user_role (role)
);

CREATE TABLE gyt_config_policy (
    id BIGINT NOT NULL AUTO_INCREMENT,
    config_key VARCHAR(64) NOT NULL,
    config_value DECIMAL(12,2) NOT NULL,
    description VARCHAR(255) DEFAULT NULL,
    version INT DEFAULT 1,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_config_key (config_key)
);

CREATE TABLE gyt_order (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_sn VARCHAR(64) NOT NULL,
    user_id BIGINT NOT NULL,
    order_type TINYINT NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    status TINYINT DEFAULT 0,
    pay_method VARCHAR(20) DEFAULT NULL,
    pay_time VARCHAR(32) DEFAULT NULL,
    transaction_id VARCHAR(64) DEFAULT NULL,
    remark VARCHAR(500) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_sn (order_sn),
    KEY idx_order_user_id (user_id),
    KEY idx_order_status (status),
    KEY idx_order_user_status (user_id, status)
);

CREATE TABLE gyt_order_item (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    product_id BIGINT NOT NULL,
    product_name VARCHAR(100) NOT NULL,
    product_image VARCHAR(255) DEFAULT NULL,
    quantity INT NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    total_price DECIMAL(12,2) NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_order_item_order_id (order_id)
);

CREATE TABLE gyt_profit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    order_sn VARCHAR(64) NOT NULL,
    receiver_id BIGINT NOT NULL,
    contributor_id BIGINT DEFAULT NULL,
    amount DECIMAL(12,2) NOT NULL,
    type VARCHAR(32) NOT NULL,
    status TINYINT DEFAULT 1,
    remark VARCHAR(255) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_order_type_receiver (order_sn, type, receiver_id),
    KEY idx_profit_receiver_id (receiver_id),
    KEY idx_profit_contributor_id (contributor_id),
    KEY idx_profit_receiver_time (receiver_id, create_time)
);

CREATE TABLE gyt_product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_name VARCHAR(100) NOT NULL,
    product_code VARCHAR(50) DEFAULT NULL,
    product_type TINYINT DEFAULT 1,
    market_price DECIMAL(12,2) NOT NULL,
    join_price DECIMAL(12,2) NOT NULL,
    agent_price DECIMAL(12,2) DEFAULT NULL,
    partner_price DECIMAL(12,2) DEFAULT NULL,
    stock INT DEFAULT 0,
    unit VARCHAR(20) DEFAULT NULL,
    image VARCHAR(255) DEFAULT NULL,
    description TEXT,
    status TINYINT DEFAULT 1,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_product_code (product_code),
    KEY idx_product_product_type (product_type)
);

CREATE TABLE gyt_withdrawal (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    withdraw_sn VARCHAR(64) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    fee DECIMAL(12,2) DEFAULT 0.00,
    actual_amount DECIMAL(12,2) NOT NULL,
    withdraw_type TINYINT NOT NULL,
    account_no VARCHAR(50) DEFAULT NULL,
    account_name VARCHAR(50) DEFAULT NULL,
    bank_name VARCHAR(50) DEFAULT NULL,
    status TINYINT DEFAULT 0,
    remark VARCHAR(255) DEFAULT NULL,
    audit_time VARCHAR(32) DEFAULT NULL,
    complete_time VARCHAR(32) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_withdraw_sn (withdraw_sn),
    KEY idx_withdrawal_user_id (user_id),
    KEY idx_withdrawal_status (status),
    KEY idx_withdrawal_user_status (user_id, status)
);

CREATE TABLE gyt_audit_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    module VARCHAR(50) DEFAULT NULL,
    operation VARCHAR(100) DEFAULT NULL,
    operator_id BIGINT DEFAULT NULL,
    operator_name VARCHAR(50) DEFAULT NULL,
    request_method VARCHAR(10) DEFAULT NULL,
    request_url VARCHAR(255) DEFAULT NULL,
    request_params TEXT,
    response_data TEXT,
    response_status INT DEFAULT NULL,
    execution_time BIGINT DEFAULT NULL,
    client_ip VARCHAR(50) DEFAULT NULL,
    user_agent VARCHAR(500) DEFAULT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_audit_log_operator (operator_id, create_time),
    KEY idx_audit_log_module (module, create_time)
);

CREATE TABLE gyt_user_hierarchy (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role TINYINT NOT NULL,
    parent_id BIGINT DEFAULT 0,
    tree_path VARCHAR(1000) DEFAULT NULL,
    agent_count INT DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted TINYINT DEFAULT 0,
    PRIMARY KEY (id),
    KEY idx_user_hierarchy_user_id (user_id),
    KEY idx_user_hierarchy_parent_id (parent_id)
);

INSERT INTO gyt_product (product_name, product_code, product_type, market_price, join_price, agent_price, partner_price, stock, unit, description, status) VALUES
('药师祖庭调理仪', 'YST-DEVICE-001', 1, 3980.00, 2980.00, 2680.00, 2380.00, 50, '台', '门店基础调理设备', 1),
('药师祖庭养护套盒', 'YST-KIT-001', 2, 1396.00, 998.00, 898.00, 798.00, 200, '套', '会员养护服务套盒', 1),
('药师祖庭草本单品', 'YST-SKU-001', 3, 398.00, 298.00, 268.00, 238.00, 500, '盒', '日常复购草本单品', 1);
