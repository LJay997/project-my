package com.qq.ijay997.dubbo.config;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Dubbo 配置类（直连模式）
 * 
 * 无需注册中心，适合本地开发测试
 * 使用 Profile 激活：@ActiveProfiles("direct")
 * 
 * @author ijay997
 */
@Configuration
@Profile("direct")
public class DubboDirectConfig {

    /**
     * 应用配置
     */
    @Bean
    public ApplicationConfig applicationConfig() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("spring-dubbo-demo-direct");
        return applicationConfig;
    }

    /**
     * 协议配置
     */
    @Bean
    public ProtocolConfig protocolConfig() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName("dubbo");
        protocolConfig.setPort(20882);
        return protocolConfig;
    }
}
