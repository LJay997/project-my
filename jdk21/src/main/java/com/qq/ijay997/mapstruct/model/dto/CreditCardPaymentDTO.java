package com.qq.ijay997.mapstruct.model.dto;

/**
 * 信用卡支付 DTO（record 实现密封接口）。
 *
 * @param type         支付类型常量 CREDIT_CARD
 * @param maskedCardNo 脱敏后的卡号
 * @param bank         发卡行
 * @param paidAt       支付时间（格式化字符串）
 */
public record CreditCardPaymentDTO(String type,
                                   String maskedCardNo,
                                   String bank,
                                   String paidAt) implements PaymentDTO {
}
