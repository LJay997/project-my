# Redis 面试题（结构化版）

> 目标：覆盖常见面试所有主线（数据结构、持久化、淘汰、复制与高可用、集群、缓存、锁、性能与排障）。示例以 Redis 6/7 的通用行为为主，个别点会标注差异。

## 目录

1. [基础与整体认知](#1-基础与整体认知)
2. [核心数据结构与底层编码](#2-核心数据结构与底层编码)
3. [键空间、过期与内存淘汰](#3-键空间过期与内存淘汰)
4. [持久化：RDB 与 AOF](#4-持久化rdb-与-aof)
5. [复制、哨兵与高可用](#5-复制哨兵与高可用)
6. [集群：Redis Cluster](#6-集群redis-cluster)
7. [事务、Lua、Pipeline 与一致性](#7-事务luapipeline-与一致性)
8. [缓存设计：穿透/击穿/雪崩](#8-缓存设计穿透击穿雪崩)
9. [分布式锁与限流](#9-分布式锁与限流)
10. [热点与大 Key、性能调优与排障](#10-热点与大-key性能调优与排障)
11. [速记对比表（面试最后 1 分钟）](#11-速记对比表面试最后-1-分钟)
12. [Spring/Java 常用落地片段](#12-springjava-常用落地片段)

---

## 1. 基础与整体认知

### 1.1 Redis 是什么？为什么快？

- **一句话结论**：Redis 是内存 KV 数据库，支持丰富数据结构；快的主要原因是 **内存访问 + 高效数据结构 + 单线程事件循环（核心命令执行）+ I/O 多路复用**。
- **常见追问**：
  - 单线程就一定不会阻塞吗？  
    - **不会被线程切换拖慢**，但 **慢命令/大 Key/阻塞命令** 仍会阻塞事件循环。
  - Redis 完全是单线程吗？  
    - 核心命令处理是单线程；网络 I/O、后台持久化 fork、部分版本/配置的 I/O 线程属于辅助。

---

### 1.2 Redis 与 Memcached 区别？

| 维度 | Redis | Memcached |
| --- | --- | --- |
| 数据结构 | String/Hash/List/Set/ZSet/Stream 等 | 主要是 KV（String） |
| 持久化 | RDB/AOF | 通常无（或依赖外部） |
| 复制/高可用 | 主从/哨兵/集群 | 能力弱/依赖外部 |
| 原子操作 | 丰富（含 Lua） | 相对有限 |
| 典型场景 | 缓存、计数、排行榜、队列、锁、会话 | 纯缓存 |

---

## 2. 核心数据结构与底层编码

### 2.1 Redis 有哪些数据结构？各自场景？

- **String**：缓存对象、计数器、分布式锁 token、简单状态
- **Hash**：对象字段（用户信息、商品信息），减少 key 数
- **List**：消息队列/任务队列（更推荐 Stream）、时间线
- **Set**：去重、共同好友、标签集合、抽奖
- **ZSet**：排行榜、延时队列（score=时间戳）、范围查询
- **Stream**：消息队列、事件流、消费组（常用替代 List）
- **Bitmap**：签到、布尔标记集合、活跃统计
- **HyperLogLog**：UV 近似去重（可接受误差）
- **GEO**：附近的人/门店

---

### 2.2 底层编码（encoding）是什么？为什么要关心？

- **一句话结论**：同一逻辑类型会根据数据规模/内容选择不同底层表示，以平衡 **内存与性能**。
- **面试说法**：小数据用紧凑结构节省内存，变大后自动转为更适合的结构。

> 易错点：不要背版本细节到“某结构一定是 ziplist/listpack”。更重要的是说明：**编码会自动升级**，大 Key 会拖慢命令执行。

---

### 2.3 String / List / Hash / Set / ZSet 到底“怎么存”的？

> 下面说的是 **逻辑结构** 与 **常见底层表示**。具体阈值（多少元素/多长字符串触发转换）与版本、配置有关，面试抓住“**小用紧凑结构，长大自动升级**”即可。

#### 2.3.1 String（最常用）

- **逻辑上**：一个 key 对应一个 value（字节序列）。
- **底层常见表示**：
  - **整数优化**：如果 value 是纯数字且可表示为整型，会用整数形式存（省内存、算术快）
  - **短字符串**：更紧凑的字符串对象表示（减少分配开销）
  - **普通字符串**：按字节存储（常见为 SDS 一类的结构，记录长度、预分配等）
- **你需要记的坑**：
  - String 不等于 Java 的 `String`：Redis value 本质是字节序列，编码（UTF-8/JSON）由你决定
  - 大 value（比如几 MB）会导致：网络传输慢、阻塞事件循环、AOF/RDB/复制更重

---

#### 2.3.2 List（链表语义 / 队列语义）

- **逻辑上**：有序、可重复，支持左右两端 push/pop（队列/栈）。
- **底层常见表示**：
  - **小列表**：用紧凑的连续内存结构存（节省指针/碎片）
  - **大列表**：用“分段的连续块 + 链接”的结构存（常见叫 quicklist 这类：每段是紧凑块，段与段之间链接）
- **性能直觉**：
  - `LPUSH/RPUSH/LPOP/RPOP`：通常是 \(O(1)\)
  - `LINDEX/LRANGE`：范围越大越慢（可能要扫描/拷贝大量元素）
- **易错点**：
  - 用 List 当消息队列要小心：消费者确认、重复消费、消息堆积等问题；生产更推荐 **Stream + 消费组**

---

#### 2.3.3 Hash（对象字段）

- **逻辑上**：key 下是一组 field -> value（类似 Map）。
- **底层常见表示**：
  - **小 Hash**：紧凑结构（field/value 成对、连续存储）
  - **大 Hash**：真正的哈希表（dict/hashtable）
- **性能直觉**：
  - `HSET/HGET`：平均 \(O(1)\)
  - `HGETALL`：返回全部字段，字段多会变慢且占带宽
- **易错点**：
  - Hash 的“对象化”不等于关系型：字段很多、字段经常变化、单 key 过热都可能成为热点/大 key

---

#### 2.3.4 Set（无序去重集合）

- **逻辑上**：无序、元素唯一。
- **底层常见表示**：
  - **全是整数且数量不大**：用紧凑整数集合（intset）存（省内存）
  - **否则**：用哈希表存（dict/hashtable），以元素为 key（value 常为空占位）
- **性能直觉**：
  - `SADD/SISMEMBER/SREM`：平均 \(O(1)\)
  - `SMEMBERS`：全量返回，元素多会慢
- **易错点**：
  - Set 适合“去重/关系集合”，不适合塞超大对象；大集合全量拉取会把 Redis 卡住

---

#### 2.3.5 ZSet（有序集合：score + member）

- **逻辑上**：member 唯一；每个 member 有 score，用 score 排序。
- **底层常见表示**：
  - **小 ZSet**：紧凑结构（连续存储 member/score）
  - **大 ZSet**：常见是“**哈希表 + 跳表**”组合
    - **哈希表**：member -> score（用于快速查 member、更新分数）
    - **跳表**：按 score 排序（用于范围查询、topN）
- **性能直觉**：
  - 插入/更新：约 \(O(\log N)\)
  - topN / 范围：约 \(O(\log N + M)\)
- **易错点**：
  - score 是 double：注意精度与排序一致性（比如用时间戳/整数更稳）

---

#### 2.3.6 一张“面试用”总结表

| 类型 | 逻辑结构 | 小数据常见表示 | 变大后常见表示 | 大 key 风险 |
| --- | --- | --- | --- | --- |
| String | 单值 | int/短字符串 | 普通字节串结构 | 大 value 传输与阻塞 |
| List | 有序可重复 | 紧凑连续结构 | 分段块 + 链接 | 大范围 LRANGE 阻塞 |
| Hash | field->value | 紧凑键值对 | 哈希表 | HGETALL/字段爆炸 |
| Set | 无序去重 | intset | 哈希表 | SMEMBERS 全量返回 |
| ZSet | score 排序 | 紧凑结构 | 跳表 + 哈希表 | 大排行与范围查询压力 |

---

### 2.4 底层结构速记：SDS / quicklist / dict(hashtable) / 跳表

> 你可以用一句话串起来：**String 用 SDS 存字节；List 用 quicklist（分段块）；Hash/Set 用 dict(hashtable)；ZSet 用 dict + skiplist（跳表）**。

#### 2.4.1 SDS（Simple Dynamic String）

- **服务谁**：主要服务 **String value**（也被很多内部模块复用）。
- **核心目标**：比 C 字符串（`\0` 结尾）更适合做数据库字符串：**O(1) 取长度**、可安全存二进制、减少扩容次数。
- **典型字段（抽象）**：
  - `len`：已用长度
  - `alloc`：已分配容量
  - `buf[]`：实际字节数组（末尾通常仍会保留 `\0` 方便与 C API 兼容，但不靠它判断长度）

简图（抽象）：

```text
SDS
┌────────┬────────┬───────────────────────┐
│  len   │ alloc  │ buf[0..alloc-1]       │
└────────┴────────┴───────────────────────┘
                 ↑ len 之前是有效数据
```

- **关键特性（面试点）**：
  - **取长度 O(1)**：避免 `strlen` 每次 O(n) 扫描
  - **二进制安全**：`buf` 可含 `\0`
  - **预分配/惰性释放**：扩容会多分一点，缩短时不一定立刻还内存（减少频繁 realloc）

---

#### 2.4.2 quicklist（List 的主力实现思路）

- **服务谁**：主要服务 **List**。
- **核心目标**：把“链表易于两端操作”和“连续内存更省/更快”结合起来：**分段的紧凑块 + 双向链表**。
- **结构抽象**：
  - 外层：双向链表，每个节点是一个“块”
  - 块内：紧凑连续结构存多个元素（历史上叫 ziplist，后续版本也可能是 listpack；面试只讲“紧凑块”即可）

简图（抽象）：

```text
quicklist (doubly linked list of packed blocks)
head ⇄ [block] ⇄ [block] ⇄ [block] ⇄ tail
          │         │
          └─ packed entries (e1,e2,e3...)
```

- **为什么快/省**：
  - 相比每个元素一个链表节点：减少指针与内存碎片
  - 相比一个超大连续数组：避免一次性扩容与大范围搬迁
- **易错点**：
  - `LRANGE` 一次取很多元素时仍会产生大响应与拷贝开销（并不“因为 quicklist 就不慢”）

---

#### 2.4.3 dict / hashtable（Redis 字典）

- **服务谁**：
  - **Hash**：field -> value（大了以后）
  - **Set**：element -> (dummy)（大了以后）
  - **ZSet**：member -> score（用于快速定位 member）
  - 以及很多内部结构（keyspace、过期字典等）
- **核心目标**：提供平均 \(O(1)\) 的查找/插入/删除。

结构抽象（典型哈希表）：

```text
dict
┌──────────────────────────────┐
│ ht[0] (current table)        │
│ ht[1] (rehash table, optional)│
└──────────────────────────────┘

hashtable (array of buckets)
bucket[i] -> entry -> entry -> ...
```

- **面试必提：渐进式 rehash**
  - 扩容/缩容不会一次性搬完所有 entry（那会卡住主线程）
  - 采用 **增量搬迁**：每次读写顺便迁移一点，直到完成
- **冲突处理**：同一个 bucket 里挂链/结构串起来（抽象理解即可）
- **易错点**：
  - 哈希表不是“永远 O(1)”：在极端冲突/大量 rehash 期间会变慢（但工程上通过扩容与随机化尽量避免）

---

#### 2.4.4 跳表（skiplist）

- **服务谁**：主要服务 **ZSet 的有序能力**（按 score 排序、范围查询、topN）。
- **核心目标**：用多层“索引”把有序链表加速到接近平衡树的查询效率，平均 \(O(\log N)\)。
- **结构抽象**：
  - 每个节点可能有多层 forward 指针（层数随机）
  - 高层用于快速跳跃，底层串起完整有序链

简图（抽象）：

```text
level3:  head ───────────────► n8 ─────► tail
level2:  head ─────► n3 ─────► n8 ─────► tail
level1:  head ─► n1 ─► n3 ─► n6 ─► n8 ─► tail
level0:  head ─► n1 ─► n2 ─► n3 ─► n4 ─► n5 ─► ... ─► tail
```

- **在 ZSet 里怎么配合 dict**：
  - **dict**：member -> score（快速定位/更新）
  - **skiplist**：按 score 排序（快速范围/topN）
- **复杂度口径**：
  - 插入/删除/按 score 查找：平均 \(O(\log N)\)
  - 范围遍历：\(O(\log N + M)\)

---

### 2.5 ZSet 为什么常用于排行榜？复杂度？

- **结论**：ZSet 有序，支持按 score 排序与范围查询。
- **常用命令**：
  - `ZADD`：加入/更新分数
  - `ZRANGE`/`ZREVRANGE`：取 topN
  - `ZRANGEBYSCORE`：按分数区间
- **复杂度（口径）**：插入/更新约 \(O(\log N)\)，取范围 \(O(\log N + M)\)（M 为结果数量）。

---

## 3. 键空间、过期与内存淘汰

### 3.1 过期删除策略是什么？

- **结论**：Redis 采用 **惰性删除 + 定期删除** 的组合。
  - **惰性删除**：访问 key 时发现过期才删
  - **定期删除**：后台定时随机采样删除过期 key
- **易错点**：如果大量 key 过期但很少被访问，依赖定期删除；极端情况下可能带来短时 CPU 波动。

---

### 3.2 内存满了怎么办？淘汰策略有哪些？

- **结论**：配置 `maxmemory` 与 `maxmemory-policy` 决定行为。
- **常见策略（面试重点）**：
  - `noeviction`：不淘汰，写入返回错误（缓存场景通常不选）
  - `allkeys-lru`：所有 key 里按 LRU 近似淘汰（常用）
  - `volatile-lru`：只淘汰设置了 TTL 的 key
  - `allkeys-lfu`：所有 key 里按 LFU 近似淘汰（热点更稳）
  - `volatile-ttl`：优先淘汰剩余 TTL 更小的
- **追问**：LRU/LFU 是精确的吗？  
  - **不是**，是采样近似（性能与精度折中）。

---

## 4. 持久化：RDB 与 AOF

### 4.1 RDB 是什么？优缺点？

- **结论**：RDB 是某一时刻的内存快照（snapshot）。
- **优点**：文件紧凑、恢复快、适合备份/迁移
- **缺点**：可能丢失最后一次快照后的数据；生成快照需要 fork，可能带来短时抖动（与内存页复制相关）

---

### 4.2 AOF 是什么？`appendfsync` 三种策略？

- **结论**：AOF 记录写命令日志，重放恢复。
- **`appendfsync`**：
  - `always`：最安全，最慢
  - `everysec`：性能/安全折中（常用），理论丢 1 秒
  - `no`：交给 OS 刷盘，风险高

---

### 4.3 AOF 重写是什么？为什么需要？

- **结论**：AOF 会膨胀；重写用等价的最小命令集生成新 AOF，减少体积并加速恢复。
- **易错点**：重写不是“读旧 AOF 生成新 AOF”，而是基于当前内存状态生成（核心思想）。

---

### 4.4 线上怎么选 RDB/AOF？

- **面试口径**：
  - 追求恢复快与备份：RDB
  - 追求更少丢数据：AOF（常配 `everysec`）
  - 生产常见：**RDB + AOF 同时开启**（兼顾恢复速度与数据安全）

---

## 5. 复制、哨兵与高可用

### 5.1 主从复制怎么做？有什么风险？

- **结论**：从库复制主库数据用于读扩展与容灾。
- **风险/注意**：
  - **复制延迟**：读到旧数据（读写一致性问题）
  - **全量同步开销**：新从库加入时可能触发全量同步（RDB 传输）
  - **主从切换**：应用端需要感知新主（通常由哨兵/代理/客户端支持）

---

### 5.2 哨兵 Sentinel 解决什么问题？怎么选主？

- **结论**：哨兵负责 **监控、故障发现、自动故障转移、通知**。
- **易错点**：哨兵本身也需要高可用（一般 3 个/5 个），避免脑裂。
- **追问**：怎么避免误判？  
  - 多哨兵投票（客观下线）+ 超时阈值。

---

## 6. 集群：Redis Cluster

### 6.1 Cluster 的核心：槽（hash slot）

- **结论**：Cluster 将 key 映射到 16384 个槽，不同节点负责不同槽，实现水平扩展。
- **追问**：多 key 操作为什么可能报错？  
  - 因为涉及多个 key，如果不在同一槽就无法原子执行。

---

### 6.2 Hash tag 是什么？怎么让多 key 落同一槽？

- **结论**：`{...}` 中的部分参与哈希，称为 hash tag。

示例：

```text
user:{100}:profile
user:{100}:orders
```

上面两个 key 会落在同一槽，便于事务/Lua/多 key 操作。

---

### 6.3 Cluster 的一致性与可用性怎么说？

- **面试口径**：
  - Cluster 偏可用；主从复制存在延迟，故障切换时可能丢少量数据
  - 强一致需要业务层补偿（如写后读走主、版本号、幂等）

---

## 7. 事务、Lua、Pipeline 与一致性

### 7.1 Redis 事务（MULTI/EXEC）保证什么？不保证什么？

- **结论**：
  - 保证：事务内命令 **按顺序串行执行**
  - 不保证：没有传统意义的隔离级别；单条命令失败通常不会回滚（语义不同）
- **易错点**：`WATCH` 才能做乐观锁（CAS）。

---

### 7.2 Lua 脚本的价值是什么？

- **结论**：把多步读写合并为一次原子执行，减少往返与竞态。
- **常见场景**：扣库存、秒杀、限流、发号器、锁的释放校验。

---

### 7.3 Pipeline 是什么？适用什么？

- **结论**：pipeline 批量发送命令减少 RTT，提高吞吐；不保证原子。
- **易错点**：pipeline 会让服务端在短时间处理大量命令，注意响应缓冲与客户端内存。

---

## 8. 缓存设计：穿透/击穿/雪崩

### 8.1 缓存穿透是什么？怎么解决？

- **结论**：请求的 key 在缓存与 DB 都不存在，导致每次都打到 DB。
- **常见方案**：
  - 缓存空值（设置短 TTL）
  - 布隆过滤器（Bloom Filter）先挡一层
  - 参数校验、风控限流

---

### 8.2 缓存击穿是什么？怎么解决？

- **结论**：某个热点 key 过期瞬间大量并发打到 DB。
- **常见方案**：
  - 互斥锁/单飞（只有一个线程回源）
  - 逻辑过期（缓存永不过期，但带版本/时间戳，异步刷新）
  - 热点 key 提前续期

---

### 8.3 缓存雪崩是什么？怎么解决？

- **结论**：大量 key 同时过期或 Redis 故障，导致 DB 被冲垮。
- **常见方案**：
  - TTL 加随机抖动
  - 多级缓存、本地缓存兜底
  - 降级熔断、限流
  - Redis 高可用（哨兵/集群）与隔离

---

## 9. 分布式锁与限流

### 9.1 用 Redis 实现分布式锁怎么做？关键点是什么？

- **结论**：用 `SET key value NX PX ttl` 获取锁；释放锁必须校验 value，避免误删他人锁（Lua 原子释放）。

获取锁（示例语义）：

```text
SET lock:order:123 <uuid> NX PX 30000
```

释放锁（Lua 伪代码语义）：

```text
if GET(key) == value then DEL(key) end
```

- **易错点（高频扣分）**：
  - 只用 `SETNX` 不加过期：可能死锁
  - 释放锁不校验 value：可能删掉别人的锁
  - TTL 过短：业务未执行完锁就过期，出现并发执行（需续期/看门狗或更大 TTL + 幂等）

---

### 9.2 RedLock 要不要用？

- **面试口径**：
  - 争议点：网络分区、时钟漂移、实现复杂度；是否满足你业务对一致性的要求
  - 多数业务：单 Redis + 主从 + 合理 TTL + 续期 + 幂等 已足够
  - 强一致锁：更倾向用 etcd/zk 这类 CP 系统（取决于一致性要求）

---

### 9.3 Redis 限流常见做法？

- **计数器**：固定窗口（简单但边界抖动）
- **滑动窗口**：ZSet 记录时间戳（精确但开销更大）
- **令牌桶/漏桶**：Lua 脚本原子更新（更贴近工程）

---

## 10. 热点与大 Key、性能调优与排障

### 10.1 什么是大 Key？为什么危险？

- **结论**：value 过大或集合元素过多的 key。
- **风险**：
  - 慢命令阻塞（单线程事件循环）
  - 网络传输耗时、客户端/服务端缓冲暴涨
  - 复制与持久化压力加大

---

### 10.2 热点 Key 怎么处理？

- **常见方案**：
  - 本地缓存（Caffeine/Guava）分担
  - key 拆分（按用户/分片）
  - 读写分离与只读副本
  - 业务层限流/降级

---

### 10.3 如何排查 Redis 变慢？

- **思路（面试回答模板）**：
  - 看慢日志：`SLOWLOG GET`
  - 看 CPU/内存/网络：是否出现大 key、热点、swap、网卡瓶颈
  - 看阻塞命令：例如全量扫描、一次性返回大量元素
  - 看持久化/复制：RDB/AOF rewrite、全量同步是否在进行
  - 看 key 分布：是否某些 key 过大或过热

> 注意：不要在生产随意用 `KEYS *`。优先 `SCAN` 分批。

---

### 10.4 `KEYS` 与 `SCAN` 区别？

| 命令 | 特性 | 风险 |
| --- | --- | --- |
| `KEYS pattern` | 一次性返回所有匹配 | 可能阻塞 |
| `SCAN cursor [MATCH] [COUNT]` | 分批迭代 | 不保证一次就返回全部，需循环 |

---

## 11. 速记对比表（面试最后 1 分钟）

### 11.1 持久化对比

| 维度 | RDB | AOF |
| --- | --- | --- |
| 形式 | 快照 | 命令日志 |
| 恢复速度 | 快 | 较慢（需重放） |
| 数据丢失 | 可能丢最近快照后的数据 | `everysec` 理论丢 1 秒 |
| 文件体积 | 小 | 可能膨胀（需重写） |

---

### 11.2 可用性方案对比

| 方案 | 解决的问题 | 代价/注意 |
| --- | --- | --- |
| 主从 | 读扩展/容灾 | 复制延迟，一致性要处理 |
| 哨兵 | 自动故障转移 | 哨兵也要高可用，注意脑裂 |
| Cluster | 水平扩展 | 多 key 受槽限制，一致性偏弱 |

---

## 12. Spring/Java 常用落地片段

> 目标：给出“能直接粘贴跑”的模板，并标注最常见坑（超时、续期、序列化、线程中断、误删锁等）。

### 12.1 Redisson 分布式锁（推荐工程落地）

#### 12.1.1 Maven 依赖（示例）

```xml
<dependency>
  <groupId>org.redisson</groupId>
  <artifactId>redisson-spring-boot-starter</artifactId>
  <version>3.24.3</version>
</dependency>
```

#### 12.1.2 基本用法：tryLock + finally 解锁

```java
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.TimeUnit;

public class LockDemo {
    private final RedissonClient redissonClient;

    public LockDemo(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    public void doWorkWithLock(String orderId) throws InterruptedException {
        String lockKey = "lock:order:" + orderId;
        RLock lock = redissonClient.getLock(lockKey);

        boolean locked = lock.tryLock(200, 30_000, TimeUnit.MILLISECONDS);
        if (!locked) {
            throw new IllegalStateException("获取锁失败，请稍后重试");
        }
        try {
            // 业务逻辑（需要保证幂等）
        } finally {
            // 只解自己持有的锁
            if (lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}
```

- **关键参数解释**：
  - `waitTime`：最多等多久拿锁（避免无限等待）
  - `leaseTime`：锁的自动释放时间（避免死锁）
- **易错点**：
  - `leaseTime` 太短：业务没做完锁就过期，出现并发执行（要么增大 leaseTime，要么开启看门狗/续期策略，并做好幂等）
  - 忽略 `InterruptedException`：建议向上抛或恢复中断状态（`Thread.currentThread().interrupt()`）

---

### 12.2 RedisTemplate + Lua：原子“校验并删除锁”

> 适用：不用 Redisson，自己实现 `SET NX PX` 锁时的“安全解锁”。

#### 12.2.1 Lua 脚本（unlock.lua 语义）

```lua
if redis.call('GET', KEYS[1]) == ARGV[1] then
  return redis.call('DEL', KEYS[1])
else
  return 0
end
```

#### 12.2.2 Java 执行 Lua（Spring Data Redis）

```java
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collections;

public class LuaUnlockDemo {
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT;

    static {
        UNLOCK_SCRIPT = new DefaultRedisScript<>();
        UNLOCK_SCRIPT.setResultType(Long.class);
        UNLOCK_SCRIPT.setScriptText(
            "if redis.call('GET', KEYS[1]) == ARGV[1] then " +
            "  return redis.call('DEL', KEYS[1]) " +
            "else " +
            "  return 0 " +
            "end"
        );
    }

    private final StringRedisTemplate stringRedisTemplate;

    public LuaUnlockDemo(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public boolean unlock(String lockKey, String token) {
        Long res = stringRedisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), token);
        return res != null && res == 1L;
    }
}
```

- **易错点**：
  - 不校验 token 直接 `DEL`：会误删别人的锁（典型事故）
  - 释放锁与校验分两条命令：会产生竞态（必须 Lua 原子）

---

### 12.3 Spring Cache（@Cacheable）缓存模板：防穿透/空值缓存

#### 12.3.1 常见写法：`unless` 避免缓存 null（按业务选择）

```java
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Cacheable(cacheNames = "user", key = "#userId", unless = "#result == null")
    public UserDTO getUserById(Long userId) {
        // 这里查询 DB
        return null;
    }
}
```

- **面试点**：
  - `unless = "#result == null"`：不缓存 null，简单但可能导致“穿透”（不存在的用户每次打 DB）
  - 另一种策略：**缓存空值但设置短 TTL**（更能防穿透，需要你在缓存层实现 TTL/空值标记）

---

### 12.4 缓存击穿：互斥/单飞（Redis SETNX 版本）

> 目标：热点 key 过期时只有一个线程回源，其它线程等待或直接返回旧值。

```java
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;
import java.util.UUID;

public class SingleFlightDemo {
    private final StringRedisTemplate redis;

    public SingleFlightDemo(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String getWithMutex(String dataKey, String lockKey) {
        String cached = redis.opsForValue().get(dataKey);
        if (cached != null) return cached;

        String token = UUID.randomUUID().toString();
        Boolean ok = redis.opsForValue().setIfAbsent(lockKey, token, Duration.ofSeconds(5));
        if (Boolean.TRUE.equals(ok)) {
            try {
                // 回源 DB
                String value = "dbValue";
                // 写缓存（建议 TTL + 抖动）
                redis.opsForValue().set(dataKey, value, Duration.ofMinutes(10));
                return value;
            } finally {
                // 安全解锁（建议用 12.2 的 Lua）
                String cur = redis.opsForValue().get(lockKey);
                if (token.equals(cur)) {
                    redis.delete(lockKey);
                }
            }
        }

        // 没拿到锁：短暂 sleep 后重试（注意不要长时间阻塞线程池）
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return redis.opsForValue().get(dataKey);
    }
}
```

- **易错点**：
  - 重试策略别太激进：会放大流量（建议指数退避/限重试次数/直接降级）
  - `sleep` 会占用线程：高并发场景更建议“逻辑过期 + 异步刷新”

---

### 12.5 典型配置：Spring Boot + Redis（Lettuce）

> 不同项目依赖可能是 lettuce 或 jedis，下面给出常见的 `application.yml` 示例（按需调整）。

```yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: ""
      database: 0
      timeout: 2s
      lettuce:
        pool:
          max-active: 16
          max-idle: 16
          min-idle: 2
          max-wait: 2s
```

- **易错点**：
  - 序列化：`RedisTemplate` 默认 JDK 序列化（可读性差、跨语言差）；缓存对象常用 JSON（Jackson）或 String
  - 连接池：`max-active` 太小会排队；太大可能压垮 Redis（结合 QPS 与响应时间估算）

---

### 12.6 RedisTemplate 序列化（最常见坑位）

#### 12.6.1 推荐：StringRedisTemplate / JSON 序列化

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisTemplateConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory, ObjectMapper objectMapper) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(factory);

        StringRedisSerializer string = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer json = new GenericJackson2JsonRedisSerializer(objectMapper);

        template.setKeySerializer(string);
        template.setHashKeySerializer(string);
        template.setValueSerializer(json);
        template.setHashValueSerializer(json);
        template.afterPropertiesSet();
        return template;
    }
}
```

- **易错点**：
  - JSON 带类型信息与否：涉及反序列化安全与兼容性（统一规范很重要）
  - 对缓存对象做版本演进：字段新增/删除要保证兼容（如默认值、容错解析）
