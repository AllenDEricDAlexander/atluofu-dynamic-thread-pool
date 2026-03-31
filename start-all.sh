#!/bin/bash

# 动态线程池 - 完整启动脚本

echo "======================================"
echo "  动态线程池 - 启动所有服务"
echo "======================================"

cd /Users/mario/SelfProject/be/atluofu-dynamic-thread-pool

# 创建日志目录
mkdir -p logs

# 停止旧进程
echo "停止旧进程..."
pkill -f "dynamic-thread-pool-admin" 2>/dev/null
pkill -f "dynamic-thread-pool-test" 2>/dev/null
pkill -f "vite" 2>/dev/null
sleep 2

# 启动 Admin 后端
echo ""
echo "=== 启动 Admin 后端 (端口 8089) ==="
java -jar dynamic-thread-pool-admin/target/dynamic-thread-pool-admin-app.jar --spring.profiles.active=dev > logs/admin.log 2>&1 &
ADMIN_PID=$!
echo "Admin PID: $ADMIN_PID"

# 启动 Test 应用
echo ""
echo "=== 启动 Test 应用 (端口 8093) ==="
java -jar atluofu-dynamic-thread-pool-test/target/dynamic-thread-pool-test-app.jar --spring.profiles.active=dev > logs/test-app.log 2>&1 &
TEST_PID=$!
echo "Test PID: $TEST_PID"

# 启动前端
echo ""
echo "=== 启动前端 (端口 3000) ==="
cd atluofu-dynamic-thread-pool-ui
npm run dev > ../logs/frontend.log 2>&1 &
UI_PID=$!
echo "Frontend PID: $UI_PID"

# 等待启动
echo ""
echo "等待服务启动 (45 秒)..."
sleep 45

# 检查服务状态
echo ""
echo "======================================"
echo "  服务状态检查"
echo "======================================"

# 检查前端
if curl -s http://localhost:3000 > /dev/null; then
    echo "✅ 前端 (3000): 运行正常"
else
    echo "❌ 前端 (3000): 未运行"
fi

# 检查 Admin 后端
if curl -s http://localhost:8089/api/v1/dynamic/thread/pool/query_thread_pool_list > /dev/null; then
    echo "✅ Admin 后端 (8089): 运行正常"
else
    echo "❌ Admin 后端 (8089): 未运行"
fi

# 检查 Test 应用
if curl -s http://localhost:8093 > /dev/null; then
    echo "✅ Test 应用 (8093): 运行正常"
else
    echo "❌ Test 应用 (8093): 未运行"
fi

# 显示线程池数据
echo ""
echo "======================================"
echo "  当前线程池数据"
echo "======================================"
curl -s http://localhost:8089/api/v1/dynamic/thread/pool/query_thread_pool_list 2>&1 | python3 -c "
import sys,json
try:
    d=json.load(sys.stdin)
    print(f'线程池数量：{len(d.get(\"data\", []))}')
    for item in d.get('data', []):
        print(f'  - {item[\"threadPoolName\"]}: 核心={item[\"corePoolSize\"]}, 最大={item[\"maximumPoolSize\"]}')
except Exception as e:
    print(f'数据获取失败：{e}')
" 2>/dev/null

echo ""
echo "======================================"
echo "  访问地址"
echo "======================================"
echo ""
echo "🌐 前端管理界面：http://localhost:3000"
echo ""
echo "📝 日志文件:"
echo "   - logs/admin.log"
echo "   - logs/test-app.log"
echo "   - logs/frontend.log"
echo ""
echo "🛑 停止服务：pkill -f 'dynamic-thread'"
echo "======================================"
