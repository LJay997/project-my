# CPU 飙升排查清单

> 系统性排查 Java 应用 CPU 使用率异常飙升的完整指南
> 
> 适用场景：生产环境、测试环境、容器化部署（Docker/Kubernetes/Rancher）

---

## 目录

- [1. 快速诊断流程](#1-快速诊断流程)
- [2. 系统层面排查](#2-系统层面排查)
  - [2.1 基础监控指标](#21-基础监控指标)
  - [2.2 常用系统命令](#22-常用系统命令)
- [3. 进程层面排查](#3-进程层面排查)
  - [3.1 定位高CPU进程](#31-定位高cpu进程)
  - [3.2 进程资源分析](#32-进程资源分析)
- [4. 线程层面排查](#4-线程层面排查)
  - [4.1 线程堆栈分析](#41-线程堆栈分析)
  - [4.2 高CPU线程定位](#42-高cpu线程定位)
- [5. JVM 层面排查](#5-jvm-层面排查)
  - [5.1 GC 日志分析](#51-gc-日志分析)
  - [5.2 JVM 参数检查](#52-jvm-参数检查)
  - [5.3 内存状态分析](#53-内存状态分析)
- [6. 常见原因与解决方案](#6-常见原因与解决方案)
  - [6.1 死循环](#61-死循环)
  - [6.2 频繁 GC](#62-频繁-gc)
  - [6.3 线程阻塞/锁竞争](#63-线程阻塞锁竞争)
  - [6.4 资源耗尽](#64-资源耗尽)
  - [6.5 外部依赖问题](#65-外部依赖问题)
- [7. 预防措施与最佳实践](#7-预防措施与最佳实践)
- [8. 应急处理流程](#8-应急处理流程)
- [附录](#附录)

---

## 1. 快速诊断流程

```
┌─────────────────────────────────────────────────────────────────┐
│                    CPU 飙升快速诊断流程                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  [1] 确认问题范围                                                │
│      ├─ 单机 CPU 还是集群整体？                                   │
│      ├─ 持续飙升还是瞬时峰值？                                   │
│      └─ 业务高峰还是空闲时段？                                   │
│                                                                 │
│  [2] 定位高 CPU 进程                                            │
│      ├─ top / htop 查看进程排行                                  │
│      ├─ pidstat 按进程统计                                      │
│      └─ ps aux 过滤 Java 进程                                    │
│                                                                 │
│  [3] 分析高 CPU 线程                                            │
│      ├─ top -H -p PID 查看线程级别 CPU                          │
│      ├─ pidstat -t -p PID 1 实时监控线程                        │
│      └─ 将高 CPU 线程 ID 转 16 进制                              │
│                                                                 │
│  [4] 获取线程堆栈                                                │
│      ├─ jstack PID > thread.log                                 │
│      ├─ 搜索对应线程 ID                                         │
│      └─ 分析线程状态和调用链                                     │
│                                                                 │
│  [5] 检查 JVM 状态                                              │
│      ├─ jstat -gcutil PID 查看 GC 统计                          │
│      ├─ jmap -histo PID 查看对象分布                             │
│      └─ GC 日志分析                                             │
│                                                                 │
│  [6] 确定根因并修复                                             │
│      ├─ 代码问题（死循环、锁竞争）                               │
│      ├─ 配置问题（内存不足、线程池配置）                         │
│      └─ 依赖问题（数据库、缓存、外部服务）                       │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 2. 系统层面排查

### 2.1 基础监控指标

| 指标 | 说明 | 正常范围 | 排查命令 |
|------|------|----------|----------|
| CPU 使用率 | 系统整体 CPU 占用 | < 70% | `top -l 1` |
| 负载平均值 | 可运行/不可中断任务数 | < CPU 核心数 | `uptime` |
| 上下文切换 | 线程切换频率 | < 5000/s | `vmstat 1` |
| 内存使用 | 物理内存占用 | < 80% | `free -m` |
| Swap 使用 | 交换分区使用 | 0（尽量不用） | `swapon --show` |
| 磁盘 IO | 磁盘读写压力 | iowait < 10% | `iostat 1` |
| 网络流量 | 网络带宽使用 | 根据网络带宽 | `iftop` |

### 2.2 常用系统命令

#### 2.2.1 top - 实时进程监控

```bash
# 基本用法
top

# 按 CPU 排序（默认）
top -o cpu

# 指定刷新间隔（秒）
top -d 2

# 指定运行次数后退出
top -n 10

# 仅显示指定用户进程
top -u java

# macOS 版本
top -o cpu -stats pid,command,cpu,mem,threads,state
```

**输出字段说明：**

| 字段 | 含义 |
|------|------|
| PID | 进程 ID |
| CPU% | CPU 使用率 |
| MEM% | 内存使用率 |
| TIME+ | 累计 CPU 时间 |
| STATE | 进程状态（R/S/T/Z） |
| #TH | 线程数量 |

#### 2.2.2 vmstat - 虚拟内存统计

```bash
# 每秒输出一次，共10次
vmstat 1 10

# 关键指标
# procs.r  - 运行队列进程数（> CPU 核心数说明瓶颈）
# procs.b  - 阻塞进程数
# cpu.us   - 用户态 CPU 时间
# cpu.sy   - 内核态 CPU 时间
# cpu.wa   - IO 等待时间（高则 IO 瓶颈）
# si/so    - Swap 换入/换出（> 0 说明内存不足）
```

#### 2.2.3 sar - 系统活动报告

```bash
# CPU 使用统计
sar -u 1 10

# 内存使用
sar -r 1 10

# 磁盘 IO
sar -d 1 10

# 网络统计
sar -n DEV 1 10
```

#### 2.2.4 mpstat - 多处理器统计

```bash
# 查看每个 CPU 核心使用情况
mpstat -P ALL 1 5

# 单核 CPU 使用率
mpstat -P 0 1
```

#### 2.2.5 dstat - 多功能统计

```bash
# 综合性能统计
dstat -c -m -d -n -l 1

# 仅 CPU 和内存
dstat -c -m 1
```

---

## 3. 进程层面排查

### 3.1 定位高CPU进程

#### 3.1.1 ps 命令筛选

```bash
# 列出所有进程按 CPU 排序
ps aux --sort=-%cpu

# 仅显示 Java 进程
ps aux | grep java | grep -v grep

# 获取 Java 进程 PID
pgrep -f java

# 获取特定应用 PID
pgrep -f spring-boot
```

#### 3.1.2 pidstat 进程级统计

```bash
# 按进程统计 CPU（每秒刷新）
pidstat -p ALL 1

# 统计 Java 进程
pidstat -C java 1

# 统计进程的内存使用
pidstat -r -p PID 1

# 统计进程的 IO 使用
pidstat -d -p PID 1
```

#### 3.1.3 进程树查看

```bash
# 查看进程树结构
pstree -p PID

# 显示进程详细信息
ps -efL | grep PID
```

### 3.2 进程资源分析

#### 3.2.1 /proc 文件系统

```bash
# 进程状态
cat /proc/PID/status

# 关键指标：
# VmRSS  - 常驻物理内存
# VmSize - 虚拟内存大小
# Threads - 线程数量

# 进程内存映射
cat /proc/PID/maps

# 进程打开的文件
ls -la /proc/PID/fd/

# 进程线程列表
ls /proc/PID/task/

# 进程 IO 统计
cat /proc/PID/io
```

#### 3.2.2 lsof 文件描述符

```bash
# 查看进程打开的文件
lsof -p PID

# 统计打开的文件数量
lsof -p PID | wc -l

# 查看网络连接
lsof -p PID -i

# 查看端口监听
lsof -i :PORT
```

#### 3.2.3 进程资源限制

```bash
# 查看进程资源限制
cat /proc/PID/limits

# 常见限制项：
# Max open files    - 最大文件句柄数
# Max processes     - 最大进程数
# Max stack size    - 栈大小限制
# Max virtual memory - 虚拟内存限制

# ulimit 查看/修改
ulimit -a          # 查看所有限制
ulimit -n 65535    # 设置文件句柄限制
```

---

## 4. 线程层面排查

### 4.1 线程堆栈分析

#### 4.1.1 获取线程堆栈

```bash
# jstack - 输出 Java 线程堆栈
jstack PID > thread_dump.log

# 包含锁信息
jstack -l PID > thread_dump_locks.log

# 强制获取（可能导致暂停）
jstack -F PID > thread_dump_force.log

# 多次采样（间隔 10 秒）
for i in {1..5}; do
    jstack PID > "thread_$(date +%s).log"
    sleep 10
done

# VisualVM 方式
# 1. 启动 jvisualvm
# 2. 连接到目标进程
# 3. 切换到 "线程" 标签
# 4. 点击 "线程 Dump"
```

#### 4.1.2 分析线程堆栈

```bash
# 统计线程状态
grep -o 'java.lang.Thread.State: [A-Z_]*' thread_dump.log | sort | uniq -c | sort -rn

# 常见线程状态：
# NEW      - 新建
# RUNNABLE - 可运行/运行中
# BLOCKED  - 等待锁
# WAITING  - 等待条件
# TIMED_WAITING - 限时等待
# TERMINATED - 已终止

# 查找 BLOCKED 状态的线程
grep -A 20 'java.lang.Thread.State: BLOCKED' thread_dump.log

# 查找 RUNNABLE 状态的线程（高 CPU 嫌疑）
grep -A 20 'java.lang.Thread.State: RUNNABLE' thread_dump.log

# 查找死锁
jstack -l PID | grep -A 50 'Found one Java-level deadlock'

# 使用 awk 提取线程摘要
awk '/^"/{thread=$0} /java.lang.Thread.State:/{print thread, $0}' thread_dump.log
```

#### 4.1.3 线程堆栈示例

```
// 典型的死循环线程
"Thread-12" #12 daemon prio=5 os_prio=0 tid=0x00007f... nid=0x1a48 runnable [0x00007f...]
   java.lang.Thread.State: RUNNABLE
        at com.example.service.CpuIntensiveService.calculate(CpuIntensiveService.java:45)
        at com.example.service.CpuIntensiveService.process(CpuIntensiveService.java:23)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1136)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:635)
        at java.lang.Thread.run(Thread.java:840)

// 锁竞争线程
"Thread-15" #15 daemon prio=5 os_prio=0 tid=0x00007f... nid=0x1a4b waiting for monitor entry [0x00007f...]
   java.lang.Thread.State: BLOCKED (on object monitor)
        at com.example.service.SharedResource.getData(SharedResource.java:15)
        - waiting to lock <0x00000000cafe1234> (a com.example.service.SharedResource)
        at com.example.service.BusinessService.process(BusinessService.java:88)

// GC 相关线程
"VM Thread" os_prio=0 tid=0x00007f... nid=0x1a00 runnable
"GC task thread#0 (ParallelGC)" os_prio=0 tid=0x00007f... nid=0x1a01 runnable
```

### 4.2 高CPU线程定位

#### 4.2.1 top -H 线程级别

```bash
# 查看进程内所有线程的 CPU 使用
top -H -p PID

# 输出中：
# PID 列显示的是系统线程 ID（LWP）
# 需要转为 16 进制才能在 jstack 中找到
```

#### 4.2.2 pidstat 线程级别

```bash
# 实时监控线程 CPU 使用
pidstat -t -p PID 1

# 按 CPU 排序
pidstat -t -p PID 1 | sort -k 3 -rn

# 持续监控 60 秒
pidstat -t -p PID 1 60
```

#### 4.2.3 线程 ID 转换

```bash
# 系统线程 ID（十进制）转十六进制
printf '0x%x\n' THREAD_ID

# 示例：
# top 显示 PID=12345
# 转换：printf '0x%x\n' 12345
# 结果：0x3039
# 在 jstack 中搜索 "nid=0x3039"
```

#### 4.2.4 完整排查脚本

```bash
#!/bin/bash
# cpu_thread_check.sh - 一键定位高CPU线程

PID=$1
if [ -z "$PID" ]; then
    echo "用法: $0 <java_pid>"
    exit 1
fi

echo "=== 1. 进程信息 ==="
ps -p $PID -o pid,ppid,%cpu,%mem,rss,vsz,etime,cmd

echo ""
echo "=== 2. 线程 CPU 排行 ==="
top -H -p $PID -n 1 | awk 'NR>2 && $3+0>5.0 {printf "THREAD PID: %s, CPU: %s%%\n", $1, $3}' | head -10

echo ""
echo "=== 3. 转换为十六进制 ==="
top -H -p $PID -n 1 | awk 'NR>2 && $3+0>5.0 {print $1}' | while read tid; do
    printf "0x%x\n" $tid
done

echo ""
echo "=== 4. 获取线程堆栈 ==="
jstack -l $PID > /tmp/jstack_${PID}.log
echo "堆栈已保存到: /tmp/jstack_${PID}.log"

echo ""
echo "=== 5. 高CPU线程堆栈 ==="
top -H -p $PID -n 1 | awk 'NR>2 && $3+0>5.0 {print $1}' | while read tid; do
    HEX=$(printf '0x%x' $tid)
    echo "--- 线程 $HEX ---"
    grep -A 30 "nid=$HEX" /tmp/jstack_${PID}.log
    echo ""
done
```

---

## 5. JVM 层面排查

### 5.1 GC 日志分析

#### 5.1.1 开启 GC 日志

```bash
# JDK 8 及以下
-XX:+PrintGCDetails \
-XX:+PrintGCDateStamps \
-XX:+PrintTenuringDistribution \
-Xloggc:/path/to/gc.log

# JDK 9+ 统一日志格式
-Xlog:gc*:file=/path/to/gc.log:time,level,tags

# JDK 21+ 格式
-Xlog:gc*:file=gc.log:time,level,tags
```

#### 5.1.2 GC 日志分析工具

```bash
# 1. GC 日志分析器（GUI）
# https://github.com/kstyrc/gc-log-viewer

# 2. GCEasy（在线分析）
# https://gceasy.io/

# 3. 手动分析 GC 日志
# 查看 GC 频率
grep -c 'GC pause' gc.log

# 查看 Full GC
grep 'Full GC' gc.log

# 查看 GC 耗时
grep 'GC pause' gc.log | awk '{print $NF}' | sort -n | tail -10

# 查看堆使用变化
grep -o 'Heap usage:[0-9.]*' gc.log
```

#### 5.1.3 GC 异常判断

| 现象 | 可能原因 | 排查方法 |
|------|----------|----------|
| GC 频率 > 1次/秒 | 堆内存不足 | 检查 Xmx 配置 |
| Full GC 频繁 | 老年代泄漏或配置不当 | jmap -histo 分析 |
| GC 耗时 > 500ms | 堆过大或 STW 问题 | 调整堆大小或 GC 算法 |
| 永久区爆满 | 元空间泄漏 | 检查动态代理/类加载 |
| GC 后内存仍高 | 对象无法回收 | 分析对象引用链 |

### 5.2 JVM 参数检查

```bash
# 查看当前 JVM 参数
jcmd PID VM.flags

# 查看所有参数（含默认值）
jcmd PID VM.flags -all

# 查看系统属性
jcmd PID VM.system_properties

# 查看 JVM 版本信息
jcmd PID VM.version

# 查看命令行参数
ps -p PID -o args=
```

#### 关键 JVM 参数

| 参数 | 说明 | 建议 |
|------|------|------|
| `-Xms` | 初始堆大小 | 与 Xmx 相同，避免动态扩缩 |
| `-Xmx` | 最大堆大小 | 根据物理内存 70-80% 设置 |
| `-Xmn` | 年轻代大小 | 堆的 1/4 - 1/3 |
| `-XX:MetaspaceSize` | 元空间初始大小 | 256m - 512m |
| `-XX:MaxMetaspaceSize` | 元空间最大值 | 512m - 1024m |
| `-XX:+UseG1GC` | 使用 G1 收集器 | 推荐大堆使用 |
| `-XX:+UseZGC` | 使用 ZGC | 超低延迟场景 |
| `-Xss` | 线程栈大小 | 默认 512k，按需调整 |

### 5.3 内存状态分析

#### 5.3.1 jstat 统计

```bash
# GC 统计概览（每秒输出）
jstat -gcutil PID 1000

# 输出字段说明：
# S0C  - Survivor 0 容量
# S1C  - Survivor 1 容量
# EC   - Eden 容量
# OC   - Old 容量
# MC   - Metaspace 容量
# YGC  - Young GC 次数
# YGCT - Young GC 总时间
# FGC  - Full GC 次数
# FGCT - Full GC 总时间
# GCT  - GC 总时间

# 垃圾回收统计
jstat -gccapacity PID 1000

# 内存分代统计
jstat -newratio PID 1000
jstat -oldratio PID 1000
```

#### 5.3.2 jmap 堆分析

```bash
# 查看堆内存使用情况
jmap -heap PID

# 查看对象直方图
jmap -histo:live PID | head -30

# 导出堆快照（⚠️ 会导致 STW）
jmap -dump:live,format=b,file=heap.hprof PID

# 分析堆快照
# 使用 Eclipse MAT 或 VisualVM 打开 heap.hprof
```

#### 5.3.3 jinfo 配置检查

```bash
# 查看 JVM 配置
jinfo PID

# 查看特定参数
jinfo -flags PID

# 查看系统属性
jinfo -sysprops PID

# 查看 Java 版本信息
jinfo -sysprops PID | grep java.version
```

---

## 6. 常见原因与解决方案

### 6.1 死循环

#### 症状识别

- CPU 持续 100%（单核或多核）
- 进程无响应
- jstack 显示 RUNNABLE 状态线程
- 堆栈定位到业务代码

#### 典型堆栈

```java
java.lang.Thread.State: RUNNABLE
    at com.example.service.DataProcessor.processData(DataProcessor.java:45)
    at com.example.controller.ApiController.handleRequest(ApiController.java:23)
    at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1089)
```

#### 解决方案

1. **检查循环终止条件**
```java
// 错误示例
while (true) {
    // 缺少退出条件
    processData();
}

// 正确示例
int maxRetries = 3;
int retryCount = 0;
while (retryCount < maxRetries && !success) {
    success = processData();
    retryCount++;
}
```

2. **添加超时保护**
```java
ExecutorService executor = Executors.newFixedThreadPool(10);
Future<?> future = executor.submit(() -> {
    // 耗时操作
});
try {
    future.get(5, TimeUnit.SECONDS); // 超时保护
} catch (TimeoutException e) {
    future.cancel(true);
    throw new ServiceException("操作超时");
}
```

3. **使用限时流操作**
```java
// Java 9+ 限时 API
Optional<User> user = Optional.ofNullable(
    CompletableFuture.supplyAsync(() -> userService.getById(id))
        .orTimeout(5, TimeUnit.SECONDS)
        .get()
);
```

### 6.2 频繁 GC

#### 症状识别

- GC 日志频繁（> 1次/秒）
- Full GC 次数增加
- CPU 在 GC 线程上
- 应用响应延迟

#### GC 日志特征

```
# 频繁 Young GC
[GC (Allocation Failure) PSYoungGen: 512M->64M(512M), 0.023456s]

# Full GC
[Full GC (Ergonomics) PSYoungGen: 64M->0M(512M) ParOldGen: 4096M->4096M(4096M), 1.234567s]
```

#### 常见原因

| 原因 | 特征 | 解决方案 |
|------|------|----------|
| 堆内存过小 | GC 后内存仍高 | 增大 -Xmx |
| 内存泄漏 | Full GC 后对象不释放 | jmap -histo 分析 |
| 对象创建过多 | 高频 GC | 对象复用/池化 |
| 大对象分配 | 直接进入老年代 | 优化批量处理 |
| 动态类加载 | Metaspace 溢出 | 检查类加载器 |

#### 解决方案

1. **调整堆内存**
```bash
# 根据物理内存调整
# 物理内存 8G → Xmx=4G~6G
# 物理内存 16G → Xmx=8G~12G

java -Xms4g -Xmx4g -Xmn1g -XX:+UseG1GC ...
```

2. **对象池化**
```java
// 使用对象池复用频繁创建的对象
ObjectPool<Buffer> pool = new GenericObjectPool<>(() -> new Buffer(1024));

try (Buffer buf = pool.borrowObject()) {
    // 使用 buffer
} catch (Exception e) {
    pool.returnObject(buf);
}
```

3. **大对象处理**
```java
// 分批处理，避免一次性大对象
List<Record> allRecords = repository.findAll();
List<List<Record>> batches = Lists.partition(allRecords, 1000);

for (List<Record> batch : batches) {
    processBatch(batch);
}
```

4. **内存泄漏排查**
```bash
# 多次 dump 对比
jmap -dump:live,format=b,file=heap1.hprof PID
sleep 30
jmap -dump:live,format=b,file=heap2.hprof PID

# 使用 MAT 分析
# 1. 打开 heap2.hprof
# 2. 对比 heap1.hprof
# 3. 查看增长最多的对象类型
# 4. 分析引用链找 GC Root
```

### 6.3 线程阻塞/锁竞争

#### 症状识别

- CPU 不高但负载高
- 线程状态多为 BLOCKED
- 响应时间变长
- 吞吐量下降

#### 典型堆栈

```
// 等待锁释放
"Thread-15" BLOCKED
    at com.example.service.OrderService.processOrder(OrderService.java:88)
    - waiting to lock <0x00000000cafe1234>

// 死锁
"Thread-1" WAITING
    at com.example.service.ResourceA.operation(ResourceA.java:10)
    - waiting to lock <0x00000000aaaa1111> (a ResourceB)
    
"Thread-2" WAITING
    at com.example.service.ResourceB.operation(ResourceB.java:20)
    - waiting to lock <0x00000000bbbb2222> (a ResourceA)
```

#### 解决方案

1. **减少锁粒度**
```java
// 错误：大锁
public synchronized void processAll(List<Order> orders) {
    for (Order order : orders) {
        process(order);
    }
}

// 正确：细粒度
public void processAll(List<Order> orders) {
    for (Order order : orders) {
        String key = order.getUserId();
        synchronized (getLock(key)) {  // 分段锁
            process(order);
        }
    }
}
```

2. **使用读写锁**
```java
private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();

public String readData(String key) {
    rwLock.readLock().lock();
    try {
        return cache.get(key);
    } finally {
        rwLock.readLock().unlock();
    }
}

public void writeData(String key, String value) {
    rwLock.writeLock().lock();
    try {
        cache.put(key, value);
    } finally {
        rwLock.writeLock().unlock();
    }
}
```

3. **避免死锁**
```java
// 错误：循环等待
void transfer(Account from, Account to, Money amount) {
    synchronized (from) {
        synchronized (to) {  // 可能死锁
            from.debit(amount);
            to.credit(amount);
        }
    }
}

// 正确：按顺序加锁
void transfer(Account from, Account to, Money amount) {
    Account first = from.compareTo(to) < 0 ? from : to;
    Account second = from.compareTo(to) < 0 ? to : from;
    
    synchronized (first) {
        synchronized (second) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}
```

4. **使用无锁数据结构**
```java
// ConcurrentHashMap 替代 HashMap
private final Map<String, Data> cache = new ConcurrentHashMap<>();

// AtomicInteger 替代 synchronized 计数
private final AtomicInteger counter = new AtomicInteger(0);

// LongAdder 高并发计数
private final LongAdder totalCount = new LongAdder();
```

### 6.4 资源耗尽

#### 症状识别

- CPU 突然飙升后下降
- 伴随大量错误日志
- 数据库连接池耗尽
- 文件句柄不足

#### 排查命令

```bash
# 查看文件句柄使用
lsof -p PID | wc -l
cat /proc/PID/limits | grep "open files"

# 查看数据库连接
# HikariCP
jcmd PID VM.system_properties | grep hikari

# Tomcat 线程池
# 默认 200 线程
```

#### 解决方案

1. **连接池配置**
```java
# HikariCP 示例
spring:
  datasource:
    hikari:
      maximum-pool-size: 50      # 最大连接数
      minimum-idle: 10           # 最小空闲连接
      connection-timeout: 30000   # 连接超时
      idle-timeout: 600000       # 空闲超时
      max-lifetime: 1800000      # 连接最大生命
      validation-timeout: 5000   # 验证超时
```

2. **Tomcat 线程池**
```properties
server.tomcat.max-threads=200
server.tomcat.min-spare-threads=20
server.tomcat.accept-count=100
server.tomcat.max-connections=8000
```

3. **文件句柄配置**
```bash
# 临时修改
ulimit -n 65535

# 永久修改 /etc/security/limits.conf
* soft nofile 65535
* hard nofile 65535

# 应用配置
sysctl -w fs.file-max=1000000
```

### 6.5 外部依赖问题

#### 症状识别

- CPU 在网络线程
- 大量超时/重试日志
- 数据库/缓存响应慢
- 第三方接口异常

#### 排查步骤

```bash
# 网络状态
netstat -anp | grep PID
ss -s  # socket 统计

# 数据库连接
# MySQL
SHOW PROCESSLIST;
SHOW STATUS LIKE 'Threads%';

# Redis
redis-cli INFO stats
redis-cli INFO clients

# 接口延迟
# 从应用日志查找超时
grep 'timeout\|Timeout' app.log | tail -100
```

#### 解决方案

1. **添加超时配置**
```java
// HTTP 客户端超时
RestTemplate restTemplate = new RestTemplate();
restTemplate.getRequestFactory().setConnectTimeout(5000);  // 连接超时
restTemplate.getRequestFactory().setReadTimeout(10000);   // 读取超时

// Feign 超时
feign:
  client:
    config:
      default:
        connectTimeout: 5000
        readTimeout: 10000
```

2. **熔断降级**
```java
// Resilience4j 示例
CircuitBreakerConfig config = CircuitBreakerConfig.custom()
    .failureRateThreshold(50)
    .waitDurationInOpenState(Duration.ofMillis(10000))
    .slidingWindowSize(20)
    .build();

CircuitBreaker circuitBreaker = CircuitBreaker.of("backend", config);

// 降级调用
String result = circuitBreaker.executeSupplier(
    () -> remoteService.call(),
    () -> "fallback value"
);
```

3. **重试策略**
```java
// Spring Retry 示例
@Retryable(value = RemoteServiceException.class, 
           maxAttempts = 3, 
           backoff = @Backoff(delay = 1000, multiplier = 2))
public String callRemoteService() {
    return remoteService.call();
}

@Recover
public String recover(RemoteServiceException e) {
    return "fallback value";
}
```

---

## 7. 预防措施与最佳实践

### 7.1 监控告警配置

#### 基础监控

| 指标 | 告警阈值 | 严重级别 |
|------|----------|----------|
| CPU 使用率 | > 80% | Warning |
| CPU 使用率 | > 95% | Critical |
| 内存使用率 | > 85% | Warning |
| 内存使用率 | > 95% | Critical |
| GC 频率 | > 5次/分钟 | Warning |
| Full GC 次数 | > 2次/小时 | Critical |
| 响应时间 P99 | > 3s | Warning |
| 响应时间 P99 | > 5s | Critical |
| 错误率 | > 1% | Warning |
| 错误率 | > 5% | Critical |

#### Prometheus 配置示例

```yaml
# CPU 告警规则
groups:
  - name: cpu_alerts
    rules:
      - alert: HighCPUUsage
        expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 80
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "CPU使用率过高"
          description: "实例 {{ $labels.instance }} CPU使用率达到 {{ $value }}%"

      - alert: CriticalCPUUsage
        expr: 100 - (avg by(instance) (irate(node_cpu_seconds_total{mode="idle"}[5m])) * 100) > 95
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "CPU使用率严重告警"

# JVM 告警规则
  - name: jvm_alerts
    rules:
      - alert: HighGCFrequency
        expr: rate(jvm_gc_pause_seconds_count[5m]) > 0.2
        for: 10m
        
      - alert: MemoryUsageHigh
        expr: jvm_memory_bytes_used / jvm_memory_bytes_max > 0.85
        for: 5m
```

#### Grafana 仪表盘

```json
// 关键面板配置
{
  "panels": [
    {
      "title": "CPU 使用率",
      "targets": [{
        "expr": "100 - (avg(irate(node_cpu_seconds_total{mode=\"idle\"}[5m])) * 100)"
      }]
    },
    {
      "title": "JVM 堆内存",
      "targets": [{
        "expr": "jvm_memory_bytes_used{area=\"heap\"} / jvm_memory_bytes_max{area=\"heap\"} * 100"
      }]
    },
    {
      "title": "GC 暂停时间",
      "targets": [{
        "expr": "rate(jvm_gc_pause_seconds_sum[5m]) / rate(jvm_gc_pause_seconds_count[5m])"
      }]
    },
    {
      "title": "线程状态分布",
      "targets": [{
        "expr": "jvm_threads_state"
      }]
    }
  ]
}
```

### 7.2 代码层面最佳实践

#### 7.2.1 避免创建不必要的对象

```java
// ❌ 错误：循环中创建对象
for (int i = 0; i < 10000; i++) {
    String key = "prefix_" + i;  // 每次创建新字符串
    Map<String, Object> map = new HashMap<>();  // 每次创建新 Map
    process(key, map);
}

// ✅ 正确：对象复用
Map<String, Object> reusableMap = new HashMap<>();
for (int i = 0; i < 10000; i++) {
    String key = "prefix_" + i;
    reusableMap.clear();  // 复用而非新建
    process(key, reusableMap);
}
```

#### 7.2.2 使用高效数据结构

```java
// ❌ 错误：LinkedList 频繁随机访问
List<User> users = new LinkedList<>();
User user = users.get(index);  // O(n)

// ✅ 正确：ArrayList 随机访问
List<User> users = new ArrayList<>();
User user = users.get(index);  // O(1)

// ❌ 错误：HashMap 已知容量
Map<String, User> userMap = new HashMap<>();
for (User user : users) {
    userMap.put(user.getId(), user);  // 触发多次扩容
}

// ✅ 正确：指定初始容量
Map<String, User> userMap = new HashMap<>(users.size() / 0.75f);
```

#### 7.2.3 合理的缓存使用

```java
// ❌ 错误：缓存无过期时间
@Cacheable
public User getUser(Long id) {
    return userRepository.findById(id);
}

// ✅ 正确：设置合理过期时间
@Cacheable(value = "users", key = "#id", unless = "#result == null")
@CacheEvict(value = "users", key = "#id", beforeInvocation = false)
public User getUser(Long id) {
    return userRepository.findById(id);
}

// 使用 Caffeine（高性能本地缓存）
Cache<Long, User> userCache = Caffeine.newBuilder()
    .maximumSize(10_000)
    .expireAfterWrite(10, TimeUnit.MINUTES)
    .refreshAfterWrite(1, TimeUnit.MINUTES)
    .build(key -> userRepository.findById(key));
```

### 7.3 JVM 参数最佳实践

```bash
# 生产环境推荐配置
java \
    -Xms4g \                              # 初始堆 = 最大堆，避免动态扩缩
    -Xmx4g \
    -Xmn1g \                              # 年轻代 1/4 堆
    -XX:MetaspaceSize=256m \              # 元空间
    -XX:MaxMetaspaceSize=512m \
    -XX:+UseG1GC \                        # G1 收集器（适合大堆）
    -XX:MaxGCPauseMillis=200 \            # GC 暂停目标
    -XX:G1HeapRegionSize=4m \             # G1 Region 大小
    -XX:+ParallelRefProcEnabled \         # 并行引用处理
    -XX:+HeapDumpOnOutOfMemoryError \     # OOM 自动 dump
    -XX:HeapDumpPath=/logs/heapdump.hprof \
    -Xlog:gc*:file=/logs/gc.log:time,level,tags \
    -Xss1m \                              # 线程栈大小
    -Djava.security.egd=file:/dev/urandom \ # 加快随机数生成
    -Dfile.encoding=UTF-8 \
    -jar application.jar
```

### 7.4 容器环境注意事项

#### Docker 资源限制

```dockerfile
# Dockerfile 示例
FROM eclipse-temurin:21-jre-alpine

# JVM 参数使用容器感知模式
ENV JAVA_OPTS="-XX:+UseContainerSupport \
               -XX:MaxRAMPercentage=75.0 \
               -XX:InitialRAMPercentage=50.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

#### Kubernetes 资源配置

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: java-app
spec:
  replicas: 3
  template:
    spec:
      containers:
        - name: java-app
          image: java-app:latest
          resources:
            requests:
              cpu: "2"
              memory: "4Gi"
            limits:
              cpu: "4"
              memory: "8Gi"
          env:
            - name: JAVA_OPTS
              value: >-
                -XX:+UseContainerSupport
                -XX:MaxRAMPercentage=75.0
                -XX:InitialRAMPercentage=50.0
          livenessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 60
            periodSeconds: 10
          readinessProbe:
            httpGet:
              path: /actuator/health
              port: 8080
            initialDelaySeconds: 30
            periodSeconds: 5
```

---

## 8. 应急处理流程

### 8.1 发现问题

```bash
# 1. 收到告警或用户反馈
# 2. 快速确认
curl -s http://health-check-url
top -o cpu -n 1 | head -20
```

### 8.2 初步处理

```bash
# 方案 A：快速缓解
# 1. 扩容（增加副本数）
kubectl scale deployment java-app --replicas=5

# 2. 限流降级
# 关闭非核心功能
# 开启熔断器

# 方案 B：根因分析（不影响服务时）
# 1. 获取线程堆栈
jstack PID > thread_dump_$(date +%s).log

# 2. 获取 GC 日志
# 从日志系统查询

# 3. 分析原因
```

### 8.3 深入分析

```bash
# 1. 分析线程堆栈
# 查看高 CPU 线程
# 查看 BLOCKED 线程
# 查找死锁

# 2. 内存分析
jmap -histo:live PID | head -50
# 对比多次 dump

# 3. 全链路追踪
# 使用 SkyWalking/Zipkin/Jaeger
```

### 8.4 修复问题

```bash
# 1. 代码修复
# - 修复死循环
# - 优化锁粒度
# - 添加超时保护

# 2. 配置调整
# - 调整 JVM 参数
# - 增加资源
# - 优化连接池

# 3. 发布验证
# - 灰度发布
# - 监控指标
# - 验证问题解决
```

### 8.5 事后复盘

1. **问题根因分析（RCA）**
   - 直接原因
   - 根本原因
   - 触发条件

2. **改进措施**
   - 代码改进
   - 监控完善
   - 流程优化

3. **文档更新**
   - 更新排查手册
   - 更新应急预案
   - 分享经验

---

## 附录

### A. 常用工具速查表

| 工具 | 类型 | 用途 | 安装 |
|------|------|------|------|
| `top` | 系统 | 进程监控 | 内置 |
| `htop` | 系统 | 增强版 top | `brew install htop` |
| `vmstat` | 系统 | 虚拟内存统计 | 内置 |
| `pidstat` | 系统 | 进程级统计 | `brew install sysstat` |
| `sar` | 系统 | 系统活动报告 | `brew install sysstat` |
| `dstat` | 系统 | 多功能统计 | `brew install dstat` |
| `mpstat` | 系统 | CPU 多核统计 | `brew install sysstat` |
| `jstat` | JVM | GC 统计 | JDK 内置 |
| `jmap` | JVM | 堆内存分析 | JDK 内置 |
| `jstack` | JVM | 线程堆栈 | JDK 内置 |
| `jinfo` | JVM | 配置查看 | JDK 内置 |
| `jcmd` | JVM | 综合命令 | JDK 内置 |
| `VisualVM` | GUI | 可视化分析 | JDK 内置或单独下载 |
| `MAT` | GUI | 堆内存分析 | Eclipse MAT |
| `Arthas` | CLI | Java 诊断 | 阿里巴巴开源 |

### B. Arthas 常用命令

```bash
# 安装
curl -O https://arthas.aliyun.com/arthas-boot.jar
java -jar arthas-boot.jar PID

# 常用诊断命令
# 1. dashboard - 实时仪表盘
dashboard

# 2. thread - 线程分析
thread                        # 查看所有线程
thread -n 5                   # 查看 CPU 最高的 5 个线程
thread -b                     # 查找阻塞线程
thread -i                     # 查看线程状态

# 3. trace - 方法调用链耗时
trace com.example.service.CpuIntensiveService calculate '#cost > 100'

# 4. watch - 方法参数/返回值
watch com.example.service.OrderService processOrder '{params,returnObj}' -n 5

# 5. monitor - 方法调用统计
monitor com.example.service.OrderService processOrder -c 5

# 6. flamegraph - 火焰图
profiler start
# 等待采样
profiler stop
```

### C. 一键排查脚本

```bash
#!/bin/bash
# cpu_emergency_check.sh - CPU 飙升应急排查脚本

PID=$1
OUTPUT_DIR="./cpu_check_$(date +%Y%m%d_%H%M%S)"

if [ -z "$PID" ]; then
    echo "用法: $0 <java_pid>"
    echo "示例: $0 $(pgrep -f spring-boot)"
    exit 1
fi

mkdir -p "$OUTPUT_DIR"

echo "=== CPU 飙升应急排查 ==="
echo "时间: $(date)"
echo "PID: $PID"
echo "输出目录: $OUTPUT_DIR"
echo ""

# 1. 基础信息
echo "[1] 收集基础信息..."
{
    echo "=== 进程状态 ==="
    ps -p $PID -o pid,ppid,%cpu,%mem,rss,vsz,etime,cmd
    
    echo ""
    echo "=== /proc/PID/status ==="
    grep -E "VmRSS|VmSize|Threads" /proc/$PID/status
    
    echo ""
    echo "=== 系统负载 ==="
    uptime
    
    echo ""
    echo "=== 内存使用 ==="
    free -m
} > "$OUTPUT_DIR/01_basic_info.txt"

# 2. 线程堆栈
echo "[2] 获取线程堆栈..."
jstack -l $PID > "$OUTPUT_DIR/02_jstack_threads.log" 2>&1

# 3. JVM 信息
echo "[3] 收集 JVM 信息..."
{
    echo "=== VM Flags ==="
    jcmd $PID VM.flags 2>&1
    
    echo ""
    echo "=== GC 统计 ==="
    jstat -gcutil $PID 1000 10 2>&1
    
    echo ""
    echo "=== 堆直方图 ==="
    jmap -histo:live $PID 2>&1 | head -50
} > "$OUTPUT_DIR/03_jvm_info.txt"

# 4. 高 CPU 线程
echo "[4] 分析高 CPU 线程..."
{
    echo "=== 线程 CPU 排行 ==="
    top -H -p $PID -n 1 | head -30
    
    echo ""
    echo "=== 高 CPU 线程堆栈 ==="
    HIGH_CPU_THREADS=$(top -H -p $PID -n 1 | awk 'NR>2 && $3+0>10.0 {print $1}')
    for TID in $HIGH_CPU_THREADS; do
        HEX=$(printf '0x%x' $TID)
        echo "--- 线程 $TID ($HEX) ---"
        grep -A 30 "nid=$HEX" "$OUTPUT_DIR/02_jstack_threads.log"
        echo ""
    done
} > "$OUTPUT_DIR/04_high_cpu_threads.txt"

# 5. 汇总
echo "[5] 生成汇总报告..."
{
    echo "=== CPU 飙升排查报告 ==="
    echo "生成时间: $(date)"
    echo "PID: $PID"
    echo ""
    
    echo "### 高 CPU 线程"
    top -H -p $PID -n 1 | awk 'NR>2 && $3+0>10.0 {printf "线程 %s (CPU: %s%%)\n", $1, $3}'
    
    echo ""
    echo "### 建议操作"
    echo "1. 查看 $OUTPUT_DIR/04_high_cpu_threads.txt 了解高 CPU 线程详情"
    echo "2. 查看 $OUTPUT_DIR/02_jstack_threads.log 了解完整堆栈"
    echo "3. 根据堆栈定位代码问题"
    echo "4. 如需紧急处理，可先重启服务或扩容"
} > "$OUTPUT_DIR/SUMMARY.md"

echo ""
echo "=== 排查完成 ==="
echo "输出目录: $OUTPUT_DIR"
echo "汇总报告: $OUTPUT_DIR/SUMMARY.md"
echo ""
echo "建议立即查看: $OUTPUT_DIR/04_high_cpu_threads.txt"
```

---

## 参考资源

- [Oracle JVM Troubleshooting Guide](https://docs.oracle.com/en/java/javase/21/troubleshooting/)
- [Java Platform Performance](https://docs.oracle.com/en/java/javase/21/management/)
- [Alibaba Arthas](https://arthas.aliyun.com/)
- [Eclipse MAT](https://eclipse.dev/mat/)
- [GC Easy](https://gceasy.io/)

---

> **文档版本**: v1.0  
> **维护者**: 运维/研发团队  
> **更新日期**: 2026-07-28