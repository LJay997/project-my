# Redis 面试考察指南（高阶版）

> **适用人群**：2-5年Java开发经验、准备中高级/资深面试的工程师
> **目标**：系统覆盖Redis核心知识、技术原理、实际应用场景及高频面试题
> **版本说明**：基于 Redis 7.x 特性，兼顾向下兼容

***

## 目录

1. [基础概念与核心数据结构](#1-基础概念与核心数据结构)
2. [持久化机制与高可用方案](#2-持久化机制与高可用方案)
3. [高级特性与核心原理](#3-高级特性与核心原理)
4. [性能优化与调优实践](#4-性能优化与调优实践)
5. [实际应用场景与解决方案](#5-实际应用场景与解决方案)
6. [常见面试题深度解析](#6-常见面试题深度解析)
7. [模拟面试问答（高频题）](#7-模拟面试问答高频题)

***

## 1. 基础概念与核心数据结构

### 1.1 Redis 核心特性概述

| 特性     | 说明                                  |
| ------ | ----------------------------------- |
| 内存存储   | 数据存在内存中，读写速度极快（微秒级）                 |
| 单线程模型  | 核心命令执行单线程，避免上下文切换和锁竞争               |
| IO多路复用 | 基于epoll/kqueue实现高并发网络处理             |
| 丰富数据结构 | 支持String/Hash/List/Set/ZSet/Stream等 |
| 持久化支持  | RDB/AOF两种机制，保证数据不丢失                 |
| 高可用    | 主从复制、哨兵、集群等多种方案                     |
| 原子操作   | 单命令原子性、Lua脚本原子执行                    |

### 1.2 核心数据结构深度解析

#### 1.2.1 数据结构与应用场景映射

| 数据类型        | 底层实现                         | 典型场景                | 复杂度        |
| ----------- | ---------------------------- | ------------------- | ---------- |
| String      | SDS (Simple Dynamic String)  | 缓存、计数器、分布式锁、Session | O(1) 读写    |
| Hash        | ziplist → hashtable          | 对象存储、用户信息、商品属性      | O(1) 字段读写  |
| List        | quicklist (双向链表+压缩列表)        | 消息队列、最新消息列表、时间线     | O(1) 两端操作  |
| Set         | intset → hashtable           | 去重、共同好友、标签、抽奖       | O(1) 成员操作  |
| ZSet        | ziplist → skiplist+hashtable | 排行榜、延时队列、范围查询       | O(logN) 操作 |
| Stream      | radix tree+list              | 消息队列、事件流、日志收集       | O(1) 写入    |
| Bitmap      | 位数组                          | 签到、活跃统计、布隆过滤器       | O(1) 位操作   |
| HyperLogLog | 概率算法                         | UV统计、近似去重           | O(1) 操作    |
| GEO         | sorted set+geohash           | 附近的人、位置服务           | O(logN) 查询 |

#### 1.2.2 Redis 7.x 新特性

**Multi-Part AOF**

```text
传统AOF: appendonly.aof (单文件)
7.x AOF: base.aof + incr-*.aof (多段)
```

- 优势：支持更灵活的RDB-AOF混合持久化
- 可按需加载单段AOF，提高重启速度

**listpack 替代 ziplist**

- 更紧凑的内存布局
- 更好的读写性能
- 减少内存分配次数

**Redis Functions**

```lua
-- 注册函数
FUNCTION LOAD "#LUA\nredis.call('SET', KEYS[1], ARGV[1])"
-- 调用
FCALL setval 1 mykey myvalue
```

- 比Lua脚本更灵活的函数管理
- 支持函数持久化和复制

**Redis Stack 模块**

| 模块              | 功能       | 场景         |
| --------------- | -------- | ---------- |
| RediSearch      | 全文搜索引擎   | 商品搜索、文档检索  |
| RedisJSON       | JSON文档存储 | 灵活数据结构存储   |
| RedisTimeSeries | 时间序列数据库  | 监控指标、IoT数据 |
| RedisGraph      | 图数据库     | 社交关系、路径分析  |
| RedisBloom      | 概率数据结构   | 布隆过滤器、基数统计 |

#### 1.2.3 底层结构详解

**SDS（Simple Dynamic String）**

```
┌────────┬────────┬───────────────────────┐
│  len   │ alloc  │ buf[0..alloc-1]       │
└────────┴────────┴───────────────────────┘
 ↑ len=5    ↑ alloc=10    ↑ "hello\0..." 保留空间
```

- **O(1)取长度**：避免C语言O(n)遍历
- **二进制安全**：可存储任意二进制数据
- **预分配策略**：减少频繁内存重分配
  - 扩容时分配 `newlen * 2` 空间
  - 缩短时不立即释放，按需回收

**跳表（SkipList）**

```
Level 3:  HEAD ──────────────► 50 ─────► NULL
Level 2:  HEAD ──────► 20 ──► 50 ─────► NULL
Level 1:  HEAD ─► 10 ► 20 ► 30 ► 50 ─► NULL
Level 0:  HEAD ─► 10 ► 20 ► 30 ► 40 ► 50 ► NULL
```

- **时间复杂度**：平均O(logN)，最坏O(N)
- **空间复杂度**：O(N)
- **ZSet中的应用**：哈希表+跳表双层结构
  - 哈希表：O(1)按member查score
  - 跳表：O(logN)按score范围查询

**渐进式Rehash**

```
dict.ht[0] ──rehash──► dict.ht[1]
     │                       │
  每次只迁移一部分，避免阻塞
```

- 触发时机：扩容/缩容时
- 迁移策略：
  - 定时任务：每100ms迁移1个bucket
  - 读写辅助：每次读写时顺便迁移1个bucket
- 好处：避免一次性迁移阻塞主线程

### 1.3 通用命令与使用模式

#### 1.3.1 常用命令速查

| 类型     | 命令                           | 用途      |
| ------ | ---------------------------- | ------- |
| String | SET/GET/DEL/INCR/DECR        | 键值操作、计数 |
| Hash   | HSET/HGET/HMGET/HKEYS        | 对象字段操作  |
| List   | LPUSH/RPUSH/LPOP/RPOP/LRANGE | 队列/栈操作  |
| Set    | SADD/SREM/SMEMBERS/SINTER    | 集合运算    |
| ZSet   | ZADD/ZRANK/ZRANGEBYSCORE     | 排行榜操作   |
| Key    | KEYS/SCAN/EXPIRE/TTL         | 键管理     |
| Server | INFO/SLOWLOG/MEMORY USAGE    | 服务器信息   |

#### 1.3.2 SCAN系列命令

```redis
-- 全量扫描（阻塞！）
KEYS *pattern*

-- 增量扫描（推荐）
SCAN cursor [MATCH pattern] [COUNT count]

-- 其他SCAN命令
HSCAN key cursor     # Hash扫描
SSCAN key cursor     # Set扫描
ZSCAN key cursor     # ZSet扫描
```

**SCAN特点**：

- 基于游标，支持增量迭代
- 不阻塞主线程
- 可能返回重复/遗漏元素（需去重）
- 适合生产环境使用

#### 1.3.3 Pipeline与Lua

**Pipeline（管道）**

```java
// 批量发送命令，减少网络RTT
Pipeline pipeline = redis.pipelined();
pipeline.set("key1", "value1");
pipeline.set("key2", "value2");
pipeline.get("key1");
List<Object> results = pipeline.syncAndReturnAll();
```

- 优势：减少网络往返次数
- 注意：pipeline不保证原子性

**Lua脚本**

```redis
-- Lua脚本保证原子性
EVAL "return redis.call('SET', KEYS[1], ARGV[1])" 1 key value

-- 带参数的Lua脚本
EVAL "
  local current = redis.call('GET', KEYS[1])
  if current == ARGV[1] then
    return redis.call('DEL', KEYS[1])
  end
  return 0
" 1 lock_key expected_value
```

***

> **考察要点提示**
>
> 面试官可能考察：
>
> 1. **数据结构选择**：为什么排行榜用ZSet而不是Hash？（考察对底层结构的理解）
> 2. **性能分析**：如何快速估算一个命令的时间复杂度？（考察算法素养）
> 3. **生产实践**：如何安全地遍历所有key？（考察对SCAN/KEYS的理解）
> 4. **Redis 7新特性**：Multi-Part AOF解决了什么问题？（考察对新版本的了解）
>
> **常见陷阱**：
>
> - 错误回答"Redis完全单线程"：应该说"核心命令处理单线程，IO和后台任务是多线程"
> - 混淆KEYS和SCAN的使用场景：KEYS只能在开发/测试环境使用

***

## 2. 持久化机制与高可用方案

### 2.1 RDB 持久化

#### 2.1.1 RDB 工作原理

```
触发方式:
├── save: 阻塞主线程保存
├── bgsave: fork子进程保存（推荐）
├── 自动触发: 根据配置规则自动触发
└── 从节点复制: 主从同步时触发

保存流程:
1. fork子进程（Copy-On-Write）
2. 子进程遍历内存数据
3. 写入临时RDB文件
4. 原子重命名为正式文件
5. 替换旧RDB文件
```

#### 2.1.2 RDB 配置参数

```conf
# 触发规则：时间+变更次数
save 3600 1      # 1小时内有1次写操作
save 300 100     # 5分钟内有100次写操作
save 60 10000    # 1分钟内有10000次写操作

# RDB文件名
dbfilename dump.rdb

# 保存路径
dir /var/lib/redis

# 压缩RDB文件（推荐开启）
rdbcompression yes

# RDB文件校验
rdbchecksum yes

# fork时如果内存占用超过阈值，是否停止保存
stop-writes-on-bgsave-error yes
rdb-save-ignore-errors no

# 自动RDB持久化
rdb-save-bgsave-after-rdb-save yes
```

#### 2.1.3 RDB 优缺点

| 优点            | 缺点             |
| ------------- | -------------- |
| 文件紧凑，适合备份和迁移  | fork可能导致CPU毛刺  |
| 恢复速度快（直接加载文件） | 可能丢失最后一次快照后的数据 |
| 实现简单，可靠性高     | 内存占用大时fork耗时   |
| 全量备份，数据一致性好   | 无法做到实时备份       |

#### 2.1.4 RDB 实战命令

```bash
# 手动触发RDB保存（阻塞）
redis-cli SAVE

# 手动触发后台保存（推荐）
redis-cli BGSAVE

# 查看最后保存时间
redis-cli LASTSAVE

# 查看保存状态
redis-cli INFO persistence
```

### 2.2 AOF 持久化

#### 2.2.1 AOF 工作原理

```
写入流程:
1. 命令执行成功后
2. 追加写入AOF缓冲区
3. 根据fsync策略刷盘
4. 周期性执行AOF重写

触发重写时机:
├── auto-aof-rewrite-percentage: AOF文件增长百分比
├── auto-aof-rewrite-min-size: AOF文件最小尺寸
└── 手动 BGREWRITEAOF
```

#### 2.2.2 AOF 配置参数

```conf
# 开启AOF
appendonly yes

# AOF文件名
appendfilename "appendonly.aof"

# 刷盘策略（核心！）
appendfsync everysec    # 推荐：每秒刷盘
# appendfsync always   # 最安全，性能差
# appendfsync no       # 交给OS，最快但最不安全

# AOF重写配置
auto-aof-rewrite-percentage 100  # 文件增长100%时触发
auto-aof-rewrite-min-size 64mb   # 最小触发大小

# AOF重写时是否允许追加
aof-rewrite-incremental-fsync yes

# 加载时是否截断不完整的AOF
aof-load-truncated yes

# Redis 4.0+：混合持久化（RDB+AOF）
aof-use-rdb-preamble yes
```

#### 2.2.3 三种刷盘策略对比

| 策略       | 数据安全性 | 性能影响 | 丢失数据窗口 | 适用场景      |
| -------- | ----- | ---- | ------ | --------- |
| always   | 最安全   | 最慢   | 无      | 数据敏感、金融场景 |
| everysec | 较安全   | 折中   | 最多1秒   | 生产环境推荐    |
| no       | 最不安全  | 最快   | OS决定   | 可容忍少量丢失   |

#### 2.2.4 AOF 重写机制

```
重写原因:
- AOF文件持续增长
- 存在冗余命令（如多次SET同一key）
- 需要压缩存储空间

重写流程:
1. 触发重写（自动/手动）
2. fork子进程
3. 子进程遍历当前内存状态
4. 生成最小化命令集
5. 写入新AOF文件
6. 重写期间的命令追加到新文件
7. 原子替换旧AOF文件
```

### 2.3 持久化方案选择

#### 2.3.1 方案对比

| 维度    | RDB         | AOF    | 混合持久化  |
| ----- | ----------- | ------ | ------ |
| 数据安全  | 可能丢失数分钟     | 最多丢失1秒 | 最多丢失1秒 |
| 恢复速度  | 快           | 较慢     | 快      |
| 文件体积  | 小           | 较大     | 小      |
| 性能影响  | 保存时fork可能卡顿 | 持续IO开销 | 较小     |
| 实现复杂度 | 简单          | 较复杂    | 复杂     |

#### 2.3.2 生产环境推荐配置

```conf
# 混合持久化（Redis 4.0+）
save 900 1
save 300 10
save 60 10000

appendonly yes
appendfsync everysec
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb
aof-use-rdb-preamble yes
```

**选择策略**：

1. **优先混合持久化**：兼顾恢复速度和数据安全
2. **纯RDB**：可容忍少量数据丢失，追求快速恢复
3. **纯AOF**：对数据安全要求极高

### 2.4 主从复制

#### 2.4.1 复制机制

```
主节点:
├── 接收写命令
├── 写入本地AOF
├── 异步同步给从节点
└── 维护复制offset

从节点:
├── 接收主节点命令流
├── 写入本地数据
├── 定期同步offset
└── 上报复制状态

同步类型:
├── 全量同步: 新从节点加入
├── 增量同步: 正常复制过程
└── 断点续传: 从节点短暂断开后重连
```

#### 2.4.2 复制配置

```conf
# 从节点配置
replicaof master_host master_port

# 主节点密码（如果设置了密码）
masterauth master_password

# 复制延迟容忍
replica-read-only yes
replica-priority 100   # 哨兵选主时的优先级

# 复制缓冲区
repl-backlog-size 1mb
repl-backlog-ttl 3600

# 全量同步触发阈值
repl-timeout 60
repl-diskless-sync yes    # 无盘复制（推荐）
```

#### 2.4.3 复制问题排查

```bash
# 查看复制状态
redis-cli INFO replication

# 查看从节点列表
redis-cli INFO replication | grep slave

# 检查复制延迟
redis-cli INFO replication | grep lag

# 监控复制健康状况
redis-cli --stat          # 实时统计
```

### 2.5 哨兵模式

#### 2.5.1 哨兵核心功能

| 功能     | 说明              |
| ------ | --------------- |
| 监控     | 定期检查主从节点状态      |
| 通知     | 节点状态变更时通知管理员    |
| 自动故障转移 | 主节点故障时自动选主      |
| 配置更新   | 客户端可通过哨兵获取主节点信息 |

#### 2.5.2 哨兵部署架构

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│ Sentinel 1  │     │ Sentinel 2  │     │ Sentinel 3  │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │                   │                   │
       └───────────────────┼───────────────────┘
                           │
                    ┌──────┴──────┐
                    │   仲裁投票   │
                    └──────┬──────┘
                           │
              ┌────────────┼────────────┐
              │                         │
        ┌─────┴─────┐             ┌─────┴─────┐
        │  Master   │◄───────────►│  Replica  │
        └───────────┘   复制流    └───────────┘
```

#### 2.5.3 哨兵配置示例

```sentinel
# 哨兵端口
port 26379

# 监控的主节点
sentinel monitor mymaster 127.0.0.1 6379 2

# 主节点密码
sentinel auth-pass mymaster redis_password

# 故障转移超时
sentinel down-after-milliseconds mymaster 5000

# 故障转移期间允许多少从节点同步
sentinel parallel-syncs mymaster 1

# 故障转移超时
sentinel failover-timeout mymaster 60000

# 配置文件不允许重命名
sentinel deny-scripts-reconfig yes
```

#### 2.5.4 哨兵选主流程

```
1. 检测主节点下线
   ├── 主观下线: 单个哨兵判定
   └── 客观下线: 多数哨兵判定（需满足quorum）

2. 选出领头哨兵
   ├── 所有哨兵投票
   └── 得票最多者执行故障转移

3. 选择新主节点
   ├── 剔除已下线的从节点
   ├── 按复制offset排序（数据最新优先）
   ├── 按replica-priority排序
   └── 选择最优从节点作为新主

4. 执行故障转移
   ├── 新主执行SLAVEOF NO ONE
   ├── 其他从节点指向新主
   └── 更新配置信息
```

### 2.6 Redis Cluster

#### 2.6.1 集群架构

```
Hash Slot 分配:
0 ────────────────────────── 16383
┌──────────┬──────────┬──────────┐
│ Master 1 │ Master 2 │ Master 3 │
│ Slots:   │ Slots:   │ Slots:   │
│ 0-5460   │ 5461-10922│10923-16383│
└────┬─────┴────┬─────┴────┬─────┘
     │          │          │
┌────┴────┐ ┌──┴────┐ ┌──┴────┐
│Replica 1│ │Replica 2│ │Replica 3│
└─────────┘ └───────┘ └───────┘

客户端路由:
1. 计算key的CRC16值
2. 对16384取模得到slot
3. 找到负责该slot的节点
4. 发送命令到正确节点
```

#### 2.6.2 集群命令

```bash
# 创建集群（6个节点，3主3从）
redis-cli --cluster create \
  127.0.0.1:6379 127.0.0.1:6380 127.0.0.1:6381 \
  127.0.0.1:6382 127.0.0.1:6383 127.0.0.1:6384 \
  --cluster-replicas 1

# 查看集群信息
redis-cli -c CLUSTER INFO
redis-cli -c CLUSTER NODES

# 添加新节点
redis-cli --cluster add-node \
  127.0.0.1:6385 127.0.0.1:6379

# 手动分配slot
redis-cli -c CLUSTER ADDSLOTS slot_list

# 重新分片
redis-cli --cluster reshard 127.0.0.1:6379

# 集群检查
redis-cli --cluster check 127.0.0.1:6379
```

#### 2.6.3 Hash Tag 使用

```redis
# 让多个key落到同一slot
# 规则：{...}内的部分参与哈希

user:{100}:profile    # slot = CRC16("user:{100}:profile") % 16384
user:{100}:orders     # slot = CRC16("100") % 16384

# 上面两个key会在同一slot，可以使用多key命令
MGET user:{100}:profile user:{100}:orders
```

#### 2.6.4 集群配置

```conf
# 集群模式开关
cluster-enabled yes

# 集群配置文件
cluster-config-file nodes-6379.conf

# 集群节点超时
cluster-node-timeout 15000

# 集群复制超时
cluster-replica-validity-factor 10

# 集群迁移阈值
cluster-migration-barrier 1

# 集群全量复制阈值
cluster-require-full-coverage yes

# 集群网络带宽
cluster-allow-reads-when-down no
```

### 2.7 高可用方案对比

| 方案      | 优点       | 缺点           | 适用场景        |
| ------- | -------- | ------------ | ----------- |
| 主从复制    | 读扩展、简单   | 主故障需手动切换     | 读多写少、可容忍故障  |
| 哨兵      | 自动故障转移   | 单点故障、扩展性差    | 中小规模、自动故障恢复 |
| Cluster | 水平扩展、高可用 | 复杂度高、跨slot限制 | 大规模、高并发场景   |
| Proxy代理 | 对客户端透明   | 额外组件、性能损耗    | 需要平滑扩容      |

***

> **考察要点提示**
>
> 面试官可能考察：
>
> 1. **持久化选择**：如何选择RDB/AOF/混合持久化？（考察工程决策能力）
> 2. **故障转移**：哨兵如何选主？依据是什么？（考察对内部机制的理解）
> 3. **集群路由**：Cluster如何定位key所在的节点？（考察对哈希槽的理解）
> 4. **数据一致性**：主从复制如何保证一致性？（考察CAP理论的理解）
>
> **常见陷阱**：
>
> - 混淆RDB和AOF的恢复速度：RDB恢复更快
> - 忽略哨兵的法定人数：必须多数哨兵同意才会触发故障转移
> - 忘记集群的slot限制：多key命令要求key在同一slot

***

## 3. 高级特性与核心原理

### 3.1 缓存策略

#### 3.1.1 缓存穿透、击穿、雪崩

**缓存穿透（Cache Penetration）**

```
请求过程:
用户请求 → Redis查询（不存在） → DB查询（不存在） → 返回空

解决方案:
1. 缓存空值：缓存不存在的key，设置短TTL
2. 布隆过滤器：先过滤不存在的key
3. 参数校验：请求前先校验参数有效性
```

```java
// 缓存空值方案
public User getUserById(Long id) {
    String key = "user:" + id;
    String cached = redisTemplate.opsForValue().get(key);
    if (cached != null) {
        if ("NULL".equals(cached)) {
            return null;  // 缓存的空值标记
        }
        return JSON.parseObject(cached, User.class);
    }
    
    User user = userRepository.findById(id);
    if (user == null) {
        // 缓存空值，TTL设短
        redisTemplate.opsForValue().set(key, "NULL", 2, TimeUnit.MINUTES);
    } else {
        redisTemplate.opsForValue().set(key, JSON.toJSONString(user), 1, TimeUnit.HOURS);
    }
    return user;
}
```

**缓存击穿（Cache Breakdown）**

```
请求过程:
热点key过期 → 大量并发请求 → 同时回源DB → DB压力突增

解决方案:
1. 互斥锁：只允许一个请求回源
2. 逻辑过期：缓存永不过期，异步刷新
3. 提前续期：热点key在过期前自动续期
```

```java
// 互斥锁方案
public String getHotData(String key) {
    String cached = redisTemplate.opsForValue().get(key);
    if (cached != null) {
        return cached;
    }
    
    // 使用SET NX获取锁
    String lockKey = "lock:" + key;
    Boolean locked = redisTemplate.opsForValue()
        .setIfAbsent(lockKey, "1", 5, TimeUnit.SECONDS);
    
    if (Boolean.TRUE.equals(locked)) {
        try {
            // 只有一个线程回源
            String value = loadFromDB(key);
            redisTemplate.opsForValue().set(key, value, 1, TimeUnit.HOURS);
            return value;
        } finally {
            redisTemplate.delete(lockKey);
        }
    } else {
        // 其他线程等待后重试
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return redisTemplate.opsForValue().get(key);
    }
}
```

**缓存雪崩（Cache Avalanche）**

```
请求过程:
大量key同时过期 → 大量请求回源DB → DB被打垮

解决方案:
1. TTL加随机值：打散过期时间
2. 多级缓存：本地缓存+Redis
3. 限流降级：保护DB不被打垮
4. 高可用Redis：避免Redis单点故障
```

```java
// TTL加随机值方案
public void setWithRandomTTL(String key, Object value, long baseTTL, TimeUnit unit) {
    // 随机TTL：基础TTL ± 20%
    long jitter = (long) (baseTTL * 0.2 * (Math.random() - 0.5));
    long actualTTL = baseTTL + jitter;
    redisTemplate.opsForValue().set(key, value, actualTTL, unit);
}
```

#### 3.1.2 缓存更新策略

| 策略            | 说明          | 优点     | 缺点       |
| ------------- | ----------- | ------ | -------- |
| Cache-Aside   | 先更新DB，再删除缓存 | 简单可靠   | 可能短暂不一致  |
| Read-Through  | 缓存层负责加载     | 透明访问   | 增加缓存层复杂度 |
| Write-Through | 写缓存同时写DB    | 数据一致性好 | 写延迟增加    |
| Write-Back    | 先写缓存，异步写DB  | 写性能好   | 数据可能丢失   |

#### 3.1.3 布隆过滤器

```redis
# 添加元素到布隆过滤器
BF.ADD filter_name item1 item2 item3

# 判断元素是否存在（可能有误判）
BF.EXISTS filter_name item1

# 创建布隆过滤器
BF.RESERVE filter_name 0.01 1000000
# 参数: key error_rate initial_size
```

**布隆过滤器特点**：

- 可能存在误判（不存在的可能被判为存在）
- 不会漏判（存在的一定会被判为存在）
- 只能添加，不能删除
- 空间效率高

### 3.2 分布式锁

#### 3.2.1 分布式锁实现

**基础实现**

```redis
# 获取锁（原子操作）
SET lock_key unique_value NX PX 30000
# 参数: NX(不存在才设置), PX(毫秒过期)

# 释放锁（Lua原子操作）
EVAL "
  if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
  else
    return 0
  end
" 1 lock_key unique_value
```

**Redisson实现**

```java
// 自动续期、可重入、公平锁
RLock lock = redissonClient.getLock("lock:order:123");

// 加锁：等待最多10秒，锁过期30秒
boolean locked = lock.tryLock(10, 30, TimeUnit.SECONDS);
if (locked) {
    try {
        // 业务逻辑
        processOrder();
    } finally {
        // 释放锁（会校验当前线程是否持有锁）
        if (lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }
}
```

#### 3.2.2 分布式锁核心要素

| 要素   | 说明             | 实现方式         |
| ---- | -------------- | ------------ |
| 互斥性  | 同一时刻只有一个客户端持有锁 | SET NX       |
| 原子性  | 加锁和设置过期时间原子执行  | SET NX PX    |
| 自动过期 | 避免死锁           | PX参数或续期      |
| 可重入  | 同一线程可多次获取同一锁   | Redisson实现   |
| 误删保护 | 只能释放自己持有的锁     | Lua脚本校验value |
| 续期机制 | 业务执行期间自动续期     | Redisson看门狗  |

#### 3.2.3 RedLock算法

```
RedLock多节点加锁流程:
1. 依次向N个独立Redis节点尝试加锁
2. 计算加锁总耗时
3. 如果超过半数成功且耗时小于锁有效期，则获取锁
4. 如果失败，则向所有节点释放锁

优点: 容忍部分节点故障
缺点: 实现复杂、网络延迟影响
适用: 对可用性要求极高的场景
```

#### 3.2.4 常见锁问题

| 问题    | 原因             | 解决方案              |
| ----- | -------------- | ----------------- |
| 死锁    | 锁未设置过期时间       | SET NX PX         |
| 误删锁   | 释放锁时未校验value   | Lua脚本原子释放         |
| 锁过期   | 业务执行时间超过TTL    | 自动续期/加大TTL        |
| 不可重入  | 同一线程重复加锁失败     | Redisson可重入锁      |
| 集群锁失效 | Redis主从切换导致锁丢失 | RedLock或Zookeeper |

### 3.3 事务处理

#### 3.3.1 Redis事务基础

```redis
# 开启事务
MULTI

# 添加命令到队列
SET key1 value1
SET key2 value2
INCR counter

# 执行事务
EXEC

# 取消事务
DISCARD
```

**事务特性**：

- 原子性：命令作为一个整体执行
- 顺序性：命令按顺序执行
- 隔离性：事务内命令不会被其他客户端插入
- 不支持回滚：命令执行失败不会自动回滚

#### 3.3.2 CAS乐观锁

```redis
# 监控key的变化
WATCH balance

# 开启事务
MULTI

# 读取当前值
GET balance

# 计算新值并更新
SET balance 200

# 执行事务（如果balance被修改，则事务失败）
EXEC
```

#### 3.3.3 Lua脚本

```redis
-- 原子扣减库存
EVAL "
  local stock = redis.call('GET', KEYS[1])
  if stock and tonumber(stock) >= tonumber(ARGV[1]) then
    redis.call('DECRBY', KEYS[1], ARGV[1])
    return 1
  end
  return 0
" 1 product:123 5

-- 限流脚本（滑动窗口）
EVAL "
  local key = KEYS[1]
  local now = redis.call('TIME')
  local current_ts = now[1] * 1000000 + now[2]
  local window_start = current_ts - tonumber(ARGV[2]) * 1000000
  
  redis.call('ZREMRANGEBYSCORE', key, 0, window_start)
  
  local count = redis.call('ZCARD', key)
  if count < tonumber(ARGV[1]) then
    redis.call('ZADD', key, current_ts, current_ts)
    redis.call('EXPIRE', key, tonumber(ARGV[2]))
    return 1
  end
  return 0
" 1 rate_limit:user:123 10 60
-- 参数: 限流10次/60秒
```

### 3.4 发布订阅

#### 3.4.1 Pub/Sub基础

```redis
# 订阅频道
SUBSCRIBE channel1 channel2

# 订阅模式
PSUBSCRIBE channel.*

# 发布消息
PUBLISH channel1 "hello redis"

# 取消订阅
UNSUBSCRIBE channel1
```

#### 3.4.2 Stream消息队列

```redis
# 添加消息到Stream
XADD mystream * user Alice message "Hello"

# 读取消息
XREAD COUNT 10 STREAMS mystream 0

# 使用消费组
XGROUP CREATE mystream mygroup
XREADGROUP GROUP mygroup consumer1 COUNT 10 STREAMS mystream >

# 确认消息处理
XACK mystream mygroup message_id

# 查看Stream信息
XINFO STREAM mystream
XINFO GROUPS mystream
XINFO CONSUMERS mystream mygroup
```

**Stream vs List**

| 特性   | List  | Stream     |
| ---- | ----- | ---------- |
| 消费模型 | 一对一   | 一对多（消费组）   |
| 消息持久 | 消费后删除 | 可保留历史消息    |
| 确认机制 | 无     | 显式ACK      |
| 消息回溯 | 不支持   | 支持按时间/ID回溯 |
| 消费组  | 不支持   | 支持         |

### 3.5 限流算法

#### 3.5.1 常见限流算法

**固定窗口**

```redis
SET count 0 EX 60  # 60秒窗口
INCR count
```

**滑动窗口（ZSet实现）**

```java
public boolean tryAcquire(String key, int limit, int windowSeconds) {
    String now = String.valueOf(System.currentTimeMillis());
    String windowStart = String.valueOf(System.currentTimeMillis() - windowSeconds * 1000L);
    
    String script = "redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, ARGV[1])" +
                    "local count = redis.call('ZCARD', KEYS[1])" +
                    "if count < tonumber(ARGV[2]) then" +
                    "  redis.call('ZADD', KEYS[1], ARGV[3], ARGV[3])" +
                    "  redis.call('EXPIRE', KEYS[1], ARGV[4])" +
                    "  return 1" +
                    "end" +
                    "return 0";
    
    Long result = redisTemplate.execute(
        new DefaultRedisScript<>(script, Long.class),
        Collections.singletonList("rate_limit:" + key),
        windowStart, String.valueOf(limit), now, String.valueOf(windowSeconds)
    );
    
    return result != null && result == 1L;
}
```

**令牌桶**

```
速率控制:
- 令牌按固定速率生成
- 桶有最大容量
- 请求需要获取令牌才能处理
```

***

> **考察要点提示**
>
> 面试官可能考察：
>
> 1. **缓存三大问题**：穿透、击穿、雪崩的区别和解决方案？（考察基础扎实度）
> 2. **分布式锁**：如何保证锁的原子性？释放锁的正确姿势？（考察并发编程能力）
> 3. **事务机制**：Redis事务和数据库事务有什么区别？（考察对事务的深入理解）
> 4. **消息队列**：Stream和List作为消息队列的区别？（考察对Redis消息能力的理解）
>
> **常见陷阱**：
>
> - 忘记释放锁时需要校验value：这是高频面试考点
> - 混淆MULTI/EXEC和Lua脚本的原子性：MULTI保证命令串行，Lua保证整体原子
> - 忽略缓存穿透的空值缓存：直接查DB会导致DB压力

***

## 4. 性能优化与调优实践

### 4.1 内存管理

#### 4.1.1 内存使用分析

```bash
# 查看内存使用概况
redis-cli INFO memory

# 详细内存分析
redis-cli MEMORY USAGE key

# 内存统计
redis-cli MEMORY STATS

# 内存医生诊断
redis-cli MEMORY DOCTOR
```

**INFO memory 关键字段**：

| 字段                 | 说明        |
| ------------------ | --------- |
| used\_memory       | 已使用内存（字节） |
| used\_memory\_peak | 历史峰值内存    |
| used\_memory\_lua  | Lua脚本占用内存 |
| maxmemory          | 最大内存限制    |
| maxmemory\_policy  | 内存淘汰策略    |

#### 4.1.2 内存优化配置

```conf
# 设置最大内存
maxmemory 4gb

# 选择淘汰策略
maxmemory-policy allkeys-lru

# LRU/LFU采样数
maxmemory-samples 5

# 内存碎片率阈值
activedefrag yes
active-defrag-ignore-bytes 100mb
active-defrag-cycle 25

# 开启 jemalloc 内存分配器（默认开启）
```

#### 4.1.3 内存泄漏排查

```
常见原因:
├── 大Key未清理：过期key未按预期删除
├── 内存碎片：频繁创建删除导致碎片
├── 持久化占用：fork导致的内存复制
├── 网络缓冲区：大量连接占用内存
└── 内部缓冲区：复制/发布订阅缓冲区

排查步骤:
1. INFO memory 查看内存分布
2. MEMORY STATS 查看各类型占用
3. 使用rdb-tools分析RDB文件
4. 对比不同时间段的key数量和大小
```

### 4.2 慢查询分析

#### 4.2.1 慢日志配置

```conf
# 慢日志阈值（微秒）
slowlog-log-slower-than 10000

# 慢日志最大长度
slowlog-max-len 128

# 动态调整
redis-cli CONFIG SET slowlog-log-slower-than 5000
redis-cli CONFIG SET slowlog-max-len 256
```

#### 4.2.2 查看慢日志

```bash
# 查看所有慢日志
redis-cli SLOWLOG GET

# 查看指定数量的慢日志
redis-cli SLOWLOG GET 10

# 查看慢日志长度
redis-cli SLOWLOG LEN

# 清空慢日志
redis-cli SLOWLOG RESET
```

#### 4.2.3 慢日志分析

```
慢日志格式:
1) 整型: 唯一ID
2) 整型: 执行时间戳（毫秒）
3) 整型: 执行耗时（微秒）
4) 数组: 执行的命令及参数
5) 字符串: 客户端IP和端口
6) 字符串: 客户端名称

分析要点:
- 哪些命令执行慢？
- 执行频率高的慢命令？
- 平均耗时和最大耗时？
```

### 4.3 大Key与热点Key

#### 4.3.1 大Key检测

```bash
# 使用redis-cli扫描大Key
redis-cli --bigkeys -i 0.1

# 按类型统计
redis-cli --stat

# 扫描指定前缀的key
redis-cli SCAN 0 MATCH prefix:* COUNT 100
```

**大Key判定标准**：

| 类型     | 阈值             | 风险         |
| ------ | -------------- | ---------- |
| String | value > 10KB   | 网络传输慢      |
| List   | 元素数 > 10000    | 遍历慢        |
| Hash   | field数 > 10000 | 全量返回慢      |
| Set    | 元素数 > 10000    | SMEMBERS阻塞 |
| ZSet   | 元素数 > 10000    | 范围查询慢      |

#### 4.3.2 大Key优化

```java
// 拆分大Hash为多个小Hash
// 原: user:profile:100 (10000 fields)
// 拆: user:profile:100:chunk1 (1000 fields)
//     user:profile:100:chunk2 (1000 fields)

// 分页获取数据
public List<User> getUsers(int page, int size) {
    String key = "users";
    long start = (long) page * size;
    long end = start + size - 1;
    return redisTemplate.opsForList().range(key, start, end);
}

// 使用Pipeline批量操作
public void batchSet(Map<String, String> data) {
    List<Object> results = redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
        for (Map.Entry<String, String> entry : data.entrySet()) {
            connection.set(entry.getKey().getBytes(), entry.getValue().getBytes());
        }
        return null;
    });
}
```

#### 4.3.3 热点Key处理

```
热点Key场景:
- 首页商品信息
- 热门排行榜
- 高频访问的用户信息

解决方案:
1. 本地缓存 + Redis 两级缓存
2. Key分片（按用户ID分片）
3. 读写分离，只读副本
4. 业务层限流
```

### 4.4 性能监控

#### 4.4.1 核心监控指标

| 指标    | 命令               | 预警阈值                      |
| ----- | ---------------- | ------------------------- |
| QPS   | INFO stats       | 根据业务基线                    |
| 内存使用率 | INFO memory      | > 80%                     |
| 连接数   | INFO clients     | > 500                     |
| 慢查询数  | SLOWLOG LEN      | > 0                       |
| 复制延迟  | INFO replication | lag > 5s                  |
| 持久化状态 | INFO persistence | rdb\_last\_bgsave\_status |

#### 4.4.2 实时监控命令

```bash
# 实时统计（每5秒刷新）
redis-cli --stat

# 监控客户端连接
redis-cli MONITOR

# 查看服务器信息
redis-cli INFO server

# 查看连接详情
redis-cli CLIENT LIST

# 查看慢命令
redis-cli SLOWLOG GET 10
```

#### 4.4.3 Prometheus监控

```yaml
# Redis Exporter 配置示例
scrape_configs:
  - job_name: 'redis'
    static_configs:
      - targets: ['redis-exporter:9121']
    params:
      redis.addr: 'redis:6379'
      redis.password: ''
```

### 4.5 集群容量规划

#### 4.5.1 容量评估

```
单机Redis容量估算:
- 内存: 最大可用物理内存的70-80%
- QPS: 单命令约10w-20w QPS（简单操作）
- 连接: 单节点支持约1w-2w并发连接

集群容量计算:
- 总内存 = 单节点内存 × 主节点数
- 总QPS = 单节点QPS × 主节点数
- 高可用成本 = 副本数 × 单节点资源
```

#### 4.5.2 集群规划

| 规模 | 主节点数 | 从节点数 | 适用场景       |
| -- | ---- | ---- | ---------- |
| 小型 | 3    | 3    | 100w+ QPS  |
| 中型 | 6    | 6    | 500w+ QPS  |
| 大型 | 9+   | 9+   | 1000w+ QPS |

#### 4.5.3 扩容策略

```
垂直扩容:
- 增加单节点内存
- 升级CPU
- 适用于小规模集群

水平扩容:
- 添加新主节点
- Redis Cluster自动分片
- 在线迁移slot
```

### 4.6 最佳实践

#### 4.6.1 命令使用规范

| 规范         | 说明      | 示例                        |
| ---------- | ------- | ------------------------- |
| 避免KEYS     | 用SCAN替代 | SCAN 0 MATCH \* COUNT 100 |
| 限制返回量      | 分页查询    | LRANGE key 0 99           |
| 避免阻塞命令     | 使用非阻塞命令 | XREAD而非BLPOP              |
| 合理设置TTL    | 避免永不过期  | SET key value EX 3600     |
| 使用Pipeline | 减少网络往返  | PIPELINE ... EXEC         |

#### 4.6.2 数据模型设计

```
设计原则:
1. Key命名规范: 业务:对象:唯一标识
   - user:profile:123
   - order:detail:order_456
   - product:stock:item_789

2. 避免大Key: value < 10KB, 集合元素 < 1000

3. 合理选型:
   - 简单KV: String
   - 对象存储: Hash
   - 队列消息: Stream
   - 排行榜: ZSet

4. 考虑TTL: 热点数据设长TTL，冷数据设短TTL
```

#### 4.6.3 客户端优化

```java
// 连接池配置
@Configuration
public class RedisConfig {
    
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        LettuceClientConfiguration clientConfig = LettuceClientConfiguration.builder()
            .connectTimeout(Duration.ofSeconds(5))
            .commandTimeout(Duration.ofSeconds(3))
            .build();
        
        return new LettuceConnectionFactory(host, port, clientConfig);
    }
    
    @Bean
    public RedisTemplate<String, Object> redisTemplate(
            RedisConnectionFactory connectionFactory) {
        
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 使用String序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // 使用JSON序列化器
        GenericJackson2JsonRedisSerializer jsonSerializer = 
            new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        return template;
    }
}

// 合理使用Pipeline
public void batchOperations() {
    redisTemplate.executePipelined((RedisCallback<Object>) connection -> {
        for (int i = 0; i < 1000; i++) {
            String key = "batch:key:" + i;
            connection.set(key.getBytes(), ("value:" + i).getBytes());
        }
        return null;
    });
}
```

***

> **考察要点提示**
>
> 面试官可能考察：
>
> 1. **大Key问题**：如何识别和处理大Key？（考察生产经验）
> 2. **内存优化**：如何分析内存使用情况？（考察排查能力）
> 3. **慢查询排查**：发现慢命令后如何优化？（考察性能调优能力）
> 4. **容量规划**：如何评估集群容量？（考察架构设计能力）
>
> **常见陷阱**：
>
> - 忽略KEYS命令的阻塞风险：生产环境禁用KEYS \*
> - 忘记设置TTL导致内存泄漏：所有缓存key都应有过期时间
> - 大Key直接删除：应用UNLINK而非DEL异步删除

***

## 5. 实际应用场景与解决方案

### 5.1 电商系统缓存设计

#### 5.1.1 商品信息缓存

```java
@Service
public class ProductCacheService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private static final String PRODUCT_KEY = "product:detail:%d";
    private static final String PRODUCT_STOCK_KEY = "product:stock:%d";
    private static final long PRODUCT_TTL = 3600; // 1小时
    
    public ProductDetail getProductDetail(Long productId) {
        String key = String.format(PRODUCT_KEY, productId);
        
        // 1. 先查Redis缓存
        Object cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            return (ProductDetail) cached;
        }
        
        // 2. 缓存未命中，查DB
        ProductDetail product = productRepository.findById(productId);
        if (product == null) {
            // 缓存空值，防止穿透
            redisTemplate.opsForValue().set(key, "NULL", 60, TimeUnit.SECONDS);
            return null;
        }
        
        // 3. 写入缓存
        redisTemplate.opsForValue().set(key, product, PRODUCT_TTL, TimeUnit.SECONDS);
        return product;
    }
    
    public void updateProductStock(Long productId, int stock) {
        // Lua原子操作扣减库存
        String script = "local stock = redis.call('GET', KEYS[1]) " +
                        "if stock and tonumber(stock) >= tonumber(ARGV[1]) then " +
                        "redis.call('DECRBY', KEYS[1], ARGV[1]) " +
                        "return 1 " +
                        "end " +
                        "return 0";
        
        redisTemplate.execute(new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList(String.format(PRODUCT_STOCK_KEY, productId)),
            String.valueOf(stock));
    }
}
```

#### 5.1.2 秒杀场景设计

```java
@Service
public class SeckillService {
    
    @Autowired
    private RedissonClient redissonClient;
    
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    
    /**
     * 秒杀核心流程
     * 1. 预扣减库存（Redis原子操作）
     * 2. 创建订单（异步）
     * 3. 失败回滚库存
     */
    public SeckillResult seckill(Long userId, Long productId) {
        String stockKey = "seckill:stock:" + productId;
        String userKey = "seckill:user:" + productId;
        
        // 1. 检查用户是否已抢购（使用Set去重）
        Boolean isMember = stringRedisTemplate.opsForSet()
            .isMember(userKey, userId.toString());
        if (Boolean.TRUE.equals(isMember)) {
            return SeckillResult.fail("您已参与过本次秒杀");
        }
        
        // 2. Lua脚本原子扣减库存+记录用户
        String script = "local stock = redis.call('GET', KEYS[1]) " +
                        "if stock and tonumber(stock) > 0 then " +
                        "redis.call('DECR', KEYS[1]) " +
                        "redis.call('SADD', KEYS[2], ARGV[1]) " +
                        "return 1 " +
                        "end " +
                        "return 0";
        
        Long result = stringRedisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Arrays.asList(stockKey, userKey),
            userId.toString());
        
        if (result == null || result != 1L) {
            return SeckillResult.fail("商品已售罄");
        }
        
        // 3. 异步创建订单
        createOrderAsync(userId, productId);
        
        return SeckillResult.success();
    }
    
    /**
     * 预加载秒杀商品库存
     */
    public void preloadStock(Long productId, int stock) {
        String stockKey = "seckill:stock:" + productId;
        String userKey = "seckill:user:" + productId;
        
        stringRedisTemplate.opsForValue().set(stockKey, stock.toString());
        stringRedisTemplate.delete(userKey);
    }
}
```

### 5.2 社交系统数据存储

#### 5.2.1 关系链存储方案

```java
@Service
public class SocialGraphService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String FOLLOWERS_KEY = "user:followers:%d";
    private static final String FOLLOWING_KEY = "user:following:%d";
    
    /**
     * 关注关系存储
     * - 使用Set存储粉丝/关注列表
     * - 使用ZSet存储关注时间（便于排序）
     */
    public void followUser(Long userId, Long targetUserId) {
        // 1. 将target加入user的关注列表
        redisTemplate.opsForSet()
            .add(String.format(FOLLOWING_KEY, userId), targetUserId.toString());
        
        // 2. 将user加入target的粉丝列表
        redisTemplate.opsForSet()
            .add(String.format(FOLLOWERS_KEY, targetUserId), userId.toString());
        
        // 3. 记录关注时间（用于排序）
        String timeKey = String.format("user:follow:time:%d", userId);
        redisTemplate.opsForZSet()
            .add(timeKey, targetUserId.toString(), System.currentTimeMillis());
    }
    
    /**
     * 获取共同关注
     */
    public Set<String> getCommonFollowing(Long userId1, Long userId2) {
        String key1 = String.format(FOLLOWING_KEY, userId1);
        String key2 = String.format(FOLLOWING_KEY, userId2);
        return redisTemplate.opsForSet().intersect(key1, key2);
    }
    
    /**
     * 粉丝列表分页
     */
    public List<String> getFollowers(Long userId, int page, int size) {
        String key = String.format(FOLLOWERS_KEY, userId);
        long start = (long) page * size;
        long end = start + size - 1;
        return redisTemplate.opsForList().range("list:" + key, start, end);
    }
}
```

#### 5.2.2 时间线（Feed流）实现

```java
@Service
public class TimelineService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 发布动态 - 推模式（Fan-out）
     * 直接推送到所有粉丝的收件箱
     */
    public void publishPost(Long userId, Post post) {
        // 1. 获取用户粉丝列表
        Set<String> followers = redisTemplate.opsForSet()
            .members("user:followers:" + userId);
        
        // 2. 推送到每个粉丝的时间线
        String postId = savePost(post);
        String postJson = JSON.toJSONString(post);
        
        for (String followerId : followers) {
            String timelineKey = "user:timeline:" + followerId;
            // 使用LPUSH最新的动态到时间线
            redisTemplate.opsForList().leftPush(timelineKey, postJson);
            // 限制时间线长度（保留最新500条）
            redisTemplate.opsForList().trim(timelineKey, 0, 499);
        }
    }
    
    /**
     * 获取用户时间线
     */
    public List<Post> getTimeline(Long userId, int page, int size) {
        String key = "user:timeline:" + userId;
        long start = (long) page * size;
        long end = start + size - 1;
        
        List<String> posts = redisTemplate.opsForList().range(key, start, end);
        return posts.stream()
            .map(json -> JSON.parseObject(json, Post.class))
            .collect(Collectors.toList());
    }
    
    /**
     * 最优方案 - 推拉结合
     * 活跃用户推模式，非活跃用户拉模式
     */
    public void publishPostHybrid(Long userId, Post post) {
        Set<String> activeFollowers = getActiveFollowers(userId);
        
        // 推给活跃粉丝
        for (String followerId : activeFollowers) {
            redisTemplate.opsForList()
                .leftPush("user:timeline:" + followerId, JSON.toJSONString(post));
        }
        
        // 记录到用户自己的动态列表，供非活跃粉丝拉取
        redisTemplate.opsForList()
            .leftPush("user:posts:" + userId, JSON.toJSONString(post));
    }
}
```

### 5.3 排行榜与统计

#### 5.3.1 排行榜实现

```java
@Service
public class RankingService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String RANKING_KEY = "ranking:%s";
    
    /**
     * 更新用户积分
     */
    public void updateScore(String rankingType, Long userId, int score) {
        String key = String.format(RANKING_KEY, rankingType);
        // ZADD：如果member已存在则更新score
        redisTemplate.opsForZSet().add(key, userId.toString(), score);
    }
    
    /**
     * 获取用户排名（从高到低）
     */
    public Long getRank(String rankingType, Long userId) {
        String key = String.format(RANKING_KEY, rankingType);
        // ZREVRANK：返回倒序排名（0-based）
        Long rank = redisTemplate.opsForZSet()
            .reverseRank(key, userId.toString());
        return rank != null ? rank + 1 : null;
    }
    
    /**
     * 获取Top N用户
     */
    public List<RankingItem> getTopN(String rankingType, int n) {
        String key = String.format(RANKING_KEY, rankingType);
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, 0, n - 1);
        
        List<RankingItem> result = new ArrayList<>();
        int rank = 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            result.add(new RankingItem(
                rank++,
                Long.parseLong(tuple.getValue()),
                tuple.getScore().intValue()
            ));
        }
        return result;
    }
    
    /**
     * 获取用户周围排名
     */
    public List<RankingItem> getRankAround(String rankingType, Long userId, int range) {
        Long rank = getRank(rankingType, userId);
        if (rank == null) {
            return Collections.emptyList();
        }
        
        int start = Math.max(0, (int) rank - range - 1);
        int end = (int) rank + range - 1;
        
        String key = String.format(RANKING_KEY, rankingType);
        Set<ZSetOperations.TypedTuple<String>> tuples = redisTemplate.opsForZSet()
            .reverseRangeWithScores(key, start, end);
        
        List<RankingItem> result = new ArrayList<>();
        int currentRank = start + 1;
        for (ZSetOperations.TypedTuple<String> tuple : tuples) {
            result.add(new RankingItem(
                currentRank++,
                Long.parseLong(tuple.getValue()),
                tuple.getScore().intValue()
            ));
        }
        return result;
    }
}
```

#### 5.3.2 UV统计（HyperLogLog）

```java
@Service
public class UvStatisticsService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 记录用户访问（增量更新）
     */
    public void recordVisit(String pageId, String userId) {
        String key = "uv:" + pageId + ":" + LocalDate.now();
        redisTemplate.opsForHyperLogLog().add(key, userId);
    }
    
    /**
     * 获取页面UV（近似值，误差约0.81%）
     */
    public long getPageUv(String pageId, LocalDate date) {
        String key = "uv:" + pageId + ":" + date;
        return redisTemplate.opsForHyperLogLog().size(key);
    }
    
    /**
     * 获取多个页面的总UV
     */
    public long getTotalUv(List<String> pageIds, LocalDate date) {
        List<String> keys = pageIds.stream()
            .map(pageId -> "uv:" + pageId + ":" + date)
            .collect(Collectors.toList());
        return redisTemplate.opsForHyperLogLog().union("uv:total:" + date, keys.toArray(new String[0]));
    }
}
```

### 5.4 系统限流与降级

#### 5.4.1 分布式限流

```java
@Component
public class DistributedRateLimiter {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 滑动窗口限流（Lua脚本原子实现）
     */
    public boolean tryAcquire(String key, int maxRequests, int windowSeconds) {
        String script = "local key = KEYS[1] " +
                        "local now = tonumber(ARGV[1]) " +
                        "local window = tonumber(ARGV[2]) " +
                        "local limit = tonumber(ARGV[3]) " +
                        "local member = now " +
                        
                        "redis.call('ZREMRANGEBYSCORE', key, '-inf', now - window) " +
                        "local current = redis.call('ZCARD', key) " +
                        
                        "if current < limit then " +
                        "redis.call('ZADD', key, now, member) " +
                        "redis.call('PEXPIRE', key, window * 1000) " +
                        "return 1 " +
                        "end " +
                        "return 0";
        
        long now = System.currentTimeMillis();
        Long result = redisTemplate.execute(
            new DefaultRedisScript<>(script, Long.class),
            Collections.singletonList("rate:" + key),
            now, windowSeconds, maxRequests);
        
        return result != null && result == 1L;
    }
    
    /**
     * 分布式注解式限流
     */
    @Aspect
    @Component
    public static class RateLimitAspect {
        
        @Autowired
        private DistributedRateLimiter rateLimiter;
        
        @Around("@annotation(rateLimit)")
        public Object around(ProceedingJoinPoint joinPoint, RateLimit rateLimit) 
                throws Throwable {
            String key = rateLimit.key() + ":" + 
                        joinPoint.getArgs()[0]; // 使用第一个参数作为限流key
            
            boolean acquired = rateLimiter.tryAcquire(
                key, rateLimit.maxRequests(), rateLimit.windowSeconds());
            
            if (!acquired) {
                throw new RateLimitException("请求过于频繁，请稍后重试");
            }
            
            return joinPoint.proceed();
        }
    }
    
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RateLimit {
        String key();
        int maxRequests() default 100;
        int windowSeconds() default 60;
    }
}
```

#### 5.4.2 多级缓存方案

```java
@Service
public class MultiLevelCacheService {
    
    // 本地缓存（Caffeine）
    private final Cache<String, Object> localCache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(60, TimeUnit.SECONDS)
        .build();
    
    // 分布式缓存（Redis）
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    // 数据库
    @Autowired
    private UserRepository userRepository;
    
    /**
     * 三级缓存读取
     */
    public User getUserById(Long userId) {
        String key = "user:" + userId;
        
        // Level 1: 本地缓存
        Object localValue = localCache.getIfPresent(key);
        if (localValue != null) {
            return (User) localValue;
        }
        
        // Level 2: Redis缓存
        String redisValue = redisTemplate.opsForValue().get(key);
        if (redisValue != null) {
            if ("NULL".equals(redisValue)) {
                return null;
            }
            User user = JSON.parseObject(redisValue, User.class);
            // 回填本地缓存
            localCache.put(key, user);
            return user;
        }
        
        // Level 3: 数据库
        User user = userRepository.findById(userId);
        if (user == null) {
            // 缓存空值
            redisTemplate.opsForValue().set(key, "NULL", 2, TimeUnit.MINUTES);
        } else {
            // 写入两级缓存
            redisTemplate.opsForValue().set(key, JSON.toJSONString(user), 1, TimeUnit.HOURS);
            localCache.put(key, user);
        }
        
        return user;
    }
    
    /**
     * 缓存更新（Cache-Aside Pattern）
     */
    public void updateUser(User user) {
        String key = "user:" + user.getId();
        
        // 1. 更新数据库
        userRepository.save(user);
        
        // 2. 删除缓存（而非更新）
        redisTemplate.delete(key);
        localCache.invalidate(key);
    }
}
```

### 5.5 延迟队列实现

#### 5.5.1 基于Redis的延迟队列

```java
@Component
public class RedisDelayQueue {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    private static final String DELAY_QUEUE_KEY = "delay:queue";
    private static final String DELAY_CHANNEL = "delay:channel";
    
    /**
     * 添加延迟任务
     */
    public void addDelayTask(DelayTask task, long delaySeconds) {
        String taskJson = JSON.toJSONString(task);
        long executeTime = System.currentTimeMillis() + delaySeconds * 1000;
        
        // 使用ZSet存储，score为执行时间戳
        redisTemplate.opsForZSet()
            .add(DELAY_QUEUE_KEY, taskJson, executeTime);
    }
    
    /**
     * 轮询执行到期任务（定时任务）
     */
    @Scheduled(fixedDelay = 1000)
    public void executeDelayTasks() {
        long now = System.currentTimeMillis();
        
        // 获取所有到期任务
        Set<ZSetOperations.TypedTuple<String>> tasks = redisTemplate.opsForZSet()
            .rangeByScoreWithScores(DELAY_QUEUE_KEY, 0, now);
        
        if (tasks.isEmpty()) {
            return;
        }
        
        for (ZSetOperations.TypedTuple<String> task : tasks) {
            // 原子移除并发布
            Boolean removed = redisTemplate.opsForZSet()
                .remove(DELAY_QUEUE_KEY, task.getValue());
            
            if (Boolean.TRUE.equals(removed)) {
                // 发布到Channel，由消费者异步处理
                redisTemplate.convertAndSend(DELAY_CHANNEL, task.getValue());
            }
        }
    }
    
    /**
     * 消费延迟任务
     */
    @RedisListener
    public void onMessage(String message) {
        DelayTask task = JSON.parseObject(message, DelayTask.class);
        try {
            executeTask(task);
        } catch (Exception e) {
            // 失败重试或记录到死信队列
            handleFailedTask(task, e);
        }
    }
}
```

#### 5.5.2 使用Redisson延迟队列

```java
@Service
public class RedissonDelayQueueService {
    
    @Autowired
    private RedissonClient redissonClient;
    
    public void sendDelayedMessage(String message, long delay, TimeUnit unit) {
        RBlockingQueue<String> blockingQueue = redissonClient
            .getBlockingQueue("delay:queue");
        blockingQueue.offer(message, delay, unit);
    }
    
    @Scheduled(fixedDelay = 1000)
    public void consumeDelayedMessages() {
        RBlockingQueue<String> blockingQueue = redissonClient
            .getBlockingQueue("delay:queue");
        
        List<String> messages = blockingQueue.drainTo(
            new ArrayList<>(), 100);
        
        for (String message : messages) {
            processMessage(message);
        }
    }
}
```

### 5.6 与其他技术栈集成

#### 5.6.1 Spring Cache 集成

```java
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory) {
        
        RedisCacheConfiguration cacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofHours(1))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()))
            .disableCachingNullValues();
        
        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(cacheConfig)
            .transactionAware()
            .build();
    }
}

@Service
public class UserService {
    
    // 缓存查询结果
    @Cacheable(value = "users", key = "#userId", unless = "#result == null")
    public User getUserById(Long userId) {
        return userRepository.findById(userId);
    }
    
    // 更新缓存
    @CachePut(value = "users", key = "#user.id")
    public User updateUser(User user) {
        return userRepository.save(user);
    }
    
    // 删除缓存
    @CacheEvict(value = "users", key = "#userId")
    public void deleteUser(Long userId) {
        userRepository.deleteById(userId);
    }
    
    // 清除所有缓存
    @CacheEvict(value = "users", allEntries = true)
    public void clearAllCache() {
    }
}
```

#### 5.6.2 分布式会话存储

```java
@Configuration
@EnableRedisHttpSession
public class SessionConfig {
    
    @Bean
    public HttpSessionIdResolver httpSessionIdResolver() {
        return CookieHttpSessionIdResolver.defaultInstance();
    }
}

@Controller
public class LoginController {
    
    @PostMapping("/login")
    public String login(@RequestBody LoginRequest request, 
                        HttpSession session) {
        User user = userService.authenticate(request);
        
        // 会话信息存储到Redis
        session.setAttribute("userId", user.getId());
        session.setAttribute("username", user.getUsername());
        session.setAttribute("loginTime", LocalDateTime.now());
        
        return "redirect:/home";
    }
    
    @GetMapping("/profile")
    public String profile(HttpSession session) {
        Long userId = (Long) session.getAttribute("userId");
        User user = userService.getUserById(userId);
        return "profile";
    }
}
```

#### 5.6.3 发布订阅与消息通知

```java
@Component
public class NotificationService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 发布通知
     */
    public void publishNotification(Notification notification) {
        String channel = "notification:" + notification.getChannel();
        String message = JSON.toJSONString(notification);
        redisTemplate.convertAndSend(channel, message);
    }
    
    /**
     * 订阅通知
     */
    @RedisListener(channels = {"notification:user"})
    public void onUserNotification(String message) {
        Notification notification = JSON.parseObject(message, Notification.class);
        sendToUser(notification);
    }
    
    @RedisListener(channels = {"notification:system"})
    public void onSystemNotification(String message) {
        Notification notification = JSON.parseObject(message, Notification.class);
        sendToAllUsers(notification);
    }
}
```

### 5.7 数据一致性保证

#### 5.7.1 双写一致性方案

```java
@Service
public class DataConsistencyService {
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    /**
     * 先更新数据库，再删除缓存（Cache-Aside）
     * 这是最推荐的方案
     */
    public void updateWithCacheAside(String key, Object value, UpdateCallback callback) {
        // 1. 先更新数据库
        callback.doUpdate();
        
        // 2. 再删除缓存（而非更新）
        redisTemplate.delete(key);
    }
    
    /**
     * 延迟双删（降低不一致概率）
     */
    public void updateWithDoubleDelete(String key, Object value, UpdateCallback callback) {
        // 1. 删除缓存
        redisTemplate.delete(key);
        
        // 2. 更新数据库
        callback.doUpdate();
        
        // 3. 延迟再次删除（防止并发读回填旧数据）
        CompletableFuture.delayedExecutor(500, TimeUnit.MILLISECONDS)
            .execute(() -> redisTemplate.delete(key));
    }
    
    /**
     * 消息队列保证最终一致性
     */
    public void updateWithMq(String key, Object value, UpdateCallback callback) {
        // 1. 更新数据库
        callback.doUpdate();
        
        // 2. 发送删除缓存消息到MQ
        String message = JSON.toJSONString(new CacheEvictMessage(key));
        rocketMQTemplate.syncSend("cache-evict-topic", message);
    }
    
    @RocketMQMessageListener(topic = "cache-evict-topic", consumerGroup = "cache-group")
    public void onCacheEvictMessage(String message) {
        CacheEvictMessage msg = JSON.parseObject(message, CacheEvictMessage.class);
        redisTemplate.delete(msg.getKey());
    }
    
    @FunctionalInterface
    public interface UpdateCallback {
        void doUpdate();
    }
}
```

#### 5.7.2 缓存穿透防护的完整方案

```java
@Service
public class PenetrationGuardService {
    
    // 使用布隆过滤器拦截不存在的key
    private final BloomFilter<String> bloomFilter = BloomFilter.create(
        Funnels.stringFunnel(CharsetUtil.UTF_8),
        1000000,  // 预计元素数量
        0.01      // 误判率
    );
    
    @PostConstruct
    public void init() {
        // 启动时将所有有效key加载到布隆过滤器
        List<Long> allUserIds = userRepository.findAllIds();
        for (Long userId : allUserIds) {
            bloomFilter.put("user:" + userId);
        }
    }
    
    /**
     * 带布隆过滤器的查询
     */
    public User getUserWithBloomFilter(Long userId) {
        String key = "user:" + userId;
        
        // 1. 布隆过滤器预判（快速拦截）
        if (!bloomFilter.mightContain(key)) {
            return null; // 一定不存在
        }
        
        // 2. 查询缓存
        String cached = redisTemplate.opsForValue().get(key);
        if (cached != null) {
            if ("NULL".equals(cached)) {
                return null;
            }
            return JSON.parseObject(cached, User.class);
        }
        
        // 3. 查询数据库
        User user = userRepository.findById(userId);
        if (user == null) {
            // 缓存空值
            redisTemplate.opsForValue().set(key, "NULL", 2, TimeUnit.MINUTES);
        } else {
            redisTemplate.opsForValue().set(key, JSON.toJSONString(user), 1, TimeUnit.HOURS);
        }
        
        return user;
    }
}
```

***

> **考察要点提示**
>
> 面试官可能考察：
>
> 1. **缓存策略选择**：为什么用Cache-Aside而不是其他模式？（考察架构设计能力）
> 2. **分布式锁实现**：秒杀场景如何保证库存扣减的原子性？（考察并发编程能力）
> 3. **数据一致性**：如何保证缓存和数据库的一致性？（考察对CAP理论的理解）
> 4. **消息队列选型**：为什么用Stream而不是List或Pub/Sub？（考察对Redis消息能力的理解）
>
> **常见陷阱**：
>
> - 缓存与数据库一致性：不要尝试做到强一致，追求最终一致即可
> - 秒杀超卖问题：Lua脚本和Redlock是常用解决方案
> - 延迟队列实现：Redis的ZSet方案比轮询更可靠

***

## 6. 常见面试题深度解析

### 6.1 基础概念类

#### Q1: Redis为什么这么快？

**考察点**：对Redis核心设计的理解

**标准答案**：

1. **基于内存**：数据存储在内存中，读写速度远快于磁盘
2. **单线程模型**：核心命令执行单线程，避免了上下文切换和锁竞争
3. **IO多路复用**：使用epoll/kqueue实现高并发网络处理，单线程处理大量连接
4. **高效数据结构**：
   - SDS字符串：预分配减少内存重分配
   - 跳表：O(logN)的范围查询
   - 压缩列表：紧凑的内存布局
5. **零拷贝**：使用sendfile等系统调用减少数据拷贝
6. **异步持久化**：fork子进程进行持久化，不阻塞主线程

**追问点**：

- Redis是完全单线程吗？（不是，IO和持久化是多线程）
- 单线程如何处理高并发？（IO多路复用）

#### Q2: Redis的数据淘汰策略有哪些？

**考察点**：对内存管理的理解

**标准答案**：

```
# 8种淘汰策略
1. noeviction: 不淘汰，写入会报错
2. allkeys-lru: 从所有key中淘汰最近最少使用的
3. volatile-lru: 从设置了TTL的key中淘汰LRU
4. allkeys-lfu: 从所有key中淘汰最不经常使用的
5. volatile-lfu: 从设置了TTL的key中淘汰LFU
6. allkeys-random: 从所有key中随机淘汰
7. volatile-random: 从设置了TTL的key中随机淘汰
8. volatile-ttl: 淘汰TTL最短的key
```

**追问点**：

- LRU和LFU的区别？（LRU看最近使用，LFU看使用频率）
- 生产环境推荐用哪种？（allkeys-lru或allkeys-lfu）
- Redis的LRU是真正的LRU吗？（是近似LRU，使用采样算法）

#### Q3: Redis如何实现分布式锁？

**考察点**：对并发编程的理解

**标准答案**：

```redis
# 1. 获取锁（原子操作）
SET lock_key unique_value NX PX 30000
# NX: 不存在才设置
# PX: 毫秒级过期时间

# 2. 释放锁（Lua原子操作）
EVAL "
  if redis.call('GET', KEYS[1]) == ARGV[1] then
    return redis.call('DEL', KEYS[1])
  else
    return 0
  end
" 1 lock_key unique_value
```

**核心要素**：

1. **原子性**：SET NX PX必须原子执行
2. **防死锁**：必须设置过期时间
3. **防误删**：释放锁时校验value
4. **可重入**：Redisson的看门狗机制

**追问点**：

- 为什么释放锁要用Lua脚本？（保证原子性）
- Redisson的续期机制如何实现？（看门狗线程定期续期）
- Redis主从切换会导致锁丢失吗？（会，需要RedLock或Zookeeper）

### 6.2 进阶原理类

#### Q4: Redis的持久化机制如何选择？

**考察点**：工程决策能力

**标准答案**：

| 维度   | RDB     | AOF  | 混合持久化 |
| ---- | ------- | ---- | ----- |
| 数据安全 | 丢失数分钟   | 丢失1秒 | 丢失1秒  |
| 恢复速度 | 快       | 较慢   | 快     |
| 性能影响 | 保存时fork | 持续IO | 较小    |
| 文件体积 | 小       | 大    | 小     |

**选择策略**：

1. **混合持久化（推荐）**：兼顾恢复速度和数据安全
2. **纯RDB**：可容忍少量数据丢失
3. **纯AOF**：对数据安全要求极高

**追问点**：

- fork的Copy-On-Write是什么？（写时复制，减少内存开销）
- AOF重写触发条件？（文件增长100%且超过64MB）
- 混合持久化的RDB+AOF如何工作？（前半段RDB，后半段AOF增量）

#### Q5: Redis集群如何保证高可用？

**考察点**：对高可用架构的理解

**标准答案**：

**哨兵模式**：

```
1. 监控：定期检查主从节点状态
2. 自动故障转移：主节点故障时自动选主
   - 主观下线 → 客观下线（多数哨兵同意）
   - 选出新主节点（按复制offset排序）
3. 配置通知：客户端通过哨兵获取最新主节点信息
```

**Cluster模式**：

```
1. 数据分片：16384个Hash Slot分配给主节点
2. 主从复制：每个主节点有一个从节点
3. 故障转移：
   - 从节点故障：不影响服务
   - 主节点故障：从节点升级为主节点
   - 集群故障：不可用（需要主从都挂）
4. 在线扩缩容：支持在线添加/移除节点
```

**追问点**：

- 哨兵如何选主？（数据最新、优先级高的从节点）
- Cluster的Hash Slot如何计算？（CRC16(key) % 16384）
- 集群中如何执行多key命令？（key必须在同一slot，使用Hash Tag）

#### Q6: 缓存穿透、击穿、雪崩的区别和解决方案？

**考察点**：对缓存问题的深入理解

**标准答案**：

| 问题 | 场景            | 解决方案              |
| -- | ------------- | ----------------- |
| 穿透 | 查询不存在的key     | 布隆过滤器、缓存空值        |
| 击穿 | 热点key过期瞬间大量并发 | 互斥锁、逻辑过期          |
| 雪崩 | 大量key同时过期     | TTL加随机值、多级缓存、限流降级 |

**穿透 vs 击穿 vs 雪崩**：

- **穿透**：key根本不存在，每次都打到DB
- **击穿**：key存在但刚好过期，大量请求同时回源
- **雪崩**：大量key同时过期，DB压力骤增

**追问点**：

- 布隆过滤器的优缺点？（优点：高效；缺点：有误判、不可删除）
- 逻辑过期如何实现？（缓存永不过期，异步更新）
- 雪崩和击穿的区别？（击穿是单个热点key，雪崩是大面积key）

### 6.3 架构设计类

#### Q7: 如何设计一个高并发的Redis缓存系统？

**考察点**：系统设计能力

**参考答案**：

1. **数据分层**：
   - 本地缓存（Caffeine/Guava Cache）：热点数据
   - Redis缓存：全量缓存
   - 数据库：持久存储
2. **缓存策略**：
   - Cache-Aside模式
   - TTL加随机值防雪崩
   - 布隆过滤器防穿透
   - 互斥锁防击穿
3. **高可用设计**：
   - Redis Cluster集群
   - 主从复制+哨兵
   - 客户端连接池
4. **性能优化**：
   - Pipeline批量操作
   - Lua脚本原子操作
   - 合理使用数据结构
5. **监控告警**：
   - 慢日志监控
   - 内存使用率监控
   - 命中率监控

**追问点**：

- 命中率低的原因？（缓存策略不合理、TTL设置不当）
- 如何评估缓存效果？（对比直接查DB和走缓存的QPS和延迟）
- 如何处理缓存雪崩？（多级缓存、限流降级、灰度发布）

#### Q8: Redis能否作为消息队列？如何实现？

**考察点**：对Redis消息能力的理解

**参考答案**：

**方案对比**：

| 方案      | 特点            | 适用场景   |
| ------- | ------------- | ------ |
| List    | 简单可靠、阻塞读取     | 简单队列   |
| Stream  | 消费组、ACK确认、可回溯 | 复杂消息队列 |
| Pub/Sub | 实时推送、不持久      | 实时通知   |
| ZSet    | 可延迟           | 延迟队列   |

**Stream消费组示例**：

```redis
# 创建消费组
XGROUP CREATE mystream mygroup

# 生产者发送消息
XADD mystream * type order_create data "..."

# 消费者组读取
XREADGROUP GROUP mygroup consumer1 COUNT 10 STREAMS mystream >

# 确认消息处理
XACK mystream mygroup message_id

# 查看未处理消息
XPENDING mystream mygroup
```

**追问点**：

- Redis Stream vs Kafka/RocketMQ？（Redis轻量级，不适合大规模消息积压）
- 如何保证消息不丢失？（Stream的持久化、ACK机制）
- 如何处理消息重复消费？（幂等设计）

#### Q9: 如何处理Redis的大Key问题？

**考察点**：生产问题排查能力

**参考答案**：

**大Key危害**：

1. 网络传输慢（客户端读取慢）
2. 阻塞命令（如KEYS/HGETALL）
3. 内存不均（集群中节点内存失衡）
4. 持久化慢（fork时内存占用大）

**检测方法**：

```bash
# 自动扫描大Key
redis-cli --bigkeys -i 0.1

# 手动扫描
redis-cli SCAN 0 MATCH * COUNT 100
redis-cli MEMORY USAGE key_name
```

**优化方案**：

```
1. 拆分大Key
   - 大Hash拆为多个小Hash
   - 大List拆为多个小List

2. 分页获取
   - LRANGE key 0 99 （每次取100条）
   - HSCAN key 0 COUNT 100

3. 异步删除
   - 使用UNLINK替代DEL
   - 后台线程异步释放内存
```

**追问点**：

- 什么是大Key？（String > 10KB，集合元素 > 10000）
- 如何预防大Key？（数据模型设计、定期检查）
- 集群中大Key的影响？（节点内存不均、slot迁移失败）

### 6.4 故障排查类

#### Q10: Redis线上问题如何排查？

**考察点**：实际问题解决能力

**常见问题与排查步骤**：

**1. 内存泄漏**

```bash
# 查看内存分布
redis-cli INFO memory
redis-cli MEMORY STATS

# 对比key数量
redis-cli INFO keyspace

# 分析RDB文件
rdb-tools redis_dump.rdb
```

**2. 性能下降**

```bash
# 查看慢日志
redis-cli SLOWLOG GET 20

# 实时监控
redis-cli --stat

# 分析命令耗时
redis-cli --latency
```

**3. 连接数过多**

```bash
# 查看连接列表
redis-cli CLIENT LIST

# 分析连接来源
redis-cli CLIENT LIST | grep -oP 'addr=.*? ' | sort | uniq -c | sort -rn

# 设置连接数上限
redis-cli CONFIG SET maxclients 10000
```

**4. 持久化问题**

```bash
# 查看持久化状态
redis-cli INFO persistence

# 查看RDB/AOF大小
redis-cli INFO persistence | grep rdb_last_save_time
ls -lh /var/lib/redis/dump.rdb
ls -lh /var/lib/redis/appendonly.aof
```

**排查流程**：

1. 监控告警 → 定位问题
2. INFO命令 → 分析整体状态
3. SLOWLOG/CLIENT LIST → 定位具体问题
4. 根据问题类型 → 针对性解决

**追问点**：

- Redis OOM怎么办？（调整maxmemory、优化数据结构、增加节点）
- 主从同步延迟怎么办？（检查网络、调整repl-backlog-size、无盘复制）
- 如何避免频繁fork？（调整持久化策略、使用AOF混合持久化）

***

> **考察要点提示**
>
> 面试官可能考察：
>
> 1. **原理深度**：Redis快的原因、数据结构实现
> 2. **工程实践**：缓存策略选择、分布式锁实现
> 3. **架构设计**：高可用方案、系统容量规划
> 4. **问题排查**：线上问题定位和解决
>
> **高分技巧**：
>
> - 不仅要给出答案，还要能说明为什么
> - 结合实际项目经验给出具体案例
> - 了解方案的优缺点和适用场景
> - 能主动提出更好的解决方案

***

## 7. 模拟面试问答（高频题）

### 7.1 初级题（1-3年经验）

#### 问答1: Redis和Memcached的区别？

**回答要点**：

1. 数据结构：Redis支持多种数据结构，Memcached只支持String
2. 持久化：Redis支持持久化，Memcached不支持
3. 高可用：Redis支持主从/集群，Memcached需要客户端实现
4. 功能丰富度：Redis支持发布订阅、事务、Lua脚本等

#### 问答2: String类型在Redis中的使用场景？

**回答要点**：

1. 缓存对象（JSON序列化存储）
2. 计数器（INCR/DECR原子操作）
3. 分布式锁（SET NX PX）
4. Session存储
5. 限流计数器

#### 问答3: 如何保证Redis中Key的命名规范？

**回答要点**：

1. 格式：业务模块:对象类型:唯一标识
   - user:profile:123
   - order:detail:order\_456
2. 前缀区分：不同业务使用不同前缀
3. 长度控制：避免过长的key名
4. 字符选择：只使用字母、数字、冒号

#### 问答4: Redis的过期删除策略？

**回答要点**：

1. **惰性删除**：访问key时检查是否过期
2. **定期删除**：定时任务随机检查部分key
3. **内存淘汰**：内存不足时触发淘汰策略

### 7.2 中级题（3-5年经验）

#### 问答5: 如何设计一个支持高并发的秒杀系统？

**回答要点**：

1. **预热阶段**：提前将库存加载到Redis
2. **原子扣减**：Lua脚本保证扣减的原子性
3. **限流措施**：滑动窗口限流防止恶意请求
4. **异步下单**：Kafka/RocketMQ异步处理订单
5. **最终一致性**：本地消息表保证DB和MQ的一致性

#### 问答6: Redis的缓存穿透如何解决？

**回答要点**：

1. **布隆过滤器**：在缓存层前增加布隆过滤器拦截无效请求
2. **缓存空值**：对不存在的数据也进行缓存，设置较短TTL
3. **参数校验**：在业务层对请求参数进行有效性校验

#### 问答7: 如何实现Redis的分布式锁？需要注意什么？

**回答要点**：

1. **加锁**：SET key value NX PX expire\_time
2. **解锁**：Lua脚本原子删除（校验value）
3. **注意事项**：
   - 必须设置过期时间防死锁
   - 释放锁必须校验value防误删
   - 考虑业务执行时间，适当延长过期时间
   - 集群环境考虑使用RedLock或Zookeeper

#### 问答8: Redis的持久化RDB和AOF如何选择？

**回答要点**：

1. **RDB**：文件小、恢复快，但可能丢失数据
2. **AOF**：数据安全，但文件大、恢复慢
3. **混合持久化**（推荐）：结合两者优点
4. **选择依据**：
   - 数据敏感场景：AOF或混合持久化
   - 可容忍少量丢失：RDB即可

### 7.3 高级题（5年以上经验）

#### 问答9: Redis Cluster如何保证数据一致性和高可用？

**回答要点**：

1. **数据分片**：16384个Hash Slot均匀分配给主节点
2. **主从复制**：每个主节点有一个或多个从节点
3. **故障转移**：
   - 主节点故障 → 从节点升级
   - 脑裂问题：quorum机制避免
4. **数据一致性**：
   - 最终一致性（主从异步复制）
   - 可通过WAIT命令同步确认

#### 问答10: 如何优化Redis的内存使用？

**回答要点**：

1. **数据结构优化**：
   - 使用Hash代替多个String
   - 合理设置ziplist阈值
   - 避免大Key
2. **内存分配器**：
   - 使用jemalloc（默认）
   - 开启activedefrag自动碎片整理
3. **TTL策略**：
   - 合理设置过期时间
   - 定期清理无用key
4. **集群分片**：
   - 横向扩展节点数
   - 均匀分布数据

#### 问答11: Redis的Lua脚本如何保证原子性？

**回答要点**：

1. **原子执行**：Lua脚本在Redis中是原子执行的，不会被其他命令中断
2. **应用场景**：
   - 分布式锁（SET+校验+DEL）
   - 库存扣减（GET+DECR）
   - 限流（ZREMRANGEBYSCORE+ZCARD+ZADD）
3. **注意事项**：
   - 脚本要短小精悍，避免长时间阻塞
   - 不要在Lua中执行耗时操作
   - 使用EVALSHA缓存脚本

#### 问答12: 如何设计一个支持百万QPS的Redis集群？

**回答要点**：

1. **集群架构**：
   - Redis Cluster：至少6个节点（3主3从）
   - 每个主节点：8-16核CPU，32-64GB内存
2. **性能优化**：
   - Pipeline批量操作
   - 连接池优化
   - 合理设置timeout
3. **数据分片**：
   - 按业务分库分表
   - 热点key分片
4. **高可用**：
   - 主从复制+哨兵
   - 异地多活
5. **监控告警**：
   - QPS、延迟、内存监控
   - 慢日志分析
   - 容量规划

### 7.4 情景题

#### 问答13: 如果Redis挂了怎么办？

**回答要点**：

1. **预防措施**：
   - Redis Cluster多节点部署
   - 主从复制+哨兵自动故障转移
   - 异地多活部署
2. **降级方案**：
   - 本地缓存（Caffeine/Guava Cache）
   - 直接查数据库（限流保护）
   - 静态页面化
3. **恢复步骤**：
   - 检查节点状态
   - 恢复主从复制
   - 预热缓存

#### 问答14: Redis内存使用超过maxmemory会怎样？

**回答要点**：

1. **触发淘汰策略**：
   - noeviction：写入报错（默认策略）
   - allkeys-lru：淘汰最近最少使用的key
   - volatile-lru：淘汰设置了TTL的key中LRU的
2. **设置合理的淘汰策略**：
   - 推荐allkeys-lru或allkeys-lfu
   - 为所有key设置TTL
3. **监控和告警**：
   - 监控内存使用率
   - 提前扩容

#### 问答15: 如何处理Redis热点Key问题？

**回答要点**：

1. **本地缓存**：热点数据存本地缓存（Caffeine）
2. **Key分片**：按用户ID等维度拆分热点Key
3. **读写分离**：只读副本分担读压力
4. **缓存预热**：提前加载热点数据
5. **业务层优化**：
   - 商品详情静态化
   - 使用CDN加速

***

## 附录

### 常用Redis命令速查表

#### 数据类型命令

| 类型     | 命令                               | 说明     |
| ------ | -------------------------------- | ------ |
| String | SET, GET, DEL, INCR, DECR        | 字符串操作  |
| Hash   | HSET, HGET, HKEYS, HVALS         | 哈希表操作  |
| List   | LPUSH, RPUSH, LPOP, RPOP, LRANGE | 列表操作   |
| Set    | SADD, SREM, SMEMBERS, SINTER     | 集合操作   |
| ZSet   | ZADD, ZRANGE, ZRANK, ZREVRANK    | 有序集合操作 |
| Bitmap | SETBIT, GETBIT, BITCOUNT         | 位图操作   |
| Stream | XADD, XREAD, XGROUP, XACK        | 流操作    |

#### 服务器命令

| 命令               | 说明         |
| ---------------- | ---------- |
| INFO \[section]  | 获取服务器信息    |
| SLOWLOG GET \[n] | 获取慢日志      |
| CLIENT LIST      | 客户端列表      |
| MONITOR          | 实时监控所有命令   |
| CONFIG GET/SET   | 获取/设置配置    |
| DBSIZE           | 当前数据库key数量 |
| FLUSHDB          | 清空当前数据库    |
| FLUSHALL         | 清空所有数据库    |

#### 集群命令

| 命令                | 说明       |
| ----------------- | -------- |
| CLUSTER INFO      | 集群信息     |
| CLUSTER NODES     | 集群节点列表   |
| CLUSTER SLOTS     | Slot分配信息 |
| CLUSTER FAILOVER  | 手动故障转移   |
| CLUSTER REPLICATE | 设置从节点    |

### 关键配置参数

```conf
# 网络配置
bind 0.0.0.0
port 6379
tcp-backlog 511
timeout 0
tcp-keepalive 300

# 内存配置
maxmemory 4gb
maxmemory-policy allkeys-lru
maxmemory-samples 5

# 持久化配置
save 900 1
save 300 10
save 60 10000
appendonly yes
appendfsync everysec
auto-aof-rewrite-percentage 100
auto-aof-rewrite-min-size 64mb

# 日志配置
loglevel notice
logfile /var/log/redis/redis.log
slowlog-log-slower-than 10000
slowlog-max-len 128

# 客户端配置
maxclients 10000
requirepass your_password

# 集群配置
cluster-enabled yes
cluster-config-file nodes-6379.conf
cluster-node-timeout 15000
```

### 推荐学习资源

| 资源                                                     | 说明             |
| ------------------------------------------------------ | -------------- |
| [Redis官方文档](https://redis.io/docs)                     | 最权威的文档         |
| [Redis设计与实现](http://redisbook.com)                     | 黄健宏著，深入剖析Redis |
| [Redis开发与运维](https://book.douban.com/subject/26612779) | 付磊、张益军著        |
| [Redis源码](https://github.com/redis/redis)              | 源码学习           |
| [Redis实战](https://book.douban.com/subject/26612779)    | 经典入门书籍         |

***

> **文档版本**：v2.0\
> **最后更新**：2026年7月\
> **适用Redis版本**：5.x / 6.x / 7.x

