package com.qq.ijay997.mapstruct.mapper;

import com.qq.ijay997.mapstruct.model.dto.AddressDTO;
import com.qq.ijay997.mapstruct.model.dto.CustomerDTO;
import com.qq.ijay997.mapstruct.model.entity.Address;
import com.qq.ijay997.mapstruct.model.entity.Customer;
import org.mapstruct.Condition;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.InheritInverseConfiguration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Mappings;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * 客户 Mapper —— 覆盖 MapStruct 基础能力：
 * <pre>
 *  1. 字段改名映射（name -&gt; customerName）
 *  2. 内置类型转换（枚举 -&gt; String、LocalDate -&gt; String）
 *  3. 自定义映射方法（@Named + qualifiedByName）
 *  4. 条件映射（@Condition + conditionQualifiedByName，MapStruct 1.6+）
 *  5. 逆映射（@InheritInverseConfiguration）
 *  6. 更新既有对象（@MappingTarget + @InheritConfiguration）
 * </pre>
 *
 * 这里故意把 unmappedTargetPolicy 收紧为 ERROR：
 * 任何目标属性漏映射都会直接编译失败，是 DTO 场景推荐的严格策略。
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface CustomerMapper {

    CustomerMapper INSTANCE = Mappers.getMapper(CustomerMapper.class);

    /**
     * 实体 -&gt; DTO。
     */
    @Mappings({
            @Mapping(target = "customerName", source = "name"),
            @Mapping(target = "birthday", source = "birthday", dateFormat = "yyyy-MM-dd"),
            @Mapping(target = "ageGroup", source = "age", qualifiedByName = "toAgeGroup"),
            // 先通过 @Condition 方法判断手机号是否有效，有效才执行脱敏映射
            @Mapping(target = "maskedPhone", source = "phone",
                    qualifiedByName = "maskPhone", conditionQualifiedByName = "validPhone")
    })
    CustomerDTO toDTO(Customer customer);

    /**
     * DTO -&gt; 实体（逆映射）。
     * {@code @InheritInverseConfiguration} 自动反转 toDTO 的名称/日期配置；
     * 枚举 String -&gt; Gender、String -&gt; LocalDate 由 MapStruct 自动反向转换。
     * 实体中 DTO 不关心的字段（age/phone/createdAt）显式忽略。
     */
    @InheritInverseConfiguration(name = "toDTO")
    @Mapping(target = "age", ignore = true)
    @Mapping(target = "phone", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Customer toEntity(CustomerDTO dto);

    /**
     * 用 DTO 的内容<b>更新</b>既有实体（不新建对象），常用于 update 接口。
     * 复用 toEntity 的全部映射配置。
     */
    @InheritConfiguration(name = "toEntity")
    void updateEntity(CustomerDTO dto, @MappingTarget Customer customer);

    // ---------- 嵌套类型的双向映射（OrderMapper 也会复用这两个方法） ----------

    AddressDTO toAddressDTO(Address address);

    Address toAddress(AddressDTO dto);

    // ---------- 自定义方法 ----------

    /**
     * 条件方法（MapStruct 1.6+ 的 {@link Condition}）：
     * 返回 true 时该属性才会被映射。配合 conditionQualifiedByName 精确绑定到 maskedPhone。
     */
    @Named("validPhone")
    @Condition
    default boolean hasValidPhone(String phone) {
        return phone != null && !phone.isBlank() && phone.length() >= 7;
    }

    /** 手机号脱敏：13812345678 -&gt; 138****5678 */
    @Named("maskPhone")
    default String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        String prefix = phone.substring(0, 3);
        String suffix = phone.substring(phone.length() - 4);
        return prefix + "****" + suffix;
    }

    /**
     * 年龄 -&gt; 年龄段。
     * 使用 JDK 21 正式特性：switch + 类型模式 + guard（when）。
     */
    @Named("toAgeGroup")
    default String toAgeGroup(Integer age) {
        if (age == null) {
            return "未知";
        }
        return switch (age) {
            case Integer a when a < 18 -> "青少年";
            case Integer a when a < 60 -> "成年";
            default -> "老年";
        };
    }
}
