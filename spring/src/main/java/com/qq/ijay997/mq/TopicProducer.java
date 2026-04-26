package com.qq.ijay997.mq;

import com.rabbitmq.client.*;

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
