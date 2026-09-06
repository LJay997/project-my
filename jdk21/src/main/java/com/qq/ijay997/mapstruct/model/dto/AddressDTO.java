package com.qq.ijay997.mapstruct.model.dto;

/**
 * 地址 DTO（JDK 21 record）。
 * <p>
 * MapStruct 1.5+ 支持 record 作为映射目标：通过<b>规范构造器</b>创建实例，
 * 因此要求每个 record 组件都能在源对象中找到匹配属性（或显式 ignore / 表达式填充）。
 */
public record AddressDTO(String province, String city, String detail) {
}
