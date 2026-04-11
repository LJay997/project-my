# JVM 内存模型详解与记忆口诀

## 📚 目录

1. [JVM 内存结构总览](#jvm-内存结构总览)
2. [程序计数器](#1-程序计数器-pc-register)
3. [Java 虚拟机栈](#2-java-虚拟机栈-jvm-stack)
4. [本地方法栈](#3-本地方法栈-native-method-stack)
5. [堆](#4-堆-heap)
6. [方法区](#5-方法区-method-area)
7. [运行时常量池](#6-运行时常量池-runtime-constant-pool)
8. [直接内存](#7-直接内存-direct-memory)
9. [内存溢出案例](#内存溢出案例)
10. [记忆口诀](#记忆口诀)

---

## JVM 内存结构总览

```
┌─────────────────────────────────────────────────┐
│                  JVM 内存模型                    │
├─────────────────────────────────────────────────┤
│                                                   │
│  线程私有                    线程共享               │
│  ┌──────────────┐           ┌──────────────┐     │
│  │ 程序计数器    │           │              │     │
│  ├──────────────┤           │     堆       │     │
│  │ Java虚拟机栈  │           │  (Heap)      │     │
│  ├──────────────┤           │              │     │
│  │ 本地方法栈    │           ├──────────────┤     │
│  └──────────────┘           │   方法区     │     │
│                             │ (Method Area)│     │
│                             │              │     │
│                             └──────────────┘     │
└─────────────────────────────────────────────────┘
```

### 核心分类

| 区域 | 线程安全 | 作用 | 异常 |
|------|---------|------|------|
| 程序计数器 | ✅ 私有 | 记录当前线程执行的字节码行号 | 无 |
| Java 虚拟机栈 | ✅ 私有 | 存储局部变量、操作数栈等 | StackOverflowError |
| 本地方法栈 | ✅ 私有 | 为 Native 方法服务 | StackOverflowError |
| 堆 | ❌ 共享 | 存储对象实例 | OutOfMemoryError |
| 方法区 | ❌ 共享 | 存储类信息、常量、静态变量 | OutOfMemoryError |

---

## 1. 程序计数器（PC Register）

### 特点

- **线程私有**：每个线程都有独立的程序计数器
- **生命周期**：随线程创建而创建，随线程销毁而销毁
- **作用**：记录当前线程正在执行的字节码指令的地址
- **唯一不会 OOM 的区域**：因为只占很小空间

### 工作原理

```java
public void test() {
    int a = 1;    // PC 指向第1条指令
    int b = 2;    // PC 指向第2条指令
    int c = a + b; // PC 指向第3条指令
}
```

**执行流程：**
```
CPU → 读取 PC 值 → 获取指令 → 执行指令 → PC + 1 → 下一条指令
```

### 为什么需要程序计数器？

- **上下文切换**：线程被 CPU 调度时，需要记住上次执行到哪
- **分支跳转**：循环、条件判断需要修改 PC 值
- **异常处理**：捕获异常后跳转到指定位置

---

## 2. Java 虚拟机栈（JVM Stack）

### 特点

- **线程私有**：每个线程创建时同时创建一个栈
- **生命周期**：与线程相同
- **作用**：描述 Java 方法执行的内存模型
- **异常**：
  - `StackOverflowError`：栈深度超过限制（递归太深）
  - `OutOfMemoryError`：栈扩展时无法申请到足够内存

### 栈帧（Stack Frame）

每个方法执行时都会创建一个栈帧，包含：

```
┌─────────────────────────────┐
│       栈帧结构               │
├─────────────────────────────┤
│  局部变量表                  │
│  操作数栈                    │
│  动态链接                    │
│  方法返回地址                │
│  附加信息                    │
└─────────────────────────────┘
```

#### A. 局部变量表

存储方法参数和局部变量。

```java
public int add(int a, int b) {
    int c = a + b;  // a, b, c 都在局部变量表中
    return c;
}
```

**Slot（槽位）：**
- 每个变量占用一个或多个 Slot
- `int`, `float`, `reference` 占 1 个 Slot
- `long`, `double` 占 2 个 Slot

_reference 就是 Java 代码中所有非基本数据类型（即除了 int, long, double, float, boolean, char, short, byte 之外的所有类型）在局部变量表中占用的类型。它本质上是一个地址索引，用来间接访问堆里的对象。_

#### B. 操作数栈

用于字节码指令的执行。

```java
int c = a + b;
```

**执行过程：**
```
1. 将 a 压入操作数栈
2. 将 b 压入操作数栈
3. 执行加法指令，弹出两个值，计算结果
4. 将结果压入操作数栈
5. 弹出结果，存入局部变量表 c
```

#### C. 动态链接

动态链接是栈帧为了支持方法调用（尤其是多态调用）而持有的一种能力。它通过一个指向常量池的引用，让 JVM 能够在运行时动态地找到并链接到正确的方法实现，这是 Java 语言灵活性和强大多态特性的基石。

#### D. 方法返回地址

方法执行完后，该回到哪里继续执行/ 调用方法之后的下一条指令的地

---

## 3. 本地方法栈（Native Method Stack）

### 特点

- **线程私有**
- **作用**：为 JVM 使用到的 Native 方法服务
- **实现**：HotSpot 虚拟机将本地方法栈和虚拟机栈合二为一
- **异常**：`StackOverflowError`、`OutOfMemoryError`

### 什么是 Native 方法？

```java
// Java 代码
public native void start0();

// C/C++ 实现
JNIEXPORT void JNICALL Java_Thread_start0(JNIEnv *env, jobject obj) {
    // 底层系统调用
}
```

**常见 Native 方法：**
- `Object.clone()`
- `Thread.start()`
- `System.arraycopy()`

---

## 4. 堆（Heap）⭐最重要

### 特点

- **线程共享**：所有线程共享一块堆内存
- **生命周期**：JVM 启动时创建，JVM 关闭时销毁
- **作用**：存储对象实例和数组
- **异常**：`OutOfMemoryError: Java heap space`
- **GC 主要区域**：垃圾回收的主要场所

### 堆内存结构（JDK 8）

```
┌────────────────────────────────────────┐
│              堆 (Heap)                  │
├────────────────────────────────────────┤
│                                        │
│  ┌──────────────┐  ┌──────────────┐   │
│  │   新生代      │  │   老年代      │   │
│  │  (Young Gen) │  │  (Old Gen)   │   │
│  ├──────────────┤  │              │   │
│  │ Eden         │  │              │   │
│  │ Survivor S0  │  │              │   │
│  │ Survivor S1  │  │              │   │
│  └──────────────┘  └──────────────┘   │
│                                        │
│  默认比例：新生代:老年代 = 1:2          │
│  Eden:S0:S1 = 8:1:1                   │
└────────────────────────────────────────┘
```

### 对象分配流程

```
新对象创建
    ↓
Eden 区有空间？
    ↓ Yes
分配对象
    ↓
Minor GC 触发？
    ↓ Yes
存活对象 → Survivor 区
    ↓
年龄达到阈值（默认15）？
    ↓ Yes
晋升到老年代
    ↓
老年代空间不足？
    ↓ Yes
Major GC / Full GC
```

### 常见 GC 算法

| 算法 | 适用区域 | 特点 |
|------|---------|------|
| 标记-清除 | 老年代 | 产生碎片 |
| 复制算法 | 新生代 | 无碎片，浪费空间 |
| 标记-整理 | 老年代 | 无碎片，效率低 |
| 分代收集 | 整个堆 | 结合多种算法 |

### 堆参数调优

```bash
# 设置初始堆大小
-Xms512m

# 设置最大堆大小
-Xmx2g

# 设置新生代大小
-Xmn512m

# 设置堆溢出时生成 Dump 文件
-XX:+HeapDumpOnOutOfMemoryError
-XX:HeapDumpPath=/path/to/dump.hprof
```

---

## 5. 方法区（Method Area）

### 特点

- **线程共享**
- **生命周期**：JVM 启动时创建，JVM 关闭时销毁
- **作用**：存储已被虚拟机加载的类信息、常量、静态变量、即时编译器编译后的代码
- **异常**：`OutOfMemoryError: Metaspace`（JDK 8+）

### JDK 版本演变

| JDK 版本 | 实现 | 说明 |
|---------|------|------|
| JDK 6 及以前 | 永久代（PermGen） | 属于堆的一部分 |
| JDK 7 | 永久代（逐步移除） | 字符串常量池移到堆 |
| JDK 8+ | 元空间（Metaspace） | 使用本地内存，不属于堆 |

### 方法区存储内容

```
┌─────────────────────────────┐
│       方法区内容             │
├─────────────────────────────┤
│  类型信息                    │
│  - 类名                      │
│  - 父类                      │
│  - 接口                      │
│  - 字段信息                  │
│  - 方法信息                  │
│                              │
│  常量池                      │
│  - 字面量                    │
│  - 符号引用                  │
│                              │
│  静态变量                    │
│  - static 修饰的变量         │
│                              │
│  JIT 编译代码                │
└─────────────────────────────┘
```

### 元空间参数

```bash
# 设置元空间初始大小
-XX:MetaspaceSize=256m

# 设置元空间最大大小（不设置则无限制）
-XX:MaxMetaspaceSize=512m
```

---

## 6. 运行时常量池（Runtime Constant Pool）

### 特点

- **方法区的一部分**
- **线程共享**
- **作用**：存储编译期生成的各种字面量和符号引用
- **异常**：`OutOfMemoryError`

### 常量池内容

```java
public class Test {
    private final String name = "Hello";  // 字面量
    private static final int MAX = 100;   // 常量
    
    public void test() {
        String str = "World";  // 字面量
    }
}
```

**存储内容：**
- 字面量：文本字符串、final 常量值
- 符号引用：类和接口的全限定名、字段的名称和描述符、方法的名称和描述符

### 字符串常量池（String Table）

**JDK 6：** 在永久代  
**JDK 7+：** 移到堆中

```java
String s1 = "Hello";
String s2 = "Hello";
String s3 = new String("Hello");

System.out.println(s1 == s2);   // true（指向常量池同一对象）
System.out.println(s1 == s3);   // false（s3 在堆中）
System.out.println(s1 == s3.intern()); // true（intern() 返回常量池引用）
```

---

## 7. 直接内存（Direct Memory）

### 特点

- **不是 JVM 运行时数据区的一部分**
- **使用 Native 函数库直接分配堆外内存**
- **通过 DirectByteBuffer 操作**
- **异常**：`OutOfMemoryError`

### 应用场景

- NIO（New I/O）
- Netty
- 大数据框架（Spark、Flink）

### 优点

- 避免数据在 Java 堆和 Native 堆之间复制
- 提高 I/O 性能

### 缺点

- 不受 JVM 垃圾回收管理
- 需要手动释放或使用 Cleaner 机制

### 参数配置

```bash
# 设置直接内存最大值
-XX:MaxDirectMemorySize=1g
```

---

## 内存溢出案例

### 1. Java 堆溢出

```java
// 模拟堆溢出
List<byte[]> list = new ArrayList<>();
while (true) {
    list.add(new byte[1024 * 1024]);  // 每次分配 1MB
}
```

**错误信息：**
```
Exception in thread "main" java.lang.OutOfMemoryError: Java heap space
```

**解决方案：**
- 增大堆内存：`-Xmx2g`
- 检查内存泄漏
- 优化数据结构

---

### 2. 虚拟机栈溢出

```java
// 模拟栈溢出（无限递归）
public void recursive() {
    recursive();  // 无限递归
}
```

**错误信息：**
```
Exception in thread "main" java.lang.StackOverflowError
```

**解决方案：**
- 检查递归终止条件
- 改用迭代
- 增大栈大小：`-Xss2m`

---

### 3. 方法区溢出（JDK 7 及以前）

```java
// 模拟永久代溢出
while (true) {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(Test.class);
    enhancer.setUseCache(false);
    enhancer.create();  // 动态生成类
}
```

**错误信息：**
```
Exception in thread "main" java.lang.OutOfMemoryError: PermGen space
```

---

### 4. 元空间溢出（JDK 8+）

```java
// 模拟元空间溢出
while (true) {
    Enhancer enhancer = new Enhancer();
    enhancer.setSuperclass(Test.class);
    enhancer.setUseCache(false);
    enhancer.create();
}
```

**错误信息：**
```
Exception in thread "main" java.lang.OutOfMemoryError: Metaspace
```

**解决方案：**
- 增大元空间：`-XX:MaxMetaspaceSize=512m`
- 避免频繁动态生成类

---

### 5. 直接内存溢出

```java
// 模拟直接内存溢出
List<ByteBuffer> buffers = new ArrayList<>();
while (true) {
    buffers.add(ByteBuffer.allocateDirect(1024 * 1024));  // 1MB
}
```

**错误信息：**
```
Exception in thread "main" java.lang.OutOfMemoryError: Direct buffer memory
```

---

## 记忆口诀

### 🎯 口诀一：五大区域分类

```
两私三共要记牢，
程序计数栈本地，
堆和方法区共享，
溢出异常各不同。
```

**解释：**
- **两私**：程序计数器、Java 虚拟机栈、本地方法栈（线程私有）
- **三共**：堆、方法区（线程共享）
- **溢出异常**：
  - 栈：StackOverflowError
  - 堆/方法区：OutOfMemoryError
  - 程序计数器：无 OOM

---

### 🎯 口诀二：堆内存分区

```
新生老年两大块，
伊甸幸存三分家，
八一一比例要记，
十五岁老进老年。
```

**解释：**
- **新生老年**：新生代、老年代
- **伊甸幸存**：Eden、Survivor（S0、S1）
- **八一一**：Eden:S0:S1 = 8:1:1
- **十五岁**：对象年龄达到 15 岁晋升到老年代

---

### 🎯 口诀三：栈帧四要素

```
局变操动返附加，
局部变量操作数，
动态链接返回址，
方法执行靠它撑。
```

**解释：**
- **局变**：局部变量表
- **操**：操作数栈
- **动**：动态链接
- **返**：方法返回地址
- **附加**：附加信息

---

### 🎯 口诀四：JDK 版本演变

```
六七永久在堆里，
七移字符串出永，
八换元空本内取，
永久时代成历史。
```

**解释：**
- **六七**：JDK 6/7 使用永久代（PermGen），属于堆
- **七移**：JDK 7 将字符串常量池移到堆
- **八换**：JDK 8 用元空间（Metaspace）替代永久代，使用本地内存

---

### 🎯 口诀五：GC 算法选择

```
新生复制效率高，
老年标记整理好，
分代收集是主流，
G1 ZGC 是新潮。
```

**解释：**
- **新生复制**：新生代使用复制算法
- **老年标记整理**：老年代使用标记-整理算法
- **分代收集**：现代 JVM 都采用分代收集
- **G1 ZGC**：G1 GC、ZGC 是新一代垃圾收集器

---

### 🎯 口诀六：内存溢出判断

```
栈溢递归太深深，
堆溢对象太多多，
元溢类载频频频，
直接内存 NIO 搞。
```

**解释：**
- **栈溢**：递归太深导致 StackOverflowError
- **堆溢**：创建太多对象导致 Java heap space
- **元溢**：频繁加载类导致 Metaspace
- **直接内存**：NIO 使用 DirectByteBuffer 导致

---

### 🎯 口诀七：字符串常量池

```
双引常量池中找，
new 出堆里新建房，
intern 回归常量池，
等号比较地址忙。
```

**解释：**
- **双引**：`"Hello"` 在常量池中查找
- **new**：`new String("Hello")` 在堆中创建新对象
- **intern**：`str.intern()` 返回常量池中的引用
- **等号**：`==` 比较的是地址，不是内容

---

## 📊 总结对比表

| 区域 | 线程 | 存储内容 | 异常 | GC |
|------|------|---------|------|-----|
| 程序计数器 | 私有 | 字节码行号 | 无 | ❌ |
| Java 虚拟机栈 | 私有 | 栈帧（局部变量、操作数栈等） | StackOverflowError | ❌ |
| 本地方法栈 | 私有 | Native 方法 | StackOverflowError | ❌ |
| 堆 | 共享 | 对象实例、数组 | OutOfMemoryError | ✅ 主要区域 |
| 方法区 | 共享 | 类信息、常量、静态变量 | OutOfMemoryError | ✅ 少量 |
| 运行时常量池 | 共享 | 字面量、符号引用 | OutOfMemoryError | ✅ |
| 直接内存 | - | NIO 缓冲区 | OutOfMemoryError | ❌ |

---

## 💡 面试高频问题

### Q1: JVM 内存区域哪些是线程私有的？

**A:** 程序计数器、Java 虚拟机栈、本地方法栈。

---

### Q2: 堆和栈的区别？

**A:**
- **堆**：线程共享，存储对象，GC 主要区域，可能 OOM
- **栈**：线程私有，存储局部变量和方法调用，可能 StackOverflowError

---

### Q3: JDK 8 为什么要用元空间替换永久代？

**A:**
1. 永久代大小难以确定，容易 OOM
2. 元空间使用本地内存，只受系统内存限制
3. 简化 GC，永久代 GC 效率低
4. 与 JRockit 虚拟机融合

---

### Q4: 什么情况下会触发 Full GC？

**A:**
1. 老年代空间不足
2. 方法区空间不足
3. System.gc() 被调用
4. Minor GC 后晋升失败
5. 堆内存分配担保失败

---

### Q5: 如何排查 OOM 问题？

**A:**
1. 添加参数：`-XX:+HeapDumpOnOutOfMemoryError`
2. 使用 MAT、JProfiler 分析 Dump 文件
3. 查找内存泄漏点
4. 优化代码或调整 JVM 参数

---

## 🎓 学习建议

1. **理解为主**：不要死记硬背，理解每个区域的作用
2. **动手实践**：编写代码模拟各种 OOM 场景
3. **工具使用**：学会使用 jvisualvm、MAT 等工具
4. **关注演进**：了解不同 JDK 版本的内存模型变化
5. **结合实际**：在生产环境中监控和调整 JVM 参数

---

**最后提醒：** JVM 内存模型是 Java 进阶的基石，掌握它不仅有助于面试，更能帮助你写出更高效、更稳定的代码！💪
