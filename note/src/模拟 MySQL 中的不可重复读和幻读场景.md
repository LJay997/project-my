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