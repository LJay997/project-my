package com.qq.ijay997.controller;

import com.qq.ijay997.dto.OrderDTO;
import com.qq.ijay997.dto.Result;
import com.qq.ijay997.dto.UserDTO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 用户服务控制器 - 模拟微服务提供者
 * 
 * @author ijay997
 */
@Slf4j
@RestController
@RequestMapping("/api/provider/users")
public class UserServiceProviderController {

    private final Map<Long, UserDTO> userStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public UserServiceProviderController() {
        // 初始化一些测试数据
        initTestData();
    }

    private void initTestData() {
        createUser("张三", "zhangsan@example.com", "13800138001", 25);
        createUser("李四", "lisi@example.com", "13800138002", 30);
        createUser("王五", "wangwu@example.com", "13800138003", 28);
    }

    private UserDTO createUser(String username, String email, String phone, Integer age) {
        Long id = idGenerator.getAndIncrement();
        UserDTO user = new UserDTO();
        user.setId(id);
        user.setUsername(username);
        user.setEmail(email);
        user.setPhone(phone);
        user.setAge(age);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        userStore.put(id, user);
        return user;
    }

    /**
     * 根据ID获取用户
     */
    @GetMapping("/{id}")
    public Result<UserDTO> getUserById(@PathVariable Long id) {
        log.info("查询用户: id={}", id);
        
        // 模拟网络延迟
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        UserDTO user = userStore.get(id);
        if (user == null) {
            return Result.error(404, "用户不存在");
        }
        return Result.success(user);
    }

    /**
     * 根据用户名获取用户
     */
    @GetMapping("/username/{username}")
    public Result<UserDTO> getUserByUsername(@PathVariable String username) {
        log.info("查询用户: username={}", username);
        
        Optional<UserDTO> user = userStore.values().stream()
                .filter(u -> u.getUsername().equals(username))
                .findFirst();
        
        return user.map(Result::success)
                .orElseGet(() -> Result.error(404, "用户不存在"));
    }

    /**
     * 获取所有用户
     */
    @GetMapping
    public Result<List<UserDTO>> getAllUsers() {
        log.info("查询所有用户");
        return Result.success(new ArrayList<>(userStore.values()));
    }

    /**
     * 创建用户
     */
    @PostMapping
    public Result<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        log.info("创建用户: {}", userDTO);
        
        UserDTO newUser = createUser(
                userDTO.getUsername(),
                userDTO.getEmail(),
                userDTO.getPhone(),
                userDTO.getAge()
        );
        
        return Result.success(newUser);
    }

    /**
     * 更新用户
     */
    @PutMapping("/{id}")
    public Result<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        log.info("更新用户: id={}, data={}", id, userDTO);
        
        UserDTO existingUser = userStore.get(id);
        if (existingUser == null) {
            return Result.error(404, "用户不存在");
        }
        
        existingUser.setUsername(userDTO.getUsername());
        existingUser.setEmail(userDTO.getEmail());
        existingUser.setPhone(userDTO.getPhone());
        existingUser.setAge(userDTO.getAge());
        existingUser.setUpdateTime(LocalDateTime.now());
        
        return Result.success(existingUser);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        log.info("删除用户: id={}", id);
        
        if (userStore.remove(id) == null) {
            return Result.error(404, "用户不存在");
        }
        
        return Result.success();
    }

    /**
     * 搜索用户
     */
    @GetMapping("/search")
    public Result<List<UserDTO>> searchUsers(@RequestParam(required = false) String keyword) {
        log.info("搜索用户: keyword={}", keyword);
        
        if (keyword == null || keyword.trim().isEmpty()) {
            return Result.success(new ArrayList<>(userStore.values()));
        }
        
        List<UserDTO> result = userStore.values().stream()
                .filter(u -> u.getUsername().contains(keyword) 
                        || u.getEmail().contains(keyword)
                        || u.getPhone().contains(keyword))
                .collect(java.util.stream.Collectors.toList());
        
        return Result.success(result);
    }
}
