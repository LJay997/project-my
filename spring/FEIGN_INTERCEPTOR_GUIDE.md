# Feign RequestInterceptor 条件化配置示例

## 📋 概述

本示例展示了如何结合 Spring Boot 的条件注解（`@ConditionalOnProperty` 和 `@Profile`）来动态配置 Feign 的 `RequestInterceptor`。

## 🎯 核心组件

### 1. 拦截器实现

- **DevRequestInterceptor**: 开发环境拦截器
  - 添加调试信息
  - 记录详细日志
  - 添加开发者标识

- **ProdRequestInterceptor**: 生产环境拦截器
  - 添加 API 认证
  - 生成请求签名
  - 防重放攻击

### 2. 配置方式

#### **方式 1: 使用 `@ConditionalOnProperty`（推荐）**

配置文件：`FeignInterceptorConfig.java`

```java
@Bean
@ConditionalOnProperty(
    name = "feign.interceptor.type", 
    havingValue = "dev", 
    matchIfMissing = true
)
public RequestInterceptor devRequestInterceptor() {
    return new DevRequestInterceptor();
}
```

**优点：**
- ✅ 灵活的属性控制
- ✅ 支持默认值
- ✅ 可以组合多个条件

**配置示例：**
```properties
# application.properties
feign.interceptor.type=dev          # 开发环境
# feign.interceptor.type=prod       # 生产环境
feign.interceptor.custom-enabled=false  # 是否启用自定义拦截器
```

#### **方式 2: 使用 `@Profile`**

配置文件：`FeignProfileInterceptorConfig.java`

```java
@Bean
@Profile("dev")
public RequestInterceptor devProfileInterceptor() {
    return new DevRequestInterceptor();
}
```

**优点：**
- ✅ 与 Spring Profile 无缝集成
- ✅ 简洁明了
- ✅ 适合多环境部署

**启动方式：**
```bash
# 开发环境
java -jar app.jar --spring.profiles.active=dev

# 生产环境
java -jar app.jar --spring.profiles.active=prod
```

## 🚀 使用步骤

### 1. 选择配置方式

**推荐使用方式 1（`@ConditionalOnProperty`）**，因为：
- 更灵活，可以在运行时通过配置切换
- 支持更复杂的条件组合
- 不依赖 Spring Profile

如果想使用方式 2，请注释掉 `FeignInterceptorConfig` 类上的 `@Configuration` 注解。

### 2. 配置属性

在 `application.properties` 中设置：

```properties
# 开发环境
feign.interceptor.type=dev

# 或生产环境
# feign.interceptor.type=prod

# 可选：启用自定义拦截器
feign.interceptor.custom-enabled=true
```

### 3. 测试拦截器

启动应用后，访问测试接口：

```bash
# 查看当前配置
curl http://localhost:8082/api/feign-interceptor/config

# 测试拦截器
curl http://localhost:8082/api/feign-interceptor/test
```

### 4. 观察日志

**开发环境日志示例：**
```
[Dev Interceptor] Method: GET, URL: http://localhost:8082/api/provider/users, TraceId: xxx
[开发环境] 请求拦截 - 方法: GET, 路径: /api/provider/users, 开发者: jay
```

**生产环境日志示例：**
```
[生产环境] 请求拦截 - 方法: GET, 路径: /api/provider/users, RequestId: xxx
```

## 💡 实际应用场景

### 场景 1: 不同环境使用不同的认证方式

```java
// 开发环境：无需认证或使用简单认证
template.header("Authorization", "Basic dGVzdDp0ZXN0");

// 生产环境：使用 OAuth2 Token
template.header("Authorization", "Bearer " + getOAuth2Token());
```

### 场景 2: 灰度发布

```java
@Bean
@ConditionalOnProperty(name = "feign.gray-release", havingValue = "true")
public RequestInterceptor grayReleaseInterceptor() {
    return template -> {
        // 添加灰度标识
        template.header("X-Gray-Release", "true");
        template.header("X-User-Group", getGrayUserGroup());
    };
}
```

### 场景 3: A/B 测试

```java
@Bean
@ConditionalOnProperty(name = "feign.ab-test.enabled", havingValue = "true")
public RequestInterceptor abTestInterceptor() {
    return template -> {
        // 根据用户 ID 分配不同的实验组
        String experimentGroup = assignExperimentGroup(userId);
        template.header("X-Experiment-Group", experimentGroup);
    };
}
```

## 🔧 高级用法

### 1. 组合多个条件

```java
@Bean
@ConditionalOnProperty(name = "feign.interceptor.type", havingValue = "prod")
@ConditionalOnProperty(name = "feign.security.enabled", havingValue = "true")
public RequestInterceptor secureProdInterceptor() {
    // 仅在生产环境且安全功能启用时加载
    return new SecureProdInterceptor();
}
```

### 2. 自定义条件注解

```java
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Conditional(FeignEnvironmentCondition.class)
public @interface ConditionalOnFeignEnvironment {
    String value();
}

// 使用
@Bean
@ConditionalOnFeignEnvironment("prod")
public RequestInterceptor prodInterceptor() {
    return new ProdRequestInterceptor();
}
```

### 3. 动态配置拦截器链

```java
@Configuration
public class DynamicInterceptorConfig {
    
    @Autowired(required = false)
    private List<RequestInterceptor> interceptors;
    
    @Bean
    public RequestInterceptor chainedInterceptor() {
        return template -> {
            if (interceptors != null) {
                for (RequestInterceptor interceptor : interceptors) {
                    interceptor.apply(template);
                }
            }
        };
    }
}
```

## ⚠️ 注意事项

1. **避免 Bean 冲突**
   - 确保同一时间只有一个拦截器 Bean 被创建
   - 使用 `@ConditionalOnProperty` 或 `@Profile` 互斥条件

2. **拦截器执行顺序**
   - 如果有多个拦截器，可以通过 `@Order` 注解控制顺序
   ```java
   @Bean
   @Order(1)
   public RequestInterceptor firstInterceptor() { ... }
   
   @Bean
   @Order(2)
   public RequestInterceptor secondInterceptor() { ... }
   ```

3. **性能考虑**
   - 拦截器会在每次 Feign 调用时执行
   - 避免在拦截器中执行耗时操作
   - 生产环境减少日志输出

4. **安全性**
   - 不要硬编码敏感信息（如 API Key、Secret）
   - 使用环境变量或配置中心管理敏感配置
   ```java
   @Value("${feign.api.key}")
   private String apiKey;
   ```

## 📊 对比总结

| 特性 | @ConditionalOnProperty | @Profile |
|------|----------------------|----------|
| **灵活性** | ⭐⭐⭐⭐⭐ | ⭐⭐⭐ |
| **复杂度** | 中等 | 简单 |
| **适用场景** | 细粒度控制 | 环境隔离 |
| **配置方式** | 自定义属性 | spring.profiles.active |
| **组合能力** | 强 | 弱 |

## 🎓 学习要点

1. ✅ 理解 `RequestInterceptor` 的作用和工作原理
2. ✅ 掌握 `@ConditionalOnProperty` 的使用方法
3. ✅ 了解 `@Profile` 的使用场景
4. ✅ 学会根据不同环境动态配置拦截器
5. ✅ 能够结合实际业务需求扩展拦截器功能

## 🔗 相关资源

- [Spring Boot Condition Annotations](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration.condition-annotations)
- [OpenFeign RequestInterceptor](https://github.com/OpenFeign/feign#request-interceptors)
- [Spring Profiles](https://docs.spring.io/spring-framework/docs/current/reference/html/core.html#beans-definition-profiles)
