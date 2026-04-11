# SQL 面试题精选

## 1. 部门最高工资员工

### 题目描述

找出每个部门中工资最高的员工。如果有多个员工工资相同且都是最高，都要返回。

### 表结构

```sql
-- 部门表 (Department)
CREATE TABLE Department (
    Id   INT PRIMARY KEY,
    Name VARCHAR(50) NOT NULL
);

-- 员工表 (Employee)
CREATE TABLE Employee (
    Id           INT PRIMARY KEY,
    Name         VARCHAR(50) NOT NULL,
    Salary       INT,
    DepartmentId INT,
    FOREIGN KEY (DepartmentId) REFERENCES Department(Id)
);
```

### 测试数据

```sql
-- 插入部门数据
INSERT INTO Department (Id, Name) VALUES
(1, 'IT'),
(2, 'Sales');

-- 插入员工数据
INSERT INTO Employee (Id, Name, Salary, DepartmentId) VALUES
(1, 'Joe', 70000, 1),
(2, 'Jim', 90000, 1),
(3, 'Henry', 80000, 2),
(4, 'Sam', 60000, 2),
(5, 'Max', 90000, 1);
```

**预期结果：**

| Department | Employee | Salary |
|-----------|----------|--------|
| IT        | Jim      | 90000  |
| IT        | Max      | 90000  |
| Sales     | Henry    | 80000  |

---

### 解法1：子查询 + JOIN（推荐）⭐

```sql
SELECT 
    d.Name AS Department,
    e.Name AS Employee,
    e.Salary
FROM Employee e
JOIN Department d ON e.DepartmentId = d.Id
INNER JOIN (
    -- 子查询：找出每个部门的最高工资
    SELECT 
        DepartmentId, 
        MAX(Salary) AS MaxSalary
    FROM Employee
    GROUP BY DepartmentId
) AS t ON e.DepartmentId = t.DepartmentId 
       AND e.Salary = t.MaxSalary;
```

**执行逻辑：**
1. 子查询先计算每个部门的最高工资
2. 将原表与子查询结果 JOIN
3. 筛选出工资等于最高工资的员工

**优点：**
- ✅ 性能好，子查询只执行一次
- ✅ 易于理解和维护
- ✅ 支持多个最高工资员工

---

### 解法2：窗口函数（MySQL 8.0+）⭐⭐

```sql
SELECT 
    Department,
    Employee,
    Salary
FROM (
    SELECT 
        d.Name AS Department,
        e.Name AS Employee,
        e.Salary,
        DENSE_RANK() OVER (
            PARTITION BY e.DepartmentId 
            ORDER BY e.Salary DESC
        ) AS rk
    FROM Employee e
    JOIN Department d ON e.DepartmentId = d.Id
) AS ranked
WHERE rk = 1;
```

**窗口函数对比：**

| 函数 | 说明 | 示例数据排名 |
|------|------|------------|
| `ROW_NUMBER()` | 唯一排名，不重复 | 1, 2, 3, 4 |
| `RANK()` | 并列排名，有间隔 | 1, 1, 3, 4 |
| `DENSE_RANK()` | 并列排名，无间隔 | 1, 1, 2, 3 ⭐ |

**为什么用 `DENSE_RANK`？**
- 如果两人工资并列第一，都能被选中
- `ROW_NUMBER()` 只会选一个
- `RANK()` 也可以，但 `DENSE_RANK` 更直观

---

### 解法3：相关子查询

```sql
SELECT 
    d.Name AS Department,
    e.Name AS Employee,
    e.Salary
FROM Employee e
JOIN Department d ON e.DepartmentId = d.Id
WHERE e.Salary = (
    SELECT MAX(e2.Salary)
    FROM Employee e2
    WHERE e2.DepartmentId = e.DepartmentId
);
```

**执行逻辑：**
- 对每一行员工记录，执行一次子查询
- 子查询计算该员工所在部门的最高工资
- 如果当前员工工资等于最高工资，则返回

**缺点：**
- ❌ 性能较差，子查询执行 N 次（N为员工数）
- ✅ 代码简洁，易于理解

---

### 解法4：元组比较（MySQL 8.0+）

```sql
SELECT 
    d.Name AS Department,
    e.Name AS Employee,
    e.Salary
FROM Employee e
JOIN Department d ON e.DepartmentId = d.Id
WHERE (e.DepartmentId, e.Salary) IN (
    SELECT DepartmentId, MAX(Salary)
    FROM Employee
    GROUP BY DepartmentId
);
```

**特点：**
- ✅ 语法简洁
- ✅ 性能较好
- ⚠️ 需要 MySQL 8.0+ 支持

---

### 性能对比

| 解法 | 时间复杂度 | 适用场景 | 推荐度 |
|------|----------|---------|--------|
| 子查询 + JOIN | O(n) | 通用 | ⭐⭐⭐⭐⭐ |
| 窗口函数 | O(n log n) | MySQL 8.0+ | ⭐⭐⭐⭐⭐ |
| 相关子查询 | O(n²) | 小数据量 | ⭐⭐⭐ |
| 元组比较 | O(n) | MySQL 8.0+ | ⭐⭐⭐⭐ |

---

## 2. 第N高的薪水

### 题目描述

编写一个 SQL 查询，获取 `Employee` 表中第 N 高的薪水（Salary）。如果不存在第 N 高的薪水，则返回 `null`。

### 解法1：LIMIT + OFFSET

```sql
CREATE FUNCTION getNthHighestSalary(N INT) RETURNS INT
BEGIN
    SET N = N - 1;  -- LIMIT 偏移量从0开始
    RETURN (
        SELECT DISTINCT Salary
        FROM Employee
        ORDER BY Salary DESC
        LIMIT 1 OFFSET N
    );
END
```

**注意：**
- 使用 `DISTINCT` 去重，避免相同薪水的干扰
- `OFFSET N` 表示跳过前 N 条

---

### 解法2：窗口函数

```sql
SELECT DISTINCT Salary
FROM (
    SELECT 
        Salary,
        DENSE_RANK() OVER (ORDER BY Salary DESC) AS rk
    FROM Employee
) AS ranked
WHERE rk = N;
```

**优势：**
- ✅ 不需要修改变量
- ✅ 逻辑清晰
- ✅ 性能好

---

### 解法3：子查询计数

```sql
SELECT DISTINCT e1.Salary
FROM Employee e1
WHERE (
    SELECT COUNT(DISTINCT e2.Salary)
    FROM Employee e2
    WHERE e2.Salary > e1.Salary
) = N - 1;
```

**执行逻辑：**
- 对于每个薪水，统计有多少个不同的薪水比它高
- 如果有 N-1 个薪水比它高，那它就是第 N 高

---

## 3. 连续出现的数字

### 题目描述

找出所有至少连续出现三次的数字。

**Logs 表：**

| Id | Num |
|----|-----|
| 1  | 1   |
| 2  | 1   |
| 3  | 1   |
| 4  | 2   |
| 5  | 1   |
| 6  | 2   |
| 7  | 2   |

**预期结果：**

| ConsecutiveNums |
|----------------|
| 1              |

---

### 解法1：自连接（经典）

```sql
SELECT DISTINCT l1.Num AS ConsecutiveNums
FROM Logs l1
JOIN Logs l2 ON l1.Id = l2.Id - 1
JOIN Logs l3 ON l1.Id = l3.Id - 2
WHERE l1.Num = l2.Num 
  AND l2.Num = l3.Num;
```

**思路：**
- 将表自连接3次
- 确保 Id 连续（l1.Id, l1.Id+1, l1.Id+2）
- 确保 Num 相同

---

### 解法2：窗口函数 LEAD/LAG

```sql
SELECT DISTINCT Num AS ConsecutiveNums
FROM (
    SELECT 
        Num,
        LEAD(Num, 1) OVER (ORDER BY Id) AS next1,
        LEAD(Num, 2) OVER (ORDER BY Id) AS next2
    FROM Logs
) AS t
WHERE Num = next1 AND next1 = next2;
```

**LEAD/LAG 函数：**
- `LEAD(Num, 1)` - 获取下一行的 Num
- `LAG(Num, 1)` - 获取上一行的 Num

---

### 解法3：ROW_NUMBER 差值法

```sql
SELECT DISTINCT Num AS ConsecutiveNums
FROM (
    SELECT 
        Num,
        ROW_NUMBER() OVER (ORDER BY Id) - 
        ROW_NUMBER() OVER (PARTITION BY Num ORDER BY Id) AS grp
    FROM Logs
) AS t
GROUP BY Num, grp
HAVING COUNT(*) >= 3;
```

**核心思想：**
- 如果数字连续出现，它们的 `ROW_NUMBER` 差值是固定的
- 按差值分组，统计每组的数量

---

## 4. 超过经理收入的员工

### 题目描述

找出收入比经理高的员工。

**Employee 表：**

| Id | Name  | Salary | ManagerId |
|----|-------|--------|-----------|
| 1  | Joe   | 70000  | 3         |
| 2  | Henry | 80000  | 4         |
| 3  | Sam   | 60000  | NULL      |
| 4  | Max   | 90000  | NULL      |

**预期结果：**

| Employee |
|----------|
| Joe      |

---

### 解法：自连接

```sql
SELECT e1.Name AS Employee
FROM Employee e1
JOIN Employee e2 ON e1.ManagerId = e2.Id
WHERE e1.Salary > e2.Salary;
```

**思路：**
- 将员工表自连接
- e1 是员工，e2 是经理
- 比较两者的薪水

---

## 5. 查找重复的电子邮箱

### 题目描述

编写一个 SQL 查询，查找 `Person` 表中所有重复的电子邮箱。

**Person 表：**

| Id | Email            |
|----|------------------|
| 1  | a@b.com          |
| 2  | c@d.com          |
| 3  | a@b.com          |

**预期结果：**

| Email   |
|---------|
| a@b.com |

---

### 解法1：GROUP BY + HAVING

```sql
SELECT Email
FROM Person
GROUP BY Email
HAVING COUNT(Email) > 1;
```

---

### 解法2：自连接

```sql
SELECT DISTINCT p1.Email
FROM Person p1
JOIN Person p2 ON p1.Email = p2.Email
WHERE p1.Id != p2.Id;
```

---

## 6. 从不订购的客户

### 题目描述

找出所有从未下过订单的客户。

**Customers 表：**

| Id | Name  |
|----|-------|
| 1  | Joe   |
| 2  | Henry |
| 3  | Sam   |
| 4  | Max   |

**Orders 表：**

| Id | CustomerId |
|----|------------|
| 1  | 3          |
| 2  | 1          |

**预期结果：**

| Customers |
|-----------|
| Henry     |
| Max       |

---

### 解法1：LEFT JOIN + IS NULL

```sql
SELECT c.Name AS Customers
FROM Customers c
LEFT JOIN Orders o ON c.Id = o.CustomerId
WHERE o.CustomerId IS NULL;
```

---

### 解法2：NOT IN

```sql
SELECT Name AS Customers
FROM Customers
WHERE Id NOT IN (
    SELECT CustomerId FROM Orders
);
```

**注意：**
- ⚠️ 如果子查询返回 NULL，NOT IN 会失效
- ✅ 建议使用 `NOT EXISTS` 或 `LEFT JOIN`

---

### 解法3：NOT EXISTS

```sql
SELECT c.Name AS Customers
FROM Customers c
WHERE NOT EXISTS (
    SELECT 1 FROM Orders o WHERE o.CustomerId = c.Id
);
```

**推荐度：** `NOT EXISTS` > `LEFT JOIN` > `NOT IN`

---

## 7. 分数排名

### 题目描述

编写 SQL 查询对分数进行排名。如果两个分数相同，则它们的排名应该相同。排名应该是连续的整数（没有间隔）。

**Scores 表：**

| Id | Score |
|----|-------|
| 1  | 3.50  |
| 2  | 3.65  |
| 3  | 4.00  |
| 4  | 3.85  |
| 5  | 4.00  |
| 6  | 3.65  |

**预期结果：**

| Score | Rank |
|-------|------|
| 4.00  | 1    |
| 4.00  | 1    |
| 3.85  | 2    |
| 3.65  | 3    |
| 3.65  | 3    |
| 3.50  | 4    |

---

### 解法：DENSE_RANK()

```sql
SELECT 
    Score,
    DENSE_RANK() OVER (ORDER BY Score DESC) AS `Rank`
FROM Scores
ORDER BY Score DESC;
```

**排名函数对比：**

```sql
-- 原始数据: [100, 100, 90, 80]

ROW_NUMBER():  [1, 2, 3, 4]    -- 唯一排名
RANK():        [1, 1, 3, 4]    -- 并列有间隔
DENSE_RANK():  [1, 1, 2, 3]    -- 并列无间隔 ⭐
```

---

## 8. 行程和用户取消率

### 题目描述

计算每天的非禁止用户（乘客和司机都未被禁止）的取消率。

**Trips 表：**

| Id | Client_Id | Driver_Id | City_Id | Status             | Request_at |
|----|-----------|-----------|---------|--------------------|------------|
| 1  | 1         | 10        | 1       | completed          | 2013-10-01 |
| 2  | 2         | 11        | 1       | cancelled_by_driver| 2013-10-01 |
| 3  | 3         | 12        | 6       | completed          | 2013-10-01 |

**Users 表：**

| Users_Id | Banned | Role   |
|----------|--------|--------|
| 1        | No     | client |
| 2        | Yes    | client |
| 3        | No     | client |
| 10       | No     | driver |
| 11       | No     | driver |
| 12       | No     | driver |

**预期结果：**

| Day        | Cancellation Rate |
|------------|-------------------|
| 2013-10-01 | 0.33              |

---

### 解法：JOIN + 条件聚合

```sql
SELECT 
    t.Request_at AS `Day`,
    ROUND(
        SUM(CASE WHEN t.Status LIKE 'cancelled%' THEN 1 ELSE 0 END) * 1.0 / 
        COUNT(*), 
        2
    ) AS `Cancellation Rate`
FROM Trips t
JOIN Users c ON t.Client_Id = c.Users_Id AND c.Banned = 'No'
JOIN Users d ON t.Driver_Id = d.Users_Id AND d.Banned = 'No'
WHERE t.Request_at BETWEEN '2013-10-01' AND '2013-10-03'
GROUP BY t.Request_at
ORDER BY t.Request_at;
```

**关键点：**
1. 两次 JOIN Users 表，分别过滤乘客和司机
2. 使用 `CASE WHEN` 统计取消的订单数
3. `ROUND(..., 2)` 保留两位小数

---

## 9. 各部门前3高工资员工

### 题目描述

找出每个部门工资前三高的员工。

---

### 解法：窗口函数

```sql
SELECT 
    Department,
    Employee,
    Salary
FROM (
    SELECT 
        d.Name AS Department,
        e.Name AS Employee,
        e.Salary,
        DENSE_RANK() OVER (
            PARTITION BY e.DepartmentId 
            ORDER BY e.Salary DESC
        ) AS rk
    FROM Employee e
    JOIN Department d ON e.DepartmentId = d.Id
) AS ranked
WHERE rk <= 3;
```

---

## 10. 交换座位

### 题目描述

小美是一所中学的信息科技老师，她有一张 `seat` 座位表，平时用来储存学生名字和与他们相对应的座位 id。其中纵列的 id 是连续递增的。小美想改变相邻俩学生的座位。如果不能交换（最后一个学生是奇数个），则保持不变。

**seat 表：**

| id | student |
|----|---------|
| 1  | Abbot   |
| 2  | Doris   |
| 3  | Emerson |
| 4  | Green   |
| 5  | Jeames  |

**预期结果：**

| id | student |
|----|---------|
| 1  | Doris   |
| 2  | Abbot   |
| 3  | Green   |
| 4  | Emerson |
| 5  | Jeames  |

---

### 解法：CASE WHEN

```sql
SELECT 
    CASE 
        WHEN id % 2 = 1 AND id = (SELECT MAX(id) FROM seat) THEN id  -- 最后一个且为奇数
        WHEN id % 2 = 1 THEN id + 1  -- 奇数id，与下一个交换
        WHEN id % 2 = 0 THEN id - 1  -- 偶数id，与上一个交换
    END AS id,
    student
FROM seat
ORDER BY id;
```

---

## 📌 SQL 优化技巧

### 1. 索引优化

```sql
-- ✅ 好的做法：在常用查询字段上创建索引
CREATE INDEX idx_employee_dept_salary ON Employee(DepartmentId, Salary);
CREATE INDEX idx_email ON Person(Email);

-- ❌ 避免：在索引列上使用函数
SELECT * FROM Employee WHERE YEAR(create_time) = 2024;  -- 索引失效
SELECT * FROM Employee WHERE create_time >= '2024-01-01';  -- 索引生效
```

### 2. 避免 SELECT *

```sql
-- ❌ 不推荐
SELECT * FROM Employee;

-- ✅ 推荐
SELECT Id, Name, Salary FROM Employee;
```

### 3. 使用 EXPLAIN 分析查询

```sql
EXPLAIN SELECT * FROM Employee WHERE DepartmentId = 1;
```

**关注指标：**
- `type`: ALL（全表扫描）→ ref/range（索引扫描）
- `key`: 实际使用的索引
- `rows`: 扫描的行数
- `Extra`: Using filesort（需要排序优化）

### 4. 分页优化

```sql
-- ❌ 深度分页性能差
SELECT * FROM Employee LIMIT 100000, 10;

-- ✅ 使用游标分页
SELECT * FROM Employee WHERE id > 100000 LIMIT 10;
```

### 5. 批量操作

```sql
-- ❌ 逐条插入
INSERT INTO Employee VALUES (1, 'Joe', 70000, 1);
INSERT INTO Employee VALUES (2, 'Jim', 90000, 1);

-- ✅ 批量插入
INSERT INTO Employee VALUES 
(1, 'Joe', 70000, 1),
(2, 'Jim', 90000, 1);
```

---

## 🎯 常见面试题总结

| 题型 | 关键技术点 | 难度 |
|------|----------|------|
| 分组统计 | GROUP BY, HAVING, 聚合函数 | ⭐⭐ |
| 排名问题 | 窗口函数 RANK/DENSE_RANK | ⭐⭐⭐ |
| 连续问题 | 自连接、LEAD/LAG、ROW_NUMBER | ⭐⭐⭐⭐ |
| 树形结构 | 自连接、递归CTE | ⭐⭐⭐⭐ |
| 留存分析 | 自连接、日期计算 | ⭐⭐⭐⭐⭐ |
| Top N 问题 | 窗口函数、子查询 | ⭐⭐⭐ |

---

## 💡 解题套路

### 1. 看到"最" → 想到聚合函数

- 最大/最小：`MAX()/MIN()`
- 第N大：`LIMIT OFFSET` 或 `DENSE_RANK()`

### 2. 看到"连续" → 想到窗口函数或自连接

- 连续出现：`LEAD/LAG` 或自连接
- 连续天数：`ROW_NUMBER()` 差值法

### 3. 看到"排名" → 想到窗口函数

- 不重复排名：`ROW_NUMBER()`
- 并列有间隔：`RANK()`
- 并列无间隔：`DENSE_RANK()`

### 4. 看到"不存在" → 想到 LEFT JOIN 或 NOT EXISTS

- 优先使用 `NOT EXISTS`
- 其次使用 `LEFT JOIN ... IS NULL`
- 避免使用 `NOT IN`（NULL 陷阱）

### 5. 看到"百分比/比率" → 想到条件聚合

```sql
SUM(CASE WHEN condition THEN 1 ELSE 0 END) * 1.0 / COUNT(*)
```