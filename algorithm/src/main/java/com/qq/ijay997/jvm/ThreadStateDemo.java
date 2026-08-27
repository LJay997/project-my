package com.qq.ijay997.jvm;

public class ThreadStateDemo { public static void main(String[] args) throws InterruptedException { Object lock = new Object();

    Thread t = new Thread(() -> {
        synchronized (lock) {
            try {
                Thread.sleep(1000);   // RUNNABLE → TIMED_WAITING
                lock.wait();          // TIMED_WAITING → WAITING（释放锁）
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    });

    // 1. NEW
    System.out.println("创建后: " + t.getState());          // NEW

    t.start();
    Thread.sleep(100);
    // 2. TIMED_WAITING（sleep 中）
    System.out.println("sleep中: " + t.getState());         // TIMED_WAITING

    Thread.sleep(1000);
    // 3. WAITING（wait 中）
    System.out.println("wait中: " + t.getState());          // WAITING

    synchronized (lock) {
        lock.notify();
        Thread.sleep(100);
        // 4. BLOCKED（被唤醒但锁被主线程持有）
        System.out.println("notify后: " + t.getState());    // BLOCKED
    }

    Thread.sleep(100);
    // 5. TERMINATED
    System.out.println("结束后: " + t.getState());          // TERMINATED
}
}