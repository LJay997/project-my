package com.qq.ijay997.mapstruct.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 订单明细 DTO。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {

    private String sku;

    private String productName;

    private Integer quantity;

    /** BigDecimal -&gt; String，演示 numberFormat 数字格式化 */
    private String unitPrice;

    /** 源对象没有该字段，演示 expression 表达式计算（quantity * unitPrice） */
    private BigDecimal lineTotal;
}
