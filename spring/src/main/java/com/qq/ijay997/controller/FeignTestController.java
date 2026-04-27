package com.qq.ijay997.controller;

import com.qq.ijay997.dto.OrderDTO;
import com.qq.ijay997.dto.Result;
import com.qq.ijay997.dto.UserDTO;
import com.qq.ijay997.feign.ExampleFeignClient;
import com.qq.ijay997.feign.OrderFeignClient;
import com.qq.ijay997.feign.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Feign 测试控制器 - 完整示例
 * 
 * @author ijay997
 */
@Slf4j
@RestController
@RequestMapping("/api/feign")
public class FeignTestController {

    @Autowired
    private ExampleFeignClient exampleFeignClient;
    
    @Autowired
    private UserFeignClient userFeignClient;
    
    @Autowired
    private OrderFeignClient orderFeignClient;

    // ==================== 外部 API 测试 ====================

    /**
     * 获取单个文章（外部 API）
     */
    @GetMapping("/post/{id}")
    public Map<String, Object> getPost(@PathVariable Long id) {
        return exampleFeignClient.getPost(id);
    }

    /**
     * 获取所有文章（外部 API）
     */
    @GetMapping("/posts")
    public List<Map<String, Object>> getPosts() {
        return exampleFeignClient.getPosts();
    }

    /**
     * 创建文章（外部 API）
     */
    @PostMapping("/post")
    public Map<String, Object> createPost(@RequestBody Map<String, Object> request) {
        return exampleFeignClient.createPost(request);
    }

    // ==================== 用户服务测试 ====================

    /**
     * 获取用户详情
     */
    @GetMapping("/user/{id}")
    public Result<UserDTO> getUser(@PathVariable Long id) {
        log.info("通过 Feign 调用获取用户: id={}", id);
        return userFeignClient.getUserById(id);
    }

    /**
     * 根据用户名获取用户
     */
    @GetMapping("/user/username/{username}")
    public Result<UserDTO> getUserByUsername(@PathVariable String username) {
        log.info("通过 Feign 调用获取用户: username={}", username);
        return userFeignClient.getUserByUsername(username);
    }

    /**
     * 获取所有用户
     */
    @GetMapping("/users")
    public Result<List<UserDTO>> getAllUsers() {
        log.info("通过 Feign 调用获取所有用户");
        return userFeignClient.getAllUsers();
    }

    /**
     * 创建用户
     */
    @PostMapping("/user")
    public Result<UserDTO> createUser(@RequestBody UserDTO userDTO) {
        log.info("通过 Feign 调用创建用户: {}", userDTO);
        return userFeignClient.createUser(userDTO);
    }

    /**
     * 更新用户
     */
    @PutMapping("/user/{id}")
    public Result<UserDTO> updateUser(@PathVariable Long id, @RequestBody UserDTO userDTO) {
        log.info("通过 Feign 调用更新用户: id={}, data={}", id, userDTO);
        return userFeignClient.updateUser(id, userDTO);
    }

    /**
     * 删除用户
     */
    @DeleteMapping("/user/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        log.info("通过 Feign 调用删除用户: id={}", id);
        return userFeignClient.deleteUser(id);
    }

    /**
     * 搜索用户
     */
    @GetMapping("/user/search")
    public Result<List<UserDTO>> searchUsers(@RequestParam String keyword) {
        log.info("通过 Feign 调用搜索用户: keyword={}", keyword);
        return userFeignClient.searchUsers(keyword);
    }

    // ==================== 订单服务测试 ====================

    /**
     * 获取订单详情
     */
    @GetMapping("/order/{id}")
    public Result<OrderDTO> getOrder(@PathVariable Long id) {
        log.info("通过 Feign 调用获取订单: id={}", id);
        return orderFeignClient.getOrderById(id);
    }

    /**
     * 获取用户的所有订单
     */
    @GetMapping("/order/user/{userId}")
    public Result<List<OrderDTO>> getUserOrders(@PathVariable Long userId) {
        log.info("通过 Feign 调用获取用户订单: userId={}", userId);
        return orderFeignClient.getOrdersByUserId(userId);
    }

    /**
     * 获取所有订单
     */
    @GetMapping("/orders")
    public Result<List<OrderDTO>> getAllOrders() {
        log.info("通过 Feign 调用获取所有订单");
        return orderFeignClient.getAllOrders();
    }

    /**
     * 创建订单
     */
    @PostMapping("/order")
    public Result<OrderDTO> createOrder(@RequestBody OrderDTO orderDTO) {
        log.info("通过 Feign 调用创建订单: {}", orderDTO);
        return orderFeignClient.createOrder(orderDTO);
    }

    /**
     * 更新订单状态
     */
    @PutMapping("/order/{id}/status")
    public Result<OrderDTO> updateOrderStatus(@PathVariable Long id, @RequestParam Integer status) {
        log.info("通过 Feign 调用更新订单状态: id={}, status={}", id, status);
        return orderFeignClient.updateOrderStatus(id, status);
    }

    /**
     * 取消订单
     */
    @PutMapping("/order/{id}/cancel")
    public Result<OrderDTO> cancelOrder(@PathVariable Long id) {
        log.info("通过 Feign 调用取消订单: id={}", id);
        return orderFeignClient.cancelOrder(id);
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/order/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        log.info("通过 Feign 调用删除订单: id={}", id);
        return orderFeignClient.deleteOrder(id);
    }

    /**
     * 根据状态查询订单
     */
    @GetMapping("/order/status/{status}")
    public Result<List<OrderDTO>> getOrdersByStatus(@PathVariable Integer status) {
        log.info("通过 Feign 调用查询订单: status={}", status);
        return orderFeignClient.getOrdersByStatus(status);
    }

    // ==================== 综合测试 ====================

    /**
     * 综合测试 - 获取用户及其订单
     */
    @GetMapping("/test/user-orders/{userId}")
    public Map<String, Object> getUserWithOrders(@PathVariable Long userId) {
        log.info("综合测试: 获取用户及其订单, userId={}", userId);
        
        Map<String, Object> result = new HashMap<>();
        
        // 获取用户信息
        Result<UserDTO> userResult = userFeignClient.getUserById(userId);
        result.put("user", userResult);
        
        // 获取用户订单
        Result<List<OrderDTO>> ordersResult = orderFeignClient.getOrdersByUserId(userId);
        result.put("orders", ordersResult);
        
        return result;
    }

    /**
     * 综合测试 - 创建用户并下单
     */
    @PostMapping("/test/create-user-order")
    public Map<String, Object> createUserAndOrder(@RequestBody Map<String, Object> request) {
        log.info("综合测试: 创建用户并下单");
        
        Map<String, Object> result = new HashMap<>();
        
        // 1. 创建用户
        UserDTO userDTO = new UserDTO();
        userDTO.setUsername((String) request.get("username"));
        userDTO.setEmail((String) request.get("email"));
        userDTO.setPhone((String) request.get("phone"));
        userDTO.setAge((Integer) request.get("age"));
        
        Result<UserDTO> userResult = userFeignClient.createUser(userDTO);
        result.put("userCreated", userResult);
        
        if (!userResult.isSuccess() || userResult.getData() == null) {
            result.put("orderCreated", Result.error("用户创建失败"));
            return result;
        }
        
        // 2. 创建订单
        OrderDTO orderDTO = new OrderDTO();
        orderDTO.setUserId(userResult.getData().getId());
        orderDTO.setProductName((String) request.get("productName"));
        orderDTO.setAmount(new BigDecimal(request.get("amount").toString()));
        orderDTO.setStatus(0);
        
        Result<OrderDTO> orderResult = orderFeignClient.createOrder(orderDTO);
        result.put("orderCreated", orderResult);
        
        return result;
    }

    /**
     * 健康检查
     */
    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> result = new HashMap<>();
        result.put("status", "UP");
        result.put("timestamp", System.currentTimeMillis());
        return result;
    }
}
