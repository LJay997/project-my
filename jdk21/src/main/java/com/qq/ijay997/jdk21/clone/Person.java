package com.qq.ijay997.jdk21.clone;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 人 —— 演示 <b>浅拷贝的坑</b>。
 *
 * <p>直接调用 {@link Object#clone()} 时，{@code address} 和 {@code hobbies}
 * 只是把引用值复制了一份，<b>原对象与克隆对象共享同一个 Address / List 实例</b>。
 * 修改其中一方，另一方也会被连带修改。</p>
 *
 * <p>这类陷阱是生产代码中 clone 方案最常见的 Bug 来源，
 * 所以 Effective Java 建议优先使用拷贝构造器或静态工厂方法。</p>
 */
public class Person implements Cloneable, Serializable {

    private static final long serialVersionUID = 1L;

    private String name;                     // 基本类型 / String（不可变）→ 浅拷贝没问题
    private int age;
    private Address address;                 // 引用类型 → 浅拷贝会共享
    private List<String> hobbies;            // 集合 → 浅拷贝会共享

    public Person(String name, int age, Address address, List<String> hobbies) {
        this.name = name;
        this.age = age;
        this.address = address;
        this.hobbies = hobbies;
    }

    /**
     * <b>Object.clone 的默认行为是浅拷贝</b>。
     * 这里故意不重写 address / hobbies，用来演示"修改克隆对象会影响原对象"。
     */
    @Override
    protected Person clone() {
        try {
            // super.clone 调用 Object.clone()，JVM 直接做位复制
            return (Person) super.clone();
        } catch (CloneNotSupportedException e) {
            // 已经 implements Cloneable，不可能走到这里
            throw new AssertionError(e);
        }
    }

    public String getName() { return name; }
    public int getAge() { return age; }
    public Address getAddress() { return address; }
    public List<String> getHobbies() { return hobbies; }

    public void setName(String name) { this.name = name; }
    public void setAge(int age) { this.age = age; }
    public void setAddress(Address address) { this.address = address; }
    public void setHobbies(List<String> hobbies) { this.hobbies = hobbies; }

    /** 为序列化深拷贝准备的自定义写法（标准写法） */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
    }

    private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        ois.defaultReadObject();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Person person)) return false;
        return age == person.age
                && Objects.equals(name, person.name)
                && Objects.equals(address, person.address)
                && Objects.equals(hobbies, person.hobbies);
    }

    @Override
    public int hashCode() { return Objects.hash(name, age, address, hobbies); }

    @Override
    public String toString() {
        return "Person{name='" + name + "', age=" + age + ", address=" + address + ", hobbies=" + hobbies + "}";
    }
}
