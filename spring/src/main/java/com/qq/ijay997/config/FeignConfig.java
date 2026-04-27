package com.qq.ijay997.config;

import feign.Logger;
import feign.Request;
import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * Feign 配置类
 * 
 * @author ijay997
 */
@Configuration
public class FeignConfig {

    /**
     * 配置 Feign 日志级别
     * NONE: 不记录日志（默认）
     * BASIC: 仅记录请求方法、URL、响应状态码和执行时间
     * HEADERS: 记录基本信息 + 请求和响应头
     * FULL: 记录完整的请求和响应信息
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;
    }

    /**
     * 配置请求超时时间
     */
    @Bean
    public Request.Options requestOptions() {
        // 连接超时 5 秒，读取超时 10 秒
        return new Request.Options(5000, TimeUnit.MILLISECONDS, 
                                   10000, TimeUnit.MILLISECONDS, 
                                   true);
    }

    /**
     * 配置重试机制
     * 注意：只有幂等操作才应该启用重试
     */
    @Bean
    public Retryer feignRetryer() {
        // 最大重试次数 3 次，初始间隔 100ms，最大间隔 1s
        return new Retryer.Default(100, TimeUnit.SECONDS.toMillis(1), 3);
    }
}
