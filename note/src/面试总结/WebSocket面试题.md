# WebSocket 面试题精选

## 📚 目录

1. [基础概念与注解](#1-基础概念与注解)
2. [配置与初始化](#2-配置与初始化)
3. [核心接口与实现](#3-核心接口与实现)
4. [消息处理机制](#4-消息处理机制)
5. [会话管理](#5-会话管理)
6. [拦截器与安全](#6-拦截器与安全)
7. [集群与扩展](#7-集群与扩展)
8. [常见问题与优化](#8-常见问题与优化)

---

## 1. 基础概念与注解

### Q1: 什么是 WebSocket？它与 HTTP 有什么区别？

**A:**

| 对比项 | HTTP | WebSocket |
|--------|------|-----------|
| **通信方式** | 请求-响应（半双工） | 全双工通信 |
| **连接状态** | 无状态，短连接 | 有状态，长连接 |
| **发起方** | 只能客户端发起 | 双方均可主动发送 |
| **头部开销** | 每次请求携带完整 Header | 仅握手时携带 Header |
| **实时性** | 低（需轮询） | 高（实时推送） |
| **适用场景** | 常规 Web 请求 | 聊天、直播、实时监控 |

**核心优势：**
- ✅ **低延迟**：无需频繁建立连接
- ✅ **双向通信**：服务器可主动推送
- ✅ **轻量级**：数据帧头部仅 2-14 字节

---

### Q2: `@EnableWebSocket` 的作用是什么？

**A:** `@EnableWebSocket` 是 Spring WebSocket 的**启用注解**，用于开启 WebSocket 支持。

**源码分析：**
```java
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Documented
@Import(DelegatingWebSocketConfiguration.class)
public @interface EnableWebSocket {
}
```

**工作原理：**
1. 通过 `@Import` 导入 `DelegatingWebSocketConfiguration`
2. 该配置类实现了 `WebSocketConfigurer` 接口
3. 注册 `WebSocketHandlerMapping` 和 `WebSocketHandlerAdapter`
4. 允许开发者通过实现 `WebSocketConfigurer` 来自定义配置

**使用示例：**
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(myHandler(), "/ws/chat")
                .addInterceptors(new HandshakeInterceptor())
                .setAllowedOrigins("*");
    }
    
    @Bean
    public WebSocketHandler myHandler() {
        return new MyWebSocketHandler();
    }
}
```

---

### Q3: `@EnableWebSocketMessageBroker` 和 `@EnableWebSocket` 有什么区别？

**A:** 这是两个不同层次的 WebSocket 支持：

| 对比项 | @EnableWebSocket | @EnableWebSocketMessageBroker |
|--------|------------------|------------------------------|
| **协议层级** | 底层 WebSocket API | STOMP 子协议 |
| **编程模型** | 手动处理消息 | 基于消息代理（Broker） |
| **复杂度** | 简单 | 较复杂 |
| **功能** | 基础双向通信 | 订阅/发布、路由、权限控制 |
| **适用场景** | 简单即时通讯 | 复杂消息系统（如聊天室） |

**@EnableWebSocketMessageBroker 特点：**
- 支持 STOMP 协议（Simple Text Oriented Messaging Protocol）
- 提供消息代理（SimpleBroker 或外部 Broker 如 RabbitMQ）
- 支持 `@MessageMapping`、`@SendTo` 等注解

```java
@Configuration
@EnableWebSocketMessageBroker
public class StompConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue"); // 启用简单消息代理
        config.setApplicationDestinationPrefixes("/app"); // 应用前缀
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws").withSockJS(); // 注册端点
    }
}
```

---

## 2. 配置与初始化

### Q4: 如何配置 WebSocket 端点？

**A:** 通过实现 `WebSocketConfigurer` 接口的 `registerWebSocketHandlers` 方法。

**关键配置项：**

```java
@Override
public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
    registry.addHandler(chatHandler(), "/chat")
            .addInterceptors(new HttpSessionHandshakeInterceptor())
            .setAllowedOrigins("https://example.com")  // 跨域配置
            .withSockJS();  // 启用 SockJS 降级支持
}
```

**配置说明：**
1. **addHandler()**: 注册处理器和路径
2. **addInterceptors()**: 添加握手拦截器
3. **setAllowedOrigins()**: 允许跨域（生产环境应指定具体域名）
4. **withSockJS()**: 启用 SockJS，兼容不支持 WebSocket 的浏览器

---

### Q5: 什么是 SockJS？为什么需要它？

**A:** SockJS 是一个**浏览器 JavaScript 库**，提供 WebSocket-like 的对象，但在不支持 WebSocket 的环境中会自动降级到其他传输方式。

**降级策略：**
```
1. WebSocket (首选)
2. HTTP Streaming
3. HTTP Long Polling
4. iframe HTMLfile
5. JSONP Polling
```

**优点：**
- ✅ **兼容性**：支持 IE8+ 等老旧浏览器
- ✅ **透明性**：API 与原生 WebSocket 一致
- ✅ **可靠性**：自动处理连接中断和重连

**前端使用：**
```javascript
var socket = new SockJS('/ws');
var stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
    console.log('Connected: ' + frame);
});
```

---

## 3. 核心接口与实现

### Q6: WebSocketHandler 的核心方法有哪些？

**A:** `WebSocketHandler` 是处理 WebSocket 消息的核心接口。

```java
public interface WebSocketHandler {
    
    // 1. 连接建立后调用
    void afterConnectionEstablished(WebSocketSession session) throws Exception;
    
    // 2. 收到文本消息
    void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception;
    
    // 3. 收到二进制消息
    void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception;
    
    // 4. 收到 Pong 消息（心跳）
    void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception;
    
    // 5. 传输错误
    void handleTransportError(WebSocketSession session, Throwable exception) throws Exception;
    
    // 6. 连接关闭
    void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception;
    
    // 7. 是否支持部分消息
    boolean supportsPartialMessages();
}
```

**常用实现类：**
- **TextWebSocketHandler**: 只处理文本消息（最常用）
- **BinaryWebSocketHandler**: 只处理二进制消息
- **ConcurrentWebSocketSessionDecorator**: 线程安全的会话装饰器

---

### Q7: 如何实现一个简单的聊天处理器？

**A:**

```java
@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {
    
    // 存储所有在线会话
    private static final CopyOnWriteArraySet<WebSocketSession> sessions = 
        new CopyOnWriteArraySet<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        sessions.add(session);
        System.out.println("新连接: " + session.getId());
        
        // 广播通知
        broadcastMessage("系统", session.getId() + " 加入聊天室");
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        System.out.println("收到消息: " + payload);
        
        // 广播给所有用户
        broadcastMessage(session.getId(), payload);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        sessions.remove(session);
        System.out.println("连接关闭: " + session.getId());
        
        broadcastMessage("系统", session.getId() + " 离开聊天室");
    }
    
    /**
     * 广播消息
     */
    private void broadcastMessage(String sender, String content) {
        TextMessage message = new TextMessage(
            String.format("[%s] %s: %s", 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                sender, content)
        );
        
        for (WebSocketSession session : sessions) {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

---

## 4. 消息处理机制

### Q8: STOMP 协议中的 @MessageMapping 如何使用？

**A:** `@MessageMapping` 类似于 Spring MVC 的 `@RequestMapping`，用于映射 STOMP 消息到处理方法。

**服务端：**
```java
@Controller
public class ChatController {
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // 接收客户端发送的消息
    @MessageMapping("/chat.send")
    @SendTo("/topic/public")  // 广播给所有订阅者
    public ChatMessage sendMessage(ChatMessage message) {
        return message;
    }
    
    // 点对点发送
    @MessageMapping("/chat.private")
    public void sendPrivateMessage(PrivateMessage message) {
        // 发送给特定用户
        messagingTemplate.convertAndSendToUser(
            message.getRecipient(), 
            "/queue/private", 
            message
        );
    }
    
    // 监听用户上线
    @EventListener
    public void handleSessionConnected(SessionConnectEvent event) {
        System.out.println("用户连接: " + event.getUser());
    }
}
```

**客户端：**
```javascript
// 发送消息
stompClient.send("/app/chat.send", {}, JSON.stringify({
    sender: '张三',
    content: '大家好'
}));

// 订阅公共频道
stompClient.subscribe('/topic/public', function(message) {
    console.log(JSON.parse(message.body));
});

// 订阅私人消息
stompClient.subscribe('/user/queue/private', function(message) {
    console.log(JSON.parse(message.body));
});
```

**路径规则：**
- 客户端发送：`/app` + `@MessageMapping` 路径
- 服务端广播：`@SendTo` 指定的路径
- 点对点：`/user/{username}/queue/xxx`

---

### Q9: SimpMessagingTemplate 的作用是什么？

**A:** `SimpMessagingTemplate` 是**服务端主动推送消息**的工具类，类似于 JMS 的 `JmsTemplate`。

**常用方法：**

```java
@Autowired
private SimpMessagingTemplate template;

// 1. 广播消息
template.convertAndSend("/topic/news", "最新消息");

// 2. 点对点发送
template.convertAndSendToUser("userId", "/queue/messages", "私人消息");

// 3. 带 Header 发送
Map<String, Object> headers = new HashMap<>();
headers.put("priority", 1);
template.convertAndSend("/topic/alert", "警告", headers);

// 4. 发送到特定目的地
template.send("/topic/chat", MessageBuilder.withPayload("Hello").build());
```

**应用场景：**
- 定时推送（如股票行情）
- 事件驱动推送（如订单状态变更）
- 系统通知

---

## 5. 会话管理

### Q10: 如何管理 WebSocket 会话？

**A:** 会话管理的核心是**存储和查找 WebSocketSession**。

**方案1：内存存储（单机）**
```java
@Component
public class SessionManager {
    
    // Key: userId, Value: WebSocketSession
    private static final ConcurrentHashMap<String, WebSocketSession> sessionMap = 
        new ConcurrentHashMap<>();
    
    public void addSession(String userId, WebSocketSession session) {
        sessionMap.put(userId, session);
    }
    
    public void removeSession(String userId) {
        sessionMap.remove(userId);
    }
    
    public WebSocketSession getSession(String userId) {
        return sessionMap.get(userId);
    }
    
    public void sendMessageToUser(String userId, String message) {
        WebSocketSession session = sessionMap.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
```

**方案2：Redis 存储（集群）**
```java
@Component
public class RedisSessionManager {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    public void addSession(String userId, String sessionId) {
        // 存储用户与 sessionId 的映射
        redisTemplate.opsForHash().put("websocket:sessions", userId, sessionId);
        // 设置过期时间
        redisTemplate.expire("websocket:sessions", 30, TimeUnit.MINUTES);
    }
    
    public void sendMessageToUser(String userId, String message) {
        // 在集群环境下，通过消息广播
        messagingTemplate.convertAndSendToUser(userId, "/queue/notify", message);
    }
}
```

**注意事项：**
- ⚠️ `WebSocketSession` **不可序列化**，不能直接存入 Redis
- ⚠️ 集群环境下需配合消息中间件（如 RabbitMQ）实现跨节点推送

---

### Q11: 如何处理心跳检测？

**A:** WebSocket 本身没有心跳机制，需要应用层实现。

**方式1：Ping/Pong（协议层）**
```java
@Override
protected void handlePongMessage(WebSocketSession session, PongMessage message) throws Exception {
    // 更新最后活跃时间
    session.getAttributes().put("lastActiveTime", System.currentTimeMillis());
}
```

**方式2：应用层心跳**
```java
// 客户端每 30 秒发送一次心跳
setInterval(() => {
    stompClient.send("/app/heartbeat", {}, '');
}, 30000);

// 服务端处理
@MessageMapping("/heartbeat")
public void handleHeartbeat() {
    // 更新用户活跃状态
}
```

**方式3：Spring 内置配置**
```java
@Override
public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
    registration.setMessageSizeLimit(128 * 1024)  // 消息大小限制
                .setSendBufferSizeLimit(512 * 1024) // 发送缓冲区
                .setSendTimeLimit(20000);           // 发送超时时间
}
```

---

## 6. 拦截器与安全

### Q12: HandshakeInterceptor 的作用是什么？

**A:** `HandshakeInterceptor` 在 **WebSocket 握手阶段**执行，用于验证请求、传递属性。

**实现示例：**
```java
@Component
public class AuthHandshakeInterceptor implements HandshakeInterceptor {
    
    @Override
    public boolean beforeHandshake(ServerHttpRequest request, 
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) throws Exception {
        
        // 1. 从 URL 参数或 Header 获取 Token
        String token = request.getURI().getQuery(); // ?token=xxx
        
        // 2. 验证 Token
        if (!validateToken(token)) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false; // 拒绝握手
        }
        
        // 3. 将用户信息存入 attributes，后续可在 Handler 中获取
        String userId = getUserIdFromToken(token);
        attributes.put("userId", userId);
        
        return true; // 允许握手
    }
    
    @Override
    public void afterHandshake(ServerHttpRequest request, 
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // 握手完成后清理资源
    }
    
    private boolean validateToken(String token) {
        // JWT 验证逻辑
        return true;
    }
    
    private String getUserIdFromToken(String token) {
        return "user123";
    }
}
```

**注册拦截器：**
```java
registry.addHandler(chatHandler(), "/chat")
        .addInterceptors(new AuthHandshakeInterceptor());
```

---

### Q13: 如何实现 WebSocket 的权限控制？

**A:**

**方式1：握手拦截器（推荐）**
```java
// 见 Q12 示例，在 beforeHandshake 中验证权限
```

**方式2：ChannelInterceptor（STOMP）**
```java
@Configuration
public class WebSocketSecurityConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(Message<?> message, MessageChannel channel) {
                StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(
                    message, StompHeaderAccessor.class);
                
                if (StompCommand.CONNECT.equals(accessor.getCommand())) {
                    // 验证 CONNECT 消息的权限
                    String token = accessor.getFirstNativeHeader("Authorization");
                    if (!isValid(token)) {
                        throw new AccessDeniedException("Invalid token");
                    }
                    
                    // 设置认证信息
                    Authentication auth = getAuthentication(token);
                    accessor.setUser(auth);
                }
                
                return message;
            }
        });
    }
}
```

**方式3：方法级安全**
```java
@MessageMapping("/admin.broadcast")
@PreAuthorize("hasRole('ADMIN')")  // 需要 ADMIN 角色
public void broadcast(Message msg) {
    // ...
}
```

---

## 7. 集群与扩展

### Q14: WebSocket 在集群环境下如何解决会话共享问题？

**A:** WebSocket 会话**不能直接共享**（因为 TCP 连接绑定到特定服务器），需要通过**消息代理**实现跨节点通信。

**架构方案：**

```
         ┌─────────────┐
         │  Load Balancer│
         └──┬─────┬─────┘
            │     │
    ┌───────▼─┐ ┌─▼────────┐
    │ Server A│ │ Server B │
    │ Session1│ │ Session2 │
    └────┬────┘ └────┬─────┘
         │           │
         └─────┬─────┘
               │
      ┌────────▼────────┐
      │  Message Broker  │ ← RabbitMQ / Redis Pub/Sub
      │  (RabbitMQ)      │
      └─────────────────┘
```

**实现步骤：**

1. **引入依赖**
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

2. **配置 RabbitMQ 消息代理**
```java
@Configuration
@EnableWebSocketMessageBroker
public class ClusterWebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 使用 RabbitMQ 作为外部消息代理
        registry.enableStompBrokerRelay("/topic", "/queue")
                .setRelayHost("localhost")
                .setRelayPort(61613)
                .setClientLogin("guest")
                .setClientPasscode("guest");
        
        registry.setApplicationDestinationPrefixes("/app");
    }
}
```

3. **发送消息**
```java
// 无论用户在哪个节点，都能收到消息
messagingTemplate.convertAndSendToUser(userId, "/queue/notify", message);
```

**原理：**
- Server A 收到消息 → 发送到 RabbitMQ Exchange
- RabbitMQ 路由到 Server B 的 Queue
- Server B 推送给本地 Session

---

### Q15: 如何监控 WebSocket 连接数？

**A:**

**方式1：Actuator 端点**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: websocket
```

**方式2：自定义指标**
```java
@Component
public class WebSocketMetrics {
    
    private final AtomicInteger onlineCount = new AtomicInteger(0);
    
    @EventListener
    public void onSessionConnected(SessionConnectEvent event) {
        onlineCount.incrementAndGet();
    }
    
    @EventListener
    public void onSessionDisconnected(SessionDisconnectEvent event) {
        onlineCount.decrementAndGet();
    }
    
    @GetMapping("/metrics/websocket")
    public int getOnlineCount() {
        return onlineCount.get();
    }
}
```

**方式3：Micrometer + Prometheus**
```java
@Autowired
private MeterRegistry meterRegistry;

private Gauge onlineGauge;

@PostConstruct
public void init() {
    onlineGauge = Gauge.builder("websocket.online.count", onlineCount::get)
        .register(meterRegistry);
}
```

---

## 8. 常见问题与优化

### Q16: WebSocket 连接频繁断开怎么办？

**A:**

**常见原因及解决方案：**

1. **Nginx 超时**
```nginx
location /ws {
    proxy_pass http://backend;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
    proxy_read_timeout 86400s;  # 设置为 24 小时
    proxy_send_timeout 86400s;
}
```

2. **防火墙/负载均衡器超时**
- 解决：应用层心跳（每 30-60 秒发送一次 Ping）

3. **浏览器标签页休眠**
- 解决：使用 Page Visibility API 检测页面激活状态

4. **网络波动**
- 解决：客户端实现自动重连
```javascript
function connect() {
    socket = new SockJS('/ws');
    stompClient = Stomp.over(socket);
    
    stompClient.connect({}, onConnected, onError);
}

function onError(error) {
    console.log('连接失败，5秒后重连...');
    setTimeout(connect, 5000);
}
```

---

### Q17: 如何优化 WebSocket 性能？

**A:**

**1. 消息压缩**
```java
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(handler(), "/ws")
                .addInterceptors(new PerConnectionWebSocketHandler(MyHandler.class));
    }
    
    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        return container;
    }
}
```

**2. 批量发送**
```java
// 避免频繁调用 sendMessage
List<String> messages = new ArrayList<>();
for (Data data : dataList) {
    messages.add(convertToJson(data));
}
session.sendMessage(new TextMessage(JsonUtils.toJson(messages)));
```

**3. 限流**
```java
@RateLimiter(name = "websocket")
@MessageMapping("/chat.send")
public void sendMessage(ChatMessage message) {
    // ...
}
```

**4. 异步发送**
```java
session.sendMessageAsync(new TextMessage(message));
```

---

### Q18: WebSocket 与 SSE（Server-Sent Events）如何选择？

**A:**

| 对比项 | WebSocket | SSE |
|--------|-----------|-----|
| **通信方向** | 双向 | 单向（服务器→客户端） |
| **协议** | WebSocket (ws://) | HTTP (text/event-stream) |
| **二进制支持** | ✅ | ❌（仅文本） |
| **自动重连** | ❌ 需手动 | ✅ 内置 |
| **复杂性** | 较高 | 简单 |
| **适用场景** | 聊天、游戏 | 新闻推送、股票行情 |

**选择建议：**
- 需要双向通信 → WebSocket
- 只需服务器推送 → SSE（更简单、更轻量）

**SSE 示例：**
```java
@GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
public Flux<ServerSentEvent<String>> stream() {
    return Flux.interval(Duration.ofSeconds(1))
        .map(seq -> ServerSentEvent.<String>builder()
            .data("Message at " + LocalTime.now())
            .build());
}
```

---

## 🎯 面试高频问题总结

| 问题 | 难度 | 出现频率 |
|------|------|---------|
| WebSocket 与 HTTP 区别？ | ⭐ | ⭐⭐⭐⭐⭐ |
| @EnableWebSocket 作用？ | ⭐⭐ | ⭐⭐⭐⭐ |
| 如何实现广播/点对点？ | ⭐⭐ | ⭐⭐⭐⭐⭐ |
| 集群会话共享方案？ | ⭐⭐⭐⭐ | ⭐⭐⭐⭐ |
| 握手拦截器作用？ | ⭐⭐ | ⭐⭐⭐ |
| 心跳检测如何实现？ | ⭐⭐ | ⭐⭐⭐ |
| STOMP 协议理解？ | ⭐⭐⭐ | ⭐⭐⭐ |
| 性能优化手段？ | ⭐⭐⭐ | ⭐⭐ |

---

## 💡 记忆口诀

```
WebSocket 全双工，长连接里显神通。
EnableWebSocket 开配置，Handler 处理消息通。
握手拦截验身份，STOMP 代理更从容。
集群要用 RabbitMQ，会话共享不落空。
心跳保活防断开，性能优化记心中。
```

---

## 📊 核心流程图

### WebSocket 握手流程
```
Client                          Server
   │                               │
   │  GET /ws HTTP/1.1             │
   │  Upgrade: websocket           │
   │  Connection: Upgrade          │
   │  Sec-WebSocket-Key: xxx       │
   │ ──────────────────────────▶   │
   │                               │ 验证 Key
   │  HTTP/1.1 101 Switching       │
   │  Upgrade: websocket           │
   │  Connection: Upgrade          │
   │  Sec-WebSocket-Accept: yyy    │
   │ ◀──────────────────────────   │
   │                               │
   │  === WebSocket 连接建立 ===    │
   │                               │
   │  [Text Frame] Hello           │
   │ ◀──────────────────────────▶  │
   │  [Text Frame] Hi              │
```

### STOMP 消息流转
```
Client App          STOMP Endpoint        Message Broker        Client Subscriber
     │                    │                      │                      │
     │ SEND /app/chat     │                      │                      │
     │ ────────────────▶  │                      │                      │
     │                    │  @MessageMapping     │                      │
     │                    │ ──────────────────▶  │                      │
     │                    │                      │ SEND /topic/chat     │
     │                    │                      │ ──────────────────▶  │
     │                    │                      │                      │
     │                    │                      │                      │ ◀── 收到消息
```

---

掌握这些知识点，WebSocket 相关的面试就能轻松应对了！💪
