# MySQL 索引优化与执行计划面试指南

## 一、索引基础概念

### 1. 什么是索引？为什么需要索引？

索引是数据库中用于加速查询的数据结构，类似于书籍的目录。

**作用：**
- 加速数据检索，减少磁盘 I/O
- 优化排序和分组操作
- 强制实施唯一性约束

**代价：**
- 占用额外存储空间
- 降低写入性能（INSERT/UPDATE/DELETE 需要维护索引）

---

### 2. MySQL 支持哪些索引类型？

| 索引类型 | 存储结构 | 特点 | 适用场景 |
|---------|---------|------|---------|
| B-Tree 索引 | B+ Tree | 最常用，支持范围查询 | 等值查询、范围查询、排序 |
| Hash 索引 | Hash 表 | 查询速度快，不支持范围查询 | 等值查询（Memory 引擎） |
| Full-Text 索引 | 倒排索引 | 支持全文搜索 | 文本搜索场景 |
| R-Tree 索引 | R Tree | 支持空间数据 | GIS 地理数据查询 |

---

### 3. 聚簇索引和非聚簇索引的区别？

| 特性 | 聚簇索引 | 非聚簇索引 |
|------|---------|-----------|
| 数据存储 | 索引即数据，叶子节点存储完整行数据 | 叶子节点存储主键值 |
| 数量限制 | 每个表只能有一个 | 可以有多个 |
| 默认情况 | InnoDB 主键默认是聚簇索引 | 普通索引都是非聚簇索引 |
| 查询效率 | 主键查询最快 | 需要回表查询 |

---

## 二、索引设计原则

### 4. 如何选择合适的索引列？

1. 高频查询条件：WHERE、JOIN、ORDER BY 中的列
2. 区分度高的列：基数大的列适合做索引（如身份证号）
3. 前缀索引：对于长字符串，使用前缀索引减少索引大小
4. 复合索引顺序：遵循最左前缀原则

---

### 5. 什么是最左前缀原则？举例说明。

复合索引 `idx_name_age (name, age)` 能匹配的查询：

```sql
-- 可以使用索引
SELECT * FROM users WHERE name = '张三';
SELECT * FROM users WHERE name = '张三' AND age = 25;
SELECT * FROM users WHERE name = '张三' AND age > 18;

-- 无法使用索引（跳过了 name）
SELECT * FROM users WHERE age = 25;
```

---

### 6. 什么情况下索引会失效？

| 场景 | 示例 | 原因 |
|------|------|------|
| 列上使用函数 | WHERE YEAR(create_time) = 2024 | 函数操作破坏了索引有序性 |
| 类型转换 | WHERE id = '123' | 隐式类型转换 |
| LIKE 左模糊 | WHERE name LIKE '%张三' | 无法利用索引有序性 |
| OR 条件 | WHERE name = 'A' OR age = 18 | OR 两边列都需要有索引 |

---

## 三、执行计划分析

### 7. 如何查看 MySQL 执行计划？

使用 `EXPLAIN` 或 `EXPLAIN ANALYZE` 命令：

```sql
EXPLAIN SELECT * FROM users WHERE name = '张三';
EXPLAIN ANALYZE SELECT * FROM orders WHERE status = 1;
```

---

### 8. EXPLAIN 输出中各字段的含义？

| 字段 | 含义 | 重要性 |
|------|------|--------|
| id | 查询序列号 | 识别子查询层级 |
| select_type | 查询类型 | SIMPLE/PRIMARY/SUBQUERY/DERIVED |
| table | 表名 | 执行的表 |
| type | 访问类型 | 最重要指标 |
| possible_keys | 可能使用的索引 | 参考 |
| key | 实际使用的索引 | 确认是否命中索引 |
| rows | 估算扫描行数 | 评估查询效率 |
| Extra | 额外信息 | 重要优化线索 |

---

### 9. type 字段的取值有哪些？性能如何排序？

从优到劣排序：
```
system > const > eq_ref > ref > range > index > ALL
```

| type | 说明 | 性能 |
|------|------|------|
| system | 表只有一行数据（系统表） | 最优 |
| const | 常量查询，最多匹配一行 | 优秀 |
| eq_ref | 主键或唯一索引等值查询 | 优秀 |
| ref | 非唯一索引等值查询 | 良好 |
| range | 索引范围扫描 | 一般 |
| ALL | 全表扫描（Full Table Scan） | 最差 |

---

### 10. Extra 字段常见值及含义？

| Extra 值 | 含义 | 优化建议 |
|----------|------|---------|
| Using index | 使用覆盖索引，无需回表 | 最优 |
| Using filesort | 外部排序，非索引排序 | 需要优化 |
| Using temporary | 使用临时表 | 需要优化 |

---

## 四、索引优化实战

### 11. 如何优化以下 SQL 查询？

```sql
SELECT * FROM orders 
WHERE status = 1 
  AND create_time >= '2024-01-01'
ORDER BY total_amount DESC
LIMIT 10;
```

**优化方案：**

1. 创建复合索引：
```sql
CREATE INDEX idx_status_create_time_total 
ON orders (status, create_time, total_amount DESC);
```

---

### 12. 如何处理 LIKE '%xxx%' 模糊查询的性能问题？

**方案一：使用全文索引**
```sql
ALTER TABLE articles ADD FULLTEXT INDEX idx_content (content);
SELECT * FROM articles WHERE MATCH(content) AGAINST('keyword');
```

**方案二：使用外部搜索引擎（Elasticsearch/Solr）**

---

### 13. 索引太多会有什么问题？

1. 写入性能下降：INSERT/UPDATE/DELETE 需要维护所有相关索引
2. 存储空间占用：每个索引都会占用额外空间
3. 查询优化器选择困难：太多索引会增加优化器的决策时间

---

## 五、进阶问题

### 14. 什么是索引下推 (ICP)？

索引下推是 MySQL 5.6+ 引入的优化技术。在索引遍历过程中，将 WHERE 条件下推到存储引擎层进行过滤，减少回表次数。

---

### 15. 如何判断索引是否被使用？

方法一：使用 `EXPLAIN` 查看 `key` 字段

方法二：使用 `sys.schema_unused_indexes` 查看未使用的索引

---

## 六、实战案例分析

**场景：** 以下 SQL 执行很慢，请分析并优化。

```sql
SELECT u.name, o.order_no, o.total_amount
FROM users u
JOIN orders o ON u.id = o.user_id
WHERE u.status = 1 
  AND o.create_time >= '2024-01-01'
ORDER BY o.total_amount DESC
LIMIT 100;
```

**优化方案：**

```sql
-- 在 users 表创建索引
CREATE INDEX idx_users_status_id ON users (status, id);

-- 在 orders 表创建索引
CREATE INDEX idx_orders_user_id_create_time_total 
ON orders (user_id, create_time, total_amount DESC);
```

---

## 七、总结

### 索引优化核心要点

1. 选择合适的索引列：高频查询条件、高基数列
2. 遵循最左前缀原则：复合索引顺序很重要
3. 避免索引失效场景：函数操作、类型转换、左模糊 LIKE
4. 使用覆盖索引：减少回表操作
5. 分析执行计划：通过 EXPLAIN 定位性能瓶颈
6. 定期维护索引：删除无用索引，监控索引使用情况