package com.qq.ijay997.feign.fallback;

import com.qq.ijay997.dto.OrderDTO;
import com.qq.ijay997.dto.Result;
import com.qq.ijay997.feign.OrderFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 订单服务 Feign Client 降级处理
 * 
 * 当服务调用失败时，返回降级数据，避免雪崩效应
 * 
 * @author ijay997
 */
@Slf4j
@Component
public class OrderFeignFallback implements OrderFeignClient {

    @Override
    public Result<OrderDTO> getOrderById(Long id) {
        log.error("OrderFeignClient.getOrderById 调用失败，id={}", id);
        return Result.error(503, "订单服务暂时不可用");
    }

    @Override
    public Result<List<OrderDTO>> getOrdersByUserId(Long userId) {
        log.error("OrderFeignClient.getOrdersByUserId 调用失败，userId={}", userId);
        return Result.success(Collections.emptyList());
    }

    @Override
    public Result<List<OrderDTO>> getAllOrders() {
        log.error("OrderFeignClient.getAllOrders 调用失败");
        return Result.success(Collections.emptyList());
    }

    @Override
    public Result<OrderDTO> createOrder(OrderDTO orderDTO) {
        log.error("OrderFeignClient.createOrder 调用失败，orderDTO={}", orderDTO);
        return Result.error(503, "订单服务暂时不可用，创建失败");
    }

    @Override
    public Result<OrderDTO> updateOrderStatus(Long id, Integer status) {
        log.error("OrderFeignClient.updateOrderStatus 调用失败，id={}, status={}", id, status);
        return Result.error(503, "订单服务暂时不可用");
    }

    @Override
    public Result<OrderDTO> cancelOrder(Long id) {
        log.error("OrderFeignClient.cancelOrder 调用失败，id={}", id);
        return Result.error(503, "订单服务暂时不可用");
    }

    @Override
    public Result<Void> deleteOrder(Long id) {
        log.error("OrderFeignClient.deleteOrder 调用失败，id={}", id);
        return Result.error(503, "订单服务暂时不可用");
    }

    @Override
    public Result<List<OrderDTO>> getOrdersByStatus(Integer status) {
        log.error("OrderFeignClient.getOrdersByStatus 调用失败，status={}", status);
        return Result.success(Collections.emptyList());
    }
}
