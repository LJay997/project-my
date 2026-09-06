package com.qq.ijay997.jdk1011;

import java.util.List;

/**
 * JDK 11 —— String 新增方法 Demo。
 *
 * <p>新增 {@code isBlank / strip / lines / repeat / indent / transform} 等，
 * 文本处理比 JDK 8 更顺手。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk1011.StringMethodsDemo</p>
 *
 * @version JDK 11+
 */
public class StringMethodsDemo {

    public static void main(String[] args) {
        // isBlank：空白（含空串、全空格、换行）返回 true
        System.out.println("isBlank(\"   \") = " + "   ".isBlank());
        System.out.println("isBlank(\"\\t\\n\") = " + "\t\n".isBlank());

        // strip：去除头尾空白（比 trim 更全面，支持 Unicode 空白）
        System.out.println("strip('[  hi  ]') = [" + "  hi  \u2005".strip() + "]");

        // lines：按行拆分成 Stream
        String poem = """
                春眠不觉晓
                处处闻啼鸟
                """;
        long lineCount = poem.lines().count();
        System.out.println("lines() 行数 = " + lineCount);

        // repeat：重复拼接
        System.out.println("repeat('ab')*3 = " + "ab".repeat(3));

        // indent：整体缩进指定空格数
        System.out.println("indent(4):");
        System.out.println("line1\nline2".indent(2));

        // transform：把字符串做一次转换回传（函数式管道）
        int length = "   hello world   ".transform(String::strip).length();
        System.out.println("transform(string->length) 结果长度 = " + length);
    }
}
