package com.qq.ijay997.mq;

import com.rabbitmq.client.*;

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