#!/bin/bash

# 动态线程池 - 快速启动脚本
# 使用前请确保 Redis 服务已启动

echo "======================================"
echo "  动态线程池 - 快速启动脚本"
echo "======================================"

# 检查 Redis 是否运行
echo "检查 Redis 服务..."
redis_ping=$(redis-cli ping 2>&1)
if [[ "$redis_ping" == "PONG" ]]; then
    echo "✓ Redis 服务运行正常"
elif [[ "$redis_ping" == *"NOAUTH"* ]]; then
    echo "⚠ Redis 需要密码认证，请设置 REDIS_PASSWORD 环境变量"
    echo "  export REDIS_PASSWORD=your_password"
    exit 1
else
    echo "✗ Redis 服务未运行"
    exit 1
fi

# 进入项目目录
cd "$(dirname "$0")"

# 构建项目
echo ""
echo "构建项目..."
mvn clean install -DskipTests -q
if [ $? -ne 0 ]; then
    echo "✗ 构建失败"
    exit 1
fi
echo "✓ 构建完成"

# 启动 Admin 后端
echo ""
echo "启动 Admin 后端服务（端口 8089）..."
cd dynamic-thread-pool-admin
nohup mvn spring-boot:run -Dspring-boot.run.profiles=dev > ../logs/admin.log 2>&1 &
ADMIN_PID=$!
echo "✓ Admin 后端已启动 (PID: $ADMIN_PID)"

# 启动 Test 应用
echo ""
echo "启动 Test 应用（端口 8093）..."
cd ../atluofu-dynamic-thread-pool-test
nohup mvn spring-boot:run -Dspring-boot.run.profiles=dev > ../logs/test-app.log 2>&1 &
TEST_PID=$!
echo "✓ Test 应用已启动 (PID: $TEST_PID)"

# 启动前端
echo ""
echo "启动前端服务（端口 3000）..."
cd ../atluofu-dynamic-thread-pool-ui
nohup npm run dev > ../logs/frontend.log 2>&1 &
FRONTEND_PID=$!
echo "✓ 前端服务已启动 (PID: $FRONTEND_PID)"

# 等待服务启动
echo ""
echo "等待服务启动..."
sleep 15

# 检查服务状态
echo ""
echo "检查服务状态..."
if curl -s http://localhost:8089/api/v1/dtp/apps > /dev/null 2>&1; then
    echo "✓ Admin 后端服务正常"
else
    echo "⚠ Admin 后端服务可能还未完全启动"
fi

if curl -s http://localhost:3000 > /dev/null 2>&1; then
    echo "✓ 前端服务正常"
else
    echo "⚠ 前端服务可能还未完全启动"
fi

echo ""
echo "======================================"
echo "  服务启动完成！"
echo "======================================"
echo ""
echo "访问地址："
echo "  - 前端管理界面：http://localhost:3000"
echo "  - Admin API:     http://localhost:8089"
echo "  - Test 应用：     http://localhost:8093"
echo ""
echo "日志文件："
echo "  - logs/admin.log"
echo "  - logs/test-app.log"
echo "  - logs/frontend.log"
echo ""
echo "停止服务：pkill -f 'dynamic-thread'"
echo "======================================"
