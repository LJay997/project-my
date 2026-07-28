package com.qq.ijay997.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Redisson 配置类
 * 用于配置 Redis 客户端，支持单机、集群、哨兵等模式
 */
@Configuration
public class RedissonConfig {

    @Value("${spring.redis.host:127.0.0.1}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.database:0}")
    private int database;

    /**
     * 配置 RedissonClient Bean
     * 默认使用单机模式，可根据需要扩展为集群或哨兵模式
     */
    @Bean(destroyMethod = "shutdown")
    public RedissonClient redissonClient() {
        Config config = new Config();
        
        // 构建 Redis 地址
        String address = String.format("redis://%s:%d", redisHost, redisPort);
        
        // 配置单机模式
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(database)
                .setConnectionPoolSize(64)        // 连接池大小
                .setConnectionMinimumIdleSize(24)  // 最小空闲连接数
                .setIdleConnectionTimeout(10000)   // 空闲连接超时时间
                .setConnectTimeout(3000)           // 连接超时时间
                .setTimeout(3000);                 // 命令等待超时时间
        
        // 如果设置了密码，则配置密码
        if (redisPassword != null && !redisPassword.isEmpty()) {
            config.useSingleServer().setPassword(redisPassword);
        }
        config.setCodec(new JsonJacksonCodec());
        return Redisson.create(config);
    }
}
