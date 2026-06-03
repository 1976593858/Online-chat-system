# ER图设计说明

## ER图

```mermaid
erDiagram
    USER ||--o{ FRIEND_GROUP : owns
    USER ||--o{ FRIENDSHIP : owner
    USER ||--o{ FRIENDSHIP : friend
    USER ||--o{ FRIEND_REQUEST : sends
    USER ||--o{ FRIEND_REQUEST : receives
    USER ||--o{ CONVERSATION : owns
    USER ||--o{ CONVERSATION : target
    FRIEND_GROUP ||--o{ FRIENDSHIP : contains

    USER {
        bigint id PK
        varchar username UK
        varchar nickname
        varchar email UK
        varchar phone UK
        varchar avatar
        varchar password_hash
        tinyint status
        datetime last_login_at
        tinyint deleted
    }

    FRIEND_GROUP {
        bigint id PK
        bigint user_id FK
        varchar name
        tinyint is_default
        int sort_order
        tinyint deleted
    }

    FRIENDSHIP {
        bigint id PK
        bigint user_id FK
        bigint friend_id FK
        bigint group_id FK
        varchar remark
        varchar status
        tinyint deleted
    }

    FRIEND_REQUEST {
        bigint id PK
        bigint sender_id FK
        bigint receiver_id FK
        varchar message
        varchar status
        varchar handle_reason
        datetime handled_at
        tinyint deleted
    }

    CONVERSATION {
        bigint id PK
        bigint owner_id FK
        bigint target_user_id FK
        varchar conversation_type
        bigint last_message_id
        varchar last_message_content
        varchar last_message_type
        int unread_count
        datetime last_message_at
        tinyint pinned
        tinyint muted
        tinyint deleted
    }
```

## 设计说明

- `user` 是系统用户主表，用户名、邮箱、手机号独立唯一索引，支持逻辑删除和账号禁用。
- `friend_group` 是用户侧分组表，每个用户注册或登录时自动保证存在一个 `默认分组`。
- `friendship` 使用用户侧有向关系保存好友，A 和 B 成为好友后写入两条记录，因此双方可以维护各自独立的备注和分组。
- `friend_request` 保存好友申请状态，状态为 `PENDING`、`ACCEPTED`、`REJECTED`，同意后由服务层双向写入 `friendship`。
- `conversation` 是消息列表/最近会话表，直接维护最后一条消息内容、最后消息时间和未读数，查询按 `pinned DESC, last_message_at DESC` 排序。
- 所有业务表都有 `deleted` 逻辑删除字段，并围绕查询条件设计组合索引，避免好友列表、申请列表和最近会话列表出现全表扫描。
