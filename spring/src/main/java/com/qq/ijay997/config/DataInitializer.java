package com.qq.ijay997.config;

import com.qq.ijay997.entity.User;
import com.qq.ijay997.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器
 * 应用启动时自动插入演示数据
 * 
 * @author ijay997
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        System.out.println("正在初始化演示数据...");

        // 清空现有数据
        userRepository.deleteAll();

        // 创建演示用户
        User user1 = new User(null, "张三", "zhangsan@example.com", 25);
        User user2 = new User(null, "李四", "lisi@example.com", 30);
        User user3 = new User(null, "王五", "wangwu@example.com", 28);
        User user4 = new User(null, "赵六", "zhaoliu@example.com", 35);
        User user5 = new User(null, "小明", "xiaoming@example.com", 22);

        userRepository.save(user1);
        userRepository.save(user2);
        userRepository.save(user3);
        userRepository.save(user4);
        userRepository.save(user5);

        System.out.println("演示数据初始化完成！共插入 5 条用户数据。");
        System.out.println("=================================");
    }
}
