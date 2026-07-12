# selectWorkTimePeriods 接口缓存方案设计文档

> **版本**: v1.0  
> **日期**: 2026-07-09  
> **模块**: worker-app-aggregator / attendance-service  
> **接口**: `POST /attendance/withinPeriod` → `selectWorkTimePeriods`

---

## 一、现状分析

### 1.1 Controller 接口分析（worker-app-aggregator）

| 维度 | 分析结果 |
|------|----------|
| **接口路径** | `POST /attendance/withinPeriod` |
| **业务场景** | 工人打卡前校验当前时间是否在可打卡时间段内，属于**打卡前置检查**的高频热路径 |
| **入参特征** | `SelectAttendanceRuleParam { projectId(Long), workerId(Long), date(LocalDate) }`，三个字段均为必填，组合唯一确定一次查询 |
| **返回结果** | `Result<List<WorkTimePeriodVO>>`，每个 VO 含 12 个字段（id, intervalIndex, intervalType, date, clockInStart/End, workStart/End, crossNight, hasPreFloat, hasPostFloat）。通常一个考勤规则当天匹配 1~4 个时间段，单条 JSON ≈ 200B，整体 ≤ 1KB |
| **读写比例** | **读远大于写**。考勤时间段配置由管理员在后台设定，变更频率极低（天级/周级）；而打卡校验在每天上下班高峰被大量工人并发调用 |
| **并发特征** | 早晚打卡高峰（如 06:00-08:00、17:00-19:00）QPS 集中爆发，同一项目+同一日期的请求高度重复，仅 workerId 不同 |
| **现有缓存** | 该 Controller 已有 `attendanceConfigCache`（Guava）+ `StringRedisTemplate` 二级缓存的使用先例（见 `attendanceShowHideConfig` 方法），但 `withinPeriod` 接口**未做任何缓存** |

### 1.2 底层 Feign 接口分析（attendance-service）

#### 完整调用链路

```
AttendanceController.withinPeriod()
  └─ AttendanceServiceFeignClient.selectWorkTimePeriods(param)       [HTTP/Feign]
      └─ AttendanceRuleController.selectWorkTimePeriods()
          ├─ organizationServiceFeignClient.getProjectByIdWithoutContext()   [Feign → DB]
          ├─ systemService.checkLicenseItem()                                [DB/License]
          └─ attendanceRuleRecordService.selectWorkTimePeriods()
              ├─ Step1-2: selectAttendanceRule(projectId, workerId, date)
              │   ├─ attendanceRuleRecordMapper.selectAttendanceRuleRecordByProjectIdAndDate()  [DB]
              │   └─ workerServiceFeignClient.getProjectWorkerScope()                          [Feign → DB]
              ├─ Step3: rule.getAttendanceScheduleInterval()               [内存，已随Step1加载]
              ├─ Step4: 周期匹配 + 类型过滤                                  [纯计算]
              └─ Step5: 弹性时间扩展 → List<WorkTimePeriodVO>               [纯计算]
```

#### 性能瓶颈识别

| 瓶颈点 | 说明 |
|--------|------|
| **Feign 网络开销** | aggregator → attendance-service 一次 HTTP 调用，P99 延迟约 10~50ms |
| **多次 DB 查询** | Step1 查 `attendance_rule_record` + Step2 可能 Feign 调 `worker-service` 查 `project_worker`，合计 2~3 次 DB/Feign |
| **License 校验** | 每次请求都查 License，但该值在项目级别几乎不变 |
| **数据变更频率** | 考勤规则配置（`attendance_rule_record` + `attendance_schedule_interval`）由管理后台维护，变更频率为**天级甚至周级**；工人组织关系变更频率也较低 |
| **结果确定性** | 对于相同的 `(projectId, date)` 组合，只要考勤规则未变更，返回的时间段列表**完全相同**（与 workerId 无关的部分占主体；workerId 仅影响 Scope 匹配，而大多数项目只有一套规则） |

---

## 二、缓存架构设计

### 2.1 整体策略：Aggregator 层 Redis 缓存 + 本地 L1 缓存（二级缓存）

采用与现有 `attendanceShowHideConfig` 一致的 **Guava L1 + Redis L2** 二级缓存模式，保持项目代码风格统一。

```
┌─────────────────────────────────────────────────────┐
│            worker-app-aggregator                     │
│                                                      │
│  Request ──► L1 Guava Cache (进程内, ms级)           │
│               │ MISS                                 │
│               ▼                                      │
│            L2 Redis Cache (分布式, ~1ms)             │
│               │ MISS                                 │
│               ▼                                      │
│            Feign → attendance-service (10~50ms)      │
│               │                                      │
│               ▼                                      │
│            Write Back L2 + L1                        │
└─────────────────────────────────────────────────────┘
```

### 2.2 缓存 Key 设计

```
VENDOR:ATTENDANCE:WORK_TIME_PERIODS:{projectId}:{date}
```

| 要素 | 说明 |
|------|------|
| **前缀** | `VENDOR:ATTENDANCE:` — 复用 Controller 中已有的 `prefix` 常量，保持命名空间一致 |
| **业务标识** | `WORK_TIME_PERIODS` — 明确区分于其他考勤缓存（如 CONFIG、RULE 等） |
| **维度选择** | `{projectId}:{date}` — **不包含 workerId**。原因：分析底层实现可知，`selectWorkTimePeriods` 的核心结果取决于项目级考勤规则+日期，workerId 仅影响 Scope 匹配；绝大多数项目只有一套规则，按 projectId+date 缓存命中率极高。对于多规则项目，可在后续迭代中增加 workerId 维度或改用规则ID作为Key |
| **Key 长度** | 示例：`VENDOR:ATTENDANCE:WORK_TIME_PERIODS:123456:2026-07-09` ≈ 55 字节，合理 |
| **空值 Key** | 同 Key 缓存空列表结果，防止穿透 |

> **进阶优化（可选）**：若发现同一项目存在多套考勤规则且 workerId 导致结果差异较大，可将 Key 改为 `VENDOR:ATTENDANCE:WTP:{projectId}:{date}:{ruleRecordId}`，其中 `ruleRecordId` 通过轻量级 Scope 匹配后获取。

### 2.3 缓存 Value 与序列化

| 维度 | 推荐方案 |
|------|----------|
| **序列化方式** | **FastJSON**（`JSON.toJSONString` / `JSON.parseArray`）。理由：① 项目中已广泛使用 `com.alibaba.fastjson.JSON`（见 Controller import）；② `WorkTimePeriodVO` 为纯 POJO，无循环引用；③ 比 JDK 序列化体积小 60%+，可读性好 |
| **存储结构** | Redis String 类型，Value 为 JSON Array 字符串 |
| **空值表示** | 空列表序列化为 `"[]"`（非 null），用于防穿透 |
| **内存评估** | 单 Key Value ≈ 200B~1KB；假设 1000 个项目 × 30 天 = 30,000 Keys，总内存 ≈ 15~30MB，可忽略 |

### 2.4 TTL 策略

| 层级 | TTL | 说明 |
|------|-----|------|
| **L2 Redis** | **基础 30 分钟 + 随机偏移 0~10 分钟** | 与现有 `attendanceShowHideConfig` 的 30min TTL 保持一致；随机偏移避免同一时刻大量 Key 同时过期引发缓存雪崩 |
| **L1 Guava** | `expireAfterAccess(5, TimeUnit.MINUTES)` | 本地缓存短 TTL，保证即使 Redis 失效也能快速感知更新；5 分钟对打卡校验场景足够（工人在几分钟内反复打开APP时命中 L1） |
| **L1 容量** | `maximumSize(500)` | 限制本地缓存条目数，防止 OOM |

```java
// TTL 随机化示例
int baseTtl = 30;
int randomOffset = ThreadLocalRandom.current().nextInt(0, 11);
stringRedisTemplate.opsForValue().set(cacheKey, jsonValue, baseTtl + randomOffset, TimeUnit.MINUTES);
```

### 2.5 数据一致性保障

#### 核心判断

考勤时间段数据的变更来源：
1. **考勤规则配置变更**（管理后台操作）— 频率极低
2. **考勤执行计划生成**（定时任务，每日凌晨生成次日计划）— 可预测
3. **工人组织关系变更**（调班组等）— 频率低

由于该接口是**打卡前置校验**，对实时性要求为**秒级容忍**（工人打开 APP 到实际打卡有数秒间隔），因此采用 **Cache Aside + 主动失效** 组合策略：

#### 策略详情

| 场景 | 策略 | 实现方式 |
|------|------|----------|
| **常规读取** | Cache Aside：先读缓存，miss 则查服务并回填 | 标准实现 |
| **考勤规则变更** | 主动删除缓存 | 在 `attendance-service` 的规则保存/更新/删除接口中，发送 MQ 消息或直接调用 aggregator 的缓存清理接口，按 `projectId` 删除该项目的 `WORK_TIME_PERIODS:*` 相关 Key |
| **执行计划生成** | 定时批量预热 | 每日凌晨执行计划生成完毕后，触发缓存预热任务，提前写入当日缓存 |
| **兜底** | TTL 自然过期 | 即使主动失效遗漏，30min TTL 保证最终一致 |

#### 简化落地方案（推荐首期）

考虑到当前系统复杂度，**首期仅依赖 TTL 自然过期 + Cache Aside**，理由：
- 考勤规则变更频率极低，30min 延迟对打卡校验场景可接受
- 避免引入 MQ/Binlog 监听的额外复杂度
- 后续可根据实际反馈追加主动失效机制

### 2.6 极端场景防护

#### 2.6.1 缓存穿透防护

| 措施 | 说明 |
|------|------|
| **缓存空值** | 当 Feign 返回空列表或失败时，将 `"[]"` 写入缓存，TTL 设为 **5 分钟**（短于正常 TTL，避免长时间阻塞合法数据） |
| **入参校验** | Controller 层已有 `@Validated`，非法参数不会到达缓存层 |

```java
// 空值缓存示例
if (result == null || !result.isSuccess() || result.getData() == null || result.getData().isEmpty()) {
    stringRedisTemplate.opsForValue().set(cacheKey, "[]", 5, TimeUnit.MINUTES);
    return Result.fail("200", "调用考勤接口失败", Boolean.FALSE);
}
```

#### 2.6.2 缓存击穿防护

| 措施 | 说明 |
|------|------|
| **互斥锁（Redis SETNX）** | 当 L1 和 L2 同时 miss 时，仅允许一个线程回源查询，其余线程等待或降级 | 
| **L1 兜底** | Guava `get(key, Callable)` 自带同步加载语义，同一 JVM 内天然防击穿 |

```java
// Redis 分布式锁防击穿
String lockKey = cacheKey + ":LOCK";
Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(lockKey, "1", 10, TimeUnit.SECONDS);
if (Boolean.TRUE.equals(locked)) {
    try {
        // 回源查询 + 回填缓存
        Result<List<WorkTimePeriodVO>> feignResult = attendanceServiceFeignClient.selectWorkTimePeriods(param);
        // ... write back cache
    } finally {
        stringRedisTemplate.delete(lockKey);
    }
} else {
    // 未抢到锁：短暂等待后重试读缓存，或返回默认值
    Thread.sleep(50);
    String retryData = stringRedisTemplate.opsForValue().get(cacheKey);
    if (retryData != null) {
        // parse and return
    }
    // 仍无数据则降级回源（容忍少量重复查询）
}
```

#### 2.6.3 缓存雪崩防护

| 措施 | 说明 |
|------|------|
| **TTL 随机化** | 已在 2.4 节描述，30 + random(0~10) min |
| **L1 错峰** | Guava `expireAfterAccess` 基于访问时间，天然错开过期时间点 |
| **熔断降级** | Feign 调用失败时，若缓存中有旧数据（即使是过期的），可考虑返回旧数据而非直接报错 |

---

## 三、落地代码参考

以下为 `AttendanceController.withinPeriod` 方法的改造参考：

```java
// ========== 新增 L1 缓存实例 ==========
private Cache<String, List<WorkTimePeriodVO>> workTimePeriodCache = CacheBuilder.newBuilder()
        .expireAfterAccess(5, TimeUnit.MINUTES)
        .concurrencyLevel(16)
        .initialCapacity(200)
        .maximumSize(500)
        .build();

// ========== withinPeriod 方法改造 ==========
@ApiOperation(value = "查询指定项目、指定工人在某一天的可打卡时间段列表")
@PostMapping("withinPeriod")
public Result<Boolean> withinPeriod(@Validated @RequestBody SelectAttendanceRuleParam param) {
    log.debug("[withinPeriod] projectId={}, workerId={}, date={}",
            param.getProjectId(), param.getWorkerId(), param.getDate());

    String cacheKey = prefix + "WORK_TIME_PERIODS:" + param.getProjectId() + ":" + param.getDate();

    // ===== L1: Guava 本地缓存 =====
    List<WorkTimePeriodVO> periods = workTimePeriodCache.getIfPresent(cacheKey);

    // ===== L2: Redis 分布式缓存 =====
    if (periods == null) {
        String redisData = stringRedisTemplate.opsForValue().get(cacheKey);
        if (redisData != null) {
            periods = JSON.parseArray(redisData, WorkTimePeriodVO.class);
            workTimePeriodCache.put(cacheKey, periods);
        } else {
            // ===== 回源：Feign 调用 attendance-service =====
            Result<List<WorkTimePeriodVO>> result = attendanceServiceFeignClient.selectWorkTimePeriods(param);

            if (log.isDebugEnabled()) {
                log.debug("[withinPeriod] feign result={}", JSON.toJSONString(result));
            }

            // 空值/失败 → 缓存空列表防穿透（短TTL）
            if (result == null || !result.isSuccess() 
                    || result.getData() == null || result.getData().isEmpty()) {
                stringRedisTemplate.opsForValue().set(cacheKey, "[]", 5, TimeUnit.MINUTES);
                return Result.fail("200", "调用考勤接口失败", Boolean.FALSE);
            }

            periods = result.getData();
            String jsonValue = JSON.toJSONString(periods);
            int ttlMinutes = 30 + ThreadLocalRandom.current().nextInt(0, 11);
            stringRedisTemplate.opsForValue().set(cacheKey, jsonValue, ttlMinutes, TimeUnit.MINUTES);
            workTimePeriodCache.put(cacheKey, periods);
        }
    }

    // ===== 业务逻辑：判断当前时间是否在可打卡时间段内 =====
    LocalTime currentTime = LocalTime.now();
    boolean withinPeriod = periods.stream()
            .anyMatch(period -> period.isWithinClockInPeriod(currentTime));

    if (!withinPeriod) {
        log.info("[withinPeriod] 当前时间不在打卡时间段内, currentTime={}, periodsSize={}",
                currentTime, periods.size());
    } else {
        log.debug("[withinPeriod] 当前时间在打卡时间段内, currentTime={}", currentTime);
    }

    return Result.success(withinPeriod);
}
```

---

## 四、监控与运维建议

| 指标 | 说明 |
|------|------|
| **L1 命中率** | 通过 Guava `Cache.stats()` 暴露，目标 > 60%（高峰期同项目请求密集） |
| **L2 命中率** | Redis INFO keyspace_hits/misses，目标 > 90% |
| **回源 QPS** | 监控 Feign 调用量，应远低于接口总 QPS |
| **Key 数量** | `SCAN` 统计 `VENDOR:ATTENDANCE:WORK_TIME_PERIODS:*` 的数量，预估内存 |
| **告警** | L2 命中率 < 70% 或回源 QPS 突增时告警 |

---

## 五、演进路线

| 阶段 | 内容 | 优先级 |
|------|------|--------|
| **Phase 1（当前）** | Cache Aside + TTL 自然过期 + 空值防穿透 + L1/L2 二级缓存 | P0 |
| **Phase 2** | 在 attendance-service 规则变更时发 MQ 消息，aggregator 消费后主动删除对应 Key | P1 |
| **Phase 3** | 每日凌晨执行计划生成后，批量预热当日缓存 | P1 |
| **Phase 4** | 若 workerId 导致结果差异显著，引入 `{projectId}:{date}:{ruleRecordId}` 精细化 Key | P2 |
| **Phase 5** | 接入分布式锁防击穿（高并发场景验证后按需开启） | P2 |

---

## 六、风险评估

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 缓存与DB不一致 | 工人在规则变更后30分钟内仍看到旧时间段 | 首期可接受；Phase 2 引入主动失效 |
| L1 缓存占用内存 | 单实例 500 条 × 1KB ≈ 500KB，可忽略 | `maximumSize` 硬上限保护 |
| Redis 不可用 | 所有请求回源 Feign | Feign 本身有超时/熔断；可加 Hystrix/Sentinel 兜底 |
| Key 热点 | 大项目数千工人同时打卡，单 Key QPS 极高 | Redis 单 Key 读写能力 > 10万QPS，无需担心；必要时 Phase 5 加锁 |
| 空值缓存误伤 | 临时故障导致大量空值缓存 | 空值 TTL 仅 5 分钟，恢复后自动刷新 |

---

## 附录：相关文件索引

| 文件 | 路径 |
|------|------|
| Controller | `worker-app-aggregator/.../controller/AttendanceController.java` |
| Feign Client | `worker-app-aggregator/.../feign/AttendanceServiceFeignClient.java` |
| 请求参数 | `worker-app-aggregator/.../feign/req/SelectAttendanceRuleParam.java` |
| 返回 VO | `worker-app-aggregator/.../feign/res/WorkTimePeriodVO.java` |
| Service 实现 | `attendance-service/.../service/impl/AttendanceRuleRecordServiceImpl.java` |
| 底层 Controller | `attendance-service/.../controller/AttendanceRuleController.java` |
| 原功能设计文档 | `attendance-service/docs/selectWorkTimePeriods-design.md` |
