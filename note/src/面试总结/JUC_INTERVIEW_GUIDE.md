# JUC（Java Util Concurrent）框架面试指南

***

## 目录

1. [线程池（ThreadPoolExecutor）](#1-线程池threadpoolexecutor)
2. [锁机制（ReentrantLock、ReadWriteLock）](#2-锁机制reentrantlock-readwritelock)
3. [原子类（AtomicInteger、LongAdder）](#3-原子类atomicinteger-longadder)
4. [并发集合（ConcurrentHashMap、CopyOnWriteArrayList）](#4-并发集合concurrenthashmap-copyonwritearraylist)
5. [AQS（AbstractQueuedSynchronizer）框架](#5-aqsabstractqueuedsynchronizer框架)
6. [同步工具（CountDownLatch、CyclicBarrier、Semaphore）](#6-同步工具countdownlatch-cyclicbarrier-semaphore)
7. [ThreadLocal](#7-threadlocal)
8. [CompletableFuture](#8-completablefuture)

***

## 1. 线程池（ThreadPoolExecutor）

### 1.1 工作原理

线程池的核心思想是**复用线程**，减少线程创建和销毁的开销。其执行流程如下：

```
任务提交
    │
    ▼
┌─────────────────────────────┐
│ 核心线程池是否已满？        │
└─────────────────────────────┘
    │           │
   是           否
    │           │
    ▼           ▼
┌─────────┐  直接执行
│队列满了？│
└─────────┘
    │           │
   是           否
    │           │
    ▼           ▼
┌───────────────┐  加入队列等待
│最大线程池已满？│
└───────────────┘
    │           │
   是           否
    │           │
    ▼           ▼
  拒绝策略     创建临时线程执行
```

### 1.2 核心参数

```java
public ThreadPoolExecutor(
    int corePoolSize,        // 核心线程数
    int maximumPoolSize,     // 最大线程数
    long keepAliveTime,      // 非核心线程空闲存活时间
    TimeUnit unit,           // 时间单位
    BlockingQueue<Runnable> workQueue,  // 任务队列
    ThreadFactory threadFactory,         // 线程工厂
    RejectedExecutionHandler handler     // 拒绝策略
)
```

| 参数                | 说明       | 配置建议                                      |
| ----------------- | -------- | ----------------------------------------- |
| `corePoolSize`    | 常驻核心线程数  | CPU密集型：N+1；IO密集型：2N                       |
| `maximumPoolSize` | 最大线程数    | 一般不超过2N                                   |
| `keepAliveTime`   | 空闲线程存活时间 | 根据业务场景调整，默认60s                            |
| `workQueue`       | 任务等待队列   | 常用：ArrayBlockingQueue、LinkedBlockingQueue |
| `threadFactory`   | 线程创建工厂   | 自定义可设置线程名、优先级                             |
| `handler`         | 拒绝策略     | 4种内置策略可选                                  |

### 1.3 拒绝策略

| 策略                    | 说明       | 适用场景          |
| --------------------- | -------- | ------------- |
| `AbortPolicy`         | 抛出异常（默认） | 重要任务，必须处理     |
| `CallerRunsPolicy`    | 调用者线程执行  | 并发量不大，允许调用者等待 |
| `DiscardPolicy`       | 静默丢弃     | 非重要任务         |
| `DiscardOldestPolicy` | 丢弃最老任务   | 需要最新数据的场景     |

### 1.4 常见面试题

**Q1：线程池为什么需要使用阻塞队列？**

A：阻塞队列可以实现任务的缓冲，避免频繁创建线程。当核心线程池已满时，任务先进入队列等待，队列满了才创建临时线程。

**Q2：核心线程和非核心线程有什么区别？**

A：核心线程默认不会被回收（可通过 `allowCoreThreadTimeOut(true)` 改变），非核心线程在空闲超过 `keepAliveTime` 后会被回收。

**Q3：如何合理配置线程池参数？**

A：

- CPU密集型（计算为主）：`corePoolSize = CPU核心数 + 1`
- IO密集型（读写为主）：`corePoolSize = CPU核心数 × 2`
- 混合型：根据实际测试调整

**Q4：`execute()`** **和** **`submit()`** **的区别？**

A：

- `execute()`：执行Runnable，无返回值
- `submit()`：执行Callable/Runnable，返回Future可获取结果

**Q5：线程池如何实现线程复用？**

A：线程池中的工作线程会循环从队列中获取任务执行，执行完一个任务后继续取下一个，而不是销毁。

### 1.5 实战示例

```java
// 推荐：自定义线程池
ThreadPoolExecutor executor = new ThreadPoolExecutor(
    4,                              // corePoolSize
    8,                              // maximumPoolSize
    60L, TimeUnit.SECONDS,          // keepAliveTime
    new LinkedBlockingQueue<>(100), // workQueue
    new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(0);
        @Override
        public Thread newThread(Runnable r) {
            Thread t = new Thread(r, "biz-pool-" + counter.incrementAndGet());
            t.setDaemon(false);
            t.setPriority(Thread.NORM_PRIORITY);
            return t;
        }
    },
    new ThreadPoolExecutor.CallerRunsPolicy() // 拒绝策略
);

// 使用
Future<String> future = executor.submit(() -> {
    // 业务逻辑
    return "result";
});
```

***

## 2. 锁机制（ReentrantLock、ReadWriteLock）

### 2.1 ReentrantLock

**可重入锁**，支持同一个线程多次获取同一把锁。

```java
Lock lock = new ReentrantLock();

lock.lock();
try {
    // 临界区代码
} finally {
    lock.unlock();
}
```

**核心特性**：

| 特性       | 说明                                             |
| -------- | ---------------------------------------------- |
| 可重入      | 同一线程可多次lock，需对应次数unlock                        |
| 公平锁/非公平锁 | 构造时指定 `new ReentrantLock(true)` 为公平锁           |
| 中断响应     | `lockInterruptibly()` 可被中断                     |
| 尝试获取     | `tryLock()` 尝试获取锁，立即返回；`tryLock(timeout)` 超时返回 |

**公平锁 vs 非公平锁**：

| 类型   | 特点           | 适用场景       |
| ---- | ------------ | ---------- |
| 公平锁  | 按等待顺序获取锁，无饥饿 | 线程数少，追求公平性 |
| 非公平锁 | 可能插队，吞吐量高    | 高并发，追求性能   |

### 2.2 ReadWriteLock

**读写锁**，允许多个读操作并发执行，写操作互斥。

```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();
Lock readLock = rwLock.readLock();
Lock writeLock = rwLock.writeLock();

// 读操作
readLock.lock();
try {
    // 读取数据
} finally {
    readLock.unlock();
}

// 写操作
writeLock.lock();
try {
    // 修改数据
} finally {
    writeLock.unlock();
}
```

**锁降级**：写锁可以降级为读锁（先获取写锁，再获取读锁，再释放写锁）

### 2.3 Lock vs Synchronized

| 特性    | Lock        | Synchronized |
| ----- | ----------- | ------------ |
| 可中断   | 支持          | 不支持          |
| 尝试获取  | `tryLock()` | 不支持          |
| 公平性   | 可配置         | 非公平          |
| 锁绑定条件 | `Condition` | 不支持          |
| 释放方式  | 必须手动unlock  | 自动释放         |

### 2.4 常见面试题

**Q1：ReentrantLock 如何实现可重入？**

A：通过 `state` 计数器实现。每次 lock 时 `state++`，unlock 时 `state--`，只有 state 为 0 时才真正释放锁。

**Q2：为什么需要读写锁？**

A：读多写少场景下，读写锁可以提升并发性能。读操作之间不互斥，只有写操作与其他操作互斥。

**Q3：什么是锁降级？如何实现？**

A：写锁降级为读锁：先获取写锁 → 再获取读锁 → 释放写锁 → 最后释放读锁。这样可以在写操作完成后持有读锁，防止其他线程写。

**Q4：Condition 的作用是什么？**

A：Condition 提供了更精细的线程等待/通知机制，可以实现分组唤醒，而 `wait/notify` 只能唤醒全部等待线程。

***

## 3. 原子类（AtomicInteger、LongAdder）

### 3.1 CAS 原理

**Compare-And-Swap**，无锁原子操作：

```
do {
    获取当前值 V
    计算新值 N
} while (!compareAndSet(V, N))
```

**ABA 问题**：变量从 A 变为 B 再变回 A，CAS 无法感知变化。

**解决方法**：使用 `AtomicStampedReference` 或 `AtomicMarkableReference`，增加版本号。

### 3.2 AtomicInteger

```java
AtomicInteger count = new AtomicInteger(0);

count.incrementAndGet();  // 相当于 ++i
count.decrementAndGet();  // 相当于 --i
count.getAndIncrement();  // 相当于 i++
count.addAndGet(10);      // 加法
count.compareAndSet(0, 1); // CAS操作
```

### 3.3 LongAdder

**高并发场景下性能优于 AtomicLong**，通过分段累加减少竞争。

```java
LongAdder counter = new LongAdder();

counter.increment();  // 递增
counter.add(10);      // 累加
long sum = counter.sum(); // 获取总和
```

**适用场景**：写多读少的统计场景。

### 3.4 常见面试题

**Q1：CAS 的优缺点？**

A：优点：无锁，高性能；缺点：ABA 问题、自旋开销、只能保证单个变量的原子性。

**Q2：AtomicInteger 和 LongAdder 如何选择？**

A：低并发用 AtomicInteger，高并发用 LongAdder。LongAdder 在高并发下通过分段累加减少锁竞争。

**Q3：如何解决 ABA 问题？**

A：使用 `AtomicStampedReference`，在比较时同时比较值和版本号。

**Q4：原子类是否真的无锁？**

A：是的，原子类基于 CAS 操作实现，不需要加锁，但存在自旋开销。

***

## 4. 并发集合（ConcurrentHashMap、CopyOnWriteArrayList）

### 4.1 ConcurrentHashMap

**线程安全的 HashMap**，JDK 7 和 JDK 8 实现不同。

**JDK 7**：Segment 分段锁

```
┌──────────────────────────────────────────┐
│         ConcurrentHashMap                │
├──────────┬──────────┬──────────┬──────────┤
│ Segment0 │ Segment1 │ Segment2 │ ...      │
│ (Lock0)  │ (Lock1)  │ (Lock2)  │          │
├──────────┼──────────┼──────────┼──────────┤
│ HashMap  │ HashMap  │ HashMap  │          │
└──────────┴──────────┴──────────┴──────────┘
```

**JDK 8**：CAS + synchronized 锁

```
┌──────────────────────────────────────────┐
│         ConcurrentHashMap                │
├──────────┬──────────┬──────────┬──────────┤
│  Node0   │  Node1   │  Node2   │ ...      │
│ (无锁)   │ (无锁)   │ (加锁)   │          │
└──────────┴──────────┴──────────┴──────────┘
```

| JDK 7       | JDK 8              |
| ----------- | ------------------ |
| Segment 数组  | Node 数组 + 红黑树      |
| 分段锁         | CAS + synchronized |
| 锁粒度：Segment | 锁粒度：Node（首节点）      |

### 4.2 CopyOnWriteArrayList

**写时复制**的线程安全 List。

```java
CopyOnWriteArrayList<String> list = new CopyOnWriteArrayList<>();

list.add("element");    // 写操作：复制数组，在新数组上操作
list.get(0);            // 读操作：直接读取原数组
```

**适用场景**：读多写少的场景。

### 4.3 常见面试题

**Q1：ConcurrentHashMap 如何保证线程安全？**

A：JDK 8 使用 CAS + synchronized。数组初始化用 CAS，节点写入用 synchronized 锁定首节点，扩容时用 CAS 标记。

**Q2：ConcurrentHashMap 为什么不允许 null 值？**

A：无法区分 null 是不存在还是值为 null，与 HashMap 保持一致性。

**Q3：CopyOnWriteArrayList 的优缺点？**

A：优点：读操作无锁，性能高；缺点：写操作开销大（复制数组），数据不一致（读取的可能是旧数据）。

**Q4：HashMap、HashTable、ConcurrentHashMap 的区别？**

A：

- HashMap：非线程安全，允许 null
- HashTable：线程安全，使用 synchronized，性能差
- ConcurrentHashMap：线程安全，CAS + synchronized，高性能

***

## 5. AQS（AbstractQueuedSynchronizer）框架

### 5.1 核心思想

AQS 是 JUC 的基础框架，提供了**同步状态管理**和**线程队列管理**。

**核心组件**：

| 组件              | 说明                 |
| --------------- | ------------------ |
| `state`         | 同步状态（volatile int） |
| CLH 队列          | 线程等待队列（双向链表）       |
| ConditionObject | 条件队列               |

**设计模式**：模板方法模式，子类实现以下方法：

- `tryAcquire(int)` - 尝试获取锁
- `tryRelease(int)` - 尝试释放锁
- `tryAcquireShared(int)` - 尝试获取共享锁
- `tryReleaseShared(int)` - 尝试释放共享锁

### 5.2 独占锁 vs 共享锁

| 类型  | 说明           | 实现类                         |
| --- | ------------ | --------------------------- |
| 独占锁 | 同一时刻只有一个线程持有 | ReentrantLock               |
| 共享锁 | 多个线程可同时持有    | ReadWriteLock（读锁）、Semaphore |

### 5.3 源码分析

**acquire() 方法流程**：

```java
public final void acquire(int arg) {
    if (!tryAcquire(arg) &&
        acquireQueued(addWaiter(Node.EXCLUSIVE), arg))
        selfInterrupt();
}
```

1. `tryAcquire()` 尝试获取锁（子类实现）
2. 获取失败，`addWaiter()` 将线程加入等待队列
3. `acquireQueued()` 自旋等待获取锁
4. 如果被中断，`selfInterrupt()` 自我中断

### 5.4 常见面试题

**Q1：AQS 的核心数据结构是什么？**

A：CLH 队列（双向链表），每个节点**包含线程引用、状态、前驱**/后继指针。

**Q2：AQS 如何实现锁的公平性？**

A：通过 `hasQueuedPredecessors()` 判断是否有等待更久的线程，公平锁必须等待队列中的线程先获取。

**Q3：ReentrantLock 如何基于 AQS 实现？**

A：继承 AQS，重写 `tryAcquire()` 和 `tryRelease()`，通过 state 计数器实现可重入。

**Q4：AQS 的 Condition 如何工作？**

A：Condition 维护一个等待队列，`await()` 将线程加入条件队列，`signal()` 将线程移到同步队列。

***

## 6. 同步工具（CountDownLatch、CyclicBarrier、Semaphore）

### 6.1 CountDownLatch

**倒计时器**，等待多个线程完成后再执行。

```java
// 主线程等待所有子线程完成
CountDownLatch latch = new CountDownLatch(3);

for (int i = 0; i < 3; i++) {
    executor.execute(() -> {
        try {
            // 任务执行
        } finally {
            latch.countDown(); // 计数减1
        }
    });
}

latch.await(); // 等待计数归0
```

**适用场景**：主线程等待多个子任务完成后汇总结果。

### 6.2 CyclicBarrier

**循环屏障**，多个线程互相等待，到达屏障点后继续执行。

```java
CyclicBarrier barrier = new CyclicBarrier(3, () -> {
    // 所有线程到达后执行的任务
});

for (int i = 0; i < 3; i++) {
    executor.execute(() -> {
        // 准备工作
        barrier.await(); // 等待其他线程
        // 继续执行
    });
}
```

**适用场景**：多阶段任务，每个阶段需要所有线程同步后才能进入下一阶段。

### 6.3 Semaphore

**信号量**，控制同时访问资源的线程数。

```java
Semaphore semaphore = new Semaphore(5); // 允许5个线程同时访问

semaphore.acquire(); // 获取许可
try {
    // 访问受限资源
} finally {
    semaphore.release(); // 释放许可
}
```

**适用场景**：限流、资源池控制。

### 6.4 区别对比

| 工具             | 核心特点    | 能否重复使用   | 等待方      |
| -------------- | ------- | -------- | -------- |
| CountDownLatch | 一个线程等多个 | 否        | 一个线程等待   |
| CyclicBarrier  | 多个线程互相等 | 是（reset） | 所有线程互相等待 |
| Semaphore      | 控制并发数量  | 是        | 获取许可的线程  |

### 6.5 常见面试题

**Q1：CountDownLatch 和 CyclicBarrier 的区别？**

A：

- CountDownLatch：一个线程等待其他线程完成，不可复用
- CyclicBarrier：多个线程互相等待，可复用（reset）

**Q2：Semaphore 如何实现限流？**

A：设置许可数量为限流阈值，每次请求 acquire() 获取许可，处理完成后 release() 释放。

**Q3：如何实现线程间的同步？**

A：

- 等待其他线程完成：CountDownLatch
- 多阶段同步：CyclicBarrier
- 控制并发数：Semaphore

***

## 7. ThreadLocal

### 7.1 实现原理

**线程本地变量**，每个线程拥有独立的变量副本。

```java
ThreadLocal<String> threadLocal = new ThreadLocal<>();

threadLocal.set("value"); // 存储到当前线程的 ThreadLocalMap
String value = threadLocal.get(); // 从当前线程的 ThreadLocalMap 获取
threadLocal.remove(); // 移除
```

**数据结构**：

```
Thread
    │
    ├── ThreadLocalMap
    │       │
    │       └── Entry[] (key: ThreadLocal, value: Object)
```

### 7.2 内存泄漏问题

**问题原因**：ThreadLocalMap 的 Entry 使用弱引用（WeakReference）指向 ThreadLocal，但 value 是强引用。当 ThreadLocal 被 GC 回收后，key 变为 null，value 无法被回收，造成内存泄漏。

**解决方案**：

```java
// 方式1：使用后及时remove
try {
    threadLocal.set(value);
    // 业务逻辑
} finally {
    threadLocal.remove(); // 必须手动清理
}

// 方式2：使用 ThreadLocal.withInitial()
ThreadLocal<String> tl = ThreadLocal.withInitial(() -> "default");
```

### 7.3 常见面试题

**Q1：ThreadLocal 的实现原理？**

A：每个 Thread 持有 ThreadLocalMap，ThreadLocalMap 使用 ThreadLocal 作为 key（弱引用），存储线程本地变量。

**Q2：为什么会内存泄漏？如何解决？**

A：Entry 的 key 是弱引用，ThreadLocal 被回收后 key 变为 null，但 value 仍是强引用。解决：使用后调用 `remove()`。

**Q3：ThreadLocal 和 synchronized 的区别？**

A：synchronized 是共享变量加锁，ThreadLocal 是每个线程独立变量，不需要锁。

**Q4：InheritableThreadLocal 的作用？**

A：允许子线程继承父线程的 ThreadLocal 值。

***

## 8. CompletableFuture

### 8.1 异步编程模型

**JDK 8 引入的异步编程工具**，支持链式调用和组合操作。

```java
// 创建异步任务
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    // 异步执行
    return "result";
});

// 链式调用
future.thenApply(result -> result.toUpperCase())
      .thenAccept(result -> System.out.println(result))
      .thenRun(() -> System.out.println("done"));
```

### 8.2 核心方法

| 方法              | 说明          | 返回值                       |
| --------------- | ----------- | ------------------------- |
| `supplyAsync()` | 异步执行，有返回值   | CompletableFuture<T>      |
| `runAsync()`    | 异步执行，无返回值   | CompletableFuture<Void>   |
| `thenApply()`   | 处理结果，有返回值   | CompletableFuture<U>      |
| `thenAccept()`  | 处理结果，无返回值   | CompletableFuture<Void>   |
| `thenRun()`     | 结果后执行       | CompletableFuture<Void>   |
| `thenCompose()` | 组合两个 Future | CompletableFuture<U>      |
| `thenCombine()` | 合并两个结果      | CompletableFuture<V>      |
| `allOf()`       | 等待所有任务完成    | CompletableFuture<Void>   |
| `anyOf()`       | 等待任一任务完成    | CompletableFuture<Object> |

### 8.3 异常处理

```java
CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
    throw new RuntimeException("error");
});

// 方式1：exceptionally
future.exceptionally(e -> {
    System.err.println("Exception: " + e.getMessage());
    return "default";
});

// 方式2：handle
future.handle((result, e) -> {
    if (e != null) {
        return "default";
    }
    return result;
});
```

### 8.4 实战示例

```java
// 并行执行多个任务，汇总结果
CompletableFuture<String> task1 = CompletableFuture.supplyAsync(() -> "result1");
CompletableFuture<String> task2 = CompletableFuture.supplyAsync(() -> "result2");
CompletableFuture<String> task3 = CompletableFuture.supplyAsync(() -> "result3");

CompletableFuture<Void> allTasks = CompletableFuture.allOf(task1, task2, task3);

allTasks.thenRun(() -> {
    String r1 = task1.join();
    String r2 = task2.join();
    String r3 = task3.join();
    System.out.println("All done: " + r1 + ", " + r2 + ", " + r3);
});
```

### 8.5 常见面试题

**Q1：CompletableFuture 和 Future 的区别？**

A：Future 只能获取结果，CompletableFuture 支持链式调用、组合操作、异常处理。

**Q2：如何实现多个异步任务的并行执行和结果汇总？**

A：使用 `CompletableFuture.allOf()` 等待所有任务完成，然后通过 `join()` 获取每个任务的结果。

**Q3：`thenApply()`** **和** **`thenCompose()`** **的区别？**

A：`thenApply()` 转换结果，`thenCompose()` 组合两个 CompletableFuture（类似 flatMap）。

**Q4：如何自定义线程池执行 CompletableFuture？**

A：使用 `supplyAsync(supplier, executor)` 指定线程池。

***

## 总结

### 核心知识点梳理

```
JUC 框架
├── 线程池（ThreadPoolExecutor）
│   ├── 核心参数与配置
│   ├── 工作原理与拒绝策略
│   └── 实战应用
├── 锁机制
│   ├── ReentrantLock（可重入、公平/非公平）
│   ├── ReadWriteLock（读写分离、锁降级）
│   └── Lock vs Synchronized
├── 原子类
│   ├── CAS 原理与 ABA 问题
│   ├── AtomicInteger/AtomicLong
│   └── LongAdder（高并发优化）
├── 并发集合
│   ├── ConcurrentHashMap（JDK 7/8 实现差异）
│   ├── CopyOnWriteArrayList（写时复制）
│   └── 使用场景与注意事项
├── AQS 框架
│   ├── 核心思想（state + CLH队列）
│   ├── 模板方法模式
│   └── 独占锁 vs 共享锁
├── 同步工具
│   ├── CountDownLatch（倒计时）
│   ├── CyclicBarrier（循环屏障）
│   └── Semaphore（信号量）
├── ThreadLocal
│   ├── 实现原理
│   ├── 内存泄漏问题
│   └── 最佳实践
└── CompletableFuture
    ├── 异步编程模型
    ├── 链式调用与组合
    └── 异常处理
```

### 面试回答策略

1. **先讲原理，再讲应用**：从底层原理讲起，再讲实际使用场景
2. **结合源码**：适当提及关键源码，展示深度理解
3. **对比分析**：横向对比相似组件（如 CountDownLatch vs CyclicBarrier）
4. **避坑指南**：说明使用注意事项和常见错误
5. **实战经验**：结合实际项目经验说明如何选型和配置

### 推荐学习路径

1. **基础篇**：线程池、原子类、并发集合
2. **进阶篇**：AQS 框架、锁机制
3. **高级篇**：CompletableFuture、源码分析
4. **实战篇**：结合业务场景设计并发方案

