# Java 并发集合面试总结

> 全面覆盖并发集合的实现原理、使用场景、性能对比及实战应用

---

## 目录

- [1. 并发集合概述](#1-并发集合概述)
- [2. 核心实现原理](#2-核心实现原理)
- [3. 主要并发集合详解](#3-主要并发集合详解)
- [4. 适用场景与性能对比](#4-适用场景与性能对比)
- [5. 使用注意事项与陷阱](#5-使用注意事项与陷阱)
- [6. 高频面试题解析](#6-高频面试题解析)
- [7. 实战项目示例](#7-实战项目示例)

---

## 1. 并发集合概述

### 1.1 定义与重要性

#### 什么是并发集合？

并发集合是 Java 提供的**线程安全**的集合类，能够在多线程环境下安全地进行读写操作，无需外部同步。

#### 为什么需要并发集合？

```
普通集合在多线程下的问题:

HashMap: 线程不安全
  → 并发 put 可能导致死循环（JDK7）或数据丢失
  → 例子: 两个线程同时扩容，链表成环导致死循环

ArrayList: 线程不安全
  → 并发 add 可能导致数组越界或数据覆盖
  → 例子: 两个线程同时修改 size 字段

HashSet: 线程不安全
  → 内部使用 HashMap，同样存在问题
```

#### 并发集合 vs 普通集合

| 特性 | 普通集合 | 并发集合 |
|------|----------|----------|
| 线程安全 | ❌ 不安全 | ✅ 安全 |
| 性能 | 高（单线程） | 较高（优化锁策略） |
| 一致性 | 一致 | 弱一致（迭代器） |
| 使用场景 | 单线程 | 多线程 |

### 1.2 Java 并发集合框架

```
┌─────────────────────────────────────────────────────────────────────┐
│                     Java 并发集合体系结构                              │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  java.util.concurrent 包:                                           │
│  ├── Map 体系                                                       │
│  │   ├── ConcurrentHashMap      ← 最常用                           │
│  │   ├── ConcurrentSkipListMap  ← 有序 Map                         │
│  │   └── ConcurrentNavigableMap ← 可导航 Map                       │
│  │                                                                 │
│  ├── List 体系                                                      │
│  │   └── CopyOnWriteArrayList   ← 写时复制                         │
│  │                                                                 │
│  ├── Set 体系                                                       │
│  │   ├── CopyOnWriteArraySet    ← 基于 CopyOnWriteArrayList       │
│  │   └── ConcurrentSkipListSet  ← 基于 ConcurrentSkipListMap      │
│  │                                                                 │
│  ├── Queue 体系                                                     │
│  │   ├── ConcurrentLinkedQueue  ← 无锁队列                        │
│  │   ├── ConcurrentLinkedDeque   ← 无锁双端队列                    │
│  │   ├── BlockingQueue 家族                                       │
│  │   │   ├── ArrayBlockingQueue  ← 数组实现                        │
│  │   │   ├── LinkedBlockingQueue ← 链表实现                        │
│  │   │   ├── PriorityBlockingQueue ← 优先级队列                   │
│  │   │   ├── SynchronousQueue    ← 同步队列                       │
│  │   │   └── DelayQueue          ← 延迟队列                       │
│  │   └── TransferQueue 家族                                       │
│  │                                                                 │
│  └── 其他                                                           │
│      ├── ConcurrentHashMap.KeySetView                              │
│      └── Arrays.asList → Collections.synchronizedList              │
│                                                                     │
└─────────────────────────────────────────────────────────────────────┘
```

---

## 2. 核心实现原理

### 2.1 锁机制演变

#### 从 HashTable 到 ConcurrentHashMap

```
HashTable (JDK 1.0):
  ┌─────────────────────────────────────────────┐
  │  synchronized 全表锁                         │
  │  ┌─────────────────────────────────────────┐ │
  │  │ 整个 Map 一把大锁                        │ │
  │  │ put → 获取全局锁 → 操作 → 释放锁        │ │
  │  │ get → 获取全局锁 → 读取 → 释放锁        │ │
  │  └─────────────────────────────────────────┘ │
  │ 缺点: 任何操作都阻塞，吞吐量低                │
  └─────────────────────────────────────────────┘

ConcurrentHashMap JDK 1.7:
  ┌─────────────────────────────────────────────┐
  │ 分段锁 (Segment)                             │
  │  ┌─────┐ ┌─────┐ ┌─────┐     ┌─────┐      │
  │  │Seg 0│ │Seg 1│ │Seg 2│ ... │Seg 15│     │
  │  │ 锁  │ │ 锁  │ │ 锁  │     │ 锁  │      │
  │  └─────┘ └─────┘ └─────┘     └─────┘      │
  │  每个 Segment 独立加锁                         │
  │  最多 16 个并发写操作                          │
  └─────────────────────────────────────────────┘

ConcurrentHashMap JDK 1.8:
  ┌─────────────────────────────────────────────┐
  │ CAS + synchronized                           │
  │  ┌──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┬──┐    │
  │  │0 │1 │2 │3 │4 │5 │6 │7 │8 │9 │..│n │    │
  │  └──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┴──┘    │
  │  桶 (Node[] table)                           │
  │                                               │
  │  put 操作:                                    │
  │  1. 定位桶位置                                │
  │  2. 桶为空 → CAS 写入（无锁）                │
  │  3. 桶非空 → synchronized 锁住链表头节点      │
  │  4. 链表 → 遍历插入；树 → 红黑树插入         │
  │                                               │
  │  优点: 锁粒度更细，读操作无锁                 │
  └─────────────────────────────────────────────┘
```

### 2.2 CAS 无锁操作

#### CAS (Compare-And-Swap) 原理

```
CAS 操作流程:
  ┌─────────────────────────────────────────┐
  │                                         │
  │  1. 读取内存位置 V 的值                  │
  │  2. 比较 V 与预期值 A 是否相等           │
  │  3. 如果相等: V = B (新值)，返回成功     │
  │  4. 如果不等: 不修改，返回失败           │
  │                                         │
  │  伪代码:                                 │
  │  boolean CAS(V, A, B) {                  │
  │    if (V == A) {                         │
  │      V = B;                              │
  │      return true;                        │
  │    }                                     │
  │    return false;                         │
  │  }                                       │
  │                                         │
  │  Java 实现:                              │
  │  Unsafe.compareAndSwapObject()           │
  │  → 调用 CPU 原子指令 (cmpxchg)           │
  │                                         │
  └─────────────────────────────────────────┘
```

#### CAS 在并发集合中的应用

```java
// ConcurrentHashMap 初始化桶数组
private static final <K,V> Node<K,V>[] resizeStamp(int size) {
    // CAS 初始化
    return (Node<K,V>[])new Node<?,?>[size];
}

// 尝试 CAS 写入空桶
static final <K,V> boolean casTabAt(Node<K,V>[] tab, int i, Node<K,V> c, Node<K,V> v) {
    return U.compareAndSwapObject(tab, ((long)i << ASHIFT) + ABASE, c, v);
}

// 自旋重试
for (Node<K,V>[] tab;;) {
    // ...
    if (casTabAt(tab, i, null, new Node<>(hash, key, value)))
        break;  // CAS 成功
    // 继续重试
}
```

### 2.3 volatile 与可见性

#### Java 内存模型 (JMM)

```
┌─────────────────────────────────────────────────────────────┐
│                    Java 内存模型                               │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  线程1              主内存              线程2                │
│  ┌──────┐           ┌──────┐           ┌──────┐             │
│  │工作内存│  读写    │主内存 │  读写    │工作内存│             │
│  │      │◄────────►│      │◄────────►│      │             │
│  └──────┘           └──────┘           └──────┘             │
│                                                             │
│  volatile 作用:                                              │
│  1. 保证可见性: 一个线程修改立即对其他线程可见               │
│  2. 禁止指令重排序: 保证执行顺序                              │
│                                                             │
│  ConcurrentHashMap 中的 volatile:                            │
│  - Node.val: volatile 保证值的可见性                        │
│  - Node.next: volatile 保证链表结构的可见性                  │
│  - sizeCtl: volatile 控制扩容状态                           │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

---

## 3. 主要并发集合详解

### 3.1 ConcurrentHashMap（重点）

#### JDK 1.8 数据结构

```
┌─────────────────────────────────────────────────────────────┐
│              ConcurrentHashMap JDK 1.8 结构                   │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  table (Node[] 数组，默认 16 容量)                           │
│  ┌───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┬───┐          │
│  │ 0 │ 1 │ 2 │ 3 │ 4 │ 5 │ 6 │ 7 │ 8 │ 9 │...│n │          │
│  └─┬─┴─┬─┴───┴───┴───┴─┬─┴─┬─┴─┬─┴───┴───┴───┴───┴───┘          │
│    │    │               │   │   │                           │
│    ▼    ▼               ▼   ▼   ▼                           │
│  ┌─────┐┌─────┐     ┌─────┐│  ┌─────┐                      │
│  │Node ││Node │     │Node ││  │Node │                      │
│  │hash ││hash │     │hash ││  │hash │                      │
│  │key  ││key  │     │key  ││  │key  │                      │
│  │val  ││val  │     │val  ││  │val  │                      │
│  │next ││next │     │next ││  │next │                      │
│  └──┬──┘└──┬──┘     └──┬──┘│  └──┬──┘                      │
│     │      │          │   │     │                           │
│     ▼      ▼          ▼   ▼     ▼                           │
│  ┌─────┐┌─────┐     ┌─────┐│  ┌─────┐                      │
│  │Node ││Node │     │Node ││  │TreeBin│ ← 红黑树           │
│  └─────┘└─────┘     └─────┘│  └─────┘                      │
│                             │     │                          │
│  链表 → 树转换:             │     ▼                          │
│  当链表长度 >= 8 且 table   │  ┌─────┐                      │
│  长度 >= 64 时转换          │  │TreeNode│                    │
│                             │  └─────┘                      │
│  红黑树 → 链表转换:          │     │                          │
│  当红黑树节点 <= 6 时转回    │     ▼                          │
│                             │  ┌─────┐                      │
│                             │  │TreeNode│                    │
│                             │  └─────┘                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  sizeCtl 控制字段                                    │   │
│  │  -1: 正在初始化                                      │   │
│  │  -N: 正在扩容（N 个线程参与）                        │   │
│  │  0: 未初始化                                          │   │
│  │  >0: 阈值 (容量 * 负载因子)                          │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### put 操作源码解析

```java
final V putVal(K key, V value, boolean onlyIfAbsent) {
    if (key == null || value == null) throw new NullPointerException();
    
    // 1. 计算 hash
    int hash = spread(key.hashCode());
    int binCount = 0;
    
    // 2. 自旋循环
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        
        // 3. 表未初始化 → 初始化
        if (tab == null)
            tab = initTable();
        
        // 4. 计算桶位置，桶为空 → CAS 写入
        else if ((f = tabAt(tab, i = (n-1) & hash)) == null) {
            if (casTabAt(tab, i, null, new Node<>(hash, key, value)))
                break;  // CAS 成功，退出
        }
        
        // 5. 桶的 hash == MOVED → 正在扩容，协助扩容
        else if ((fh = f.hash) == MOVED)
            tab = helpTransfer(tab, f);
        
        // 6. 桶非空 → synchronized 锁住头节点
        else {
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    // 7. 链表情况
                    if (fh >= 0) {
                        binCount = 1;
                        // 遍历链表
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                            if (e.hash == hash && ((ek = e.key) == key || (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    e.val = value;  // 更新值
                                break;
                            }
                            Node<K,V> pred = e;
                            if ((e = e.next) == null) {
                                pred.next = new Node<>(hash, key, value);  // 尾插
                                break;
                            }
                        }
                    }
                    // 8. 红黑树情况
                    else if (f instanceof TreeBin) {
                        Node<K,V> p;
                        binCount = 2;
                        if ((p = ((TreeBin<K,V>)f).putVal(hash, key, value)) != null) {
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            
            // 9. 链表长度超过阈值 → 转树或扩容
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD)
                    treeifyBin(tab, i);  // 链表转红黑树
                break;
            }
        }
    }
    
    // 10. 更新 size 计数
    if (binCount >= 0)
        addCount(1L, binCount);
    return oldVal;
}
```

#### get 操作源码解析（无锁读）

```java
public V get(Object key) {
    Node<K,V>[] tab; Node<K,V> e, p; int n, eh; K ek;
    int h = spread(key.hashCode());
    
    // 1. 表非空且桶非空
    if ((tab = table) != null && (n = tab.length) > 0 &&
        (e = tabAt(tab, (n-1) & h)) != null) {
        
        // 2. 头节点直接命中
        if ((eh = e.hash) == h) {
            if ((ek = e.key) == key || (ek != null && key.equals(ek)))
                return e.val;  // volatile 读 val
        }
        // 3. 红黑树查找
        else if (eh < 0)
            return (p = e.find(h, key)) != null ? p.val : null;
        
        // 4. 链表遍历查找
        while ((e = e.next) != null) {
            if (e.hash == h &&
                ((ek = e.key) == key || (ek != null && key.equals(ek))))
                return e.val;  // volatile 读 val
        }
    }
    return null;
}

// tabAt 使用 volatile 读
static final <K,V> Node<K,V> tabAt(Node<K,V>[] tab, int i) {
    return (Node<K,V>)U.getObjectVolatile(tab, ((long)i << ASHIFT) + ABASE);
}
```

**无锁读核心原理**：
1. `table` 数组引用是 `volatile`，保证可见性
2. `Node.val` 是 `volatile`，保证值的可见性
3. `Node.next` 是 `volatile`，保证链表结构的可见性
4. `tabAt()` 使用 `U.getObjectVolatile()` 读取桶元素
5. 整个读取过程**无需加锁**，直接基于 volatile 语义保证一致性

#### 扩容机制与 ForwardingNode

```
┌─────────────────────────────────────────────────────────────┐
│              ConcurrentHashMap 多线程扩容机制                  │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  扩容触发: size > threshold (容量 * 0.75)                   │
│                                                             │
│  ForwardingNode 特殊节点:                                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ hash = MOVED (-1)                                    │   │
│  │ nextTable 指向新数组                                  │   │
│  │ 作用: 标记桶已迁移，引导其他线程协助扩容               │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  扩容流程:                                                   │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 1. 触发扩容的线程:                                    │   │
│  │    - 创建新数组 (容量翻倍)                            │   │
│  │    - 设置 sizeCtl = -1 (标记初始化中)                 │   │
│  │    - 迁移第一个桶，放置 ForwardingNode                │   │
│  │                                                       │   │
│  │ 2. 其他线程 put/get 时发现 ForwardingNode:            │   │
│  │    - 调用 helpTransfer() 协助迁移                    │   │
│  │    - 每个线程负责一段桶区间                           │   │
│  │    - 更新 sizeCtl 计数参与线程数                      │   │
│  │                                                       │   │
│  │ 3. 迁移完成:                                          │   │
│  │    - 所有桶都迁移完毕                                 │   │
│  │    - 切换 table 引用到新数组                          │   │
│  │    - sizeCtl 重新设置为阈值                           │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  helpTransfer 关键逻辑:                                      │
│  - 检查 sizeCtl < 0 → 正在扩容                              │
│  - 计算当前线程负责的桶区间 (stride)                         │
│  - 迁移分配到的桶                                           │
│  - 迁移完自己的区间后退出                                    │
│                                                             │
│  好处:                                                       │
│  - 避免单线程扩容成为瓶颈                                   │
│  - 利用多线程并行迁移，提升扩容效率                         │
│  - 扩容期间其他线程可以继续操作已迁移的桶                    │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### JDK 1.7 vs JDK 1.8 对比

| 特性 | JDK 1.7 | JDK 1.8 |
|------|---------|---------|
| 锁机制 | 分段锁（Segment） | CAS + synchronized |
| 锁粒度 | Segment 级别（16 段） | Node 桶级别 |
| 并发度 | 最多 16 并发写 | 理论上无限 |
| 读操作 | 需获取 Segment 锁 | 无锁（volatile 读） |
| 数据结构 | Segment[] + HashEntry[] | Node[] + 链表/红黑树 |
| 树结构 | 无 | 红黑树（链表≥8） |
| 扩容 | 每个 Segment 独立扩容 | 多线程协助扩容 |
| 实现复杂度 | 较复杂 | 更复杂但更高效 |

---

### 3.2 CopyOnWriteArrayList

#### 实现原理

```
┌─────────────────────────────────────────────────────────────┐
│              CopyOnWriteArrayList 原理                        │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  写操作 (add/set/remove):                                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 1. 获取 ReentrantLock 锁                              │   │
│  │ 2. 获取当前数组快照                                   │   │
│  │ 3. 创建新数组 (复制 + 修改)                           │   │
│  │ 4. 替换数组引用 (setArray)                            │   │
│  │ 5. 释放锁                                             │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  读操作 (get/iterator):                                      │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ 1. 获取当前数组引用 (volatile 读)                     │   │
│  │ 2. 直接读取，无需加锁                                 │   │
│  │ 3. 基于快照读取，不受写操作影响                       │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  示例:                                                      │
│  初始: array = [A, B, C]                                    │
│  线程1: add(D)                                               │
│    → 复制 [A, B, C] → [A, B, C, D]                         │
│    → array 指向新数组                                       │
│  线程2: get(2)                                               │
│    → 可能读到旧数组 [A, B, C] 或新数组 [A, B, C, D]         │
│    → 保证最终一致性（弱一致性）                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 核心源码

```java
public class CopyOnWriteArrayList<E> {
    private final transient ReentrantLock lock = new ReentrantLock();
    private transient volatile Object[] array;  // volatile 保证可见性
    
    // 写操作 - 加锁 + 复制
    public boolean add(E e) {
        final ReentrantLock lock = this.lock;
        lock.lock();
        try {
            Object[] elements = getArray();
            int len = elements.length;
            // 复制 + 扩容
            Object[] newElements = Arrays.copyOf(elements, len + 1);
            newElements[len] = e;
            // 替换数组引用
            setArray(newElements);
            return true;
        } finally {
            lock.unlock();
        }
    }
    
    // 读操作 - 无锁
    public E get(int index) {
        return getArray()[index];  // volatile 读
    }
    
    public Iterator<E> iterator() {
        return new COWIterator<>(getArray(), 0);  // 基于快照
    }
}
```

#### 特性总结

| 优点 | 缺点 |
|------|------|
| 读操作无锁，性能极高 | 写操作开销大（复制整个数组） |
| 实现简单，易于理解 | 内存占用高（每次写创建新数组） |
| 迭代器安全（基于快照，不会 ConcurrentModificationException） | 不适合大量写操作 |
| 最终一致性，无需同步读 | 内存一致性问题（写后读可能不一致） |

---

### 3.3 ConcurrentLinkedQueue

#### 实现原理

```
┌─────────────────────────────────────────────────────────────┐
│              ConcurrentLinkedQueue 原理                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  核心: CAS + 链表 + 无锁                                      │
│                                                             │
│  数据结构:                                                   │
│  ┌───┐    ┌───┐    ┌───┐    ┌───┐    ┌───┐               │
│  │ H │───►│ 1 │───►│ 2 │───►│ 3 │───►│ T │               │
│  └───┘    └───┘    └───┘    └───┘    └───┘               │
│  head                          tail                         │
│                                                             │
│  offer (入队) 操作:                                          │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ for (;;) {                                           │   │
│  │   Node<E> t = tail;                                  │   │
│  │   Node<E> next = t.next;                            │   │
│  │   if (t != tail) continue;  // tail 被其他线程更新    │   │
│  │   if (next != null) {                                  │   │
│  │     // tail 落后，CAS 更新 tail                       │   │
│  │     CAS(tail, t, next);                               │   │
│  │   } else {                                            │   │
│  │     Node<E> newNode = new Node<>(e);                  │   │
│  │     // CAS 设置 tail.next                             │   │
│  │     if (CAS(t.next, null, newNode)) {                 │   │
│  │       // 成功，CAS 更新 tail                          │   │
│  │       CAS(tail, t, newNode);                          │   │
│  │       return true;                                    │   │
│  │     }                                                 │   │
│  │     // CAS 失败，重试                                 │   │
│  │   }                                                   │   │
│  │ }                                                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
│  poll (出队) 操作:                                           │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ for (;;) {                                           │   │
│  │   Node<E> h = head;                                  │   │
│  │   Node<E> t = tail;                                  │   │
│  │   Node<E> next = h.next;                             │   │
│  │   if (h != head) continue;  // head 被更新            │   │
│  │   if (next == null) return null;  // 队列空          │   │
│  │   if (h == t) {  // head 和 tail 重合                 │   │
│  │     // tail 落后，CAS 推进 tail                       │   │
│  │     CAS(tail, t, next);                               │   │
│  │   } else {                                            │   │
│  │     E item = next.item;                               │   │
│  │     if (CAS(next.item, item, null)) {                 │   │
│  │       // CAS 更新 head                                │   │
│  │       CAS(head, h, next);                             │   │
│  │       return item;                                    │   │
│  │     }                                                 │   │
│  │   }                                                   │   │
│  │ }                                                     │   │
│  └─────────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 核心源码

```java
public class ConcurrentLinkedQueue<E> extends AbstractQueue<E> {
    
    private transient volatile Node<E> head;
    private transient volatile Node<E> tail;
    
    // CAS 更新 tail (使用 Unsafe.compareAndSwapObject)
    private static final Unsafe U = Unsafe.getUnsafe();
    private static final long TAIL_OFFSET;
    
    static {
        TAIL_OFFSET = U.objectFieldOffset(ConcurrentLinkedQueue.class.getDeclaredField("tail"));
    }
    
    // CAS 原子更新 tail
    private boolean casTail(Node<E> expect, Node<E> update) {
        return U.compareAndSwapObject(this, TAIL_OFFSET, expect, update);
    }
    
    // 入队
    public boolean offer(E e) {
        checkNotNull(e);
        final Node<E> newNode = new Node<>(e);
        
        for (Node<E> t = tail, p = t;;) {
            Node<E> q = p.next;
            if (q == null) {
                // p 是最后一个节点，CAS 插入新节点
                if (p.casNext(null, newNode)) {
                    // CAS 成功
                    if (p != t)  // 队列长度 > 1，推进 tail
                        casTail(t, newNode);
                    return true;
                }
                // CAS 失败，重试
            } else if (p == q) {
                // p 被标记删除 (自环)，从头开始
                p = (t != (t = tail)) ? t : head;
            } else {
                // 推进 p 到下一个节点
                p = (p != t && t != (t = tail)) ? t : q;
            }
        }
    }
    
    // 出队
    public E poll() {
        restartFromHead:
        for (;;) {
            for (Node<E> h = head, p = h, q;;) {
                E item = p.item;
                
                // CAS 标记删除 item (原子置 null)
                if (item != null && p.casItem(item, null)) {
                    // 成功出队
                    if (p != h)  // 推进 head
                        updateHead(h, ((q = p.next) != null) ? q : p);
                    return item;
                }
                else if ((q = p.next) == null) {
                    // 队列空
                    updateHead(h, p);
                    return null;
                }
                else if (p == q) {
                    // head 被标记删除，重新开始
                    continue restartFromHead;
                } else {
                    p = q;  // 推进 p
                }
            }
        }
    }
    
    // CAS 更新 head (volatile 写)
    private void updateHead(Node<E> oldHead, Node<E> newHead) {
        head = newHead;  // volatile 写保证可见性
    }
    
    // Node 内部类
    static class Node<E> {
        volatile E item;
        volatile Node<E> next;
        
        Node(E item) {
            this.item = item;
        }
        
        boolean casItem(E expect, E update) {
            return U.compareAndSwapObject(this, ITEM_OFFSET, expect, update);
        }
        
        boolean casNext(Node<E> expect, Node<E> update) {
            return U.compareAndSwapObject(this, NEXT_OFFSET, expect, update);
        }
    }
}
```

#### 特性总结

| 优点 | 缺点 |
|------|------|
| 完全无锁，高并发性能优异 | 实现复杂，难以理解 |
| 无阻塞，高吞吐量 | 调试困难 |
| 可用于生产者-消费者模式 | 不支持 null 元素 |
| 弱一致性（size() 不精确） | 批量操作效率较低 |

---

### 3.4 ConcurrentSkipListMap

#### 跳表结构

```
┌─────────────────────────────────────────────────────────────┐
│              ConcurrentSkipListMap 跳表结构                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  Level 3 (最高):  ┌───┐                                    │
│                   │HEAD│────────────────────────────────►   │
│                   └─┬─┘                                    │
│                     │                                       │
│  Level 2:       ┌───┴───┐  ┌───┐                           │
│                 │  HEAD  │─►│ 25 │──────────────────►      │
│                 └───┬───┘  └─┬─┘                           │
│                     │        │                              │
│  Level 1:       ┌───┴───┐  ┌─┴─┐  ┌───┐  ┌───┐           │
│                 │  HEAD  │─►│ 5 │─►│ 25 │─►│ 50 │─►       │
│                 └───┬───┘  └─┬─┘  └─┬─┘  └─┬─┘           │
│                     │        │        │        │            │
│  Level 0:       ┌───┴───┐  ┌─┴─┐  ┌─┴─┐  ┌─┴─┐  ┌───┐   │
│                 │  HEAD  │─►│ 5 │─►│ 25 │─►│ 50 │─►│ 75 │  │
│                 └────────┘  └───┘  └───┘  └───┘  └───┘   │
│                                                             │
│  查找过程 (查找 50):                                         │
│  1. 从最高层开始: HEAD → 25 (25 < 50, 继续) → null         │
│  2. 下降一层: HEAD → 25 → 50 (找到!)                       │
│                                                             │
│  时间复杂度: O(log n)                                        │
│  空间复杂度: O(n)                                            │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 特性总结

| 特性 | 说明 |
|------|------|
| 数据结构 | 跳表（SkipList） |
| 时间复杂度 | O(log n) |
| 线程安全 | CAS + 每个节点独立锁 |
| 有序性 | 按键排序（自然序或自定义 Comparator） |
| 并发度 | 高（每个节点独立锁） |
| 适用场景 | 需要有序遍历、范围查询的并发 Map |

---

### 3.5 BlockingQueue 家族

#### 阻塞队列对比

```
┌─────────────────────────────────────────────────────────────┐
│                   BlockingQueue 家族对比                      │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  1. ArrayBlockingQueue                                      │
│     ┌──────────────────────────────────────────────────┐   │
│     │ 基于数组，固定容量                                 │   │
│     │ 一把 ReentrantLock + 两个 Condition               │   │
│     │ 公平/非公平锁可选                                 │   │
│     │ 内存开销小，实现简单                              │   │
│     │ 缺点: 固定容量，需预先分配                        │   │
│     └──────────────────────────────────────────────────┘   │
│                                                             │
│  2. LinkedBlockingQueue                                      │
│     ┌──────────────────────────────────────────────────┐   │
│     │ 基于链表，可选容量                                │   │
│     │ 两把锁（takeLock + putLock）+ 两个 Condition       │   │
│     │ 读写分离，高并发性能好                            │   │
│     │ 默认容量: Integer.MAX_VALUE                      │   │
│     │ 缺点: 内存占用大                                  │   │
│     └──────────────────────────────────────────────────┘   │
│                                                             │
│  3. PriorityBlockingQueue                                    │
│     ┌──────────────────────────────────────────────────┐   │
│     │ 优先级队列，按元素排序                            │   │
│     │ 基于二叉堆                                        │   │
│     │ 无界（自动扩容）                                  │   │
│     │ 时间复杂度: O(log n)                             │   │
│     │ 缺点: 不保证同优先级元素顺序                      │   │
│     └──────────────────────────────────────────────────┘   │
│                                                             │
│  4. SynchronousQueue                                        │
│     ┌──────────────────────────────────────────────────┐   │
│     │ 容量为 0 的特殊队列                               │   │
│     │ 生产者 put 必须等待消费者 take                     │   │
│     │ 直接交付，不存储元素                              │   │
│     │ 适用于任务分发场景                                │   │
│     │ Executors.newCachedThreadPool() 使用              │   │
│     └──────────────────────────────────────────────────┘   │
│                                                             │
│  5. DelayQueue                                               │
│     ┌──────────────────────────────────────────────────┐   │
│     │ 延迟队列，元素需实现 Delayed 接口                 │   │
│     │ 基于 PriorityQueue                                │   │
│     │ 只有到期元素才能被 take                            │   │
│     │ 适用于定时任务、超时处理                          │   │
│     └──────────────────────────────────────────────────┘   │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 核心特性对比

| 特性 | ArrayBlockingQueue | LinkedBlockingQueue | SynchronousQueue |
|------|-------------------|-------------------|------------------|
| 数据结构 | 数组 | 链表 | 无 |
| 容量 | 固定 | 可选（默认无限） | 0 |
| 锁机制 | 1 把锁 | 2 把锁（读写分离） | CAS |
| 内存 | 低 | 高 | 极低 |
| 适用场景 | 固定容量生产-消费 | 高并发生产-消费 | 直接交付 |

---

## 4. 适用场景与性能对比

### 4.1 场景选择指南

```
┌─────────────────────────────────────────────────────────────┐
│                   并发集合选择决策树                          │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  需求: 并发 Map?                                             │
│  ├── 是 → 需要有序/范围查询?                                │
│  │       ├── 是 → ConcurrentSkipListMap                     │
│  │       └── 否 → ConcurrentHashMap                        │
│  └── 否 → 继续                                              │
│                                                             │
│  需求: 并发 List?                                            │
│  ├── 是 → 读多写少?                                         │
│  │       ├── 是 → CopyOnWriteArrayList                      │
│  │       └── 否 → Collections.synchronizedList()            │
│  └── 否 → 继续                                              │
│                                                             │
│  需求: 并发 Set?                                             │
│  ├── 是 → 需要有序?                                         │
│  │       ├── 是 → ConcurrentSkipListSet                     │
│  │       └── 否 → CopyOnWriteArraySet                       │
│  └── 否 → 继续                                              │
│                                                             │
│  需求: 并发 Queue?                                           │
│  ├── 是 → 需要阻塞?                                         │
│  │       ├── 是 → 固定容量?                                 │
│  │       │       ├── 是 → ArrayBlockingQueue               │
│  │       │       └── 否 → LinkedBlockingQueue               │
│  │       └── 否 → 需要直接交付?                             │
│  │               ├── 是 → SynchronousQueue                 │
│  │               └── 否 → ConcurrentLinkedQueue            │
│  └── 否 → 根据具体需求选择其他                              │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.2 性能对比

#### 吞吐量测试（单线程）

```
┌─────────────────────────────────────────────────────────────┐
│           单线程写入性能对比 (ops/ms)                         │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  HashMap:           ████████████████████  12000            │
│  ConcurrentHashMap: ███████████████████   11000            │
│  HashTable:         ███████████████       8500              │
│                                                             │
│  → 单线程下 ConcurrentHashMap 略慢于 HashMap（锁开销）       │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

#### 多线程并发测试

```
┌─────────────────────────────────────────────────────────────┐
│           16 线程并发写入性能对比 (ops/ms)                    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│  ConcurrentHashMap: ████████████████████  95000            │
│  ConcurrentSkipListMap: ████████████████  80000           │
│  Collections.synchronizedMap: ██████████  35000             │
│  HashTable:         ████████████          30000             │
│                                                             │
│  → 多线程下 ConcurrentHashMap 性能远超其他实现               │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### 4.3 适用场景总结

| 集合类型 | 适用场景 | 反模式 |
|----------|----------|--------|
| **ConcurrentHashMap** | 高并发读写、缓存、计数器 | 读多写少且无需遍历 |
| **CopyOnWriteArrayList** | 读多写少、配置列表、白名单 | 频繁写入、大数据量 |
| **ConcurrentLinkedQueue** | 高并发队列、任务分发 | 需要阻塞等待 |
| **BlockingQueue** | 生产者-消费者、线程池 | 需要随机访问 |
| **ConcurrentSkipListMap** | 有序 Map、范围查询、排行榜 | 简单 KV 存储 |

---

## 5. 使用注意事项与陷阱

### 5.1 迭代器弱一致性

```java
// ConcurrentHashMap 迭代器
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
map.put("A", 1);
map.put("B", 2);

// 迭代过程中其他线程修改
Iterator<Map.Entry<String, Integer>> it = map.entrySet().iterator();
while (it.hasNext()) {
    Map.Entry<String, Integer> entry = it.next();
    System.out.println(entry.getKey() + "=" + entry.getValue());
    
    // 其他线程可能新增/删除元素
    map.put("C", 3);  // 不会抛 ConcurrentModificationException
    map.remove("A");  // 不会抛异常
}

// 注意: 迭代器可能看到部分修改，保证最终一致性
```

#### 各集合迭代器行为

| 集合 | 迭代器类型 | 行为 |
|------|----------|------|
| ConcurrentHashMap | 弱一致 | 不抛异常，可能看到部分修改 |
| CopyOnWriteArrayList | 快照 | 基于创建时的快照，不受后续修改影响 |
| ConcurrentLinkedQueue | 弱一致 | 不抛异常 |
| BlockingQueue | 弱一致 | 不抛异常 |

### 5.2 size() 精度问题

```java
// ConcurrentHashMap.size() 可能不精确
ConcurrentHashMap<String, Integer> map = new ConcurrentHashMap<>();
// ... 多线程并发修改 ...
int size = map.size();  // 可能不精确！

// 原因: addCount 方法使用 CAS 更新计数
// 解决: 使用 mappingCount() 方法（近似精确）
long count = map.mappingCount();
```

### 5.3 null 值限制

```java
// ConcurrentHashMap 不允许 null key 或 null value
try {
    concurrentHashMap.put(null, "value");  // ❌ 抛 NullPointerException
} catch (NullPointerException e) {
    // 处理
}

// ConcurrentLinkedQueue 不允许 null 元素
try {
    concurrentLinkedQueue.offer(null);  // ❌ 抛 NullPointerException
} catch (NullPointerException e) {
    // 处理
}

// 为什么？
// 1. null 用于区分哨兵值（如 poll 返回 null 表示空）
// 2. 避免歧义（get(key) 返回 null 是不存在还是 value 为 null？）
```

### 5.4 复合操作非原子性

```java
// ❌ 错误: 复合操作不是原子的
if (!concurrentHashMap.containsKey(key)) {
    concurrentHashMap.put(key, value);  // 非原子！其他线程可能在此期间插入
}

// ✅ 正确: 使用原子操作
concurrentHashMap.putIfAbsent(key, value);  // 原子操作

// ✅ 正确: 使用 computeIfAbsent
concurrentHashMap.computeIfAbsent(key, k -> createValue(k));

// ✅ 正确: 使用 replace
concurrentHashMap.replace(key, oldValue, newValue);  // CAS 语义
```

### 5.5 共享变量的可见性

```java
// ❌ 陷阱: 非 volatile 变量的可见性
public class Counter {
    private int count = 0;  // 没有 volatile
    
    public void increment() {
        count++;  // 不是原子操作！
    }
    
    public int getCount() {
        return count;  // 可能读到过时的值
    }
}

// ✅ 正确: 使用并发集合或 AtomicInteger
ConcurrentHashMap<String, AtomicInteger> counters = new ConcurrentHashMap<>();
counters.computeIfAbsent("key", k -> new AtomicInteger(0)).incrementAndGet();
```

---

## 6. 高频面试题解析

### Q1: ConcurrentHashMap 在 JDK 1.8 中是如何实现线程安全的？

**答案**:
1. **CAS + synchronized** 混合锁机制
2. **桶数组** 使用 volatile 修饰，保证可见性
3. **空桶写入** 使用 CAS 无锁操作
4. **非空桶写入** 使用 synchronized 锁住链表/红黑树的头节点
5. **读操作** 基于 volatile 直接读取，无需加锁
6. **扩容** 使用多线程协助扩容（helpTransfer）

---

### Q2: ConcurrentHashMap 中为什么使用 synchronized 而不是 ReentrantLock？

**答案**:
1. **锁对象**：synchronized 锁的是 Node 对象（链表头），粒度更细
2. **JVM 优化**：synchronized 在 JDK 1.6 后有偏向锁、轻量级锁等优化，性能优异
3. **内存开销**：synchronized 不需要额外的 Lock 对象
4. **简洁性**：代码更简洁，不容易出错
5. **锁释放**：synchronized 自动释放，避免忘记 unlock

---

### Q3: CopyOnWriteArrayList 为什么适合读多写少的场景？

**答案**:
1. **读操作无锁**：直接读取 volatile 数组，性能极高
2. **写操作开销大**：每次写入都要复制整个数组
3. **迭代器安全**：基于快照，不会抛 ConcurrentModificationException
4. **典型场景**：配置列表、白名单、监听器列表等

---

### Q4: ConcurrentLinkedQueue 和 LinkedBlockingQueue 的区别？

**答案**:

| 对比项 | ConcurrentLinkedQueue | LinkedBlockingQueue |
|--------|----------------------|-------------------|
| 锁机制 | 无锁（CAS） | 两把锁（读写分离） |
| 阻塞性 | 非阻塞 | 可阻塞（put/take） |
| 容量 | 无界 | 可选容量 |
| 适用场景 | 高并发非阻塞队列 | 生产者-消费者 |
| 实现复杂度 | 高 | 中等 |

---

### Q5: 如何选择合适的并发集合？

**答案**:
1. **Map 场景**：默认 ConcurrentHashMap，需要有序选 ConcurrentSkipListMap
2. **List 场景**：读多写少用 CopyOnWriteArrayList，写多用 synchronizedList
3. **Queue 场景**：生产者-消费者用 BlockingQueue，高并发用 ConcurrentLinkedQueue
4. **特殊需求**：阻塞用 BlockingQueue，延迟用 DelayQueue，优先级用 PriorityBlockingQueue

---

### Q6: 并发集合的 size() 方法为什么可能不精确？

**答案**:
1. **ConcurrentHashMap**：使用 `baseCount + Counter[]` 分散计数，并发更新时可能不精确
2. **ConcurrentLinkedQueue**：遍历计数，中间状态可能不精确
3. **解决方法**：
   - ConcurrentHashMap: 使用 `mappingCount()`（近似精确）
   - CopyOnWriteArrayList: size() 精确（基于数组长度）
   - BlockingQueue: size() 相对精确（使用一把锁）

---

### Q7: 如何在生产环境中使用并发集合避免问题？

**答案**:
1. **明确场景**：根据读写比例选择合适的集合
2. **避免复合操作**：使用原子方法（putIfAbsent、computeIfAbsent）
3. **注意迭代器**：不要在迭代过程中依赖实时性
4. **避免 null**：大多数并发集合不允许 null 值
5. **监控 size**：对 size 敏感的场景使用精确计数
6. **压力测试**：上线前进行并发压力测试

---

### Q8: 说一下 ConcurrentHashMap 的扩容机制？

**答案**:
1. **触发条件**：size > threshold（容量 * 负载因子 0.75）
2. **扩容过程**：
   - 创建新数组（容量翻倍）
   - 单线程迁移第一个桶
   - 多线程协助迁移（helpTransfer）
   - 设置 `sizeCtl = -N` 标记扩容状态
3. **多线程协助**：每个线程负责迁移一段桶区间
4. **迁移完成**：切换数组引用

---

### Q9: 什么是分段锁？JDK 1.8 中还使用吗？

**答案**:
1. **分段锁**（Segment）：JDK 1.7 中 ConcurrentHashMap 使用的锁机制
   - 将 Map 分成 16 个 Segment
   - 每个 Segment 独立加锁
   - 最多 16 个并发写操作
2. **JDK 1.8 已弃用**：改用 CAS + synchronized
   - 锁粒度更细（桶级别）
   - 并发度更高
   - 读操作无锁

---

### Q10: 如何理解并发集合的"弱一致性"？

**答案**:
1. **强一致性**：读操作总能看到最新的写操作结果
2. **弱一致性**：读操作可能看到过时的值，但保证最终一致
3. **并发集合的弱一致性**：
   - ConcurrentHashMap 的迭代器可能看不到最新添加的元素
   - CopyOnWriteArrayList 的迭代器基于快照，完全看不到后续修改
   - size() 方法可能返回不精确的值
4. **为什么这样设计**：
   - 性能考虑：强一致性需要更多同步开销
   - 大多数场景下可接受：最终一致即可

---

## 7. 实战项目示例

### 7.1 缓存管理器

```java
/**
 * 基于 ConcurrentHashMap 的线程安全缓存
 */
public class ConcurrentCache<K, V> {
    
    private final ConcurrentHashMap<K, CacheEntry<V>> cache = new ConcurrentHashMap<>();
    private final long expireMillis;
    
    public ConcurrentCache(long expireMillis) {
        this.expireMillis = expireMillis;
    }
    
    // 线程安全的缓存写入
    public void put(K key, V value) {
        cache.put(key, new CacheEntry<>(value, System.currentTimeMillis()));
    }
    
    // 线程安全的缓存读取
    public V get(K key) {
        CacheEntry<V> entry = cache.get(key);
        if (entry == null) return null;
        
        // 检查过期
        if (System.currentTimeMillis() - entry.createTime > expireMillis) {
            cache.remove(key);
            return null;
        }
        return entry.value;
    }
    
    // 原子操作: 不存在则写入
    public V getOrPut(K key, Supplier<V> supplier) {
        return cache.computeIfAbsent(key, k -> 
            new CacheEntry<>(supplier.get(), System.currentTimeMillis())
        ).value;
    }
    
    // 清理过期缓存
    public void cleanExpired() {
        Iterator<Map.Entry<K, CacheEntry<V>>> it = cache.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<K, CacheEntry<V>> entry = it.next();
            if (System.currentTimeMillis() - entry.getValue().createTime > expireMillis) {
                it.remove();
            }
        }
    }
    
    private static class CacheEntry<V> {
        final V value;
        final long createTime;
        
        CacheEntry(V value, long createTime) {
            this.value = value;
            this.createTime = createTime;
        }
    }
}
```

### 7.2 高性能计数器

```java
/**
 * 基于 ConcurrentHashMap 的分布式计数器
 */
public class ConcurrentCounter {
    
    private final ConcurrentHashMap<String, LongAdder> counters = new ConcurrentHashMap<>();
    
    // 原子递增
    public void increment(String key) {
        counters.computeIfAbsent(key, k -> new LongAdder()).increment();
    }
    
    // 原子递减
    public void decrement(String key) {
        counters.computeIfAbsent(key, k -> new LongAdder()).decrement();
    }
    
    // 获取计数
    public long getCount(String key) {
        LongAdder adder = counters.get(key);
        return adder == null ? 0 : adder.sum();
    }
    
    // 重置计数
    public void reset(String key) {
        LongAdder adder = counters.get(key);
        if (adder != null) {
            adder.reset();
        }
    }
    
    // 获取所有计数
    public Map<String, Long> getAllCounts() {
        return counters.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> e.getValue().sum()
            ));
    }
}
```

### 7.3 生产者-消费者队列

```java
/**
 * 基于 BlockingQueue 的生产者-消费者示例
 */
public class ProducerConsumerExample {
    
    private final BlockingQueue<Task> taskQueue;
    private final ExecutorService executor;
    
    public ProducerConsumerExample(int threadCount, int queueCapacity) {
        this.taskQueue = new LinkedBlockingQueue<>(queueCapacity);
        this.executor = Executors.newFixedThreadPool(threadCount);
        
        // 启动消费者线程
        for (int i = 0; i < threadCount; i++) {
            executor.submit(this::consume);
        }
    }
    
    // 生产者: 添加任务
    public void produce(Task task) throws InterruptedException {
        taskQueue.put(task);  // 满时阻塞
    }
    
    // 消费者: 处理任务
    private void consume() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                Task task = taskQueue.take();  // 空时阻塞
                processTask(task);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    private void processTask(Task task) {
        // 处理任务逻辑
        task.execute();
    }
    
    // 关闭
    public void shutdown() {
        executor.shutdown();
    }
}

// 任务接口
@FunctionalInterface
public interface Task {
    void execute();
}
```

### 7.4 排行榜实现

```java
/**
 * 基于 ConcurrentSkipListMap 的实时排行榜
 */
public class Leaderboard {
    
    // key: 分数(降序), value: 用户ID集合
    private final ConcurrentSkipListMap<Integer, Set<String>> scoreMap 
        = new ConcurrentSkipListMap<>(Comparator.reverseOrder());
    
    // 用户分数映射
    private final ConcurrentHashMap<String, Integer> userScores 
        = new ConcurrentHashMap<>();
    
    // 更新分数（原子操作）
    public void updateScore(String userId, int newScore) {
        Integer oldScore = userScores.put(userId, newScore);
        
        // 从旧分数集合移除
        if (oldScore != null) {
            scoreMap.computeIfPresent(oldScore, (score, users) -> {
                users.remove(userId);
                return users.isEmpty() ? null : users;
            });
        }
        
        // 添加到新分数集合
        scoreMap.computeIfAbsent(newScore, k -> ConcurrentHashMap.newKeySet()).add(userId);
    }
    
    // 获取用户排名
    public int getRank(String userId) {
        Integer score = userScores.get(userId);
        if (score == null) return -1;
        
        // 使用 headMap 计算排名
        return scoreMap.headMap(score, false).values().stream()
            .mapToInt(Set::size)
            .sum() + 1;
    }
    
    // 获取 Top N
    public List<String> getTopN(int n) {
        List<String> result = new ArrayList<>();
        for (Map.Entry<Integer, Set<String>> entry : scoreMap.entrySet()) {
            for (String userId : entry.getValue()) {
                if (result.size() >= n) return result;
                result.add(userId);
            }
        }
        return result;
    }
    
    // 获取分数范围查询
    public Set<String> getUsersByScoreRange(int minScore, int maxScore) {
        return scoreMap.subMap(maxScore, true, minScore, true).values().stream()
            .flatMap(Set::stream)
            .collect(Collectors.toSet());
    }
}
```

---

## 附录：快速选择清单

### 并发集合选择 Checklist

```
□ 是否需要并发？
  □ 否 → 使用普通集合（HashMap, ArrayList）

□ 选择集合类型:
  □ Map: 
    □ 有序/范围查询 → ConcurrentSkipListMap
    □ 普通 KV → ConcurrentHashMap
  □ List:
    □ 读多写少 → CopyOnWriteArrayList
    □ 写多 → Collections.synchronizedList
  □ Set:
    □ 有序 → ConcurrentSkipListSet
    □ 普通 → CopyOnWriteArraySet
  □ Queue:
    □ 生产者-消费者 → BlockingQueue
    □ 高并发 → ConcurrentLinkedQueue
    □ 直接交付 → SynchronousQueue
    □ 延迟任务 → DelayQueue

□ 检查注意事项:
  □ 是否需要原子复合操作？（使用 putIfAbsent, computeIfAbsent）
  □ 是否需要精确 size？（使用 mappingCount, 或 AtomicInteger 自行计数）
  □ 是否允许 null？（大多数并发集合不允许）
  □ 是否需要强一致？（大多数场景接受弱一致）

□ 性能测试:
  □ 模拟实际并发量
  □ 测试读写比例
  □ 监控内存使用
```

---

## 总结对比表

| 集合 | 锁机制 | 时间复杂度 | 适用场景 | 典型代码示例 |
|------|--------|-----------|----------|-------------|
| **ConcurrentHashMap** | CAS + synchronized | O(1) | 高并发 KV 存储、缓存、计数器 | `cache.putIfAbsent(key, value)` |
| **CopyOnWriteArrayList** | ReentrantLock + 写时复制 | O(n) 写/O(1) 读 | 读多写少、配置列表 | `list.add(item)` |
| **ConcurrentLinkedQueue** | CAS 无锁 | O(1) | 高并发队列、任务分发 | `queue.offer(item)` |
| **LinkedBlockingQueue** | 双锁（读写分离） | O(1) | 生产者-消费者 | `queue.put(item)` / `queue.take()` |
| **ArrayBlockingQueue** | 单锁 + 双 Condition | O(1) | 固定容量生产-消费 | `queue.put(item)` |
| **ConcurrentSkipListMap** | CAS + 节点锁 | O(log n) | 有序 Map、排行榜 | `map.get(key)` |

---

> **文档版本**: v1.0  
> **适用对象**: Java 后端面试复习  
> **更新日期**: 2026-07-29