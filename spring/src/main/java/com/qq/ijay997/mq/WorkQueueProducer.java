package com.qq.ijay997.mq;

import com.rabbitmq.client.*;

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

