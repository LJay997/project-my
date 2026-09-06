package com.qq.ijay997.mapstruct.model.entity;

import java.time.LocalDateTime;

/**
 * 信用卡支付（record 实现密封接口）。
 *
 * @param cardNo 完整卡号（敏感信息，出参需脱敏）
 * @param bank   发卡行
 * @param paidAt 支付时间
 */
public record CreditCardPayment(String cardNo, String bank, LocalDateTime paidAt) implements Payment {
}
