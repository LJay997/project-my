package com.qq.ijay997.ws;

import java.io.Serializable;

/**
 * Cross-instance message envelope carried by Redis pub/sub.
 */
public class WsMessageEnvelope implements Serializable {
    private String toUserId;
    private String payload;

    public WsMessageEnvelope() {
    }

    public WsMessageEnvelope(String toUserId, String payload) {
        this.toUserId = toUserId;
        this.payload = payload;
    }

    public String getToUserId() {
        return toUserId;
    }

    public void setToUserId(String toUserId) {
        this.toUserId = toUserId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }
}

