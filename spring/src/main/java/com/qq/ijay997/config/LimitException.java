package com.qq.ijay997.config;

/**
 * 限流异常
 * 当接口访问超过限制时抛出此异常
 */
public class LimitException extends RuntimeException {

    public LimitException(String message) {
        super(message);
    }

    public LimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
