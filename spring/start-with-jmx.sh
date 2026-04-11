#!/bin/bash

# VisualVM 监控配置
# 使用此脚本启动 Spring Boot 应用以支持 VisualVM 监控

JMX_PORT=9090
APP_NAME="spring-demo"

echo "=========================================="
echo "启动 Spring Boot 应用（启用 VisualVM 监控）"
echo "=========================================="
echo "应用名称: $APP_NAME"
echo "JMX 端口: $JMX_PORT"
echo "=========================================="

mvn spring-boot:run \
  -Dspring-boot.run.jvmArguments="\
    -Dcom.sun.management.jmxremote \
    -Dcom.sun.management.jmxremote.port=$JMX_PORT \
    -Dcom.sun.management.jmxremote.authenticate=false \
    -Dcom.sun.management.jmxremote.ssl=false \
    -Djava.rmi.server.hostname=127.0.0.1 \
    -Dcom.sun.management.jmxremote.local.only=true"
