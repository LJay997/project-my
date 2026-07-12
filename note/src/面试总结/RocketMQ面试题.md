# RocketMQ 通俗名词介绍 & 面试题

> 目标：用通俗类比讲清 RocketMQ 核心概念，再覆盖面试高频考点（架构、消息类型、事务消息、可靠性、集群、对比选型）。示例以 RocketMQ 4.x/5.x 通用行为为主。

## 目录

1. [通俗名词介绍（先搞懂这些词）](#1-通俗名词介绍先搞懂这些词)
2. [基础与整体认知](#2-基础与整体认知)
3. [核心架构与组件](#3-核心架构与组件)
4. [消息类型详解（高频考点）](#4-消息类型详解高频考点)
5. [消息可靠性](#5-消息可靠性)
6. [集群与高可用](#6-集群与高可用)
7. [消费模式、重试与幂等](#7-消费模式重试与幂等)
8. [与其他 MQ 对比（Kafka / RabbitMQ）](#8-与其他-mq-对比kafka--rabbitmq)
9. [Spring Boot / Spring Cloud Stream 落地](#9-spring-boot--spring-cloud-stream-落地)
10. [速记对比表（面试最后 1 分钟）](#10-速记对比表面试最后-1-分钟)

---

## 1. 通俗名词介绍（先搞懂这些词）

> 用"快递公司"类比 RocketMQ，帮助快速理解每个概念是干什么的。

### 1.1 核心角色（快递公司三件套）

| 概念 | 通俗类比 | 技术解释 |
|------|----------|----------|
| **Producer（生产者）** | 寄件人 | 生产和发送消息的应用程序 |
| **Consumer（消费者）** | 收件人 | 接收并处理消息的应用程序 |
| **Broker（消息代理）** | 快递中转站 | 存储和转发消息的服务器 |

### 1.2 消息组织（快递包裹怎么分）

```
一个快递公司（RocketMQ 集群）
 └── 多个省份分拨中心（Broker）
      └── 每个分拨中心有多个仓库分区（Topic）
           └── 每个仓库分区有多个货架（MessageQueue）
                └── 货架上摆放包裹（Message）
```

| 概念 | 通俗类比 | 技术解释 |
|------|----------|----------|
| **Topic（主题）** | 快递类型（如"生鲜件"、"文件件"） | 消息的分类，生产者发到 Topic，消费者订阅 Topic |
| **MessageQueue（消息队列）** | 货架编号（如"3号货架"） | Topic 下的物理分区，消息实际存储的地方 |
| **Tag（标签）** | 包裹上的标签贴纸（"加急"/"普通"） | 消息子分类，消费者可以按 Tag 过滤 |
| **Message（消息）** | 快递包裹本身 | 一条条的消息数据 |
| **Message Key** | 快递单号 | 唯一标识一条消息，用于查询和幂等 |

### 1.3 消费相关（谁去取快递）

| 概念 | 通俗类比 | 技术解释 |
|------|----------|----------|
| **ConsumerGroup（消费组）** | 一个收件团队 | 一组 Consumer 的逻辑集合，集群模式下组内分摊消息 |
| **集群消费（Clustering）** | 团队分工：每人拿不同的包裹 | 一条消息只被组内**一个**消费者处理 |
| **广播消费（Broadcasting）** | 全员通知：每个人都收到同一份 | 一条消息被组内**所有**消费者处理 |
| **Offset（消费位点）** | 书签：读到第几页了 | 记录消费者消费到队列的哪个位置 |

### 1.4 特殊角色（快递调度员）

| 概念 | 通俗类比 | 技术解释 |
|------|----------|----------|
| **NameServer（名称服务器）** | 快递调度中心 | 管理 Broker 路由信息，告诉 Producer/Consumer 该找哪个 Broker |
| **ProducerGroup（生产组）** | 寄件人队伍 | 一组 Producer 的逻辑集合，事务消息中用于回查 |

### 1.5 一句话串起来

```
Producer 寄快递 → 先问 NameServer（调度中心）"该发到哪个中转站？"
    → NameServer 返回 Broker 地址
    → Producer 把包裹（Message）发到 Broker 的某个 Topic 的某个 MessageQueue
    → Consumer 也问 NameServer → 去对应的 Broker 取包裹
    → 取完后更新 Offset（书签），下次接着读
```

---

## 2. 基础与整体认知

### 2.1 RocketMQ 是什么？为什么用它？

- **一句话结论**：RocketMQ 是阿里巴巴开源的**金融级**分布式消息中间件，Java 编写，核心卖点是**低延迟、高可靠、事务消息和顺序消息原生支持**。
- **常见追问**：
  - 为什么叫"金融级"？  
    - 支持**事务消息**（分布式事务最终一致性）、**消息轨迹**（查链路）、**死信队列**自动兜底，满足金融交易的严苛要求。
  - 经历了什么考验？  
    - 阿里双十一核心链路，每秒处理数十亿条消息。

---

### 2.2 RocketMQ 的典型应用场景

| 场景 | 说明 | RocketMQ 优势 |
|------|------|---------------|
| 异步解耦 | 服务间通过消息通信，不直接调用 | 高性能、低延迟 |
| **分布式事务** | 订单-支付-库存的一致性 | **事务消息**（半消息 + 回查） |
| 顺序消息 | 订单状态流转必须有序 | **原生支持**分区有序 |
| 延迟消息 | 超时未支付自动取消 | **18 个延迟级别**，内置支持 |
| 流量削峰 | 秒杀活动缓冲 | 大容量队列，持久化 |
| 消息轨迹 | 追踪消息流转路径 | 消息轨迹 Trace 功能 |

---

### 2.3 RocketMQ vs RabbitMQ vs Kafka 一句话定位

| 产品 | 一句话定位 | 核心卖点 |
|------|-----------|----------|
| RocketMQ | 阿里出品，金融级业务消息 | 事务消息、顺序消息、延迟消息 |
| RabbitMQ | 老牌通用消息中间件 | 路由灵活、插件丰富、管理方便 |
| Kafka | 分布式流处理平台 | 高吞吐、持久化、顺序保证 |

---

## 3. 核心架构与组件

### 3.1 架构全景图

```
┌──────────────────────────────────────────────────┐
│                    NameServer                    │  ← 路由中心（无状态）
│              (多个，彼此不通信)                    │
└──────┬─────────────────────────┬────────────────┘
       │ 注册/心跳                │ 查询路由
       ▼                         ▼
┌──────────────┐         ┌──────────────┐
│   Broker     │         │   Producer   │
│  (Master)    │ ←────── │              │
│  同步/异步    │  发送消息  │              │
│  (Slave)     │         └──────────────┘
│              │
│  存储消息     │         ┌──────────────┐
│  CommitLog   │ ──────→ │   Consumer   │
└──────────────┘  拉取/推送 └──────────────┘
```

### 3.2 四大核心组件

| 组件 | 职责 | 关键点 |
|------|------|--------|
| **NameServer** | 路由注册中心 | 无状态、轻量级、彼此不通信、CAP 中的 AP |
| **Broker** | 消息存储与转发 | 主从架构、Master 读写、Slave 只读 |
| **Producer** | 消息生产 | 从 NameServer 获取路由，发送到 Broker |
| **Consumer** | 消息消费 | 支持 Pull/Push 两种模式 |

### 3.3 NameServer 的设计

**追问**：为什么不用 Zookeeper 而自己写 NameServer？

- **轻量级**：NameServer 代码极简，无状态，不维护一致性协议
- **AP 模型**：牺牲一致性换可用性，每个 NameServer 独立，不符合 CAP 的 CP 要求
- **部署简单**：不依赖外部组件，直接启动
- **对比 ZK**：ZK 是 CP 系统，网络分区时可能不可用；RocketMQ 选择可用性优先

---

### 3.4 Broker 的存储设计

```
┌─────────────────────────────────────┐
│            CommitLog                 │  ← 所有 Topic 的消息顺序写入
│  (单文件 1GB，顺序写，所有消息混存)     │     一个 Broker 只有一份
└─────────────────────────────────────┘
                    │
                    ▼ 异步构建
┌─────────────────────────────────────┐
│          ConsumeQueue                │  ← 按 Topic-Queue 维度索引
│  (只存 offset + size + tag hash)    │     每个 Topic 的每个 Queue 一份
└─────────────────────────────────────┘
```

| 存储组件 | 作用 | 特点 |
|----------|------|------|
| **CommitLog** | 所有消息的物理存储 | 顺序写，1GB 一个文件，高吞吐 |
| **ConsumeQueue** | 消息的逻辑索引 | 很小（20 字节/条），快速定位 |
| **IndexFile** | 按 Key 查询消息 | 哈希索引，支持按 Message Key 查询 |

**追问**：为什么 CommitLog 要所有 Topic 混存？

- **顺序写**是磁盘最高效的写入方式，所有消息混存只需一个文件顺序追加，**避免随机写**
- 相当于把随机写变成了顺序写，性能提升巨大

---

## 4. 消息类型详解（高频考点）

### 4.1 消息类型总览

| 类型 | 说明 | 面试热度 |
|------|------|----------|
| 普通消息 | 最基本的发送-消费 | ⭐⭐ |
| 顺序消息 | 保证消息按顺序消费 | ⭐⭐⭐⭐ |
| 延迟消息 | 指定延迟时间后投递 | ⭐⭐⭐ |
| **事务消息** | 分布式事务最终一致性 | ⭐⭐⭐⭐⭐ |
| 批量消息 | 一次发送多条消息 | ⭐⭐ |

---

### 4.2 普通消息

**三种发送方式**：

| 方式 | 特点 | 可靠性 | 场景 |
|------|------|--------|------|
| 同步发送 | 等待 Broker 确认后返回 | 高 | 重要通知 |
| 异步发送 | 回调获取结果，不阻塞 | 中 | 响应时间敏感 |
| 单向发送 | 发了就不管，不管结果 | 低 | 日志收集 |

```java
// 同步发送
SendResult result = producer.send(msg);
// 异步发送
producer.send(msg, new SendCallback() {
    public void onSuccess(SendResult result) { }
    public void onException(Throwable e) { }
});
// 单向发送
producer.sendOneway(msg);
```

---

### 4.3 顺序消息（⭐⭐⭐⭐）

**问题**：RocketMQ 如何保证消息顺序？

**原理**：
- 将需要顺序的消息发送到**同一个 MessageQueue**
- 消费者**单线程**消费该队列
- **分区有序**（不是全局有序）

```
订单 1001 的创建 → 支付 → 发货 → 完成
     ↓ 都发到同一个 MessageQueue
     Queue-3: [创建] [支付] [发货] [完成]
     ↓ 单线程消费
     Consumer: 按顺序处理
```

**代码实现**：
```java
// 生产者：按订单ID取模，保证同一订单进同一队列
MessageQueueSelector selector = (mqs, msg, arg) -> {
    long orderId = (long) arg;
    int index = (int) (orderId % mqs.size());
    return mqs.get(index);
};
producer.send(msg, selector, orderId);

// 消费者：注册 MessageListenerOrderly（顺序消费）
consumer.registerMessageListener((MessageListenerOrderly) (msgs, context) -> {
    for (MessageExt msg : msgs) {
        processOrder(msg);
    }
    return ConsumeOrderlyStatus.SUCCESS;
});
```

**追问**：顺序消费时遇到失败怎么办？

- `SUSPEND_CURRENT_QUEUE_A_MOMENT`：暂停当前队列一会儿，稍后重试，**不会跳过**消息
- 这就是顺序消费和并发消费的关键区别：并发消费可以跳过失败消息继续处理后面的，顺序消费必须等当前消息成功

---

### 4.4 延迟消息（⭐⭐⭐）

**原理**：RocketMQ 内置 **18 个延迟级别**：

```
1s / 5s / 10s / 30s / 1m / 2m / 3m / 4m / 5m / 6m / 7m / 8m / 9m / 10m / 20m / 30m / 1h / 2h
```

```java
Message msg = new Message("order-topic", "order-cancel", body);
msg.setDelayTimeLevel(3);  // 第3级 = 10秒
producer.send(msg);
```

**追问**：RocketMQ 延迟消息和 RabbitMQ 延迟插件的区别？

| 维度 | RocketMQ | RabbitMQ |
|------|----------|----------|
| 实现方式 | 内置 18 个延迟级别 | 需要安装插件 |
| 灵活性 | 仅支持预设级别 | 可任意指定延迟时间 |
| 可靠性 | 原生支持 | 插件支持 |

---

### 4.5 事务消息（⭐⭐⭐⭐⭐）

> 这是 RocketMQ 的**王牌特性**，面试必问。

**问题**：RocketMQ 如何实现分布式事务？

**场景**：用户下单 → 扣减库存，两个操作需要原子性。

**事务消息流程**：

```
Producer                          Broker                         Consumer
   │                                │                               │
   │ ① 发送半消息（Half Message）    │                               │
   │ ─────────────────────────────→ │ （消息对消费者不可见）          │
   │                                │                               │
   │ ② 执行本地事务（扣库存）         │                               │
   │    ┌── 成功 ──→ ③ COMMIT      │                               │
   │    │          ────────────────→ │ ④ 消息对消费者可见             │
   │    │                           │ ─────────────────────────────→│
   │    │                           │                               │
   │    └── 失败 ──→ ③ ROLLBACK    │                               │
   │               ────────────────→│ 消息删除，消费者不可见          │
   │                                │                               │
   │ ⑤ 如果超时未收到 COMMIT/ROLLBACK                                │
   │    Broker 主动回查（Check）     │                               │
   │ ←───────────────────────────── │                               │
   │   检查本地事务状态，决定 COMMIT/ROLLBACK                         │
```

**核心概念**：

| 概念 | 解释 |
|------|------|
| **半消息（Half Message）** | 发送到 Broker 但对消费者不可见的消息 |
| **事务状态回查** | Broker 长时间没收到确认，主动问 Producer 本地事务状态 |
| **COMMIT** | 确认提交，消息对消费者可见 |
| **ROLLBACK** | 确认回滚，消息删除 |

**代码实现**：
```java
// 使用事务消息生产者
TransactionMQProducer producer = new TransactionMQProducer("tx-group");
producer.setTransactionListener(new TransactionListener() {
    
    // 执行本地事务
    @Override
    public LocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
            // 执行本地业务（如扣库存）
            inventoryService.deduct((Order) arg);
            return LocalTransactionState.COMMIT_MESSAGE;
        } catch (Exception e) {
            return LocalTransactionState.ROLLBACK_MESSAGE;
        }
    }
    
    // 回查：Broker 调用来确认本地事务状态
    @Override
    public LocalTransactionState checkLocalTransaction(MessageExt msg) {
        // 根据消息内容查询本地事务是否执行成功
        String orderId = msg.getKeys();
        boolean success = orderService.isOrderPaid(orderId);
        return success ? LocalTransactionState.COMMIT_MESSAGE 
                       : LocalTransactionState.ROLLBACK_MESSAGE;
    }
});
producer.sendMessageInTransaction(msg, order);
```

**追问**：事务消息和本地事务表方案有什么区别？

| 维度 | 事务消息 | 本地事务表 |
|------|---------|-----------|
| 实现复杂度 | 依赖 RocketMQ | 自己实现 |
| 可靠性 | 高（Broker 回查兜底） | 依赖定时任务扫描 |
| 实时性 | 近实时 | 取决于扫描间隔 |
| 侵入性 | 低（只需实现回查接口） | 高（每个业务表都要加事务日志表） |

---

### 4.6 批量消息

```java
List<Message> messages = new ArrayList<>();
messages.add(new Message("topic", "tag", "msg1".getBytes()));
messages.add(new Message("topic", "tag", "msg2".getBytes()));
producer.send(messages);
```

**注意**：批量消息必须属于同一个 Topic，且总大小不超过 4MB。

---

## 5. 消息可靠性

### 5.1 可靠性全链路

```
┌──────────────────────────────────────────────────────┐
│ [1] 同步刷盘 / 异步刷盘          Broker 存储阶段       │
│ [2] 同步复制 / 异步复制          Master→Slave 同步     │
│ [3] 消费重试 + 死信队列          消费失败兜底          │
│ [4] ACK 确认                    消费完成确认          │
└──────────────────────────────────────────────────────┘
```

### 5.2 刷盘机制

**问题**：Broker 收到消息后，什么时候算"可靠存储"？

| 模式 | 行为 | 可靠性 | 性能 |
|------|------|--------|------|
| **同步刷盘** | 消息写入磁盘后才返回成功 | 高 | 低 |
| **异步刷盘** | 写入 PageCache 即返回 | 中 | 高 |

```properties
# broker.conf
flushDiskType = SYNC_FLUSH   # 同步刷盘
flushDiskType = ASYNC_FLUSH  # 异步刷盘（默认）
```

---

### 5.3 主从复制

| 模式 | 行为 | 可靠性 | 性能 |
|------|------|--------|------|
| **同步复制** | Master 等 Slave 确认后才返回 | 高 | 低 |
| **异步复制** | Master 直接返回，异步同步给 Slave | 中 | 高 |

```properties
# broker.conf
brokerRole = SYNC_MASTER   # 同步主
brokerRole = ASYNC_MASTER  # 异步主（默认）
```

---

### 5.4 消费重试与死信队列

**问题**：消息消费失败怎么办？

**重试机制**：
```
第1次失败 → 10s 后重试
第2次失败 → 30s 后重试
第3次失败 → 1m 后重试
...
第16次失败 → 进入死信队列（DLQ）
```

**死信队列**：
- 命名规则：`%DLQ%消费者组名`
- 默认重试 16 次后进入死信
- 人工介入处理死信消息

```java
// 消费失败返回 RECONSUME_LATER，触发重试
consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
    try {
        process(msgs);
        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
    } catch (Exception e) {
        return ConsumeConcurrentlyStatus.RECONSUME_LATER;  // 触发重试
    }
});
```

---

## 6. 集群与高可用

### 6.1 集群架构

```
┌─────────────────────────────────────────────────┐
│  NameServer-1    NameServer-2    NameServer-3   │  ← 彼此独立，不通信
└──────┬───────────────┬───────────────┬──────────┘
       │               │               │
       ▼               ▼               ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  Broker-A    │ │  Broker-B    │ │  Broker-C    │
│  Master      │ │  Master      │ │  Master      │
│  Slave       │ │  Slave       │ │  Slave       │
└──────────────┘ └──────────────┘ └──────────────┘
```

### 6.2 集群模式

| 模式 | 说明 | 特点 |
|------|------|------|
| **单 Master** | 只有一台 Master | 测试用，无高可用 |
| **多 Master** | 多台 Master，无 Slave | 性能好，但宕机会丢消息 |
| **多 Master 多 Slave（异步）** | 每台 Master 配 Slave，异步复制 | 平衡可靠性和性能，主流方案 |
| **多 Master 多 Slave（同步）** | 每台 Master 配 Slave，同步复制 | 可靠性最高，但性能有损耗 |

---

### 6.3 Dledger（Raft 自动选主）

> RocketMQ 4.5+ 引入 Dledger，基于 Raft 协议实现自动选主。

**问题**：传统模式 Master 挂了怎么办？

- 传统模式：手动切换或依赖第三方（如 zk）
- **Dledger 模式**：自动选主，无需人工干预

```properties
# Dledger 配置
enableDLegerCommitLog = true
dLegerGroup = broker-group-1
dLegerPeers = n0-127.0.0.1:40911;n1-127.0.0.1:40912;n2-127.0.0.1:40913
dLegerSelfId = n0
```

---

## 7. 消费模式、重试与幂等

### 7.1 两种消费模式

| 模式 | 行为 | 适用场景 |
|------|------|----------|
| **集群消费（Clustering）** | 一条消息只被组内**一个**消费者处理 | 订单处理、库存扣减 |
| **广播消费（Broadcasting）** | 一条消息被组内**所有**消费者处理 | 配置刷新、缓存更新 |

```java
// 集群消费（默认）
consumer.setMessageModel(MessageModel.CLUSTERING);
// 广播消费
consumer.setMessageModel(MessageModel.BROADCASTING);
```

---

### 7.2 Push vs Pull

**问题**：RocketMQ 是 Push 还是 Pull？

**答案**：RocketMQ 底层是 **Pull（长轮询）**，但封装了 Push 的 API 外观。

| 方式 | 优点 | 缺点 |
|------|------|------|
| Push | 实时性好 | 消费者可能被压垮 |
| Pull | 消费者自主控制 | 可能有延迟 |
| **长轮询** | 折中方案 | — |

RocketMQ 的做法：Consumer 发起 Pull 请求，Broker 如果没有新消息，会 hold 住请求一段时间（默认 15s），有消息了立即返回，没消息超时返回空。这既保证了实时性，又不会让客户端空转。

---

### 7.3 消息幂等

**问题**：RocketMQ 如何保证消息不重复消费？

**前提认知**：RocketMQ **无法保证 exactly-once**，只能保证 **at-least-once**。去重需要业务自己实现。

```java
consumer.registerMessageListener((MessageListenerConcurrently) (msgs, context) -> {
    for (MessageExt msg : msgs) {
        String msgId = msg.getMsgId();  // 全局唯一
        
        // 方案1：Redis 去重
        Boolean ok = redisTemplate.opsForValue()
                .setIfAbsent("rocketmq:" + msgId, "1", 1, TimeUnit.HOURS);
        if (Boolean.FALSE.equals(ok)) {
            continue;  // 已处理，跳过
        }
        
        // 方案2：DB 唯一索引
        // INSERT INTO msg_consumed (msg_id) VALUES (?)
        
        process(msg);
    }
    return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
});
```

---

### 7.4 消息过滤

RocketMQ 支持两种过滤方式：

| 方式 | 原理 | 性能 | 灵活性 |
|------|------|------|--------|
| **Tag 过滤** | 消费者指定 Tag，Broker 端过滤 | 高 | 低（只能按 Tag） |
| **SQL 过滤** | 消费者指定 SQL 表达式，Broker 端过滤 | 中 | 高（可按属性） |

```java
// Tag 过滤：只消费 pay 和 cancel 标签
consumer.subscribe("order-topic", "pay || cancel");

// SQL 过滤：按属性过滤（需在 Broker 配置 enablePropertyFilter=true）
consumer.subscribe("order-topic", MessageSelector.bySql("amount > 100"));
```

---

## 8. 与其他 MQ 对比（Kafka / RabbitMQ）

### 8.1 核心对比表

| 维度 | RocketMQ | Kafka | RabbitMQ |
|------|----------|-------|----------|
| 开发语言 | Java | Java/Scala | Erlang |
| 单机吞吐量 | 十万级 | **百万级** | 万级 |
| 延迟 | 毫秒级 | 毫秒级 | 微秒级 |
| 消息顺序 | **分区有序** | 分区有序 | 单队列单消费者支持 |
| 事务消息 | **原生支持** ✅ | 不支持 | 不支持 |
| 延迟消息 | **18 个级别** ✅ | 不支持 | 插件支持 |
| 消息回溯 | **支持（按时间/offset）** | 支持 | 不支持 |
| 消息过滤 | Tag / SQL 表达式 | 不支持服务端过滤 | 不支持 |
| 消息轨迹 | **内置支持** ✅ | 需第三方 | 需插件 |
| 管理界面 | 自带 Web 控制台 | 需第三方 | 自带 Web 管理 |
| 社区生态 | 国内活跃，阿里主导 | 全球最大，Confluent 主导 | 老牌社区，VMware 维护 |

---

### 8.2 选型决策树

```
需要事务消息？
├── 是 → RocketMQ（唯一原生支持）
└── 否 → 需要超高吞吐量？
          ├── 是 → Kafka（日志/大数据场景）
          └── 否 → 路由复杂、管理方便？
                    ├── 是 → RabbitMQ
                    └── 否 → RocketMQ（综合最强）
```

---

## 9. Spring Boot / Spring Cloud Stream 落地

### 9.1 基础依赖

```xml
<dependency>
    <groupId>org.apache.rocketmq</groupId>
    <artifactId>rocketmq-spring-boot-starter</artifactId>
    <version>2.2.3</version>
</dependency>
```

### 9.2 配置文件

```yaml
rocketmq:
  name-server: 127.0.0.1:9876
  producer:
    group: order-producer-group
    send-message-timeout: 3000
    retry-times-when-send-failed: 2
  consumer:
    group: order-consumer-group
    topic: order-topic
```

### 9.3 生产者

```java
@Component
public class OrderProducer {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    // 普通消息
    public void sendOrder(Order order) {
        rocketMQTemplate.convertAndSend("order-topic:create", order);
    }

    // 延迟消息（第4级 = 30秒）
    public void sendDelayCancel(Order order) {
        rocketMQTemplate.syncSend("order-topic:cancel",
                MessageBuilder.withPayload(order).build(),
                3000,  // 超时
                4);    // 延迟级别
    }

    // 顺序消息
    public void sendOrderly(Order order) {
        rocketMQTemplate.syncSendOrderly("order-topic:pay", order, order.getOrderId());
    }

    // 事务消息
    public void sendTransaction(Order order) {
        rocketMQTemplate.sendMessageInTransaction(
                "tx-producer-group",
                "order-topic:pay",
                MessageBuilder.withPayload(order).build(),
                order
        );
    }
}
```

### 9.4 消费者

```java
@Component
@RocketMQMessageListener(
        topic = "order-topic",
        consumerGroup = "order-consumer-group",
        selectorExpression = "create || pay",  // Tag 过滤
        consumeMode = ConsumeMode.CONCURRENTLY  // 并发消费
)
public class OrderConsumer implements RocketMQListener<Order> {

    @Override
    public void onMessage(Order order) {
        // 业务处理
        orderService.process(order);
    }
}

// 顺序消费
@Component
@RocketMQMessageListener(
        topic = "order-topic",
        consumerGroup = "order-consumer-group",
        consumeMode = ConsumeMode.ORDERLY  // 顺序消费
)
public class OrderOrderlyConsumer implements RocketMQListener<Order> {
    @Override
    public void onMessage(Order order) {
        orderService.process(order);
    }
}
```

### 9.5 事务消息监听器

```java
@RocketMQTransactionListener
public class OrderTransactionListener implements RocketMQLocalTransactionListener {

    @Override
    public RocketMQLocalTransactionState executeLocalTransaction(Message msg, Object arg) {
        try {
            inventoryService.deduct((Order) arg);
            return RocketMQLocalTransactionState.COMMIT;
        } catch (Exception e) {
            return RocketMQLocalTransactionState.ROLLBACK;
        }
    }

    @Override
    public RocketMQLocalTransactionState checkLocalTransaction(Message msg) {
        String orderId = (String) msg.getHeaders().get("orderId");
        return orderService.isPaid(orderId)
                ? RocketMQLocalTransactionState.COMMIT
                : RocketMQLocalTransactionState.ROLLBACK;
    }
}
```

---

## 10. 速记对比表（面试最后 1 分钟）

### 10.1 核心概念速记

| 概念 | 一句话 |
|------|--------|
| NameServer | 轻量级路由中心，无状态，彼此不通信 |
| Broker | 消息存储与转发，Master 读写，Slave 备份 |
| Topic | 消息分类，类似"快递类型" |
| MessageQueue | Topic 下的物理分区，并行度的基础 |
| ConsumerGroup | 一组消费者的逻辑集合，组内分摊消息 |
| CommitLog | 所有消息顺序写入的物理文件 |
| ConsumeQueue | 消息的逻辑索引，快速定位 |

### 10.2 消息类型速记

| 类型 | 关键词 |
|------|--------|
| 普通消息 | 同步/异步/单向 |
| 顺序消息 | 同一个 Queue + 单线程消费 |
| 延迟消息 | 18 个延迟级别 |
| 事务消息 | 半消息 + 回查 + COMMIT/ROLLBACK |

### 10.3 可靠性速记

| 层面 | 机制 |
|------|------|
| 存储 | 同步刷盘 / 异步刷盘 |
| 复制 | 同步复制 / 异步复制 |
| 消费 | 重试 16 次 → 死信队列 |
| 高可用 | Dledger（Raft）自动选主 |

### 10.4 高频一问一答

| 问题 | 答案关键词 |
|------|-----------|
| 事务消息怎么实现？ | 半消息 + 本地事务执行 + 回查 + COMMIT/ROLLBACK |
| 如何保证顺序？ | 同一 Queue + MessageListenerOrderly |
| 如何保证幂等？ | msgId + Redis setIfAbsent / DB 唯一索引 |
| 消息丢了怎么办？ | 同步刷盘 + 同步复制 + 手动 ACK |
| NameServer 为什么不用 ZK？ | 轻量、AP 模型、无状态、部署简单 |
| 和 Kafka 主要区别？ | 事务消息、延迟消息、消息过滤、阿里主导 |
| 消费模式有几种？ | 集群消费（分摊）、广播消费（全收） |

---

> **面试建议**：重点掌握 **事务消息**（第 4.5 节）和 **存储架构**（第 3.4 节），这两块是 RocketMQ 面试区别于其他 MQ 的核心考点。MQ 选型对比能说出事务消息、延迟消息两大差异点即可。