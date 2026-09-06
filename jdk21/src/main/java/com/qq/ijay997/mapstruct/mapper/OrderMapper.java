package com.qq.ijay997.mapstruct.mapper;

import com.qq.ijay997.mapstruct.model.dto.CreateOrderRequest;
import com.qq.ijay997.mapstruct.model.dto.ItemRequest;
import com.qq.ijay997.mapstruct.model.dto.OrderDTO;
import com.qq.ijay997.mapstruct.model.dto.OrderItemDTO;
import com.qq.ijay997.mapstruct.model.dto.OrderSummaryDTO;
import com.qq.ijay997.mapstruct.model.entity.Customer;
import com.qq.ijay997.mapstruct.model.entity.Order;
import com.qq.ijay997.mapstruct.model.entity.OrderItem;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.MapMapping;
import org.mapstruct.factory.Mappers;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 订单 Mapper —— 覆盖 MapStruct 高级能力：
 * <pre>
 *  1. 嵌套深层属性映射（customer.name、customer.address.city）
 *  2. 集合映射（List 元素级方法自动发现 + @IterableMapping 的 null 策略）
 *  3. Map 映射（@MapMapping key 日期格式化）
 *  4. 表达式（expression 动态计算 lineTotal / itemCount）
 *  5. 条件映射（conditionExpression，MapStruct 1.6+）
 *  6. record 作为目标（OrderSummaryDTO）与作为源（CreateOrderRequest）
 *  7. uses 引用其他 Mapper 组合完成复杂对象图（CustomerMapper / PaymentMapper）
 * </pre>
 */
@Mapper(config = BaseMapperConfig.class,
        uses = {CustomerMapper.class, PaymentMapper.class})
public interface OrderMapper {

    OrderMapper INSTANCE = Mappers.getMapper(OrderMapper.class);

    /**
     * 订单实体 -&gt; 订单 DTO。
     */
    @Mappings({
            // 深层嵌套属性：直接用点号访问
            @Mapping(target = "customerName", source = "customer.name"),
            @Mapping(target = "customerCity", source = "customer.address.city"),
            // BigDecimal -&gt; String，DecimalFormat 模式
            @Mapping(target = "totalAmount", source = "totalAmount", numberFormat = "¥#,##0.00"),
            // LocalDateTime -&gt; String
            @Mapping(target = "createdAt", source = "createdAt", dateFormat = "yyyy-MM-dd HH:mm:ss"),
            // 条件表达式：备注为空白字符串时视为无备注（目标置 null）。
            // 注意表达式中使用的是映射方法的参数名 order，而非固定的 "source"。
            @Mapping(target = "remark", source = "remark",
                    conditionExpression = "java(order.getRemark() != null && !order.getRemark().isBlank())")
            // items / payment / statusHistory / status 均按类型自动匹配合适的映射方法
    })
    OrderDTO toDTO(Order order);

    /**
     * List 映射：源 List 为 null 时返回空 List（RETURN_DEFAULT）。
     * MapStruct 会自动对每个元素调用 {@link #toItemDTO(OrderItem)}。
     */
    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<OrderItemDTO> toItemDTOs(List<OrderItem> items);

    /**
     * 集合元素级映射方法，同时被 List 映射和其他入口复用。
     */
    @Mappings({
            @Mapping(target = "unitPrice", source = "unitPrice", numberFormat = "0.00"),
            // 源对象没有 lineTotal 属性：用 java 表达式现算（单价 * 数量）
            @Mapping(target = "lineTotal",
                    expression = "java(calcLineTotal(item.getUnitPrice(), item.getQuantity()))")
    })
    OrderItemDTO toItemDTO(OrderItem item);

    /**
     * Map 映射：key 为 LocalDate，按 valueDateFormat 转成字符串 key。
     */
    @MapMapping(valueDateFormat = "yyyy-MM-dd")
    Map<String, String> mapStatusHistory(Map<LocalDate, String> statusHistory);

    /**
     * 实体 -&gt; record 摘要 DTO。
     * MapStruct 调用 record 的规范构造器创建实例。
     */
    @Mappings({
            @Mapping(target = "customerName", source = "customer.name"),
            @Mapping(target = "status",
                    expression = "java(order.getStatus() == null ? null : order.getStatus().name())"),
            @Mapping(target = "itemCount",
                    expression = "java(order.getItems() == null ? 0 : order.getItems().size())")
    })
    OrderSummaryDTO toSummary(Order order);

    // ------------------------------------------------------------------
    // record 作为映射源：CreateOrderRequest -&gt; Order 实体
    // ------------------------------------------------------------------

    @Mappings({
            @Mapping(target = "id", ignore = true),
            // String customerName -&gt; Customer 对象，走下方的自定义方法
            @Mapping(target = "customer", source = "customerName"),
            @Mapping(target = "status", ignore = true),
            @Mapping(target = "totalAmount", ignore = true),
            @Mapping(target = "payment", ignore = true),
            @Mapping(target = "statusHistory", ignore = true),
            @Mapping(target = "createdAt", ignore = true)
    })
    Order toEntity(CreateOrderRequest request);

    @Mappings({
            @Mapping(target = "id", ignore = true),
            // String -&gt; BigDecimal 由 MapStruct 内置完成
            @Mapping(target = "unitPrice", source = "unitPrice")
    })
    OrderItem toItemEntity(ItemRequest request);

    // ---------- 自定义辅助方法（接口 default 方法，生成的实现类可直接调用） ----------

    /** 仅有客户名时构造一个“轻量 Customer”，持久化前再补全其他字段。 */
    default Customer customerNameToCustomer(String customerName) {
        if (customerName == null || customerName.isBlank()) {
            return null;
        }
        return Customer.builder().name(customerName).build();
    }

    /** 计算明细行小计。 */
    default BigDecimal calcLineTotal(BigDecimal unitPrice, Integer quantity) {
        if (unitPrice == null || quantity == null) {
            return null;
        }
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
