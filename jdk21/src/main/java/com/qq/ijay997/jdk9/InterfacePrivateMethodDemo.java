package com.qq.ijay997.jdk9;

/**
 * JDK 9 —— 接口私有方法 Demo。
 *
 * <p>JDK 9 允许接口中出现 {@code private} / {@code private static} 方法，
 * 让多个 default 方法之间共享公共逻辑，减少重复代码，且不外泄给实现类。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk9.InterfacePrivateMethodDemo</p>
 *
 * @version JDK 9+
 */
public class InterfacePrivateMethodDemo {

    /** 业务接口：default 方法调用私有方法复用逻辑，对外只暴露公共契约 */
    interface Greeter {
        default String greet(String name) {
            return decorate(sayHello(name)); // 调用接口私有方法
        }

        default String farewell(String name) {
            return decorate(sayBye(name));   // 复用同一个私有方法
        }

        // JDK 9：接口私有方法（实例方法，可被 default 方法调用）
        private String decorate(String raw) {
            return ">>> " + raw + " <<<";
        }

        // JDK 9：接口私有静态方法
        private static String sayHello(String name) {
            return "Hello, " + name;
        }

        private static String sayBye(String name) {
            return "Bye, " + name;
        }
    }

    static class PersonGreeter implements Greeter {
    }

    public static void main(String[] args) {
        Greeter greeter = new PersonGreeter();
        System.out.println(greeter.greet("TRAE"));
        System.out.println(greeter.farewell("TRAE"));

        // 私有方法外部不可见，无法直接调用 —— 这是它被设计为 private 的意义
        System.out.println(">> 私有方法仅对接口内部调用方可见，外部实现类不可访问。");
    }
}
