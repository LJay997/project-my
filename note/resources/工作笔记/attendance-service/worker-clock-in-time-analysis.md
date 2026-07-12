# 工人可打卡时间段 — 完整数据链路技术分析

---

## 一、相关表梳理

### 1.1 核心表

#### `attendance_rule`（考勤规则主表）

> 定义一条考勤规则的元信息，是整个考勤体系的顶层实体。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint(20) PK | 主键 |
| `tenant_id` | bigint(20) | 租户 ID |
| `org_id` | bigint(20) | 创建该规则的组织节点 ID |
| `rule_name` | varchar(128) | 规则名称，如"项目部白班" |
| `rule_label` | varchar(100) | 规则标签（PROJECT 等） |
| `enable_over_time` | smallint(2) | 是否开启加班规则（0=否，1=是） |
| `enable_late_early` | tinyint(4) | 是否开启迟到早退管理 |
| `deduct_float_time` | tinyint(4) | 全局开关：是否扣除弹性时段（1=扣除，0=不扣除） |

#### `attendance_rule_record`（考勤规则引用/执行计划表）

> 将一条 `attendance_rule` 绑定到某个项目/组织，并设定生效时间段。**同一项目在同一日期可能有多条记录**（对应不同适用范围）。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint(20) PK | 主键 |
| `attendance_rule_id` | bigint(20) | **关联 `attendance_rule.id`** |
| `tenant_id` | bigint(20) | 租户 ID |
| `org_id` | bigint(20) | 组织 ID |
| `project_id` | bigint(20) | **项目 ID**（核心查询条件） |
| `company_id` | bigint(20) | 参建单位 ID（旧规则使用） |
| `team_id` | bigint(20) | 队伍 ID（旧规则使用） |
| `group_id` | bigint(20) | 班组 ID（旧规则使用） |
| `effect_date` | date | **生效日期**（核心查询条件） |
| `lose_effect_date` | date | **失效日期**（NULL 表示永久有效） |
| `scope_type` | varchar(32) | 适用范围维度（旧字段，新逻辑使用 `attendance_rule_scope` 表） |
| `default_rule` | tinyint(1) | **是否默认计划**（0=否，1=是） |
| `reference_super` | tinyint(4) | 是否引用上级规则 |

#### `attendance_rule_scope`（考勤规则维度关联表）

> 定义一条 `attendance_rule_record` 适用于哪些维度（班组/队伍/单位/工种/人员类型）。**一对多关系**：一条 rule_record 可关联多条 scope 记录。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint(20) PK | 主键 |
| `rule_record_id` | bigint(20) | **关联 `attendance_rule_record.id`** |
| `scope_type` | varchar(32) | **适用范围维度**：`COMPANY`/`GROUP`/`TEAM`/`JOB`/`PERSON_TYPE` |
| `scope_ref` | varchar(32) | **关联值**：单位ID/班组ID/队伍ID/工种code/人员类型ID。**特殊值 `-1` 表示"全部"** |
| `scope_ref_name` | varchar(128) | 冗余名称 |

**`scope_type` 枚举说明：**

| scope_type | scope_ref 含义 | 示例 |
|------------|---------------|------|
| `COMPANY` | 参建单位 ID | `"100"` 表示单位 100 |
| `GROUP` | 班组 ID | `"50"` 表示班组 50 |
| `TEAM` | 队伍 ID | `"80"` 表示队伍 80 |
| `JOB` | 工种 code | `"7"` 表示工种 7 |
| `PERSON_TYPE` | 人员类型 | `"0"` = 工人，`"1"` = 管理 |
| *任意* | `"-1"` | **通配符，表示该维度下全部适用** |

#### `attendance_schedule_interval`（考勤区间表）★ 核心

> 定义一条考勤规则在周期内每天的具体时间段。**这是可打卡时间段的唯一数据来源。**

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint(20) PK | 主键 |
| `attendance_rule_id` | bigint(20) | **关联 `attendance_rule.id`**（注意：不是关联 rule_record） |
| `cycle_days` | int(11) | **周期总天数**（如 2 = "做二休一"中的 2 天周期） |
| `day_index` | int(11) | **周期第几天**（1-based，如 cycle_days=2 时取值 1 或 2） |
| `interval_index` | int(11) | **当天第几个区间**（排序用，如上午=1，下午=2） |
| `interval_type` | varchar(16) | **区间类型**：`WORK`（上班）/ `REST`（休息）/ `OVERTIME`（加班） |
| `interval_name` | varchar(64) | 区间名称（如"上午班"） |
| `start_time` | time | **标准开始时间**（如 08:00） |
| `end_time` | time | **标准结束时间**（如 12:00） |
| `is_cross_night` | tinyint(4) | **是否跨夜**（0=否，1=是；跨夜时 end_time 实际落在次日） |
| `pre_float_start_time` | time | **前弹性起点**（NULL=无前弹性，须 < start_time） |
| `post_float_end_time` | time | **后弹性终点**（NULL=无后弹性，须 > end_time） |
| `deduct_float_time` | tinyint(4) | 该区间工时是否扣除弹性时段（1=扣除，0=不扣除） |

### 1.2 辅助规则表

#### `work_hour_rule`（工时规则表）

| 字段 | 说明 |
|------|------|
| `attendance_rule_id` | 关联 `attendance_rule.id` |
| `rule_type` | 工时算法类型 |
| `cross_night_calc_type` | **跨夜结算类型**：0=不结算，1=结算至前一天，2=结算至后一天，3=按零点分别结算 |

#### `work_day_rule`（工日规则表）

| 字段 | 说明 |
|------|------|
| `attendance_rule_id` | 关联 `attendance_rule.id` |
| `rule_type` | 工日算法类型 |

#### `attendance_late_early_rule`（迟到早退规则表）

| 字段 | 说明 |
|------|------|
| `attendance_rule_id` | 关联 `attendance_rule.id`（UNIQUE KEY，一对一） |
| `late_threshold_minutes` | 迟到阈值（分钟）：首次打卡 >= start_time + 阈值 → 迟到 |
| `early_threshold_minutes` | 早退阈值（分钟）：末次打卡 <= end_time - 阈值 → 早退 |

#### `attendance_rule_role`（考勤规则权限表）

| 字段 | 说明 |
|------|------|
| `org_id` | 组织 ID |
| `child_setting` | 是否允许下级自定义考勤 |
| `rule_type` | 1=考勤规则，2=考勤率规则 |
| `project_rule_scope` | 项目适用范围：`ORGANIZATION`/`WORK_TYPE`/`PERSON_TYPE` |

### 1.3 请假/休息相关表

#### `leave_record`（请假记录表）

> 记录工人的请假申请，与审批流程关联。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint(20) PK | 主键 |
| `tenant_id` | bigint(20) | 租户 ID |
| `project_id` | bigint(20) | 项目 ID |
| `project_worker_id` | bigint(20) | **项目工人 ID** |
| `reason` | varchar(100) | 请假原因 |
| `state` | tinyint(4) | **审批状态**：`-1`=已撤销，`0`=待审核，`10`=驳回，`20`=通过 |
| `approval_record_id` | bigint(20) | 关联 `approval_record.id` |

#### `leave_date`（请假日期明细表）

> 一次请假可跨多天，每天一条明细，记录当天请假的具体时段。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint(20) PK | 主键 |
| `tenant_id` | bigint(20) | 租户 ID |
| `project_id` | bigint(20) | 项目 ID |
| `leave_record_id` | bigint(20) | **关联 `leave_record.id`** |
| `project_worker_id` | bigint(20) | 项目工人 ID |
| `date` | date | **请假日期** |
| `start_time` | time | **请假开始时间**（如 `08:00`） |
| `end_time` | time | **请假截止时间**（如 `12:00`） |

#### `rest_record`（休息记录表）

> 记录工人的休息申请，休息通常为全天。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint(20) PK | 主键 |
| `tenant_id` | bigint(20) | 租户 ID |
| `project_id` | bigint(20) | 项目 ID |
| `project_worker_id` | bigint(20) | **项目工人 ID** |
| `reason` | varchar(100) | 休息原因 |
| `remark` | varchar(200) | 备注 |
| `approval_record_id` | bigint(20) | 关联 `approval_record.id` |
| `state` | tinyint(4) | **审批状态**：`-1`=已撤销，`0`=待审核，`10`=驳回，`20`=通过 |

#### `rest_date`（休息日期表）

> 休息通常是全天的，`rest_date` 仅记录日期，无时段字段。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint(20) PK | 主键 |
| `tenant_id` | bigint(20) | 租户 ID |
| `project_id` | bigint(20) | 项目 ID |
| `rest_record_id` | bigint(20) | **关联 `rest_record.id`** |
| `project_worker_id` | bigint(20) | 项目工人 ID |
| `date` | date | **休息日期** |

#### `revoke_rest_record`（撤销休息记录表）

> 撤销某天已审批通过的休息，恢复为正常工作日。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint(20) PK | 主键 |
| `tenant_id` | bigint(20) | 租户 ID |
| `project_id` | bigint(20) | 项目 ID |
| `rest_date_id` | bigint(20) | **关联 `rest_date.id`**（被撤销的休息日） |
| `project_worker_id` | bigint(20) | 项目工人 ID |
| `date` | date | **撤销休息日期** |
| `deleted` | smallint(6) | 删除状态：`0`=未删除，`1`=已删除 |

#### `terminate_leave_record`（销假记录表）

> 工人提前结束请假时提交的销假申请。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint(20) PK | 主键 |
| `tenant_id` | bigint(20) | 租户 ID |
| `project_id` | bigint(20) | 项目 ID |
| `project_worker_id` | bigint(20) | 项目工人 ID |
| `approval_record_id` | bigint(20) | 关联 `approval_record.id` |
| `reason` | varchar(100) | 销假原因 |

#### `terminate_leave_date`（销假日期明细表）

> 记录销假的具体日期和时段，通过 `source_leave_date_id` 关联到原请假日期。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint(20) PK | 主键 |
| `tenant_id` | bigint(20) | 租户 ID |
| `project_id` | bigint(20) | 项目 ID |
| `terminate_leave_record_id` | bigint(20) | 关联 `terminate_leave_record.id` |
| `project_worker_id` | bigint(20) | 项目工人 ID |
| `date` | date | 销假日期 |
| `start_time` | time | 销假开始时间 |
| `end_time` | time | 销假结束时间 |
| `source_leave_record_id` | bigint(20) | 原请假记录 ID |
| `source_leave_date_id` | bigint(20) | **原请假日期 ID** |

#### `approval_record`（审批记录表）

> 请假/补刷卡/续假/销假的审批流记录。

| 字段 | 类型 | 说明 |
|------|------|------|
| `id` | bigint(20) PK | 主键 |
| `project_id` | bigint(20) | 项目 ID |
| `applicant_id` | bigint(20) | 申请人 ID |
| `content_type` | tinyint(4) | 审批内容类型：`1`=请假，`2`=补刷卡，`3`=续假，`4`=销假，`5`=休息 |
| `content_id` | bigint(20) | 审批内容 ID（`content_type=1` 时为请假记录 ID） |
| `state` | tinyint(4) | **审批状态**：`-1`=已撤销，`0`=待审核，`10`=驳回，`20`=通过 |

> 这些表影响工人"当天是否需要打卡"，以及 Step 6 中可打卡时间段的扣减逻辑。

---

## 二、表间关联关系

### 2.1 关联关系图

```
                    ┌─────────────────────┐
                    │  attendance_rule     │
                    │  (考勤规则主表)       │
                    │  PK: id              │
                    └────────┬────────────┘
                             │ 1:N (同一规则可被多个项目引用)
                             ▼
              ┌──────────────────────────────┐
              │  attendance_rule_record       │
              │  (规则引用/执行计划表)          │
              │  FK: attendance_rule_id → rule.id │
              │  核心查询条件: project_id + date   │
              └───────┬──────────┬────────────┘
                      │          │
            1:N       │          │  1:N (通过 attendance_rule_id)
                      ▼          ▼
        ┌──────────────────┐  ┌──────────────────────────────┐
        │ attendance_rule  │  │ attendance_schedule_interval  │
        │ _scope           │  │ (考勤区间表) ★                  │
        │ (适用范围维度表)   │  │ FK: attendance_rule_id         │
        │ FK: rule_record  │  │ 核心数据: start_time, end_time │
        │     _id          │  │ pre_float, post_float          │
        └──────────────────┘  └──────────────────────────────┘
                      │
                      │  1:1 (辅助规则，均通过 attendance_rule_id 关联)
                      ▼
        ┌──────────────────────────────────────────────────┐
        │ work_hour_rule │ work_day_rule │ late_early_rule │
        │ (工时算法)     │ (工日算法)    │ (迟到早退)       │
        └──────────────────────────────────────────────────┘
```

### 2.2 关联关系汇总

| 关系 | 关联方式 | 说明 |
|------|----------|------|
| `attendance_rule` → `attendance_rule_record` | `rule.id = record.attendance_rule_id` | 一条规则可被多个项目引用 |
| `attendance_rule_record` → `attendance_rule_scope` | `record.id = scope.rule_record_id` | 一条执行计划可配多个适用维度 |
| `attendance_rule` → `attendance_schedule_interval` | `rule.id = interval.attendance_rule_id` | 一条规则包含多个考勤区间 |
| `attendance_rule` → `work_hour_rule` | `rule.id = whr.attendance_rule_id` | 一对一 |
| `attendance_rule` → `work_day_rule` | `rule.id = wdr.attendance_rule_id` | 一对一 |
| `attendance_rule` → `attendance_late_early_rule` | `rule.id = ler.attendance_rule_id` | 一对一（UNIQUE KEY） |

> **关键发现**：`attendance_schedule_interval` 关联的是 `attendance_rule_id`（规则级别），而非 `rule_record_id`（执行计划级别）。这意味着同一个规则下的所有执行计划共享同一套考勤区间。区间通过 `cycle_days` + `day_index` 区分不同天。

---

## 三、可打卡时间计算逻辑

### 3.1 完整数据链路（6 步）

```
输入: projectId + workerInfo(groupId, teamId, companyId, workTypeId) + date
                         │
    ──── Step 1 ─────────▼────────
    │ 查找当天生效的执行计划        │
    │ attendance_rule_record       │
    │ WHERE project_id = ?         │
    │   AND effect_date <= date    │
    │   AND (lose_effect_date      │
    │        IS NULL OR >= date)   │
    ────────────────────────────────
                         │ 返回 N 条 rule_record
    ──── Step 2 ─────────▼────────
    │ 多维度匹配（scope 过滤）      │
    │ attendance_rule_scope        │
    │ WHERE rule_record_id = ?     │
    │   AND scope_type 匹配工人属性 │
    │   AND (scope_ref = 工人ID    │
    │        OR scope_ref = '-1')  │
    ────────────────────────────────
                         │ 命中 1 条 rule_record
    ──── Step 3 ─────────▼────────
    │ 获取考勤区间                  │
    │ attendance_schedule_interval │
    │ WHERE attendance_rule_id = ? │
    ────────────────────────────────
                         │ 返回所有区间
    ──── Step 4 ─────────▼────────
    │ 周期匹配 + 类型过滤           │
    │ 计算 dayInCycle              │
    │ 过滤 interval_type IN        │
    │   ('WORK','OVERTIME')        │
    │ 过滤 day_index = dayInCycle  │
    ────────────────────────────────
                         │ 当天适用的工作时间段
    ──── Step 5 ─────────▼────────
    │ 弹性时间扩展                  │
    │ clockInStart = COALESCE      │
    │   (pre_float_start_time,     │
    │    start_time)               │
    │ clockInEnd = COALESCE        │
    │   (post_float_end_time,      │
    │    end_time)                 │
    ────────────────────────────────
                         │ 当天可打卡时间段
    ──── Step 6 ─────────▼────────
    │ 请假/休息扣减                 │
    │ leave_date  → 差集扣除       │
    │ rest_date   → 全天清空       │
    │ revoke_rest → 恢复可打卡     │
    ────────────────────────────────
                         │
                         ▼
              最终可打卡时间段列表
```

### 3.2 Step 1：查找当天生效的执行计划

**SQL 逻辑**（来自 Mapper XML `selectAttendanceRuleRecordByProjectIdAndDate`）：

```sql
SELECT arr.*, ar.rule_name, ar.rule_label, ...
    from attendance_rule_record arr
    left join attendance_rule ar on arr.attendance_rule_id = ar.id
    left join attendance_rule_scope ars on ars.rule_record_id = arr.id
    left join attendance_schedule_interval asi on asi.attendance_rule_id = arr.attendance_rule_id
WHERE arr.project_id = #{projectId}
  AND arr.effect_date <= #{date}
  AND (arr.lose_effect_date IS NULL OR arr.lose_effect_date >= #{date})
ORDER BY arr.default_rule ASC,     -- 非默认计划排前面
         arr.effect_date DESC,     -- 最新的排前面
         arr.create_time DESC
```

**关键点：**
- `effect_date <= date`：规则在目标日期之前已生效
- `lose_effect_date IS NULL OR >= date`：规则尚未失效
- **排序规则**：`default_rule ASC` 确保非默认计划（0）排在默认计划（1）之前；`effect_date DESC` 确保最新生效的排最前

### 3.3 Step 2：多维度匹配（Scope 过滤）
通过 workerId 和 projectId 查询 project_worker 获取组、队伍、公司、是否管理岗、工种 信息
当 Step 1 返回**多条** rule_record 时，需要通过 `attendance_rule_scope` 表匹配工人的适用范围。

**匹配逻辑**（来自 Java `matchesScope` 方法）：

```
FOR EACH rule_record IN results:
    FOR EACH scope IN rule_record.attendanceRuleScope:
        IF scope.scope_ref == '-1':
            → 通配符，匹配所有工人，直接返回该 rule_record
        
        SWITCH scope.scope_type:
            CASE 'GROUP':
                IF worker.groupId == scope.scope_ref → 匹配
            CASE 'TEAM':
                IF worker.teamId == scope.scope_ref → 匹配
            CASE 'COMPANY':
                IF worker.companyId == scope.scope_ref → 匹配
            CASE 'JOB':
                IF worker.workTypeId == scope.scope_ref → 匹配
```

**回退策略**（当所有 scope 都不匹配时）：

```
1. 优先选择第一条 非默认规则 (default_rule = 0)
2. 如果全部是默认规则 → 选择第一条（排序后最靠前的）
```

**仅一条记录时**：直接返回，无需 scope 匹配。

### 3.4 Step 3-4：获取考勤区间 + 周期匹配

**获取区间**：通过 `attendance_rule_id` 查询 `attendance_schedule_interval` 表。

**周期匹配算法**：

```
输入:
  effectDate  = rule_record.effect_date   -- 规则生效日期
  targetDate  = 查询的目标日期
  cycleDays   = interval.cycle_days       -- 周期总天数
  dayIndex    = interval.day_index        -- 区间对应周期第几天（1-based）

计算:
  daysDiff    = abs(ChronoUnit.DAYS.between(effectDate, targetDate))
  dayInCycle  = (daysDiff % cycleDays) + 1   -- 目标日期在周期中的第几天（1-based）

匹配条件:
  dayInCycle == dayIndex
```

**示例：做二休一（cycle_days = 3, 上班 day_index = 1,2, 休息 day_index = 3）**

| 天数差 daysDiff | daysDiff % 3 | dayInCycle | 是否上班 |
|:-:|:-:|:-:|:-:|
| 0 | 0 | 1 | 上班（day_index=1） |
| 1 | 1 | 2 | 上班（day_index=2） |
| 2 | 2 | 3 | 休息（day_index=3） |
| 3 | 0 | 1 | 上班 |
| 4 | 1 | 2 | 上班 |
| 5 | 2 | 3 | 休息 |

**类型过滤**：仅保留 `interval_type IN ('WORK', 'OVERTIME')`，过滤掉 `REST`。

### 3.5 Step 5：弹性时间扩展

```
可打卡起点 clockInStart:
    IF pre_float_start_time IS NOT NULL:
        clockInStart = pre_float_start_time
    ELSE:
        clockInStart = start_time

可打卡终点 clockInEnd:
    IF post_float_end_time IS NOT NULL:
        clockInEnd = post_float_end_time
    ELSE:
        clockInEnd = end_time
```

**示例**：

```
区间配置:
  start_time           = 08:00
  end_time             = 12:00
  pre_float_start_time = 07:30   -- 提前30分钟可打卡
  post_float_end_time  = NULL    -- 无后弹性

结果:
  可打卡时间段 = 07:30 ~ 12:00
  标准工作时间 = 08:00 ~ 12:00
```

~~3.6 Step 6：请假/休息扣减~~ 
不考虑此场景
在 Step 1~5 计算出"当天原始可打卡时间段列表"之后，还需要检查工人当天是否有已审批通过的请假或休息记录，并执行扣减。

**处理流程**：

```
Step 5 输出的可打卡时间段列表
                  │
      ──── Step 6a ──────────▼────────
      │ 查询工人当天已审批通过的       │
      │ 休息记录 (rest_date)             │
      ─────────────────────────────────
                  │
         ┌─────┼─────┐
         ▼           ▼
     有休息记录    无休息记录
     且未撤销         │
         │               │
         ▼               │
    可打卡时间 = []      │
    (全天无可打卡)      │
         │               │
      ──── Step 6b ────────▼────────
      │ 查询工人当天已审批通过的       │
      │ 请假记录 (leave_date)            │
      ─────────────────────────────────
                  │
         ┌─────┼─────┐
         ▼           ▼
     有请假记录    无请假记录
         │               │
         ▼               │
   对每个可打卡区间   │
   执行差集计算      │
   (扣除请假时段)     │
         │               │
      ──── Step 6c ────────▼────────
      │ 检查撤销休息记录             │
      │ (revoke_rest_record)          │
      ─────────────────────────────────
                  │
         ┌─────┼─────┐
         ▼           ▼
     有撤销记录    无撤销记录
     (恢复可打卡)      │
         │               │
         ▼               │
   重新计算当天        │
   完整可打卡时间      │
         │               │
         └────┬────┘
              ▼
    更新后的可打卡时间段列表
```

**三种场景**：

| 场景 | 处理逻辑 |
|------|----------|
| 全天休息 | `rest_date` 匹配 → 可打卡时间段直接置空（最高优先级） |
| 撤销休息 | `revoke_rest_record` 匹配 → 恢复为正常可打卡时间（无视休息） |
| 部分请假 | `leave_date` 时段与可打卡区间做差集，扣除请假部分 |

---

## 四、最终可打卡时间段定义

### 4.1 计算公式

对于每一条适用的考勤区间 `i`：

```
可打卡起点(i) = COALESCE(i.pre_float_start_time, i.start_time)
可打卡终点(i) = COALESCE(i.post_float_end_time,  i.end_time)
```

### 4.2 伪 SQL

```sql
-- 完整查询：获取某项目某天所有可打卡时间段
SELECT
    asi.interval_index,
    asi.interval_type,
    asi.start_time       AS work_start,
    asi.end_time         AS work_end,
    COALESCE(asi.pre_float_start_time,  asi.start_time) AS clock_in_start,
    COALESCE(asi.post_float_end_time,   asi.end_time)   AS clock_in_end,
    asi.is_cross_night,
    asi.cycle_days,
    asi.day_index
FROM attendance_rule_record arr
JOIN attendance_rule ar ON arr.attendance_rule_id = ar.id
JOIN attendance_schedule_interval asi ON ar.id = asi.attendance_rule_id
WHERE arr.project_id = #{projectId}
  AND arr.effect_date <= #{date}
  AND (arr.lose_effect_date IS NULL OR arr.lose_effect_date >= #{date})
  AND asi.interval_type IN ('WORK', 'OVERTIME')
  AND asi.day_index = (
      -- 周期计算
      MOD(DATEDIFF(#{date}, arr.effect_date), asi.cycle_days) + 1
  )
ORDER BY asi.interval_index ASC
```

> **注意**：当存在多条 rule_record 时，上述 SQL 还需要加入 scope 匹配逻辑来选择正确的那一条。实际实现中，scope 匹配在 Java 层完成（`matchRuleWithWorker` 方法），而非 SQL 层。

### 4.3 结果示例

```
项目 "XX工地"，2026-07-03（周三），周期第 1 天：

┌──────────┬──────────┬───────────┬──────────┬──────────┬──────────┬────────────┐
│ interval │ type     │ workStart │ workEnd  │ clockIn  │ clockIn  │ crossNight │
│ _index   │          │           │          │ Start    │ End      │            │
├──────────┼──────────┼───────────┼──────────┼──────────┼──────────┼────────────┤
│ 1        │ WORK     │ 08:00     │ 12:00    │ 07:30    │ 12:00    │ false      │
│ 2        │ WORK     │ 14:00     │ 18:00    │ 14:00    │ 18:30    │ false      │
│ 3        │ OVERTIME │ 19:00     │ 21:00    │ 19:00    │ 21:00    │ false      │
└──────────┴──────────┴───────────┴──────────┴──────────┴──────────┴────────────┘

可打卡时间段：
  上午  07:30 ~ 12:00  (弹性提前30分钟)
  下午  14:00 ~ 18:30  (弹性延后30分钟)
  加班  19:00 ~ 21:00  (无弹性)
```

---

## 五、边界场景说明

### 5.1 跨夜区间（`is_cross_night = 1`）

**场景**：夜班 22:00 ~ 次日 06:00

```
interval 配置:
  start_time     = 22:00
  end_time       = 06:00
  is_cross_night = 1
  day_index      = 1

含义:
  该区间从 day_index=1 那天的 22:00 开始，
  到 day_index=2 那天的 06:00 结束。

打卡归属:
  - 进场打卡（22:00 附近）：归属 day_index=1 的日期
  - 离场打卡（06:00 附近）：归属 day_index=1 的日期（结算至前一天）
                            或 day_index=2 的日期（结算至后一天）
                            取决于 work_hour_rule.cross_night_calc_type

cross_night_calc_type 结算规则:
  0 = 不结算（忽略跨夜部分）
  1 = 结算至前一天（全部算 day_index=1 那天的工时）
  2 = 结算至后一天（全部算 day_index=2 那天的工时）
  3 = 按零点分别结算（22:00~00:00 算前一天，00:00~06:00 算后一天）
```

**判断当前时刻是否在跨夜区间内**：

```
IF is_cross_night == 1:
    -- 跨夜区间跨越两天
    IF currentTime >= clockInStart:
        -- 当天晚间（如 22:00 ~ 23:59）
        可打卡 = true
    ELSE IF currentTime <= clockInEnd:
        -- 次日凌晨（如 00:00 ~ 06:00）
        可打卡 = true
    ELSE:
        可打卡 = false
ELSE:
    -- 普通区间
    可打卡 = (currentTime >= clockInStart AND currentTime <= clockInEnd)
```

### 5.2 工人无匹配规则时的回退策略

```
                    selectAttendanceRule()
                            │
                            ▼
              ┌─────────────────────────┐
              │ Mapper 查询返回 N 条记录  │
              └────────┬────────────────┘
                       │
            ┌──────────┼──────────┐
            ▼          ▼          ▼
        N = 0      N = 1      N > 1
        返回 null  直接返回    进入 scope 匹配
                                │
                    ┌───────────┼───────────┐
                    ▼           ▼           ▼
              scope 匹配    scope 全不    scope 全不
              命中 1 条     匹配且有      匹配且全部
              → 返回       非默认规则    为默认规则
                           → 返回第一条  → 返回第一条
                             非默认规则    (get(0))
```

**Java 层回退逻辑**（`matchRuleWithWorker` 方法）：

```java
// 1. 遍历所有规则，逐条检查 scope
for (rule : ruleRecords) {
    for (scope : rule.scopes) {
        if (matchesScope(scope, groupId, teamId, companyId, workTypeId)) {
            return rule;  // ← 第一个匹配的即为最终结果
        }
    }
}

// 2. 全部不匹配 → 回退到第一条非默认规则
return ruleRecords.stream()
    .filter(r -> !r.defaultRule)
    .findFirst()
    .orElse(ruleRecords.get(0));  // 3. 全为默认 → 返回第一条
```

### 5.3 多条规则记录时的优先级

**SQL 排序保证的优先级**（`ORDER BY` 子句）：

| 优先级 | 排序字段 | 说明 |
|:---:|---|---|
| 1 | `default_rule ASC` | **非默认计划（0）优先于默认计划（1）** |
| 2 | `effect_date DESC` | **最新生效的优先** |
| 3 | `create_time DESC` | 同时生效的，最新创建的优先 |

**完整优先级链**：

```
1. scope 精确匹配（GROUP/TEAM/COMPANY/JOB 维度）
   ↓ 若 scope_ref = '-1'，视为通配，直接匹配
   ↓ 若无任何 scope 匹配
2. 第一条非默认规则（default_rule = 0）
   ↓ 若全部为默认规则
3. 排序后的第一条（即 effect_date 最新的默认规则）
```

### 5.4 无考勤区间配置

当 `attendance_schedule_interval` 表中无对应记录时：
- 返回空列表，表示当天无可打卡时间段
- 可能场景：该天为休息日（cycle 中只有 `REST` 类型区间）

### 5.5 `cycle_days = 1` 的特殊情况

```
cycle_days = 1, day_index = 1:
  daysDiff % 1 == 0 → dayInCycle = 1 → 始终匹配
  含义：每天适用，无周期轮休
```

### 5.6 `effect_date` 为 NULL 或早于查询日期很久

- `effect_date` 不允许 NULL（表定义为 `NOT NULL`）
- 当 `effect_date` 远早于查询日期时，`daysDiff` 会很大，但取模运算保证周期循环正确
- 当查询日期 **早于** `effect_date` 时，SQL 的 `effect_date <= date` 条件会过滤掉该记录

---

## 六、完整数据示例

### 场景：查询项目 100 的工人在 2026-07-03 的可打卡时间

**数据库状态**：

```sql
-- 1. 考勤规则
attendance_rule: {id=1, rule_name='项目部白班', enable_over_time=1}

-- 2. 两条执行计划（同一项目，不同适用范围）
attendance_rule_record:
  {id=10, attendance_rule_id=1, project_id=100, effect_date='2026-01-01',
   lose_effect_date=NULL, default_rule=0}
  {id=20, attendance_rule_id=1, project_id=100, effect_date='2026-06-01',
   lose_effect_date=NULL, default_rule=1}

-- 3. 适用范围
attendance_rule_scope:
  {rule_record_id=10, scope_type='GROUP', scope_ref='50'}   -- 适用于班组50
  {rule_record_id=20, scope_type='COMPANY', scope_ref='-1'} -- 适用于全部单位

-- 4. 考勤区间（做六休一，cycle_days=7）
attendance_schedule_interval:
  {attendance_rule_id=1, cycle_days=7, day_index=1, interval_index=1,
   interval_type='WORK', start_time='08:00', end_time='12:00',
   pre_float_start_time='07:30', is_cross_night=0}
  {attendance_rule_id=1, cycle_days=7, day_index=1, interval_index=2,
   interval_type='WORK', start_time='14:00', end_time='18:00',
   post_float_end_time='18:30', is_cross_night=0}
  -- day_index 2~6 类似...
  {attendance_rule_id=1, cycle_days=7, day_index=7, interval_index=1,
   interval_type='REST', start_time='00:00', end_time='23:59'}
```

**查询过程**：

```
Step 1: project_id=100, date=2026-07-03
  → 命中 rule_record #10 和 #20（均满足 effect_date <= 07-03 且未失效）

Step 2: 工人属于班组 50
  → rule_record #10 的 scope (GROUP, '50') 精确匹配 → 选择 #10

Step 3: attendance_rule_id=1 的所有区间

Step 4: effect_date=2026-01-01, target=2026-07-03
  → daysDiff = 183
  → dayInCycle = (183 % 7) + 1 = 1 + 1 = 2
  → 过滤 day_index=2 且 interval_type IN ('WORK','OVERTIME')

Step 5: 返回当天可打卡时间段
  → [{08:00~12:00 (弹性07:30起)}, {14:00~18:00 (弹性至18:30)}]
```

---

## 七、请假/休息对可打卡时间的影响

### 7.1 概述

前五步计算出的"可打卡时间段"仅基于考勤规则配置。在实际业务中，工人可能因**请假**或**休息**而导致部分或全部时段不可打卡。Step 6 负责在原始可打卡时间段基础上，结合请假/休息审批记录进行**扣减或清空**。

**影响的表间关联关系**：

```
                          ┌─────────────────┐
                          │ project_worker   │
                          │ (项目工人)        │
                          └───────┬─────────┘
                                  │ project_worker_id
                ┌─────────┬────┴─────┬──────────┐
                ▼            ▼            ▼
       ┌────────────┐ ┌────────────┐ ┌───────────────┐
       │ leave_record│ │ rest_record │ │terminate_leave│
       │ (请假记录)   │ │ (休息记录)   │ │  _record       │
       │ state=20    │ │ state=20   │ │ (销假记录)     │
       └─────┬──────┘ └─────┬──────┘ └───────┬───────┘
             │ 1:N            │ 1:N                │ 1:N
             ▼                ▼                    ▼
       ┌────────────┐ ┌────────────┐ ┌───────────────┐
       │ leave_date  │ │ rest_date  │ │terminate_leave│
       │ (请假日期)   │ │ (休息日期)   │ │  _date         │
       │ date +      │ │ date       │ │ date +        │
       │ start/end   │ │ (全天)      │ │ start/end     │
       └────────────┘ └─────┬──────┘ └───────────────┘
                              │ 可撤销
                              ▼
                    ┌──────────────────┐
                    │revoke_rest_record│
                    │ (撤销休息记录)     │
                    │ rest_date_id     │
                    └──────────────────┘
```

### 7.2 请假场景分析

#### 7.2.1 查询已审批通过的请假记录

```sql
SELECT ld.date, ld.start_time, ld.end_time
FROM leave_record lr
JOIN leave_date ld ON lr.id = ld.leave_record_id
WHERE lr.project_worker_id = #{projectWorkerId}
  AND ld.project_id = #{projectId}
  AND ld.date = #{targetDate}
  AND lr.state = 20    -- 仅审批通过的记录
```

> 一条 `leave_record` 可对应多条 `leave_date`（请假跨多天）。对于目标日期，可能有多条 `leave_date`（如同一天的上午和下午分别请假）。

#### 7.2.2 时间区间差集计算

对于每个原始可打卡区间 `[clockInStart, clockInEnd]`，需要与所有请假时段 `[leaveStart, leaveEnd]` 做**差集运算**：

```
原始可打卡区间:  [clockInStart ───────────────── clockInEnd]
请假时段:                 [leaveStart ──── leaveEnd]
差集结果:          [clockInStart ─ leaveStart]  [leaveEnd ─ clockInEnd]
```

**差集规则**：

```
overlap_start = max(clockInStart, leaveStart)
overlap_end   = min(clockInEnd,   leaveEnd)

IF overlap_start >= overlap_end:
    -- 无交集，保留原区间
    result = [{clockInStart, clockInEnd}]

ELSE IF overlap_start <= clockInStart AND overlap_end >= clockInEnd:
    -- 请假完全覆盖区间，整个区间被扣除
    result = []

ELSE IF overlap_start <= clockInStart:
    -- 请假覆盖区间开头，截短前部
    result = [{overlap_end, clockInEnd}]

ELSE IF overlap_end >= clockInEnd:
    -- 请假覆盖区间尾部，截短后部
    result = [{clockInStart, overlap_start}]

ELSE:
    -- 请假在区间中间，分裂为两段
    result = [{clockInStart, overlap_start}, {overlap_end, clockInEnd}]
```

**差集示例**：

```
上午班可打卡: [07:30, 12:00]
请假时段:     [08:00, 12:00]

overlap_start = max(07:30, 08:00) = 08:00
overlap_end   = min(12:00, 12:00) = 12:00
→ overlap_start (08:00) > clockInStart (07:30)
  且 overlap_end (12:00) >= clockInEnd (12:00)
→ 截短后部: [07:30, 08:00] (仅利 30 分钟弹性前段)
→ 由于利余时段过短，实际业务中可判定上午无可打卡时段
```

#### 7.2.3 全天请假 vs 部分时段请假

| 场景 | 请假时段 | 对可打卡时间的影响 |
|------|----------|---------------------|
| **全天请假** | `start_time` / `end_time` 覆盖整天（如 `00:00` ~ `23:59`），或涵盖当天所有考勤区间 | 可打卡时间段列表 = **空**，等效于休息日 |
| **部分时段请假** | 仅覆盖部分区间（如 `08:00` ~ `12:00`） | 仅扣除受影响区间，其余区间保留 |
| **中间时段请假** | 在某个考勤区间内部（如 `09:00` ~ `11:00`） | 该区间被**分裂**为前后两段可打卡时段 |

> **注意**：即使工人在某区间仅请假部分时间，弹性起点 `pre_float_start_time` 仍然生效。例如上午班 `[07:30, 12:00]`，请假 `[08:00, 12:00]`，利余可打卡时段为 `[07:30, 08:00]`。

#### 7.2.4 销假对可打卡时间的恢复

当工人提前结束请假（销假）时，通过 `terminate_leave_record` + `terminate_leave_date` 记录销假信息：

```
销假查询:
SELECT tld.date, tld.start_time, tld.end_time,
       tld.source_leave_date_id
FROM terminate_leave_record tlr
JOIN terminate_leave_date tld ON tlr.id = tld.terminate_leave_record_id
JOIN approval_record ar ON tlr.approval_record_id = ar.id
WHERE tld.project_worker_id = #{projectWorkerId}
  AND tld.date = #{targetDate}
  AND ar.state = 20    -- 仅审批通过的销假
```

**销假扣减逻辑**：

```
原始请假时段:  [08:00 ──────────────── 18:00]
销假时段:             [14:00 ─────── 18:00]
实际请假时段:  [08:00 ─── 14:00]           ← 销假部分恢复为可打卡

等效计算: 将销假时段从请假时段中扣除，得到实际请假时段
再用实际请假时段与可打卡区间做差集
```

### 7.3 休息场景分析

#### 7.3.1 休息记录查询

休息与请假不同，休息是班组长/管理员主动为工人安排的休息日，通常为全天。

```sql
SELECT rd.date
FROM rest_record rr
JOIN rest_date rd ON rr.id = rd.rest_record_id
WHERE rr.project_worker_id = #{projectWorkerId}
  AND rd.project_id = #{projectId}
  AND rd.date = #{targetDate}
  AND rr.state = 20    -- 仅审批通过的记录
```

> `rest_date` 表仅有 `date` 字段，**无 `start_time` / `end_time`**。这意味着休息始终是**全天**的。

#### 7.3.2 全天休息处理

当确认工人在目标日期有已审批通过的休息记录时：

```
可打卡时间段 = []  (空列表)
```

即使考勤规则中配置了工作区间，休息审批的优先级更高，当天**无可打卡时间段**。

#### 7.3.3 撤销休息（`revoke_rest_record`）

当已审批通过的休息被撤销时，需要恢复该天的可打卡时间段：

```sql
SELECT rrr.id
FROM revoke_rest_record rrr
WHERE rrr.project_worker_id = #{projectWorkerId}
  AND rrr.date = #{targetDate}
  AND rrr.deleted = 0    -- 未删除的撤销记录
```

**处理逻辑**：

```
IF 存在 rest_date 记录 (state=20):
    IF 存在 revoke_rest_record 记录 (deleted=0):
        → 休息已撤销，恢复正常的可打卡时间段（重新走 Step 1~5 的结果）
    ELSE:
        → 休息未撤销，可打卡时间段 = []
```

> `revoke_rest_record.deleted` 字段表示该撤销记录本身是否被删除。`deleted=0` 表示撤销有效，休息已恢复；`deleted=1` 表示撤销被撤回，休息仍然生效。

### 7.4 审批状态过滤

所有请假/休息记录都必须经过审批流程。**只有审批通过的记录才影响可打卡时间段的计算。**

| `state` 值 | 含义 | 是否影响可打卡时间 |
|:---:|------|:---:|
| `-1` | 已撤销 | ✘ 忽略 |
| `0` | 待审核 | ✘ 忽略 |
| `10` | 驳回 | ✘ 忽略 |
| `20` | 通过 | ✔ 纳入扣除逻辑 |

**完整过滤查询示例**：

```sql
-- 查询某工人在某项目某天的有效请假（已通过）
SELECT ld.start_time, ld.end_time
FROM leave_record lr
JOIN leave_date ld ON lr.id = ld.leave_record_id
WHERE lr.project_worker_id = #{projectWorkerId}
  AND ld.project_id = #{projectId}
  AND ld.date = #{targetDate}
  AND lr.state = 20

-- 查询某工人在某项目某天的有效休息（已通过）
SELECT rd.date
FROM rest_record rr
JOIN rest_date rd ON rr.id = rd.rest_record_id
WHERE rr.project_worker_id = #{projectWorkerId}
  AND rd.project_id = #{projectId}
  AND rd.date = #{targetDate}
  AND rr.state = 20
```

> **重要**：未审批通过的请假/休息记录不影响可打卡时间段的计算，即使工人在系统中已提交了请假申请，只要未审批通过，仍视为正常工作日。

### 7.5 完整示例：请假/休息对可打卡时间的影响

延续第六节场景（项目 100，班组 50 的工人 A），补充请假/休息数据：

#### 场景 A：工人在 2026-07-03 上午请假（08:00 ~ 12:00），下午正常上班

**补充数据**：

```sql
-- 请假记录（已审批通过）
leave_record: {id=100, project_id=100, project_worker_id=A,
               state=20, reason='个人事务'}

-- 请假日期明细（仅上午）
leave_date: {id=200, leave_record_id=100, project_worker_id=A,
             date='2026-07-03', start_time='08:00', end_time='12:00'}
```

**计算过程**：

```
Step 1~5: 计算当天原始可打卡时间段
  2026-07-03, dayInCycle = 2

  上午班: [07:30, 12:00]  (弹性07:30起)
  下午班: [14:00, 18:30]  (弹性至18:30)

Step 6a: 检查休息记录
  → 无已审批通过的休息记录 → 跳过

Step 6b: 检查请假记录
  → leave_record #100, state=20 (已通过)
  → leave_date: 2026-07-03, [08:00, 12:00]

Step 6b-1: 差集计算 — 上午班 [07:30, 12:00] 与请假 [08:00, 12:00]
  overlap_start = max(07:30, 08:00) = 08:00
  overlap_end   = min(12:00, 12:00) = 12:00
  → overlap_start > clockInStart 且 overlap_end >= clockInEnd
  → 截短后部: [07:30, 08:00] (仅利 30 分钟弹性前段)
  → 实际业务判定: 上午无可打卡时段 (利余过短)

Step 6b-2: 差集计算 — 下午班 [14:00, 18:30] 与请假 [08:00, 12:00]
  overlap_start = max(14:00, 08:00) = 14:00
  overlap_end   = min(18:30, 12:00) = 12:00
  → overlap_start (14:00) > overlap_end (12:00) → 无交集
  → 保留原区间: [14:00, 18:30]

Step 6c: 检查撤销休息记录 → 无

最终可打卡时间段:
  ┌──────────┬──────────────┬──────────┬──────────┬────────────┐
  │ 区间     │ 类型          │ workStart │ workEnd  │ 可打卡时段  │
  ├──────────┼──────────────┼──────────┼──────────┼────────────┤
  │ 1(上午)  │ WORK(请假扣除) │ 08:00     │ 12:00    │ 无          │
  │ 2(下午)  │ WORK          │ 14:00     │ 18:00    │ 14:00~18:30 │
  └──────────┴──────────────┴──────────┴──────────┴────────────┘
```

#### 场景 B：工人在 2026-07-04 全天休息

**补充数据**：

```sql
-- 休息记录（已审批通过）
rest_record: {id=300, project_id=100, project_worker_id=A,
              state=20, reason='临时调休'}

-- 休息日期
rest_date: {id=400, rest_record_id=300, project_worker_id=A,
            date='2026-07-04'}
```

**计算过程**：

```
Step 1~5: 计算 2026-07-04 原始可打卡时间段
  dayInCycle = 3 (假设对应考勤区间配置)
  → 当天可打卡时间段: [{08:00~12:00}, {14:00~18:00}]

Step 6a: 检查休息记录
  → rest_record #300, state=20 (已通过)
  → rest_date: 2026-07-04 → 匹配！

Step 6a-1: 检查撤销休息记录
  → 无 revoke_rest_record 记录

结果: 全天休息，可打卡时间段 = []

最终可打卡时间段: (空)
```

#### 场景对比总结

| 日期 | 原始可打卡时间 | 请假/休息状态 | 最终可打卡时间 |
|------|--------------|-------------|--------------|
| 07-03 | 上午 07:30~12:00, 下午 14:00~18:30 | 上午请假 08:00~12:00 (state=20) | 下午 14:00~18:30 |
| 07-04 | 上午 07:30~12:00, 下午 14:00~18:30 | 全天休息 (state=20) | (空) |
| 07-04 | 上午 07:30~12:00, 下午 14:00~18:30 | 全天休息但已撤销 (deleted=0) | 上午 07:30~12:00, 下午 14:00~18:30 |
| 07-05 | 上午 07:30~12:00, 下午 14:00~18:30 | 请假待审批 (state=0) | 上午 07:30~12:00, 下午 14:00~18:30 |

> **关键规则**：只有 `state=20`（审批通过）的请假/休息记录才会触发 Step 6 的扣减逻辑。待审核、驳回、已撤销的记录均不影响可打卡时间段。
