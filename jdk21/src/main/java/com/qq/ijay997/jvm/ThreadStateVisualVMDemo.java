package com.qq.ijay997.jvm;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

/**
 * {@link Thread.State} 六种状态演示 —— <b>每个状态一个独立的测试方法</b>，配合 VisualVM / jstack 逐个观察。
 *
 * <p>方法之间互不影响：每个方法自带线程、自带锁、自带超时与观察窗口，
 * 可以单独运行某一个方法来学习某个状态，不会污染其他场景。</p>
 *
 * <h3>运行方式</h3>
 * <pre>
 * cd jdk21
 * mvn compile -q
 * java -cp target/classes com.qq.ijay997.jvm.ThreadStateVisualVMDemo
 * </pre>
 *
 * <h3>VisualVM 观察步骤</h3>
 * <ol>
 *   <li>程序启动后，让 VisualVM attach 到该进程。</li>
 *   <li>打开 <b>Threads</b> 页签，按 <code>State</code> 列排序，或直接按线程名查找。</li>
 *   <li>选中线程，下方 <b>State / Stack Trace</b> 区即为最终判据。</li>
 * </ol>
 *
 * <h3>各方法对应关系</h3>
 * <table border="1" cellpadding="4">
 *   <tr><th>方法</th><th>状态</th><th>线程名</th><th>VisualVM 预期栈帧</th></tr>
 *   <tr><td>{@link #demoNew()}</td><td>NEW</td><td>Worker-NEW</td><td>列表里看不到；getState()=NEW</td></tr>
 *   <tr><td>{@link #demoRunnable()}</td><td>RUNNABLE</td><td>Worker-RUNNABLE</td><td>State=RUNNABLE；栈顶在 heavyLoop；CPU 有消耗</td></tr>
 *   <tr><td>{@link #demoTimedWaiting()}</td><td>TIMED_WAITING</td><td>Worker-TIMED</td><td>状态行 <b>TIMED_WAITING (sleeping)</b>；栈里有 Thread.sleep</td></tr>
 *   <tr><td>{@link #demoBlocked()}</td><td>BLOCKED</td><td>Worker-BLOCKED</td><td>首行 <b>waiting for monitor entry</b>；关键行 <b>- waiting to lock &lt;0x...&gt;</b></td></tr>
 *   <tr><td>{@link #demoWaiting()}</td><td>WAITING</td><td>Worker-WAITING</td><td>首行 <b>in Object.wait() [...]</b>；关键行 <b>- waiting on &lt;0x...&gt;</b></td></tr>
 *   <tr><td>{@link #demoParked()}</td><td>WAITING (park)</td><td>Worker-PARKED</td><td>状态行 <b>WAITING (parking)</b>；关键行 <b>- parking to wait for &lt;0x...&gt;</b></td></tr>
 *   <tr><td>{@link #demoTerminated()}</td><td>TERMINATED</td><td>Worker-TERMINATED</td><td>线程从列表消失；getState()=TERMINATED</td></tr>
 * </table>
 *
 * <h4>BLOCKED vs WAITING 速记</h4>
 * <pre>
 * BLOCKED: - waiting to lock &lt;0x...&gt;   抢不到锁，在 EntryList 排队
 * WAITING: - waiting on      &lt;0x...&gt;   已释放锁，在 WaitSet 等 notify
 * </pre>
 *
 * <h4>断点调试说明</h4>
 * <p>所有跨线程等待都带超时（{@code awaitState} / {@code join} / {@code CountDownLatch.await}），
 * 所有 Worker 均为 daemon 线程，因此断点停靠时主线程不会无限自旋、JVM 也不会因残留线程无法退出。</p>
 *
 * @see Thread.State
 * @version JDK 21
 */
public class ThreadStateVisualVMDemo {

    /** 等待线程进入目标状态的上限；足够容忍调试器暂停数分钟 */
    private static final long AWAIT_TIMEOUT_MS = 60_000L;

    /** join 兜底上限 */
    private static final long JOIN_TIMEOUT_MS = 5_000L;

    /** 给 VisualVM 观察的窗口时长 */
    private static final long WATCH_WINDOW_MS = 5_000L;

    /** 防止 JIT 把 heavyLoop 的空循环整体消除 */
    private static volatile long heavySum;

    public static void main(String[] args) throws Exception {
//        banner();
//        demoNew();
//        demoRunnable();
//        demoTimedWaiting();
//        demoBlocked();
        demoWaiting();
//        demoParked();
//        demoTerminated();
//        System.out.println();
//        System.out.println("╔══════════════════════════════════════════════════╗");
//        System.out.println("║  全部状态演示结束。                                ║");
//        System.out.println("║  建议单独跑某个方法，在 VisualVM Threads 页签      ║");
//        System.out.println("║  选中对应线程，观察完整线程栈。                    ║");
//        System.out.println("╚══════════════════════════════════════════════════╝");
    }

    // ── ① NEW ──────────────────────────────────────────────────────

    /**
     * NEW：线程对象已创建，但还没有调用 {@code start()}。
     *
     * <p>VisualVM 里根本看不到这个线程，只有 {@code getState()} 能读到 NEW。</p>
     */
    static void demoNew() {
        Thread t = new Thread(() -> log("这行不会被执行"), "Worker-NEW");
        printState("① NEW — 创建但未 start", t,
                "VisualVM 列表里看不到；getState() = NEW");
    }

    // ── ② RUNNABLE ─────────────────────────────────────────────────

    /**
     * RUNNABLE：正在执行（或已就绪等待 CPU 时间片）。
     *
     * <p>打断点后暂停在 {@code heavyLoop} 内部，VisualVM 里能看到栈顶是循环。</p>
     */
    static void demoRunnable() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        Thread t = new Thread(() -> {
            started.countDown();
            heavyLoop(3_000L);
        }, "Worker-RUNNABLE");
        t.setDaemon(true);
        t.start();

        // 必须带超时：断点若停在 countDown() 之前，不能无限自旋
        started.await(JOIN_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        Thread.sleep(200L);

        printState("② RUNNABLE — 运行中", t,
                "State = RUNNABLE；栈顶是 heavyLoop 的循环；CPU 有消耗");
        t.join(JOIN_TIMEOUT_MS);
    }

    // ── ③ TIMED_WAITING ────────────────────────────────────────────

    /**
     * TIMED_WAITING：{@code Thread.sleep(ms)} / {@code wait(ms)} / {@code join(ms)} 等带超时的等待。
     *
     * <p>断点建议打在 {@link Thread#sleep} 那一行之前，命中后查看状态行为
     * {@code TIMED_WAITING (sleeping)}。</p>
     */
    static void demoTimedWaiting() throws Exception {
        Thread t = new Thread(() -> {
            try {
                Thread.sleep(8_000L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Worker-TIMED");
        t.setDaemon(true);
        t.start();

        awaitState(t, Thread.State.TIMED_WAITING);
        printState("③ TIMED_WAITING — sleep(8000)", t,
                "状态行 'TIMED_WAITING (sleeping)'；栈里有 Thread.sleep");

        pauseForVisualVM("TIMED_WAITING");
        t.join(JOIN_TIMEOUT_MS);
    }

    // ── ④ BLOCKED ──────────────────────────────────────────────────

    /**
     * BLOCKED：抢 {@code synchronized} 锁失败，在 EntryList 里排队。
     *
     * <p>关键：本方法先由主线程持有锁，Worker 一启动就卡在 monitor entry。
     * 断点打在主线程持锁期间，即可在 VisualVM 看到
     * {@code waiting for monitor entry} 与 {@code - waiting to lock <0x...>}。</p>
     */
    static void demoBlocked() throws Exception {
        final Object lock = new Object();
        Thread t = new Thread(() -> {
            synchronized (lock) {
                log("抢到锁：BLOCKED → RUNNABLE");
            }
        }, "Worker-BLOCKED");
        t.setDaemon(true);

        synchronized (lock) {
            t.start();
            awaitState(t, Thread.State.BLOCKED);
            printState("④ BLOCKED — 抢锁失败", t,
                    "首行 'waiting for monitor entry'；关键行 '- waiting to lock <0x...>'");

            // 主线程仍持锁，留出观察窗口
            pauseForVisualVM("BLOCKED");
        }
        // 出 synchronized 后锁释放，Worker 抢到并跑完
        t.join(JOIN_TIMEOUT_MS);
    }

    // ── ⑤ WAITING (Object.wait) ────────────────────────────────────

    /**
     * WAITING：{@code Object.wait()} 无超时等待，线程释放锁并进入 WaitSet。
     *
     * <p>与 BLOCKED 的差别：BLOCKED 是"还没拿到锁"，WAITING 是"拿到过锁、主动释放并等通知"。
     * 断点打在主线程 {@code notify()} 之前，可在 VisualVM 看到
     * {@code in Object.wait()} 与 {@code - waiting on <0x...>}。</p>
     */
    static void demoWaiting() throws Exception {
        final Object lock = new Object();
        Thread t = new Thread(() -> {
            try {
                synchronized (lock) {
                    log("持有锁，即将调用 wait() 释放并进入 WaitSet");
                    System.out.println(1);
                    lock.wait();
                    System.out.println(2);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }, "Worker-WAITING");
//        t.setDaemon(true);
        t.start();
        boolean ready = awaitState(t, Thread.State.WAITING);
        printState("⑤ WAITING — Object.wait()", t,
                "首行 'in Object.wait() [...]'；关键行 '- waiting on <0x...>'");

        pauseForVisualVM("WAITING");

        // 只有确实在等待时才发信号，避免 notify 打在空 WaitSet 上
        if (ready) {
            synchronized (lock) {
                lock.notify();
            }
        }
        t.join();
    }

    // ── ⑤' WAITING (LockSupport.park) ──────────────────────────────

    /**
     * WAITING（parking）：{@link LockSupport#park(Object)}，AQS 等并发工具内部的常见形态。
     *
     * <p>VisualVM 中状态行为 {@code WAITING (parking)}，关键行
     * {@code - parking to wait for <0x...>}，栈里能看到 {@code Unsafe.park}。</p>
     */
    static void demoParked() throws Exception {
        final Object token = new Object();
        Thread t = new Thread(() -> LockSupport.park(token), "Worker-PARKED");
        t.setDaemon(true);
        t.start();

        boolean ready = awaitState(t, Thread.State.WAITING);
        printState("⑤' WAITING — LockSupport.park", t,
                "状态行 'WAITING (parking)'；关键行 '- parking to wait for <0x...>'；栈里有 Unsafe.park");

        pauseForVisualVM("WAITING(parking)");

        if (ready) {
            LockSupport.unpark(t);
        }
        t.join(JOIN_TIMEOUT_MS);
    }

    // ── ⑥ TERMINATED ───────────────────────────────────────────────

    /**
     * TERMINATED：线程的 {@code run()} 已执行完毕。
     *
     * <p>短生命周期线程结束后会从 VisualVM 列表里消失，用 {@code getState()} 才能确认。</p>
     */
    static void demoTerminated() throws Exception {
        Thread t = new Thread(() -> { /* 立即结束 */ }, "Worker-TERMINATED");
        t.setDaemon(true);
        t.start();
        t.join(JOIN_TIMEOUT_MS);

        printState("⑥ TERMINATED — 执行完毕", t,
                "线程可能已从 VisualVM 列表消失；getState() = TERMINATED");
    }

    // ── 辅助方法 ───────────────────────────────────────────────────

    private static void banner() {
        System.out.println("╔══════════════════════════════════════════════════╗");
        System.out.println("║  Thread.State 六种状态 × VisualVM 断点演示        ║");
        System.out.println("║  每个状态一个独立测试方法                          ║");
        System.out.println("╚══════════════════════════════════════════════════╝");
    }

    /** 占位循环：让 RUNNABLE 线程真实运行指定时长 */
    private static void heavyLoop(long durationMs) {
        long deadline = System.nanoTime() + durationMs * 1_000_000L;
        long sum = 0;
        long i = 0;
        while (System.nanoTime() < deadline) {
            sum += i++;
            if ((i & 0x3FFF) == 0) {
                heavySum = sum;
            }
        }
        heavySum = sum;
        log("heavyLoop 结束，循环次数 ~" + i);
    }

    /**
     * 自旋等待线程进入目标状态，最多 {@value #AWAIT_TIMEOUT_MS} ms。
     *
     * <p>超时只返回 false、不抛异常：调试器暂停时系统时钟仍在走，
     * 恢复后可能立即"超时"，此时应由调用方跳过信号而不是让主线程崩溃。</p>
     *
     * @return true 表示已进入期望状态；false 表示超时
     */
    private static boolean awaitState(Thread t, Thread.State expected) throws InterruptedException {
        long deadline = System.nanoTime() + AWAIT_TIMEOUT_MS * 1_000_000L;
        while (t.getState() != expected && System.nanoTime() < deadline) {
            Thread.sleep(2L);
        }
        boolean ok = t.getState() == expected;
        if (!ok) {
            log("⚠️ " + t.getName() + " 未在 " + AWAIT_TIMEOUT_MS
                    + "ms 内进入 " + expected + "（当前 " + t.getState()
                    + "，可能停在断点），跳过后续信号");
        }
        return ok;
    }

    /** 留出固定的观察窗口，供 VisualVM / jstack 抓栈 */
    private static void pauseForVisualVM(String phase) throws InterruptedException {
        System.out.println("   ── 观察窗口 5 秒 [" + phase + "]，请到 VisualVM Threads 页签查看 ──");
        Thread.sleep(WATCH_WINDOW_MS);
    }

    /** 统一的状态打印 */
    private static void printState(String label, Thread t, String visualVMTip) {
        System.out.println();
        System.out.println(label);
        System.out.printf("   线程名: %-20s  state() = %s%n", t.getName(), t.getState());
        System.out.println("   VisualVM 提示: " + visualVMTip);
    }

    private static void log(String msg) {
        System.out.println("   · " + msg);
    }
}
