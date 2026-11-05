# MyBatis-Plus 使用指南

## 📋 概述

本项目已集成 MyBatis-Plus 3.5.7（对应 Spring Boot 2.x），并提供一套基于 H2 内存库的完整 CRUD 演示，覆盖逻辑删除、乐观锁、自动填充、分页与条件构造器等特性。

## 🔧 已添加的依赖（pom.xml）

```xml
<!-- MyBatis-Plus：基于 MyBatis 的增强 ORM。3.5.x 对应 Spring Boot 2.x -->
<dependency>
    <groupId>com.baomidou</groupId>
    <artifactId>mybatis-plus-boot-starter</artifactId>
    <version>3.5.7</version>
</dependency>
```

> 说明：项目为 Spring Boot 2.7.18，故使用 `mybatis-plus-boot-starter`（Boot 3 才用 `mybatis-plus-spring-boot3-starter`）。

## ⚙️ 配置说明

### 1. application.yml（MyBatis-Plus 全局配置）

```yaml
spring:
  jpa:
    # 与 JPA(Hibernate DDL) 共存时，把 schema.sql 延后到 Hibernate DDL 之后执行
    defer-datasource-initialization: true

mybatis-plus:
  mapper-locations: classpath*:/mapper/**/*.xml
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
  global-config:
    banner: false
    db-config:
      id-type: assign_id          # 雪花主键
      logic-delete-field: deleted # 全局逻辑删除字段
      logic-delete-value: 1
      logic-not-delete-value: 0
```

### 2. 分页 / 乐观锁插件（MybatisPlusConfig.java）

```java
@Configuration
@MapperScan("com.qq.ijay997.mp.mapper")
public class MybatisPlusConfig {
    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new OptimisticLockerInnerInterceptor()); // 乐观锁
        PaginationInnerInterceptor pagination = new PaginationInnerInterceptor(DbType.H2); // 分页
        pagination.setMaxLimit(500L);
        interceptor.addInnerInterceptor(pagination);
        return interceptor;
    }
}
```

### 3. 建表脚本（schema.sql）

```sql
CREATE TABLE IF NOT EXISTS mp_product (
    id          BIGINT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    price       DECIMAL(10, 2),
    stock       INT,
    version     INT DEFAULT 0,
    deleted     INT DEFAULT 0,
    create_time TIMESTAMP,
    update_time TIMESTAMP
);
```

## 💡 核心组件

| 组件 | 路径 | 说明 |
|------|------|------|
| 实体 | `mp/entity/Product.java` | `@TableName` + 雪花主键 + 乐观锁 + 逻辑删除 + 自动填充 |
| Mapper | `mp/mapper/ProductMapper.java` | 继承 `BaseMapper<Product>` 获得通用 CRUD |
| Service | `mp/service/ProductService.java` | 继承 `IService<Product>`，附加分页/条件查询 |
| ServiceImpl | `mp/service/impl/ProductServiceImpl.java` | 继承 `ServiceImpl`，用 `lambdaQuery()` 演示 |
| Controller | `mp/controller/ProductController.java` | 完整 REST CRUD 接口 |
| 配置 | `mp/config/MybatisPlusConfig.java` | 分页 + 乐观锁 + `@MapperScan` |
| 填充 | `mp/handler/MyMetaObjectHandler.java` | 插入/更新自动填充时间字段 |

## 🚀 演示的 MyBatis-Plus 特性

- ✅ **雪花主键**：`@TableId(type = IdType.ASSIGN_ID)`
- ✅ **逻辑删除**：`@TableLogic`，删除变为 `UPDATE deleted=1`，查询自动过滤
- ✅ **乐观锁**：`@Version` + `OptimisticLockerInnerInterceptor`，更新自动 `version+1`
- ✅ **自动填充**：`@TableField(fill = ...)` + `MetaObjectHandler`
- ✅ **分页插件**：`PaginationInnerInterceptor` + `Page<T>`
- ✅ **条件构造器**：`lambdaQuery()` 链式 `like / ge / le / orderBy`

## 🧪 测试接口（自测方式）

启动应用（换空闲端口，避免影响现有 8082 实例）：

```bash
cd spring
mvn spring-boot:run -Dspring-boot.run.arguments=--server.port=8083
```

### 1. 新增商品

```bash
curl -X POST http://localhost:8083/api/mp/products \
  -H 'Content-Type: application/json' \
  -d '{"name":"iPhone 15","price":5999.00,"stock":100}'
```

### 2. 按 ID 查询

```bash
curl http://localhost:8083/api/mp/products/{id}
```

### 3. 更新（演示乐观锁，版本号自增）

```bash
curl -X PUT http://localhost:8083/api/mp/products/{id} \
  -H 'Content-Type: application/json' \
  -d '{"name":"iPhone 15 Pro","price":7999.00,"stock":80}'
```

### 4. 逻辑删除

```bash
curl -X DELETE http://localhost:8083/api/mp/products/{id}
```

### 5. 分页查询（可按最低价过滤）

```bash
curl 'http://localhost:8083/api/mp/products/page?current=1&size=5&minPrice=100'
```

### 6. 条件构造器查询（名称模糊 + 价格区间）

```bash
curl 'http://localhost:8083/api/mp/products/condition?name=iPhone&minPrice=5000&maxPrice=9000'
```

### 7. 新增分类（level 字段写入）

```bash
curl -X POST http://localhost:8083/api/mp/categories \
  -H 'Content-Type: application/json' \
  -d '{"name":"手机","level":1}'
```

### 8. 从分类侧查商品（按分类名，自定义 JOIN SQL）

```bash
curl 'http://localhost:8083/api/mp/categories/products?name=手机'
```

### 9. 从分类侧查商品（分页，关联查询配合分页插件）

```bash
curl 'http://localhost:8083/api/mp/categories/products/page?name=手机&current=1&size=5'
```

### 10. 从商品侧查关联信息（商品详情带分类）

```bash
curl 'http://localhost:8083/api/mp/categories/product/{productId}/detail'
```

> 关联查询返回 `ProductCategoryVO`，字段为 `productId / productName / price / stock / productLevel / categoryId / categoryName / categoryLevel`。

### 单元 / 集成测试

```bash
cd spring
mvn -pl spring test -Dtest='ProductServiceTest,ProductControllerTest,CategoryServiceTest'
```

测试覆盖：插入自动填充、按 ID 查询、更新（版本号 +1）、乐观锁过期更新失败、逻辑删除、条件构造器、分页，以及分类 CRUD、level 字段写入/查询、商品-分类关联查询（列表/详情/分页）。

## 🔗 关联查询与 level 字段

### 1. level 字段说明

`Product` 与 `Category` 均新增 `level` 字段，类型选择 `Integer`：

- 等级/层级是**数值语义**，`Integer` 便于 `BETWEEN` 区间过滤与 `ORDER BY` 排序。
- 相比 `String` 更紧凑、无编码歧义；相比 `int` 支持 `null` 表示“未评级/未分层”。

| 表 | 字段 | 语义 |
|----|------|------|
| `mp_product` | `level` | 商品等级（如 1~5） |
| `mp_category` | `level` | 分类层级（1=一级，2=二级） |

### 2. 关联查询实现

MyBatis-Plus 不支持 JOIN，故在 [CategoryMapper](file:///Users/jay/Documents/ideaProject/demo/project-my/spring/src/main/java/com/qq/ijay997/mp/mapper/CategoryMapper.java) 中用 `@Select` 手写 SQL：

```sql
SELECT p.id AS product_id, p.name AS product_name, p.price, p.stock,
       p.level AS product_level, p.category_id AS category_id,
       c.name AS category_name, c.level AS category_level
FROM mp_product p
LEFT JOIN mp_category c ON p.category_id = c.id
WHERE c.name = #{name} AND p.deleted = 0
```

列别名用下划线，配合 `mybatis-plus.configuration.map-underscore-to-camel-case: true` 自动映射到 `ProductCategoryVO` 的驼峰属性。

### 3. 分页关联查询

分页版方法首个参数传 `Page<ProductCategoryVO>`，`PaginationInnerInterceptor` 会自动生成 `COUNT` 与 `LIMIT`：

```java
IPage<ProductCategoryVO> selectProductsByCategoryNamePage(Page<ProductCategoryVO> page, @Param("name") String name);
```

## 🌱 初始化演示数据

`schema.sql` 在表结构之后追加了幂等的种子数据：5 个分类、14 个商品，用于启动后直接 curl 演示关联查询、分页/条件过滤与子查询。

- **幂等写法**：用 H2 原生 `MERGE INTO ... KEY(id)`，以主键为匹配键，存在则更新、不存在则插入，重启/重复加载不会重复插入或主键冲突。
- **固定主键**：分类 `1~5`、商品 `101~114`，与 MyBatis-Plus 雪花主键（19 位）不冲突；`111` 的 `category_id` 为 `NULL`，用于验证左连接/未分类场景。

### 种子数据一览

| 分类 id | 分类名 | level |
|--------|--------|-------|
| 1 | 手机数码 | 1 |
| 2 | 家用电器 | 1 |
| 3 | 图书 | 2 |
| 4 | 运动户外 | 2 |
| 5 | 智能穿戴 | 2 |

| 商品 | category_id | level | 价格 |
|------|-------------|-------|------|
| 智能手机 / 蓝牙耳机 / 笔记本电脑 | 1 | 5 / 4 / 5 | 3999 / 299 / 8999 |
| 滚筒洗衣机 / 空调 / 冰柜 | 2 | 4 / 4 / 3 | 2999 / 3599 / 1999 |
| Java编程思想 / Spring实战 | 3 | 3 / 3 | 99 / 129 |
| 跑步鞋 / 瑜伽垫 | 4 | 4 / 2 | 499 / 59 |
| 智能手表 / 智能手环 / 蓝牙眼镜 | 5 | 4 / 2 / 5 | 1999 / 199 / 9999 |
| 未分类样品 | NULL | 1 | 9.90 |

> 提示：种子数据名称刻意避开测试里用到的「苹果 / 香蕉 / 苹果派」等关键字，避免污染 `CategoryServiceTest` 的 `like('苹果')` 计数断言。

## 🔎 子查询关联查询

演示「价格高于所属分类商品均价」的关联子查询，SQL 在 [CategoryMapper](file:///Users/jay/Documents/ideaProject/demo/project-my/spring/src/main/java/com/qq/ijay997/mp/mapper/CategoryMapper.java)：

```sql
SELECT p.id AS product_id, p.name AS product_name, p.price AS price,
       p.stock AS stock, p.level AS product_level, p.category_id AS category_id,
       c.name AS category_name, c.level AS category_level,
       (SELECT AVG(p2.price) FROM mp_product p2
         WHERE p2.category_id = p.category_id AND p2.deleted = 0) AS category_avg_price
FROM mp_product p
LEFT JOIN mp_category c ON p.category_id = c.id
WHERE p.deleted = 0 AND p.category_id IS NOT NULL
  AND p.price > (SELECT AVG(p3.price) FROM mp_product p3
                  WHERE p3.category_id = p.category_id AND p3.deleted = 0)
ORDER BY p.id ASC
```

- 返回 `ProductCategoryVO`，其中 `categoryAvgPrice` 为子查询算出的分类均价。
- 未分类商品（`category_id IS NULL`）不参与；`deleted = 0` 需手写在自定义 SQL 中（MP 不会为 `@Select` 自动拼逻辑删除）。

### 与 `LevelInterceptor` 的相互作用（重要局限）

该方法标注了 `@Level("p")`。`LevelInterceptor` 用 JSqlParser 只改写**外层** `PlainSelect`，会追加 `p.level <= threshold`；但它**不会递归进入 WHERE 中的子查询**，因此：

- 外层商品行会被权限过滤；
- 子查询的 `AVG(p3.price)` 仍按**全量**计算（包含高等级商品）→ **权限穿透**；
- 同理，`LEFT JOIN` 的分类表也不会被过滤。

观察方式：种子分类 5 的商品为 1999 / 199 / 9999，全量均价 ≈ 4065.67。手动设置 `UserContextHolder.setLevel(4)` 后：

- 外层过滤掉 level=5 的「蓝牙眼镜」；
- 但子查询均价仍是 4065.67，1999 的「智能手表」依旧低于均价、不会出现；
- 若子查询也被正确过滤，均价会变成 (1999+199)/2 = 1099，「智能手表」就应出现 —— 这正是权限穿透的可观察差异。

> 局限点：需要「子查询也受权限约束」时，当前拦截器无法满足，需扩展 `LevelInterceptor` 对子查询的递归处理，或在 SQL 内自行带上等级条件。

### curl 示例

```bash
# 高于所属分类均价的商品（预期返回 6 条：103/104/105/108/109/114）
curl 'http://localhost:8083/api/mp/categories/products/above-avg'
```

预期返回（节选）：

```json
[
  {"productId":103,"productName":"笔记本电脑","price":8999.00,"productLevel":5,
   "categoryName":"手机数码","categoryLevel":1,"categoryAvgPrice":4432.33},
  {"productId":114,"productName":"蓝牙眼镜","price":9999.00,"productLevel":5,
   "categoryName":"智能穿戴","categoryLevel":2,"categoryAvgPrice":4065.67}
]
```

### `X-Level` 请求头（HTTP 模拟数据权限）

`MockUserContextFilter` 会解析 `X-Level`，把值写入当前 `UserPrincipal#level`，最终由 `LevelInterceptor` 对**标注了 `@Level` 的查询**追加 `alias.level <= threshold`。它只影响数据权限阈值，不影响 `X-Role` / `X-Permissions` 决定的权限码，也不影响脱敏链路。

解析规则（fail-safe，不会返回 500）：

| `X-Level` | 结果 |
|---|---|
| 缺省 / 空白 / 非数字（`abc`、`4.5`）/ 负数 / 超出 int 范围 | 视为未设置（`null`），拦截器不追加任何条件，与不带该头完全一致 |
| `0`、`2`、`4` … | 作为阈值，追加 `level <= N` |

```bash
# 1) 不带 X-Level：拦截器休眠，返回 6 条（含 level=5 的 103/114）
curl 'http://localhost:8083/api/mp/categories/products/above-avg'

# 2) 带 X-Level: 4：level=5 的 103/114 被过滤，只剩 104/105/108/109
curl -H 'X-Level: 4' 'http://localhost:8083/api/mp/categories/products/above-avg'

# 3) 非法值：与不带完全一致（仍返回 6 条，且 HTTP 200）
curl -H 'X-Level: abc' 'http://localhost:8083/api/mp/categories/products/above-avg'
```

补充观察「智能手表(112, level=4) 被过滤」：

```bash
curl -H 'X-Level: 2' 'http://localhost:8083/api/mp/categories/products/page?name=智能穿戴&current=1&size=50'
```

预期只返回 level<=2 的 110 / 111 / 113，112 不出现。注意：该分页 SQL 当前的 `name` 条件被注释掉了，此处演示的是**等级过滤**而非分类范围筛选。

## 🔍 注意事项

1. **与 JPA 共存**：项目已有 `spring-boot-starter-data-jpa`，MyBatis-Plus 与 JPA 使用同一 `DataSource`（H2），互为独立 ORM，互不干扰。为保证 `schema.sql` 不被 Hibernate `create-drop` 覆盖，设置了 `spring.jpa.defer-datasource-initialization: true`。
2. **离线可运行**：使用 H2 内存库，`schema.sql` 自动建表，测试无需外部数据库。
3. **版本对齐**：MyBatis-Plus 3.5.7 仅对应 Spring Boot 2.x；不要升级到 Boot 3 后仍用本 starter。
4. **逻辑删除字段名**：全局配置了 `deleted`，实体上也用 `@TableLogic` 标注，二者保持一致。
5. **乐观锁失败判定**：`updateById` 返回 `false` 表示版本号已过期（被并发修改），需业务侧处理冲突。