#!/bin/bash
# 书染美术 - 一键部署脚本
# 用法: ./deploy.sh
# 需要输入服务器密码（SSH）

set -e

SERVER="ubuntu@111.231.24.51"
REMOTE_DIR="/home/ubuntu/shuranArt"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "========================================="
echo "  书染美术 部署脚本"
echo "  服务器: $SERVER"
echo "  域名: tianma.chat"
echo "========================================="

# Step 1: 构建后端 JAR
echo ""
echo "[1/4] 构建后端 JAR 包..."
cd "$PROJECT_DIR/backend"
JAVA_HOME=$(/usr/libexec/java_home) mvn clean package -DskipTests -q
if [ ! -f target/art-backend-1.0.0.jar ]; then
  echo "❌ JAR 构建失败！"
  exit 1
fi
echo "✅ JAR 构建成功: $(du -h target/art-backend-1.0.0.jar | cut -f1)"

# Step 2: 同步文件到服务器
echo ""
echo "[2/4] 同步文件到服务器..."
echo "  (需要输入服务器密码)"

# 确保远程目录存在
ssh "$SERVER" "mkdir -p $REMOTE_DIR/{backend/target,nginx/ssl,mysql,miniprogram/images}"

# 同步项目文件（排除不需要的）
rsync -avz --progress \
  --exclude '.git' \
  --exclude 'node_modules' \
  --exclude 'backend/target' \
  --exclude 'backend/src' \
  --exclude 'backend/.mvn' \
  --exclude 'miniprogram/images/_backup' \
  --exclude 'hhspec' \
  --exclude '.mcp.json' \
  --exclude '*.md' \
  "$PROJECT_DIR/" "$SERVER:$REMOTE_DIR/"

# 单独上传 JAR（比较大，单独传方便看进度）
echo ""
echo "  上传 JAR 包..."
rsync -avz --progress \
  "$PROJECT_DIR/backend/target/art-backend-1.0.0.jar" \
  "$SERVER:$REMOTE_DIR/backend/target/"

# Step 3: 在服务器上重启服务
echo ""
echo "[3/4] 重启服务..."
ssh "$SERVER" "cd $REMOTE_DIR && sudo docker compose down && sudo docker compose up -d --build"

# Step 4: 验证服务状态
echo ""
echo "[4/4] 验证服务状态..."
sleep 5
ssh "$SERVER" "cd $REMOTE_DIR && sudo docker compose ps"

echo ""
echo "========================================="
echo "  ✅ 部署完成！"
echo "  API: https://tianma.chat/api/"
echo "  图片: https://tianma.chat/images/"
echo "========================================="
