package com.qq.ijay997.jdk9;

/**
 * JDK 9 —— try-with-resources 改进 Demo。
 *
 * <p>JDK 9 起允许在 try-with-resources 中使用「已经是 final（或 effectively final）
 * 的外部变量」，而不必像 JDK 7 那样必须在 try(...) 内创建资源。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk9.TryWithResourcesDemo</p>
 *
 * @version JDK 9+
 */
public class TryWithResourcesDemo {

    /**
     * 简单的可关闭资源组合，记录 open/close 状态
     */
    static class GreetingSource implements AutoCloseable {
        private final String name;
        private boolean closed = false;

        GreetingSource(String name) {
            this.name = name;
            System.out.println("  [打开资源] " + name);
        }

        String readLine() {
            return closed ? "<已关闭>" : "内容来自 " + name + " 的通道";
        }

        @Override
        public void close() {
            closed = true;
            System.out.println("  [关闭资源] " + name);
        }
    }

    public static void main(String[] args) {
        // 资源在 try 之前创建，引用是 effectively final——JDK 9 可用，JDK 7 不行
        GreetingSource bookSource = new GreetingSource("book");
        // 用作 try-with-resources 资源的变量应为 final 或有效 final
//        bookSource = new GreetingSource("book");
        GreetingSource noteSource = new GreetingSource("note");

        System.out.println("--- JDK 9: 在外部声明的资源也可用于 try-with-resources ---");
//        编译器通过解析 try() 括号内资源的书写顺序来确定声明序列，并严格遵循“后进先出”原则生成逆序关闭的字节码。
        try (bookSource; noteSource) {
            System.out.println("  已读取: " + bookSource.readLine());
            System.out.println("  已读取: " + noteSource.readLine());
        }
        System.out.println("--- 两个资源均已自动关闭，无需手动 close() ---");
    }
}
