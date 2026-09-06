package com.qq.ijay997.mapstruct.mapper;

import com.qq.ijay997.mapstruct.model.dto.OrderSummaryDTO;
import com.qq.ijay997.mapstruct.model.entity.Order;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

/**
 * Spring 集成示例：{@code componentModel = "spring"}。
 * <p>
 * MapStruct 生成的实现类会带上 {@code @Component}，
 * 在 Spring Boot 中直接 {@code @Autowired / 构造注入} 即可，无需 {@code Mappers.getMapper()}。
 * <p>
 * 注意：一旦某条对象图链路使用 spring 模型，{@code uses} 引用的其他 Mapper
 * 也应统一为 spring 模型，由容器负责注入，不能混用 default 模型。
 */
@Mapper(componentModel = "spring")
public interface SpringOrderMapper {

    @Mappings({
            @Mapping(target = "customerName", source = "customer.name"),
            @Mapping(target = "status",
                    expression = "java(order.getStatus() == null ? null : order.getStatus().name())"),
            @Mapping(target = "itemCount",
                    expression = "java(order.getItems() == null ? 0 : order.getItems().size())")
    })
    OrderSummaryDTO toSummary(Order order);
}
