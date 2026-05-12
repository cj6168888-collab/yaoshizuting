-- 添加乐观锁版本字段
ALTER TABLE gyt_user ADD COLUMN version INT DEFAULT 0 COMMENT '乐观锁版本号';

-- 添加唯一约束防止重复分润
ALTER TABLE gyt_profit_log ADD UNIQUE INDEX uk_order_type_receiver (order_sn, type, receiver_id);

-- 添加索引优化
CREATE INDEX idx_profit_receiver_time ON gyt_profit_log(receiver_id, create_time);

-- 添加提现单号索引
CREATE INDEX idx_withdrawal_user_status ON gyt_withdrawal(user_id, status);

-- 添加订单状态索引
CREATE INDEX idx_order_user_status ON gyt_order(user_id, status);

-- 添加审计日志表
CREATE TABLE IF NOT EXISTS gyt_audit_log (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
    module VARCHAR(50) COMMENT '模块',
    operation VARCHAR(100) COMMENT '操作',
    operator_id BIGINT COMMENT '操作人ID',
    operator_name VARCHAR(50) COMMENT '操作人名称',
    request_method VARCHAR(10) COMMENT '请求方法',
    request_url VARCHAR(255) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    response_data TEXT COMMENT '响应数据',
    response_status INT COMMENT '响应状态',
    execution_time BIGINT COMMENT '执行时间(ms)',
    client_ip VARCHAR(50) COMMENT '客户端IP',
    user_agent VARCHAR(500) COMMENT '用户代理',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    deleted TINYINT DEFAULT 0 COMMENT '逻辑删除'
);

CREATE INDEX idx_audit_log_operator ON gyt_audit_log(operator_id, create_time);
CREATE INDEX idx_audit_log_module ON gyt_audit_log(module, create_time);
