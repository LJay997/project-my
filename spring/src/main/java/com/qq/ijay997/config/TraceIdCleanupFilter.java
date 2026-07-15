package com.qq.ijay997.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * TraceId 清理过滤器
 * 
 * 作用：
 * 1. 在请求开始时生成 traceId 并放入 MDC
 * 2. 在请求结束时清理 MDC 和 ThreadLocal，防止内存泄漏
 * 
 * 注意：
 * - 这个过滤器会覆盖 Feign Interceptor 中设置的 traceId
 * - 如果希望 Feign 调用使用独立的 traceId，可以注释掉此过滤器
 * 
 * @author ijay997
 */
@Slf4j
@Component
@Order(1)  // 确保在其他过滤器之前执行
public class TraceIdCleanupFilter extends OncePerRequestFilter {

    private static final String TRACE_ID_KEY = "traceId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, 
                                    HttpServletResponse response, 
                                    FilterChain filterChain) 
            throws ServletException, IOException {
        
        try {
            // 1. 检查是否已有 traceId（可能由网关或上游服务传入）
            String traceId = request.getHeader("X-Trace-Id");
            
            // 2. 如果没有，生成新的 traceId
            if (traceId == null || traceId.isEmpty()) {
                traceId = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            }
            
            // 3. 将 traceId 放入 MDC
            org.slf4j.MDC.put(TRACE_ID_KEY, traceId);
            
            // 4. 将 traceId 放入响应头，方便前端追踪
            response.setHeader("X-Trace-Id", traceId);
            
            log.debug("请求开始 - TraceId: {}, URL: {}", traceId, request.getRequestURI());
            
            // 5. 执行过滤链
            filterChain.doFilter(request, response);
            
            log.debug("请求结束 - TraceId: {}, Status: {}", traceId, response.getStatus());
            
        } finally {
            // 6. 清理 MDC 和 ThreadLocal（非常重要！）
            DevRequestInterceptor.clear();
            org.slf4j.MDC.clear();
            
            log.debug("已清理 TraceId 上下文");
        }
    }
}
