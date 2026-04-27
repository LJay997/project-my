package com.qq.ijay997.ws;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Per-instance in-memory registry.
 */
public class WsConnectionRegistry {

    private final Map<String, WebSocketSession> connIdToSession = new ConcurrentHashMap<>();
    private final Map<String, String> userIdToConnId = new ConcurrentHashMap<>();

    public void register(String userId, String connId, WebSocketSession session) {
        connIdToSession.put(connId, session);
        userIdToConnId.put(userId, connId);
    }

    public void unregister(String userId, String connId) {
        connIdToSession.remove(connId);
        // Only remove if mapping still points to this connId
        userIdToConnId.remove(userId, connId);
    }

    public WebSocketSession getSessionByConnId(String connId) {
        return connIdToSession.get(connId);
    }

    public WebSocketSession getSessionByUserId(String userId) {
        String connId = userIdToConnId.get(userId);
        return connId == null ? null : connIdToSession.get(connId);
    }

    public boolean sendToUserIfLocal(String userId, String payload) {
        WebSocketSession session = getSessionByUserId(userId);
        if (session == null || !session.isOpen()) return false;
        try {
            session.sendMessage(new TextMessage(payload));
            return true;
        } catch (IOException e) {
            return false;
        }
    }
}

