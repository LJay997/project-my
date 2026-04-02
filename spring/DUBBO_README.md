# Spring + Dubbo 演示组件

这是一个基于 Spring Boot + Apache Dubbo 的 RPC 服务演示组件。

## 技术栈

- **Spring Boot**: 2.7.18
- **Apache Dubbo**: 3.2.0
- **注册中心**: Nacos 2.2.0 (支持 Zookeeper)
- **协议**: Dubbo 协议

## 项目结构

```
com.qq.ijay997.dubbo/
├── api/                        # Dubbo API 接口定义
│   └── UserService.java       # 用户服务接口
├── provider/                   # 服务提供者实现
│   └── UserServiceImpl.java   # 用户服务实现类
├── consumer/                   # 服务消费者
│   └── DubboConsumerController.java  # REST 控制器
├── demo/                       # 演示示例
│   └── DubboDemoController.java      # 演示控制器
└── config/                     # 配置类
    ├── DubboConfig.java              # Nacos 配置
    ├── DubboZookeeperConfig.java     # Zookeeper 配置
    ├── DubboDirectConfig.java        # 直连模式配置
    └── DubboConsumerConfig.java      # 消费者配置
```

## 快速开始

### 前置要求

1. **JDK 8+**
2. **Nacos 服务器** (或使用 Zookeeper/直连模式)
   - 下载 Nacos: https://github.com/alibaba/nacos/releases
   - 启动命令 (Linux/Mac): `sh bin/startup.sh -m standalone`
   - 启动命令 (Windows): `cmd/bin/startup.cmd -m standalone`

### 方式一：使用 Nacos (推荐)

1. 确保 Nacos 已启动 (默认端口 8848)
2. 应用会自动连接到 Nacos 注册中心
3. 启动 Spring Boot 应用

### 方式二：使用 Zookeeper

1. 启动 Zookeeper: `zkServer.sh start`
2. 激活 profile: `--spring.profiles.active=zookeeper`
3. 或在 application.properties 中设置:
   ```properties
   spring.profiles.active=zookeeper
   ```

### 方式三：直连模式 (无需注册中心)

适合本地开发测试，不需要额外的中间件：

1. 激活 profile: `--spring.profiles.active=direct`
2. 服务提供者和消费者直接通过 IP:Port 通信

## API 端点

### 1. 基本问候
```bash
GET http://localhost:8080/api/dubbo/hello?userName=张三
```

响应示例:
```
Hello, 张三！欢迎使用 Dubbo!
```

### 2. 获取用户名称
```bash
GET http://localhost:8080/api/dubbo/user/name?userId=123
```

响应示例:
```
用户_123
```

### 3. 加法计算
```bash
GET http://localhost:8080/api/dubbo/add?a=10&b=20
```

响应示例:
```
30
```

### 4. 演示示例

#### 基本演示
```bash
GET http://localhost:8080/api/dubbo/demo/basic?name=李四
```

#### 获取用户
```bash
GET http://localhost:8080/api/dubbo/demo/user?userId=456
```

#### 远程计算
```bash
GET http://localhost:8080/api/dubbo/demo/calculate?a=100&b=200
```

#### 组合调用
```bash
GET http://localhost:8080/api/dubbo/demo/combined
```

#### 性能测试
```bash
GET http://localhost:8080/api/dubbo/demo/performance
```

## 配置说明

### Nacos 配置 (默认)

```properties
dubbo.application.name=spring-dubbo-demo
dubbo.registry.address=nacos://127.0.0.1:8848
dubbo.protocol.name=dubbo
dubbo.protocol.port=20880
```

### Zookeeper 配置

```properties
dubbo.application.name=spring-dubbo-demo-zk
dubbo.registry.address=zookeeper://127.0.0.1:2181
dubbo.protocol.name=dubbo
dubbo.protocol.port=20881
```

### 直连模式配置

```properties
dubbo.application.name=spring-dubbo-demo-direct
dubbo.protocol.name=dubbo
dubbo.protocol.port=20882
```

在代码中使用直连地址:
```java
@DubboReference(url = "dubbo://127.0.0.1:20882")
private UserService userService;
```

## 核心注解说明

### @DubboService (服务提供者)

```java
@DubboService(
    interfaceClass = UserService.class,  // 接口类
    timeout = 5000,                       // 超时时间 (ms)
    retries = 1,                          // 重试次数
    loadbalance = "roundrobin"           // 负载均衡策略
)
public class UserServiceImpl implements UserService {
    // ...
}
```

### @DubboReference (服务消费者)

```java
@DubboReference(
    interfaceClass = UserService.class,  // 接口类
    timeout = 5000,                       // 超时时间 (ms)
    check = false,                        // 启动时是否检查依赖
    lazy = true,                          // 是否延迟连接
    url = "dubbo://127.0.0.1:20880"      // 直连地址 (可选)
)
private UserService userService;
```

## Dubbo 高级特性

### 1. 负载均衡策略

在消费者端配置:
```java
@DubboReference(loadbalance = "roundrobin")  // 轮询 (默认)
@DubboReference(loadbalance = "random")      // 随机
@DubboReference(loadbalance = "leastactive") // 最少活跃调用数
@DubboReference(loadbalance = "consistenthash") // 一致性哈希
```

### 2. 超时与重试

```java
@DubboReference(
    timeout = 5000,    // 超时时间 5 秒
    retries = 1        // 重试 1 次 (失败后)
)
```

### 3. 异步调用

```java
@DubboReference(async = true)
private UserService userService;

// 使用 CompletableFuture
CompletableFuture<String> future = CompletableFuture
    .supplyAsync(() -> userService.sayHello("张三"));
```

### 4. 集群容错

```java
@DubboReference(cluster = "failover")     // 失败切换 (默认)
@DubboReference(cluster = "failfast")     // 快速失败
@DubboReference(cluster = "failsafe")     // 失败安全
@DubboReference(cluster = "failback")     // 失败自动恢复
```

## 运行测试

### 单元测试
```bash
cd spring
mvn test -Dtest=DubboServiceTest
```

### 集成测试
```bash
mvn test
```

## 监控与管理

### 1. Dubbo Admin

访问 Dubbo 控制台管理界面:
```
http://localhost:8080/actuator/dubbo
```

### 2. QoS 命令行

启用 QoS 后可以 telnet 连接:
```bash
telnet localhost 22222
ls          # 查看服务列表
ps          # 查看服务属性
invoke        # 调用方法
```

### 3. 日志监控

在 application.properties 中开启详细日志:
```properties
logging.level.org.apache.dubbo=DEBUG
```

## 常见问题

### Q1: 启动时报 "No provider available" 错误

**解决方案:**
1. 检查服务提供者是否启动
2. 检查注册中心连接是否正常
3. 临时关闭检查：`dubbo.consumer.check=false`

### Q2: 如何切换到 Zookeeper?

**方案:**
1. 添加 Zookeeper 依赖
2. 修改配置：`dubbo.registry.address=zookeeper://127.0.0.1:2181`
3. 激活 profile: `zookeeper`

### Q3: 本地开发不想启动 Nacos/Zookeeper 怎么办？

**方案:**
使用直连模式，不需要注册中心:
```java
@DubboReference(url = "dubbo://127.0.0.1:20880")
```

### Q4: 如何配置多个注册中心？

```java
@Bean
public RegistryConfig registryConfig1() {
    RegistryConfig registryConfig = new RegistryConfig();
    registryConfig.setId("nacos-registry");
    registryConfig.setAddress("nacos://127.0.0.1:8848");
    return registryConfig;
}

@Bean
public RegistryConfig registryConfig2() {
    RegistryConfig registryConfig = new RegistryConfig();
    registryConfig.setId("zk-registry");
    registryConfig.setAddress("zookeeper://127.0.0.1:2181");
    return registryConfig;
}
```

## 扩展练习

建议尝试以下扩展:

1. ✅ 实现一个完整的 CRUD 服务
2. ✅ 配置服务降级策略
3. ✅ 实现服务限流
4. ✅ 添加 Dubbo Filter
5. ✅ 集成 Sentinel 做熔断限流
6. ✅ 使用泛化调用
7. ✅ 实现自定义负载均衡策略
8. ✅ 配置多协议支持

## 参考资料

- [Apache Dubbo 官方文档](https://dubbo.apache.org/zh/docs/)
- [Spring Boot 官方文档](https://spring.io/projects/spring-boot)
- [Nacos 官方文档](https://nacos.io/zh-cn/docs/quick-start.html)

---

**作者**: ijay997  
**创建时间**: 2026-03-30  
**更新时间**: 2026-03-30
