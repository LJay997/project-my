# Redisson 配置与使用指南

## 📦 已添加的依赖

### 1. Maven 依赖（pom.xml）

```xml
<!-- Redisson - Redis Java 客户端 -->
<dependency>
    <groupId>org.redisson</groupId>
    <artifactId>redisson-spring-boot-starter</artifactId>
    <version>3.27.0</version>
</dependency>

<!-- Spring Boot Starter Data Redis -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<!-- Hutool 工具类库 -->
<dependency>
    <groupId>cn.hutool</groupId>
    <artifactId>hutool-all</artifactId>
    <version>5.8.25</version>
</dependency>
```

### 2. 配置文件（application.properties）

```properties
# Redis 配置（Redisson 使用）
spring.redis.host=127.0.0.1
spring.redis.port=6379
spring.redis.password=
spring.redis.database=0
spring.redis.timeout=3000ms
```

## 🔧 核心组件

### 1. RedissonConfig.java
Redisson 客户端配置类，支持：
- ✅ 单机模式（默认）
- ✅ 密码认证
- ✅ 连接池配置
- ✅ 自定义数据库索引

### 2. AccessLimitAspect.java
基于 AOP 的限流切面，功能：
- ✅ 分布式计数器（RAtomicLong）
- ✅ 自动过期机制
- ✅ 灵活的限流策略配置

### 3. LimitException.java
限流异常类，当访问超过限制时抛出。

### 4. GlobalExceptionHandler.java
全局异常处理器，统一返回友好的错误信息。

## 🚀 使用方法

### 基本用法

在需要限流的方法上添加 `@AccessLimit` 注解：

```java
@GetMapping("/api/users")
@AccessLimit(
    preKey = "get_users",           // Redis key 前缀
    times = 60,                      // 时间窗口（秒）
    maxCount = 10,                   // 最大访问次数
    timeUnit = TimeUnit.SECONDS,     // 时间单位
    msg = "访问过于频繁，请稍后再试"  // 超限提示消息
)
public ResponseEntity<List<User>> getUsers() {
    return userService.findAll();
}
```

### 注解参数说明

| 参数 | 类型 | 说明 | 示例 |
|------|------|------|------|
| preKey | String | Redis key 的前缀标识 | "get_users" |
| times | int | 时间窗口大小 | 60 |
| maxCount | int | 允许的最大访问次数 | 10 |
| timeUnit | TimeUnit | 时间单位 | SECONDS, MINUTES, HOURS |
| msg | String | 限流时的提示消息 | "访问过于频繁" |

### 实际示例

```java
// 示例 1: 每分钟最多访问 5 次
@AccessLimit(preKey = "search", times = 1, maxCount = 5, 
             timeUnit = TimeUnit.MINUTES, msg = "搜索太频繁了")
public SearchResult search(String keyword) { ... }

// 示例 2: 每小时最多提交 10 次表单
@AccessLimit(preKey = "submit_form", times = 1, maxCount = 10, 
             timeUnit = TimeUnit.HOURS, msg = "提交次数已达上限")
public void submitForm(FormData data) { ... }

// 示例 3: 每秒最多调用 100 次（高并发接口）
@AccessLimit(preKey = "api_call", times = 1, maxCount = 100, 
             timeUnit = TimeUnit.SECONDS, msg = "请求过多")
public ApiResponse apiCall() { ... }
```

## 🧪 测试限流功能

### 1. 启动 Redis

```bash
# macOS (使用 Homebrew)
brew install redis
brew services start redis

# 或者直接运行
redis-server
```

### 2. 启动 Spring Boot 应用

```bash
cd spring
mvn spring-boot:run
```

### 3. 测试接口

```bash
# 正常访问（前 5 次成功）
curl http://localhost:8082/api/users

# 第 6 次访问会返回 429 错误
{
    "code": 429,
    "message": "访问过于频繁，请稍后再试",
    "success": false
}
```

## 📊 Redisson 高级用法

### 1. 直接使用 RedissonClient

```java
@Autowired
private RedissonClient redissonClient;

// 分布式锁
RLock lock = redissonClient.getLock("myLock");
lock.lock();
try {
    // 业务逻辑
} finally {
    lock.unlock();
}

// 分布式 Map
RMap<String, Object> map = redissonClient.getMap("myMap");
map.put("key", "value");

// 分布式队列
RQueue<String> queue = redissonClient.getQueue("myQueue");
queue.add("item");
```

### 2. 其他数据结构

```java
// 原子长整型（用于计数器）
RAtomicLong counter = redissonClient.getAtomicLong("counter");
counter.incrementAndGet();

// 布隆过滤器
RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("bloom");
bloomFilter.tryInit(1000000L, 0.01);
bloomFilter.add("data");

// 发布订阅
RTopic topic = redissonClient.getTopic("myTopic");
topic.addListener(Message.class, (channel, msg) -> {
    System.out.println("收到消息: " + msg);
});
```

## ⚙️ 配置优化

### 调整连接池大小

在 `RedissonConfig.java` 中修改：

```java
config.useSingleServer()
    .setConnectionPoolSize(64)        // 连接池大小
    .setConnectionMinimumIdleSize(24)  // 最小空闲连接
    .setIdleConnectionTimeout(10000)   // 空闲超时
    .setConnectTimeout(3000)           // 连接超时
    .setTimeout(3000);                 // 命令超时
```

### 集群模式配置

如果需要支持 Redis 集群，修改 `RedissonConfig.java`：

```java
Config config = new Config();
config.useClusterServers()
    .addNodeAddress("redis://127.0.0.1:7001")
    .addNodeAddress("redis://127.0.0.1:7002")
    .addNodeAddress("redis://127.0.0.1:7003");
```

## 🔍 常见问题

### Q1: Redis 连接失败？
**A:** 检查以下几点：
1. Redis 服务是否启动：`redis-cli ping` 应返回 `PONG`
2. 配置文件中的 host 和 port 是否正确
3. 防火墙是否阻止了 6379 端口

### Q2: 限流不生效？
**A:** 确认：
1. `@EnableAspectJAutoProxy` 是否启用（Spring Boot 默认启用）
2. 方法是否是 public 的
3. 是否通过 Spring 容器调用（自调用不会触发 AOP）

### Q3: 如何查看 Redis 中的限流数据？

```bash
redis-cli
> KEYS ACCESS_LIMIT_LOCK_KEY:*
> GET ACCESS_LIMIT_LOCK_KEY:get_all_users:xxx
> TTL ACCESS_LIMIT_LOCK_KEY:get_all_users:xxx
```

## 📝 注意事项

1. **生产环境建议**：
   - 设置 Redis 密码
   - 启用 TLS/SSL 加密
   - 监控 Redis 性能指标
   - 合理设置超时时间

2. **限流策略选择**：
   - 固定窗口：简单但可能有边界问题
   - 滑动窗口：更精确但实现复杂
   - 令牌桶/漏桶：适合平滑限流

3. **性能考虑**：
   - Redisson 默认使用 Netty 异步通信
   - 避免在热点 key 上频繁操作
   - 合理设置连接池大小

## 🎯 总结

✅ 已完成：
- Redisson 依赖配置
- RedissonClient Bean 配置
- 限流 AOP 切面实现
- 全局异常处理
- 使用示例

现在你可以轻松地在项目中使用 Redisson 和限流功能了！🎉
