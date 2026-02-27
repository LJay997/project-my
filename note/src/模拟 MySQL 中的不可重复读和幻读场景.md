我已经为你创建了一个完整的 SQL 脚本文件 `mysql_simulation.sql`，用于模拟 MySQL 中的不可重复读和幻读场景。这个脚本包含了详细的步骤和注释，你可以直接在 MySQL 环境中执行来观察不同隔离级别的效果。

### 脚本内容概览

1. **初始化测试环境**
    - 创建数据库和表
    - 插入测试数据

2. **查看当前事务隔离级别**
    - 使用 `SELECT @@transaction_isolation;` 查看默认设置

3. **模拟不可重复读场景**
    - 会话 A 开启事务并读取数据
    - 会话 B 在中间修改数据并提交
    - 会话 A 再次读取发现数据不一致

4. **模拟幻读场景**
    - 会话 A 查询某个范围的数据
    - 会话 B 插入新数据并提交
    - 会话 A 再次查询发现多了一条记录

5. **设置隔离级别为 REPEATABLE READ**
    - 重新测试不可重复读和幻读场景，观察问题是否解决

6. **设置隔离级别为 SERIALIZABLE**
    - 测试最高隔离级别下的幻读防护机制

### 使用方法

1. 下载生成的 `mysql_simulation.sql` 文件
2. 在你的 MySQL 环境中执行脚本中的 SQL 语句
3. 按照注释中的说明，在两个不同的会话中分别执行对应的操作
4. 观察不同隔离级别下事务行为的差异

这个实验可以帮助你深入理解 MySQL 事务隔离级别的实际效果和工作机制。


MySQL 的 REPEATABLE READ 隔离级别下是否存在幻读？

在 MySQL 的 InnoDB 存储引擎 中，REPEATABLE READ（可重复读）隔离级别通常不会出现幻读（Phantom Read）。这是 MySQL 对 SQL 标准隔离级别的一种增强实现。

什么是幻读？

幻读（Phantom Read） 是指：

在同一个事务中，两次执行相同的查询，但第二次查询返回了第一次查询时不存在的“新行”（即其他事务在此期间插入并提交了满足条件的新数据）。

示例：
-- 事务 A
SELECT * FROM users WHERE age > 20; -- 返回 10 行

-- 此时事务 B 插入一条 age = 25 的记录并提交

-- 事务 A 再次执行
SELECT * FROM users WHERE age > 20; -- 返回 11 行（多出一行“幻影”）

SQL 标准 vs MySQL 实现
隔离级别           SQL 标准是否允许幻读   MySQL (InnoDB) 是否允许幻读
READ UNCOMMITTED   ✅ 允许                ✅ 允许

READ COMMITTED     ✅ 允许                ✅ 允许

REPEATABLE READ    ✅ 允许（标准）        ❌ 不允许（通过 MVCC + 间隙锁）

SERIALIZABLE       ❌ 不允许              ❌ 不允许

⚠️ 注意：SQL 标准规定 REPEATABLE READ 允许幻读，但 MySQL 的 InnoDB 引擎通过机制优化，在该级别下避免了幻读。

# MySQL 如何避免幻读？

1. MVCC（多版本并发控制）
- 在 REPEATABLE READ 下，事务启动时会创建一个 一致性读视图（consistent read view）。
- 所有普通 SELECT（快照读）都基于该视图，看不到其他事务之后插入或修改的数据。

2. 间隙锁（Gap Lock） + Next-Key Lock
- 对于 当前读（current read） 操作（如 SELECT ... FOR UPDATE、UPDATE、DELETE），InnoDB 使用 Next-Key Lock（记录锁 + 间隙锁）。
- 这能阻止其他事务在查询范围的“间隙”中插入新记录，从而防止幻读。

例外情况（看似幻读）

虽然 InnoDB 基本消除了幻读，但在以下场景可能观察到类似现象：

混合使用快照读与当前读
-- 事务 A
START TRANSACTION;
SELECT * FROM t WHERE id > 10;               -- 快照读，基于一致性视图
SELECT * FROM t WHERE id > 10 FOR UPDATE;    -- 当前读，可能看到其他事务已提交的新行

如果中间有其他事务插入并提交了 id > 10 的记录，第二个查询会看到新行。这不是真正的幻读，而是当前读的语义决定的。

无索引导致全表扫描
- 若 WHERE 条件无法使用索引，InnoDB 可能对整个表加间隙锁。
- 虽仍能防幻读，但锁范围过大，性能差；极端情况下行为可能不符合预期（但不会破坏一致性）。

结论

✅ 在 MySQL InnoDB 的 REPEATABLE READ 隔离级别下，通过 MVCC + 间隙锁机制，已经有效解决了幻读问题。

因此，MySQL 的 REPEATABLE READ 比 SQL 标准更严格，在防止幻读方面行为接近 SERIALIZABLE，但保持了更高的并发性能。

如需绝对串行化，可使用 SERIALIZABLE，但通常 REPEATABLE READ 已足够且高效。

## ReadView 与 Undo Log 实现可重复读的原理

InnoDB 通过 ‌Undo Log 版本链‌ 保存数据历史版本，结合 ‌ReadView‌ 判断版本可见性，确保事务在整个过程中读取到一致的快照数据，实现可重复读隔离级别。

1. Undo Log 版本链：数据历史版本的存储
   版本链生成‌：事务执行 INSERT/UPDATE/DELETE 时，InnoDB 会将修改前的记录写入 Undo Log，并通过隐藏列 DB_TRX_ID（修改事务 ID）和 DB_ROLL_PTR（回滚指针）串联成版本链。例如，初始数据版本 V1 被事务 T1（trx_id=100）修改后生成 V2，V2 的 roll_ptr 指向 V1，后续修改会继续追加版本链。
   作用‌：为快照读提供历史数据，事务可通过 roll_ptr 回溯到任意历史版本，避免直接覆盖数据导致的读冲突。
2. ReadView：版本可见性的判断规则

事务首次执行快照读（如普通 SELECT）时生成 ReadView，包含以下核心字段：

m_ids：当前活跃事务 ID 列表（未提交事务）；
min_trx_id：活跃事务中的最小 ID；
max_trx_id：下一个待分配的事务 ID（当前最大事务 ID + 1）；
creator_trx_id：生成 ReadView 的事务自身 ID。

可见性判断逻辑‌：读取数据时，通过对比数据版本的 trx_id 与 ReadView 字段决定是否可见：

trx_id < min_trx_id：版本由已提交事务生成，可见；
trx_id > max_trx_id：版本由 ReadView 生成后启动的事务创建，不可见；
min_trx_id ≤ trx_id ≤ max_trx_id：若 trx_id 在 m_ids 中（事务未提交），不可见；否则可见。
3. 可重复读的实现逻辑
   快照生成‌：事务首次快照读时生成 ReadView，记录当前活跃事务状态。
   复用快照‌：可重复读隔离级别下，事务后续快照读‌复用首次生成的 ReadView‌，确保多次读取基于同一快照，避免数据变化影响结果。
   trx_id > max_trx_id 场景解释

max_trx_id 是 ReadView 生成时系统“下一个待分配的事务 ID”（即当前最大事务 ID + 1）。当数据版本的 trx_id > max_trx_id 时，意味着该版本由 ‌ReadView 生成后才启动的事务‌ 创建，对当前事务不可见。

示例
事务 T3‌（trx_id=150）生成 ReadView 时，max_trx_id=201（此时系统最大事务 ID 为 200）。
新事务 T4‌ 启动，分配 trx_id=201，修改数据生成新版本（trx_id=201）。
T3 读取时‌，该版本 trx_id=201 > max_trx_id=201（实际为 ≥），判定为“ReadView 生成后启动的事务创建”，不可见，T3 继续回溯读取历史版本。

本质‌：该规则确保事务只能看到快照生成前已存在的事务修改，屏蔽后续新事务的影响，从而保证快照读的一致性。

## MySQL 中 Redo Log、Binlog、Undo Log 的作用及设计原因
1. ‌Redo Log（重做日志）‌
   ‌作用‌：确保事务的持久性（ACID 中的 Durability），记录数据页的物理修改（如“某数据页某偏移量的值修改为 X”），用于崩溃恢复。当数据库意外宕机后，重启时可通过 Redo Log 重放未刷盘的修改，保证数据不丢失‌
   5
   。
   ‌设计原因‌：直接将修改同步到磁盘文件（如 InnoDB 数据文件）是随机 I/O，效率低。Redo Log 通过顺序 I/O 记录修改，先写入内存缓冲区（Redo Log Buffer），再定期刷盘，大幅提升写入性能；同时，其“预写日志”（WAL）机制确保事务提交前日志先落盘，避免数据丢失。
2. ‌Binlog（二进制日志）‌
   ‌作用‌：记录所有对数据库的写操作（如 INSERT/UPDATE/DELETE、表结构变更等），以“事务”形式存储，用于主从复制和数据恢复‌
   1
   2
   。主库通过 Binlog 将变更同步到从库，实现数据一致性；通过回放 Binlog 可恢复指定时间点的数据。
   ‌设计原因‌：
   ‌与存储引擎解耦‌：Binlog 由 MySQL 服务器层实现，独立于 InnoDB 等存储引擎，支持跨引擎的数据同步。
   ‌逻辑日志特性‌：记录操作逻辑（如“给 id=1 的行的 age 字段加 1”），而非物理地址，便于数据恢复和跨版本兼容。
   ‌主从复制核心‌：通过 Binlog 实现主从数据同步，支撑分布式架构中的读写分离和高可用。
3. ‌Undo Log（回滚日志）‌
   ‌作用‌：记录数据修改前的逻辑状态（如“INSERT 对应 DELETE 日志，UPDATE 对应反向 UPDATE 日志”），用于事务回滚和多版本并发控制（MVCC）‌
   3
   4
   。事务回滚时，通过 Undo Log 撤销已执行的修改；快照读时，通过 Undo Log 版本链读取历史数据，实现可重复读隔离级别。
   ‌设计原因‌：
   ‌事务原子性保障‌：支持事务回滚，确保未提交事务的修改不影响数据库状态。
   ‌MVCC 实现基础‌：通过维护数据的多版本历史，允许读写不阻塞，提升并发性能。
   
**三者协同设计的核心目标**

   ‌性能与可靠性平衡‌：Redo Log 解决随机 I/O 效率问题，Binlog 满足跨引擎和分布式需求，Undo Log 支撑事务回滚和并发控制，共同保障 MySQL 在高并发场景下的 ACID 特性。
   ‌分层职责明确‌：Redo Log 关注物理页恢复，Binlog 关注逻辑操作记录，Undo Log 关注事务回滚与版本管理，三者分工互补，构成 MySQL 数据一致性的核心保障体系。