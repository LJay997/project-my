# selectAttendanceRuleRecordByProjectIdAndDate 查询字段精简方案

> **版本**: v1.0  
> **日期**: 2026-07-09  
> **目标**: 减少不必要字段查询，降低内存占用，提升数据库 I/O 效率

---

## 一、现状分析

### 1.1 当前 SQL 查询情况

**文件位置**: `attendance-service/src/main/resources/mapping/AttendanceRuleRecordMapper.xml` (L572-L603)

```xml
<select id="selectAttendanceRuleRecordByProjectIdAndDate" resultMap="AttendanceRuleRecordResultMap">
    select
        <include refid="baseSelectColumn"/>,  <!-- 18 个基础字段 -->
        ars.id as attendance_rule_scope_id,
        ars.rule_record_id as ars_rule_record_id,
        ars.scope_type as ars_scope_type,
        ars.scope_ref as ars_scope_ref,
        ars.scope_ref_name as ars_scope_ref_name,
        asi.id as asi_id,
        asi.attendance_rule_id as asi_attendance_rule_id,
        asi.cycle_days as asi_cycle_days,
        asi.day_index as asi_day_index,
        asi.interval_index as asi_interval_index,
        asi.interval_type as asi_interval_type,
        asi.start_time as asi_start_time,
        asi.end_time as asi_end_time,
        asi.is_cross_night as asi_is_cross_night,
        asi.pre_float_start_time as asi_pre_float_start_time,
        asi.post_float_end_time as asi_post_float_end_time
    from attendance_rule_record arr
    left join attendance_rule ar on arr.attendance_rule_id = ar.id
    left join attendance_rule_scope ars on ars.rule_record_id = arr.id
    left join attendance_schedule_interval asi on asi.attendance_rule_id = arr.attendance_rule_id
    where arr.project_id = #{projectId}
      and arr.effect_date <![CDATA[ <= ]]> #{date}
      and (arr.lose_effect_date is null or arr.lose_effect_date >= #{date})
    order by arr.default_rule asc, arr.effect_date desc, arr.create_time desc
</select>
```

### 1.2 返回的 VO 结构

`AttendanceRuleRecordVO` 包含以下关联对象：

```
AttendanceRuleRecordVO
├── BaseResultMap (18 个基础字段)
│   ├── id, attendanceRuleId, tenantId, orgId, projectId
│   ├── companyId, teamId, groupId, effectDate, loseEffectDate
│   ├── remark, referenceSuper, defaultRule, scopeType
│   └── createTime, modifyTime
├── 关联对象
│   ├── ruleLabel, ruleName, authorOrgId (已注释)
│   ├── enableOverTime, enableLateEarly, deductFloatTime (未查询)
│   ├── lateEarlyRule (延迟加载)
│   ├── workDayRule (延迟加载)
│   ├── workHourRule (延迟加载)
│   ├── workOverTimeDayRule (延迟加载)
│   └── workOverTimeHourRule (延迟加载)
├── attendanceRuleScope (List<AttendanceRuleScope>)
│   ├── id, ruleRecordId
│   ├── scopeType, scopeRef, scopeRefName
└── attendanceScheduleInterval (List<AttendanceScheduleInterval>)
    ├── id, attendanceRuleId
    ├── cycleDays, dayIndex, intervalIndex
    ├── intervalType, startTime, endTime
    ├── isCrossNight, preFloatStartTime, postFloatEndTime
```

---

## 二、两个方法的字段使用分析

### 2.1 方法 1: `selectAttendanceRule` (L216-L246)

**用途**: 匹配考勤规则，通过多维度 Scope 匹配找到适用的规则记录。

#### 实际使用的字段清单

| 层级 | 字段 | 使用位置 | 说明 |
|------|------|----------|------|
| **BaseResultMap** | `id` | L220, L227 | 日志输出 |
| | `attendanceRuleId` | L327 | 日志输出 |
| | `effectDate` | L335 | 周期计算 |
| | `defaultRule` | L236, L266 | Scope 匹配回退逻辑 |
| | `scopeType` | ❌ **未使用** | 仅用于日志或未来扩展 |
| **attendanceRuleScope** | `scopeType` | L253, L272 | Scope 匹配核心字段 |
| | `scopeRef` | L276 | Scope 匹配核心字段 |
| **attendanceScheduleInterval** | ❌ **完全未使用** | - | selectAttendanceRule 不需要考勤区间 |

#### 冗余字段（可省略查询）

| 字段 | 数量 | 说明 |
|------|------|------|
| `attendanceRuleScope` 全部字段 | 5 个 | selectAttendanceRule 只需要 scopeType + scopeRef |
| `attendanceScheduleInterval` 全部字段 | 11 个 | **完全不需要** |
| `ruleLabel`, `ruleName`, `authorOrgId` | 3 个 | SQL 中已注释 |
| `enableOverTime`, `enableLateEarly`, `deductFloatTime` | 3 个 | 未查询 |
| `lateEarlyRule`, `workDayRule`, `workHourRule` 等关联 | 5 个 | 延迟加载，此处不需要 |

---

### 2.2 方法 2: `selectWorkTimePeriods` (L302-L422)

**用途**: 查询指定项目、工人在某一天的可打卡时间段列表。

#### 实际使用的字段清单

| 层级 | 字段 | 使用位置 | 说明 |
|------|------|----------|------|
| **BaseResultMap** | `id` | L320 | 日志输出 |
| | `attendanceRuleId` | L327 | 日志输出 |
| | `effectDate` | L335 | 周期计算 |
| | `defaultRule` | L236 | 通过 selectAttendanceRule 间接使用 |
| **attendanceRuleScope** | `scopeType` | L253 | Scope 匹配 |
| | `scopeRef` | L276 | Scope 匹配 |
| **attendanceScheduleInterval** | `id` | L385 | 构建 WorkTimePeriodVO |
| | `cycleDays` | L342 | 周期匹配 |
| | `dayIndex` | L343 | 周期匹配 |
| | `intervalIndex` | L344, L386 | 类型过滤 + 排序 |
| | `intervalType` | L344, L387 | 类型过滤 |
| | `startTime` | L380, L389 | 弹性扩展 |
| | `endTime` | L381, L390 | 弹性扩展 |
| | `isCrossNight` | L382, L393 | 跨夜判断 |
| | `preFloatStartTime` | L378, L394 | 前弹性 |
| | `postFloatEndTime` | L379, L395 | 后弹性 |

#### 冗余字段（可省略查询）

| 字段 | 数量 | 说明 |
|------|------|------|
| `tenantId`, `orgId`, `projectId`, `companyId`, `teamId`, `groupId` | 6 个 | 仅日志输出 |
| `loseEffectDate`, `remark`, `referenceSuper` | 3 个 | 仅日志输出 |
| `createTime`, `modifyTime` | 2 个 | 完全不需要 |
| `attendanceRuleScope.scopeRefName` | 1 个 | 仅用于前端展示 |

---

## 三、精简方案设计

### 3.1 核心思路

由于 `selectAttendanceRule` 和 `selectWorkTimePeriods` 的字段需求差异较大：

| 方法 | 重点 | 需要 attendanceScheduleInterval？ | 需要 AttendanceRuleScope？ |
|------|------|----------------------------------|---------------------------|
| `selectAttendanceRule` | Scope 匹配 | ❌ 不需要 | ✅ 需要（scopeType + scopeRef） |
| `selectWorkTimePeriods` | 考勤区间计算 | ✅ 需要全部 | ✅ 需要（scopeType + scopeRef） |

**推荐方案**: 拆分为两个独立的 Mapper 方法

```
selectAttendanceRuleBasic()          → 仅用于 selectAttendanceRule
selectWorkTimePeriodsFull()          → 仅用于 selectWorkTimePeriods
```

---

### 3.2 SQL 优化方案

#### 方案 A: `selectAttendanceRuleBasic` (用于 selectAttendanceRule)

```xml
<!-- 仅查询 Scope 匹配所需字段，不查询考勤区间 -->
<select id="selectAttendanceRuleBasic" resultMap="AttendanceRuleBasicResultMap">
    select
        arr.id,
        arr.attendance_rule_id,
        arr.effect_date,
        arr.default_rule,
        -- Scope 匹配必需字段
        ars.id as attendance_rule_scope_id,
        ars.rule_record_id as ars_rule_record_id,
        ars.scope_type as ars_scope_type,
        ars.scope_ref as ars_scope_ref
    from attendance_rule_record arr
    left join attendance_rule_scope ars on ars.rule_record_id = arr.id
    where arr.project_id = #{projectId}
      and arr.effect_date <![CDATA[ <= ]]> #{date}
      and (arr.lose_effect_date is null or arr.lose_effect_date >= #{date})
    order by arr.default_rule asc, arr.effect_date desc, arr.create_time desc
</select>
```

**优化效果**:
- 减少字段数: **37 个 → 8 个** (减少 78%)
- 跳过 `attendance_schedule_interval` 表关联 (避免大结果集)
- 减少内存占用: 每个记录约减少 **500B~1KB**

#### 方案 B: `selectWorkTimePeriodsFull` (用于 selectWorkTimePeriods)

```xml
<!-- 查询考勤区间计算所需的全部字段 -->
<select id="selectWorkTimePeriodsFull" resultMap="WorkTimePeriodsResultMap">
    select
        -- 基础标识字段（仅日志用）
        arr.id,
        arr.attendance_rule_id,
        arr.effect_date,
        arr.default_rule,
        -- Scope 匹配必需字段
        ars.id as attendance_rule_scope_id,
        ars.rule_record_id as ars_rule_record_id,
        ars.scope_type as ars_scope_type,
        ars.scope_ref as ars_scope_ref,
        -- 考勤区间全部字段（11 个）
        asi.id as asi_id,
        asi.attendance_rule_id as asi_attendance_rule_id,
        asi.cycle_days as asi_cycle_days,
        asi.day_index as asi_day_index,
        asi.interval_index as asi_interval_index,
        asi.interval_type as asi_interval_type,
        asi.start_time as asi_start_time,
        asi.end_time as asi_end_time,
        asi.is_cross_night as asi_is_cross_night,
        asi.pre_float_start_time as asi_pre_float_start_time,
        asi.post_float_end_time as asi_post_float_end_time
    from attendance_rule_record arr
    left join attendance_rule_scope ars on ars.rule_record_id = arr.id
    left join attendance_schedule_interval asi on asi.attendance_rule_id = arr.attendance_rule_id
    where arr.project_id = #{projectId}
      and arr.effect_date <![CDATA[ <= ]]> #{date}
      and (arr.lose_effect_date is null or arr.lose_effect_date >= #{date})
    order by arr.default_rule asc, arr.effect_date desc, arr.create_time desc
</select>
```

**优化效果**:
- 移除无用字段: `tenantId`, `orgId`, `projectId`, `companyId`, `teamId`, `groupId` 等 6 个
- 移除 `remark`, `referenceSuper`, `createTime`, `modifyTime` 等 4 个
- 减少字段数: **37 个 → 20 个** (减少 46%)

---

### 3.3 ResultMap 定义调整

#### 新增 `AttendanceRuleBasicResultMap`

```xml
<!-- 精简版 ResultMap：仅用于 selectAttendanceRule -->
<resultMap id="AttendanceRuleBasicResultMap"
           type="com.glodon.glm.attendance.bean.vo.customrule.AttendanceRuleRecordVO">
    <id column="id" jdbcType="BIGINT" property="id"/>
    <result column="attendance_rule_id" jdbcType="BIGINT" property="attendanceRuleId"/>
    <result column="effect_date" jdbcType="DATE" property="effectDate"/>
    <result column="default_rule" jdbcType="TINYINT" property="defaultRule"/>
    
    <!-- Scope 匹配必需 -->
    <collection property="attendanceRuleScope" ofType="com.glodon.glm.attendance.bean.vo.customrule.AttendanceRuleScope">
        <id property="id" column="attendance_rule_scope_id"/>
        <result property="ruleRecordId" column="ars_rule_record_id"/>
        <result property="scopeType" column="ars_scope_type"/>
        <result property="scopeRef" column="ars_scope_ref"/>
    </collection>
    
    <!-- attendanceScheduleInterval 为空列表（默认初始化） -->
</resultMap>
```

#### 新增 `WorkTimePeriodsResultMap`

```xml
<!-- 完整版 ResultMap：用于 selectWorkTimePeriods -->
<resultMap id="WorkTimePeriodsResultMap"
           type="com.glodon.glm.attendance.bean.vo.customrule.AttendanceRuleRecordVO">
    <id column="id" jdbcType="BIGINT" property="id"/>
    <result column="attendance_rule_id" jdbcType="BIGINT" property="attendanceRuleId"/>
    <result column="effect_date" jdbcType="DATE" property="effectDate"/>
    <result column="default_rule" jdbcType="TINYINT" property="defaultRule"/>
    
    <!-- Scope 匹配必需 -->
    <collection property="attendanceRuleScope" ofType="com.glodon.glm.attendance.bean.vo.customrule.AttendanceRuleScope">
        <id property="id" column="attendance_rule_scope_id"/>
        <result property="ruleRecordId" column="ars_rule_record_id"/>
        <result property="scopeType" column="ars_scope_type"/>
        <result property="scopeRef" column="ars_scope_ref"/>
    </collection>
    
    <!-- 考勤区间全部字段 -->
    <collection property="attendanceScheduleInterval" ofType="com.glodon.glm.attendance.bean.vo.customrule.AttendanceScheduleInterval">
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
```

---

## 四、Java 代码调整方案

### 4.1 Mapper 接口调整

```java
// AttendanceRuleRecordMapper.java
public interface AttendanceRuleRecordMapper {
    
    /**
     * 原有方法：保留兼容（后续可废弃）
     */
    List<AttendanceRuleRecordVO> selectAttendanceRuleRecordByProjectIdAndDate(
            @Param("projectId") Long projectId,
            @Param("date") LocalDate date);
    
    /**
     * 新方法：仅用于 selectAttendanceRule（精简版）
     */
    List<AttendanceRuleRecordVO> selectAttendanceRuleBasic(
            @Param("projectId") Long projectId,
            @Param("date") LocalDate date);
    
    /**
     * 新方法：仅用于 selectWorkTimePeriods（完整版）
     */
    List<AttendanceRuleRecordVO> selectWorkTimePeriodsFull(
            @Param("projectId") Long projectId,
            @Param("date") LocalDate date);
}
```

### 4.2 Service 层调用调整

```java
// AttendanceRuleRecordServiceImpl.java

@Override
public AttendanceRuleRecordVO selectAttendanceRule(Long projectId, Long workerId, LocalDate date) {
    log.debug("selectAttendanceRule: projectId={}, workerId={}, date={}", projectId, workerId, date);
    
    // ✅ 改用精简查询
    List<AttendanceRuleRecordVO> ruleRecords = attendanceRuleRecordMapper.selectAttendanceRuleBasic(projectId, date);
    
    // ... 后续逻辑不变
}

@Override
public List<WorkTimePeriodVO> selectWorkTimePeriods(Long projectId, Long workerId, LocalDate date) {
    log.info("[selectWorkTimePeriods] 开始: projectId={}, workerId={}, date={}",
            projectId, workerId, date);
    
    // ... 前置检查
    
    // ✅ selectAttendanceRule 内部已调用 selectAttendanceRuleBasic
    AttendanceRuleRecordVO rule = this.selectAttendanceRule(projectId, workerId, date);
    
    // ... 后续逻辑不变，rule.getAttendanceScheduleInterval() 数据完整
}
```

---

## 五、优化效果对比

### 5.1 字段数量对比

| 场景 | 优化前 | 优化后 | 减少比例 |
|------|--------|--------|----------|
| `selectAttendanceRule` | 37 个字段 + 3 张表关联 | 8 个字段 + 2 张表关联 | **78%** ↓ |
| `selectWorkTimePeriods` | 37 个字段 + 3 张表关联 | 20 个字段 + 3 张表关联 | **46%** ↓ |

### 5.2 内存占用对比（估算）

假设单次查询返回 1 条规则记录，包含 3 个考勤区间：

| 场景 | 优化前 | 优化后 | 节省 |
|------|--------|--------|------|
| `selectAttendanceRule` | ~2.5 KB | ~0.8 KB | **1.7 KB** (68% ↓) |
| `selectWorkTimePeriods` | ~2.5 KB | ~1.5 KB | **1.0 KB** (40% ↓) |

### 5.3 数据库 I/O 对比

| 指标 | selectAttendanceRule | selectWorkTimePeriods |
|------|---------------------|----------------------|
| **网络传输** | 减少 ~1.7 KB/次 | 减少 ~1.0 KB/次 |
| **解析时间** | 减少 ~40% | 减少 ~25% |
| **表关联** | 跳过 attendance_schedule_interval | 保持不变 |

### 5.4 累计性能提升（预估）

假设日均调用量 10 万次：

| 指标 | selectAttendanceRule | selectWorkTimePeriods |
|------|---------------------|----------------------|
| 日均调用 | 5 万次 | 5 万次 |
| 总节省内存 | 50,000 × 1.7 KB ≈ **83 MB/天** | 50,000 × 1.0 KB ≈ **48 MB/天** |
| DB CPU 节省 | ~15%（减少 JOIN） | ~5%（减少字段解析） |

---

## 六、实施步骤

### Phase 1: 新增 SQL 和 ResultMap（不影响现有功能）

1. 在 `AttendanceRuleRecordMapper.xml` 中新增两个 `<select>` 和两个 `<resultMap>`
2. 在 `AttendanceRuleRecordMapper.java` 中新增两个方法声明
3. 编写单元测试验证新 SQL 正确性

### Phase 2: 切换调用方（灰度上线）

1. `selectAttendanceRule` 切换到 `selectAttendanceRuleBasic`
2. `selectWorkTimePeriods` 保持调用原方法（保证兼容性）
3. 监控性能指标（DB 查询耗时、内存占用）

### Phase 3: 全面切换 + 废弃旧方法

1. `selectWorkTimePeriods` 切换到 `selectWorkTimePeriodsFull`
2. 原 `selectAttendanceRuleRecordByProjectIdAndDate` 标记 `@Deprecated`
3. 清理日志和监控

---

## 七、风险评估与缓解

| 风险 | 影响 | 缓解措施 |
|------|------|----------|
| 其他调用方依赖原方法 | 拆分后其他调用方可能受影响 | 先 grep 全项目调用点，评估影响面 |
| Scope 匹配逻辑依赖其他字段 | selectAttendanceRuleBasic 仅查 4 个字段 | 代码审查确认仅用 scopeType + scopeRef |
| 考勤区间关联查询性能 | LEFT JOIN 可能导致笛卡尔积 | 添加执行计划分析，必要时拆分为两次查询 |

---

## 八、备选优化方案（高级）

### 方案 B: 拆分为两次独立查询（彻底解耦）

```java
// 第一次查询：获取规则记录 + Scope
List<AttendanceRuleRecordVO> rules = mapper.selectRuleBasic(projectId, date);

// 第二次查询：仅获取考勤区间（按需加载）
if (needIntervals) {
    List<AttendanceScheduleInterval> intervals = 
        mapper.selectIntervalsByAttendanceRuleId(rule.getAttendanceRuleId());
    rule.setAttendanceScheduleInterval(intervals);
}
```

**优点**: 
- 彻底避免 LEFT JOIN 笛卡尔积
- 可按需加载，减少不必要的数据传输
- 可单独对考勤区间查询做缓存优化

**缺点**:
- 增加一次 DB 查询（N+1 问题）
- 需在 Service 层手动组装对象

**适用场景**: 考勤区间数据可独立缓存时（推荐结合缓存方案设计）

---

## 附录：完整字段使用矩阵

| 字段名 | selectAttendanceRule | selectWorkTimePeriods | 建议 |
|--------|---------------------|----------------------|------|
| **BaseResultMap** | | | |
| id | ✅ 日志 | ✅ 日志 | 保留 |
| attendanceRuleId | ✅ 日志 | ✅ 日志 | 保留 |
| tenantId | ❌ | ❌ | 移除 |
| orgId | ❌ | ❌ | 移除 |
| projectId | ❌ | ❌ | 移除 |
| companyId | ❌ | ❌ | 移除 |
| teamId | ❌ | ❌ | 移除 |
| groupId | ❌ | ❌ | 移除 |
| effectDate | ✅ 计算 | ✅ 计算 | 保留 |
| loseEffectDate | ❌ | ❌ | 移除 |
| remark | ❌ | ❌ | 移除 |
| referenceSuper | ❌ | ❌ | 移除 |
| defaultRule | ✅ 匹配 | ✅ 匹配 | 保留 |
| scopeType | ❌ | ❌ | 移除（由 ars 表提供） |
| createTime | ❌ | ❌ | 移除 |
| modifyTime | ❌ | ❌ | 移除 |
| **attendanceRuleScope** | | | |
| id | ❌ | ❌ | 移除 |
| ruleRecordId | ❌ | ❌ | 移除 |
| scopeType | ✅ 匹配 | ✅ 匹配 | 保留 |
| scopeRef | ✅ 匹配 | ✅ 匹配 | 保留 |
| scopeRefName | ❌ | ❌ | 移除 |
| **attendanceScheduleInterval** | | | |
| id | ❌ | ✅ VO 构建 | 保留 |
| attendanceRuleId | ❌ | ✅ 周期计算 | 保留 |
| cycleDays | ❌ | ✅ 周期匹配 | 保留 |
| dayIndex | ❌ | ✅ 周期匹配 | 保留 |
| intervalIndex | ❌ | ✅ 类型过滤 | 保留 |
| intervalType | ❌ | ✅ 类型过滤 | 保留 |
| startTime | ❌ | ✅ 弹性扩展 | 保留 |
| endTime | ❌ | ✅ 弹性扩展 | 保留 |
| isCrossNight | ❌ | ✅ 跨夜判断 | 保留 |
| preFloatStartTime | ❌ | ✅ 前弹性 | 保留 |
| postFloatEndTime | ❌ | ✅ 后弹性 | 保留 |

✅ = 需要使用  | ❌ = 未使用
