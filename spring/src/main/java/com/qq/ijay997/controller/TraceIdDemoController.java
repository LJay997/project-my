package com.qq.ijay997.controller;

import com.qq.ijay997.config.DevRequestInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * TraceId 使用示例控制器
 * 
 * 演示如何在业务代码中获取和使用 traceId
 * 
 * @author ijay997
 */
@Slf4j
@RestController
@RequestMapping("/api/trace-demo")
public class TraceIdDemoController {

    /**
     * 示例 1: 从 MDC 中获取 traceId
     * 
     * 访问：GET /api/trace-demo/mdc
     */
    @GetMapping("/mdc")
    public String getTraceIdFromMDC() {
        // 从 MDC 获取 traceId
        String traceId = MDC.get("traceId");
        
        log.info("业务逻辑执行中 - TraceId: {}", traceId);
        
        return "从 MDC 获取的 TraceId: " + (traceId != null ? traceId : "N/A");
    }

    /**
     * 示例 2: 从 DevRequestInterceptor 中获取 traceId
     * 
     * 访问：GET /api/trace-demo/interceptor
     */
    @GetMapping("/interceptor")
    public String getTraceIdFromInterceptor() {
        // 从拦截器中获取 traceId
        String traceId = DevRequestInterceptor.getTraceId();
        
        log.info("业务逻辑执行中 - TraceId: {}", traceId);
        
        return "从 Interceptor 获取的 TraceId: " + traceId;
    }

    /**
     * 示例 3: 日志中自动包含 traceId（因为已放入 MDC）
     * 
     * 访问：GET /api/trace-demo/auto
     */
    @GetMapping("/auto")
    public String autoTraceInLog() {
        // 这些日志会自动包含 traceId（需要在 logback.xml 中配置 %X{traceId}）
        log.info("第一条日志 - 处理用户请求");
        log.info("第二条日志 - 查询数据库");
        log.info("第三条日志 - 返回结果");
        
        return "查看日志，应该能看到所有日志都包含相同的 traceId";
    }

    /**
     * 示例 4: 模拟完整的业务流程
     * 
     * 访问：GET /api/trace-demo/business
     */
    @GetMapping("/business")
    public String businessProcess() {
        String traceId = DevRequestInterceptor.getTraceId();
        
        log.info("[{}] 开始处理业务流程", traceId);
        
        // 步骤 1: 参数校验
        log.info("[{}] 步骤1: 参数校验通过", traceId);
        
        // 步骤 2: 查询数据库
        log.info("[{}] 步骤2: 查询数据库", traceId);
        
        // 步骤 3: 业务逻辑处理
        log.info("[{}] 步骤3: 业务逻辑处理", traceId);
        
        // 步骤 4: 返回结果
        log.info("[{}] 步骤4: 返回结果", traceId);
        
        return "业务流程完成，TraceId: " + traceId;
    }

    /**
     * 示例 5: 在异常处理中使用 traceId
     * 
     * 访问：GET /api/trace-demo/error
     */
    @GetMapping("/error")
    public String handleError() {
        String traceId = DevRequestInterceptor.getTraceId();
        
        try {
            log.info("[{}] 执行业务逻辑", traceId);
            
            // 模拟异常
            int result = 1 / 0;
            
            return "成功";
        } catch (Exception e) {
            // 异常日志中也包含 traceId，方便排查问题
            log.error("[{}] 业务处理失败: {}", traceId, e.getMessage(), e);
            return "处理失败，TraceId: " + traceId + "，请根据此 ID 查找日志";
        }
    }
}
