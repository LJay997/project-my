package com.qq.ijay997.jdk21;

/**
 * JDK 21 —— switch 的 null 处理 Demo（正式特性，JEP 441）。
 *
 * <p>传统 switch 遇到 {@code null} 会抛 NullPointerException；
 * JDK 21 允许显式声明 {@code case null}，配合模式匹配 switch 更安全完备。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk21.SwitchNullDemo</p>
 *
 * @version JDK 21+
 */
public class SwitchNullDemo {

    sealed interface Animal permits Cat, Dog {
        String name();
    }

    record Cat(String name) implements Animal { }
    record Dog(String name) implements Animal { }

    static String classify(Animal a) {
        // case null 显式处理空值，case default 兜底
        return switch (a) {
            case null -> "空引用";
            case Cat c -> "猫: " + c.name();
            case Dog d -> "狗: " + d.name();
        };
    }

    public static void main(String[] args) {
        System.out.println(classify(new Cat("Tom")));
        System.out.println(classify(new Dog("Bob")));
        System.out.println(classify(null));   // JDK21 不再抛 NPE，走 case null
    }
}
