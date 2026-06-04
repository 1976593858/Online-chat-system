#!/bin/bash
# Online Chat System — Ubuntu 22.04 部署脚本
# 用法: chmod +x deploy.sh && ./deploy.sh

set -e

echo "=== Online Chat System 部署 ==="

# --- 环境检查 ---
command -v java >/dev/null 2>&1 || { echo "请先安装 JDK 17+"; exit 1; }
command -v mvn >/dev/null 2>&1 || { echo "请先安装 Maven 3.x"; exit 1; }
command -v mysql >/dev/null 2>&1 || { echo "请先安装 MySQL 8.0"; exit 1; }
command -v node >/dev/null 2>&1 || { echo "请先安装 Node.js 16+"; exit 1; }

# --- 加载环境变量 ---
if [ -f .env ]; then
    set -a; source .env; set +a
fi

# --- 数据库初始化 ---
echo "[1/4] 初始化数据库..."
mysql -u"${DB_USERNAME:-root}" -p"${DB_PASSWORD:-123456}" < sql/schema.sql 2>/dev/null || true
echo "  数据库表已创建"

# --- 后端构建 ---
echo "[2/4] 构建后端..."
cd backend
mvn clean package -DskipTests -q
cd ..
echo "  后端构建完成"

# --- 前端构建 ---
echo "[3/4] 构建前端..."
cd frontend
npm install --silent
npm run build
cd ..
echo "  前端构建完成"

# --- 启动后端 ---
echo "[4/4] 启动服务..."
export DB_URL="${DB_URL:-jdbc:mysql://localhost:3306/online_chat?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true}"
export DB_USERNAME="${DB_USERNAME:-root}"
export DB_PASSWORD="${DB_PASSWORD:-123456}"
export JWT_SECRET="${JWT_SECRET:-change-me-to-a-random-secret-at-least-32-chars}"
export SERVER_PORT="${SERVER_PORT:-8080}"

nohup java -jar backend/target/online-chat-backend-1.0.0.jar > app.log 2>&1 &
echo "  后端 PID: $!"

echo ""
echo "=== 部署完成 ==="
echo "后端: http://localhost:${SERVER_PORT:-8080}"
echo "Swagger: http://localhost:${SERVER_PORT:-8080}/swagger-ui.html"
echo ""
echo "部署前端到 Nginx:"
echo "  sudo cp -r frontend/dist/* /var/www/html/"
echo "  sudo cp nginx.conf /etc/nginx/conf.d/online-chat.conf"
echo "  sudo nginx -t && sudo systemctl reload nginx"
