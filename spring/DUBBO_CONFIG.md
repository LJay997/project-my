# Dubbo 配置说明

## 当前状态

Dubbo 功能已**禁用**，应用可以正常启动而无需 Nacos 服务器。

## 如何启用 Dubbo

### 方案一：使用 Nacos（生产环境推荐）

1. **启动 Nacos 服务器**
```bash
# Linux/Mac
sh nacos/bin/startup.sh -m standalone

# Windows
cmd nacos/bin/startup.cmd -m standalone
```

2. **取消注释 application.properties 中的 Dubbo 配置**
```properties
dubbo.application.name=spring-dubbo-demo
dubbo.application.qos-enable=true
dubbo.registry.address=nacos://127.0.0.1:8848
dubbo.protocol.name=dubbo
dubbo.protocol.port=20880
dubbo.provider.timeout=5000
dubbo.provider.retries=1
dubbo.consumer.check=false
dubbo.consumer.timeout=5000
```

3. **重启应用**

### 方案二：使用 Zookeeper

1. **启动 Zookeeper**
```bash
zkServer.sh start
```

2. **激活 zookeeper profile 并配置**
```properties
spring.profiles.active=zookeeper
dubbo.registry.address=zookeeper://127.0.0.1:2181
```

### 方案三：直连模式（本地开发）

1. **激活 direct profile**
```properties
spring.profiles.active=direct
```

2. **在 Consumer 中使用直连地址**
```java
@DubboReference(url = "dubbo://127.0.0.1:20880")
private UserService userService;
```

## 技术说明

### @ConditionalOnProperty

所有 Dubbo 配置类都使用了 `@ConditionalOnProperty` 注解：

```java
@Configuration
@ConditionalOnProperty(prefix = "dubbo", name = "registry.address")
public class DubboConfig {
    // ...
}
```

这意味着：
- ✅ **只有**当配置文件中存在 `dubbo.registry.address` 时，Dubbo 配置才会生效
- ✅ 如果没有该配置，Dubbo 相关的 Bean 不会创建
- ✅ 避免了"没有 Nacos 服务器就无法启动"的问题

### Profile 支持

项目提供了三种 Dubbo 配置：

| Profile | 配置类 | 注册中心 | 适用场景 |
|---------|--------|----------|----------|
| 默认 | DubboConfig | Nacos | 生产环境 |
| zookeeper | DubboZookeeperConfig | Zookeeper | 使用 ZK 的环境 |
| direct | DubboDirectConfig | 无 | 本地开发 |

## 快速测试

### 测试 Web 功能（Dubbo 禁用）
```bash
# 直接运行应用
mvn spring-boot:run

# 访问 Web API
curl http://localhost:8082/api/users
```

### 测试 Dubbo 功能（需要先启动 Nacos）
1. 启动 Nacos
2. 取消注释 Dubbo 配置
3. 重启应用
4. 访问 Dubbo API
```bash
curl http://localhost:8082/api/dubbo/hello?userName=张三
```

## 常见问题

### Q: 为什么要禁用 Dubbo？
A: 
- 本地开发时不需要每次都启动 Nacos
- 减少对外部服务的依赖
- 加快应用启动速度

### Q: 如何确认 Dubbo 是否启用？
A: 查看启动日志：
- 如果看到 "Dubbo 服务已暴露" 相关日志，说明已启用
- 如果没有相关日志，说明已禁用

### Q: 只想用 Web 功能，可以删除 Dubbo 代码吗？
A: 可以，但建议保留代码并禁用配置：
- 保留代码可以在需要时快速启用
- 不影响 Web 功能的正常使用
- 学习/演示时更方便

---

**更新时间**: 2026-03-30
