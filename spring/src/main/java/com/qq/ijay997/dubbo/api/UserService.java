package com.qq.ijay997.dubbo.api;

/**
 * 用户服务接口（Dubbo API）
 * 
 * @author ijay997
 */
public interface UserService {

    /**
     * 根据 ID 获取用户名称
     * 
     * @param userId 用户 ID
     * @return 用户名称
     */
    String getUserNameById(Long userId);

    /**
     * 创建用户问候语
     * 
     * @param userName 用户名
     * @return 问候语
     */
    String sayHello(String userName);

    /**
     * 计算两个数的和
     * 
     * @param a 数字 a
     * @param b 数字 b
     * @return 和
     */
    int add(int a, int b);
}
