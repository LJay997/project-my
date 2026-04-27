package com.qq.ijay997.ws;

import org.redisson.api.RScript;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.StringCodec;

import java.util.Collections;

/**
 * Coordinates cross-instance deduplication via Redis.
 */
public class WsRedisCoordinator {

    private final RedissonClient redissonClient;
    private final WsProperties props;

    public WsRedisCoordinator(RedissonClient redissonClient, WsProperties props) {
        this.redissonClient = redissonClient;
        this.props = props;
    }

    public String userConnKey(String userId) {
        return props.getUserConnKeyPrefix() + userId;
    }

    /**
     * Atomically set user->connId with TTL and return previous connId (or null).
     */
    public String setUserConnAndGetOld(String userId, String connId) {
        String key = userConnKey(userId);
        long ttlMs = props.getUserConnTtlSeconds() * 1000L;
        String script =
            "local old = redis.call('GET', KEYS[1]); " +
            "redis.call('SET', KEYS[1], ARGV[1], 'PX', ARGV[2]); " +
            "return old;";
        Object res = redissonClient.getScript(StringCodec.INSTANCE).eval(
            RScript.Mode.READ_WRITE,
            script,
            RScript.ReturnType.VALUE,
            Collections.singletonList(key),
            connId,
            String.valueOf(ttlMs)
        );
        return res == null ? null : String.valueOf(res);
    }

    /**
     * Compare-and-delete: delete user conn key only if it still equals connId.
     */
    public boolean deleteUserConnIfMatch(String userId, String connId) {
        String key = userConnKey(userId);
        String script =
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('DEL', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end";
        Object res = redissonClient.getScript(StringCodec.INSTANCE).eval(
            RScript.Mode.READ_WRITE,
            script,
            RScript.ReturnType.INTEGER,
            Collections.singletonList(key),
            connId
        );
        return res != null && Long.parseLong(String.valueOf(res)) == 1L;
    }
}

