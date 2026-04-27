# RabbitMQ 完整使用指南

## 📋 目录

1. [RabbitMQ 简介](#rabbitmq-简介)
2. [核心概念](#核心概念)
3. [六种工作模式详解](#六种工作模式详解)
4. [Spring Boot 集成](#spring-boot-集成)
5. [最佳实践](#最佳实践)
6. [常见问题](#常见问题)

---

## RabbitMQ 简介

RabbitMQ 是一个开源的消息代理和队列服务器，基于 AMQP（Advanced Message Queuing Protocol）协议实现。

### 主要特点
- ✅ 可靠性：支持消息持久化、确认机制
- ✅ 灵活性：支持多种路由模式和交换器类型
- ✅ 高可用：支持集群部署
- ✅ 多语言：提供多种语言的客户端库
- ✅ 插件系统：丰富的插件生态

### 应用场景
- 异步处理
- 应用解耦
- 流量削峰
- 日志处理
- 分布式事务

---

## 核心概念

| 概念 | 说明 |
|------|------|
| **Producer** | 消息生产者，发送消息到 Exchange |
| **Consumer** | 消息消费者，从 Queue 接收消息 |
| **Exchange** | 交换机，接收消息并路由到 Queue |
| **Queue** | 消息队列，存储消息直到被消费 |
| **Binding** | 绑定关系，连接 Exchange 和 Queue |
| **Routing Key** | 路由键，决定消息路由规则 |
| **Virtual Host** | 虚拟主机，隔离不同环境的资源 |

### 工作流程

```
Producer → Exchange → (Binding + Routing Key) → Queue → Consumer
```

---

## 六种工作模式详解

### 1️⃣ 简单模式（Simple Queue）

最简单的模式，一个生产者对应一个消费者。

#### 架构图
```
Producer → Queue → Consumer
```

#### 适用场景
- 简单的任务队列
- 一对一消息传递

#### Spring Boot 实现

**配置类：**
```java
@Configuration
public class SimpleQueueConfig {
    
    @Bean
    public Queue simpleQueue() {
        return new Queue("simple.queue", true);
    }
}
```

**生产者：**
```java
@Component
public class SimpleProducer {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void send(String message) {
        rabbitTemplate.convertAndSend("simple.queue", message);
        System.out.println("发送消息: " + message);
    }
}
```

**消费者：**
```java
@Component
public class SimpleConsumer {
    
    @RabbitListener(queues = "simple.queue")
    public void receive(String message) {
        System.out.println("收到消息: " + message);
    }
}
```

---

### 2️⃣ 工作队列模式（Work Queues）

一个生产者，多个消费者，消息会被轮询分发。

#### 架构图
```
         → Consumer 1
Producer → Queue
         → Consumer 2
```

#### 特点
- 默认轮询分发（Round-Robin）
- 可以设置公平分发（basicQos）

#### Spring Boot 实现

**配置类：**
```java
@Configuration
public class WorkQueueConfig {
    
    @Bean
    public Queue workQueue() {
        return new Queue("work.queue", true);
    }
}
```

**消费者（设置预取计数）：**
```java
@Component
public class WorkConsumer {
    
    @RabbitListener(queues = "work.queue")
    public void receive1(String message) throws InterruptedException {
        processMessage("消费者1", message);
    }
    
    @RabbitListener(queues = "work.queue")
    public void receive2(String message) throws InterruptedException {
        processMessage("消费者2", message);
    }
    
    private void processMessage(String consumer, String message) throws InterruptedException {
        System.out.println(consumer + " 收到消息: " + message);
        Thread.sleep(1000); // 模拟耗时操作
        System.out.println(consumer + " 处理完成");
    }
}
```

**配置文件（设置预取计数）：**
```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 1  # 每次只处理一条消息
```

---

### 3️⃣ 发布订阅模式（Publish/Subscribe）

一个生产者，多个消费者，每个消费者都收到完整的消息副本。

#### 架构图
```
         → Queue 1 → Consumer 1
Producer → Fanout Exchange
         → Queue 2 → Consumer 2
```

#### 特点
- 使用 Fanout Exchange
- 不需要 Routing Key
- 所有绑定的队列都会收到消息

#### Spring Boot 实现

**配置类：**
```java
@Configuration
public class FanoutConfig {
    
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange("fanout.exchange");
    }
    
    @Bean
    public Queue fanoutQueue1() {
        return new Queue("fanout.queue1", true);
    }
    
    @Bean
    public Queue fanoutQueue2() {
        return new Queue("fanout.queue2", true);
    }
    
    @Bean
    public Binding binding1(Queue fanoutQueue1, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(fanoutQueue1).to(fanoutExchange);
    }
    
    @Bean
    public Binding binding2(Queue fanoutQueue2, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(fanoutQueue2).to(fanoutExchange);
    }
}
```

**生产者：**
```java
@Component
public class FanoutProducer {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void send(String message) {
        rabbitTemplate.convertAndSend("fanout.exchange", "", message);
        System.out.println("发送消息: " + message);
    }
}
```

**消费者：**
```java
@Component
public class FanoutConsumer {
    
    @RabbitListener(queues = "fanout.queue1")
    public void receive1(String message) {
        System.out.println("消费者1 收到消息: " + message);
    }
    
    @RabbitListener(queues = "fanout.queue2")
    public void receive2(String message) {
        System.out.println("消费者2 收到消息: " + message);
    }
}
```

---

### 4️⃣ 路由模式（Routing）

根据 Routing Key 将消息路由到不同的队列。

#### 架构图
```
         → Queue 1 (error)    → Consumer 1
Producer → Direct Exchange
         → Queue 2 (info,warn) → Consumer 2
```

#### 特点
- 使用 Direct Exchange
- 需要精确匹配 Routing Key
- 一个队列可以绑定多个 Routing Key

#### Spring Boot 实现

**配置类：**
```java
@Configuration
public class DirectConfig {
    
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("direct.exchange");
    }
    
    @Bean
    public Queue errorQueue() {
        return new Queue("error.queue", true);
    }
    
    @Bean
    public Queue infoQueue() {
        return new Queue("info.queue", true);
    }
    
    @Bean
    public Binding errorBinding(Queue errorQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(errorQueue)
                .to(directExchange)
                .with("error");
    }
    
    @Bean
    public Binding infoBinding(Queue infoQueue, DirectExchange directExchange) {
        return BindingBuilder.bind(infoQueue)
                .to(directExchange)
                .with("info");
    }
}
```

**生产者：**
```java
@Component
public class DirectProducer {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void send(String routingKey, String message) {
        rabbitTemplate.convertAndSend("direct.exchange", routingKey, message);
        System.out.println("发送消息 [" + routingKey + "]: " + message);
    }
}
```

**消费者：**
```java
@Component
public class DirectConsumer {
    
    @RabbitListener(queues = "error.queue")
    public void receiveError(String message) {
        System.out.println("错误队列收到: " + message);
    }
    
    @RabbitListener(queues = "info.queue")
    public void receiveInfo(String message) {
        System.out.println("信息队列收到: " + message);
    }
}
```

---

### 5️⃣ 主题模式（Topics）

使用通配符进行模糊匹配的路由模式。

#### 通配符规则
- `*`：匹配一个单词
- `#`：匹配零个或多个单词

#### 架构图
```
            → Queue 1 (*.orange.*)   → Consumer 1
Producer → Topic Exchange
            → Queue 2 (*.*.rabbit)   → Consumer 2
            → Queue 3 (lazy.#)       → Consumer 3
```

#### 示例
- Routing Key: `quick.orange.rabbit`
  - 匹配: `*.orange.*` ✓
  - 匹配: `*.*.rabbit` ✓
  - 匹配: `lazy.#` ✗

#### Spring Boot 实现

**配置类：**
```java
@Configuration
public class TopicConfig {
    
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange("topic.exchange");
    }
    
    @Bean
    public Queue queue1() {
        return new Queue("topic.queue1", true);
    }
    
    @Bean
    public Queue queue2() {
        return new Queue("topic.queue2", true);
    }
    
    @Bean
    public Binding binding1(Queue queue1, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue1)
                .to(topicExchange)
                .with("*.orange.*");
    }
    
    @Bean
    public Binding binding2(Queue queue2, TopicExchange topicExchange) {
        return BindingBuilder.bind(queue2)
                .to(topicExchange)
                .with("*.*.rabbit");
    }
}
```

**生产者：**
```java
@Component
public class TopicProducer {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void send(String routingKey, String message) {
        rabbitTemplate.convertAndSend("topic.exchange", routingKey, message);
        System.out.println("发送消息 [" + routingKey + "]: " + message);
    }
}
```

---

### 6️⃣ RPC 模式

实现远程过程调用，生产者发送请求并等待响应。

#### 架构图
```
Client → Queue (request) → Server
Client ← Queue (reply) ← Server
```

#### Spring Boot 实现

**服务端：**
```java
@Component
public class RpcServer {
    
    @RabbitListener(queues = "rpc.request.queue")
    @SendTo("rpc.reply.queue")
    public String process(String message) {
        System.out.println("收到请求: " + message);
        return "处理结果: " + message.toUpperCase();
    }
}
```

**客户端：**
```java
@Component
public class RpcClient {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public String call(String message) {
        return (String) rabbitTemplate.convertSendAndReceive(
            "rpc.exchange", 
            "rpc.request", 
            message
        );
    }
}
```

---

## Spring Boot 集成

### 1. 添加依赖

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-amqp</artifactId>
</dependency>
```

### 2. 配置文件

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    
    # 连接池配置
    connection-timeout: 15000
    
    # 监听器配置
    listener:
      simple:
        acknowledge-mode: manual  # 手动确认
        prefetch: 1               # 预取计数
        retry:
          enabled: true           # 启用重试
          max-attempts: 3         # 最大重试次数
          initial-interval: 1000  # 初始间隔
```

### 3. 消息确认机制

**手动确认示例：**
```java
@Component
public class AckConsumer {
    
    @RabbitListener(queues = "ack.queue")
    public void receive(Message message, Channel channel) throws IOException {
        try {
            String msg = new String(message.getBody());
            System.out.println("收到消息: " + msg);
            
            // 业务处理
            processBusiness(msg);
            
            // 手动确认
            channel.basicAck(message.getMessageProperties().getDeliveryTag(), false);
            
        } catch (Exception e) {
            // 拒绝消息，重新入队
            channel.basicNack(message.getMessageProperties().getDeliveryTag(), false, true);
        }
    }
}
```

### 4. 消息持久化

```java
@Bean
public Queue durableQueue() {
    return QueueBuilder.durable("durable.queue")
            .withArgument("x-message-ttl", 60000)  // 消息TTL 60秒
            .withArgument("x-max-length", 1000)     // 最大消息数
            .build();
}
```

### 5. 死信队列

```java
@Configuration
public class DeadLetterConfig {
    
    // 普通队列
    @Bean
    public Queue normalQueue() {
        return QueueBuilder.durable("normal.queue")
                .withArgument("x-dead-letter-exchange", "dlx.exchange")
                .withArgument("x-dead-letter-routing-key", "dlx.routing.key")
                .build();
    }
    
    // 死信交换机
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("dlx.exchange");
    }
    
    // 死信队列
    @Bean
    public Queue deadLetterQueue() {
        return new Queue("dlx.queue", true);
    }
    
    // 绑定
    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with("dlx.routing.key");
    }
}
```

---

## 最佳实践

### 1. 消息可靠性保证

```java
// 生产者确认
spring:
  rabbitmq:
    publisher-confirm-type: correlated  # 开启确认
    publisher-returns: true             # 开启返回

// 配置回调
@Configuration
public class RabbitConfig {
    
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        
        // 确认回调
        template.setConfirmCallback((correlationData, ack, cause) -> {
            if (!ack) {
                log.error("消息发送失败: {}", cause);
            }
        });
        
        // 返回回调
        template.setReturnsCallback(returned -> {
            log.error("消息路由失败: {}", returned.getMessage());
        });
        
        return template;
    }
}
```

### 2. 幂等性处理

```java
@Component
public class IdempotentConsumer {
    
    @Autowired
    private RedisTemplate<String, String> redisTemplate;
    
    @RabbitListener(queues = "idempotent.queue")
    public void receive(Message message) {
        String messageId = message.getMessageProperties().getMessageId();
        
        // 检查是否已处理
        Boolean exists = redisTemplate.opsForValue()
                .setIfAbsent("msg:" + messageId, "1", 24, TimeUnit.HOURS);
        
        if (Boolean.FALSE.equals(exists)) {
            log.warn("重复消息，已忽略: {}", messageId);
            return;
        }
        
        // 处理业务
        processBusiness(message);
    }
}
```

### 3. 限流控制

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 10              # 预取10条
        concurrency: 5            # 最小消费者数量
        max-concurrency: 10       # 最大消费者数量
```

### 4. 消息序列化

```java
@Configuration
public class MessageConfig {
    
    @Bean
    public MessageConverter messageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true);
        return converter;
    }
}
```

### 5. 监控和管理

```java
@RestController
@RequestMapping("/rabbitmq")
public class RabbitMqController {
    
    @Autowired
    private AmqpAdmin amqpAdmin;
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    // 查看队列信息
    @GetMapping("/queues")
    public List<String> getQueues() {
        return amqpAdmin.getQueueNames();
    }
    
    // 清理队列
    @DeleteMapping("/queue/{name}")
    public void purgeQueue(@PathVariable String name) {
        rabbitTemplate.purgeQueue(name, true);
    }
}
```

---

## 常见问题

### Q1: 消息丢失怎么办？

**解决方案：**
1. 生产者确认机制
2. 消息持久化
3. 消费者手动确认
4. 死信队列

### Q2: 如何处理大量积压消息？

**解决方案：**
1. 增加消费者数量
2. 提高 prefetch 值
3. 优化消费者处理逻辑
4. 临时扩容队列

### Q3: 如何保证消息顺序？

**解决方案：**
1. 使用单一队列
2. 设置 single-active-consumer
3. 在消息中添加序列号

```java
@Bean
public Queue orderedQueue() {
    return QueueBuilder.durable("ordered.queue")
            .withArgument("x-single-active-consumer", true)
            .build();
}
```

### Q4: 如何实现延迟队列？

**方案一：使用插件**
```bash
# 安装延迟插件
rabbitmq-plugins enable rabbitmq_delayed_message_exchange
```

```java
@Bean
public CustomExchange delayedExchange() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-delayed-type", "direct");
    return new CustomExchange("delayed.exchange", "x-delayed-message", true, false, args);
}
```

**方案二：使用 TTL + 死信队列**
```java
@Bean
public Queue delayQueue() {
    return QueueBuilder.durable("delay.queue")
            .withArgument("x-message-ttl", 60000)  // 60秒
            .withArgument("x-dead-letter-exchange", "dlx.exchange")
            .build();
}
```

### Q5: 如何监控 RabbitMQ？

**方式一：管理界面**
- 访问: http://localhost:15672
- 默认账号: guest/guest

**方式二：Prometheus + Grafana**
```yaml
# docker-compose.yml
version: '3'
services:
  rabbitmq:
    image: rabbitmq:management
    ports:
      - "5672:5672"
      - "15672:15672"
  
  prometheus:
    image: prom/prometheus
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
  
  grafana:
    image: grafana/grafana
    ports:
      - "3000:3000"
```

---

## 模式对比总结

| 模式 | Exchange类型 | Routing Key | 应用场景 |
|------|-------------|-------------|----------|
| 简单模式 | - | - | 简单任务队列 |
| 工作队列 | - | - | 负载均衡 |
| 发布订阅 | Fanout | 不需要 | 广播通知 |
| 路由模式 | Direct | 精确匹配 | 分类处理 |
| 主题模式 | Topic | 通配符匹配 | 灵活路由 |
| RPC | Direct | 请求/响应 | 远程调用 |

---

## 参考资料

- [RabbitMQ 官方文档](https://www.rabbitmq.com/documentation.html)
- [Spring AMQP 文档](https://spring.io/projects/spring-amqp)
- [RabbitMQ GitHub](https://github.com/rabbitmq/rabbitmq-server)

---

**提示：** 建议根据实际业务需求选择合适的模式，不要过度设计。大多数场景下，简单模式、工作队列和主题模式已经足够使用。
