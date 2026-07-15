package com.qq.ijay997.controller;

import com.qq.ijay997.feign.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Feign 拦截器测试控制器
 * 
 * 用于演示不同环境下 RequestInterceptor 的工作效果
 * 
 * @author ijay997
 */
@Slf4j
@RestController
@RequestMapping("/api/feign-interceptor")
public class FeignInterceptorTestController {

    @Autowired
    private UserFeignClient userFeignClient;

    /**
     * 测试 Feign 请求拦截器
     * 
     * 调用此接口时，观察控制台日志和请求头信息
     * - 开发环境：会看到 [Dev Interceptor] 的日志
     * - 生产环境：会看到 [生产环境] 的日志
     * - 如果启用了 custom-enabled：会看到 [Custom Interceptor] 的日志
     * 
     * @return 测试结果
     */
    @GetMapping("/test")
    public String testInterceptor() {
        log.info("========== 开始测试 Feign 拦截器 ==========");
        
        try {
            // 调用 Feign Client，会触发 RequestInterceptor
            // 注意：由于目标服务可能不存在，这里可能会抛出异常
            // 主要目的是观察拦截器的日志输出
            userFeignClient.getAllUsers();
            
            return "拦截器测试完成，请查看日志输出";
        } catch (Exception e) {
            log.warn("Feign 调用失败（预期行为）: {}", e.getMessage());
            return "拦截器已执行，但目标服务不可用（这是正常的，主要观察拦截器日志）";
        } finally {
            log.info("========== Feign 拦截器测试结束 ==========");
        }
    }

    /**
     * 获取当前拦截器配置信息
     * 
     * @return 配置信息
     */
    @GetMapping("/config")
    public String getConfigInfo() {
        String interceptorType = System.getProperty("feign.interceptor.type", 
                               System.getenv("FEIGN_INTERCEPTOR_TYPE") != null ? 
                               System.getenv("FEIGN_INTERCEPTOR_TYPE") : "dev");
        
        String customEnabled = System.getProperty("feign.interceptor.custom-enabled", "false");
        
        StringBuilder info = new StringBuilder();
        info.append("当前 Feign 拦截器配置:\n");
        info.append("- 拦截器类型: ").append(interceptorType).append("\n");
        info.append("- 自定义拦截器: ").append(customEnabled).append("\n");
        info.append("- 激活的 Profile: ").append(
            System.getProperty("spring.profiles.active", "default")
        ).append("\n");
        
        log.info(info.toString());
        return info.toString();
    }
}
