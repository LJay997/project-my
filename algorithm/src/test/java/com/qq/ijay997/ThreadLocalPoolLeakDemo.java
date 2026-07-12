package com.qq.ijay997;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadLocalPoolLeakDemo {
    // 模拟酒店前台的 ThreadLocal
    private static final ThreadLocal<String> STORAGE = new ThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        // 模拟一个只有 1 个线程的线程池（保证两个任务由同一个线程执行）
        ExecutorService executor = Executors.newFixedThreadPool(1);

        // 任务1：存入物品A，但忘记清理
        executor.submit(() -> {
            STORAGE.set("物品A");
            System.out.println("[任务1] 存入物品A，当前获取到: " + STORAGE.get());
            // 【致命错误】没有调用 remove()，物品A留在了线程里
        });

        // 任务2：处理其他逻辑，并没有主动去清理物品A
        executor.submit(() -> {
            // 此时任务2拿到的竟然是任务1留下的物品A！
            System.out.println("[任务2] 预期是null，但实际获取到: " + STORAGE.get()); 
        });

        Thread.sleep(1000); // 等待任务执行完毕
        executor.shutdown();
    }
}