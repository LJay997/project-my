package com.qq.ijay997.mapstruct.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 收货地址（传统可变 JavaBean，Lombok 生成 getter/setter）。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    /** 省份 */
    private String province;

    /** 城市 */
    private String city;

    /** 详细地址 */
    private String detail;
}
