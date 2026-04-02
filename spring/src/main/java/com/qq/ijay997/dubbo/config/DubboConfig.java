package com.qq.ijay997.dubbo.config;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ConsumerConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Dubbo 配置类
 * 
 * 只有在配置文件中设置了 dubbo.registry.address 时才启用
 * 
 * @author ijay997
 */
@Configuration
@ConditionalOnProperty(prefix = "dubbo", name = "registry.address")
public class DubboConfig {

    /**
     * 应用配置
     */
    @Bean
    public ApplicationConfig applicationConfig() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("spring-dubbo-demo");
        applicationConfig.setQosEnable(true);
        return applicationConfig;
    }

    /**
     * 注册中心配置（使用 Nacos）
     * 如果没有 Nacos，可以改用 Zookeeper 或直连方式
     */
    @Bean
    public RegistryConfig registryConfig() {
        RegistryConfig registryConfig = new RegistryConfig();
        // 使用 Nacos 作为注册中心
        registryConfig.setAddress("nacos://127.0.0.1:8848");
        // 如果要用 Zookeeper，使用下面这行：
        // registryConfig.setAddress("zookeeper://127.0.0.1:2181");
        return registryConfig;
    }

    /**
     * 协议配置
     */
    @Bean
    public ProtocolConfig protocolConfig() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName("dubbo");
        protocolConfig.setPort(20880);
        return protocolConfig;
    }
}
