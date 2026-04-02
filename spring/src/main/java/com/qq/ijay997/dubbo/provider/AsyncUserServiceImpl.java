package com.qq.ijay997.dubbo.provider;

import com.qq.ijay997.dubbo.api.UserService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

/**
 * 用户服务实现类（异步演示版本）
 * 
 * @author ijay997
 */
@DubboService
public class AsyncUserServiceImpl implements UserService {

    @Override
    public String getUserNameById(Long userId) {
        System.out.println("[AsyncProvider] 同步调用：getUserNameById, userId = " + userId);
        
        // 模拟耗时操作
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        
        return "异步用户_" + userId;
    }

    @Override
    public String sayHello(String userName) {
        System.out.println("[AsyncProvider] 同步调用：sayHello, userName = " + userName);
        return "Hello, " + userName + "! 这是异步服务!";
    }

    @Override
    public int add(int a, int b) {
        System.out.println("[AsyncProvider] 同步调用：add, a = " + a + ", b = " + b);
        return a + b;
    }
}
