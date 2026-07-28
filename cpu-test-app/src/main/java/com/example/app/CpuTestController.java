package com.example.app;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/cpu")
public class CpuTestController {

    @GetMapping("/loop")
    public String deadLoop() {
        while (true) {
            double result = Math.random() * Math.random();
        }
    }

    @GetMapping("/busy")
    public String busyCalculation(@RequestParam(defaultValue = "10000000") int iterations) {
        long sum = 0;
        for (long i = 0; i < iterations; i++) {
            sum += Math.sqrt(i) * Math.sin(i);
        }
        return "Result: " + sum;
    }

    @GetMapping("/gc")
    public String triggerGc() {
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 1000000; i++) {
            list.add(new String("test" + i));
        }
        return "Created " + list.size() + " objects";
    }

    @GetMapping("/lock")
    public String lockContention(@RequestParam(defaultValue = "10") int threads) throws InterruptedException {
        Object lock = new Object();
        for (int i = 0; i < threads; i++) {
            new Thread(() -> {
                synchronized (lock) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            }).start();
        }
        Thread.sleep(threads * 500L);
        return "Started " + threads + " threads with lock contention";
    }

    @GetMapping("/status")
    public String status() {
        return "CPU Test App is running";
    }
}