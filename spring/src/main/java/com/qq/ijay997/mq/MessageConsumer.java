package com.qq.ijay997.mq;

import com.rabbitmq.client.Channel;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
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