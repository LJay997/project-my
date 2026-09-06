package com.qq.ijay997.jdk21;

/**
 * JDK 21 —— 记录模式（Record Patterns）Demo（正式特性，JEP 440）。
 *
 * <p>允许在 instanceof 和 switch 模式中用解构语法直接取出 record 的组件，
 * 与类型模式结合可写出非常声明式的数据遍历逻辑。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk21.RecordPatternDemo</p>
 *
 * @version JDK 21+
 */
public class RecordPatternDemo {

    record Point(int x, int y) { }

    sealed interface Shape permits Circle, Rect, PointShape {
    }

    record Circle(Point center, double radius) implements Shape { }
    record Rect(Point topLeft, Point bottomRight) implements Shape { }
    record PointShape(Point p) implements Shape { }

    static String describe(Object obj) {
        // switch 中使用记录模式解构
        return switch (obj) {
            case Circle(var c, var r) -> "圆: 圆心" + c + " 半径=" + r;
            case Rect(var a, var b) -> "矩形: " + a + " ~ " + b;
            case PointShape(var p) -> "单点: " + p;
            case null -> "空引用";
            default -> "其它: " + obj;
        };
    }

    // instanceof + 记录模式：直接解构
    static double area(Shape s) {
        if (s instanceof Rect(var tl, var br)) {
            return Math.abs(br.x() - tl.x()) * Math.abs(br.y() - tl.y());
        }
        if (s instanceof Circle(var c, var r)) {
            return Math.PI * r * r;
        }
        return 0;
    }

    public static void main(String[] args) {
        Point O = new Point(0, 0);
        System.out.println(describe(new Circle(O, 2.5)));
        System.out.println(describe(new Rect(new Point(1, 1), new Point(4, 5))));
        System.out.println(describe(new PointShape(new Point(7, 8))));
        System.out.println(describe(null));

        System.out.println("Rect 面积 = " + area(new Rect(new Point(1, 1), new Point(4, 5))));
        System.out.println("Circle 面积 = " + area(new Circle(O, 2)));
    }
}
