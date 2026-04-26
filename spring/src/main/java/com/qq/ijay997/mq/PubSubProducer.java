package com.qq.ijay997.mq;

import com.rabbitmq.client.*;

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
