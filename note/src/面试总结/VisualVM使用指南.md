# VisualVM 使用指南 - JVM 监控与问题排查

## 📥 一、安装 VisualVM

### 方法1：使用 SDKMAN（推荐）

```bash
# 安装 SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# 安装 VisualVM
sdk install visualvm
```

### 方法2：Homebrew（macOS）

```bash
brew install --cask visualvm
```

### 方法3：官网下载

访问：https://visualvm.github.io/download.html

---

## 🔧 二、配置 Java 应用支持监控

### 1. 本地应用（自动识别）

VisualVM 会自动检测本地运行的 Java 应用，无需额外配置。

### 2. 远程应用（需要 JMX）

#### 启动参数配置

```bash
java \
  -Dcom.sun.management.jmxremote \
  -Dcom.sun.management.jmxremote.port=9090 \
  -Dcom.sun.management.jmxremote.authenticate=false \
  -Dcom.sun.management.jmxremote.ssl=false \
  -Djava.rmi.server.hostname=192.168.1.100 \
  -jar your-app.jar
```

#### 参数说明

| 参数 | 说明 |
|------|------|
| `jmxremote.port` | JMX 端口号 |
| `authenticate` | 是否启用认证（开发环境可设为 false） |
| `ssl` | 是否启用 SSL |
| `rmi.server.hostname` | 服务器 IP 地址 |

---

## 🚀 三、启动带监控参数的应用

### 示例1：模拟堆溢出

```bash
cd /Users/jay/Documents/ideaProject/demo/project-my/algorithm

mvn exec:java -Dexec.mainClass="com.qq.ijay997.jvm.JVMMemoryExceptionDemo" \
  -Dexec.args="1" \
  -Dexec.jvmArgs="-Xms10m -Xmx10m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./heap_dump.hprof"
```

### 示例2：模拟栈溢出

```bash
mvn exec:java -Dexec.mainClass="com.qq.ijay997.jvm.JVMMemoryExceptionDemo" \
  -Dexec.args="2" \
  -Dexec.jvmArgs="-Xss128k"
```

### 示例3：正常应用（推荐用于监控练习）

```bash
mvn exec:java -Dexec.mainClass="com.qq.ijay997.jvm.JVMMemoryExceptionDemo" \
  -Dexec.args="6" \
  -Dexec.jvmArgs="-Xms512m -Xmx512m -verbose:gc -XX:+PrintGCDetails -XX:+PrintGCDateStamps"
```

---

## 📊 四、VisualVM 核心功能详解

### 1. 概览（Overview）

**显示内容：**
- JVM 基本信息（版本、供应商）
- 系统信息（CPU、内存）
- 启动参数
- 系统属性

**用途：**
- 快速了解应用运行环境
- 确认 JVM 参数是否正确

---

### 2. 监视（Monitor）⭐最常用

#### A. CPU 使用率

- **实时图表**：显示 CPU 占用情况
- **用途**：发现 CPU 密集型操作

**排查步骤：**
1. 观察 CPU 是否持续高位
2. 切换到"线程"标签页
3. 找到 CPU 占用高的线程
4. 右键 → "线程转储"分析

---

#### B. 堆内存使用

**图表说明：**
- **已用堆**：当前使用的堆内存
- **已提交堆**：JVM 向操作系统申请的堆内存
- **最大堆**：`-Xmx` 设置的最大值

**正常模式：**
```
已用堆呈锯齿状波动 ↗↘↗↘
  ↑ GC 回收    ↓ 对象分配
```

**异常模式：**
```
❌ 内存泄漏：已用堆持续上升，不下降
❌ 内存不足：已用堆接近最大堆，频繁 GC
```

---

#### C. Metaspace（元空间）

**监控要点：**
- 持续增长可能表示类加载过多
- 动态代理、Groovy 脚本可能导致元空间泄漏

---

#### D. 类加载

- **已加载类**：当前加载的类数量
- **卸载类**：被 GC 卸载的类数量

**异常信号：**
- 已加载类持续增长，不卸载
- 可能存在类加载器泄漏

---

#### E. 线程

- **活动线程**：当前活跃的线程数
- **峰值线程**：历史最高线程数
- **守护线程**：后台线程数

**异常信号：**
- 线程数持续增长 → 线程泄漏
- 活动线程接近上限 → 可能需要调整线程池

---

### 3. 线程（Threads）

#### A. 线程状态

| 状态 | 颜色 | 说明 |
|------|------|------|
| Running | 🟢 绿色 | 正在执行 |
| Sleeping | 🔵 蓝色 | 休眠中 |
| Waiting | 🟡 黄色 | 等待锁或条件 |
| Monitor | 🔴 红色 | 等待监视器锁 |

#### B. 线程转储（Thread Dump）

**操作步骤：**
1. 点击"线程转储"按钮
2. 保存 `.tdump` 文件
3. 分析线程状态

**常见问题：**

##### 死锁检测

```
Found one Java-level deadlock:
=============================
"Thread-1":
  waiting to lock monitor 0x00007f8a1c003b38
  which is held by "Thread-2"
"Thread-2":
  waiting to lock monitor 0x00007f8a1c003a58
  which is held by "Thread-1"
```

##### 线程阻塞

```
"http-nio-8080-exec-10" #45 daemon prio=5 os_prio=31
   java.lang.Thread.State: BLOCKED (on object monitor)
   at com.example.Service.method(Service.java:100)
   - waiting to lock <0x000000076ab28c40>
```

---

### 4. 抽样器（Sampler）

#### A. CPU 抽样

**用途：** 找出最耗时的方法

**操作步骤：**
1. 点击"CPU"按钮开始抽样
2. 运行一段时间
3. 点击"停止"查看结果

**结果分析：**
- **Self Time**：方法自身执行时间
- **Total Time**：包含子调用的总时间
- 按 Self Time 排序，找到瓶颈方法

---

#### B. 内存抽样

**用途：** 找出创建最多对象的方法

**操作步骤：**
1. 点击"内存"按钮
2. 运行一段时间
3. 查看对象分配情况

**结果分析：**
- **Allocated Size**：分配的内存大小
- **Instances**：实例数量
- 重点关注大对象和大量小对象

---

### 5. Profiler（性能分析器）⭐最强大

#### A. CPU 性能分析

**特点：**
- 比抽样器更精确
- 记录每个方法的调用次数和执行时间
- 可以生成调用树

**操作步骤：**
1. 点击"CPU"按钮
2. 选择要分析的包（如 `com.qq.ijay997.*`）
3. 运行应用
4. 点击"快照"保存结果

**结果解读：**

```
方法名                          Self Time    Total Time    Invocations
======================================================================
UserService.getUser()           500ms        800ms         1000
  ├─ UserRepository.findById()  200ms        200ms         1000
  └─ convertToDTO()             100ms        100ms         1000
```

**优化方向：**
- Self Time 高 → 优化方法本身
- Total Time 高但 Self Time 低 → 优化子调用

---

#### B. 内存性能分析

**特点：**
- 跟踪对象分配
- 找出内存泄漏
- 生成堆转储

**操作步骤：**
1. 点击"内存"按钮
2. 选择要监控的类
3. 运行应用
4. 点击"快照"查看对象分布

---

### 6. MBeans

**用途：** 监控和管理应用的 MBean

**常见场景：**
- Spring Boot Actuator 暴露的指标
- 自定义 MBean
- Tomcat/Jetty 连接池监控

---

## 🔍 五、实际问题排查案例

### 案例1：内存泄漏排查

#### 症状
- 堆内存持续增长，不下降
- 频繁 Full GC
- 最终抛出 `OutOfMemoryError`

#### 排查步骤

**步骤1：监控堆内存**
```
VisualVM → 监视 → 观察堆内存曲线
```

**步骤2：执行堆转储**
```
VisualVM → 监视 → 堆 Dump → 保存 .hprof 文件
```

**步骤3：分析 Dump 文件**
```
VisualVM → 文件 → 装入 → 选择 .hprof 文件
→ OQL 控制台 → 查询大对象
```

**OQL 查询示例：**

```sql
-- 查找最大的 10 个对象
SELECT * FROM java.lang.Object[] 
ORDER BY @sizeof(this) DESC 
LIMIT 10

-- 查找某个类的实例
SELECT * FROM com.example.User

-- 统计各类对象数量
SELECT class.name, count(*) 
FROM java.lang.Object 
GROUP BY class.name
```

**步骤4：查找引用链**
```
右键对象 → 显示最近 GC 根节点
→ 查看谁在引用这些对象
```

**步骤5：定位代码**
- 根据引用链找到泄漏点
- 修复代码（关闭资源、移除监听器等）

---

### 案例2：CPU 飙高排查

#### 症状
- CPU 使用率持续 100%
- 应用响应缓慢

#### 排查步骤

**步骤1：定位高 CPU 线程**

```bash
# Linux/Mac
top -H -p <pid>

# 找到 CPU 最高的线程 ID（十进制）
# 转换为十六进制
printf "%x\n" <thread_id>
```

**步骤2：获取线程转储**

```bash
jstack <pid> > thread_dump.txt
```

**步骤3：查找对应线程**

```bash
# 在 thread_dump.txt 中搜索十六进制线程 ID
grep -A 20 "0x<hex_thread_id>" thread_dump.txt
```

**步骤4：VisualVM 辅助**

```
VisualVM → 线程 → 观察 CPU 占用
→ 线程转储 → 分析热点代码
```

**步骤5：优化代码**
- 无限循环
- 复杂计算
- 频繁 GC

---

### 案例3：死锁检测

#### 症状
- 应用无响应
- 线程处于 BLOCKED 状态

#### 排查步骤

**步骤1：VisualVM 自动检测**

```
VisualVM → 线程 → 检测到死锁会自动提示
```

**步骤2：线程转储分析**

```bash
jstack <pid> | grep -A 10 "deadlock"
```

**步骤3：查看详细信息**

```
Found one Java-level deadlock:
=============================
"Thread-1":
  waiting to lock monitor A (held by Thread-2)
"Thread-2":
  waiting to lock monitor B (held by Thread-1)
```

**步骤4：修复死锁**
- 统一锁获取顺序
- 使用 `tryLock` 设置超时
- 减少锁粒度

---

### 案例4：频繁 GC 排查

#### 症状
- 应用间歇性停顿
- 日志中出现大量 GC 记录

#### 排查步骤

**步骤1：开启 GC 日志**

```bash
-verbose:gc \
-XX:+PrintGCDetails \
-XX:+PrintGCDateStamps \
-Xloggc:/path/to/gc.log
```

**步骤2：分析 GC 日志**

```bash
# 使用 GCViewer 或 GCEasy.io 分析
```

**步骤3：VisualVM 监控**

```
VisualVM → 监视 → 观察 GC 频率和持续时间
```

**步骤4：堆转储分析**

```
VisualVM → 堆 Dump → 分析对象分布
→ 找出占用内存最多的对象
```

**步骤5：优化方案**
- 增大堆内存：`-Xmx4g`
- 调整新生代比例：`-XX:NewRatio=2`
- 更换 GC 算法：`-XX:+UseG1GC`
- 优化代码，减少对象创建

---

## 🛠️ 六、VisualVM 插件推荐

### 1. 安装插件

```
VisualVM → 工具 → 插件 → 可用插件
```

### 2. 推荐插件

| 插件 | 功能 |
|------|------|
| **VisualGC** | 可视化 GC 过程，显示各代内存变化 |
| **MBeans** | 增强的 MBean 浏览器 |
| **Thread Inspector** | 更详细的线程分析 |
| **BTrace** | 动态追踪 Java 代码（无需重启） |

### 3. VisualGC 使用说明

```
VisualVM → 插件 → VisualGC
```

**显示内容：**
- Eden、Survivor、Old Gen 实时变化
- GC 事件标记
- 对象晋升过程

---

## 📝 七、常用命令速查

### 1. jps - 查看 Java 进程

```bash
jps -l  # 显示完整类名
jps -v  # 显示 JVM 参数
```

### 2. jstat - 统计信息

```bash
# 每 1 秒输出一次 GC 统计
jstat -gc <pid> 1000

# 输出 10 次
jstat -gc <pid> 1000 10
```

### 3. jmap - 内存映像

```bash
# 生成堆转储
jmap -dump:format=b,file=heap.hprof <pid>

# 查看堆摘要
jmap -heap <pid>

# 查看对象统计
jmap -histo <pid> | head -20
```

### 4. jstack - 线程转储

```bash
jstack <pid> > thread_dump.txt

# 包含锁信息
jstack -l <pid>
```

### 5. jcmd - 多功能诊断

```bash
# 列出所有 Java 进程
jcmd

# 触发 GC
jcmd <pid> GC.run

# 生成堆转储
jcmd <pid> GC.heap_dump /path/to/dump.hprof

# 生成线程转储
jcmd <pid> Thread.print
```

---

## 💡 八、最佳实践

### 1. 生产环境配置

```bash
java \
  -Xms4g \
  -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -XX:+HeapDumpOnOutOfMemoryError \
  -XX:HeapDumpPath=/var/log/app/heap_dump.hprof \
  -Xloggc:/var/log/app/gc.log \
  -XX:+PrintGCDetails \
  -XX:+PrintGCDateStamps \
  -jar app.jar
```

### 2. 监控建议

- ✅ 定期检查堆内存趋势
- ✅ 监控 GC 频率和持续时间
- ✅ 关注线程数变化
- ✅ 设置告警阈值（堆使用率 > 80%）

### 3. 问题排查流程

```
发现问题
  ↓
VisualVM 监控（实时）
  ↓
收集证据（堆转储、线程转储、GC 日志）
  ↓
离线分析（MAT、GCViewer）
  ↓
定位根因
  ↓
修复并验证
```

### 4. 注意事项

- ⚠️ 堆转储会暂停应用（Stop-The-World）
- ⚠️ Profiler 会影响性能，生产环境慎用
- ⚠️ 定期清理旧的 Dump 文件
- ⚠️ 敏感信息脱敏后再分享 Dump 文件

---

## 🎯 九、实战练习

### 练习1：监控 JVMMemoryExceptionDemo

```bash
# 终端1：启动应用
cd /Users/jay/Documents/ideaProject/demo/project-my/algorithm
mvn exec:java -Dexec.mainClass="com.qq.ijay997.jvm.JVMMemoryExceptionDemo" \
  -Dexec.args="6"

# 终端2：启动 VisualVM
visualvm

# 在 VisualVM 中：
# 1. 找到 JVMMemoryExceptionDemo 进程
# 2. 观察"监视"标签页
# 3. 查看堆内存变化
# 4. 执行几次 GC，观察效果
```

### 练习2：模拟并排查内存泄漏

```bash
# 启动应用（限制堆大小）
mvn exec:java -Dexec.mainClass="com.qq.ijay997.jvm.JVMMemoryExceptionDemo" \
  -Dexec.args="1" \
  -Dexec.jvmArgs="-Xms10m -Xmx10m"

# 在 VisualVM 中：
# 1. 观察堆内存持续增长
# 2. 执行堆 Dump
# 3. 分析哪些对象占用最多内存
# 4. 查看引用链，找到泄漏点
```

### 练习3：CPU 性能分析

```bash
# 创建一个 CPU 密集型任务
# 在 VisualVM 中使用 Profiler → CPU
# 找出最耗时的方法
```

---

## 📚 十、相关工具对比

| 工具 | 优点 | 缺点 | 适用场景 |
|------|------|------|---------|
| **VisualVM** | 图形化、功能全面 | 性能开销较大 | 开发、测试环境 |
| **jstat** | 轻量、实时 | 命令行、信息有限 | 生产环境快速检查 |
| **MAT** | 强大的堆分析 | 需要 Dump 文件 | 深度内存分析 |
| **Arthas** | 在线诊断、无需重启 | 学习成本高 | 生产环境动态排查 |
| **JProfiler** | 商业软件、功能最强 | 收费 | 企业级应用 |

---

## 🎓 总结

VisualVM 是 Java 开发者必备的性能分析工具，掌握它可以：

1. ✅ 快速定位内存泄漏
2. ✅ 发现性能瓶颈
3. ✅ 检测死锁和线程问题
4. ✅ 优化 GC 策略
5. ✅ 提升应用稳定性

**记住：** 预防胜于治疗，定期监控比事后排查更重要！💪
