# 乐工宝：非可打卡时间禁用打卡按钮

**日期**：2026-07-10  
**状态**：待实现  
**仓库**：`attendance-service` · `worker-app-aggregator` · `web-lgb-v3`

## 1. 目标

不在考勤计划可打卡时段内时：打卡按钮置灰 + 页面常驻提示。仅前端拦截，写接口不改。

| 做 | 不做 |
|----|------|
| 预检接口 + 前端置灰/文案 | toast、定时/回前台刷新 |
| 工人端 + 管理端打卡页（同一接口） | 代打他人、改 Redis 库、引 component-rule |
| L1：`projectId+date` 缓存当天规则 | 判灰度、服务端下发提示文案 |

## 2. 调用链与上下文

```text
web-lgb-v3（工人 mobileClockNew / 管理端 mobileClockManager）
  → GET /attendance/clock/timeWindow/check   # BFF：解析项目/工人后透传
  → attendance-service                        # query 收参 + 查工人属性 + 规则匹配 + L1
  ← Result<{ canClockNow, timeRestricted, allowedWindows, serverTime }>
```

### 2.1 前端如何带上项目 / 人员

两端**同一 BFF 接口**；靠 Header 区分项目上下文。工人身份始终是**登录人本人**（非代打）。

| | 工人端 `mobileClockNew` | 管理端打卡页 `mobileClockManager` |
|--|-------------------------|-----------------------------------|
| 项目/班组/租户来源 | `localStorage`：`clockProjectId` / `clockGroupId` / `clockTenantId` | 本页下拉选中项，经 `getManagerClockRequestConfig()` |
| 默认 Header | 拦截器：`x-project-id` / `x-group-id` / `x-tenant-id`，`x-current-role=WORKER` | 若走默认会落到门户 `lgb-pre-*`，**不能**直接用 |
| 本接口额外 Header | 无（默认工人上下文即可） | `x-lgb-request-project-id` / `x-lgb-request-group-id` + `x-lgb-request-role=WORKER` |
| 拦截器覆盖 | — | 将上述覆盖为最终 `x-project-id` / `x-group-id`，并设 `x-current-role=WORKER`（与首页 `lgb-pre-*` 独立，不写 `clockProjectId`） |
| 人员 | token → 服务端解析 `workerId`（登录人） | 同左 |

Token：`x-glm-access-token`（`lgb_login_info.accessToken`）。**不传** `projectWorkerId` query；由 BFF 解析。

### 2.2 BFF（worker-app-aggregator）如何取项目与人员

入口：`AttendanceController`（紧挨现有 `/clock/pre/check`），**无强制 query**。

```text
1. token → getLaborUser() → workerId（登录人）
2. Header x-project-id → getProjectId()
3. Header x-current-role（须为 WORKER；管理端打卡页已强制）
4. getProjectGroup()（BaseController，WORKER 分支）：
     registerServiceFeignClient.searchProjectWorker(projectId, workerId)
       → projectWorkerId、tenantId、companyId（有本地 pwId 缓存）
     groupId ← Header x-group-id（getCurrentGroupId）
5. Feign → attendance-service：
     GET .../timeWindow/check?projectId=&projectWorkerId=
```

要点：

- `projectId` / `projectWorkerId` **由 BFF 从上下文算出**，不信任前端随意传工人 id。
- `projectWorkerId` 为空 → `Result.fail`（无法解析项目工人）。
- BFF **不查**规则、**不查**工人 scope 明细、**不缓存**预检业务结果。

### 2.3 attendance-service 如何取项目与人员

| 信息 | 来源 |
|------|------|
| `projectId` | Feign/HTTP query（BFF 传入） |
| `projectWorkerId` | 同上 |
| `date` | 默认 `LocalDate.now(Asia/Shanghai)`（可不暴露 query） |
| `serverNow` | `LocalDateTime.now()` |
| 匹配用工人属性 | **本服务再查**：优先 `WorkerServiceRegionFeignClient.listProjectWorkerByCondition` → `ProjectWorkerVO`（含 `jobCode`）；或 `ProjectWorkerRemoteApi.getById`（若用后者，JOB 维度需确认 `workTypeCode` 与 scope 的 `jobCode` 是否一致，不一致则仍走 VO） |

组装 `ClockMatchWorker`（供 §4.2 scope）：

| 字段 | 用途 |
|------|------|
| `companyId` / `groupId` / `teamId` | COMPANY / GROUP / TEAM |
| `jobCode` | JOB（来自 `ProjectWorkerVO.jobCode`） |
| `postType` | PERSON_TYPE |

工人查不到 → `Result.fail`（与「查询异常 fail」一致；前端失败不误禁用）。

```text
BFF 已解析身份 ──query──► attendance-service
                              ├─ projectId → L1/SQL 当天规则
                              └─ projectWorkerId → Feign 工人属性 → pickRule
```

## 3. 接口

`GET /attendance/clock/timeWindow/check`  
→ `Result<ClockTimeWindowCheckVO>`（`com.glodon.glm.utils.Result`）

```json
{
  "success": true,
  "data": {
    "canClockNow": false,
    "timeRestricted": true,
    "allowedWindows": [
      { "start": "08:00", "end": "12:00" },
      { "start": "13:00", "end": "18:00" }
    ],
    "serverTime": "2026-07-10T14:40:00"
  }
}
```

| 字段 | 含义 |
|------|------|
| `canClockNow` | 当前能否手动打卡 |
| `timeRestricted` | 是否限制打卡时间（有可用时段配置） |
| `allowedWindows` | 今日有效窗（含浮动后）；**`timeRestricted=true` 时必须非空** |
| `serverTime` | 服务端时间（判定基准） |

| `timeRestricted` | `allowedWindows` | `canClockNow` |
|------------------|------------------|---------------|
| `false` | `[]` | 恒 `true` |
| `true` | ≥1 段 | now 是否落在任一段 |

不可打 = `success=true` + `canClockNow=false`（不是 `Result.fail`）。查询异常才 `fail`；前端失败时不误禁用。

## 4. attendance-service 逻辑

本服务自实现匹配（对齐落库 `NONE`），**不引 component-rule、不查灰度**。

### 4.1 编排

```text
取 projectId / date / 工人属性
  → L1 取当天规则列表（miss 则 SQL 回填）
  → 选规则 pickRule
  → 算今日有效窗 buildWindows
  → 无窗：timeRestricted=false, canClockNow=true, windows=[]
     有窗：timeRestricted=true, windows=非空, canClockNow=now∈任一窗
```

禁止：`timeRestricted=true` 且 `windows=[]`。不判休息（仍走现有 `todayRest`）。

### 4.2 选规则 `pickRule`

```text
非默认规则（effectDate↓、createTime↓）中，第一个 scope 命中工人的
  → 否则用默认规则
  → 再无则 null（上层按「无规则」不限制）
```

**scope 命中**（任一 scope 即可）：

| scopeType | 命中条件 |
|-----------|----------|
| （scopes 空） | 视为全员命中 |
| `scopeRef = ALL` | 命中 |
| COMPANY / GROUP / TEAM | 工人对应 id 字符串等于 `scopeRef` |
| JOB | `jobCode == scopeRef` |
| PERSON_TYPE | `postType` 值等于 `scopeRef` |

### 4.3 算有效窗 `buildWindows`

对选中规则的 `intervals`：

```text
1. dayIndex = (daysBetween(effectDate, date) % cycleDays) + 1
   └─ effectDate 空或 cycleDays≤0 → 不过滤
   └─ 按 dayIndex 过滤后为空 → 回退用全部 interval
2. 每段有效窗（闭区间）：
   start = preFloat ?? startTime
   end   = postFloat ?? endTime
   └─ preFloat>start 或 postFloat<end → 回退 [startTime, endTime]
3. 输出 HH:mm 列表；全空则上层按「无窗」不限制
```

### 4.4 当前能否打卡

`canClockNow = windows` 中存在 `[start, end]` 使 `start ≤ now.toLocalTime() ≤ end`。

## 5. 规则查询 SQL

新增 Mapper 方法（本库，不走 component-rule / Redis 8）：

```java
List<AttendanceRuleRecordVO> listEffectiveRulesByProjectAndDate(
    @Param("projectId") Long projectId,
    @Param("date") LocalDate date);
```

### 5.1 生效条件（对齐 component-rule `selectByProjectIdAndDate`）

```sql
arr.project_id = #{projectId}
AND arr.effect_date <= #{date}
AND (arr.lose_effect_date IS NULL OR arr.lose_effect_date >= #{date})
```

- **拉全量生效规则**（无 `LIMIT 1`）：同一项目同一天可有多条非默认 + 默认规则，工人匹配在内存完成。
- 排序：`ORDER BY arr.effect_date DESC, arr.create_time DESC`（与 component-rule 一致，供 `pickRule` 优先非默认）。

### 5.2 完整 SQL（建议稿）

**不要**复用现有 `AttendanceRuleRecordResultMap` 整图：其嵌套 `lateEarlyRule` / `workDayRule` / `workHourRule` 等会 N+1 打库，预检不需要。

新增瘦 resultMap（仅 record + scope + interval），复用已有 column/join 片段：

```xml
<resultMap id="ClockTimeWindowRuleResultMap"
           type="com.glodon.glm.attendance.bean.vo.customrule.AttendanceRuleRecordVO">
    <id column="id" property="id"/>
    <result column="attendance_rule_id" property="attendanceRuleId"/>
    <result column="project_id" property="projectId"/>
    <result column="effect_date" property="effectDate"/>
    <result column="lose_effect_date" property="loseEffectDate"/>
    <result column="default_rule" property="defaultRule"/>
    <result column="record_scope_type" property="scopeType"/>
    <result column="create_time" property="createTime"/>
    <collection property="attendanceRuleScope"
                ofType="com.glodon.glm.attendance.bean.vo.customrule.AttendanceRuleScope">
        <id property="id" column="attendance_rule_scope_id"/>
        <result property="ruleRecordId" column="ars_rule_record_id"/>
        <result property="scopeType" column="ars_scope_type"/>
        <result property="scopeRef" column="ars_scope_ref"/>
        <result property="scopeRefName" column="ars_scope_ref_name"/>
    </collection>
    <collection property="attendanceScheduleInterval"
                ofType="com.glodon.glm.attendance.bean.vo.customrule.AttendanceScheduleInterval">
        <id property="id" column="asi_id"/>
        <result property="attendanceRuleId" column="asi_attendance_rule_id"/>
        <result property="cycleDays" column="asi_cycle_days"/>
        <result property="dayIndex" column="asi_day_index"/>
        <result property="intervalIndex" column="asi_interval_index"/>
        <result property="intervalType" column="asi_interval_type"/>
        <result property="startTime" column="asi_start_time"/>
        <result property="endTime" column="asi_end_time"/>
        <result property="isCrossNight" column="asi_is_cross_night"/>
        <result property="preFloatStartTime" column="asi_pre_float_start_time"/>
        <result property="postFloatEndTime" column="asi_post_float_end_time"/>
    </collection>
</resultMap>

<select id="listEffectiveRulesByProjectAndDate"
        resultMap="ClockTimeWindowRuleResultMap">
    SELECT
        arr.id,
        arr.attendance_rule_id,
        arr.project_id,
        arr.effect_date,
        arr.lose_effect_date,
        arr.default_rule,
        arr.scope_type AS record_scope_type,
        arr.create_time
        <include refid="granularScopeIntervalColumns"/>
    FROM attendance_rule_record arr
    <include refid="granularScopeIntervalJoins"/>
    WHERE arr.project_id = #{projectId}
      AND arr.effect_date <![CDATA[ <= ]]> #{date}
      AND (arr.lose_effect_date IS NULL
           OR arr.lose_effect_date <![CDATA[ >= ]]> #{date})
    ORDER BY arr.effect_date DESC, arr.create_time DESC, arr.id ASC,
             asi.day_index ASC, asi.interval_index ASC
</select>
```

`granularScopeIntervalJoins`（现网已有）：

```sql
LEFT JOIN attendance_rule_scope ars ON ars.rule_record_id = arr.id
LEFT JOIN attendance_schedule_interval asi ON asi.attendance_rule_id = arr.attendance_rule_id
```

### 5.3 查询说明

| 项 | 说明 |
|----|------|
| 一对多折叠 | MyBatis `collection` 按 `arr.id` 折叠；同一规则多 scope / 多 interval 会多行，属预期 |
| 无 scope | `attendanceRuleScope` 为空 → 见 §4.2，视为全员命中 |
| 无 interval | `attendanceScheduleInterval` 为空 → 上层视为不限制（`timeRestricted=false`） |
| 不 join `attendance_rule` | 预检不需要 rule_name / 工日工时等；`attendance_rule_id` 仅用于挂 interval |
| 工人属性 | **不在本 SQL**；见 §2.3（`projectWorkerId` → Feign → `ClockMatchWorker`） |
| 空结果 | 返回 `[]`，仍可写入 L1，避免无规则项目反复打库 |

## 6. 缓存设计

仅 **attendance-service 进程内 L1**；不引入 component-rule、不读 Redis 8、BFF 不缓存。

### 6.1 缓存什么 / 不缓存什么

| 缓存 | 不缓存 |
|------|--------|
| 当天生效**全部规则列表**（瘦字段，见下） | `canClockNow`（随 now 变化） |
| 含作用范围 + intervals | 某工人匹配结果 / `allowedWindows` |
| 空列表 `[]`（负缓存） | 工人属性、灰度 |

```text
请求 → L1.get(projectId, date)
         ├─ hit  → 内存 pickRule(worker) → 算 windows / canClockNow
         └─ miss → SQL listEffectiveRules… → put L1 → 同上
```

### 6.2 Key / Value

| 项 | 约定 |
|----|------|
| Key | `"{projectId}:{yyyy-MM-dd}"`（`date` 为业务日，预检默认 today） |
| Value | `List<ClockTimeWindowCachedRule>`（不可变拷贝；勿直接缓存可变 MyBatis VO 若存在写风险） |
| 最大条目 | 建议 `maximumSize ≥ 5000`（按项目×日期；防异常日期扫库撑爆堆） |

**CachedRule 必要字段**

```text
id, attendanceRuleId, defaultRule, effectDate, loseEffectDate, createTime, scopeType
scopes[]:  scopeType, scopeRef
intervals[]: cycleDays, dayIndex, intervalIndex, intervalType,
             startTime, endTime, isCrossNight, preFloatStartTime, postFloatEndTime
```

### 6.3 TTL：精确到业务日次日 0 点（类库自动过期）

**已定**

| 项 | 约定 |
|----|------|
| 过期时刻 | `date.plusDays(1).atStartOfDay(Asia/Shanghai)`，即该业务日**次日 00:00:00** |
| 实现 | **Caffeine**（类库负责过期与淘汰）；**禁止**自写 `ConcurrentHashMap` + 手动判 `expireAt` |
| 写入时 TTL | `Duration.between(now, expireAt)`（越晚写入剩余越短） |
| 已过期 date | `Duration` ≤ 0 时**不 put**（或 put 且 TTL=0，等价不缓存） |
| 主动失效 / 定时扫 | **不做**；到点由 Caffeine 自动失效 |
| 进程重启 / 多实例 | 缓存清空或各 JVM 独立；可接受 |

```text
date=2026-07-10 → expireAt=2026-07-11 00:00:00+08
Caffeine 在 expireAt 后自动 miss，业务 get 无需再比 now 与 expireAt
```

**禁止**：固定 `expireAfterWrite(24h)`（会越过次日 0 点）。

#### 实现方式（Caffeine，二选一）

工程已有 `spring-boot-starter-cache`；落地时显式加 `com.github.ben-manes.caffeine:caffeine`（若 BOM 未带入）。

| # | 方式 | 要点 |
|---|------|------|
| **A（推荐）** | `expireAfter(Expiry)` | create 时按 key 内 `date` 算到次日 0 点的纳秒；**与 `cache.get(key, loader)` 配合**，写入即由库接管过期 |
| B | `expireVariably().put(..., duration)` | put 时传入 `Duration.between(now, 次日0点)`；同样自动过期，加载路径需自己 put |

**推荐 A 示意**

```java
ZoneId ZONE = ZoneId.of("Asia/Shanghai");

Cache<String, List<ClockTimeWindowCachedRule>> cache = Caffeine.newBuilder()
    .maximumSize(5000)
    .expireAfter(new Expiry<String, List<ClockTimeWindowCachedRule>>() {
        public long expireAfterCreate(String key, List<ClockTimeWindowCachedRule> value, long currentTime) {
            LocalDate date = LocalDate.parse(key.substring(key.indexOf(':') + 1));
            long nanos = Duration.between(Instant.now(),
                date.plusDays(1).atStartOfDay(ZONE).toInstant()).toNanos();
            return Math.max(0, nanos); // ≤0 等价不缓存
        }
        public long expireAfterUpdate(String key, List<ClockTimeWindowCachedRule> value,
                                      long currentTime, long currentDuration) {
            return currentDuration; // 不因 update 改变「次日 0 点」
        }
        public long expireAfterRead(String key, List<ClockTimeWindowCachedRule> value,
                                    long currentTime, long currentDuration) {
            return currentDuration; // 读不续期
        }
    })
    .build();

// 业务只调 get；到次日 0 点后类库自动 miss 并再走 loader
List<ClockTimeWindowCachedRule> rules = cache.get(key, k -> loadImmutableFromDb(projectId, date));
```

业务侧**不**自写 Map、**不**手写 `now < expireAt`、**不**定时扫 key。过期与淘汰交给 Caffeine。

**单测**：`Caffeine` + `FakeTicker` 拨到次日 0 点后同 key miss，mapper 再调一次。

### 6.4 并发与加载

- 同 key：Caffeine `get(key, loader)` 合并加载。
- put 前 VO → CachedRule 深拷贝。
- 单测：同 key 第二次 `verify(mapper, times(1))`；TTL 见 §6.3。

### 6.5 与编排的边界

| 步骤 | 是否走 L1 |
|------|-----------|
| 拉项目当天规则列表 | ✅（Caffeine，次日 0 点自动过期） |
| 查工人属性 | ❌ 每次 |
| pickRule / buildWindows / canClockNow | ❌ 纯内存算，不回写 L1 |

## 7. 前端

| 页面 | 要点 |
|------|------|
| `mobileClockNew.vue` | 工人端 |
| `mobileClockManager.vue` | 管理端；预检带 `getManagerClockRequestConfig()` |

**`canClockNow=false` 时**

- 进场/出场/外勤置灰；下方常驻：「当前为非打卡时间，无法打卡」（可选用 `allowedWindows` 拼时段）
- 点击不 toast、不进入打卡；并入 `computeClockAvailable()`（默认 true，防接口未回误伤）
- 仅 `timeRestricted=true` 时展示时间限制 UI

**自动打卡条**（仅工人端、`needAutoAttendance`）：`canClockNow=false` 时徽章改为「当前为非打卡时间」，保留下方自动打卡规则说明（与计划时段语义分离）。

**时机**：进页、切项目。失败不禁用。

Figma：node `6879:3036`（[设计稿](https://www.figma.com/design/UC9lcfiQLYH8eYcTvRBV8k/26%E5%B9%B4%E5%8A%B3%E5%8A%A1%E7%BB%84%E4%BB%B6%E7%A0%94%E5%8F%91%E8%BF%AD%E4%BB%A3%E6%96%87%E4%BB%B6?node-id=6879-3036)）

## 8. 实现切分（写计划用）

| # | 模块 | 工作项 |
|---|------|--------|
| 1 | `attendance-service` | 预检 API；本库查当天规则；范围匹配；区间匹配；L1 缓存；单测 |
| 2 | `worker-app-aggregator` | Controller + VO + Feign 透传 |
| 3 | `web-lgb-v3` | 工人/管理端：调预检、置灰、文案、自动条徽章 |

## 9. 验收要点

1. 无规则/无时段 → 不限制、无新提示  
2. 有时段窗内/窗外 → `canClockNow` 与 UI 一致，且 `allowedWindows.length≥1`  
3. 禁止 `timeRestricted=true && windows` 为空  
4. 浮动 / dayIndex 与落库 `NONE` 语义一致（单测）  
5. 管理端切换打卡页项目后预检跟选中项  
6. 预检失败不误禁用；置灰点击无 toast  
7. L1：同项目同日二次请求不重复打库，仅内存匹配工人  

## 10. 推迟

Redis 库统一、复用 component-rule、灰度判断、接口下发文案、服务端拒绝打卡。
