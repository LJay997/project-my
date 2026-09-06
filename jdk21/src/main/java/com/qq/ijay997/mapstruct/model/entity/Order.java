package com.qq.ijay997.mapstruct.model.entity;

import com.qq.ijay997.mapstruct.model.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单实体（聚合根）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Order {

    private Long id;

    private String orderNo;

    /** 嵌套对象：演示深层属性映射 customer.name -&gt; customerName */
    private Customer customer;

    /** 集合：演示 List 元素级映射 */
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    private OrderStatus status;

    private BigDecimal totalAmount;

    /** 密封类型字段：演示 @SubclassMapping 多态映射 */
    private Payment payment;

    /** 状态流转历史：演示 @MapMapping 的 key 日期格式化 */
    @Builder.Default
    private Map<LocalDate, String> statusHistory = new HashMap<>();

    private LocalDateTime createdAt;

    /** 备注（可能为空白串，演示 conditionExpression 条件映射） */
    private String remark;
}
