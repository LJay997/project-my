package com.qq.ijay997.outbox;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 远端调用调度配置。
 *
 * @author ijay997
 */
@Data
@Component
@ConfigurationProperties(prefix = "remote-call")
public class RemoteCallProperties {

    /** 每个批次包含的最大消息条数（批次大小） */
    private int batchSize = 50;

    /** 并发度：线程池核心/最大线程数，限制同时对远端发起的批次数 */
    private int concurrency = 4;

    /** 异步线程池等待队列容量 */
    private int queueCapacity = 100;

    /** 事务提交后是否自动异步调度发送（关闭后需手动调用 dispatch，便于确定性测试） */
    private boolean autoDispatch = true;
}