package com.qq.ijay997;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 共享缓冲区类：使用 ReentrantLock + 多 Condition 实现精准唤醒
 */
class BufferWithConditions {
    private final Queue<Integer> queue = new LinkedList<>();
    private final int capacity = 5; // 缓冲区容量
    private final Lock lock = new ReentrantLock();
    
    // 定义两个条件变量
    // notFull: 当队列满时，生产者在此等待
    private final Condition notFull = lock.newCondition();  
    // notEmpty: 当队列空时，消费者在此等待
    private final Condition notEmpty = lock.newCondition(); 

    /**
     * 生产者方法
     */
    public void produce(int val) throws InterruptedException {
        lock.lock();
        try {
            // 1. 如果缓冲区满了，生产者进入等待状态 (await)
            // 注意：必须用 while 循环防止虚假唤醒
            while (queue.size() == capacity) {
                System.out.println("🔴 [生产者] 缓冲区已满 (Size: " + queue.size() + "), 等待消费...");
                notFull.await(); // 释放锁并挂起，直到被 notFull.signal() 唤醒
            }

            // 2. 执行生产
            queue.offer(val);
            System.out.println("✅ [生产者] 生产数据: " + val + " | 当前大小: " + queue.size());

            // 3. 生产完成后，通知消费者 (notEmpty) 可以取数据了
            notEmpty.signal(); 
        } finally {
            lock.unlock();
        }
    }

    /**
     * 消费者方法
     */
    public int consume() throws InterruptedException {
        lock.lock();
        try {
            // 1. 如果缓冲区空了，消费者进入等待状态 (await)
            while (queue.isEmpty()) {
                System.out.println("🔵 [消费者] 缓冲区已空 (Size: 0), 等待生产...");
                notEmpty.await(); // 释放锁并挂起，直到被 notEmpty.signal() 唤醒
            }

            // 2. 执行消费
            int val = queue.poll();
            System.out.println("🍽️ [消费者] 消费数据: " + val + " | 当前大小: " + queue.size());

            // 3. 消费完成后，通知生产者 (notFull) 可以继续生产了
            notFull.signal();
            
            return val;
        } finally {
            lock.unlock();
        }
    }
}

/**
 * 生产者线程任务
 */
class ProducerTask implements Runnable {
    private final BufferWithConditions buffer;
    private final int id;
    private final Random random = new Random();

    public ProducerTask(BufferWithConditions buffer, int id) {
        this.buffer = buffer;
        this.id = id;
    }

    @Override
    public void run() {
        int count = 0;
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // 模拟生产耗时 (0~500ms)
                Thread.sleep(random.nextInt(500)); 
                int data = id * 1000 + (++count); // 生成唯一数据
                buffer.produce(data);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("⛔ 生产者-" + id + " 被中断停止。");
        }
    }
}

/**
 * 消费者线程任务
 */
class ConsumerTask implements Runnable {
    private final BufferWithConditions buffer;
    private final int id;
    private final Random random = new Random();

    public ConsumerTask(BufferWithConditions buffer, int id) {
        this.buffer = buffer;
        this.id = id;
    }

    @Override
    public void run() {
        try {
            while (!Thread.currentThread().isInterrupted()) {
                // 模拟消费耗时 (0~800ms，故意比生产慢一点，容易触发"空"等待)
                Thread.sleep(random.nextInt(800)); 
                buffer.consume();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.out.println("⛔ 消费者-" + id + " 被中断停止。");
        }
    }
}

/**
 * 主启动类
 */
public class MultiConditionDemo {
    public static void main(String[] args) throws InterruptedException {
        BufferWithConditions buffer = new BufferWithConditions();

        // 创建 2 个生产者
        Thread p1 = new Thread(new ProducerTask(buffer, 1), "Producer-1");
        Thread p2 = new Thread(new ProducerTask(buffer, 2), "Producer-2");

        // 创建 3 个消费者 (故意多设消费者，观察"空"等待场景)
        Thread c1 = new Thread(new ConsumerTask(buffer, 1), "Consumer-1");
        Thread c2 = new Thread(new ConsumerTask(buffer, 2), "Consumer-2");
        Thread c3 = new Thread(new ConsumerTask(buffer, 3), "Consumer-3");

        // 启动所有线程
        p1.start();
        p2.start();
        c1.start();
        c2.start();
        c3.start();

        System.out.println("🚀 多条件变量演示启动... (观察控制台输出)\n");

        // 运行 10 秒后自动停止，避免死循环
        Thread.sleep(10000);

        // 优雅关闭
        System.out.println("\n⏹️ 正在停止所有线程...");
        p1.interrupt();
        p2.interrupt();
        c1.interrupt();
        c2.interrupt();
        c3.interrupt();

        // 等待线程结束
        p1.join();
        p2.join();
        c1.join();
        c2.join();
        c3.join();

        System.out.println("✅ 程序正常退出。");
    }
}