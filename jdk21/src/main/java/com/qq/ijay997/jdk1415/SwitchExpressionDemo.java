package com.qq.ijay997.jdk1415;

/**
 * JDK 14 —— switch 表达式 Demo。
 *
 * <p>传统 switch 是「语句」；JDK 12（预览）/14（正式）引入可作为「表达式」
 * 的 switch，支持箭头语法与 {@code yield} 返回值，且不再需要到处写 break。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk1415.SwitchExpressionDemo</p>
 *
 * @version JDK 14+
 */
public class SwitchExpressionDemo {

    enum Day { MON, TUE, WED, THU, FRI, SAT, SUN }

    public static void main(String[] args) {
        Day day = Day.SAT;

        // 1) JDK 8 旧写法：容易漏写 break
        String oldResult;
        switch (day) {
            case MON:
            case TUE:
            case WED:
            case THU:
            case FRI:
                oldResult = "工作日";
                break;
            case SAT:
            case SUN:
                oldResult = "休息日";
                break;
            default:
                oldResult = "未知";
        }
        System.out.println("JDK8 写法: " + day + " -> " + oldResult);

        // 2) JDK 14 switch 表达式（箭头，多标签，天然无穿透）
        String newResult = switch (day) {
            case MON, TUE, WED, THU, FRI -> "工作日";
            case SAT, SUN -> "休息日";
        };
        System.out.println("JDK14 表达式: " + day + " -> " + newResult);

        // 3) 用 yield 在块内返回复杂值
        String judged = switch (day) {
            default -> {
                if (day.name().startsWith("S")) {
                    yield "周末";
                }
                yield "平日";
            }
        };
        System.out.println("yield 返回值: " + day + " -> " + judged);
    }
}
