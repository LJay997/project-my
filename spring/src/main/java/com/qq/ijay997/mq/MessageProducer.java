package com.qq.ijay997.mq;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
public class MessageProducer implements CommandLineRunner {
    
    @Autowired
    private RabbitTemplate rabbitTemplate;
    
    public void sendSimpleMessage(String message) {
        rabbitTemplate.convertAndSend("simple.queue", message);
    }
    
    public void sendFanoutMessage(String message) {
        rabbitTemplate.convertAndSend("fanout.exchange", "", message);
    }
    
    public void sendDirectMessage(String routingKey, String message) {
        rabbitTemplate.convertAndSend("direct.exchange", routingKey, message);
    }
    
    public void sendTopicMessage(String routingKey, String message) {
        rabbitTemplate.convertAndSend("topic.exchange", routingKey, message);
    }

    @Override
    public void run(String... args) throws Exception {
        this.sendSimpleMessage("Hello, Simple Queue!");
        this.sendFanoutMessage("Hello, Fanout Exchange!");
        this.sendDirectMessage("direct.queue.error", "Hello, Direct Exchange - Error!");
        this.sendDirectMessage("direct.queue.all", "Hello, Direct Exchange - All!");
        this.sendTopicMessage("topic.queue1", "Hello, Topic Exchange - Queue1!");

    }
}