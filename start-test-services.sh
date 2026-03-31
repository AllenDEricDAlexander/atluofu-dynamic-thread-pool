#!/bin/bash
#
# start-test-services.sh
# 启动两个不同应用名的动态线程池测试服务，供 Admin 后台管理平台监控测试
#
# 服务列表：
#   - test-app    : dynamic-thread-pool-test-app    (端口 8093)
#   - test-app2   : dynamic-thread-pool-test-app2   (端口 8094)
#
# 依赖：Redis（默认 127.0.0.1:6379，密码 HomeLab666）
#
# 用法：
#   ./start-test-services.sh       # 前台启动（Ctrl+C 停止）
#   ./start-test-services.sh &     # 后台启动
#   ./start-test-services.sh stop  # 停止服务
#

# ========== 配置 ==========
JAR_FILE="dynamic-thread-pool-test-app.jar"
PROJECT_DIR="$(cd "$(dirname "$0")/atluofu-dynamic-thread-pool-test" && pwd)"
JAR_PATH="${PROJECT_DIR}/target/${JAR_FILE}"

REDIS_HOST="127.0.0.1"
REDIS_PORT="6379"
REDIS_PASSWORD="HomeLab666"
JVM_OPTS="-Xms256m -Xmx512m"

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
PID_DIR="${SCRIPT_DIR}/.test-pids"
LOG_DIR="${SCRIPT_DIR}/logs/test-services"

mkdir -p "$PID_DIR" "$LOG_DIR"

# 服务配置：名称 | 应用名 | 端口 | 核心线程数 | 最大线程数
declare -a APP_SERVICES=(
    "test-app-1:dynamic-thread-pool-test-app:8093:20:50"
    "test-app-2:dynamic-thread-pool-test-app2:8094:15:40"
)

# ========== 停止所有服务 ==========
stop_all() {
    echo "========== 停止测试服务 =========="
    for svc in "${APP_SERVICES[@]}"; do
        IFS=':' read -r name _ _ _ _ <<< "$svc"
        pid_file="${PID_DIR}/${name}.pid"
        if [[ -f "$pid_file" ]]; then
            pid=$(cat "$pid_file")
            if kill -0 "$pid" 2>/dev/null; then
                echo "停止 ${name} (PID: $pid)..."
                kill "$pid" 2>/dev/null
                sleep 1
                kill -9 "$pid" 2>/dev/null || true
            fi
            rm -f "$pid_file"
        else
            echo "${name} 未运行"
        fi
    done
    echo "已停止"
}

# ========== 检查 Redis ==========
check_redis() {
    echo "========== 检查 Redis =========="
    if command -v redis-cli &>/dev/null; then
        result=$(redis-cli -h "$REDIS_HOST" -p "$REDIS_PORT" -a "$REDIS_PASSWORD" ping 2>/dev/null)
        if [[ "$result" == "PONG" ]]; then
            echo "✅ Redis 连接正常 (${REDIS_HOST}:${REDIS_PORT})"
        else
            echo "❌ Redis 连接失败"
            exit 1
        fi
    else
        echo "⚠️  redis-cli 未安装，跳过检查"
    fi
    echo ""
}

# ========== 构建 ==========
build_project() {
    echo "========== 构建测试应用 =========="
    cd "$PROJECT_DIR"
    mvn clean package -DskipTests -q
    if [[ ! -f "target/${JAR_FILE}" ]]; then
        echo "❌ 构建失败"
        exit 1
    fi
    echo "✅ 构建成功: ${JAR_PATH}"
    echo ""
}

# ========== 启动单个服务 ==========
start_service() {
    local name="$1"
    local app_name="$2"
    local port="$3"
    local core_pool="$4"
    local max_pool="$5"

    local pid_file="${PID_DIR}/${name}.pid"
    local log_file="${LOG_DIR}/${name}.log"

    if [[ -f "$pid_file" ]]; then
        local old_pid=$(cat "$pid_file")
        if kill -0 "$old_pid" 2>/dev/null; then
            echo "⚠️  ${name} 已在运行 (PID: $old_pid)，跳过"
            return
        fi
        rm -f "$pid_file"
    fi

    echo "🚀 启动 ${name} (appName=${app_name}, port=${port}, corePool=${core_pool}, maxPool=${max_pool})..."

    nohup java ${JVM_OPTS} \
        -Dspring.application.name="${app_name}" \
        -Dserver.port="${port}" \
        -Dspring.profiles.active=dev \
        -Dthread.pool.executor.config.core-pool-size="${core_pool}" \
        -Dthread.pool.executor.config.max-pool-size="${max_pool}" \
        -Ddynamic.thread.pool.config.host="${REDIS_HOST}" \
        -Ddynamic.thread.pool.config.port="${REDIS_PORT}" \
        -Ddynamic.thread.pool.config.password="${REDIS_PASSWORD}" \
        -jar "${JAR_PATH}" \
        > "$log_file" 2>&1 &

    local pid=$!
    echo "$pid" > "$pid_file"
    echo "$port" > "${PID_DIR}/${name}.port"
    echo "   PID: ${pid}，日志: ${log_file}"
}

# ========== 显示状态 ==========
show_status() {
    echo ""
    echo "========== 服务状态 =========="
    local running=0
    for svc in "${APP_SERVICES[@]}"; do
        IFS=':' read -r name app_name port _ _ <<< "$svc"
        local pid_file="${PID_DIR}/${name}.pid"
        if [[ -f "$pid_file" ]]; then
            local pid=$(cat "$pid_file")
            if kill -0 "$pid" 2>/dev/null; then
                echo "  ✅ ${name} | appName=${app_name} | port=${port} | PID=${pid}"
                ((running++))
            else
                echo "  ❌ ${name} | 已退出"
            fi
        else
            echo "  ⏸️  ${name} | 未启动"
        fi
    done
    echo ""
    echo "   Admin 后台: http://localhost:8089"
    echo "   前端 UI:    http://localhost:3000"
    echo ""
    if [[ $running -eq ${#APP_SERVICES[@]} ]]; then
        echo "✅ 全部服务运行正常（${running}/${#APP_SERVICES[@]}）"
    else
        echo "⚠️  部分服务未运行 (${running}/${#APP_SERVICES[@]})"
    fi
}

# ========== 主流程 ==========
case "${1:-start}" in
    stop)
        stop_all
        exit 0
        ;;
    status)
        check_redis
        show_status
        exit 0
        ;;
    start|*)
        ;;
esac

echo "========================================"
echo "  动态线程池 - 测试服务启动脚本"
echo "========================================"
echo ""

check_redis
build_project

echo "========== 启动测试服务 =========="
for svc in "${APP_SERVICES[@]}"; do
    IFS=':' read -r name app_name port core_pool max_pool <<< "$svc"
    start_service "$name" "$app_name" "$port" "$core_pool" "$max_pool"
done

sleep 3
show_status

echo "========================================"
echo "按 Ctrl+C 停止所有服务，或使用: ./start-test-services.sh stop"
echo "========================================"

# 前台模式：等待 Ctrl+C
while true; do
    sleep 5
    all_dead=true
    for svc in "${APP_SERVICES[@]}"; do
        IFS=':' read -r name _ _ _ _ <<< "$svc"
        pid_file="${PID_DIR}/${name}.pid"
        if [[ -f "$pid_file" ]]; then
            if kill -0 "$(cat "$pid_file")" 2>/dev/null; then
                all_dead=false
            fi
        fi
    done
    if $all_dead; then
        echo "所有测试服务已退出"
        exit 0
    fi
done
