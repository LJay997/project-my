# Docker 面试总结

> 覆盖 Docker 核心概念、常用命令、Dockerfile、Docker Compose、网络/存储、及高频面试题

---

## 目录

- [1. Docker 核心概念](#1-docker-核心概念)
- [2. 常用命令速查](#2-常用命令速查)
- [3. Dockerfile 编写](#3-dockerfile-编写)
- [4. Docker Compose](#4-docker-compose)
- [5. 网络与存储](#5-网络与存储)
- [6. 高频面试题](#6-高频面试题)

---

## 1. Docker 核心概念

### 1.1 三大核心：镜像、容器、仓库

```
┌──────────┐    docker build    ┌──────────┐    docker run     ┌──────────┐
│ Dockerfile│ ──────────────────►│  Image   │ ──────────────────►│Container │
└──────────┘                    └────┬─────┘                    └────┬─────┘
                                     │                              │
                                     │ docker push/pull             │ docker commit
                                     ▼                              ▼
                              ┌──────────────┐              ┌──────────────┐
                              │   Registry   │              │   New Image  │
                              │ (Docker Hub) │              └──────────────┘
                              └──────────────┘
```

| 概念      | 说明                                           | 类比          |
| --------- | ---------------------------------------------- | ------------- |
| Image     | 只读模板，包含运行环境和代码                       | 类（Class）    |
| Container | 镜像的运行实例，可读写层                           | 对象（Object） |
| Registry  | 镜像仓库（Docker Hub / 私有仓库）                  | GitHub        |
| Dockerfile| 构建镜像的脚本                                   | Makefile      |

### 1.2 Docker 架构

```
┌─────────────────────────────────────────────┐
│                  Docker Client               │
│              (docker CLI / API)              │
└──────────────────┬──────────────────────────┘
                   │ REST API
                   ▼
┌─────────────────────────────────────────────┐
│                 Docker Daemon                │
│  ┌───────────┐ ┌──────────┐ ┌────────────┐  │
│  │  containerd│ │ runc     │ │  Network   │  │
│  └───────────┘ └──────────┘ └────────────┘  │
└─────────────────────────────────────────────┘
```

### 1.3 与虚拟机的区别

| 对比维度   | Docker 容器            | 虚拟机（VM）           |
| ---------- | ---------------------- | --------------------- |
| 启动速度   | 秒级                    | 分钟级                |
| 资源占用   | MB 级                   | GB 级                 |
| 隔离级别   | 进程级隔离               | 完全隔离               |
| 操作系统   | 共享宿主机内核            | 独立 Guest OS          |
| 迁移性     | 强（镜像小，随处运行）     | 弱（镜像大）           |

---

## 2. 常用命令速查

### 2.1 镜像管理

```bash
# 搜索镜像
docker search nginx

# 拉取镜像
docker pull nginx:latest
docker pull nginx:1.25

# 查看本地镜像
docker images
docker images -a                    # 包含中间层镜像

# 删除镜像
docker rmi nginx:latest
docker rmi $(docker images -q)     # 删除所有镜像

# 导出/导入镜像
docker save -o nginx.tar nginx:latest
docker load -i nginx.tar

# 查看镜像历史
docker history nginx:latest

# 给镜像打标签
docker tag nginx:latest myregistry.com/nginx:v1.0
```

### 2.2 容器管理

```bash
# 运行容器
docker run -d --name my-nginx -p 8080:80 nginx:latest
docker run -it ubuntu /bin/bash                  # 交互模式

# 查看容器
docker ps                                        # 运行中的容器
docker ps -a                                     # 所有容器

# 启停容器
docker start my-nginx
docker stop my-nginx
docker restart my-nginx
docker rm my-nginx                               # 删除已停止容器
docker rm -f my-nginx                            # 强制删除运行中容器

# 进入容器
docker exec -it my-nginx /bin/bash
docker attach my-nginx                           # 附加到主进程

# 查看日志
docker logs -f --tail 100 my-nginx

# 查看容器详情
docker inspect my-nginx

# 查看资源占用
docker stats

# 容器与宿主机文件拷贝
docker cp my-nginx:/etc/nginx/nginx.conf ./
docker cp ./nginx.conf my-nginx:/etc/nginx/

# 清理
docker container prune                           # 清理停止的容器
docker system prune -a                           # 清理所有未使用资源
```

### 2.3 run 常用参数

```bash
docker run \
  -d \                    # 后台运行
  --name my-app \         # 容器名称
  -p 8080:8080 \          # 端口映射（宿主机:容器）
  -v /data:/app/data \    # 挂载卷（宿主机:容器）
  -e JAVA_OPTS="-Xmx512m" \  # 环境变量
  --restart always \      # 重启策略
  --memory 512m \         # 内存限制
  --cpus 1.5 \            # CPU 限制
  my-app:latest
```

---

## 3. Dockerfile 编写

### 3.1 核心指令

| 指令         | 说明                     | 示例                                          |
| ------------ | ------------------------ | --------------------------------------------- |
| `FROM`       | 基础镜像                 | `FROM openjdk:17-slim`                        |
| `WORKDIR`    | 工作目录                 | `WORKDIR /app`                                |
| `COPY`       | 复制文件（推荐）          | `COPY target/app.jar app.jar`                 |
| `ADD`        | 复制+解压（少用）         | `ADD app.tar.gz /app`                         |
| `RUN`        | 构建时执行命令            | `RUN apt-get update && apt-get install -y curl` |
| `ENV`        | 环境变量                 | `ENV JAVA_OPTS="-Xmx512m"`                     |
| `EXPOSE`     | 声明端口（文档用途）       | `EXPOSE 8080`                                 |
| `CMD`        | 容器启动默认命令（可覆盖）  | `CMD ["java", "-jar", "app.jar"]`              |
| `ENTRYPOINT` | 容器入口命令（不可覆盖）   | `ENTRYPOINT ["java", "-jar", "app.jar"]`       |
| `VOLUME`     | 声明匿名卷               | `VOLUME /data`                                |
| `ARG`        | 构建参数                 | `ARG JAR_FILE=app.jar`                        |
| `USER`       | 运行用户                 | `USER nobody`                                 |

### 3.2 Spring Boot 应用 Dockerfile

```dockerfile
# 多阶段构建
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENV JAVA_OPTS="-Xms256m -Xmx512m -XX:+UseG1GC"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 3.3 构建优化技巧

```dockerfile
# 1. 利用缓存层：先复制不常变的文件
COPY pom.xml .
RUN mvn dependency:resolve    # 依赖层先构建，不常变
COPY src ./src                 # 源码层后构建，常变
RUN mvn package

# 2. 减少层数：合并 RUN 命令
RUN apt-get update \
    && apt-get install -y curl vim \
    && rm -rf /var/lib/apt/lists/*

# 3. .dockerignore 排除无关文件
# node_modules/
# target/
# *.log
# .git/
```

---

## 4. Docker Compose

### 4.1 多服务编排

```yaml
# docker-compose.yml
version: '3.8'

services:
  mysql:
    image: mysql:8.0
    container_name: mysql
    environment:
      MYSQL_ROOT_PASSWORD: root123
      MYSQL_DATABASE: mydb
    ports:
      - "3306:3306"
    volumes:
      - mysql_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
      interval: 10s
      timeout: 5s
      retries: 5

  redis:
    image: redis:7-alpine
    container_name: redis
    ports:
      - "6379:6379"
    volumes:
      - redis_data:/data

  app:
    build: .
    container_name: my-app
    ports:
      - "8080:8080"
    environment:
      SPRING_DATASOURCE_URL: jdbc:mysql://mysql:3306/mydb
      SPRING_REDIS_HOST: redis
    depends_on:
      mysql:
        condition: service_healthy
      redis:
        condition: service_started

volumes:
  mysql_data:
  redis_data:
```

### 4.2 常用命令

```bash
docker-compose up -d              # 启动所有服务
docker-compose down               # 停止并删除容器/网络
docker-compose down -v            # 同时删除数据卷
docker-compose ps                 # 查看服务状态
docker-compose logs -f app        # 查看指定服务日志
docker-compose restart app        # 重启指定服务
docker-compose build --no-cache   # 重新构建
docker-compose up -d --scale app=3  # 扩展服务实例
```

---

## 5. 网络与存储

### 5.1 网络模式

| 模式      | 说明                       | 使用场景         |
| --------- | -------------------------- | ---------------- |
| bridge    | 默认桥接网络，容器间通过 IP 通信 | 单机多容器通信     |
| host      | 与宿主机共享网络栈            | 高性能网络需求     |
| none      | 无网络                     | 安全隔离场景       |
| overlay   | 跨主机容器通信（Swarm）       | 集群部署          |

```bash
# 创建自定义网络
docker network create my-net

# 容器加入网络
docker run --network my-net --name app1 my-app
docker run --network my-net --name mysql mysql:8.0

# 同一网络下容器可通过容器名互相访问
# app1 中: ping mysql  ✅
```

### 5.2 数据卷

| 类型     | 说明                       | 命令                                        |
| -------- | -------------------------- | ------------------------------------------- |
| Volume   | 由 Docker 管理（推荐）       | `docker volume create my-vol`               |
| Bind     | 绑定宿主机目录               | `-v /host/path:/container/path`             |
| tmpfs    | 临时内存文件系统             | `--tmpfs /container/path`                   |

```bash
docker volume ls
docker volume inspect my-vol
docker volume prune    # 清理未使用的卷
```

---

## 6. 高频面试题

### 6.1 基础题

**Q1: Docker 镜像分层原理是什么？**

镜像由多个只读层组成，每层对应 Dockerfile 的一条指令。容器启动时在最上层添加可写层。分层的好处：
- **复用**：不同镜像共享相同层，节省磁盘
- **加速**：构建时缓存未变化的层

**Q2: COPY 和 ADD 的区别？**

- `COPY`：仅复制本地文件到容器
- `ADD`：复制 + 自动解压 tar，支持 URL 下载（不推荐，增加构建不确定性）

**Q3: CMD 和 ENTRYPOINT 的区别？**

| 指令         | 作用         | 可被 docker run 覆盖 |
| ------------ | ------------ | -------------------- |
| CMD          | 默认命令     | 是 ✅                |
| ENTRYPOINT   | 入口命令     | 否（需 `--entrypoint`） |

**推荐组合**：`ENTRYPOINT` 定义固定执行程序，`CMD` 定义默认参数

```dockerfile
ENTRYPOINT ["java", "-jar", "app.jar"]
CMD ["--spring.profiles.active=dev"]
# docker run my-app --spring.profiles.active=prod  → 覆盖 CMD
```

**Q4: 如何进入正在运行的容器？**

```bash
docker exec -it <container> /bin/bash   # 推荐，新开终端
docker attach <container>               # 附加到主进程
```

### 6.2 进阶题

**Q5: Docker 如何限制容器资源？**

```bash
docker run --memory 512m --cpus 1.5 my-app
```

或在 compose 中：

```yaml
deploy:
  resources:
    limits:
      cpus: '1.5'
      memory: 512M
```

**Q6: 什么是 Docker 多阶段构建？有什么好处？**

多阶段构建在一个 Dockerfile 中使用多个 `FROM`，将编译环境和运行环境分离：

```dockerfile
FROM maven:3.9 AS build     # 阶段1：编译（大镜像）
RUN mvn package

FROM openjdk:17-slim         # 阶段2：运行（小镜像）
COPY --from=build app.jar .
```

**好处**：最终镜像只包含运行时依赖，减小镜像体积，消除构建工具链的安全风险。

**Q7: 容器退出后如何排查问题？**

```bash
docker logs my-app                    # 查看日志
docker inspect my-app                 # 查看完整配置
docker inspect my-app | grep -A5 State  # 查看退出码
docker logs --tail 50 my-app 2>&1 | grep ERROR
```

**Q8: 如何优化 Docker 镜像大小？**

1. 选择轻量基础镜像（`alpine` / `slim`）
2. 多阶段构建
3. 合并 RUN 命令减少层数
4. 清理安装缓存（`apt-get clean`）
5. 使用 `.dockerignore` 排除无关文件
6. 使用 `jlink` 定制最小 JRE

### 6.3 高级题

**Q9: Docker 和 Kubernetes 的关系？**

- Docker 是**容器引擎**，负责创建和运行容器
- Kubernetes 是**容器编排平台**，负责调度、管理、扩展容器集群
- K8s 1.24+ 已弃用 dockershim，改用 containerd/CRI-O 作为容器运行时

**Q10: 线上排查"容器内 Java 应用 OOM 后被重启"的流程？**

```bash
# 1. 查看容器退出历史
docker ps -a | grep my-app

# 2. 查看容器日志（OOM 前日志）
docker logs --tail 200 my-app

# 3. 查看宿主机 dmesg（OOM Killer 记录）
dmesg | grep -i oom

# 4. 检查资源限制
docker inspect my-app | grep -A5 Memory

# 5. 导出堆转储（如果还在）
docker cp my-app:/app/heap_dump.hprof ./
```

| 排查方向       | 命令                                 |
| -------------- | ------------------------------------ |
| 容器资源限制   | `docker stats` / `docker inspect`    |
| 宿主机 OOM     | `dmesg \| grep OOM`                  |
| JVM 堆设置     | 检查 `-Xmx` 是否超过容器内存限制       |
| 内存泄漏       | `jmap -dump` 后 MAT 分析             |

---

> **最后建议**
> - 掌握 Dockerfile 多阶段构建
> - 理解镜像分层原理和缓存策略
> - 熟悉 docker-compose 编排多服务
> - 了解 K8s 基本概念（Pod/Service/Deployment）