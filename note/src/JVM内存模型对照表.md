# JVM 运行时数据区（JVM 内存模型）

> 各区域 · 存放数据 · 调整参数 · 常见异常 · 排查方法

---

## 一、六大区域总览

### 线程私有（随线程创建/销毁，每个线程一份）

| 区域 | 核心内容 | 说明 |
| --- | --- | --- |
| 程序计数器（PC Register） | 当前线程执行字节码的行号指示器 | 唯一不会发生 OOM 的区域；线程切换后能恢复到正确的执行位置 |
| 虚拟机栈（Stack） | 每个方法调用压入一个栈帧：局部变量表、操作数栈、动态链接、方法返回地址 | 生命周期与方法一致；递归过深会 StackOverflowError |
| 本地方法栈 | 为 Native（C/C++）方法服务 | HotSpot 中通常与虚拟机栈合二为一 |

### 线程共享（所有线程共用同一份）

| 区域 | 核心内容 | 说明 |
| --- | --- | --- |
| 堆（Heap） | 所有对象实例与数组 | GC 主战场；分年轻代（Eden + From/To Survivor）与老年代（Old） |
| 方法区 / 元空间（Metaspace） | 类元信息、运行时常量池、静态变量、JIT 编译代码 | JDK8 起用本地内存实现，称为元空间 |
| 直接内存（堆外） | NIO DirectByteBuffer / Netty 零拷贝 | 不受 -Xmx 限制，由 -XX:MaxDirectMemorySize 控制 |

---

## 二、数据 · 参数 · 异常 · 排查 对照表

| 区域 | 存放的数据 | 调整参数 | 常见异常/报错 | 排查方法与思路 |
| --- | --- | --- | --- | --- |
| 程序计数器 | 当前线程执行字节码地址；分支/跳转/异常处理下一条指令地址 | 无需调整（JVM 自动） | 几乎不会 OOM；注意线程数过多导致无法创建 | 无需处理；线程过多导致建不出线程时，调小 -Xss 或减少线程数 |
| 虚拟机栈 | 栈帧：局部变量表、操作数栈、动态链接、方法返回地址 | -Xss（单栈容量，默认约 512k~1M） | StackOverflowError；线程过多报 OutOfMemoryError: unable to create new native thread | 递归看栈是否收敛；线程数过多则调小 -Xss 或限制线程池；jstack 看 STACK 段定位卡死方法 |
| 本地方法栈 | Native 方法的局部变量表、操作数栈、动态链接、返回地址 | 同 -Xss（HotSpot 与虚拟机栈共用） | StackOverflowError / OutOfMemoryError（系统内存耗尽） | jstack / Native 栈调试；core dump 分析 |
| 堆 Heap | 对象实例、数组；几乎全部 Java 对象都在这里分配 | -Xms 初始、-Xmx 最大、-Xmn 新生代、-XX:NewRatio 新老年比、-XX:SurvivorRatio、-XX:+UseG1GC | OutOfMemoryError: Java heap space | jmap -dump 导堆 -> MAT/JProfiler 分析 GC Roots 与大对象；看 GC 日志判断泄漏 |
| 方法区 / 元空间 | 类元信息、运行时常量池、静态变量、JIT 编译代码、动态代理生成的类 | -XX:MetaspaceSize、-XX:MaxMetaspaceSize（不设则用物理内存） | OutOfMemoryError: Metaspace | 观察类加载数是否只增不减；排查 CGLIB/动态代理/反射导致的类加载器泄漏；设 MaxMetaspaceSize 兜底 |
| 直接内存 | 堆外 Native 内存；DirectByteBuffer、Netty UnpooledByteBufAllocator | -XX:MaxDirectMemorySize（JNA/NIO 堆外，不设默认约等于 -Xmx） | OutOfMemoryError: Direct buffer memory | 用 NMT / jcmd NativeMemoryTracking=detail 查；Netty 靠引用计数 + 泄漏检测；限制单次分配总量 |

---

## 三、关键 JVM 参数速查

- 栈：-Xss 单栈容量（默认约 512k~1M）
- 堆：-Xms 初始堆、-Xmx 最大堆、-Xmn 新生代大小、-XX:NewRatio 老年/年轻比、-XX:SurvivorRatio Eden/Survivor 比、GC 器如 -XX:+UseG1GC / -XX:+UseZGC
- 元空间：-XX:MetaspaceSize 触发 Full GC 阈值、-XX:MaxMetaspaceSize 上限（不设则用物理内存）
- 堆外：-XX:MaxDirectMemorySize 直接内存上限

---

## 四、常见异常与排查口诀

- StackOverflowError：递归/栈深过深 -> 查栈收敛；线程过多建不出栈 -> 降 -Xss 或限线程池，用 jstack 看 STACK 段定位卡死方法。
- OutOfMemoryError: Java heap space：堆内存不足/泄漏 -> jmap -dump 导堆，用 MAT/JProfiler 找大对象与 GC Roots，看 GC 日志判是否泄漏。
- OutOfMemoryError: Metaspace：类元空间耗尽 -> 观察类加载数是否只增不减，排查 CGLIB/动态代理/反射导致的类加载器泄漏，设 MaxMetaspaceSize 兜底。
- Direct buffer memory：堆外涨但堆不大 -> 用 NMT / jcmd NativeMemoryTracking 看堆外，Netty 靠引用计数 + 泄漏检测，限制单次分配总量。

> 一句话口诀：栈溢出查递归/线程数（jstack）；堆 OOM 查泄漏（jmap + MAT）；元空间 OOM 查类加载器泄漏；堆外 OOM 看 NMT。
