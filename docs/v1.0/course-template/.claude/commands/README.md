# Claude Code 斜杠命令索引(教师维护)

本目录是项目模板的 Claude Code 斜杠命令(`/xxx`)。学生在 Claude Code CLI 终端输入 `/` 即可触发命令候选。

> **版本历史**:2026-05-12 工具链从 Claude Code IDE + DeepSeek 切换到 **Claude Code CLI + cc-switch + DeepSeek V4 Pro/Flash API**(Claude Code 对接 DeepSeek V4 有重大 bug)。命令文件正文保持不变,frontmatter 字段从中文 `名称/描述` 改为英文 `name/description`(Claude Code 标准)。

## 使用前提

1. **获取**:学生从教师下发的 `course-project-template.zip`(QQ 群文件 / 学习通)解压得到本目录(详见学生 08b §1.4)
2. **识别**:Claude Code CLI 在工作目录启动时**自动识别** `.claude/commands/`,**无需任何开关**
3. **触发**:CLI 终端输入 `/`,从候选列表选命令,带参数调用,如:
   ```
   /srs-writer 题目=社区物业综合管理系统 核心实体=user/house 核心功能=缴费/报修
   ```

## 32 个命令清单(2026-05-12 加 page-reviewer + tech-reviewer · 按 Phase 排序)

| 命令 | 06 模板 | 用在 Phase | 默认模型 | 简述 |
|-------|:------:|:------:|------|------|
| **`/scoping-reviewer`** | R-00 | **0(首)** | V4 Pro | **项目源头审核** · 审 docs/00-选题标定.md 5 维度(功能完整性/合理性/角色与权限/业务闭环/P0-P1-P2 划分) · 含 §一审核 + §二应用修复双模式 · **2026-05-11 顺序调整:R-00 在 init-skeleton 之前**(防 §一改动后需重跑 init-skeleton 补救) |
| `/init-skeleton` | G-01a | 0 | V4 Pro | 一次性生成全栈项目骨架(含 5 项硬门槛自检)· **2026-05-11 前置硬约束:调用前必须 R-00 已审已修**(scoping-reviewer §一 + §二 跑完) · **2026-05-12 升级:CLAUDE.md inject 操作**(不覆盖模板自带 CLAUDE.md · 只替换占位符 + 末尾追加 §五 项目元信息) |
| `/srs-writer` | G-02 | 1 | V4 Pro | 基于 P0 标定生成 SRS,写入 docs/PRD.md |
| `/tech-designer` | G-03 | 1 | V4 Pro | 基于 PRD 生成概要设计,写入 docs/TECH_DESIGN.md |
| `/tech-reviewer` | R-02 | 1 | V4 Flash | 审核 docs/TECH_DESIGN.md §1-§5(R-02),自动写审核报告 + 标 issue 注释 |
| `/page-prototyper` | G-04 | 1 | V4 Pro | 基于 PRD 映射表追加页面原型节 |
| `/page-reviewer` | R-02b | 1 | V4 Flash | **2026-05-12 新增** · 审核 docs/TECH_DESIGN.md §6 页面原型,解决 page-prototyper 产出"不被任何 reviewer 审"的缺口 |
| `/srs-reviewer` | R-01 | 1 | V4 Flash | 切换模型审核 PRD,生成审核报告 + 标 issue 注释 |
| `/db-designer` | G-05 | 2 | V4 Pro | 生成 ER 图 + 建表 SQL,写入 docs/DATABASE_DESIGN.md + sql/ |
| `/db-reviewer` | R-03 | 2 | V4 Flash | 审核数据库设计(范式/索引/约束) |
| `/api-designer` | G-06 | 3 | V4 Pro | 生成 RESTful API 清单,写入 docs/API_DESIGN.md |
| `/api-reviewer` | R-04 | 3 | V4 Flash | 审核 API 设计(REST 规范/状态码/参数命名) |
| `/entity-coder` | G-08 | 4 | V4 Flash | 基于 DATABASE_DESIGN.md 生成 Entity + Mapper(三阶段教学第一阶段 P0-1 用 + 兜底) |
| `/service-coder` | G-09 | 4 | V4 Flash | 基于 API_DESIGN.md 生成 Service + Controller(三阶段教学第一阶段 P0-1 用 + 兜底) |
| **`/feature-coder`** 🆕 | **G-30** | **4(主)** | **V4 Pro** | **Vertical Slice 全栈功能实现 · 一次跨 8-15 文件全栈生成 + 11 类特殊场景自动识别 + R-05+R-06 双层审核 + §二 跨层修复 · 三阶段教学第二阶段(P0-2 起)主路径** |
| `/code-reviewer-be` | R-05 | 4 | V4 Pro | 后端代码审查(分层/MP用法/异常处理/事务 · 位置参数双切片:`/code-reviewer-be auth`=模块切片 / `/code-reviewer-be P0-3`=功能切片) |
| `/bug-tracer-be` | D-01 | 4/6 | V4 Flash | 后端 bug 排查(日志/异常栈/数据库) |
| `/vue-page-coder` | G-11 | 5 | V4 Flash | 基于 PRD + API 生成 Vue 页面 |
| `/login-coder` | G-12 | 5 | V4 Flash | 生成登录页 + 路由守卫 + token 持久化 |
| `/axios-coder` | G-13 | 5 | V4 Flash | 生成 axios 封装 + API 模块 |
| `/code-reviewer-fe` | R-06 | 5 | V4 Pro | 前端代码审查(组件/Pinia/API 调用 · 位置参数三切片:`/code-reviewer-fe LoginPage`=页面 / `/code-reviewer-fe user`=模块 / `/code-reviewer-fe P0-3`=功能 · PascalCase/小写/P0-N 自动识别) |
| `/bug-tracer-fe` | D-02 | 5/6 | V4 Flash | 前端 bug 排查(控制台/网络/响应式) |
| `/unittest-coder` | G-16 | 6 | V4 Flash | 生成 JUnit + Vitest 单元测试 |
| `/perf-optimizer` | G-20 | 7 | V4 Pro | R-07 维度 4 性能 + R-08 维度 8 幂等性中性能子集 应用修复主路径 · 跟 refactor-helper 横向协同(perf 改性能/refactor 改结构 · 同 issue 不同时调)· Postman 实测前后强约束 · 接 reviewer 会话不退出 `claude` 重启 |
| `/refactor-helper` | G-21 | 7 | V4 Pro | R-XX 协议家族应用修复主路径 · 基于 R-05/R-06/R-07/R-08 报告高严重度 issue 做小步重构 · 双修复(代码注释 in-place 改「已修复」+ review.md 加 ✅)· 接 reviewer 会话不退出 `claude` 重启 |
| `/code-reviewer-full` | R-07 | 7 | V4 Pro | 全栈综合审查 · 6 维度 · 按范围(Backend/Frontend/Util)分次审 · 重点扫跨模块/跨层/端到端横切问题 |
| `/security-reviewer` | R-08 | 7 | V4 Pro | OWASP 深度安全专项 · 8 维度 · Backend/Frontend/Full 拆段(默认 Full)· 跟 R-07 8 项基础扫描互补不重复 · 不审 CSRF(JWT Bearer Token 模式) |
| `/readme-writer` | G-15 | 8 | V4 Pro | 完善项目 README.md(双胞胎模式 · 内容写入器)· 跟 init-skeleton 8 节占位章节顺序+节名 100% 一致 + 末尾追加 §九 AI 协作 + §十 联系方式 2 个 Phase 8 完善节 · 跟 deploy-writer 横向协同(必须在 deploy-writer 之后跑) |
| `/deploy-writer` | G-22 | 8 | V4 Pro | 完善部署文档 docs/DEPLOY.md(双胞胎模式 · 内容写入器)· 跟 init-skeleton 5 节占位章节顺序+节名 100% 一致 + 末尾追加 §六 默认账号 + §七 安全检查清单(对齐 R-08 维度 5/7)+ §八 云服务器进阶 · 跟 readme-writer 横向协同(必须在 readme-writer 之前跑)· mvn 不跳过单测 |
| `/git-committer` | (通用) | 全 Phase | V4 Flash | 智能生成 commit + 自动 commit |
| `/rules-updater` | (通用) | 每 Phase 末 | V4 Flash | 重写 `.claude/project-status.md` 9 个字段值(只填值不破坏教师维护结构 · 双模式) |
| `/prompt-evolver` | (通用 · 06 §10.3) | 全 Phase | V4 Pro | 提示词演化助手 · **三段式 v1→v2→v3 完整演化**(对齐 05 验收 ≥ 3 个 + 08b §10.4 真实示例 + §13 Q26 不算演化判定 + readme-writer §九 演化记录数引用)· 双场景(主场景增量调用 · 备用 mode=draft 一次性 7 段)· 升级 V4 Pro(深度诊断推理) |

> **不必先配齐所有命令**——Phase 0 用到 `/scoping-reviewer`(R-00 · 首)+ `/init-skeleton`(R-00 之后)· Phase 1 加用 6 个(srs-writer/tech-designer/tech-reviewer/page-prototyper/page-reviewer/srs-reviewer),以此类推。因为 commands 目录全部预置,Claude Code CLI 会自动加载,无需手动配。
>
> **历史命令 `/needs-analyzer` 已于 2026-05-10 V4-D04 删除**(下游 srs-writer 设计为只读 `docs/00-选题标定.md`,不接受其对话框输出 → 兜底路径形同虚设;且 08b 官方流程从未引用)。06 模板库 G-01 段保留作历史参考。

## 命令文件格式规范

每个 .md 文件必须按以下结构:

```markdown
---
name: <命令唯一标识,与文件名一致>
description: <一句话职责,Claude Code 命令候选会显示>
---

(下面是给 AI 的指令正文,按需包含:任务/输入/输出结构/输出指令/调用示例/checklist/衔接 等章节)
```

**禁止**在 frontmatter 上方或 frontmatter 内添加 markdown 标题、教师维护说明等——Claude Code 会把这些当指令塞给 AI,污染 prompt。教师维护元信息全部放在本 README 表格里。

## 维护说明

### 修改某个命令

直接编辑 `.claude/commands/<command>.md` 的 `---` 下方正文。**不要删除 frontmatter**。

### 新增命令

1. `.claude/commands/` 新建 `<new-command>.md`
2. 添加 frontmatter(name + description · 英文字段名)
3. 写指令正文
4. 在本 README 表格里追加一行(命令 / 06 模板 / Phase / 默认模型 / 简述)

### 删除命令

1. 删除对应 `.md` 文件
2. 本 README 表格里删除对应行
3. 全局搜 `/<command-name>` 看是否还有引用,一并清理(常见在 08b 文档、其他 commands 的"衔接"章节)

### 与 06 模板的同步

本目录命令是 `06-提示词与审核模板库.md` 中 G-XX/R-XX/D-XX 的封装。06 模板更新时,需同步本目录:
- G-XX/R-XX/D-XX 的 prompt 主体 → 命令 .md 的正文
- G-XX 的"输出指令" → 命令 .md 的「输出指令」章节
- 不要丢失 frontmatter

### 模型选择规则

- **V4 Pro**(`deepseek-v4-pro[1m]` 等推理强):**地基类**(SRS/概要设计/数据库/API)、**审查类**(code-reviewer / security-reviewer)、**优化类**(perf/refactor)
- **V4 Flash**(`deepseek-v4-flash` 代码生成快):**编码类**(entity/service/vue-page/axios/login/unittest)、**轻量任务**(bug-tracer/git-committer/rules-updater)

> 📌 模型在 cc-switch GUI 的 DeepSeek 供应商 env 字段里配置(`ANTHROPIC_DEFAULT_HAIKU_MODEL` = Flash · `ANTHROPIC_DEFAULT_SONNET_MODEL` / `ANTHROPIC_DEFAULT_OPUS_MODEL` = Pro)· 学生不需要在每次命令调用时手动选模型 · Claude Code 会按命令的"难度档"自动匹配。
