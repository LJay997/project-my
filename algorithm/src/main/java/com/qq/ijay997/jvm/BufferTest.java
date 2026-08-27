package com.qq.ijay997.jvm;


public class BufferTest {
    public static void main(String[] args) {
        Buffer buffer = new Buffer();

        // 生产者线程：生产 10 个商品
        Thread producer = new Thread(() -> {
            for (int i = 1; i <= 10; i++) {
                try {
                    buffer.produce(i);
                    Thread.sleep(200); // 模拟生产耗时
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "Producer");

        // 消费者线程：消费 10 个商品
        Thread consumer = new Thread(() -> {
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(500); // 模拟消费比生产慢，触发生产者 wait
                    int item = buffer.consume();
                    System.out.println("消费者拿到: " + item);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "Consumer");

        producer.start();
        consumer.start();
    }
}