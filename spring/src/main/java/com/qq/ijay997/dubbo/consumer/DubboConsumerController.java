package com.qq.ijay997.dubbo.consumer;

import com.qq.ijay997.dubbo.api.UserService;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dubbo 服务消费者控制器
 * 
 * 只有在配置文件中设置了 dubbo.registry.address 时才启用
 * 
 * @author ijay997
 */
@RestController
@RequestMapping("/api/dubbo")
@ConditionalOnProperty(prefix = "dubbo", name = "registry.address")
public class DubboConsumerController {

    @DubboReference
    private UserService userService;

    /**
     * 通过 Dubbo 调用获取用户名称
     * 
     * @param userId 用户 ID
     * @return 用户名称
     */
    @GetMapping("/user/name")
    public String getUserName(@RequestParam Long userId) {
        System.out.println("[Consumer] 接收到 HTTP 请求：/api/dubbo/user/name, userId = " + userId);
        String userName = userService.getUserNameById(userId);
        return userName;
    }

    /**
     * 通过 Dubbo 调用发送问候
     * 
     * @param userName 用户名
     * @return 问候语
     */
    @GetMapping("/hello")
    public String sayHello(@RequestParam String userName) {
        System.out.println("[Consumer] 接收到 HTTP 请求：/api/dubbo/hello, userName = " + userName);
        return userService.sayHello(userName);
    }

    /**
     * 通过 Dubbo 调用计算加法
     * 
     * @param a 数字 a
     * @param b 数字 b
     * @return 和
     */
    @GetMapping("/add")
    public int add(@RequestParam int a, @RequestParam int b) {
        System.out.println("[Consumer] 接收到 HTTP 请求：/api/dubbo/add, a = " + a + ", b = " + b);
        return userService.add(a, b);
    }
}
