package com.qq.ijay997.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

// 生产者
public class SimpleProducer {
    private static final String QUEUE_NAME = "hello";
    
    public void sendMessage(String message) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        factory.setPort(5672);          // RabbitMQ 默认端口
        factory.setUsername("guest");   // 默认用户名
        factory.setPassword("guest");   // 默认密码
        factory.setVirtualHost("/");    // 默认虚拟主机
        
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

