# Spring + Dubbo 测试指南

## 测试文件说明

已创建以下测试类来覆盖 `createUser` 方法和相关功能：

### 1. **单元测试** (`UserControllerTest.java`)

针对 `UserController.createUser()` 方法的详细测试，包含：

#### ✅ 正常场景测试
- `testCreateUser_Success` - 成功创建用户
- `testCreateUser_CompleteData` - 完整数据创建

#### ✅ 异常场景测试
- `testCreateUser_EmptyUsername` - 用户名为空
- `testCreateUser_EmptyEmail` - 邮箱为空
- `testCreateUser_InvalidAge` - 年龄无效（负数）
- `testCreateUser_ServiceException` - Service 层异常
- `testCreateUser_InvalidEmailFormat` - 邮箱格式错误
- `testCreateUser_DuplicateUsername` - 用户名重复

#### ✅ 边界值测试
- `testCreateUser_MaxAge` - 最大年龄（150 岁）
- `testCreateUser_MinAge` - 最小年龄（0 岁）

### 2. **集成测试** (`UserControllerIntegrationTest.java`)

测试整个 Controller 的完整流程：

- ✅ 创建用户完整流程
- ✅ 获取所有用户
- ✅ 更新用户信息
- ✅ 删除用户
- ✅ 根据用户名查询
- ✅ 查询不存在的用户
- ✅ 按年龄范围查询

## 运行测试

### 方式一：运行单个测试类

```bash
# 运行单元测试
cd spring
mvn test -Dtest=UserControllerTest

# 运行集成测试
mvn test -Dtest=UserControllerIntegrationTest
```

### 方式二：运行特定测试方法

```bash
# 运行特定的测试方法
mvn test -Dtest=UserControllerTest#testCreateUser_Success

# 运行所有创建用户的测试
mvn test -Dtest=UserControllerTest#testCreateUser*
```

### 方式三：运行所有测试

```bash
mvn test
```

## 测试覆盖率

当前测试覆盖了以下场景：

### UserController.createUser() 方法
- ✅ 正常创建流程
- ✅ 参数验证（用户名、邮箱、年龄）
- ✅ 异常处理
- ✅ 边界值验证
- ✅ HTTP 状态码验证
- ✅ 响应数据验证

### 其他 Controller 方法
- ✅ GET /api/users - 获取所有用户
- ✅ GET /api/users/{id} - 获取单个用户
- ✅ PUT /api/users/{id} - 更新用户
- ✅ DELETE /api/users/{id} - 删除用户
- ✅ GET /api/users/username/{username} - 按用户名查询
- ✅ GET /api/users/age-range - 按年龄范围查询

## 测试技术要点

### 1. MockMvc 使用

```java
@Autowired
private MockMvc mockMvc;

// 发送 POST 请求
mockMvc.perform(post("/api/users")
        .contentType(MediaType.APPLICATION_JSON)
        .content(objectMapper.writeValueAsString(user)))
    .andExpect(status().isCreated())
    .andExpect(jsonPath("$.id").value(1));
```

### 2. Mockito Mock 使用

```java
@MockBean
private UserService userService;

// 模拟服务行为
given(userService.save(any(User.class))).willReturn(savedUser);

// 模拟异常
given(userService.save(any(User.class)))
    .willThrow(new RuntimeException("错误"));

// 验证调用次数
verify(userService, times(1)).save(any(User.class));
```

### 3. JSON 断言

```java
.andExpect(jsonPath("$.id").value(1))
.andExpect(jsonPath("$.username").value("张三"))
.andExpect(jsonPath("$.email").value("zhangsan@example.com"))
.andExpect(jsonPath("$.age").value(25))
```

## 测试最佳实践

### ✅ 好的测试习惯

1. **测试方法命名清晰**
   ```java
   @DisplayName("测试创建用户 - 成功")
   void testCreateUser_Success()
   ```

2. **使用 @BeforeEach 准备数据**
   ```java
   @BeforeEach
   void setUp() {
       testUser = new User(1L, "张三", "zhangsan@example.com", 25);
   }
   ```

3. **验证所有重要字段**
   ```java
   .andExpect(jsonPath("$.id").value(1))
   .andExpect(jsonPath("$.username").value("张三"))
   ```

4. **Mock 对象行为验证**
   ```java
   verify(userService, times(1)).save(any(User.class));
   ```

5. **包含边界值和异常测试**
   ```java
   testCreateUser_EmptyUsername()
   testCreateUser_MaxAge()
   ```

### ❌ 避免的问题

1. 不要依赖数据库状态
2. 不要测试多个不相关的功能
3. 不要忘记清理测试数据
4. 不要忽略异常情况的测试

## 扩展建议

可以进一步添加的测试：

1. **性能测试**
   ```java
   @Test
   void testCreateUser_Performance() {
       long startTime = System.currentTimeMillis();
       // 创建 1000 个用户
       long endTime = System.currentTimeMillis();
       assertTrue(endTime - startTime < 5000);
   }
   ```

2. **并发测试**
   ```java
   @Test
   void testCreateUser_Concurrent() throws Exception {
       // 同时创建多个同名用户
   }
   ```

3. **大数据量测试**
   ```java
   @Test
   void testGetUsers_LargeData() {
       // 创建 10000 个用户后查询
   }
   ```

## 常见问题

### Q: 为什么使用 @WebMvcTest 而不是 @SpringBootTest?

**A:** 
- `@WebMvcTest` 只加载 Web 层配置，速度更快
- `@SpringBootTest` 加载完整应用上下文，适合集成测试

### Q: 如何调试失败的测试？

**A:**
1. 使用 `.andDo(print())` 打印详细请求响应
2. 在 IDE 中运行测试并查看输出
3. 添加日志断言

### Q: 如何处理异步测试？

**A:**
```java
@Test
void testAsync() throws Exception {
    CountDownLatch latch = new CountDownLatch(1);
    // 执行异步操作
    latch.await(5, TimeUnit.SECONDS);
}
```

---

**创建时间**: 2026-03-30  
**作者**: ijay997
