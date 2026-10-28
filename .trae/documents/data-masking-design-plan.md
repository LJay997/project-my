# 数据脱敏功能设计方案（ThreadLocal 权限上下文 + JSON / 文件双场景）

> 状态：待评审
> 范围：`spring` 模块（Spring Boot 2.7.18 / Java 11 / Jackson / EasyExcel 4.0.3）

## 一、目标与总体思路

在不改动数据库、实体和业务逻辑的前提下，实现**基于当前用户权限的数据脱敏**：

1. 用 mock 的请求头模拟登录用户，把用户身份与权限码放入 `ThreadLocal`，请求结束立即清理；
2. **JSON 响应场景**：字段上声明 `@Sensitive`，由自定义 Jackson 序列化器在序列化时按权限决定输出明文还是脱敏值；
3. **文件响应场景**：导出 Excel 前，用统一的注解处理器反射处理导出 DTO，复用与 JSON 完全相同的注解和脱敏规则；
4. **默认拒绝（fail-closed）**：没有携带权限或权限为空时，所有敏感字段一律脱敏，避免"忘记配置导致泄露"。

核心原则：

- **脱敏属于展示层能力**：数据库存原文，实体不感知脱敏；
- **权限判断只发生在一处**：`UserContextHolder.hasPermission(code)`；
- **规则声明在字段上**：JSON 和文件共用 `@Sensitive(type, permission)`，不出现两套配置；
- **不依赖真实鉴权框架**：通过请求头 mock 用户与权限码，后续可无缝替换为真实登录上下文。

## 二、现状分析（基于代码探索）

| 现状 | 位置 | 对本方案的意义 |
|---|---|---|
| 已有 `ThreadLocal` + `finally clear` 的实践 | [TraceIdCleanupFilter.java](file:///Users/jay/Documents/ideaProject/demo/project-my/spring/src/main/java/com/qq/ijay997/config/TraceIdCleanupFilter.java)、[DevRequestInterceptor.java](file:///Users/jay/Documents/ideaProject/demo/project-my/spring/src/main/java/com/qq/ijay997/config/DevRequestInterceptor.java) | 直接沿用"过滤器设置上下文、finally 清理"的生命周期模式 |
| JSON 接口返回实体/DTO | [UserController.java](file:///Users/jay/Documents/ideaProject/demo/project-my/spring/src/main/java/com/qq/ijay997/controller/UserController.java)、[UserDTO.java](file:///Users/jay/Documents/ideaProject/demo/project-my/spring/src/main/java/com/qq/ijay997/dto/UserDTO.java) | DTO 可作为 JSON 脱敏演示载体 |
| Excel 导出已有固定链路 | [ExcelController.java](file:///Users/jay/Documents/ideaProject/demo/project-my/spring/src/main/java/com/qq/ijay997/controller/ExcelController.java)、[ExcelServiceImpl.java](file:///Users/jay/Documents/ideaProject/demo/project-my/spring/src/main/java/com/qq/ijay997/service/impl/ExcelServiceImpl.java) | 在 `exportUsers` 组装完 `List<UserExcel>` 后、写文件前插入脱敏处理即可 |
| 导出模型已有字段注解基础 | [UserExcel.java](file:///Users/jay/Documents/ideaProject/demo/project-my/spring/src/main/java/com/qq/ijay997/dto/UserExcel.java) | 在 username / email 上追加 `@Sensitive` |
| Jackson 由 `spring-boot-starter-web` 自带，AOP 可用但无需引入新依赖 | `spring/pom.xml` | 不需要新增依赖 |
| 现有 [ExcelServiceTest](file:///Users/jay/Documents/ideaProject/demo/project-my/spring/src/test/java/com/qq/ijay997/service/ExcelServiceTest.java) 断言导出邮箱为明文 | 测试代码 | 加脱敏后需在该测试中显式授予权限，否则断言需改为脱敏值 |

## 三、设计思想（核心）

### 3.1 分层与数据流

```
请求
 │  Header: X-Role / X-Permissions（mock）
 ▼
MockUserContextFilter (OncePerRequestFilter)
 │  - 解析角色/权限码
 │  - UserContextHolder.set(principal)
 ▼
Controller / Service（同一线程）
 ├── 返回 JSON ──► Jackson 序列化
 │                  └─ @Sensitive 字段 → SensitiveJsonSerializer
 │                       └─ UserContextHolder.hasPermission(?)
 │                            ├─ 有权限 → 输出原文
 │                            └─ 无权限 → MaskRules.mask(type, value)
 │
 └── 导出文件 ──► SensitiveDataMasker.maskBeans(rows)
                    └─ 反射读取 @Sensitive → 同一套权限判断与脱敏规则
                         └─ EasyExcel 写已脱敏的 DTO
 ▼
finally: UserContextHolder.clear()
```

**为什么 JSON 选 Jackson 序列化而不是统一后处理**：Jackson 自定义 `ContextualSerializer` 天然覆盖嵌套对象、集合元素和泛型，无需手动遍历对象图；脱敏只发生在目标字段的序列化瞬间，复杂度最低、性能最好。权限读取只依赖 ThreadLocal，业务代码零侵入。

**为什么文件选统一注解处理器而不是 Converter**：EasyExcel 的 `Converter` 无法从字段注解中携带 `SensitiveType` 与 `permission`，且每个敏感字段都要单独指定 Converter，规则会分散。导出前对 DTO 做一次反射处理，能 100% 复用 JSON 的规则，新增字段只需加注解。

**为什么权限码而不是角色**：注解上直接声明 `permission = "user:email:read"`，与未来真实权限系统对齐；角色只用于 mock 时映射成一组权限码，脱敏核心不感知角色。

### 3.2 ThreadLocal 上下文

- `UserPrincipal`：immutable，字段 `userId`、`username`、`role`、`Set<String> permissions`；
- `UserContextHolder`：
  - `set(UserPrincipal)` / `get()` / `clear()`；
  - `hasPermission(String code)`；
  - `get()` 为 null 时返回匿名 principal（空权限），保证 **fail-closed**；
  - ThreadLocal 必须 `remove()`，避免线程池复用导致权限串号与内存泄漏。
- 生命周期：Filter `doFilterInternal` 中 `try { set(); chain.doFilter(); } finally { clear(); }`。

**边界说明**：本方案面向同步请求。若后续使用 `@Async`、CompletableFuture、消息消费，需要手动传递上下文（`TaskDecorator` 或显式参数），不在本次范围内。

### 3.3 权限码与 mock 规则

| 权限码 | 含义 |
|---|---|
| `user:name:read` | 允许看到用户名明文 |
| `user:email:read` | 允许看到邮箱明文 |
| `user:phone:read` | 允许看到手机号明文 |

mock 请求头（由 `MockUserContextFilter` 解析）：

| 请求头 | 示例 | 行为 |
|---|---|---|
| `X-Role` | `ADMIN` / `OPERATOR` / `GUEST` | 映射到预置权限集合，缺省 `GUEST` |
| `X-Permissions` | `user:email:read,user:phone:read` | 显式指定权限码，优先级高于 `X-Role` |
| `X-User-Id` | `1001` | 用户标识，缺省 `1001` |

角色预置：

- `ADMIN` → 全部三个权限码；
- `OPERATOR` → 仅 `user:name:read`；
- `GUEST` → 空集合（全部脱敏）。

### 3.4 脱敏策略

`SensitiveType` 与规则（仅作用于 `String` 字段；null 原样返回 null）：

| 类型 | 规则 | 示例 |
|---|---|---|
| `EMAIL` | 保留本地部分首字符 + `***` + 原域名 | `zhangsan@example.com` → `z***@example.com` |
| `PHONE` | 前 3 位 + `****` + 后 4 位（不足 7 位全遮） | `13812345678` → `138****5678` |
| `NAME` | 首字符 + `*`（后续字符全遮） | `张三` → `张*` |
| `ID_CARD` | 前 6 位 + `********` + 后 4 位 | `110101199001011234` → `110101********1234` |
| `BANK_CARD` | 前 4 位 + `********` + 后 4 位 | `6222021234561234` → `6222********1234` |
| `ADDRESS` | 保留前 6 个字符 + `***` | `广东省深圳市南山区科技园` → `广东省深圳市南***` |

规则解析失败（格式不符、长度不足）时统一降级为 `***`，不抛异常。

### 3.5 注解设计

```java
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@JacksonAnnotationsInside
@JsonSerialize(using = SensitiveJsonSerializer.class)
public @interface Sensitive {
    SensitiveType type();
    String permission();
}
```

借助 `@JacksonAnnotationsInside`，字段只需写 `@Sensitive(type = EMAIL, permission = "user:email:read")`，Jackson 会自动套用对应序列化器，无需再重复写 `@JsonSerialize`。

## 四、具体改动清单

### 4.1 新增包 `com.qq.ijay997.masking`

| 文件 | 职责与关键签名 |
|---|---|
| `SensitiveType.java` | 脱敏类型枚举：EMAIL / PHONE / NAME / ID_CARD / BANK_CARD / ADDRESS |
| `Sensitive.java` | 敏感字段注解，见 3.5 |
| `PermissionCodes.java` | 权限码常量（`USER_NAME_READ` 等）+ 角色→权限集合的预置映射 |
| `UserPrincipal.java` | 当前用户模型（userId / username / role / permissions），提供 `hasPermission` |
| `UserContextHolder.java` | static ThreadLocal：`set/get/clear/hasPermission`，null 时返回匿名空权限 |
| `MaskRules.java` | 纯函数静态工具：`mask(SensitiveType, String)`，便于单测与复用 |
| `SensitiveJsonSerializer.java` | `extends JsonSerializer<String> implements ContextualSerializer`；`createContextual` 读取 `@Sensitive`，`serialize` 按权限输出原文或 `MaskRules.mask` 结果 |
| `SensitiveDataMasker.java` | `@Component`；`maskBeans(Iterable<?>)` / `maskBean(Object)`：反射遍历字段，读取 `@Sensitive`，无权限时写回脱敏值（只处理当前层字段，不递归嵌套对象） |

### 4.2 新增 `config/MockUserContextFilter.java`

- `@Component` + `@Order(2)`（在 `TraceIdCleanupFilter` 的 `@Order(1)` 之后）；
- 解析 `X-User-Id`、`X-Role`、`X-Permissions`；
- `try/finally` 中 `UserContextHolder.set()` / `clear()`。

### 4.3 新增 `controller/MaskingDemoController.java`

| 接口 | 行为 |
|---|---|
| `GET /api/demo/masking/users` | 调用 `userService.findAll()`，映射为 `List<UserDTO>` 返回，演示 JSON 脱敏 |
| `GET /api/demo/masking/me` | 返回当前 `UserPrincipal`（userId、role、permissions），用于观察 ThreadLocal 与 mock 权限 |

### 4.4 修改现有文件

| 文件 | 改动 |
|---|---|
| [UserDTO.java](file:///Users/jay/Documents/ideaProject/demo/project-my/spring/src/main/java/com/qq/ijay997/dto/UserDTO.java) | `username` 加 `@Sensitive(type = NAME, permission = "user:name:read")`；`email` 加 `@Sensitive(type = EMAIL, permission = "user:email:read")`；`phone` 加 `@Sensitive(type = PHONE, permission = "user:phone:read")` |
| [UserExcel.java](file:///Users/jay/Documents/ideaProject/demo/project-my/spring/src/main/java/com/qq/ijay997/dto/UserExcel.java) | `username`、`email` 加与 DTO 相同的 `@Sensitive`（文件与 JSON 共用规则） |
| [ExcelServiceImpl.java](file:///Users/jay/Documents/ideaProject/demo/project-my/spring/src/main/java/com/qq/ijay997/service/impl/ExcelServiceImpl.java) | 注入 `SensitiveDataMasker`；在 Sheet1 的 `rows` 组装完成后、`writer.write(...)` 之前调用 `sensitiveDataMasker.maskBeans(rows)` |
| [ExcelServiceTest.java](file:///Users/jay/Documents/ideaProject/demo/project-my/spring/src/test/java/com/qq/ijay997/service/ExcelServiceTest.java) | 导出用例前置 `UserContextHolder.set(有 email/name 权限的 principal)`，保持原明文断言；`finally` 清理。另新增一个"无权限 → 邮箱脱敏"用例 |

> 注意：不改动 `User` 实体，避免持久化模型与展示逻辑耦合；`/api/users` 原有行为保持不变，脱敏演示走独立接口。

### 4.5 新增测试

| 测试类 | 覆盖点 |
|---|---|
| `MaskRulesTest` | 6 种脱敏类型的边界（null、空串、长度不足、格式异常降级 `***`） |
| `SensitiveJsonSerializerTest` | 用 `ObjectMapper` 序列化 `UserDTO`：无权限 → 脱敏；有权限 → 明文；嵌套/集合中的 DTO 同样生效 |
| `SensitiveDataMaskerTest` | `maskBeans` 在无权限/有权限下对 `UserExcel` 的反射改写结果 |
| `MaskingDemoControllerTest` | `@WebMvcTest` + `@Import(MockUserContextFilter.class)`：`X-Role: GUEST/ADMIN`、`X-Permissions` 覆盖角色、无请求头默认脱敏 |
| 更新 `ExcelServiceTest` | Excel 导出在有/无权限时的单元格值 |

## 五、关键决策与备选方案

| 决策 | 选择 | 备选与不选原因 |
|---|---|---|
| JSON 脱敏时机 | Jackson `ContextualSerializer` | `ResponseBodyAdvice` 需自行处理泛型/嵌套/集合，容易漏字段；AOP 需处理多种返回类型且侵入业务 |
| 文件脱敏方式 | 导出前统一注解处理器 | EasyExcel `Converter` 无法从注解携带类型与权限码，规则分散；手写 setter 易漏 |
| 权限模型 | 权限码级 | 角色级不够细，无法表达"能看邮箱但不能看手机号"；接口级会把规则从字段移到配置，新增字段易漏 |
| 无权限时默认行为 | 默认脱敏（fail-closed） | 默认明文一旦请求头缺失就泄露数据 |
| 脱敏范围 | 仅 `String` 字段 | 当前需求字段均为字符串；数字/日期脱敏后续可扩展 |
| 实体是否加注解 | 不加，只在 DTO / Excel 模型加 | 避免实体与展示层耦合，也避免影响既有 `/api/users` 接口 |
| 异步场景 | 暂不支持自动传递 | 演示项目为同步请求；后续可通过 `TaskDecorator` 扩展 |

## 六、验证步骤

### 6.1 自动化测试

```bash
mvn -pl spring test \
  -Dtest='MaskRulesTest,SensitiveJsonSerializerTest,SensitiveDataMaskerTest,MaskingDemoControllerTest,ExcelServiceTest'
```

预期全部通过；`ExcelControllerTest` 等既有测试不受影响（Mock 了 `ExcelService`）。

### 6.2 手工验证（`application.properties` 中端口为 8082）

```bash
# 1. 无权限（默认 GUEST）：用户名/邮箱均脱敏
curl -s http://localhost:8082/api/demo/masking/users

# 2. OPERATOR：仅用户名明文，邮箱脱敏
curl -s -H 'X-Role: OPERATOR' http://localhost:8082/api/demo/masking/users

# 3. ADMIN：全部明文
curl -s -H 'X-Role: ADMIN' http://localhost:8082/api/demo/masking/users

# 4. 显式权限码覆盖角色：只有邮箱明文
curl -s -H 'X-Role: GUEST' -H 'X-Permissions: user:email:read' \
  http://localhost:8082/api/demo/masking/users

# 5. 查看当前 mock 用户与权限
curl -s -H 'X-Role: OPERATOR' http://localhost:8082/api/demo/masking/me

# 6. 文件场景：无权限导出的 email 列应为脱敏值
curl -s -H 'X-Role: GUEST' -o masked.xlsx http://localhost:8082/api/excel/users/export

# 7. 文件场景：有权限导出的 email 列应为明文
curl -s -H 'X-Role: ADMIN' -o plain.xlsx http://localhost:8082/api/excel/users/export
```

### 6.3 验收标准

- [ ] 无请求头 / `GUEST` 时，JSON 与 Excel 中的 username、email 均为脱敏值；
- [ ] `ADMIN` 或携带对应权限码时，对应字段恢复明文；
- [ ] `X-Permissions` 能覆盖 `X-Role` 的预置权限；
- [ ] 请求结束后 ThreadLocal 被清理（`UserContextHolder.get()` 为匿名空权限）；
- [ ] 原有 `/api/users`、Excel 导入导出接口行为不受破坏；
- [ ] 全部相关测试通过。

## 七、实施顺序

1. 新增 `masking` 包的枚举、注解、上下文、规则与序列化器；
2. 新增 `MockUserContextFilter`，打通 mock 权限注入与清理；
3. 改造 `UserDTO`、`UserExcel`，新增 `SensitiveDataMasker` 并接入 `ExcelServiceImpl`；
4. 新增 `MaskingDemoController`；
5. 补齐/更新测试，执行验证命令；
6. 按需补充接口说明（仅在用户要求时产出文档）。
