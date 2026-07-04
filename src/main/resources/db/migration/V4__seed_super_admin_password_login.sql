UPDATE gyt_user
SET username = 'admin',
    password = '$2a$12$j9WkQWTVk8hvBA1/N6R0Ye9fbry9lEaXjHMIcc65LFgDfwa5pVIU2',
    role = 10,
    status = 1,
    deleted = 0
WHERE mobile = '13800000000';

INSERT INTO gyt_user (
    username, password, mobile, nickname, role, parent_id, tree_path,
    agent_count, store_count, balance, total_earnings, status, deleted
)
SELECT
    'admin',
    '$2a$12$j9WkQWTVk8hvBA1/N6R0Ye9fbry9lEaXjHMIcc65LFgDfwa5pVIU2',
    '13800000000',
    '系统管理员',
    10,
    0,
    '/0/',
    0,
    0,
    0,
    0,
    1,
    0
WHERE NOT EXISTS (
    SELECT 1 FROM gyt_user WHERE username = 'admin' AND deleted = 0
)
AND NOT EXISTS (
    SELECT 1 FROM gyt_user WHERE mobile = '13800000000' AND deleted = 0
);
