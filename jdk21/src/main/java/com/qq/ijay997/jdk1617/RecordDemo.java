package com.qq.ijay997.jdk1617;

/**
 * JDK 16 —— record 数据结构 Demo（正式发布于 16）。
 *
 * <p>record 是一种简约的数据载体：一行声明即可自动生成构造器、equals/hashCode/toString
 * 以及 component 访问器，极大减少 DTO 的样板代码。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk1617.RecordDemo</p>
 *
 * @version JDK 16+
 */
public class RecordDemo {

    // record：字段默认 final，自动生成一个构造器与对应访问器
    record Point(int x, int y) {
        // 紧凑构造器：用于参数校验/规约
        public Point {
            if (x < 0 || y < 0) {
                throw new IllegalArgumentException("坐标为负: " + x + "," + y);
            }
        }

        // 可添加非正式方法
        int distanceSquared() {
            return x * x + y * y;
        }
    }

    // record 也可以实现接口
    interface Named {
        String displayName();
    }

    record User(String name, int age) implements Named {
        @Override
        public String displayName() {
            return name.toUpperCase() + "(" + age + ")";
        }
    }

    public static void main(String[] args) {
        Point p = new Point(3, 4);
        System.out.println("点坐标: " + p);
        System.out.println("component x()=" + p.x() + "  y()=" + p.y());
        System.out.println("distanceSquared()=" + p.distanceSquared());

        // 自动生成的 equals/hashCode 基于字段
        Point q = new Point(3, 4);
        System.out.println("p.equals(q)=" + p.equals(q) + "   hashCode相等=" + (p.hashCode() == q.hashCode()));

        // 校验生效
        try {
            new Point(-1, 2);
        } catch (IllegalArgumentException e) {
            System.out.println("构造校验触发: " + e.getMessage());
        }

        User u = new User("trae", 7);
        System.out.println("接口方法: " + u.displayName());
    }
}
