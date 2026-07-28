# Java应用CPU使用率异常飙升问题完整解决方案文档

## Overview
- **Summary**: 本文档提供在Rancher容器化环境中Java应用CPU使用率异常飙升问题的完整解决方案，包括问题模拟、系统化排查流程及针对常见原因的优化方案。
- **Purpose**: 帮助开发和运维团队快速定位、诊断和解决Java应用在Rancher部署环境中的CPU性能问题。
- **Target Users**: Java后端开发工程师、DevOps工程师、SRE工程师

## Goals
- 提供可稳定复现Java应用CPU飙升场景的测试环境搭建指南
- 建立系统化的CPU问题排查流程
- 提供针对常见CPU飙升原因的解决方案和代码修复建议
- 包含命令示例、日志分析样例及工具使用指南

## Non-Goals (Out of Scope)
- 不涉及Rancher平台本身的安装和配置
- 不涉及Java应用业务逻辑的具体实现
- 不涉及硬件层面的CPU性能优化

## Background & Context
- Rancher是一个企业级容器管理平台，用于部署、管理和监控Kubernetes集群
- Java应用在容器环境中运行时，CPU问题表现形式与传统虚拟机环境有所不同
- 容器资源限制(cpu limit/request)会影响JVM的行为和GC策略
- 常见的CPU飙升原因包括：死循环、频繁GC、线程阻塞、资源竞争等

## Functional Requirements
- **FR-1**: 设计可稳定复现CPU飙升的测试环境和负载模拟方案
- **FR-2**: 提供Rancher平台监控指标分析方法
- **FR-3**: 提供容器内进程状态检查步骤
- **FR-4**: 提供JVM线程堆栈分析方法
- **FR-5**: 提供CPU热点方法定位工具使用指南
- **FR-6**: 针对常见CPU飙升原因提供解决方案

## Non-Functional Requirements
- **NFR-1**: 文档中的命令和步骤应可在真实环境中执行
- **NFR-2**: 排查流程应系统化、可重复
- **NFR-3**: 解决方案应具有可操作性和可验证性

## Constraints
- **Technical**: 假设读者具备Linux基础命令、Docker/Kubernetes基础知识、JVM基础概念
- **Dependencies**: 需要Rancher平台、kubectl、JDK工具(jstack, jmap, jstat, jcmd)、Arthas等工具

## Assumptions
- [Assumption 1]: Rancher平台已部署并正常运行
- [Assumption 2]: Java应用已通过Rancher部署在Kubernetes集群中
- [Assumption 3]: 集群节点可通过kubectl访问
- [Assumption 4]: 应用容器内已安装JDK或可执行jcmd等工具

## Acceptance Criteria

### AC-1: 测试环境搭建完成
- **Given**: 具备Rancher管理的Kubernetes集群
- **When**: 按照文档步骤部署测试应用和负载模拟工具
- **Then**: 能够稳定复现CPU使用率超过80%的场景
- **Verification**: `programmatic`
- **Notes**: 通过Rancher监控面板确认CPU使用率

### AC-2: Rancher监控指标分析
- **Given**: Java应用出现CPU飙升
- **When**: 在Rancher监控面板查看Pod CPU使用率、容器CPU使用率、节点CPU使用率
- **Then**: 能够识别出CPU使用率异常的Pod和容器
- **Verification**: `human-judgment`
- **Notes**: 对比历史基线数据判断是否异常

### AC-3: 容器内进程状态检查
- **Given**: 已定位CPU异常的Pod
- **When**: 进入容器执行top、ps等命令
- **Then**: 能够识别出占用CPU的Java进程和线程
- **Verification**: `programmatic`
- **Notes**: 确认PID和线程ID

### AC-4: JVM线程堆栈分析
- **Given**: 已获取Java进程ID
- **When**: 执行jstack命令获取线程堆栈
- **Then**: 能够识别出CPU密集型线程和可能的问题代码位置
- **Verification**: `human-judgment`
- **Notes**: 分析RUNNABLE状态的线程

### AC-5: CPU热点方法定位
- **Given**: Java进程CPU使用率超过80%
- **When**: 使用Arthas或jprofiler进行CPU profiling
- **Then**: 能够定位到消耗CPU最多的方法
- **Verification**: `programmatic`
- **Notes**: 获取方法执行耗时占比

### AC-6: 死循环问题修复
- **Given**: 定位到死循环代码
- **When**: 修改代码添加终止条件或优化循环逻辑
- **Then**: CPU使用率恢复正常(低于30%)
- **Verification**: `programmatic`

### AC-7: 频繁GC问题优化
- **Given**: jstat显示频繁GC或GC时间过长
- **When**: 调整JVM堆大小参数或优化代码减少对象创建
- **Then**: GC频率降低，CPU使用率恢复正常
- **Verification**: `programmatic`

### AC-8: 线程阻塞问题解决
- **Given**: jstack显示大量线程处于BLOCKED状态
- **When**: 分析锁竞争并优化同步代码
- **Then**: BLOCKED线程数量减少，CPU使用率恢复正常
- **Verification**: `programmatic`

## Open Questions
- [ ] 是否需要考虑容器资源限制对JVM的影响？
- [ ] 是否需要包含容器网络I/O对CPU的影响分析？
- [ ] 是否需要提供针对特定Java框架(Spring Boot)的优化建议？

---

## 1. 问题模拟

### 1.1 测试环境搭建

#### 1.1.1 部署测试应用

创建一个包含CPU密集型代码的Spring Boot应用：

```yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: cpu-test-app
spec:
  replicas: 1
  selector:
    matchLabels:
      app: cpu-test-app
  template:
    metadata:
      labels:
        app: cpu-test-app
    spec:
      containers:
      - name: cpu-test-app
        image: your-registry/cpu-test-app:1.0.0
        resources:
          requests:
            cpu: "500m"
            memory: "512Mi"
          limits:
            cpu: "2"
            memory: "1Gi"
        ports:
        - containerPort: 8080
```

#### 1.1.2 模拟CPU密集型接口

```java
@RestController
@RequestMapping("/cpu")
public class CpuTestController {
    
    @GetMapping("/loop")
    public String deadLoop() {
        // 死循环模拟
        while (true) {
            // CPU密集型计算
            double result = Math.random() * Math.random();
        }
    }
    
    @GetMapping("/busy")
    public String busyCalculation(@RequestParam int iterations) {
        long sum = 0;
        for (long i = 0; i < iterations; i++) {
            sum += Math.sqrt(i) * Math.sin(i);
        }
        return "Result: " + sum;
    }
    
    @GetMapping("/gc")
    public String triggerGc() {
        // 大量对象创建触发频繁GC
        List<String> list = new ArrayList<>();
        for (int i = 0; i < 1000000; i++) {
            list.add(new String("test" + i));
        }
        return "Created " + list.size() + " objects";
    }
}
```

#### 1.1.3 部署负载模拟工具

```yaml
apiVersion: v1
kind: Pod
metadata:
  name: load-test
spec:
  containers:
  - name: load-test
    image: busybox
    command: ["sh", "-c", "while true; do wget -q -O /dev/null http://cpu-test-app:8080/cpu/busy?iterations=10000000; sleep 0.1; done"]
```

### 1.2 负载模拟方法

| 负载类型 | 模拟方法 | 参数配置 | 预期CPU使用率 |
|---------|---------|---------|-------------|
| 死循环 | 调用 `/cpu/loop` | 单次调用，无参数 | 95-100% |
| CPU密集计算 | 调用 `/cpu/busy` | iterations=10000000 | 60-80% |
| 频繁GC | 调用 `/cpu/gc` | 循环调用，间隔1秒 | 40-60% |

### 1.3 预期结果

在Rancher监控面板中应观察到：
- Pod CPU使用率超过80%持续5分钟以上
- 容器CPU使用率接近或达到资源限制
- 节点CPU使用率显著上升

---

## 2. 问题排查

### 2.1 Rancher平台监控指标分析

#### 2.1.1 查看Pod CPU使用率

1. 登录Rancher平台
2. 进入对应的集群和命名空间
3. 查看工作负载列表
4. 找到目标Pod，查看CPU使用率图表

**命令行方式：**

> **前提条件**：集群需要安装Metrics Server才能使用`kubectl top`命令。

```bash
# 查看Pod CPU使用率
kubectl top pods -n <namespace>

# 持续监控
watch -n 2 kubectl top pods -n <namespace>
```

**预期输出：**
```
NAME            CPU(cores)   MEMORY(bytes)   
cpu-test-app-0  1980m        512Mi           
```

#### 2.1.2 查看容器CPU使用率

```bash
# 查看容器级别的CPU使用率
kubectl get pod <pod-name> -n <namespace> -o jsonpath='{.status.containerStatuses[*].name}'
kubectl exec -it <pod-name> -n <namespace> -- top
```

**判断标准：**
- CPU使用率超过80%持续5分钟以上视为异常
- 接近或达到资源限制(cpu limit)需要关注

### 2.2 容器内进程状态检查

#### 2.2.1 进入容器

```bash
# 进入容器
kubectl exec -it <pod-name> -n <namespace> -- /bin/bash

# 如果容器没有bash，使用sh
kubectl exec -it <pod-name> -n <namespace> -- /bin/sh
```

#### 2.2.2 查看进程状态

```bash
# 查看CPU占用前10的进程
top -b -n 1 | head -15

# 查看Java进程
ps aux | grep java

# 查看线程状态
ps -eLf | grep java | head -20
```

**预期输出示例：**
```
PID USER      PR  NI    VIRT    RES    SHR S  %CPU %MEM     TIME+ COMMAND
1 root      20   0 4321232 523864  16860 R  99.8 12.8  25:32.15 java
```

**判断标准：**
- Java进程CPU使用率超过80%需要进一步分析
- R状态(运行中)的线程数量过多需要关注

#### 2.2.3 获取线程CPU使用情况

```bash
# 查看线程级别的CPU使用
top -H -p <java-pid> -b -n 1 | head -20

# 转换线程ID为十六进制
printf "%x\n" <thread-id>
```

### 2.3 JVM线程堆栈分析

#### 2.3.1 获取线程堆栈

```bash
# 使用jstack获取线程堆栈
jstack <java-pid> > thread_dump.log

# 如果没有jstack，使用jcmd
jcmd <java-pid> Thread.print > thread_dump.log

# 如果容器内没有JDK工具，从宿主机获取
kubectl cp <namespace>/<pod-name>:/proc/<java-pid>/task/<thread-id>/stack stack.log
```

#### 2.3.2 分析线程堆栈

**查看RUNNABLE状态的线程：**
```bash
grep -A 5 "RUNNABLE" thread_dump.log | head -50
```

**查看CPU占用最高的线程：**
```bash
# 获取线程ID的十六进制值
printf "%x\n" <thread-id>

# 在线程堆栈中查找对应线程
grep -A 20 "nid=0x<hex-thread-id>" thread_dump.log
```

**分析要点：**
- 大量RUNNABLE状态线程可能表示CPU密集型操作
- 大量BLOCKED状态线程可能表示锁竞争
- 大量WAITING/TIMED_WAITING状态线程可能表示线程池阻塞

**示例输出：**
```
"http-nio-8080-exec-1" #23 daemon prio=5 os_prio=0 tid=0x00007f9b00001000 nid=0x41 runnable [0x00007f9af8000000]
   java.lang.Thread.State: RUNNABLE
        at com.example.CpuTestController.deadLoop(CpuTestController.java:15)
        at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
        at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
        at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
        at java.lang.reflect.Method.invoke(Method.java:498)
```

### 2.4 CPU热点方法定位

#### 2.4.1 使用Arthas

```bash
# 在容器内安装Arthas
curl -O https://arthas.aliyun.com/arthas-boot.jar
java -jar arthas-boot.jar

# 选择目标Java进程
# 输入数字选择进程

# 查看CPU使用情况
thread

# 查看CPU占用最高的线程
thread -n 3

# 执行CPU profiling
profiler start
# 等待30秒
profiler stop --format html

# 查看方法执行耗时（热点方法）
trace com.example.CpuTestController *
```

**Arthas命令说明：**

| 命令 | 用途 | 参数 |
|-----|------|-----|
| `thread` | 查看线程状态 | `-n 3`: 显示CPU占用最高的3个线程 |
| `thread -b` | 查看锁阻塞 | 无 |
| `profiler start` | 启动CPU profiling | 无 |
| `profiler stop` | 停止并生成报告 | `--format html`: 生成HTML格式报告 |
| `trace` | 方法调用追踪 | `trace com.example.ClassName methodName` |
| `watch` | 方法执行数据观测 | `watch com.example.ClassName methodName "{params,returnObj}"` |
| `monitor` | 方法执行监控 | `monitor -c 5 com.example.ClassName methodName` |

#### 2.4.2 使用jcmd

```bash
# 查看JVM进程
jcmd

# 启动CPU profiling
jcmd <java-pid> Compiler.directives_add "{exclude: \"java/*\", exclude: \"javax/*\"}"
jcmd <java-pid> JFR.start name=cpurecording settings=profile duration=30s
jcmd <java-pid> JFR.stop name=cpurecording filename=cpu.jfr

# 使用jfr分析工具打开cpu.jfr文件
```

#### 2.4.3 使用jstat分析GC

```bash
# 查看GC统计信息
jstat -gc <java-pid> 1000 10

# 查看详细GC信息
jstat -gcutil <java-pid> 1000 10
```

**jstat输出字段说明：**

| 字段 | 含义 | 判断标准 |
|-----|------|---------|
| S0C/S1C | Survivor区大小 | - |
| S0U/S1U | Survivor区已使用 | - |
| EC | Eden区大小 | - |
| EU | Eden区已使用 | - |
| OC | Old区大小 | - |
| OU | Old区已使用 | 超过80%需要关注 |
| YGC | Young GC次数 | 频繁YGC需要关注 |
| YGCT | Young GC耗时 | 单次超过100ms需要关注 |
| FGC | Full GC次数 | 频繁FGC需要关注 |
| FGCT | Full GC耗时 | 单次超过1秒需要关注 |

**预期输出示例：**
```
 S0C    S1C    S0U    S1U      EC       EU        OC         OU       YGC    YGCT    FGC    FGCT     GCT   
 256.0  256.0   0.0    64.0   2048.0   1890.0    5120.0     4890.0      15    0.852     3    2.341    3.193
```

#### 2.4.4 使用jmap分析堆内存

```bash
# 查看堆内存使用情况
jmap -heap <java-pid>

# 查看堆中对象统计
jmap -histo <java-pid> | head -30

# 生成堆转储文件（注意：此操作会导致应用暂停，建议在低峰期执行）
# -XX:+HeapDumpOnOutOfMemoryError 参数可以配置在OOM时自动生成堆转储
jmap -dump:format=b,file=heap_dump.hprof <java-pid>
```

**分析要点：**
- 大对象数量过多可能导致频繁GC
- 某些类的实例数量异常增长需要关注

---

## 3. 解决方案

### 3.1 死循环问题

#### 3.1.1 问题特征

- CPU使用率接近100%
- jstack显示线程处于RUNNABLE状态，停留在同一代码位置
- 没有IO等待或锁竞争

#### 3.1.2 代码修复建议

**问题代码：**
```java
@GetMapping("/loop")
public String deadLoop() {
    while (true) {
        double result = Math.random() * Math.random();
    }
}
```

**修复后代码：**
```java
@GetMapping("/loop")
public String loopWithLimit(@RequestParam(defaultValue = "1000000") int iterations) {
    double result = 0;
    for (int i = 0; i < iterations; i++) {
        result += Math.random() * Math.random();
    }
    return "Result: " + result;
}
```

#### 3.1.3 预防措施

- 添加循环终止条件
- 设置操作超时时间
- 使用线程池控制并发
- 添加监控告警

### 3.2 频繁GC问题

#### 3.2.1 问题特征

- jstat显示频繁YGC或FGC
- GC耗时占比高
- CPU使用率波动较大
- 内存使用持续增长

#### 3.2.2 JVM参数优化

**调整堆大小：**
```bash
# 增加堆大小
-Xms2g -Xmx2g -Xmn1g

# 调整Survivor区比例
-XX:SurvivorRatio=8

# 使用G1垃圾收集器
-XX:+UseG1GC
-XX:MaxGCPauseMillis=200
```

**容器环境下的JVM参数建议：**
```bash
# 根据容器资源限制设置
-XX:InitialRAMPercentage=75.0
-XX:MaxRAMPercentage=75.0
-XX:+UseContainerSupport
```

#### 3.2.3 代码优化建议

**问题代码：**
```java
public String processData(List<String> data) {
    StringBuilder result = new StringBuilder();
    for (String item : data) {
        // 每次循环创建新对象
        String processed = new String(item.toUpperCase());
        result.append(processed);
    }
    return result.toString();
}
```

**优化后代码：**
```java
public String processData(List<String> data) {
    StringBuilder result = new StringBuilder();
    for (String item : data) {
        // 直接使用方法返回值，避免创建新对象
        result.append(item.toUpperCase());
    }
    return result.toString();
}
```

**常见优化点：**
- 避免在循环中创建对象
- 使用StringBuilder代替String拼接
- 复用对象池中的对象
- 及时释放无用对象的引用

### 3.3 线程阻塞问题

#### 3.3.1 问题特征

- jstack显示大量BLOCKED状态线程
- CPU使用率不高但响应缓慢
- 线程等待获取锁

#### 3.3.2 锁竞争分析

```bash
# 使用Arthas查看锁信息
thread -b

# 查看死锁
jstack <java-pid> | grep -A 50 "Deadlock"
```

#### 3.3.3 代码优化建议

**问题代码：**
```java
public synchronized String getResource(String key) {
    // 整个方法加锁，并发性能差
    if (cache.containsKey(key)) {
        return cache.get(key);
    }
    String value = loadFromDatabase(key);
    cache.put(key, value);
    return value;
}
```

**优化后代码（使用双重检查锁）：**
```java
public String getResource(String key) {
    // 先检查，不加锁
    if (!cache.containsKey(key)) {
        // 只在需要时加锁
        synchronized (this) {
            // 双重检查
            if (!cache.containsKey(key)) {
                String value = loadFromDatabase(key);
                cache.put(key, value);
            }
        }
    }
    return cache.get(key);
}
```

**使用并发容器：**
```java
// 使用ConcurrentHashMap
private ConcurrentHashMap<String, String> cache = new ConcurrentHashMap<>();

public String getResource(String key) {
    return cache.computeIfAbsent(key, this::loadFromDatabase);
}
```

### 3.4 资源竞争问题

#### 3.4.1 问题特征

- CPU使用率高但业务处理慢
- 大量线程等待IO或外部资源
- 数据库连接池耗尽

#### 3.4.2 资源使用分析

**查看线程等待状态（使用Arthas）：**
```bash
# 查看WAITING状态的线程
thread --state WAITING

# 查看BLOCKED状态的线程
thread --state BLOCKED

# 查看TIMED_WAITING状态的线程（可能在等待IO）
thread --state TIMED_WAITING
```

**查看数据库连接池状态（使用Arthas ognl）：**
```bash
# 查看HikariCP连接池状态
ognl '@com.zaxxer.hikari.HikariDataSource@getInstance().getHikariPoolMXBean().getActiveConnections()'

# 查看连接池信息
watch com.zaxxer.hikari.pool.HikariPool getPoolState 'returnObj'
```

**查看线程池状态（使用Arthas）：**
```bash
# 查看所有线程池信息
thread -pool

# 查看特定线程池的详细信息
watch java.util.concurrent.ThreadPoolExecutor getActiveCount 'returnObj'
```

#### 3.4.3 配置优化建议

**线程池配置：**
```java
@Bean
public ExecutorService taskExecutor() {
    ThreadPoolExecutor executor = new ThreadPoolExecutor(
        10,                              // corePoolSize
        50,                              // maximumPoolSize
        60L, TimeUnit.SECONDS,           // keepAliveTime
        new LinkedBlockingQueue<>(100),  // workQueue
        new ThreadFactory() {
            private final AtomicInteger counter = new AtomicInteger(0);
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "task-executor-" + counter.incrementAndGet());
            }
        },
        new ThreadPoolExecutor.CallerRunsPolicy()  // 拒绝策略
    );
    executor.allowCoreThreadTimeOut(true);
    return executor;
}
```

**数据库连接池配置：**
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      connection-timeout: 30000
      max-lifetime: 1800000
```

### 3.5 代码优化最佳实践

#### 3.5.1 使用高效的数据结构

```java
// 避免
List<String> list = new ArrayList<>();
for (int i = 0; i < 100000; i++) {
    if (list.contains("key" + i)) { ... }
}

// 推荐
Set<String> set = new HashSet<>();
for (int i = 0; i < 100000; i++) {
    if (set.contains("key" + i)) { ... }
}
```

#### 3.5.2 批量操作

```java
// 避免
for (User user : users) {
    userRepository.save(user);  // 每次都执行SQL
}

// 推荐
userRepository.saveAll(users);  // 批量保存
```

#### 3.5.3 使用缓存

```java
@Cacheable(value = "users", key = "#id")
public User getUserById(Long id) {
    return userRepository.findById(id).orElse(null);
}
```

---

## 4. 排查流程总结

### 4.1 快速排查流程图

```
CPU飙升问题
    │
    ▼
┌─────────────────────────────────┐
│ 1. Rancher监控面板确认异常       │
│    - Pod CPU使用率              │
│    - 容器CPU使用率              │
│    - 对比历史基线               │
└─────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────┐
│ 2. 进入容器检查进程             │
│    - top查看CPU占用             │
│    - ps查看Java进程PID          │
└─────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────┐
│ 3. 初步判断问题类型             │
│    - CPU接近100% → 死循环/热点  │
│    - GC频繁 → 内存问题          │
│    - 线程阻塞 → 锁竞争          │
└─────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────┐
│ 4. 使用工具深入分析             │
│    - jstack: 线程堆栈           │
│    - jstat: GC统计              │
│    - Arthas: CPU profiling      │
│    - jmap: 堆内存分析           │
└─────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────┐
│ 5. 定位问题代码                 │
│    - 分析热点方法               │
│    - 检查锁竞争                 │
│    - 查看GC日志                 │
└─────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────┐
│ 6. 实施优化方案                 │
│    - 代码修复                   │
│    - 参数调整                   │
│    - 配置优化                   │
└─────────────────────────────────┘
    │
    ▼
┌─────────────────────────────────┐
│ 7. 验证修复效果                 │
│    - 监控CPU使用率              │
│    - 对比优化前后数据           │
│    - 确认问题解决               │
└─────────────────────────────────┘
```

### 4.2 常用命令速查表

| 步骤 | 命令 | 用途 |
|-----|------|-----|
| 1 | `kubectl top pods` | 查看Pod CPU使用率 |
| 2 | `kubectl exec -it <pod> -- top` | 容器内进程状态 |
| 3 | `ps aux \| grep java` | 查看Java进程 |
| 4 | `jstack <pid>` | 获取线程堆栈 |
| 5 | `jstat -gcutil <pid> 1000 10` | 实时GC统计 |
| 6 | `jmap -histo <pid>` | 堆对象统计 |
| 7 | `thread -n 3` (Arthas) | CPU最高线程 |
| 8 | `profiler start/stop` (Arthas) | CPU profiling |
| 9 | `thread -b` (Arthas) | 锁阻塞分析 |

---

## 附录

### A. Arthas安装和使用

```bash
# 在线安装
curl -O https://arthas.aliyun.com/arthas-boot.jar
java -jar arthas-boot.jar

# 常用命令
help          # 查看帮助
thread        # 查看线程状态
thread -n 3   # CPU最高的3个线程
thread -b     # 查看锁阻塞
top           # 热点方法
profiler start # 启动CPU profiling
profiler stop --format html # 停止并生成报告
quit          # 退出
```

### B. JVM参数参考

| 参数 | 说明 | 推荐值 |
|-----|------|-------|
| -Xms | 初始堆大小 | 物理内存的50% |
| -Xmx | 最大堆大小 | 物理内存的50-75% |
| -Xmn | 年轻代大小 | 堆大小的1/3-1/2 |
| -XX:+UseG1GC | 使用G1收集器 | 推荐 |
| -XX:MaxGCPauseMillis | 最大GC停顿时间 | 200ms |
| -XX:+UseContainerSupport | 支持容器环境 | 必须 |
| -XX:MaxRAMPercentage | 最大内存百分比 | 75% |

### C. 容器资源配置参考

```yaml
resources:
  requests:
    cpu: "1"           # 保证的CPU资源
    memory: "1Gi"      # 保证的内存资源
  limits:
    cpu: "2"           # CPU上限
    memory: "2Gi"      # 内存上限
```

**配置原则：**
- request设置为应用正常运行所需的最小资源
- limit设置为应用峰值时所需的最大资源
- CPU limit不应超过节点可用CPU的80%
- 内存limit应略大于JVM的-Xmx值

### D. 常见问题对照表

| 现象 | 可能原因 | 排查方法 | 解决方案 |
|-----|---------|---------|---------|
| CPU 100%持续 | 死循环 | jstack分析RUNNABLE线程 | 添加终止条件 |
| CPU波动大 | 频繁GC | jstat查看GC频率 | 优化内存使用 |
| CPU中等但响应慢 | 锁竞争 | jstack查看BLOCKED线程 | 优化锁设计 |
| CPU高但业务处理慢 | IO阻塞 | 查看线程等待状态 | 优化资源配置 |
| CPU间歇性飙升 | 定时任务 | 查看定时任务执行时间 | 优化任务逻辑 |

### E. 日志分析要点

**GC日志配置：**

> **注意**：JDK 8和JDK 9+的GC日志配置方式不同，请勿混合使用。

**JDK 9+（推荐）：**
```bash
# 统一日志格式
-Xlog:gc*:file=gc.log:time,level,tags
-Xlog:gc+heap=info:file=gc.log
-Xlog:gc+metaspace=info:file=gc.log
```

**JDK 8：**
```bash
# 传统日志格式
-XX:+PrintGCDetails
-XX:+PrintGCDateStamps
-XX:+PrintHeapAtGC
-Xloggc:gc.log
```

**日志分析：**
```
# JDK 9+ 正常GC日志
[2024-01-15T10:30:00.123+0800] [gc] [info] GC(123) Pause Young (G1 Evacuation Pause) 200M->100M(1G) 50.0ms

# JDK 8 正常GC日志
2024-01-15T10:30:00.123+0800: 1234.567: [GC (Allocation Failure) 1234.567: [G1Ergonomics (CSet Construction) start choosing CSet, _pending_cards: 0, predicted base time: 20.00 ms, remaining time: 180.00 ms, target pause time: 200.00 ms]

# 需要关注的日志
- Allocation Failure: 内存分配失败
- Full GC: 频繁Full GC
- GC pause时间过长: 超过200ms
- 内存溢出: OutOfMemoryError
```
