package com.qq.ijay997.jdk9;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * JDK 9 —— 集合工厂方法 Demo。
 *
 * <p>新增 {@code List.of / Set.of / Map.of} 等静态工厂方法，
 * 可一行创建「不可变」集合，相比 JDK 8 的 Arrays.asList / 匿名 Map 更简洁安全。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk9.CollectionFactoryDemo</p>
 *
 * @version JDK 9+
 */
public class CollectionFactoryDemo {

    public static void main(String[] args) {
        // 不可变 List
        List<String> fruits = List.of("Apple", "Banana", "Cherry");
        System.out.println("List.of = " + fruits);

        // 不可变 Set（拒绝重复，重复会抛 IllegalArgumentException）
        Set<String> colors = Set.of("Red", "Green", "Blue");
        System.out.println("Set.of  = " + colors);

        // 不可变 Map（最多支持 10 对键值；键不可重复）
        Map<String, Integer> scores = Map.of("Java", 9, "Spring", 5);
        System.out.println("Map.of  = " + scores);

        // Map 键值对较多时用 ofEntries
        Map<String, Integer> more =
                Map.ofEntries(Map.entry("A", 1), Map.entry("B", 2), Map.entry("C", 3));
        System.out.println("Map.ofEntries = " + more);

        // 不可变性验证：任何修改都会抛 UnsupportedOperationException
        try {
            fruits.add("Durian");
        } catch (UnsupportedOperationException e) {
            System.out.println(">> 尝试修改不可变集合: 抛出 " + e.getClass().getSimpleName());
        }

        // 注意：of 不接受 null 元素
        try {
            List.of("a", null);
        } catch (NullPointerException e) {
            System.out.println(">> 尝试放入 null: 抛出 " + e.getClass().getSimpleName());
        }
    }
}
