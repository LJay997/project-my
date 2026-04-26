package com.qq.ijay997.mq;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;
import lombok.SneakyThrows;

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

    @SneakyThrows
    private void doWork(String task) {
        Thread.sleep(task.length() * 1000);
    }
}