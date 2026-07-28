package com.qq.ijay997.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@RestController
@RequestMapping("/cpu")
public class CpuTestController {

    private final AtomicBoolean running = new AtomicBoolean(false);
    private final List<Thread> activeThreads = new ArrayList<>();

    private int getCores() {
        return Runtime.getRuntime().availableProcessors();
    }

    @GetMapping("/loop")
    public String deadLoop(
            @RequestParam(defaultValue = "0") int threads,
            @RequestParam(defaultValue = "0") int duration) throws Exception {
        
        int threadCount = threads > 0 ? threads : getCores();
        int durSec = duration;
        
        running.set(true);
        List<Thread> threads_list = new ArrayList<>();
        AtomicInteger counter = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> {
                long x = 0;
                long start = System.currentTimeMillis();
                while (running.get()) {
                    for (int j = 0; j < 100000; j++) {
                        x += Math.sqrt(j) * Math.sin(x);
                        x ^= (x << 13);
                        x ^= (x >>> 17);
                        x ^= (x << 5);
                    }
                    counter.incrementAndGet();
                    if (durSec > 0 && (System.currentTimeMillis() - start) > durSec * 1000L) {
                        break;
                    }
                }
            }, "cpu-burner-" + i);
            t.setDaemon(true);
            threads_list.add(t);
            t.start();
        }
        
        synchronized (activeThreads) {
            activeThreads.addAll(threads_list);
        }
        
        if (durSec > 0) {
            Thread.sleep(durSec * 1000L + 2000L);
            return String.format("完成！%d个CPU核心线程运行了%d秒，总循环次数: %,d",
                    threadCount, durSec, counter.get());
        }
        
        return String.format("已启动%d个CPU密集线程（持续运行中），调用 /cpu/stop 停止", threadCount);
    }

    @GetMapping("/busy")
    public String busyCalculation(
            @RequestParam(defaultValue = "0") int threads,
            @RequestParam(defaultValue = "30") int duration) throws Exception {
        
        int threadCount = threads > 0 ? threads : getCores();
        int durSec = duration;
        
        running.set(true);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger progress = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            Thread t = new Thread(() -> {
                long sum = 0;
                long start = System.currentTimeMillis();
                long perBatch = 5000000L;
                
                try {
                    while (running.get()) {
                        for (long j = 0; j < perBatch; j++) {
                            sum += Math.sqrt(j) * Math.sin(j) * Math.cos(j);
                            sum = (sum ^ (sum << 13)) & 0x7FFFFFFFFFFFFFFFL;
                            sum = (sum ^ (sum >>> 17)) & 0x7FFFFFFFFFFFFFFFL;
                        }
                        progress.incrementAndGet();
                        if (durSec > 0 && (System.currentTimeMillis() - start) > durSec * 1000L) {
                            break;
                        }
                    }
                } finally {
                    latch.countDown();
                }
            }, "busy-calc-" + i);
            t.setDaemon(true);
            t.start();
        }
        
        latch.await();
        
        return String.format("计算完成！%d线程×%.0f万次迭代，进度报告次数: %,d",
                threadCount, threadCount * 500.0, progress.get());
    }

    @GetMapping("/gc")
    public String triggerGc(
            @RequestParam(defaultValue = "500") int rounds,
            @RequestParam(defaultValue = "2000000") int objectCount) throws Exception {
        
        List<byte[]> memoryHold = new ArrayList<>();
        AtomicInteger round = new AtomicInteger(0);
        
        for (int r = 0; r < rounds; r++) {
            List<byte[]> batch = new ArrayList<>();
            for (int i = 0; i < objectCount / rounds; i++) {
                batch.add(new byte[1024]);
            }
            memoryHold.addAll(batch);
            round.incrementAndGet();
            
            if (r % 50 == 0) {
                long usedMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
                System.out.printf("[GC压力测试] 第%d轮, 已分配对象: %,d, 堆使用: %d MB%n",
                        r, memoryHold.size(), usedMB);
            }
            
            Thread.sleep(100);
        }
        
        long usedMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
        long maxMB = Runtime.getRuntime().maxMemory() / 1024 / 1024;
        return String.format("GC测试完成！总对象: %,d, 堆使用: %d MB / %d MB (%.1f%%)",
                memoryHold.size(), usedMB, maxMB, usedMB * 100.0 / maxMB);
    }

    @GetMapping("/lock")
    public String lockContention(
            @RequestParam(defaultValue = "0") int threads,
            @RequestParam(defaultValue = "15") int duration) throws Exception {
        
        int threadCount = threads > 0 ? threads : getCores() * 2;
        int durSec = duration;
        Object lock = new Object();
        
        running.set(true);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        AtomicInteger contendedCount = new AtomicInteger(0);
        
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                readyLatch.countDown();
                try {
                    while (running.get()) {
                        synchronized (lock) {
                            contendedCount.incrementAndGet();
                            long start = System.currentTimeMillis();
                            long x = 0;
                            while (System.currentTimeMillis() - start < 50) {
                                x += Math.sqrt(x + 1);
                                x ^= (x << 13);
                                x ^= (x >>> 17);
                            }
                        }
                        if (durSec > 0 && 
                            (System.currentTimeMillis() - (System.currentTimeMillis() - durSec * 1000L)) > durSec * 1000L) {
                            break;
                        }
                    }
                } finally {
                    doneLatch.countDown();
                }
            }, "lock-contender-" + i).start();
        }
        
        readyLatch.await();
        long startTime = System.currentTimeMillis();
        
        if (durSec > 0) {
            Thread.sleep(durSec * 1000L);
            running.set(false);
        } else {
            Thread.sleep(3000L);
            running.set(false);
        }
        
        doneLatch.await();
        long elapsed = (System.currentTimeMillis() - startTime) / 1000;
        
        return String.format("锁竞争测试完成！%d线程竞争%s%d秒，总竞争次数: %,d",
                threadCount, durSec > 0 ? "持续" : "",
                durSec > 0 ? durSec : elapsed, contendedCount.get());
    }

    /**
     * 死锁模拟接口
     * 场景：线程A持有锁lock1，等待获取lock2；线程B持有锁lock2，等待获取lock1
     * 当两组线程同时运行时，产生循环等待导致死锁
     * 
     * @param pairs  死锁线程对数（每对2个线程，默认3对=6个线程）
     * @param delay  获取两个锁之间的延迟毫秒数（越大越容易死锁，默认100ms）
     */
    @GetMapping("/deadlock")
    public String simulateDeadlock(
            @RequestParam(defaultValue = "3") int pairs,
            @RequestParam(defaultValue = "100") int delay) throws Exception {
        
        int pairCount = Math.max(1, pairs);
        int threadCount = pairCount * 2;
        
        running.set(true);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger blockedCount = new AtomicInteger(0);
        
        StringBuilder result = new StringBuilder();
        result.append(String.format("=== 死锁模拟启动 ===%n"));
        result.append(String.format("死锁对数: %d 对（共 %d 个线程）%n", pairCount, threadCount));
        result.append(String.format("锁间延迟: %d ms%n%n", delay));
        
        for (int pair = 0; pair < pairCount; pair++) {
            // 每对创建两个独立的锁对象
            final Object lockA = new Object();
            final Object lockB = new Object();
            final int currentPair = pair;
            
            // 线程1：先获取 lockA，再获取 lockB
            Thread t1 = new Thread(() -> {
                try {
                    latch.countDown();
                    synchronized (lockA) {
                        successCount.incrementAndGet();
                        Thread.sleep(delay);  // 故意延迟，让另一个线程获取lockB
                        synchronized (lockB) {
                            successCount.incrementAndGet();
                            // 到这里说明没发生死锁（概率较低）
                            long x = 0;
                            for (int i = 0; i < 10000; i++) {
                                x += Math.sqrt(i);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    blockedCount.incrementAndGet();
                }
            }, "deadlock-AB-" + pair);
            t1.setDaemon(true);
            
            // 线程2：先获取 lockB，再获取 lockA（顺序相反导致循环等待）
            Thread t2 = new Thread(() -> {
                try {
                    latch.countDown();
                    synchronized (lockB) {
                        successCount.incrementAndGet();
                        Thread.sleep(delay);  // 故意延迟，让另一个线程获取lockA
                        synchronized (lockA) {
                            successCount.incrementAndGet();
                            long x = 0;
                            for (int i = 0; i < 10000; i++) {
                                x += Math.sqrt(i);
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (Exception e) {
                    blockedCount.incrementAndGet();
                }
            }, "deadlock-BA-" + pair);
            t2.setDaemon(true);
            
            // 几乎同时启动两个线程，增加死锁概率
            t1.start();
            t2.start();
            
            synchronized (activeThreads) {
                activeThreads.add(t1);
                activeThreads.add(t2);
            }
            
            result.append(String.format("第[%d]对: 线程(AB:lockA→lockB) + 线程(BA:lockB→lockA) 已启动%n", pair));
        }
        
        // 等待所有线程就绪后，给足够时间让死锁发生
        Thread.sleep(3000L);
        
        // 检查死锁情况
        result.append(String.format("%n=== 死锁状态检测 ===%n"));
        result.append(String.format("已成功获取第一个锁的线程数: %d / %d%n", 
                successCount.get(), threadCount));
        
        // 如果 successCount 接近 pairCount*2 但还在运行，说明大概率死锁
        if (successCount.get() >= pairCount * 2 && successCount.get() < pairCount * 4) {
            result.append(String.format("⚠️  检测到死锁！%n"));
            result.append(String.format("   - 已持有锁的线程: %d%n", successCount.get()));
            result.append(String.format("   - 被阻塞的线程: %d%n", threadCount - (successCount.get() / 2)));
            result.append(String.format("   - 这些线程将永久等待，直到进程终止%n"));
            result.append(String.format("%n🔍 使用 jstack 验证:%n"));
            result.append(String.format("   jstack %d | grep -A 20 'deadlock-AB' | head -50%n", 
                    ProcessHandle.current().pid()));
            result.append(String.format("   jstack %d | grep -A 20 'deadlock-BA' | head -50%n", 
                    ProcessHandle.current().pid()));
        } else {
            result.append(String.format("✅ 未发生明显死锁（锁获取太快，尝试增加 delay 参数）%n"));
            result.append(String.format("   建议: /cpu/deadlock?pairs=%d&delay=500%n", pairCount));
        }
        
        result.append(String.format("%n📌 说明：%n"));
        result.append(String.format("   死锁发生后，线程永久阻塞无法释放，调用 /cpu/stop 也无法终止%n"));
        result.append(String.format("   必须使用 kill -9 %d 或重启应用才能解决%n", ProcessHandle.current().pid()));
        
        return result.toString();
    }

    @GetMapping("/stop")
    public String stop() {
        running.set(false);
        synchronized (activeThreads) {
            int count = activeThreads.size();
            activeThreads.clear();
            return String.format("已停止所有 %d 个CPU压力测试线程", count);
        }
    }

    @GetMapping("/status")
    public String status() {
        Runtime rt = Runtime.getRuntime();
        long usedMemory = (rt.totalMemory() - rt.freeMemory()) / 1024 / 1024;
        long maxMemory = rt.maxMemory() / 1024 / 1024;
        int threads = Thread.activeCount();
        
        return String.format(
                "CPU Test App is running\n" +
                "CPU Cores: %d\n" +
                "Active Threads: %d\n" +
                "Memory: %d MB / %d MB (%.1f%%)\n" +
                "Running: %s",
                getCores(),
                threads,
                usedMemory, maxMemory,
                usedMemory * 100.0 / maxMemory,
                running.get() ? "YES" : "NO"
        );
    }
}