# Elasticsearch 查询语句详解

## 1. 实际案例解析

### 示例查询

```json
{
  "query": {
    "bool": {
      "must": [
        {
          "terms": {
            "biddingNo": ["BN001", "BN002"]
          }
        },
        {
          "terms": {
            "collUserCode": ["user001"]
          }
        },
        {
          "range": {
            "callTime": {
              "gte": "2024-01-01 00:00:00",
              "lte": "2024-01-31 23:59:59"
            }
          }
        }
      ]
    }
  },
  "from": 0,
  "size": 20,
  "sort": [
    {
      "createTime": {
        "order": "desc"
      }
    }
  ]
}
```

### 查询逻辑拆解

**核心结构：**
- **`bool`（布尔查询）**：容器，用于组合多个子查询
- **`must`（必须满足）**：所有条件都必须为真（相当于 SQL 的 `AND`）

**业务逻辑：**
```
条件1: biddingNo IN ("BN001", "BN002")
  AND
条件2: collUserCode = "user001"
  AND
条件3: callTime BETWEEN "2024-01-01" AND "2024-01-31"
```

**其他参数：**
- `from: 0` - 从第 0 条开始（分页起始位置）
- `size: 20` - 返回 20 条数据
- `sort` - 按 `createTime` 降序排列

---

## 2. 常用查询类型详解

根据搜索内容的不同，ES 查询主要分为以下几类：

### A. 全文检索（Full Text Query）

针对 `text` 类型字段，查询词会被**分词器**处理。

#### 1️⃣ match - 标准全文搜索

对输入进行分词，只要包含任意词就会匹配（默认 OR 关系），并按相关性排序。

```json
{
  "match": {
    "content": "Elasticsearch 学习"
  }
}
```

**执行过程：**
1. 分词：`["Elasticsearch", "学习"]`
2. 匹配：包含任一词汇的文档都会返回
3. 排序：按 `_score`（相关性分数）降序

#### 2️⃣ match_phrase - 短语搜索

要求词语必须**按顺序紧挨着**出现。

```json
{
  "match_phrase": {
    "content": "Elasticsearch 学习"
  }
}
```

**区别：**
- `match`：包含 "Elasticsearch" 或 "学习" 即可
- `match_phrase`：必须是 "Elasticsearch 学习" 完整短语

#### 3️⃣ multi_match - 多字段搜索

用一个查询文本在**多个字段**中同时搜索。

```json
{
  "multi_match": {
    "query": "elasticsearch",
    "fields": ["title", "description"]
  }
}
```

**适用场景：**
- 搜索引擎：在标题、描述、标签中同时搜索关键词
- 电商网站：在商品名称、品牌、分类中搜索

**高级用法：**

```json
{
  "multi_match": {
    "query": "手机",
    "fields": [
      "title^3",        // title 字段权重提高 3 倍
      "description^1",
      "tags^2"
    ],
    "type": "best_fields"  // 取最佳匹配字段的分数
  }
}
```

---

### B. 精确匹配（Term Level Query）

针对 `keyword`、数字、日期等**不分词**的字段。

#### 1️⃣ term - 精确匹配单个值

相当于 SQL 的 `=`。

```json
{
  "term": {
    "status": "published"
  }
}
```

#### 2️⃣ terms - 精确匹配多个值

相当于 SQL 的 `IN`。

```json
{
  "terms": {
    "user_id": [101, 102, 103]
  }
}
```

#### 3️⃣ range - 范围查询

支持大于、小于、区间查询。

```json
{
  "range": {
    "price": {
      "gte": 100,   // >= 100
      "lte": 500    // <= 500
    }
  }
}
```

**操作符说明：**
- `gt` - 大于（>）
- `gte` - 大于等于（>=）
- `lt` - 小于（<）
- `lte` - 小于等于（<=）

---

### C. 复合查询（Compound Query）

用来组合上述查询，最常用的是 **`bool` 查询**。

#### bool 查询的四个关键子句

| 子句 | 含义 | SQL 对应 | 是否算分 | 性能 |
|------|------|----------|---------|------|
| `must` | 必须匹配 | `AND` | ✅ 是 | 一般 |
| `filter` | 必须匹配 | `AND` | ❌ 否 | ⚡ 快（有缓存） |
| `should` | 应该匹配 | `OR` | ✅ 是 | 一般 |
| `must_not` | 必须不匹配 | `NOT` | ❌ 否 | ⚡ 快 |

#### 组合示例

```json
{
  "query": {
    "bool": {
      "must": [
        {
          "match": {
            "description": "手机"
          }
        }
      ],
      "filter": [
        {
          "term": {
            "brand": "Huawei"
          }
        },
        {
          "range": {
            "price": {
              "lte": 5000
            }
          }
        }
      ],
      "should": [
        {
          "term": {
            "tags": "5G"
          }
        }
      ],
      "must_not": [
        {
          "term": {
            "status": "out_of_stock"
          }
        }
      ]
    }
  }
}
```

**查询含义：**
```
描述中包含"手机"
  AND 品牌 = "Huawei"
  AND 价格 <= 5000
  AND (最好有 "5G" 标签，加分)
  AND 状态 != "out_of_stock"
```

#### 💡 最佳实践：优先使用 filter

```json
// ❌ 不推荐：全部用 must，性能较差
{
  "bool": {
    "must": [
      { "term": { "status": "active" } },
      { "range": { "age": { "gte": 18 } } }
    ]
  }
}

// ✅ 推荐：精确匹配和范围查询用 filter
{
  "bool": {
    "must": [
      { "match": { "name": "张三" } }  // 全文检索需要算分
    ],
    "filter": [
      { "term": { "status": "active" } },     // 精确匹配，不需要算分
      { "range": { "age": { "gte": 18 } } }   // 范围查询，不需要算分
    ]
  }
}
```

**为什么 filter 更快？**
1. **不计算相关性分数**（`_score`）
2. **结果可缓存**（bitset 缓存）
3. **适合二元判断**（是/否）

---

## 3. 辅助功能

### A. 分页（Pagination）

```json
{
  "from": 0,    // 从第几条开始（默认 0）
  "size": 10    // 返回多少条（默认 10）
}
```

**注意事项：**
- 深度分页问题：`from + size` 不能超过 `index.max_result_window`（默认 10000）
- 大数据量建议使用 `search_after`（见下文）

### B. 排序（Sorting）

```json
{
  "sort": [
    { "price": { "order": "asc" } },      // 价格升序
    { "create_time": { "order": "desc" } }, // 创建时间降序
    { "_score": { "order": "desc" } }      // 相关性分数降序（默认）
  ]
}
```

### C. 高亮（Highlighting）

让搜索关键词在结果中**加粗显示**。

```json
{
  "query": {
    "match": {
      "content": "Elasticsearch"
    }
  },
  "highlight": {
    "fields": {
      "content": {}
    }
  }
}
```

**返回结果：**
```json
{
  "highlight": {
    "content": [
      "学习 <em>Elasticsearch</em> 的最佳实践"
    ]
  }
}
```

### D. 聚合（Aggregations）

用于统计分析，类似 SQL 的 `GROUP BY`、`COUNT`、`AVG` 等。

#### 示例：按品牌分组统计

```json
{
   "size": 0,  // 不返回具体文档，只返回聚合结果
   "aggs": {
      "brands": {                    // 第1层：按品牌分组
         "terms": {
            "field": "brand",
            "size": 10                 // 只返回前10个品牌
         },
         "aggs": {                    // 第2层：嵌套聚合（子聚合）
            "avg_price": {             // 计算每个品牌的平均价格
               "avg": {
                  "field": "price"
               }
            }
         }
      }
   }
}
```
相当于 SQL 的
SELECT
brand,
COUNT(*) as doc_count,
AVG(price) as avg_price
FROM products
GROUP BY brand
ORDER BY doc_count DESC
LIMIT 10

**返回结果：**
```json
{
  "aggregations": {
    "brands": {
      "buckets": [
        {
          "key": "Huawei",
          "doc_count": 150,
          "avg_price": { "value": 4500.0 }
        },
        {
          "key": "Apple",
          "doc_count": 120,
          "avg_price": { "value": 6800.0 }
        }
      ]
    }
  }
}
```

**常用聚合类型：**
- `terms` - 分组统计（GROUP BY）
- `avg` - 平均值
- `sum` - 求和
- `min/max` - 最小/最大值
- `cardinality` - 去重计数（DISTINCT COUNT）
- `date_histogram` - 时间直方图

---

## 4. 深度分页解决方案：search_after

### 问题背景

传统分页 `from + size` 在深度分页时性能极差：

```json
// ❌ 查询第 1000 页，每页 10 条
{
  "from": 9990,
  "size": 10
}
```

**性能问题：**
- ES 需要在每个分片上收集前 10000 条数据
- 协调节点需要合并所有分片的结果并排序
- 内存消耗大，响应慢
- `from + size` 不能超过 `index.max_result_window`（默认 10000）

---

### search_after 原理

基于**游标（Cursor）**的分页方式，每次查询依赖上一次最后一条记录的排序值。

**核心思想：**
```
第1页: 查询前10条 → 返回最后一条的 sort 值
第2页: 使用 sort 值作为起点 → 查询下10条
第3页: 使用新的 sort 值 → 继续查询
...
```

**关键要求：**
1. ✅ **必须有 `sort` 字段**
2. ✅ **sort 必须包含唯一字段**（如 `_id`），确保排序稳定
3. ✅ **每次查询条件必须一致**（query、sort 不能变）

---

### 完整实现示例

#### 场景：商品列表无限滚动

假设有一个商品索引，需要实现类似抖音/小红书的无限滚动加载。

#### 第1步：首次查询（第1页）

```json
GET /products/_search
{
  "size": 10,
  "query": {
    "match": {
      "category": "手机"
    }
  },
  "sort": [
    { "create_time": "desc" },  // 主排序：创建时间降序
    { "_id": "asc" }             // 次排序：ID升序（保证唯一性）
  ]
}
```

**返回结果：**
```json
{
  "hits": {
    "total": { "value": 1500 },
    "hits": [
      {
        "_id": "101",
        "_source": { "name": "iPhone 15", "price": 7999 },
        "sort": [1704153600000, "101"]  // ⭐ 记录这个值
      },
      { "_id": "102", "sort": [1704153500000, "102"] },
      // ... 共10条
      {
        "_id": "110",
        "_source": { "name": "小米14", "price": 3999 },
        "sort": [1704150000000, "110"]  // ⭐ 最后一条的 sort 值
      }
    ]
  }
}
```

**客户端需要保存：**
```javascript
// 前端/后端保存最后一条的 sort 值
let lastSortValues = [1704150000000, "110"];
```

#### 第2步：查询下一页（第2页）

```json
GET /products/_search
{
  "size": 10,
  "query": {
    "match": {
      "category": "手机"
    }
  },
  "sort": [
    { "create_time": "desc" },
    { "_id": "asc" }
  ],
  "search_after": [1704150000000, "110"]  // ⭐ 使用上一页最后的 sort 值
}
```

**返回结果：**
```json
{
  "hits": {
    "hits": [
      {
        "_id": "111",
        "sort": [1704149900000, "111"]  // 新的最后一条
      },
      // ... 共10条
      {
        "_id": "120",
        "sort": [1704149000000, "120"]  // ⭐ 更新 lastSortValues
      }
    ]
  }
}
```

**更新游标：**
```javascript
lastSortValues = [1704149000000, "120"];
```

#### 第3步：循环加载

重复第2步，每次使用最新的 `lastSortValues`。

---

### Java 代码实现

#### Spring Data Elasticsearch 示例

```java
@Service
public class ProductSearchService {
    
    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;
    
    /**
     * 使用 search_after 实现深度分页
     */
    public SearchHits<Product> searchWithAfter(
            String keyword, 
            List<Object> searchAfter,  // 上一页的 sort 值
            int pageSize) {
        
        // 1. 构建查询
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(QueryBuilders.matchQuery("name", keyword));
        
        // 2. 设置排序（必须包含唯一字段）
        queryBuilder.withSort(SortBuilders.fieldSort("create_time").order(SortOrder.DESC));
        queryBuilder.withSort(SortBuilders.fieldSort("_id").order(SortOrder.ASC));
        
        // 3. 设置 page size
        queryBuilder.withPageable(PageRequest.of(0, pageSize));
        
        // 4. 设置 search_after
        if (searchAfter != null && !searchAfter.isEmpty()) {
            queryBuilder.withSearchAfter(searchAfter.toArray());
        }
        
        // 5. 执行查询
        SearchHits<Product> results = elasticsearchTemplate.search(
            queryBuilder.build(), 
            Product.class
        );
        
        return results;
    }
    
    /**
     * 提取最后一条记录的 sort 值
     */
    public List<Object> extractLastSortValues(SearchHits<?> results) {
        if (results.isEmpty()) {
            return Collections.emptyList();
        }
        
        // 获取最后一条记录
        SearchHit<?> lastHit = results.getSearchHits().get(results.size() - 1);
        return lastHit.getSortValues();
    }
}
```

#### Controller 层调用

```java
@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductSearchService productSearchService;
    
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) List<Object> searchAfter,
            @RequestParam(defaultValue = "10") int size) {
        
        // 执行查询
        SearchHits<Product> results = productSearchService.searchWithAfter(
            keyword, searchAfter, size
        );
        
        // 提取下一页的游标
        List<Object> nextSearchAfter = productSearchService.extractLastSortValues(results);
        
        // 构建响应
        Map<String, Object> response = new HashMap<>();
        response.put("total", results.getTotalHits());
        response.put("products", results.getSearchHits().stream()
            .map(SearchHit::getContent)
            .collect(Collectors.toList()));
        response.put("nextSearchAfter", nextSearchAfter);  // 传给前端
        response.put("hasMore", results.size() == size);   // 是否还有更多
        
        return ResponseEntity.ok(response);
    }
}
```

#### 前端调用示例

```javascript
let searchAfter = null;

async function loadMoreProducts() {
    const response = await fetch(
        `/api/products/search?keyword=手机&size=10` +
        (searchAfter ? `&searchAfter=${JSON.stringify(searchAfter)}` : '')
    );
    
    const data = await response.json();
    
    // 渲染商品列表
    renderProducts(data.products);
    
    // 保存游标，用于下次加载
    searchAfter = data.nextSearchAfter;
    
    // 判断是否还有更多
    if (!data.hasMore) {
        showNoMoreMessage();
    }
}

// 首次加载
loadMoreProducts();

// 滚动到底部时加载下一页
window.addEventListener('scroll', () => {
    if (isNearBottom()) {
        loadMoreProducts();
    }
});
```

---

### 高级用法

#### 1. 多字段排序

```json
{
  "sort": [
    { "category": "asc" },       // 第1优先级：分类
    { "price": "desc" },         // 第2优先级：价格
    { "create_time": "desc" },   // 第3优先级：时间
    { "_id": "asc" }             // 第4优先级：ID（保证唯一）
  ],
  "search_after": ["手机", 7999, 1704150000000, "110"]
}
```

**注意：** `search_after` 数组的顺序和长度必须与 `sort` 完全一致！

#### 2. 结合过滤条件

```json
{
  "size": 10,
  "query": {
    "bool": {
      "must": [
        { "match": { "category": "手机" } }
      ],
      "filter": [
        { "range": { "price": { "lte": 5000 } } },
        { "term": { "brand": "Huawei" } }
      ]
    }
  },
  "sort": [
    { "create_time": "desc" },
    { "_id": "asc" }
  ],
  "search_after": [1704150000000, "110"]
}
```

#### 3. 反向遍历（上一页）

`search_after` **不支持直接跳转到上一页**，但可以这样实现：

**方案A：缓存历史游标**
```javascript
// 维护一个游标栈
let cursorStack = [];
let currentCursor = null;

// 下一页
function nextPage() {
    cursorStack.push(currentCursor);  // 保存当前游标
    currentCursor = lastSortValues;   // 更新为新游标
    loadData(currentCursor);
}

// 上一页
function prevPage() {
    if (cursorStack.length > 0) {
        currentCursor = cursorStack.pop();  // 弹出上一个游标
        loadData(currentCursor);
    }
}
```

**方案B：重新查询（不推荐）**
- 如果必须支持随机跳转，还是用 `from + size`
- 限制最大页数（如最多100页）

---

### search_after vs 其他方案对比

| 特性 | from + size | search_after | scroll API |
|------|------------|--------------|------------|
| **适用场景** | 浅分页（< 10000） | 深分页、无限滚动 | 全量导出 |
| **随机跳转** | ✅ 支持 | ❌ 不支持 | ❌ 不支持 |
| **性能稳定性** | 深度分页差 | ⚡ 始终稳定 | ⚡ 稳定 |
| **实时性** | ✅ 实时 | ✅ 实时 | ❌ 快照 |
| **资源占用** | 高（深度分页） | 低 | 中（需维护上下文） |
| **实现复杂度** | 简单 | 中等 | 简单 |
| **状态保持** | 无状态 | 客户端维护 | 服务端维护 |
| **过期机制** | - | - | 需设置 expire |

---

### 常见陷阱与解决方案

#### 陷阱1：忘记添加唯一排序字段

```json
// ❌ 错误：只有时间排序，可能导致数据重复或遗漏
{
  "sort": [{ "create_time": "desc" }]
}

// ✅ 正确：添加 _id 保证唯一性
{
  "sort": [
    { "create_time": "desc" },
    { "_id": "asc" }
  ]
}
```

**原因：** 如果两条记录的 `create_time` 相同，没有唯一字段会导致排序不稳定。

#### 陷阱2：查询条件不一致

```javascript
// 第1页
query: { match: { category: "手机" } }
sort: [{ create_time: "desc" }, { _id: "asc" }]

// 第2页 - ❌ 错误：改变了查询条件
query: { match: { category: "电脑" } }  // 变了！
sort: [{ create_time: "desc" }, { _id: "asc" }]
search_after: [...]
```

**规则：** `search_after` 分页期间，`query` 和 `sort` **必须保持不变**。

#### 陷阱3：数据类型不匹配

```json
// 索引映射
{
  "mappings": {
    "properties": {
      "create_time": { "type": "date" },
      "price": { "type": "float" }
    }
  }
}

// ❌ 错误：sort 值是字符串
"search_after": ["1704150000000", "110"]

// ✅ 正确：保持数据类型一致
"search_after": [1704150000000, "110"]  // 时间戳是数字
```

#### 陷阱4：空结果处理

```java
// ✅ 正确处理空结果
if (results.isEmpty()) {
    return Collections.emptyList();
}

// 获取最后一条的 sort 值
SearchHit<?> lastHit = results.getSearchHits().get(results.size() - 1);
return lastHit.getSortValues();
```

---

### 最佳实践总结

#### ✅ 推荐使用 search_after 的场景

1. **无限滚动加载**（抖音、小红书、Twitter）
2. **大数据量导出**（替代 scroll API）
3. **日志浏览**（Kibana 内部使用）
4. **实时监控列表**（订单流、交易流）

#### ❌ 不推荐使用 search_after 的场景

1. **需要页码导航**（第1页、第2页...第100页）
2. **需要跳转到指定页**（直接跳到第50页）
3. **用户需要知道总页数**（search_after 无法提前知道总数）

#### 💡 混合方案

对于既有列表又有分页需求的场景：

```javascript
// 前100页使用 from + size（支持页码跳转）
if (page <= 100) {
    useFromSize(page, size);
} 
// 超过100页使用 search_after（无限滚动）
else {
    useSearchAfter(cursor);
}
```

---

### 性能测试对比

假设有 100万条数据，每页 10 条：

| 页码 | from + size 耗时 | search_after 耗时 | 性能提升 |
|------|-----------------|-------------------|----------|
| 第1页 | 5ms | 5ms | - |
| 第10页 | 8ms | 5ms | 37% ↑ |
| 第100页 | 50ms | 5ms | **90% ↑** |
| 第1000页 | 500ms | 5ms | **99% ↑** |
| 第10000页 | 超时 | 5ms | ∞ |

**结论：** 数据量越大，`search_after` 优势越明显！

---

### 相关面试题

**Q: search_after 和 scroll API 的区别？**

A:
- `scroll`：服务端维护上下文，适合离线批量导出，有 TTL 过期机制
- `search_after`：客户端维护游标，适合实时在线查询，无状态
- ES 官方推荐：优先使用 `search_after`，`scroll` 已标记为废弃

**Q: 为什么 search_after 需要唯一排序字段？**

A:
- 保证排序的稳定性（Stable Sort）
- 避免相同排序值的文档被重复返回或遗漏
- 通常使用 `_id` 作为最后的排序字段

**Q: search_after 能实现上一页功能吗？**

A:
- 不能直接实现，因为它是单向游标
- 可以通过客户端缓存历史游标栈来实现
- 如果需要频繁上下翻页，建议使用 `from + size` 并限制最大页数

---

## 5. 性能优化建议

### ✅ 推荐做法

#### 1. 优先使用 filter

不需要算分的查询都用 `filter`，性能更好且有缓存。

```json
{
  "bool": {
    "must": [
      { "match": { "name": "手机" } }  // 全文检索需要算分
    ],
    "filter": [
      { "term": { "brand": "Huawei" } },     // 精确匹配，不需要算分
      { "range": { "price": { "lte": 5000 } } }  // 范围查询，不需要算分
    ]
  }
}
```

---

#### 2. 只返回需要的字段（_source 过滤）⭐重要

**问题背景：**

默认情况下，ES 会返回文档的完整 `_source`，如果文档很大（如包含大段文本、图片 Base64 等），会造成：
- ❌ 网络传输开销大
- ❌ 内存占用高
- ❌ 响应速度慢

**解决方案：** 使用 `_source` 字段过滤，只返回需要的字段。

---

##### 基础用法：指定字段列表

```json
GET /products/_search
{
  "_source": ["name", "price", "brand"],  // ⭐ 只返回这3个字段
  "query": {
    "match": {
      "category": "手机"
    }
  }
}
```

**返回结果对比：**

```json
// ❌ 不使用 _source 过滤：返回完整文档
{
  "_source": {
    "id": 101,
    "name": "iPhone 15",
    "price": 7999,
    "brand": "Apple",
    "category": "手机",
    "description": "这是一段很长的商品描述...",  // 可能几KB
    "images": ["base64...", "base64..."],        // 可能几十KB
    "specs": {...},                                // 详细规格
    "reviews": [...],                              // 用户评价
    "create_time": "2024-01-01",
    "update_time": "2024-01-15"
  }
}

// ✅ 使用 _source 过滤：只返回需要的字段
{
  "_source": {
    "name": "iPhone 15",
    "price": 7999,
    "brand": "Apple"
  }
}
```

**性能提升：**
- 网络传输减少 **80%-95%**
- 响应时间减少 **50%-70%**
- 内存占用减少 **60%-80%**

---

##### 高级用法1：排除特定字段

```json
{
  "_source": {
    "excludes": ["description", "images", "reviews"]  // 排除这些字段
  },
  "query": {
    "match": {
      "category": "手机"
    }
  }
}
```

**适用场景：**
- 大部分字段都需要，只排除几个大字段
- 动态排除敏感信息（如密码、身份证号）

---

##### 高级用法2：包含 + 排除组合

```json
{
  "_source": {
    "includes": ["name", "price", "brand", "category"],  // 包含这些
    "excludes": ["internal_notes", "cost_price"]          // 但排除这些
  },
  "query": {
    "match": {
      "category": "手机"
    }
  }
}
```

---

##### 高级用法3：通配符匹配

```json
{
  "_source": {
    "includes": ["name", "price", "stats.*"]  // 返回所有 stats 开头的字段
  },
  "query": {
    "match": {
      "category": "手机"
    }
  }
}
```

**示例数据结构：**
```json
{
  "name": "iPhone 15",
  "price": 7999,
  "stats_views": 1500,      // ✅ 返回
  "stats_likes": 320,       // ✅ 返回
  "stats_shares": 85,       // ✅ 返回
  "description": "...",     // ❌ 不返回
  "images": [...]           // ❌ 不返回
}
```

---

##### 实战案例1：商品列表页

**需求：** 展示商品列表，只需要名称、价格、图片URL、评分。

```json
GET /products/_search
{
  "size": 20,
  "_source": [
    "name",
    "price",
    "thumbnail_url",    // 缩略图URL（不是Base64）
    "rating",
    "sales_count"
  ],
  "query": {
    "bool": {
      "must": [
        { "match": { "category": "手机" } }
      ],
      "filter": [
        { "range": { "price": { "lte": 5000 } } }
      ]
    }
  },
  "sort": [
    { "sales_count": "desc" }
  ]
}
```

**性能对比：**

| 场景 | 文档大小 | 响应时间 | 网络流量 |
|------|---------|---------|----------|
| 不过滤 | 15 KB | 120 ms | 300 KB (20条) |
| 过滤后 | 0.5 KB | 45 ms | 10 KB (20条) |
| **提升** | **-97%** | **-62%** | **-97%** |

---

##### 实战案例2：搜索建议（Autocomplete）

**需求：** 用户输入时实时显示搜索建议，只需要返回名称和ID。

```json
GET /products/_search
{
  "size": 10,
  "_source": ["id", "name"],  // 最小化返回
  "query": {
    "prefix": {
      "name.keyword": "iPh"
    }
  }
}
```

**返回：**
```json
{
  "hits": [
    {
      "_id": "101",
      "_source": {
        "id": 101,
        "name": "iPhone 15"
      }
    },
    {
      "_id": "102",
      "_source": {
        "id": 102,
        "name": "iPhone 14"
      }
    }
  ]
}
```

---

##### 实战案例3：统计数据（不需要文档内容）

**需求：** 只需要统计数量，不需要返回任何文档。

```json
GET /products/_search
{
  "size": 0,  // ⭐ 不返回任何文档
  "_source": false,  // ⭐ 明确禁用 _source
  "query": {
    "match": {
      "category": "手机"
    }
  },
  "aggs": {
    "avg_price": {
      "avg": { "field": "price" }
    },
    "total_sales": {
      "sum": { "field": "sales_count" }
    }
  }
}
```

**返回：**
```json
{
  "hits": {
    "total": { "value": 1500 },
    "hits": []  // 空数组，不返回文档
  },
  "aggregations": {
    "avg_price": { "value": 3500.5 },
    "total_sales": { "value": 45000 }
  }
}
```

---

##### Java 代码实现

**Spring Data Elasticsearch 示例：**

```java
@Service
public class ProductSearchService {
    
    @Autowired
    private ElasticsearchRestTemplate elasticsearchTemplate;
    
    /**
     * 搜索商品，只返回指定字段
     */
    public List<Map<String, Object>> searchProducts(String keyword) {
        
        // 1. 构建查询
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(QueryBuilders.matchQuery("name", keyword));
        
        // 2. ⭐ 设置 _source 过滤
        queryBuilder.withSourceFilter(
            new FetchSourceFilter(
                new String[]{"name", "price", "brand", "thumbnail_url"},  // includes
                null  // excludes
            )
        );
        
        // 3. 执行查询
        SearchHits<Map> results = elasticsearchTemplate.search(
            queryBuilder.build(),
            Map.class
        );
        
        // 4. 提取结果
        return results.getSearchHits().stream()
            .map(hit -> hit.getContent())
            .collect(Collectors.toList());
    }
    
    /**
     * 排除敏感字段
     */
    public List<Map<String, Object>> searchWithExclusion(String keyword) {
        
        NativeSearchQueryBuilder queryBuilder = new NativeSearchQueryBuilder();
        queryBuilder.withQuery(QueryBuilders.matchQuery("name", keyword));
        
        // ⭐ 排除敏感字段
        queryBuilder.withSourceFilter(
            new FetchSourceFilter(
                null,  // includes（null表示全部）
                new String[]{"password", "id_card", "internal_notes"}  // excludes
            )
        );
        
        SearchHits<Map> results = elasticsearchTemplate.search(
            queryBuilder.build(),
            Map.class
        );
        
        return results.getSearchHits().stream()
            .map(hit -> hit.getContent())
            .collect(Collectors.toList());
    }
}
```

---

##### 注意事项

**⚠️ 陷阱1：_source 过滤不影响聚合**

```json
{
  "_source": ["name"],  // 只返回 name
  "aggs": {
    "avg_price": {
      "avg": { "field": "price" }  // ✅ 仍然可以聚合 price 字段
    }
  }
}
```

**说明：** `_source` 过滤只影响返回的文档内容，不影响聚合计算。

---

**⚠️ 陷阱2：嵌套字段需要完整路径**

```json
{
  "_source": [
    "name",
    "specs.cpu",      // ✅ 正确：完整路径
    "specs.ram"       // ✅ 正确
  ]
}
```

---

**⚠️ 陷阱3：_source: false 后无法获取字段值**

```json
{
  "_source": false,  // 完全禁用 _source
  "query": {
    "match": { "name": "iPhone" }
  }
}
```

**返回：**
```json
{
  "hits": [
    {
      "_id": "101",
      "_source": null  // ❌ 没有 _source
    }
  ]
}
```

**适用场景：**
- ✅ 只需要文档 ID（如删除操作）
- ✅ 只需要统计数量（配合 `size: 0`）
- ❌ 需要显示文档内容时不要用

---

#### 3. 避免深度分页

使用 `search_after` 替代大偏移量的 `from + size`。

```json
// ❌ 不推荐：深度分页
{
  "from": 10000,
  "size": 10
}

// ✅ 推荐：search_after
{
  "size": 10,
  "search_after": [1704150000000, "110"],
  "sort": [
    { "create_time": "desc" },
    { "_id": "asc" }
  ]
}
```

---

#### 4. 合理设置 size

不要一次性返回太多数据，建议：
- 列表页：`size: 10-20`
- 搜索建议：`size: 5-10`
- 导出全量：使用 `search_after` 分批获取

---

#### 5. 利用缓存

频繁使用的 `filter` 会被自动缓存（bitset cache），提升性能。

```json
{
  "bool": {
    "filter": [
      { "term": { "status": "active" } },  // ✅ 会被缓存
      { "range": { "price": { "lte": 5000 } } }  // ✅ 会被缓存
    ]
  }
}
```

---

### ❌ 避免做法

#### 1. 避免通配符前缀查询

```json
// ❌ 性能差：无法利用倒排索引
{
  "wildcard": {
    "name": "*phone"
  }
}

// ✅ 推荐：使用 ngram 分词器或 edge_ngram
{
  "match": {
    "name": "phone"
  }
}
```

---

#### 2. 避免正则表达式

```json
// ❌ 性能极差：需要扫描所有文档
{
  "regexp": {
    "email": ".*@gmail\\.com"
  }
}

// ✅ 推荐：使用 term 查询
{
  "term": {
    "email_domain": "gmail.com"
  }
}
```

---

#### 3. 避免大规模聚合

```json
// ❌ 不推荐：返回太多 bucket
{
  "aggs": {
    "brands": {
      "terms": {
        "field": "brand",
        "size": 10000  // 太大！
      }
    }
  }
}

// ✅ 推荐：限制 bucket 数量
{
  "aggs": {
    "brands": {
      "terms": {
        "field": "brand",
        "size": 10  // 只返回 Top 10
      }
    }
  }
}
```

---

#### 4. 避免在循环中查询 ES

```java
// ❌ 不推荐：N 次查询
for (String keyword : keywords) {
    search(keyword);
}

// ✅ 推荐：使用 bulk API 或多值查询
{
  "query": {
    "terms": {
      "name": ["iPhone", "Samsung", "Huawei"]
    }
  }
}
```

---

#### 5. 避免 SELECT * 思维

```json
// ❌ 不推荐：返回所有字段
{
  "query": { ... }
  // 默认返回完整 _source
}

// ✅ 推荐：只返回需要的字段
{
  "_source": ["name", "price"],
  "query": { ... }
}
```

---

## 6. 常见面试题

### Q1: match 和 term 的区别？

**A:**
- `match`：用于 `text` 字段，会先分词再查询，算分
- `term`：用于 `keyword`/数字/日期，精确匹配，不分词

### Q2: must 和 filter 的区别？

**A:**
- `must`：计算相关性分数，结果不缓存
- `filter`：不计算分数，结果缓存（bitset），性能更好

### Q3: 如何实现深度分页？

**A:**
- 小数据量：`from + size`（限制最大页数）
- 大数据量：`search_after`（基于游标）
- 实时性要求不高：`scroll` API（已废弃，推荐 `search_after`）

### Q4: bool 查询中 should 的作用？

**A:**
- 单独使用：相当于 `OR`，至少匹配一个
- 与 `must/filter` 一起使用：不影响过滤，但匹配会提高 `_score`

### Q5: 如何优化 ES 查询性能？

**A:**
1. 尽量使用 `filter` 代替 `must`
2. 避免深度分页，使用 `search_after`
3. 只返回需要的字段（`_source` 过滤）
4. 合理使用缓存（filter 自动缓存）
5. 控制聚合的 bucket 数量
6. 避免在查询中使用脚本（script）
