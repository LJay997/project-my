package com.qq.ijay997.mq;

import com.rabbitmq.client.*;

// 消费者
public class TopicConsumer {
    private static final String EXCHANGE_NAME = "topic_logs";
    
    public void receiveMessages(String bindingKey) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("/");
        
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