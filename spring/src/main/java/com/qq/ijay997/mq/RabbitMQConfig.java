package com.qq.ijay997.mq;

import org.springframework.amqp.core.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@ConditionalOnProperty(prefix = "spring.rabbitmq", name = "host")
public class RabbitMQConfig {
    
    // 简单队列
    @Bean
    public Queue simpleQueue() {
        return new Queue("simple.queue", true);
    }
    
    // 工作队列
    @Bean
    public Queue workQueue() {
        return new Queue("work.queue", true);
    }
    
    // 发布订阅
    @Bean
    public FanoutExchange fanoutExchange() {
        return new FanoutExchange("fanout.exchange");
    }
    
    @Bean
    public Queue fanoutQueue1() {
        return new Queue("fanout.queue1", true);
    }
    
    @Bean
    public Queue fanoutQueue2() {
        return new Queue("fanout.queue2", true);
    }
    
    @Bean
    public Binding fanoutBinding1(Queue fanoutQueue1, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(fanoutQueue1).to(fanoutExchange);
    }
    
    @Bean
    public Binding fanoutBinding2(Queue fanoutQueue2, FanoutExchange fanoutExchange) {
        return BindingBuilder.bind(fanoutQueue2).to(fanoutExchange);
    }
    
    // 路由模式
    @Bean
    public DirectExchange directExchange() {
        return new DirectExchange("direct.exchange");
    }
    
    @Bean
    public Queue directQueueError() {
        return new Queue("direct.queue.error", true);
    }
    
    @Bean
    public Queue directQueueAll() {
        return new Queue("direct.queue.all", true);
    }
    
    @Bean
    public Binding directBindingError(Queue directQueueError, DirectExchange directExchange) {
        return BindingBuilder.bind(directQueueError).to(directExchange).with("error");
    }
    
    @Bean
    public Binding directBindingAll(Queue directQueueAll, DirectExchange directExchange) {
        return BindingBuilder.bind(directQueueAll).to(directExchange).with("#");
    }
    
    // 主题模式
    @Bean
    public TopicExchange topicExchange() {
        return new TopicExchange("topic.exchange");
    }
    
    @Bean
    public Queue topicQueue1() {
        return new Queue("topic.queue1", true);
    }
    
    @Bean
    public Queue topicQueue2() {
        return new Queue("topic.queue2", true);
    }
    
    @Bean
    public Binding topicBinding1(Queue topicQueue1, TopicExchange topicExchange) {
        return BindingBuilder.bind(topicQueue1).to(topicExchange).with("*.error.*");
    }
    
    @Bean
    public Binding topicBinding2(Queue topicQueue2, TopicExchange topicExchange) {
        return BindingBuilder.bind(topicQueue2).to(topicExchange).with("kernel.#");
    }
    
    // 死信队列
    @Bean
    public DirectExchange dlxExchange() {
        return new DirectExchange("dlx.exchange");
    }
    
    @Bean
    public Queue dlxQueue() {
        return new Queue("dlx.queue", true);
    }
    
    @Bean
    public Binding dlxBinding(Queue dlxQueue, DirectExchange dlxExchange) {
        return BindingBuilder.bind(dlxQueue).to(dlxExchange).with("dlx.routing.key");
    }
    
    @Bean
    public Queue mainQueue() {
        Map<String, Object> args = new HashMap<>();
        args.put("x-dead-letter-exchange", "dlx.exchange");
        args.put("x-dead-letter-routing-key", "dlx.routing.key");
        args.put("x-message-ttl", 60000);
        return new Queue("main.queue", true, false, false, args);
    }
}