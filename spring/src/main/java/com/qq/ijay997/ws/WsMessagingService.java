package com.qq.ijay997.ws;

import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;

/**
 * Send message to a user in multi-instance deployment.
 * - Try local delivery first.
 * - If not local, publish to Redis topic for other instances to deliver.
 */
public class WsMessagingService {

    private final WsConnectionRegistry registry;
    private final WsProperties props;
    private final RedissonClient redissonClient;

    public WsMessagingService(WsConnectionRegistry registry, WsProperties props, RedissonClient redissonClient) {
        this.registry = registry;
        this.props = props;
        this.redissonClient = redissonClient;
    }

    public void sendToUser(String userId, String payload) {
        boolean deliveredLocal = registry.sendToUserIfLocal(userId, payload);
        if (deliveredLocal) return;

        RTopic topic = redissonClient.getTopic(props.getMessageTopic());
        topic.publish(new WsMessageEnvelope(userId, payload));
    }
}

