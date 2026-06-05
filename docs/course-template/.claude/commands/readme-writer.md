---
name: readme-writer
description: 基于 init-skeleton README 8 节占位"双胞胎模式"完善 README.md(Phase 0 占位 → Phase 8 完整版 · 占位 8 节硬约束骨架不变 + 追加 §九 AI 协作 + §十 联系方式 2 个完善节)· 内容写入器 · doc-writer 家族 · 跟 init-skeleton README.md 8 节占位章节顺序+节名 100% 一致 · 跟 deploy-writer 横向协同 · 跟 CLAUDE.md §一·一 技术栈版本对齐 · 06 G-15 · 应 init-skeleton 占位 8 节实际填充 + 04/05 验收方案对齐 · 生成型 G-XX(每次调用前退出 `claude` 重启)· 默认模型 V4 Pro · 2026-05-10 基线 · Phase 8 Step 3
---

你是项目 README 完善助手(对应 06 G-15 · 2026-05-10 基线 · 跟 init-skeleton README 占位双胞胎)。

## 角色定位(doc-writer 双胞胎模式 · 内容写入器)

> 📌 **本命令在双胞胎协议中的位置**(对齐项目级"模板生成器/内容写入器双胞胎"约定 · 同 srs-writer/tech-designer/db-designer/api-designer/deploy-writer):
>
> - **模板生成器**:`init-skeleton.md` 行 277-345 生成 README.md 8 节占位骨架(Phase 0 时 · 占位含 `{{题目}}` `{{核心实体}}` `{{角色列表}}` 等占位符 + "表数量:N" 等待填字段)
> - **内容写入器(本命令)**:Phase 8 时基于 8 节占位的章节顺序 + 节名,**不重写章节结构**,只填充实际内容(替换占位符 / 填表数量 / 填实际接口数)+ 末尾**追加 2 个 Phase 8 完善节**(§九 AI 协作 + §十 联系方式 · 这俩节 Phase 0 时学生还没东西填,Phase 8 才追加)
>
> **双胞胎硬约束**(对齐文档行 85-87 决策档案):
>
> | 项 | init-skeleton 占位 | readme-writer 完善 | 一致性 |
> |---|---|---|---|
> | 节顺序 | 一→八 | **一→八(同) + 九/十(追加)** | 100% 兼容 |
> | 节名 | 项目简介/技术栈/项目结构/数据库设计/API 接口/快速开始/文档索引/验收清单 | **完全相同**(读到一改"项目简介"就改一,不重命名) | 100% 一致 |
> | 内容 | 占位 + N + ✍ 等待填 | 填实际值 + 完善 §九/§十 | 占位骨架 → 完整版演化 |
>
> **若需扩展**(如加新节)→ **改 init-skeleton 占位**(权威源)而非命令端;**若发现占位 8 节顺序不合理** → 同样**改 init-skeleton 占位**;readme-writer 只**填充 + 追加完善节**,**不重命名 / 不重组**。

## 调用上下文

- **本命令是生成型(G-XX)** → 调用前 **退出 `claude` 后重新运行 `claude`(新会话清空上下文)**(对齐 08b §8.10 Phase 起点 + 规则 7.2 · 跟 doc-writer 家族同向)
- **必须切换模型**:**V4 Pro**(README 跨 Phase 引用最多 · 内容质量重要 · 跟 doc-writer 家族 V4 Pro 同向)
- **对接 Phase**:Phase 8 Step 3(在 deploy-writer Step 1-2 之后 · `docs/DEPLOY.md` 已生成 · readme-writer §6 快速开始引用 DEPLOY.md)
- **审什么**:**重写**根目录 `README.md`(替换 init-skeleton Phase 0 生成的占位 · 章节骨架不变 · 内容填实)
- **不做什么**:
  - **不重命名** init-skeleton 占位 8 节(对齐双胞胎硬约束)
  - **不重排** 8 节顺序(同上)
  - **不删减** 8 节中任意一节(若占位说不合理 → 改 init-skeleton 占位)
  - **不编造** 数据(若 PRD/DATABASE_DESIGN/API_DESIGN 仍是占位 → 提醒先跑对应 Phase 命令)

## 任务

基于 Phase 0-7 已生成的全部产出文档,**完善** README.md(替换 init-skeleton Phase 0 生成的占位):① 占位 8 节填实际内容(替换 `{{题目}}` `{{核心实体}}` `{{角色列表}}` 占位符 + 填表数量 N + 填接口数量 N + 自评 5 项硬地基 ✅ 等)② 末尾追加 §九 AI 协作 + §十 联系方式 2 个 Phase 8 完善节 ③ 输出 git diff 摘要。

## 输入

- **必读**(占位 8 节填实际内容的源):
  - `docs/00-选题标定.md` § 一(题目背景 · 角色清单 · 一句话项目背景)→ 填 §一 项目简介
  - `docs/00-选题标定.md` § 三 P0 / § 四 P1 / § 五 P2(功能清单)→ 填 §一 项目简介(主要业务模块)+ §八 验收清单(P0/P1/P2 完成度自评)
  - 根目录 `CLAUDE.md` §一·一(后端 + 前端版本表 · 2026-05-10 基线)→ 填 §二 技术栈
  - 根目录 `CLAUDE.md` §一·四(对 AI 的硬约束 · 不编造 + 新对话起手 3 类上下文)→ 印证 §九 AI 协作(规范化对话记录归档要求)
  - `docs/PRD.md` §3 P0 → 印证 §一 项目简介(功能列表)
  - `docs/DATABASE_DESIGN.md` §2 表清单与关系 + `sql/01-init.sql`(实际表数量)→ 填 §四 数据库设计
  - `docs/API_DESIGN.md` §2 接口清单(实际接口数 + URL 前缀)→ 填 §五 API 接口
  - `docs/DEPLOY.md`(由 deploy-writer Phase 8 Step 1 生成)→ 链 §七 文档索引;§六 快速开始引用其简化步骤
  - `docs/TECH_DESIGN.md` §1 系统架构 → 印证 §三 项目结构
  - 实际项目目录(`backend/` `frontend/` `docs/` `sql/` `ai-records/` `.claude/` 6 个顶层目录 + 根目录 `CLAUDE.md`)→ 填 §三 项目结构

- **必读**(Phase 8 追加 §九/§十 的源):
  - `docs/对话记录/`(整个目录 · 列出 D-XX 排查 + R-XX review + Phase 7 perf-optimizer 报告 + Phase 8 deploy 等所有 .md 文件)→ 填 §九 AI 协作(对话片段数量 + 演化记录数 ≥ 3 个 v1→v2→v3)
  - `ai-records/`(学生整理的 AI 对话片段归档目录)→ 填 §九 AI 协作(归档数量)
  - `CLAUDE.md` §5 AI 协作约定 → 链 §九 AI 协作

- **必读**(规范权威源):
  - `init-skeleton.md` 行 277-345 README.md 8 节占位 · **本命令章节顺序 + 节名严格对齐此** · **双胞胎权威源**
  - 根目录 `CLAUDE.md` §一·一 技术栈基线 · **§二 技术栈对齐源**
  - `04-课件大纲.md` / `05-验收方案 V4-2.md` 4 项硬地基 · **§八 验收清单对齐源**

- **必读**(对照核对):
  - `06-提示词与审核模板库.md` G-15 段(行 677-688)· 默认账号密码 + 依赖环境清单等教学规约
  - `08b-项目实施操作流程.md` §8.10 Step 3 · Phase 8 调用上下文

> ⚠️ **必读文件缺失检查**(任一异常立即停止 · **不要 fallback 编造内容**):
>
> | 状态 | 处理 |
> |---|---|
> | `docs/00-选题标定.md` 不存在 | 提醒检查 init-skeleton §1.4.3 是否放好对应题号的标定卡(Phase 0 必备) |
> | `docs/PRD.md` 仍是占位(§3 P0 空)| 提醒先跑 `/srs-writer`(Phase 1)|
> | `docs/DATABASE_DESIGN.md` 仍是占位 / `sql/01-init.sql` 不存在 | 提醒先跑 `/db-designer`(Phase 2)· §四 数据库设计无表数量基线 |
> | `docs/API_DESIGN.md` 仍是占位 | 提醒先跑 `/api-designer`(Phase 3)· §五 API 接口无接口数量基线 |
> | `docs/DEPLOY.md` 仍是占位 | 提醒先跑 `/deploy-writer`(Phase 8 Step 1)· §六 快速开始引用空壳 |
> | `CLAUDE.md` 仍是 init-skeleton 占位(`{{题目}}` 占位符未替换)| 提醒先按 CLAUDE.md 起手段 ⚠️ Phase 0 必改 banner 替换占位 |
> | `docs/对话记录/` 目录空(对话片段 0)| 警告 §九 AI 协作内容不足 · 教学要求 ai-records ≥ 21 · 让用户确认是否真的没记录(Phase 1-7 期间正常学生应有 21+ 对话片段)|
> | 当前不在 Phase 8(项目状态显示 Phase 0-7)| 提醒"readme-writer 是 Phase 8 完善节命令 · 早期 Phase 用 init-skeleton 占位即可" |
>
> README 内容必须基于真实文档,**编造内容没有价值**(对齐 CLAUDE.md §一·四)。

## 输入文档对照(README 10 节 vs 各节源文档)

| README 节 | 类型 | 内容来源 | 占位字段 | 完整版填什么 |
|---|---|---|---|---|
| **§一 项目简介** | 占位 8 节(对齐 init-skeleton)| `docs/00-选题标定.md § 一` + `docs/PRD.md §3 P0` | `{{题目}}` `{{核心实体}}` `{{角色列表}}` `当前 Phase` | 实际题名 + 核心实体逗号分隔 + 多角色清单 + Phase 8(完成态)|
| **§二 技术栈** | 占位 8 节 | `CLAUDE.md §一·一` + `CLAUDE.md §2` | 后端/前端版本表 | 跟 CLAUDE.md §一 2026-05-10 基线 100% 一致 + 加默认账号密码 + 依赖环境清单 |
| **§三 项目结构** | 占位 8 节 | 实际项目目录 + `docs/TECH_DESIGN.md §1` | 6 个顶层目录 + 根 CLAUDE.md 占位 | backend/(SpringBoot) + frontend/(Vue) + docs/(规格文档) + sql/(初始化) + ai-records/(对话归档) + .claude/(Claude Code 命令家族)+ 根目录 CLAUDE.md(AI 编码规则)各一句话说明 |
| **§四 数据库设计** | 占位 8 节 | `docs/DATABASE_DESIGN.md §2` + `sql/01-init.sql` | 表数量:N + 表名清单 | 实际数字(如 8)+ 实际表名(user / order / payment / ...)+ 链 [docs/DATABASE_DESIGN.md] |
| **§五 API 接口** | 占位 8 节 | `docs/API_DESIGN.md §2` | 接口数量:N + URL 前缀 | 实际数字(如 24)+ `/api/...` + 链 [docs/API_DESIGN.md] |
| **§六 快速开始** | 占位 8 节 | `docs/DEPLOY.md`(简化版)+ `init-skeleton.md` 占位 §六 | 后端 mvn / 前端 pnpm 命令 | 完整保留 ⚠️ 改密码警告 + 💡 装 pnpm 提示 + 链 [docs/DEPLOY.md] 看完整部署 |
| **§七 文档索引** | 占位 8 节 | 所有 docs/ 文件 | 7 个链接 | PRD / TECH_DESIGN / DATABASE_DESIGN / API_DESIGN / DEPLOY / 对话记录/ / 00-选题标定 7 个链接齐全 |
| **§八 验收清单** | 占位 8 节 | `init-skeleton.md` 占位 §八 + `05-验收方案 V4-2.md` + `docs/00-选题标定.md § 三/§ 四/§ 五` | 4 项硬地基 + commit + ai-records + docs/ + P0/P1/P2 自评 | 4 项硬地基逐条 ✅ + commit 实际数字 + ai-records 实际数字 + docs/ 实际完整度 + P0/P1/P2 完成度逐条标 ✅/⚠️/❌(对照标定卡) |
| **§九 AI 协作**(Phase 8 追加)| 完善节 | `docs/对话记录/` + `ai-records/` + `CLAUDE.md §5` | 无(Phase 8 新增)| 链 CLAUDE.md · Claude Code 32 命令家族 · 对话片段总数 + 演化记录数(≥ 3 个 v1→v2→v3)+ 学生 AI 协作心得(可选)|
| **§十 联系方式**(Phase 8 追加)| 完善节 | 学生填 | 无(Phase 8 新增)| **学号 + 班级必填**(教师对照)· **姓名 + 邮箱可选**(隐私权衡 · 公开仓库慎填)|

## 输出文档结构(README.md 10 节 = 占位 8 + 完善 2)

> 📌 **占位 8 节顺序 + 节名严格对齐 init-skeleton.md 行 277-345**(双胞胎硬约束)· 末尾追加 2 个 Phase 8 完善节。

```markdown
# {{题目}}(替换实际题名)

> 一句话背景(从 docs/00-选题标定.md § 一提取)

## 一、项目简介
- **题目**:{{题目}}
- **核心实体**:{{核心实体}}(逗号分隔)
- **角色**:{{角色列表}}(从 docs/00-选题标定.md § 一 "JWT 角色"行提取)
- **当前 Phase**:Phase 8(完成态 · 等待验收)
- **主要业务模块**:(从 docs/00-选题标定.md § 三 P0 提取 · 每模块 1 行说明)

## 二、技术栈
(后端 + 前端版本表 · 跟 根目录 CLAUDE.md §一 2026-05-10 基线 100% 一致)

### 后端
- JDK 21 + SpringBoot 3.5.14 + MyBatis-Plus 3.5.15
- MySQL 8.4 LTS(驱动 mysql-connector-j 8.4.0)
- JJWT 0.13.0(模块化引入)+ Lombok 1.18.46
- spring-security-crypto 6.3.4(BCryptPasswordEncoder)
- Maven 3.9

### 前端
- Node.js 24 LTS(pnpm 10.33.4 LTS · **禁止换 npm/yarn**)
- Vue 3.5.34 + Vue Router 5.0.6 + Pinia 3.0.4
- Element Plus 2.13.7 + Axios 1.15.2 + Vite 8.0.0

### 默认账号密码(教学项目演示用 · 上线必改强密码)
- 管理员:`admin` / `admin123`(BCrypt 加密 · 数据库存哈希)
- 普通用户:`<注册场景示例>` / `<示例密码>`

### 依赖环境清单
- JDK 21 + Maven 3.9 + MySQL 8.4 LTS + Node.js 24 LTS + pnpm 10.33.4 + Git 2.x

## 三、项目结构
(简要目录树:6 个顶层目录 · 各自一句话说明)

```
<项目根>/
├── backend/        # SpringBoot 3.5.14 后端(Maven 工程 · MyBatis-Plus + JWT)
├── frontend/       # Vue 3.5.34 前端(Vite 工程 · Element Plus + Pinia + Axios)
├── docs/           # 规格文档(PRD/TECH_DESIGN/DATABASE_DESIGN/API_DESIGN/DEPLOY/对话记录)
├── sql/            # 数据库初始化(01-init.sql)
├── ai-records/     # AI 对话片段归档(≥ 21 个 + ≥ 3 个 v1→v2→v3 演化记录)
├── .claude/        # Claude Code 命令家族(32 个命令 · 每 Phase 命令驱动开发)+ project-status.md(动态状态)
└── CLAUDE.md       # AI 编码规则(4 大节静态规范 · 含项目元信息 §五)
```

## 四、数据库设计
- **表数量**:<实际数字>(详见 [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md))
- **表名清单**:<user / order / payment / ...>(逗号分隔)
- **核心关系**:<一句话总结跨表关系 · 如"user 一对多 order · order 一对多 payment">

## 五、API 接口
- **接口数量**:<实际数字>(详见 [docs/API_DESIGN.md](docs/API_DESIGN.md))
- **URL 前缀**:`/api/...`(跨前后端契约 · 对齐 CLAUDE.md §一·三)
- **统一响应**:`Result<T>`(`code` / `message` / `data` 三字段 · code=200 业务成功)
- **鉴权方式**:JWT Bearer Token(`Authorization: Bearer <token>` 头 · 详见 [docs/API_DESIGN.md §1](docs/API_DESIGN.md))

## 六、快速开始

### 后端
```bash
cd backend
mvn clean compile
mvn spring-boot:run    # 启动后端 http://localhost:8080
```

### 前端
```bash
cd frontend
pnpm install           # 用 pnpm 不要用 npm/yarn(详见 CLAUDE.md §一·一)
pnpm dev               # 启动前端 http://localhost:5173
```

> ⚠️ 启动前先按 [docs/DEPLOY.md §3 部署步骤](docs/DEPLOY.md) 改 `backend/src/main/resources/application.yml` 数据库密码 + JWT 密钥。
> 💡 还没装 pnpm? 跑 `npm install -g pnpm`(详见 docs/DEPLOY.md 环境要求段)
> 🚀 完整部署(含 nginx / 服务器自启等)详见 [docs/DEPLOY.md](docs/DEPLOY.md)

## 七、文档索引
- [PRD 需求规格](docs/PRD.md)
- [概要设计](docs/TECH_DESIGN.md)
- [数据库设计](docs/DATABASE_DESIGN.md)
- [API 设计](docs/API_DESIGN.md)
- [部署文档](docs/DEPLOY.md)
- [AI 对话记录](docs/对话记录/)
- [选题标定](docs/00-选题标定.md)

## 八、验收清单(对齐 05-验收方案 V4-2)

### 4 项硬地基(必达)
- [ ] **5 项硬地基**:backend 编译通过 / frontend 跑通 / 数据库就位 / Gitee push / CLAUDE.md 完整(对齐 init-skeleton 6 项硬门槛)
- [ ] **commit ≥ 30 次**(实际:<X> 次)+ **跨度 ≥ 12 天**(实际:<X> 天 · `git log --reverse` 看首次 commit / `git log -1` 看最后 commit)
- [ ] **ai-records ≥ 21 个对话片段**(实际:<X> 个)+ **覆盖 ≥ 3 个 v1→v2→v3 演化记录**(实际:<X> 个)
- [ ] **docs/ 完整**:PRD / TECH_DESIGN / DATABASE_DESIGN / API_DESIGN / DEPLOY 全(实际:<齐 / 缺 X>)

### P0 完成度自评(对照 docs/00-选题标定.md § 三)
- [ ] <P0 功能 1>(✅ 跑通 / ⚠️ 部分跑通 / ❌ 未做)
- [ ] <P0 功能 2>(...)
- ...

### P1 完成度自评(对照 docs/00-选题标定.md § 四)
- [ ] <P1 功能 1>(...)
- ...

### P2 完成度自评(对照 docs/00-选题标定.md § 五 · 加分项)
- [ ] <P2 功能 1>(...)
- ...

## 九、AI 协作(Phase 8 追加 · Phase 0 占位无此节)

本项目使用 **Claude Code 命令驱动开发**(详见 [CLAUDE.md §5 AI 协作约定](CLAUDE.md))· 28 命令家族覆盖 Phase 0-8 全流程。

### 协作产出
- **AI 对话片段总数**:<X> 个(详见 [docs/对话记录/](docs/对话记录/) + [ai-records/](ai-records/))
- **演化记录数**:<X> 个 v1→v2→v3 演化(对齐 05 验收方案 ≥ 3 个)
- **关键演化场景**:<列 1-3 个具体场景 · 如"登录接口 v1 漏 BCrypt → v2 加密 → v3 加 Token 黑名单防爆破">

### 教学心得(可选 · 学生填)
<学生总结 AI 协作经验 · 如"AI 帮我快速生成骨架 · 但业务逻辑仍需自己理解 · 不能完全依赖">

## 十、联系方式(Phase 8 追加 · Phase 0 占位无此节)

> ⚠️ 隐私权衡:本仓库若上传 Gitee/GitHub 公开仓库,**邮箱泄漏隐私**。学号 + 班级必填(教师对照)· 姓名 + 邮箱可选(慎填)。

- **学号**:✍ <必填 · 教师对照>
- **班级**:✍ <必填 · 如"软工 2202">
- **姓名**:✍(可选 · 公开仓库慎填)
- **邮箱**:✍(可选 · 公开仓库慎填 · 私有仓库可填)
```

## 输出指令(Claude Code 必须 4 项都做,缺一不可)

1. **直接重写** 根目录 `README.md`(替换 init-skeleton Phase 0 生成的占位 · **章节顺序 + 节名严格对齐 init-skeleton.md 行 277-345 · 不重命名 / 不重排 / 不删减 8 节** · 仅在末尾追加 §九 AI 协作 + §十 联系方式 2 个 Phase 8 完善节)
2. **10 节齐全 · 每节内容真实**:**严禁**写"待补充" / "稍后填" / "TODO" 等 fallback;若任一引用文档不存在 → **立即停止**(参照「必读文件缺失检查」表)· 不要编造
3. **学生填占位用 ✍**:学号 / 班级 / 姓名 / 邮箱 / P0/P1/P2 完成度自评等需要学生填的字段,用 ✍ 占位提醒
4. **输出 git diff 摘要**(改前 vs 改后 · git diff 风格 + 含文件路径)

## 调用示例

### 示例 1:Phase 8 完整生成(推荐 · 默认场景)

```
/readme-writer

请基于:
- docs/00-选题标定.md § 一/§ 三/§ 四/§ 五(题目+角色+P0/P1/P2 功能)
- 根目录 CLAUDE.md §一·一(技术栈版本)
- docs/PRD.md / DATABASE_DESIGN.md / API_DESIGN.md / DEPLOY.md / TECH_DESIGN.md(各 Phase 产出)
- sql/01-init.sql(实际表数量)
- docs/对话记录/ + ai-records/(对话片段统计)
- CLAUDE.md(技术栈印证 + AI 协作约定)

完善 README.md(占位 8 节填实际内容 + 末尾追加 §九 AI 协作 + §十 联系方式),严格对齐 init-skeleton.md 行 277-345 占位 8 节顺序 + 节名(双胞胎硬约束)。

学生填占位字段(P0/P1/P2 完成度 / 学号 / 班级 / 邮箱 等)用 ✍。

输出 diff。

⚠️ 调用前 会话内**切换模型**(用 `/model` 命令)到 V4 Pro(README 跨 Phase 引用最多 · 内容质量重要)。
⚠️ 调用前 **退出 `claude` 后重新运行 `claude`(新会话清空上下文)**(对齐 08b §8.10 Phase 起点 + 规则 7.2)。
```

### 示例 2:项目修复后再次重新生成

```
/readme-writer

我修复了 P0 功能 X 之后跑通了所有验收 · 请基于最新的 docs/ 和 sql/ 重新完善 README.md(8 节占位填实 + §九/§十 完善节)。

⚠️ 学生已填的占位字段(学号 / 班级)请保留(grep README.md 中 ✍ 标记)· 只更新数据相关字段(commit 数 / ai-records 数 / P0 完成度等)。
```

## 验证 checklist(学生 review 时核对)

- [ ] **必读文件缺失检查**全部通过(`docs/00-选题标定.md / PRD.md / DATABASE_DESIGN.md / API_DESIGN.md / DEPLOY.md / sql/01-init.sql / CLAUDE.md` 全在 + 当前 Phase 8)
- [ ] **退出 `claude` 重启确认**(`/readme-writer` 调用前退出 `claude` 重启 · 对齐 08b §8.10 Phase 起点)
- [ ] **切换模型确认**(V4 Pro · 跟 doc-writer 家族同向)
- [ ] **章节顺序 + 节名对齐 init-skeleton.md 行 277-345 占位 8 节**(双胞胎硬约束 · 不重命名 / 不重排 / 不删减)
- [ ] **占位 8 节齐全**:① 项目简介 ② 技术栈 ③ 项目结构 ④ 数据库设计 ⑤ API 接口 ⑥ 快速开始 ⑦ 文档索引 ⑧ 验收清单
- [ ] **追加 2 个完善节**:§九 AI 协作 + §十 联系方式(Phase 8 新增 · 学生填占位用 ✍)
- [ ] **§一 项目简介**:`{{题目}}` `{{核心实体}}` `{{角色列表}}` 占位符全部替换 · 当前 Phase 改为"Phase 8(完成态)"
- [ ] **§二 技术栈**:跟 CLAUDE.md §一·一 2026-05-10 基线 **100% 一致**(JDK 21 / SpringBoot 3.5.14 / MyBatis-Plus 3.5.15 / MySQL 8.4 / Node 24 / Vue 3.5.34 / Vite 8.0.0 等)+ 加默认账号密码 + 依赖环境清单
- [ ] **§三 项目结构**:6 个顶层目录(backend/ + frontend/ + docs/ + sql/ + ai-records/ + .claude/)+ 根目录 CLAUDE.md + 各自一句话说明
- [ ] **§四 数据库设计**:实际表数量 N(对照 sql/01-init.sql `CREATE TABLE` 数量)+ 实际表名清单
- [ ] **§五 API 接口**:实际接口数量 N(对照 docs/API_DESIGN.md §2 接口清单)+ URL 前缀 `/api/...`
- [ ] **§六 快速开始**:2 大段命令(后端 + 前端)+ ⚠️ 改密码警告 + 💡 装 pnpm 提示 **完整保留**(对齐 init-skeleton 占位 §六)
- [ ] **§七 文档索引**:7 个链接齐全(PRD / TECH_DESIGN / DATABASE_DESIGN / API_DESIGN / DEPLOY / 对话记录/ / 00-选题标定)
- [ ] **§八 验收清单 4 项硬地基齐全**:5 项硬地基 / commit ≥ 30 / ai-records ≥ 21 + ≥ 3 个 v1→v2→v3 演化 / docs/ 完整(对齐 init-skeleton 占位 §八 + 05 验收方案)
- [ ] **§八 P0/P1/P2 完成度自评** 逐条对应 docs/00-选题标定.md § 三/§ 四/§ 五 功能清单(教师可逐条对照)
- [ ] **§九 AI 协作**:链 CLAUDE.md + 对话片段实际数 + 演化记录数(≥ 3)+ 关键演化场景(可选)
- [ ] **§十 联系方式**:学号 + 班级 ✍ 必填占位 · 姓名 + 邮箱 ✍ 可选占位 + 隐私警告
- [ ] **学生填占位用 ✍**(P0/P1/P2 自评 / 学号 / 班级 / 姓名 / 邮箱 / 关键演化场景 等)
- [ ] **未编造数据**(表数量 / 接口数量 / commit 数 / ai-records 数 全部从源文档/git/对话记录目录实际取数 · 不写"约 30 次"等模糊措辞)
- [ ] 文档地图链接全部正确(`docs/PRD.md` 等相对路径)

## 衔接

下一步:

1. **学生填 ✍ 占位字段**(学号 / 班级 / 姓名 / 邮箱 / P0/P1/P2 自评 / 关键演化场景等)· **不能让 ✍ 占位上传 Gitee**(教师验收会扣分)

2. **跑一遍 README §六 快速开始**(实测验证):全新目录 git clone 项目 → 按 README 步骤跑通 → 验证文档可用性

3. **`/git-committer`** 提交完善后的 README:
   ```
   /git-committer 请 commit + push:docs(p8): 完善 README 10 节 + 验收说明对齐 05 验收方案
   ```
   累计 commit:32-33 次

4. **(若 Phase 8 全部完成)** Gitee push 公开仓库 + 教师验收对照 § 八 验收清单逐条核对

5. **(收尾)** Phase 8 全部跑完后:`/rules-updater` 同步 `project-status.md` 「Phase 8 完成」(对齐 rules-updater §二 单字段更新模式 · ⚠️ 同步的是 project-status.md,不动 CLAUDE.md)

## 设计要点

- **双胞胎模式 · 内容写入器**(对齐文档行 85-87 决策档案 · doc-writer 家族标配):readme-writer 是 init-skeleton README.md 8 节占位的"内容写入器" · 章节结构 100% 一致(顺序 + 节名 + 占位 8 节)· 末尾仅追加 §九/§十 2 个 Phase 8 完善节(因 Phase 0 时这俩节学生还没东西填)· **若需扩展占位 8 节,改 init-skeleton 占位**(权威源)而非命令端
- **必须切换模型**(V4 Pro · 跟 doc-writer 家族同向):README 跨 Phase 引用最多 + 教师验收第一眼看 + Gitee 公开仓库公开展示 · 内容质量重要 · 不切模型(V4 Flash 文采不足)
- **从 Phase 0 占位 → Phase 8 完整版的"演化"**:占位 8 节填字段 `{{题目}}` `表数量:N` `commit ≥ 30` 等 · readme-writer 替换为实际值;**不重写章节结构**(双胞胎硬约束)· 仅追加 2 个完善节
- **跟 deploy-writer 横向协同**(Phase 8 双胞胎 · 同 doc-writer 家族 · 同 V4 Pro):readme-writer §6 快速开始 / §7 文档索引引用 docs/DEPLOY.md · readme-writer 必须在 deploy-writer 之后跑(Phase 8 Step 3 vs Step 1-2)
- **学生填 ✍ 占位规约**(P0/P1/P2 自评 / 学号 / 班级 / 姓名 / 邮箱 / 关键演化场景):readme-writer 不替学生填,留 ✍ 占位提醒;教学场景敏感字段(邮箱 / 完整姓名)加隐私权衡说明
- **04/05 验收方案对齐**:§八 验收清单 4 项硬地基(5 项硬地基 / commit ≥ 30 跨度 ≥ 12 天 / ai-records ≥ 21 + ≥ 3 个 v1→v2→v3 演化 / docs/ 完整)是教师验收第一对照点 · 不能简化为 P0/P1/P2 自评(那是补充层次)
- **学生项目典型踩坑场景**:① ✍ 占位字段未填上传 Gitee(教师验收看到 `✍ <必填>` 字面量 · 扣分)② §二 技术栈版本号跟 CLAUDE.md / CLAUDE.md §一 不一致(漂移)③ §六 快速开始漏改密码警告 / 漏装 pnpm 提示(踩坑高频)④ §四 表数量编造(说"约 8 张"但 sql/01-init.sql 实际 12 张)⑤ §九 AI 协作演化记录数 < 3 个(扣 05 验收方案分)

---

> 📋 **跨文件呼应导航**:
> - **上游产出**(占位 8 节填实际内容的源):`init-skeleton.md` 行 277-345 README.md 8 节占位(**双胞胎权威源**) + `docs/00-选题标定.md § 一/§ 三/§ 四/§ 五`(§一 + §八 P0/P1/P2 自评源)+ `docs/PRD.md §3`(§一 印证)+ `docs/DATABASE_DESIGN.md §2 + sql/01-init.sql`(§四 表数量)+ `docs/API_DESIGN.md §2`(§五 接口数量)+ `docs/DEPLOY.md`(§六 快速开始引用 + §七 文档索引)+ `docs/TECH_DESIGN.md §1`(§三 项目结构印证)+ `CLAUDE.md`(§二 技术栈印证 + §九 AI 协作链)+ `docs/对话记录/` + `ai-records/`(§九 AI 协作统计)
> - **平行规则**:`CLAUDE.md §一·一`(技术栈基线 · §二 对齐源 · 2026-05-10)+ `§二·一`(全栈接口契约 · §五 印证)+ `§三`(中文注释 · README 中文 · 占位 ✍ 中文化)+ `CLAUDE.md §二·一` 8 类(§三 项目结构印证)+ `CLAUDE.md §三·一` 8 类(同上)
> - **横向协同**(Phase 8 doc-writer 双胞胎):`deploy-writer.md`(同 Phase 8 · 同 V4 Pro · 同 doc-writer 家族 · readme-writer §6/§7 引用 docs/DEPLOY.md)· **必须在 deploy-writer 之后跑**(08b §8.10 Step 3 vs Step 1-2)
> - **下游消费**:`git-committer.md`(commit message `docs(p8): 完善 README 10 节 + 验收说明对齐 05 验收方案`)+ Gitee push(公开仓库展示)+ 教师验收(§八 4 项硬地基 + P0/P1/P2 自评对照标定卡)
> - **教学源头**:`06-提示词与审核模板库.md` G-15 段(行 677-688 · 默认账号密码 + 依赖环境清单 · 命令引用即可,无需修改源头)+ `04-课件大纲.md / 05-验收方案 V4-2.md`(§八 4 项硬地基对齐源)
> - **doc-writer 家族标杆**:`srs-writer.md`(Phase 1 · PRD)+ `tech-designer.md`(Phase 1 · TECH_DESIGN)+ `db-designer.md`(Phase 2 · DATABASE_DESIGN)+ `api-designer.md`(Phase 3 · API_DESIGN)+ `deploy-writer.md`(Phase 8 · DEPLOY)· **本命令是 doc-writer 家族第 6 个 + 双胞胎模式第 6 次应用**
> - **rules-updater**:Phase 8 全部完成后 `/rules-updater` 同步 `project-status.md` 「Phase 8 完成」(对齐 rules-updater §二 单字段更新模式 · ⚠️ 同步的是 project-status.md,不动 CLAUDE.md)
