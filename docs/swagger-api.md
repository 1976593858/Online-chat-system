# Swagger接口文档

启动后访问：

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/v3/api-docs`

除登录和注册外，请求头需要携带：

```http
Authorization: Bearer <jwt-token>
```

## 认证接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| POST | `/api/auth/register` | 用户注册，返回 JWT，并自动创建默认分组 |
| POST | `/api/auth/login` | 用户登录，返回 JWT |
| GET | `/api/auth/me` | 获取当前登录用户 |

## 用户接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | `/api/users/search?keyword=&pageNo=1&pageSize=10` | 搜索用户，返回关系状态 `SELF`、`FRIEND`、`NONE`、`PENDING_SENT`、`PENDING_RECEIVED` |

## 好友申请接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| POST | `/api/friend-requests` | 发送好友申请 |
| GET | `/api/friend-requests?direction=received&status=PENDING&pageNo=1&pageSize=10` | 分页查询收到或发出的申请 |
| PUT | `/api/friend-requests/{requestId}/accept` | 同意好友申请，可指定当前用户侧分组和备注 |
| PUT | `/api/friend-requests/{requestId}/reject` | 拒绝好友申请，可填写拒绝原因 |

## 好友管理接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | `/api/friends?groupId=&keyword=&pageNo=1&pageSize=10` | 分页查询好友 |
| GET | `/api/friends/{friendId}` | 好友详情 |
| PUT | `/api/friends/{friendId}/remark` | 修改好友备注 |
| PUT | `/api/friends/{friendId}/group` | 移动好友到指定分组 |
| DELETE | `/api/friends/{friendId}` | 双向删除好友 |

## 好友分组接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | `/api/friend-groups` | 分组列表，含好友数量 |
| POST | `/api/friend-groups` | 创建分组 |
| PUT | `/api/friend-groups/{groupId}` | 修改分组名称 |
| DELETE | `/api/friend-groups/{groupId}` | 删除分组，好友自动迁移到默认分组 |

## 消息列表接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| GET | `/api/conversations/recent?pageNo=1&pageSize=20` | 最近会话，包含最后消息、未读数，按最后消息时间排序 |
| GET | `/api/conversations/private/{targetUserId}` | 打开/创建与某用户的私聊会话，返回会话信息 |
| PUT | `/api/conversations/{conversationId}/read` | 标记会话已读 |

## 私聊消息接口

| 方法 | 地址 | 说明 |
| --- | --- | --- |
| POST | `/api/messages/private` | 发送私聊消息 |
| GET | `/api/messages/private/{targetUserId}?pageNo=1&pageSize=20` | 分页查询与某用户的私聊记录（倒序） |
| GET | `/api/messages/private/{targetUserId}/export` | 导出与某用户的私聊记录（txt 下载） |
