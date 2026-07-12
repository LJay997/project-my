# 腾讯多模态理解 OCR 技术选型说明

## 一、官方文档参考

### 1. 腾讯云 MaaS TokenHub 平台
- **平台地址**: https://tokenhub.tencentmaas.com/
- **API 端点**: `https://tokenhub.tencentmaas.com/v1/chat/completions`
- **兼容协议**: OpenAI Chat Completions API 格式

### 2. 使用的模型
- **初始模型**: `youtu-vita`（已注释）
- **当前模型**: `hy-vision-2.0-instruct`（混元视觉理解模型 2.0 指令版）

### 3. 调用方式
采用 **OpenAI 兼容格式**，支持多模态输入（图片 + 文本提示词）：

```json
{
  "model": "hy-vision-2.0-instruct",
  "messages": [
    {
      "role": "user",
      "content": [
        {
          "type": "image_url",
          "image_url": {
            "url": "data:image/jpeg;base64,{base64编码}"
          }
        },
        {
          "type": "text",
          "text": "{自定义提示词}"
        }
      ]
    }
  ],
  "stream": false
}
```

---

## 二、技术选型原因

### 1. 为什么选择腾讯而非其他厂商？

#### ✅ 优势分析

| 维度 | 腾讯 MaaS | 阿里云 OCR | 百度 OCR | 自建模型 |
|------|-----------|------------|----------|---------|
| **多语言支持** | ⭐⭐⭐⭐⭐ 天然支持各国护照 | ⭐⭐⭐ 主要针对中文 | ⭐⭐⭐ 主要针对中文 | 需大量训练数据 |
| **灵活性** | ⭐⭐⭐⭐⭐ Prompt 驱动，可定制提取字段 | ⭐⭐ 固定模板 | ⭐⭐ 固定模板 | 开发周期长 |
| **海外证件** | ⭐⭐⭐⭐⭐ 通用视觉理解能力强 | ⭐⭐ 专注国内身份证 | ⭐⭐ 专注国内身份证 | 需专门优化 |
| **集成成本** | ⭐⭐⭐⭐ OpenAI 兼容接口 | ⭐⭐⭐ 专有 SDK | ⭐⭐⭐ 专有 SDK | ⭐ 极高 |
| **准确率** | ⭐⭐⭐⭐ 大模型泛化能力强 | ⭐⭐⭐⭐ 模板匹配准确 | ⭐⭐⭐⭐ 模板匹配准确 | 取决于训练质量 |
| **维护成本** | ⭐⭐⭐⭐ 无需维护模型 | ⭐⭐⭐ 依赖厂商更新 | ⭐⭐⭐ 依赖厂商更新 | ⭐⭐⭐⭐⭐ 完全自主 |

#### 核心决策因素

1. **业务场景特殊性**
   - 需要识别**多国护照**（中国、美国、英国、加拿大、澳大利亚、日本、韩国、申根国家等）
   - 传统 OCR 厂商主要优化**国内身份证**，对海外证件支持有限
   - 腾讯多模态大模型具备**通用视觉理解能力**，不依赖固定模板

2. **字段提取的灵活性**
   - 护照格式因国家而异，字段位置不固定
   - 通过 **Prompt 工程**可以精确控制输出格式（JSON）
   - 支持**原文原样提取**，禁止翻译，符合国际化需求

3. **快速迭代能力**
   - 新增证件类型只需调整 Prompt，无需重新训练模型
   - 策略模式 + Prompt 配置，扩展成本低

4. **企业级稳定性**
   - 腾讯云提供 SLA 保障
   - TokenHub 平台统一管理 API Key 和配额
   - 支持高并发请求

### 2. 为什么不使用传统 OCR SDK？

#### 传统 OCR 的局限性

```java
// 传统方式：固定模板匹配
IdCardOcrResult result = tencentOcrClient.recognizePassport(imageBase64);
// ❌ 问题：只能识别预设的护照模板，新国家护照无法识别
```

#### 多模态大模型的优势

```java
// 多模态方式：Prompt 驱动
String prompt = "识别护照图片，提取姓名、护照号、国籍...以 JSON 返回";
String response = tencentOcrClient.recognize(imageBase64, prompt);
// ✅ 优势：通用性强，支持任意国家护照，只需调整 Prompt
```

---

## 三、模型选择依据

### 1. 从 `youtu-vita` 切换到 `hy-vision-2.0-instruct` 的原因

| 对比项 | youtu-vita | hy-vision-2.0-instruct |
|--------|------------|------------------------|
| **发布时间** | 早期版本 | 2024 年发布（较新） |
| **视觉理解能力** | 基础图文理解 | 强化视觉细节识别 |
| **指令遵循** | 一般 | 针对指令微调，遵循度更高 |
| **OCR 精度** | 良好 | 优秀（专门优化文字识别） |
| **JSON 输出稳定性** | 偶尔格式错误 | 更稳定 |

### 2. hy-vision-2.0-instruct 的核心特性

- **混元视觉大模型 2.0**：腾讯最新一代视觉理解模型
- **指令微调版（instruct）**：针对结构化输出任务优化
- **支持千分比坐标**：可用于头像定位裁剪（faceBox）
- **多语言原生支持**：无需额外翻译层

### 3. 实际效果验证

根据代码中的 Prompt 设计：

```java
private static final String OCR_PROMPT =
    "## 任务：识别护照证件图片...\n"
    + "### 硬性规则\n"
    + "1. 固定输出JSON键：[\"name\",\"name_en\",\"idNumber\",...]\n"
    + "2. 文本规则：**原文原样提取，严禁翻译**\n"
    + "3. 过滤规则：镭射防伪图案、花纹、水印全部忽略\n"
    + "4. 分区精准定位（按位置找文字）...\n"
    + "### 输出约束\n"
    + "只返回标准JSON，无任何多余解释...";
```

**关键能力要求**：
- ✅ 精确遵循 JSON Schema
- ✅ 理解复杂的空间定位指令
- ✅ 处理多语言文字混合场景
- ✅ 过滤干扰元素（水印、防伪图案）

`hy-vision-2.0-instruct` 在这些方面表现优于早期的 `youtu-vita`。

---

## 四、架构设计亮点

### 1. 策略模式 + 注册表

```java
@Component
public class OcrStrategyRegistry {
    @Autowired
    private List<OcrStrategy<?>> strategies; // Spring 自动注入
    
    public <T> OcrStrategy<T> getStrategy(String type) {
        return strategies.stream()
            .filter(s -> s.supportedType().equals(type))
            .findFirst()
            .map(s -> (OcrStrategy<T>) s)
            .orElseThrow(() -> new IllegalArgumentException("不支持的类型: " + type));
    }
}
```

**优势**：
- 新增证件类型无需修改现有代码（开闭原则）
- 每个策略独立维护自己的 Prompt
- 易于单元测试和 A/B 测试

### 2. 统一的多模态客户端

```java
@Component
public class TencentOcrClient {
    private static final String API_URL = "https://tokenhub.tencentmaas.com/v1/chat/completions";
    private static final String MODEL = "hy-vision-2.0-instruct";
    
    public String recognize(String imageBase64, String prompt) {
        // 构建 OpenAI 兼容格式请求
        // 发送 HTTP POST
        // 解析 choices[0].message.content
    }
}
```

**优势**：
- 屏蔽底层 API 差异
- 集中管理认证和超时配置
- 便于切换模型或厂商

### 3. 容错与降级

```java
@PostConstruct
public void init() {
    if (apiKey == null || apiKey.isEmpty()) {
        log.warn("腾讯 MaaS API Key 未配置，OCR 服务不可用");
    } else {
        log.info("腾讯 MaaS OCR 客户端初始化完成");
    }
}
```

- 启动时检查配置，避免运行时才发现不可用
- 异常时抛出明确错误信息，便于排查

---

## 五、成本与性能考量

### 1. 计费模式
- 按 **Token 数量**计费（输入图片 + 输出文本）
- 相比传统 OCR 按次计费，更适合复杂场景

### 2. 性能指标
- **连接超时**: 30 秒
- **读取超时**: 60 秒
- **典型响应时间**: 2-5 秒（取决于图片复杂度）

### 3. 优化建议
- 前端压缩图片至合理尺寸（建议 ≤ 2MB）
- 缓存常见证件的识别结果（相同图片哈希）
- 异步处理批量导入场景

---

## 六、未来演进方向

### 1. 短期优化
- [ ] 增加重试机制（网络抖动容错）
- [ ] 接入监控告警（识别失败率、响应时长）
- [ ] Prompt 版本管理（A/B 测试不同提示词效果）

### 2. 中期规划
- [ ] 支持更多证件类型（驾驶证、工作许可证等）
- [ ] 引入置信度评分（低置信度转人工审核）
- [ ] 建立错题本机制（持续优化 Prompt）

### 3. 长期愿景
- [ ] 评估自研小模型替代方案（降低长期成本）
- [ ] 探索端侧 OCR（隐私敏感场景）
- [ ] 多模型融合（腾讯 + 阿里 + 百度投票机制）

---

## 七、参考资料

1. **腾讯云 MaaS 官方文档**: https://cloud.tencent.com/document/product/1774
2. **混元视觉大模型介绍**: https://hunyuan.tencent.com/vision
3. **OpenAI Chat Completions API**: https://platform.openai.com/docs/api-reference/chat
4. **项目设计文档**: `organization-service/docs/superpowers/specs/2026-06-04-overseas-ocr-design.md`

---

**文档版本**: v1.0  
**最后更新**: 2026-07-11  
**维护人**: zhangtj-a
