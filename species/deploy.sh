#!/bin/bash
# ============================================
# 物种知识库 - 一键部署脚本
# 用法: bash deploy.sh
# 前置条件: 已配置 SSH 免密登录到服务器
# ============================================
set -e

SERVER="root@47.119.120.200"
REMOTE_DIR="/opt/species"

echo "========================================"
echo "  物种知识库 一键部署"
echo "========================================"
echo ""

# ---- 1. 后端打包 ----
echo "[1/5] 后端打包..."
cd backend
mvn clean package -DskipTests -q
echo "  ✅ species.jar 已生成"
cd ..

# ---- 2. 前端构建 ----
echo "[2/5] 前端构建..."
cd frontend
npm install --silent
npm run build --silent
echo "  ✅ dist/ 已生成"
cd ..

# ---- 3. 上传到服务器 ----
echo "[3/5] 上传文件到服务器..."
ssh "$SERVER" "mkdir -p $REMOTE_DIR/backend $REMOTE_DIR/frontend"

# docker-compose + init.sql
scp docker-compose.yml "$SERVER:$REMOTE_DIR/"
scp init.sql "$SERVER:$REMOTE_DIR/"

# 后端
scp backend/target/species.jar "$SERVER:$REMOTE_DIR/backend/"
scp backend/Dockerfile "$SERVER:$REMOTE_DIR/backend/"

# 前端
scp frontend/Dockerfile "$SERVER:$REMOTE_DIR/frontend/"
scp frontend/nginx.conf "$SERVER:$REMOTE_DIR/frontend/"
scp -r frontend/dist "$SERVER:$REMOTE_DIR/frontend/"
echo "  ✅ 上传完成"

# ---- 4. 服务器上重新构建并启动 ----
echo "[4/5] 在服务器上启动 Docker 服务..."
ssh "$SERVER" "cd $REMOTE_DIR && docker compose up -d --build"
echo "  ✅ 服务已启动"

# ---- 5. 验证 ----
echo "[5/5] 验证服务状态..."
echo ""
ssh "$SERVER" "cd $REMOTE_DIR && docker compose ps"
echo ""
echo "查看后端日志: ssh $SERVER 'docker compose -f $REMOTE_DIR/docker-compose.yml logs --tail=30 backend'"
echo ""

echo "========================================"
echo "  部署完成！"
echo "  访问地址: http://47.119.120.200"
echo "  管理员: admin / admin123"
echo "========================================"
