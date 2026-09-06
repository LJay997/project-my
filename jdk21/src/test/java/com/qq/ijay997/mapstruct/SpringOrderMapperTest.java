package com.qq.ijay997.mapstruct;

import com.qq.ijay997.mapstruct.mapper.SpringOrderMapperImpl;
import com.qq.ijay997.mapstruct.model.dto.OrderSummaryDTO;
import com.qq.ijay997.mapstruct.model.entity.Customer;
import com.qq.ijay997.mapstruct.model.entity.Order;
import com.qq.ijay997.mapstruct.model.entity.OrderItem;
import com.qq.ijay997.mapstruct.model.enums.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Component;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证 componentModel = "spring"：
 * 生成的实现类带有 @Component 注解，可由 Spring 容器托管。
 * 这里不启动容器，直接实例化验证；业务代码中使用 {@code @Autowired} 注入即可。
 */
@DisplayName("Spring 组件模型测试")
class SpringOrderMapperTest {

    @Test
    @DisplayName("生成类标注 @Component 且映射功能正常")
    void generatedImplShouldBeSpringComponent() {
        SpringOrderMapperImpl mapper = new SpringOrderMapperImpl();

        // 类上存在 @Component 注解
        Component component = mapper.getClass().getAnnotation(Component.class);
        assertNotNull(component, "生成的 Mapper 实现类必须标注 @Component");
        // Spring bean 名称默认规则：首字母小写
        assertTrue(component.value().isEmpty() || "springOrderMapperImpl".equals(component.value()));

        Order order = Order.builder()
                .orderNo("ORD-SPRING-1")
                .customer(Customer.builder().name("钱七").build())
                .status(OrderStatus.COMPLETED)
                .items(List.of(OrderItem.builder().sku("X").build(),
                        OrderItem.builder().sku("Y").build()))
                .build();

        OrderSummaryDTO summary = mapper.toSummary(order);

        assertEquals("ORD-SPRING-1", summary.orderNo());
        assertEquals("钱七", summary.customerName());
        assertEquals("COMPLETED", summary.status());
        assertEquals(2, summary.itemCount());
    }
}
