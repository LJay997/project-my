# Elasticsearch 聚合 (Aggregation) 指南与性能优化

> **版本**: Elasticsearch 7.x / 8.x  
> **适用场景**: 数据分析、报表统计、实时监控  
> **阅读建议**: 先理解聚合分类，再深入各类聚合用法，最后掌握优化策略

---

## 目录

1. [聚合基础概念](#一聚合基础概念)
2. [桶聚合 (Bucket Aggregations)](#二桶聚合-bucket-aggregations)
3. [指标聚合 (Metric Aggregations)](#三指标聚合-metric-aggregations)
4. [矩阵聚合 (Matrix Aggregations)](#四矩阵聚合-matrix-aggregations)
5. [管道聚合 (Pipeline Aggregations)](#五管道聚合-pipeline-aggregations)
6. [聚合性能优化清单](#六聚合性能优化清单)
7. [实战案例](#七实战案例)
8. [常见问题排查](#八常见问题排查)

---

## 一、聚合基础概念

### 1.1 什么是聚合

聚合是 Elasticsearch 提供的强大数据分析功能，用于对文档进行统计、分组、计算等操作，类似于 SQL 中的 `GROUP BY`、`COUNT`、`SUM`、`AVG` 等。

### 1.2 聚合分类

| 分类 | 说明 | 类比 SQL |
|------|------|----------|
| **桶聚合 (Bucket)** | 将文档分桶，每个桶包含匹配特定条件的文档 | `GROUP BY` |
| **指标聚合 (Metric)** | 对桶内文档计算指标（如求和、平均值等） | `COUNT`, `SUM`, `AVG` |
| **矩阵聚合 (Matrix)** | 对多个字段进行矩阵操作 | 协方差矩阵、相关性矩阵 |
| **管道聚合 (Pipeline)** | 对其他聚合的结果进行二次计算 | 子查询、窗口函数 |

### 1.3 聚合结构

```json
{
  "aggs": {
    "<聚合名称>": {
      "<聚合类型>": {
        "<聚合参数>"
      },
      "aggs": {
        "<子聚合>": {
          "<子聚合类型>": { ... }
        }
      }
    }
  }
}
```

### 🧠 记忆口诀

> **"桶分天下，指标计算，管道串联，矩阵关联"**

---

## 二、桶聚合 (Bucket Aggregations)

### 🧠 记忆口诀

> **"Terms 分组排，Range 区间来，Date_histogram 时序采，Filter 筛精彩"**

### 2.1 Terms Aggregation（词项桶聚合）

**用途**: 按字段值分组，统计每个值的文档数

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "terms": {
        "field": "<字段名>",
        "size": 10,
        "order": { "_count": "desc" },
        "min_doc_count": 1,
        "shard_size": 20
      }
    }
  }
}
```

**参数说明**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `field` | string | - | 用于分组的字段（keyword 类型） |
| `size` | int | 10 | 返回的桶数量 |
| `shard_size` | int | size * 1.5 + 10 | 每个分片返回的候选桶数 |
| `order` | object | `{_count: desc}` | 排序方式 |
| `min_doc_count` | int | 1 | 最小文档数过滤 |
| `shard_min_doc_count` | int | 0 | 分片级最小文档数 |
| `include` | string/regex | - | 包含匹配的词项 |
| `exclude` | string/regex | - | 排除匹配的词项 |

**使用场景**:
- 统计 Top N 类别
- 分组统计

**示例**:
```json
{
  "aggs": {
    "top_categories": {
      "terms": {
        "field": "category.keyword",
        "size": 5,
        "order": { "_count": "desc" },
        "min_doc_count": 10
      }
    }
  }
}
```

**性能优化**:
- ✅ 使用 `keyword` 类型字段
- ✅ 设置合理的 `size` 和 `shard_size`
- ✅ 使用 `min_doc_count` 过滤低频词

---

### 2.2 Range Aggregation（范围桶聚合）

**用途**: 按数值范围分组

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "range": {
        "field": "<字段名>",
        "ranges": [
          { "to": 100 },
          { "from": 100, "to": 500 },
          { "from": 500 }
        ],
        "keyed": true
      }
    }
  }
}
```

**参数说明**:

| 参数 | 说明 |
|------|------|
| `ranges` | 范围数组，每个范围包含 `from` 和/或 `to` |
| `keyed` | 是否为每个桶生成唯一 key |
| `format` | 日期格式（日期字段） |

**使用场景**:
- 价格区间统计
- 年龄分组

**示例**:
```json
{
  "aggs": {
    "price_ranges": {
      "range": {
        "field": "price",
        "ranges": [
          { "key": "低价", "to": 100 },
          { "key": "中价", "from": 100, "to": 500 },
          { "key": "高价", "from": 500 }
        ],
        "keyed": true
      }
    }
  }
}
```

---

### 2.3 Date Range Aggregation（日期范围桶聚合）

**用途**: 按日期范围分组

**语法**:
```json
{
  "aggs": {
    "date_ranges": {
      "date_range": {
        "field": "timestamp",
        "ranges": [
          { "to": "now-1w" },
          { "from": "now-1w", "to": "now-1d" },
          { "from": "now-1d" }
        ],
        "format": "yyyy-MM-dd",
        "time_zone": "+08:00"
      }
    }
  }
}
```

**使用场景**:
- 按时间范围统计数据

---

### 2.4 Date Histogram Aggregation（日期直方图聚合）

**用途**: 按时间间隔分组（最常用的时序聚合）

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "date_histogram": {
        "field": "<日期字段>",
        "interval": "1d",
        "format": "yyyy-MM-dd",
        "time_zone": "+08:00",
        "min_doc_count": 0,
        "extended_bounds": {
          "min": "2024-01-01",
          "max": "2024-01-31"
        }
      }
    }
  }
}
```

**参数说明**:

| 参数 | 说明 |
|------|------|
| `interval` | 时间间隔（1m/5m/1h/1d/1w/1M/1y） |
| `fixed_interval` | 固定时间间隔（如 3600000ms） |
| `calendar_interval` | 日历时间间隔（如 1d/1w/1M） |
| `format` | 日期格式 |
| `time_zone` | 时区 |
| `min_doc_count` | 最小文档数（0 表示显示空桶） |
| `extended_bounds` | 扩展边界（强制显示范围外的桶） |

**使用场景**:
- 时序数据统计
- 趋势分析
- 监控告警

**示例**:
```json
{
  "aggs": {
    "daily_sales": {
      "date_histogram": {
        "field": "order_date",
        "interval": "1d",
        "format": "yyyy-MM-dd",
        "time_zone": "+08:00",
        "min_doc_count": 0,
        "extended_bounds": {
          "min": "2024-01-01",
          "max": "2024-01-31"
        }
      },
      "aggs": {
        "total_amount": { "sum": { "field": "amount" } },
        "avg_amount": { "avg": { "field": "amount" } }
      }
    }
  }
}
```

**性能优化**:
- ✅ 使用 `calendar_interval` 替代 `interval`（更精确）
- ✅ 设置 `min_doc_count: 0` 仅在需要时
- ✅ 使用 `extended_bounds` 限制范围

---

### 2.5 Histogram Aggregation（数值直方图聚合）

**用途**: 按数值间隔分组

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "histogram": {
        "field": "<数值字段>",
        "interval": 100,
        "min_doc_count": 0,
        "extended_bounds": {
          "min": 0,
          "max": 1000
        }
      }
    }
  }
}
```

**使用场景**:
- 数值分布统计

---

### 2.6 Filter Aggregation（过滤器聚合）

**用途**: 按过滤条件分桶

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "filter": {
        "term": { "status": "active" }
      },
      "aggs": {
        "<子聚合>": { ... }
      }
    }
  }
}
```

**使用场景**:
- 过滤后再聚合

**示例**:
```json
{
  "aggs": {
    "active_users": {
      "filter": { "term": { "status": "active" } },
      "aggs": {
        "avg_age": { "avg": { "field": "age" } }
      }
    }
  }
}
```

---

### 2.7 Filters Aggregation（多过滤器聚合）

**用途**: 多个过滤器同时分桶

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "filters": {
        "filters": {
          "active": { "term": { "status": "active" } },
          "inactive": { "term": { "status": "inactive" } }
        }
      },
      "aggs": { ... }
    }
  }
}
```

**使用场景**:
- 对比不同条件的数据

---

### 2.8 Missing Aggregation（缺失值聚合）

**用途**: 统计字段缺失的文档

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "missing": {
        "field": "<字段名>"
      }
    }
  }
}
```

**使用场景**:
- 数据质量检查

---

### 2.9 Nested Aggregation（嵌套聚合）

**用途**: 对嵌套对象字段进行聚合

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "nested": {
        "path": "<嵌套路径>"
      },
      "aggs": {
        "<子聚合>": {
          "terms": { "field": "<嵌套字段>" }
        }
      }
    }
  }
}
```

**使用场景**:
- 查询嵌套对象的聚合统计

---

### 2.10 Composite Aggregation（复合聚合）

**用途**: 分页获取聚合结果（ES 7.x 新增）

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "composite": {
        "size": 1000,
        "sources": [
          { "category": { "terms": { "field": "category.keyword" } } },
          { "date": { "date_histogram": { "field": "date", "interval": "1d" } } }
        ],
        "after": { "category": "last_value", "date": "2024-01-15" }
      }
    }
  }
}
```

**参数说明**:

| 参数 | 说明 |
|------|------|
| `sources` | 复合聚合源（terms/date_histogram/histogram） |
| `size` | 每页返回的桶数 |
| `after` | 分页游标（上一页最后一个桶的值） |

**使用场景**:
- 大数据量聚合的分页处理

**性能优化**:
- ✅ 使用 `composite` 替代深度分页的 `terms` 聚合

---

## 三、指标聚合 (Metric Aggregations)

### 🧠 记忆口诀

> **"Count 计数，Sum 求和，Avg 平均，Max/Min 极值，Stats 全能，Top Hits 详情"**

### 3.1 Value Count Aggregation（值计数）

**用途**: 统计字段值非空的文档数

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "value_count": {
        "field": "<字段名>"
      }
    }
  }
}
```

**使用场景**:
- 统计某字段有值的文档数量

---

### 3.2 Sum Aggregation（求和）

**用途**: 计算字段值的总和

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "sum": {
        "field": "<数值字段>"
      }
    }
  }
}
```

**使用场景**:
- 统计销售额、总金额等

---

### 3.3 Avg Aggregation（平均值）

**用途**: 计算字段值的平均值

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "avg": {
        "field": "<数值字段>"
      }
    }
  }
}
```

**使用场景**:
- 平均价格、平均年龄等

---

### 3.4 Min/Max Aggregation（最小/最大值）

**用途**: 计算字段值的最小/最大值

**语法**:
```json
{
  "aggs": {
    "min_price": { "min": { "field": "price" } },
    "max_price": { "max": { "field": "price" } }
  }
}
```

---

### 3.5 Stats Aggregation（统计聚合）

**用途**: 一次性返回多个统计指标

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "stats": {
        "field": "<数值字段>"
      }
    }
  }
}
```

**返回结果**:
```json
{
  "stats": {
    "count": 100,
    "min": 10,
    "max": 1000,
    "avg": 500,
    "sum": 50000
  }
}
```

---

### 3.6 Extended Stats Aggregation（扩展统计聚合）

**用途**: 返回更详细的统计信息（包含方差、标准差等）

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "extended_stats": {
        "field": "<数值字段>",
        "sigma": 2
      }
    }
  }
}
```

**返回结果**:
```json
{
  "extended_stats": {
    "count": 100,
    "min": 10,
    "max": 1000,
    "avg": 500,
    "sum": 50000,
    "sum_of_squares": 25000000,
    "variance": 25000,
    "std_deviation": 158.11,
    "std_deviation_bounds": {
      "upper": 816.22,
      "lower": 183.78
    }
  }
}
```

---

### 3.7 Cardinality Aggregation（基数聚合）

**用途**: 计算字段的唯一值数量（近似值）

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "cardinality": {
        "field": "<字段名>",
        "precision_threshold": 40000,
        "rehash": true
      }
    }
  }
}
```

**参数说明**:

| 参数 | 说明 |
|------|------|
| `precision_threshold` | 精度阈值（0-40000），越高越精确但内存消耗越大 |
| `rehash` | 是否重新哈希（优化内存使用） |

**使用场景**:
- 统计独立用户数、独立 IP 数等

**性能优化**:
- ⚠️ **高基数字段内存消耗大**：合理设置 `precision_threshold`
- ✅ **使用近似值**：对于大数据量，接受一定误差

---

### 3.8 Top Hits Aggregation（顶部命中聚合）

**用途**: 返回每个桶内的前 N 条文档详情

**语法**:
```json
{
  "aggs": {
    "<桶聚合>": {
      "terms": { "field": "category.keyword" },
      "aggs": {
        "<聚合名称>": {
          "top_hits": {
            "size": 5,
            "_source": ["title", "price"],
            "sort": [{ "price": "desc" }]
          }
        }
      }
    }
  }
}
```

**参数说明**:

| 参数 | 说明 |
|------|------|
| `size` | 返回的文档数 |
| `_source` | 返回的字段 |
| `sort` | 排序方式 |

**使用场景**:
- 查看每个分组的详情数据

**性能优化**:
- ⚠️ **避免返回大量文档**：设置合理的 `size`
- ✅ **使用 `_source` 过滤字段**：只返回需要的字段

---

### 3.9 Percentiles Aggregation（百分位聚合）

**用途**: 计算数值分布的百分位数

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "percentiles": {
        "field": "<数值字段>",
        "percents": [10, 50, 90, 99],
        "tdigest": {
          "compression": 100
        }
      }
    }
  }
}
```

**参数说明**:

| 参数 | 说明 |
|------|------|
| `percents` | 要计算的百分位数数组 |
| `compression` | 压缩因子（100 默认，越高越精确） |

**使用场景**:
- 性能监控（P90、P99 响应时间）
- 数据分布分析

---

### 3.10 Geocentroid Aggregation（地理质心聚合）

**用途**: 计算地理位置的中心点

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "geocentroid": {
        "field": "<geo_point字段>"
      }
    }
  }
}
```

**使用场景**:
- 计算用户分布中心点

---

## 四、矩阵聚合 (Matrix Aggregations)

### 4.1 Matrix Stats Aggregation（矩阵统计聚合）

**用途**: 计算多个数值字段之间的相关性和协方差

**语法**:
```json
{
  "aggs": {
    "<聚合名称>": {
      "matrix_stats": {
        "fields": ["price", "sales", "rating"]
      }
    }
  }
}
```

**返回结果**:
```json
{
  "matrix_stats": {
    "fields": [
      {
        "name": "price",
        "count": 1000,
        "mean": 500,
        "variance": 25000,
        "skewness": 0.5,
        "kurtosis": 2.8,
        "covariance": {
          "price": 25000,
          "sales": 12500,
          "rating": 500
        },
        "correlation": {
          "price": 1,
          "sales": 0.8,
          "rating": 0.3
        }
      }
    ]
  }
}
```

**使用场景**:
- 多变量数据分析
- 相关性分析

---

## 五、管道聚合 (Pipeline Aggregations)

### 🧠 记忆口诀

> **"Derivative 求导，Cumulative Sum 累加，Moving Avg 滑动平均，Stats Bucket 桶统计"**

### 5.1 Derivative Aggregation（导数聚合）

**用途**: 计算相邻桶之间的变化率

**语法**:
```json
{
  "aggs": {
    "daily_sales": {
      "date_histogram": { "field": "date", "interval": "1d" },
      "aggs": {
        "sales": { "sum": { "field": "amount" } },
        "sales_derivative": {
          "derivative": { "buckets_path": "sales" }
        }
      }
    }
  }
}
```

**使用场景**:
- 计算增长率、变化率

---

### 5.2 Cumulative Sum Aggregation（累积求和聚合）

**用途**: 计算累计值

**语法**:
```json
{
  "aggs": {
    "daily_sales": {
      "date_histogram": { "field": "date", "interval": "1d" },
      "aggs": {
        "sales": { "sum": { "field": "amount" } },
        "cumulative_sales": {
          "cumulative_sum": { "buckets_path": "sales" }
        }
      }
    }
  }
}
```

**使用场景**:
- 计算累计销售额、累计用户数

---

### 5.3 Moving Average Aggregation（移动平均聚合）

**用途**: 计算滑动窗口内的平均值

**语法**:
```json
{
  "aggs": {
    "daily_sales": {
      "date_histogram": { "field": "date", "interval": "1d" },
      "aggs": {
        "sales": { "sum": { "field": "amount" } },
        "moving_avg": {
          "moving_avg": {
            "buckets_path": "sales",
            "window": 7,
            "model": "simple"
          }
        }
      }
    }
  }
}
```

**参数说明**:

| 参数 | 说明 |
|------|------|
| `window` | 窗口大小 |
| `model` | 计算模型（simple/linear/exponential/single_ewma/double_ewma/holt/holt_winters） |

**使用场景**:
- 平滑时序数据
- 趋势预测

---

### 5.4 Stats Bucket Aggregation（桶统计聚合）

**用途**: 对桶聚合的结果进行统计

**语法**:
```json
{
  "aggs": {
    "daily_sales": {
      "date_histogram": { "field": "date", "interval": "1d" },
      "aggs": {
        "sales": { "sum": { "field": "amount" } }
      }
    },
    "sales_stats": {
      "stats_bucket": {
        "buckets_path": "daily_sales>sales"
      }
    }
  }
}
```

**使用场景**:
- 对时序数据的统计指标再统计

---

### 5.5 Avg Bucket Aggregation（桶平均聚合）

**用途**: 计算桶聚合结果的平均值

**语法**:
```json
{
  "aggs": {
    "daily_sales": {
      "date_histogram": { "field": "date", "interval": "1d" },
      "aggs": {
        "sales": { "sum": { "field": "amount" } }
      }
    },
    "avg_daily_sales": {
      "avg_bucket": {
        "buckets_path": "daily_sales>sales"
      }
    }
  }
}
```

---

### 5.6 Max Bucket / Min Bucket Aggregation（桶最大/最小聚合）

**用途**: 找出桶聚合结果中的最大/最小值

**语法**:
```json
{
  "aggs": {
    "daily_sales": {
      "date_histogram": { "field": "date", "interval": "1d" },
      "aggs": {
        "sales": { "sum": { "field": "amount" } }
      }
    },
    "max_daily_sales": {
      "max_bucket": {
        "buckets_path": "daily_sales>sales"
      }
    }
  }
}
```

---

### 5.7 Percentiles Bucket Aggregation（桶百分位聚合）

**用途**: 计算桶聚合结果的百分位数

**语法**:
```json
{
  "aggs": {
    "daily_sales": {
      "date_histogram": { "field": "date", "interval": "1d" },
      "aggs": {
        "sales": { "sum": { "field": "amount" } }
      }
    },
    "sales_percentiles": {
      "percentiles_bucket": {
        "buckets_path": "daily_sales>sales",
        "percents": [90, 95, 99]
      }
    }
  }
}
```

---

## 六、聚合性能优化清单

### 6.1 查询语句优化

#### ✅ 使用 Filter 限制数据范围

```json
{
  "query": {
    "bool": {
      "filter": [
        { "range": { "@timestamp": { "gte": "now-7d" } } },
        { "term": { "status": "active" } }
      ]
    }
  },
  "aggs": { ... }
}
```

**原理**: Filter 不计算评分，结果可缓存，减少聚合处理的数据量。

---

#### ✅ 避免不必要的聚合

```json
// ❌ 不好：返回了不需要的文档
{
  "query": { "match": { "title": "elasticsearch" } },
  "aggs": { "categories": { "terms": { "field": "category.keyword" } } }
}

// ✅ 好：只聚合，不返回文档
{
  "size": 0,  // 关键：设置为0
  "query": { "match": { "title": "elasticsearch" } },
  "aggs": { "categories": { "terms": { "field": "category.keyword" } } }
}
```

**原理**: 设置 `size: 0` 避免返回命中的文档，节省网络传输和内存。

---

#### ✅ 限制 Terms 聚合的大小

```json
{
  "aggs": {
    "top_categories": {
      "terms": {
        "field": "category.keyword",
        "size": 10,           // 返回前10个
        "shard_size": 20,     // 每个分片返回20个候选
        "min_doc_count": 10   // 过滤低频词
      }
    }
  }
}
```

**原理**: 减少内存占用和网络传输。

---

#### ✅ 使用 Composite 聚合进行分页

```json
{
  "aggs": {
    "paged_agg": {
      "composite": {
        "size": 1000,
        "sources": [
          { "category": { "terms": { "field": "category.keyword" } } }
        ],
        "after": { "category": "last_value" }
      }
    }
  }
}
```

**原理**: 避免深度分页带来的性能问题。

---

### 6.2 索引设计建议

#### ✅ 使用 Keyword 类型字段进行聚合

```json
// mapping 配置
{
  "mappings": {
    "properties": {
      "category": {
        "type": "text",
        "fields": {
          "keyword": {
            "type": "keyword",
            "ignore_above": 256
          }
        }
      }
    }
  }
}

// 查询时使用 keyword 字段
{
  "aggs": {
    "categories": {
      "terms": { "field": "category.keyword" }
    }
  }
}
```

**原理**: Text 字段经过分词，不适合用于精确分组。

---

#### ✅ 禁用字段的 norms 和 doc_values（如果不需要）

```json
{
  "mappings": {
    "properties": {
      "status": {
        "type": "keyword",
        "norms": false,           // 禁用 norms（不需要评分）
        "doc_values": true        // 启用 doc_values（用于聚合和排序）
      },
      "description": {
        "type": "text",
        "doc_values": false       // 禁用 doc_values（不需要聚合）
      }
    }
  }
}
```

**原理**: 
- `norms` 影响评分，聚合不需要评分时可禁用
- `doc_values` 启用后支持高效聚合，但会增加索引大小

---

#### ✅ 预聚合设计

对于高频查询的聚合结果，可以提前计算并存储：

```json
// 方案1：使用 Transform 功能
PUT _transform/sales_summary
{
  "source": { "index": "orders" },
  "dest": { "index": "sales_summary" },
  "pivot": {
    "group_by": {
      "date": { "date_histogram": { "field": "order_date", "interval": "1d" } },
      "category": { "terms": { "field": "category.keyword" } }
    },
    "aggregations": {
      "total_sales": { "sum": { "field": "amount" } }
    }
  },
  "frequency": "1h"
}

// 方案2：在应用层预计算，写入独立索引
```

**原理**: 将复杂聚合转化为简单查询，显著提升性能。

---

### 6.3 硬件资源配置

#### ✅ 内存配置

| 参数 | 建议值 | 说明 |
|------|--------|------|
| `ES_JAVA_OPTS` | `-Xms16g -Xmx16g` | 堆内存，建议为物理内存的 50% |
| `indices.fielddata.cache.size` | `20%` | Fielddata 缓存大小 |
| `indices.queries.cache.size` | `10%` | 查询缓存大小 |

**原理**: 聚合操作需要大量内存，尤其是高基数聚合。

---

#### ✅ CPU 配置

| 场景 | CPU 核心数建议 |
|------|---------------|
| 轻量聚合 | 8-16 核 |
| 中等聚合 | 16-32 核 |
| 重度聚合 | 32-64 核 |

**原理**: 聚合操作是 CPU 密集型任务。

---

#### ✅ 磁盘配置

| 场景 | 磁盘类型建议 |
|------|-------------|
| 日志聚合 | SSD（高 IOPS） |
| 大数据分析 | SSD 或 NVMe |
| 归档数据 | HDD（大容量） |

**原理**: 聚合需要扫描大量数据，磁盘 IO 是关键瓶颈。

---

### 6.4 缓存策略

#### ✅ 启用查询缓存

```json
// 索引级别配置
PUT /my_index/_settings
{
  "index": {
    "request_cache": { "enabled": true }
  }
}

// 查询级别配置
{
  "size": 0,
  "request_cache": true,
  "query": { "bool": { "filter": [...] } },
  "aggs": { ... }
}
```

**原理**: 查询缓存会缓存聚合结果，相同查询可直接返回缓存。

---

#### ✅ 控制 Fielddata 缓存

```json
// 禁用特定字段的 fielddata
PUT /my_index/_mapping
{
  "properties": {
    "content": {
      "type": "text",
      "fielddata": false
    }
  }
}

// 设置 fielddata 缓存大小
PUT /_cluster/settings
{
  "persistent": {
    "indices.fielddata.cache.size": "20%"
  }
}
```

**原理**: Fielddata 是用于聚合的内存数据结构，需合理控制。

---

### 6.5 分片设置

#### ✅ 合理设置分片数

| 数据量 | 主分片数建议 | 副本数建议 |
|--------|-------------|-----------|
| < 10GB | 1-2 | 1 |
| 10GB-100GB | 2-5 | 1-2 |
| 100GB-1TB | 5-10 | 1-2 |
| > 1TB | 10-30 | 1-2 |

**原理**: 
- 分片过多：增加协调开销
- 分片过少：无法充分利用集群资源

---

#### ✅ 使用路由减少分片扫描

```json
{
  "query": {
    "bool": {
      "filter": [
        { "term": { "user_id": "123" } }
      ]
    }
  },
  "aggs": { ... },
  "routing": "123"  // 关键：指定路由
}
```

**原理**: 路由可以将查询限制在特定分片，减少扫描范围。

---

### 6.6 聚合结果大小限制

#### ✅ 设置最大桶数限制

```json
// 集群级别配置
PUT /_cluster/settings
{
  "persistent": {
    "search.max_buckets": 10000
  }
}

// 查询级别配置
{
  "aggs": {
    "categories": {
      "terms": {
        "field": "category.keyword",
        "size": 1000
      }
    }
  },
  "max_buckets": 5000
}
```

**原理**: 防止单个聚合产生过多桶导致内存溢出。

---

#### ✅ 使用 `shard_size` 控制分片级返回量

```json
{
  "aggs": {
    "top_terms": {
      "terms": {
        "field": "category.keyword",
        "size": 10,
        "shard_size": 20  // 每个分片返回20个候选
      }
    }
  }
}
```

**原理**: 减少跨分片的数据传输量。

---

## 七、实战案例

### 7.1 电商销售统计

**需求**: 统计各分类的销售额、订单数、平均客单价，按销售额降序排列

```json
{
  "size": 0,
  "query": {
    "bool": {
      "filter": [
        { "range": { "order_date": { "gte": "2024-01-01" } } },
        { "term": { "status": "completed" } }
      ]
    }
  },
  "aggs": {
    "by_category": {
      "terms": {
        "field": "category.keyword",
        "size": 10,
        "order": { "total_sales": "desc" }
      },
      "aggs": {
        "total_sales": { "sum": { "field": "amount" } },
        "order_count": { "value_count": { "field": "order_id" } },
        "avg_amount": { "avg": { "field": "amount" } },
        "top_products": {
          "top_hits": {
            "size": 3,
            "_source": ["product_name", "price"]
          }
        }
      }
    }
  }
}
```

---

### 7.2 实时监控仪表盘

**需求**: 统计最近 24 小时的请求量、平均响应时间、P99 响应时间

```json
{
  "size": 0,
  "query": {
    "bool": {
      "filter": [
        { "range": { "@timestamp": { "gte": "now-24h" } } }
      ]
    }
  },
  "aggs": {
    "requests_over_time": {
      "date_histogram": {
        "field": "@timestamp",
        "interval": "5m",
        "min_doc_count": 0,
        "extended_bounds": {
          "min": "now-24h",
          "max": "now"
        }
      },
      "aggs": {
        "request_count": { "value_count": { "field": "request_id" } },
        "avg_response_time": { "avg": { "field": "response_time" } },
        "p99_response_time": { "percentiles": { "field": "response_time", "percents": [99] } }
      }
    },
    "overall_stats": {
      "stats": { "field": "response_time" }
    },
    "p99_overall": {
      "percentiles": { "field": "response_time", "percents": [99] }
    }
  }
}
```

---

### 7.3 用户行为分析

**需求**: 统计各年龄段用户的活跃度、平均消费金额

```json
{
  "size": 0,
  "aggs": {
    "age_groups": {
      "range": {
        "field": "age",
        "ranges": [
          { "key": "18-25", "from": 18, "to": 26 },
          { "key": "26-35", "from": 26, "to": 36 },
          { "key": "36-45", "from": 36, "to": 46 },
          { "key": "45+", "from": 46 }
        ],
        "keyed": true
      },
      "aggs": {
        "user_count": { "cardinality": { "field": "user_id", "precision_threshold": 40000 } },
        "avg_spending": { "avg": { "field": "total_spending" } },
        "active_users": {
          "filter": { "term": { "active": true } },
          "aggs": {
            "count": { "cardinality": { "field": "user_id" } }
          }
        }
      }
    }
  }
}
```

---

## 八、常见问题排查

### 8.1 聚合查询慢

**排查步骤**:

1. **检查查询范围**：是否过滤了足够的数据？
2. **检查分片数**：分片数是否合理？
3. **检查内存使用**：是否有内存压力？
4. **检查字段类型**：是否使用了 text 字段进行聚合？
5. **检查聚合复杂度**：是否有过多的嵌套聚合？

**解决方案**:
- 添加 `filter` 限制数据范围
- 使用 `size: 0` 避免返回文档
- 使用 keyword 类型字段
- 考虑预聚合

---

### 8.2 内存溢出

**常见原因**:

1. **高基数字段聚合**：如 `terms` 聚合字段有大量唯一值
2. **过大的 `size` 参数**：返回了过多的桶
3. **嵌套聚合过深**：多层嵌套导致内存爆炸

**解决方案**:
- 设置合理的 `size` 和 `shard_size`
- 使用 `min_doc_count` 过滤低频词
- 使用 `composite` 聚合分页
- 增加堆内存

---

### 8.3 聚合结果不准确

**常见原因**:

1. **分片级聚合误差**：`terms` 聚合在分片级别取 top N，可能遗漏全局 top N
2. **基数聚合近似值**：`cardinality` 聚合返回的是近似值
3. **时区问题**：日期聚合时区设置不正确

**解决方案**:
- 增大 `shard_size` 参数
- 使用 `shard_min_doc_count` 控制分片级过滤
- 理解并接受近似值的误差范围
- 正确设置时区

---

### 8.4 聚合结果排序错误

**常见原因**:

1. **排序字段类型错误**：对 text 字段排序
2. **排序方向错误**：`asc`/`desc` 设置错误
3. **多级排序冲突**：多个排序条件冲突

**解决方案**:
- 使用 keyword 或数值字段排序
- 检查排序方向设置
- 明确排序优先级

---

## 九、聚合最佳实践

### 9.1 设计原则

1. **先过滤后聚合**：使用 filter 减少聚合数据量
2. **避免返回文档**：设置 `size: 0`
3. **使用合适的字段类型**：keyword 字段用于精确聚合
4. **限制聚合结果**：设置合理的 `size`
5. **考虑预聚合**：高频查询使用 Transform 或预计算

### 9.2 性能检查清单

- [ ] 查询是否使用了 filter 限制范围？
- [ ] 是否设置了 `size: 0`？
- [ ] 聚合字段是否为 keyword 类型？
- [ ] `size` 和 `shard_size` 是否合理？
- [ ] 是否启用了查询缓存？
- [ ] 分片数是否合理？
- [ ] 是否需要预聚合？

### 9.3 监控指标

| 指标 | 说明 | 关注值 |
|------|------|--------|
| `search.query_total` | 查询总数 | 异常增长 |
| `search.query_time_in_millis` | 查询耗时 | 超过预期 |
| `search.fetch_time_in_millis` | 获取结果耗时 | 超过预期 |
| `indices.fielddata.memory_size` | fielddata 内存使用 | 接近上限 |
| `indices.request_cache.hit_count` | 查询缓存命中率 | 低于 50% |

---

## 十、参考资源

| 资源 | 链接 |
|------|------|
| Elasticsearch 官方文档 | https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations.html |
| Aggregation 参考 | https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations.html |
| Transform 功能 | https://www.elastic.co/guide/en/elasticsearch/reference/current/transforms.html |
| 性能优化指南 | https://www.elastic.co/guide/en/elasticsearch/reference/current/tune-for-search-speed.html |

---

## 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 1.0 | 2026-07-16 | 初始版本 |

---

> **提示**: 本手册基于 Elasticsearch 7.x/8.x 版本编写，部分语法可能与旧版本不兼容。建议结合官方文档使用。