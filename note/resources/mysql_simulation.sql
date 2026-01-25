-- 模拟 MySQL 不可重复读和幻读的 SQL 实验脚本

-- 1. 创建测试数据库和表
DROP DATABASE IF EXISTS isolation_test;
CREATE DATABASE isolation_test;
USE isolation_test;

CREATE TABLE user_accounts (
    id INT PRIMARY KEY,
    name VARCHAR(50),
    balance DECIMAL(10, 2)
);

INSERT INTO user_accounts (id, name, balance) VALUES 
(1, 'Alice', 1000.00),
(2, 'Bob', 500.00);

-- 2. 查看当前事务隔离级别
SELECT @@transaction_isolation;

-- 3. 模拟不可重复读场景
-- 会话 A: 开启事务并第一次读取数据
START TRANSACTION;
SELECT * FROM user_accounts WHERE id = 1;

-- 此时在会话 B 执行以下语句:
-- START TRANSACTION;
-- UPDATE user_accounts SET balance = 1200.00 WHERE id = 1;
-- COMMIT;

-- 会话 A: 再次读取相同数据 (会出现不可重复读)
SELECT * FROM user_accounts WHERE id = 1;
COMMIT;

-- 4. 模拟幻读场景
-- 会话 A: 开启事务并第一次查询范围数据
START TRANSACTION;
SELECT * FROM user_accounts WHERE balance > 600;

-- 此时在会话 B 执行以下语句:
-- START TRANSACTION;
-- INSERT INTO user_accounts (id, name, balance) VALUES (3, 'Charlie', 800.00);
-- COMMIT;

-- 会话 A: 再次查询相同范围数据 (会出现幻读)
SELECT * FROM user_accounts WHERE balance > 600;
COMMIT;

-- 5. 设置隔离级别为 REPEATABLE READ (默认)
SET SESSION TRANSACTION ISOLATION LEVEL REPEATABLE READ;

-- 6. 在 REPEATABLE READ 隔离级别下重新测试不可重复读
START TRANSACTION;
SELECT * FROM user_accounts WHERE id = 1;

-- 会话 B 执行:
-- START TRANSACTION;
-- UPDATE user_accounts SET balance = 1500.00 WHERE id = 1;
-- COMMIT;

-- 会话 A: 再次读取相同数据 (不会出现不可重复读)
SELECT * FROM user_accounts WHERE id = 1;
COMMIT;

-- 7. 在 REPEATABLE READ 隔离级别下重新测试幻读
START TRANSACTION;
SELECT * FROM user_accounts WHERE balance > 600;

-- 会话 B 执行:
-- START TRANSACTION;
-- INSERT INTO user_accounts (id, name, balance) VALUES (4, 'David', 900.00);
-- COMMIT;

-- 会话 A: 再次查询相同范围数据 (不会出现幻读)  Mysql 的 可重复读的级别下,使用了 MVCC（多版本并发控制）和 间隙锁（Gap Lock） + Next-Key Lock
SELECT * FROM user_accounts WHERE balance > 600  ; -- 快照读
-- 出现幻读 （FOR UPDATE 和 不加的语句不要同时执行）
SELECT * FROM user_accounts WHERE balance > 600 FOR UPDATE ; -- 当前读
COMMIT;

-- 8. 设置隔离级别为 SERIALIZABLE
SET SESSION TRANSACTION ISOLATION LEVEL SERIALIZABLE;

-- 9. 在 SERIALIZABLE 隔离级别下测试幻读
START TRANSACTION;
SELECT * FROM user_accounts WHERE balance > 600;

-- 会话 B 尝试执行以下语句会阻塞:
-- START TRANSACTION;
-- INSERT INTO user_accounts (id, name, balance) VALUES (5, 'Eve', 700.00);
-- COMMIT;

-- 会话 A: 再次查询相同范围数据 (不会出现幻读)
SELECT * FROM user_accounts WHERE balance > 600;
COMMIT;