package com.qq.ijay997.feign;

import com.qq.ijay997.dto.OrderDTO;
import com.qq.ijay997.dto.Result;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 订单服务 Feign Client
 * 
 * 使用方式：
 * 1. 注入此接口：@Autowired private OrderFeignClient orderFeignClient;
 * 2. 调用方法：orderFeignClient.getOrderById(1L);
 * 
 * @author ijay997
 */
@FeignClient(
    name = "order-service",
    url = "${feign.order-service.url:http://localhost:8082}",
    path = "/api/provider/orders",
    fallback = com.qq.ijay997.feign.fallback.OrderFeignFallback.class
)
public interface OrderFeignClient {

    /**
     * 根据ID获取订单
     * 
     * @param id 订单ID
     * @return 订单信息
     */
    @GetMapping("/{id}")
    Result<OrderDTO> getOrderById(@PathVariable("id") Long id);

    /**
     * 根据用户ID获取订单列表
     * 
     * @param userId 用户ID
     * @return 订单列表
     */
    @GetMapping("/user/{userId}")
    Result<List<OrderDTO>> getOrdersByUserId(@PathVariable("userId") Long userId);

    /**
     * 获取所有订单
     * 
     * @return 订单列表
     */
    @GetMapping
    Result<List<OrderDTO>> getAllOrders();

    /**
     * 创建订单
     * 
     * @param orderDTO 订单信息
     * @return 创建结果
     */
    @PostMapping
    Result<OrderDTO> createOrder(@RequestBody OrderDTO orderDTO);

    /**
     * 更新订单状态
     * 
     * @param id 订单ID
     * @param status 订单状态
     * @return 更新结果
     */
    @PutMapping("/{id}/status")
    Result<OrderDTO> updateOrderStatus(@PathVariable("id") Long id, @RequestParam("status") Integer status);

    /**
     * 取消订单
     * 
     * @param id 订单ID
     * @return 取消结果
     */
    @PutMapping("/{id}/cancel")
    Result<OrderDTO> cancelOrder(@PathVariable("id") Long id);

    /**
     * 删除订单
     * 
     * @param id 订单ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    Result<Void> deleteOrder(@PathVariable("id") Long id);

    /**
     * 根据状态查询订单
     * 
     * @param status 订单状态
     * @return 订单列表
     */
    @GetMapping("/status/{status}")
    Result<List<OrderDTO>> getOrdersByStatus(@PathVariable("status") Integer status);
}
