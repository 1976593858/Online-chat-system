USE online_chat;

INSERT INTO `user` (id, username, nickname, email, phone, avatar, password_hash, status)
VALUES
    (1, 'alice', 'Alice', 'alice@example.com', '13800138001', NULL, '$2a$10$vbTShqyu0DQUFtXhvFwdTe1qte5AGxIFHRJk9FRiokTsiEFkUtP82', 1),
    (2, 'bob', 'Bob', 'bob@example.com', '13800138002', NULL, '$2a$10$vbTShqyu0DQUFtXhvFwdTe1qte5AGxIFHRJk9FRiokTsiEFkUtP82', 1);

INSERT INTO friend_group (id, user_id, name, is_default, sort_order)
VALUES
    (1, 1, '默认分组', 1, 0),
    (2, 2, '默认分组', 1, 0);

INSERT INTO friendship (user_id, friend_id, group_id, remark, status)
VALUES
    (1, 2, 1, 'Bob', 'ACTIVE'),
    (2, 1, 2, 'Alice', 'ACTIVE');

INSERT INTO conversation (owner_id, target_user_id, conversation_type, last_message_content, last_message_type, unread_count, last_message_at)
VALUES
    (1, 2, 'PRIVATE', '你好，Alice', 'TEXT', 1, NOW()),
    (2, 1, 'PRIVATE', '你好，Alice', 'TEXT', 0, NOW());
