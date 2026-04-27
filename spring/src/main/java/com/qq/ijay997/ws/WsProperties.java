package com.qq.ijay997.ws;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ws")
public class WsProperties {

    /**
     * WebSocket endpoint path.
     */
    private String endpoint = "/ws";

    /**
     * If true, a new connection for the same user will kick the old one (across instances).
     */
    private boolean kickDuplicate = true;

    /**
     * Redis key prefix used to store user->connection mapping.
     */
    private String userConnKeyPrefix = "ws:conn:user:";

    /**
     * Redis pub/sub topic used to kick stale connections.
     */
    private String kickTopic = "ws:kick";

    /**
     * Redis pub/sub topic used to deliver messages across instances.
     */
    private String messageTopic = "ws:msg";

    /**
     * TTL (seconds) for the user->connection mapping.
     * Should be >= your expected connection keepalive interval.
     */
    private long userConnTtlSeconds = 90;

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public boolean isKickDuplicate() {
        return kickDuplicate;
    }

    public void setKickDuplicate(boolean kickDuplicate) {
        this.kickDuplicate = kickDuplicate;
    }

    public String getUserConnKeyPrefix() {
        return userConnKeyPrefix;
    }

    public void setUserConnKeyPrefix(String userConnKeyPrefix) {
        this.userConnKeyPrefix = userConnKeyPrefix;
    }

    public String getKickTopic() {
        return kickTopic;
    }

    public void setKickTopic(String kickTopic) {
        this.kickTopic = kickTopic;
    }

    public String getMessageTopic() {
        return messageTopic;
    }

    public void setMessageTopic(String messageTopic) {
        this.messageTopic = messageTopic;
    }

    public long getUserConnTtlSeconds() {
        return userConnTtlSeconds;
    }

    public void setUserConnTtlSeconds(long userConnTtlSeconds) {
        this.userConnTtlSeconds = userConnTtlSeconds;
    }
}

