package com.qq.ijay997.mapstruct.model.entity;

import java.math.BigDecimal;

/**
 * 货到付款 / 现金支付（record 实现密封接口）。
 *
 * @param cashReceived 实收现金
 * @param change       找零金额
 */
public record CashPayment(BigDecimal cashReceived, BigDecimal change) implements Payment {
}
