# Win11 本地大模型部署指南：Ollama + Qwen2.5-Coder + Trae 接入

> 适用环境：Win11 / 32GB 内存 / 500GB 硬盘 / Java 开发
> 目标：在测试环境（网络受限）下本地运行大模型，并接入 TraeCode 使用

---

## 一、环境准备

### 1.1 系统要求确认

| 项目 | 最低要求 | 你的配置 | 状态 |
|---|---|---|---|
| 操作系统 | Windows 10/11 (64位) | Win11 | ✅ |
| 内存 | 16GB（推荐 32GB） | 32GB | ✅ 可跑 14B/32B |
| 磁盘空间 | 20GB 可用 | 500GB | ✅ 充裕 |
| GPU | 非必须（CPU 也可跑） | — | 有 GPU 更快 |

### 1.2 下载 Ollama

1. 打开官网：**https://ollama.com/download/windows**
2. 自动下载 `OllamaSetup.exe`（约 800MB）
3. 如果官网无法访问（测试环境无外网），在**有外网的机器**上下载好安装包，用 U 盘拷贝过来

### 1.3 安装 Ollama

1. 双击 `OllamaSetup.exe`
2. 使用默认安装路径：`C:\Users\<你的用户名>\AppData\Local\Programs\Ollama`
3. 安装完成后，Ollama 会：
   - 自动在系统托盘（右下角）显示 Ollama 图标 
   - 自动注册为后台服务，开机自启
   - 默认监听地址：`http://localhost:11434`

### 1.4 验证安装

打开 **PowerShell** 或 **CMD**，执行：

```powershell
ollama --version
```

应输出类似：

```
ollama version is 0.6.x
```

---

## 二、拉取 Qwen2.5-Coder 14B 模型

### 2.1 选择模型

| 模型 | 参数量 | 内存占用 | 下载大小 | 推荐场景 |
|---|---|---|---|---|
| qwen2.5-coder:7b | 7B | ~4.5GB | ~4.7GB | 快速问答、简单补全 |
| **qwen2.5-coder:14b** | **14B** | **~9GB** | **~9GB** | **日常编码（推荐）** |
| qwen2.5-coder:32b | 32B | ~19GB | ~20GB | 复杂逻辑、架构设计 |

> 32GB 内存跑 14B 只占约 9GB，剩余 20GB+ 给 IDE、浏览器、测试环境，完全无压力。

### 2.2 拉取模型（需要外网）

```powershell
ollama pull qwen2.5-coder:14b
```

下载过程会显示进度条，约 9GB，根据网速需要几分钟到几十分钟不等。

### 2.3 离线迁移方案（测试环境无外网时）

如果目标机器完全无法联网：

1. **在有外网的机器上**拉取模型：
   ```powershell
   ollama pull qwen2.5-coder:14b
   ```

2. **拷贝模型目录**：
   - 源路径：`C:\Users\<用户名>\.ollama\models`
   - 将整个 `models` 文件夹拷贝到 U 盘或移动硬盘

3. **在目标机器上**：
   - 先安装 Ollama（1.3 节）
   - 将 `models` 文件夹粘贴到 `C:\Users\<目标用户名>\.ollama\` 下（覆盖或合并）
   - 执行 `ollama list` 确认模型已识别

### 2.4 验证模型

```powershell
# 查看已安装的模型
ollama list

# 预期输出：
# NAME                    ID              SIZE      MODIFIED
# qwen2.5-coder:14b       abc123...       9.0 GB    2 minutes ago
```

### 2.5 试运行

```powershell
ollama run qwen2.5-coder:14b "用 Java 写一个快速排序算法"
```

如果正常输出代码，说明模型已就绪。输入 `/bye` 退出对话。

---

## 三、Ollama 服务管理

### 3.1 服务状态

Ollama 安装后自动作为后台服务运行，无需手动启动。

- **查看是否运行**：系统托盘是否有 🦙 图标
- **查看监听端口**：
  ```powershell
  netstat -ano | findstr 11434
  ```

### 3.2 手动控制

```powershell
# 启动服务（一般不需要，已自动启动）
ollama serve

# 停止服务：右键系统托盘 Ollama 图标 → Quit Ollama

# 重启服务：先 Quit，再重新打开 Ollama 应用
```

### 3.3 验证 API 服务

```powershell
# PowerShell
curl http://localhost:11434/api/tags

# 或浏览器直接访问
# http://localhost:11434
```

应返回 JSON，包含已安装的模型列表。

---

## 四、TraeCode 接入本地模型

### 4.1 打开模型管理

1. 打开 **TraeCode**
2. 点击左下角 **齿轮图标**（Settings）或按 `Ctrl + ,`
3. 左侧导航找到 **模型**（Models）
4. 点击 **添加模型** 按钮

### 4.2 配置自定义模型

在「添加模型」窗口中，选择 **自定义模型**，填写以下参数：

| 参数 | 填写值 | 说明 |
|---|---|---|
| **模型名称** | `Qwen2.5-Coder-14B (本地)` | 你在 Trae 中看到的显示名，随意取 |
| **模型 ID** | `qwen2.5-coder:14b` | 必须与 `ollama list` 中的 NAME 完全一致 |
| **API 格式** | `OpenAI 兼容` | Ollama 提供 OpenAI 兼容接口 |
| **请求地址（Base URL）** | `http://localhost:11434/v1` | Ollama 默认地址，注意末尾的 `/v1` |
| **API Key** | `ollama` | Ollama 不校验 Key，随便填即可 |

> ⚠️ **关键点**：Base URL 必须带 `/v1` 后缀，否则 Trae 会报 404。

### 4.3 保存并切换模型

1. 点击 **保存**
2. 回到 AI 对话界面
3. 在对话输入框**右下角**，点击当前模型名称
4. 在列表中选择 **Qwen2.5-Coder-14B (本地)**

### 4.4 测试对话

在 Trae 对话中输入：

```
用 Java 写一个线程安全的单例模式
```

如果正常返回代码，说明接入成功。

### 4.5 多模型切换（可选）

如果你同时安装了多个模型（7B/14B/32B），可以按上述步骤分别添加，然后在对话时随时切换：

- 简单问答 → 切 7B（速度快）
- 日常编码 → 切 14B（平衡）
- 复杂任务 → 切 32B（质量高）

---

## 五、常见问题排查

### 5.1 Trae 提示「连接失败」或「请求超时」

| 可能原因 | 排查方法 |
|---|---|
| Ollama 服务未启动 | 检查系统托盘是否有 🦙 图标，没有则打开 Ollama |
| 端口被占用 | `netstat -ano \| findstr 11434` 查看是否有其他进程占用 |
| Base URL 写错 | 确认是 `http://localhost:11434/v1`，注意 `/v1` |
| 模型 ID 不匹配 | `ollama list` 查看准确名称，必须完全一致 |
| 防火墙拦截 | 临时关闭 Windows 防火墙测试，或添加 Ollama 例外规则 |

### 5.2 模型拉取失败/超时

```powershell
# 重试拉取
ollama pull qwen2.5-coder:14b

# 如果网络不稳定，可以设置更长的超时
$env:OLLAMA_FLASH_ATTENTION = "1"
ollama pull qwen2.5-coder:14b
```

### 5.3 内存不足 / 运行卡顿

- 32GB 内存跑 14B 应该没问题。如果同时开了大量应用导致卡顿：
  - 关闭不必要的浏览器标签页
  - 或降级到 7B 模型：`ollama pull qwen2.5-coder:7b`

### 5.4 中文乱码 / 输出异常

- 确保 PowerShell 编码为 UTF-8：
  ```powershell
  [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
  ```
- Trae 中一般不会有此问题

### 5.5 删除不需要的模型

```powershell
ollama rm qwen2.5-coder:7b
```

---

## 六、进阶配置（可选）

### 6.1 自定义模型存储路径

默认模型存在 `C:\Users\<用户名>\.ollama\models`，如果 C 盘空间不够：

```powershell
# 设置环境变量，将模型存到 D 盘
[System.Environment]::SetEnvironmentVariable("OLLAMA_MODELS", "D:\ollama-models", "User")
```

设置后重启 Ollama 服务生效。

### 6.2 局域网内其他设备访问

默认 Ollama 只监听 `localhost`。如果需要局域网内其他机器访问：

```powershell
[System.Environment]::SetEnvironmentVariable("OLLAMA_HOST", "0.0.0.0:11434", "User")
```

重启 Ollama 后，其他机器可通过 `http://<你的IP>:11434` 访问。

### 6.3 同时安装多个模型

```powershell
ollama pull qwen2.5-coder:7b
ollama pull qwen2.5-coder:14b
ollama pull qwen2.5-coder:32b
```

三个全装约占用 35GB 磁盘空间，500GB 硬盘完全够用。在 Trae 中分别添加为三个自定义模型，按需切换。

---

## 七、快速参考命令

```powershell
# 安装验证
ollama --version

# 拉取模型
ollama pull qwen2.5-coder:14b

# 查看已装模型
ollama list

# 运行对话
ollama run qwen2.5-coder:14b

# 删除模型
ollama rm qwen2.5-coder:7b

# 验证 API
curl http://localhost:11434/api/tags

# 停止服务（系统托盘 Quit）
# 启动服务
ollama serve
```

---

## 八、整体架构图

```
┌─────────────────────────────────────────────┐
│                  Win11 本机                   │
│                                             │
│  ┌──────────────┐    HTTP /v1    ┌──────── │
│  │  TraeCode    │ ──────────────▶ │ Ollama │ │
│  │  (IDE)       │ ◀────────────── │ :11434 │ │
│  │              │   JSON 请求/响应 │        │ │
│  └──────────────┘                 └───┬────┘ │
│                                       │      │
│                                  ┌────▼────┐ │
│                                  │ 模型文件  │ │
│                                  │ 14B ~9GB│ │
│                                  └─────────┘ │
└─────────────────────────────────────────────┘
         不依赖外网，完全本地运行
```

---

> **文档版本**：v1.0
> **适用 Ollama 版本**：0.5.x / 0.6.x
> **适用 TraeCode 版本**：最新版
> **模型**：Qwen2.5-Coder 7B / 14B / 32B
