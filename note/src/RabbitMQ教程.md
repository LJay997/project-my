# RabbitMQ 消息队列完整教程

## 目录
1. [RabbitMQ 简介](#rabbitmq-简介)
2. [核心概念](#核心概念)
3. [五种工作模式详解](#五种工作模式详解)
4. [高级特性](#高级特性)
5. [最佳实践](#最佳实践)
6. [常见问题](#常见问题)

---

## RabbitMQ 简介

RabbitMQ 是一个开源的消息代理和队列服务器，基于 AMQP（Advanced Message Queuing Protocol）协议实现。它用于在分布式系统中存储和转发消息，实现应用解耦、异步处理和流量削峰。

### 主要特点
- **可靠性**: 支持持久化、传输确认、发布确认等机制
- **灵活性**: 支持多种消息路由模式和插件扩展
- **高可用**: 支持集群和镜像队列
- **多语言支持**: 提供多种编程语言的客户端库

---

## 核心概念

### 1. Producer（生产者）
创建并发送消息的应用程序。

### 2. Consumer（消费者）
接收并处理消息的应用程序。

### 3. Queue（队列）
消息的容器，存储在 RabbitMQ 服务器中。消息只能存储在队列中，遵循 FIFO（先进先出）原则。

### 4. Exchange（交换机）
接收生产者发送的消息，并根据路由规则将消息路由到一个或多个队列。

### 5. Binding（绑定）
连接 Exchange 和 Queue 的规则，定义消息如何从交换机路由到队列。

### 6. Routing Key（路由键）
生产者发送消息时指定的标识符，Exchange 根据路由键决定消息的去向。

### 7. Virtual Host（虚拟主机）
逻辑隔离单元，类似数据库中的 schema，不同虚拟主机之间的资源完全隔离。

---

## 五种工作模式详解

### 1. 简单模式（Simple/Hello World）

最简单的模式，一个生产者对应一个消费者。

```
Producer → Queue → Consumer
```

**特点：**
- 一对一关系
- 无需交换机，使用默认交换机
- 适合简单的任务分发场景

**应用场景：**
- 日志记录
- 简单的异步通知

**代码示例：**

```java
// 生产者
public class SimpleProducer {
    private static final String QUEUE_NAME = "hello";
    
    public void sendMessage(String message) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            
            // 声明队列
            channel.queueDeclare(QUEUE_NAME, false, false, false, null);
            
            // 发送消息
            channel.basicPublish("", QUEUE_NAME, null, message.getBytes());
            System.out.println("Sent: " + message);
        }
    }
}

// 消费者
public class SimpleConsumer {
    private static final String QUEUE_NAME = "hello";
    
    public void receiveMessage() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        
        // 声明队列
        channel.queueDeclare(QUEUE_NAME, false, false, false, null);
        
        // 创建消费者
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println("Received: " + message);
        };
        
        // 消费消息
        channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {});
    }
}
```

---

### 2. 工作队列模式（Work Queues）

一个生产者对应多个消费者，消息会被均匀分发给所有消费者。

```
Producer → Queue → Consumer1
                → Consumer2
                → Consumer3
```

**特点：**
- 一对多关系
- 消息轮询分发（Round-Robin）
- 每个消息只被一个消费者处理
- 支持公平分发（basicQos）

**应用场景：**
- 任务分发系统
- 订单处理
- 图片/视频处理

**代码示例：**

```java
// 生产者 - 发送多个任务
public class WorkQueueProducer {
    private static final String QUEUE_NAME = "task_queue";
    
    public void sendTasks() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            
            // 声明持久化队列
            channel.queueDeclare(QUEUE_NAME, true, false, false, null);
            
            for (int i = 0; i < 10; i++) {
                String message = "Task " + i;
                
                // 设置消息持久化
                AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
                    .deliveryMode(2) // 持久化
                    .build();
                
                channel.basicPublish("", QUEUE_NAME, properties, message.getBytes());
                System.out.println("Sent: " + message);
            }
        }
    }
}

// 消费者 - 公平分发
public class WorkQueueConsumer {
    private static final String QUEUE_NAME = "task_queue";
    
    public void startWorker(int workerId) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        
        // 声明队列
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        
        // 设置公平分发：每次只处理一个消息
        channel.basicQos(1);
        
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println("Worker " + workerId + " Received: " + message);
            
            try {
                // 模拟耗时操作
                doWork(message);
            } finally {
                System.out.println("Worker " + workerId + " Done");
                // 手动确认消息
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        
        // 手动确认模式
        channel.basicConsume(QUEUE_NAME, false, deliverCallback, consumerTag -> {});
    }
    
    private void doWork(String task) throws InterruptedException {
        Thread.sleep(task.length() * 1000);
    }
}
```

---

### 3. 发布订阅模式（Publish/Subscribe）

一个生产者发送的消息会被所有消费者接收，通过 Fanout 交换机实现。

```
Producer → Fanout Exchange → Queue1 → Consumer1
                          → Queue2 → Consumer2
                          → Queue3 → Consumer3
```

**特点：**
- 广播模式
- 使用 Fanout 交换机
- 不需要 routing key
- 所有绑定的队列都会收到消息副本

**应用场景：**
- 缓存更新
- 日志广播
- 配置刷新

**代码示例：**

```java
// 生产者
public class PubSubProducer {
    private static final String EXCHANGE_NAME = "logs";
    
    public void publishMessage(String message) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            
            // 声明 Fanout 交换机
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
            
            // 发送消息（不需要 routing key）
            channel.basicPublish(EXCHANGE_NAME, "", null, message.getBytes());
            System.out.println("Published: " + message);
        }
    }
}

// 消费者
public class PubSubConsumer {
    private static final String EXCHANGE_NAME = "logs";
    
    public void subscribe(String consumerName) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        
        // 声明交换机
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.FANOUT);
        
        // 创建临时队列（独占、自动删除）
        String queueName = channel.queueDeclare().getQueue();
        
        // 绑定队列到交换机
        channel.queueBind(queueName, EXCHANGE_NAME, "");
        
        System.out.println(consumerName + " waiting for messages...");
        
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(consumerName + " Received: " + message);
        };
        
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }
}
```

---

### 4. 路由模式（Routing）

生产者发送消息时指定 routing key，消费者根据 binding key 选择性接收消息。使用 Direct 交换机。

```
Producer → Direct Exchange → Queue1 (routing.key=error) → Consumer1
                          → Queue2 (routing.key=info/warning/error) → Consumer2
```

**特点：**
- 精确匹配
- 使用 Direct 交换机
- routing key 必须完全匹配 binding key
- 可以实现消息过滤

**应用场景：**
- 日志分级处理（error、warning、info）
- 订单状态通知
- 消息分类处理

**代码示例：**

```java
// 生产者
public class RoutingProducer {
    private static final String EXCHANGE_NAME = "direct_logs";
    
    public void sendLog(String severity, String message) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            
            // 声明 Direct 交换机
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
            
            // 发送消息，指定 routing key
            channel.basicPublish(EXCHANGE_NAME, severity, null, message.getBytes());
            System.out.println("Sent [" + severity + "] : " + message);
        }
    }
}

// 消费者
public class RoutingConsumer {
    private static final String EXCHANGE_NAME = "direct_logs";
    
    public void receiveLogs(String[] severities) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        
        // 声明交换机
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.DIRECT);
        
        // 创建临时队列
        String queueName = channel.queueDeclare().getQueue();
        
        // 绑定队列，指定感兴趣的 severity
        for (String severity : severities) {
            channel.queueBind(queueName, EXCHANGE_NAME, severity);
        }
        
        System.out.println("Waiting for messages with severities: " + 
                          Arrays.toString(severities));
        
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println("Received [" + delivery.getEnvelope().getRoutingKey() + 
                             "] : " + message);
        };
        
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }
}

// 使用示例
// 消费者1：只接收 error 级别日志
// consumer.receiveLogs(new String[]{"error"});

// 消费者2：接收所有级别日志
// consumer.receiveLogs(new String[]{"info", "warning", "error"});
```

---

### 5. 主题模式（Topics）

最灵活的路由模式，使用通配符进行模式匹配。使用 Topic 交换机。

```
Producer → Topic Exchange → Queue1 (*.orange.*) → Consumer1
                         → Queue2 (*.*.rabbit / lazy.#) → Consumer2
```

**特点：**
- 模式匹配
- 使用 Topic 交换机
- 支持通配符：`*`（匹配一个单词）、`#`（匹配零个或多个单词）
- routing key 是由点分隔的单词序列

**通配符规则：**
- `*`：匹配恰好一个单词
- `#`：匹配零个或多个单词
- 单词之间用 `.` 分隔

**应用场景：**
- 复杂的日志路由
- 多条件消息过滤
- 事件驱动架构

**代码示例：**

```java
// 生产者
public class TopicProducer {
    private static final String EXCHANGE_NAME = "topic_logs";
    
    public void sendMessage(String routingKey, String message) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel()) {
            
            // 声明 Topic 交换机
            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
            
            channel.basicPublish(EXCHANGE_NAME, routingKey, null, message.getBytes());
            System.out.println("Sent [" + routingKey + "] : " + message);
        }
    }
}

// 消费者
public class TopicConsumer {
    private static final String EXCHANGE_NAME = "topic_logs";
    
    public void receiveMessages(String bindingKey) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        
        // 声明交换机
        channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);
        
        // 创建临时队列
        String queueName = channel.queueDeclare().getQueue();
        
        // 绑定队列，使用通配符
        channel.queueBind(queueName, EXCHANGE_NAME, bindingKey);
        
        System.out.println("Waiting for messages with binding key: " + bindingKey);
        
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println("Received [" + delivery.getEnvelope().getRoutingKey() + 
                             "] : " + message);
        };
        
        channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
    }
}

// 使用示例
// routing key 格式：<facility>.<severity>.<module>
// 例如：kernel.error.auth, user.info.login

// 消费者1：接收所有 error 级别的消息
// consumer.receiveMessages("*.error.*");

// 消费者2：接收 kernel 模块的所有消息
// consumer.receiveMessages("kernel.#");

// 消费者3：接收 auth 模块的所有消息
// consumer.receiveMessages("#.auth");

// 消费者4：接收所有消息
// consumer.receiveMessages("#");
```

---

## 高级特性

### 1. 消息确认机制

#### 自动确认（Auto Ack）
```java
// 简单但不可靠，消息一旦发送就认为已处理
channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {});
```

#### 手动确认（Manual Ack）
```java
// 可靠，处理完成后才确认
channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {});

// 在回调中确认
channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);

// 批量确认
channel.basicAck(deliveryTag, true);

// 拒绝消息
channel.basicNack(deliveryTag, false, true); // requeue=true 重新入队
channel.basicReject(deliveryTag, false);      // 不重新入队
```

### 2. 消息持久化

```java
// 队列持久化
channel.queueDeclare(queueName, true, false, false, null);

// 消息持久化
AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
    .deliveryMode(2) // 2=持久化, 1=非持久化
    .build();
channel.basicPublish(exchange, routingKey, properties, message.getBytes());
```

### 3. 死信队列（DLX）

```java
// 声明死信交换机
channel.exchangeDeclare("dlx_exchange", BuiltinExchangeType.DIRECT);
channel.queueDeclare("dlx_queue", true, false, false, null);
channel.queueBind("dlx_queue", "dlx_exchange", "dlx_routing_key");

// 声明主队列，设置死信参数
Map<String, Object> args = new HashMap<>();
args.put("x-dead-letter-exchange", "dlx_exchange");
args.put("x-dead-letter-routing-key", "dlx_routing_key");
args.put("x-message-ttl", 60000); // 消息 TTL 60秒

channel.queueDeclare("main_queue", true, false, false, args);
```

**消息成为死信的三种情况：**
1. 消息被拒绝（basic.reject/nack）且 requeue=false
2. 消息过期（TTL）
3. 队列达到最大长度

### 4. 延迟队列

RabbitMQ 本身不支持延迟队列，可以通过以下方式实现：

#### 方式一：TTL + 死信队列
```java
// 见上面的死信队列示例，设置 x-message-ttl
```

#### 方式二：延迟插件（推荐）
```bash
# 安装 rabbitmq-delayed-message-exchange 插件
rabbitmq-plugins enable rabbitmq_delayed_message_exchange
```

```java
// 使用延迟交换机
Map<String, Object> args = new HashMap<>();
args.put("x-delayed-type", "direct");
channel.exchangeDeclare("delayed_exchange", "x-delayed-message", true, false, args);

// 发送延迟消息
AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
    .headers(Collections.singletonMap("x-delay", 5000)) // 延迟 5 秒
    .build();
channel.basicPublish("delayed_exchange", "routing_key", properties, message.getBytes());
```

### 5. 优先级队列

```java
// 声明优先级队列
Map<String, Object> args = new HashMap<>();
args.put("x-max-priority", 10); // 最大优先级 10
channel.queueDeclare("priority_queue", true, false, false, args);

// 发送高优先级消息
AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder()
    .priority(8) // 优先级 8
    .build();
channel.basicPublish("", "priority_queue", properties, message.getBytes());
```

### 6. 事务机制

```java
// 开启事务
channel.txSelect();

try {
    channel.basicPublish("", queueName, null, message.getBytes());
    // 提交事务
    channel.txCommit();
} catch (Exception e) {
    // 回滚事务
    channel.txRollback();
}
```

**注意：** 事务会严重影响性能，推荐使用 Confirm 模式。

### 7. 发布者确认（Publisher Confirms）

```java
// 启用确认模式
channel.confirmSelect();

// 异步确认
channel.addConfirmListener(new ConfirmListener() {
    @Override
    public void handleAck(long deliveryTag, boolean multiple) {
        System.out.println("Message acknowledged: " + deliveryTag);
    }
    
    @Override
    public void handleNack(long deliveryTag, boolean multiple) {
        System.out.println("Message not acknowledged: " + deliveryTag);
    }
});

// 发送消息
channel.basicPublish("", queueName, null, message.getBytes());

// 等待确认
if (!channel.waitForConfirms()) {
    System.out.println("Message not confirmed");
}
```

### 8. QoS 流控

```java
// 限制未确认消息数量
channel.basicQos(1);     // 每次只处理 1 条消息
channel.basicQos(10);    // 每次最多处理 10 条消息
channel.basicQos(0, 10, false); // prefetchSize=0, prefetchCount=10, global=false
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
    listener:
      simple:
        acknowledge-mode: manual  # 手动确认
        concurrency: 5            # 最小消费者数量
        max-concurrency: 10       # 最大消费者数量
        prefetch: 1               # 每次预取消息数
```

### 3. 配置类

```java
@Configuration
public class RabbitMQConfig {
    
    // 简单队列
    @Bean
    public Queue simpleQueue() {
        return new Queue("simple.queue", true);
    }
    
    // 工作队列
    @Bean
    public Queue workQueue() {
        return new Queue("work.queue", true);
    }
    
    // 发布订阅
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
    public Binding fanoutBinding1(Queue fanoutQueue1, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(fanoutQueue1).to(fanoutExchange);
    }
    
    @Bean
    public Binding fanoutBinding2(Queue fanoutQueue2, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(fanoutQueue2).to(fanoutExchange);
    }
    
    // 路由模式
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("direct.exchange");
    }
    
    @Bean
    public Queue directQueueError() {
        return new Queue("direct.queue.error", true);
    }
    
    @Bean
    public Queue directQueueAll() {
        return new Queue("direct.queue.all", true);
    }
    
    @Bean
    public Binding directBindingError(Queue directQueueError, DirectExchange directExchange) {
        return BindingBuilder.bind(directQueueError).to(directExchange).with("error");
    }
    
    @Bean
    public Binding directBindingAll(Queue directQueueAll, DirectExchange directExchange) {
        return BindingBuilder.bind(directQueueAll).to(directExchange).with("#");
    }
    
    // 主题模式
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange("topic.exchange");
    }
    
    @Bean
    public Queue topicQueue1() {
        return new Queue("topic.queue1", true);
    }
    
    @Bean
    public Queue topicQueue2() {
        return new Queue("topic.queue2", true);
    }
    
    @Bean
    public Binding topicBinding1(Queue topicQueue1, TopicExchange topicExchange) {
        return BindingBuilder.bind(topicQueue1).to(topicExchange).with("*.error.*");
    }
    
    @Bean
    public Binding topicBinding2(Queue topicQueue2, TopicExchange topicExchange) {
        return BindingBuilder.bind(topicQueue2).to(topicExchange).with("kernel.#");
    }
    
    // 死信队列
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("dlx.exchange");
    }
    
    @Bean
    public Queue dlxQueue() {
        return new Queue("dlx.queue", true);
    }
    
    @Bean
    public Binding dlxBinding(Queue dlxQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(dlxQueue).to(dlxExchange).with("dlx.routing.key");
    }
    
    @Bean
    public Queue mainQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "dlx.exchange");
        args.put("x-dead-letter-routing-key", "dlx.routing.key");
        args.put("x-message-ttl", 60000);
        return new Queue("main.queue", true, false, false, args);
    }
}
```

### 4. 生产者

```java
@Component
public class MessageProducer {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendSimpleMessage(String message) {
        rabbitTemplate.convertAndSend("simple.queue", message);
    }
    
    public void sendFanoutMessage(String message) {
        rabbitTemplate.convertAndSend("fanout.exchange", "", message);
    }
    
    public void sendDirectMessage(String routingKey, String message) {
        rabbitTemplate.convertAndSend("direct.exchange", routingKey, message);
    }
    
    public void sendTopicMessage(String routingKey, String message) {
        rabbitTemplate.convertAndSend("topic.exchange", routingKey, message);
    }
}
```

### 5. 消费者

```java
@Component
public class MessageConsumer {
    
    @RabbitListener(queues = "simple.queue")
    public void receiveSimpleMessage(String message) {
        System.out.println("Received: " + message);
    }
    
    @RabbitListener(queues = "work.queue")
    public void receiveWorkMessage(String message, Channel channel, 
                                   @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
        try {
            System.out.println("Processing: " + message);
            // 处理业务逻辑
            channel.basicAck(tag, false);
        } catch (Exception e) {
            try {
                channel.basicNack(tag, false, true); // 重新入队
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
    
    @RabbitListener(queues = "fanout.queue1")
    public void receiveFanoutMessage1(String message) {
        System.out.println("Fanout Queue1 Received: " + message);
    }
    
    @RabbitListener(queues = "fanout.queue2")
    public void receiveFanoutMessage2(String message) {
        System.out.println("Fanout Queue2 Received: " + message);
    }
    
    @RabbitListener(queues = "direct.queue.error")
    public void receiveDirectErrorMessage(String message) {
        System.out.println("Error Log: " + message);
    }
    
    @RabbitListener(queues = "direct.queue.all")
    public void receiveDirectAllMessage(String message) {
        System.out.println("All Log: " + message);
    }
    
    @RabbitListener(queues = "topic.queue1")
    public void receiveTopicMessage1(String message) {
        System.out.println("Topic Queue1 Received: " + message);
    }
    
    @RabbitListener(queues = "topic.queue2")
    public void receiveTopicMessage2(String message) {
        System.out.println("Topic Queue2 Received: " + message);
    }
}
```

---

## 最佳实践

### 1. 消息可靠性保证

- **生产者端：**
  - 启用 Publisher Confirms
  - 消息持久化
  - 重试机制
  
- **Broker 端：**
  - 队列持久化
  - 镜像队列（高可用）
  - 集群部署
  
- **消费者端：**
  - 手动确认
  - 幂等性处理
  - 异常处理和重试

### 2. 性能优化

- 合理设置 prefetch count
- 批量发送和确认消息
- 避免频繁创建连接和通道（使用连接池）
- 控制队列长度，防止消息堆积
- 监控队列深度和消费速率

### 3. 幂等性设计

```java
@RabbitListener(queues = "order.queue")
public void processOrder(Order order, Channel channel, 
                        @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
    String messageId = order.getId();
    
    // 检查是否已处理
    if (isProcessed(messageId)) {
        channel.basicAck(tag, false);
        return;
    }
    
    try {
        // 处理业务逻辑
        processBusinessLogic(order);
        
        // 标记为已处理
        markAsProcessed(messageId);
        
        channel.basicAck(tag, false);
    } catch (Exception e) {
        channel.basicNack(tag, false, false); // 不重新入队，进入死信队列
    }
}
```

### 4. 监控和告警

- 监控队列长度
- 监控消息积压
- 监控消费者健康状态
- 设置告警阈值

### 5. 错误处理策略

```java
// 重试策略
@RabbitListener(queues = "main.queue")
public void handleMessage(Message message, Channel channel,
                         @Header(AmqpHeaders.DELIVERY_TAG) long tag) {
    int retryCount = getRetryCount(message);
    
    if (retryCount > MAX_RETRY) {
        // 超过最大重试次数，发送到死信队列
        channel.basicNack(tag, false, false);
        return;
    }
    
    try {
        processMessage(message);
        channel.basicAck(tag, false);
    } catch (Exception e) {
        // 增加重试次数
        incrementRetryCount(message);
        // 延迟后重新入队
        channel.basicNack(tag, false, true);
    }
}
```

---

## 常见问题

### 1. 消息丢失问题

**原因：**
- 生产者发送失败
- Broker 宕机
- 消费者处理失败但未正确处理

**解决方案：**
- 启用 Publisher Confirms
- 消息和队列持久化
- 手动确认机制
- 死信队列兜底

### 2. 消息重复消费

**原因：**
- 网络抖动导致重复投递
- 消费者重启
- 确认超时

**解决方案：**
- 实现幂等性（唯一键、去重表、Redis）
- 业务层面判断是否已处理

### 3. 消息积压

**原因：**
- 消费者处理能力不足
- 消费者宕机
- 消息生产速度过快

**解决方案：**
- 增加消费者数量
- 优化消费者处理逻辑
- 临时扩容
- 设置队列最大长度

### 4. 顺序性问题

**解决方案：**
- 将需要保证顺序的消息发送到同一个队列
- 使用 routing key 确保相关消息路由到同一队列
- 单线程消费

### 5. 连接泄漏

**解决方案：**
- 使用连接池
- 正确关闭连接和通道
- 监控连接数

---

## 总结

| 模式 | 交换机类型 | 特点 | 适用场景 |
|------|-----------|------|---------|
| 简单模式 | 默认交换机 | 一对一 | 简单任务 |
| 工作队列 | 默认交换机 | 一对多，负载均衡 | 任务分发 |
| 发布订阅 | Fanout | 广播，无路由 | 日志广播、缓存更新 |
| 路由模式 | Direct | 精确匹配 | 日志分级、消息过滤 |
| 主题模式 | Topic | 通配符匹配 | 复杂路由、事件驱动 |

选择哪种模式取决于具体的业务需求：
- 需要广播？→ Fanout
- 需要精确路由？→ Direct
- 需要灵活匹配？→ Topic
- 需要负载均衡？→ Work Queues

---

## 参考资料

- [RabbitMQ 官方文档](https://www.rabbitmq.com/documentation.html)
- [AMQP 协议规范](https://www.amqp.org/)
- [Spring AMQP 文档](https://spring.io/projects/spring-amqp)
