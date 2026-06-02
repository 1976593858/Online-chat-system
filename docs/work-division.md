# 功能开发分工文档

## 个人负责模块

本次在线聊天系统中，我主要负责开发“好友管理模块”和“消息列表模块”，并补充相关数据库设计、后端接口、前端页面、接口文档和测试内容。

## 具体工作内容

### 1. 数据库设计

我完成了好友管理和消息列表相关的数据表设计，包括：

- `user`：用户基础信息表，用于登录、注册、用户搜索和好友信息展示。
- `friend_group`：好友分组表，用于支持默认分组、自定义分组、修改分组和删除分组。
- `friendship`：好友关系表，用于保存双向好友关系、好友备注和好友所在分组。
- `friend_request`：好友申请表，用于保存好友申请、同意、拒绝和处理状态。
- `conversation`：最近会话表，用于展示消息列表中的最后一条消息、未读数量和最后消息时间。

同时，我为这些表设计了主键、外键、唯一索引和组合索引，保证好友列表、好友申请列表、用户搜索和最近会话查询的效率。

相关文件：

- `sql/schema.sql`
- `sql/seed.sql`
- `docs/er-design.md`

### 2. 后端好友管理功能

我完成了好友管理相关的 RESTful API，主要包括：

- 搜索用户。
- 发送好友申请。
- 查询收到或发出的好友申请。
- 同意好友申请，并自动建立双向好友关系。
- 拒绝好友申请。
- 分页查询好友列表。
- 查看好友详情。
- 修改好友备注。
- 删除好友，并同步删除双方好友关系。

后端实现中，我遵守了 Controller 不写业务逻辑的规范，将业务规则放在 Service 层处理，例如不能添加自己为好友、不能重复发送申请、同意申请后双向写入好友关系等。

相关文件：

- `backend/src/main/java/com/onlinechat/controller/FriendController.java`
- `backend/src/main/java/com/onlinechat/controller/FriendRequestController.java`
- `backend/src/main/java/com/onlinechat/controller/UserController.java`
- `backend/src/main/java/com/onlinechat/service/FriendService.java`
- `backend/src/main/java/com/onlinechat/service/FriendRequestService.java`
- `backend/src/main/java/com/onlinechat/service/impl/FriendServiceImpl.java`
- `backend/src/main/java/com/onlinechat/service/impl/FriendRequestServiceImpl.java`

### 3. 好友分组功能

我完成了好友分组管理功能，包括：

- 用户注册或登录后自动创建默认分组。
- 查询好友分组列表，并显示每个分组下的好友数量。
- 创建新的好友分组。
- 修改好友分组名称。
- 删除普通分组。
- 删除分组时，将该分组下的好友移动到默认分组。
- 移动好友到指定分组。

该部分保证了默认分组不能被删除，同时对分组归属进行了校验，防止用户操作不属于自己的分组。

相关文件：

- `backend/src/main/java/com/onlinechat/controller/FriendGroupController.java`
- `backend/src/main/java/com/onlinechat/service/FriendGroupService.java`
- `backend/src/main/java/com/onlinechat/service/impl/FriendGroupServiceImpl.java`
- `backend/src/main/java/com/onlinechat/mapper/FriendGroupMapper.java`

### 4. 消息列表功能

我完成了最近会话列表功能，包括：

- 分页获取最近会话。
- 显示会话目标用户信息。
- 显示最后一条消息内容。
- 显示最后消息类型。
- 显示未读消息数量。
- 按置顶状态和最后消息时间倒序排序。
- 支持将会话标记为已读。

该模块通过 `conversation` 表维护会话列表数据，为后续接入真实聊天消息表或 WebSocket 实时消息提供了扩展基础。

相关文件：

- `backend/src/main/java/com/onlinechat/controller/ConversationController.java`
- `backend/src/main/java/com/onlinechat/service/ConversationService.java`
- `backend/src/main/java/com/onlinechat/service/impl/ConversationServiceImpl.java`
- `backend/src/main/java/com/onlinechat/mapper/ConversationMapper.java`

### 5. 后端通用能力

为了保证功能符合企业级开发规范，我还完成了以下通用能力：

- 统一返回结果 `Result<T>`。
- 分页返回对象 `PageResult<T>`。
- 全局异常处理 `GlobalExceptionHandler`。
- 自定义业务异常 `BusinessException`。
- JWT 登录校验。
- Spring Security 无状态认证配置。
- Swagger/OpenAPI 接口文档配置。
- MyBatis Plus 分页插件配置。

相关文件：

- `backend/src/main/java/com/onlinechat/common/Result.java`
- `backend/src/main/java/com/onlinechat/common/PageResult.java`
- `backend/src/main/java/com/onlinechat/exception/GlobalExceptionHandler.java`
- `backend/src/main/java/com/onlinechat/security/JwtTokenProvider.java`
- `backend/src/main/java/com/onlinechat/security/JwtAuthenticationFilter.java`
- `backend/src/main/java/com/onlinechat/config/SecurityConfig.java`
- `backend/src/main/java/com/onlinechat/config/OpenApiConfig.java`
- `backend/src/main/java/com/onlinechat/config/MybatisPlusConfig.java`

### 6. 前端页面开发

我完成了 Vue3 前端页面和请求封装，包括：

- 登录/注册页面。
- 好友管理页面。
- 好友分组侧边栏。
- 好友列表分页展示。
- 用户搜索与添加好友。
- 好友申请列表、同意和拒绝操作。
- 好友详情抽屉。
- 移动好友分组弹窗。
- 最近会话列表页面。
- 未读数量展示和标记已读操作。

前端使用 Axios 统一封装请求，使用 Pinia 管理登录状态、好友状态和会话状态。

相关文件：

- `frontend/src/views/LoginView.vue`
- `frontend/src/views/FriendManagementView.vue`
- `frontend/src/views/ConversationListView.vue`
- `frontend/src/api/`
- `frontend/src/stores/`
- `frontend/src/utils/request.js`
- `frontend/src/router/index.js`

### 7. 测试与验证

我补充了单元测试，主要验证：

- JWT 令牌生成和解析。
- 不能添加自己为好友。
- 好友申请创建时状态为待处理。
- 同意好友申请后会创建双向好友关系。

相关文件：

- `backend/src/test/java/com/onlinechat/security/JwtTokenProviderTest.java`
- `backend/src/test/java/com/onlinechat/service/FriendRequestServiceImplTest.java`

## 接口文档

我为本次功能补充了 Swagger 接口说明，后端启动后可以通过以下地址查看接口文档：

```text
http://localhost:8080/swagger-ui.html
```

接口说明文档文件：

- `docs/swagger-api.md`

## 完成效果

通过本次开发，系统已经具备完整的好友管理能力和消息列表展示能力。用户可以注册登录后搜索其他用户、发送好友申请、处理好友申请、维护好友分组、查看好友详情，并在消息列表中查看最近会话、最后消息和未读数量。

该功能模块前后端分离，接口采用 RESTful 风格，后端代码按照 Controller、Service、Mapper 分层实现，数据库设计包含必要索引，整体满足课程项目和企业级开发规范要求。
