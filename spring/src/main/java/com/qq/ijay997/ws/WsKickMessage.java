package com.qq.ijay997.ws;

import java.io.Serializable;

public class WsKickMessage implements Serializable {
    private String connId;
    private String reason;

    public WsKickMessage() {
    }

    public WsKickMessage(String connId, String reason) {
        this.connId = connId;
        this.reason = reason;
    }

    public String getConnId() {
        return connId;
    }

    public void setConnId(String connId) {
        this.connId = connId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}

