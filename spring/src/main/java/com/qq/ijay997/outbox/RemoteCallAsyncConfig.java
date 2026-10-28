package com.qq.ijay997.outbox;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

/**
 * 远端调用异步执行配置。
 * <p>
 * 线程池核心/最大线程数取 {@link RemoteCallProperties#getConcurrency()}，
 * 从线程池维度限制同时对远端发起的并发批次数，避免压垮远端服务。
 *
 * @author ijay997
 */
@Configuration
@EnableAsync
public class RemoteCallAsyncConfig {

    @Bean("remoteCallExecutor")
    public ThreadPoolTaskExecutor remoteCallExecutor(RemoteCallProperties properties) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(properties.getConcurrency());
        executor.setMaxPoolSize(properties.getConcurrency());
        executor.setQueueCapacity(properties.getQueueCapacity());
        executor.setThreadNamePrefix("remote-call-");
        // 队列满时由提交线程自行执行，保证不丢失消息
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}