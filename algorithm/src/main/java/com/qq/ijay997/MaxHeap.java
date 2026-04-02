package com.qq.ijay997;

import java.util.ArrayList;
import java.util.List;

public class MaxHeap {
    private List<Integer> maxHeap;

    MaxHeap() {
        maxHeap = new ArrayList<>();
    }

    private int left(int i) {
        return 2 * i + 1;
    }

    private int right(int i) {
        return 2 * i + 2;
    }

    private int parent(int i) {
        return (i - 1) / 2;
    }

    public int peek() {
        return maxHeap.get(0);
    }

    /* 元素入堆 */
    void push(int val) {
        maxHeap.add(val);
        siftUp(maxHeap.size() - 1);
    }

    public int pop() {
        if (maxHeap == null || maxHeap.isEmpty()) return -1;

        Integer value = maxHeap.get(0);
        Integer lastValue = maxHeap.remove(maxHeap.size() - 1);
        if (!maxHeap.isEmpty()) {
            maxHeap.set(0, lastValue);
            sitDown(0);
        }
        return value;
    }

    private void sitDown(int i) {
        if (i < 0 || i >= maxHeap.size() - 1) return;

        int curValue = maxHeap.get(i), leftIndex = left(i), rightIndex = right(i);
        // 子节点索引未越界
        if (leftIndex >= maxHeap.size() || rightIndex >= maxHeap.size()) return;

        if (maxHeap.get(leftIndex) > curValue && maxHeap.get(leftIndex) > maxHeap.get(rightIndex)) {
            swap(i, leftIndex);
            sitDown(leftIndex);
        } else if (maxHeap.get(rightIndex) > curValue && maxHeap.get(rightIndex) > maxHeap.get(leftIndex)) {
            swap(i, right(i));
            sitDown(rightIndex);
        }
    }

    private void swap(int i, int j) {
        Integer value1 = maxHeap.get(i);
        maxHeap.set(i, maxHeap.get(j));
        maxHeap.set(j, value1);
    }

    /* 从节点 i 开始，从底至顶堆化 */
    private void siftUp(int i) {
        if (i == 0) return;

        Integer parent = maxHeap.get(parent(i));
        if (parent >= maxHeap.get(i)) return;

        if (parent < maxHeap.get(i)) {
            maxHeap.set(parent(i), maxHeap.get(i));
            maxHeap.set(i, parent);
        }
        siftUp(parent(i));
    }

    public static void main(String[] args) {
        MaxHeap maxHeap = new MaxHeap();
        maxHeap.push(4);
        maxHeap.push(1);
        maxHeap.push(2);
        maxHeap.push(3);
        maxHeap.pop();
        System.out.println(maxHeap.peek());
    }
}
