# Online Chat System

在线聊天系统课程项目，采用前后端分离架构，支持用户认证、好友管理、好友分组、私聊消息和群聊 WebSocket 实时通信。

## 功能模块

### 用户认证

- 用户注册：用户名/昵称/邮箱/手机号注册，BCrypt 密码加密，注册后自动创建默认好友分组
- 用户登录：用户名密码登录，返回 JWT 令牌（24小时有效）
- JWT 鉴权：Spring Security 无状态认证，除登录注册外所有接口需携带 `Authorization: Bearer <token>`

### 好友管理

- 搜索用户：按用户名/昵称/邮箱模糊搜索，返回与当前用户的关系状态（SELF / FRIEND / NONE / PENDING_SENT / PENDING_RECEIVED）
- 发送好友申请：可附带申请备注，自动校验不能添加自己、已是好友或已有待处理申请
- 处理好友申请：同意（自动创建双向好友关系，可指定分组和备注）或拒绝（可填写拒绝原因）
- 重新发送申请：已拒绝的申请可重新发送，更新申请时间，保留历史记录
- 好友列表：分页展示，支持按分组和关键字筛选
- 好友详情：查看好友完整资料（昵称、邮箱、手机号、成为好友时间、最后登录时间）
- 修改备注 / 删除好友：双向删除好友关系

### 好友分组

- 默认分组：用户注册时自动创建，不可删除
- 自定义分组：创建、修改名称、删除（删除后好友自动迁移到默认分组）
- 移动好友：将好友移动到指定分组
- 分组列表：展示每个分组下的好友数量

### 私聊消息

- 发送消息：支持 TEXT / IMAGE / VOICE / FILE 四种消息类型，仅好友间可发送
- 聊天历史：分页查询双向消息记录，按时间倒序
- 消息已读：进入会话时自动标记对方消息为已读
- 导出记录：将聊天记录导出为 TXT 文件下载

### 会话列表

- 最近会话：展示每个会话的最后一条消息、未读数量和最后消息时间
- 按置顶状态和最后消息时间倒序排列
- 标记已读：将指定会话的未读数清零
- 打开/创建私聊：点击会话进入私聊页面，同时更新双方会话记录

### 群聊功能

- 群组管理：创建群组、加入/退出群组、查询群详情和成员列表
- WebSocket 实时通信：基于 JSR 356 `@ServerEndpoint` 注解，端点 `ws://localhost:8080/ws/{userId}`
- 群消息广播：通过 WebSocket 向群内所有在线成员实时推送消息
- 群消息持久化：所有群消息入库存储
- 群历史消息：分页查询群聊历史记录
- 群记录导出：导出群聊记录为 TXT 文件
- 在线成员查询：查看群组当前在线成员列表

### 通用能力

- 统一返回格式 `Result<T>`：`{ code, message, data, timestamp }`
- 分页对象 `PageResult<T>`：`{ records, total, pageNo, pageSize, pages }`
- 统一异常处理：`@RestControllerAdvice` 全局捕获 BusinessException、参数校验异常等
- MyBatis Plus 分页插件：MySQL 方言自动分页
- Swagger / OpenAPI 文档：SpringDoc，支持 Bearer JWT 鉴权
- 单元测试：JWT 令牌生成与解析、好友申请业务逻辑（Mockito + AssertJ）
- 所有配置支持环境变量覆盖

## 技术栈

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 2.7.18 |
| **安全认证** | Spring Security + JJWT | 0.11.5 |
| **ORM** | MyBatis Plus | 3.5.7 |
| **数据库** | MySQL | 8.0 |
| **WebSocket** | javax.websocket (JSR 356) | — |
| **API 文档** | SpringDoc OpenAPI | 1.7.0 |
| **JSON** | Fastjson | 1.2.83 |
| **构建工具** | Maven | — |
| **前端框架** | Vue 3 (Composition API) | 3.x |
| **构建工具** | Vite | 6.x |
| **UI 组件库** | Element Plus | 2.9 |
| **路由** | Vue Router | 4.x |
| **状态管理** | Pinia | 3.x |
| **HTTP 客户端** | Axios | 1.x |
| **语言** | Java 17 / JavaScript | — |

## 项目结构

```text
backend/                          # Spring Boot 2.7 后端
├── pom.xml
└── src/main/
    ├── java/com/onlinechat/      # 主模块：认证 + 好友 + 私聊 + 会话
    │   ├── OnlineChatApplication.java
    │   ├── common/                # Result、PageResult、ResultCode、状态枚举
    │   ├── config/                # Security、OpenApi、MybatisPlus、JWT 属性
    │   ├── controller/            # 7 个 REST 控制器
    │   ├── dto/                   # 10 个请求体 DTO
    │   ├── entity/                # 6 个实体类
    │   ├── exception/             # BusinessException + GlobalExceptionHandler
    │   ├── mapper/                # 6 个 MyBatis Plus Mapper
    │   ├── security/              # JWT 令牌、认证过滤器、CurrentUser
    │   ├── service/               # 7 个 Service 接口
    │   │   └── impl/              # 7 个 Service 实现
    │   └── vo/                    # 9 个响应 VO
    ├── java/com/chat/             # 群聊模块：WebSocket + REST
    │   ├── config/WebSocketConfig.java
    │   ├── controller/GroupMessageController.java
    │   └── websocket/GroupChatWebSocket.java
    └── resources/application.yml

frontend/                          # Vue 3 + Element Plus 前端
├── index.html
├── package.json
├── vite.config.js
└── src/
    ├── App.vue
    ├── main.js
    ├── api/                       # 9 个 Axios API 模块
    │   ├── auth.js               # 登录/注册/当前用户
    │   ├── chat.js               # WebSocket 客户端
    │   ├── conversations.js      # 会话相关
    │   ├── friendGroups.js       # 好友分组 CRUD
    │   ├── friendRequests.js     # 好友申请 CRUD
    │   ├── friends.js            # 好友管理 CRUD
    │   ├── groups.js             # 群聊相关
    │   ├── messages.js           # 私聊消息
    │   └── users.js              # 用户搜索
    ├── router/index.js           # 路由配置（6 条路由）
    ├── stores/                   # 3 个 Pinia Store
    ├── utils/request.js          # Axios 请求封装（JWT 注入 + 401 拦截）
    └── views/
        ├── LoginView.vue          # 登录/注册页
        ├── FriendManagementView.vue  # 好友管理主页
        ├── ConversationListView.vue  # 会话列表页
        ├── ChatView.vue           # 私聊页
        └── group/
            └── GroupChat.vue      # 群聊组件

sql/
├── schema.sql                    # 9 张表建表脚本
└── seed.sql                      # 示例数据（alice / bob）
```

## 数据库设计

共 9 张表，均使用 InnoDB 引擎、utf8mb4 字符集、逻辑删除：

| 表名 | 说明 | 关键字段 |
|------|------|----------|
| `user` | 用户表 | id, username, nickname, email, phone, avatar, password_hash, status |
| `friend_group` | 好友分组表 | id, user_id, name, is_default, sort_order |
| `friendship` | 好友关系表（双向存储） | id, user_id, friend_id, group_id, remark, status |
| `friend_request` | 好友申请表 | id, sender_id, receiver_id, message, status, handle_reason |
| `private_message` | 私聊消息表 | id, from_user_id, to_user_id, content, message_type, read_at |
| `conversation` | 最近会话表 | id, owner_id, target_user_id, conversation_type, unread_count, pinned, muted |
| `group_info` | 群组信息表 | id, name, announcement, owner_id |
| `group_member` | 群成员关系表 | id, group_id, user_id, role |
| `group_message` | 群消息表 | id, group_id, from_user_id, content, message_type |

## API 接口一览

### 认证接口（无需 Token）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/auth/register` | 用户注册，返回 JWT |
| POST | `/api/auth/login` | 用户登录，返回 JWT |
| GET | `/api/auth/me` | 获取当前登录用户 |

### 用户接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/users/search` | 搜索用户，返回关系状态 |

### 好友管理接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/friends` | 分页查询好友列表 |
| GET | `/api/friends/{friendId}` | 好友详情 |
| PUT | `/api/friends/{friendId}/remark` | 修改好友备注 |
| PUT | `/api/friends/{friendId}/group` | 移动好友到分组 |
| DELETE | `/api/friends/{friendId}` | 双向删除好友 |

### 好友分组接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/friend-groups` | 分组列表（含好友数量） |
| POST | `/api/friend-groups` | 创建分组 |
| PUT | `/api/friend-groups/{groupId}` | 修改分组名称 |
| DELETE | `/api/friend-groups/{groupId}` | 删除分组（好友迁移到默认分组） |

### 好友申请接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/friend-requests` | 发送好友申请 |
| GET | `/api/friend-requests` | 分页查询申请（received / sent） |
| PUT | `/api/friend-requests/{requestId}/accept` | 同意申请 |
| PUT | `/api/friend-requests/{requestId}/reject` | 拒绝申请 |
| POST | `/api/friend-requests/{requestId}/resend` | 重新发送已拒绝的申请 |

### 私聊消息接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/messages/private` | 发送私聊消息 |
| GET | `/api/messages/private/{targetUserId}` | 分页查询聊天历史 |
| GET | `/api/messages/private/{targetUserId}/export` | 导出聊天记录为 TXT |

### 会话接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | `/api/conversations/recent` | 最近会话列表 |
| GET | `/api/conversations/private/{targetUserId}` | 打开/创建私聊会话 |
| PUT | `/api/conversations/{conversationId}/read` | 标记会话已读 |

### 群聊接口

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | `/api/group/send` | 发送群消息（WebSocket 广播 + 入库） |
| GET | `/api/group/online/{groupId}` | 查询群在线成员 |
| POST | `/api/groups` | 创建群聊 |
| GET | `/api/groups` | 查询当前用户加入的群 |
| GET | `/api/groups/{groupId}` | 查询群详情 |
| POST | `/api/groups/{groupId}/join` | 加入群聊 |
| POST | `/api/groups/{groupId}/leave` | 退出群聊 |
| GET | `/api/groups/{groupId}/members` | 查询群成员列表 |
| GET | `/api/groups/{groupId}/messages` | 分页查询群历史消息 |
| GET | `/api/groups/{groupId}/export` | 导出群聊记录 TXT |
| WS | `ws://localhost:8080/ws/{userId}` | WebSocket 群聊连接 |

## 前端路由

| 路径 | 页面 | 说明 |
|------|------|------|
| `/` | — | 重定向到 `/friends` |
| `/login` | LoginView | 登录/注册页（无需登录） |
| `/friends` | FriendManagementView | 好友管理主页 |
| `/conversations` | ConversationListView | 最近会话列表 |
| `/chat/:targetUserId` | ChatView | 私聊对话页 |
| `/group/:groupId` | GroupChat | 群聊页 |

## 启动与运行

### 环境要求

- JDK 17+
- Maven 3.x
- Node.js 16+
- MySQL 8.0

### 1. 配置环境变量

复制并编辑环境变量文件：

```bash
cp .env.example .env
# 编辑 .env 修改数据库密码和 JWT Secret
```

关键环境变量：

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `SERVER_PORT` | `8080` | 后端服务端口 |
| `DB_URL` | `jdbc:mysql://localhost:3306/online_chat?...` | 数据库连接地址 |
| `DB_USERNAME` | `root` | 数据库用户名 |
| `DB_PASSWORD` | `123456` | 数据库密码 |
| `JWT_SECRET` | `online-chat-system-jwt-secret-key-for-hs256` | JWT 签名密钥（生产环境务必修改） |
| `JWT_EXPIRE_MILLIS` | `86400000` | JWT 过期时间（毫秒，默认 24 小时） |

### 2. 数据库初始化

```bash
mysql -u root -p < sql/schema.sql
mysql -u root -p --default-character-set=utf8mb4 < sql/seed.sql
```

### 3. 后端启动

```bash
cd backend
mvn spring-boot:run
```

后端运行在 `http://localhost:8080`，Swagger 文档：`http://localhost:8080/swagger-ui.html`

### 4. 前端启动

```bash
cd frontend
npm install
npm run dev
```

前端运行在 `http://localhost:5173`，Vite 自动将 `/api` 请求代理到后端。

### 5. 测试账号

| 用户名 | 密码 | 说明 |
|--------|------|------|
| `alice` | `123456` | 与 bob 已是好友，有历史会话 |
| `bob` | `123456` | 与 alice 已是好友，有历史会话 |

### 6. Docker 部署（推荐）

```bash
docker-compose up -d
```

## 文档

- ER 图设计说明：[docs/er-design.md](docs/er-design.md)
- API 接口说明：[docs/swagger-api.md](docs/swagger-api.md)
- 完整目录结构：[docs/project-structure.md](docs/project-structure.md)
- 建表脚本：[sql/schema.sql](sql/schema.sql)
