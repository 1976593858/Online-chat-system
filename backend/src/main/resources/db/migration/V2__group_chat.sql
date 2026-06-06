-- V2__group_chat.sql — 群聊功能数据库升级脚本
-- Flyway 迁移，前提：V1 = sql/schema.sql 已执行

CREATE TABLE IF NOT EXISTS group_info (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '群ID',
    name VARCHAR(64) NOT NULL COMMENT '群名称',
    announcement VARCHAR(500) NULL COMMENT '群公告',
    owner_id BIGINT UNSIGNED NOT NULL COMMENT '群主用户ID',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0未删除，1已删除',
    PRIMARY KEY (id),
    KEY idx_group_info_owner (owner_id, deleted),
    CONSTRAINT fk_group_info_owner FOREIGN KEY (owner_id) REFERENCES `user` (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='群组信息表';

CREATE TABLE IF NOT EXISTS group_member (
    id BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '成员关系ID',
    group_id BIGINT UNSIGNED NOT NULL COMMENT '群ID',
    user_id BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
    role VARCHAR(16) NOT NULL DEFAULT 'MEMBER' COMMENT '角色：OWNER、ADMIN、MEMBER',
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

CREATE TABLE IF NOT EXISTS group_message (
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
