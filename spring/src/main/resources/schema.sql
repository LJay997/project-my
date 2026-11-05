-- MyBatis-Plus 演示表结构（离线可运行：H2 内存库自动初始化）

-- 商品分类表（先建，供 mp_product 外键引用）
CREATE TABLE IF NOT EXISTS mp_category (
    id          BIGINT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    level       INT,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);

-- 商品表
CREATE TABLE IF NOT EXISTS mp_product (
    id          BIGINT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    price       DECIMAL(10, 2),
    stock       INT,
    level       INT,
    category_id BIGINT,
    version     INT DEFAULT 0,
    deleted     INT DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP,
    CONSTRAINT fk_product_category FOREIGN KEY (category_id) REFERENCES mp_category(id)
);

-- ============================================================
-- 演示种子数据（幂等）
-- 选用 H2 原生 MERGE INTO ... KEY(id)：以主键 id 为匹配键，
-- 已存在则更新、不存在则插入，应用重启/测试重复加载不会重复插入或主键冲突。
-- ============================================================

-- 分类（不同 level 体现层级差异）
MERGE INTO mp_category (id, name, level, create_time, update_time) KEY(id)
    VALUES (1, '手机数码', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO mp_category (id, name, level, create_time, update_time) KEY(id)
    VALUES (2, '家用电器', 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO mp_category (id, name, level, create_time, update_time) KEY(id)
    VALUES (3, '图书', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO mp_category (id, name, level, create_time, update_time) KEY(id)
    VALUES (4, '运动户外', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
-- 子查询演示分类：价格跨度大，便于观察「均价 vs 单价」
MERGE INTO mp_category (id, name, level, create_time, update_time) KEY(id)
    VALUES (5, '智能穿戴', 2, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 商品（category_id 正确指向分类；level 有区分度；价格/库存合理；111 为未分类场景）
MERGE INTO mp_product (id, name, price, stock, level, category_id, version, deleted, create_time, update_time) KEY(id)
    VALUES (101, '智能手机', 3999.00, 50, 5, 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO mp_product (id, name, price, stock, level, category_id, version, deleted, create_time, update_time) KEY(id)
    VALUES (102, '蓝牙耳机', 299.00, 200, 4, 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO mp_product (id, name, price, stock, level, category_id, version, deleted, create_time, update_time) KEY(id)
    VALUES (103, '笔记本电脑', 8999.00, 30, 5, 1, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO mp_product (id, name, price, stock, level, category_id, version, deleted, create_time, update_time) KEY(id)
    VALUES (104, '滚筒洗衣机', 2999.00, 20, 4, 2, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO mp_product (id, name, price, stock, level, category_id, version, deleted, create_time, update_time) KEY(id)
    VALUES (105, '空调', 3599.00, 15, 4, 2, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO mp_product (id, name, price, stock, level, category_id, version, deleted, create_time, update_time) KEY(id)
    VALUES (106, '冰柜', 1999.00, 10, 3, 2, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO mp_product (id, name, price, stock, level, category_id, version, deleted, create_time, update_time) KEY(id)
    VALUES (107, 'Java编程思想', 99.00, 300, 3, 3, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO mp_product (id, name, price, stock, level, category_id, version, deleted, create_time, update_time) KEY(id)
    VALUES (108, 'Spring实战', 129.00, 200, 3, 3, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO mp_product (id, name, price, stock, level, category_id, version, deleted, create_time, update_time) KEY(id)
    VALUES (109, '跑步鞋', 499.00, 80, 4, 4, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO mp_product (id, name, price, stock, level, category_id, version, deleted, create_time, update_time) KEY(id)
    VALUES (110, '瑜伽垫', 59.00, 150, 2, 4, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO mp_product (id, name, price, stock, level, category_id, version, deleted, create_time, update_time) KEY(id)
    VALUES (111, '未分类样品', 9.90, 999, 1, NULL, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- 子查询演示数据（分类 5「智能穿戴」）：均价 = (1999+199+9999)/3 ≈ 4065.67
-- 仅 9999 的「蓝牙眼镜」高于均价；1999 的「智能手表」低于均价。
MERGE INTO mp_product (id, name, price, stock, level, category_id, version, deleted, create_time, update_time) KEY(id)
    VALUES (112, '智能手表', 1999.00, 40, 4, 5, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO mp_product (id, name, price, stock, level, category_id, version, deleted, create_time, update_time) KEY(id)
    VALUES (113, '智能手环', 199.00, 100, 2, 5, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
MERGE INTO mp_product (id, name, price, stock, level, category_id, version, deleted, create_time, update_time) KEY(id)
    VALUES (114, '蓝牙眼镜', 9999.00, 5, 5, 5, 0, 0, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);