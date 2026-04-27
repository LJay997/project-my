package com.qq.ijay997.mq;

import com.rabbitmq.client.*;

import java.util.Arrays;

// 消费者
public class RoutingConsumer {
    private static final String EXCHANGE_NAME = "direct_logs";
    
    public void receiveLogs(String[] severities) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");
        factory.setVirtualHost("/");
        
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