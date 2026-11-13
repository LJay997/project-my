package com.qq.ijay997.jdk21.iobound;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * IO 密集型业务 DEMO —— 用 IDEA Profiler 找出线程池瓶颈并优化。
 *
 * <h3>业务场景</h3>
 * <p>订单详情聚合：每个订单要依次查 5 个下游（用户、商品、库存、优惠券、物流）。
 * 单次调用约 25ms IO + 1ms CPU，属于典型的 <b>IO 密集型</b>负载。</p>
 *
 * <h3>运行方式</h3>
 * <pre>
 * cd jdk21
 * mvn compile -q
 * java -cp target/classes com.qq.ijay997.jdk21.iobound.IoBoundThreadPoolDemo
 * </pre>
 *
 * <h3>用 IDEA Profiler 观察什么</h3>
 * <ol>
 *   <li><b>CPU 采样</b>：固定池很小时 CPU 占用也很低 → 说明瓶颈不是算力，而是线程数不足；</li>
 *   <li><b>Wall-clock 采样</b>：火焰图里 {@code DownstreamClient.io} 占绝对大头 → 阻塞型调用；</li>
 *   <li><b>线程数曲线</b>：固定池是一条水平线（受上限约束），虚拟线程按需增长但平台线程很少。</li>
 * </ol>
 *
 * <h3>线程池大小公式（Brian Goetz）</h3>
 * <pre>
 * N_threads = N_cpu × U_cpu × (1 + W / C)
 *
 * 本 demo：N_cpu ≈ 8，U_cpu ≈ 0.8
 *          W ≈ 26ms（平均 IO），C ≈ 1ms（CPU）
 *          N ≈ 8 × 0.8 × (1 + 26) ≈ 172
 * </pre>
 * <p>结论：IO 密集型任务的线程数应远大于 CPU 核数，"固定池 = CPU 核数"是常见误区。</p>
 *
 * @version JDK 21
 */
public class IoBoundThreadPoolDemo {

    /** 固定池线程命名序号 */
    private static final java.util.concurrent.atomic.AtomicInteger THREAD_SEQ =
            new java.util.concurrent.atomic.AtomicInteger();

    /** 订单数量：每个订单 5 次下游调用 */
    private static final int ORDER_COUNT = 60;

    /** 单个订单顺序调用 5 个下游的总 IO 时间 */
    private static final long ORDER_TOTAL_IO_MS = DownstreamClient.USER_IO_MS
            + DownstreamClient.PRODUCT_IO_MS
            + DownstreamClient.STOCK_IO_MS
            + DownstreamClient.COUPON_IO_MS
            + DownstreamClient.LOGISTICS_IO_MS;

    public static void main(String[] args) throws Exception {
        banner();
        printFormula();
        printWorkload();

        List<Result> results = new ArrayList<>();

        results.add(runSerial());
        results.add(runFixedPool("② 固定池 size=" + cpuCores() + "（CPU 核数）", cpuCores()));
        results.add(runFixedPool("③ 固定池 size=32", 32));
        results.add(runFixedPool("④ 固定池 size=200", 200));
        results.add(runVirtualThreads());

        printComparison(results);
        printProfilerGuide();
    }

    // ── 五种策略 ───────────────────────────────────────────────────

    /** ① 串行基线：不并发，一个一个查 */
    static Result runSerial() throws Exception {
        List<OrderAggregate> aggregates = new ArrayList<>(ORDER_COUNT);
        try (Sampler sampler = new Sampler()) {
            long t0 = System.nanoTime();
            long cpu0 = processCpuNanos();
            for (long id = 1; id <= ORDER_COUNT; id++) {
                aggregates.add(aggregateSerial(id));
            }
            long costMs = elapsedMs(t0);
            long cpuMs = (processCpuNanos() - cpu0) / 1_000_000L;
            return new Result("① 串行（基线）", costMs, sampler.peakThreads(), cpuMs, ORDER_COUNT);
        }
    }

    /** ②③④ 固定线程池，只改 size */
    static Result runFixedPool(String label, int poolSize) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(poolSize, r -> {
            Thread t = new Thread(r, "fixed-pool-" + THREAD_SEQ.incrementAndGet());
            t.setDaemon(true);
            return t;
        });
        try (Sampler sampler = new Sampler()) {
            long t0 = System.nanoTime();
            long cpu0 = processCpuNanos();
            runOrders(pool);
            long costMs = elapsedMs(t0);
            long cpuMs = (processCpuNanos() - cpu0) / 1_000_000L;
            return new Result(label, costMs, sampler.peakThreads(), cpuMs, ORDER_COUNT);
        } finally {
            pool.shutdownNow();
            pool.awaitTermination(1, TimeUnit.SECONDS);
        }
    }

    /** ⑤ 虚拟线程：每个任务一个虚拟线程，阻塞时自动让出载体线程 */
    static Result runVirtualThreads() throws Exception {
        try (ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();
             Sampler sampler = new Sampler()) {
            long t0 = System.nanoTime();
            long cpu0 = processCpuNanos();
            runOrders(pool);
            long costMs = elapsedMs(t0);
            long cpuMs = (processCpuNanos() - cpu0) / 1_000_000L;
            return new Result("⑤ 虚拟线程（JDK 21）", costMs, sampler.peakThreads(), cpuMs, ORDER_COUNT);
        }
    }

    // ── 业务逻辑 ───────────────────────────────────────────────────

    /** 串行聚合：5 次调用顺序执行，约 130ms */
    static OrderAggregate aggregateSerial(long orderId) {
        String user = DownstreamClient.queryUser(orderId);
        String product = DownstreamClient.queryProduct(orderId);
        int stock = DownstreamClient.queryStock(orderId);
        double coupon = DownstreamClient.queryCoupon(orderId);
        String logistics = DownstreamClient.queryLogistics(orderId);
        return new OrderAggregate(orderId, user, product, stock, coupon, logistics);
    }

    /**
     * 并发聚合：5 次调用同时发起，理论耗时 = 最慢的那个（40ms）。
     *
     * <p><b>关键实现约束</b>：必须由<b>调用线程</b>直接提交这 5 个叶子任务，
     * 不能写成"先把订单级任务提交到池，再在池线程里提交子任务"。
     * 后者在固定大小池里会引发 <b>线程饥饿死锁</b>：
     * 池被外层订单任务占满，每个都在等子任务，而子任务永远排不到线程。
     * 这是线程池使用中最常见的死锁形态之一。</p>
     */
    static CompletableFuture<OrderAggregate> aggregateAsync(long orderId, ExecutorService pool) {
        var user = CompletableFuture.supplyAsync(() -> DownstreamClient.queryUser(orderId), pool);
        var product = CompletableFuture.supplyAsync(() -> DownstreamClient.queryProduct(orderId), pool);
        var stock = CompletableFuture.supplyAsync(() -> DownstreamClient.queryStock(orderId), pool);
        var coupon = CompletableFuture.supplyAsync(() -> DownstreamClient.queryCoupon(orderId), pool);
        var logistics = CompletableFuture.supplyAsync(() -> DownstreamClient.queryLogistics(orderId), pool);

        return CompletableFuture.allOf(user, product, stock, coupon, logistics)
                .thenApply(ignored -> new OrderAggregate(orderId,
                        user.join(), product.join(), stock.join(), coupon.join(), logistics.join()));
    }

    /** 由主线程提交全部订单，池中只有"叶子任务"，不存在任何嵌套等待 */
    static void runOrders(ExecutorService pool) {
        List<CompletableFuture<OrderAggregate>> futures = new ArrayList<>(ORDER_COUNT);
        for (long id = 1; id <= ORDER_COUNT; id++) {
            futures.add(aggregateAsync(id, pool));
        }
        CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new)).join();
    }

    // ── 指标采集 ───────────────────────────────────────────────────

    /** 后台采样线程数与 CPU，用于对比不同策略的资源消耗 */
    static final class Sampler implements AutoCloseable {
        private final ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
        private final Thread thread;
        private volatile boolean running = true;
        private volatile int peakThreads;

        Sampler() {
            thread = new Thread(() -> {
                while (running) {
                    peakThreads = Math.max(peakThreads, threadMXBean.getThreadCount());
                    try {
                        Thread.sleep(5L);
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }, "metrics-sampler");
            thread.setDaemon(true);
            thread.start();
        }

        int peakThreads() { return peakThreads; }

        @Override
        public void close() {
            running = false;
            thread.interrupt();
        }
    }

    private static long processCpuNanos() {
        var os = ManagementFactory.getOperatingSystemMXBean();
        if (os instanceof com.sun.management.OperatingSystemMXBean sunOs) {
            return sunOs.getProcessCpuTime();
        }
        return 0L;
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static int cpuCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    // ── 输出 ───────────────────────────────────────────────────────

    private static void banner() {
        System.out.println("╔══════════════════════════════════════════════════════════════════════════╗");
        System.out.println("║  IO 密集型业务 DEMO —— 订单详情聚合 × 线程池策略对比                      ║");
        System.out.println("╚══════════════════════════════════════════════════════════════════════════╝");
    }

    private static void printFormula() {
        System.out.println();
        System.out.println("【线程池大小公式】N_threads = N_cpu × U_cpu × (1 + W / C)");
        System.out.printf("  N_cpu = %d, W ≈ %.0fms, C = %dms%n",
                cpuCores(), ORDER_TOTAL_IO_MS / 5.0, DownstreamClient.CPU_PER_CALL_MS);
        System.out.printf("  理论值 ≈ %d × 0.8 × (1 + %.0f) ≈ %d%n",
                cpuCores(), (ORDER_TOTAL_IO_MS / 5.0) / DownstreamClient.CPU_PER_CALL_MS,
                (int) (cpuCores() * 0.8 * (1 + (ORDER_TOTAL_IO_MS / 5.0) / DownstreamClient.CPU_PER_CALL_MS)));
        System.out.println("  → IO 密集型任务的线程数应远大于 CPU 核数");
    }

    private static void printWorkload() {
        System.out.println();
        System.out.println("【负载】");
        System.out.printf("  订单数 = %d，每单 5 次下游调用，单次 ≈ 25ms IO + %dms CPU%n",
                ORDER_COUNT, DownstreamClient.CPU_PER_CALL_MS);
        System.out.printf("  顺序聚合单订单耗时 ≈ %dms；并发聚合理论耗时 ≈ 40ms（最慢的物流）%n",
                ORDER_TOTAL_IO_MS);
        System.out.printf("  总任务数 = %d%n", ORDER_COUNT * 5);
    }

    private static void printComparison(List<Result> results) {
        System.out.println();
        System.out.println("【实测结果】");
        System.out.println(border('┌', '┬', '┐'));
        System.out.println(row("策略", "总耗时(ms)", "峰值线程数", "CPU(ms)", "吞吐(单/秒)"));
        System.out.println(border('├', '┼', '┤'));
        for (Result r : results) {
            System.out.println(row(r.label,
                    String.valueOf(r.costMs),
                    String.valueOf(r.peakThreads),
                    String.valueOf(r.cpuMs),
                    String.format("%.1f", r.throughput())));
        }
        System.out.println(border('└', '┴', '┘'));
        System.out.println("注：峰值线程数为 JVM 平台线程总数（含 JVM 内部线程），虚拟线程不计入该统计。");

        Result baseline = results.get(0);
        System.out.println();
        System.out.println("【相对串行基线的加速比】");
        for (Result r : results) {
            System.out.printf("  %s ×%.1f%n", pad(r.label, 34),
                    (double) baseline.costMs / Math.max(1, r.costMs));
        }
    }

    // ── 表格对齐（按显示宽度计算，中文占 2 格） ──────────────────────

    private static final int[] COL_WIDTH = {34, 10, 10, 10, 12};

    private static String row(String... cells) {
        StringBuilder sb = new StringBuilder("│");
        for (int i = 0; i < cells.length; i++) {
            sb.append(' ').append(pad(cells[i], COL_WIDTH[i])).append(' ').append('│');
        }
        return sb.toString();
    }

    private static String border(char left, char mid, char right) {
        StringBuilder sb = new StringBuilder().append(left);
        for (int i = 0; i < COL_WIDTH.length; i++) {
            sb.append("─".repeat(COL_WIDTH[i] + 2));
            sb.append(i == COL_WIDTH.length - 1 ? right : mid);
        }
        return sb.toString();
    }

    /** 按终端显示宽度右补空格：ASCII 占 1 格，CJK / 带圈数字 / 全角符号占 2 格 */
    private static String pad(String s, int width) {
        int display = 0;
        for (int i = 0; i < s.length(); i++) {
            display += isWide(s.charAt(i)) ? 2 : 1;
        }
        return s + " ".repeat(Math.max(0, width - display));
    }

    private static boolean isWide(char c) {
        return (c >= 0x1100 && c <= 0x115F)      // 韩文字母
                || (c >= 0x2460 && c <= 0x24FF)  // 带圈数字 ①-⑳
                || (c >= 0x2E80 && c <= 0xA4CF)  // CJK 及扩展
                || (c >= 0xAC00 && c <= 0xD7A3)  // 韩文音节
                || (c >= 0xF900 && c <= 0xFAFF)  // CJK 兼容
                || (c >= 0xFF00 && c <= 0xFF60)  // 全角 ASCII
                || (c >= 0xFFE0 && c <= 0xFFE6);
    }

    private static void printProfilerGuide() {
        System.out.println();
        System.out.println("【用 IDEA Profiler 复现与解读】");
        System.out.println("  1) Run → Run with Profiler → CPU 采样：CPU 占用很低");
        System.out.println("     → 瓶颈不是算力，加机器没用，要加并发度");
        System.out.println("  2) 换成 Wall-clock 采样：火焰图顶部是 DownstreamClient.io");
        System.out.println("     → 阻塞占比 ≈ 96%，典型 IO 密集型");
        System.out.println("  3) 看线程数曲线：size=CPU核数 时任务排队，size=200 时线程创建开销上升");
        System.out.println("  4) 虚拟线程：平台线程数 ≈ CPU 核数，吞吐却接近 size=200");
        System.out.println();
        System.out.println("【优化顺序（成本由低到高）】");
        System.out.println("  ① 先用 Profiler 区分 IO 阻塞与 CPU 瓶颈（CPU vs Wall 对比）");
        System.out.println("  ② 合并下游调用：5 次串行 → 并发（本 demo 已做）");
        System.out.println("  ③ 按公式放大线程池，但不要超过下游承载能力（连接池 / 限流）");
        System.out.println("  ④ 加缓存 / 批量接口，从根上减少 IO 次数");
        System.out.println("  ⑤ 终极方案：虚拟线程，免调参且天然适配 IO 密集");
        System.out.println();
        System.out.println("【常见误区】");
        System.out.println("  · 固定池大小 = CPU 核数 —— 只适合 CPU 密集型");
        System.out.println("  · 池内再向同一个池提交子任务 —— 线程饥饿死锁（本 demo 已规避）");
        System.out.println("  · 无限放大池 —— 下游连接池打满后，线程都在等连接，反而更慢");
    }

    // ── 数据结构 ───────────────────────────────────────────────────

    /** 聚合结果 */
    public record OrderAggregate(
            long orderId,
            String user,
            String product,
            int stock,
            double coupon,
            String logistics
    ) {}

    /** 单次策略的测量结果 */
    record Result(String label, long costMs, int peakThreads, long cpuMs, int orders) {
        double throughput() {
            return costMs == 0 ? 0 : orders * 1000.0 / costMs;
        }
    }
}
