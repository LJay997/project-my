package com.qq.ijay997.jdk21.clone;

import java.io.Serializable;
import java.util.Objects;

/**
 * 地址 —— 作为引用类型字段，用于演示浅拷贝 vs 深拷贝的区别。
 * <p>实现了 {@link Serializable}，这样后续基于序列化的深拷贝方案才能处理它。</p>
 */
public class Address implements Serializable {

    private static final long serialVersionUID = 1L;

    private String street;
    private String city;

    public Address(String street, String city) {
        this.street = street;
        this.city = city;
    }

    /** 拷贝构造器：构造器拷贝与构造函数拷贝的核心工具 */
    public Address(Address other) {
        this.street = other.street;
        this.city = other.city;
    }

    public String getStreet() { return street; }
    public void setStreet(String street) { this.street = street; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Address address)) return false;
        return Objects.equals(street, address.street) && Objects.equals(city, address.city);
    }

    @Override
    public int hashCode() { return Objects.hash(street, city); }

    @Override
    public String toString() { return street + ", " + city; }
}
