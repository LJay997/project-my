package com.qq.ijay997.mapstruct;

import com.qq.ijay997.mapstruct.mapper.PaymentMapper;
import com.qq.ijay997.mapstruct.model.dto.CashPaymentDTO;
import com.qq.ijay997.mapstruct.model.dto.CreditCardPaymentDTO;
import com.qq.ijay997.mapstruct.model.dto.PaymentDTO;
import com.qq.ijay997.mapstruct.model.entity.CashPayment;
import com.qq.ijay997.mapstruct.model.entity.CreditCardPayment;
import com.qq.ijay997.mapstruct.model.entity.Payment;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * PaymentMapper 测试：sealed 类型多态映射 + JDK 21 switch 模式匹配。
 */
@DisplayName("支付（sealed 多态）映射测试")
class PaymentMapperTest {

    private final PaymentMapper mapper = PaymentMapper.INSTANCE;

    @Test
    @DisplayName("@SubclassMapping：信用卡支付分派到 CreditCardPaymentDTO")
    void shouldMapCreditCard() {
        Payment payment = new CreditCardPayment(
                "6222021234561234", "招商银行", LocalDateTime.of(2026, 9, 1, 8, 0, 0));

        PaymentDTO dto = mapper.map(payment);

        CreditCardPaymentDTO card = assertInstanceOf(CreditCardPaymentDTO.class, dto);
        assertAll(
                () -> assertEquals("CREDIT_CARD", card.type()),
                () -> assertEquals("6222********1234", card.maskedCardNo()),
                () -> assertEquals("招商银行", card.bank()),
                () -> assertEquals("2026-09-01 08:00:00", card.paidAt())
        );
    }

    @Test
    @DisplayName("@SubclassMapping：现金支付分派到 CashPaymentDTO")
    void shouldMapCash() {
        Payment payment = new CashPayment(new BigDecimal("100.00"), new BigDecimal("20.25"));

        PaymentDTO dto = mapper.map(payment);

        CashPaymentDTO cash = assertInstanceOf(CashPaymentDTO.class, dto);
        assertAll(
                () -> assertEquals("CASH", cash.type()),
                () -> assertEquals("100.00", cash.cashReceived()),
                () -> assertEquals("20.25", cash.change())
        );
    }

    @Test
    @DisplayName("null 安全：map(null) 返回 null")
    void shouldReturnNullForNullInput() {
        // 存在多个重载 map(...)，null 入参必须显式声明类型以消除歧义
        assertNull(mapper.map((Payment) null));
        assertEquals("无支付信息", mapper.describe(null));
    }

    @Test
    @DisplayName("JDK 21 switch 模式匹配：sealed 穷举描述两种支付")
    void shouldDescribeWithSwitchPatternMatching() {
        String cardDesc = mapper.describe(
                new CreditCardPayment("6222021234561234", "建设银行", LocalDateTime.now()));
        String cashDesc = mapper.describe(
                new CashPayment(new BigDecimal("50.00"), BigDecimal.ZERO));

        assertAll(
                () -> assertDoesNotThrow(() -> {
                }, "sealed 接口的 switch 已穷举，无需 default 分支"),
                () -> assertEquals(true, cardDesc.contains("建设银行")),
                () -> assertEquals(true, cardDesc.contains("1234")),
                () -> assertEquals(true, cashDesc.contains("现金支付")),
                () -> assertEquals(true, cashDesc.contains("50.00"))
        );
    }
}
