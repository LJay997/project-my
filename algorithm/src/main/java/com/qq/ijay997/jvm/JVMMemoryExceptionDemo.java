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
    
    private static void recursiveMethod(int n) {
        depth = n;
        
        // 每100层打印一次深度（避免日志过多）
        if (n % 100 == 0) {
            System.out.println("当前递归深度: " + n);
            if (n > maxDepth) {
                maxDepth = n;
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
     * -XX:MetaspaceSize=10m -XX:MaxMetaspaceSize=10m
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
        
        List<java.nio.ByteBuffer> buffers = new ArrayList<>();
        int count = 0;
        
        try {
            while (true) {
                // 分配 1MB 直接内存
                buffers.add(java.nio.ByteBuffer.allocateDirect(1024 * 1024));
                count++;
                
                if (count % 5 == 0) {
                    System.out.println("已分配 " + count + " MB 直接内存");
                }
            }
        } catch (OutOfMemoryError e) {
            System.err.println("❌ 直接内存溢出！已分配 " + count + " MB");
            System.err.println("错误信息：" + e.getMessage());
            e.printStackTrace();
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
        
        for (int i = 0; i < 5; i++) {
            // 创建临时对象
            List<String> list = new ArrayList<>();
            for (int j = 0; j < 10000; j++) {
                list.add("String-" + j);
            }
            
            System.out.println("第 " + (i + 1) + " 轮：创建了 " + list.size() + " 个对象");
            
            // 手动触发 GC（仅用于演示，生产环境不建议）
            System.gc();
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        
        System.out.println("✅ 正常 GC 演示完成");
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
}
