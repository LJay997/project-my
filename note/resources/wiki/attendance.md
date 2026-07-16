# Attendance Service 考勤服务 Wiki

## 一、项目概述

### 1.1 功能定位

`attendance-service` 是 GLM（广联达劳动管理）系统的**核心考勤服务模块**，负责处理项目现场工人的考勤打卡、规则校验、数据统计和报表生成等业务。

### 1.2 技术栈

| 分类 | 技术 | 版本 |
|------|------|------|
| 框架 | Spring Boot | 2.x |
| 微服务 | Spring Cloud (Eureka/Feign/Hystrix) | - |
| 数据库 | MySQL + MongoDB | MySQL 5.7+ |
| 缓存 | Redis + Caffeine | - |
| 消息队列 | RabbitMQ | - |
| ORM | MyBatis (tk.mybatis) | 2.1.5 |
| API文档 | Swagger2 | 2.9.2 |
| 小程序 | 微信小程序 SDK | 3.6.0 |

### 1.3 项目版本

- **版本号**：3.4.0-SNAPSHOT
- **Java版本**：JDK 8

---

## 二、项目结构

### 2.1 目录结构

```
attendance/
├── attendance-infrastructure/    # 基础设施层
│   └── src/main/resources/mapping/  # MyBatis Mapper XML
├── attendance-service/           # 业务服务层（核心）
│   ├── docs/                     # 技术文档
│   ├── scripts/                  # 脚本工具
│   └── src/main/java/com/glodon/glm/attendance/
│       ├── config/               # 配置类
│       ├── controller/           # REST API 控制层
│       ├── dao/                  # 数据访问层
│       ├── feign/                # Feign 客户端
│       ├── mq/                   # 消息队列监听
│       ├── service/              # 业务服务层
│       └── AttendanceComputeApplication.java  # 启动类
```

### 2.2 模块职责

| 模块 | 职责 | 关键文件 |
|------|------|----------|
| **config** | Spring 配置类 | RedisCacheConfig, MongoConfig, AsyncConfig |
| **controller** | REST API 入口 | AttendanceController, ClockRecordController |
| **dao** | 数据访问层 | ClockRecordDao, AttendanceResultDao |
| **feign** | 微服务远程调用 | WorkerServiceFeignClient, ProjectFeignClient |
| **mq** | 消息队列消费 | OrgMoveMessageListener, SlsdApprovalPostProcessConsumer |
| **service** | 核心业务逻辑 | AttendanceRuleService, ClockRecordService |

---

## 三、核心模块介绍

### 3.1 Controller 层

Controller 层按业务场景划分，包含多个子模块：

| 子模块 | 路径 | 说明 |
|--------|------|------|
| **app** | `controller/app/` | 移动端（小程序）API |
| **pc** | `controller/pc/` | PC端管理后台API |
| **open** | `controller/open/` | 开放平台API |
| **inner** | `controller/inner/` | 内部服务调用API |
| **restructure** | `controller/restructure/` | 重构后新API |
| **construction** | `controller/construction/` | 施工端API |

**主要控制器说明**：

| 控制器 | 说明 | 核心功能 |
|--------|------|----------|
| `AttendanceController` | 考勤主控制器 | 考勤查询、统计 |
| `ClockRecordController` | 打卡记录控制器 | 打卡、补卡、记录查询 |
| `AttendanceRuleController` | 考勤规则控制器 | 规则配置、管理 |
| `ApprovalController` | 审批控制器 | 审批流程处理 |
| `ReportController` | 报表控制器 | 报表生成、导出 |
| `LeaveController` | 请假控制器 | 请假申请、审批 |

### 3.2 Service 层

Service 层是业务逻辑的核心，主要包含以下模块：

#### 3.2.1 考勤规则服务

| 服务 | 说明 |
|------|------|
| `AttendanceRuleService` | 考勤规则管理（班次、时段、弹性时间） |
| `WorkDayRuleService` | 工作日规则（工作日/休息日配置） |
| `WorkHourRuleService` | 工时规则（正常工时、加班计算） |
| `ClockTimeWindowCheckService` | 打卡时间窗口校验（判断是否在允许时间段内打卡） |

#### 3.2.2 打卡记录服务

| 服务 | 说明 |
|------|------|
| `ClockRecordService` | 打卡记录管理（打卡、补卡、申诉） |
| `AttendanceSupplementService` | 补卡服务 |
| `AttendanceAppealService` | 考勤申诉服务 |

#### 3.2.3 审批服务

| 服务 | 说明 |
|------|------|
| `ApprovalService` | 审批流程管理 |
| `LeaveService` | 请假管理 |
| `SlsdApprovalService` | 劳务审批服务 |

#### 3.2.4 报表统计服务

| 服务 | 说明 |
|------|------|
| `AttendanceRateCalculateService` | 考勤率计算 |
| `AttendanceSummaryService` | 考勤汇总 |
| `AttendanceStreamQueryService` | 流式查询服务 |

### 3.3 Feign 客户端

Feign 客户端用于调用其他微服务：

| 客户端 | 说明 |
|--------|------|
| `WorkerServiceFeignClient` | 工人服务（获取工人信息） |
| `ProjectFeignClient` | 项目服务（获取项目信息） |
| `LocationServiceFeignClient` | 位置服务（定位校验） |
| `DeviceFeignClient` | 设备服务（设备信息） |
| `OrganizationServiceFeignClient` | 组织服务（组织架构） |
| `AttendanceCalculateServiceFeign` | 考勤计算服务 |

---

## 四、核心业务流程

### 4.1 打卡流程

```
工人打卡请求
↓
ClockRecordController
↓
ClockRecordService
↓
┌───────────────────────────────────────┐
│ 1. 定位校验（LocationCheatingService） │
│ 2. 时间窗口校验（ClockTimeWindowCheckService）│
│ 3. 规则匹配（AttendanceRuleService）   │
│ 4. 记录保存（ClockRecordDao）          │
│ 5. 消息通知（WebSocket/MQ）            │
└───────────────────────────────────────┘
↓
返回打卡结果
```

### 4.2 考勤计算流程

```
触发计算（定时任务/MQ消息）
↓
WorkOverDayRule / WorkOverHourRule
↓
┌───────────────────────────────────────┐
│ 1. 获取打卡记录                        │
│ 2. 匹配考勤规则                        │
│ 3. 计算工时（WorkHourCalculator）      │
│ 4. 判断异常（迟到/早退/旷工）            │
│ 5. 保存计算结果                        │
└───────────────────────────────────────┘
↓
推送结果（WebSocket/报表）
```

### 4.3 审批流程

```
提交申请（请假/补卡/申诉）
↓
ApprovalController
↓
ApprovalService
↓
┌───────────────────────────────────────┐
│ 1. 创建审批记录                        │
│ 2. 发送审批通知（MQ/短信）              │
│ 3. 审批人处理                          │
│ 4. 更新状态（ApprovalPassedListener）   │
│ 5. 执行后续逻辑（如补卡生效）            │
└───────────────────────────────────────┘
```

---

## 五、配置说明

### 5.1 数据库配置

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/glm_attendance?useUnicode=true&characterEncoding=utf-8
    username: root
    password: password
  data:
    mongodb:
      uri: mongodb://localhost:27017/glm_attendance
```

### 5.2 Redis 配置

```yaml
spring:
  redis:
    host: localhost
    port: 6379
    timeout: 3000ms
    password:
```

### 5.3 消息队列配置

```yaml
mq:
  topics:
    org-move: glm.org.move
    slsd-approval: glm.slsd.approval
```

### 5.4 微信小程序配置

```yaml
wx:
  miniapp:
    appid: your-app-id
    secret: your-app-secret
```

### 5.5 缓存配置

项目使用两级缓存：
- **Caffeine**：本地缓存，用于高频访问的数据（如考勤规则）
- **Redis**：分布式缓存，用于跨节点共享数据

---

## 六、使用指南

### 6.1 启动服务

**开发环境**：
```bash
cd attendance-service
mvn spring-boot:run
```

**生产环境**：
```bash
cd attendance-service
mvn clean package
java -jar target/glm-attendance-service-3.4.0-SNAPSHOT.jar
```

### 6.2 API 文档

启动服务后，访问 Swagger UI：
```
http://localhost:8080/swagger-ui.html
```

### 6.3 数据库初始化

使用项目自带的初始化脚本：
```bash
python scripts/init-attendance-service-db.py
```

### 6.4 测试数据

加载测试数据：
```bash
mysql -u root -p glm_attendance < scripts/attendance-service-test-seed.sql
```

---

## 七、开发规范

### 7.1 代码规范

1. **命名规范**：
    - 类名：驼峰式，首字母大写（如 `ClockRecordService`）
    - 方法名：驼峰式，首字母小写（如 `getClockRecordById`）
    - 变量名：驼峰式，首字母小写
    - 常量名：全大写，下划线分隔（如 `MAX_RETRY_COUNT`）

2. **包结构**：
    - `controller`：REST API 控制层
    - `service`：业务逻辑层（接口）
    - `service/impl`：业务逻辑层（实现）
    - `dao`：数据访问层
    - `feign`：远程调用客户端
    - `config`：配置类
    - `bean`：数据传输对象（DTO/VO/Entity）

3. **异常处理**：
    - 使用 `BusinessException` 处理业务异常
    - 在 `CommonExceptionHandler` 中统一处理异常返回

### 7.2 数据库规范

1. **表命名**：使用下划线分隔，小写（如 `attendance_rule_record`）
2. **字段命名**：使用下划线分隔，小写（如 `clock_time`）
3. **主键**：使用自增主键或 UUID
4. **时间字段**：使用 `datetime` 类型，存储 UTC 时间或东八区时间

### 7.3 API 规范

1. **路径命名**：使用 RESTful 风格，小写，下划线分隔
    - 列表查询：`GET /api/attendance/records`
    - 单个查询：`GET /api/attendance/records/{id}`
    - 创建：`POST /api/attendance/records`
    - 更新：`PUT /api/attendance/records/{id}`
    - 删除：`DELETE /api/attendance/records/{id}`

2. **响应格式**：
   ```json
   {
     "code": 0,
     "message": "success",
     "data": {...},
     "timestamp": 1620000000000
   }
   ```

3. **错误码**：
    - `0`：成功
    - `1001`：参数错误
    - `1002`：权限不足
    - `2001`：业务异常

### 7.4 缓存规范

1. **本地缓存（Caffeine）**：
    - 用于高频访问、变更不频繁的数据（如考勤规则）
    - 设置合理的过期时间

2. **分布式缓存（Redis）**：
    - 用于跨节点共享的数据
    - 注意缓存一致性，更新数据库后及时更新缓存

---

## 八、常见问题解答

### Q1：打卡提示"不在允许的时间范围内"怎么办？

**原因**：考勤规则配置了打卡时间窗口，当前时间不在允许范围内。

**解决**：
1. 检查考勤规则的时间段配置
2. 确认是否启用了时间窗口校验
3. 如果是紧急情况，可以通过补卡功能处理

### Q2：定位校验失败如何处理？

**原因**：工人打卡时的位置不在允许的地理围栏范围内。

**解决**：
1. 检查设备定位是否准确
2. 确认项目地理围栏配置
3. 通过 `LocationCheatingController` 处理异常情况

### Q3：考勤计算结果不准确？

**原因**：可能是规则配置错误或计算逻辑问题。

**解决**：
1. 检查考勤规则配置（工作日、工时、弹性时间等）
2. 查看日志分析计算过程
3. 使用测试脚本验证计算逻辑

### Q4：如何添加新的考勤规则类型？

**步骤**：
1. 在 `AttendanceRuleScopeTypeEnum` 中添加新类型
2. 在 `ScopeRuleService` 中实现规则匹配逻辑
3. 在 `AttendanceRuleController` 中添加相关 API
4. 更新前端配置页面

### Q5：服务启动失败，提示数据库连接异常？

**检查项**：
1. 数据库服务是否正常运行
2. 数据库连接配置是否正确
3. 网络是否可达
4. 数据库用户权限是否正确

---

## 九、附录

### 9.1 关键配置文件路径

| 文件 | 路径 | 说明 |
|------|------|------|
| 应用配置 | `src/main/resources/application.yml` | Spring Boot 配置 |
| MyBatis映射 | `src/main/resources/mapping/` | SQL映射文件 |
| Feign配置 | `config/FeignConfig.java` | Feign客户端配置 |
| Redis配置 | `config/RedisCacheConfig.java` | 缓存配置 |

### 9.2 定时任务

| 任务 | 说明 | 频率 |
|------|------|------|
| 考勤计算 | 计算每日考勤结果 | 每日凌晨 |
| 报表生成 | 生成统计报表 | 每日/每周/每月 |
| 缓存刷新 | 刷新规则缓存 | 定时 |

### 9.3 消息队列主题

| 主题 | 说明 | 消费端 |
|------|------|--------|
| `glm.org.move` | 组织架构变更 | `OrgMoveMessageListener` |
| `glm.slsd.approval` | 劳务审批完成 | `SlsdApprovalPostProcessConsumer` |

---

## 十、维护记录

| 版本 | 日期 | 变更内容 | 作者 |
|------|------|----------|------|
| 3.4.0 | 2026-07 | 新增打卡时间窗口校验功能 | system |
| 3.3.0 | 2026-06 | 重构考勤计算逻辑 | - |
| 3.2.0 | 2026-05 | 新增劳务审批流程 | - |
| 3.1.0 | 2026-04 | 优化定位校验逻辑 | - |
