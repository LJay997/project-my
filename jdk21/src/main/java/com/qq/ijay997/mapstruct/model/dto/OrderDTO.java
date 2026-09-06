package com.qq.ijay997.mapstruct.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 订单出参 DTO，集中演示：
 * 嵌套深层属性、集合映射、枚举/日期/金额格式化、多态映射、Map key 转换。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {

    private String orderNo;

    /** 深层嵌套：customer.name */
    private String customerName;

    /** 深层嵌套：customer.address.city */
    private String customerCity;

    /** List 元素级映射 */
    private List<OrderItemDTO> items;

    /** 枚举 -&gt; 字符串 */
    private String status;

    /** BigDecimal -&gt; 带货币符号的格式化字符串 */
    private String totalAmount;

    /** sealed 接口 -&gt; sealed DTO 接口，运行时按实际子类型映射 */
    private PaymentDTO payment;

    /** Map&lt;LocalDate, String&gt; -&gt; Map&lt;String, String&gt;，key 按日期格式化 */
    private Map<String, String> statusHistory;

    /** LocalDateTime -&gt; 格式化字符串 */
    private String createdAt;

    /** 仅当源备注非空白时才映射（conditionExpression），否则为 null */
    private String remark;
}
