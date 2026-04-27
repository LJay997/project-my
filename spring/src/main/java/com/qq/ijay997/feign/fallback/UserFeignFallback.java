package com.qq.ijay997.feign.fallback;

import com.qq.ijay997.dto.Result;
import com.qq.ijay997.dto.UserDTO;
import com.qq.ijay997.feign.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 用户服务 Feign Client 降级处理
 * 
 * 当服务调用失败时，返回降级数据，避免雪崩效应
 * 
 * @author ijay997
 */
@Slf4j
@Component
public class UserFeignFallback implements UserFeignClient {

    @Override
    public Result<UserDTO> getUserById(Long id) {
        log.error("UserFeignClient.getUserById 调用失败，id={}", id);
        return Result.error(503, "用户服务暂时不可用");
    }

    @Override
    public Result<UserDTO> getUserByUsername(String username) {
        log.error("UserFeignClient.getUserByUsername 调用失败，username={}", username);
        return Result.error(503, "用户服务暂时不可用");
    }

    @Override
    public Result<List<UserDTO>> getAllUsers() {
        log.error("UserFeignClient.getAllUsers 调用失败");
        return Result.success(Collections.emptyList());
    }

    @Override
    public Result<UserDTO> createUser(UserDTO userDTO) {
        log.error("UserFeignClient.createUser 调用失败，userDTO={}", userDTO);
        return Result.error(503, "用户服务暂时不可用，创建失败");
    }

    @Override
    public Result<UserDTO> updateUser(Long id, UserDTO userDTO) {
        log.error("UserFeignClient.updateUser 调用失败，id={}, userDTO={}", id, userDTO);
        return Result.error(503, "用户服务暂时不可用");
    }

    @Override
    public Result<Void> deleteUser(Long id) {
        log.error("UserFeignClient.deleteUser 调用失败，id={}", id);
        return Result.error(503, "用户服务暂时不可用");
    }

    @Override
    public Result<List<UserDTO>> searchUsers(String keyword) {
        log.error("UserFeignClient.searchUsers 调用失败，keyword={}", keyword);
        return Result.success(Collections.emptyList());
    }
}
