package com.qq.ijay997.config;

import feign.RequestInterceptor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Feign 请求拦截器配置类
 * 
 * 根据 spring.profiles.active 或自定义配置，动态加载不同的拦截器
 * 
 * 使用方式：
 * 1. 开发环境：设置 spring.profiles.active=dev 或 feign.interceptor.type=dev
 * 2. 生产环境：设置 spring.profiles.active=prod 或 feign.interceptor.type=prod
 * 
 * @author ijay997
 */
@Configuration
public class FeignInterceptorConfig {

    /**
     * 开发环境拦截器配置
     * 
     * 条件：当 feign.interceptor.type=dev 或 spring.profiles.active 包含 dev 时启用
     */
    @Bean
    @ConditionalOnProperty(
        name = "feign.interceptor.type", 
        havingValue = "dev", 
        matchIfMissing = true  // 默认使用开发环境
    )
    public RequestInterceptor devRequestInterceptor() {
        return new DevRequestInterceptor();
    }

    /**
     * 生产环境拦截器配置
     * 
     * 条件：当 feign.interceptor.type=prod 时启用
     */
    @Bean
    @ConditionalOnProperty(
        name = "feign.interceptor.type", 
        havingValue = "prod"
    )
    public RequestInterceptor prodRequestInterceptor() {
        return new ProdRequestInterceptor();
    }

    /**
     * 自定义拦截器（可选）
     * 
     * 条件：当 feign.interceptor.custom-enabled=true 时启用
     * 可以添加额外的通用拦截逻辑
     */
    @Bean
    @ConditionalOnProperty(
        name = "feign.interceptor.custom-enabled", 
        havingValue = "true", 
        matchIfMissing = false
    )
    public RequestInterceptor customRequestInterceptor() {
        return template -> {
            // 添加通用的请求头
            template.header("X-Client-Version", "1.0.0");
            template.header("X-Request-Source", "spring-boot-app");
            
            // 可以在这里添加限流、熔断等逻辑
            System.out.println("[Custom Interceptor] 处理请求: " + template.method() + " " + template.path());
        };
    }
}
