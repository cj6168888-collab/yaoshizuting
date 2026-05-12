-- Optimize admin list and export queries.
CREATE INDEX idx_user_role_status_time ON gyt_user(deleted, role, status, create_time);

CREATE INDEX idx_product_type_status_time ON gyt_product(deleted, product_type, status, update_time);

CREATE INDEX idx_withdrawal_status_time ON gyt_withdrawal(deleted, status, create_time);

CREATE INDEX idx_profit_type_time ON gyt_profit_log(deleted, type, create_time);
