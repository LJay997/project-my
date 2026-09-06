package com.qq.ijay997.jdk1011;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * JDK 10 —— 局部变量类型推断（var）Demo。
 *
 * <p>用 {@code var} 声明局部变量，类型由编译器根据初始化表达式推断，
 * 减少冗余的显式类型书写。注意 var 只能用于局部变量，不能用做字段/参数/返回类型。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk1011.VarDemo</p>
 *
 * @version JDK 10+
 */
public class VarDemo {

    public static void main(String[] args) {
        // 基本推断：编译器推断为 String / int
        var message = "Hello, var!";
        var count = 42;
        System.out.println("var 推断类型 -> " + message + " / " + count);

        // 复杂泛型：避免写一长串显式类型
        Map<String, Map<String, List<Integer>>> groupMap = new HashMap<>();
        var inferredMap = new HashMap<String, Map<String, List<Integer>>>();
        System.out.println("map 推断类型: " + inferredMap.getClass().getSimpleName());

        // 用于循环
        for (var i = 0; i < 3; i++) {
            System.out.println("  loop var i = " + i);
        }

        var words = List.of("A", "B", "C");
        for (var w : words) {
            System.out.print(w + " ");
        }
        System.out.println();

        // var 与 lambda / 方法引用结合
        var stream = Stream.of(1, 2, 3).map(n -> n * n);
        System.out.println("var 承接流: " + stream.toList());

        // 注意：var 不能为 null 字面量、不能用于声明数组元素类型
        // var x = null;   // 编译错误
    }
}
