# String 面试题（结构化版）

> 适用：JDK 8/11/17 常见面试（文中会标注 JDK 6/7/9 的差异点）。

## 📚 目录

1. [String 基础与底层](#1-string-基础与底层)
2. [字符串常量池与对象创建](#2-字符串常量池与对象创建)
3. [不可变性：原因、收益与陷阱](#3-不可变性原因收益与陷阱)
4. [String vs StringBuilder vs StringBuffer](#4-string-vs-stringbuilder-vs-stringbuffer)
5. [高频题型与代码输出题](#5-高频题型与代码输出题)
6. [intern() 详解（含版本差异对比表）](#6-intern-详解含版本差异对比表)
7. [常用 API 易错点（对比表）](#7-常用-api-易错点对比表)
8. [性能优化：拼接、正则、编码与分配](#8-性能优化拼接正则编码与分配)
9. [面试速记：一页表格](#9-面试速记一页表格)

---

<a id="1-string-基础与底层"></a>
## 1. String 基础与底层

### 1.1 String 是基本数据类型吗？

- **结论**：❌ 不是。`String` 是 **引用类型**，类为 `java.lang.String`。
- **常见追问**：Java 8 种基本类型是什么？
  - **整数**：`byte`, `short`, `int`, `long`
  - **浮点**：`float`, `double`
  - **字符**：`char`
  - **布尔**：`boolean`

---

### 1.2 String 底层存储是什么？

- **结论**：
  - **JDK 8 及以前**：`char[] value`
  - **JDK 9+（Compact Strings）**：`byte[] value + byte coder(LATIN1/UTF16)`

```java
// JDK 8-
public final class String {
    private final char[] value;
}
```

```java
// JDK 9+
public final class String {
    private final byte[] value;
    private final byte coder; // LATIN1 或 UTF16
}
```

- **为什么变更**：多数业务字符串落在 Latin-1，`byte[]` 能显著省内存并提升缓存命中率。

---

### 1.3 String 为什么是 final？

- **一句话**：为 **安全性 + 可共享（常量池）+ 语义稳定** 服务。
- **常见点**：
  - **安全性**：类加载、反射、URL、文件路径等安全敏感参数不希望被子类篡改语义
  - **共享**：常量池共享同一对象，必须保证不会被修改
  - **Hash 语义稳定**：不可变对象适合作为 `HashMap` key（hash 可缓存且不会变）

---

### 1.4 `length()` 是“字符数”吗？（Unicode 易错点）

- **结论**：`String.length()` 返回的是 **UTF-16 code unit 数**，不等于“用户看到的字符数”。

```java
String s = "😊";
System.out.println(s.length()); // 2（代理对）
System.out.println(s.codePointCount(0, s.length())); // 1
```

- **易错点**：表情、部分生僻字、组合字符会让“字符长度”与 `length()` 不一致。

---

<a id="2-字符串常量池与对象创建"></a>
## 2. 字符串常量池与对象创建

### 2.1 什么是字符串常量池（String Constant Pool）？

- **结论**：JVM 维护的一块用于 **复用字符串字面量** 的区域。
- **位置**：
  - **JDK 6**：永久代
  - **JDK 7+**：堆（HotSpot 实现）

```java
String a = "hello";
String b = "hello";
System.out.println(a == b); // true
```

---

### 2.2 `new String("hello")` 创建几个对象？

```java
String s1 = "hello";
String s2 = new String("hello");
```

- **结论（面试口径）**：
  - `s1`：若常量池无 `"hello"`，会把字面量放入池（可视为 1 个对象）；若已有则不新增。
  - `s2`：**一定**在堆上 new 出一个 `String` 对象（至少 1 个）。
- **更严谨的易错点**：不同 JDK/实现对“字面量何时入池、是否已存在”的描述略有差异；回答时抓住核心：**`new` 一定有堆对象，字面量复用常量池**。

---

### 2.3 `==` 与 `equals()` 的区别？（必背）

| 对比项 | `==` | `equals()` |
|---|---|---|
| **比较内容** | 引用地址 | 逻辑相等（`String` 比内容） |
| **String 场景** | 是否同一对象 | 内容是否相同 |
| **面试建议** | 尽量别用来比内容 | 比内容用它（或 `Objects.equals`） |

```java
String x = "hello";
String y = new String("hello");
System.out.println(x == y);      // false
System.out.println(x.equals(y)); // true
```

---

<a id="3-不可变性原因收益与陷阱"></a>
## 3. 不可变性：原因、收益与陷阱

### 3.1 什么是不可变？

- **结论**：`String` 一旦创建，内容不可改；任何“修改”都会产生新对象。

```java
String s = "hello";
String t = s + " world";
System.out.println(s); // hello
System.out.println(t); // hello world
```

---

### 3.2 不可变性如何实现？

- **核心点**：
  - 类 `final`，字段 `private final`
  - 不提供可变更内部数组的 API
  - 构造与部分场景使用 **防御性拷贝**

---

### 3.3 不可变性的收益（怎么“说人话”）

- **线程安全**：共享无需同步
- **安全**：避免敏感参数被篡改语义
- **可缓存**：`hashCode` 可缓存（实现层面会做）
- **常量池共享**：复用省内存

---

### 3.4 易错点：`String` 做 key 的坑

- **结论**：`String` 适合做 key，但要注意 **大小写、空白、规范化**。

```java
Map<String, Integer> map = new HashMap<>();
map.put("a", 1);
System.out.println(map.get("A")); // null（大小写不同）
```

- **建议**：必要时先 `trim/strip`、统一大小写、统一编码规范（如 NFC/NFD）。

---

<a id="4-string-vs-stringbuilder-vs-stringbuffer"></a>
## 4. String vs StringBuilder vs StringBuffer

### 4.1 三者对比（高频表）

| 维度 | String | StringBuilder | StringBuffer |
|---|---|---|---|
| **可变性** | 不可变 | 可变 | 可变 |
| **线程安全** | 天然安全（不可变） | 不安全 | 安全（方法多为 `synchronized`） |
| **性能（拼接）** | 最慢（频繁新对象） | 最快 | 次之（同步开销） |
| **典型场景** | 少量操作/常量 | 单线程大量拼接 | 多线程共享拼接（较少见） |

---

### 4.2 `+` 拼接一定慢吗？（追问点）

- **结论**：
  - **编译期常量拼接**：会被编译器折叠，不慢
  - **运行期循环拼接**：慢（会反复创建对象 / builder）

```java
String a = "hello" + "world"; // 编译期折叠 => "helloworld"

String s = "";
for (int i = 0; i < 10000; i++) {
    s += i; // 运行期反复创建
}
```

---

<a id="5-高频题型与代码输出题"></a>
## 5. 高频题型与代码输出题

### 5.1 输出题：哪些在常量池，哪些在堆？

```java
String s1 = "hello";
String s2 = "world";
String s3 = "helloworld";
String s4 = "hello" + "world";
String s5 = s1 + "world";
String s6 = s1 + s2;
String s7 = (s1 + s2).intern();

System.out.println(s3 == s4);  // ?
System.out.println(s3 == s5);  // ?
System.out.println(s3 == s6);  // ?
System.out.println(s3 == s7);  // ?
```

- **参考答案（JDK 7+ 常见口径）**：
  - `s3 == s4`：`true`（编译期折叠）
  - `s3 == s5`：`false`（运行期拼接，新对象）
  - `s3 == s6`：`false`（运行期拼接，新对象）
  - `s3 == s7`：`true`（`intern()` 返回池中引用）

---

### 5.2 `switch` 能用 String 吗？原理是什么？

- **结论**：✅ JDK 7+ 支持。
- **原理（面试一句话）**：编译器把它转成 `hashCode()` + `equals()` 的分发逻辑（并非直接比引用）。

---

### 5.3 `String.valueOf(obj)` vs `obj.toString()`（易错）

- **结论**：`String.valueOf(null)` 返回 `"null"`；`null.toString()` 直接 NPE。

```java
Object o = null;
System.out.println(String.valueOf(o)); // "null"
// System.out.println(o.toString());   // NPE
```

---

### 5.4 反转字符串（补“正确处理 Unicode”版本）

- **普通 ASCII 场景（够用）**：

```java
String str = "hello";
String reversed = new StringBuilder(str).reverse().toString();
```

- **Unicode 码点场景（更严谨）**：

```java
int[] cps = "a😊b".codePoints().toArray();
StringBuilder sb = new StringBuilder();
for (int i = cps.length - 1; i >= 0; i--) {
    sb.appendCodePoint(cps[i]);
}
System.out.println(sb.toString()); // b😊a
```

---

<a id="6-intern-详解含版本差异对比表"></a>
## 6. intern() 详解（含版本差异对比表）

### 6.1 `intern()` 的作用与规则

- **结论**：返回常量池中“等值字符串”的引用；没有则把当前字符串**登记/放入**池中（实现细节依 JDK 版本）。

```java
String s1 = "hello";
String s2 = new String("hello").intern();
System.out.println(s1 == s2); // true
```

---

### 6.2 JDK 6 vs JDK 7+：intern 差异表

| 维度 | JDK 6 | JDK 7+ |
|---|---|---|
| **常量池位置** | 永久代 | 堆 |
| **intern 行为** | 往池里放“副本”（复制） | 池记录对堆字符串的引用（更常见口径：不复制） |
| **内存风险** | 容易 PermGen OOM | 池过大也会占堆，影响 GC |

> 易错点：面试别死抠“100% 一定 true/false”，讲清版本与机制即可拿分。

---

<a id="7-常用-api-易错点对比表"></a>
## 7. 常用 API 易错点（对比表）

### 7.1 空判断对比表

| 目标 | 推荐写法 | 说明 |
|---|---|---|
| 判空引用 | `str == null` | 只判断是否 null |
| 判空串 | `str != null && !str.isEmpty()` | `isEmpty()` 等价 `length()==0` |
| 判空白（含空格/制表） | `str != null && !str.isBlank()` | JDK 11+ |

---

### 7.2 `replace` vs `replaceAll` vs `replaceFirst`

| 方法 | 参数 | 是否正则 | 典型坑 |
|---|---|---|---|
| `replace(CharSequence, CharSequence)` | 字面量 | 否 | 最安全，性能好 |
| `replaceAll(String regex, String repl)` | 正则 | ✅ | `.` `*` `?` 需要转义；性能受正则影响 |
| `replaceFirst(String regex, String repl)` | 正则 | ✅ | 只替换第一个匹配 |

```java
System.out.println("a.b".replace(".", "_"));    // a_b（字面量）
System.out.println("a.b".replaceAll(".", "_")); // ___（正则：任意字符）
System.out.println("a.b".replaceAll("\\.", "_"));// a_b（转义点）
```

---

### 7.3 `trim()` vs `strip()`（JDK 11+）

| 方法 | 处理范围 | 备注 |
|---|---|---|
| `trim()` | \u0020 及少量 ASCII 空白 | 传统方法 |
| `strip()` | Unicode 空白 | 更符合“用户输入”场景 |

---

### 7.4 `split()` 的坑

- **结论**：`split` 用的是 **正则**，并且会处理尾部空串（默认会丢）。

```java
System.out.println(Arrays.toString("a,b,".split(",")));   // [a, b]
System.out.println(Arrays.toString("a,b,".split(",", -1)));// [a, b, ]
```

---

<a id="8-性能优化拼接正则编码与分配"></a>
## 8. 性能优化：拼接、正则、编码与分配

### 8.1 循环拼接：用 StringBuilder（并预估容量）

```java
int n = 10_000;
StringBuilder sb = new StringBuilder(n * 5); // 粗略预估，减少扩容
for (int i = 0; i < n; i++) {
    sb.append(i);
}
String result = sb.toString();
```

---

### 8.2 正则热点：预编译 Pattern 或避免正则

```java
Pattern comma = Pattern.compile(",");
for (String line : lines) {
    String[] parts = comma.split(line);
}
```

---

### 8.3 `String.format` 不是性能最优

- **结论**：可读性好，但在热点路径通常不如 `StringBuilder` / 模板化日志参数（如 slf4j）。

---

### 8.4 `intern()` 的使用建议

- **适合**：海量重复、生命周期长、值域小的字符串（状态码、协议常量等）
- **不适合**：用户输入、无界增长、短生命周期字符串（容易把池撑大）

---

<a id="9-面试速记一页表格"></a>
## 9. 面试速记：一页表格

### 9.1 必背结论表

| 题 | 一句话结论 |
|---|---|
| String 不可变？ | 设计成不可变以保证共享安全、线程安全与 hash 语义稳定 |
| `==` vs `equals`？ | `==` 比引用，`equals`（String）比内容 |
| 常量池在哪？ | JDK 7+ 在堆；JDK 6 在永久代 |
| `+` 拼接优化？ | 编译期常量会折叠；循环运行期拼接用 Builder |
| `intern()`？ | 返回池中引用；版本差异要说明 |
| `length()`？ | 返回 UTF-16 code unit 数，不等于“字符数” |

---

### 9.2 String / Builder / Buffer 对比表（复习版）

| 特性 | String | StringBuilder | StringBuffer |
|---|---|---|---|
| **不可变** | ✅ | ❌ | ❌ |
| **线程安全** | ✅ | ❌ | ✅ |
| **拼接性能** | 慢 | 快 | 中 |
| **适用场景** | 少量操作/常量 | 单线程拼接 | 多线程共享拼接 |
