package com.qq.ijay997.dubbo.fallback;

import com.qq.ijay997.dubbo.api.UserService;
import org.springframework.stereotype.Component;

/**
 * 服务降级实现类
 * 当远程调用失败时，提供降级策略
 * 
 * @author ijay997
 */
@Component
public class UserServiceFallback implements UserService {

    @Override
    public String getUserNameById(Long userId) {
        System.err.println("[Fallback] 服务降级：getUserNameById, userId = " + userId);
        return "降级用户_" + userId;
    }

    @Override
    public String sayHello(String userName) {
        System.err.println("[Fallback] 服务降级：sayHello, userName = " + userName);
        return "服务繁忙，请稍后再试！[降级响应]";
    }

    @Override
    public int add(int a, int b) {
        System.err.println("[Fallback] 服务降级：add, a = " + a + ", b = " + b);
        return -1; // 返回错误标识
    }
}
