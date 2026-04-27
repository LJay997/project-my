package com.qq.ijay997.controller;

import com.qq.ijay997.dto.OrderDTO;
import com.qq.ijay997.dto.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 订单服务控制器 - 模拟微服务提供者
 * 
 * @author ijay997
 */
@Slf4j
@RestController
@RequestMapping("/api/provider/orders")
public class OrderServiceProviderController {

    private final Map<Long, OrderDTO> orderStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public OrderServiceProviderController() {
        // 初始化一些测试数据
        initTestData();
    }

    private void initTestData() {
        createOrder(1L, "iPhone 15", new BigDecimal("7999.00"), 1);
        createOrder(1L, "MacBook Pro", new BigDecimal("14999.00"), 0);
        createOrder(2L, "AirPods Pro", new BigDecimal("1899.00"), 2);
    }

    private OrderDTO createOrder(Long userId, String productName, BigDecimal amount, Integer status) {
        Long id = idGenerator.getAndIncrement();
        OrderDTO order = new OrderDTO();
        order.setId(id);
        order.setOrderNo("ORD" + System.currentTimeMillis() + id);
        order.setUserId(userId);
        order.setProductName(productName);
        order.setAmount(amount);
        order.setStatus(status);
        order.setCreateTime(LocalDateTime.now());
        order.setUpdateTime(LocalDateTime.now());
        orderStore.put(id, order);
        return order;
    }

    /**
     * 根据ID获取订单
     */
    @GetMapping("/{id}")
    public Result<OrderDTO> getOrderById(@PathVariable Long id) {
        log.info("查询订单: id={}", id);
        
        // 模拟网络延迟
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        OrderDTO order = orderStore.get(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        return Result.success(order);
    }

    /**
     * 根据用户ID获取订单列表
     */
    @GetMapping("/user/{userId}")
    public Result<List<OrderDTO>> getOrdersByUserId(@PathVariable Long userId) {
        log.info("查询用户订单: userId={}", userId);
        
        List<OrderDTO> orders = orderStore.values().stream()
                .filter(o -> o.getUserId().equals(userId))
                .collect(java.util.stream.Collectors.toList());
        
        return Result.success(orders);
    }

    /**
     * 获取所有订单
     */
    @GetMapping
    public Result<List<OrderDTO>> getAllOrders() {
        log.info("查询所有订单");
        return Result.success(new ArrayList<>(orderStore.values()));
    }

    /**
     * 创建订单
     */
    @PostMapping
    public Result<OrderDTO> createOrder(@RequestBody OrderDTO orderDTO) {
        log.info("创建订单: {}", orderDTO);
        
        OrderDTO newOrder = createOrder(
                orderDTO.getUserId(),
                orderDTO.getProductName(),
                orderDTO.getAmount(),
                orderDTO.getStatus() != null ? orderDTO.getStatus() : 0
        );
        
        return Result.success(newOrder);
    }

    /**
     * 更新订单状态
     */
    @PutMapping("/{id}/status")
    public Result<OrderDTO> updateOrderStatus(@PathVariable Long id, @RequestParam Integer status) {
        log.info("更新订单状态: id={}, status={}", id, status);
        
        OrderDTO order = orderStore.get(id);
        if (order == null) {
            return Result.error(404, "订单不存在");
        }
        
        order.setStatus(status);
        order.setUpdateTime(LocalDateTime.now());
        
        return Result.success(order);
    }

    /**
     * 取消订单
     */
    @PutMapping("/{id}/cancel")
    public Result<OrderDTO> cancelOrder(@PathVariable Long id) {
        log.info("取消订单: id={}", id);
        return updateOrderStatus(id, 4);
    }

    /**
     * 删除订单
     */
    @DeleteMapping("/{id}")
    public Result<Void> deleteOrder(@PathVariable Long id) {
        log.info("删除订单: id={}", id);
        
        if (orderStore.remove(id) == null) {
            return Result.error(404, "订单不存在");
        }
        
        return Result.success();
    }

    /**
     * 根据状态查询订单
     */
    @GetMapping("/status/{status}")
    public Result<List<OrderDTO>> getOrdersByStatus(@PathVariable Integer status) {
        log.info("查询订单: status={}", status);
        
        List<OrderDTO> orders = orderStore.values().stream()
                .filter(o -> o.getStatus().equals(status))
                .collect(java.util.stream.Collectors.toList());
        
        return Result.success(orders);
    }
}
