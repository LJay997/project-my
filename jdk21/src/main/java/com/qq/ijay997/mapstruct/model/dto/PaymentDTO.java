package com.qq.ijay997.mapstruct.model.dto;

/**
 * 支付方式 DTO（JDK 21 密封接口），与实体层 sealed 层次一一对应。
 */
public sealed interface PaymentDTO permits CreditCardPaymentDTO, CashPaymentDTO {
}
