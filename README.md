# TestPaperGen — 在线组卷系统（AI 自动出试卷）

基于开源项目 **二次开发** 的在线组卷系统：保留题库管理、遗传/贪心算法自动组卷、Word 导出、Echarts 可视化等成熟能力，**新增 AI 一键生成试题解析创新模块**。

> 上游项目：[yx8118/TestPapaerGen-WebApp](https://github.com/yx8118/TestPapaerGen-WebApp)（原作者 inferno0303）
> 本仓库为其二次开发版本，目录结构与包名保持与上游一致，便于对照学习。上游未附加开源许可证，本项目仅用于课程学习与实践，**不得用于商业用途**。

## 技术栈

| 层 | 技术 |
|---|---|
| 后端 | Java 8 + Spring Boot 2.2.5 + MyBatis + MySQL |
| 前端 | React 16 + Umi 3 + Antd 4 + Echarts 5 |
| 算法 | 遗传算法 + 贪心算法（自动组卷） |
| AI | 第三方大模型 API（OpenAI 兼容协议），RestTemplate 调用，15s 超时 |
| 文档导出 | Apache POI（OpenXML Word） |

## 功能特性

**沿用上游（已具备）**
1. 登录注册，拦截器鉴权，未登录禁止访问业务接口
2. 题库管理：选择题/判断题/填空题/阅读题/设计题，增删改查 + Excel 批量导入
3. 手动组卷 + 自动算法组卷（按章节、难度、题型数量、总分），支持 A/B 平行卷
4. 导出 Word 试卷与参考答案，出卷历史管理，历史试卷二次编辑
5. Echarts 题库/试卷统计可视化

**二次开发新增（AI 解析模块）**
6. AI 一键生成试题解析：选中题目点击生成，后端调用大模型，解析入库
7. 解析手动编辑与查看；批量生成入口已在接口层预留
8. 导出试卷可选附带解析（接口已预留，模板填充为后续迭代项，见 docs/06）
9. Mock 模式：未配置 API Key 时生成占位解析，保证全链路可演示、可联调

## 快速开始

环境要求：JDK 8+、Maven 3.6+、Node.js 14+、MySQL 5.7+。

```bash
# 1、数据库（二选一）
mysql -u root -p < database/base/test_paper_generation_base.sql   # 全新安装
# 或在已有上游库中执行增量脚本：
mysql -u root -p < database/migrations/001_ai_analysis.sql       # 二次开发增量

# 2、后端（默认端口 80）
cd TestPapaerGen-Backend
mvn install && mvn package
java -jar target/test-paper-generation-3.0.0.jar
# 数据库密码与 AI 密钥通过环境变量注入，见 docs/07-交接说明.md

# 3、前端（默认端口 8000，代理到 80）
cd TestPapaerGen-Frontend
npm install && npm start
```

详细步骤与常见问题见 [docs/07-交接说明.md](docs/07-交接说明.md)。

## 目录结构

```
TestPaperGen/
├── TestPapaerGen-Backend/     # 后端（Spring Boot）
│   └── src/main/java/com/teemo/testpapergeneration/
│       ├── ai/                # 【新增】AI 解析模块
│       ├── controller/        # 接口层（上游 + AiAnalysisController）
│       ├── services/          # 业务层（上游组卷/导出 + AI 编排）
│       ├── entity/ mapper/    # 实体与数据访问（QuestionBank 新增 3 个解析字段）
│       └── config/ component/ # 拦截器鉴权（沿用）
├── TestPapaerGen-Frontend/    # 前端（Umi + React）
│   └── src/
│       ├── components/AiAnalysisModal.js   # 【新增】解析弹窗
│       ├── services/requestServices.js     # 【新增】AI 接口请求函数
│       └── pages/questionBank/index.js     # 【修改】操作列新增"解析"入口
├── database/
│   ├── base/                  # 上游原版建表脚本
│   └── migrations/            # 二次开发增量脚本
├── assets/ 文档.zip           # 上游示例文件与文档
└── docs/                      # 交接文档（见下方索引）
```

## 文档索引

| 文档 | 内容 |
|---|---|
| [docs/01-需求摘要.md](docs/01-需求摘要.md) | 需求分析报告有效信息提取、10 条需求规格 |
| [docs/02-框架设计.md](docs/02-框架设计.md) | 总体架构、模块边界、与上游差异清单 |
| [docs/03-AI解析模块.md](docs/03-AI解析模块.md) | AI 模块设计、配置、容错、Mock 模式 |
| [docs/04-数据库变更.md](docs/04-数据库变更.md) | 增量字段、执行与回滚 |
| [docs/05-接口文档.md](docs/05-接口文档.md) | 新增 5 个 AI 接口 + 上游接口索引 |
| [docs/06-开发任务清单.md](docs/06-开发任务清单.md) | 迭代计划、验收标准、已知 TODO |
| [docs/07-交接说明.md](docs/07-交接说明.md) | 环境搭建、启动步骤、常见问题 |

## 安全注意事项

1. **数据库密码、AI API Key 一律通过环境变量注入**（`DB_PASSWORD`、`AI_API_KEY`），不要明文写入配置文件，不要提交到 git。
2. API Key 只允许存在于后端，前端代码与接口响应中不得出现（`/aiAnalysis/status` 已做脱敏）。
3. 上游源码中的历史明文密码已在本次整理中移除，如发现其他残留请及时上报处理。
