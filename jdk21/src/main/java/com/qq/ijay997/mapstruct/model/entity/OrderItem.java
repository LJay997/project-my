package com.qq.ijay997.mapstruct.model.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单明细行实体。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

    private Long id;

    /** 商品 SKU 编码 */
    private String sku;

    private String productName;

    private Integer quantity;

    /** 单价（金额一律使用 BigDecimal，禁止 double） */
    private BigDecimal unitPrice;
}
