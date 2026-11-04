# 面试题：敏感字段缓存的线程安全设计

---

## 题目背景

以下是某 Spring Boot 项目中用于文件导出场景的**敏感数据脱敏处理器**。其核心逻辑是：反射扫描 DTO 的 `@Sensitive` 字段并缓存，避免每次导出都重复反射。

### 当前使用 `ConcurrentHashMap` 的版本

```java
@Component
public class SensitiveDataMasker {

    private final ConcurrentMap<Class<?>, List<Field>> sensitiveFieldCache = new ConcurrentHashMap<>();

    public void maskBean(Object bean) {
        Class<?> clazz = bean.getClass();
        // 当前实现
        List<Field> fields = sensitiveFieldCache.computeIfAbsent(clazz, SensitiveDataMasker::resolveSensitiveFields);
        for (Field field : fields) {
            // 脱敏逻辑...
        }
    }

    private static List<Field> resolveSensitiveFields(Class<?> clazz) {
        List<Field> result = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(Sensitive.class)) {
                field.setAccessible(true);
                result.add(field);
            }
        }
        return result;
    }
}
```

---

## 问题（共 5 小问，分层递进）

### 【Q1 · 基础题】

如果把 `ConcurrentHashMap` 改成普通的 `HashMap`，直接用 DCL 写法如下，**是否正确？如果有问题请指出**：

```java
// 改成普通 HashMap
private final Map<Class<?>, List<Field>> sensitiveFieldCache = new HashMap<>();

public void maskBean(Object bean) {
    Class<?> clazz = bean.getClass();
    
    // 外层无锁读
    List<Field> fields = sensitiveFieldCache.get(clazz);
    if (fields == null) {
        synchronized (sensitiveFieldCache) {
            // 内层二次检查
            fields = sensitiveFieldCache.get(clazz);
            if (fields == null) {
                fields = resolveSensitiveFields(clazz);
                sensitiveFieldCache.put(clazz, fields);
            }
        }
    }
    
    // 后续遍历 fields 进行脱敏...
}
```

---

### 【Q2 · 进阶题】

针对 Q1 的写法，如果 `synchronized (sensitiveFieldCache)` 改成 `synchronized (clazz)`，**是否存在问题？请从并发正确性和性能两个维度分析**。

---

### 【Q3 · 核心题】

回到 DCL 这个模式本身——**为什么需要二次检查？如果去掉二次检查会发生什么？请从 happens-before 规则的角度说明**。

---

### 【Q4 · 深入题】

**DCL 的外层无锁 `HashMap.get()` 在 Java 内存模型下有什么可见性风险？** 为什么这里**不需要**给 `sensitiveFieldCache` 加 `volatile`？

---

### 【Q5 · 综合题】

请对比当前代码（`ConcurrentHashMap + computeIfAbsent`）和手写 DCL 的优劣，**你会选择哪个方案？为什么？**

---

## 参考答案与评分标准

### Q1 参考答案（基础段位）

**完全正确**，这是 HashMap 下 DCL 的标准写法。

评分标准：
- 说出「外层无锁快路径 + 内层同步块」→ 基础合格
- 说出「HashMap 非线程安全，必须用 synchronized 保护 put 操作」→ 基础合格
- 如果说「HashMap 有线程安全问题所以 DCL 也没用」→ **扣大分**，说明对 HashMap 和 ConcurrentHashMap 的区别只停留在表面

---

### Q2 参考答案（中级段位）

**存在问题**，分两个维度：

#### 维度一：并发正确性 ❌

`synchronized (clazz)` 只保证**同一个 key 的写操作互斥**，但 `HashMap` 本身是非线程安全容器。当 T1 持 `userDTO.class` 锁、T2 持 `orderDTO.class` 锁时，两个线程**同时操作同一个 HashMap 实例**，可能触发：

- HashMap 的 resize 扩容时，内部 table 数组的并发修改 → 读到中间状态导致 NPE 或脏数据
- JDK 7 及之前，链表结构并发修改成环 → **死循环**（CPU 100%）

#### 维度二：性能 ⚠️（假设 Q1 写法已保证正确性后的优化）

- 不同类的缓存写操作**可以并行**（各自锁自己的 Class 对象），比全局锁性能好
- 但如果 HashMap 的内部安全没保护好，这个性能优势就是空中楼阁

评分标准：
- 只说出「性能更好」→ 部分正确
- 说出「HashMap 容器本身非线程安全，分片锁保护不了」→ **关键得分点**
- 说出「如果在 HashMap 外面再加全局锁，clazz 锁的优势就没了」→ 优秀

---

### Q3 参考答案（高级段位）

#### 去掉二次检查会发生什么？

假设 T1 和 T2 同时处理**一个全新的、缓存未命中的 DTO 类型**：

```
T1: 外层 get() → null → 进入 synchronized 前
T2: 外层 get() → null → 进入 synchronized 前
T1: 拿到锁 → resolveSensitiveFields() → put() → 释放锁
T2: 拿到锁 → 【如果不二次检查】→ 再次 resolveSensitiveFields() → 重复计算！
```

#### 从 happens-before 规则说明

> 二次检查的本质是**利用 synchronized 的 monitor lock 规则建立 happens-before 关系**：
>
> JMM 规定：**对同一把锁的 unlock 操作，happens-before 后续对这把锁的 lock 操作**。
>
> T1 在 synchronized 块内执行了 `put`（写操作），释放锁 → T2 后续拿到同一把锁 → T2 **必然能看到 T1 写入的值**。
>
> 所以 T2 拿到锁后再 `get` 一次，如果有值就直接复用，避免重复计算。

评分标准：
- 说出「会重复调用 resolveSensitiveFields」→ 基础正确
- 说出「monitor lock unlock happens-before lock」→ **关键得分点**
- 能结合到 JMM 的 happens-before 规则原文 → 高级

---

### Q4 参考答案（专家段位）

#### 外层 `HashMap.get()` 的可见性风险是什么？

> JMM 没有给普通 `HashMap.get()` 任何 happens-before 保证。也就是说：
> - T1 在 synchronized 块内 `put` 并释放锁
> - T2 的外层无锁 `get` **有可能看不到** T1 刚写入的值（因为没有建立 happens-before 关系）
> - T2 会误以为缓存还是 null，**重新进入 synchronized 块**（但二次检查时能看到了，所以不会产生错误结果，只是多了一次锁竞争）

**这不是 bug，是性能 trade-off**：多进几次锁，总比每次都进锁好。

#### 为什么不需要给 `sensitiveFieldCache` 加 `volatile`？

三个原因叠在一起回答：

1. **`volatile` 修饰的是「引用」，不是容器内部元素的可见性**。`volatile Map<...>` 只能保证 `Map` 这个引用本身的可见性（比如把整个 Map 替换掉），不能保证 Map 内部的 `put/get` 操作有可见性保证。

2. **写入侧的可见性已经由 synchronized 保证**：T1 在 synchronized 块内 `put`，释放锁 happens-before 后续 T2 拿到同一把锁。

3. **外层无锁读即使看不到新值，后果也只是「多进一次锁」**（进入后二次检查能看到），不会产生错误结果。加了 `volatile` 也解决不了这个问题（`volatile` 不影响 HashMap 内部可见性），反而会降低读写性能。

评分标准：
- 说出「volatile 修饰容器引用不能保证内部元素可见性」→ **关键得分点**
- 说出「外层无锁读可能看不到新值，但只是多进锁，不会错」→ 优秀
- 说出「不需要 volatile 的三个原因」→ 专家级

---

### Q5 参考答案（架构思维）

| 维度 | ConcurrentHashMap + computeIfAbsent | 手写 DCL + HashMap |
|------|------|------|
| **代码量** | 一行搞定，零心智负担 | 必须写对 DCL 逻辑（稍不留神就错） |
| **并发安全** | CAS + bucket 级锁，容器内保证原子性 | 依赖手写 synchronized + 二次检查 |
| **可见性** | Node.val 是 volatile，容器内保证 | 依赖 synchronized monitor lock |
| **不同类并行** | ✅ 天然支持（不同 bucket） | ✅ （clazz 锁方案）/ ❌（全局锁方案） |
| **读性能** | ✅ CAS 无锁 | ✅ 外层无锁 get |
| **写性能** | ✅ CAS 竞争 | ⚠️ synchronized 竞争 |
| **维护成本** | 低（标准库） | 高（后续改代码容易破坏 DCL） |

**结论**：**毫无悬念选择 ConcurrentHashMap**。

理由：ConcurrentHashMap 的 `computeIfAbsent` 本质上是**容器内部帮你实现好了最优版的 DCL**（CAS 快路径 + bucket 级锁写），性能不比手写差多少，代码量少 N 倍，不会写出有 bug 的 DCL。

唯一可能考虑手写 DCL 的场景：**被编码规范强制禁用并发容器**（极少见），且业务确定是读多写少。

评分标准：
- 说出「ConcurrentHashMap 性能不比手写差」→ 合格
- 说出「computeIfAbsent 就是容器内置的 DCL」→ **关键得分点**
- 能说出手写 DCL 的维护风险 → 优秀
- 能举例说出什么情况下会考虑手写 DCL → 架构思维

---

## 段位总结

| 段位 | 对应题目 | 核心能力 |
|------|----------|----------|
| 初级 Java | Q1 | 知道 HashMap 和 ConcurrentHashMap 的基本区别 |
| 中级 Java | Q2 | 能意识到「容器安全」和「业务锁」是两个独立问题 |
| 高级 Java | Q3 | 能用 JMM happens-before 规则解释并发正确性 |
| 专家 Java | Q4 | 理解 volatile 的局限性，能从性能角度做 trade-off |
| 架构师 | Q5 | 能从多个维度做技术选型，有体系化思维 |

---

## 面试使用建议

| 场景 | 怎么用 |
|------|--------|
| **校招** | 只问 Q1 + Q5 基础版，考察对并发容器的基本认知 |
| **社招 3-5 年** | Q1 + Q2 + Q3，重点看能否说出 happens-before 和 HashMap 容器安全的关系 |
| **社招 5 年以上** | 5 题全问，尤其 Q4 和 Q5 是区分普通人和高手的分水岭 |
| **架构师面试** | Q5 展开讨论，引导候选人思考「并发容器选择的基本原则」 |