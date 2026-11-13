package com.qq.ijay997.jdk21.clone;

import java.util.List;

/**
 * 不可变人（Java Record）—— Record 天然适合做不可变值对象。
 *
 * <p>Record 不需要实现 Cloneable，也不要重写 clone()。
 * 需要"拷贝并修改"时，手动写一个 {@code withXxx} 或 {@code copy} 方法即可。</p>
 *
 * <p>Record 的组件默认 {@code private final}，构造器自动完成，equals/hashCode/toString 也都自动生成，
 * 是 JDK 16+ 里最适合做值对象的写法。</p>
 *
 * @param name    姓名（不可变）
 * @param age     年龄（不可变）
 * @param address 地址 Record（不可变）
 * @param hobbies 爱好列表 —— 构造时通过 {@link List#copyOf} 做防御性拷贝
 */
public record ImmutablePerson(
        String name,
        int age,
        ImmutableAddress address,
        List<String> hobbies
) {
    /** 紧凑构造器：保证 hobbies 不被外部修改 */
    public ImmutablePerson {
        hobbies = List.copyOf(hobbies);
    }

    /** 显式的 copy 方法，语义清晰。Record 没有默认提供 clone() */
    public ImmutablePerson copy() { return this; }

    public ImmutablePerson withName(String newName) {
        return new ImmutablePerson(newName, this.age, this.address, this.hobbies);
    }

    public ImmutablePerson withAddress(ImmutableAddress newAddress) {
        return new ImmutablePerson(this.name, this.age, newAddress, this.hobbies);
    }

    public ImmutablePerson addHobby(String hobby) {
        var newHobbies = new java.util.ArrayList<>(this.hobbies);
        newHobbies.add(hobby);
        return new ImmutablePerson(this.name, this.age, this.address, newHobbies);
    }

    /** 嵌套的不可变地址 Record */
    public record ImmutableAddress(String street, String city) {
        public ImmutableAddress withCity(String newCity) {
            return new ImmutableAddress(this.street, newCity);
        }
    }
}
