package com.qq.ijay997.ws;

import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Manages Redis pub/sub subscriptions for cross-instance kick & message delivery.
 */
@Component
public class WsRedisSubscribers {

    private final WsProperties props;
    private final RedissonClient redissonClient;
    private final WsServer wsServer;

    private Integer kickListenerId;
    private Integer msgListenerId;

    public WsRedisSubscribers(WsProperties props, RedissonClient redissonClient, WsServer wsServer) {
        this.props = props;
        this.redissonClient = redissonClient;
        this.wsServer = wsServer;
    }

    @PostConstruct
    public void subscribe() {
        RTopic kick = redissonClient.getTopic(props.getKickTopic());
        kickListenerId = kick.addListener(WsKickMessage.class, (channel, msg) -> {
            if (msg == null || msg.getConnId() == null) return;
            wsServer.kickIfLocal(msg.getConnId(), msg.getReason());
        });

        RTopic msgTopic = redissonClient.getTopic(props.getMessageTopic());
        msgListenerId = msgTopic.addListener(WsMessageEnvelope.class, (channel, msg) -> wsServer.deliverIfLocal(msg));
    }

    @PreDestroy
    public void unsubscribe() {
        try {
            if (kickListenerId != null) {
                redissonClient.getTopic(props.getKickTopic()).removeListener(kickListenerId);
            }
        } catch (Exception ignored) {
        }
        try {
            if (msgListenerId != null) {
                redissonClient.getTopic(props.getMessageTopic()).removeListener(msgListenerId);
            }
        } catch (Exception ignored) {
        }
    }
}

