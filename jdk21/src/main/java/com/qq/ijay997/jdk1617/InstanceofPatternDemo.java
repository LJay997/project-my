package com.qq.ijay997.jdk1617;

/**
 * JDK 16 —— instanceof 模式匹配 Demo（正式发布于 16）。
 *
 * <p>模式变量使 instanceof 判断后无需再单独强转和声明变量，
 * 且生效范围自适应当前代码的求值域。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk1617.InstanceofPatternDemo</p>
 *
 * @version JDK 16+
 */
public class InstanceofPatternDemo {

    interface Shape { }

    record Circle(double radius) implements Shape { }
    record Rect(double w, double h) implements Shape { }

    static double area(Shape s) {
        // JDK 8 写法：先判断再强转
        // if (s instanceof Circle) { Circle c = (Circle) s; ... }

        // JDK 16：模式变量 c / r 在 instanceof 后直接可用于该分支内
        if (s instanceof Circle c) {
            return Math.PI * c.radius() * c.radius();
        }
        if (s instanceof Rect r) {
            return r.w() * r.h();
        }
        return 0;
    }

    public static void main(String[] args) {
        System.out.println("Circle(2) 面积 = " + area(new Circle(2)));
        System.out.println("Rect(3,4) 面积 = " + area(new Rect(3, 4)));

        Object obj = "hello java";
        // instanceof 后直接使用模式变量 pair
        if (obj instanceof String s && s.length() > 5) {
            System.out.println("字符串长度: " + s.length());
        }
        System.out.println("模式变量 s 在短路 && 后仍可用: " + (obj instanceof String s && s.startsWith("h")));
    }
}
