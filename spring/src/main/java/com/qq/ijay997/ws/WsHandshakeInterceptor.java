package com.qq.ijay997.ws;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.MultiValueMap;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

public class WsHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "wsUserId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        String userId = extractUserId(request);
        if (userId == null || userId.trim().isEmpty()) {
            return false;
        }
        attributes.put(ATTR_USER_ID, userId.trim());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // no-op
    }

    private String extractUserId(ServerHttpRequest request) {
        // Prefer header, fallback to query param.
        String header = request.getHeaders().getFirst("X-User-Id");
        if (header != null && !header.trim().isEmpty()) {
            return header;
        }

        URI uri = request.getURI();
        MultiValueMap<String, String> params = UriComponentsBuilder.fromUri(uri).build().getQueryParams();
        String uid = params.getFirst("uid");
        if (uid != null && !uid.trim().isEmpty()) {
            return uid;
        }
        return null;
    }
}

