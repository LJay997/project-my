package com.qq.ijay997.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

/**
 * 开发环境请求拦截器
 * 用于在开发环境中添加调试信息和特殊的请求头
 * 
 * 特性：
 * 1. 将 traceId 放入 MDC，自动在所有日志中携带
 * 2. 使用 ThreadLocal 存储 traceId，方便在代码中获取
 * 3. 打印完整的请求信息（请求头、参数、 body）
 * 
 * @author ijay997
 */
@Slf4j
public class DevRequestInterceptor implements RequestInterceptor {

    /**
     * MDC 中的 traceId key
     */
    private static final String TRACE_ID_KEY = "traceId";
    
    /**
     * ThreadLocal 存储 traceId（备用方案）
     */
    private static final ThreadLocal<String> traceIdHolder = new ThreadLocal<>();

    @Override
    public void apply(RequestTemplate template) {
        // 生成或获取 traceId
        String traceId = generateOrGetTraceId();
        
        // ========== 方式 1: 将 traceId 放入 MDC（推荐）==========
        MDC.put(TRACE_ID_KEY, traceId);
        
        // ========== 方式 2: 将 traceId 放入 ThreadLocal（备用）==========
        traceIdHolder.set(traceId);
        
        // 添加请求头
        template.header("X-Environment", "development");
        template.header("X-Trace-Id", traceId);
        
        // 添加开发者信息
        String developer = System.getProperty("user.name", "unknown");
        template.header("X-Developer", developer);
        
        // 打印完整的请求信息（日志中会自动包含 traceId，因为已放入 MDC）
        log.info("\n" +
                 "========================================\n" +
                 "[Dev Interceptor] Feign 请求详情\n" +
                 "========================================\n" +
                 "请求方法: {}\n" +
                 "请求URL: {}\n" +
                 "请求路径: {}\n" +
                 "查询参数: {}\n" +
                 "请求头: {}\n" +
                 "请求体: {}\n" +
                 "开发者: {}\n" +
                 "========================================",
                 template.method(),
                 template.url(),
                 template.path(),
                 formatQueryParams(template.queries()),
                 formatHeaders(template.headers()),
                 formatBody(template.requestBody()),
                 developer
        );
        
        // 演示：从 MDC 和 ThreadLocal 中获取 traceId
        log.debug("从 MDC 获取 traceId: {}", MDC.get(TRACE_ID_KEY));
        log.debug("从 ThreadLocal 获取 traceId: {}", getTraceId());
    }

    /**
     * 生成或获取 traceId
     * 优先从 MDC 中获取（如果已有），否则生成新的
     */
    private String generateOrGetTraceId() {
        // 尝试从 MDC 获取（可能由上游服务传入）
        String existingTraceId = MDC.get(TRACE_ID_KEY);
        if (existingTraceId != null && !existingTraceId.isEmpty()) {
            return existingTraceId;
        }
        
        // 尝试从 ThreadLocal 获取
        existingTraceId = traceIdHolder.get();
        if (existingTraceId != null && !existingTraceId.isEmpty()) {
            return existingTraceId;
        }
        
        // 生成新的 traceId
        return java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    /**
     * 获取当前线程的 traceId
     * 可以在业务代码中调用此方法获取 traceId
     * 
     * @return traceId
     */
    public static String getTraceId() {
        // 优先从 MDC 获取
        String traceId = MDC.get(TRACE_ID_KEY);
        if (traceId != null) {
            return traceId;
        }
        
        // 从 ThreadLocal 获取
        traceId = traceIdHolder.get();
        return traceId != null ? traceId : "N/A";
    }

    /**
     * 清理 ThreadLocal（防止内存泄漏）
     * 应该在请求结束时调用
     */
    public static void clear() {
        traceIdHolder.remove();
        MDC.clear();
    }

    /**
     * 格式化查询参数
     */
    private String formatQueryParams(java.util.Map<String, java.util.Collection<String>> queries) {
        if (queries == null || queries.isEmpty()) {
            return "无";
        }
        StringBuilder sb = new StringBuilder();
        queries.forEach((key, values) -> {
            sb.append(key).append("=").append(String.join(",", values)).append("; ");
        });
        return sb.toString();
    }

    /**
     * 格式化请求头
     */
    private String formatHeaders(java.util.Map<String, java.util.Collection<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return "无";
        }
        StringBuilder sb = new StringBuilder("\n");
        headers.forEach((key, values) -> {
            // 隐藏敏感信息
            if (key.toLowerCase().contains("authorization") || 
                key.toLowerCase().contains("token") ||
                key.toLowerCase().contains("secret")) {
                sb.append("  ").append(key).append(": ***隐藏***\n");
            } else {
                sb.append("  ").append(key).append(": ").append(String.join(",", values)).append("\n");
            }
        });
        return sb.toString();
    }

    /**
     * 格式化请求体
     */
    private String formatBody(feign.Request.Body body) {
        if (body == null || body.length() == 0) {
            return "无";
        }
        try {
            String bodyStr = new String(body.asBytes(), "UTF-8");
            // 如果请求体太长，截取前500个字符
            if (bodyStr.length() > 500) {
                return bodyStr.substring(0, 500) + "... (已截断)";
            }
            return bodyStr;
        } catch (Exception e) {
            return "[无法解析请求体]";
        }
    }
}
