package com.qq.ijay997.mq;

import com.rabbitmq.client.*;

import java.util.Arrays;

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
