CREATE DATABASE IF NOT EXISTS online_chat
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE online_chat;

DROP TABLE IF EXISTS conversation;
DROP TABLE IF EXISTS private_message;
DROP TABLE IF EXISTS friend_request;
DROP TABLE IF EXISTS friendship;
DROP TABLE IF EXISTS friend_group;
DROP TABLE IF EXISTS group_message;
DROP TABLE IF EXISTS group_member;
DROP TABLE IF EXISTS group_info;
DROP TABLE IF EXISTS `user`;

CREATE TABLE `user` (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(32) NOT NULL COMMENT '登录用户名',
    nickname VARCHAR(32) NOT NULL COMMENT '昵称',
    email VARCHAR(128) NULL COMMENT '邮箱',
    phone VARCHAR(20) NULL COMMENT '手机号',
    avatar VARCHAR(255) NULL COMMENT '头像URL',
    password_hash VARCHAR(100) NOT NULL COMMENT 'BCrypt密码哈希',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：1正常，0禁用',
    last_login_at DATETIME NULL COMMENT '最后登录时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    UNIQUE KEY uk_user_email (email),
    UNIQUE KEY uk_user_phone (phone),
    KEY idx_user_nickname (nickname),
    KEY idx_user_status_deleted (status, deleted),
    KEY idx_user_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';

CREATE TABLE friend_group (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '分组ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    name VARCHAR(32) NOT NULL COMMENT '分组名称',
    is_default TINYINT NOT NULL DEFAULT 0 COMMENT '是否默认分组：1是，0否',
    sort_order INT NOT NULL DEFAULT 0 COMMENT '排序值',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_friend_group_user_sort (user_id, deleted, is_default, sort_order),
    KEY idx_friend_group_name (user_id, name, deleted),
    CONSTRAINT fk_friend_group_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友分组表';

CREATE TABLE friendship (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '好友关系ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    friend_id BIGINT UNSIGNED NOT NULL COMMENT '好友用户ID',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '当前用户侧好友分组ID',
    remark VARCHAR(50) NULL COMMENT '好友备注',
    status VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT '关系状态：ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_friendship_user_group (user_id, group_id, status, deleted),
    KEY idx_friendship_user_friend (user_id, friend_id, status, deleted),
    KEY idx_friendship_friend_user (friend_id, user_id, status, deleted),
    CONSTRAINT fk_friendship_user FOREIGN KEY (user_id) REFERENCES `user` (id),
    CONSTRAINT fk_friendship_friend FOREIGN KEY (friend_id) REFERENCES `user` (id),
    CONSTRAINT fk_friendship_group FOREIGN KEY (group_id) REFERENCES friend_group (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友关系表，按用户侧存储双向关系';

CREATE TABLE friend_request (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '好友申请ID',
    sender_id BIGINT UNSIGNED NOT NULL COMMENT '申请发起人ID',
    receiver_id BIGINT UNSIGNED NOT NULL COMMENT '申请接收人ID',
    message VARCHAR(200) NULL COMMENT '申请备注',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING、ACCEPTED、REJECTED',
    handle_reason VARCHAR(200) NULL COMMENT '处理原因',
    handled_at DATETIME NULL COMMENT '处理时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_friend_request_receiver_status (receiver_id, status, deleted, created_at),
    KEY idx_friend_request_sender_status (sender_id, status, deleted, created_at),
    KEY idx_friend_request_pair_status (sender_id, receiver_id, status, deleted),
    CONSTRAINT fk_friend_request_sender FOREIGN KEY (sender_id) REFERENCES `user` (id),
    CONSTRAINT fk_friend_request_receiver FOREIGN KEY (receiver_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='好友申请表';

CREATE TABLE private_message (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    from_user_id BIGINT UNSIGNED NOT NULL COMMENT '发送者ID',
    to_user_id BIGINT UNSIGNED NOT NULL COMMENT '接收者ID',
    content VARCHAR(2000) NOT NULL COMMENT '消息内容',
    message_type VARCHAR(16) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT、IMAGE、VOICE、FILE',
    read_at DATETIME NULL COMMENT '已读时间（接收方读取后写入）',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_private_message_pair_time (from_user_id, to_user_id, created_at),
    KEY idx_private_message_to_read (to_user_id, read_at, created_at),
    CONSTRAINT fk_private_message_from_user FOREIGN KEY (from_user_id) REFERENCES `user` (id),
    CONSTRAINT fk_private_message_to_user FOREIGN KEY (to_user_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='私聊消息表';

ALTER TABLE private_message ADD FULLTEXT INDEX ft_private_message_content (content) WITH PARSER ngram;

CREATE TABLE conversation (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '会话ID',
    owner_id BIGINT UNSIGNED NOT NULL COMMENT '会话所属用户ID',
    target_user_id BIGINT UNSIGNED NULL COMMENT '私聊目标用户ID',
    conversation_type VARCHAR(16) NOT NULL DEFAULT 'PRIVATE' COMMENT '会话类型：PRIVATE、GROUP',
    last_message_id BIGINT UNSIGNED NULL COMMENT '最后消息ID',
    last_message_content VARCHAR(1000) NULL COMMENT '最后一条消息内容',
    last_message_type VARCHAR(16) NOT NULL DEFAULT 'TEXT' COMMENT '最后消息类型：TEXT、IMAGE、VOICE、FILE',
    unread_count INT NOT NULL DEFAULT 0 COMMENT '未读数量',
    last_message_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后消息时间',
    pinned TINYINT NOT NULL DEFAULT 0 COMMENT '是否置顶：1是，0否',
    muted TINYINT NOT NULL DEFAULT 0 COMMENT '是否免打扰：1是，0否',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_conversation_owner_recent (owner_id, deleted, pinned, last_message_at),
    KEY idx_conversation_owner_target (owner_id, target_user_id, conversation_type, deleted),
    KEY idx_conversation_unread (owner_id, unread_count, deleted),
    CONSTRAINT fk_conversation_owner FOREIGN KEY (owner_id) REFERENCES `user` (id),
    CONSTRAINT fk_conversation_target_user FOREIGN KEY (target_user_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='最近会话表';

CREATE TABLE group_info (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '群ID',
    name VARCHAR(64) NOT NULL COMMENT '群名称',
    announcement VARCHAR(500) NULL COMMENT '群公告',
    invite_code VARCHAR(8) NULL COMMENT '8位随机邀请码',
    owner_id BIGINT UNSIGNED NOT NULL COMMENT '群主用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_invite_code (invite_code),
    KEY idx_group_info_owner (owner_id, deleted),
    CONSTRAINT fk_group_info_owner FOREIGN KEY (owner_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='群组信息表';

CREATE TABLE group_member (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '成员关系ID',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '群ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    role VARCHAR(16) NOT NULL DEFAULT 'MEMBER' COMMENT '角色：OWNER、ADMIN、MEMBER',
    muted TINYINT NOT NULL DEFAULT 0 COMMENT '是否免打扰：1是，0否',
    joined_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_group_member (group_id, user_id),
    KEY idx_group_member_user (user_id, deleted),
    CONSTRAINT fk_group_member_group FOREIGN KEY (group_id) REFERENCES group_info (id),
    CONSTRAINT fk_group_member_user FOREIGN KEY (user_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='群成员关系表';

CREATE TABLE group_message (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '群ID',
    from_user_id BIGINT UNSIGNED NOT NULL COMMENT '发送者ID',
    content VARCHAR(2000) NOT NULL COMMENT '消息内容',
    message_type VARCHAR(16) NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT、IMAGE、VOICE、FILE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_group_message_group_time (group_id, created_at),
    KEY idx_group_message_from (from_user_id, created_at),
    CONSTRAINT fk_group_message_group FOREIGN KEY (group_id) REFERENCES group_info (id),
    CONSTRAINT fk_group_message_from_user FOREIGN KEY (from_user_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='群消息表';

CREATE TABLE group_invite (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '邀请ID',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '群ID',
    sender_id BIGINT UNSIGNED NOT NULL COMMENT '邀请人ID',
    invitee_id BIGINT UNSIGNED NOT NULL COMMENT '被邀请人ID',
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING、ACCEPTED、REJECTED',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_group_invite_invitee_status (invitee_id, status, deleted),
    KEY idx_group_invite_group (group_id, status, deleted),
    CONSTRAINT fk_group_invite_group FOREIGN KEY (group_id) REFERENCES group_info (id),
    CONSTRAINT fk_group_invite_sender FOREIGN KEY (sender_id) REFERENCES `user` (id),
    CONSTRAINT fk_group_invite_invitee FOREIGN KEY (invitee_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='群邀请表';
