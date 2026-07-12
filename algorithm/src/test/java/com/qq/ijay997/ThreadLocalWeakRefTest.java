package com.qq.ijay997;

public class ThreadLocalWeakRefTest {
    public static void main(String[] args) throws InterruptedException {
        ThreadLocal<String> threadLocal = new ThreadLocal<>();
        threadLocal.set("TestValue");
        System.out.println(1);
        // 断开对 ThreadLocal 实例的外部强引用
        threadLocal = null; 

        // 建议 JVM 立即进行垃圾回收
        System.gc();

        // 2. 疯狂触发 GC，给 JVM 足够的时间
        for (int i = 0; i < 10; i++) {
            System.gc();
            Thread.sleep(100);
        }

        // 断点查看 Thread.currentThread().threadLocals.table[?].referent

        // 再次获取值
        // 如果 Key 是弱引用，此时 Key 已经被回收变成 null，
        // 即使 Value 还在，get() 方法也会因为找不到 Key 而返回 null
        String value = threadLocal.get(); // 注意：这里会报空指针，因为 threadLocal 已经是 null
        // 正确的验证方式是观察内存或内部 Map，但 get() 返回 null 是弱引用被回收的侧面印证
        System.out.println("Value: " + value);
    }
}