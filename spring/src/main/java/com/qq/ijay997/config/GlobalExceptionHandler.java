package com.qq.ijay997.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理器
 * 统一处理限流异常和其他业务异常
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 处理限流异常
     */
    @ExceptionHandler(LimitException.class)
    @ResponseStatus(HttpStatus.TOO_MANY_REQUESTS)
    public Map<String, Object> handleLimitException(LimitException e) {
        log.warn("请求被限流: {}", e.getMessage());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 429);
        result.put("message", e.getMessage());
        result.put("success", false);
        
        return result;
    }

    /**
     * 处理其他异常
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Map<String, Object> handleException(Exception e) {
        log.error("系统异常: ", e);
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 500);
        result.put("message", "系统内部错误");
        result.put("success", false);
        
        return result;
    }
}
