package com.qq.ijay997.jvm;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.TtlCallable;
import com.alibaba.ttl.TtlRunnable;
import com.alibaba.ttl.threadpool.TtlExecutors;

import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TtlDemo {

//    private static final TransmittableThreadLocal<String> context = new TransmittableThreadLocal<>();
    private static final ThreadLocal<String> context = new ThreadLocal<>();

    public static void main(String[] args) throws Exception {
        context.set("main-thread-context");

        ttlRunnableDemo();
        ttlCallableDemo();
        ttlExecutorDemo();
        ttlExecutorServiceDemo();

        context.remove();
    }

    private static void ttlRunnableDemo() throws InterruptedException {
        System.out.println("=== TtlRunnable.get(Runnable) ===");

        Runnable task = () -> {
            System.out.println("  TtlRunnable 子线程获取上下文: " + context.get());
        };

        Thread t = new Thread(TtlRunnable.get(task));
        t.start();
        t.join();
        System.out.println();
    }

    private static void ttlCallableDemo() throws Exception {
        System.out.println("=== TtlCallable.get(Callable) ===");

        Callable<String> task = () -> {
            String value = context.get();
            System.out.println("  TtlCallable 子线程获取上下文: " + value);
            return value;
        };

        ExecutorService pool = Executors.newSingleThreadExecutor();
        String result = pool.submit(TtlCallable.get(task)).get();
        System.out.println("  Callable 返回值: " + result);
        pool.shutdown();
        pool.awaitTermination(1, TimeUnit.SECONDS);
        System.out.println();
    }

    private static void ttlExecutorDemo() throws InterruptedException {
        System.out.println("=== TtlExecutors.getTtlExecutor(Executor) ===");

        Executor rawExecutor = Executors.newSingleThreadExecutor();
        Executor ttlExecutor = TtlExecutors.getTtlExecutor(rawExecutor);

        ttlExecutor.execute(() -> {
            System.out.println("  TtlExecutor 子线程获取上下文: " + context.get());
        });

        Thread.sleep(200);
        ((ExecutorService) rawExecutor).shutdown();
        ((ExecutorService) rawExecutor).awaitTermination(1, TimeUnit.SECONDS);
        System.out.println();
    }

    private static void ttlExecutorServiceDemo() throws InterruptedException {
        System.out.println("=== TtlExecutors.getTtlExecutorService(ExecutorService) ===");

        ExecutorService rawPool = Executors.newSingleThreadExecutor();
        ExecutorService ttlPool = TtlExecutors.getTtlExecutorService(rawPool);

        ttlPool.execute(() -> {
            System.out.println("  TtlExecutorService execute 子线程获取上下文: " + context.get());
        });

        ttlPool.submit(() -> {
            System.out.println("  TtlExecutorService submit 子线程获取上下文: " + context.get());
        });

        Thread.sleep(200);
        ttlPool.shutdown();
        ttlPool.awaitTermination(1, TimeUnit.SECONDS);
        System.out.println();
    }
}