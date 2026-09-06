package com.qq.ijay997.mapstruct.model.dto;

import java.util.List;

/**
 * 创建订单请求（JDK 21 record），演示 record 作为映射源。
 *
 * @param orderNo      订单号
 * @param customerName 客户姓名（映射时需转换成 Customer 实体）
 * @param remark       备注
 * @param items        明细行请求
 */
public record CreateOrderRequest(String orderNo,
                                 String customerName,
                                 String remark,
                                 List<ItemRequest> items) {
}
