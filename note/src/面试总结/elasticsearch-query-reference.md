# Elasticsearch Query DSL 参考手册

> **版本**: Elasticsearch 7.x / 8.x  
> **适用场景**: 全文搜索、数据分析、日志检索  
> **阅读建议**: 初学者从基础查询开始，进阶用户直接查阅分类目录

---

## 目录

1. [基础查询 (Basic Queries)](#一基础查询-basic-queries)
2. [复合查询 (Compound Queries)](#二复合查询-compound-queries)
3. [全文查询 (Full-Text Queries)](#三全文查询-full-text-queries)
4. [词项级别查询 (Term-Level Queries)](#四词项级别查询-term-level-queries)
5. [特殊查询 (Specialized Queries)](#五特殊查询-specialized-queries)
6. [查询模式速查表](#六查询模式速查表)
7. [性能优化指南](#七性能优化指南)

---

## 一、基础查询 (Basic Queries)

### 🧠 记忆口诀

> **"Match 模糊搜，Term 精准找，Bool 组合妙，Range 范围跑"**

### 1.1 Match Query（匹配查询）

**用途**: 全文搜索的基础查询，对搜索词进行分词后匹配

**语法**:
```json
{
  "query": {
    "match": {
      "field_name": {
        "query": "search text",
        "operator": "or",
        "minimum_should_match": "75%"
      }
    }
  }
}
```

**参数说明**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `query` | string | - | 搜索文本 |
| `operator` | string | `or` | 分词后的匹配逻辑（`or`/`and`） |
| `minimum_should_match` | string/int | - | 最少匹配的分词数量 |
| `fuzziness` | string/int | `AUTO` | 模糊匹配容忍度 |

**使用场景**:
- 搜索文章标题、描述等文本字段
- 用户输入的自然语言搜索

**示例**:
```json
{
  "query": {
    "match": {
      "title": {
        "query": "elastic search tutorial",
        "operator": "and"
      }
    }
  }
}
```

---

### 1.2 Term Query（词项查询）

**用途**: 精确匹配单个词项，不进行分词

**语法**:
```json
{
  "query": {
    "term": {
      "field_name": {
        "value": "exact_value",
        "boost": 1.0
      }
    }
  }
}
```

**参数说明**:

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| `value` | string/number | - | 精确匹配的值 |
| `boost` | float | 1.0 | 权重系数 |

**使用场景**:
- 匹配关键字段（如状态、标签）
- 精确值过滤

**示例**:
```json
{
  "query": {
    "term": {
      "status": {
        "value": "active"
      }
    }
  }
}
```

**注意事项**:
- ⚠️ **不适用于文本字段**：文本字段经过分词后，term 查询可能无法匹配
- ✅ **适用于 keyword 类型字段**：如 `status.keyword`

---

### 1.3 Terms Query（多词项查询）

**用途**: 匹配多个精确值中的任意一个

**语法**:
```json
{
  "query": {
    "terms": {
      "field_name": ["value1", "value2", "value3"],
      "boost": 1.0
    }
  }
}
```

**使用场景**:
- 批量匹配多个标签、ID 等

**示例**:
```json
{
  "query": {
    "terms": {
      "tags": ["java", "elasticsearch", "spring"]
    }
  }
}
```

---

### 1.4 Range Query（范围查询）

**用途**: 匹配指定范围内的值

**语法**:
```json
{
  "query": {
    "range": {
      "field_name": {
        "gte": "2024-01-01",
        "lte": "2024-12-31",
        "format": "yyyy-MM-dd",
        "time_zone": "+08:00"
      }
    }
  }
}
```

**参数说明**:

| 参数 | 说明 |
|------|------|
| `gt` | 大于 (greater than) |
| `gte` | 大于等于 (greater than or equal) |
| `lt` | 小于 (less than) |
| `lte` | 小于等于 (less than or equal) |
| `format` | 日期格式（仅日期字段） |
| `time_zone` | 时区（仅日期字段） |

**使用场景**:
- 时间范围过滤
- 数值范围过滤

**示例**:
```json
{
  "query": {
    "range": {
      "price": {
        "gte": 100,
        "lte": 500
      }
    }
  }
}
```

---

### 1.5 Exists Query（存在查询）

**用途**: 匹配字段存在且不为 null 的文档

**语法**:
```json
{
  "query": {
    "exists": {
      "field": "field_name"
    }
  }
}
```

**使用场景**:
- 查询有特定字段的文档
- 排除缺失字段的文档

**示例**:
```json
{
  "query": {
    "exists": {
      "field": "email"
    }
  }
}
```

---

### 1.6 Prefix Query（前缀查询）

**用途**: 匹配字段以指定前缀开头的文档

**语法**:
```json
{
  "query": {
    "prefix": {
      "field_name": {
        "value": "prefix_",
        "boost": 1.0
      }
    }
  }
}
```

**使用场景**:
- 搜索以特定前缀开头的单词
- 自动补全功能

**示例**:
```json
{
  "query": {
    "prefix": {
      "title": "elastic"
    }
  }
}
```

---

## 二、复合查询 (Compound Queries)

### 🧠 记忆口诀

> **"Bool 当家，must/should/filter/must_not 分四家"**

### 2.1 Bool Query（布尔查询）

**用途**: 组合多个查询条件，实现复杂的逻辑运算

**语法**:
```json
{
  "query": {
    "bool": {
      "must": [],
      "should": [],
      "must_not": [],
      "filter": [],
      "minimum_should_match": 1,
      "boost": 1.0
    }
  }
}
```

**参数说明**:

| 参数 | 逻辑 | 评分 | 说明 |
|------|------|------|------|
| `must` | AND | 参与评分 | 必须匹配 |
| `should` | OR | 参与评分 | 至少匹配一个（受 `minimum_should_match` 控制） |
| `must_not` | NOT | 不参与评分 | 必须不匹配 |
| `filter` | AND | 不参与评分 | 必须匹配，用于过滤 |

**使用场景**:
- 复杂的多条件搜索
- 结合全文搜索和精确过滤

**示例**:
```json
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "elasticsearch" } }
      ],
      "should": [
        { "match": { "tags": "tutorial" } },
        { "match": { "tags": "guide" } }
      ],
      "filter": [
        { "term": { "status": "published" } },
        { "range": { "publish_date": { "gte": "2024-01-01" } } }
      ],
      "must_not": [
        { "term": { "category": "deprecated" } }
      ],
      "minimum_should_match": 1
    }
  }
}
```

**性能优化**:
- ✅ **使用 filter 替代 must**：filter 不计算评分，缓存效果更好
- ✅ **将高频过滤条件放在 filter 中**：如状态、日期范围

---

### 2.2 Boosting Query（权重查询）

**用途**: 通过权重调整影响文档评分

**语法**:
```json
{
  "query": {
    "boosting": {
      "positive": { "match": { "title": "elasticsearch" } },
      "negative": { "term": { "category": "advertisement" } },
      "negative_boost": 0.2
    }
  }
}
```

**参数说明**:

| 参数 | 说明 |
|------|------|
| `positive` | 正向查询，匹配的文档得分较高 |
| `negative` | 负向查询，匹配的文档得分降低 |
| `negative_boost` | 负向查询的权重系数（0-1） |

**使用场景**:
- 降低广告文档的优先级
- 提升特定类型文档的排名

---

### 2.3 Constant Score Query（常量评分查询）

**用途**: 为匹配的文档分配固定分数，不计算相关性

**语法**:
```json
{
  "query": {
    "constant_score": {
      "filter": { "term": { "status": "active" } },
      "boost": 1.0
    }
  }
}
```

**使用场景**:
- 过滤查询但需要返回分数
- 所有匹配文档同等重要的场景

---

## 三、全文查询 (Full-Text Queries)

### 🧠 记忆口诀

> **"Match 分词搜，Multi-match 多字段，Query-string 灵活写，Simple-query 简写法"**

### 3.1 Multi-Match Query（多字段匹配）

**用途**: 在多个字段上执行相同的 match 查询

**语法**:
```json
{
  "query": {
    "multi_match": {
      "query": "search text",
      "fields": ["title", "description", "content"],
      "type": "best_fields"
    }
  }
}
```

**类型说明**:

| 类型 | 说明 |
|------|------|
| `best_fields` | 取最佳匹配字段的分数（默认） |
| `most_fields` | 合并所有匹配字段的分数 |
| `cross_fields` | 将多个字段视为一个大字段 |
| `phrase` | 短语匹配 |
| `phrase_prefix` | 短语前缀匹配 |

**使用场景**:
- 在标题、描述、内容等多个字段中搜索

**示例**:
```json
{
  "query": {
    "multi_match": {
      "query": "elastic search tutorial",
      "fields": ["title^3", "description^2", "content"],
      "type": "best_fields"
    }
  }
}
```

**字段权重**:
- 使用 `^` 符号设置字段权重：`title^3` 表示 title 字段权重为 3

---

### 3.2 Query String Query（查询字符串）

**用途**: 使用 Lucene 查询语法进行灵活搜索

**语法**:
```json
{
  "query": {
    "query_string": {
      "query": "(elasticsearch OR lucene) AND tutorial",
      "default_field": "content",
      "analyzer": "standard"
    }
  }
}
```

**支持的操作符**:

| 操作符 | 说明 | 示例 |
|--------|------|------|
| `AND` | 必须同时匹配 | `java AND spring` |
| `OR` | 匹配任一 | `java OR python` |
| `NOT` | 排除 | `java NOT script` |
| `+` | 必须匹配 | `+java spring` |
| `-` | 必须不匹配 | `java -script` |
| `*` | 通配符 | `elast*` |
| `?` | 单字符通配符 | `elast?c` |
| `"` | 短语匹配 | `"elastic search"` |
| `()` | 分组 | `(java OR python) AND spring` |
| `^` | 权重 | `title^3` |

**使用场景**:
- 高级用户的复杂搜索
- 需要精确控制查询逻辑的场景

**注意事项**:
- ⚠️ **避免在用户输入中直接使用**：可能导致注入攻击
- ✅ **适合管理员界面或高级搜索功能**

---

### 3.3 Simple Query String Query（简单查询字符串）

**用途**: 更安全的查询字符串语法，忽略无效语法

**语法**:
```json
{
  "query": {
    "simple_query_string": {
      "query": "elasticsearch tutorial -deprecated",
      "fields": ["title", "content"]
    }
  }
}
```

**支持的操作符**:

| 操作符 | 说明 |
|--------|------|
| `+` | AND |
| `\|` | OR |
| `-` | NOT |
| `"` | 短语 |
| `*` | 前缀 |

**使用场景**:
- 用户输入的搜索框
- 需要容错处理的场景

---

### 3.4 Match Phrase Query（短语匹配）

**用途**: 精确匹配连续的短语

**语法**:
```json
{
  "query": {
    "match_phrase": {
      "title": {
        "query": "elastic search",
        "slop": 0
      }
    }
  }
}
```

**参数说明**:

| 参数 | 说明 |
|------|------|
| `slop` | 允许的词间距（0=严格连续） |

**使用场景**:
- 精确短语搜索
- 搜索特定的术语或名称

**示例**:
```json
{
  "query": {
    "match_phrase": {
      "content": {
        "query": "machine learning algorithm",
        "slop": 1
      }
    }
  }
}
```

---

### 3.5 Match Phrase Prefix Query（短语前缀匹配）

**用途**: 匹配以指定前缀结尾的短语

**语法**:
```json
{
  "query": {
    "match_phrase_prefix": {
      "title": {
        "query": "elastic sea",
        "max_expansions": 50
      }
    }
  }
}
```

**使用场景**:
- 自动补全功能
- 搜索建议

---

## 四、词项级别查询 (Term-Level Queries)

### 🧠 记忆口诀

> **"Term 精确查，Terms 多值拉，Wildcard 通配卡，Regexp 正则抓"**

### 4.1 Wildcard Query（通配符查询）

**用途**: 使用通配符匹配字段值

**语法**:
```json
{
  "query": {
    "wildcard": {
      "field_name": {
        "value": "pattern*",
        "boost": 1.0
      }
    }
  }
}
```

**通配符说明**:

| 符号 | 说明 | 示例 |
|------|------|------|
| `*` | 匹配零个或多个字符 | `elast*` |
| `?` | 匹配单个字符 | `elast?c` |

**使用场景**:
- 模糊匹配以特定模式开头/结尾的值
- 搜索产品型号等

**示例**:
```json
{
  "query": {
    "wildcard": {
      "product_code": "PROD-2024*"
    }
  }
}
```

**性能注意**:
- ⚠️ **避免以 `*` 开头**：会导致全索引扫描
- ✅ **前缀匹配优先使用 prefix 查询**

---

### 4.2 Regexp Query（正则表达式查询）

**用途**: 使用正则表达式匹配字段值

**语法**:
```json
{
  "query": {
    "regexp": {
      "field_name": {
        "value": "pattern",
        "flags": "ALL",
        "max_determinized_states": 10000
      }
    }
  }
}
```

**标志说明**:

| 标志 | 说明 |
|------|------|
| `ALL` | 启用所有可选标志 |
| `ANYSTRING` | 启用 `.` 匹配换行符 |
| `COMPLEMENT` | 启用 `~` 作为否定 |
| `EMPTY` | 启用空字符串匹配 |
| `INTERSECTION` | 启用 `&` 作为交集 |
| `INTERVAL` | 启用 `<> ` 作为区间 |
| `NONE` | 禁用所有可选标志 |

**使用场景**:
- 复杂模式匹配
- 验证特定格式的数据

**示例**:
```json
{
  "query": {
    "regexp": {
      "email": "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    }
  }
}
```

**性能注意**:
- ⚠️ **复杂正则会严重影响性能**：使用简单模式
- ✅ **限制 `max_determinized_states`**：防止过度计算

---

### 4.3 Fuzzy Query（模糊查询）

**用途**: 允许一定程度的拼写错误匹配

**语法**:
```json
{
  "query": {
    "fuzzy": {
      "field_name": {
        "value": "search term",
        "fuzziness": "AUTO",
        "prefix_length": 2
      }
    }
  }
}
```

**参数说明**:

| 参数 | 说明 |
|------|------|
| `fuzziness` | 模糊度（`AUTO`/`0`/`1`/`2`） |
| `prefix_length` | 前缀不模糊的字符数 |
| `max_expansions` | 最大扩展词数 |

**使用场景**:
- 拼写纠错
- 用户输入容错

**示例**:
```json
{
  "query": {
    "fuzzy": {
      "title": {
        "value": "elastc seach",
        "fuzziness": 2,
        "prefix_length": 3
      }
    }
  }
}
```

---

### 4.4 Type Query（类型查询）

**用途**: 按文档类型过滤（ES 7.x 已弃用）

**语法**:
```json
{
  "query": {
    "type": {
      "value": "_doc"
    }
  }
}
```

---

### 4.5 IDs Query（ID 查询）

**用途**: 按文档 ID 批量查询

**语法**:
```json
{
  "query": {
    "ids": {
      "values": ["1", "2", "3"]
    }
  }
}
```

**使用场景**:
- 根据已知 ID 列表查询文档

---

## 五、特殊查询 (Specialized Queries)

### 🧠 记忆口诀

> **"Function_score 自定义，Geo 查询位置精，Nested 嵌套层层进，Parent-child 父子亲"**

### 5.1 Function Score Query（函数评分查询）

**用途**: 使用自定义函数调整文档评分

**语法**:
```json
{
  "query": {
    "function_score": {
      "query": { "match": { "title": "elasticsearch" } },
      "boost": "5",
      "functions": [
        {
          "filter": { "term": { "status": "featured" } },
          "weight": 2
        },
        {
          "field_value_factor": {
            "field": "view_count",
            "factor": 0.1,
            "modifier": "log1p"
          }
        }
      ],
      "score_mode": "sum",
      "boost_mode": "multiply"
    }
  }
}
```

**评分函数**:

| 函数 | 说明 |
|------|------|
| `weight` | 固定权重 |
| `field_value_factor` | 使用字段值计算分数 |
| `random_score` | 随机评分 |
| `script_score` | 自定义脚本计算分数 |
| `decay_functions` | 衰减函数（gauss/linear/exponential） |

**Modifier 说明**:

| Modifier | 公式 | 说明 |
|----------|------|------|
| `none` | field_value | 原值 |
| `log` | log(field_value) | 对数 |
| `log1p` | log(1 + field_value) | 防止零值 |
| `log2p` | log(2 + field_value) | 更平滑 |
| `square` | field_value^2 | 平方 |
| `sqrt` | sqrt(field_value) | 平方根 |
| `reciprocal` | 1/field_value | 倒数 |

**使用场景**:
- 自定义排序逻辑
- 结合业务指标调整排名

---

### 5.2 Geo Queries（地理查询）

#### 5.2.1 Geo Distance Query（距离查询）

**用途**: 查询指定地理位置范围内的文档

**语法**:
```json
{
  "query": {
    "geo_distance": {
      "location": {
        "lat": 31.2304,
        "lon": 121.4737
      },
      "distance": "5km",
      "distance_type": "arc",
      "validation_method": "STRICT"
    }
  }
}
```

**参数说明**:

| 参数 | 说明 |
|------|------|
| `distance` | 距离（支持 km/m/mi/yd） |
| `distance_type` | 距离计算方式（arc/plane） |
| `validation_method` | 坐标验证（STRICT/IGNORE_MALFORMED/COERCE） |

**使用场景**:
- 附近搜索
- LBS 应用

#### 5.2.2 Geo Bounding Box Query（边界框查询）

**语法**:
```json
{
  "query": {
    "geo_bounding_box": {
      "location": {
        "top_left": { "lat": 31.3, "lon": 121.4 },
        "bottom_right": { "lat": 31.2, "lon": 121.5 }
      }
    }
  }
}
```

#### 5.2.3 Geo Polygon Query（多边形查询）

**语法**:
```json
{
  "query": {
    "geo_polygon": {
      "location": {
        "points": [
          { "lat": 31.2, "lon": 121.4 },
          { "lat": 31.3, "lon": 121.4 },
          { "lat": 31.25, "lon": 121.5 }
        ]
      }
    }
  }
}
```

---

### 5.3 Nested Query（嵌套查询）

**用途**: 查询嵌套对象字段

**语法**:
```json
{
  "query": {
    "nested": {
      "path": "comments",
      "query": {
        "bool": {
          "must": [
            { "match": { "comments.author": "John" } },
            { "range": { "comments.date": { "gte": "2024-01-01" } } }
          ]
        }
      },
      "score_mode": "avg"
    }
  }
}
```

**参数说明**:

| 参数 | 说明 |
|------|------|
| `path` | 嵌套对象的路径 |
| `score_mode` | 评分模式（avg/max/min/sum） |

**使用场景**:
- 查询包含嵌套对象的文档
- 关联多个嵌套字段的条件

---

### 5.4 Parent-Child Query（父子查询）

#### 5.4.1 Has Child Query（子文档查询）

**用途**: 查询有匹配子文档的父文档

**语法**:
```json
{
  "query": {
    "has_child": {
      "type": "comment",
      "query": {
        "match": { "content": "elasticsearch" }
      },
      "max_children": 10,
      "min_children": 1,
      "score_mode": "sum"
    }
  }
}
```

#### 5.4.2 Has Parent Query（父文档查询）

**用途**: 查询有匹配父文档的子文档

**语法**:
```json
{
  "query": {
    "has_parent": {
      "parent_type": "article",
      "query": {
        "term": { "status": "published" }
      }
    }
  }
}
```

---

### 5.5 Script Query（脚本查询）

**用途**: 使用 Painless 脚本进行复杂条件判断

**语法**:
```json
{
  "query": {
    "script": {
      "script": {
        "source": "doc['price'].value * params.discount < 100",
        "params": {
          "discount": 0.8
        }
      }
    }
  }
}
```

**使用场景**:
- 复杂的计算逻辑
- 动态条件判断

**性能注意**:
- ⚠️ **避免频繁使用**：脚本查询性能较差
- ✅ **缓存脚本**：使用 `id` 参数引用缓存的脚本

---

### 5.6 More Like This Query（相似查询）

**用途**: 查询与指定文档相似的文档

**语法**:
```json
{
  "query": {
    "more_like_this": {
      "fields": ["title", "content"],
      "like": [
        {
          "_index": "articles",
          "_id": "1"
        }
      ],
      "min_term_freq": 1,
      "max_query_terms": 25
    }
  }
}
```

**使用场景**:
- 相关推荐
- 查找相似文档

---

## 六、查询模式速查表

### 6.1 按场景选择查询类型

| 场景 | 推荐查询 | 说明 |
|------|----------|------|
| 全文搜索 | `match` / `multi_match` | 分词匹配 |
| 精确匹配 | `term` / `terms` | 不分词 |
| 范围过滤 | `range` | 数值/日期范围 |
| 字段存在 | `exists` | 检查字段是否存在 |
| 前缀匹配 | `prefix` | 以指定前缀开头 |
| 通配符匹配 | `wildcard` | 使用 `*`/`?` |
| 正则匹配 | `regexp` | 正则表达式 |
| 模糊匹配 | `fuzzy` | 允许拼写错误 |
| 短语匹配 | `match_phrase` | 精确短语 |
| 多条件组合 | `bool` | must/should/filter/must_not |
| 自定义评分 | `function_score` | 调整文档分数 |
| 地理位置 | `geo_distance` | 距离查询 |
| 嵌套对象 | `nested` | 查询嵌套字段 |

### 6.2 查询类型分类汇总

| 类别 | 查询类型 |
|------|----------|
| **基础查询** | `match`, `term`, `terms`, `range`, `exists`, `prefix` |
| **复合查询** | `bool`, `boosting`, `constant_score`, `dis_max` |
| **全文查询** | `multi_match`, `query_string`, `simple_query_string`, `match_phrase`, `match_phrase_prefix` |
| **词项查询** | `wildcard`, `regexp`, `fuzzy`, `type`, `ids` |
| **特殊查询** | `function_score`, `geo_*`, `nested`, `has_child`, `has_parent`, `script`, `more_like_this` |

---

## 七、性能优化指南

### 7.1 通用优化策略

| 策略 | 说明 | 示例 |
|------|------|------|
| **使用 filter** | filter 不计算评分，结果可缓存 | 将高频条件放入 `bool.filter` |
| **避免通配符开头** | `*term` 会导致全索引扫描 | 使用 `prefix` 查询替代 |
| **限制结果集** | 使用 `size` 和 `from` 限制返回 | 设置合理的分页参数 |
| **禁用评分** | 使用 `constant_score` 或 `filter` | 过滤场景不需要评分 |
| **使用路由** | 指定 `routing` 参数减少分片查询 | `routing: "user_id"` |

### 7.2 查询缓存优化

**启用查询缓存**:
```json
{
  "index": {
    "request_cache": {
      "enabled": true
    }
  }
}
```

**缓存条件**:
- 查询不包含 `from` 参数
- 查询不包含脚本
- 查询结果集大小 <= `index.max_result_window`

### 7.3 索引优化

| 优化项 | 说明 |
|--------|------|
| **合理的字段类型** | 文本字段用 `text`，关键字段用 `keyword` |
| **禁用不必要的字段** | 设置 `"index": false` 跳过不需要搜索的字段 |
| **使用倒排索引优化** | 合理设置 `analyzer` 和 `normalizer` |
| **预热缓存** | 定期执行常用查询预热缓存 |

### 7.4 常见性能陷阱

| 陷阱 | 后果 | 解决方案 |
|------|------|----------|
| `*` 开头的 wildcard | 全索引扫描 | 使用 `prefix` 查询 |
| 复杂正则表达式 | CPU 密集 | 简化正则模式 |
| 深度分页 (`from` 大) | 内存压力 | 使用 `search_after` |
| 大量 `should` 子句 | 评分计算慢 | 使用 `dis_max` 或限制数量 |
| 频繁脚本查询 | 性能差 | 使用 `function_score` 替代 |

---

## 八、实战示例

### 8.1 电商商品搜索

**需求**: 搜索"智能手机"，价格在 1000-5000 之间，评分 >= 4.5

```json
{
  "query": {
    "bool": {
      "must": [
        { "match": { "title": "智能手机" } }
      ],
      "filter": [
        { "range": { "price": { "gte": 1000, "lte": 5000 } } },
        { "range": { "rating": { "gte": 4.5 } } },
        { "term": { "status": "in_stock" } }
      ]
    }
  },
  "sort": [
    { "sales": { "order": "desc" } },
    { "_score": { "order": "desc" } }
  ],
  "size": 20
}
```

### 8.2 日志分析

**需求**: 查询最近 1 小时内的 ERROR 级别日志，包含关键词 "exception"

```json
{
  "query": {
    "bool": {
      "must": [
        { "match": { "message": "exception" } }
      ],
      "filter": [
        { "term": { "level": "ERROR" } },
        { "range": { "@timestamp": { "gte": "now-1h" } } }
      ]
    }
  },
  "aggs": {
    "by_service": {
      "terms": { "field": "service_name", "size": 10 }
    }
  }
}
```

### 8.3 文档推荐

**需求**: 查找与当前文档相似的文档

```json
{
  "query": {
    "more_like_this": {
      "fields": ["title", "content"],
      "like": {
        "_index": "documents",
        "_id": "current_doc_id"
      },
      "min_term_freq": 2,
      "max_query_terms": 30
    }
  }
}
```

---

## 九、参考资源

| 资源 | 链接 |
|------|------|
| Elasticsearch 官方文档 | https://www.elastic.co/guide/en/elasticsearch/reference/current/index.html |
| Query DSL 参考 | https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl.html |
| Painless 脚本 | https://www.elastic.co/guide/en/elasticsearch/painless/current/index.html |
| 中文文档 | https://www.elastic.co/guide/cn/elasticsearch/guide/current/index.html |

---

## 版本历史

| 版本 | 日期 | 更新内容 |
|------|------|----------|
| 1.0 | 2026-07-16 | 初始版本 |

---

> **提示**: 本手册基于 Elasticsearch 7.x/8.x 版本编写，部分语法可能与旧版本不兼容。建议结合官方文档使用。
>
> # Elasticsearch 查询性能优化清单

## 一、 查询 DSL 编写优化

### 1. 优先使用 Filter 代替 Query
*   **优化原理**：`filter` 上下文不参与相关性评分（不计算 `_score`），且查询结果会被 ES 自动缓存（Filter Cache），性能极高。
*   **最佳实践**：将所有不需要影响相关性排序的条件（如状态、分类、时间范围、精确匹配）放入 `filter` 子句；仅将全文检索（如 `match`）放入 `must` 或 `should` 子句。

### 2. 精准控制返回字段（_source 过滤）
*   **优化原理**：避免加载大文本字段，减少网络传输开销和内存占用。
*   **最佳实践**：使用 `_source: ["field1", "field2"]` 明确指定需要返回的字段；当返回字段极少（少于 40 个）时，优先使用 `docvalue_fields` 拉取列存数据；当返回字段极多时，直接读取 `_source` 性能更优。

### 3. 彻底禁用深度分页
*   **优化原理**：`from + size` 深度分页会导致协调节点从所有相关分片拉取大量数据并在内存中进行全局排序，极易引发 OOM（内存溢出）或超时。
*   **最佳实践**：浅分页（如前 1000 条）保留 `from + size`；深分页（如瀑布流、后台导出）强制使用 `search_after` 或 `scroll` API。

### 4. 规避高危查询语法
*   **优化原理**：前导通配符（如 `*keyword`）、复杂的正则表达式和 Script 脚本查询会导致全量词条扫描或 CPU 密集计算，是典型的性能杀手。
*   **最佳实践**：禁止使用前导通配符，改用 `completion` 或 `edge_ngram` 实现前缀匹配；尽量避免在高频查询中使用 Script；日期范围查询尽量使用固定时间戳替代动态表达式（如 `now-7d/d`），以提高请求缓存命中率。

## 二、 索引设计与 Mapping 优化

### 1. 合理设计字段类型
*   **优化原理**：字段类型直接决定底层存储结构和查询效率。
*   **最佳实践**：严格区分 `text` 和 `keyword`。全文搜索使用 `text`；精确过滤、排序、聚合必须使用 `keyword`。禁止对 `text` 字段进行聚合或排序操作。

### 2. 控制分片与副本规模
*   **优化原理**：过多的小分片会增加协调节点的转发开销和 JVM 内存压力；过少的大分片则无法充分利用多核并行能力。
*   **最佳实践**：单个分片大小建议控制在 **10GB - 50GB** 之间；对于高频读场景，适当增加副本数（如 2 个副本），利用副本机制实现查询负载均衡。

### 3. 利用 Index Sorting（索引预排序）
*   **优化原理**：在写入阶段牺牲少量性能，将文档按特定字段归类存储，大幅减少查询时的随机 I/O。
*   **最佳实践**：针对有明确排序或裁剪需求的索引，开启 `index.sort.field`，极限压测下通常可提升 20% - 40% 的查询性能。

## 三、 缓存与集群架构优化

### 1. 充分利用多层缓存机制
*   **优化原理**：ES 提供了分片级和请求级的缓存，减少重复计算。
*   **最佳实践**：最大化 Filter Cache 命中率（适合静态/准静态数据）；对于高频且固定的聚合/统计请求，开启 Shard Query Cache（`index.queries.cache.enabled: true`）；对于完全静态的字典数据，在业务端（如 Redis）实现客户端缓存。

### 2. 查询路由与裁剪（Routing）
*   **优化原理**：默认查询会广播到所有分片，造成大量节点空转和长尾效应。
*   **最佳实践**：写入时指定自定义 `routing` 值，查询时携带相同 `routing` 参数，将查询精准定位到特定分片；对于时序数据，通过别名或时间范围指定具体的索引名进行查询，避免全量扫描。

### 3. 定期执行 Force Merge（强制段合并）
*   **优化原理**：ES 写入会产生大量小 Segment，查询时产生大量随机 I/O。
*   **最佳实践**：对只读索引或历史数据，在业务低峰期执行 `forcemerge` 操作，将 Segment 合并至 1 个，通常可提升 20% - 30% 的查询性能。

### 4. 硬件与 JVM 调优
*   **优化原理**：底层硬件和 JVM 垃圾回收（GC）直接决定查询延迟的下限。
*   **最佳实践**：生产环境**必须使用 SSD**；JVM 堆内存设置不超过物理内存的 50%，且最大不超过 32GB（以利用 Compressed OOP）；强烈建议禁用系统 Swap，防止堆内存被换出到磁盘导致严重卡顿。

## 四、 监控与排查工具

*   **慢查询日志（Slow Log）**：开启并分析慢查询日志，重点关注 `aggs` 深度嵌套、`wildcard` 前导通配等异常模式。
*   **Profile API**：使用 `_search/profile` 精准下钻到 `QueryPhase` 与 `FetchPhase`，定位具体是哪个子查询耗时最长。
*   **集群监控**：通过 Kibana 或 Prometheus + Grafana 实时监控 CPU、内存、查询延迟、缓存命中率等核心指标，建立告警机制。



---
需要我帮你整理一份 ES 聚合（Aggregation）性能优化清单吗？聚合和查询经常一起用，性能优化策略也值得单独拎出来。