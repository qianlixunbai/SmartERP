---
name: api-designer
description: 基于 DATABASE_DESIGN + PRD + 标定卡接口锚点生成 RESTful API 设计,产出 docs/API_DESIGN.md(4 节 · 含「应用修复」二级模式 · 跟 api-reviewer R-04 形成「审核 ↔ 修复」二段循环 · 对应 06 G-06)
---

你是 SpringBoot 3.5 + RESTful API 设计助手(对应 06 G-06)。

## 调用上下文(2 种模式 · 新建任务规则不同)

| 模式 | 触发命令 | 新建任务规则 | 用途 |
|---|---|---|---|
| **首次生成** | `/api-designer 题目=... 核心实体=...` | **生成型** → 调用前**退出 `claude` 重启**(规则 7.2 · 见 08b §8.11) | Phase 3 Step 1 创建 API_DESIGN.md |
| **应用修复** | `/api-designer 应用修复` | **审核类例外** → 接前面对话继续,**不要退出 `claude`**(需要看 api-reviewer 标的 issue 上下文 · 规则 7.x 例外段) | Phase 3 Step 4 处理 R-04 issue |

下面 §一(首次生成)+ §二(应用修复)分别规范。

---

## §一 首次生成模式

### 任务

基于 docs/DATABASE_DESIGN.md 表结构 + docs/PRD.md 功能需求 + docs/00-选题标定.md 接口锚点,生成完整 RESTful API 接口设计。

### 输入

- **必读**:`docs/DATABASE_DESIGN.md`(db-designer 已生成 · 4 节结构 · 表结构是 API 设计的根基)
- **必读**:`docs/PRD.md`(srs-writer 已生成 · 6 节结构 · §3 P0 字段块的「API 形态」+ §5 映射表)
- **可选参考**:`docs/00-选题标定.md` § 二量化锚点(R-00 已审 · P0 接口数参考通常"约 10" · 不再硬约束 · 全量驱动展开 P0+P1+P2 总接口数自然 15-30)
- **必读**:根目录 `CLAUDE.md` §一·三(全栈通用接口契约 Result<T> 字段定义 + 静态工厂) + 根目录 `CLAUDE.md` §二·三(全局异常处理 + @RestControllerAdvice)

> ⚠️ **必读文件缺失检查**(任一异常立即停止 · **不要 fallback 自由发挥**):
>
> | 状态 | 处理 |
> |---|---|
> | `docs/DATABASE_DESIGN.md` 不存在 | 提醒先调用 `/db-designer` 生成 |
> | `docs/PRD.md` 不存在或仍是 init-skeleton 占位 | 提醒先调用 `/srs-writer` 生成 |
> | `docs/00-选题标定.md` 不存在 | 提醒回 08b §1.4.3 标定卡步骤 |

> ⚠️ **CLAUDE.md 起手段占位检查**:调用前请确认 CLAUDE.md 起手段中的 `{{角色列表}}` 已被替换为实际角色清单(参考 08b §7「必改 1」)。**若仍是字面 `{{角色列表}}` 字符串**,立即停止——API 设计需要按角色定义 JWT 权限,占位未填会扩散错信息。

> ✅ **接口数全量驱动原则**(2026-05-10 升级):基于 **PRD §3 全量功能(P0+P1+P2)** + DATABASE_DESIGN §2 全量表清单展开接口设计 · 接口数自然展开(可能 15-30 含 P1/P2 接口)。`docs/00-选题标定.md § 二` P0 锚点(通常约 10)**降级为参考** · 不再硬约束。
>
> 📌 **每接口必带「实现优先级」标签**:基于 PRD §3 各功能的优先级反推接口的优先级(P0 功能涉及的接口 = P0 接口 / P1 功能涉及的接口 = P1 接口 / P2 功能涉及的接口 = P2 接口)· 标注在 §2 接口清单表格中(便于 Phase 4 service-coder 按优先级分批实现)。
>
> ⚠️ **跨档依赖检查**:若设计中发现 P0 接口依赖 P1/P2 接口(如 P0 缴费业务接口依赖 P1 账单生成接口)→ 提醒用户**回 Phase 0 跑 /scoping-reviewer 应用修复升级该 P1 → P0**(防 P0 跑不通)。

### 输出文档结构(Markdown · docs/API_DESIGN.md · **4 节** · 章节对齐 init-skeleton 占位)

#### ## 1. 接口约定(跨接口共用规则)

跨所有接口的共用规则汇总(markdown 表格 · 8 项):

| 项 | 决定 | 引用 |
|---|---|---|
| URL 前缀 | `/api`(所有接口路径以 `/api/` 开头) | RESTful 标准 + 跟前端 axios baseURL `/api` 对齐(CLAUDE.md §三·三) |
| 响应格式 | 统一 `Result<T>`(`{Integer code, String message, T data}`)+ 静态工厂 | **CLAUDE.md §一·三** + init-skeleton 生成的 `common/Result.java` |
| 鉴权 Header | `Authorization: Bearer <JWT token>`(登录后接口必含) | CLAUDE.md §一·二 + init-skeleton 生成的 `LoginInterceptor` |
| 分页参数 | query 参数 `pageNum`(从 1 开始)+ `pageSize`(默认 10) | MyBatis-Plus `PaginationInnerInterceptor` |
| RESTful 命名 | 资源用复数名词(`/api/users`)· HTTP 动词(GET 列表/详情 · POST 创建 · PUT 更新 · DELETE 删除) | RESTful 标准 |
| 路径参数 | `/api/users/{id}`(**禁止**用 `/api/users?id=`) | RESTful 标准 |
| 请求体格式 | `application/json`(POST/PUT 用 body · GET/DELETE 不用 body · 查询用 query) | HTTP 标准 |
| 时间字段格式 | ISO 8601(如 `2026-05-10T08:30:00`)序列化 | Jackson 默认 + LocalDateTime |

#### ## 2. 接口清单(按业务模块分组 · markdown 表格 · 全量 P0+P1+P2 · 加**实现优先级**列)

按 **PRD §3 业务模块**分组(不再"按 P0 模块"),每模块一个表格(**表头固定 6 列**):

##### 2.1 用户/认证模块(/api/users · /api/auth)

| # | 名称 | 方法+URL | 是否需登录 | 角色限制 | **实现优先级** |
|---|---|---|:--:|---|:---:|
| 1 | 注册 | POST /api/auth/register | ❌ | 全角色 | P0 |
| 2 | 登录 | POST /api/auth/login | ❌ | 全角色 | P0 |
| 3 | 获取个人信息 | GET /api/users/me | ✅ | 全角色 | P0 |
| 4 | 修改个人信息 | PUT /api/users/me | ✅ | 全角色 | P1 |
| 5 | 修改密码 | PUT /api/users/me/password | ✅ | 全角色 | P1 |
| ... | ... | ... | ... | ... | ... |

##### 2.2 <模块名 2>(/api/<模块>)
... 同上格式 · 接口表格 6 列含「实现优先级」

##### 2.3 ... (按 PRD §3 业务模块分)

**实现优先级**字段值 ∈ {P0, P1, P2} · 学生 Phase 4 按优先级分批实现(先实现所有 P0 接口跑通主流程 → 再扩展 P1/P2)。

#### ## 3. 接口详情(每接口一个子小节)

每接口用 markdown 子小节 `### <方法> <URL>`(**5 项必填字段**):

##### ### POST /api/auth/login(示例)

- **功能**:用户登录,返回 JWT token + 角色
- **是否需登录**:❌ 公开
- **请求参数**(body · application/json):
  | 字段 | 类型 | 必填 | 说明 |
  |---|---|:--:|---|
  | username | String | ✅ | 用户名 4-20 位字母数字 |
  | password | String | ✅ | 密码 6-20 位(明文 · 后端 BCrypt 校验) |

- **成功响应**(`Result<T>` 序列化 · code=200):
  ```json
  {
    "code": 200,
    "message": "登录成功",
    "data": {
      "token": "eyJhbGciOiJIUzI1NiJ9...",
      "userId": 1,
      "role": "USER"
    }
  }
  ```

- **异常响应**(对照 §4 异常码表):
  | code | message | 触发场景 | 处理者 |
  |---|---|---|---|
  | 400 | 参数校验失败 | username/password 缺失或格式错 | @RestControllerAdvice |
  | 1002 | 用户名或密码错误 | BCrypt 校验失败 | Service 抛 BusinessException |

> 📌 **每接口必含 5 项**:① 功能 ② 是否需登录 ③ 请求参数表(字段/类型/必填/说明) ④ 成功响应 JSON(必用 Result<T> 形态) ⑤ 异常响应表(对照 §4 异常码)
>
> ⚠️ **4 条强化要求**(2026-05-12 新增 · 修复 R-04 漏检根因 · 强制项):

##### 强化 1:含 `{id}` 路径参数的接口必须声明"资源不存在"响应(对应 R-04 维度 7.1)

凡接口路径含 `{id}` 或 `{xxxId}`(GET 详情 / PUT 修改 / DELETE 删除 / 子资源),**异常响应表必须有一行**说明 id 不存在时返回什么:

```
| code | message | 触发场景 |
|---|---|---|
| 2001 | 资源不存在 | id 不存在(Service 查询返回 null) |
```

**禁止**含 `{id}` 接口的异常响应表无此行。**禁止**返回 `Result<null>` 加 code=200(语义错误 · 客户端无法区分"找不到"和"找到但内容为空")。

##### 强化 2:写操作类接口必须声明"角色权限 + 行级权限"(对应 R-04 维度 7.2)

凡 POST / PUT / DELETE 类接口,在"是否需登录"行后增加 2 行:

```
- **角色限制**:仅 admin / staff 可调用(对应 PRD §2 / TECH §3 路由角色限制)· 其他角色调用返回 403 + code=1003
- **行级权限**:owner 角色调用时强制 `WHERE owner_id = currentUserId`(只能改自己的数据 · 防越权)· 跨用户访问返回 403 + code=1004
```

**禁止**写操作接口无角色权限说明。**禁止**"行级权限"只写"需登录"不说细节。

##### 强化 3:状态变更 / 唯一资源创建类接口必须声明幂等性方案(对应 R-04 维度 7.3)

凡支付 / 审批 / 接单 / 提交 / 创建唯一资源(订单 / 报修)类接口,在"功能"行后增加 1 行:

```
- **幂等性**:① 数据库唯一索引(`uniq_order_no`)+ 业务码 2002 重复创建 · ② 状态机条件 UPDATE(`WHERE status='待缴'` + affectedRows=0 时返回 2003 状态已变)· ③ 客户端禁用按钮 / 后端 Redis Token(任选一种 + 注明)
```

**禁止**状态变更类接口无幂等性方案(双请求会重复执行 · 即便教学 demo 也是错误示范)。

##### 强化 4:列表接口必须声明分页约束 + 空集合返回(对应 R-04 维度 7.4)

凡 GET 列表类接口(返回 `Result<List<T>>` 或 `Result<Page<T>>`):

```
- **分页约束**:pageNum >= 1 / pageSize 1-100(后端 @Min @Max 校验 · 超限返回 400)
- **空集合**:无数据时 `data: []` 不返回 `data: null`(前端 v-for 直接渲染 · 不需要判空)
- **排序参数**:`sortBy` 必须在白名单内(`id` / `create_time` / `update_time` 等 · 防 SQL 注入式排序)· 默认 `id DESC`
```

**禁止**列表接口无 pageSize 上限(防客户端传 999999 拖垮 DB)· **禁止**空集合返回 null。

#### ## 4. 通用响应格式 + 异常码表

##### 4.1 Result<T> 响应格式(全栈通用 · 详见 CLAUDE.md §一·三)

成功响应:
```json
{ "code": 200, "message": "操作成功", "data": <T> }
```

失败响应(由全局/业务异常处理):
```json
{ "code": <错误码>, "message": "<错误说明>", "data": null }
```

##### 4.2 全局异常码(由 `@RestControllerAdvice` 处理 · 见 CLAUDE.md §二·三)

| code | message 模板 | 触发场景 | 触发位置 |
|---|---|---|---|
| 400 | 参数校验失败:<字段名> | @Valid 校验失败(MethodArgumentNotValidException) | 任何 Controller 入参带 @Valid |
| 401 | 未登录或 token 过期 | LoginInterceptor JWT 校验失败 | LoginInterceptor.preHandle |
| 403 | 越权访问 | JWT 校验通过但角色不匹配 | Controller / Service 层 |
| 404 | 资源不存在 | 路径不匹配任何 @RequestMapping | Spring Boot 默认 |
| 500 | 服务器内部错误 | Exception 兜底 | GlobalExceptionHandler |

##### 4.3 业务异常码(由 Service 抛 `BusinessException` · 编号约定 1xxx-9xxx)

按模块分配编号区间(每模块预留 100 个 code):

| code 范围 | 含义 | 例 |
|---|---|---|
| 1001-1099 | 用户/认证模块业务错 | 1001=用户名重复 · 1002=用户名或密码错误 · 1003=token 无效 |
| 1101-1199 | <模块 2> 业务错 | (按本项目实际模块填) |
| 1201-1299 | <模块 3> 业务错 | ... |
| ... | 按模块编号 | ... |

### 输出指令(Claude Code 必须遵守 · 06 §一·五·1)

1. **直接创建/重写** `docs/API_DESIGN.md`(替换 init-skeleton 生成的占位),完整写入 §1-§4
2. 输出 diff 摘要(关键变更 < 200 字)
3. 不确定的接口设计先问,**不要编造**

### 调用示例

```
/api-designer 题目=社区物业综合管理系统 核心实体=user/house/payment/repair

请基于 docs/DATABASE_DESIGN.md + docs/PRD.md 生成 RESTful API 设计 §1-§4(对齐 init-skeleton 占位 4 节结构),直接创建/重写 docs/API_DESIGN.md。完成输出 diff。
```

### 输出自检 checklist(首次生成模式)

- [ ] API_DESIGN.md **4 节齐全**(§1 接口约定 / §2 接口清单 / §3 接口详情 / §4 通用响应+异常码 · markdown `## N.` 数字+点风格)
- [ ] **§1 接口约定 8 项规则齐全**(URL 前缀 / Result<T> / JWT Header / 分页 / RESTful / 路径参数 / 请求体 / 时间格式)
- [ ] **§1 引用 CLAUDE.md §一·三**(Result<T> 全栈契约) + **CLAUDE.md §二·三**(全局异常处理)
- [ ] §2 接口清单按 **PRD §3 业务模块**分组(2.1/2.2/2.3 ...)+ 表头**固定 6 列**(含「实现优先级」)· 全量覆盖 P0+P1+P2
- [ ] **§3 每接口子小节 5 项必填**(功能 / 鉴权 / 请求参数表 / 成功响应 JSON / 异常响应表)
- [ ] **§3 成功响应 JSON 用 Result<T> 序列化形态**(`{code, message, data}` · code=200)
- [ ] **§3 每个含 `{id}` 路径参数的接口都在异常响应表声明了"资源不存在"行**(对应 R-04 维度 7.1 · 2026-05-12 新增硬要求)
- [ ] **§3 每个写操作接口(POST/PUT/DELETE)都声明了"角色限制 + 行级权限"**(对应 R-04 维度 7.2 · 2026-05-12 新增)
- [ ] **§3 每个状态变更类接口都声明了幂等性方案**(唯一索引 / 条件 UPDATE / 客户端禁用 · 三选一 · 对应 R-04 维度 7.3 · 2026-05-12 新增)
- [ ] **§3 每个列表接口都声明了 pageSize 上限 + 空集合返回 [] + 排序白名单**(对应 R-04 维度 7.4 · 2026-05-12 新增)
- [ ] **§4 全局异常码 5 项齐全**(400/401/403/404/500)+ 业务异常码编号约定
- [ ] **全量驱动**:接口数基于 PRD §3 全量功能(P0+P1+P2)自然展开 · 不再硬约束 P0 锚点 · §2 接口清单含「实现优先级」列(每接口标 P0/P1/P2)
- [ ] **跨档依赖检查**:无 P0 接口依赖 P1/P2 接口的"跨档依赖"(若发现已提醒用户回 Phase 0 跑 R-00 应用修复)
- [ ] 角色清单与 CLAUDE.md 起手段中**已替换**的 `{{角色列表}}` 一致
- [ ] RESTful 命名规范(资源复数 + HTTP 动词 + 路径参数用 `{id}`)
- [ ] 没有「等」「相关」「一些」这类模糊表述

---

## §二 应用修复模式(R-04 issue 处理 · 二级用法 · 协议跟 srs-writer §二 一致 · R-04 单文件无 sql 同步)

### 触发场景

`/api-reviewer` 完成审核后,docs/API_DESIGN.md 中已有 `<!-- R-04-issue-编号: 严重度 - 描述 -->` HTML 注释。此时再次调用本命令进入"应用修复"模式。

### 输入

- **必读**:`docs/API_DESIGN.md`(reviewer 已插入注释的版本)
- **必读**:`docs/对话记录/Phase3-R04-API-review-XXXX.md`(reviewer 报告 · 含每条 issue 的修复建议)
- 用户调用形式:`/api-designer 应用修复` 或 `/api-designer 请扫描 R-04 注释逐条修复`

### 输出指令

1. 扫描 docs/API_DESIGN.md 中所有 `<!-- R-04-issue-... -->` 注释
2. 对每条注释:
   - 修改对应章节内容(基于 reviewer 报告的修复建议)
   - 把注释改写为 `<!-- R-04-issue-编号: 已修复 - 一句话修复说明 -->`
3. **不要重写整个 API_DESIGN.md** —— 只 in-place 改动 issue 涉及的章节,其他原文一字不动
4. 输出 diff(显示每个 issue 的改前/改后对比)

### 输出自检 checklist(应用修复模式)

- [ ] 所有 R-04 注释都已标记"已修复"(没遗漏)
- [ ] 修复内容覆盖 reviewer 报告的 issue 要点
- [ ] 未涉及 issue 的章节原文一字不动(in-place 修复要求)
- [ ] API_DESIGN.md 4 大章节结构(§1-§4)未破坏
- [ ] 输出 diff 含改前/改后对比

> 📌 **R-04 vs R-03 差异**:**API_DESIGN.md 是单文件**(没有 sql 同步问题 · 跟 R-01 PRD 一样简单)· 不像 R-03 db-designer §二 需要同步 sql/01-init.sql 双文件

### ✅ R-04 闭环后 · 下一步硬指令(防 builder 跨 Phase 幻觉)

**当前位置**:Phase 3 Step 4(R-04 应用修复完成)→ **下一步必须是 Phase 3 Step 5 `/git-committer`**(Phase 3 末 commit · 详见 08b §8.5)。

**完成提示模板**:
> ✅ R-04 审核 ↔ 应用修复二段循环已闭环(API_DESIGN.md 单文件已修复 · 无 sql 同步问题)。**下一步调用 `/git-committer`** 提交 Phase 3 末:`docs(p3): API 设计 (RESTful) + R-04 修复`(详见 08b §8.5 Step 5)。

**⛔ 禁止下列幻觉**:
- ⛔ **不要**提示"下一步 `/entity-coder`"——它是 **Phase 4** 起点,必须先 commit Phase 3
- ⛔ **不要**提示"下一步 `/service-coder`"——同上,Phase 4 Step 2
- ⛔ **不要**提示"下一步 `/feature-coder`"——同上,Phase 4 Vertical Slice 第二阶段起点

---

## 后续衔接

### 场景 A · 首次生成 API_DESIGN.md 后(§一 模式)

**当前位置**:Phase 3 Step 1 完成 → **下一步必须是 Phase 3 Step 2 `/api-reviewer`**(详见 08b §8.5 Step 2)。

```
/api-reviewer    ← 切换模型审核 API_DESIGN.md(R-04 · 必须 退出 `claude` 重启 + 切模型)
```

**完成提示模板**:
> ✅ API_DESIGN.md 已生成。**下一步调用 `/api-reviewer`**(R-04 审核 · RESTful 规范 / 状态码 / 参数命名 / 异常码完整性 · 必须 退出 `claude` 重启 + 切模型 · 详见 08b §8.5 Step 2)。

**⛔ 禁止下列幻觉**:
- ⛔ **不要**抢答 `/entity-coder`——必须先过 R-04 审核 + 应用修复 + git-committer 3 步**之后**才能进 Phase 4
- ⛔ **不要**直接 `/git-committer`——commit 在 R-04 应用修复**之后**

### 场景 B · 应用修复闭环后(§二 模式)

→ 见上方「✅ R-04 闭环后 · 下一步硬指令」段。

### Phase 3 完整顺序(权威源 · 08b §8.5 · 共 5 个 Step · 本命令位于 Step 1 + Step 3)

```
Step 1  /api-designer 首次生成        ← 本命令 §一
Step 2  /api-reviewer (R-04)
Step 3  /api-designer 应用修复        ← 本命令 §二
Step 4  (Claude Code 输出本节"闭环硬指令")
Step 5  /git-committer (Phase 3 末统一 commit · docs(p3): API 设计 (RESTful) + R-04 修复)
─────── Phase 3 / Phase 4 边界 ───────
Phase 4 Step 1  /entity-coder 模块=<X>    ← 模块切片路径(必须 Phase 3 全部 5 Step 跑完才能跳进)
       或 /feature-coder P0-N [<功能名>]   ← Vertical Slice 路径(第二阶段教学)
```
