# OpenFeign 使用指南

## 📋 概述

本项目已集成 Spring Cloud OpenFeign，用于简化 HTTP 客户端调用。

## 🔧 配置说明

### 1. 依赖添加

已在 `pom.xml` 中添加以下依赖：

```xml
<!-- Spring Cloud OpenFeign -->
<dependency>
    <groupId>org.springframework.cloud</groupId>
    <artifactId>spring-cloud-starter-openfeign</artifactId>
</dependency>
```

### 2. 启用 Feign

在启动类上添加了 `@EnableFeignClients` 注解：

```java
@SpringBootApplication
@EnableFeignClients
public class SpringDemoApplication {
    // ...
}
```

### 3. 配置文件

在 `application.properties` 中配置了 Feign 的基本参数：

```properties
# 连接超时时间（毫秒）
feign.client.config.default.connectTimeout=5000
# 读取超时时间（毫秒）
feign.client.config.default.readTimeout=10000
# 日志级别：NONE, BASIC, HEADERS, FULL
feign.client.config.default.loggerLevel=basic
```

## 💡 使用示例

### 创建 Feign Client

```java
@FeignClient(name = "jsonPlaceholder", url = "https://jsonplaceholder.typicode.com")
public interface ExampleFeignClient {

    @GetMapping("/posts/{id}")
    Map<String, Object> getPost(@PathVariable("id") Long id);

    @GetMapping("/posts")
    List<Map<String, Object>> getPosts();

    @PostMapping("/posts")
    Map<String, Object> createPost(@RequestBody Map<String, Object> body);
}
```

### 注入并使用

```java
@RestController
public class MyController {

    @Autowired
    private ExampleFeignClient exampleFeignClient;

    @GetMapping("/test")
    public Map<String, Object> test() {
        return exampleFeignClient.getPost(1L);
    }
}
```

## 🧪 测试接口

项目提供了测试 Controller：`FeignTestController`

### 接口列表

1. **获取单个文章**
   ```
   GET http://localhost:8082/api/feign/post/1
   ```

2. **获取所有文章**
   ```
   GET http://localhost:8082/api/feign/posts
   ```

3. **创建文章**
   ```
   POST http://localhost:8082/api/feign/post
   Content-Type: application/json
   
   {
     "title": "Test Title",
     "body": "Test Body",
     "userId": 1
   }
   ```

4. **综合测试**
   ```
   GET http://localhost:8082/api/feign/test
   ```

## 📚 Feign 常用注解

| 注解 | 说明 |
|------|------|
| `@FeignClient` | 声明 Feign 客户端 |
| `@GetMapping` | GET 请求映射 |
| `@PostMapping` | POST 请求映射 |
| `@PutMapping` | PUT 请求映射 |
| `@DeleteMapping` | DELETE 请求映射 |
| `@PathVariable` | 路径变量 |
| `@RequestParam` | 请求参数 |
| `@RequestBody` | 请求体 |
| `@RequestHeader` | 请求头 |

## ⚙️ 高级配置

### 1. 服务发现集成

如果使用 Nacos/Eureka 等服务发现：

```java
@FeignClient(name = "user-service")  // name 为服务名
public interface UserFeignClient {
    @GetMapping("/users/{id}")
    User getUser(@PathVariable("id") Long id);
}
```

### 2. 自定义配置

```java
@FeignClient(
    name = "user-service",
    configuration = FeignConfig.class,
    fallback = UserFeignFallback.class  // 熔断降级
)
public interface UserFeignClient {
    // ...
}
```

### 3. 日志级别配置

```properties
# 针对特定客户端配置
feign.client.config.jsonPlaceholder.loggerLevel=full

# 全局配置
feign.client.config.default.loggerLevel=basic
```

日志级别说明：
- `NONE`: 不记录日志（默认）
- `BASIC`: 仅记录请求方法、URL、响应状态码和执行时间
- `HEADERS`: 记录基本信息 + 请求和响应头
- `FULL`: 记录完整的请求和响应信息

### 4. 超时配置

```properties
# 全局配置
feign.client.config.default.connectTimeout=5000
feign.client.config.default.readTimeout=10000

# 针对特定客户端
feign.client.config.jsonPlaceholder.connectTimeout=3000
feign.client.config.jsonPlaceholder.readTimeout=5000
```

## 🔍 注意事项

1. **循环依赖问题**：避免在 Feign Client 和 Service 之间形成循环依赖
2. **超时设置**：根据实际业务需求合理设置超时时间
3. **重试机制**：Feign 默认不重试，如需重试需额外配置
4. **日志性能**：生产环境建议使用 `BASIC` 或 `NONE` 级别，`FULL` 级别会影响性能
5. **异常处理**：建议配置熔断降级（如使用 Resilience4j 或 Sentinel）

## 📖 参考资料

- [Spring Cloud OpenFeign 官方文档](https://spring.io/projects/spring-cloud-openfeign)
- [OpenFeign GitHub](https://github.com/OpenFeign/feign)
