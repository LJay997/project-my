package com.qq.ijay997.dubbo.config;

import org.apache.dubbo.config.ApplicationConfig;
import org.apache.dubbo.config.ProtocolConfig;
import org.apache.dubbo.config.RegistryConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Dubbo 配置类（Zookeeper 版本）
 * 
 * 使用 Profile 激活：@ActiveProfiles("zookeeper")
 * 只有在配置文件中设置了 dubbo.registry.address 时才启用
 * 
 * @author ijay997
 */
@Configuration
@Profile("zookeeper")
@ConditionalOnProperty(prefix = "dubbo", name = "registry.address")
public class DubboZookeeperConfig {

    /**
     * 应用配置
     */
    @Bean
    public ApplicationConfig applicationConfig() {
        ApplicationConfig applicationConfig = new ApplicationConfig();
        applicationConfig.setName("spring-dubbo-demo-zk");
        applicationConfig.setQosEnable(true);
        return applicationConfig;
    }

    /**
     * 注册中心配置（使用 Zookeeper）
     */
    @Bean
    public RegistryConfig registryConfig() {
        RegistryConfig registryConfig = new RegistryConfig();
        registryConfig.setAddress("zookeeper://127.0.0.1:2181");
        return registryConfig;
    }

    /**
     * 协议配置
     */
    @Bean
    public ProtocolConfig protocolConfig() {
        ProtocolConfig protocolConfig = new ProtocolConfig();
        protocolConfig.setName("dubbo");
        protocolConfig.setPort(20881);
        return protocolConfig;
    }
}
