# Spring Boot 演示项目

这是一个基于 Spring Boot 的 RESTful API 演示项目，用于学习和展示 Spring Boot 的核心功能。

## 技术栈

- **Spring Boot**: 2.7.18
- **Spring Data JPA**: 数据持久层框架
- **H2 Database**: 内存数据库（用于演示）
- **Lombok**: 简化 Java 代码
- **JUnit 5**: 单元测试框架

## 项目结构

```
spring/
├── src/main/java/com/qq/ijay997/
│   ├── SpringDemoApplication.java    # 主启动类
│   ├── config/
│   │   └── DataInitializer.java      # 数据初始化器
│   ├── controller/
│   │   └── UserController.java       # REST API 控制器
│   ├── entity/
│   │   └── User.java                 # 用户实体类
│   ├── repository/
│   │   └── UserRepository.java       # 数据访问层
│   └── service/
│       ├── UserService.java          # 服务接口
│       └── impl/
│           └── UserServiceImpl.java  # 服务实现类
├── src/main/resources/
│   └── application.properties        # 配置文件
└── src/test/java/
    └── SpringDemoApplicationTests.java  # 测试类
```

## 快速开始

### 1. 构建项目

在项目根目录执行：
```bash
mvn clean install
```

### 2. 运行应用

方式一：通过 IDE 运行
- 打开 `SpringDemoApplication.java`
- 点击运行按钮

方式二：通过 Maven 命令
```bash
cd spring
mvn spring-boot:run
```

### 3. 访问应用

应用启动后，默认端口为 `8080`

#### API 端点

**1. 获取所有用户**
```bash
GET http://localhost:8080/api/users
```

**2. 根据 ID 获取用户**
```bash
GET http://localhost:8080/api/users/{id}
```

**3. 根据用户名获取用户**
```bash
GET http://localhost:8080/api/users/username/{username}
```

**4. 根据年龄范围查询用户**
```bash
GET http://localhost:8080/api/users/age-range?minAge=20&maxAge=30
```

**5. 创建用户**
```bash
POST http://localhost:8080/api/users
Content-Type: application/json

{
  "username": "新用户",
  "email": "newuser@example.com",
  "age": 25
}
```

**6. 更新用户**
```bash
PUT http://localhost:8080/api/users/{id}
Content-Type: application/json

{
  "username": "更新后的用户名",
  "email": "updated@example.com",
  "age": 30
}
```

**7. 删除用户**
```bash
DELETE http://localhost:8080/api/users/{id}
```

#### H2 数据库控制台

访问 H2 控制台查看数据库：
```
URL: http://localhost:8080/h2-console
JDBC URL: jdbc:h2:mem:testdb
Username: sa
Password: (空)
```

## 运行测试

执行所有测试：
```bash
cd spring
mvn test
```

或在 IDE 中右键点击测试类运行。

## 特性说明

### 1. 自动数据初始化

应用启动时会自动插入 5 条演示数据：
- 张三 (25 岁)
- 李四 (30 岁)
- 王五 (28 岁)
- 赵六 (35 岁)
- 小明 (22 岁)

### 2. RESTful API 设计

- 使用标准的 HTTP 方法（GET, POST, PUT, DELETE）
- 返回合适的 HTTP 状态码（200, 201, 404, 204 等）
- RESTful 路径设计

### 3. JPA 数据访问

- 继承 JpaRepository 获得 CRUD 操作
- 自定义查询方法（findByUsername, findByAgeBetween）
- 自动 SQL 生成

### 4. 异常处理

- 资源不存在返回 404
- 数据验证失败返回 400
- 成功创建返回 201

## 配置说明

主要配置在 `application.properties` 中：

- **server.port**: 服务器端口（默认 8080）
- **spring.datasource.url**: H2 数据库连接
- **spring.jpa.hibernate.ddl-auto**: 自动建表策略
- **spring.h2.console.enabled**: H2 控制台开关

## 学习要点

通过这个项目，你可以学习到：

1. ✅ Spring Boot 项目的创建和配置
2. ✅ RESTful API 的设计与实现
3. ✅ Spring Data JPA 的使用
4. ✅ 依赖注入和控制反转（DI/IoC）
5. ✅ 分层架构（Controller - Service - Repository）
6. ✅ 内存数据库 H2 的使用
7. ✅ 单元测试编写
8. ✅ Lombok 简化代码

## 常见问题

**Q: 为什么使用 H2 数据库？**
A: H2 是内存数据库，无需安装配置，适合学习和演示场景。

**Q: 如何切换到 MySQL？**
A: 修改 `pom.xml` 添加 MySQL 依赖，修改 `application.properties` 中的数据源配置。

**Q: 启动失败怎么办？**
A: 检查端口 8080 是否被占用，检查 JDK 版本是否为 8 或以上。

## 扩展练习

建议尝试以下扩展：

1. 添加全局异常处理器
2. 添加数据验证（@Valid）
3. 添加分页查询功能
4. 添加 Swagger 文档
5. 集成 Redis 缓存
6. 添加登录认证功能

---

**作者**: ijay997  
**创建时间**: 2026-03-30
