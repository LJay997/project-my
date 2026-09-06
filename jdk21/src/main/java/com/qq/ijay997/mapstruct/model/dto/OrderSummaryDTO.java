package com.qq.ijay997.mapstruct.model.dto;

/**
 * 订单摘要（JDK 21 record），演示 record 作为映射目标。
 *
 * @param orderNo      订单号
 * @param customerName 客户姓名（来自嵌套对象 customer.name）
 * @param status       状态字符串
 * @param itemCount    商品行数（由表达式从 items.size() 计算）
 */
public record OrderSummaryDTO(String orderNo,
                              String customerName,
                              String status,
                              Integer itemCount) {
}
