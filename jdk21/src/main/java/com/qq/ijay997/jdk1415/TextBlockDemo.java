package com.qq.ijay997.jdk1415;

/**
 * JDK 15 —— 文本块（Text Blocks）Demo。
 *
 * <p>用三个双引号 {@code """ ... """} 书写多行字符串，
 * 自动处理缩进对齐（去除公共前导空白），比字符串拼接/转义清晰得多。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk1415.TextBlockDemo</p>
 *
 * @version JDK 15+
 */
public class TextBlockDemo {

    public static void main(String[] args) {
        // 文本块：保留换行，自动去掉公共缩进
        String json = """
                {
                  "name": "TRAE",
                  "tags": ["video", "ai"],
                  "active": true
                }
                """;
        System.out.println("--- 文本块 (JSON) ---");
        System.out.print(json);

        // 配合 formatted 占位符（JDK 15 formal 也在 String 上提供）
        String greeting = """
                Hello %s,
                today is %s.
                """.formatted("Java 17", "a fine day");
        System.out.println("--- formatted ---");
        System.out.print(greeting);

        // 与普通字符串拼接的对比示例
        String oldWay = "line1\nline2\nline3";
        System.out.println("--- 是否包含换行 ---");
        System.out.println("文本块含换行: " + json.contains("\n"));
    }
}
