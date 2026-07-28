#!/bin/bash

# Spring Boot CPU 测试启动脚本
# 适用 JDK 21 (GraalVM)
# 使用方法:
#   ./start.sh              # 基础模式启动
#   ./start.sh jmx          # 启用 JMX 监控（VisualVM）
#   ./start.sh cpu          # CPU测试模式
#   ./start.sh gc           # GC测试模式
#   ./start.sh stop         # 停止应用

MODE=${1:-basic}
PID_FILE=".app.pid"
LOG_DIR="./logs"
mkdir -p "$LOG_DIR"

stop_app() {
    if [ -f "$PID_FILE" ]; then
        PID=$(cat "$PID_FILE")
        if kill -0 "$PID" 2>/dev/null; then
            echo "停止应用 (PID: $PID)..."
            kill "$PID" 2>/dev/null
            sleep 2
        fi
        rm -f "$PID_FILE"
    fi
    local PIDS=$(lsof -ti:8082 2>/dev/null)
    if [ -n "$PIDS" ]; then
        echo "清理端口 8082..."
        echo "$PIDS" | xargs kill -9 2>/dev/null
    fi
    echo "已停止"
}

start_app() {
    local JVM_ARGS="$1"
    local MAVEN_ARGS="$2"
    local APP_LOG="$LOG_DIR/app-$MODE.log"
    
    echo "============================================"
    echo " Spring Boot CPU 测试启动脚本"
    echo "============================================"
    echo "模式: $MODE"
    echo "JVM参数: $JVM_ARGS"
    echo "日志: $APP_LOG"
    echo "============================================"
    
    nohup mvn spring-boot:run \
        "-Dspring-boot.run.jvmArguments=$JVM_ARGS" \
        $MAVEN_ARGS \
        > "$APP_LOG" 2>&1 &
    
    local APP_PID=$!
    echo $APP_PID > "$PID_FILE"
    echo "启动中 (PID: $APP_PID) ..."
    
    for i in {1..30}; do
        sleep 2
        if curl -s http://localhost:8082/cpu/status > /dev/null 2>&1; then
            echo ""
            echo "============================================"
            echo "✅ 应用启动成功！"
            echo "   地址: http://localhost:8082"
            echo "============================================"
            echo ""
            echo "📊 CPU 测试接口:"
            echo "   /cpu/loop?threads=N&duration=S   # 死循环测试"
            echo "   /cpu/busy?threads=N&duration=S    # 密集计算"
            echo "   /cpu/gc?rounds=N&count=N          # GC压力"
            echo "   /cpu/lock?threads=N&duration=S    # 锁竞争"
            echo "   /cpu/stop                          # 停止测试"
            echo ""
            echo "🔍 查看日志: tail -f $APP_LOG"
            echo "🛑 停止应用: ./start.sh stop"
            echo ""
            echo "💡 快速测试:"
            echo "   curl 'http://localhost:8082/cpu/loop?threads=10&duration=30'"
            return 0
        fi
        if ! kill -0 "$APP_PID" 2>/dev/null; then
            echo "⚠️  进程已退出，查看日志:"
            tail -30 "$APP_LOG"
            return 1
        fi
        echo "等待中... ($i/30)"
    done
    
    echo "⚠️  启动超时"
    tail -20 "$APP_LOG"
    return 1
}

case "$MODE" in
    stop)
        stop_app
        ;;
    jmx)
        start_app \
            "-Xms512m -Xmx2048m -Xlog:gc*:file=${LOG_DIR}/gc-jmx.log:time,level,tags -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.port=9090 -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false -Djava.rmi.server.hostname=127.0.0.1 -Dcom.sun.management.jmxremote.local.only=true" \
            ""
        echo ""
        echo "💡 VisualVM: jvisualvm → 添加 JMX: localhost:9090"
        ;;
    cpu)
        start_app \
            "-Xms2048m -Xmx2048m -Xmn512m -XX:+UseG1GC -XX:MaxGCPauseMillis=200 -Xlog:gc*:file=${LOG_DIR}/gc-cpu.log:time,level,tags -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_DIR}/heapdump-cpu.hprof" \
            ""
        ;;
    gc)
        start_app \
            "-Xms128m -Xmx256m -Xmn64m -Xlog:gc*:file=${LOG_DIR}/gc-test.log:time,level,tags -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_DIR}/heapdump-gc.hprof" \
            ""
        ;;
    prod)
        start_app \
            "-Xms4096m -Xmx4096m -Xmn2048m -XX:+UseG1GC -XX:MaxGCPauseMillis=100 -Xlog:gc*:file=${LOG_DIR}/gc-prod.log:time,level,tags -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=${LOG_DIR}/heapdump-prod.hprof" \
            "--spring.profiles.active=prod"
        ;;
    *)
        start_app \
            "-Xms512m -Xmx1024m -Xmn256m -Xlog:gc*:file=${LOG_DIR}/gc-basic.log:time,level,tags" \
            ""
        ;;
esac