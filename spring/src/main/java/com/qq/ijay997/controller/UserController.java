package com.qq.ijay997.controller;

import com.qq.ijay997.config.BloomFilterConfig;
import com.qq.ijay997.entity.User;
import com.qq.ijay997.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 *
 * @author ijay997
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private BloomFilterConfig bloomFilterConfig;

    /**
     * 获取所有用户
     *
     * @return 用户列表
     */
    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        List<User> users = userService.findAll();
        return ResponseEntity.ok(users);
    }

    /**
     * 根据 ID 获取用户
     * 使用布隆过滤器防止缓存穿透
     *
     * @param id 用户 ID
     * @return 用户对象
     */
    @GetMapping("/{id}")
    public ResponseEntity<User> getUserById(@PathVariable Long id) {
        // 1. 先通过布隆过滤器判断用户ID是否可能存在
        if (!bloomFilterConfig.mightContain(id)) {
            System.out.println(String.format("[布隆过滤器拦截] 用户ID %d 一定不存在，直接返回404", id));
            return ResponseEntity.notFound().build();
        }

        // 2. 布隆过滤器说可能存在，继续查询数据库
        System.out.println(String.format("[布隆过滤器放行] 用户ID %d 可能存在，查询数据库", id));

        return userService.findById(id)
                .map(user -> {
                    // 3. 如果查询成功，确保用户ID在布隆过滤器中（兜底）
                    bloomFilterConfig.addUser(id);
                    return ResponseEntity.ok(user);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据用户名获取用户
     *
     * @param username 用户名
     * @return 用户对象
     */
    @GetMapping("/username/{username}")
    public ResponseEntity<User> getUserByUsername(@PathVariable String username) {
        return userService.findByUsername(username)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * 根据年龄范围查询用户
     *
     * @param minAge 最小年龄
     * @param maxAge 最大年龄
     * @return 用户列表
     */
    @GetMapping("/age-range")
    public ResponseEntity<List<User>> getUsersByAgeRange(
            @RequestParam Integer minAge,
            @RequestParam Integer maxAge) {
        List<User> users = userService.findByAgeRange(minAge, maxAge);
        return ResponseEntity.ok(users);
    }

    /**
     * 创建用户
     *
     * @param user 用户对象
     * @return 创建的用户
     */
    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        try {
            User createdUser = userService.save(user);
            // 将新用户ID添加到布隆过滤器
            if (createdUser.getId() != null) {
                bloomFilterConfig.addUser(createdUser.getId());
                System.out.println(String.format("[布隆过滤器更新] 新增用户ID: %d", createdUser.getId()));
            }
            return ResponseEntity.status(HttpStatus.CREATED).body(createdUser);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 更新用户
     *
     * @param id   用户 ID
     * @param user 用户信息
     * @return 更新后的用户
     */
    @PutMapping("/{id}")
    public ResponseEntity<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        try {
            User updatedUser = userService.update(id, user);
            return ResponseEntity.ok(updatedUser);
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 删除用户
     *
     * @param id 用户 ID
     * @return 响应状态
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userService.deleteById(id);
        // 注意：布隆过滤器不支持删除操作
        // 如果需要支持删除，可以考虑使用布谷鸟过滤器（Cuckoo Filter）
        System.out.println(String.format("[提示] 用户ID %d 已删除，但布隆过滤器中仍保留（布隆过滤器不支持删除）", id));
        return ResponseEntity.noContent().build();
    }
}
