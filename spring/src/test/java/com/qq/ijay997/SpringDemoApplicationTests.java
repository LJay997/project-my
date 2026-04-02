package com.qq.ijay997;

import com.qq.ijay997.entity.User;
import com.qq.ijay997.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Spring Boot 应用测试
 * 
 * @author ijay997
 */
@SpringBootTest
class SpringDemoApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoads() {
        // 验证应用上下文加载成功
        assertNotNull(userRepository);
    }

    @Test
    void testSaveUser() {
        // 创建用户
        User user = new User(null, "测试用户", "test@example.com", 20);
        User savedUser = userRepository.save(user);

        // 验证用户保存成功
        assertNotNull(savedUser.getId());
        assertEquals("测试用户", savedUser.getUsername());
        assertEquals("test@example.com", savedUser.getEmail());
        assertEquals(20, savedUser.getAge());
    }

    @Test
    void testFindUserById() {
        // 先保存一个用户
        User user = new User(null, "查找测试", "find@example.com", 25);
        User savedUser = userRepository.save(user);

        // 根据 ID 查找
        Optional<User> foundUser = userRepository.findById(savedUser.getId());

        // 验证查找结果
        assertTrue(foundUser.isPresent());
        assertEquals("查找测试", foundUser.get().getUsername());
    }

    @Test
    void testFindByUsername() {
        // 先保存一个用户
        User user = new User(null, "用户名查找", "username@example.com", 30);
        userRepository.save(user);

        // 根据用户名查找
        Optional<User> foundUser = userRepository.findByUsername("用户名查找");

        // 验证查找结果
        assertTrue(foundUser.isPresent());
        assertEquals("username@example.com", foundUser.get().getEmail());
    }

    @Test
    void testFindByAgeRange() {
        // 保存多个用户
        userRepository.save(new User(null, "用户 1", "user1@example.com", 20));
        userRepository.save(new User(null, "用户 2", "user2@example.com", 25));
        userRepository.save(new User(null, "用户 3", "user3@example.com", 30));
        userRepository.save(new User(null, "用户 4", "user4@example.com", 35));

        // 查找年龄在 25-35 之间的用户
        List<User> users = userRepository.findByAgeBetween(25, 35);

        // 验证查找结果
        assertEquals(3, users.size());
    }

    @Test
    void testUpdateUser() {
        // 先保存一个用户
        User user = new User(null, "更新前", "update@example.com", 28);
        User savedUser = userRepository.save(user);

        // 更新用户信息
        savedUser.setUsername("更新后");
        savedUser.setAge(29);
        User updatedUser = userRepository.save(savedUser);

        // 验证更新结果
        assertEquals("更新后", updatedUser.getUsername());
        assertEquals(29, updatedUser.getAge());
    }

    @Test
    void testDeleteUser() {
        // 先保存一个用户
        User user = new User(null, "删除测试", "delete@example.com", 32);
        User savedUser = userRepository.save(user);

        // 删除用户
        userRepository.deleteById(savedUser.getId());

        // 验证删除结果
        Optional<User> deletedUser = userRepository.findById(savedUser.getId());
        assertFalse(deletedUser.isPresent());
    }

    @Test
    void testFindAllUsers() {
        // 清空所有用户
        userRepository.deleteAll();

        // 保存多个用户
        userRepository.save(new User(null, "全部用户 1", "all1@example.com", 22));
        userRepository.save(new User(null, "全部用户 2", "all2@example.com", 24));
        userRepository.save(new User(null, "全部用户 3", "all3@example.com", 26));

        // 查找所有用户
        List<User> users = userRepository.findAll();

        // 验证查找结果
        assertEquals(3, users.size());
    }
}
