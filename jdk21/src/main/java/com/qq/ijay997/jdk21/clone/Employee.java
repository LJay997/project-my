package com.qq.ijay997.jdk21.clone;

import java.util.List;

/**
 * 雇员（不可变）—— 演示 <b>不可变对象不需要 clone</b>。
 *
 * <p>当一个对象的所有字段都是 {@code final}、且字段类型本身也不可变时，
 * "克隆"等于直接返回原对象引用。Java 标准库里的 {@link String}、
 * {@link Integer}、{@link java.time.LocalDateTime} 都是这种策略。</p>
 *
 * <p>如果业务上希望"修改后得到新对象"，提供一个 {@code withXxx(newValue)} 风格的方法更直观。</p>
 */
public final class Employee {

    private final String id;
    private final String name;
    private final int salary;
    /** 注意：不可变类里仍然可以持有可变字段，必须在构造时做防御性拷贝 */
    private final List<String> skills;

    public Employee(String id, String name, int salary, List<String> skills) {
        this.id = id;
        this.name = name;
        this.salary = salary;
        // 防御性拷贝：外部传入的 skills 可能被修改，这里先隔离
        this.skills = List.copyOf(skills);
    }

    /**
     * 不可变对象的"拷贝"——直接返回 this 即可。
     * 或者提供一个 {@code copy()} 方法，让 API 更清晰。
     */
    public Employee copy() { return this; }

    /** withXxx：修改某个字段并返回新实例，不可变对象的惯用模式 */
    public Employee withSalary(int newSalary) {
        return new Employee(this.id, this.name, newSalary, this.skills);
    }

    public Employee addSkill(String skill) {
        var newSkills = new java.util.ArrayList<>(this.skills);
        newSkills.add(skill);
        return new Employee(this.id, this.name, this.salary, newSkills);
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public int getSalary() { return salary; }
    public List<String> getSkills() { return skills; }

    @Override
    public String toString() {
        return "Employee{id='" + id + "', name='" + name + "', salary=" + salary + ", skills=" + skills + "}";
    }
}
