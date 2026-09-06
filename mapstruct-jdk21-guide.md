# JDK 21 环境下 MapStruct 全面使用指南

> 适用环境：JDK 21 LTS / MapStruct 1.6.3（当前稳定版）/ Maven 3.8+ / Gradle 8+
> 配套代码：本仓库 `jdk21` 模块 `com.qq.ijay997.mapstruct` 包，已通过 `mvn clean test`（15 个用例全部通过）
> 目标读者：需要在分层应用中做 Entity / DTO / VO 转换的 Java 后端工程师

---

## 目录

- [一、核心概念与优势](#一核心概念与优势)
- [二、JDK 21 项目配置](#二jdk-21-项目配置)
- [三、快速上手（3 分钟）](#三快速上手3-分钟)
- [四、基础映射](#四基础映射)
- [五、高级特性](#五高级特性)
- [六、与 JDK 21 新特性结合](#六与-jdk-21-新特性结合)
- [七、Spring 集成](#七spring-集成)
- [八、常见问题与排错](#八常见问题与排错)
- [九、最佳实践清单](#九最佳实践清单)
- [十、配套示例代码与运行方式](#十配套示例代码与运行方式)

---

## 一、核心概念与优势

### 1.1 MapStruct 是什么

MapStruct 是一个 **编译期代码生成器**（Java Annotation Processor）。你只需声明一个映射接口：

```java
@Mapper
public interface CustomerMapper {
    CustomerMapper INSTANCE = Mappers.getMapper(CustomerMapper.class);

    @Mapping(target = "customerName", source = "name")
    CustomerDTO toDTO(Customer customer);
}
```

编译时，MapStruct 会自动生成 `CustomerMapperImpl.java` —— **普通 Java 代码**，通过 getter/setter（或 record 构造器）逐字段赋值，不依赖任何运行时反射。

### 1.2 与其他映射方案的对比

| 维度 | 手写 getter/setter | Spring BeanUtils / Apache BeanUtils | ModelMapper（运行时） | **MapStruct（编译期）** |
|---|---|---|---|---|
| 执行方式 | 直接调用 | 运行时反射 | 运行时反射 + 配置匹配 | 编译期生成直接调用代码 |
| 性能 | 最高 | 差（反射、缓存开销） | 较差 | **等同手写** |
| 类型安全 | 安全 | 字段名写错运行时才发现 | 运行时异常 | **编译期报错** |
| 漏映射检测 | 靠人 review | 静默忽略 | 静默/难追踪 | **编译期 WARN/ERROR** |
| 重构友好 | 一般 | 差（字符串属性名） | 差 | **好（跟着编译错误改）** |
| 可调试性 | 好 | 黑盒 | 黑盒 | **生成代码可直接打断点** |
| 开发效率 | 低 | 高但不可控 | 高但不可控 | **高且可控** |

### 1.3 工作原理

```
@Mapper 接口 + javac 注解处理器
            │
            ▼
  target/generated-sources/annotations/**/*Impl.java（生成的普通 Java 类）
            │
            ▼
   javac 编译为字节码，运行时零反射、零额外依赖
```

生成代码位置（Maven）：`target/generated-sources/annotations/`，建议在 IDEA 中标记为 Generated Sources Root（Maven 项目通常自动识别）。

### 1.4 版本选择（2026 年视角）

| 版本 | 状态 | 说明 |
|---|---|---|
| **1.6.3** | **推荐（稳定版）** | 完整支持 JDK 21、record、`@Condition`/`conditionExpression`、`@SubclassMapping` |
| 1.7.0.Beta2 | Beta（2026-06） | 新增原生 `Optional` 支持、**JDK 21 Sequenced Collections**、JSpecify 空值注解、批量 ignore 多个属性，暂不建议生产使用 |
| 1.5.5.Final | 旧稳定版 | 支持 JDK 21 但缺少 1.6 的条件映射等特性 |

> Lombok 与 JDK 21 搭配时，版本必须 **1.18.30 或更高**，低版本会出现注解处理器静默失效。

---

## 二、JDK 21 项目配置

### 2.1 Maven 配置（生产推荐）

关键三点：

1. `maven-compiler-plugin` 用 **3.13.0+**，`<release>21</release>`；
2. MapStruct 处理器放入 `<annotationProcessorPaths>`（**不要**指望它从普通依赖中自动发现）；
3. 若同时使用 Lombok，必须加 `lombok-mapstruct-binding`，且顺序为 **Lombok → binding → mapstruct-processor**，让 Lombok 先把 getter/setter 生成出来。

完整可运行配置见 [jdk21/pom.xml](file:///Users/jay/Documents/ideaProject/demo/project-my/jdk21/pom.xml)，核心片段：

```xml
<properties>
    <org.mapstruct.version>1.6.3</org.mapstruct.version>
    <lombok.version>1.18.46</lombok.version>
    <lombok.mapstruct.binding.version>0.2.0</lombok.mapstruct.binding.version>
</properties>

<dependencies>
    <dependency>
        <groupId>org.mapstruct</groupId>
        <artifactId>mapstruct</artifactId>
        <version>${org.mapstruct.version}</version>
    </dependency>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>${lombok.version}</version>
        <scope>provided</scope>
    </dependency>
</dependencies>

<build>
    <plugins>
        <plugin>
            <groupId>org.apache.maven.plugins</groupId>
            <artifactId>maven-compiler-plugin</artifactId>
            <version>3.13.0</version>
            <configuration>
                <release>21</release>
                <!-- 多源参数映射依赖参数名保留 -->
                <parameters>true</parameters>
                <!-- 建议关闭增量编译：注解处理器多轮生成源码时，
                     增量清理偶尔与 surefire fork JVM 产生时序竞态（见 8.11） -->
                <useIncrementalCompilation>false</useIncrementalCompilation>
                <annotationProcessorPaths>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok</artifactId>
                        <version>${lombok.version}</version>
                    </path>
                    <path>
                        <groupId>org.projectlombok</groupId>
                        <artifactId>lombok-mapstruct-binding</artifactId>
                        <version>${lombok.mapstruct.binding.version}</version>
                    </path>
                    <path>
                        <groupId>org.mapstruct</groupId>
                        <artifactId>mapstruct-processor</artifactId>
                        <version>${org.mapstruct.version}</version>
                    </path>
                </annotationProcessorPaths>
            </configuration>
        </plugin>
    </plugins>
</build>
```

### 2.2 常用编译器参数

通过 `-A` 传给处理器（也可用注解属性覆盖）：

| 参数 | 作用 | 示例 |
|---|---|---|
| `mapstruct.unmappedTargetPolicy` | 未映射目标属性的全局策略：`IGNORE`/`WARN`/`ERROR` | `-Amapstruct.unmappedTargetPolicy=WARN` |
| `mapstruct.defaultComponentModel` | 默认组件模型：`default`/`spring`/`jsr330` | `-Amapstruct.defaultComponentModel=spring` |
| `mapstruct.suppressGeneratorTimestamp` | 生成类不写时间戳（保证构建可复现） | `-Amapstruct.suppressGeneratorTimestamp=true` |
| `mapstruct.suppressGeneratorVersionInfoComment` | 生成类不写版本注释 | `-Amapstruct.suppressGeneratorVersionInfoComment=true` |
| `mapstruct.verbose` | 打印处理器诊断细节（排障用） | `-Amapstruct.verbose=true` |

### 2.3 Gradle 配置

Groovy DSL（`build.gradle`）：

```groovy
plugins {
    id 'java'
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation 'org.mapstruct:mapstruct:1.6.3'
    annotationProcessor 'org.mapstruct:mapstruct-processor:1.6.3'

    compileOnly 'org.projectlombok:lombok:1.18.46'
    annotationProcessor 'org.projectlombok:lombok:1.18.46'
    annotationProcessor 'org.projectlombok:lombok-mapstruct-binding:0.2.0'
}

tasks.withType(JavaCompile).configureEach {
    options.compilerArgs += ['-parameters',
            '-Amapstruct.unmappedTargetPolicy=WARN']
}
```

Kotlin DSL（`build.gradle.kts`）：

```kotlin
java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

dependencies {
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    compileOnly("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok:1.18.46")
    annotationProcessor("org.projectlombok:lombok-mapstruct-binding:0.2.0")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.addAll(listOf("-parameters", "-Amapstruct.unmappedTargetPolicy=WARN"))
}
```

### 2.4 IntelliJ IDEA 设置

1. 安装官方插件 **MapStruct Support**（Plugins 市场搜索，支持属性名自动补全、跳转、Quick Fix）；
2. `Settings → Build, Execution, Deployment → Compiler → Annotation Processors` → 勾选 **Enable annotation processing**（使用 `annotationProcessorPaths` 的 Maven 工程通常自动识别，未识别时手动勾选）；
3. 确认 Project SDK 为 21，Language Level 为 **21**（record、switch 模式匹配在 21 已正式，无需 `--enable-preview`）；
4. 生成的实现类若报红：`mvn clean compile` 后右键 `target/generated-sources/annotations` → Mark Directory as → Generated Sources Root。

---

## 三、快速上手（3 分钟）

以"订单实体 → 订单 DTO"为例，完整代码见示例工程。

**第 1 步：定义源与目标类型**

```java
// 源：实体（Lombok 生成 getter/setter）
@Data
public class OrderItem {
    private String sku;
    private Integer quantity;
    private BigDecimal unitPrice;
}

// 目标：DTO
@Data
public class OrderItemDTO {
    private String sku;
    private Integer quantity;
    private String unitPrice;   // BigDecimal -> String
}
```

**第 2 步：声明 Mapper**

```java
@Mapper
public interface OrderItemMapper {
    OrderItemMapper INSTANCE = Mappers.getMapper(OrderItemMapper.class);

    @Mapping(target = "unitPrice", source = "unitPrice", numberFormat = "0.00")
    OrderItemDTO toDTO(OrderItem item);
}
```

**第 3 步：使用**

```java
OrderItem item = new OrderItem();
item.setSku("SKU-1");
item.setQuantity(2);
item.setUnitPrice(new BigDecimal("199.00"));

OrderItemDTO dto = OrderItemMapper.INSTANCE.toDTO(item);
// dto.getUnitPrice() == "199.00"
```

`mvn compile` 后可在 `target/generated-sources/annotations` 查看生成的 `OrderItemMapperImpl.java` —— 就是普通的 if-null 判断 + setter 调用。

---

## 四、基础映射

> 本节完整实现见 [CustomerMapper.java](file:///Users/jay/Documents/ideaProject/demo/project-my/jdk21/src/main/java/com/qq/ijay997/mapstruct/mapper/CustomerMapper.java)。

### 4.1 同名属性：零配置

源与目标字段名、类型一致时无需任何注解，MapStruct 按 JavaBean 规范自动匹配。

### 4.2 字段改名：@Mapping source / target

```java
@Mapping(target = "customerName", source = "name")
CustomerDTO toDTO(Customer customer);
```

多个 `@Mapping` 可用 `@Mappings({...})` 包裹，也可直接在方法上重复标注（JDK 8+ 支持重复注解时可省略容器）。

### 4.3 内置类型转换

MapStruct 自动处理常见类型互转，无需手写：

| 源类型 | 目标类型 | 说明 |
|---|---|---|
| 枚举 | `String` | 默认 `Enum.name()`；String→枚举默认 `valueOf()`（非法值抛 IllegalArgumentException） |
| `LocalDate`/`LocalDateTime`/`Date`/`Calendar` | `String` | 配 `dateFormat = "yyyy-MM-dd HH:mm:ss"`，反向自动解析 |
| `BigDecimal`/`Number` | `String` | 配 `numberFormat = "¥#,##0.00"`（DecimalFormat 模式） |
| `String` | `BigDecimal`/`Integer` 等 | 自动解析 |
| 基本类型与包装类型 | 互转 | 如 `int` ↔ `Integer` |
| `String` ↔ `URL`/`URI`/`UUID` | 内置支持 |

```java
@Mapping(target = "birthday", source = "birthday", dateFormat = "yyyy-MM-dd")
@Mapping(target = "totalAmount", source = "totalAmount", numberFormat = "¥#,##0.00")
```

### 4.4 自定义映射方法

三种形式，按作用范围选择：

**① 单 Mapper 内的 default 方法 + `@Named` 限定名**

```java
@Mapping(target = "maskedPhone", source = "phone", qualifiedByName = "maskPhone")
CustomerDTO toDTO(Customer customer);

@Named("maskPhone")
default String maskPhone(String phone) {
    return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
}
```

**② 独立 Mapper 复用：`uses`**

```java
@Mapper(uses = {PaymentMapper.class})   // 遇到 Payment -> PaymentDTO 自动委托
public interface OrderMapper { ... }
```

**③ 抽象类 Mapper**：当需要成员字段或复杂公共逻辑时，把 `interface` 换成 `abstract class`，见 [PaymentMapper.java](file:///Users/jay/Documents/ideaProject/demo/project-my/jdk21/src/main/java/com/qq/ijay997/mapstruct/mapper/PaymentMapper.java)。

### 4.5 更新既有对象：@MappingTarget

适用于 update 接口 —— 不新建对象，把 DTO 内容写入已有实体：

```java
void updateEntity(CustomerDTO dto, @MappingTarget Customer customer);
```

> 注意：record 不可变，**不能作为 `@MappingTarget` 目标**，更新模式只能用于传统 JavaBean。

### 4.6 逆映射：@InheritInverseConfiguration

正向映射声明一次，反向自动继承并翻转：

```java
@Mapping(target = "customerName", source = "name")
@Mapping(target = "birthday", dateFormat = "yyyy-MM-dd")
CustomerDTO toDTO(Customer customer);

@InheritInverseConfiguration(name = "toDTO")
@Mapping(target = "phone", ignore = true)   // 反向时目标多出来的字段要显式忽略
Customer toEntity(CustomerDTO dto);
```

日期格式会自动反向用于解析；若个别字段不需要继承，同名 `@Mapping` 可覆盖。

### 4.7 常量与默认值

```java
@Mapping(target = "type", constant = "CREDIT_CARD")   // 恒为常量
@Mapping(target = "channel", defaultValue = "WEB")    // 源为 null 时使用
@Mapping(target = "traceId",
         defaultExpression = "java(java.util.UUID.randomUUID().toString())")
```

### 4.8 null 处理策略

| 注解属性 | 可选值 | 含义 |
|---|---|---|
| `nullValueCheckStrategy` | `IMMEDIATE`/`ON_DEMAND` | 调用 setter 前是否先生成 null 判断 |
| `nullValuePropertyMappingStrategy`（更新方法） | `SET_TO_NULL`/`IGNORE`/`SET_TO_DEFAULT` | 源属性为 null 时是否覆盖目标（patch 场景用 **IGNORE**） |
| `nullValueMappingStrategy`（集合/Bean） | `RETURN_NULL`/`RETURN_DEFAULT` | 源对象/集合为 null 时返回 null 还是默认值（空集合） |
| `nullValueIterableMappingStrategy` / `nullValueMapMappingStrategy` | 同上 | 集合与 Map 的独立控制 |

---

## 五、高级特性

> 本节完整实现见 [OrderMapper.java](file:///Users/jay/Documents/ideaProject/demo/project-my/jdk21/src/main/java/com/qq/ijay997/mapstruct/mapper/OrderMapper.java)。

### 5.1 嵌套对象与深层属性

直接用点号访问深层属性，无需手写中间对象：

```java
@Mapping(target = "customerName", source = "customer.name")
@Mapping(target = "customerCity", source = "customer.address.city")
OrderDTO toDTO(Order order);
```

对于嵌套 **对象**（`Customer -> CustomerDTO`），MapStruct 自动在当前 Mapper 或 `uses` 声明的 Mapper 中寻找匹配的元素方法：

```java
AddressDTO toAddressDTO(Address address);
Address toAddress(AddressDTO dto);
```

### 5.2 集合映射

`List<A> -> List<B>`、`Set<A> -> Set<B>` 会自动对每个元素调用元素级映射方法：

```java
// 集合入口：null 策略、元素级限定名都在这里声明
@IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
List<OrderItemDTO> toItemDTOs(List<OrderItem> items);

// 元素级方法：被集合入口及其他映射自动复用
@Mapping(target = "unitPrice", numberFormat = "0.00")
OrderItemDTO toItemDTO(OrderItem item);
```

**Map 映射**（key/value 分别转换）：

```java
@MapMapping(valueDateFormat = "yyyy-MM-dd")   // Map<LocalDate,String> -> Map<String,String>
Map<String, String> mapStatusHistory(Map<LocalDate, String> history);
```

> **重要排错经验**：出现 `Can't map Collection element "A to B"` 时，说明 MapStruct 找到了集合入口方法 `List<A> -> List<B>`，但找不到 **元素级** 的 `B map(A)` 方法。修复要按"入口闭包"处理：扫描该模块所有返回 `List/Set` 的映射方法，逐个确认元素方法存在或用 `@IterableMapping(qualifiedByName=...)` 显式绑定，不要只补当前一个报错类型。

### 5.3 表达式动态计算

源对象没有的目标字段，用 Java 表达式现算（表达式中直接写 Java 代码，可以调用当前 Mapper 的 default 方法）：

```java
@Mapping(target = "lineTotal",
         expression = "java(calcLineTotal(item.getUnitPrice(), item.getQuantity()))")
OrderItemDTO toItemDTO(OrderItem item);

default BigDecimal calcLineTotal(BigDecimal unitPrice, Integer quantity) {
    return (unitPrice == null || quantity == null)
            ? null : unitPrice.multiply(BigDecimal.valueOf(quantity));
}
```

### 5.4 条件映射（MapStruct 1.6+）

控制"源属性满足什么条件才映射"，三种写法：

**① `@Condition` 自定义判断方法**（可按 `@Named` 精确绑定）

```java
@Mapping(target = "maskedPhone", source = "phone",
         qualifiedByName = "maskPhone",
         conditionQualifiedByName = "validPhone")
CustomerDTO toDTO(Customer customer);

@Named("validPhone")
@Condition
default boolean hasValidPhone(String phone) {
    return phone != null && !phone.isBlank() && phone.length() >= 7;
}
```

**② `conditionExpression` 内联表达式**（注意变量名是**映射方法的参数名**，不是固定的 `source`）：

```java
@Mapping(target = "remark", source = "remark",
         conditionExpression = "java(order.getRemark() != null && !order.getRemark().isBlank())")
OrderDTO toDTO(Order order);
```

**③ 多源参数 + dependsOn 控制赋值顺序**：

```java
OrderDTO merge(@MappingTarget OrderDTO target, Order order, ExtraInfo extra);

@Mapping(target = "fullAddress", source = "address.detail", dependsOn = "customerName")
```

`dependsOn` 用于保证生成代码中某属性先于另一属性赋值（如 JPA 关联字段有先后要求时）。

### 5.5 多源参数合并

```java
@Mapping(target = "id", source = "order.id")
@Mapping(target = "customerName", source = "customer.name")
OrderDTO merge(Order order, Customer customer);
```

编译需保留参数名（pom 中 `<parameters>true</parameters>`），否则 source 前缀要写 `param1`/`param2`。

### 5.6 映射器继承与共享配置

**全局配置**（统一报告策略、null 策略、组件模型）：

```java
@MapperConfig(
    unmappedTargetPolicy = ReportingPolicy.WARN,
    nullValueMapMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT
)
public interface BaseMapperConfig {}

@Mapper(config = BaseMapperConfig.class, uses = {...})
public interface OrderMapper { ... }
```

**方法级配置复用**：

- `@InheritConfiguration(name = "toEntity")`：同向方法继承全部 `@Mapping` 配置（常用于 `@MappingTarget` 更新方法）；
- `@InheritInverseConfiguration`：反向继承（见 4.6）。

### 5.7 多态映射：@SubclassMapping

父类型转父类型时，声明每个子类的去向，生成代码用 `instanceof` 链分派：

```java
@SubclassMapping(source = CreditCardPayment.class, target = CreditCardPaymentDTO.class)
@SubclassMapping(source = CashPayment.class, target = CashPaymentDTO.class)
PaymentDTO map(Payment payment);

CreditCardPaymentDTO map(CreditCardPayment payment);
CashPaymentDTO map(CashPayment payment);
```

未匹配到任何子类时生成代码抛 `IllegalArgumentException`，不会静默返回错误类型。

### 5.8 接口 vs 抽象类

| 形式 | 适用场景 |
|---|---|
| `interface`（主流） | 纯映射声明 + default 辅助方法 |
| `abstract class` | 需要成员字段、构造器注入、模板方法等复杂逻辑；生成类 `extends` 它 |

---

## 六、与 JDK 21 新特性结合

### 6.1 Record 作为映射目标

MapStruct 1.5+ 通过 record 的**规范构造器（canonical constructor）**创建实例：

```java
public record OrderSummaryDTO(String orderNo, String customerName,
                              String status, Integer itemCount) {}

@Mapping(target = "customerName", source = "customer.name")
@Mapping(target = "itemCount", expression = "java(order.getItems().size())")
OrderSummaryDTO toSummary(Order order);
```

注意点：

1. 每个 record 组件必须有来源（同名属性 / `source` / `expression` / `constant` / `ignore`），否则编译告警或报错；
2. record **没有 setter**，不能用于 `@MappingTarget` 更新模式；
3. 嵌套 record（`AddressDTO`）作为目标时同样自动走全参构造器，已在示例 [CustomerMapperTest](file:///Users/jay/Documents/ideaProject/demo/project-my/jdk21/src/test/java/com/qq/ijay997/mapstruct/CustomerMapperTest.java) 中验证。

### 6.2 Record 作为映射源

record 的访问器（`orderNo()` 等）与 JavaBean getter 等效，直接使用：

```java
public record CreateOrderRequest(String orderNo, String customerName,
                                 String remark, List<ItemRequest> items) {}

@Mapping(target = "customer", source = "customerName")  // String -> Customer，走自定义方法
@Mapping(target = "status", ignore = true)
Order toEntity(CreateOrderRequest request);
```

record 组件为 `String`、目标为 `BigDecimal` 等类型时，内置转换照常生效（示例中 `"299.50" -> BigDecimal(299.50)` 有测试覆盖）。

### 6.3 密封类/接口 + @SubclassMapping（黄金组合）

sealed 类型体系给出了**封闭、可穷举**的子类集合，与 `@SubclassMapping` 的声明式分派天然契合：

```java
// 源：sealed 接口 + record 实现
public sealed interface Payment permits CreditCardPayment, CashPayment {}
public record CreditCardPayment(String cardNo, String bank, LocalDateTime paidAt)
        implements Payment {}
public record CashPayment(BigDecimal cashReceived, BigDecimal change)
        implements Payment {}

// 目标：同样密封
public sealed interface PaymentDTO permits CreditCardPaymentDTO, CashPaymentDTO {}
```

完整实现见 [PaymentMapper.java](file:///Users/jay/Documents/ideaProject/demo/project-my/jdk21/src/main/java/com/qq/ijay997/mapstruct/mapper/PaymentMapper.java)。

### 6.4 在自定义方法中使用 switch 模式匹配（JDK 21 正式特性）

sealed 接口 + record 组件解构 + 穷举 switch，无需 `default`，新增子类漏改时**直接编译失败**：

```java
public String describe(Payment payment) {
    if (payment == null) {
        return "无支付信息";
    }
    return switch (payment) {
        case CreditCardPayment cc -> "信用卡支付[" + cc.bank() + "] 尾号 " + tail(cc.cardNo());
        case CashPayment c -> "现金支付 实收 " + c.cashReceived() + " 找零 " + c.change();
    };
}
```

带 guard 的类型模式同样可用于映射辅助方法：

```java
default String toAgeGroup(Integer age) {
    if (age == null) return "未知";
    return switch (age) {
        case Integer a when a < 18 -> "青少年";
        case Integer a when a < 60 -> "成年";
        default -> "老年";
    };
}
```

### 6.5 Record Patterns 解构（JDK 21 正式）

在 Mapper 的 default 方法、业务代码中均可直接用：

```java
static String format(Payment payment) {
    if (payment instanceof CreditCardPayment(String cardNo, String bank, var paidAt)) {
        return bank + " " + mask(cardNo) + " @ " + paidAt;
    }
    return "其他支付";
}
```

### 6.6 其他 JDK 21 特性的注意事项

| 特性 | MapStruct 1.6.3 支持情况 |
|---|---|
| Record（源/目标） | ✅ 完整支持 |
| Sealed + `@SubclassMapping` | ✅ 完整支持 |
| switch/record 模式匹配 | ✅ 在自定义方法中自由使用（JDK 21 已 GA，无需 `--enable-preview`） |
| SequencedCollection（`SequencedMap`/`SequencedCollection`） | ⚠️ 1.6.3 按普通 Collection/Map 处理；**1.7.0 才原生识别** |
| `Optional` 属性 | ⚠️ 1.6.3 需手写解包方法；**1.7.0 原生支持** |
| 虚拟线程 | 与映射无关：映射本身是纯 CPU 计算，直接跑在虚拟线程上没有任何问题 |

---

## 七、Spring 集成

### 7.1 声明组件模型

```java
@Mapper(componentModel = "spring")
public interface SpringOrderMapper {
    OrderSummaryDTO toSummary(Order order);
}
```

生成的实现类自动带 `@org.springframework.stereotype.Component`，业务中直接注入：

```java
@Service
@RequiredArgsConstructor
public class OrderQueryService {
    private final SpringOrderMapper orderMapper;

    public OrderSummaryDTO query(String orderNo) {
        return orderMapper.toSummary(...);
    }
}
```

示例：[SpringOrderMapper.java](file:///Users/jay/Documents/ideaProject/demo/project-my/jdk21/src/main/java/com/qq/ijay997/mapstruct/mapper/SpringOrderMapper.java)，其测试 [SpringOrderMapperTest](file:///Users/jay/Documents/ideaProject/demo/project-my/jdk21/src/test/java/com/qq/ijay997/mapstruct/SpringOrderMapperTest.java) 用反射验证了 `@Component` 注解的存在。

### 7.2 两条铁律

1. **组件模型要全链路统一**：spring 模型的 Mapper 通过 `uses` 引用其他 Mapper 时，被引用方也必须是 spring 模型（由容器注入），default 与 spring 混用会启动报错；
2. **Spring Boot 项目**不需要手写任何 `@Bean`，注解处理器生成的 `@Component` 会被组件扫描发现。若 Mapper 不在启动类的扫描包下，用 `@MapperScan`（MyBatis 生态）式思路或补充扫描路径。

`jsr330` 模型（生成 `@Named`/`@Inject`）适合 Dagger、Quarkus 等环境，配置方式相同。

---

## 八、常见问题与排错

### 8.1 Lombok 实体映射全部失效 / "Property has no read accessor"

**原因**：Lombok 没在 MapStruct 之前生成 getter/setter，或 Lombok 版本不支持 JDK 21。
**解决**：

- Lombok 升到 **1.18.30+**（本文用 1.18.46）；
- `annotationProcessorPaths` 顺序严格为 `lombok → lombok-mapstruct-binding → mapstruct-processor`；
- IDEA 中确认 Annotation Processing 已启用并重新 `mvn clean compile`。

### 8.2 "Unmapped target property: xxx" 警告/错误

**含义**：目标类的属性 `xxx` 在源中找不到任何映射来源。处理方式：

| 场景 | 处理 |
|---|---|
| 确实不需要映射（如审计字段、主键） | `@Mapping(target = "createdAt", ignore = true)` |
| 应该映射但字段名不同 | 补 `@Mapping(source=..., target=...)` |
| 需要计算 | 补 `expression` / `defaultExpression` / default 方法 |
| 团队规范要求零遗漏 | `unmappedTargetPolicy = ReportingPolicy.ERROR` 把它升级为编译错误（推荐 DTO 出参场景） |

批量实体入参（Create/Update 命令）建议配合 `@BeanMapping(ignoreByDefault = true)` 收敛白名单，避免展示字段、派生字段被误写入持久层。

### 8.3 "Can't map Collection element A to B"

集合入口存在但**元素级方法缺失**。按闭包处理：

1. 定位报错的 `List<A> -> List<B>` 入口所在 Mapper；
2. 补上 `B map(A a)` 元素方法，或在入口上 `@IterableMapping(qualifiedByName = "xxx")` 绑定已有方法；
3. 顺带排查同模块其他集合入口是否有同类缺口，避免"打地鼠"式反复编译报错。

### 8.4 conditionExpression 报 "找不到符号 变量 source"

表达式里能引用的是**映射方法的实际参数名**。方法签名是 `toDTO(Order order)`，就要写 `java(order.getXxx() != null)`。若编译拿不到参数名，检查 `<parameters>true</parameters>`。

### 8.5 生成的实现类 IDEA 报红 / 找不到 *Impl

- 执行 `mvn clean compile`；
- 确认 `target/generated-sources/annotations` 被识别为生成源码目录；
- 确认处理器在 `annotationProcessorPaths` 中（仅声明 mapstruct 普通依赖不会注册处理器）；
- IDEA 偶尔缓存异常：`File → Invalidate Caches`。

### 8.6 LocalDateTime 格式化不生效 / 反解析异常

- `dateFormat` 对 `java.time.*` 与老式 `java.util.Date` 均生效，模式区分大小写：`yyyy-MM-dd HH:mm:ss`（HH 为 24 小时制）；
- String→日期反向解析失败会抛异常，入参 DTO 建议先做格式校验，或自定义 `@Named` 方法容错。

### 8.7 重载 map 方法导致 null 入参歧义

同一 Mapper 中 `map(Payment)`、`map(CreditCardPayment)` 重载并存时，`mapper.map(null)` 无法推断类型，显式转型：`mapper.map((Payment) null)`。

### 8.8 双向映射的循环引用

`Order -> OrderDTO` 与 `OrderDTO -> Order` 不会互相递归（MapStruct 不做自动反向调用），但如果你在自定义方法中手动互相 new 就会死循环。正确做法：两个方向各自声明独立映射方法，由 MapStruct 按类型选择。

### 8.9 想看生成了什么 / 映射为什么不符合预期

- 直接读 `*Impl.java`（最有效）；
- 加 `-Amapstruct.verbose=true` 获取处理器详细日志；
- `@Mapper` 上临时开 `unmappedTargetPolicy = ReportingPolicy.WARN` 观察全部告警。

### 8.10 枚举 String 反向转换抛 IllegalArgumentException

`"MAIL"` 无法 `valueOf(MALE)`。可自定义反向方法并标记 `@Named` + `qualifiedByName`，或用 `@ValueMapping`（枚举→枚举）做显式映射与 `@BeanMapping(nullValueMappingStrategy=...)` 的兜底。

### 8.11 编译成功但测试偶发 NoClassDefFoundError / TestEngine failed to discover tests

**现象**：`mvn clean test` 偶发失败，报 `NoClassDefFoundError: ...Mapper` 或 `TestEngine with ID 'junit-jupiter' failed to discover tests`，但 `target/classes` 下 class 文件实际存在，重跑又可能通过。

**根因**：maven-compiler-plugin 3.13.x 的增量编译会在注解处理多轮生成源码期间清理/重写输出目录，与 surefire fork 出的 JVM 扫描 classpath 产生时序竞态（Lombok + MapStruct 双处理器更容易触发，JDK 21 上以 GraalVM 发行版尤为常见）。

**解决**（任选）：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <useIncrementalCompilation>false</useIncrementalCompilation>  <!-- 推荐：全量编译 -->
    </configuration>
</plugin>
```

或升级编译器插件到 3.14+；CI 环境始终执行 `clean test` 也可大幅降低概率。

---

## 九、最佳实践清单

**配置层**

1. 生产项目统一用 `annotationProcessorPaths` 显式注册处理器，不靠依赖传递；
2. 开启 `<parameters>true</parameters>`；CI 用固定版本插件（compiler 3.13.0+、surefire 3.2.5+）；
3. 出参 DTO Mapper 用 `unmappedTargetPolicy = ERROR`，让漏映射在编译期暴露。

**设计层**

4. 一个聚合根一套 Mapper（`OrderMapper` 管 Order 及 OrderItem），跨聚合的对象转换通过 `uses` 组合，不要建巨型 Mapper；
5. 转换方向与分层对齐：Entity↔DTO 可以双向，但**前端入参用专用的 Create/Update 命令对象**，不要拿 RespVO 反向生成 DO，避免只读/派生字段污染持久层；
6. 敏感字段（手机号、卡号）用 `@Named` 脱敏方法显式映射，杜绝实体原文外泄；
7. 集合映射统一声明 null 策略 `RETURN_DEFAULT`，下游不再写 `if (list != null)`；
8. 更新接口（patch 语义）用 `@MappingTarget` + `nullValuePropertyMappingStrategy = IGNORE`，避免 null 覆盖存量数据；
9. JDK 21 项目优先用 **record 承载 DTO**（不可变、线程安全、少样板），可变实体仍用传统 JavaBean/JPA 实体；
10. 多态类型用 **sealed + @SubclassMapping**，借编译器保证穷举；
11. 复杂转换逻辑放 default/抽象方法并配单元测试，表达式（`expression`）只写一行能读完的简单逻辑；
12. Spring 项目全链路统一 `componentModel = "spring"`，通过构造注入使用。

**测试层**

13. 每个 Mapper 至少一条"完整对象图"测试 + 一条 null/空集合边界测试；
14. 测试格式化字段时用精确断言（如 `"¥1,234.50"`、`"2026-09-14 10:30:00"`），格式回归能被立刻发现；
15. 金额断言用 `BigDecimal.compareTo == 0`，不要用 `equals`（`199.0` 与 `199.00` scale 不同）。

---

## 十、配套示例代码与运行方式

示例位于本仓库 `jdk21` 模块（包 `com.qq.ijay997.mapstruct`），是一个连贯的"电商订单"场景：

```
jdk21/src/main/java/com/qq/ijay997/mapstruct/
├── model/
│   ├── enums/
│   │   ├── Gender.java                 # 普通枚举
│   │   └── OrderStatus.java            # 带 description 的枚举
│   ├── entity/                         # 持久层模型（Lombok 传统 JavaBean + sealed record）
│   │   ├── Address.java
│   │   ├── Customer.java
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   ├── Payment.java                # sealed interface
│   │   ├── CreditCardPayment.java      # record permits 子类
│   │   └── CashPayment.java
│   └── dto/                            # 出参/入参模型（传统 DTO + record + sealed DTO）
│       ├── AddressDTO.java             # record
│       ├── CustomerDTO.java
│       ├── OrderDTO.java
│       ├── OrderItemDTO.java
│       ├── OrderSummaryDTO.java        # record 目标
│       ├── PaymentDTO.java             # sealed interface
│       ├── CreditCardPaymentDTO.java   # record
│       ├── CashPaymentDTO.java         # record
│       ├── CreateOrderRequest.java     # record 源
│       └── ItemRequest.java
└── mapper/
    ├── BaseMapperConfig.java           # @MapperConfig 共享配置
    ├── CustomerMapper.java             # 基础映射/条件/逆映射/更新/JDK21 switch
    ├── OrderMapper.java                # 嵌套/集合/Map/表达式/record 源与目标
    ├── PaymentMapper.java              # abstract class + @SubclassMapping + switch 模式匹配
    └── SpringOrderMapper.java          # componentModel = "spring"

jdk21/src/test/java/com/qq/ijay997/mapstruct/
├── CustomerMapperTest.java             # 5 个用例
├── OrderMapperTest.java                # 5 个用例
├── PaymentMapperTest.java              # 4 个用例
└── SpringOrderMapperTest.java          # 1 个用例
```

### 特性覆盖对照表

| 指南章节 | 对应代码 |
|---|---|
| 字段改名 / 枚举 / 日期 / 自定义方法 | `CustomerMapper` |
| `@Condition` 条件映射 | `CustomerMapper.hasValidPhone` |
| `conditionExpression` | `OrderMapper.toDTO(remark)` |
| 逆映射 / 更新对象 | `CustomerMapper.toEntity / updateEntity` |
| 嵌套深层属性 | `OrderMapper`（customer.name / customer.address.city） |
| List / Map 集合与 null 策略 | `OrderMapper.toItemDTOs / mapStatusHistory` |
| expression 计算 | `OrderMapper`（lineTotal、itemCount） |
| `@MapperConfig` 共享配置 | `BaseMapperConfig` |
| sealed + `@SubclassMapping` | `PaymentMapper` |
| record 源 / 目标 | `OrderSummaryDTO`、`CreateOrderRequest`、`AddressDTO` |
| JDK 21 switch 模式匹配 | `PaymentMapper.describe`、`CustomerMapper.toAgeGroup` |
| Spring 组件模型 | `SpringOrderMapper` |

### 运行命令

```bash
# 在仓库根目录执行（需要 JDK 21）
mvn -pl jdk21 clean test

# 只编译并查看 MapStruct 生成的实现类
mvn -pl jdk21 clean compile
# 生成产物目录：
# jdk21/target/generated-sources/annotations/com/qq/ijay997/mapstruct/mapper/
```

预期结果（2026-09 验证通过）：

```
Tests run: 15, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

---

## 附：官方资源

- 安装文档：https://mapstruct.org/documentation/installation/
- 参考指南（全部注解属性）：https://mapstruct.org/documentation/reference-guide/
- 官方示例仓库：https://github.com/mapstruct/mapstruct-examples
- IDEA 插件：MapStruct Support（JetBrains Marketplace）
