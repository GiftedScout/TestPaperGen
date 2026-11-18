# TestPaperGen

在线组卷系统：题库管理、算法自动组卷、Word 试卷导出，以及基于大模型的试题解析自动生成。

前后端分离 Web 应用，后端 Java Spring Boot，前端 React + Umi，MySQL 存储，遗传算法与贪心算法完成自动组卷。

## 功能特性

**题库与组卷**
- 账号注册与登录，基于拦截器的登录鉴权，未登录无法访问业务接口
- 题库管理：选择题、判断题、填空题、阅读题、设计题；增删改查；Excel 批量导入
- 手动组卷：从题库勾选题目直接成卷
- 自动组卷：按章节、难度、题型数量、总分等条件，遗传算法 + 贪心算法自动选题，支持一次生成多套 A/B 平行卷
- 试卷导出：OpenXML 格式 Word 试卷与参考答案；新组卷和历史重导出可选「附带解析」，默认保持原输出
- 出卷历史：保存出卷记录，支持历史试卷查看与二次编辑
- 数据可视化：基于 ECharts 的题库与试卷题型、难度分布统计

**AI 试题解析**
- 一键生成：选中题目生成解析并自动入库，解析状态可在题库页筛选
- 批量生成：题库页多选后批量生成，结果弹窗逐题展示成功/失败与原因
- 解析查看与人工编辑：生成后可手工修改并保存
- 管理员配置页：顶部导航「AI 配置」（`/aiConfig`），可改接口地址、模型、Key，支持**拉取模型列表**与**连通性测试**，保存即时生效
- 密钥边界：只写后端进程内存、脱敏回显（`sk-****1234`）、不入库、不写浏览器存储；重启后回落到配置文件
- 容错设计：空返回/限流/5xx 自动重试 3 次，401/403/404 直接给中文提示；失败原因展示在弹窗内，不影响组卷等主流程
- Mock 模式：未配置密钥时生成 `[Mock]` 占位解析，开发联调不消耗额度

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 8 · Spring Boot 2.2.5 · MyBatis · MySQL |
| 前端 | React 16 · Umi 3 · Ant Design 4 · ECharts 5 |
| 算法 | 遗传算法 + 贪心算法 |
| AI 调用 | 第三方大模型 API（OpenAI 兼容协议），RestTemplate，15s 超时 |
| 文档导出 | Apache POI（OpenXML Word） |

## 快速开始

前置依赖：**JDK 8**、**Maven 3.6+**、**Node.js 14+（建议 16）**、**MySQL 5.7+/8.0**（字符集 utf8mb4）。下面的命令与操作系统无关，按你本机实际的软件安装位置执行即可。

```bash
# 1. 建库并导入（先建库，再按编号顺序执行脚本）
mysql -u root -p -e "create database if not exists test_paper_generation default charset utf8mb4"
mysql -u root -p test_paper_generation < database/base/test_paper_generation_base.sql
mysql -u root -p test_paper_generation < database/migrations/001_ai_analysis.sql

# 2. 后端（application.yml 默认 80 端口；本地建议显式指定端口）
cd TestPapaerGen-Backend
mvn -DskipTests package
DB_PASSWORD=你的密码 AI_API_KEY=sk-xxx java -jar target/test-paper-generation-3.0.0.jar --server.port=8081

# 3. 前端（另开一个终端，开发服务器 8000）
cd TestPapaerGen-Frontend
npm install
npm start
```

打开 <http://localhost:8000>。数据库密码、端口、AI 密钥全部通过环境变量或命令行参数注入，见下方「配置」；不要把任何密钥写进 `application.yml` 后提交。

### 关于启动脚本

本仓库**不收录环境专用的启动脚本**。原因很直接：JDK / Maven / MySQL 的安装位置、端口占用、代理设置、Node 版本每台机器都不一样，把 `D:\...` 这类绝对路径写进脚本提交上去，对别人只是一堆失效命令，还会掩盖"到底需要哪些前置条件"。

需要一键启动时，请按自己的环境写一个，放在本地并加入 `.gitignore`，不要提交；要交接给他人时，写清**前置条件 + 命令**，而不是附一个只在你机器上成立的脚本。

> 端口注意：前端 `src/config/requestConfig.js` 目前把 `:8000` 替换为 `:8081` 生成 API 地址，因此后端需监听 **8081** 才能联调。这一硬编码已在任务包 B 中立项改造（见 [docs/11](docs/11-任务包B-平台化与体验优化.md)），改造后排 B4 时前后端端口互不约束。

## 配置

### 环境变量

| 变量 | 必填 | 说明 |
|---|---|---|
| `DB_USERNAME` | 否 | 数据库用户名，默认 `root` |
| `DB_PASSWORD` | 是 | 数据库密码 |
| `AI_API_KEY` | 否 | 大模型 API Key；不配置则 AI 模块运行在 Mock 模式 |

### AI 模块（`application.yml` → `ai.*`）

| 配置 | 默认 | 说明 |
|---|---|---|
| `ai.enabled` | `false` | 总开关；`false` 时返回 `[Mock]` 占位解析 |
| `ai.provider` | `openai-compatible` | 提供商标识 |
| `ai.base-url` | `https://api.openai.com/v1` | OpenAI 兼容接口地址，需含 `/v1` |
| `ai.model` | `gpt-3.5-turbo` | 模型名，建议 `deepseek-chat` |
| `ai.timeout-ms` | `15000` | 连接/读取超时 |
| `ai.max-tokens` | `1200` | 单次生成上限；提示词要求 200 字内，留余量防截断 |
| `ai.temperature` | `0.3` | 采样温度，低温更稳定 |
| `ai.max-analysis-length` | `2000` | 解析长度上限（字符），超出不落库 |
| `ai.prompt-template` | 见 yml | 提示词模板，占位符 `{topicType}` `{topic}` `{answer}` |

### 运行时配置的当前边界

管理员可在 `/aiConfig` 修改 `enabled / baseUrl / model / apiKey` 并立即生效，但**其余参数与提示词仍只能改 `application.yml`**，配置也**未持久化到磁盘**（重启回落）。这两项已作为任务包 B 的 B2 / B3 立项，见 [docs/11](docs/11-任务包B-平台化与体验优化.md)。

完整配置与提示词调优说明见 [docs/03-AI解析模块.md](docs/03-AI解析模块.md)。

## 已知限制

| 事项 | 说明 |
|---|---|
| 题库答案大量为占位符 | 当前开发库 589 题中 587 题 `answer` 是 `AAAA`，**自动判题需先做答案治理**，见 [docs/10](docs/10-任务包A-学生端与判题报告.md) A0 |
| 历史重导出无解析快照 | 重导出读取当前题库内容与解析，题库更新会反映到历史试卷 |
| 批量生成为顺序执行 | 耗时 ≈ 题数 × 单次耗时，建议单批 ≤ 10 题 |
| 运行时配置不持久化 | 重启丢失 Key，需重新填写或改用环境变量 `AI_API_KEY` |
| 后端端口硬编码映射 | 前端按 `:8000 → :8081` 替换生成 API 地址，改端口需同步改 `requestConfig.js` |

## 文档

| 文档 | 内容 |
|---|---|
| [docs/01-需求摘要.md](docs/01-需求摘要.md) | 产品需求：功能规格与非功能规格 |
| [docs/02-框架设计.md](docs/02-框架设计.md) | 系统组成、模块边界与修改约束、数据模型 |
| [docs/03-AI解析模块.md](docs/03-AI解析模块.md) | AI 模块设计：配置、流程、容错、Mock、扩展 |
| [docs/04-数据库变更.md](docs/04-数据库变更.md) | 数据库变更与回滚 |
| [docs/05-接口文档.md](docs/05-接口文档.md) | 接口清单与约定 |
| [docs/06-开发任务清单.md](docs/06-开发任务清单.md) | 路线图、验收标准、已知限制 |
| [docs/07-交接说明.md](docs/07-交接说明.md) | 开发环境搭建、目录速查、故障排查 |
| [docs/08-M3导出与集成.md](docs/08-M3导出与集成.md) | M3 变更范围、参数语义与验证记录 |
| [docs/09-人工验收手册.md](docs/09-人工验收手册.md) | A–H 八组人工验收步骤与踩坑提示 |
| [docs/10-任务包A-学生端与判题报告.md](docs/10-任务包A-学生端与判题报告.md) | 计划中：学生端、自动判题、综合报告、局域网消息 |
| [docs/11-任务包B-平台化与体验优化.md](docs/11-任务包B-平台化与体验优化.md) | 计划中：UI 现代化、AI 参数全量暴露、配置持久化、跨平台运行 |

## 开发规范

- 分支：`main` 保持可编译、可启动；功能开发走 `feature/*` 分支，通过 PR 合并
- Commit：`<模块>: <做了什么>`，如 `ai: 新增批量生成接口`
- 数据库变更：必须新增 `database/migrations/` 编号脚本，幂等且附回滚说明，不允许直接修改 base 脚本
- 禁止提交：数据库密码、AI 密钥、本地配置文件（根 `.gitignore` 已屏蔽）
- 测试：后端改动需保证 `mvn test` 全绿（当前 23 项）；前端改动需 `npx umi build` 通过

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 授权。项目包含上游基线代码，相关权利归属与第三方组件许可见 [NOTICE](NOTICE)。

## 致谢

- 基线代码来源：[TestPapaerGen-WebApp](https://github.com/yx8118/TestPapaerGen-WebApp)，作者 inferno0303。
