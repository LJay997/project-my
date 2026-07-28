用户问的是如何快速创建一个自己的 Spring Boot Starter 项目。这是一个关于 Java/Spring Boot 的问题，我需要给出一个清晰、步骤明确的回答。

Spring Boot Starter 是一种约定大于配置的方式，让开发者可以快速集成各种功能。创建自己的 Starter 通常需要：
1. 创建一个 Maven/Gradle 项目
2. 添加必要的依赖
3. 创建自动配置类
4. 创建配置属性类
5. 在 resources/META-INF 下创建 spring.factories 文件

我应该给出一个详细的步骤指南，包括：
1. 使用 Spring Initializr 创建项目
2. 配置 pom.xml
3. 创建配置属性类
4. 创建自动配置类
5. 创建 spring.factories
6. 打包和使用

让我提供一个完整的、可操作的指南。
创建自己的 Spring Boot Starter 项目非常简单！下面是详细的步骤指南：

---

### **步骤 1：使用 Spring Initializr 创建基础项目**

访问 [Spring Initializr](https://start.spring.io/)：
- **Project**: Maven Project（推荐）
- **Language**: Java
- **Spring Boot**: 选择最新稳定版本
- **Group/Artifact**: 填写您的包名和项目名
- **Dependencies**: 只需要勾选 `Spring Boot Starter`（基础依赖）

点击 **Generate** 下载项目。

---

### **步骤 2：配置 `pom.xml`**

添加必要的依赖和配置：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starters</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>my-spring-boot-starter</artifactId>
    <version>1.0.0</version>
    <name>my-spring-boot-starter</name>
    <description>自定义 Spring Boot Starter</description>

    <properties>
        <java.version>21</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot 自动配置支持 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-autoconfigure</artifactId>
        </dependency>

        <!-- 配置属性绑定支持 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-configuration-processor</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- Lombok（可选，简化代码） -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>

        <!-- 测试依赖 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
                <configuration>
                    <excludes>
                        <exclude>
                            <groupId>org.projectlombok</groupId>
                            <artifactId>lombok</artifactId>
                        </exclude>
                    </excludes>
                </configuration>
            </plugin>
        </plugins>
    </build>
</project>
```

---

### **步骤 3：创建配置属性类**

创建 `MyStarterProperties.java`，用于接收用户配置：

```java
package com.example.mystarter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "my.starter")
public class MyStarterProperties {

    // 是否启用
    private boolean enabled = true;

    // 自定义配置项
    private String appName = "My Starter";

    // 超时时间（毫秒）
    private int timeout = 5000;
}
```

---

### **步骤 4：创建自动配置类**

创建 `MyStarterAutoConfiguration.java`，实现自动配置逻辑：

```java
package com.example.mystarter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(MyStarterService.class)  // 当类路径中存在 MyStarterService 时生效
@EnableConfigurationProperties(MyStarterProperties.class)  // 启用配置属性
@ConditionalOnProperty(prefix = "my.starter", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MyStarterAutoConfiguration {

    private final MyStarterProperties properties;

    public MyStarterAutoConfiguration(MyStarterProperties properties) {
        this.properties = properties;
    }

    @Bean
    @ConditionalOnMissingBean  // 当容器中不存在该 Bean 时才创建
    public MyStarterService myStarterService() {
        return new MyStarterService(properties);
    }
}
```

---

### **步骤 5：创建核心服务类**

创建 `MyStarterService.java`，实现具体功能：

```java
package com.example.mystarter;

public class MyStarterService {

    private final MyStarterProperties properties;

    public MyStarterService(MyStarterProperties properties) {
        this.properties = properties;
    }

    public String sayHello() {
        return String.format("Hello from %s! Timeout: %dms",
            properties.getAppName(),
            properties.getTimeout());
    }
}
```

---

### **步骤 6：创建 `spring.factories` 文件**

在 `src/main/resources/META-INF/` 目录下创建 `spring.factories`：

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.mystarter.MyStarterAutoConfiguration
```

---

### **步骤 7：打包并发布**

运行以下命令打包：

```bash
mvn clean install
```

打包成功后，您的 Starter 就会被安装到本地 Maven 仓库。

---

### **步骤 8：在其他项目中使用**

在目标项目的 `pom.xml` 中添加依赖：

```xml
<dependency>
    <groupId>com.example</groupId>
    <artifactId>my-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

在 `application.yml` 中配置：

```yaml
my:
  starter:
    enabled: true
    app-name: "My Custom Starter"
    timeout: 10000
```

在代码中使用：

```java
@RestController
public class TestController {

    private final MyStarterService starterService;

    public TestController(MyStarterService starterService) {
        this.starterService = starterService;
    }

    @GetMapping("/hello")
    public String hello() {
        return starterService.sayHello();
    }
}
```

---

### **关键注解说明**

| 注解 | 作用 |
|------|------|
| `@ConfigurationProperties` | 绑定配置文件中的属性 |
| `@EnableConfigurationProperties` | 启用配置属性类 |
| `@ConditionalOnClass` | 当类路径存在指定类时生效 |
| `@ConditionalOnMissingBean` | 当容器中不存在指定 Bean 时创建 |
| `@ConditionalOnProperty` | 根据配置属性决定是否生效 |

---

### **进阶：使用 `ImportSelector` 动态导入配置**

`ImportSelector` 是 Spring 提供的一个强大接口，允许**动态选择**需要导入的配置类。这在 Starter 开发中非常有用，可以根据不同条件导入不同的配置。

#### **什么是 `ImportSelector`**

`ImportSelector` 接口只有一个核心方法：
```java
String[] selectImports(AnnotationMetadata importingClassMetadata);
```

该方法返回需要导入的配置类全限定名数组，Spring 会自动将这些类注册为 Bean。

#### **创建自定义 `ImportSelector`**

创建 `MyStarterImportSelector.java`：
```java
package com.example.mystarter;

import org.springframework.context.annotation.ImportSelector;
import org.springframework.core.type.AnnotationMetadata;

public class MyStarterImportSelector implements ImportSelector {

    @Override
    public String[] selectImports(AnnotationMetadata importingClassMetadata) {
        // 获取注解属性（如果需要）
        // Map<String, Object> attributes = importingClassMetadata.getAnnotationAttributes(EnableMyStarter.class.getName());
        
        // 根据条件动态决定要导入的配置类
        boolean needAdvancedFeature = true; // 可以从配置文件或其他来源获取
        
        if (needAdvancedFeature) {
            // 导入高级配置
            return new String[]{
                MyStarterAutoConfiguration.class.getName(),
                MyStarterAdvancedConfiguration.class.getName()
            };
        } else {
            // 只导入基础配置
            return new String[]{
                MyStarterAutoConfiguration.class.getName()
            };
        }
    }
}
```

#### **创建注解驱动的启用方式**

创建 `@EnableMyStarter` 注解，让用户可以显式启用您的 Starter：

```java
package com.example.mystarter;

import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(MyStarterImportSelector.class)
public @interface EnableMyStarter {
    
    // 可以定义配置属性
    boolean advanced() default true;
}
```

#### **创建高级配置类**

```java
package com.example.mystarter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyStarterAdvancedConfiguration {

    @Bean
    public MyAdvancedService myAdvancedService() {
        return new MyAdvancedService();
    }
}
```

```java
package com.example.mystarter;

public class MyAdvancedService {
    
    public String advancedOperation() {
        return "Advanced feature enabled!";
    }
}
```

#### **使用方式**

用户可以通过两种方式使用：

**方式一：自动配置（通过 `spring.factories`）**
```java
// 无需任何注解，自动生效
@SpringBootApplication
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

**方式二：注解显式启用**
```java
@SpringBootApplication
@EnableMyStarter(advanced = true)
public class MyApplication {
    public static void main(String[] args) {
        SpringApplication.run(MyApplication.class, args);
    }
}
```

#### **`ImportSelector` vs `@Conditional`**

| 特性 | `ImportSelector` | `@Conditional` |
|------|------------------|----------------|
| **动态性** | 高，可以运行时决定导入哪些类 | 中等，基于条件判断是否生效 |
| **复杂度** | 较高，需要实现接口 | 较低，注解方式 |
| **适用场景** | 需要根据复杂逻辑动态选择配置 | 简单的条件判断 |
| **灵活性** | 非常灵活，可以完全控制导入逻辑 | 相对固定，基于预设条件 |

#### **在 `spring.factories` 中注册 `ImportSelector`**

您也可以将 `ImportSelector` 注册到 `spring.factories`：

```properties
org.springframework.boot.autoconfigure.EnableAutoConfiguration=\
com.example.mystarter.MyStarterAutoConfiguration,\
com.example.mystarter.MyStarterImportSelector
```

---

### **项目结构**

```
my-spring-boot-starter/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/example/mystarter/
│       │       ├── MyStarterProperties.java          # 配置属性类
│       │       ├── MyStarterService.java             # 核心服务类
│       │       ├── MyStarterAutoConfiguration.java   # 自动配置类
│       │       ├── MyStarterImportSelector.java      # 动态导入选择器
│       │       ├── EnableMyStarter.java              # 启用注解
│       │       ├── MyStarterAdvancedConfiguration.java # 高级配置类
│       │       └── MyAdvancedService.java            # 高级服务类
│       └── resources/
│           └── META-INF/
│               └── spring.factories                   # 自动配置入口
└── pom.xml
```

这样您就成功创建了一个自定义的 Spring Boot Starter！🎉