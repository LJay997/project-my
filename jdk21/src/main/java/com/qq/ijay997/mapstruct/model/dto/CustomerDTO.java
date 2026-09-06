package com.qq.ijay997.mapstruct.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 客户出参 DTO（传统可变 JavaBean）。
 * 字段名 / 类型与实体存在差异，用于演示 {@code @Mapping} 的各种能力。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDTO {

    private Long id;

    /** 源属性名为 name，演示改名映射 */
    private String customerName;

    private String email;

    /** 枚举 -&gt; 字符串（MapStruct 内置转换） */
    private String gender;

    /** LocalDate -&gt; 格式化字符串 */
    private String birthday;

    /** 嵌套对象 -&gt; record DTO */
    private AddressDTO address;

    /** Integer age -&gt; 自定义年龄段文案 */
    private String ageGroup;

    /** 手机号脱敏后的值，且仅当源手机号有效时才映射（条件映射） */
    private String maskedPhone;
}
