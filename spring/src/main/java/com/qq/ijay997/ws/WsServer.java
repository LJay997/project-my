package com.qq.ijay997.ws;

import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.lang.NonNull;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;

/**
 * WebSocket handler with:
 * - per-user dedup (kick old connection)
 * - cross-instance kick via Redis pub/sub
 * - cross-instance message delivery via Redis pub/sub
 */
@SuppressWarnings("null")
public class WsServer extends TextWebSocketHandler {

    private final WsProperties props;
    private final WsInstanceId instanceId;
    private final WsConnectionRegistry registry;
    private final WsRedisCoordinator coordinator;
    private final RedissonClient redissonClient;

    public WsServer(WsProperties props,
                    WsInstanceId instanceId,
                    WsConnectionRegistry registry,
                    WsRedisCoordinator coordinator,
                    RedissonClient redissonClient) {
        this.props = props;
        this.instanceId = instanceId;
        this.registry = registry;
        this.coordinator = coordinator;
        this.redissonClient = redissonClient;
    }

    @Override
    public void afterConnectionEstablished(@NonNull WebSocketSession session) throws Exception {
        String userId = (String) session.getAttributes().get(WsHandshakeInterceptor.ATTR_USER_ID);
        if (userId == null || userId.trim().isEmpty()) {
            CloseStatus cs = CloseStatus.POLICY_VIOLATION;
            session.close(cs);
            return;
        }

        String connId = instanceId.getId() + ":" + session.getId();
        registry.register(userId, connId, session);

        if (props.isKickDuplicate()) {
            String oldConnId = coordinator.setUserConnAndGetOld(userId, connId);
            if (oldConnId != null && !oldConnId.isEmpty() && !oldConnId.equals(connId)) {
                publishKick(oldConnId, "duplicate_connection");
            }
        }

        // Optional: send an ACK with connId for debugging/client correlation.
        safeSend(session, "{\"type\":\"CONNECTED\",\"connId\":\"" + connId + "\"}");
    }

    @Override
    protected void handleTextMessage(@NonNull WebSocketSession session, @NonNull TextMessage message) throws Exception {
        // Simple echo contract (can be replaced by business protocol).
        safeSend(session, "{\"type\":\"ECHO\",\"payload\":" + jsonQuote(message.getPayload()) + "}");
    }

    @Override
    public void afterConnectionClosed(@NonNull WebSocketSession session, @NonNull CloseStatus status) throws Exception {
        String userId = (String) session.getAttributes().get(WsHandshakeInterceptor.ATTR_USER_ID);
        if (userId == null) return;

        String connId = instanceId.getId() + ":" + session.getId();
        registry.unregister(userId, connId);

        if (props.isKickDuplicate()) {
            coordinator.deleteUserConnIfMatch(userId, connId);
        }
    }

    public void kickIfLocal(String connId, String reason) {
        WebSocketSession session = registry.getSessionByConnId(connId);
        if (session == null) return;
        try {
            safeSend(session, "{\"type\":\"KICK\",\"reason\":\"" + escapeJson(reason) + "\"}");
            CloseStatus cs = CloseStatus.NORMAL;
            session.close(cs);
        } catch (Exception ignored) {
        }
    }

    public void deliverIfLocal(WsMessageEnvelope env) {
        if (env == null || env.getToUserId() == null) return;
        registry.sendToUserIfLocal(env.getToUserId(), env.getPayload());
    }

    private void publishKick(String connId, String reason) {
        RTopic topic = redissonClient.getTopic(props.getKickTopic());
        topic.publish(new WsKickMessage(connId, reason));
    }

    private void safeSend(WebSocketSession session, String payload) {
        if (session == null || !session.isOpen()) return;
        try {
            session.sendMessage(new TextMessage(payload));
        } catch (IOException ignored) {
        }
    }

    private static String jsonQuote(String s) {
        return "\"" + escapeJson(s) + "\"";
    }

    @SuppressWarnings("null")
    private static String escapeJson(String s) {
        if (s == null) return "";
        String v = s;
        return v.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

