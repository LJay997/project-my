package com.qq.ijay997.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单数据传输对象
 * 
 * @author ijay997
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO implements Serializable {
    
    private static final long serialVersionUID = 1L;
    
    private Long id;
    private String orderNo;
    private Long userId;
    private String productName;
    private BigDecimal amount;
    private Integer status; // 0-待支付, 1-已支付, 2-已发货, 3-已完成, 4-已取消
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
