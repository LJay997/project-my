package com.qq.ijay997.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * 基于 Spring Profile 的 Feign 拦截器配置
 * 
 * 这是使用 @Profile 注解的另一种方式，与 FeignInterceptorConfig 二选一使用
 * 
 * 使用方式：
 * 1. 开发环境：启动时指定 --spring.profiles.active=dev
 * 2. 生产环境：启动时指定 --spring.profiles.active=prod
 * 
 * @author ijay997
 */
@Configuration
public class FeignProfileInterceptorConfig {

    /**
     * 开发环境拦截器
     * 
     * 只在 spring.profiles.active 包含 "dev" 时启用
     */
    @Bean
    @Profile("dev")
    public RequestInterceptor devProfileInterceptor() {
        return new DevRequestInterceptor();
    }

    /**
     * 生产环境拦截器
     * 
     * 只在 spring.profiles.active 包含 "prod" 时启用
     */
    @Bean
    @Profile("prod")
    public RequestInterceptor prodProfileInterceptor() {
        return new ProdRequestInterceptor();
    }

    /**
     * 测试环境拦截器（示例）
     * 
     * 只在 spring.profiles.active 包含 "test" 时启用
     */
    @Bean
    @Profile("test")
    public RequestInterceptor testProfileInterceptor() {
        return template -> {
            // 测试环境添加特殊标识
            template.header("X-Environment", "test");
            template.header("X-Test-Mode", "true");
            
            System.out.println("[Test Interceptor] 测试环境请求: " + template.path());
        };
    }
}
