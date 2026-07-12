# 护照 OCR Skill 配置

## YAML 配置文件

```yaml
# config/ocr-skills/passport.yaml
skill:
  metadata:
    name: passport_ocr
    version: "1.0"
    description: "国际护照OCR识别技能，支持各国护照（中国、美国、英国、加拿大、澳大利亚、日本、韩国、申根国家等）"
    author: zhangtj-a
    created_at: 2026-07-11
    
  model_config:
    provider: tencent
    model: hy-vision-2.0-instruct
    temperature: 0.1  # 降低随机性，提高稳定性
    max_tokens: 1200
    
  input_schema:
    type: object
    properties:
      image_base64:
        type: string
        description: "护照图片base64编码（不含data:image前缀）"
        
  system_prompt: |
    你是一个专业的国际护照OCR识别专家，能够识别各国护照并提取结构化信息。
    
  prompt_template: |
    ## 任务：识别护照证件图片，基于原版证件图片提取结构化信息。支持各国护照（中国、美国、英国、加拿大、澳大利亚、日本、韩国、申根国家等）。
    
    ### 硬性规则
    1. 固定输出JSON键：["name","name_en","idNumber","nationality","dateOfBirth","gender","validFrom","validUntil","faceBox"]，键名一字不差
    2. 文本规则：**原文原样提取，严禁翻译**；英文、数字、本地语言原样提取；识别不清才填空字符串，禁止主动空值
    3. 过滤规则：镭射防伪图案、花纹、防伪线条、水印全部忽略，仅识别印刷文字
    4. 分区精准定位（按位置找文字）：
       - 【name】：持证人姓名，通常位于护照资料页中部，取全名原文
       - 【name_en】：持证人英文拼音/拉丁字母姓名，可从机读区（MRZ）第一行或资料页英文姓名栏提取，原样输出
       - 【idNumber】：护照号码，通常位于资料页右上角或左上角，取字母+数字组合，去除空格
       - 【nationality】：国籍，将护照上的国籍英文缩写或全称翻译为中文国籍名输出，例如中国、美国、英国、加拿大、澳大利亚、日本、韩国、法国、德国、意大利、西班牙、新加坡、马来西亚等
       - 【dateOfBirth】：出生日期，格式yyyy-MM-dd（如1990-01-15），若原证件日期格式不同请自动转换
       - 【gender】：性别，从护照性别栏或MRZ区识别（M/F/Male/Female/男/女等），统一输出为中文"男"或"女"
       - 【validFrom】：有效期开始日期（签发日期/Date of issue），格式yyyy-MM-dd，若原证件日期格式不同请自动转换
       - 【validUntil】：有效期结束日期（到期日期/Date of expiry），格式yyyy-MM-dd，若原证件日期格式不同请自动转换
       - 【faceBox】：证件照片中的持证人头像区域，输出为对象 {"x": 整数, "y": 整数, "width": 整数, "height": 整数}，使用千分比坐标（0-1000），x为头像左边界，y为头像上边界，width为头像宽度，height为头像高度；若无法定位头像则输出空对象 {}
    
    ### 输出约束
    只返回标准JSON，无任何多余解释、注释、markdown代码块标记；能看清文字必须填入，杜绝无理由空字段。
    
    图片：{{image}}
    
  tools:
    - name: extract_passport_info
      signature: |
        function extract_passport_info(
          name: string,              // 持证人姓名原文
          name_en: string,           // 英文拼音/拉丁字母姓名
          idNumber: string,          // 护照号码（字母+数字）
          nationality: string,       // 中文国籍名
          dateOfBirth: string,       // 出生日期 yyyy-MM-dd
          gender: "男"|"女",         // 性别
          validFrom: string,         // 有效期开始日期 yyyy-MM-dd
          validUntil: string,        // 有效期结束日期 yyyy-MM-dd
          faceBox?: {x:number, y:number, width:number, height:number}  // 头像坐标
        ): PassportData
        
  output_schema:
    json_schema_ref: "#/definitions/PassportSchema"
    required_fields: 
      - idNumber
      - name
      - name_en
      - nationality
      - dateOfBirth
      - gender
      
  validation_rules:
    idNumber:
      pattern: "^[A-Z0-9]{6,15}$"  # 护照号通常为6-15位字母数字组合
      description: "护照号码必须是6-15位字母或数字"
      
    name_en:
      pattern: "^[A-Za-z\\s\\.\\-']+$"
      description: "英文姓名只能包含字母、空格、点、连字符和撇号"
      
    dateOfBirth:
      pattern: "^\\d{4}-\\d{2}-\\d{2}$"
      description: "日期格式必须为yyyy-MM-dd"
      
    validFrom:
      pattern: "^\\d{4}-\\d{2}-\\d{2}$"
      description: "签发日期格式必须为yyyy-MM-dd"
      
    validUntil:
      pattern: "^\\d{4}-\\d{2}-\\d{2}$"
      description: "到期日期格式必须为yyyy-MM-dd"
      
    gender:
      enum: ["男", "女"]
      description: "性别必须是'男'或'女'"
      
    nationality:
      type: string
      minLength: 1
      description: "国籍必须是中文国家名"
      
    faceBox:
      type: object
      properties:
        x: {type: integer, minimum: 0, maximum: 1000}
        y: {type: integer, minimum: 0, maximum: 1000}
        width: {type: integer, minimum: 0, maximum: 1000}
        height: {type: integer, minimum: 0, maximum: 1000}
        
  few_shot_examples:
    - description: "中国护照示例"
      image_ref: "examples/chinese_passport_sample.jpg"
      expected_output:
        name: "张三"
        name_en: "ZHANG SAN"
        idNumber: "E12345678"
        nationality: "中国"
        dateOfBirth: "1990-05-15"
        gender: "男"
        validFrom: "2020-01-10"
        validUntil: "2030-01-09"
        faceBox:
          x: 720
          y: 180
          width: 200
          height: 240
          
    - description: "美国护照示例"
      image_ref: "examples/us_passport_sample.jpg"
      expected_output:
        name: "JOHN MICHAEL SMITH"
        name_en: "JOHN MICHAEL SMITH"
        idNumber: "123456789"
        nationality: "美国"
        dateOfBirth: "1985-08-20"
        gender: "男"
        validFrom: "2019-03-15"
        validUntil: "2029-03-14"
        faceBox:
          x: 750
          y: 200
          width: 180
          height: 220
          
    - description: "日本护照示例"
      image_ref: "examples/japanese_passport_sample.jpg"
      expected_output:
        name: "田中太郎"
        name_en: "TANAKA TARO"
        idNumber: "TK1234567"
        nationality: "日本"
        dateOfBirth: "1992-11-03"
        gender: "男"
        validFrom: "2021-06-01"
        validUntil: "2026-05-31"
        faceBox:
          x: 740
          y: 190
          width: 190
          height: 230
          
  error_handling:
    retry_on_validation_failure: true
    max_retries: 2
    retry_delay_ms: 2000  # 重试间隔2秒
    fallback_strategy: "human_review"  # 最终失败转人工审核
    
  monitoring:
    metrics:
      - success_rate
      - avg_response_time
      - validation_error_rate
      - field_completion_rate  # 字段填充率
    alerts:
      - condition: "success_rate < 0.9"
        action: "notify_team"
        channel: "dingtalk"
      - condition: "avg_response_time > 5000"
        action: "notify_team"
        channel: "dingtalk"
```

---

## JSON Schema 定义

```json
{
  "definitions": {
    "PassportSchema": {
      "type": "object",
      "required": [
        "idNumber",
        "name",
        "name_en",
        "nationality",
        "dateOfBirth",
        "gender"
      ],
      "properties": {
        "name": {
          "type": "string",
          "description": "持证人姓名原文",
          "minLength": 1
        },
        "name_en": {
          "type": "string",
          "description": "英文拼音/拉丁字母姓名",
          "pattern": "^[A-Za-z\\s\\.\\-']+$",
          "minLength": 1
        },
        "idNumber": {
          "type": "string",
          "description": "护照号码（6-15位字母数字组合）",
          "pattern": "^[A-Z0-9]{6,15}$"
        },
        "nationality": {
          "type": "string",
          "description": "中文国籍名",
          "minLength": 1
        },
        "dateOfBirth": {
          "type": "string",
          "description": "出生日期",
          "pattern": "^\\d{4}-\\d{2}-\\d{2}$"
        },
        "gender": {
          "type": "string",
          "description": "性别",
          "enum": ["男", "女"]
        },
        "validFrom": {
          "type": "string",
          "description": "有效期开始日期（签发日期）",
          "pattern": "^\\d{4}-\\d{2}-\\d{2}$"
        },
        "validUntil": {
          "type": "string",
          "description": "有效期结束日期（到期日期）",
          "pattern": "^\\d{4}-\\d{2}-\\d{2}$"
        },
        "faceBox": {
          "type": ["object", "null"],
          "description": "头像坐标（千分比坐标）",
          "properties": {
            "x": {
              "type": "integer",
              "minimum": 0,
              "maximum": 1000,
              "description": "头像左边界"
            },
            "y": {
              "type": "integer",
              "minimum": 0,
              "maximum": 1000,
              "description": "头像上边界"
            },
            "width": {
              "type": "integer",
              "minimum": 0,
              "maximum": 1000,
              "description": "头像宽度"
            },
            "height": {
              "type": "integer",
              "minimum": 0,
              "maximum": 1000,
              "description": "头像高度"
            }
          },
          "required": ["x", "y", "width", "height"]
        }
      },
      "additionalProperties": false
    }
  }
}
```

---

## 关键特性说明

### 1. 核心设计要点

| 特性 | 说明 |
|------|------|
| **多语言支持** | 支持中文、英文、日文、韩文等多国语言护照 |
| **原文保留** | 姓名字段原样提取，不进行翻译 |
| **国籍翻译** | 仅国籍字段需要从外文翻译为中文 |
| **日期标准化** | 所有日期统一转换为 yyyy-MM-dd 格式 |
| **性别标准化** | 多种表示方式（M/F/Male/Female/男/女）统一为"男"或"女" |

### 2. 验证规则亮点

- ✅ **护照号格式校验**：`^[A-Z0-9]{6,15}$` 覆盖各国护照号格式
- ✅ **英文姓名校验**：允许字母、空格、点、连字符、撇号（如 O'Brien）
- ✅ **日期格式校验**：确保所有日期字段符合 ISO 8601 标准
- ✅ **性别枚举校验**：严格限制为"男"或"女"
- ✅ **坐标范围校验**：faceBox 坐标必须在 0-1000 范围内

### 3. Few-Shot 示例覆盖

提供了 3 个典型示例：
1. **中国护照**：中文姓名 + 拼音
2. **美国护照**：纯英文姓名
3. **日本护照**：日文姓名 + 罗马音

这些示例帮助模型理解不同国家护照的格式差异。

### 4. 与 Iqama Skill 的对比

| 对比项 | 护照 Skill | Iqama Skill |
|--------|-----------|-------------|
| **证件类型** | 国际护照 | 沙特居留卡 |
| **语言复杂度** | 多语言混合 | 阿拉伯语 + 英文 |
| **特殊处理** | 性别标准化、日期转换 | 回历转公历、国籍翻译 |
| **字段数量** | 9个 | 9个 |
| **Token消耗** | ~1200 | ~1500 |
| **验证复杂度** | 中等 | 较高（枚举值更多） |

---

## 使用示例

```java
@Service
public class PassportOcrService {
    
    @Autowired
    private SkillEngine skillEngine;
    
    /**
     * 识别护照
     */
    public IdCardOcrResult recognizePassport(String imageBase64) {
        return skillEngine.execute(
            "passport_ocr",  // Skill 名称
            Map.of("image_base64", imageBase64),
            IdCardOcrResult.class
        );
    }
    
    /**
     * 批量识别护照（带缓存）
     */
    public List<IdCardOcrResult> batchRecognize(List<String> imageBase64List) {
        return imageBase64List.parallelStream()
            .map(this::recognizePassport)
            .collect(Collectors.toList());
    }
}
```

---

## 监控指标说明

### 关键指标

1. **success_rate**（成功率）
   - 目标：≥ 90%
   - 低于阈值时触发告警

2. **avg_response_time**（平均响应时间）
   - 目标：≤ 5秒
   - 超时可能影响用户体验

3. **validation_error_rate**（验证错误率）
   - 反映模型输出质量
   - 高错误率需要优化 Prompt 或增加 Few-Shot 示例

4. **field_completion_rate**（字段填充率）
   - 计算公式：已填充字段数 / 总字段数
   - 目标：≥ 95%
   - 低填充率说明模型识别能力不足

### 告警配置

```yaml
alerts:
  - condition: "success_rate < 0.9"
    action: "notify_team"
    channel: "dingtalk"
    severity: "warning"
    
  - condition: "avg_response_time > 5000"
    action: "notify_team"
    channel: "dingtalk"
    severity: "warning"
    
  - condition: "validation_error_rate > 0.15"
    action: "notify_team"
    channel: "dingtalk"
    severity: "critical"
```

---

## 扩展建议

### 短期优化（1-2周）

1. **增加更多 Few-Shot 示例**
   - 欧洲护照（法国、德国、意大利）
   - 亚洲护照（韩国、新加坡、马来西亚）
   - 中东护照（阿联酋、卡塔尔）

2. **优化验证规则**
   - 针对不同国家护照设置不同的护照号格式规则
   - 增加日期合理性校验（如 validFrom < validUntil）

3. **建立错误案例库**
   - 收集识别失败的案例
   - 分析失败原因（模糊、反光、角度问题等）
   - 针对性优化 Prompt

### 中期优化（1-2月）

1. **引入 Function Calling**
   - 将日期转换逻辑封装为工具函数
   - 提高输出格式的稳定性

2. **实现智能缓存**
   - 基于图片哈希缓存识别结果
   - 减少重复调用降低成本

3. **开发管理后台**
   - 可视化配置 Skill
   - 实时监控指标
   - 手动刷新缓存

### 长期优化（3-6月）

1. **构建自学习机制**
   - 人工审核结果自动加入 Few-Shot 示例库
   - 持续优化识别准确率

2. **多模型融合**
   - 腾讯 + 阿里 + 百度投票机制
   - 提高鲁棒性

3. **端侧 OCR 探索**
   - 隐私敏感场景使用本地模型
   - 降低网络依赖
