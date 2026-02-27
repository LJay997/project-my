package com.qq.ijay997;

import java.util.*;

class Plant {
    /**
     * 生命周期枚举，表示植物的生长周期类型。
     * ANNUAL: 一年生植物，生命周期为一年。
     * PERENNIAL: 多年生植物，生命周期超过两年。
     * BIENNIAL: 两年生植物，生命周期为两年。
     */
    enum LifeCycle {ANNUAL, PERENNIAL, BIENNIAL}

    final String name;
    final LifeCycle lifeCycle;

    Plant(String name, LifeCycle lifeCycle) {
        this.name = name;
        this.lifeCycle = lifeCycle;
    }

    @Override
    public String toString() {
        return name;
    }

    enum Permission { READ, WRITE, EXECUTE, DELETE }

    public static <E extends Enum<E>> EnumSet<E> fromBitVector(Class<E> enumClass, long bits) {
        EnumSet<E> set = EnumSet.noneOf(enumClass);
        E[] constants = enumClass.getEnumConstants();

        // 遍历所有枚举常量
        for (int i = 0; i < constants.length && i < 64; i++) {
            if ((bits & (1L << i)) != 0) {
                set.add(constants[i]);
            }
        }
        return set;
    }

    public static void main(String[] args) {
        // 假设位向量 bits = 0b1101 (十进制 13)
// 对应：READ(0)=1, WRITE(1)=0, EXECUTE(2)=1, DELETE(3)=1
        long bits = 13L;

        EnumSet<Permission> perms = fromBitVector(Permission.class, bits);
        System.out.println(perms); // 输出: [READ, EXECUTE, DELETE]
    }


}