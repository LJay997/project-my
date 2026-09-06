package com.qq.ijay997.mapstruct;

import com.qq.ijay997.mapstruct.mapper.OrderMapper;
import com.qq.ijay997.mapstruct.model.dto.CashPaymentDTO;
import com.qq.ijay997.mapstruct.model.dto.CreateOrderRequest;
import com.qq.ijay997.mapstruct.model.dto.CreditCardPaymentDTO;
import com.qq.ijay997.mapstruct.model.dto.ItemRequest;
import com.qq.ijay997.mapstruct.model.dto.OrderDTO;
import com.qq.ijay997.mapstruct.model.dto.OrderItemDTO;
import com.qq.ijay997.mapstruct.model.dto.OrderSummaryDTO;
import com.qq.ijay997.mapstruct.model.dto.PaymentDTO;
import com.qq.ijay997.mapstruct.model.entity.CashPayment;
import com.qq.ijay997.mapstruct.model.entity.CreditCardPayment;
import com.qq.ijay997.mapstruct.model.entity.Customer;
import com.qq.ijay997.mapstruct.model.entity.Order;
import com.qq.ijay997.mapstruct.model.entity.OrderItem;
import com.qq.ijay997.mapstruct.model.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * OrderMapper 测试：嵌套属性、集合、Map、表达式、条件表达式、
 * sealed 多态联动、record 目标/源、集合 null 策略。
 */
@DisplayName("订单映射测试")
class OrderMapperTest {

    private final OrderMapper mapper = OrderMapper.INSTANCE;

    @Test
    @DisplayName("完整对象图：深层嵌套 + List 元素映射 + 金额/日期格式化 + Map key 转换 + 多态支付")
    void shouldMapOrderToDTO() {
        // given
        Customer customer = Customer.builder()
                .id(1L)
                .name("张三")
                .address(new com.qq.ijay997.mapstruct.model.entity.Address(
                        "广东省", "深圳市", "南山区"))
                .build();

        OrderItem item = OrderItem.builder()
                .id(10L)
                .sku("SKU-001")
                .productName("无线鼠标")
                .quantity(2)
                .unitPrice(new BigDecimal("199.00"))
                .build();

        Order order = Order.builder()
                .id(100L)
                .orderNo("ORD20260914001")
                .customer(customer)
                .items(List.of(item))
                .status(OrderStatus.PAID)
                .totalAmount(new BigDecimal("1234.50"))
                .payment(new CreditCardPayment("6222021234561234", "工商银行",
                        LocalDateTime.of(2026, 9, 14, 10, 30, 0)))
                .statusHistory(Map.of(
                        LocalDate.of(2026, 9, 10), "下单",
                        LocalDate.of(2026, 9, 14), "支付"
                ))
                .createdAt(LocalDateTime.of(2026, 9, 14, 10, 30, 0))
                .remark("  ")  // 空白备注：应被 conditionExpression 拦截为 null
                .build();

        // when
        OrderDTO dto = mapper.toDTO(order);

        // then：标量与嵌套
        assertAll(
                () -> assertEquals("ORD20260914001", dto.getOrderNo()),
                () -> assertEquals("张三", dto.getCustomerName()),
                () -> assertEquals("深圳市", dto.getCustomerCity()),
                () -> assertEquals("PAID", dto.getStatus()),
                () -> assertEquals("¥1,234.50", dto.getTotalAmount()),
                () -> assertEquals("2026-09-14 10:30:00", dto.getCreatedAt()),
                () -> assertNull(dto.getRemark(), "空白备注应被条件表达式拦截")
        );

        // 集合元素映射 + expression 行小计 + numberFormat
        assertEquals(1, dto.getItems().size());
        OrderItemDTO itemDTO = dto.getItems().get(0);
        assertAll(
                () -> assertEquals("SKU-001", itemDTO.getSku()),
                () -> assertEquals("无线鼠标", itemDTO.getProductName()),
                () -> assertEquals("199.00", itemDTO.getUnitPrice()),
                () -> assertEquals(0, new BigDecimal("398.00").compareTo(itemDTO.getLineTotal()),
                        "行小计应为 单价199.00 × 数量2 = 398.00")
        );

        // Map key 日期格式化
        assertAll(
                () -> assertEquals("下单", dto.getStatusHistory().get("2026-09-10")),
                () -> assertEquals("支付", dto.getStatusHistory().get("2026-09-14"))
        );

        // sealed 多态：信用卡
        PaymentDTO payment = dto.getPayment();
        CreditCardPaymentDTO card = assertInstanceOf(CreditCardPaymentDTO.class, payment);
        assertAll(
                () -> assertEquals("CREDIT_CARD", card.type()),
                () -> assertEquals("6222********1234", card.maskedCardNo()),
                () -> assertEquals("工商银行", card.bank()),
                () -> assertEquals("2026-09-14 10:30:00", card.paidAt())
        );
    }

    @Test
    @DisplayName("多态：现金支付映射为 CashPaymentDTO")
    void shouldMapCashPaymentPolymorphically() {
        Order order = Order.builder()
                .orderNo("ORD-CASH-1")
                .payment(new CashPayment(new BigDecimal("200.00"), new BigDecimal("35.50")))
                .build();

        OrderDTO dto = mapper.toDTO(order);

        CashPaymentDTO cash = assertInstanceOf(CashPaymentDTO.class, dto.getPayment());
        assertAll(
                () -> assertEquals("CASH", cash.type()),
                () -> assertEquals("200.00", cash.cashReceived()),
                () -> assertEquals("35.50", cash.change())
        );
    }

    @Test
    @DisplayName("record 目标：Order -> OrderSummaryDTO，itemCount 由表达式计算")
    void shouldMapToRecordSummary() {
        Order order = Order.builder()
                .orderNo("ORD-S-1")
                .customer(Customer.builder().name("赵六").build())
                .status(OrderStatus.SHIPPED)
                .items(List.of(
                        OrderItem.builder().sku("A").build(),
                        OrderItem.builder().sku("B").build(),
                        OrderItem.builder().sku("C").build()))
                .build();

        OrderSummaryDTO summary = mapper.toSummary(order);

        assertAll(
                () -> assertEquals("ORD-S-1", summary.orderNo()),
                () -> assertEquals("赵六", summary.customerName()),
                () -> assertEquals("SHIPPED", summary.status()),
                () -> assertEquals(3, summary.itemCount())
        );
    }

    @Test
    @DisplayName("集合 null 策略：items 为 null 时返回空集合而非 null")
    void shouldReturnEmptyCollectionWhenSourceItemsNull() {
        Order order = Order.builder().orderNo("ORD-NULL").items(null).build();

        OrderDTO dto = mapper.toDTO(order);

        assertNotNull(dto.getItems());
        assertTrue(dto.getItems().isEmpty());
    }

    @Test
    @DisplayName("record 源：CreateOrderRequest -> Order，String 单价自动转 BigDecimal")
    void shouldMapFromRecordRequest() {
        CreateOrderRequest request = new CreateOrderRequest(
                "ORD-NEW-1",
                "新客户",
                "尽快发货",
                List.of(new ItemRequest("SKU-N", "键盘", 1, "299.50"))
        );

        Order entity = mapper.toEntity(request);

        assertAll(
                () -> assertEquals("ORD-NEW-1", entity.getOrderNo()),
                () -> assertEquals("尽快发货", entity.getRemark()),
                () -> assertNotNull(entity.getCustomer()),
                () -> assertEquals("新客户", entity.getCustomer().getName()),
                () -> assertNull(entity.getStatus(), "ignore 的字段为 null"),
                () -> assertNull(entity.getId())
        );

        OrderItem item = entity.getItems().get(0);
        assertAll(
                () -> assertEquals("SKU-N", item.getSku()),
                () -> assertEquals(0, new BigDecimal("299.50").compareTo(item.getUnitPrice()),
                        "String \"299.50\" 应自动转换为 BigDecimal"),
                () -> assertEquals(1, item.getQuantity()),
                () -> assertNull(item.getId())
        );
    }
}
