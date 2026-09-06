package com.qq.ijay997.jdk1617;

/**
 * JDK 17 —— sealed 密封类 Demo（正式发布于 17）。
 *
 * <p>sealed 限定类的直接子类只能是 permits 声明的这几个，
 * 结合 {@code non-sealed} 或再 {@code sealed} 形成受限继承层级，
 * 让「枚举可变集合」式的类型建模成为可能。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk1617.SealedDemo</p>
 *
 * @version JDK 17+
 */
public class SealedDemo {

    // 顶层 sealed 接口：只允许 Cat / Dog / Bird 实现
    sealed interface Animal permits Cat, Dog, Bird {
        String sound();
    }

    // 直接实现，用 final 完全封闭
    static final class Cat implements Animal {
        public String sound() { return "喵"; }
    }

    // 用 non-sealed 重新开放继承
    static non-sealed class Dog implements Animal {
        public String sound() { return "汪"; }
    }

    // Bird 继续 sealed 自己的子类，演示层级
    sealed static class Bird implements Animal permits Sparrow {
        public String sound() { return "叽"; }
    }

    static final class Sparrow extends Bird {
        @Override
        public String sound() { return "叽叽喳喳"; }
    }

    public static void main(String[] args) {
        // sealed 配合 switch 可做到完备性（无需 default）
        Animal a = new Cat();
        String msg = switch (a) {
            case Cat c -> "猫叫: " + c.sound();
            case Dog d -> "狗叫: " + d.sound();
            case Bird b -> "鸟叫: " + b.sound();
        };
        System.out.println(msg);

        // permits 背景说明：外部类无法继承 Animal（编译器不允许）
        System.out.println("Animal 的直接子类被限定在 permits 声明内，外部不可随意继承。");
    }
}
