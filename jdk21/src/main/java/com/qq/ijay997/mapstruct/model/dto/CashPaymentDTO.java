package com.qq.ijay997.mapstruct.model.dto;

/**
 * 现金支付 DTO（record 实现密封接口）。
 *
 * @param type         支付类型常量 CASH
 * @param cashReceived 实收金额（格式化字符串）
 * @param change       找零金额（格式化字符串）
 */
public record CashPaymentDTO(String type,
                             String cashReceived,
                             String change) implements PaymentDTO {
}
