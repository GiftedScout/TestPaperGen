# TestPaperGen

在线组卷系统：题库管理、算法自动组卷、Word 试卷导出，以及基于大模型的试题解析自动生成。

前后端分离 Web 应用，后端 Java Spring Boot，前端 React + Umi，MySQL 存储，遗传算法与贪心算法完成自动组卷。

## 功能特性

**题库与组卷**
- 账号注册与登录，基于拦截器的登录鉴权，未登录无法访问业务接口
- 题库管理：选择题、判断题、填空题、阅读题、设计题；增删改查；Excel 批量导入
- 手动组卷：从题库勾选题目直接成卷
- 自动组卷：按章节、难度、题型数量、总分等条件，遗传算法 + 贪心算法自动选题，支持一次生成多套 A/B 平行卷
- 试卷导出：OpenXML 格式 Word 试卷与参考答案，排版可直接打印，兼容 Office / WPS
- 出卷历史：保存出卷记录，支持历史试卷查看与二次编辑
- 数据可视化：基于 ECharts 的题库与试卷题型、难度分布统计

**AI 试题解析**
- 一键生成：选中题目生成解析并自动入库
- 解析查看与人工编辑
- Mock 模式：未配置密钥时生成占位解析，开发联调无需消耗额度
- 容错设计：网络异常、超时（15s）、返回格式非法均返回友好提示，不影响组卷等主流程

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 8 · Spring Boot 2.2.5 · MyBatis · MySQL |
| 前端 | React 16 · Umi 3 · Ant Design 4 · ECharts 5 |
| 算法 | 遗传算法 + 贪心算法 |
| AI 调用 | 第三方大模型 API（OpenAI 兼容协议），RestTemplate，15s 超时 |
| 文档导出 | Apache POI（OpenXML Word） |

## 快速开始

环境要求：JDK 8+、Maven 3.6+、Node.js 14+、MySQL 5.7+。

```bash
# 1. 数据库
mysql -u root -p < database/base/test_paper_generation_base.sql
mysql -u root -p < database/migrations/001_ai_analysis.sql

# 2. 后端（默认端口 80）
cd TestPapaerGen-Backend
mvn install && mvn package
java -jar target/test-paper-generation-3.0.0.jar

# 3. 前端（开发服务器 8000）
cd TestPapaerGen-Frontend
npm install && npm start
```

数据库密码与 AI 密钥通过环境变量注入，见下方「配置」。

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
| `ai.enabled` | `false` | 总开关；`false` 时返回占位解析 |
| `ai.base-url` | `https://api.openai.com/v1` | OpenAI 兼容接口地址，可替换为任意兼容网关 |
| `ai.model` | `gpt-3.5-turbo` | 模型名 |
| `ai.timeout-ms` | `15000` | 请求超时 |
| `ai.max-analysis-length` | `2000` | 解析长度上限（字符） |
| `ai.prompt-template` | 见 yml | 提示词模板，占位符 `{topicType}` `{topic}` `{answer}` |

完整配置与提示词调优说明见 [docs/03-AI解析模块.md](docs/03-AI解析模块.md)。

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

## 开发规范

- 分支：`main` 保持可编译、可启动；功能开发走 `feature/*` 分支，通过 PR 合并
- Commit：`<模块>: <做了什么>`，如 `ai: 新增批量生成接口`
- 数据库变更：必须新增 `database/migrations/` 编号脚本，幂等且附回滚说明，不允许直接修改 base 脚本
- 禁止提交：数据库密码、AI 密钥、本地配置文件（根 `.gitignore` 已屏蔽）

## 许可证

本项目采用 [Apache License 2.0](LICENSE) 授权。项目包含上游基线代码，相关权利归属与第三方组件许可见 [NOTICE](NOTICE)。

## 致谢

- 基线代码来源：[TestPapaerGen-WebApp](https://github.com/yx8118/TestPapaerGen-WebApp)，作者 inferno0303。
