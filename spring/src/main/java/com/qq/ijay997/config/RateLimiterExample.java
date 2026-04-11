package com.qq.ijay997.config;

import org.redisson.api.RRateLimiter;
import org.redisson.api.RateIntervalUnit;
import org.redisson.api.RateType;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
public class RateLimiterExample {

    private final RedissonClient redissonClient;

    private RRateLimiter rateLimiter;
    public RateLimiterExample(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;

        // 1. 获取一个名为 "myRateLimiter" 的限流器实例
        this.rateLimiter= redissonClient.getRateLimiter("myRateLimiter");

        // 2. 使用 setRate 方法强制设置限流规则
        // 规则为：全局限流，每秒允许 10 个请求
        rateLimiter.setRate(
                RateType.OVERALL, // 类型：全局限流
                1,               // 速率：10个请求
                1,                // 时间间隔：1
                RateIntervalUnit.MINUTES // 时间单位：秒
        );

        System.out.println("限流器配置已强制设置为：每10秒1个请求（全局）");
    }


    public void tryAcquirePermit() {
        RRateLimiter rateLimiter = redissonClient.getRateLimiter("myRateLimiter");
        
        // 尝试获取1个许可
        boolean isAcquired = rateLimiter.tryAcquire(1);
        if (isAcquired) {
            System.out.println("成功获取许可，执行业务逻辑...");
            // 执行业务逻辑
        } else {
            System.out.println("获取许可失败，请求被限流。");
            // 处理限流逻辑，如返回错误或等待
            throw new RuntimeException("请求被限流");
        }
    }
}