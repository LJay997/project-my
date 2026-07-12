# 查询某项目某人某天可工作/可打卡时间段 — 设计文档

---

## 一、需求背景

在考勤精细化管理场景下，不同班组、队伍、单位或工种在同一项目内可能适用不同的考勤规则，每条规则中定义了若干个**考勤区间**（`AttendanceScheduleInterval`），包括上班时间（WORK）、休息时间（REST）、加班时间（OVERTIME）三种类型，并支持多日循环排班（如"做二休一"）和跨夜班次。

现需新增一个 Service 方法，**对外暴露"某项目、某人在某一天内可工作/可打卡的时间段"**，供打卡校验、迟到早退判定、前端展示等下游场景调用。

---

## 二、接口设计

### 2.1 方法签名

```java
/**
 * 查询某项目、某人在指定日期的可工作/可打卡时间段列表。
 *
 * @param projectId       项目 ID（必填）
 * @param projectWorkerId 项目工人 ID（可选，用于匹配考勤规则）
 * @param date            查询日期（必填）
 * @param groupId         班组 ID（可选）
 * @param teamId          队伍 ID（可选）
 * @param companyId       单位 ID（可选）
 * @param workTypeId      工种 ID（可选）
 * @return 当日可工作时间段列表；无规则匹配时返回空列表
 */
List<WorkTimePeriodVO> selectWorkTimePeriods(
        Long projectId,
        Long projectWorkerId,
        LocalDate date,
        Long groupId,
        Long teamId,
        Long companyId,
        Integer workTypeId
);
```

### 2.2 入参说明

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| `projectId` | `Long` | 是 | 项目主键，用于查询该项目下所有考勤规则记录 |
| `projectWorkerId` | `Long` | 否 | 项目工人主键，透传给 `selectAttendanceRule` 用于多维度匹配 |
| `date` | `LocalDate` | 是 | 查询的目标日期 |
| `groupId` | `Long` | 否 | 班组 ID，用于 GROUP 维度精确匹配 |
| `teamId` | `Long` | 否 | 队伍 ID，用于 TEAM 维度精确匹配 |
| `companyId` | `Long` | 否 | 单位 ID，用于 COMPANY 维度精确匹配 |
| `workTypeId` | `Integer` | 否 | 工种 ID，用于 JOB 维度精确匹配 |

### 2.3 出参说明

返回 `List<WorkTimePeriodVO>`，按 `intervalIndex` 升序排列。

---

## 三、数据结构

### 3.1 新增 VO：`WorkTimePeriodVO`

```java
package com.glodon.glm.attendance.bean.vo.customrule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * 某天内一个可工作/可打卡的时间段
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkTimePeriodVO {

    /**
     * 区间序号（当日内的顺序，从 1 开始）
     */
    private Integer intervalIndex;

    /**
     * 区间类型：WORK / OVERTIME
     */
    private String intervalType;

    /**
     * 时间段所属日期（可能因跨夜而落在次日）
     */
    private LocalDate date;

    /**
     * 上班打卡起始时间（含弹性时为弹性起点，否则为 startTime）
     */
    private LocalTime clockInStart;

    /**
     * 上班打卡结束时间（即 endTime；含弹性时为弹性终点）
     */
    private LocalTime clockInEnd;

    /**
     * 标准上班时间
     */
    private LocalTime workStart;

    /**
     * 标准下班时间
     */
    private LocalTime workEnd;

    /**
     * 是否跨夜（true 表示该区间跨越零点，结束时间实际落在次日）
     */
    private Boolean crossNight;

    /**
     * 是否有前弹性时段
     */
    private Boolean hasPreFloat;

    /**
     * 是否有后弹性时段
     */
    private Boolean hasPostFloat;
}
```

### 3.2 依赖的已有结构

| 类 | 关键字段 |
|----|----------|
| `AttendanceRuleRecordVO` | `effectDate`、`attendanceScheduleInterval` |
| `AttendanceScheduleInterval` | `cycleDays`、`dayIndex`、`intervalIndex`、`intervalType`、`startTime`、`endTime`、`isCrossNight`、`preFloatStartTime`、`postFloatEndTime` |
| `AttendanceIntervalTypeEnum` | `WORK`（上班）、`REST`（休息）、`OVERTIME`（加班） |

---

## 四、核心逻辑流程

### 4.1 伪代码

```
function selectWorkTimePeriods(projectId, projectWorkerId, date,
                               groupId, teamId, companyId, workTypeId):

    // Step 1: 复用已有方法，获取匹配的考勤规则
    rule = selectAttendanceRule(projectId, projectWorkerId, date,
                                groupId, teamId, companyId, workTypeId)
    if rule == null:
        return emptyList()

    // Step 2: 取出考勤区间列表
    intervals = rule.getAttendanceScheduleInterval()
    if intervals is null or empty:
        return emptyList()

    // Step 3: 计算目标日期在循环周期中的 dayIndex
    effectDate = rule.getEffectDate()
    cycleDay = calculateCycleDay(effectDate, date)

    // Step 4: 过滤出当日适用的 WORK / OVERTIME 区间
    result = []
    for interval in intervals:
        if interval.intervalType == REST:
            continue
        if not matchCycleDay(interval, cycleDay):
            continue
        // Step 5: 构造 WorkTimePeriodVO
        period = buildWorkTimePeriod(interval, date)
        result.add(period)

    // Step 6: 按 intervalIndex 排序后返回
    sort result by intervalIndex ascending
    return result
```

### 4.2 循环日计算逻辑

```
function calculateCycleDay(effectDate, targetDate):
    // 计算从生效日期到目标日期的天数差
    daysDiff = ChronoUnit.DAYS.between(effectDate, targetDate)
    // daysDiff 可能为负（查询生效前的日期），取绝对值取模
    return abs(daysDiff)   // 不取模，保留原始天数差供 matchCycleDay 使用
```

```
function matchCycleDay(interval, daysDiff):
    cycleDays = interval.getCycleDays()
    dayIndex  = interval.getDayIndex()
    if cycleDays == null or cycleDays <= 0:
        // 无循环配置，视为每日适用
        return true
    // dayIndex 从 1 开始，daysDiff 从 0 开始，需对齐
    return (daysDiff % cycleDays) + 1 == dayIndex
```

### 4.3 构造时间段对象

```
function buildWorkTimePeriod(interval, date):
    crossNight = interval.getIsCrossNight() != null && interval.getIsCrossNight()
    actualDate = crossNight ? date : date   // 跨夜时段的归属日期仍为打卡当天

    preFloat  = interval.getPreFloatStartTime()
    postFloat = interval.getPostFloatEndTime()

    return WorkTimePeriodVO.builder()
        .intervalIndex(interval.getIntervalIndex())
        .intervalType(interval.getIntervalType().name())
        .date(actualDate)
        .workStart(interval.getStartTime())
        .workEnd(interval.getEndTime())
        .clockInStart(preFloat  != null ? preFloat  : interval.getStartTime())
        .clockInEnd  (postFloat != null ? postFloat : interval.getEndTime())
        .crossNight(crossNight)
        .hasPreFloat(preFloat  != null)
        .hasPostFloat(postFloat != null)
        .build()
```

---

## 五、异常与边界处理

| 场景 | 处理策略 |
|------|----------|
| **无匹配规则** | `selectAttendanceRule` 返回 `null` 时，直接返回空列表 `[]` |
| **规则无区间配置** | `attendanceScheduleInterval` 为 `null` 或空，返回空列表 |
| **当日无适用区间** | 循环过滤后无匹配（如休息日），返回空列表 |
| **跨夜班次** | `isCrossNight=true` 时，区间归属打卡当天，`endTime` 实际落在次日凌晨；调用方需结合日期+时间判断 |
| **弹性时段** | 存在 `preFloatStartTime`/`postFloatEndTime` 时，`clockInStart`/`clockInEnd` 返回弹性边界；标准时间仍通过 `workStart`/`workEnd` 获取 |
| **生效日期为 null** | 若 `effectDate` 为空，默认 `cycleDay=0`，所有无循环配置的区间均适用 |
| **查询日期早于生效日期** | `daysDiff` 为负数时取绝对值计算，保证取模结果正确 |
| **cycleDays=0 或 null** | 视为不启用循环，该区间每日均适用 |
| **OVERTIME 区间** | 与 WORK 区间同样纳入返回结果，调用方可通过 `intervalType` 字段区分 |

---

## 六、调用示例

### 6.1 基本调用

```java
@Autowired
private AttendanceRuleRecordService attendanceRuleRecordService;

// 查询某项目、某班组在 2026-07-03 的工作时间段
List<WorkTimePeriodVO> periods = attendanceRuleRecordService.selectWorkTimePeriods(
        100L,       // projectId
        null,       // projectWorkerId（不指定具体工人）
        LocalDate.of(2026, 7, 3),
        50L,        // groupId
        null,       // teamId
        null,       // companyId
        null        // workTypeId
);

// 输出示例：
// [WorkTimePeriodVO(intervalIndex=1, intervalType=WORK,
//     date=2026-07-03, workStart=08:00, workEnd=12:00,
//     clockInStart=07:30, clockInEnd=12:00, crossNight=false, ...),
//  WorkTimePeriodVO(intervalIndex=2, intervalType=WORK,
//     date=2026-07-03, workStart=14:00, workEnd=18:00,
//     clockInStart=14:00, clockInEnd=18:30, crossNight=false, ...)]
```

### 6.2 判断当前时刻是否在可打卡时间内

```java
LocalTime now = LocalTime.now();
boolean canClockIn = periods.stream().anyMatch(p ->
    p.getDate().equals(LocalDate.now())
        && !now.isBefore(p.getClockInStart())
        && !now.isAfter(p.getClockInEnd())
);
```

### 6.3 计算当日应出勤工时

```java
double totalHours = periods.stream()
    .filter(p -> "WORK".equals(p.getIntervalType()))
    .mapToDouble(p -> Duration.between(p.getWorkStart(), p.getWorkEnd()).toMinutes() / 60.0)
    .sum();
// totalHours = 8.0 (上午4h + 下午4h)
```

---

## 七、与现有方法的关系

```
selectAttendanceRule(...)          ← 已有的规则匹配方法（不变）
        │
        ▼
AttendanceRuleRecordVO
        │
        ├── attendanceScheduleInterval  ← 提取区间列表
        │
        ▼
selectWorkTimePeriods(...)         ← 新增方法（调用 selectAttendanceRule 后二次加工）
        │
        ▼
List<WorkTimePeriodVO>             ← 返回结构化时间段
```

新增方法完全复用 `selectAttendanceRule` 的多维度匹配逻辑（GROUP/TEAM/COMPANY/JOB scope 匹配、单条直接返回、默认规则回退等），自身仅负责**区间过滤与结构化转换**，不引入任何新的数据库查询。
