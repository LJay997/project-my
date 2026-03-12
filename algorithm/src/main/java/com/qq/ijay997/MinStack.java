package com.qq.ijay997;

import java.util.PriorityQueue;
import java.util.Stack;

class MinStack {

    private Integer [] elements;

    private int size;

    /**
     * 指向下一个可用位置
     */
    private int curIndex;

    public MinStack() {
        elements = new Integer[10];
        size = 0;
        curIndex = 0;
    }
    
    public void push(int val) {
        if (size == elements.length){
            Integer [] newElements = new Integer[elements.length * 2];
            System.arraycopy(elements, 0, newElements, 0, size);
            elements = newElements;
        }
        elements[curIndex++] = val;
        size++;
    }

    public void pop() {
        elements[--curIndex] = null;
        size--;
    }
    
    public int top() {
        return elements[curIndex -1];
    }
    
    public int getMin() {
        PriorityQueue<Integer> queue = new PriorityQueue<>();
        for (int i = 0; i < size; i++) {
            queue.add(elements[i]);
        }
        return queue.peek();
    }
}