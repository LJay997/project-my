package com.qq.ijay997.jvm;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * JVM 内存异常模拟类
 * 用于演示各种内存溢出场景
 * 
 * @author ijay997
 */
public class JVMMemoryExceptionDemo {

    // ==================== 1. 堆内存溢出 ====================
    
    /**
     * 模拟 Java 堆溢出（OutOfMemoryError: Java heap space）
     * 
     * 启动参数：
     * -Xms10m -Xmx10m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./heap_dump.hprof
     * 
     * 预期结果：抛出 OutOfMemoryError
     */
    public static void simulateHeapOOM() {
        System.out.println("=== 开始模拟堆内存溢出 ===");
        System.out.println("提示：请使用 -Xms10m -Xmx10m 参数启动");
        
        List<byte[]> list = new ArrayList<>();
        int count = 0;
        
        try {
            while (true) {
                // 每次分配 1MB 空间
                list.add(new byte[1024 * 1024]);
                count++;
                
                if (count % 10 == 0) {
                    System.out.println("已分配 " + count + " MB");
                }
            }
        } catch (OutOfMemoryError e) {
            System.err.println("❌ 堆内存溢出！已分配 " + count + " MB");
            System.err.println("错误信息：" + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 2. 虚拟机栈溢出 ====================
    
    /**
     * 模拟虚拟机栈溢出（StackOverflowError）
     * 
     * 启动参数：
     * -Xss128k （减小栈大小，更容易触发）
     * 
     * 预期结果：抛出 StackOverflowError
     */
    public static void simulateStackOverflow() {
        System.out.println("=== 开始模拟栈溢出 ===");
        System.out.println("提示：请使用 -Xss128k 参数启动");
        
        try {
            recursiveMethod(0);
        } catch (StackOverflowError e) {
            System.err.println("❌ 虚拟机栈溢出！");
            System.err.println("最大递归深度: " + maxDepth);
            System.err.println("错误信息：" + e.getMessage());
            
            // 自动保存线程转储
            String dumpFile = saveThreadDump();
            if (dumpFile != null) {
                System.err.println("✅ 线程转储已保存到: " + dumpFile);
                System.err.println("💡 使用以下命令查看:");
                System.err.println("   cat " + dumpFile);
            }
            // e.printStackTrace(); // 栈太深，打印会很慢
        }
    }
    
    private static int depth = 0;
    private static int maxDepth = 0;  // 记录最大深度
    private static boolean dumpSaved = false;  // 标记是否已保存转储
    
    private static void recursiveMethod(int n) {
        depth = n;
        
        // 每100层打印一次深度（避免日志过多）
        if (n % 100 == 0) {
            System.out.println("当前递归深度: " + n);
            if (n > maxDepth) {
                maxDepth = n;
            }
            
            // 在达到一定深度时保存一次转储（能看到递归过程）
            if (n == 3300 && !dumpSaved) {
                System.out.println("\n💡 在深度500时保存线程转储...");
                saveThreadDump();
                dumpSaved = true;
            }
        }
        
        // 无限递归，没有终止条件
        recursiveMethod(n + 1);
    }

    // ==================== 3. 方法区/元空间溢出 ====================
    
    /**
     * 模拟元空间溢出（OutOfMemoryError: Metaspace）
     * JDK 8+ 使用元空间替代永久代
     * 
     * 启动参数：
     * -XX:MetaspaceSize=10m
     * 
     * 需要依赖：CGLIB 或 ASM
     * <dependency>
     *     <groupId>cglib</groupId>
     *     <artifactId>cglib</artifactId>
     *     <version>3.3.0</version>
     * </dependency>
     * 
     * 预期结果：抛出 OutOfMemoryError: Metaspace
     */
    public static void simulateMetaspaceOOM() {
        System.out.println("=== 开始模拟元空间溢出 ===");
        System.out.println("提示：请使用 -XX:MaxMetaspaceSize=10m 参数启动");
        System.out.println("需要添加 CGLIB 依赖");
        
        try {
            // 使用 CGLIB 动态生成类
            int count = 0;
            while (true) {
                net.sf.cglib.proxy.Enhancer enhancer = new net.sf.cglib.proxy.Enhancer();
                enhancer.setSuperclass(JVMMemoryExceptionDemo.class);
                enhancer.setUseCache(false);  // 不使用缓存，每次都生成新类
                enhancer.setCallback(new net.sf.cglib.proxy.NoOp() {  // 设置空回调
                    @Override
                    public String toString() {
                        return "NoOp";
                    }
                });
                enhancer.create();
                count++;
                
                if (count % 1000 == 0) {
                    System.out.println("已生成 " + count + " 个动态类");
                }
            }
        } catch (OutOfMemoryError e) {
            System.err.println("❌ 元空间溢出！");
            System.err.println("错误信息：" + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 4. 直接内存溢出 ====================
    
    /**
     * 模拟直接内存溢出（OutOfMemoryError: Direct buffer memory）
     * 
     * 启动参数：
     * -XX:MaxDirectMemorySize=10m
     * 
     * 预期结果：抛出 OutOfMemoryError
     */
    public static void simulateDirectMemoryOOM() {
        System.out.println("=== 开始模拟直接内存溢出 ===");
        System.out.println("提示：请使用 -XX:MaxDirectMemorySize=10m 参数启动");
        System.out.println("💡 可以在 VisualVM 的 MBeans 中查看 'java.nio.BufferPool.direct' 监控直接内存");
        System.out.println();
        try {
            Thread.sleep(1000 * 5);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        List<java.nio.ByteBuffer> buffers = new ArrayList<>();
        int count = 0;
        
        try {
            while (true) {
                // 分配 1MB 直接内存
                buffers.add(java.nio.ByteBuffer.allocateDirect(1024 * 1024));
                count++;
                
                if (count % 5 == 0) {
                    System.out.println("已分配 " + count + " MB 直接内存");
                    
                    // 打印 BufferPool 信息
                    printBufferPoolInfo();
                }
                Thread.sleep(1000 * 7);
            }
        } catch (OutOfMemoryError e) {
            System.err.println("❌ 直接内存溢出！已分配 " + count + " MB");
            System.err.println("错误信息：" + e.getMessage());
            
            // 保存诊断信息
            String dumpFile = saveDirectMemoryDiagnostic(count);
            if (dumpFile != null) {
                System.err.println("✅ 诊断报告已保存到: " + dumpFile);
            }
            
            // e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
    
    /**
     * 打印 BufferPool 信息（用于 VisualVM MBeans 对比）
     */
    private static void printBufferPoolInfo() {
        try {
            // 通过反射获取 BufferPool MXBean
            Class<?> managementFactoryClass = Class.forName("java.lang.management.ManagementFactory");
            java.lang.reflect.Method getPlatformMXBeansMethod = managementFactoryClass.getMethod(
                "getPlatformMXBeans", Class.class);
            
            Class<?> bufferPoolMXBeanClass = Class.forName("java.lang.management.BufferPoolMXBean");
            java.util.List<?> pools = (java.util.List<?>) getPlatformMXBeansMethod.invoke(
                null, bufferPoolMXBeanClass);
            
            for (Object pool : pools) {
                java.lang.reflect.Method getNameMethod = pool.getClass().getMethod("getName");
                java.lang.reflect.Method getCountMethod = pool.getClass().getMethod("getCount");
                java.lang.reflect.Method getTotalCapacityMethod = pool.getClass().getMethod("getTotalCapacity");
                java.lang.reflect.Method getMemoryUsedMethod = pool.getClass().getMethod("getMemoryUsed");
                
                String name = (String) getNameMethod.invoke(pool);
                if ("direct".equals(name)) {
                    long count = (Long) getCountMethod.invoke(pool);
                    long capacity = (Long) getTotalCapacityMethod.invoke(pool);
                    long used = (Long) getMemoryUsedMethod.invoke(pool);
                    
                    System.out.printf("  [BufferPool] direct: count=%d, capacity=%d MB, used=%d MB%n",
                        count, capacity / 1024 / 1024, used / 1024 / 1024);
                }
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }

    // ==================== 5. GC 过度频繁 ====================
    
    /**
     * 模拟 GC 过度频繁（GC overhead limit exceeded）
     * 
     * 启动参数：
     * -Xms10m -Xmx10m -XX:+UseParallelGC
     * 
     * 预期结果：抛出 OutOfMemoryError: GC overhead limit exceeded
     */
    public static void simulateGCOverhead() {
        System.out.println("=== 开始模拟 GC 过度频繁 ===");
        System.out.println("提示：请使用 -Xms10m -Xmx10m 参数启动");
        
        try {
            // 创建大量短生命周期对象，导致频繁 GC
            while (true) {
                List<byte[]> list = new ArrayList<>();
                for (int i = 0; i < 1000; i++) {
                    list.add(new byte[1024]);  // 1KB
                }
                // 立即丢弃，产生大量垃圾
            }
        } catch (OutOfMemoryError e) {
            System.err.println("❌ GC 过度频繁！");
            System.err.println("错误信息：" + e.getMessage());
            e.printStackTrace();
        }
    }

    // ==================== 6. 正常情况演示 ====================
    
    /**
     * 演示正常的对象创建和垃圾回收
     */
    public static void demonstrateNormalGC() {
        System.out.println("=== 演示正常 GC ===");
        
        for (int i = 0;  ; i++) {
            // 创建临时对象
            List<String> list = new ArrayList<>();
            for (int j = 0; j < 10000; j++) {
                list.add("String-" + j);
            }
            
            System.out.println("第 " + (i + 1) + " 轮：创建了 " + list.size() + " 个对象");
            
            // 手动触发 GC（仅用于演示，生产环境不建议）
            System.gc();
            
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//            }
        }
        
//        System.out.println("✅ 正常 GC 演示完成");
    }

    // ==================== 7. GC 算法对比测试 ====================
    
    /**
     * GC 算法对比测试
     * 通过不同的内存分配模式，观察各种 GC 算法的表现
     * 
     * 支持的 GC 算法：
     * - Serial GC: -XX:+UseSerialGC
     * - Parallel GC: -XX:+UseParallelGC (默认)
     * - CMS GC: -XX:+UseConcMarkSweepGC (JDK 8)
     * - G1 GC: -XX:+UseG1GC (JDK 9+ 默认)
     * 
     * 启动参数示例：
     * -Xms512m -Xmx512m -XX:+UseG1GC -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps
     */
    public static void testGCAlgorithm() {
        System.out.println("=== GC 算法对比测试 ===");
        System.out.println("提示：请使用不同的 GC 参数启动以对比效果");
        System.out.println();
        System.out.println("推荐参数组合：");
        System.out.println("1. Serial GC:  -Xms512m -Xmx512m -XX:+UseSerialGC -verbose:gc");
        System.out.println("2. Parallel GC: -Xms512m -Xmx512m -XX:+UseParallelGC -verbose:gc");
        System.out.println("3. G1 GC:      -Xms512m -Xmx512m -XX:+UseG1GC -verbose:gc");
        System.out.println();
        
        // 打印当前 GC 信息
        printGCInfo();
        
        System.out.println();
        System.out.println("开始测试...");
        System.out.println("每 5 秒执行一次 GC，观察 GC 日志输出");
        System.out.println("按 Ctrl+C 停止\n");
        
        int round = 0;
        long totalAllocated = 0;
        
        try {
            while (true) {
                round++;
                long startMem = getUsedMemory();
                
                // 模拟不同的内存分配场景
                scenario1_ShortLivedObjects();   // 短生命周期对象
                scenario2_LongLivedObjects();    // 长生命周期对象
                scenario3_LargeObjects();        // 大对象
                
                long endMem = getUsedMemory();
                long allocated = endMem - startMem;
                totalAllocated += allocated;
                
                if (round % 10 == 0) {
                    System.out.printf("[Round %d] 本轮分配: %d MB, 累计分配: %d MB, 当前堆使用: %d MB / %d MB%n",
                        round,
                        allocated / 1024 / 1024,
                        totalAllocated / 1024 / 1024,
                        getUsedMemory() / 1024 / 1024,
                        getMaxMemory() / 1024 / 1024
                    );
                }

                // 每 5 秒触发一次 GC
                Thread.sleep(5000);
                System.gc();
                
                // 打印 GC 统计
                if (round % 5 == 0) {
                    printGCStats();
                }
            }
        } catch (InterruptedException e) {
            System.out.println("\n测试被中断");
            Thread.currentThread().interrupt();
        } catch (OutOfMemoryError e) {
            System.err.println("❌ 内存溢出！");
            System.err.println("错误信息：" + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * 场景1：短生命周期对象（新生代 GC 测试）
     * 模拟 Web 请求中的临时对象
     */
    private static void scenario1_ShortLivedObjects() {
        // 创建大量短期对象，应该很快被回收
        for (int i = 0; i < 10000; i++) {
            byte[] temp = new byte[1024];  // 1KB
            // 立即丢弃，不保存引用
        }
    }
    
    /**
     * 场景2：长生命周期对象（老年代 GC 测试）
     * 模拟缓存、连接池等长期存在的对象
     */
    private static java.util.List<byte[]> longLivedCache = new ArrayList<>();
    
    private static void scenario2_LongLivedObjects() {
        // 创建长期存活的对象
        if (longLivedCache.size() < 100) {
            longLivedCache.add(new byte[10 * 1024]);  // 10KB
        }
    }
    
    /**
     * 场景3：大对象（直接晋升老年代测试）
     * 模拟大数组、大字符串等
     */
    private static void scenario3_LargeObjects() {
        // 创建大对象，可能直接进入老年代
        byte[] largeObject = new byte[100 * 1024];  // 100KB
        // 立即丢弃
    }
    
    /**
     * 打印当前 GC 配置信息
     */
    private static void printGCInfo() {
        System.out.println("--- JVM 内存配置 ---");
        System.out.println("初始堆大小 (-Xms): " + (getInitMemory() / 1024 / 1024) + " MB");
        System.out.println("最大堆大小 (-Xmx): " + (getMaxMemory() / 1024 / 1024) + " MB");
        System.out.println("当前堆使用: " + (getUsedMemory() / 1024 / 1024) + " MB");
        System.out.println();
        
        System.out.println("--- GC 算法信息 ---");
        try {
            // 通过 JMX 获取 GC 信息
            Class<?> managementFactoryClass = Class.forName("java.lang.management.ManagementFactory");
            java.lang.reflect.Method getGarbageCollectorMXBeansMethod = 
                managementFactoryClass.getMethod("getGarbageCollectorMXBeans");
            
            java.util.List<?> gcBeans = (java.util.List<?>) getGarbageCollectorMXBeansMethod.invoke(null);
            
            for (Object gcBean : gcBeans) {
                java.lang.reflect.Method getNameMethod = gcBean.getClass().getMethod("getName");
                String gcName = (String) getNameMethod.invoke(gcBean);
                System.out.println("GC 收集器: " + gcName);
            }
        } catch (Exception e) {
            System.out.println("无法获取 GC 信息: " + e.getMessage());
        }
    }
    
    /**
     * 打印 GC 统计信息
     */
    private static void printGCStats() {
        try {
            Class<?> managementFactoryClass = Class.forName("java.lang.management.ManagementFactory");
            java.lang.reflect.Method getGarbageCollectorMXBeansMethod = 
                managementFactoryClass.getMethod("getGarbageCollectorMXBeans");
            
            java.util.List<?> gcBeans = (java.util.List<?>) getGarbageCollectorMXBeansMethod.invoke(null);
            
            System.out.println("\n--- GC 统计 ---");
            for (Object gcBean : gcBeans) {
                java.lang.reflect.Method getNameMethod = gcBean.getClass().getMethod("getName");
                java.lang.reflect.Method getCollectionCountMethod = gcBean.getClass().getMethod("getCollectionCount");
                java.lang.reflect.Method getCollectionTimeMethod = gcBean.getClass().getMethod("getCollectionTime");
                
                String name = (String) getNameMethod.invoke(gcBean);
                long count = (Long) getCollectionCountMethod.invoke(gcBean);
                long time = (Long) getCollectionTimeMethod.invoke(gcBean);
                
                System.out.printf("  %-20s 次数: %-5d  总耗时: %-6d ms  平均: %.2f ms/次%n",
                    name, count, time, count > 0 ? (double)time / count : 0);
            }
            System.out.println();
        } catch (Exception e) {
            // 忽略异常
        }
    }
    
    /**
     * 获取已使用的堆内存（字节）
     */
    private static long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
    
    /**
     * 获取最大堆内存（字节）
     */
    private static long getMaxMemory() {
        return Runtime.getRuntime().maxMemory();
    }
    
    /**
     * 获取初始堆内存（字节）
     */
    private static long getInitMemory() {
        return Runtime.getRuntime().totalMemory();
    }

    // ==================== 8. 死锁模拟 ====================
    
    /**
     * 模拟线程死锁（Deadlock）
     * 
     * 启动参数：
     * 无需特殊参数
     * 
     * 预期结果：程序卡住，两个线程互相等待对方持有的锁
     */
    public static void simulateDeadlock() {
        System.out.println("=== 开始模拟线程死锁 ===");
        System.out.println("提示：程序将进入死锁状态，请使用 jstack 或 VisualVM 检测");
        System.out.println();
        
        // 创建两个共享资源
        final Object resource1 = "资源1";
        final Object resource2 = "资源2";
        
        // 线程1：先锁定 resource1，再尝试锁定 resource2
        Thread thread1 = new Thread(() -> {
            synchronized (resource1) {
                System.out.println("[线程1] 已获取资源1，等待资源2...");
                try {
                    Thread.sleep(100); // 确保线程2有时间获取 resource2
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                System.out.println("[线程1] 尝试获取资源2...");
                synchronized (resource2) {
                    System.out.println("[线程1] 已获取资源2");
                }
            }
        }, "Thread-Deadlock-1");
        
        // 线程2：先锁定 resource2，再尝试锁定 resource1
        Thread thread2 = new Thread(() -> {
            synchronized (resource2) {
                System.out.println("[线程2] 已获取资源2，等待资源1...");
                try {
                    Thread.sleep(100); // 确保线程1有时间获取 resource1
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                
                System.out.println("[线程2] 尝试获取资源1...");
                synchronized (resource1) {
                    System.out.println("[线程2] 已获取资源1");
                }
            }
        }, "Thread-Deadlock-2");
        
        // 启动线程
        thread1.start();
        thread2.start();
        
        System.out.println();
        System.out.println("💡 死锁已形成！请使用以下工具检测：");
        System.out.println("   1. jstack <pid> - 查看线程堆栈");
        System.out.println("   2. VisualVM - 线程标签页会显示'检测到死锁'");
        System.out.println("   3. jcmd <pid> Thread.print - 打印线程信息");
        System.out.println();
        System.out.println("按 Ctrl+C 退出程序");
        
        // 保持主线程运行
        try {
            thread1.join();
            thread2.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    // ==================== 主方法 ====================
    
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("JVM 内存异常模拟程序");
        System.out.println("========================================");
        System.out.println();
        System.out.println("请选择要模拟的场景：");
        System.out.println("1. 堆内存溢出（Heap OOM）");
        System.out.println("2. 虚拟机栈溢出（Stack Overflow）");
        System.out.println("3. 元空间溢出（Metaspace OOM）");
        System.out.println("4. 直接内存溢出（Direct Memory OOM）");
        System.out.println("5. GC 过度频繁（GC Overhead）");
        System.out.println("6. 正常 GC 演示");
        System.out.println("7. GC 算法对比测试 ⭐");
        System.out.println("8. 线程死锁（Deadlock） ⭐");
        System.out.println("========================================");
        
        if (args.length > 0) {
            int choice = Integer.parseInt(args[0]);
            runSimulation(choice);
        } else {
            // 默认运行正常演示
            demonstrateNormalGC();
        }
    }
    
    private static void runSimulation(int choice) {
        switch (choice) {
            case 1:
                simulateHeapOOM();
                break;
            case 2:
                simulateStackOverflow();
                break;
            case 3:
                simulateMetaspaceOOM();
                break;
            case 4:
                simulateDirectMemoryOOM();
                break;
            case 5:
                simulateGCOverhead();
                break;
            case 6:
                demonstrateNormalGC();
                break;
            case 7:
                testGCAlgorithm();
                break;
            case 8:
                simulateDeadlock();
                break;
            default:
                System.out.println("无效的选择！");
        }
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 保存线程转储到文件
     * @return 文件路径，失败返回 null
     */
    private static String saveThreadDump() {
        try {
            // 生成文件名（带时间戳）
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = sdf.format(new Date());
            String fileName = "thread_dump_" + timestamp + ".tdump";
            String filePath = "./" + fileName;
            
            // 获取线程 MXBean
            ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
            ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
            
            // 写入文件
            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
                writer.println("Thread Dump generated at: " + new Date());
                writer.println("================================================================================");
                writer.println();
                
                for (ThreadInfo threadInfo : threadInfos) {
                    writer.println(threadInfo.toString());
                    writer.println();
                }
            }
            
            return filePath;
        } catch (IOException e) {
            System.err.println("⚠️ 保存线程转储失败: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 保存直接内存溢出的诊断报告
     * 当 MBeans 无法满足需求（特别是排查内存泄漏或 Netty 内存问题）时，必须使用 JDK 自带的 NMT 功能。它不依赖 MBean，而是直接跟踪 JVM 的本地内存分配。
     * 开启方法
     * 在启动参数中添加：-XX:NativeMemoryTracking=detail
     *  * 或使用 Visual VM 的 MBeans 查看 java.nio 模块
     *  * 使用 jcmd 查看（不需要连接图形界面）： jcmd <pid> VM.native_memory summary
     *
     * @param allocatedMB 已分配的直接内存大小（MB）
     * @return 文件路径，失败返回 null
     */
    private static String saveDirectMemoryDiagnostic(int allocatedMB) {
        try {
            // 生成文件名
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            String timestamp = sdf.format(new Date());
            String fileName = "direct_memory_diagnostic_" + timestamp + ".txt";
            String filePath = "./" + fileName;
            
            // 获取 JVM 信息
            Runtime runtime = Runtime.getRuntime();
            long heapUsed = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024;
            long heapMax = runtime.maxMemory() / 1024 / 1024;
            
            // 获取直接内存信息（通过反射）
            String directMemoryInfo = "N/A";
            try {
                Class<?> bitsClass = Class.forName("sun.misc.VM");
                java.lang.reflect.Method maxDirectMethod = bitsClass.getMethod("maxDirectMemory");
                long maxDirect = (Long) maxDirectMethod.invoke(null);
                directMemoryInfo = (maxDirect / 1024 / 1024) + " MB";
            } catch (Exception ex) {
                directMemoryInfo = "无法获取（可能不是 Sun/Oracle JDK）";
            }
            
            // 写入诊断报告
            try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
                writer.println("================================================================================");
                writer.println("直接内存溢出诊断报告");
                writer.println("================================================================================");
                writer.println();
                writer.println("生成时间: " + new Date());
                writer.println();
                writer.println("--- 错误信息 ---");
                writer.println("异常类型: java.lang.OutOfMemoryError");
                writer.println("错误消息: Direct buffer memory");
                writer.println("已分配直接内存: " + allocatedMB + " MB");
                writer.println();
                writer.println("--- JVM 堆信息 ---");
                writer.println("堆已使用: " + heapUsed + " MB");
                writer.println("堆最大值: " + heapMax + " MB");
                writer.println("堆使用率: " + String.format("%.2f", (double)heapUsed / heapMax * 100) + "%");
                writer.println();
                writer.println("--- 直接内存配置 ---");
                writer.println("最大直接内存 (-XX:MaxDirectMemorySize): " + directMemoryInfo);
                writer.println();
                writer.println("--- 问题分析 ---");
                writer.println("1. 直接内存在堆外分配，不受 -Xmx 限制");
                writer.println("2. 由 ByteBuffer.allocateDirect() 分配");
                writer.println("3. 需要通过 Cleaner 机制或显式调用 free() 释放");
                writer.println("4. GC 不会立即回收直接内存，有延迟");
                writer.println();
                writer.println("--- 解决方案 ---");
                writer.println("1. 增大直接内存: -XX:MaxDirectMemorySize=2g");
                writer.println("2. 及时释放 ByteBuffer: ((DirectBuffer)buffer).cleaner().clean()");
                writer.println("3. 避免频繁创建/销毁 DirectBuffer");
                writer.println("4. 使用对象池复用 DirectBuffer");
                writer.println();
                writer.println("--- 线程信息 ---");
                
                // 添加线程转储
                ThreadMXBean threadMXBean = ManagementFactory.getThreadMXBean();
                ThreadInfo[] threadInfos = threadMXBean.dumpAllThreads(true, true);
                for (ThreadInfo threadInfo : threadInfos) {
                    writer.println(threadInfo.toString());
                    writer.println();
                }
                
                writer.println("================================================================================");
                writer.println("报告结束");
                writer.println("================================================================================");
            }
            
            return filePath;
        } catch (IOException e) {
            System.err.println("⚠️ 保存诊断报告失败: " + e.getMessage());
            return null;
        }
    }
}
