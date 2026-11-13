package com.qq.ijay997.jdk21.iobound;

/**
 * 模拟下游调用（RPC / 数据库 / HTTP）。
 *
 * <p>每次调用 = 一段固定 IO 等待 + 少量 CPU 计算（模拟反序列化）。
 * 这是典型的 IO 密集型负载：线程大部分时间都在等，CPU 并不忙。</p>
 *
 * <p>为什么用 {@link Thread#sleep} 模拟 IO：</p>
 * <ul>
 *   <li>不依赖外网，结果可复现；</li>
 *   <li>在虚拟线程下 {@code sleep} 会主动让出载体线程，能真实反映 JDK 21 的收益；</li>
 *   <li>Profiler 的 Wall-clock 视图会显示成 {@code Thread.sleep}，便于观察阻塞占比。</li>
 * </ul>
 */
public final class DownstreamClient {

    /** 各下游的平均延迟（毫秒），用于计算 W/C 比 */
    public static final long USER_IO_MS      = 30;
    public static final long PRODUCT_IO_MS   = 25;
    public static final long STOCK_IO_MS     = 20;
    public static final long COUPON_IO_MS    = 15;
    public static final long LOGISTICS_IO_MS = 40;

    /** 单次调用的 CPU 计算耗时（模拟 JSON 反序列化 / 字段映射） */
    public static final long CPU_PER_CALL_MS = 1;

    private DownstreamClient() {}

    public static String queryUser(long orderId) {
        io(USER_IO_MS);
        return "user-" + orderId;
    }

    public static String queryProduct(long orderId) {
        io(PRODUCT_IO_MS);
        return "product-" + orderId;
    }

    public static int queryStock(long orderId) {
        io(STOCK_IO_MS);
        return 100;
    }

    public static double queryCoupon(long orderId) {
        io(COUPON_IO_MS);
        return 9.9d;
    }

    public static String queryLogistics(long orderId) {
        io(LOGISTICS_IO_MS);
        return "SF" + (1_000_000 + orderId);
    }

    /** IO 等待 + 少量 CPU */
    private static void io(long ioMillis) {
        try {
            Thread.sleep(ioMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return;
        }
        busyCpu(CPU_PER_CALL_MS);
    }

    /** 纯 CPU 忙碌，用于制造可观测的计算耗时；结果写入 volatile 防 JIT 消除 */
    private static void busyCpu(long millis) {
        long deadline = System.nanoTime() + millis * 1_000_000L;
        double acc = 0;
        long i = 0;
        while (System.nanoTime() < deadline) {
            acc += Math.sqrt(i++);
        }
        SINK = acc;
    }

    private static volatile double SINK;
}
