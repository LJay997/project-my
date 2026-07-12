# Saudi Iqama OCR Skill 配置

## YAML 配置文件

```yaml
# config/ocr-skills/saudi_iqama.yaml
skill:
  metadata:
    name: saudi_iqama_ocr
    version: "1.0"
    description: "沙特阿拉伯外籍居民居留卡（IQAMA）OCR识别技能"
    author: zhangtj-a
    created_at: 2026-07-11
    
  model_config:
    provider: tencent
    model: hy-vision-2.0-instruct
    temperature: 0.1  # 降低随机性，提高稳定性
    max_tokens: 1500  # Iqama 字段较多，需要更多 token
    
  input_schema:
    type: object
    properties:
      image_base64:
        type: string
        description: "Iqama证件图片base64编码（不含data:image前缀）"
        
  system_prompt: |
    你是一个专业的沙特阿拉伯外籍居民居留卡（IQAMA）OCR识别专家。
    
  prompt_template: |
    ## 任务：识别沙特阿拉伯内政部外籍居民居留卡（IQAMA），基于原版证件图片提取结构化信息。
    
    ### 硬性规则
    1. 固定输出JSON键：["name","name_en","idNumber","nationality","dateOfBirth","birthPlace","occupation","employer","faceBox"]，键名一字不差
    2. 文本规则：**阿拉伯语字符、阿拉伯数字原样原字提取，严禁翻译、转英文/中文（nationality字段除外，需翻译为中文国籍名）；英文原样提取；识别不清才填空字符串，禁止主动空值**
    3. 过滤规则：镭射防伪图案、花纹、防伪线条全部忽略，仅识别印刷英文、阿拉伯文、阿拉伯数字
    4. 分区精准定位（按位置找文字）：
       - 【name】：阿拉伯语姓名原文，通常位于证件中部偏上
       - 【name_en】：证件右上角印刷的英文人名，原样提取
       - 【idNumber】：证件左下角条码下方数字（两处号码一致，优先取清晰的），只输出纯数字
       - 【nationality】：将国籍栏阿拉伯原文翻译为中文国籍名输出，例如沙特阿拉伯、埃及、印度、巴基斯坦、菲律宾、孟加拉国、也门、约旦、叙利亚、苏丹等
       - 【dateOfBirth】：将证件上的伊斯兰回历日期（Hijri）换算为公历日期，格式yyyy-MM-dd（如1998-09-11），由AI直接换算输出
       - 【birthPlace】：出生地对应栏位阿拉伯原文，通常位于证件左侧中部
       - 【occupation】：持证人职业栏阿拉伯原文，通常位于出生地下方或附近
       - 【employer】：证件最下方担保人/劳务公司所属公司阿拉伯全称，通常伴随「صاحب العمل」或类似标识
       - 【faceBox】：证件照片中的持证人头像区域，输出为对象 {"x": 整数, "y": 整数, "width": 整数, "height": 整数}，使用千分比坐标（0-1000），x为头像左边界，y为头像上边界，width为头像宽度，height为头像高度；若无法定位头像则输出空对象 {}
    
    ### 输出约束
    只返回标准JSON，无任何多余解释、注释、markdown代码块标记；能看清文字必须填入，杜绝无理由空字段。
    
    图片：{{image}}
    
  tools:
    - name: extract_iqama_info
      signature: |
        function extract_iqama_info(
          name: string,              // 阿拉伯语姓名原文
          name_en: string,           // 英文姓名
          idNumber: string,          // Iqama号码（纯数字）
          nationality: string,       // 中文国籍名
          dateOfBirth: string,       // 公历出生日期 yyyy-MM-dd
          birthPlace: string,        // 出生地阿拉伯原文
          occupation: string,        // 职业阿拉伯原文
          employer: string,          // 雇主/担保公司阿拉伯全称
          faceBox?: {x:number, y:number, width:number, height:number}  // 头像坐标
        ): IqamaData
        
    - name: hijri_to_gregorian
      signature: |
        function hijri_to_gregorian(
          year: number,
          month: number,
          day: number
        ): string  // yyyy-MM-dd
        
  output_schema:
    json_schema_ref: "#/definitions/IqamaSchema"
    required_fields: 
      - idNumber
      - name
      - name_en
      - nationality
      - dateOfBirth
      
  validation_rules:
    idNumber:
      pattern: "^\\d{10}$"  # Iqama号码通常为10位纯数字
      description: "Iqama号码必须是10位数字"
      
    dateOfBirth:
      pattern: "^\\d{4}-\\d{2}-\\d{2}$"
      description: "日期格式必须为yyyy-MM-dd"
      
    nationality:
      enum: 
        - "沙特阿拉伯"
        - "埃及"
        - "印度"
        - "巴基斯坦"
        - "菲律宾"
        - "孟加拉国"
        - "也门"
        - "约旦"
        - "叙利亚"
        - "苏丹"
        - "其他"
      description: "国籍必须是已知的中文国家名"
      
    faceBox:
      type: object
      properties:
        x: {type: integer, minimum: 0, maximum: 1000}
        y: {type: integer, minimum: 0, maximum: 1000}
        width: {type: integer, minimum: 0, maximum: 1000}
        height: {type: integer, minimum: 0, maximum: 1000}
        
  few_shot_examples:
    - description: "标准沙特Iqama示例"
      image_ref: "examples/saudi_iqama_sample_1.jpg"
      expected_output:
        name: "محمد أحمد العلي"
        name_en: "MOHAMMAD AHMED ALI"
        idNumber: "1234567890"
        nationality: "巴基斯坦"
        dateOfBirth: "1990-05-15"
        birthPlace: "كراتشي"
        occupation: "عامل بناء"
        employer: "شركة المقاولات السعودية"
        faceBox:
          x: 750
          y: 200
          width: 180
          height: 220
          
    - description: "不同国籍Iqama示例"
      image_ref: "examples/saudi_iqama_sample_2.jpg"
      expected_output:
        name: "राज कुमार शर्मा"
        name_en: "RAJ KUMAR SHARMA"
        idNumber: "2345678901"
        nationality: "印度"
        dateOfBirth: "1985-08-20"
        birthPlace: "दिल्ली"
        occupation: "كهربائي"
        employer: "شركة الكهرباء الوطنية"
        faceBox:
          x: 760
          y: 210
          width: 175
          height: 215
          
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
    "IqamaSchema": {
      "type": "object",
      "required": [
        "idNumber",
        "name",
        "name_en",
        "nationality",
        "dateOfBirth"
      ],
      "properties": {
        "name": {
          "type": "string",
          "description": "阿拉伯语姓名原文",
          "minLength": 1
        },
        "name_en": {
          "type": "string",
          "description": "英文姓名",
          "pattern": "^[A-Z\\s]+$",
          "minLength": 1
        },
        "idNumber": {
          "type": "string",
          "description": "Iqama号码（10位纯数字）",
          "pattern": "^\\d{10}$"
        },
        "nationality": {
          "type": "string",
          "description": "中文国籍名",
          "enum": [
            "沙特阿拉伯",
            "埃及",
            "印度",
            "巴基斯坦",
            "菲律宾",
            "孟加拉国",
            "也门",
            "约旦",
            "叙利亚",
            "苏丹",
            "其他"
          ]
        },
        "dateOfBirth": {
          "type": "string",
          "description": "公历出生日期",
          "pattern": "^\\d{4}-\\d{2}-\\d{2}$"
        },
        "birthPlace": {
          "type": "string",
          "description": "出生地（阿拉伯原文）"
        },
        "occupation": {
          "type": "string",
          "description": "职业（阿拉伯原文）"
        },
        "employer": {
          "type": "string",
          "description": "雇主/担保公司（阿拉伯全称）"
        },
        "faceBox": {
          "type": ["object", "null"],
          "properties": {
            "x": {
              "type": "integer",
              "minimum": 0,
              "maximum": 1000,
              "description": "头像左边界（千分比坐标）"
            },
            "y": {
              "type": "integer",
              "minimum": 0,
              "maximum": 1000,
              "description": "头像上边界（千分比坐标）"
            },
            "width": {
              "type": "integer",
              "minimum": 0,
              "maximum": 1000,
              "description": "头像宽度（千分比坐标）"
            },
            "height": {
              "type": "integer",
              "minimum": 0,
              "maximum": 1000,
              "description": "头像高度（千分比坐标）"
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

### 1. 与护照 Skill 的差异

| 对比项 | 护照 Skill | Iqama Skill |
|--------|-----------|-------------|
| **证件类型** | 国际护照 | 沙特居留卡 |
| **语言复杂度** | 单一语言为主 | 阿拉伯语 + 英文混合 |
| **特殊处理** | 无 | 回历转公历、国籍翻译 |
| **字段数量** | 7个 | 9个 |
| **Token消耗** | ~1200 | ~1500 |
| **验证规则** | 相对简单 | 更复杂（国籍枚举、数字格式） |

### 2. 核心难点

1. **多语言混合识别**
   - 阿拉伯语姓名原文保留
   - 英文姓名原样提取
   - 国籍需要从阿拉伯语翻译为中文

2. **日期转换**
   - 伊斯兰回历（Hijri）→ 公历（Gregorian）
   - 需要通过工具函数或模型内置能力完成

3. **字段完整性要求高**
   - 雇主信息（employer）对劳务管理至关重要
   - 职业信息（occupation）用于工种匹配

### 3. 优化建议

- ✅ **启用 Function Calling**：将日期转换和字段提取分离，降低模型负担
- ✅ **增加 Few-Shot 示例**：覆盖不同国籍、不同版式的 Iqama
- ✅ **设置字段填充率监控**：确保关键字段（employer、occupation）不被遗漏
- ✅ **建立错误案例库**：收集识别失败的案例，持续优化 Prompt

---

## 使用示例

```java
@Service
public class SaudiIqamaOcrService {
    
    @Autowired
    private SkillEngine skillEngine;
    
    public IdCardOcrResult recognizeIqama(String imageBase64) {
        return skillEngine.execute(
            "saudi_iqama_ocr",  // Skill 名称
            Map.of("image_base64", imageBase64),
            IdCardOcrResult.class
        );
    }
}
```
