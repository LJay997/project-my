# 工日数据混合查询功能说明

## 功能概述

新增了基于时间阈值的工日数据混合查询功能。该功能允许根据时间阈值，从不同的数据源（ES 和 MongoDB）查询工人的工日数据：
- **阈值之前的数据**：从 ElasticSearch 查询
- **阈值之后的数据**：从 MongoDB (`work_day_calculation_results` 集合) 查询

## 核心文件说明

### 1. MongoDB 实体类
**文件**: `com.glodon.glm.attendance.bean.po.WorkDayCalculationResult`

对应 MongoDB 集合 `work_day_calculation_results`，包含以下字段：
- `date`: 日期
- `project_id`: 项目ID
- `tenant_id`: 租户ID
- `type`: 类型（如：cove_times）
- `worker_id`: 工人ID
- `labor_worker_id`: 工人履历ID
- `project_worker_id`: 项目工人ID
- `work_day`: 工日数据（核心字段）
- `project_work_day_config_id`: 项目工日配置ID

### 2. DAO 层
**文件**: `com.glodon.glm.attendance.dao.WorkDayCalculationResultDao`

主要方法：
- `findByProjectIdAndLaborWorkerIdsAndDateRange()`: 根据项目ID、工人履历ID列表和日期范围查询工日数据
- `getWorkDayTypeByProjectId()`: 获取项目的工日计算类型（目前默认返回 "cove_times"）

### 3. 数据增强接口
**文件**: `com.glodon.glm.attendance.service.WorkDayDataEnhancer`

扩展接口，用于对从 MongoDB 查询出来的工日数据进行额外字段填充。

默认实现：`com.glodon.glm.attendance.service.impl.DefaultWorkDayDataEnhancer`
- 不做任何额外处理，直接返回原数据
- 如需扩展填充逻辑，可以创建新的实现类

### 4. Service 层
**文件**: `com.glodon.glm.attendance.service.restructure.AttendanceNewQueryService`

新增方法：`laborWorkerAttendanceDataWithThreshold()`

**方法逻辑**：
1. 接收包含时间阈值日期的查询参数
2. **提前比较** `thresholdDate` 与 `beginDate`、`endDate` 的关系
3. 根据比较结果，**快速确定查询策略**：
   - **情况1**：`endDate < thresholdDate` → 完全在阈值之前，**只查询 ES**，立即返回
   - **情况2**：`beginDate >= thresholdDate` → 完全在阈值之后，**只查询 MongoDB**，立即返回
   - **情况3**：`beginDate < thresholdDate <= endDate` → 跨越阈值，**分段查询并合并结果**
4. 调用数据增强器填充额外字段
5. 返回结果

### 6. Controller 层
**文件**: `com.glodon.glm.attendance.controller.restructure.AttendanceNewController`

新增接口：
- **URL**: `POST /attendanceNew/laborWorker/attendanceDataWithThreshold`
- **参数**: `LaborWorkerAttendanceWithThresholdParam`（阈值日期可选，不传时自动从Redis获取）
- **返回**: `Result<List<LaborWorkerAttendanceCountVO>>`

## API 使用示例

### 请求示例

```json
POST /attendanceNew/laborWorker/attendanceDataWithThreshold
Content-Type: application/json

{
  "projectId": 875605502353920,
  "beginDate": "2025-09-01",
  "endDate": "2025-10-17",
  "laborWorkerIds": [12345678, 87654321],
  "thresholdDate": "2025-09-17"
}
```

### 参数说明

| 参数名 | 类型 | 必填 | 说明 |
|--------|------|------|------|
| projectId | Long | 是 | 项目ID |
| beginDate | LocalDate | 是 | 开始日期 (yyyy-MM-dd) |
| endDate | LocalDate | 是 | 结束日期 (yyyy-MM-dd) |
| laborWorkerIds | List<Long> | 是 | 工人履历ID列表 |
| thresholdDate | LocalDate | 否 | 时间阈值日期 (yyyy-MM-dd)，阈值之前查询ES，阈值及之后查询MongoDB。如果为空则自动从Redis获取项目配置，仍为空则使用原有查询逻辑（只查ES） |

### 响应示例

```json
{
  "success": true,
  "code": null,
  "message": null,
  "data": [
    {
      "tenantId": 884855267241984,
      "projectId": 875605502353920,
      "workerId": 123456,
      "laborWorkerId": 12345678,
      "workDays": 25.5,
      "workOverDays": 0,
      "workHours": 204.0,
      "attendanceDays": 26,
      "leaveDays": 0
    }
  ]
}
```

## 工作原理示意

```
查询区间: 2025-09-01 至 2025-10-17
阈值日期: 2025-09-17

时间线:
|-------- ES 数据 --------|-------- MongoDB 数据 --------|
2025-09-01        2025-09-16  2025-09-17          2025-10-17
                          ↑
                      阈值日期
                      
说明：
- 2025-09-01 ~ 2025-09-16：从 ES 查询
- 2025-09-17 ~ 2025-10-17：从 MongoDB 查询
```

## 查询策略决策树

系统会**提前比较**日期范围，快速确定查询策略，避免不必要的判断：

```
开始
  ↓
判断: endDate < thresholdDate?
  ↓ 是                               ↓ 否
只查ES，返回                          判断: beginDate >= thresholdDate?
                                    ↓ 是                      ↓ 否
                                  只查MongoDB，返回          跨阈值，ES+MongoDB合并
```

**性能优化**：
- 完全在阈值前/后的查询会提前返回，无需进入复杂的分段逻辑
- 减少不必要的条件判断和数据处理
- 代码结构更清晰，易于维护

**代码优化**：
- **消除重复**：情况2和情况3的MongoDB查询共用 `queryMongoAndConvert()` 方法
- **利用累加性质**：工日数据具有累加性质，ES和MongoDB的结果可以直接合并累加
- **清晰分离**：三种查询策略完全独立，互不干扰
- **易于扩展**：新增数据源时只需添加新的查询方法，不影响现有逻辑

## Redis项目配置管理

### 配置存储结构

每个项目在Redis中存储一个Map，Key格式：`attendance:project_work_day_config:{projectId}`

配置内容：
```json
{
  "projectId": 875605502353920,
  "type": "cove_times",
  "thresholdDate": "2025-09-17",
  "version": 1
}
```

### 配置管理API

#### Service层方法
`ProjectWorkDayConfigService` 提供以下方法：

```java
// 获取项目配置（自动使用默认值）
ProjectWorkDayConfigDTO getProjectConfig(Long projectId);

// 保存项目配置
void saveProjectConfig(ProjectWorkDayConfigDTO config);

// 删除项目配置
void deleteProjectConfig(Long projectId);
```

#### Controller接口
`ProjectWorkDayConfigController` 提供REST接口：

| 接口 | 方法 | 路径 | 说明 |
|------|------|------|------|
| 获取配置 | GET | `/projectWorkDayConfig/get?projectId={projectId}` | 获取指定项目的工日配置 |
| 保存配置 | POST | `/projectWorkDayConfig/save` | 保存项目工日配置 |
| 删除配置 | DELETE | `/projectWorkDayConfig/delete?projectId={projectId}` | 删除项目工日配置 |
| 批量保存 | POST | `/projectWorkDayConfig/batchSave` | 批量保存多个项目的配置 |

### 使用示例

#### 1. 通过接口保存配置
```bash
POST /projectWorkDayConfig/save
Content-Type: application/json

{
  "projectId": 875605502353920,
  "type": "cove_times",
  "thresholdDate": "2025-09-17",
  "version": 1
}
```

#### 2. 获取配置
```bash
GET /projectWorkDayConfig/get?projectId=875605502353920
```

#### 3. 删除配置
```bash
DELETE /projectWorkDayConfig/delete?projectId=875605502353920
```

#### 4. 代码调用示例
```java
// Service层调用
ProjectWorkDayConfigDTO config = new ProjectWorkDayConfigDTO();
config.setProjectId(875605502353920L);
config.setType("cove_times");
config.setThresholdDate(LocalDate.of(2025, 9, 17));
config.setVersion(1L);
projectWorkDayConfigService.saveProjectConfig(config);

// 查询时自动获取配置
// 如果参数未传thresholdDate，系统会自动从Redis获取
```

## 数据增强扩展

如果需要对 MongoDB 查询的数据进行额外字段填充（如工时、出勤天数、请假天数等），可以：

1. 创建新的 `WorkDayDataEnhancer` 实现类
2. 在实现类中添加 `@Service` 注解和 `@Primary` 注解（替换默认实现）
3. 实现 `enhance()` 方法，编写自定义的数据填充逻辑

示例：
```java
@Slf4j
@Service
@Primary
public class CustomWorkDayDataEnhancer implements WorkDayDataEnhancer {
    
    @Override
    public List<LaborWorkerAttendanceCountVO> enhance(List<LaborWorkerAttendanceCountVO> voList, Long projectId) {
        // 自定义填充逻辑
        for (LaborWorkerAttendanceCountVO vo : voList) {
            // 填充工时、出勤天数等字段
        }
        return voList;
    }
}
```

## 注意事项

1. **时间阈值参数**：
   - 参数传入 `thresholdDate`：使用传入的阈值
   - 参数未传：自动从Redis获取项目配置
   - Redis也没有：使用原有逻辑（只查ES）

2. **Redis配置**：
   - 建议为每个项目配置Redis，设置合适的 `type` 和 `thresholdDate`
   - Redis配置永久保存，不会过期
   - 配置变更后直接更新即可，或手动删除配置

3. **MongoDB 集合**：确保 MongoDB 中存在 `work_day_calculation_results` 集合，且数据结构与实体类匹配

4. **类型字段**：查询 MongoDB 时会根据 `type` 字段过滤，从Redis配置中获取，默认为 `"cove_times"`

5. **数据合并**：当查询范围跨越阈值时，系统利用累加性质自动合并 ES 和 MongoDB 的数据

6. **性能考虑**：大批量数据查询时，建议合理设置阈值日期，避免单次查询数据量过大

## 后续扩展建议

1. **数据库持久化**：在数据库中创建项目工日配置表，作为Redis的数据源
2. **配置管理界面**：提供Web界面管理项目的type和thresholdDate配置
3. **数据增强器**：实现更丰富的数据增强器，填充 MongoDB 中缺失的字段（如工时、出勤天数等）
4. **监控告警**：添加查询性能监控，超时或失败时告警
5. **灰度开关**：考虑添加灰度开关，控制新功能的使用范围

## 核心优势总结

✅ **配置灵活**：Redis集中管理项目配置，易于动态调整  
✅ **代码清晰**：三种查询策略完全分离，消除重复代码  
✅ **性能优化**：提前判断，快速返回，避免不必要的查询  
✅ **易于扩展**：利用累加性质，新增数据源时只需添加查询方法  
✅ **向后兼容**：未配置阈值时自动降级到原有逻辑

