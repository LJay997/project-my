package com.qq.ijay997.jvm;

import java.util.ArrayList;
import java.util.List;

class Buffer {
    private final List<Integer> list = new ArrayList<>();
    private final int MAX = 5;

    public synchronized void produce(int item) throws InterruptedException {
        for (int i = 0; list.size() == MAX; i++) {
            wait();  // 缓冲区满了，等待（释放锁）
            System.out.println("produce wait 之后" + i);
        }
        list.add(item);
        System.out.println("生产: " + item);
        notifyAll();  // 通知消费者可以取了
    }

    public synchronized int consume() throws InterruptedException {
        for (int i = 0; list.isEmpty(); i++) {
            wait();  // 缓冲区满了，等待（释放锁）
            System.out.println("consume wait 之后" + i);
        }
        int item = list.remove(0);
        System.out.println("消费: " + item);
        notifyAll();  // 通知生产者可以放了
        return item;
    }
}