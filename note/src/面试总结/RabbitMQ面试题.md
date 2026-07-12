# RabbitMQ 面试题（结构化版）

> 目标：覆盖 RabbitMQ 常见面试所有主线（核心概念、工作模式、消息可靠性、死信/延迟、集群高可用、幂等/顺序、对比选型）。示例以 RabbitMQ 3.x 通用行为为主。

## 目录

1. [基础与整体认知](#1-基础与整体认知)
2. [核心概念与架构](#2-核心概念与架构)
3. [六种工作模式详解](#3-六种工作模式详解)
4. [消息可靠性（核心重点）](#4-消息可靠性核心重点)
5. [死信队列与延迟队列](#5-死信队列与延迟队列)
6. [集群与高可用](#6-集群与高可用)
7. [消息顺序性、幂等性与积压](#7-消息顺序性幂等性与积压)
8. [与其他 MQ 对比（Kafka / RocketMQ）](#8-与其他-mq-对比kafka--rocketmq)
9. [Spring Boot 落地常用片段](#9-spring-boot-落地常用片段)
10. [速记对比表（面试最后 1 分钟）](#10-速记对比表面试最后-1-分钟)

---

## 1. 基础与整体认知

### 1.1 RabbitMQ 是什么？为什么用它？

- **一句话结论**：RabbitMQ 是 Erlang 编写的开源消息中间件，基于 AMQP 协议，支持多种消息路由模式，核心优势是 **可靠性高、路由灵活、插件生态丰富**。
- **常见追问**：
  - 为什么用 Erlang 写？  
    - Erlang 天生适合高并发、分布式、容错系统，自带 Actor 模型，天然支持进程间通信。
  - AMQP 和 JMS 有什么区别？  
    - AMQP 是**跨语言**的二进制协议（wire-level），定义了消息格式和路由行为；JMS 是 Java 层面的 API 规范，与语言绑定。
    - RabbitMQ 支持 AMQP 0-9-1，也通过插件支持 MQTT、STOMP 等协议。

---

### 1.2 RabbitMQ 的典型应用场景

| 场景 | 说明 | 示例 |
|------|------|------|
| 异步处理 | 耗时操作异步化，快速响应 | 注册后异步发邮件/短信 |
| 应用解耦 | 各服务通过消息通信，互不依赖 | 订单系统 → 库存系统 |
| 流量削峰 | 缓冲瞬时高峰流量 | 秒杀活动 |
| 日志收集 | 分布式日志聚合 | ELK 日志系统 |
| 分布式事务 | 最终一致性方案 | 订单-支付-库存联动 |

---

### 1.3 RabbitMQ vs Kafka vs RocketMQ 一句话定位

| 产品 | 定位 | 一句话 |
|------|------|--------|
| RabbitMQ | 通用消息中间件 | 路由灵活、可靠性高，适合中小规模业务 |
| Kafka | 分布式流平台 | 高吞吐、持久化、顺序保证，适合日志/大数据 |
| RocketMQ | 金融级业务消息 | 阿里出品，事务消息、顺序消息原生支持 |

---

## 2. 核心概念与架构

### 2.1 核心组件

```
┌──────────┐     ┌──────────────┐     ┌──────────┐
│ Producer │ ──→ │   Exchange   │ ──→ │  Queue   │ ──→ │ Consumer │
└──────────┘     └──────┬───────┘     └──────────┘
                        │ Binding
                        │ Routing Key
```

| 概念 | 说明 | 类比 |
|------|------|------|
| **Producer** | 消息生产者，发送消息到 Exchange | 寄件人 |
| **Consumer** | 消息消费者，从 Queue 拉取/推送消息 | 收件人 |
| **Exchange** | 交换机，接收消息并按规则路由到 Queue | 快递分拣中心 |
| **Queue** | 消息队列，存储消息直到被消费 | 收件箱 |
| **Binding** | 绑定关系，定义 Exchange 到 Queue 的路由规则 | 分拣规则 |
| **Routing Key** | 路由键，生产者指定，交换机根据它路由 | 目的地地址 |
| **Virtual Host** | 虚拟主机，逻辑隔离，类似 MySQL 的 database | 数据库 |
| **Connection** | 长连接，TCP 连接 | 电话线 |
| **Channel** | 信道，复用 Connection，轻量级虚拟连接 | 分机号 |

---

### 2.2 四种交换机类型

| 类型 | 路由规则 | Routing Key | 典型场景 |
|------|----------|-------------|----------|
| **Direct** | 精确匹配 | 必须完全一致 | 日志分级（error/info/warning） |
| **Fanout** | 广播到所有绑定队列 | 忽略 | 配置刷新、缓存更新 |
| **Topic** | 通配符匹配 `*` 和 `#` | 点分隔的单词序列 | 复杂路由、事件驱动 |
| **Headers** | 根据 Header 匹配 | 不使用 | 较少使用，性能差 |

**通配符规则：**
- `*`：匹配恰好 **一个** 单词
- `#`：匹配 **零个或多个** 单词
- 单词之间用 `.` 分隔

**追问示例：**
- Routing Key `quick.orange.rabbit` 能否匹配 `*.orange.*`？→ **能**（3个单词各匹配）
- 能否匹配 `lazy.#`？→ **不能**（第一个单词不匹配）
- 能否匹配 `*.*.rabbit`？→ **能**（前两个单词用 `*` 匹配）

---

### 2.3 消息流转流程

```
1. Producer 创建 Connection → Channel
2. Producer 声明 Exchange（如不存在则创建）
3. Producer 发送消息到 Exchange，指定 Routing Key
4. Exchange 根据 Binding 将消息路由到匹配的 Queue
5. Consumer 监听 Queue，收到消息后处理
6. Consumer 发送 ACK（确认），Broker 删除消息
```

---

## 3. 六种工作模式详解

### 3.1 模式总览

| 模式 | Exchange | 路由方式 | 消费方式 | 场景 |
|------|----------|----------|----------|------|
| 简单模式 | 无（默认） | 直接入队 | 一对一 | 简单任务 |
| 工作队列 | 无（默认） | 轮询分发 | 竞争消费 | 任务分发 |
| 发布订阅 | Fanout | 广播 | 每人一份 | 广播通知 |
| 路由模式 | Direct | 精确匹配 | 按需接收 | 日志分级 |
| 主题模式 | Topic | 通配符匹配 | 灵活路由 | 复杂路由 |
| RPC | Direct | 请求/响应 | 双向通信 | 远程调用 |

---

### 3.2 简单模式（Simple）

```
Producer → Queue → Consumer
```

- 最简单，一对一，无需交换机（使用默认交换机）
- 默认交换机名称为 `""`（空字符串），Routing Key = Queue 名称

---

### 3.3 工作队列模式（Work Queues）

```
            → Consumer 1
Producer → Queue
            → Consumer 2
```

- **轮询分发（Round-Robin）**：默认，消息均匀分配给消费者
- **公平分发（Fair Dispatch）**：设置 `basicQos(1)`，让处理快的消费者多拿消息
- **关键点**：每个消息只被一个消费者处理，属于**竞争消费**模式

---

### 3.4 发布订阅模式（Publish/Subscribe）

```
            → Queue 1 → Consumer 1
Producer → Fanout Exchange
            → Queue 2 → Consumer 2
```

- 使用 Fanout Exchange，忽略 Routing Key
- 所有绑定的队列都会收到消息副本
- 一个消息可以被多个消费者同时消费

---

### 3.5 路由模式（Routing）

```
            → Queue (error)    → Consumer 1
Producer → Direct Exchange
            → Queue (info,warn) → Consumer 2
```

- 使用 Direct Exchange，精确匹配 Routing Key
- 一个队列可以绑定多个 Routing Key

---

### 3.6 主题模式（Topics）

```
            → Queue (*.orange.*)   → Consumer 1
Producer → Topic Exchange
            → Queue (*.*.rabbit)   → Consumer 2
            → Queue (lazy.#)       → Consumer 3
```

- 最灵活的模式，使用通配符实现模糊匹配
- 面试常问：`*` vs `#` 的区别

---

### 3.7 RPC 模式

```
Client → request.queue → Server
Client ← reply.queue ← Server
```

- 使用 `convertSendAndReceive` 实现同步调用
- 通过 CorrelationId 关联请求和响应
- 注意：RPC 模式下消息通道会阻塞，不适合高并发

---

## 4. 消息可靠性（核心重点）

> 这是 RabbitMQ 面试的**高频考点**，核心是回答"消息从生产到消费的全链路如何保证不丢失"。

### 4.1 消息丢失的三种场景

```
┌────────────────────────────────────────────────────────────┐
│ [1] Producer → Broker          发送阶段丢失                │
│ [2] Broker 内部                 存储阶段丢失                │
│ [3] Broker → Consumer          消费阶段丢失                │
└────────────────────────────────────────────────────────────┘
```

---

### 4.2 生产者端：Publisher Confirm（发布确认）

**问题**：生产者发送消息后，Broker 是否收到？

**方案**：
```yaml
spring:
  rabbitmq:
    publisher-confirm-type: correlated  # 开启确认
    publisher-returns: true             # 开启路由失败返回
```

```java
// 异步确认回调
rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {
    if (ack) {
        // 消息成功到达 Exchange
    } else {
        // 发送失败，记录日志或重试
        log.error("消息发送失败: {}", cause);
    }
});

// 路由失败回调（消息到了 Exchange 但没路由到任何 Queue）
rabbitTemplate.setReturnsCallback(returned -> {
    log.error("消息路由失败: {}", returned.getMessage());
});
```

**追问**：Confirm 和 Transaction 选哪个？
- **Confirm 模式**：异步，性能高，推荐
- **Transaction 模式**：同步，性能差（约 250 倍差距），不推荐

---

### 4.3 Broker 端：持久化

**问题**：RabbitMQ 重启后消息丢失？

**方案**：
```java
// 队列持久化
@Bean
public Queue durableQueue() {
    return new Queue("queue.name", true);  // durable=true
}

// 消息持久化
MessageProperties props = new MessageProperties();
props.setDeliveryMode(MessageDeliveryMode.PERSISTENT);  // deliveryMode=2
```

**注意**：持久化不是实时写入磁盘，有短时间窗口。如需强持久化，使用镜像队列 + 配置 `ha-sync-mode: automatic`。

---

### 4.4 消费者端：手动确认（Manual ACK）

**问题**：消费者拿到消息但处理失败，消息丢失？

**确认模式对比**：

| 模式 | 行为 | 可靠性 | 场景 |
|------|------|--------|------|
| 自动确认（auto） | 消息一到消费者就删除 | 低 | 不关心丢失 |
| 手动确认（manual） | 处理完业务后手动 ACK | 高 | 核心业务 |
| 不确认（none） | 不发送 ACK | 低 | 较少使用 |

```yaml
spring:
  rabbitmq:
    listener:
      simple:
        acknowledge-mode: manual
```

```java
@RabbitListener(queues = "order.queue")
public void receive(Message message, Channel channel) throws IOException {
    long deliveryTag = message.getMessageProperties().getDeliveryTag();
    try {
        // 1. 业务处理
        processOrder(message);
        // 2. 成功确认，单条
        channel.basicAck(deliveryTag, false);
    } catch (Exception e) {
        // 方案A：重新入队
        channel.basicNack(deliveryTag, false, true);
        // 方案B：拒绝且不入队（进入死信队列）
        // channel.basicNack(deliveryTag, false, false);
    }
}
```

**追问**：`basicNack` 和 `basicReject` 的区别？
- `basicReject`：只能拒绝**单条**消息
- `basicNack`：可以**批量**拒绝（设置 `multiple=true`）

---

### 4.5 可靠性总结（全链路）

```
【发送阶段】Publisher Confirm → 保证消息到达 Broker
【存储阶段】队列持久化 + 消息持久化 → 保证 Broker 重启不丢
【镜像阶段】镜像队列 → 保证节点宕机不丢
【消费阶段】手动 ACK → 保证消费成功才删除
【补偿阶段】死信队列 → 兜底失败消息，人工处理
```

---

## 5. 死信队列与延迟队列

### 5.1 死信队列（DLX）

**一句话**：死信队列是"兜底队列"，处理无法正常消费的消息。

**消息成为死信的三种情况**：

| 情况 | 触发条件 | 示例 |
|------|----------|------|
| 消息被拒绝 | `basicNack`/`basicReject` 且 `requeue=false` | 业务异常不想重试 |
| 消息过期 | 消息 TTL 超时 | 超时未支付的订单 |
| 队列已满 | 队列达到最大长度 | 消息积压 |

**Spring Boot 配置**：
```java
// 死信队列配置
@Bean
public Queue normalQueue() {
    return QueueBuilder.durable("normal.queue")
            .withArgument("x-dead-letter-exchange", "dlx.exchange")   // 死信交换机
            .withArgument("x-dead-letter-routing-key", "dlx.key")     // 死信路由键
            .withArgument("x-message-ttl", 60000)                     // 消息 TTL 60s
            .build();
}

@Bean
public DirectExchange dlxExchange() {
    return new DirectExchange("dlx.exchange");
}

@Bean
public Queue dlxQueue() {
    return new Queue("dlx.queue", true);
}

@Bean
public Binding dlxBinding() {
    return BindingBuilder.bind(dlxQueue())
            .to(dlxExchange())
            .with("dlx.key");
}
```

---

### 5.2 延迟队列

**场景**：订单 30 分钟未支付自动取消、定时任务。

**方案一：TTL + 死信队列（不推荐）**
- 缺陷：如果队列中多条消息 TTL 不同，先过期的消息会阻塞后面消息的死信转移

**方案二：延迟插件（推荐）**
```bash
rabbitmq-plugins enable rabbitmq_delayed_message_exchange
```

```java
@Bean
public CustomExchange delayedExchange() {
    Map<String, Object> args = new HashMap<>();
    args.put("x-delayed-type", "direct");  // 底层使用 Direct 类型
    return new CustomExchange("delayed.exchange", "x-delayed-message", true, false, args);
}

// 发送时指定延迟时间
MessagePostProcessor postProcessor = message -> {
    message.getMessageProperties().setDelay(5000);  // 延迟 5 秒
    return message;
};
rabbitTemplate.convertAndSend("delayed.exchange", "order.cancel", msg, postProcessor);
```

---

## 6. 集群与高可用

### 6.1 RabbitMQ 集群架构

**三种节点类型**：

| 节点类型 | 存储数据 | 作用 |
|----------|----------|------|
| 磁盘节点（Disc） | 元数据持久化到磁盘 | 必须有，保证元数据不丢 |
| 内存节点（RAM） | 元数据仅存内存 | 性能高，可多个 |
| 统计节点 | 不存储 | 仅做负载均衡（不推荐） |

---

### 6.2 镜像队列（Mirror Queue）

**问题**：队列所在节点宕机，消息丢失？

**方案**：镜像队列，将队列内容复制到集群中多个节点。

```bash
# 配置策略：所有队列自动镜像到所有节点
rabbitmqctl set_policy ha-all "^" '{"ha-mode":"all","ha-sync-mode":"automatic"}'
```

**镜像模式**：

| 模式 | 说明 | 适用场景 |
|------|------|----------|
| `ha-mode: all` | 镜像到所有节点 | 高可靠性要求 |
| `ha-mode: exactly` | 镜像到指定数量节点 | 平衡可靠性 & 性能 |
| `ha-mode: nodes` | 镜像到指定节点列表 | 精细控制 |

**注意**：3.8+ 版本推荐使用 **Quorum Queue**（仲裁队列）替代镜像队列，基于 Raft 协议，一致性更强。

---

### 6.3 仲裁队列（Quorum Queue）

```java
@Bean
public Queue quorumQueue() {
    return QueueBuilder.durable("quorum.queue")
            .quorum()  // 声明为仲裁队列
            .build();
}
```

**特点**：
- 基于 Raft 协议，强一致性
- 支持自动故障转移
- 消息持久化，可设置 TTL
- 不支持消息优先级（与经典队列的区别）

---

### 6.4 集群脑裂

**问题**：网络分区导致集群分裂成多个孤岛，各自认为自己是主。

**方案**：
```ini
# rabbitmq.conf
cluster_partition_handling = pause_minority  # 少数派暂停
```

| 策略 | 说明 |
|------|------|
| `ignore` | 忽略（不推荐） |
| `pause_minority` | 少数派节点自动暂停 |
| `autoheal` | 自动选择胜出方，失败方重启 |

---

## 7. 消息顺序性、幂等性与积压

### 7.1 消息顺序性

**问题**：RabbitMQ 能保证消息顺序吗？

**答案**：
- 单个 Queue + 单个 Consumer：**可以保证**
- 多个 Consumer：**不保证**（并行消费，乱序）
- 有重试/死信：**不保证**（失败重入队可能打乱顺序）

**解决方案**：
```java
// 方案1：单队列单消费者（天然保证）
// 方案2：设置 Single Active Consumer
@Bean
public Queue orderedQueue() {
    return QueueBuilder.durable("ordered.queue")
            .withArgument("x-single-active-consumer", true)
            .build();
}
// 方案3：业务层面，消息体加 sequenceId，消费端自行排序
```

---

### 7.2 消息幂等性

**问题**：消费者处理成功但 ACK 丢失，消息被重复投递怎么办？

**方案**：
```java
@RabbitListener(queues = "order.queue")
public void receive(Message message) {
    String messageId = message.getMessageProperties().getMessageId();
    
    // 1. Redis 去重（推荐）
    Boolean success = redisTemplate.opsForValue()
            .setIfAbsent("msg:" + messageId, "1", 24, TimeUnit.HOURS);
    if (Boolean.FALSE.equals(success)) {
        return;  // 重复消息，忽略
    }
    
    // 2. 数据库唯一索引去重
    // INSERT INTO processed_messages (message_id) VALUES (?)

    // 3. 业务状态机判断
    // 如果订单已支付，不再处理
}
```

**思路总结**：去重唯一键（消息ID） + Redis/DB 标记已处理

---

### 7.3 消息积压

**问题**：消费者跟不上，队列堆积几百万消息怎么办？

**排查路径**：
1. 消费者是否挂了？→ 重启
2. 消费者处理太慢？→ 优化代码
3. 消费者数量不够？→ 扩容

**解决方案**：
```yaml
spring:
  rabbitmq:
    listener:
      simple:
        prefetch: 10        # 提高预取数量
        concurrency: 5      # 最小消费者数
        max-concurrency: 20 # 最大消费者数
```

**应急方案**：
1. 临时增加消费者实例（水平扩容）
2. 消息转发到新队列，多消费者并行处理
3. 紧急情况：跳过非关键消息处理，先消峰

---

## 8. 与其他 MQ 对比（Kafka / RocketMQ）

### 8.1 核心对比表

| 维度 | RabbitMQ | Kafka | RocketMQ |
|------|----------|-------|----------|
| 开发语言 | Erlang | Java/Scala | Java |
| 协议 | AMQP | 自定义 TCP | 自定义（类 JMS） |
| 吞吐量 | 万级 | 百万级 | 十万级 |
| 延迟 | 微秒级 | 毫秒级 | 毫秒级 |
| 消息顺序 | 单队列单消费者支持 | 分区内有序 | 队列内有序 |
| 事务消息 | 不支持 | 不支持 | **原生支持** |
| 延迟消息 | 插件支持 | 不支持 | **原生支持** |
| 消息回溯 | 不支持 | **支持**（按时间/offset） | 支持（按时间） |
| 集群 | 镜像队列/仲裁队列 | 分区 + 副本 | 主从同步 |
| 管理界面 | 自带 Web 管理 | 需第三方 | 自带 Web 控制台 |
| 适用场景 | 中小规模、路由灵活 | 大数据、日志、流计算 | 金融、电商核心链路 |

---

### 8.2 选型建议

| 场景 | 推荐 | 原因 |
|------|------|------|
| 企业内部解耦，路由复杂 | RabbitMQ | 路由灵活，管理方便 |
| 日志收集、大数据管道 | Kafka | 高吞吐，持久化，顺序保证 |
| 电商交易、金融支付 | RocketMQ | 事务消息，延迟消息，阿里背书 |
| IoT 设备通信 | RabbitMQ（MQTT 插件） | 原生支持 MQTT 协议 |

---

## 9. Spring Boot 落地常用片段

### 9.1 基础配置

```yaml
spring:
  rabbitmq:
    host: localhost
    port: 5672
    username: guest
    password: guest
    virtual-host: /
    publisher-confirm-type: correlated
    publisher-returns: true
    listener:
      simple:
        acknowledge-mode: manual
        prefetch: 5
        concurrency: 3
        max-concurrency: 10
        retry:
          enabled: true
          max-attempts: 3
          initial-interval: 1000ms
          multiplier: 2.0
```

---

### 9.2 配置类模板

```java
@Configuration
public class RabbitConfig {

    // ========== 序列化 ==========
    @Bean
    public MessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();
        converter.setCreateMessageIds(true);  // 自动生成 MessageId
        return converter;
    }

    // ========== 发送确认回调 ==========
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(jsonMessageConverter());
        template.setConfirmCallback((data, ack, cause) -> {
            if (!ack) log.error("发送失败: {}", cause);
        });
        template.setReturnsCallback(returned -> 
            log.error("路由失败: {}", returned.getMessage()));
        return template;
    }

    // ========== 队列声明 ==========
    @Bean
    public Queue orderQueue() {
        return QueueBuilder.durable("order.queue")
                .deadLetterExchange("dlx.exchange")
                .deadLetterRoutingKey("dlx.order")
                .ttl(60000)
                .maxLength(10000)
                .build();
    }
}
```

---

### 9.3 消费者模板

```java
@Component
@Slf4j
public class OrderConsumer {

    @RabbitListener(queues = "order.queue")
    public void handle(OrderMessage msg, Message message, Channel channel) throws IOException {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            // 幂等判断
            String msgId = message.getMessageProperties().getMessageId();
            Boolean ok = redisTemplate.opsForValue()
                    .setIfAbsent("msg:" + msgId, "1", 1, TimeUnit.HOURS);
            if (Boolean.FALSE.equals(ok)) {
                channel.basicAck(deliveryTag, false);  // 重复消息，确认丢弃
                return;
            }
            // 业务处理
            orderService.process(msg);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理失败", e);
            channel.basicNack(deliveryTag, false, false);  // 进入死信
        }
    }
}
```

---

### 9.4 生产者模板

```java
@Component
public class OrderProducer {

    @Autowired
    private RabbitTemplate rabbitTemplate;

    public void sendOrder(OrderMessage msg) {
        CorrelationData data = new CorrelationData(msg.getOrderId());
        rabbitTemplate.convertAndSend("order.exchange", "order.create", msg, data);
    }

    // 延迟消息
    public void sendDelay(OrderMessage msg, int delayMs) {
        rabbitTemplate.convertAndSend("delayed.exchange", "order.cancel", msg, post -> {
            post.getMessageProperties().setDelay(delayMs);
            return post;
        });
    }
}
```

---

## 10. 速记对比表（面试最后 1 分钟）

### 10.1 交换机类型速记

| 类型 | 关键词 | 路由规则 |
|------|--------|----------|
| Direct | 精确匹配 | Routing Key == Binding Key |
| Fanout | 广播 | 忽略 Routing Key |
| Topic | 通配符 | `*` 一个词，`#` 零或多个词 |
| Headers | Header 匹配 | 忽略 Routing Key |

### 10.2 可靠性速记

| 阶段 | 机制 | 关键词 |
|------|------|--------|
| 发送 | Publisher Confirm | confirm + return |
| 存储 | 持久化 | durable + deliveryMode=2 |
| 消费 | 手动 ACK | basicAck / basicNack |
| 补偿 | 死信队列 | DLX + DLK |

### 10.3 高频一问一答

| 问题 | 答案关键词 |
|------|-----------|
| 如何保证消息不丢？ | Confirm + 持久化 + 手动ACK + 镜像队列 |
| 如何保证幂等？ | Redis setIfAbsent / DB 唯一索引 |
| 如何保证顺序？ | 单队列单消费者 / x-single-active-consumer |
| 如何实现延迟消息？ | 延迟插件 / TTL + DLX |
| 消息积压怎么办？ | 增加消费者 / 提高 prefetch / 新队列分流 |
| 事务 vs Confirm？ | 选 Confirm，性能差 250 倍 |
| TTL + DLX 实现延迟的缺陷？ | 先入队消息阻塞后入队消息的死信转移 |

---

> **面试建议**：重点掌握 **消息可靠性全链路**（第 4 节）和 **死信/延迟队列**（第 5 节），这两块是 RabbitMQ 面试被问最多的。MQ 选型对比能说出 3-4 个关键差异点即可。