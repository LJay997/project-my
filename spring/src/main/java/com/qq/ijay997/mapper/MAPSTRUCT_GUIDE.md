# Spring 模块 MapStruct 学习与使用指南

> 适用模块：`spring`（Spring Boot 2.7.18 / Java 11 / `javax.*` / Lombok）
> 代码位置：`spring/src/main/java/com/qq/ijay997/mapper`
> 参考文档：根目录 [mapstruct-jdk21-guide.md](file:///d:/Project/IdeaProjects/local-project/project-my/mapstruct-jdk21-guide.md)（JDK 21、record/sealed、MapStruct 1.6 专属特性以该文为准）
>
> 现状说明：本文创建时 `mapper` 包为空，`spring/pom.xml` 尚未引入 MapStruct。
> 本文只负责说明用法与配置，不修改任何既有业务代码；第 3 节的依赖和处理器配置需要你手动补充。

---

## 目录

- [一、项目现状与结论](#一项目现状与结论)
- [二、MapStruct 是什么](#二mapstruct-是什么)
- [三、依赖与编译配置](#三依赖与编译配置)
- [四、注解基础](#四注解基础)
- [五、集合、嵌套对象与多源映射](#五集合嵌套对象与多源映射)
- [六、类型转换与自定义方法](#六类型转换与自定义方法)
- [七、与 Spring 集成](#七与-spring-集成)
- [八、常见坑与排错](#八常见坑与排错)
- [九、在本工程落地](#九在本工程落地)
- [十、参考资料](#十参考资料)

---

## 一、项目现状与结论

### 1.1 目录与依赖的实际检查结果

| 检查项 | 实际情况 | 对引入 MapStruct 的影响 |
|---|---|---|
| `com.qq.ijay997.mapper` 包 | 空目录，0 个文件 | 没有历史 Mapper 需要兼容，也不会与既有代码重名 |
| `spring/pom.xml` | 只有 Lombok，没有 `mapstruct`、`mapstruct-processor` | 必须新增运行时依赖 + 注解处理器 |
| 编译级别 | 根 POM 为 8，`spring` 模块覆盖为 `source/target=11` | 不能使用 record/sealed；MapStruct 生成代码可运行在 Java 11 |
| Spring 版本 | Spring Boot 2.7.18，使用 `javax.persistence`、`javax.validation` | 不需要改成 `jakarta.*`；与 MapStruct 无冲突 |
| Lombok | 已引入，实体/DTO 使用 `@Data`、`@NoArgsConstructor` 等 | 必须补 `lombok-mapstruct-binding`，并保证处理器顺序 |
| 现有手写转换 | `MaskingDemoController#toDTO`、`ExcelServiceImpl#exportUsers` | 是最直接的迁移对象 |

能够直接复用的真实类型：

- 实体：[User.java](file:///d:/Project/IdeaProjects/local-project/project-my/spring/src/main/java/com/qq/ijay997/entity/User.java)、[Order.java](file:///d:/Project/IdeaProjects/local-project/project-my/spring/src/main/java/com/qq/ijay997/entity/Order.java)
- DTO：[UserDTO.java](file:///d:/Project/IdeaProjects/local-project/project-my/spring/src/main/java/com/qq/ijay997/dto/UserDTO.java)、[OrderDTO.java](file:///d:/Project/IdeaProjects/local-project/project-my/spring/src/main/java/com/qq/ijay997/dto/OrderDTO.java)、[AddressDTO.java](file:///d:/Project/IdeaProjects/local-project/project-my/spring/src/main/java/com/qq/ijay997/dto/AddressDTO.java)
- Excel 模型：[UserExcel.java](file:///d:/Project/IdeaProjects/local-project/project-my/spring/src/main/java/com/qq/ijay997/dto/UserExcel.java)

现有手写转换位置：

- [MaskingDemoController.java](file:///d:/Project/IdeaProjects/local-project/project-my/spring/src/main/java/com/qq/ijay997/controller/MaskingDemoController.java#L73-L76)：7 个参数的 `UserDTO` 构造调用。
- [ExcelServiceImpl.java](file:///d:/Project/IdeaProjects/local-project/project-my/spring/src/main/java/com/qq/ijay997/service/impl/ExcelServiceImpl.java#L49-L58)：循环 `User`，用 Builder 组装 `UserExcel`，并计算 `ageGroup`。

### 1.2 结论

1. **需要新增依赖**：`org.mapstruct:mapstruct` 和注解处理器 `org.mapstruct:mapstruct-processor`。
2. **必须配置 `annotationProcessorPaths`**：只把 `mapstruct` 当普通依赖引入不会生成实现类。
3. **需要 Lombok 协同配置**：顺序为 Lombok → `lombok-mapstruct-binding` → `mapstruct-processor`。
4. **无需改动现有类**：`User`、`UserDTO`、`Order`、`OrderDTO`、`UserExcel` 都能直接作为映射源/目标。
5. **注意 `@Sensitive` 的职责边界**：MapStruct 只搬运原值，脱敏仍由 Jackson 序列化器 / Excel 写入处理器完成，不要在 Mapper 里提前脱敏。

---

## 二、MapStruct 是什么

### 2.1 编译期代码生成器

MapStruct 是一个 Java 注解处理器。你只声明接口：

```java
@Mapper
public interface UserMapper {
    UserDTO toDTO(User user);
}
```

编译时它会生成一个普通 Java 实现类（例如 `UserMapperImpl.java`），内部就是 null 判断 + getter/setter 调用，**运行时不使用反射**。

生成位置：

```text
spring/target/generated-sources/annotations/com/qq/ijay997/mapper/UserMapperImpl.java
```

### 2.2 与手写 setter 的差别

| 维度 | 手写 setter | `BeanUtils` / 运行时映射 | MapStruct |
|---|---|---|---|
| 执行方式 | 直接调用 | 运行时反射 | 编译期生成直接调用代码 |
| 性能 | 最高 | 有反射和缓存开销 | 与手写基本一致 |
| 字段改名 | 手写赋值 | 靠字符串属性名，运行时才发现 | `@Mapping` 编译期校验 |
| 漏映射 | 靠 review | 静默忽略 | 编译期 WARN/ERROR |
| 重构 | 手工改 | 容易漏 | 跟着编译错误改 |
| 可调试性 | 好 | 黑盒 | 生成代码可直接断点 |

本工程现有的手写转换：

```java
// MaskingDemoController 现有写法
private UserDTO toDTO(User user) {
    return new UserDTO(user.getId(), user.getUsername(), user.getEmail(),
            DemoMockData.phoneOf(user.getId()), user.getAge(), null, null);
}
```

用 MapStruct 后，调用方只需要：

```java
UserDTO dto = userMapper.toDTO(user);
```

字段改名、类型转换、派生字段统一在接口上声明，新增字段时编译期就能发现漏映射。

### 2.3 生成代码长什么样

以 `UserMapper#toDTO` 为例，生成的实现大致如下（省略部分注释）：

```java
@Component
public class UserMapperImpl implements UserMapper {

    @Override
    public UserDTO toDTO(User user) {
        if (user == null) {
            return null;
        }
        UserDTO userDTO = new UserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setEmail(user.getEmail());
        userDTO.setAge(user.getAge());
        // @AfterMapping 中补 phone
        fillPhone(user, userDTO);
        return userDTO;
    }
}
```

可以直接阅读 `*Impl.java` 排查映射行为，这是 MapStruct 相比运行时框架最大的优势之一。

---

## 三、依赖与编译配置

### 3.1 修改 `spring/pom.xml`

当前 `spring/pom.xml` 中已有 Lombok 依赖，但没有 MapStruct，也没有 `<annotationProcessorPaths>`。需要补三处。

**第一处：属性**

```xml
<properties>
    <drools.version>8.41.0.Final</drools.version>
    <easyexcel.version>4.0.3</easyexcel.version>
    <poi.version>5.2.5</poi.version>

    <!-- 新增 -->
    <org.mapstruct.version>1.6.3</org.mapstruct.version>
    <lombok.version>1.18.46</lombok.version>
</properties>
```

**第二处：运行依赖**

```xml
<dependency>
    <groupId>org.mapstruct</groupId>
    <artifactId>mapstruct</artifactId>
    <version>${org.mapstruct.version}</version>
</dependency>
```

同时给现有 Lombok 依赖补上版本，避免 Maven 依赖和注解处理器路径中的 Lombok 版本不一致：

```xml
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>${lombok.version}</version>
    <optional>true</optional>
</dependency>
```

> `mapstruct-processor` 不要作为普通 `<dependency>` 引入，只放在 `annotationProcessorPaths` 中。

**第三处：替换现有 `maven-compiler-plugin` 配置**

当前配置只有：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <configuration>
        <source>11</source>
        <target>11</target>
    </configuration>
</plugin>
```

建议替换为：

```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-compiler-plugin</artifactId>
    <version>3.13.0</version>
    <configuration>
        <release>11</release>
        <!-- 多源参数映射、expression 中使用参数名时需要 -->
        <parameters>true</parameters>
        <annotationProcessorPaths>
            <!-- 1. Lombok 先执行，先生成 getter/setter/builder -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok</artifactId>
                <version>${lombok.version}</version>
            </path>
            <!-- 2. 让 MapStruct 能识别 Lombok 生成的成员 -->
            <path>
                <groupId>org.projectlombok</groupId>
                <artifactId>lombok-mapstruct-binding</artifactId>
                <version>0.2.0</version>
            </path>
            <!-- 3. MapStruct 处理器放最后 -->
            <path>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct-processor</artifactId>
                <version>${org.mapstruct.version}</version>
            </path>
        </annotationProcessorPaths>
    </configuration>
</plugin>
```

### 3.2 版本选择

| 版本 | 适用场景 | 说明 |
|---|---|---|
| `1.6.3` | 推荐，与根目录 JDK 21 指南一致 | 支持 `@Condition`、`conditionExpression` 等 1.6 特性 |
| `1.5.5.Final` | CI 固定在较老 JDK 时的保守选择 | 本文用到的 `@Mapper`、`@Mapping`、`@MappingTarget`、`@InheritConfiguration`、`@Named` 均支持 |

Lombok 版本：

- 本机当前 JDK 为 17（2026-11-04 实测），Lombok `1.18.24`（Boot 2.7.18 默认）可以工作；
- 如果 CI 或本机切换到 JDK 21，Lombok 必须升到 `1.18.30+`，建议统一用 `1.18.46`。

### 3.3 IDEA 设置

1. 安装插件 **MapStruct Support**（属性名补全、跳转、Quick Fix）。
2. `Settings → Build, Execution, Deployment → Compiler → Annotation Processors` 勾选 **Enable annotation processing**。
3. Project SDK 与 Language Level 至少为 11（与 `spring` 模块一致即可）。
4. 生成目录 `target/generated-sources/annotations` 通常会被自动识别；若 `*Impl` 报红，先执行 `mvn clean compile`，再将该目录标记为 Generated Sources Root。

### 3.4 验证是否生效

在仓库根目录执行：

```bash
mvn -pl spring -am clean compile
```

检查生成文件：

```text
spring/target/generated-sources/annotations/com/qq/ijay997/mapper/UserMapperImpl.java
```

如果没有生成，直接跳到 [八、常见坑与排错](#八常见坑与排错)。

---

## 四、注解基础

### 4.1 `@Mapper`

`org.mapstruct.Mapper` 标记映射接口。不写 `componentModel` 时，默认通过 `Mappers.getMapper(...)` 获取实例；Spring 项目建议统一写：

```java
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDTO(User user);
}
```

注意：这是 `org.mapstruct.Mapper`，不是 MyBatis 的 `org.apache.ibatis.annotations.Mapper`。本工程目前没有 MyBatis，但未来同时引入时务必写全限定名或检查 import。

### 4.2 `@Mapping` 与 `@Mappings`

`@Mapping` 用于声明一个属性的映射关系；多个 `@Mapping` 可以直接重复标注（Java 8+ 可重复注解），也可以用 `@Mappings({...})` 包起来。

```java
import org.mapstruct.Mapping;
import org.mapstruct.Mappings;

@Mappings({
    @Mapping(target = "amount", expression = "java(java.math.BigDecimal.valueOf(order.getAmout()))"),
    @Mapping(target = "status", source = "score", qualifiedByName = "scoreToStatus")
})
OrderDTO toDTO(Order order);
```

常用属性：

| 属性 | 作用 | 示例 |
|---|---|---|
| `target` | 目标属性名，必填 | `target = "amount"` |
| `source` | 源属性名；支持点号深层属性 | `source = "user.username"` |
| `ignore` | 忽略该目标属性 | `ignore = true` |
| `expression` | 用 Java 表达式计算目标值 | `java(BigDecimal.valueOf(order.getAmout()))` |
| `constant` | 固定常量 | `constant = "WEB"` |
| `defaultValue` | 源为 null 时使用的默认值 | `defaultValue = "未知"` |
| `defaultExpression` | 源为 null 时执行的表达式 | `java(UUID.randomUUID().toString())` |
| `qualifiedByName` | 按 `@Named` 选择具体转换方法 | `qualifiedByName = "trimToNull"` |
| `dateFormat` | 日期 ↔ String 的格式 | `"yyyy-MM-dd HH:mm:ss"` |
| `numberFormat` | 数字 ↔ String 的格式 | `"0.00"` |
| `conditionExpression` | 1.6+，满足条件才映射 | 见根目录指南 |

### 4.3 `@MappingTarget`：更新已有对象

用于 update 场景，不新建对象，把 DTO 写进已有实体：

```java
void updateEntity(UserDTO dto, @MappingTarget User user);
```

注意：

- 目标必须是可变的 JavaBean（本工程的 `User` 有 Lombok setter，满足条件）；
- 如果 source 是 null，生成代码会直接返回，不会清空目标；
- 源属性为 null 时是否覆盖目标，由 `nullValuePropertyMappingStrategy` 决定，见 8.4；
- `@MappingTarget` 方法通常返回 `void`，也可以返回目标类型，便于链式调用。

### 4.4 `@InheritConfiguration`：复用同向配置

```java
import org.mapstruct.InheritConfiguration;

@Mapping(target = "id", ignore = true)
User toEntity(UserDTO dto);

@InheritConfiguration(name = "toEntity")
void updateEntity(UserDTO dto, @MappingTarget User user);
```

要点：

- 只能继承同一 Mapper 中**同方向、同源/目标类型**的方法配置；
- `name` 写被继承的方法名；
- 反向映射使用 `@InheritInverseConfiguration(name = "toDTO")`，它会翻转 `source`/`target`；
- 继承后仍可在当前方法上追加 `@Mapping` 覆盖个别属性。

### 4.5 `@Named`：给转换方法起名字

当同一 Mapper import 中存在多个参数/返回类型相同的 `default` 方法时，MapStruct 无法判断用哪个，需要 `@Named` + `qualifiedByName`：

```java
import org.mapstruct.Named;

@Mapping(target = "username", source = "username", qualifiedByName = "trimToNull")
UserDTO toDTO(User user);

@Named("trimToNull")
default String trimToNull(String value) {
    if (value == null) {
        return null;
    }
    String trimmed = value.trim();
    return trimmed.isEmpty() ? null : trimmed;
}
```

### 4.6 `@BeforeMapping` 与 `@AfterMapping`

两者都是映射生命周期回调，方法写在 Mapper 接口中，用 `default` 实现。

- `@BeforeMapping`：在属性赋值之前执行，适合参数校验、预处理、日志；
- `@AfterMapping`：在所有属性赋值之后执行，适合补派生字段、默认值、审计字段。

```java
import org.mapstruct.AfterMapping;
import org.mapstruct.BeforeMapping;
import org.mapstruct.MappingTarget;

@BeforeMapping
default void checkSource(User user) {
    if (user == null) {
        throw new IllegalArgumentException("user 不能为空");
    }
}

@AfterMapping
default void fillPhone(User source, @MappingTarget UserDTO target) {
    if (target.getPhone() == null) {
        target.setPhone(DemoMockData.phoneOf(source.getId()));
    }
}
```

回调参数可以组合使用源对象、`@MappingTarget` 目标对象和 `@Context` 上下文对象。生成代码会在对应时机调用它们；如果 source 为 null 且方法直接返回 null，后续回调可能不会执行，因此不要依赖回调做 null 兜底。

### 4.7 完整示例：`UserMapper`

以下代码可直接放到 `com.qq.ijay997.mapper` 包（类名可按团队规范调整，注意不要与未来的 MyBatis Mapper 重名）。

```java
package com.qq.ijay997.mapper;

import com.qq.ijay997.dto.UserDTO;
import com.qq.ijay997.entity.User;
import com.qq.ijay997.support.DemoMockData;
import org.mapstruct.AfterMapping;
import org.mapstruct.BeanMapping;
import org.mapstruct.InheritConfiguration;
import org.mapstruct.IterableMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.NullValueMappingStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import org.mapstruct.ReportingPolicy;

import java.util.List;

/**
 * User <-> UserDTO 转换。
 * 示例代码：使用前请先按第 3 节补齐依赖与注解处理器。
 *
 * @author ijay997
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.WARN)
public interface UserMapper {

    /**
     * 实体转 DTO：同名属性自动映射，phone 由 @AfterMapping 填充。
     */
    @Mapping(target = "username", source = "username", qualifiedByName = "trimToNull")
    UserDTO toDTO(User user);

    /**
     * 集合入口：源集合为 null 时返回空列表，避免调用方判空。
     */
    @IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
    List<UserDTO> toDTOList(List<User> users);

    /**
     * DTO 转实体：主键由数据库生成，忽略 DTO 中的 id。
     */
    @Mapping(target = "id", ignore = true)
    User toEntity(UserDTO dto);

    /**
     * 更新已有实体：继承 toEntity 的配置，源为 null 的属性不覆盖目标。
     */
    @InheritConfiguration(name = "toEntity")
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    void updateEntity(UserDTO dto, @MappingTarget User user);

    /**
     * 实体 -> DTO 后置处理：phone 未落库，按 userId 稳定生成。
     */
    @AfterMapping
    default void fillPhone(User source, @MappingTarget UserDTO target) {
        if (target.getPhone() == null) {
            target.setPhone(DemoMockData.phoneOf(source.getId()));
        }
    }

    /**
     * 用户名去空格，空字符串归一为 null。
     */
    @Named("trimToNull")
    default String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
```

说明：

- `toDTO` 声明了 `@Mapping(target = "username", ...)`，其余同名属性（`id`、`email`、`age`）自动映射。
- `toEntity` 忽略 `id`，`UserDTO` 中多出来的 `phone`、`createTime`、`updateTime` 没有对应目标属性，MapStruct 默认忽略这些源字段。
- `updateEntity` 只更新 DTO 中非 null 的属性，适合 PATCH 语义。
- `fillPhone` 与 `trimToNull` 都是 `default` 方法，生成类会直接调用它们。

---

## 五、集合、嵌套对象与多源映射

### 5.1 集合映射

`List<A> -> List<B>` 会自动生成循环，并复用元素级映射方法：

```java
@IterableMapping(nullValueMappingStrategy = NullValueMappingStrategy.RETURN_DEFAULT)
List<UserDTO> toDTOList(List<User> users);
```

上面这个方法在 `UserMapper` 中已给出，生成逻辑大致是：

```java
if (users == null) {
    return new ArrayList<>();
}
List<UserDTO> list = new ArrayList<>(users.size());
for (User user : users) {
    list.add(toDTO(user));
}
return list;
```

`Set`、`Map` 同理：

```java
Set<UserDTO> toDTOSet(Set<User> users);

@MapMapping
Map<Long, UserDTO> toDTOMap(Map<Long, User> users);
```

注意：

- `RETURN_NULL`（默认）在源为 null 时返回 null；`RETURN_DEFAULT` 返回空集合，两者按调用方约定选择；
- 集合入口只负责“遍历”，元素级方法缺失时会报 `Can't map Collection element ...`，见 8.7；
- `Map` 映射的 key 和 value 都会走类型转换，key 可从 `Long` 直接映射到 `Long`。

### 5.2 嵌套对象

嵌套对象有两种写法：**打平**和**保留对象**。

以下 `Address`、`UserProfile`、`UserProfileDTO` 为示例类（本工程只有 [AddressDTO.java](file:///d:/Project/IdeaProjects/local-project/project-my/spring/src/main/java/com/qq/ijay997/dto/AddressDTO.java)，没有对应实体）：

```java
import lombok.Data;

@Data
public class Address {
    private String province;
    private String city;
    private String detail;
}

@Data
public class UserProfile {
    private Long id;
    private String username;
    private Address address;
}

@Data
public class UserProfileDTO {
    private Long id;
    private String username;
    private AddressDTO address;
}
```

**写法一：保留嵌套对象**

```java
@Mapper(componentModel = "spring")
public interface UserProfileMapper {

    /**
     * Address -> AddressDTO 会自动委托给下面的 toAddressDTO。
     */
    UserProfileDTO toDTO(UserProfile profile);

    AddressDTO toAddressDTO(Address address);
}
```

MapStruct 在当前 Mapper 中查找 `Address -> AddressDTO` 的方法；如果方法不在当前 Mapper，可通过 `uses` 组合：

```java
@Mapper(componentModel = "spring", uses = {AddressMapper.class})
public interface UserProfileMapper {
    UserProfileDTO toDTO(UserProfile profile);
}
```

被 `uses` 引用的 Mapper 必须同样是 `componentModel = "spring"`，否则 Spring 注入会失败。

**写法二：打平成平铺字段（深层属性）**

目标 DTO 没有嵌套对象时，用点号直接取深层属性：

```java
@Data
public class FlatUserProfileDTO {
    private Long id;
    private String username;
    private String province;
    private String city;
    private String detail;
}

@Mapper(componentModel = "spring")
public interface FlatUserProfileMapper {

    @Mapping(target = "province", source = "address.province")
    @Mapping(target = "city", source = "address.city")
    @Mapping(target = "detail", source = "address.detail")
    FlatUserProfileDTO toDTO(UserProfile profile);
}
```

### 5.3 多源参数

一个映射方法可以接收多个源对象，用 `source = "参数名.属性名"` 区分：

```java
@Mapping(target = "id", source = "user.id")
@Mapping(target = "username", source = "user.username")
@Mapping(target = "province", source = "address.province")
@Mapping(target = "city", source = "address.city")
@Mapping(target = "detail", source = "address.detail")
FlatUserProfileDTO merge(User user, Address address);
```

前提是编译时保留参数名，即 `maven-compiler-plugin` 配置中的 `<parameters>true</parameters>`。否则 MapStruct 只能看到 `param1`、`param2`，`source` 需要改成 `param1.id` 这类形式。

### 5.4 对象更新与嵌套集合

- 更新已有对象：`void update(UserProfileDTO dto, @MappingTarget UserProfile profile);`
- 嵌套集合：`List<Address> -> List<AddressDTO>` 会自动逐元素调用 `toAddressDTO`；
- `User` 是 JPA 实体，映射集合字段时要小心懒加载；在 Mapper 中遍历 `@OneToMany` 懒加载集合可能触发额外 SQL，建议在 Service 层完成必要的加载后再映射。

---

## 六、类型转换与自定义方法

### 6.1 内置转换

MapStruct 会自动处理常见类型，不需要手写：

| 源类型 | 目标类型 | 行为 |
|---|---|---|
| 基本类型 ↔ 包装类型 | 如 `int` ↔ `Integer` | 自动装箱/拆箱 |
| `String` ↔ 数字 | `"123"` ↔ `Integer`/`Long` | 自动解析 |
| `BigDecimal`/`Number` ↔ `String` | 配合 `numberFormat` | 按格式输出/解析 |
| 枚举 ↔ `String` | `Enum.name()` / `valueOf()` | 反向非法值抛 `IllegalArgumentException` |
| `LocalDate`/`LocalDateTime`/`Date` ↔ `String` | 配合 `dateFormat` | 按格式输出/解析 |
| `String` ↔ `UUID`/`URL`/`URI` | 内置 | 直接转换 |

本工程中的 `Order` 有一个拼写为 `amout` 的 `double` 字段，而 `OrderDTO.amount` 是 `BigDecimal`。这种“名称不一致 + 类型不一致”的情况不要依赖自动转换，直接显式声明：

```java
@Mapper(componentModel = "spring")
public interface OrderMapper {

    @Mappings({
        @Mapping(target = "amount", expression = "java(java.math.BigDecimal.valueOf(order.getAmout()))"),
        @Mapping(target = "status", source = "score", qualifiedByName = "scoreToStatus"),
        @Mapping(target = "id", ignore = true),
        @Mapping(target = "orderNo", ignore = true),
        @Mapping(target = "userId", ignore = true),
        @Mapping(target = "productName", ignore = true),
        @Mapping(target = "createTime", ignore = true),
        @Mapping(target = "updateTime", ignore = true)
    })
    OrderDTO toDTO(Order order);

    @Named("scoreToStatus")
    default Integer scoreToStatus(double score) {
        return score >= 60 ? 1 : 0;
    }
}
```

说明：

- `Order` 没有 `id` 字段，目标 `OrderDTO.id` 必须 `ignore = true`，否则 `unmappedTargetPolicy = ERROR` 时会编译失败；
- `amout` 是实体现有字段，不要为了映射去改实体字段名（会牵动 JPA 映射和历史代码），用 `source` / `expression` 适配即可；
- `score -> status` 是示例性的类型转换，实际业务按真实规则实现。

### 6.2 `dateFormat`

`dateFormat` 只在**日期类型 ↔ String** 时生效：

```java
import lombok.Data;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.time.LocalDateTime;

// 示例类：本工程没有这两个类，仅演示日期格式转换
@Data
public class AuditSource {
    private Long id;
    private LocalDateTime createTime;
}

@Data
public class AuditRow {
    private Long id;
    private String createTime;
}

@Mapper(componentModel = "spring")
public interface AuditMapper {

    @Mapping(target = "createTime", source = "createTime", dateFormat = "yyyy-MM-dd HH:mm:ss")
    AuditRow toRow(AuditSource source);
}
```

要点：

- `HH` 是 24 小时制，`hh` 是 12 小时制；
- `java.time.*` 与 `java.util.Date` 都支持；
- String → 日期方向解析失败会抛异常，入参 DTO 应先在 Controller 层做格式校验；
- 源和目标都是 `LocalDateTime` 时不需要写 `dateFormat`，写了也不起作用。

### 6.3 `numberFormat`

`numberFormat` 同样只在**数字 ↔ String** 时生效：

```java
import lombok.Data;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.math.BigDecimal;

// 示例类：本工程没有这两个类，仅演示金额格式化
@Data
public class AmountSource {
    private BigDecimal amount;
}

@Data
public class AmountRow {
    private String amount;
}

@Mapper(componentModel = "spring")
public interface AmountRowMapper {

    @Mapping(target = "amount", source = "amount", numberFormat = "0.00")
    AmountRow toRow(AmountSource source);
}
```

常见误区：

- `BigDecimal -> BigDecimal` 加 `numberFormat` 不会改变 scale；
- `double -> BigDecimal` 不能靠 `numberFormat` 完成，需 `expression` 或自定义方法；
- `numberFormat` 使用 `DecimalFormat` 模式，默认受 JVM Locale 影响；需要固定格式时可用 `expression` 显式 `setScale`。

### 6.4 自定义 `default` 方法

自定义方法优先于内置转换。把它写在 Mapper 接口里，用 `default` 提供实现：

```java
@Mapper(componentModel = "spring")
public interface UserExcelMapper {

    @Mapping(target = "ageGroup", source = "age", qualifiedByName = "ageToGroup")
    UserExcel toExcel(User user);

    List<UserExcel> toExcelList(List<User> users);

    /**
     * 与 ExcelServiceImpl#toAgeGroup 保持同一分组口径。
     */
    @Named("ageToGroup")
    default String ageToGroup(Integer age) {
        if (age == null) {
            return "未知";
        }
        if (age < 18) {
            return "青少年";
        }
        return age < 60 ? "成年" : "老年";
    }
}
```

这段映射可以直接替代 [ExcelServiceImpl.java](file:///d:/Project/IdeaProjects/local-project/project-my/spring/src/main/java/com/qq/ijay997/service/impl/ExcelServiceImpl.java#L49-L58) 中的循环与 Builder 组装（前提是 `UserExcel` 的字段名与 `User` 对齐，当前 `id`、`username`、`email`、`age` 都对齐）。

### 6.5 转换方法的选择顺序

同类型转换有多个候选时，MapStruct 大致按以下优先级选择：

1. 当前 Mapper 中签名匹配的 `default`/`abstract` 方法；
2. `uses` 引入的其他 Mapper 中的方法；
3. MapStruct 内置转换；
4. 仍无法确定则编译报错，需用 `@Named` + `qualifiedByName` 明确指定。

因此不要在同一个 Mapper 里放多个 `String -> String` 且语义不同的方法而不加 `@Named`，否则会出现 “Ambiguous mapping methods” 报错。

### 6.6 枚举转换

```java
// 示例枚举
public enum UserStatus {
    ACTIVE, DISABLED
}
```

- 枚举 → String：默认取 `name()`；
- String → 枚举：默认 `valueOf()`，非法值抛 `IllegalArgumentException`；
- 需要自定义文案或容错时，写 `default` 方法并加 `@Named`；
- 枚举 → 枚举可用 `@ValueMapping` 显式声明对应关系。

---

## 七、与 Spring 集成

### 7.1 `componentModel = "spring"`

```java
@Mapper(componentModel = "spring")
public interface UserMapper {
    UserDTO toDTO(User user);
}
```

生成的实现类会自动带上 `@org.springframework.stereotype.Component`，等价于：

```java
@Component
public class UserMapperImpl implements UserMapper {
    // ...
}
```

Spring Boot 的组件扫描范围是 `com.qq.ijay997`，`com.qq.ijay997.mapper` 是其子包，因此**不需要额外配置 `@ComponentScan` 或 `@MapperScan`**。

### 7.2 在 Service / Controller 中注入

```java
import com.qq.ijay997.dto.UserDTO;
import com.qq.ijay997.mapper.UserMapper;
import com.qq.ijay997.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserQueryController {

    private final UserService userService;
    private final UserMapper userMapper;

    public UserQueryController(UserService userService, UserMapper userMapper) {
        this.userService = userService;
        this.userMapper = userMapper;
    }

    @GetMapping("/dto")
    public List<UserDTO> list() {
        return userMapper.toDTOList(userService.findAll());
    }
}
```

### 7.3 单元测试

`componentModel = "spring"` 时，生产代码应通过容器注入；测试中如果 Mapper 不依赖其他 Bean，可以直接实例化生成类：

```java
import com.qq.ijay997.dto.UserDTO;
import com.qq.ijay997.entity.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class UserMapperTest {

    private final UserMapper mapper = new UserMapperImpl();

    @Test
    void shouldMapUserToDTO() {
        User user = new User();
        user.setId(1L);
        user.setUsername("  ijay997  ");
        user.setEmail("ijay997@example.com");
        user.setAge(30);

        UserDTO dto = mapper.toDTO(user);

        assertNotNull(dto);
        assertEquals(1L, dto.getId());
        assertEquals("ijay997", dto.getUsername());
        assertEquals("13800000001", dto.getPhone());
    }
}
```

如果 Mapper 通过 `uses` 依赖其他 Mapper，建议用 `@SpringBootTest` 注入，或手动传入依赖实例，不要依赖 `Mappers.getMapper(...)`。

### 7.4 全局配置 `@MapperConfig`

多个 Mapper 需要统一组件模型、未映射策略时，可以抽一个配置接口：

```java
import org.mapstruct.MapperConfig;
import org.mapstruct.ReportingPolicy;

@MapperConfig(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface BaseMapperConfig {
}
```

```java
@Mapper(config = BaseMapperConfig.class)
public interface UserMapper {
    // ...
}
```

建议：

- 出参 DTO 映射用 `ReportingPolicy.ERROR`，让漏映射在编译期暴露；
- 入参转实体、更新方法可以先用 `WARN`，避免因为业务字段过多导致编译失败；
- 一旦使用 `uses`，被引用的 Mapper 也必须是 `spring` 模型。

---

## 八、常见坑与排错

### 8.1 编译后没有生成 `*Impl`

按顺序排查：

1. `mapstruct` 依赖是否已加入 `spring/pom.xml`；
2. `mapstruct-processor` 是否只放在 `annotationProcessorPaths`，而不是普通依赖；
3. `annotationProcessorPaths` 中 Lombok → binding → MapStruct 顺序是否正确；
4. IDEA 是否启用了 Annotation Processing；
5. 执行 `mvn -pl spring -am clean compile`，不要只点 IDEA 的 Build；
6. 检查 `spring/target/generated-sources/annotations/com/qq/ijay997/mapper/`；
7. 仍然异常时执行 `File → Invalidate Caches / Restart`。

### 8.2 Lombok 实体映射全部失效

典型报错：

```text
Property "xxx" has no read accessor
```

原因与处理：

- Lombok 在 MapStruct 之后执行，MapStruct 还看不到 getter/setter；
- `annotationProcessorPaths` 顺序必须是 Lombok → `lombok-mapstruct-binding` → `mapstruct-processor`；
- JDK 17/21 下 Lombok 版本过低（Boot 2.7.18 默认 1.18.24，JDK 21 需要 1.18.30+）；
- 使用 `@Builder` 的 DTO（如 `UserExcel`）必须加 `lombok-mapstruct-binding`，否则 MapStruct 识别不到 builder。

### 8.3 `Unmapped target property: xxx`

含义：目标类有属性 `xxx`，但源里找不到对应来源。

处理方式：

| 场景 | 处理 |
|---|---|
| 目标字段由数据库/框架生成（如 `id`） | `@Mapping(target = "id", ignore = true)` |
| 字段名不一致 | 显式写 `source` |
| 需要计算 | 用 `expression` 或 `default` 方法 |
| 确实不需要 | `ignore = true` |
| 团队要求零遗漏 | `unmappedTargetPolicy = ReportingPolicy.ERROR` |

本工程里 `Order -> OrderDTO` 必须忽略 `id`、`orderNo`、`userId`、`productName`、`createTime`、`updateTime`，否则开启 ERROR 策略会直接编译失败。

### 8.4 null 处理

| 配置 | 作用范围 | 可选值 |
|---|---|---|
| `nullValueMappingStrategy` | Bean / 集合整体 | `RETURN_NULL` / `RETURN_DEFAULT` |
| `nullValueIterableMappingStrategy` | 集合 | 同上 |
| `nullValueMapMappingStrategy` | Map | 同上 |
| `nullValuePropertyMappingStrategy` | 更新方法的单个属性 | `SET_TO_NULL` / `IGNORE` / `SET_TO_DEFAULT` |
| `nullValueCheckStrategy` | 是否生成 null 判断 | `IMMEDIATE` / `ON_DEMAND` |

推荐：

- 出参集合统一 `RETURN_DEFAULT`，调用方少写判空；
- PATCH 更新统一 `NullValuePropertyMappingStrategy.IGNORE`，避免 null 覆盖存量数据；
- 基本类型目标（`int`、`double`）遇到包装类型 null 时可能拆箱 NPE，优先把 DTO 字段改成包装类型，或在映射前做默认值处理。

### 8.5 `dateFormat` / `numberFormat` 不生效

- 两者只在目标/源为 `String` 时生效；
- 日期/时间模式区分大小写：`yyyy-MM-dd HH:mm:ss`；
- `numberFormat` 不会改变 `BigDecimal` 的 scale；
- `double -> BigDecimal` 需要 `expression` 或自定义方法；
- 需要固定 Locale 时，用 `expression` 显式格式化（例如 `String.format(Locale.ROOT, ...)`）。

### 8.6 字段名不一致或拼写错误

`Order` 现有字段是 `amout`（少了一个 `n`），而 `OrderDTO` 是 `amount`。不要依赖“自动同名匹配”，必须显式写：

```java
@Mapping(target = "amount", source = "amout")
```

如果同时存在类型不一致（`double -> BigDecimal`），改用：

```java
@Mapping(target = "amount", expression = "java(java.math.BigDecimal.valueOf(order.getAmout()))")
```

### 8.7 `Can't map Collection element "A to B"`

说明集合入口 `List<A> -> List<B>` 找到了，但元素级方法 `B map(A)` 缺失。

处理：

1. 在当前 Mapper 补 `B map(A)`；
2. 或在集合入口上用 `@IterableMapping(qualifiedByName = "...")` 绑定已有方法；
3. 检查同模块所有返回 `List`/`Set` 的入口，一次性补齐，避免反复编译。

### 8.8 `@Mapper` 与 MyBatis 冲突

- MapStruct：`org.mapstruct.Mapper`
- MyBatis：`org.apache.ibatis.annotations.Mapper`

本工程目前没有 MyBatis 依赖，但 `mapper` 是 MyBatis 的常见包名。未来同时引入时：

- 不要使用通配 import；
- 不要把 Spring 的 `@MapperScan`（MyBatis 提供）用于 MapStruct；
- 接口命名避免与 MyBatis Mapper 重名，例如 MapStruct 侧用 `UserConvertMapper` 或 `UserConverter`。

### 8.9 `componentModel` 混用

如果 `UserMapper` 是 `spring` 模型，而 `uses = {AddressMapper.class}` 中的 `AddressMapper` 是默认模型，生成代码会尝试 `new AddressMapperImpl()` 或注入失败。解决：所有参与组合的 Mapper 统一 `componentModel = "spring"`。

### 8.10 `expression` 报“找不到符号 source”

表达式里能引用的是**映射方法的真实参数名**：

```java
@Mapping(target = "amount",
         expression = "java(java.math.BigDecimal.valueOf(order.getAmout()))")
OrderDTO toDTO(Order order);
```

如果编译拿不到参数名，检查：

```xml
<parameters>true</parameters>
```

### 8.11 基本类型 null 拆箱

源字段是 `Integer`、目标字段是 `int` 时，源为 null 会生成 `Integer.intValue()` 并抛出 `NullPointerException`。处理：

- 目标字段改用包装类型；
- 或使用 `defaultValue = "0"`；
- 或在 `@AfterMapping` / `default` 方法里补默认值。

### 8.12 生成的实现类 IDEA 报红

- 先执行 Maven 编译；
- 确认 `target/generated-sources/annotations` 被识别；
- 确认处理器路径配置正确；
- 清理 IDEA 缓存。

### 8.13 `BigDecimal` 断言使用 `equals`

`new BigDecimal("199.0")` 与 `new BigDecimal("199.00")` 的 `equals` 为 false。测试断言用：

```java
assertEquals(0, actual.compareTo(expected));
```

### 8.14 不要直接抄根目录指南的 record 示例

根目录指南面向 JDK 21，包含 record、sealed、`@SubclassMapping` 等示例。`spring` 模块是 Java 11，record/sealed 无法编译。本文的示例全部基于普通 JavaBean，可直接使用。

### 8.15 JPA 懒加载字段

`User` 当前只有普通字段，暂时没有懒加载集合。未来给实体增加 `@OneToMany(fetch = LAZY)` 后，不要在 Mapper 的 `toDTO` 中直接遍历集合属性，否则可能在序列化/映射阶段触发额外查询。应在 Service 层明确加载后再映射。

---

## 九、在本工程落地

### 9.1 建议新增的 Mapper 文件

按当前目录结构，建议放在 `spring/src/main/java/com/qq/ijay997/mapper/`：

```text
mapper/
├── UserMapper.java          # User <-> UserDTO，含集合、更新、phone 派生
├── UserExcelMapper.java     # User -> UserExcel，含 ageGroup 计算
├── OrderMapper.java         # Order -> OrderDTO，处理 amout/amount 与 score/status
└── MAPSTRUCT_GUIDE.md       # 本文
```

如果后续需要地址转换，再新增 `AddressMapper.java`，不要把所有转换都塞进一个巨型 Mapper。

### 9.2 替换 `MaskingDemoController` 中的手写转换

现有代码：

```java
List<UserDTO> users = userService.findAll().stream()
        .map(this::toDTO)
        .collect(Collectors.toList());
```

替换为：

```java
List<UserDTO> users = userMapper.toDTOList(userService.findAll());
```

`@Sensitive` 不需要在 Mapper 中处理：`UserDTO` 上的注解会在 Jackson 序列化阶段按权限脱敏，Mapper 只负责把原值填进去。

### 9.3 替换 `ExcelServiceImpl` 中的循环组装

现有代码：

```java
List<UserExcel> rows = new ArrayList<>(users.size());
for (User user : users) {
    rows.add(UserExcel.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .age(user.getAge())
            .ageGroup(toAgeGroup(user.getAge()))
            .build());
}
```

替换为：

```java
List<UserExcel> rows = userExcelMapper.toExcelList(users);
```

并且把 `toAgeGroup` 的逻辑收敛到 `UserExcelMapper#ageToGroup`，保证导出与统计使用同一口径。

### 9.4 落地顺序建议

1. 先补 `spring/pom.xml` 的依赖与处理器配置；
2. 新增 `UserMapper`，先只做 `toDTO` / `toDTOList`，编译验证生成类；
3. 再补 `toEntity` / `updateEntity`，确认更新语义符合业务；
4. 迁移 `MaskingDemoController` 的查询转换；
5. 新增 `UserExcelMapper`，迁移 `ExcelServiceImpl`；
6. 最后处理 `OrderMapper`（字段拼写和类型不一致最多，单独验证）；
7. 每个 Mapper 至少写一条完整对象映射测试 + 一条 null/空集合测试。

### 9.5 测试建议

- 完整对象：验证所有业务字段、派生字段、格式化字段；
- 边界：`null` 源对象、空集合、属性为 null 的更新场景；
- 更新语义：确认 `IGNORE` 不会把已有值覆盖成 null；
- 金额：用 `BigDecimal.compareTo` 断言；
- 日期：用精确字符串断言（如 `2026-11-04 10:30:00`）。

---

## 十、参考资料

- 根目录 JDK 21 指南：[mapstruct-jdk21-guide.md](file:///d:/Project/IdeaProjects/local-project/project-my/mapstruct-jdk21-guide.md)
- MapStruct 安装文档：https://mapstruct.org/documentation/installation/
- MapStruct 参考指南：https://mapstruct.org/documentation/reference-guide/
- MapStruct 官方示例：https://github.com/mapstruct/mapstruct-examples
- IDEA 插件：MapStruct Support（JetBrains Marketplace）
- 本工程相关代码：
  - [User.java](file:///d:/Project/IdeaProjects/local-project/project-my/spring/src/main/java/com/qq/ijay997/entity/User.java)
  - [UserDTO.java](file:///d:/Project/IdeaProjects/local-project/project-my/spring/src/main/java/com/qq/ijay997/dto/UserDTO.java)
  - [Order.java](file:///d:/Project/IdeaProjects/local-project/project-my/spring/src/main/java/com/qq/ijay997/entity/Order.java)
  - [OrderDTO.java](file:///d:/Project/IdeaProjects/local-project/project-my/spring/src/main/java/com/qq/ijay997/dto/OrderDTO.java)
  - [UserExcel.java](file:///d:/Project/IdeaProjects/local-project/project-my/spring/src/main/java/com/qq/ijay997/dto/UserExcel.java)
  - [spring/pom.xml](file:///d:/Project/IdeaProjects/local-project/project-my/spring/pom.xml)
