---
name: init-skeleton
description: 一次性初始化全栈项目骨架(backend/+frontend/+docs/+CLAUDE.md+git init,6 项硬门槛自检 · 2026-05-10 版本基线)
---

你是 SpringBoot 3.5 + Vue 3.5 全栈项目骨架初始化助手(对应 06 G-01a · 2026-05-10 版本基线)。

## 任务
基于用户给的 6 个变量,一次性生成完整项目骨架,跑通后**用户进入 08b §3-§6 完成 yml/数据库/编译/Gitee push**(本命令只做骨架生成 + git init 第 1 次 commit)。

## 输入(6 个变量)

- 题目: [如 社区物业综合管理系统] · 用于 README/CLAUDE.md/数据库注释
- 核心实体: [4-5 个,如 用户、房屋、物业费、报修单] · 后续 Phase 2 /db-designer 据此设计表
- 核心功能: [3-5 个,如 缴费管理、报修工单、公告通知] · ⚠️ 仅用于 README §一 + CLAUDE.md §五·一 业务模块清单引导,init-skeleton 阶段不直接生成业务代码
- 包名: [如 com.example.property] · Java 包路径 + MyBatis-Plus type-aliases-package
- 数据库名: [如 property_db] · application.yml datasource.url
- 项目根目录名: [如 property-management] · 项目根目录 + git commit message 主语

## 必须生成的目录结构(每个文件都要有完整内容,不要只创建空文件)

> ⚠️ **重要前置约束**:学生调用本命令时,工作目录已通过教师项目模板 zip 解压到位(详见学生 08b §1.4)。这意味着 `.claude/commands/` (32 个 .md) + 根目录 `CLAUDE.md` (AI 编码规则) + `.claude/project-status.md` (动态状态) **已经存在**——你**不要重建/覆盖**这些文件的任何内容。
>
> 你需要做的:**按学生题目生成下面列出的 backend / frontend / docs / 配置文件**(根据学生提供的 6 个变量)。
>
> 🚨 **关键约束**:
> 1. **`.claude/` 目录已存在**(zip 解压带来的 commands/ + project-status.md),**不要触碰**
> 2. **根目录 `CLAUDE.md` 已存在**(zip 解压带来的 AI 编码规则 · 4 大节静态规范),你只对它做 2 处 inject(详见本节末"### CLAUDE.md inject 操作"),**不要覆盖整个文件**
> 3. **`docs/00-选题标定.md` 如已存在则保留不覆盖**(学生 §1.4.3 已放好对应题号的标定卡 · 且 §1.5/§1.6/§1.7 已跑完 R-00 审核 + 修复 + commit C1 · Phase 1 srs-writer 依赖此文件)
> 3. 其他文件:不存在则创建,已存在则按学生题目改写
> 4. ⚠️ **2026-05-11 前置硬约束**:**调用本命令前必须 R-00 已审已修**(`/scoping-reviewer` §一审核 + §二应用修复都跑完 · `docs/00-选题标定.md` 内已无 `<!-- R-00-issue-N -->` 注释 · 已有 `<!-- R-00-issue-N: 已修复 -->` 标记)· 否则源头未净 · 本命令生成的 README §一 / CLAUDE.md §五·一 业务模块清单等占位文档**会基于错的标定卡污染下游 Phase 1-7**。新顺序 R-00 提前 · 本命令一次到位 · 不再有"重跑 init-skeleton 补救"机制(详见 08b §1.5/§1.6/§1.7 + §8.2 Phase 0 顺序简表)。

[项目根目录名]/
├── backend/
│   ├── pom.xml(锁定**精确版本**无 ^/~/LATEST · 2026-05-10 基线 · 详见本节末"### pom.xml 规范")
│   ├── src/main/java/[包路径]/
│   │   ├── Application.java                ← @SpringBootApplication + @MapperScan({{包名}}.mapper) + main 方法
│   │   ├── config/CorsConfig.java          ← @Configuration + WebMvcConfigurer.addCorsMappings;允许 http://localhost:5173,所有方法+所有头,exposed-headers 含 Authorization
│   │   ├── config/MybatisPlusConfig.java    ← @Configuration + PaginationInnerInterceptor(DbType.MYSQL);可加自动填充 createTime/updateTime
│   │   ├── config/WebMvcConfig.java         ← @Configuration + WebMvcConfigurer.addInterceptors;注册 LoginInterceptor 拦截 /api/** 但放过 /api/login + /api/register
│   │   ├── common/Result.java               ← 泛型类 Result<T>;字段:Integer code/String message/T data;静态工厂:success(T)/success(T,String)/error(Integer,String)/error(String)
│   │   ├── common/BusinessException.java    ← 业务异常类(继承 RuntimeException);字段:Integer code/String message;构造器 BusinessException(Integer code, String message);Service 层抛此异常携带 api-designer §4.3 业务异常码(1xxx-9xxx)· 2026-05-10 第 2 次链路断点修复 · service-coder 直接 throw 即可不需自创
│   │   ├── common/GlobalExceptionHandler.java  ← @RestControllerAdvice;处理 MethodArgumentNotValidException(400 校验错)、@ExceptionHandler(BusinessException.class)→ Result.error(e.getCode(), e.getMessage())、Exception(500 兜底)→ 统一返 Result.error
│   │   ├── util/JwtUtils.java               ← 工具类;静态方法 generateToken(Long userId,String role)→String、parseToken(String)→Claims、validateToken(String)→boolean;secret 从 application.yml jwt.secret 读
│   │   ├── interceptor/LoginInterceptor.java   ← implements HandlerInterceptor;preHandle 从 Header `Authorization` 取 `Bearer <token>` → JwtUtils 验证 → 失败 401+Result.error("未登录") → 成功 request.setAttribute("userId"/"role")
│   │   └── controller/, service/, mapper/, entity/(4 个空目录,**每个加 .gitkeep 占位**防 git 不跟踪空目录;Phase 4 填业务代码)
│   └── src/main/resources/
│       ├── application.yml(必含 6 项配置 · 详见本节末"### application.yml 必含项")
│       └── mapper/.gitkeep
├── frontend/
│   ├── package.json(锁定**精确版本** · 2026-05-10 基线:Node.js 24 LTS + Vue 3.5.34 + Vue Router 5.0.6 + Pinia 3.0.4 + Element Plus 2.13.7 + Axios 1.15.2 + Vite 8.0.0;scripts:dev/build/preview;`engines.node: ">=24.0.0"`;`packageManager: pnpm@10.33.4` · 用 pnpm LTS 不用 npm/yarn)
│   ├── vite.config.js(详见本节末"### vite.config.js 规范")
│   ├── index.html(`<title>` 设为 {{题目}})
│   └── src/
│       ├── main.js                          ← createApp(App) + 挂载 router + 创建 Pinia + 全注册 ElementPlus + 引入 element-plus/dist/index.css + .mount('#app')
│       ├── App.vue                          ← `<template><router-view /></template>` + 全局 reset CSS(margin/padding 0)
│       ├── router/index.js                  ← createRouter + createWebHistory();默认路由:`/login`(LoginPage 占位 · meta.requiresAuth=false 显式公开)+ `/`(HomePage 占位 · meta.requiresAuth=true)+ 全局守卫 beforeEach:`if (to.meta.requiresAuth && !localStorage.getItem('token')) return { path: '/login', query: { redirect: to.fullPath } }`(回跳支持 · login-coder 登录成功后读 route.query.redirect 跳回原页面 · 对齐 TECH_DESIGN.md §3 + login-coder.md §一)
│       ├── api/request.js                   ← axios.create({baseURL:'/api',timeout:10000});请求拦截器:从 localStorage 读 token 加 `Authorization: Bearer <token>`;响应拦截器:401 跳 /login + 业务 code≠200 ElMessage.error(msg);return res.data
│       ├── stores/.gitkeep                  ← Pinia stores 目录占位(Phase 4 填 · Pinia 惯例双数 · 2026-05-10 stores/store 链路断点闭合 · 方案 B)
│       ├── views/
│       │   ├── LoginPage.vue                ← ⚠️ **最小占位文件**(2026-05-11 链路断点修复 · Phase 5 /login-coder 会覆盖 · 不要手删 · 否则 pnpm dev 启 Vite 8 Rolldown 扫描器报 ENOENT · router/index.js 静态 import 引用它)
│       │   └── HomePage.vue                 ← ⚠️ **最小占位文件**(同上 · Phase 5 /vue-page-coder 页面=HomePage 会覆盖 · 不要手删)
│       ├── components/.gitkeep              ← 通用组件目录占位
│       └── styles/.gitkeep                  ← 全局样式目录占位
├── docs/
│   ├── 00-选题标定.md(⚠️ **如已存在则保留不覆盖**——学生 §1.4.3 已放好对应题号的标定卡)
│   ├── PRD.md / TECH_DESIGN.md / DATABASE_DESIGN.md / API_DESIGN.md / DEPLOY.md(空白模板 · 5 个文件标准章节标题清单详见本节末"### docs/ 5 个 .md 标准章节")
│   └── 对话记录/.gitkeep
├── sql/.gitkeep              ← Phase 2 /db-designer 生成 01-init.sql 等;init 阶段空目录占位
│                                ⚠️ application.yml 引用的是 `classpath:mapper/*.xml`(指向 backend/.../resources/mapper/)而不是 sql/,所以 sql/ 此时为空**不影响 mvn compile**
├── ai-records/.gitkeep       ← 学生整理 AI 对话片段的归档目录
├── .claude/                  ← ⚠️ 已存在(zip 解压时带来的 commands/ 32 个 .md + project-status.md),不要重建
├── CLAUDE.md                 ← ⚠️ 已存在(zip 解压时带来的 AI 编码规则 · 4 大节静态规范) · 你只做 inject(详见本节末"### CLAUDE.md inject 操作"),**不要覆盖**
├── AGENTS.md(1 句话:"请先读 CLAUDE.md")
├── README.md(8 节 · 详见本节末"### README.md 8 节定义")
├── .gitignore(完整列表防仓库爆 · 见下方"⚠️ .gitignore 必须包含项")
└── .editorconfig ⚠️ **如已存在则保留不覆盖**(zip 已带通用版本)

### pom.xml 规范

完整 pom.xml 必含 `<properties>` + 7 类 dependency:

```xml
<properties>
    <maven.compiler.source>21</maven.compiler.source>
    <maven.compiler.target>21</maven.compiler.target>
    <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
</properties>

<dependencies>
    <!-- 1. Web -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <!-- 2. 参数校验 @Valid -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-validation</artifactId>
    </dependency>
    <!-- 3. MySQL 驱动 -->
    <dependency>
        <groupId>com.mysql</groupId>
        <artifactId>mysql-connector-j</artifactId>
        <version>8.4.0</version>
    </dependency>
    <!-- 4. MyBatis-Plus(SpringBoot 3 必须用 spring-boot3 后缀!)-->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-spring-boot3-starter</artifactId>
        <version>3.5.15</version>
    </dependency>
    <!-- 4.1 MP 分页插件(3.5.9+ jsqlparser 已从主 JAR 拆出,必须显式引入,否则 PaginationInnerInterceptor 找不到) -->
    <dependency>
        <groupId>com.baomidou</groupId>
        <artifactId>mybatis-plus-jsqlparser</artifactId>
        <version>3.5.15</version>
    </dependency>
    <!-- 5. JJWT 模块化(详见本节末) -->
    <!-- 6. spring-security-crypto(只用于 BCryptPasswordEncoder · 不引完整 starter-security 以避免默认 Filter Chain 干扰) -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-crypto</artifactId>
        <version>6.3.4</version>
    </dependency>
    <!-- 7. Lombok -->
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <version>1.18.46</version>
        <optional>true</optional>
    </dependency>
    <!-- 8. 测试 -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

⚠️ MyBatis-Plus 必须用 `mybatis-plus-spring-boot3-starter`(SpringBoot 3 适配版),用错为旧版 `mybatis-plus-boot-starter` 会启动失败。

⚠️ **MyBatis-Plus 3.5.9+ 已将 `jsqlparser` 从主 JAR 拆为独立模块 `mybatis-plus-jsqlparser`**——必须显式引入(版本与 starter 对齐 `3.5.15`)。否则 `MybatisPlusConfig` 里 `new PaginationInnerInterceptor(DbType.MYSQL)` 编译/启动报 `ClassNotFoundException: com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor`(2026-05-11 链路断点修复 · 现象:学生 init-skeleton 后第一次 mvn compile 直接红屏)。

⚠️ **`spring-security-crypto` 不在 Spring Boot BOM 中**——必须显式写 `<version>6.3.4</version>`(对齐 CLAUDE.md §一·一·后端版本表)。Phase 4 学生写 `new BCryptPasswordEncoder()` 时才能编译通过;若漏掉本依赖,UserService 直接编译失败"找不到符号 BCryptPasswordEncoder"。

### vite.config.js 规范

```javascript
import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
        // 不用 rewrite——后端 controller 也用 /api 前缀
      }
    }
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    }
  }
})
```

### views/ 两个占位 .vue 文件(2026-05-11 链路断点修复 · 必生成)

> ⚠️ **为什么必须生成占位**:`router/index.js` 静态 import 了 `LoginPage` + `HomePage` 两个路由组件 · Vite 8 + Rolldown 扫描器在 `pnpm dev` 启动时会扫这些 import 路径 · 若 `views/` 下文件不存在 → 直接报 `ENOENT: no such file or directory, open '.../LoginPage.vue'` → **Phase 0 §5.2 `pnpm dev` 验证就过不去**(2026-05-11 学生踩坑实录 · trae-work/code2/teacher-demo-38 项目)。
>
> ✅ **设计原则**:占位文件内容最小化,但**必须是合法 SFC**(让 Vite 能解析)· Phase 5 学生跑 `/login-coder` / `/vue-page-coder 页面=HomePage` 时会**覆盖式重写**这两个文件(不需要先手删)。

#### views/LoginPage.vue(最小占位)

```vue
<!-- views/LoginPage.vue · init-skeleton 占位 · 2026-05-11 链路断点修复 -->
<!-- ⚠️ 不要手删本文件,否则 pnpm dev 报 ENOENT · Phase 5 /login-coder 会覆盖式重写本文件 -->
<template>
  <div class="placeholder">
    <h2>登录页占位</h2>
    <p>本页由 <code>/init-skeleton</code> 生成 · Phase 5 调用 <code>/login-coder</code> 会用真实登录页覆盖。</p>
  </div>
</template>

<script setup>
</script>

<style scoped>
.placeholder {
  padding: 40px;
  text-align: center;
  color: #666;
}
</style>
```

#### views/HomePage.vue(最小占位)

```vue
<!-- views/HomePage.vue · init-skeleton 占位 · 2026-05-11 链路断点修复 -->
<!-- ⚠️ 不要手删本文件,否则 pnpm dev 报 ENOENT · Phase 5 /vue-page-coder 页面=HomePage 会覆盖式重写本文件 -->
<template>
  <div class="placeholder">
    <h2>首页占位</h2>
    <p>本页由 <code>/init-skeleton</code> 生成 · Phase 5 调用 <code>/vue-page-coder 页面=HomePage</code> 会用真实首页覆盖。</p>
  </div>
</template>

<script setup>
</script>

<style scoped>
.placeholder {
  padding: 40px;
  text-align: center;
  color: #666;
}
</style>
```

### docs/ 5 个 .md 标准章节(只生成章节标题占位,内容由后续 Phase 命令填)

#### docs/PRD.md(由 Phase 1 /srs-writer 填 · 全量 P0+P1+P2 三档合并 · 不再分子章节)

```markdown
# {{题目}} - 需求规格说明书

## 1. 项目概述
## 2. 用户角色定义
## 3. 功能需求列表(单一列表 · 全量 P0+P1+P2 · 每功能 8 字段含「实现优先级」)
## 4. 非功能需求
## 5. 功能与页面映射表(全量 · 表头 4 列含「实现优先级」)
## 6. 优先级调整说明(含 R-00 标定卡层 + R-01 SRS 层两层调整)
```

> 📌 **2026-05-10 升级**:原 §3.1/§3.2/§3.3 三档子节合并为单一 §3 全量列表 · 加「实现优先级」字段(P0 必做/P1 应做/P2 可选)· 学生答辩时按字段值即可分清优先级 · 实现阶段按优先级分批做。

#### docs/TECH_DESIGN.md(由 Phase 1 /tech-designer + /page-prototyper 填)

```markdown
# {{题目}} - 概要设计

## 1. 系统架构
## 2. 后端模块划分
## 3. 前端路由设计
## 4. 关键业务流程图(Mermaid sequenceDiagram / flowchart)
## 5. 技术方案选型
## 6. 页面原型描述(由 /page-prototyper 追加)
```

#### docs/DATABASE_DESIGN.md(由 Phase 2 /db-designer 填)

```markdown
# {{题目}} - 数据库设计

## 1. ER 图(Mermaid)
## 2. 表清单与关系
## 3. CREATE TABLE 完整 SQL(utf8mb4)
## 4. 测试数据(INSERT)
```

#### docs/API_DESIGN.md(由 Phase 3 /api-designer 填)

```markdown
# {{题目}} - API 接口设计

## 1. 接口约定(跨接口共用规则 · URL 前缀 /api / 统一 Result<T> / JWT Header / 分页 / RESTful 命名)
## 2. 接口清单(按业务模块分组 · markdown 表格)
## 3. 接口详情(每接口一节 · 请求参数表 + 成功响应 JSON + 异常响应表)
## 4. 通用响应格式 + 异常码表(全局异常码 + 业务异常码)
```

#### docs/DEPLOY.md(由 Phase 8 /deploy-writer 填)

```markdown
# {{题目}} - 部署文档

## 1. 部署架构
## 2. 环境要求
## 3. 部署步骤(后端/前端/数据库)
## 4. 启动验证
## 5. 故障排查
```

### application.yml 必含 6 项配置

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/{{数据库名}}?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai&useSSL=false
    driver-class-name: com.mysql.cj.jdbc.Driver
    username: root
    password: <请填写>          # ⚠️ 学生 08b §3 改;教师演示项目也用 <请填写> 占位防泄密
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

mybatis-plus:
  mapper-locations: classpath:mapper/*.xml
  type-aliases-package: {{包名}}.entity
  configuration:
    map-underscore-to-camel-case: true
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

jwt:
  secret: <请填写至少32字符的密钥>
  expiration: 7200             # 单位:秒(2 小时)

logging:
  level:
    {{包名}}: debug
```

⚠️ `{{数据库名}}` 和 `{{包名}}` 必须替换为学生提供的实际值(从 6 个变量中取)。

### CLAUDE.md inject 操作

> 📌 **背景**:根目录 `CLAUDE.md` 由教师项目模板维护(zip 解压时已存在 · 含 4 大节 AI 编码规则:§一 项目基础 / §二 后端规范 / §三 前端规范 / §四 Git commit 规范)。本命令**不覆盖** CLAUDE.md,只做以下 2 处 inject。

#### Inject 1:替换起手段占位符

CLAUDE.md 起手段含两个占位符,本命令必须替换:

| 占位符 | 学生提供的来源 | 替换示例 |
|--------|---------------|----------|
| `{{题目}}` | 学生题目变量(本命令 6 个变量之一) | `社区物业综合管理系统`(第 38 题教师演示用) |
| `{{角色列表}}` | 从 `docs/00-选题标定.md § 一` "JWT 角色"行提取(多角色项目去字段说明括注后照抄全集 · 单角色项目填"单一用户角色")· 分隔符跟卡保持一致 | `业主、物业人员、管理员`(第 38 题教师演示用) |

替换后起手段示例:
```
你是一个 SpringBoot 3 + Vue 3 全栈开发助手。本项目是 **社区物业综合管理系统**,用户角色:**业主、物业人员、管理员**(从 `docs/00-选题标定.md §一` "JWT 角色"行复制 · 多角色项目去掉字段说明括注后照抄全集 · 单角色项目填 `单一用户角色` · 详见 08b §7.2)。
```

#### Inject 2:末尾追加 §五 项目元信息

在 CLAUDE.md 文件末尾(§四 Git commit 规范之后,文末引用块之前)追加一节:

```markdown
---

## 五、项目元信息(由 /init-skeleton 注入)

### 五·一 业务模块清单

(从 docs/00-选题标定.md § 三 P0 清单提取 · 每模块 1 行说明 + 优先级标签,如「P0」「P1」)

- **<模块名>**(P0):一句话说明
- ...

### 五·二 关键路径速查(高频文件位置)

后端:
- 入口:`backend/src/main/java/{{包路径}}/Application.java`
- 通用:`common/Result.java` / `common/BusinessException.java` / `common/GlobalExceptionHandler.java`
- 工具:`util/JwtUtils.java`
- 拦截器:`interceptor/LoginInterceptor.java`
- 配置:`config/CorsConfig.java` / `config/MybatisPlusConfig.java` / `config/WebMvcConfig.java`
- 业务层:`controller/` / `service/` + `service/impl/` / `mapper/` / `entity/`

前端:
- 入口:`frontend/src/main.js`
- 路由:`router/index.js`
- API:`api/request.js`(axios 实例) + `api/<module>.js`(业务模块)
- 状态:`stores/`
- 页面:`views/`(`*Page.vue`) / 组件:`components/`

文档:
- 业务需求:`docs/PRD.md`
- 技术设计:`docs/TECH_DESIGN.md`
- 数据库:`docs/DATABASE_DESIGN.md`
- API:`docs/API_DESIGN.md`
- 部署:`docs/DEPLOY.md`
- 选题:`docs/00-选题标定.md`
- 对话归档:`docs/对话记录/`
```

> 📌 当前阶段(Phase / 已完成模块 / 已建表 / 已有接口)由 `/rules-updater` 命令维护在 `.claude/project-status.md`,**不**写入 CLAUDE.md。

### README.md 8 节定义

```markdown
# {{题目}}

> 一句话背景(从 docs/00-选题标定.md § 一提取)

## 一、项目简介
- **题目**:{{题目}}
- **核心实体**:{{核心实体}}(逗号分隔)
- **角色**:{{角色列表}}(从 docs/00-选题标定.md § 一 "JWT 角色"行提取)
- **当前 Phase**:Phase 0(项目初始化 · 由 /rules-updater 自动更新)

## 二、技术栈
(后端 + 前端版本表 · 与 根目录 CLAUDE.md §一·一(技术栈)对齐 · 2026-05-10 基线)

### 后端
- JDK 21 + SpringBoot 3.5.14 + MyBatis-Plus 3.5.15(starter + jsqlparser 子模块,3.5.9+ 拆包后必须同时引入)
- MySQL 8.4 LTS(驱动 mysql-connector-j 8.4.0)
- JJWT 0.13.0(模块化引入)+ Lombok 1.18.46

### 前端
- Vue 3.5.34 + Vue Router 5.0.6 + Pinia 3.0.4
- Element Plus 2.13.7 + Axios 1.15.2 + Vite 8.0.0

## 三、项目结构
(简要目录树:backend/ frontend/ docs/ sql/ ai-records/ .claude/ + 根 CLAUDE.md)

## 四、数据库设计
- 表数量:N(Phase 2 /db-designer 生成后填,详见 [docs/DATABASE_DESIGN.md](docs/DATABASE_DESIGN.md))
- 表名清单:user / xxx / xxx ...

## 五、API 接口
- 接口数量:N(Phase 3 /api-designer 生成后填,详见 [docs/API_DESIGN.md](docs/API_DESIGN.md))
- URL 前缀:`/api/...`

## 六、快速开始

### 后端
\`\`\`bash
cd backend
mvn clean compile
mvn spring-boot:run    # 启动后端 http://localhost:8080
\`\`\`

### 前端
\`\`\`bash
cd frontend
pnpm install           # 用 pnpm 不要用 npm/yarn(详见 CLAUDE.md §一·一·前端)
pnpm dev               # 启动前端 http://localhost:5173
\`\`\`

> ⚠️ 启动前先按 08b §3 改 `backend/src/main/resources/application.yml` 数据库密码。
> 💡 还没装 pnpm? 跑 `npm install -g pnpm`(详见 08a §5 末尾 pnpm 安装步骤)

## 七、文档索引
- [PRD 需求规格](docs/PRD.md)
- [概要设计](docs/TECH_DESIGN.md)
- [数据库设计](docs/DATABASE_DESIGN.md)
- [API 设计](docs/API_DESIGN.md)
- [部署文档](docs/DEPLOY.md)
- [AI 对话记录](docs/对话记录/)
- [选题标定](docs/00-选题标定.md)

## 八、验收清单(05-验收方案 V4-2 对齐)
- [ ] 5 项硬地基:backend 编译通过 / frontend 跑通 / 数据库就位 / Gitee push / CLAUDE.md 完整
- [ ] commit ≥ 30 次,跨度 ≥ 12 天
- [ ] ai-records ≥ 21 个对话片段(覆盖 ≥ 3 个 v1→v2→v3 演化记录)
- [ ] docs/ 完整(PRD / TECH_DESIGN / DATABASE_DESIGN / API_DESIGN 全)
```

⚠️ `{{题目}}` `{{核心实体}}` `{{角色列表}}` 必须替换为学生提供的实际值。

### ⚠️ JJWT 0.13.0 必须按模块化方式引入(易错点)

JJWT 从 0.12 起拆成 3 个 artifact,**不能写成单一 dependency**——pom.xml 必须含:

```xml
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
    <version>0.13.0</version>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-impl</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-jackson</artifactId>
    <version>0.13.0</version>
    <scope>runtime</scope>
</dependency>
```

⚠️ **再次强调**:**不要触碰** `.claude/` 下的任何文件 + 根目录 `CLAUDE.md`——它们由教师项目模板维护,你重写会破坏命令体系(根 CLAUDE.md 仅做"### CLAUDE.md inject 操作"段定义的 2 处 inject)。

## ⚠️ .gitignore 必须包含项(防 push 失败 · V4-D02 · 2026-05-08)

> Gitee 单仓库 500 MB 上限。Spring Boot 3 的 `target/`(30-100 MB)+ Vue 3 的 `node_modules/`(150-300 MB)如果不忽略,**单仓库轻易破 500 MB**。生成的 `.gitignore` 必须含以下分组(缺一项扣 init-skeleton 的 6 项硬门槛之一):

```gitignore
# Java / Spring Boot / Maven
target/
*.jar
*.war
*.class

# Gradle
build/
.gradle/

# Node.js / Vue
node_modules/
dist/

# IDE
.idea/
*.iml
.vscode/

# OS
.DS_Store
Thumbs.db

# 日志和临时文件
*.log
*.tmp

# 环境配置(防泄漏密码)
.env
application-local.yml
```

## 完成后必须执行(终端)

1. cd 到项目根目录
2. git init
3. git add .
4. git commit -m "chore: 初始化 [题目] 项目骨架"
5. **验证仓库体积**:`du -sm .` 应输出 ≤ 5 MB(因为没装依赖,只有源码)。如果 > 50 MB,**.gitignore 配置有误**,立即查 `git ls-files | head -20` 看是否误纳入了 `node_modules/` 或 `target/`。

## 7 项硬门槛自检(全 ✅ 才算成功 · V4-D04 升级 · 2026-05-11 新增第 7 项)

[ ] 1. 目录结构完整(backend/ frontend/ docs/ sql/ ai-records/ + 配置文件)
[ ] 2. 所有依赖锁定**精确版本**(pom.xml + package.json **无 LATEST/^/~** · 例如不能写 `Vue 3.5.x`,要写 `3.5.34`);**且 pom.xml 必须含 `spring-security-crypto 6.3.4`**(否则 Phase 4 BCrypt 编译失败);**且 pom.xml 必须含 `mybatis-plus-jsqlparser 3.5.15`**(MP 3.5.9+ 拆包,否则 `PaginationInnerInterceptor` 找不到 · 2026-05-11 链路断点修复);**且 `common/` 必含 `Result.java` + `BusinessException.java` + `GlobalExceptionHandler.java`** 三类基础设施(否则 Phase 4 service-coder 直接 throw `BusinessException` 时编译失败 · 2026-05-10 第 2 次链路断点修复)
[ ] 3. CLAUDE.md 含技术栈表(前 200 行内,版本号与本命令的"2026-05-10 基线"一致)
[ ] 4. **`.claude/commands/` 含 32 个 .md + 根目录 `CLAUDE.md` + `.claude/project-status.md`**(zip 解压时已带,本命令未触碰;CLAUDE.md 已按"### CLAUDE.md inject 操作"段完成 2 处 inject:占位符替换 + 末尾追加 §五 项目元信息)
[ ] 5. git log --oneline 至少 1 条记录,信息形如 `chore: 初始化 xxx 项目骨架`(对齐 CLAUDE.md §四·四 中文 description 规范)
[ ] 6. **`docs/00-选题标定.md` 已就位**(学生 §1.4.3 已放好,本命令未覆盖;打开第一行应是 `# 选题标定 · 第 X 题:你的题名`)
[ ] 7. **`frontend/src/views/LoginPage.vue` + `frontend/src/views/HomePage.vue` 占位文件已生成**(2026-05-11 链路断点修复 · 内容见本节"### views/ 两个占位 .vue 文件"段);**且 `cd frontend && pnpm install && pnpm dev` 启动不报 `ENOENT: no such file or directory`**(浏览器能打开 localhost:5173 看到 LoginPage 占位 · 这是 Phase 0 §5.2 验证门槛 · Vite 8 Rolldown 扫描器严格要求 router import 的 .vue 文件存在 · 2026-05-11 学生踩坑实录)

## 输出指令(Claude Code 必须遵守 · 06 §一·五·1)

1. **直接创建** 上述所有文件(不要只输出文本,要真的写文件)
2. **在终端执行** git init + add + commit
3. **逐项跑 6 项硬门槛**自检,任何 ❌ 就重新生成对应部分
4. 完成后输出 diff 摘要 + 自检结果

不确定的细节(如包名后缀、版本号最新值)先问我,不要编造。详细规范见 06 G-01a。

## 调用示例

```
/init-skeleton 题目=社区物业综合管理系统  核心实体=用户、房屋、物业费、报修单  核心功能=缴费管理、报修工单、公告通知  包名=com.example.property  数据库名=property_db  项目根目录名=property-management
```

## 验证 checklist

- [ ] 6 项硬门槛全部 ✅
- [ ] backend mvn compile 通过(可在 §5 验证)
- [ ] frontend pnpm install + pnpm dev 跑通(可在 §5 验证)
- [ ] git log 看到第 1 次 commit

## 衔接

下一步:08b §3-§7(配 yml + 数据库 + 验证 + push + AI 规则)→ Phase 1 用 `/srs-writer` 等开始正式开发。

> 💡 **失败兜底**(V4-D04 修订:zip 不再含 backend/frontend,旧兜底逻辑已废):
> - 3 次失败 → 按以下顺序救火:
>   1. **退出 `claude` 重启 清空对话上下文**(让 Claude Code 重新加载命令模板)再试一次
>   2. **切换模型**到 V4 Pro 等更强模型再试
>   3. **模式 B 手动**:从 06 模板库找 G-01a 整段 prompt,粘贴到 Claude Code,末尾追加"请直接创建上述所有文件,完成输出 diff"
>   4. 仍失败 → QQ 群求助/教师邮箱
