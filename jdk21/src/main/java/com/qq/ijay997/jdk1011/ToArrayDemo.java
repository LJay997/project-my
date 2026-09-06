package com.qq.ijay997.jdk1011;

import java.util.ArrayList;
import java.util.List;

/**
 * JDK 11 —— Collection.toArray(IntFunction) 方法引用 Demo。
 *
 * <p>新版 {@code toArray} 接受一个 {@code IntFunction} 生成目标数组，
 * 可用方法引用 {@code String[]::new} 一步到位，替代 JDK 8 的长度参数写法。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk1011.ToArrayDemo</p>
 *
 * @version JDK 11+
 */
public class ToArrayDemo {

    public static void main(String[] args) {
        List<String> names = new ArrayList<>(List.of("TRAE", "Java", "JDK11"));

        // JDK 8 的旧写法
        String[] oldWay = names.toArray(new String[0]);

        // JDK 11 的新写法：方法引用
        String[] newWay = names.toArray(String[]::new);

        System.out.println("旧写法数组: " + String.join(", ", oldWay));
        System.out.println("新写法数组: " + String.join(", ", newWay));
        System.out.println("两种写法元素一致: " + java.util.Arrays.equals(oldWay, newWay));
    }
}
