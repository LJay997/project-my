package com.qq.ijay997.jdk21;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.IntStream;

/**
 * JDK 21 —— 虚拟线程（Virtual Thread）Demo。
 *
 * <p>虚拟线程是 JDK 21 的正式特性（JEP 444）。轻量、随「载体线程」调度，
 * 可创建海量线程处理阻塞式任务而几乎不消耗系统线程资源。</p>
 *
 * <p>运行方式：java --module-path target/classes -m jdk21demos/com.qq.ijay997.jdk21.VirtualThreadDemo</p>
 *
 * @version JDK 21+
 */
public class VirtualThreadDemo {

    public static void main(String[] args) throws Exception {
        int n = 10_000;
        CountDownLatch latch = new CountDownLatch(n);

        long start = System.currentTimeMillis();

        // 用虚拟线程逐个执行一个小任务（体现低开销可大规模）
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            IntStream.range(0, n).forEach(i -> executor.submit(() -> {
                try {
                    simulateBlocking();
                } finally {
                    latch.countDown();
                }
            }));
        }

        latch.await();
        long cost = System.currentTimeMillis() - start;

        System.out.println("使用虚拟线程创建执行 " + n + " 个阻塞任务，耗时 " + cost + " ms");

        // Thread.ofVirtual() 常见构造方式
        Thread v = Thread.ofVirtual()
                .name("demo-virtual")
                .start(() -> System.out.println("Thread.ofVirtual() 运行于虚拟线程"));
        v.join();
        System.out.println("虚拟线程是否还存活: " + v.isAlive());
    }

    /** 模拟一次耗时阻塞（如 IO / 远程调用） */
    private static void simulateBlocking() {
        try {
            Thread.sleep(1);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
