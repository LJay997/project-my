# MyBatis 面试考察指南（高阶版）

> **适用人群**：2-5年Java开发经验、准备中高级/资深面试的工程师
> **目标**：系统覆盖MyBatis核心知识、技术原理、实际应用场景及高频面试题
> **版本说明**：基于 MyBatis 3.5.x 特性，兼顾 Spring Boot 3.x 集成

***

## 目录

1. [基础概念与核心原理](#1-基础概念与核心原理)
2. [映射与配置详解](#2-映射与配置详解)
3. [缓存机制深度解析](#3-缓存机制深度解析)
4. [插件机制与拦截器](#4-插件机制与拦截器)
5. [Spring Boot 集成与自动配置](#5-spring-boot-集成与自动配置)
6. [常见面试题深度解析](#6-常见面试题深度解析)
7. [模拟面试问答（高频题）](#7-模拟面试问答高频题)

***

## 1. 基础概念与核心原理

### 1.1 MyBatis 核心特性概述

| 特性         | 说明                                                     |
| ------------ | -------------------------------------------------------- |
| SQL 与代码分离 | SQL 写在 XML 或注解中，便于统一管理和优化                      |
| 动态 SQL     | 强大的动态 SQL 标签（if/foreach/choose/where/set），灵活构建查询  |
| 结果映射      | 支持自动映射和手动映射，处理复杂对象关系（一对一、一对多）              |
| 插件机制      | 基于拦截器链的插件体系，可扩展分页、加密、审计等功能                  |
| 缓存机制      | 一级缓存（SqlSession 级别）+ 二级缓存（Mapper 级别），支持自定义缓存    |
| 延迟加载      | 关联查询按需加载，减少不必要的数据库查询                          |
| 注解支持      | 支持 @Select/@Insert/@Update/@Delete 注解，简化简单 CRUD       |
| 批量操作      | 支持批量插入、批量更新，配合 BatchExecutor 提升性能                |

### 1.2 MyBatis 核心架构

```
┌─────────────────────────────────────────────────────────────┐
│                        接口层（API）                           │
│            SqlSession / Mapper 接口                          │
├─────────────────────────────────────────────────────────────┤
│                       数据处理层（Core）                        │
│    参数映射 → SQL 解析 → SQL 执行 → 结果映射                    │
├─────────────────────────────────────────────────────────────┤
│                       基础支撑层（Base）                        │
│   连接池管理 | 事务管理 | 缓存管理 | 配置加载                     │
└─────────────────────────────────────────────────────────────┘
```

#### 1.2.1 核心组件与执行流程

```
Mapper 接口调用
      │
      ▼
┌─────────────┐
│ MapperProxy │  ← 动态代理拦截
└──────┬──────┘
       │
       ▼
┌─────────────┐
│ SqlSession  │  ← 门面接口
└──────┬──────┘
       │
       ▼
┌─────────────┐
│  Executor   │  ← SQL 执行器（Simple/Reuse/Batch）
└──────┬──────┘
       │
       ▼
┌─────────────────┐
│StatementHandler │  ← 处理 JDBC Statement
└──────┬──────────┘
       │
       ▼
┌─────────────────┐
│ParameterHandler │  ← 参数映射处理
└──────┬──────────┘
       │
       ▼
┌──────────────────┐
│ResultSetHandler  │  ← 结果集映射处理
└──────────────────┘
```

**执行流程详解**：

1. **Mapper 接口代理**：MyBatis 通过 JDK 动态代理为 Mapper 接口生成代理对象 `MapperProxy`
2. **SqlSession**：作为门面，提供增删改查操作，内部委托给 `Executor` 执行
3. **Executor**：负责 SQL 执行的生命周期管理
   - `SimpleExecutor`：每次执行都创建新的 Statement（默认）
   - `ReuseExecutor`：复用 Statement
   - `BatchExecutor`：批量执行，适合批量操作
   - `CachingExecutor`：装饰器模式，在 Executor 之上增加缓存功能
4. **StatementHandler**：封装 JDBC Statement 操作，设置参数、执行 SQL
5. **ParameterHandler**：将 Java 参数映射到 SQL 占位符
6. **ResultSetHandler**：将 JDBC ResultSet 映射为 Java 对象

#### 1.2.2 配置文件层级结构

```xml
<!-- mybatis-config.xml -->
<configuration>
    <!-- 1. properties 属性 -->
    <properties resource="db.properties"/>

    <!-- 2. settings 全局设置 -->
    <settings>
        <setting name="cacheEnabled" value="true"/>
        <setting name="lazyLoadingEnabled" value="true"/>
        <setting name="mapUnderscoreToCamelCase" value="true"/>
    </settings>

    <!-- 3. typeAliases 类型别名 -->
    <typeAliases>
        <package name="com.example.entity"/>
    </typeAliases>

    <!-- 4. typeHandlers 类型处理器 -->
    <!-- 5. objectFactory 对象工厂 -->
    <!-- 6. plugins 插件 -->
    <!-- 7. environments 环境配置 -->
    <!-- 8. mappers 映射器 -->
</configuration>
```

> **配置加载顺序**：properties → settings → typeAliases → typeHandlers → objectFactory → plugins → environments → mappers

### 1.3 动态代理与 Mapper 接口

#### 1.3.1 Mapper 接口为什么不需要实现类？

```java
// 你只需要定义接口
public interface UserMapper {
    User selectById(Long id);
}
// MyBatis 通过 MapperProxy 动态代理，底层做这些事：
// 1. 根据方法名（或注解）找到对应的 SQL 语句
// 2. 解析参数
// 3. 执行 SQL
// 4. 映射结果
```

**核心源码逻辑**（简化版）：

```java
// MapperProxy 实现了 InvocationHandler
public class MapperProxy<T> implements InvocationHandler {
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        // 如果是 Object 方法，直接执行
        if (Object.class.equals(method.getDeclaringClass())) {
            return method.invoke(this, args);
        }
        // 构建 MapperMethod，执行 SQL
        MapperMethod mapperMethod = cachedMapperMethod(method);
        return mapperMethod.execute(sqlSession, args);
    }
}
```

#### 1.3.2 方法名与 SQL 的绑定方式

| 绑定方式     | 说明                             | 示例                                          |
| ------------ | -------------------------------- | --------------------------------------------- |
| XML 映射     | 通过 namespace + id 匹配          | `<mapper namespace="com.example.UserMapper">` |
| 注解映射     | 直接在方法上写 SQL                | `@Select("SELECT * FROM user")`               |
| 方法名解析   | 方法名与 statement id 必须一致    | `sqlSession.selectOne("com.example.UserMapper.selectById")` |

***

> **考察要点提示**
>
> 面试官可能考察：
> 1. **架构理解**：画出 MyBatis 的执行流程图并解释每个组件的作用
> 2. **动态代理**：Mapper 接口如何实现不需要实现类就能执行 SQL？
> 3. **Executor 类型**：三种 Executor 的使用场景和区别？
> 4. **#{} 和 ${} 的区别**：为什么推荐使用 #{}
>
> **常见陷阱**：
> - 错误认为 MyBatis 是 ORM 框架：MyBatis 是半自动 ORM，SQL 需手写
> - 混淆一级缓存和二级缓存的作用域：一级是 SqlSession，二级是 Namespace

***

## 2. 映射与配置详解

### 2.1 参数映射

#### 2.1.1 #{} 与 ${} 深度对比

| 对比维度     | #{}（预编译）                    | ${}（字符串替换）              |
| ------------ | -------------------------------- | ------------------------------ |
| SQL 注入     | 防注入 ✅                        | 不防注入 ❌                    |
| 处理方式     | 使用 ? 占位符，PreparedStatement | 直接拼接字符串                  |
| 类型处理     | 自动添加引号                     | 原样拼接                        |
| 适用场景     | 条件参数值                       | 动态表名、列名、ORDER BY         |
| 性能         | 预编译可复用，更快                | 每次都重新编译                  |

**示例代码**：

```java
// #{id} → 生成: SELECT * FROM user WHERE id = ?
// 执行时: SELECT * FROM user WHERE id = '1'

// ${tableName} → 生成: SELECT * FROM user
// 执行时: SELECT * FROM user（直接拼接，无引号）
```

```xml
<!-- 正确用法：条件值用 #{} -->
<select id="selectById" resultType="User">
    SELECT * FROM user WHERE id = #{id}
</select>

<!-- 特殊场景：表名/列名只能用 ${}（需注意防注入） -->
<select id="selectByTable" resultType="User">
    SELECT * FROM ${tableName} WHERE id = #{id}
</select>

<!-- 排序场景：ORDER BY 只能用 ${} -->
<select id="selectByOrder" resultType="User">
    SELECT * FROM user ORDER BY ${column} ${direction}
</select>
```

> **安全建议**：使用 `${}` 时，必须在业务层做白名单校验，防止 SQL 注入。

#### 2.1.2 多参数传递

```java
// 方式一：@Param 注解（推荐）
User selectByNameAndAge(@Param("name") String name, @Param("age") Integer age);

// 方式二：Map 传参
User selectByMap(Map<String, Object> params);

// 方式三：对象传参
User selectByUser(User user);

// 方式四：混合传参
List<User> selectByCondition(@Param("user") User user,
                              @Param("page") PageInfo page);
```

```xml
<!-- 方式一：@Param 取值 -->
<select id="selectByNameAndAge" resultType="User">
    SELECT * FROM user WHERE name = #{name} AND age = #{age}
</select>

<!-- 方式二：Map 取值 -->
<select id="selectByMap" resultType="User">
    SELECT * FROM user WHERE name = #{name} AND age = #{age}
</select>

<!-- 方式三：对象属性取值 -->
<select id="selectByUser" resultType="User">
    SELECT * FROM user WHERE name = #{name} AND age = #{age}
</select>
```

### 2.2 结果映射

#### 2.2.1 resultType vs resultMap

| 对比维度   | resultType                   | resultMap                      |
| ---------- | ---------------------------- | ------------------------------ |
| 映射方式   | 自动映射（通过字段名/别名）     | 手动配置映射规则               |
| 适用场景   | 简单查询、POJO 与列名一致     | 复杂映射、关联查询、嵌套对象     |
| 灵活性     | 较低                         | 高                             |
| 配置复杂度 | 低                           | 较高                           |

#### 2.2.2 一对一关联查询

```xml
<!-- 方式一：嵌套结果（推荐，一条 SQL 搞定） -->
<resultMap id="UserWithDetailResult" type="User">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <association property="detail" javaType="UserDetail">
        <id property="id" column="detail_id"/>
        <result property="address" column="address"/>
        <result property="phone" column="phone"/>
    </association>
</resultMap>

<select id="selectUserWithDetail" resultMap="UserWithDetailResult">
    SELECT u.*, d.id AS detail_id, d.address, d.phone
    FROM user u
    LEFT JOIN user_detail d ON u.id = d.user_id
    WHERE u.id = #{id}
</select>

<!-- 方式二：嵌套查询（N+1 问题，配合延迟加载） -->
<resultMap id="UserWithDetailQuery" type="User">
    <id property="id" column="id"/>
    <association property="detail"
                 column="id"
                 select="com.example.mapper.UserDetailMapper.selectByUserId"/>
</resultMap>

<select id="selectUser" resultMap="UserWithDetailQuery">
    SELECT * FROM user WHERE id = #{id}
</select>
```

#### 2.2.3 一对多关联查询

```xml
<resultMap id="UserWithOrdersResult" type="User">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <collection property="orders" ofType="Order">
        <id property="id" column="order_id"/>
        <result property="orderNo" column="order_no"/>
        <result property="amount" column="amount"/>
        <result property="createTime" column="create_time"/>
    </collection>
</resultMap>

<select id="selectUserWithOrders" resultMap="UserWithOrdersResult">
    SELECT u.*, o.id AS order_id, o.order_no, o.amount, o.create_time
    FROM user u
    LEFT JOIN `order` o ON u.id = o.user_id
    WHERE u.id = #{id}
</select>
```

### 2.3 动态 SQL

#### 2.3.1 核心标签一览

| 标签          | 作用                             | 示例场景                  |
| ------------- | -------------------------------- | ------------------------- |
| `<if>`        | 条件判断                          | 根据参数是否为空动态拼接    |
| `<choose>`    | 多条件分支（类似 switch）          | 多条件互斥选择             |
| `<where>`     | 自动添加 WHERE 并去除多余 AND/OR   | 动态条件查询               |
| `<set>`       | 自动添加 SET 并去除多余逗号        | 动态更新字段               |
| `<foreach>`   | 遍历集合                          | IN 查询、批量插入           |
| `<trim>`      | 自定义前缀/后缀和去除规则           | 灵活的字符串处理            |
| `<bind>`      | 绑定变量（OGNL 表达式）             | 模糊查询拼接 `%`           |
| `<sql>`       | 定义可复用的 SQL 片段              | 公共列、公共条件            |

#### 2.3.2 动态 SQL 实战

```xml
<!-- 动态条件查询 -->
<select id="selectByCondition" resultType="User">
    SELECT * FROM user
    <where>
        <if test="name != null and name != ''">
            AND name LIKE CONCAT('%', #{name}, '%')
        </if>
        <if test="age != null">
            AND age = #{age}
        </if>
        <if test="status != null">
            AND status = #{status}
        </if>
        <if test="createTime != null">
            AND create_time >= #{createTime}
        </if>
    </where>
</select>

<!-- 动态更新 -->
<update id="updateSelective">
    UPDATE user
    <set>
        <if test="name != null">name = #{name},</if>
        <if test="age != null">age = #{age},</if>
        <if test="email != null">email = #{email},</if>
        <if test="updateTime != null">update_time = #{updateTime},</if>
    </set>
    WHERE id = #{id}
</update>

<!-- 批量插入 -->
<insert id="batchInsert">
    INSERT INTO user (name, age, email) VALUES
    <foreach collection="list" item="user" separator=",">
        (#{user.name}, #{user.age}, #{user.email})
    </foreach>
</insert>

<!-- IN 查询 -->
<select id="selectByIds" resultType="User">
    SELECT * FROM user
    WHERE id IN
    <foreach collection="ids" item="id" open="(" separator="," close=")">
        #{id}
    </foreach>
</select>

<!-- choose 分支选择 -->
<select id="selectByPriority" resultType="User">
    SELECT * FROM user
    <where>
        <choose>
            <when test="id != null">
                AND id = #{id}
            </when>
            <when test="name != null">
                AND name = #{name}
            </when>
            <otherwise>
                AND status = 1
            </otherwise>
        </choose>
    </where>
</select>
```

***

> **考察要点提示**
>
> 面试官可能考察：
> 1. **#{} 和 ${} 的区别**：为什么 #{} 能防 SQL 注入？
> 2. **resultMap 和 resultType 的选择**：什么时候必须用 resultMap？
> 3. **动态 SQL 原理**：`<if>` 标签如何判断参数是否存在？
> 4. **N+1 问题**：什么是 N+1 问题？如何解决？
>
> **常见陷阱**：
> - `<if test="name != null and name != ''">` 中 `and` 是 OGNL 表达式，不能用 `&&`
> - `<foreach>` 中 `collection` 属性：List 用 `list`，数组用 `array`，`@Param` 用指定的名字

***

## 3. 缓存机制深度解析

### 3.1 一级缓存 vs 二级缓存

| 对比维度   | 一级缓存（Local Cache）           | 二级缓存（Second Level Cache）      |
| ---------- | -------------------------------- | ---------------------------------- |
| 作用域     | SqlSession 级别                   | Namespace/Mapper 级别               |
| 默认开启   | 是 ✅                             | 否 ❌                              |
| 生命周期   | SqlSession 关闭时清空              | 整个应用生命周期                      |
| 共享范围   | 同一个 SqlSession 内共享           | 跨 SqlSession 共享（同 Namespace）   |
| 更新清理   | 执行增删改时清空                    | 执行增删改时清空                     |
| 存储位置   | 堆内存（HashMap）                  | 可配置（默认 PerpetualCache）        |
| 可替换     | 否                                | 是（可集成 Redis/Ehcache 等）        |

### 3.2 一级缓存工作原理

```
┌─────────────────────────────────────────────────────────┐
│  SqlSession                                              │
│  ┌───────────────┐                                       │
│  │  LocalCache   │  HashMap<CacheKey, Object>            │
│  │               │                                       │
│  │  key1 → obj1  │                                       │
│  │  key2 → obj2  │                                       │
│  └───────────────┘                                       │
│                                                          │
│  执行流程：                                               │
│  1. 查询前：先查 LocalCache，命中则直接返回                 │
│  2. 查不到：执行 SQL 查数据库，结果存入 LocalCache          │
│  3. 增删改操作：清空 LocalCache                            │
│  4. SqlSession.close()：清空 LocalCache                   │
└─────────────────────────────────────────────────────────┘
```

**一级缓存失效条件**：

```java
// 1. 不同的 SqlSession
SqlSession session1 = sqlSessionFactory.openSession();
SqlSession session2 = sqlSessionFactory.openSession();
// session1 和 session2 各自有一级缓存，互不可见

// 2. 同一个 SqlSession 但查询条件不同
session.selectOne("selectById", 1);  // 缓存 key1
session.selectOne("selectById", 2);  // 缓存 key2（不同 key）

// 3. 执行了增删改操作
session.selectOne("selectById", 1);  // 查数据库，缓存
session.insert("insert", user);      // 增删改 → 清空缓存
session.selectOne("selectById", 1);  // 再次查数据库

// 4. 手动清空缓存
session.clearCache();

// 5. 提交或回滚事务
session.commit();  // 清空缓存
```

### 3.3 二级缓存配置与使用

```xml
<!-- 第一步：全局开启二级缓存 -->
<settings>
    <setting name="cacheEnabled" value="true"/>
</settings>

<!-- 第二步：Mapper XML 中声明 -->
<mapper namespace="com.example.mapper.UserMapper">
    <!-- 开启二级缓存 -->
    <cache
        eviction="LRU"           <!-- 淘汰策略：LRU/FIFO/SOFT/WEAK -->
        flushInterval="60000"    <!-- 刷新间隔（毫秒） -->
        size="512"               <!-- 最多缓存 512 个对象 -->
        readOnly="true"          <!-- 只读（性能更好） -->
    />

    <!-- 某条 SQL 不使用缓存 -->
    <select id="selectById" resultType="User" useCache="false">
        SELECT * FROM user WHERE id = #{id}
    </select>

    <!-- 执行后刷新缓存 -->
    <update id="update" flushCache="true">
        UPDATE user SET name = #{name} WHERE id = #{id}
    </update>
</mapper>
```

**二级缓存注意事项**：

- 查询结果必须实现 `Serializable` 接口
- 跨 Namespace 的关联查询慎用缓存，可能导致脏数据
- 多表关联查询，一个表更新会导致另一个 Mapper 的缓存不失效

### 3.4 集成 Redis 作为二级缓存

```java
// 自定义 Redis 缓存实现
public class RedisCache implements Cache {

    private final String id;

    public RedisCache(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void putObject(Object key, Object value) {
        RedisTemplate redisTemplate = SpringContextHolder.getBean(RedisTemplate.class);
        redisTemplate.opsForValue().set(key.toString(), value, 30, TimeUnit.MINUTES);
    }

    @Override
    public Object getObject(Object key) {
        RedisTemplate redisTemplate = SpringContextHolder.getBean(RedisTemplate.class);
        return redisTemplate.opsForValue().get(key.toString());
    }

    @Override
    public Object removeObject(Object key) {
        RedisTemplate redisTemplate = SpringContextHolder.getBean(RedisTemplate.class);
        return redisTemplate.delete(key.toString());
    }

    @Override
    public void clear() {
        // 清空当前 namespace 的所有缓存
    }

    @Override
    public int getSize() {
        return 0;
    }
}
```

```xml
<!-- 使用自定义缓存 -->
<cache type="com.example.cache.RedisCache"/>
```

***

> **考察要点提示**
>
> 面试官可能考察：
> 1. **一级缓存失效场景**：哪些操作会导致一级缓存失效？
> 2. **二级缓存的坑**：为什么多表关联查询不适合用二级缓存？
> 3. **缓存和事务**：一级缓存和事务的关系？
> 4. **分布式缓存**：在分布式环境下如何保证缓存一致性？
>
> **常见陷阱**：
> - 认为"开启二级缓存就能提升性能"：二级缓存只适合读多写少、数据不敏感的场景
> - 混淆一级缓存和二级缓存的作用域

***

## 4. 插件机制与拦截器

### 4.1 插件原理

MyBatis 插件基于 **责任链模式 + JDK 动态代理**，允许拦截以下四个核心组件的方法：

| 拦截对象            | 拦截方法                                      | 常见用途            |
| ------------------- | --------------------------------------------- | ------------------- |
| `Executor`          | update/query/flushStatements/commit/rollback   | 分页、缓存、审计      |
| `StatementHandler`  | prepare/parameterize/batch/update/query        | SQL 改写、分表路由    |
| `ParameterHandler`  | getParameterObject/setParameters               | 参数加密、脱敏        |
| `ResultSetHandler`  | handleResultSets/handleOutputParameters        | 结果加密、数据脱敏    |

### 4.2 自定义插件示例

```java
// SQL 执行耗时统计插件
@Intercepts({
    @Signature(
        type = StatementHandler.class,
        method = "query",
        args = {Statement.class, ResultHandler.class}
    )
})
public class SqlCostInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        long startTime = System.currentTimeMillis();
        try {
            return invocation.proceed();  // 执行原方法
        } finally {
            long costTime = System.currentTimeMillis() - startTime;
            // 获取原始 SQL
            StatementHandler handler = (StatementHandler) invocation.getTarget();
            BoundSql boundSql = handler.getBoundSql();
            String sql = boundSql.getSql().replaceAll("\\s+", " ");
            if (costTime > 1000) {
                log.warn("慢SQL [{}ms] -> {}", costTime, sql);
            }
        }
    }

    @Override
    public Object plugin(Object target) {
        // 生成代理对象
        return Plugin.wrap(target, this);
    }
}
```

```xml
<!-- 注册插件 -->
<plugins>
    <plugin interceptor="com.example.plugin.SqlCostInterceptor"/>
</plugins>
```

### 4.3 分页插件原理（PageHelper）

```java
// PageHelper 核心原理：
// 1. 使用 ThreadLocal 存储分页参数
PageHelper.startPage(1, 10);

// 2. Executor 拦截器拦截 query 方法
// 3. 先执行 COUNT 查询获取总数
// SELECT COUNT(*) FROM user WHERE ...

// 4. 再改写 SQL 追加 LIMIT 分页
// SELECT * FROM user WHERE ... LIMIT 0, 10

// 5. 将结果封装为 Page 对象
```

**PageHelper 核心拦截逻辑**：

```java
@Intercepts({
    @Signature(type = Executor.class, method = "query",
               args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
})
public class PageInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 1. 从 ThreadLocal 获取分页参数
        Page page = PageHelper.getLocalPage();
        if (page == null) {
            return invocation.proceed();
        }
        // 2. 执行 COUNT 查询
        Long total = executeCountQuery(...);
        page.setTotal(total);
        // 3. 改写 SQL，追加 LIMIT
        String pageSql = getPageSql(originalSql, page);
        // 4. 执行分页查询
        return executePageQuery(pageSql);
    }
}
```

***

> **考察要点提示**
>
> 面试官可能考察：
> 1. **插件原理**：MyBatis 插件的实现原理是什么？它是如何拦截方法调用的？
> 2. **插件链**：多个插件同时存在时，执行顺序是怎样的？
> 3. **PageHelper 原理**：画一下 PageHelper 的拦截流程图
> 4. **自定义插件**：你写过哪些自定义插件？解决什么问题？
>
> **常见陷阱**：
> - `@Signature` 中 `args` 参数必须和方法签名完全匹配，否则拦截不生效
> - 多个插件执行顺序：按 `plugins` 配置顺序倒序执行（类似栈）

***

## 5. Spring Boot 集成与自动配置

### 5.1 自动配置原理

```
@SpringBootApplication
    │
    ▼
@EnableAutoConfiguration
    │
    ▼
@Import(AutoConfigurationImportSelector.class)
    │
    ▼
读取 META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
    │
    ▼
MybatisAutoConfiguration
    │
    ├── SqlSessionFactory → SqlSessionFactoryBean
    ├── SqlSessionTemplate  → SqlSession → Mapper 执行
    └── MapperScannerConfigurer → 扫描 @Mapper 接口
```

### 5.2 核心配置

```yaml
# application.yml
mybatis:
  # XML 映射文件路径
  mapper-locations: classpath:mapper/**/*.xml
  # 实体类别名包
  type-aliases-package: com.example.entity
  # 配置文件路径
  config-location: classpath:mybatis-config.xml
  configuration:
    # 驼峰命名自动映射
    map-underscore-to-camel-case: true
    # 延迟加载
    lazy-loading-enabled: true
    # 日志实现
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl
```

### 5.3 Mapper 扫描方式

```java
// 方式一：@MapperScan 注解
@SpringBootApplication
@MapperScan("com.example.mapper")
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}

// 方式二：每个 Mapper 接口加 @Mapper
@Mapper
public interface UserMapper {
    User selectById(Long id);
}

// 方式三：MyBatis-Plus 的 BaseMapper
public interface UserMapper extends BaseMapper<User> {
}
```

### 5.4 事务管理

```java
@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    // MyBatis 的事务基于 Spring 事务管理
    @Transactional(rollbackFor = Exception.class)
    public void createUser(User user) {
        userMapper.insert(user);
        // 如果这里抛出异常，上面的 insert 会回滚
        if (user.getName() == null) {
            throw new RuntimeException("用户名不能为空");
        }
    }
}
```

**事务传播行为**：

| 传播行为          | 说明                           |
| ----------------- | ------------------------------ |
| REQUIRED（默认）   | 存在事务则加入，不存在则新建      |
| REQUIRES_NEW      | 总是新建事务，挂起当前事务        |
| NESTED            | 嵌套事务，内部回滚不影响外部      |
| SUPPORTS          | 有事务就加入，没有就不加          |
| NOT_SUPPORTED     | 非事务方式执行，挂起当前事务      |
| NEVER             | 非事务方式执行，有事务则抛异常     |
| MANDATORY         | 必须在事务中执行，否则抛异常       |

***

> **考察要点提示**
>
> 面试官可能考察：
> 1. **自动配置**：Spring Boot 如何自动配置 MyBatis 的 SqlSessionFactory？
> 2. **@MapperScan 原理**：如何扫描并注册 Mapper 接口的 BeanDefinition？
> 3. **事务集成**：MyBatis 如何与 Spring 事务管理集成？
> 4. **多数据源**：如何配置多个数据源并动态切换？

***

## 6. 常见面试题深度解析

### 6.1 MyBatis 和 Hibernate 的区别？

| 对比维度     | MyBatis                        | Hibernate/JPA                    |
| ------------ | ------------------------------ | -------------------------------- |
| 类型         | 半自动 ORM                      | 全自动 ORM                        |
| SQL 控制     | 手写 SQL，灵活度高               | 自动生成 SQL，控制力弱             |
| 学习成本     | 较低，需掌握 SQL 和 XML          | 较高，需掌握 HQL/JPA 规范          |
| 性能优化     | 直接优化 SQL，粒度细             | 需理解 Hibernate 的 SQL 生成规则   |
| 动态 SQL     | 强大的动态 SQL 标签              | Criteria API/JPA Specification   |
| 数据库移植   | 需手动修改 SQL 方言              | 自动适配不同数据库                 |
| 缓存机制     | 一级/二级缓存 + 自定义缓存        | 一级/二级缓存 + 查询缓存            |
| 适用场景     | 复杂查询、高性能要求的项目        | 标准 CRUD 为主的项目               |

### 6.2 MyBatis 是如何防止 SQL 注入的？

MyBatis 使用 `#{}` 时，底层通过 **PreparedStatement** 预编译实现：

```java
// XML: SELECT * FROM user WHERE name = #{name}
// 底层执行：
PreparedStatement ps = connection.prepareStatement("SELECT * FROM user WHERE name = ?");
ps.setString(1, name);  // 参数安全设置，特殊字符会被转义
ResultSet rs = ps.executeQuery();
```

- **预编译 SQL**：SQL 结构在编译阶段确定，参数只是占位符
- **参数转义**：JDBC 驱动会自动转义特殊字符（如 `'` → `\'`）
- **SQL 与参数分离**：恶意注入的 SQL 代码只会被当作字符串参数处理

### 6.3 MyBatis 的分页是如何实现的？

**逻辑分页（RowBounds）**：

```java
// 一次性查出全部数据，在内存中分页
RowBounds rowBounds = new RowBounds(0, 10);
List<User> users = sqlSession.selectList("selectAll", null, rowBounds);
```

- 优点：简单
- 缺点：数据量大时性能差，OOM 风险

**物理分页（PageHelper / 手写 SQL）**：

```java
// 方式一：手写 SQL 分页
// SELECT * FROM user LIMIT #{offset}, #{pageSize}

// 方式二：PageHelper 插件
PageHelper.startPage(1, 10);
List<User> users = userMapper.selectAll();
PageInfo<User> pageInfo = new PageInfo<>(users);
```

- 优点：只查询需要的数据，性能好
- 缺点：不同数据库分页语法不同

### 6.4 什么是 N+1 问题？如何解决？

**问题描述**：

```java
// 查询所有用户（1次查询）
List<User> users = userMapper.selectAll();

// 遍历每个用户，查询其订单（N次查询）
for (User user : users) {
    List<Order> orders = orderMapper.selectByUserId(user.getId());
    // 一共 N+1 次查询！
}
```

**解决方案**：

```xml
<!-- 方案一：JOIN 联表查询（推荐） -->
<resultMap id="UserWithOrders" type="User">
    <id property="id" column="id"/>
    <result property="name" column="name"/>
    <collection property="orders" ofType="Order"
                select="selectOrdersByUserId"
                column="id"
                fetchType="lazy"/>  <!-- 延迟加载 -->
</resultMap>

<!-- 方案二：批量查询 -->
<select id="selectUsersBatch" resultMap="userWithOrdersMap">
    SELECT u.*, o.* FROM user u
    LEFT JOIN `order` o ON u.id = o.user_id
</select>
```

### 6.5 MyBatis 延迟加载原理

```java
// 配置开启延迟加载
<setting name="lazyLoadingEnabled" value="true"/>
<setting name="aggressiveLazyLoading" value="false"/>  // 按需加载

// 原理：返回代理对象，属性被访问时才触发查询
User user = userMapper.selectById(1);
// 此时只查询了 user 表，未查询 orders

List<Order> orders = user.getOrders();  // 触发 orders 查询
```

**核心实现**：MyBatis 使用 **CGLIB/Javassist** 创建代理对象，当调用 `getOrders()` 时：
1. 拦截方法调用
2. 执行关联的 `select` 语句
3. 将查询结果设置到代理对象中
4. 返回结果

### 6.6 MyBatis-Plus 的核心功能和原理

| 功能         | 说明                          |
| ------------ | ----------------------------- |
| BaseMapper   | 内置通用 CRUD 方法             |
| 条件构造器    | Wrapper 链式构建查询条件        |
| 分页插件      | 自动分页，拦截器实现            |
| 乐观锁        | `@Version` 注解，自动版本管理   |
| 逻辑删除      | `@TableLogic` 注解，标记删除    |
| 自动填充      | `@TableField(fill = ...)` 自动填充时间等 |
| 代码生成器    | 生成 Mapper/Service/Controller |

```java
// 条件构造器示例
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.eq(User::getName, "张三")
       .gt(User::getAge, 18)
       .orderByDesc(User::getCreateTime);
List<User> users = userMapper.selectList(wrapper);
```

### 6.7 如何处理大数据量查询？

| 方案       | 说明                             | 适用场景           |
| ---------- | -------------------------------- | ------------------ |
| 分页查询    | LIMIT offset, size               | 前端展示           |
| 游标查询    | 基于 ID 递增，WHERE id > lastId   | 数据导出、批处理    |
| 流式查询    | ResultHandler 逐条处理            | 百万级数据处理      |
| 分批查询    | 按时间范围/ID 范围分批             | 定时任务、数据迁移  |

```java
// 流式查询（游标）
sqlSession.select("selectAll", resultContext -> {
    User user = resultContext.getResultObject();
    // 逐条处理，不会一次性加载到内存
    processUser(user);
});
```

***

## 7. 模拟面试问答（高频题）

### 7.1 初级题（1-3年经验）

#### 问答1: #{} 和 ${} 的区别是什么？

**回答要点**：

1. `#{}` 是预编译占位符，防 SQL 注入；`${}` 是字符串替换，不防注入
2. `#{}` 底层使用 PreparedStatement，参数自动加引号；`${}` 直接拼接
3. 使用场景：条件值用 `#{}`，动态表名/列名/ORDER BY 用 `${}`（需白名单校验）
4. 性能：`#{}` 预编译可复用执行计划，更快

#### 问答2: MyBatis 一级缓存和二级缓存有什么区别？

**回答要点**：

1. **作用域**：一级缓存是 SqlSession 级别，二级缓存是 Namespace/Mapper 级别
2. **默认开启**：一级缓存默认开启，二级缓存需手动配置
3. **生命周期**：一级缓存随 SqlSession 关闭而清空，二级缓存整个应用生命周期
4. **共享范围**：一级缓存同一 SqlSession 内共享，二级缓存跨 SqlSession 共享
5. **使用场景**：一级缓存自动生效；二级缓存适合读多写少、数据不敏感的场景

#### 问答3: 如何获取自动生成的主键？

**回答要点**：

```xml
<!-- 方式一：useGeneratedKeys -->
<insert id="insert" useGeneratedKeys="true" keyProperty="id">
    INSERT INTO user (name, age) VALUES (#{name}, #{age})
</insert>

<!-- 方式二：selectKey -->
<insert id="insert">
    <selectKey keyProperty="id" resultType="long" order="AFTER">
        SELECT LAST_INSERT_ID()
    </selectKey>
    INSERT INTO user (name, age) VALUES (#{name}, #{age})
</insert>
```

#### 问答4: MyBatis 有哪些 Executor 执行器？区别是什么？

**回答要点**：

1. **SimpleExecutor**（默认）：每次执行都创建新的 Statement，用完关闭
2. **ReuseExecutor**：复用 Statement，缓存起来重复使用
3. **BatchExecutor**：批量执行，所有 update 操作批量提交
4. **CachingExecutor**：装饰器，在以上 Executor 基础上增加二级缓存功能
5. 配置方式：`<setting name="defaultExecutorType" value="BATCH"/>`

### 7.2 中级题（3-5年经验）

#### 问答5: MyBatis 插件原理是什么？如何实现一个分页插件？

**回答要点**：

1. **原理**：基于责任链模式 + JDK 动态代理
2. **可拦截对象**：Executor、StatementHandler、ParameterHandler、ResultSetHandler
3. **分页插件实现**：
   - 拦截 Executor.query() 方法
   - 从 ThreadLocal 获取分页参数
   - 先执行 COUNT 查询获取总数
   - 改写原始 SQL，追加 LIMIT 子句
   - 执行分页查询，将结果封装为 Page 对象
4. **注意事项**：多个插件按配置顺序的倒序执行（栈结构）

#### 问答6: 什么是 N+1 问题？如何排查和解决？

**回答要点**：

1. **问题**：查询主表（1次）+ 遍历查关联表（N次），共 N+1 次查询
2. **排查**：开启 SQL 日志，观察是否有大量重复的关联查询
3. **解决方案**：
   - JOIN 联表查询（一次性查出所有数据）
   - 延迟加载 + 按需加载（`aggressiveLazyLoading=false`）
   - 批量查询（先查所有 ID，再一次性查关联数据）
4. **实际案例**：查询订单列表时，每个订单都要查用户信息

#### 问答7: MyBatis 如何与 Spring 事务集成？

**回答要点**：

1. MyBatis 通过 `SqlSessionTemplate` 与 Spring 事务绑定
2. `SqlSessionTemplate` 是线程安全的，内部使用 `SqlSessionHolder` 持有 `SqlSession`
3. 同一个事务中，多次调用 Mapper 方法共享同一个 `SqlSession`
4. 事务提交时，`SqlSession` 自动提交或回滚
5. `@Transactional` 通过 AOP 代理控制事务边界

#### 问答8: 如何实现多数据源动态切换？

**回答要点**：

```java
// 1. 定义数据源枚举
public enum DataSourceType {
    MASTER, SLAVE
}

// 2. ThreadLocal 存储当前数据源
public class DynamicDataSourceContextHolder {
    private static final ThreadLocal<DataSourceType> contextHolder = new ThreadLocal<>();
    public static void set(DataSourceType type) { contextHolder.set(type); }
    public static DataSourceType get() { return contextHolder.get(); }
    public static void clear() { contextHolder.remove(); }
}

// 3. 继承 AbstractRoutingDataSource
public class DynamicDataSource extends AbstractRoutingDataSource {
    @Override
    protected Object determineCurrentLookupKey() {
        return DynamicDataSourceContextHolder.get();
    }
}

// 4. AOP 切换数据源
@Aspect
@Component
public class DataSourceAspect {
    @Before("@annotation(readOnly)")
    public void before(ReadOnly readOnly) {
        DynamicDataSourceContextHolder.set(DataSourceType.SLAVE);
    }
    @After("@annotation(readOnly)")
    public void after(ReadOnly readOnly) {
        DynamicDataSourceContextHolder.clear();
    }
}
```

### 7.3 高级题（5年以上经验）

#### 问答9: 如果有几十万条数据需要批量插入，如何优化？

**回答要点**：

1. **分批插入**：每批 500-1000 条，避免单次 SQL 过长或内存溢出
2. **使用 BatchExecutor**：`sqlSessionFactory.openSession(ExecutorType.BATCH)`
3. **关闭自动提交**：`sqlSession.commit()` 手动批量提交
4. **使用 foreach 批量语法**：`INSERT INTO ... VALUES (...), (...), (...)`
5. **JDBC 参数优化**：`rewriteBatchedStatements=true`（MySQL）
6. **避免大事务**：大数据量场景下事务回滚成本极高
7. **异步处理**：MQ + 分批消费写入

```java
// 优化后的批量插入
SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH, false);
try {
    UserMapper mapper = session.getMapper(UserMapper.class);
    for (int i = 0; i < list.size(); i++) {
        mapper.insert(list.get(i));
        if (i % 1000 == 0) {
            session.commit();
            session.clearCache();  // 防止一级缓存过大
        }
    }
    session.commit();
} finally {
    session.close();
}
```

#### 问答10: 如何设计一个通用的 MyBatis 审计插件？

**回答要点**：

```java
@Intercepts({
    @Signature(type = Executor.class, method = "update",
               args = {MappedStatement.class, Object.class})
})
public class AuditInterceptor implements Interceptor {

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object parameter = invocation.getArgs()[1];

        // 判断 SQL 类型
        SqlCommandType commandType = ms.getSqlCommandType();

        // 自动填充审计字段
        if (parameter instanceof BaseEntity) {
            BaseEntity entity = (BaseEntity) parameter;
            if (commandType == SqlCommandType.INSERT) {
                entity.setCreateTime(new Date());
                entity.setCreateBy(getCurrentUser());
            }
            entity.setUpdateTime(new Date());
            entity.setUpdateBy(getCurrentUser());
        }

        // 记录审计日志
        BoundSql boundSql = ms.getBoundSql(parameter);
        log.info("SQL: {}, 参数: {}, 操作人: {}",
                 boundSql.getSql(), parameter, getCurrentUser());

        return invocation.proceed();
    }
}
```

#### 问答11: MyBatis 源码中如何防止 SQL 注入的？从源码层面解释

**回答要点**：

1. `#{}` 解析时，`SqlSourceBuilder` 将 `#{}` 替换为 `?` 占位符
2. 生成 `StaticSqlSource`（静态 SQL）+ `ParameterMapping` 列表
3. `DefaultParameterHandler.setParameters()` 中通过 `TypeHandler` 安全设置参数
4. `PreparedStatement.setXxx()` 由 JDBC 驱动完成最终转义
5. `${}` 由 `TextSqlNode` 直接拼接，不做任何转义处理，所以不安全

**源码关键类**：

- `SqlSourceBuilder`：解析 `#{}`，构建 `ParameterMapping`
- `GenericTokenParser`：通用的 `#{}` / `${}` 标记解析器
- `DefaultParameterHandler`：设置 PreparedStatement 参数
- `TypeHandler`：Java 类型到 JDBC 类型的转换

#### 问答12: 如果线上 MySQL 数据库 CPU 飙升，如何排查是否与 MyBatis 有关？

**回答要点**：

1. **开启慢 SQL 日志**：`<setting name="logImpl" value="SLF4J"/>` + 慢 SQL 拦截器
2. **查看 MySQL 慢查询日志**：`SHOW FULL PROCESSLIST` 查看正在执行的 SQL
3. **分析 SQL 执行计划**：`EXPLAIN` 分析索引使用情况
4. **常见问题**：
   - 缺少索引导致全表扫描
   - N+1 查询导致大量 SQL 执行
   - 动态 SQL 中 `<if>` 条件失误导致查询条件失效
   - 大事务持有锁过久
   - 批量操作未分批导致长事务
5. **MyBatis 层面优化**：
   - 检查 `resultMap` 中关联查询是否有 N+1 问题
   - 检查一级缓存是否导致数据不一致
   - 检查是否使用了 `${}` 导致无法使用预编译

---

> **最后建议**
>
> MyBatis 面试越来越注重**源码理解**和**实际优化经验**。除了掌握基本用法，建议：
> 1. 阅读 MyBatis 核心源码（SqlSession、Executor、插件机制）
> 2. 在项目中积累性能优化经验（慢 SQL 排查、批量操作、缓存策略）
> 3. 理解 MyBatis-Plus 的增强功能和原理
> 4. 能结合具体业务场景，选择合适的方案