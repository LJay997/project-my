# 海外 OCR 接口设计

**日期**: 2026-06-04
**分支**: feature/overseas_ocr
**调用方**: worker-app-aggregator (Feign)

---

## 概述

为 organization-service 新增两个海外 OCR 接口，供 worker-app-aggregator 通过 Feign 调用：
- `POST /overseas/ocr/idCard` — 海外证件 OCR（护照、沙特 Iqama）
- `POST /overseas/ocr/bankCard` — 银行卡 OCR（SAB、Al-Rajhi）

采用**策略模式**设计，底层 OCR 最终使用**腾讯多模态理解接口**（目前尚未对接，先留接口，各策略实现暂为占位）。

---

## 包结构

所有代码收敛在 `com.glodon.glm.overseas` 父包下：

```
src/main/java/com/glodon/glm/overseas/
├── controller/
│   └── OverseasOcrController.java
├── service/
│   ├── IOverseasOcrService.java
│   ├── impl/
│   │   └── OverseasOcrServiceImpl.java
│   └── ocr/
│       ├── OcrStrategy.java              # 策略接口(泛型)
│       ├── OcrStrategyRegistry.java      # 策略注册表
│       ├── IdCardOcrResult.java          # 证件识别结果DTO
│       ├── BankCardOcrResult.java        # 银行卡识别结果DTO
│       ├── idcard/
│       │   ├── PassportOcrStrategy.java
│       │   └── SaudiIqamaOcrStrategy.java
│       └── bankcard/
│           ├── SabBankCardOcrStrategy.java
│           └── AlRajhiBankCardOcrStrategy.java
└── model/
    ├── OverseasIdCardOcrReq.java         # 证件OCR请求DTO
    └── OverseasBankCardOcrReq.java       # 银行卡OCR请求DTO
```

---

## 接口设计

### 接口一：海外证件 OCR

**POST /overseas/ocr/idCard**

请求体:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| imageBase64 | String | 是 | 图片 base64，不含 data:image/... 前缀 |
| idType | String | 是 | PASSPORT_NO / SAUDI_IQAMA |

返回: `Result<IdCardOcrResult>`

### 接口二：银行卡 OCR

**POST /overseas/ocr/bankCard**

请求体:
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| imageBase64 | String | 是 | 图片 base64，不含前缀 |
| bankType | String | 是 | SAB / AL_RAJHI |

返回: `Result<BankCardOcrResult>`

---

## 核心类设计

### 请求 DTO

```java
// model/OverseasIdCardOcrReq.java
public class OverseasIdCardOcrReq {
    @NotBlank
    private String imageBase64;
    @NotBlank
    private String idType;   // PASSPORT_NO / SAUDI_IQAMA
}

// model/OverseasBankCardOcrReq.java
public class OverseasBankCardOcrReq {
    @NotBlank
    private String imageBase64;
    @NotBlank
    private String bankType; // SAB / AL_RAJHI
}
```

使用 String 而非枚举存储 type，方便后续扩展新类型而不改动 DTO。

### 识别结果 DTO

```java
// service/ocr/IdCardOcrResult.java
public class IdCardOcrResult {
    private String idNumber;        // 护照号 或 Iqama 号
    private String name;            // 姓名
    private String nationality;     // 国籍
    private String dateOfBirth;     // 出生日期
    private String expiryDate;      // 有效期
    private Map<String, String> extraFields; // 兜底扩展字段
}

// service/ocr/BankCardOcrResult.java
public class BankCardOcrResult {
    private String cardNumber;      // 银行卡号
    private String bankName;        // 银行名称
    private String cardType;        // 卡类型
    private Map<String, String> extraFields; // 兜底扩展字段
}
```

`extraFields` 用于不同证件/银行卡的差异字段，避免频繁修改 DTO。

### 策略接口

```java
public interface OcrStrategy<T> {
    /** 该策略支持的 type 值，如 "PASSPORT_NO"、"SAB" */
    String supportedType();

    /** 执行 OCR 识别 */
    T ocr(String imageBase64);
}
```

泛型 `T` 支持不同类型的识别结果（`IdCardOcrResult` / `BankCardOcrResult`）。

### 策略注册表

```java
@Component
public class OcrStrategyRegistry {
    @Autowired
    private List<OcrStrategy<?>> strategies;

    @SuppressWarnings("unchecked")
    public <T> OcrStrategy<T> getStrategy(String type) {
        return strategies.stream()
            .filter(s -> s.supportedType().equals(type))
            .findFirst()
            .map(s -> (OcrStrategy<T>) s)
            .orElseThrow(() -> new IllegalArgumentException("不支持的类型: " + type));
    }
}
```

Spring 自动注入所有 `OcrStrategy` 实现，新增策略只需加 `@Component`。

### Service

```java
public interface IOverseasOcrService {
    IdCardOcrResult ocrIdCard(OverseasIdCardOcrReq req);
    BankCardOcrResult ocrBankCard(OverseasBankCardOcrReq req);
}

@Service
public class OverseasOcrServiceImpl implements IOverseasOcrService {
    @Autowired
    private OcrStrategyRegistry registry;

    @Override
    public IdCardOcrResult ocrIdCard(OverseasIdCardOcrReq req) {
        OcrStrategy<IdCardOcrResult> strategy = registry.getStrategy(req.getIdType());
        return strategy.ocr(req.getImageBase64());
    }

    @Override
    public BankCardOcrResult ocrBankCard(OverseasBankCardOcrReq req) {
        OcrStrategy<BankCardOcrResult> strategy = registry.getStrategy(req.getBankType());
        return strategy.ocr(req.getImageBase64());
    }
}
```

### Controller

```java
@RestController
@RequestMapping("overseas/ocr")
public class OverseasOcrController {

    @Autowired
    private IOverseasOcrService overseasOcrService;

    @PostMapping("/idCard")
    public Result<IdCardOcrResult> idCardOcr(@RequestBody @Valid OverseasIdCardOcrReq req) {
        return Result.success(overseasOcrService.ocrIdCard(req));
    }

    @PostMapping("/bankCard")
    public Result<BankCardOcrResult> bankCardOcr(@RequestBody @Valid OverseasBankCardOcrReq req) {
        return Result.success(overseasOcrService.ocrBankCard(req));
    }
}
```

---

## 数据流

```
Feign Client (worker-app-aggregator)
  │  POST /overseas/ocr/idCard
  ▼
OverseasOcrController
  │  @Valid 校验参数
  ▼
IOverseasOcrService.ocrIdCard(req)
  │
  ▼
OcrStrategyRegistry.getStrategy(req.getIdType())
  │  ├── "PASSPORT_NO"  → PassportOcrStrategy
  │  ├── "SAUDI_IQAMA"  → SaudiIqamaOcrStrategy
  │  ├── "SAB"          → SabBankCardOcrStrategy
  │  └── "AL_RAJHI"     → AlRajhiBankCardOcrStrategy
  │
  ▼
Strategy.ocr(imageBase64) → IdCardOcrResult / BankCardOcrResult
  │
  ▼
Result.success(result) → 返回给调用方
```

---

## 异常处理

| 场景 | 处理方式 |
|------|----------|
| 不支持的 idType/bankType | `Result.fail("不支持的类型: xxx")` |
| imageBase64 为空 | `Result.fail("图片不能为空")`（@Valid 校验） |
| OCR 识别失败 | `Result.fail("识别失败: reason")` |

ServiceImpl 负责 catch 策略层异常，统一转为 `Result.fail()`，Controller 层不处理异常。

---

## 扩展方式

新增 OCR 类型只需两步：

1. 创建策略类实现 `OcrStrategy<T>`，添加 `@Component`
2. `supportedType()` 返回新 type 字符串

示例：新增驾驶证识别
```java
@Component
public class DrivingLicenseOcrStrategy implements OcrStrategy<IdCardOcrResult> {
    @Override
    public String supportedType() { return "DRIVING_LICENSE"; }
    @Override
    public IdCardOcrResult ocr(String imageBase64) { /* 实现 */ }
}
```

无需修改 Controller、Service、Registry 任何现有代码。

---

## 后续对接（腾讯多模态理解）

当前阶段各策略为占位实现，统一返回 `Result.fail("OCR 服务暂未开放")`。

后续接入腾讯多模态理解接口时：
- 引入腾讯云 SDK 依赖到 pom.xml（如 `tencentcloud-sdk-java-lke` 等）
- 抽象一个 `TencentMultimodalClient` bean，封装签名、鉴权、API 调用
- 各策略 `ocr()` 方法调用腾讯多模态理解对应接口，根据图片+Prompt 提取结构化字段
- 证件识别 Prompt 示例：「识别图片中的证件信息，提取：证件号码、姓名、国籍、出生日期、有效期，以 JSON 格式返回」
- 银行卡识别 Prompt 示例：「识别图片中的银行卡信息，提取：银行卡号、银行名称、卡类型，以 JSON 格式返回」