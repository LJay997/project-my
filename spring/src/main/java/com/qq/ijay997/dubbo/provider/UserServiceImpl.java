package com.qq.ijay997.dubbo.provider;

import com.qq.ijay997.dubbo.api.UserService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * 用户服务实现类（Dubbo 服务提供者）
 * 
 * 只有在配置文件中设置了 dubbo.registry.address 时才启用
 * 
 * @author ijay997
 */
@DubboService
@ConditionalOnProperty(prefix = "dubbo", name = "registry.address")
public class UserServiceImpl implements UserService {

    @Override
    public String getUserNameById(Long userId) {
        System.out.println("[Provider] 收到请求：getUserNameById, userId = " + userId);
        return "用户_" + userId;
    }

    @Override
    public String sayHello(String userName) {
        System.out.println("[Provider] 收到请求：sayHello, userName = " + userName);
        return "Hello, " + userName + "! 欢迎使用 Dubbo!";
    }

    @Override
    public int add(int a, int b) {
        System.out.println("[Provider] 收到请求：add, a = " + a + ", b = " + b);
        int result = a + b;
        System.out.println("[Provider] 计算结果：" + result);
        return result;
    }
}
