package com.qq.ijay997.jdk21.clone;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 人（正确实现深拷贝）—— 演示重写 clone() 的正确姿势。
 *
 * <p>关键点：对每个可变的引用类型字段，必须显式递归调用其 clone 或使用拷贝构造器。
 * 漏掉任何一个字段，就是半深半浅的"假深拷贝"。</p>
 *
 * <p>反例：如果 Address 也实现了 Cloneable 并重写了 clone()，
 * 可以写成 {@code (Address) address.clone()}；
 * 但 Address 没实现 Cloneable，所以这里用的是拷贝构造器 {@code new Address(address)}。</p>
 */
public class DeepCopyPerson implements Cloneable {

    private String name;
    private int age;
    private Address address;
    private List<String> hobbies;

    public DeepCopyPerson(String name, int age, Address address, List<String> hobbies) {
        this.name = name;
        this.age = age;
        this.address = address;
        this.hobbies = hobbies;
    }

    /**
     * 拷贝构造器：直接做深拷贝，不需要调用 Object.clone。
     * <p>这是 Effective Java 推荐的方式，比重写 clone() 更灵活、更安全。</p>
     */
    public DeepCopyPerson(DeepCopyPerson other) {
        this.name = other.name;                 // String 不可变，直接引用
        this.age = other.age;
        // ↓ 下面才是深拷贝的核心
        this.address = new Address(other.address);
        this.hobbies = new ArrayList<>(other.hobbies);
    }

    /** 重写 clone()，逐字段深拷贝 */
    @Override
    protected DeepCopyPerson clone() {
        try {
            DeepCopyPerson cloned = (DeepCopyPerson) super.clone();
            // Object.clone 已完成基本类型 + String 的复制，
            // 但可变引用类型必须显式重建
            cloned.address = new Address(this.address);
            cloned.hobbies = new ArrayList<>(this.hobbies);
            return cloned;
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public Address getAddress() { return address; }
    public List<String> getHobbies() { return hobbies; }

    @Override
    public String toString() {
        return "DeepCopyPerson{name='" + name + "', age=" + age + ", address=" + address + ", hobbies=" + hobbies + "}";
    }
}
