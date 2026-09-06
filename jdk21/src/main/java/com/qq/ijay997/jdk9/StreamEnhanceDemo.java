package com.qq.ijay997.jdk9;

import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * JDK 9 —— Stream 新增 API Demo。
 *
 * <p>新增 {@code ofNullable / takeWhile / dropWhile / iterate(有限)} 等，
 * 让流的截断与空值处理更简洁。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk9.StreamEnhanceDemo</p>
 *
 * @version JDK 9+
 */
public class StreamEnhanceDemo {

    public static void main(String[] args) {
        // ofNullable：元素可能为 null 时避免抛 NPE，null 得到空流
        long withNull = Stream.ofNullable(null).count();
        System.out.println("ofNullable(null) 元素数 = " + withNull);

        // takeWhile：从头截取满足条件的元素，遇到第一个不满足即停止
        List<Integer> taken = Stream.of(1, 2, 3, 1, 4)
                .takeWhile(n -> n < 3)
                .toList();
        System.out.println("takeWhile(<3)   = " + taken);

        // dropWhile：从头丢弃满足条件的元素，遇到第一个不满足即保留余下
        List<Integer> dropped = Stream.of(1, 2, 3, 1, 4)
                .dropWhile(n -> n < 3)
                .toList();
        System.out.println("dropWhile(<3)   = " + dropped);

        // iterate：三参版本，可生成有限序列（JDK 8 的两参版本是无限流）
        List<Integer> evens = IntStream.iterate(0, n -> n < 10, n -> n + 2)
                .boxed()
                .toList();
        System.out.println("iterate(有限)   = " + evens);

        // containsAny / count 等补充：stream 元素集合判断（JDK 9）
        var words = List.of("apple", "banana");
        boolean anyMatch = words.stream().anyMatch(w -> w.startsWith("a"));
        System.out.println("anyMatch example  = " + anyMatch);
    }
}
