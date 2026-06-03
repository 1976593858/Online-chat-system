# Online Chat System

在线聊天系统课程项目，当前已实现：

- 登录、注册、JWT登录校验
- 好友管理：搜索用户、发送好友申请、同意/拒绝申请、删除好友、好友详情、修改备注
- 好友分组：默认分组、创建分组、修改分组、删除分组、移动好友
- 消息列表：最近会话、最后一条消息、未读数量、按最后消息时间排序
- 统一返回 `Result<T>`、统一异常处理、MyBatis Plus 分页、Swagger 文档、单元测试
- Vue3 + Element Plus + Axios + Pinia 前端页面

## 目录

```text
backend/   Spring Boot 2.7.x 后端
frontend/  Vue3 + Element Plus 前端
docs/      ER图与接口文档
sql/       MySQL 8 建表与示例数据
```

## 后端启动

1. 执行 `sql/schema.sql` 创建数据库和表。
2. 按需执行 `sql/seed.sql` 写入示例数据。
3. 配置环境变量，或直接使用 `backend/src/main/resources/application.yml` 默认值。
4. 在 `backend/` 下运行：

```bash
mvn spring-boot:run
```

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

## 前端启动

在 `frontend/` 下运行：

```bash
npm install
npm run dev
```

默认前端地址：

```text
http://localhost:5173
```

## 文档

- ER图设计说明：[docs/er-design.md](docs/er-design.md)
- Swagger接口说明：[docs/swagger-api.md](docs/swagger-api.md)
- 建表脚本：[sql/schema.sql](sql/schema.sql)
