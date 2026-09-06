package com.qq.ijay997.jdk9;

import java.util.Optional;
import java.util.stream.Stream;

/**
 * JDK 9 —— Optional 新增 API Demo。
 *
 * <p>新增 {@code ifPresentOrElse / or / stream} 等方法，
 * 让 Optional 处理更流畅，减少嵌套 if 判断。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk9.OptionalEnhanceDemo</p>
 *
 * @version JDK 9+
 */
public class OptionalEnhanceDemo {

    public static void main(String[] args) {
        // ifPresentOrElse：有值执行一个动作，无值执行另一个动作
        Optional.of("Hello").ifPresentOrElse(v -> System.out.println("有值: v=" + v),
                                              () -> System.out.println("无值"));
        Optional.<String>empty().ifPresentOrElse(v -> System.out.println("有值"),
                                                 () -> System.out.println("无值分支被触发"));

        // or：Optional 为空时，回退到另一个 Optional（JDK 9 新增）
        Optional<String> result = Optional.<String>empty()
                .or(() -> Optional.of("fallback"));
        System.out.println("or() 回退结果 = " + result.orElse("无"));

        // stream：把 Optional 转成 0 或 1 个元素的 Stream（JDK 9 新增）
        Stream<String> s1 = Optional.of("x").stream();
        Stream<String> s2 = Optional.<String>empty().stream();
        System.out.println("stream() 有值流元素数 = " + s1.count());
        System.out.println("stream() 空流元素数   = " + s2.count());

        // orElseThrow()：无参版本，抛 NoSuchElementException（JDK 10 收紧语义）
        try {
            Optional.<Integer>empty().orElseThrow();
        } catch (Exception e) {
            System.out.println("orElseThrow() 空值抛出: " + e.getClass().getSimpleName());
        }
    }
}
