package com.qq.ijay997.config;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;

/**
 * 生产环境请求拦截器
 * 用于在生产环境中添加认证、签名等安全相关的请求头
 * 
 * @author ijay997
 */
@Slf4j
public class ProdRequestInterceptor implements RequestInterceptor {

    private static final String API_KEY = "your-production-api-key";
    private static final String API_SECRET = "your-production-api-secret";

    @Override
    public void apply(RequestTemplate template) {
        // 添加生产环境标识
        template.header("X-Environment", "production");
        
        // 添加 API 认证信息
        template.header("X-API-Key", API_KEY);
        
        // 添加请求时间戳（用于防重放攻击）
        long timestamp = System.currentTimeMillis();
        template.header("X-Timestamp", String.valueOf(timestamp));
        
        // 添加请求签名（简化示例，实际应使用更安全的签名算法）
        String signature = generateSignature(template, timestamp);
        template.header("X-Signature", signature);
        
        // 添加请求 ID（用于日志追踪）
        String requestId = java.util.UUID.randomUUID().toString();
        template.header("X-Request-Id", requestId);
        
        // 生产环境只记录关键信息（不记录敏感数据）
        log.info("\n" +
                 "========================================\n" +
                 "[Prod Interceptor] Feign 请求摘要\n" +
                 "========================================\n" +
                 "请求方法: {}\n" +
                 "请求路径: {}\n" +
                 "查询参数数量: {}\n" +
                 "请求头数量: {}\n" +
                 "请求体大小: {} bytes\n" +
                 "RequestId: {}\n" +
                 "Timestamp: {}\n" +
                 "========================================",
                 template.method(),
                 template.path(),
                 template.queries() != null ? template.queries().size() : 0,
                 template.headers() != null ? template.headers().size() : 0,
                 template.requestBody() != null ? template.requestBody().length() : 0,
                 requestId,
                 timestamp
        );
    }

    /**
     * 生成请求签名
     * 
     * @param template 请求模板
     * @param timestamp 时间戳
     * @return 签名字符串
     */
    private String generateSignature(RequestTemplate template, long timestamp) {
        // 简化示例：实际生产环境应使用 HMAC-SHA256 等安全算法
        String data = template.method() + template.path() + timestamp + API_SECRET;
        return java.util.Base64.getEncoder().encodeToString(data.getBytes());
    }
}
