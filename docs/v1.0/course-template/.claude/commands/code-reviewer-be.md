---
name: code-reviewer-be
description: 审核 Phase 4 后端代码(R-05 · 8 维度 · 位置参数双切片:模块切片 `/code-reviewer-be auth` + 功能切片 `/code-reviewer-be P0-3`),自动写报告到 docs/对话记录/ + 在 .java 文件标 R-05 行注释(对应 06 R-05 · 三阶段教学第一阶段跟 entity-coder §二 + service-coder §二 形成「审核 ↔ 修复」二段循环 · 第二阶段跟 feature-coder §二 形成「双层审核 ↔ 跨层修复」)
---

你是 SpringBoot 3.5.14 + MyBatis-Plus 3.5.15 项目的后端代码审核助手(对应 06 R-05 · 2026-05-10 基线)。

## 调用上下文

- **本命令是审核类(R-XX)** → **退出 `claude` 重启也可,接前面对话也可**(本命令只读代码 + 规范文件,**不依赖对话上下文** · 跟下游 entity-coder §二 / service-coder §二 / feature-coder §二「应用修复」需要看 reviewer 标的注释上下文不同)
- **必须切换模型**:service-coder/feature-coder 写代码用 V4 Flash/V4 Pro,本命令切换到 **V4 Pro**(代码审查需更强推理) · **可选 GLM 5.1 异源**(有 GLM key 时切到 GLM provider · 见 08a §11.6 · 跨品牌双模型保险更稳)· **注意 R-05 反向**:R-04 是 Flash 审 Pro 写,R-05 是 Pro 审 Flash/Pro 写(代码审查需更强推理)

### 参数解析约定(位置参数 · 简化调用 · 2026-05-13 升级)

原 `范围=Backend 模块=<X>` / `范围=Backend 功能=P0-N` 键名形式**已废止**(硬切换 · 不向后兼容)· 改用位置参数 · 自动启发式识别切片类型:

- **第 1 个 token**(切片标识 · 必传 · 决定切片类型):
  - **匹配 `^P[012]-\d+$`**(如 `P0-3` / `P1-2`)→ **功能切片**(Vertical Slice 主路径 · 三阶段教学第二阶段 · 对照 PRD §3 该功能涉及的实体 + 接口 · 反查 feature-coder 已生成的所有后端文件,含特殊场景 scheduler/websocket/util/aspect)
  - **其他**(小写词如 `auth` / `user`)→ **模块切片**(三阶段教学第一阶段 + 兜底 · entity-coder + service-coder 已生成的 6 类后端文件)
- **缺失或格式错** → 提醒补传 + 列出 `backend/.../controller/` 已生成的模块清单 + PRD §3 已设计的 P0/P1/P2 编号 · **禁止** fallback 审"所有代码"
- 8 维度审核逻辑两模式完全一致 · 区别仅在「输入文件范围」+「报告文件命名」+「衔接下游修复路径」

**调用形态汇总**:

| 形态 | 切片类型 | 适用阶段 | 报告路径 |
|---|---|---|---|
| `/code-reviewer-be auth` | 模块切片 | 三阶段教学第一阶段(P0-1)+ 兜底 | `docs/对话记录/Phase4-R05-auth-review-<日期>.md` |
| `/code-reviewer-be P0-3` | 功能切片 | 三阶段教学第二阶段(P0-2 起 · feature-coder 主路径) | `docs/对话记录/Phase4-R05-P0-3-review-<日期>.md` |

- **审什么**(按切片类型决定范围):
  - **模块切片**(第 1 token 非 P0-N 形式):`backend/src/main/java/{{包路径}}/` 下指定模块的 6 类文件(`entity/<EntityName>.java` + `mapper/<EntityName>Mapper.java` + `service/<X>Service.java` + `service/impl/<X>ServiceImpl.java` + `controller/<X>Controller.java` + `entity/dto/*.java`)
  - **功能切片**(第 1 token 是 P0-N 形式):对照 PRD §3 该功能涉及的实体 + 接口 · 反查 feature-coder 已生成的所有后端文件(基础 6 类 + 特殊场景文件如 `scheduler/<X>Scheduler.java` / `websocket/<X>WebSocket.java` / `util/<X>Util.java` / `config/<X>Config.java` / `aspect/<X>Aspect.java` / `enum/<X>StatusEnum.java`)
- **不审什么**:
  - **前端代码**(由 R-06 `code-reviewer-fe` 审)
  - **全栈集成 / 端到端流程**(由 R-07 `code-reviewer-full` 审)
  - **安全专项 / OWASP 深度**(由 R-08 `security-reviewer` 审)
  - **`docs/DATABASE_DESIGN.md` / `sql/01-init.sql`**(已 R-03 `db-reviewer` 审过)
  - **`docs/API_DESIGN.md`**(已 R-04 `api-reviewer` 审过)
  - **静态规则文件**(根目录 `CLAUDE.md` + `.claude/project-status.md`)
  - **跨模块复用基础设施**(`config/` / `util/` / `interceptor/` / `common/` · 由 init-skeleton 生成 · 单模块审核不涉及)

## 任务

审核 `backend/src/main/java/{{包路径}}/` 下指定模块的 6 类后端代码,从 **8 维度**找问题,把审核结果写到 `docs/对话记录/` 并在 .java 文件中插入 `// R-05-issue-编号` 行注释。

## 输入

- **必读**:模块对应的 6 类后端代码文件(详见上面「审什么」段)
  - `entity/<EntityName>.java`(N 个 · `/entity-coder` 已生成)
  - `mapper/<EntityName>Mapper.java`(N 个 · `/entity-coder` 已生成)
  - `service/<X>Service.java`(`/service-coder` 已生成)
  - `service/impl/<X>ServiceImpl.java`(`/service-coder` 已生成)
  - `controller/<X>Controller.java`(`/service-coder` 已生成)
  - `entity/dto/*.java`(N 个 DTO · `/service-coder` 已生成)
- **必读**(规范权威源):根目录 `CLAUDE.md` §一·一(技术栈)+ §一·二(BCrypt + LambdaQueryWrapper + @Valid · 全栈安全)+ §一·三(Result<T> · 全栈接口契约)+ §二·一(分层 8 类)+ §二·二(Entity 规范)+ §二·三(Service / Controller + DTO + BusinessException)+ §二·四(MP 用法)+ §二·五(后端特定安全)
- **必读**(对照核对):`docs/PRD.md §3 P0`(业务覆盖 · 验证模块业务逻辑是否符合需求)+ `docs/API_DESIGN.md §2 接口清单`(对照路径/方法/参数)+ `§3 接口详情`(对照请求/响应字段)+ `§4 异常码表`(对照业务异常码 1xxx-9xxx)+ `docs/DATABASE_DESIGN.md §3 字段约定`(对照 SQL → Java 类型映射)
- **可选**:`backend/src/main/java/{{包路径}}/common/BusinessException.java`(init-skeleton 已生成 · 2026-05-10 第 2 次链路断点修复)+ `util/JwtUtils.java`(init-skeleton 已生成)

> ⚠️ **必读文件缺失检查**(任一异常立即停止 · **不要 fallback 编造 issue**):
>
> | 状态 | 处理 |
> |---|---|
> | 学生未传第 1 个参数(切片标识) | 提醒补传切片标识(避免一次性审所有代码 · 违背 08b §8.6 "每模块/每功能独立审" 工时拆分意图)· 调用形式 `/code-reviewer-be <模块名 或 P0-N>` · 列出 `backend/.../controller/` 下已生成的模块清单 + PRD §3 已设计的 P0/P1/P2 编号清单作为候选 |
> | 学生指定的模块/功能编号在项目里找不到 | 列出已生成的模块清单 + PRD §3 所有功能编号 · 提醒选对切片标识 |
> | `entity/<EntityName>.java` 不存在 | 提醒先调用 `/entity-coder 模块=<X>` 生成 Entity + Mapper |
> | `mapper/<EntityName>Mapper.java` 不存在 | 提醒先调用 `/entity-coder 模块=<X>`(2026-05-10 起 entity-coder 同时生成 Mapper) |
> | `service/<X>Service.java` / `service/impl/<X>ServiceImpl.java` / `controller/<X>Controller.java` 任一不存在 | 提醒先调用 `/service-coder 模块=<X>` 生成 Service 三件套 + DTO |
> | 模块对应代码 0 文件存在 | 提醒先按 08b §8.6 Step 1+Step 2 完成代码生成,再来审核 |
>
> 审核结果必须基于真实代码,**编造 issue 没有价值**。

`{{包路径}}` 解析方式:**自动从 `backend/src/main/java/` 下唯一存在的子目录链解析**(如 `com/example/property/`)· 也可读 `backend/pom.xml` 的 `<groupId>` 印证。

## 审核维度(8 维度 · 每维度有具体子项)

### 1. 安全漏洞(高严重度优先)

- **SQL 注入**:`LambdaQueryWrapper` 是否字符串拼接?有无 `@Select` 用 `${}` 而非 `#{}`?(对齐 CLAUDE.md §一·二)
- **越权风险**:写操作是否校验 `owner_id` 归属?(用户能改/删别人的数据吗?)
- **密码处理**:Service 层注册时是否 BCrypt 加密(`BCryptPasswordEncoder.encode`)?登录时是否 `matches` 比对?响应中是否过滤密码字段(@JsonIgnore on Entity)?
- **JWT 验证**:登录后接口是否被 LoginInterceptor 覆盖(对齐 init-skeleton)?未鉴权接口是否在 WebMvcConfig 白名单?
- **敏感信息泄漏**:日志是否打印密码 / 完整 token / 完整身份证号?(对齐 CLAUDE.md §二·五)
- **路径穿越**:文件上传 / 下载是否拼接路径?(对齐 CLAUDE.md §二·五)

### 2. 逻辑正确性

- **业务逻辑符合 PRD §3 P0 需求**:每个 P0 功能是否完整实现?
- **API_DESIGN.md §3 接口详情对照**:HTTP 方法 / 路径 / 请求参数 / 响应字段是否一致?
- **业务规则**:状态机 / 唯一性校验 / 唯一约束 等是否正确实现?
- **空场景 / 边界值 / 默认值** 处理是否正确?
- **数据一致性**:跨表更新是否在同一事务?

### 3. 异常处理

- **业务异常**:是否抛 `BusinessException(code, message)` · code 取自 API_DESIGN.md §4.3 模块编号(1xxx-9xxx)?
- **全局异常**:Controller 是否 try-catch?(应**禁止** · 由 GlobalExceptionHandler 统一)
- **异常吞掉**:catch 后是否只 `log.error` 不 throw?(应禁止)
- **运行时异常**:是否抛 `RuntimeException` / `IllegalArgumentException` 等通用异常代替 BusinessException?(应禁止 · 用 BusinessException 携带 code)
- **空指针保护**:`getById` 返回 null 是否处理?
- **参数校验失败**:是否返回 4xx + 友好 message?

### 4. 代码规范

- **分层职责**(对齐 CLAUDE.md §二·一 8 类):
  - Controller 是否只做参数校验 + Service 转发?(**禁止**写业务逻辑)
  - Service 是否处理业务逻辑(不放 Controller / Mapper)?
  - Mapper 是否只放数据访问(不放业务)?
- **依赖注入**:是否用 `@RequiredArgsConstructor + final` 构造器注入?(**禁止** `@Autowired` 字段注入)
- **注解**:`@Service` / `@RestController` / `@RequestMapping` / `@RequiredArgsConstructor` / `@Slf4j` 是否齐全?
- **命名**:类名大驼峰 · 方法名/字段名小驼峰 · 常量全大写下划线 · 见名知意(对齐 CLAUDE.md §二·七)
- **路径前缀**:Controller 是否对齐 API_DESIGN.md §1 RESTful 命名(资源复数 `/api/users` / 认证动作 `/api/auth/login`)?

### 5. MyBatis-Plus 用法

- **简单 CRUD**:是否用 `BaseMapper` 内置方法(`save` / `getById` / `updateById` / `removeById`)?
- **条件查询**:是否用 `LambdaQueryWrapper`?(**禁止**字符串拼接 SQL · 对齐 CLAUDE.md §一·二)
- **复杂查询**:是否走 CLAUDE.md §二·四 例外路径(XML 或 `@Select` + `#{}` 参数化)?
- **分页**:是否用 `Page<T>` + `IPage<T>`?(对齐 API_DESIGN.md §1 分页 `pageNum` + `pageSize`)
- **Entity 注解**:`@TableName` / `@TableId(IdType.AUTO)` / `@TableLogic`(软删除)/ `@TableField`(字段名差异)是否齐全?(对齐 CLAUDE.md §二·二 + entity-coder §一)

### 6. 性能

- **N+1 查询**:循环里调 `getById` 还是用 `listByIds` 批量?
- **缺索引的 where**:`LambdaQueryWrapper` 用的字段是否在 DATABASE_DESIGN.md §3 #9 加了索引?
- **大对象传输**:列表接口是否分页(`Page<T>` 必须)?是否传输不必要字段?
- **N 次更新合一**:多次 `updateById` 是否能合一次?
- **事务范围**:`@Transactional` 范围是否过大(包了 IO 调用 / 远程调用)?

### 7. 🆕 幂等性(2026 加强 · 06 R-05 模板特别标注)

- **创建类接口防重**:UNIQUE 索引 / 业务唯一键 / 状态机校验是否到位?
- **状态流转校验源状态**:订单 `待支付 → 已支付` 时校验源状态是否必须为 `待支付`?(避免"已支付"再次被改导致重复扣款)
- **删除接口幂等**:删 1 次 / N 次效果是否一致?(用 `@TableLogic` 软删除 + `is_deleted` 校验)
- **学生项目典型踩坑**:用户连续点击"提交订单"按钮 → 同一订单创建 N 次(库存扣 N 次)· 后端必须有防重机制

### 8. 依规范核对(R-05 多文件配对核对 · 跟 entity-coder + service-coder 双 G-XX 对齐)

> 📌 **本维度是 R-05 多文件首次拆分协议的核心校验** · 逐项核对生成代码是否符合 `entity-coder + service-coder` 已确立的规范
>
> ⚠️ **本维度零容忍走过场**(2026-05-12 强化 · 修复"假装审了"漏检根因):每个子目核对**必须显式输出三段式**(参考集 / 被检集 / 差集) · 即便结论是"核对通过"也要列出参考集和被检集证据 · **严禁**只写"已核对 · 无问题"这种空结论。每个子目核对报告格式如下:
>
> ```
> #### <子目名称> 核对
> - **参考集**(规范要求的全部项):[逐条列出该规范的全部硬要求 · 如 Entity 子目列出 @TableName / @TableId / @TableLogic / @JsonIgnore / 类型映射规则 / 字段顺序 / @Data 等]
> - **被检集**(本次审核的实际代码中出现的项):[逐条列出实际代码使用的对应项]
> - **差集 / 结论**:<列出每条 issue · 或"全部对齐 · 无 issue"+ 简短证据(如"检查 6 项 · 全符合")>
> ```

#### Entity 核对(对齐 entity-coder §一)

- `@TableName("<表名>")` 显式声明
- 主键 `@TableId(type = IdType.AUTO)`
- 软删除字段 `@TableLogic` + `@TableField("is_deleted")`
- 密码字段 `@JsonIgnore`
- SQL → Java 类型映射:**DECIMAL → BigDecimal**(禁 Double/Float)· **DATETIME → LocalDateTime**(禁 Date)· `is_deleted` TINYINT(1) → Integer + @TableLogic
- 字段顺序对齐 DATABASE_DESIGN.md §3:`id` → 业务字段 → `is_deleted` → `create_time` → `update_time`
- `@Data` Lombok 注解

#### Mapper 核对(对齐 entity-coder §一)

- `extends BaseMapper<EntityName>`
- `@Mapper` 注解
- 空方法体(简单 CRUD 全走 BaseMapper · 复杂查询走 CLAUDE.md §二·四 例外路径)

#### Service 核对(对齐 service-coder §一)

- 继承 `IService<EntityName>`
- 业务方法签名:**禁止返回 `Result<T>`**(那是 Controller 职责)· 入参用 DTO / 基本类型(**禁止 `Map`** / `JSONObject`)· 分页返回 `IPage<T>`
- 方法名动词开头见名知意

#### ServiceImpl 核对(对齐 service-coder §一)

- 类注解 `@Service` + `@Slf4j` + `@RequiredArgsConstructor`(构造器注入 · **禁** `@Autowired` 字段)
- 继承 `ServiceImpl<<EntityName>Mapper, <EntityName>>`
- **写操作必加 `@Transactional`**(insert / update / delete · 跨表必加)
- 业务异常用 `BusinessException(code, message)` · code 取自 API_DESIGN.md §4.3 模块编号(1xxx-9xxx)
- 密码加密用 `BCryptPasswordEncoder.encode` / `matches`
- 登录接口用 `JwtUtils.generateToken`

#### Controller 核对(对齐 service-coder §一)

- 类注解 `@RestController` + `@RequestMapping("/api/<resource>")` + `@RequiredArgsConstructor`
- 路径前缀:资源复数(`/api/users`)/ 认证动作(`/api/auth/login`)
- 方法入参:写操作用 `@RequestBody @Valid <Action>Request` / 资源 ID 用 `@PathVariable` / 查询参数用 `@RequestParam`
- 返参统一 `Result<T>`(`Result.success(data)` / `Result.error(code, msg)` 静态工厂 · 对齐 CLAUDE.md §一·三)
- **不写业务逻辑** · **不 try-catch**

#### DTO 核对(对齐 service-coder §一)

- 放 `entity/dto/` 子目录(对齐 CLAUDE.md §二·三)
- 命名 `<功能>Request` / `<功能>Response`(`UserRegisterRequest` / `UserLoginResponse` 等)
- `@Data` Lombok
- 校验注解齐全:`@NotBlank` / `@NotNull` / `@Size` / `@Pattern` / `@Email` / `@Min` / `@Max`(对齐 CLAUDE.md §一·二 必须校验)
- **密码字段不加 `@JsonIgnore`**(双向陷阱 · DTO 接收明文 · Service 层 BCrypt 加密)

### 🆕 反例推演显式输出(2026-05-12 强化 · 把维度 1/3/7 散落的反例汇总为显式推演链)

> 📌 **本段要求"动态推演"**:不能只在维度 1/3/7 列 issue 就完事 · 必须**假设业务发生 X 操作 → 显式列推演链 → 列出后果** · 推演链必须**显式写到报告中**(不只给结论)。这是教学产物 · 推演过程本身就是给学生看的反例。
>
> ⚠️ **严禁**只写"考虑了越权"/"幂等性 OK" 这种空结论。每类推演必须给出至少 1 条**具体推演链**(像写测试用例一样具体)。

对本次审核范围内的代码,逐类显式输出推演:

#### 推演 A · 越权推演(对应维度 1.2 + 1.3)

每个**写操作**(POST / PUT / DELETE 接口)+ 每个"读他人数据"接口:
- 假设角色 A 携合法 token 调用本属于角色 B 的接口 → 推演链 → 结论
- 假设同角色 A 用户 用合法 token 调用 / 改 / 删 B 用户的数据 → 推演链(Service 是否查 `owner_id`?Mapper 是否带 `WHERE owner_id = currentUserId`?) → 结论
- **必含格式**(每条推演链):`假设场景 → Controller 入口 → Service 方法 → Mapper 查询 → 结果 → 是否符合 PRD 业务规则?`
- 若任一接口的越权场景未在推演中得到防御 → 🔴 高严重度 issue

#### 推演 B · 空指针推演(对应维度 3.5)

每个调用 `BaseMapper.getById` / `lambdaQuery().eq(...).one()` / `findByXxx()` 后**直接访问返回对象字段**的位置:
- 假设查询返回 null → 推演链 → NullPointerException 在哪一行抛出?GlobalExceptionHandler 怎么处理?前端看到什么?
- **必含格式**:`假设 getById(99999) 返回 null → 第 N 行 user.getName() → NPE → GlobalExceptionHandler 兜 500 → 前端看到"服务器内部错误"`
- 期望:Service 应**显式判 null + 抛 BusinessException(2001, "资源不存在")** · 不能任由 NPE 兜底

#### 推演 C · 并发 / 幂等推演(对应维度 7)

每个含**状态变更**(支付/审批/接单/提交)或**资源创建**(下单/报修)的 Service 方法:
- 假设两个请求同时进入该方法 / 客户端连点 2 次提交 → 推演链 → 后果
- **必含格式**:`假设双请求并发 → 请求 1 SELECT 看到 status='待缴' → 请求 2 SELECT 也看到 status='待缴' → 请求 1 UPDATE → 请求 2 UPDATE → pay_time 被覆盖 + 业务可能重复执行`
- 期望:① 条件 UPDATE(`WHERE id=? AND status='待缴'` + 按 affectedRows 判断)· 或 ② 乐观锁(`@Version`)· 或 ③ 唯一索引防重(资源创建场景)

#### 推演 D · 异常传播推演(对应维度 3)

每个 Controller 方法 + 每处 Service 抛 BusinessException 的位置:
- 假设 Service 抛 `BusinessException(1002, "用户名或密码错误")` → 推演链 → 异常如何传播到前端?
- **必含格式**:`Service throw BusinessException(1002) → ServiceImpl 不 catch → Controller 不 try-catch → 抛到 @RestControllerAdvice → 转 Result.error(1002, msg) → 前端 axios 拦截器 ElMessage.error`
- 若 Controller 写了 try-catch 吞掉异常 / Service 抛 RuntimeException 而非 BusinessException → 🔴 高严重度 issue(全局异常机制失效)

**输出要求**:每类推演至少 1 条**具体推演链**(像写测试用例一样具体)· 写在审核报告的独立段落 "推演结果" · 不允许只写"已考虑"或"无问题"。即便所有推演都通过,也要显式列出推演证据(如"推演了 3 个 PUT 接口的越权场景 · 都通过 owner_id 校验 · 见下方")。

## 输出指令(Claude Code 必须 3 项都做,缺一不可)

### 1. 创建审核报告文件

按切片参数决定报告路径:
- **`模块=<X>`** 模式 → `docs/对话记录/Phase4-R05-<模块>-review-<YYYY-MM-DD>.md`
- **`功能=P0-N`** 模式 → `docs/对话记录/Phase4-R05-<功能>-review-<YYYY-MM-DD>.md`(如 `Phase4-R05-P0-1-review-2026-05-10.md`)

(日期换今天):

- 如 `docs/对话记录/` 目录不存在,**先创建目录再写文件**
- 文件结构(markdown 标题层级固定):

```
# Phase 4 R-05 后端代码审核报告 · <模块或功能切片> · YYYY-MM-DD

## 审核元数据
- 审核日期:YYYY-MM-DD
- 审核切片:<模块=X 或 功能=P0-N>(对应 N 张表 · M 个接口 · K 个特殊场景文件)
- 使用模型:<本对话用的模型 · 跟 service-coder/feature-coder 不同>
- 输入摘要:<被审文件路径清单 + 文件总行数>

## 审核报告

### 维度 1:安全漏洞
- **issue-1** [严重度: 高/中/低]:<问题描述>
  - **位置**:<文件路径:行号 · 如 `service/impl/UserServiceImpl.java:45`>
  - **修复建议**:<具体可执行 · 如「`registerUser` 方法漏掉 BCrypt 加密 · 改为 `user.setPassword(new BCryptPasswordEncoder().encode(rawPassword))`」 · 而非「加强密码处理」套话>
- **issue-2** ...

### 维度 2:逻辑正确性 ...
### 维度 3:异常处理 ...
### 维度 4:代码规范 ...
### 维度 5:MyBatis-Plus 用法 ...
### 维度 6:性能 ...
### 维度 7:幂等性 ...
### 维度 8:依规范核对(强制三段式 · 每个子目 6 个均输出"参考集/被检集/差集")
  #### Entity 核对
  - **参考集**:[列出 entity-coder §一 规范要求项]
  - **被检集**:[列出实际代码项]
  - **差集 / 结论**:<或"全部对齐 · 无 issue · 检查 N 项">
  #### Mapper 核对(三段式 · 同上)
  #### Service 核对(三段式 · 同上)
  #### ServiceImpl 核对(三段式 · 同上)
  #### Controller 核对(三段式 · 同上)
  #### DTO 核对(三段式 · 同上)

### 🆕 反例推演结果(2026-05-12 新增 · 4 类推演链显式输出)
  #### 推演 A · 越权推演:逐写操作接口列出推演链(假设 → Service → Mapper → 结果)
  #### 推演 B · 空指针推演:逐查询位置列推演链(假设 null → NPE 位置 → 全局异常处理)
  #### 推演 C · 并发/幂等推演:逐状态变更方法列双请求并发推演
  #### 推演 D · 异常传播推演:逐 BusinessException 抛点列异常传播链

## 修复行动建议
<总结性段落 · 按严重度排序的修复优先级 · 区分 entity-coder §二 修哪些 / service-coder §二 修哪些>

## R-05 多文件拆分修复路径(给学生提示 · 按本次切片参数选)

**若本次审核切片是模块切片(第 1 token 非 P0-N · 三阶段教学第一阶段 + 兜底)**:
- **`/entity-coder 应用修复 模块=<X>`** → 修复 `entity/` + `mapper/` 下的 N 条 R-05 注释
- **`/service-coder 应用修复 模块=<X>`** → 修复 `service/` + `service/impl/` + `controller/` + `entity/dto/` 下的 M 条 R-05 注释
- 两调用接对话顺序执行(不需要退出 `claude` · 应用修复模式是审核类例外)

**若本次审核切片是功能切片(第 1 token 是 P0-N · 三阶段教学第二阶段 · feature-coder 主路径)** 🆕:
- **建议先跑 `/code-reviewer-fe P0-N`** 把 R-06 也跑完(R-05 + R-06 双层审核完再一次性跨层修)
- **`/feature-coder 应用修复`** → 一次跨层修 R-05(后端)+ R-06(前端)所有 issue · 跨层一致性自动保证(R-05 改 entity 字段 → 同步改前端 form 类型 + 校验;R-06 改前端 pattern → 同步改后端 DTO `@Pattern`)
- 详见 `feature-coder.md §二`(权威源)
```

### 2. 修改对应代码文件,在 issue 位置上方加 Java 行注释

```java
// R-05-issue-编号: 严重度 - 一句话问题描述
```

- **格式严格**:`//`(Java 单行注释)+ 空格 + `R-05-issue-` + 编号(1 顺序递增)+ `:` + 空格 + `严重度`(高/中/低) + ` - ` + 问题描述
- ⚠️ **跨多文件统一编号**:全模块 R-05 注释**1 顺序递增**(不分文件)· 如 entity/User.java:23 是 `R-05-issue-3` · service/impl/UserServiceImpl.java:45 是 `R-05-issue-7`
- **原文一字不改 · 只插注释**(放在 issue 涉及行的**上方一行**)
- **跟 review.md 中的 issue 编号一致**

> 📌 **R-05 注释生命周期 + 多文件拆分协议**(2026-05-10 entity-coder + service-coder 审完后确立 · 二段循环协议第 4 次完整应用 · 多文件首次跨命令拆分配对完整闭合):
>
> | 命令 | 修复目录范围 |
> |---|---|
> | `/entity-coder 应用修复 模块=<X>` | `entity/` + `mapper/` 下的 R-05 注释 in-place 改为「已修复」 |
> | `/service-coder 应用修复 模块=<X>` | `service/` + `service/impl/` + `controller/` + `entity/dto/` 下的 R-05 注释 in-place 改为「已修复」 |
>
> **本命令(reviewer)只插 R-05 注释 · 不要插带「已修复」字样的注释** —— 那是下游 G-XX 命令的产出。
>
> **`//` 行注释 vs `<!-- -->` HTML 注释**:R-05 用 Java `//` 行注释(代码文件)· 区别于 R-01/R-03/R-04 的 HTML 注释(markdown 文件)。**严格用 `//` 不要用 `/* */` 块注释**(避免跟代码块注释混淆)。

### 3. 输出 diff 摘要

(N + 1 个文件改动 · N = 模块代码文件数 · 1 = 新建 review.md)

## 严重度判定标准

| 严重度 | 判定标准(对齐 06 R-05 + R-05 后端特化)|
|---|---|
| **高** | SQL 注入 / 越权(没校验 owner_id)/ 密码明文存储 / 事务漏洞(漏 @Transactional 跨表)/ 业务逻辑错误(P0 功能未实现 / 跟 PRD §3 不符)/ JWT 验证缺失 / 幂等性漏洞(订单重复创建 / 重复扣款)/ 关键规范偏离(Service 返 Result<T> / Controller 写业务) |
| **中** | 空指针保护缺失 / 异常吞掉(catch 后只 log)/ N+1 查询 / 大对象传输 / 漏 @Valid / DTO 命名错位 / 注解漏(@Service / @TableLogic 等)/ 错误的依赖注入(@Autowired 字段)/ 跟 DATABASE_DESIGN.md §3 字段类型映射不符 |
| **低** | 命名风格不一致 / 注释缺失 / 日志级别不当 / 字段顺序未对齐 / 路径前缀小拼写差异 |

不确定的地方先问,**不要编造问题**。

## 调用示例

#### 示例 1 · 三阶段教学第一阶段(模块切片 · entity-coder + service-coder 旧命令路径)

```
/code-reviewer-be auth

请审核 auth 模块代码(entity/User.java + entity/UserRole.java + mapper/UserMapper.java + mapper/UserRoleMapper.java + service/UserService.java + service/impl/UserServiceImpl.java + controller/AuthController.java + entity/dto/UserLoginRequest.java + UserRegisterRequest.java + UserLoginResponse.java),从 8 维度找问题(安全/逻辑正确性/异常处理/代码规范/MP用法/性能/幂等性/依规范核对)。

输出:
1. 创建 docs/对话记录/Phase4-R05-auth-review-<今天日期>.md(8 维度报告 + R-05 多文件拆分修复路径 · 提示 entity-coder + service-coder 双调用)
2. 在每个被点出 issue 的 .java 文件中插入 // R-05-issue-编号 行注释(跨文件统一编号)
3. 输出 diff(N+1 个文件)

⚠️ 调用前 会话内**切换模型**(用 `/model` 命令)!如果 service-coder 用 V4 Flash,这里换 V4 Pro主审(同源自审) · 有 GLM key 推荐**异源审核**(切到 GLM provider · 见 08a §11.6 · 双品牌保险(V2-D01))。
```

#### 示例 2 · 三阶段教学第二阶段(Vertical Slice 切片 · feature-coder 主路径)🆕

```
/code-reviewer-be P0-7

请审核 P0-7 缴费统计看板功能涉及的所有后端文件(对照 PRD §3 P0-7 + feature-coder 已生成的全栈切片):
- 基础 6 类:entity/Payment.java + mapper/PaymentMapper.java + dto/PaymentStatsResponse.java + service/PaymentService.java + impl/PaymentServiceImpl.java + controller/PaymentController.java(含 /api/payments/stats 聚合接口)
- 特殊场景:scheduler/PaymentScheduler.java(每天凌晨自动汇总)+ 启动类 @EnableScheduling

从 8 维度找问题(安全/逻辑正确性/异常处理/代码规范/MP用法/性能/幂等性/依规范核对)。

输出:
1. 创建 docs/对话记录/Phase4-R05-P0-7-review-<今天日期>.md(8 维度报告 + R-05 修复路径提示「建议先跑 /code-reviewer-fe P0-7,再用 /feature-coder 应用修复 一次跨层修」)
2. 在每个被点出 issue 的 .java 文件中插入 // R-05-issue-编号 行注释(跨文件统一编号 · 含特殊场景文件)
3. 输出 diff

⚠️ 保持 V4 Pro 主审(同源自审) · 有 GLM key 推荐切到 GLM 5.1 异源(见 08a §11.6)。
```

## 验证 checklist(学生 review 时核对)

- [ ] **必读文件缺失检查**全部通过(模块 6 类文件齐全 + 第 1 个参数(切片标识)已传 · 模块名或 P0-N 形式)
- [ ] `docs/对话记录/Phase4-R05-<模块名>-review-<YYYY-MM-DD>.md` 已创建
- [ ] 报告 markdown 结构齐全:H1 标题 / H2 元数据 + 审核报告 + 修复建议 + R-05 多文件拆分修复路径 / H3 8 个维度
- [ ] **8 维度都有覆盖**(安全 / 逻辑正确性 / 异常处理 / 代码规范 / MP 用法 / 性能 / 幂等性 / 依规范核对)
- [ ] **维度 8「依规范核对」分 6 子目分组**(Entity / Mapper / Service / ServiceImpl / Controller / DTO)· 每个子目**显式输出三段式**(参考集 / 被检集 / 差集 · 不允许"已核对 · 无问题"空结论 · 2026-05-12 强化)
- [ ] **🆕 反例推演段已显式输出 4 类推演链**(越权 / 空指针 / 并发幂等 / 异常传播)· 每类至少 1 条具体推演链(像测试用例一样具体)· 不允许"已考虑"空结论(2026-05-12 强化)
- [ ] 代码文件中插入了 `// R-05-issue-编号: 严重度 - 描述` Java 行注释(**不是** HTML 注释 · **不是** `/* */` 块注释)
- [ ] **跨多文件统一编号**(1 顺序递增 · 不分文件)
- [ ] 注释**不带** "已修复" 字样(那是下游 entity-coder §二 / service-coder §二 的产出)
- [ ] issue 编号在 review.md 和代码文件 R-05 注释中**一致**
- [ ] 严重度标签**合理**(高/中/低 · 不要一片"高")
- [ ] 修复建议**具体可执行 + 含位置**(文件路径:行号)· 不是「优化代码」「加强规范」套话
- [ ] **修复建议按 entity-coder §二 / service-coder §二 拆分明示**(哪些 issue 由哪个命令修)
- [ ] 用了与 service-coder **不同的模型**(切换确认!Pro 审 Flash 写)
- [ ] 业务覆盖**对照 PRD.md §3 P0** 完整核对(无遗漏)
- [ ] 接口路径/方法/参数**对照 API_DESIGN.md §2/§3** 完整核对
- [ ] 字段类型**对照 DATABASE_DESIGN.md §3 #6** 完整核对(尤其 DECIMAL → BigDecimal · DATETIME → LocalDateTime)
- [ ] 审核**只针对模块代码 6 类文件**(entity / mapper / service / impl / controller / dto · 未涉及前端 / `config/` / `util/` / 跨模块基础设施)

## 衔接(双切片模式两条修复路径)

### 路径 A · 三阶段教学第一阶段(模块切片 · 旧命令路径)

下一步(详见 08b §8.6 第一阶段):

1. **`/entity-coder 应用修复 模块=<X>`**(短调用 · 进入 entity-coder §二)
   - 自动扫描 `entity/` + `mapper/` 下的 R-05 注释逐条修复 + 标记「已修复」
   - 详见 `entity-coder.md §二`

2. **`/service-coder 应用修复 模块=<X>`**(短调用 · 进入 service-coder §二)
   - 自动扫描 `service/` + `service/impl/` + `controller/` + `entity/dto/` 下的 R-05 注释逐条修复 + 标记「已修复」
   - 详见 `service-coder.md §二`

3. **`/git-committer`** 提交本模块:`feat(p4-<X>): <模块名> Service+R-05 修复`(对齐 CLAUDE.md §四·三 scope 命名约定 · Phase 前缀复合)

### 路径 B · 三阶段教学第二阶段(Vertical Slice 切片 · feature-coder 主路径)🆕

下一步(详见 08b §8.6 第二阶段):

1. **跑 `/code-reviewer-fe P0-N`**(R-06 前端审核 · 跟本命令 R-05 形成双层审核)

2. **`/feature-coder 应用修复`**(短调用 · 进入 feature-coder §二)
   - 一次扫 R-05(后端)+ R-06(前端)所有注释逐条跨层修复
   - 跨层一致性自动保证(R-05 改 entity 字段 → 同步前端 form 类型 + 校验;R-06 改前端 pattern → 同步后端 DTO `@Pattern`)
   - 详见 `feature-coder.md §二`

3. **`/git-committer`** 提交本功能:`feat(p4-<功能>): P0-N <功能名> Vertical Slice + R-05+R-06 修复`

### 全部跑完后(两路径共用)

Phase 4 全部模块/功能跑完后:**`/rules-updater`**(走 §二 单字段更新模式)同步 `project-status.md` 「已有接口」+「已完成的后端模块」+「P0/P1/P2 完成数」字段(对齐 rules-updater 审核档案 + 08b §8.6 末步调用 · ⚠️ rules-updater 同步的是 project-status.md,**不动** CLAUDE.md 任何节)

## 设计要点

- **🆕 双切片粒度参数**(2026-05-13 升级 · 位置参数 · 对应三阶段教学双路径):
  - **模块切片** `<模块名>`(如 `auth` / `user`)→ 三阶段教学第一阶段(P0-1 用 entity-coder + service-coder 旧命令)+ 兜底(极端复杂功能 feature-coder 输出后局部用旧命令优化)
  - **功能切片** `P0-N`(如 `P0-3`)→ 三阶段教学第二阶段(P0-2 起 feature-coder 主路径 · Vertical Slice 切片审)
  - 8 维度审核逻辑两模式完全一致 · 区别仅在「输入文件范围」+「报告命名」+「衔接路径」
- **审核模型策略**:service-coder/feature-coder 用 V4 Flash/V4 Pro 写代码 · 本命令**保持 V4 Pro 主审**(用**不同模型** · 代码审查需更强推理 · 跟 R-04 反向因为 api-designer 用 V4 Pro 写) · **可选 GLM 5.1 异源**(有 GLM key 时 · 见 08a §11.6 · 跨品牌双模型保险更稳)
- **Java `//` 行注释 + Git diff = 改前/改后证据**(05 验收要求 ≥5 处)· **区别于 R-01/R-03/R-04 的 HTML 注释**(因为 R-01/R-03/R-04 审 markdown 文件 · R-05/R-06/R-07/R-08 审代码文件)
- **审核报告自动落盘**(V2 相对 V1 最大改进):学生不需要手动整理对话记录
- **8 维度审核 · 含「🆕 幂等性」**(2026 加强 · 06 R-05 模板特别标注 · 学生项目典型踩坑场景:订单重复创建 / 重复扣款)
- **「审核 ↔ 应用修复」二段循环协议第 4 次完整应用 · R-05 多文件首次跨命令拆分配对完整闭合**:
  - 之前 R-01/R-03/R-04 都是单文件 reviewer · 单命令 §二 修
  - **R-05 是首次跨多文件需要按目录边界拆分到 2 个下游命令的 reviewer**:
    - 本命令(reviewer)插 R-05 注释 → `/entity-coder §二` 修 entity/+mapper/ + `/service-coder §二` 修 service/+impl/+controller/+dto/
    - 边界对称 · 100% 覆盖 · 无空隙 · 无重叠
- **R-05 vs R-04 差异**:
  - R-05 跨 6 类代码文件(entity / mapper / service / impl / controller / dto)· R-04 单 markdown 文件
  - R-05 用 Pro 审 · R-04 用 Flash 审(因写者使用模型反向)
  - R-05 注释统一编号跨文件 · R-04 注释顺序递增单文件
- **「按目录拆分 R-XX 修复职责」协议正式成立**:R-05 是首次完整闭合案例 · 后续 R-06(前端)按 CLAUDE.md §三·一 8 类目录拆分 · R-07(全栈)/ R-08(安全)同样模式

---

> 📋 **跨文件呼应导航**:
> - **上游产出**:`entity-coder.md`(读其生成的 Entity + Mapper · R-05 修复跟 §二 配对)+ `service-coder.md`(读其生成的 Service+ServiceImpl+Controller+DTO · R-05 修复跟 §二 配对)
> - **平行规则**:`CLAUDE.md §二·一`(分层 8 类 · 维度 4 代码规范核对)+ `§二·二`(Entity 规范 · 维度 8 Entity 子目核对)+ `§一·三`(Result<T> · 全栈接口契约 · 维度 4 + 8)+ `§二·三`(DTO + BusinessException · 维度 4 + 8)+ `§二·四`(MP 用法 · 维度 5)+ `§二·五`(后端特定安全 · 维度 1)
> - **全栈契约**:`CLAUDE.md §一·一·后端`(版本 + spring-security-crypto)+ `§一·三`(Result<T> 单一权威源 · 全栈接口契约)+ `§一·二`(BCrypt + LambdaQueryWrapper + @Valid · 全栈通用安全 · 全维度核对)
> - **输入文档对照**:`API_DESIGN.md §1`(接口约定 · 维度 4 路径前缀)+ `§2`(接口清单 · 维度 2 业务覆盖)+ `§3`(接口详情 · 维度 2 字段对照)+ `§4`(异常码 · 维度 3 BusinessException 引用)+ `DATABASE_DESIGN.md §3`(字段约定 · 维度 8 SQL→Java 类型映射)+ `PRD.md §3` P0(维度 2 业务覆盖核对)
> - **下游消费**:`entity-coder.md §二`(R-05 修 entity/+mapper/) + `service-coder.md §二`(R-05 修 service+impl+controller+dto) + 二段循环协议跟 `srs-reviewer / db-reviewer / api-reviewer` 一致 · **R-05 多文件首次拆分配对完整闭合**
> - **基础设施**:`init-skeleton.md backend/src/main/java/{{包路径}}/common/`(Result + BusinessException + GlobalExceptionHandler · 2026-05-10 第 2 次链路断点修复)+ `util/JwtUtils`(登录接口审核用)
> - **rules-updater**:`/rules-updater` 同步 `project-status.md`(**不动** CLAUDE.md 任何节 · Phase 4 全部模块跑完后 · 对齐 rules-updater §二 单字段更新模式)
> - **R-XX reviewer 标杆**:`api-reviewer.md`(R-04 已审 · 报告结构 + 严重度 + 自检模板)+ `srs-reviewer.md`(R-01 已审)+ `db-reviewer.md`(R-03 已审)
