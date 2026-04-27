package com.qq.ijay997.ws;

import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@EnableConfigurationProperties(WsProperties.class)
@SuppressWarnings("null")
public class WsConfig implements WebSocketConfigurer {

    @Value("${spring.application.name:app}")
    private String appName;

    @Value("${server.port:0}")
    private String port;

    private final WsProperties props;
    private final RedissonClient redissonClient;
    
    @Autowired
    @Lazy
    private WsServer wsServer;

    public WsConfig(WsProperties props, RedissonClient redissonClient) {
        this.props = props;
        this.redissonClient = redissonClient;
    }

    @Bean
    public WsInstanceId wsInstanceId() {
        return WsInstanceId.create(appName, port);
    }

    @Bean
    public WsConnectionRegistry wsConnectionRegistry() {
        return new WsConnectionRegistry();
    }

    @Bean
    public WsRedisCoordinator wsRedisCoordinator() {
        return new WsRedisCoordinator(redissonClient, props);
    }

    @Bean
    public WsServer wsServer(WsInstanceId instanceId,
                             WsConnectionRegistry registry,
                             WsRedisCoordinator coordinator) {
        return new WsServer(props, instanceId, registry, coordinator, redissonClient);
    }

    @Bean
    public WsMessagingService wsMessagingService(WsConnectionRegistry registry) {
        return new WsMessagingService(registry, props, redissonClient);
    }

    @Override
    public void registerWebSocketHandlers(@NonNull WebSocketHandlerRegistry registry) {
        registry.addHandler((WebSocketHandler) wsServer, props.getEndpoint())
                .addInterceptors(new WsHandshakeInterceptor())
                .setAllowedOrigins("*");
    }
}
