package com.qq.ijay997.dubbo.demo;

import com.qq.ijay997.dubbo.api.UserService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dubbo 服务演示控制器
 * 展示 Dubbo 的各种使用场景
 * 
 * 只有在配置文件中设置了 dubbo.registry.address 时才启用
 * 
 * @author ijay997
 */
@RestController
@RequestMapping("/api/dubbo/demo")
@ConditionalOnProperty(prefix = "dubbo", name = "registry.address")
public class DubboDemoController {

    @DubboReference(timeout = 5000, retries = 1)
    private UserService userService;

    /**
     * 演示 1: 基本调用
     */
    @GetMapping("/basic")
    public String basicDemo(@RequestParam String name) {
        return userService.sayHello(name);
    }

    /**
     * 演示 2: 获取用户信息
     */
    @GetMapping("/user")
    public String getUserDemo(@RequestParam Long userId) {
        String userName = userService.getUserNameById(userId);
        return "用户 ID: " + userId + ", 用户名：" + userName;
    }

    /**
     * 演示 3: 远程计算
     */
    @GetMapping("/calculate")
    public String calculateDemo(
            @RequestParam int a,
            @RequestParam int b) {
        int result = userService.add(a, b);
        return String.format("计算结果：%d + %d = %d", a, b, result);
    }

    /**
     * 演示 4: 组合调用
     */
    @GetMapping("/combined")
    public String combinedDemo() {
        StringBuilder sb = new StringBuilder();
        
        // 多次调用
        for (int i = 1; i <= 3; i++) {
            sb.append("第 ").append(i).append(" 次调用：");
            sb.append(userService.sayHello("用户" + i)).append("\n");
        }
        
        return sb.toString();
    }

    /**
     * 演示 5: 性能测试
     */
    @GetMapping("/performance")
    public String performanceDemo() {
        long startTime = System.currentTimeMillis();
        
        // 连续调用 10 次
        for (int i = 0; i < 10; i++) {
            userService.getUserNameById((long) i);
        }
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        return String.format("10 次远程调用耗时：%d ms, 平均每次：%.2f ms", 
                duration, (double) duration / 10);
    }
}
