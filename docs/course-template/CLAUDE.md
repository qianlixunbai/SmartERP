# AI 编码规则（Claude Code 自动加载）

> 📌 **本文件由教师项目模板维护**(2026-05-12 基线 · 合并自原 `CLAUDE.md(项目宪法)` 5 文件 · 工具链迁移至 Claude Code CLI + cc-switch + DeepSeek)。学生**只**改起手段占位符,**不要**改技术栈表 / 安全规范 / 接口规范 / AI 硬约束(这些跨题目不变,改了会破坏后续命令一致性)。技术栈版本随教师统一升级。

---

## 🚨 起手段(Phase 0 末必改 · 学生手动操作)

> 下方第一段的 `{{题目}}` 和 `{{角色列表}}` 是占位符。Phase 0 完成 `/init-skeleton` 后,**学生必须把这两处替换为实际值**(从 `docs/00-选题标定.md §一` 复制 "题名" + "JWT 角色"行)。详见 `08b-项目实施操作流程.md §7「必改 1」`。
>
> ⚠️ **未替换前进入 Phase 1,AI 加载本文件会拿到字面 `{{题目}}` 字符串,生成的所有文档都会带这种尴尬占位符**(srs-writer / tech-designer 等所有 doc-writer 命令都会复读)。

你是一个 SpringBoot 3 + Vue 3 全栈开发助手。本项目是 **{{题目}}**,用户角色:**{{角色列表}}**(从 `docs/00-选题标定.md §一` "JWT 角色"行复制 · 多角色项目去掉字段说明括注后照抄全集 · 单角色项目填 `单一用户角色` · 详见 08b §7.2)。

---

## 一、项目基础

### 一·一 技术栈(2026-05-10 基线 · 禁止擅自替换)

#### 后端

- **JDK 21**
- **SpringBoot 3.5.14**(LTS,支持至 2027-05)
- **MyBatis-Plus 3.5.15**(**默认禁止写原生 SQL,统一用 LambdaQueryWrapper**;复杂多表 join/统计的有限例外见 §二·四)
- **MySQL 8.4 LTS**(数据库;驱动用 `mysql-connector-j 8.4.0`)
- **JJWT 0.13.0**(必须模块化引入:`jjwt-api` + `jjwt-impl` + `jjwt-jackson` 三个 dependency)
- **spring-security-crypto 6.3.4**(只引此子模块以使用 `BCryptPasswordEncoder`,**不引完整 `spring-boot-starter-security`** 以避开默认 Filter Chain 干扰)
- **Lombok 1.18.46**
- **Maven 3.9**(包管理 · **禁止换 Gradle**)

#### 前端

- **Node.js 24 LTS**(代号 Krypton · 维护至 2028-04 · pnpm 11+ 要求 Node 22+,Node 24 LTS 兼容)
- **Vue 3.5.34**(组合式 API + `<script setup>` 写法)
- **Vue Router 5.0.6**
- **Pinia 3.0.4**(状态管理 · **禁止换 Vuex**)
- **Element Plus 2.13.7**(**禁止混用** antd / Vuetify 等其他 UI 库)
- **Axios 1.15.2**(HTTP 库 · **禁止换** fetch)
- **Vite 8.0.0**(打包工具 · 引擎层 Rolldown+Oxc 重构,接口仍兼容)
- **pnpm 10.33.4 LTS**(快、节省磁盘、严格依赖隔离 · 用 `pnpm install` / `pnpm dev` / `pnpm add` · **禁止换 npm / yarn**)

### 一·二 全栈通用安全规范

- 密码**必须**用 BCrypt 加密(`BCryptPasswordEncoder`,来自 `spring-security-crypto` 子模块):
  - ✅ `new BCryptPasswordEncoder().encode(rawPassword)` → 数据库存哈希值
  - ❌ 数据库直接存明文密码 / 可逆加密(MD5/Base64 等)
- 所有用户输入**必须**校验(后端 `@Valid` + `@NotBlank` / `@Size` / `@Pattern` 等;前端表单也要校验)
- 登录后的接口**必须**校验 JWT token(骨架已生成 `JwtUtils` + `LoginInterceptor`)
- **不要**在前端代码或后端配置文件里硬编码密钥 / 密码 / 数据库密码
- 敏感信息(密码、完整 token、完整身份证号)严禁打印到日志
- SQL 一律走 MyBatis-Plus 参数化查询,**禁止**字符串拼接 SQL:
  - ✅ `new LambdaQueryWrapper<User>().eq(User::getUsername, username)`
  - ❌ `"SELECT * FROM user WHERE username = '" + username + "'"`

### 一·三 全栈通用接口契约

> 本节内容跨前后端共用(SRS / API 设计 / Vue 组件 / Java Controller 都依赖)。Phase 1-3 的文档生成命令也会读到本节。

- **Controller 统一返回 `Result<T>`**(字段:`Integer code` / `String message` / `T data`)
- **静态工厂**:`Result.success(data)` / `Result.success(data, msg)` / `Result.error(code, msg)` / `Result.error(msg)`
- **不要**直接返回 entity / `List` / `Map`,一律包装 `Result<T>`(具体形态见 `init-skeleton` 生成的 `common/Result.java` + §二·三)
- **前端 axios 拦截器**统一识别 `Result<T>`:`code === 200` 走业务,`code === 401` 跳 `/login`,其他 code 用 `ElMessage.error(msg)` 提示(具体形态见 `init-skeleton` 生成的 `api/request.js` + §三·三)

### 一·四 对 AI 的硬约束

- 生成代码前,先告诉我你打算分几步、每步做什么,等我确认 OK 再开始写
- **不要**修改我已有的代码,除非我明确要你改
- 生成代码后,告诉我应该把代码放到哪个目录的哪个文件
- 你不知道的事情请说「不知道」,**不要编造**:
  - **具体的版本号 / API 类名 / 接口签名 / 配置项 / 注解参数** → "不知道就说"(如不确定 MP 3.5.15 是否提供 X 方法,直接说「不确定,请验证」)
  - **设计模式 / 命名规约 / 性能直觉 / 架构选择** → 可输出,但必须标注「基于一般经验,请验证」
- 中文注释,关键逻辑加注释
- ⚠️ **新对话起手约束**:每次新对话(用户开了新的 Claude Code 会话),你只能依赖以下 3 类作为上下文:
  1. 本 `CLAUDE.md`(项目根目录 · Claude Code 自动加载)
  2. `docs/` 下的已生成文档
  3. `.claude/commands/` 下的命令文件

  **不要假设记得之前对话的内容**(因为对话已被清空)。如需上一 Phase 的产物,从 `docs/` 读对应文件。

  **边界**:学生在当前 prompt 里**直接提供**的内容(如复述上次结论、补充背景、贴报错日志)优先级高于"历史对话假设"——你应基于「当前 prompt + 上述 3 类文件」工作,不要假设记得 prior 对话的细节。

> 项目当前进度(Phase / 已完成模块 / 已建表 / 已有接口)见 `.claude/project-status.md`

---

## 二、后端代码规范(SpringBoot 3.5.14 + MyBatis-Plus 3.5.15 + MySQL 8.4 LTS)

> 📌 **Phase 4 写后端代码时主要参考**。全栈通用规则(技术栈版本 / 全栈安全 / 全栈接口契约 Result<T> / AI 协作)见 §一。

### 二·一 分层职责(对齐 init-skeleton 生成的 backend/ 8 类目录 + tech-designer §2 后端模块表)

| 层 | 类型 | 职责 | 关键类(示例) |
|---|---|---|---|
| `controller/` | 业务层 | 接收 HTTP 请求 + 参数校验(@Valid)+ 转发 Service · **禁止**写业务逻辑 | UserController / ProductController |
| `service/` + `service/impl/` | 业务层 | 业务逻辑全部放这里 · 方法名见名知意(`registerUser` / `checkUsernameExists`)· 抛业务异常(BusinessException) | UserService + UserServiceImpl |
| `mapper/` | 数据访问层 | 继承 `BaseMapper<Entity>` · 简单 CRUD 用内置方法 · 复杂查询有限例外见 §二·四 | UserMapper extends BaseMapper |
| `entity/` | 数据访问层 | 数据库表的 ORM 映射(@TableName / @TableId / @TableField · 详见 §二·二) | User / Product |
| `config/` | 配置层 | CORS / MybatisPlus / WebMvc 等配置类 | CorsConfig / MybatisPlusConfig / WebMvcConfig |
| `util/` | 工具层 | JwtUtils 等通用工具类 | JwtUtils |
| `interceptor/` | 拦截层 | LoginInterceptor 校验 JWT | LoginInterceptor |
| `common/` | 通用层 | `Result<T>`(全栈接口契约 · 见 §一·三)+ GlobalExceptionHandler + BusinessException(Phase 4 一并创建 · 见 §二·三) | Result / GlobalExceptionHandler / BusinessException |

### 二·二 Entity 规范

- **`@TableName`** 必加(MP 默认下划线↔驼峰策略对单词表名 OK · 但缩写表名/带下划线表名/复数差异时**必须**显式声明 · 防误映射)
- **主键**用 `@TableId(IdType.AUTO)`(数据库自增 · 对齐 `DATABASE_DESIGN.md §3 #4` 主键规范)
- **字段**用 `@TableField` 标注(下划线↔驼峰转换)
- **时间字段**一律用 `LocalDateTime`,**禁止**用 `Date`(对齐 `DATABASE_DESIGN.md §3 #5` DATETIME → LocalDateTime 映射)
- **业务字段类型选择**对齐 `DATABASE_DESIGN.md §3 #6` 字段约定(VARCHAR(N) / TEXT / DECIMAL(M,N) / TINYINT(1)· **DECIMAL 禁止用 FLOAT/DOUBLE** 防精度丢失)
- **密码字段**加 `@JsonIgnore` 防响应泄漏(注意:**注册时密码也会被忽略,需用 DTO 接收**)
- **逻辑删除字段**用 `@TableLogic`(对齐 `DATABASE_DESIGN.md §3 #6` 软删除字段 `is_deleted TINYINT(1) NOT NULL DEFAULT 0`)

### 二·三 Service / Controller

#### 接口入参/返参

- Controller 入参用 **DTO** 接收(详见下方 DTO 命名约定)
- Controller 返参**统一**用 `Result<T>` 包装(**完整定义 + 静态工厂签名见 §一·三 全栈接口契约** · 单一权威源 · 不在本处重复)
- **禁止**直接返回 entity / List / Map(必须 Result<T> 包装)

#### DTO 命名约定

- `<功能>Request`:接口入参(如 `UserLoginRequest` / `UserCreateRequest`)
- `<功能>Response`:接口返参(如 `UserLoginResponse`)
- `<实体>DTO`:跨层通用传输(如 `UserDTO`)
- 统一放 `entity/dto/` 子目录(或单独 `dto/` 目录 · 项目内统一即可)

#### 业务异常处理(对齐 `API_DESIGN.md §4` 业务异常码表)

- Service 检查业务规则失败时抛 `BusinessException(code, message)` —— code 用 `API_DESIGN.md §4.3` 模块编号(如 1001=用户名重复 · 1002=密码错误)
- Controller **禁止**做 try-catch · 业务异常由全局异常处理器统一处理 → `Result.error(code, msg)`
- 异常处理:统一用 `@RestControllerAdvice`(骨架已生成 GlobalExceptionHandler · 含全局异常 5 项 400/401/403/404/500 处理者)

> 📌 **BusinessException 类由 init-skeleton 生成**(放 `common/` 目录 · 含 `Integer code` + `String message` 字段 + 标准构造器 · **2026-05-10 第 2 次链路断点修复方案 B**);GlobalExceptionHandler 已含 `@ExceptionHandler(BusinessException.class)` 处理者 → `Result.error(e.getCode(), e.getMessage())`。Service 层直接 `throw new BusinessException(code, message)` 即可,**无需自创**。

### 二·四 MyBatis-Plus 用法

- 简单 CRUD:用 `BaseMapper` 的内置方法(`selectById` / `insert` / `updateById` 等)
- **条件查询**:**用 `LambdaQueryWrapper`**,**禁止**字符串拼接 SQL
  ```java
  // ✅ 正确
  new LambdaQueryWrapper<User>().eq(User::getUsername, username);
  // ❌ 错误
  "SELECT * FROM user WHERE username = '" + username + "'"
  ```
- **复杂查询**(多表 join / 统计 / 子查询)**有限例外**才允许写 XML 或 `@Select` · 必须用 `#{}` 参数化(主路径「统一用 LambdaQueryWrapper」见 §一·一·后端):
  ```java
  // ✅ XML 映射(放 backend/src/main/resources/mapper/UserMapper.xml)
  // <select id="selectUserStats" resultType="UserStatsDTO">
  //   SELECT u.id, u.username, COUNT(p.id) as productCount
  //   FROM user u LEFT JOIN product p ON p.user_id = u.id
  //   WHERE u.create_time > #{since} GROUP BY u.id
  // </select>

  // ✅ @Select 注解(适合简短复杂查询 · 不必 XML)
  @Select("SELECT COUNT(*) FROM user WHERE role = #{role}")
  long countByRole(@Param("role") String role);
  ```
- **分页**用 MP 的 `IPage` + `Page<T>`,**禁止**手动 limit(对齐 `API_DESIGN.md §1` 分页参数 pageNum + pageSize)

### 二·五 后端特定安全(全栈通用安全见 §一·二)

- **密码**必须 BCrypt 加密(`BCryptPasswordEncoder`,来自 `spring-security-crypto 6.3.4` 子模块 · 见 §一·一·后端)· 数据库存哈希值不存明文
- **登录后接口**校验 JWT(骨架已生成 `JwtUtils` + `LoginInterceptor`)
- **输入参数**加 `@Valid` + 字段加 `@NotBlank` / `@Size` / `@Pattern` 等校验注解
- **敏感日志脱敏**:**禁止**打印密码 / 完整 token / 完整身份证号
- **文件上传**统一为本地 `uploads/` 目录(详见 `TECH_DESIGN.md §5 技术方案选型` · OSS 留 P2 加分项)· 校验文件类型 + 大小 · **禁止**直接拼接路径(防路径穿越)

### 二·六 配置规范

- `application.yml` **禁止**放生产密码 · 用 `application-dev.yml` / `application-prod.yml` 区分
- 数据库密码 / JWT 密钥放环境变量或 `application-local.yml`(已加入 .gitignore)
- 端口 / 日志级别等环境无关配置可写入主 yml

### 二·七 后端代码风格

- **2 空格缩进**(项目统一 · 虽然 Java 习惯用 4 空格,但本项目按 .editorconfig)
- **类名大驼峰** · 方法名/字段名小驼峰 · 常量全大写下划线
- **中文注释**,关键业务逻辑加注释

---

## 三、前端代码规范(Vue 3.5.34 + Vite 8.0.0 + Element Plus 2.13.7 + Pinia 3.0.4 + Axios 1.15.2)

> 📌 **Phase 5 写前端代码时主要参考**。全栈通用规则(技术栈版本 / 全栈安全 / 全栈接口契约 Result<T> 含 axios 拦截器约定 / AI 协作)见 §一。

### 三·一 文件组织(对齐 init-skeleton 生成的 frontend/src/ 8 类)

| 路径 | 类型 | 内容 | 关键文件(示例) |
|---|---|---|---|
| `src/main.js` | 入口 | createApp + router + Pinia + 全注册 ElementPlus + 引入 `element-plus/dist/index.css` + `.mount('#app')` | main.js |
| `src/App.vue` | 根组件 | `<template><router-view /></template>` + 全局 reset CSS | App.vue |
| `src/router/index.js` | 路由 | `createRouter` + `createWebHistory()` + 路由表 + 守卫 `beforeEach`(详见 `TECH_DESIGN.md §3` 前端路由设计) | router/index.js |
| `src/api/` | API 模块 | axios 实例 + 业务模块文件(详见 §三·三) | request.js / user.js / order.js |
| `src/stores/` | 状态管理 | Pinia store 文件(详见 §三·四) | user.js / cart.js |
| `src/views/` | 页面级组件 | 对应路由的页面 · **大驼峰 + `Page` 后缀**(2026-05-11 升级 · 强制 · 详见 `TECH_DESIGN.md §6(由 page-prototyper 生成)` 原型描述 + `vue-page-coder.md` 命名映射规则) | LoginPage.vue / HomePage.vue / PaymentListPage.vue |
| `src/components/` | 可复用组件 | 跨页面共用组件 · 大驼峰命名(**不带 Page 后缀** · 区分页面与可复用组件) | ConfirmDialog.vue / DataTable.vue |
| `src/styles/` | 全局样式 | 跨页面共用样式 / 主题变量 | reset.css / variables.scss |

> ✅ **`stores/` 命名链路断点已闭合**(2026-05-10 vue-page-coder 审核 · 方案 B):init-skeleton 已升级为生成 `stores/.gitkeep`(双数 Pinia 惯例)· 全栈链路统一用 `stores/` · **累计 4 次链路断点全部闭合**。
>
> ⚠️ **页面 vs 可复用组件**:**禁止**把「页面」和「可复用组件」混在一起;单个 .vue 文件超过 300 行考虑拆分子组件。

### 三·二 Composition API 写法

- 使用 `<script setup>` 语法,**禁止**用 `export default { setup() {} }` 旧写法
- 响应式:`ref` 用于基本类型 + 字符串 + 单值对象 · `reactive` 用于复杂对象 + 数组
- 计算属性用 `computed`,**禁止**在模板里写复杂表达式(`{{ a + b * c }}` 之类)
- 副作用用 `watch` 或 `watchEffect`,生命周期用 `onMounted` / `onUnmounted` 等

### 三·三 API 调用

#### 模块组织

- API 调用**统一**在 `src/api/` 目录管理,**禁止**在组件里直接 `axios.get` / `fetch`
- 每个业务模块对应一个 API 文件(如 `src/api/user.js` / `src/api/order.js`)· 函数命名 `<动作><实体>`(如 `loginUser` / `listProducts` / `createOrder`)
- 统一通过 `src/api/request.js` 的 axios 实例调用(骨架已生成)

#### axios 实例约定(对齐 §一·三 全栈接口契约 · 单一权威源)

- **baseURL** = `'/api'`(跟 vite.config.js 的 proxy + `API_DESIGN.md §1` 接口前缀对齐)
- **timeout** = `10000`(10 秒)
- **请求拦截器**:从 localStorage 读 token → 加 `Authorization: Bearer <token>` Header
- **响应拦截器**(三段处理 · 跟 §一·三 约定一致):
  - `code === 200` → 返 `res.data` 走业务
  - `code === 401` → 清 localStorage token + 跳 `/login` + `ElMessage.error('未登录')`
  - 其他 code(业务错 / 全局异常码 · 见 `API_DESIGN.md §4`) → `ElMessage.error(msg)` + reject
- 时间字段:Jackson 默认 ISO 8601 反序列化(`'2026-05-10T08:30:00'` → JS Date 对象 · 对齐 `API_DESIGN.md §1` 时间格式)

#### 错误处理

- axios 拦截器**统一**处理 401 / 业务错 → 组件层只 try-catch **业务关联错**(如「用户名重复」需在表单字段上特殊提示)
- **禁止**在组件里写 `if (res.code !== 200) ...` 的硬编码(那是拦截器的职责)

### 三·四 状态管理

- 跨组件共享状态用 **Pinia 3.0.4**,**禁止**用 Vuex(Vuex 4 已停止维护 · 官方推荐迁移 Pinia)
- 简单父子通信用 `props` / `emit`,**禁止**滥用 Pinia(状态只在两个组件之间用就 props/emit)
- Pinia store 放 `src/stores/` 目录(惯例双数 · 对齐 init-skeleton 生成的 `stores/.gitkeep`)
- store 命名:`use<功能>Store`(如 `useUserStore` / `useCartStore`)· 文件名小驼峰(`user.js`)
- store 模板用**组合式 store** 写法:`defineStore('<id>', () => { ... })`(对齐 §三·二 Composition API 风格)

### 三·五 UI 组件(Element Plus 2.13.7)

- 优先用 **Element Plus** 组件,**禁止**混用其他 UI 库(antd / Vuetify / Naive UI 等)
- **常用组件清单**(对齐 `TECH_DESIGN.md §6(由 page-prototyper 生成)` 原型描述 · 6 类):
  - **表格**:`el-table` + `el-table-column`
  - **表单**:`el-form` + `el-form-item` + `el-input` + `el-select` + `el-date-picker`
  - **按钮**:`el-button`(`type` 属性:primary / success / warning / danger / info)
  - **弹窗**:`el-dialog` + `el-message-box`(确认 / 警告)
  - **提示**:`ElMessage`(由 axios 拦截器统一调用 · 见 §三·三)
  - **分页**:`el-pagination`(对齐 `API_DESIGN.md §1` 分页参数 pageNum + pageSize)
- 表单校验通过 `el-form` 的 `rules` 属性,**禁止**在 submit 时手写校验

### 三·六 前端特定安全(全栈通用安全见 §一·二)

- **禁止**在前端代码里硬编码任何**密钥 / 密码 / token / API key**
- 密钥**必须**放后端 · 前端只通过登录接口拿 JWT token
- **JWT token 存储**:`localStorage`(键名约定 `'token'` · 对齐 init-skeleton 生成的 `api/request.js` 请求拦截器 + `TECH_DESIGN.md §3` 路由守卫)· **禁止**用 cookie(本项目用 JWT 无 session,无需 cookie)
- 表单输入做基础校验(必填 / 长度 / 格式 · 用 el-form rules)· 但**安全校验仍以后端为准**
- localStorage / sessionStorage **禁止**存敏感信息(密码 / 完整身份证号 / 银行卡号)

### 三·七 前端代码风格

- **2 空格缩进**(对齐 §二·七 + .editorconfig 全局)
- **单引号优先**(JS / template 内属性都用单引号)
- 语句末尾**加分号**
- **`const` / `let`**,**禁止** `var`
- 模板里属性按字母排序(可选)

---

## 四、Git commit message 规范

> 📌 **本节是项目 commit message 的单一权威源**——AI 在「生成提交内容」时按此规范输出;`/git-committer` 命令也会引用本节;`08b-项目实施操作流程.md §9.2` 与本节对齐(本节优先)。

生成 commit message 时,严格遵循以下规范。

### 四·一 格式

```
type(scope): description

[可选 body]
```

- **type 和 description 必填**(三段中至少这两段)
- **scope 视情况可省略**(无明确 scope 时可写成 `type: description`,但**有 scope 时必须用括号包裹**)
- **首行**(标题):≤ 50 字符(中文每字按 2 字符算,即纯中文 ≤ 25 字)
- **不要**句末加句号
- 简单示例:`fix(p4-auth): 修复 JWT 过期未刷新` 或 `chore: 初始化项目骨架`

### 四·二 type 清单(必须从下表选 · 8 选 1)

| type | 用途 | 示例 |
|------|------|------|
| `feat` | 新功能 | `feat(p4-auth): 添加用户注册接口` |
| `fix` | bug 修复 | `fix(p4-auth): 修复 JWT 过期未刷新` |
| `docs` | 文档变更 | `docs(p1): SRS + 概要设计 + R-01 修复` |
| `style` | 代码格式(不影响功能) | `style: 统一前端缩进为 2 空格` |
| `refactor` | 重构(既非新功能也非 bug fix) | `refactor(p4-service): 抽出公共校验方法` |
| `test` | 测试相关 | `test(p4-user): 补充注册单元测试` |
| `chore` | 杂项(构建/工具/依赖/初始化) | `chore: 升级 MyBatis-Plus 到 3.5.15` |
| `perf` | 性能优化 | `perf(p2-db): 用户列表加索引` |

### 四·三 scope 命名约定

scope 是改动涉及的模块/层/Phase,**小写英文**。本项目允许两种合规风格并存:

#### A. Phase 前缀复合(推荐 · 验收 grep 友好)

格式 `p<N>` 或 `p<N>-<模块>`,N 是 Phase 编号(0-8):
- `p1` / `p2` / `p3` —— 文档 Phase(SRS / 数据库 / API 设计)
- `p4-auth` / `p4-user` / `p4-repair` —— Phase 4 后端各业务模块
- `p5-LoginPage` / `p5-HomePage` —— Phase 5 前端各页面(页面名保留 PascalCase 例外)
- `p7` / `p8` —— Phase 7 重构 / Phase 8 部署

> 验收时跑 `git log --oneline | grep "p4"` 能一眼看到 Phase 4 的所有 commit,这是推荐复合 scope 的核心原因。

#### B. 单词模块名(简单/跨 Phase 场景)

- 后端模块:`auth` / `user` / `order` / `service` / `controller`
- 前端页面/模块:`login` / `home` / `router` / `axios`
- 跨层/工具:`api`(联调) / `db`(数据库) / `docs` / `rules`(规则文件) / `init`(初始化)

#### 选择策略

- 同一项目内**两种风格可并存**(如 rules 改动用 `rules`,业务代码用 `p4-auth`)
- **业务模块改动首选 phase 前缀复合**(便于 ≥30 次 commit 跨 Phase 时按 phase 检索)
- **跨 Phase 的工具/配置改动用单词**(如 `chore(rules)` `docs(api)` `chore: 初始化项目骨架`)

### 四·四 description 写法

- **中文为主**(贴近 60 人中国课堂),**英文术语保留**:BCrypt / JWT / SRS / API / CRUD / Mapper / Pinia / Axios 等不强行翻译
- 简洁,**说明「做了什么」**(不是「为什么做」)
- 动词开头:添加 / 修复 / 重构 / 优化 / 调整 / 移除 / 升级 / 初始化 / 配置 / 完善
- ≤ 25 字(纯中文)或 ≤ 50 字符(中英文混排),精炼
- **不写**「等」「相关」「一些」这类模糊表述

### 四·五 body(可选)

- 与标题空一行
- 说明**为什么改**(动机、上下文)
- 行宽 72 字符内换行
- **总长度建议 ≤ 200 字符**(超过说明改动范围太大,应拆成多个 commit)

### 四·六 示例(对齐本规范 · 8 个 Phase 各取 1-2 例)

✅ **好的**:
- `chore: 初始化 [题目] 项目骨架`(scope 可省 · `/init-skeleton` 第 1 commit)
- `chore: 配置数据库 + 验证开发环境`(Phase 0 §6 第 2 commit)
- `docs(rules): 写入 CLAUDE.md §一 题目和角色信息`(Phase 0 §7)
- `docs(p1): SRS + 概要设计 + 页面原型 + R-01 修复`(Phase 1 末)
- `feat(p2): 数据库脚本 + R-03 修复 + 测试数据`(Phase 2 末)
- `docs(p3): API 设计 (RESTful) + R-04 修复`(Phase 3 末)
- `feat(p4-auth): JWT 登录注册 + R-05 修复`(Phase 4 业务模块)
- `fix(p4-repair): 修复派单流程的空状态问题`
- `feat(p5-LoginPage): 登录表单 + axios 模块 + R-06 修复`
- `test(p6): 补充 repair-service 单元测试`
- `refactor(p7): 应用 R-07/R-08 安全和命名修复`
- `docs(p8): 部署文档 + README 完善`
- `chore: 课程验收最终提交`

❌ **不好的**(违反规则):
- `更新代码`(无 type)
- `feat: 改了一些东西`(description 模糊 + 用「一些」)
- `Feature(User): Add register API.`(type 大写 + scope 大写 + 句末句号)
- `fix(login): 修复登录页面的样式问题以及登录接口的密码加密问题`(description ≥25 字 + 应拆 2 个 commit)
- `feat(P4-AUTH): 添加注册`(scope 大写)
- `feat (p4-auth): 添加注册`(`type` 与 `(scope)` 之间有空格)

### 四·七 拒绝场景

如果暂存改动**跨多个不相关功能**(既改了 user 又改了 order,且无关联),**提示用户拆分提交**,不要硬合一个 commit。

---

> **完整 commit/push 节奏**(≥30 次 / ≥12 天分布)见 `08b-项目实施操作流程.md §9`
> **自动执行 commit + push** 用 `/git-committer` 命令(详见 `.claude/commands/git-committer.md`)
> **项目当前进度**(Phase / 已完成模块 / 已建表 / 已有接口)见 `.claude/project-status.md`(由 `/rules-updater` 每 Phase 末自动更新)
