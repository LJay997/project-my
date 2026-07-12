# AI OCR 识别系统优化方案 - 基于 Skill 设计理念

**文档版本**: v1.0  
**创建日期**: 2026-07-11  
**适用范围**: organization-service 海外证件 OCR 模块

---

## 一、现状分析

### 1.1 当前架构特点

当前系统采用 **Prompt 驱动的多模态大模型** 方案：

```java
// 硬编码的 Prompt 字符串（护照示例，约 800 字符）
private static final String OCR_PROMPT = 
    "## 任务：识别护照证件图片...\n"
    + "### 硬性规则\n"
    + "1. 固定输出JSON键...\n"
    + "2. 文本规则...\n"
    + ...;

// 每次识别都发送完整 Prompt
String responseText = tencentOcrClient.recognize(imageBase64, OCR_PROMPT);
```

### 1.2 存在的问题

| 问题维度 | 具体表现 | 影响程度 |
|---------|---------|---------|
| **Token 成本** | 每次请求重复发送冗长 Prompt（~800 字符） | ⭐⭐⭐⭐ 高成本 |
| **维护困难** | Prompt 硬编码在 Java 类中，修改需重新编译部署 | ⭐⭐⭐ 中等 |
| **缺乏标准化** | 各策略自行定义字段名，无统一 Schema 管理 | ⭐⭐⭐ 中等 |
| **容错能力弱** | 依赖模型一次性输出正确 JSON，无重试/校验机制 | ⭐⭐⭐⭐ 高风险 |
| **无法增量学习** | 错误案例无法沉淀为经验，同类错误反复出现 | ⭐⭐⭐ 中等 |
| **缺少可观测性** | 仅记录原始响应，无结构化指标监控 | ⭐⭐ 低 |

---

## 二、优化方向总览

### 2.1 核心理念：从 Prompt 到 Skill

**当前模式（Prompt-Based）**：
```
用户输入 → 拼接长 Prompt → 调用模型 → 解析结果
```

**目标模式（Skill-Based）**：
```
用户输入 → 加载预定义 Skill → 执行标准化流程 → 验证输出 → 返回结果
```

**Skill 的核心特征**：
1. **可复用**：一次定义，多次调用
2. **可组合**：多个子 Skill 组合成复杂任务
3. **可验证**：内置输出格式校验和纠错机制
4. **可进化**：通过反馈数据持续优化

---

## 三、具体优化方案

### 3.1 【短期】Prompt 工程优化（1-2周）

#### 3.1.1 Prompt 模板化与外部化

**现状问题**：
```java
// 硬编码在类中，修改需重新编译
private static final String OCR_PROMPT = "...";
```

**优化方案**：
```yaml
# config/ocr-skills/passport.yaml
skill:
  name: passport_ocr
  version: "1.0"
  model: hy-vision-2.0-instruct
  
  system_prompt: |
    你是一个专业的证件OCR识别助手。
    
  user_template: |
    请识别这张{{document_type}}图片，提取以下字段：
    {% for field in required_fields %}
    - {{field.name}}: {{field.description}}
    {% endfor %}
    
    输出格式：标准JSON，键名必须严格匹配
    
  fields:
    - name: idNumber
      description: "护照号码，通常位于右上角"
      pattern: "^[A-Z0-9]{6,15}$"
      required: true
      
    - name: name_en
      description: "英文姓名，原样提取"
      required: true
      
    - name: dateOfBirth
      description: "出生日期，格式yyyy-MM-dd"
      pattern: "^\\d{4}-\\d{2}-\\d{2}$"
      required: true
      
  validation:
    json_schema_ref: "#/definitions/PassportSchema"
    retry_on_failure: true
    max_retries: 2
```
```json
{
    "definitions": {
        "PassportSchema": {
            "type": "object",
            "required": [
                "idNumber",
                "name_en",
                "nationality",
                "dateOfBirth"
            ],
            "properties": {
                "idNumber": {
                    "type": "string",
                    "pattern": "^[A-Z0-9]{6,15}$"
                },
                "name_en": {
                    "type": "string",
                    "minLength": 1
                },
                "nationality": {
                    "type": "string"
                },
                "dateOfBirth": {
                    "type": "string",
                    "pattern": "^\\d{4}-\\d{2}-\\d{2}$"
                }
            }
        }
    }
}
```
**代码改造**：
```java
@Component
public class PassportOcrStrategy implements OcrStrategy<IdCardOcrResult> {
    
    @Autowired
    private SkillTemplateEngine templateEngine; // 新增
    
    @Override
    public IdCardOcrResult ocr(String imageBase64) {
        // 1. 加载 Skill 配置
        SkillConfig skill = skillRegistry.get("passport_ocr");
        
        // 2. 渲染 Prompt（填充变量）
        String prompt = templateEngine.render(skill.getUserTemplate(), Map.of(
            "document_type", "护照",
            "required_fields", skill.getFields()
        ));
        
        // 3. 调用模型
        String response = tencentOcrClient.recognize(imageBase64, prompt);
        
        // 4. 验证输出（新增）
        ValidationResult validation = validator.validate(response, skill.getValidation());
        if (!validation.isValid()) {
            throw new OcrValidationException(validation.getErrors());
        }
        
        return parseResponse(response, imageBase64);
    }
}
```

**收益**：
- ✅ Prompt 修改无需重新编译（配置热更新）
- ✅ 统一管理字段定义和校验规则
- ✅ 支持多版本 A/B 测试

---

#### 3.1.2 增加输出验证与自动重试

**现状问题**：
```java
// 直接解析，失败就抛异常
JsonNode root = objectMapper.readTree(json);
```

**优化方案**：
```java
@Component
public class OcrOutputValidator {
    
    /**
     * 验证模型输出是否符合要求
     */
    public ValidationResult validate(String response, ValidationRules rules) {
        List<String> errors = new ArrayList<>();
        
        try {
            JsonNode json = objectMapper.readTree(response);
            
            // 1. 必填字段检查
            for (String requiredField : rules.getRequiredFields()) {
                if (json.get(requiredField) == null || json.get(requiredField).asText().isEmpty()) {
                    errors.add("缺少必填字段: " + requiredField);
                }
            }
            
            // 2. 格式校验（正则表达式）
            rules.getFieldPatterns().forEach((field, pattern) -> {
                String value = json.get(field).asText();
                if (!value.isEmpty() && !Pattern.matches(pattern, value)) {
                    errors.add("字段 " + field + " 格式不符: " + value);
                }
            });
            
            // 3. 逻辑校验（如日期合理性）
            if (json.get("dateOfBirth") != null) {
                LocalDate dob = LocalDate.parse(json.get("dateOfBirth").asText());
                if (dob.isAfter(LocalDate.now())) {
                    errors.add("出生日期不能是未来: " + dob);
                }
            }
            
        } catch (Exception e) {
            errors.add("JSON 解析失败: " + e.getMessage());
        }
        
        return new ValidationResult(errors.isEmpty(), errors);
    }
}
```

**重试机制**：
```java
public IdCardOcrResult ocrWithRetry(String imageBase64, int maxRetries) {
    Exception lastException = null;
    
    for (int attempt = 1; attempt <= maxRetries; attempt++) {
        try {
            String response = tencentOcrClient.recognize(imageBase64, OCR_PROMPT);
            ValidationResult validation = validator.validate(response, validationRules);
            
            if (validation.isValid()) {
                return parseResponse(response, imageBase64);
            } else {
                log.warn("第{}次尝试验证失败: {}", attempt, validation.getErrors());
                lastException = new OcrValidationException(validation.getErrors());
            }
        } catch (Exception e) {
            log.error("第{}次尝试异常: {}", attempt, e.getMessage());
            lastException = e;
        }
        
        // 指数退避等待
        if (attempt < maxRetries) {
            Thread.sleep((long) Math.pow(2, attempt) * 1000);
        }
    }
    
    throw new RuntimeException("OCR 识别失败，已重试 " + maxRetries + " 次", lastException);
}
```

**收益**：
- ✅ 减少因模型偶发错误导致的失败
- ✅ 提前发现数据质量问题
- ✅ 提供明确的错误定位信息

---

### 3.2 【中期】引入 Function Calling / Tool Use（2-4周）

#### 3.2.1 为什么需要 Function Calling？

**当前痛点**：
- 模型需要同时理解任务 + 记住字段格式 + 执行坐标计算
- 复杂任务（如回历转公历）容易出错
- 输出格式不稳定（偶尔带 markdown 标记）

**Function Calling 优势**：
```
传统方式：
用户："请提取护照信息并以JSON返回"
模型：[自己决定输出格式] → 可能出错

Function Calling：
用户："调用 extract_passport_info 函数"
模型：[严格按照函数签名返回参数] → 格式保证正确
```

#### 3.2.2 实现方案

**步骤1：定义工具函数**

```java
@Component
public class OcrTools {
    
    /**
     * 提取护照信息的工具函数（供模型调用）
     */
    @ToolFunction(
        name = "extract_passport_info",
        description = "从护照图片中提取结构化信息"
    )
    public PassportData extractPassportInfo(
        @ToolParam(description = "护照号码") String idNumber,
        @ToolParam(description = "英文姓名") String nameEn,
        @ToolParam(description = "国籍") String nationality,
        @ToolParam(description = "出生日期(yyyy-MM-dd)") String dateOfBirth,
        @ToolParam(description = "有效期结束日期(yyyy-MM-dd)") String validUntil,
        @ToolParam(description = "性别(男/女)") String gender,
        @ToolParam(description = "头像坐标{x,y,width,height}") FaceBox faceBox
    ) {
        // 模型只需返回参数，不需要构造 JSON
        return new PassportData(idNumber, nameEn, nationality, dateOfBirth, validUntil, gender, faceBox);
    }
    
    /**
     * 伊斯兰回历转公历的工具函数
     */
    @ToolFunction(
        name = "hijri_to_gregorian",
        description = "将伊斯兰回历日期转换为公历日期"
    )
    public String convertHijriToGregorian(
        @ToolParam(description = "回历年份") int hijriYear,
        @ToolParam(description = "回历月份") int hijriMonth,
        @ToolParam(description = "回历日期") int hijriDay
    ) {
        // 调用专门的日期转换库
        return HijriCalendarConverter.toGregorian(hijriYear, hijriMonth, hijriDay);
    }
}
```

**步骤2：改造腾讯客户端支持 Function Calling**

```java
public class TencentOcrClient {
    
    /**
     * 支持 Function Calling 的识别方法
     */
    public String recognizeWithTools(String imageBase64, String prompt, List<ToolDefinition> tools) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", MODEL);
        
        // 构建消息
        List<Map<String, Object>> messages = new ArrayList<>();
        Map<String, Object> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        
        // 内容：图片 + 文本
        List<Map<String, Object>> content = new ArrayList<>();
        content.add(buildImagePart(imageBase64));
        content.add(buildTextPart(prompt));
        userMessage.put("content", content);
        messages.add(userMessage);
        
        body.put("messages", messages);
        
        // 新增：声明可用工具
        body.put("tools", tools.stream().map(tool -> Map.of(
            "type", "function",
            "function", Map.of(
                "name", tool.getName(),
                "description", tool.getDescription(),
                "parameters", tool.getParameterSchema()
            )
        )).collect(Collectors.toList()));
        
        // 强制模型使用工具
        body.put("tool_choice", "auto");
        
        String requestBody = objectMapper.writeValueAsString(body);
        String responseBody = sendRequest(requestBody);
        
        // 解析工具调用结果
        return parseToolCallResponse(responseBody);
    }
}
```

**步骤3：策略层调用**

```java
@Override
public IdCardOcrResult ocr(String imageBase64) {
    // 定义可用的工具
    List<ToolDefinition> tools = Arrays.asList(
        ToolDefinition.builder()
            .name("extract_passport_info")
            .description("提取护照结构化信息")
            .parameters(PassportData.class)
            .build(),
        ToolDefinition.builder()
            .name("hijri_to_gregorian")
            .description("回历转公历")
            .parameters(HijriDateInput.class)
            .build()
    );
    
    String prompt = "请识别这张护照图片，调用 extract_passport_info 函数返回结果";
    
    // 调用支持工具的接口
    String result = tencentOcrClient.recognizeWithTools(imageBase64, prompt, tools);
    
    return deserializeFromToolCall(result);
}
```

**收益**：
- ✅ **格式保证正确**：模型按函数签名返回，不会漏字段
- ✅ **降低复杂度**：模型专注识别，工具负责计算（如日期转换）
- ✅ **易于扩展**：新增工具函数即可增强能力

---

### 3.3 【长期】构建 OCR Skill 引擎（1-2月）

#### 3.3.1 架构设计

```
┌─────────────────────────────────────────────────────┐
│              OCR Skill Engine                       │
├─────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────┐         │
│  │ Skill    │  │ Skill    │  │ Skill    │         │
│  │ Registry │  │ Executor │  │ Monitor  │         │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘         │
│       │             │             │                │
│  ┌────▼──────────────▼─────────────▼────┐          │
│  │         Skill Definition DSL         │          │
│  │  (YAML/JSON 定义技能规范)             │          │
│  └──────────────────────────────────────┘          │
│                                                    │
│  ┌──────────────────────────────────────────┐     │
│  │  Core Capabilities                       │     │
│  │  • Prompt Template Management            │     │
│  │  • Output Validation & Retry             │     │
│  │  • Function Calling Orchestration        │     │
│  │  • Few-Shot Example Selection            │     │
│  │  • Error Pattern Learning                │     │
│  └──────────────────────────────────────────┘     │
└─────────────────────────────────────────────────────┘
```

#### 3.3.2 Skill 定义规范

```yaml
# skills/passport_ocr_v2.yaml
skill:
  metadata:
    name: passport_ocr
    version: "2.0"
    description: "护照OCR识别技能"
    author: zhangtj-a
    created_at: 2026-07-11
    
  model_config:
    provider: tencent
    model: hy-vision-2.0-instruct
    temperature: 0.1  # 降低随机性
    max_tokens: 1000
    
  input_schema:
    type: object
    properties:
      image_base64:
        type: string
        description: "证件图片base64编码"
      document_subtype:
        type: string
        enum: [chinese, american, british, ...]
        description: "护照子类型（可选，用于优化识别）"
        
  prompt_template: |
    你是一个专业的护照OCR识别专家。
    
    任务：识别护照图片并提取结构化信息
    
    要求：
    1. 调用 extract_passport_info 函数返回结果
    2. 阿拉伯数字和英文字母原样提取，严禁翻译
    3. 如果某个字段看不清，填空字符串 ""
    
    图片：{{image}}
    
  tools:
    - name: extract_passport_info
      signature: |
        function extract_passport_info(
          idNumber: string,      // 护照号
          name_en: string,       // 英文姓名
          nationality: string,   // 国籍
          dateOfBirth: string,   // yyyy-MM-dd
          validUntil: string,    // yyyy-MM-dd
          gender: "男"|"女",
          faceBox?: {x:number, y:number, width:number, height:number}
        ): PassportData
      
    - name: hijri_to_gregorian
      signature: |
        function hijri_to_gregorian(
          year: number,
          month: number,
          day: number
        ): string  // yyyy-MM-dd
        
  output_schema:
    type: PassportData
    required_fields: [idNumber, name_en, nationality, dateOfBirth]
    validation_rules:
      idNumber: "^[A-Z0-9]{6,15}$"
      dateOfBirth: "^\\d{4}-\\d{2}-\\d{2}$"
      gender: "^(男|女)$"
      
  few_shot_examples:
    - description: "中国护照示例"
      image_ref: "examples/chinese_passport_sample.jpg"
      expected_output: {...}
      
    - description: "美国护照示例"
      image_ref: "examples/us_passport_sample.jpg"
      expected_output: {...}
      
  error_handling:
    retry_on_validation_failure: true
    max_retries: 2
    fallback_strategy: "human_review"  # 最终失败转人工审核
    
  monitoring:
    metrics:
      - success_rate
      - avg_response_time
      - validation_error_rate
    alerts:
      - condition: "success_rate < 0.9"
        action: "notify_team"
```

#### 3.3.3 Skill 执行引擎

```java
@Component
public class SkillEngine {
    
    @Autowired
    private SkillRegistry registry;
    
    @Autowired
    private PromptRenderer renderer;
    
    @Autowired
    private OutputValidator validator;
    
    @Autowired
    private ExampleSelector exampleSelector;
    
    /**
     * 执行指定的 OCR Skill
     */
    public <T> T execute(String skillName, Map<String, Object> inputs, Class<T> outputType) {
        // 1. 加载 Skill 定义
        SkillDefinition skill = registry.get(skillName);
        
        // 2. 选择 Few-Shot 示例（基于图片特征）
        List<Example> examples = exampleSelector.select(skill, inputs);
        
        // 3. 渲染 Prompt（含示例）
        String prompt = renderer.render(skill.getPromptTemplate(), Map.of(
            "image", inputs.get("image_base64"),
            "examples", examples
        ));
        
        // 4. 执行重试逻辑
        return RetryTemplate.execute(() -> {
            // 5. 调用模型（支持 Function Calling）
            String response = modelClient.recognizeWithTools(
                inputs.get("image_base64"),
                prompt,
                skill.getTools()
            );
            
            // 6. 验证输出
            ValidationResult validation = validator.validate(response, skill.getOutputSchema());
            if (!validation.isValid()) {
                throw new ValidationException(validation.getErrors());
            }
            
            // 7. 反序列化为目标类型
            return objectMapper.readValue(response, outputType);
            
        }, skill.getErrorHandling().getMaxRetries());
    }
}
```

#### 3.3.4 使用示例

```java
@Service
public class PassportOcrService {
    
    @Autowired
    private SkillEngine skillEngine;
    
    public IdCardOcrResult recognizePassport(String imageBase64) {
        // 一行代码搞定！
        return skillEngine.execute(
            "passport_ocr",  // Skill 名称
            Map.of("image_base64", imageBase64),
            IdCardOcrResult.class
        );
    }
}
```

**收益**：
- ✅ **零样板代码**：业务层只需声明用哪个 Skill
- ✅ **热更新**：修改 YAML 配置立即生效
- ✅ **可观测**：统一的监控指标和告警
- ✅ **持续进化**：错误案例自动加入 Few-Shot 示例库

---

## 四、成本优化方案

### 4.1 Token 消耗对比

| 方案 | 单次请求 Token | 日均 1000 次成本 | 月度成本 |
|------|---------------|-----------------|---------|
| **当前（硬编码 Prompt）** | ~1200 tokens | ¥180 | ¥5,400 |
| **优化后（模板化 + 缓存）** | ~800 tokens | ¥120 | ¥3,600 |
| **Function Calling** | ~600 tokens | ¥90 | ¥2,700 |
| **引入缓存（30%命中率）** | ~560 tokens | ¥84 | ¥2,520 |

**节省比例**：约 **53%**

### 4.2 缓存策略

```java
@Component
public class OcrResultCache {
    
    private final Cache<String, IdCardOcrResult> cache = Caffeine.newBuilder()
        .maximumSize(10000)
        .expireAfterWrite(Duration.ofDays(7))
        .build();
    
    /**
     * 基于图片哈希的缓存
     */
    public IdCardOcrResult getOrCompute(String imageBase64, Supplier<IdCardOcrResult> computer) {
        String imageHash = DigestUtils.sha256Hex(imageBase64.getBytes());
        
        return cache.get(imageHash, key -> {
            log.info("OCR 缓存命中: {}", key);
            return computer.get();
        });
    }
}
```

**适用场景**：
- 同一证件多次上传（如工人信息补全）
- 批量导入时的去重检测

---

## 五、实施路线图

### Phase 1：基础优化（1-2周）
- [ ] Prompt 外部化为 YAML 配置
- [ ] 实现输出验证器
- [ ] 增加自动重试机制
- [ ] 建立监控指标（成功率、响应时长）

**预期收益**：
- 稳定性提升 20%
- 维护效率提升 50%

---

### Phase 2：Function Calling（2-4周）
- [ ] 改造腾讯客户端支持工具调用
- [ ] 定义核心工具函数（提取信息、日期转换）
- [ ] 迁移护照和 Iqama 策略
- [ ] A/B 测试对比效果

**预期收益**：
- 输出格式错误率降低 80%
- Token 成本降低 30%

---

### Phase 3：Skill 引擎（1-2月）
- [ ] 设计 Skill DSL 规范
- [ ] 实现 Skill 注册表和渲染引擎
- [ ] 集成 Few-Shot 示例选择
- [ ] 建立错误案例学习闭环
- [ ] 开发管理后台（可视化配置 Skills）

**预期收益**：
- 新增证件类型开发时间从 3 天降至 2 小时
- 整体识别准确率提升至 95%+

---

## 六、风险评估

| 风险项 | 概率 | 影响 | 缓解措施 |
|--------|------|------|---------|
| 腾讯云 API 限流 | 中 | 高 | 实现请求队列 + 降级策略 |
| Function Calling 兼容性 | 低 | 中 | 保留传统方式作为兜底 |
| Skill 学习曲线 | 中 | 低 | 编写详细文档 + 培训 |
| 缓存一致性问题 | 低 | 低 | 设置合理 TTL + 手动刷新接口 |

---

## 七、总结与建议

### 7.1 核心观点

1. **AI 模型的 Skill 不是 Prompt，而是标准化的能力封装**
   - Prompt 是临时的指令
   - Skill 是可复用、可验证、可进化的能力单元

2. **从"信任模型"到"验证模型"**
   - 不要假设模型每次都输出正确
   - 建立多层防御：格式校验 → 逻辑校验 → 人工审核

3. **数据驱动持续优化**
   - 收集错误案例 → 加入 Few-Shot 示例 → 监控效果提升
   - 形成闭环而非一次性开发

### 7.2 立即行动

**本周可做**：
1. 将两个策略的 Prompt 提取为 YAML 配置
2. 增加必填字段校验和正则验证
3. 实现简单的重试机制（最多 2 次）

**本月可做**：
1. 调研腾讯云 Function Calling 支持情况
2. 设计工具函数接口
3. 搭建监控看板（Grafana + Prometheus）

**本季度可做**：
1. 构建完整的 Skill 引擎
2. 开发管理后台
3. 建立错误案例学习机制

---

**附录**：
- [腾讯云 MaaS 文档](https://cloud.tencent.com/document/product/1774)
- [Function Calling 最佳实践](https://platform.openai.com/docs/guides/function-calling)
- [Prompt Engineering 指南](https://www.promptingguide.ai/)
