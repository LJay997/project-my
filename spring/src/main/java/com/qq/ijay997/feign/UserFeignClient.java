package com.qq.ijay997.feign;

import com.qq.ijay997.dto.Result;
import com.qq.ijay997.dto.UserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户服务 Feign Client
 * 
 * 使用方式：
 * 1. 注入此接口：@Autowired private UserFeignClient userFeignClient;
 * 2. 调用方法：userFeignClient.getUserById(1L);
 * 
 * @author ijay997
 */
@FeignClient(
    name = "user-service",
    url = "${feign.user-service.url:http://localhost:8082}",
    path = "/api/provider/users",
    fallback = com.qq.ijay997.feign.fallback.UserFeignFallback.class
)
public interface UserFeignClient {

    /**
     * 根据ID获取用户
     * 
     * @param id 用户ID
     * @return 用户信息
     */
    @GetMapping("/{id}")
    Result<UserDTO> getUserById(@PathVariable("id") Long id);

    /**
     * 根据用户名获取用户
     * 
     * @param username 用户名
     * @return 用户信息
     */
    @GetMapping("/username/{username}")
    Result<UserDTO> getUserByUsername(@PathVariable("username") String username);

    /**
     * 获取所有用户
     * 
     * @return 用户列表
     */
    @GetMapping
    Result<List<UserDTO>> getAllUsers();

    /**
     * 创建用户
     * 
     * @param userDTO 用户信息
     * @return 创建结果
     */
    @PostMapping
    Result<UserDTO> createUser(@RequestBody UserDTO userDTO);

    /**
     * 更新用户
     * 
     * @param id 用户ID
     * @param userDTO 用户信息
     * @return 更新结果
     */
    @PutMapping("/{id}")
    Result<UserDTO> updateUser(@PathVariable("id") Long id, @RequestBody UserDTO userDTO);

    /**
     * 删除用户
     * 
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    Result<Void> deleteUser(@PathVariable("id") Long id);

    /**
     * 搜索用户
     * 
     * @param keyword 关键词
     * @return 用户列表
     */
    @GetMapping("/search")
    Result<List<UserDTO>> searchUsers(@RequestParam("keyword") String keyword);
}
