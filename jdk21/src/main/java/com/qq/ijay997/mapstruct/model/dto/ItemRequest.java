package com.qq.ijay997.mapstruct.model.dto;

/**
 * 明细行请求（record）。unitPrice 用 String 接收前端入参，
 * 映射到实体时由 MapStruct 自动转换为 BigDecimal。
 */
public record ItemRequest(String sku,
                          String productName,
                          Integer quantity,
                          String unitPrice) {
}
