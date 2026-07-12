# 乐工宝打卡时间窗预检 — 简历与面试准备

## 一、简历项目描述

设计打卡时段预检接口，采用Caffeine进程缓存按业务日精确过期，BFF层解析身份透传，前端联动置灰，实现零侵入时间管控。

（49字）

---

## 二、面试问答准备

### Q1：为什么选择 Caffeine 进程内缓存而不是 Redis？

**回答要点：**
- **时效性特征匹配**：规则以"业务日"为粒度变化，次日0点统一失效，无需跨实例共享；进程内缓存延迟远低于Redis网络IO。
- **避免过度设计**：BFF不缓存业务结果，attendance-service单服务承载预检，无多实例一致性问题；引入Redis会增加运维复杂度和序列化开销。
- **负缓存友好**：空列表`[]`也可缓存，防止无规则项目反复打库；Caffeine原生支持`maximumSize`防堆溢出。
- **精确过期保障**：通过`Expiry`接口按key内date动态计算到次日0点的纳秒级TTL，避免固定24h TTL跨越业务日边界的问题。

### Q2：为什么只做前端拦截，不改写打卡接口？

**回答要点：**
- **最小侵入原则**：打卡写入链路涉及考勤记录、工时计算等多下游系统，改动风险高；预检仅为UX增强，不应阻塞核心业务流。
- **容错设计**：预检失败时前端不禁用按钮（`success=false`不误伤），保证可用性优先；服务端打卡接口本身已有完整校验兜底。
- **职责分离**：预检是"能否打"的提示，打卡接口是"是否有效"的裁决，两者语义不同，解耦后各自可独立演进。

### Q3：L1 缓存的 TTL 策略如何保证业务日切换的正确性？

**回答要点：**
- **动态TTL而非固定时长**：使用Caffeine `expireAfter(Expiry)` 接口，create时解析key中的date，计算`Duration.between(now, date+1天0点)`作为TTL。
- **防越界**：禁止`expireAfterWrite(24h)`，因为写入时刻不固定，可能导致缓存在当日未结束时就过期或跨日到次日仍有效。
- **过期即miss**：Caffeine在expireAt后自动失效，业务侧`cache.get(key, loader)`触发重新加载，无需手写定时扫描或now比较。
- **已过期date防护**：当`Duration ≤ 0`时不put，避免无效缓存占用空间。

### Q4：BFF 层与 attendance-service 的职责如何划分？

**回答要点：**
- **BFF（worker-app-aggregator）**：仅负责身份解析（token→workerId）、项目上下文提取（Header→projectId/groupId）、Feign透传；不查规则、不缓存业务结果、不做匹配计算。
- **attendance-service**：接收projectId+projectWorkerId，自主查询工人属性、加载规则、执行scope匹配和窗口计算；拥有完整的业务领域知识。
- **设计考量**：BFF保持薄层通用性，避免业务逻辑泄漏到聚合层；attendance-service作为规则权威方，确保预检与实际打卡判定逻辑同源。

### Q5：规则匹配中如何处理跨昼夜窗口和并发加载问题？

**回答要点：**
- **跨昼夜窗口**：interval含`isCrossNight`字段显式标识，匹配时依据该字段控制判断逻辑，而非通过start/end大小关系自动推断；空安全使用`Boolean.TRUE.equals()`防null误判。
- **并发加载**：Caffeine `get(key, loader)` 天然合并同key并发请求，仅一个线程执行loader；VO→CachedRule深拷贝后再put，避免MyBatis对象被多线程修改。
- **规则优先级**：非默认规则按effectDate↓、createTime↓排序，第一个scope命中工人的即选中；scope为空视为全员命中，保证兜底。
- **浮动回退**：preFloat > startTime 或 postFloat < endTime 时回退原始区间，防止配置错误导致窗口反转。
