package com.qq.ijay997.mapstruct.model.enums;

/**
 * 订单状态枚举。
 * MapStruct 内置支持 枚举 &lt;-&gt; String：默认使用 {@link Enum#name()}。
 */
public enum OrderStatus {

    PENDING("待支付"),
    PAID("已支付"),
    SHIPPED("已发货"),
    COMPLETED("已完成"),
    CANCELLED("已取消");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
