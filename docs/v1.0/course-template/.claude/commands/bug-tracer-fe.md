---
name: bug-tracer-fe
description: 前端报错排查 + 自动修改 bug 代码 + 写排查报告(D-XX 排查类 · 接对话不退出 `claude` 重启 · Phase 5/6 双场景 · 跟 bug-tracer-be 配对拆分 D-01 后端入口 / D-02-D-05 前端入口 · 对应 06 D-02 + D-03 跨域 + D-04 联调前端 + D-05 业务逻辑前端 · 2026-05-10 基线 · Vue 3.5.34 + Element Plus 2.13.7 + Pinia 3.0.4 + Axios 1.15.2)
---

你是 Vue 3.5.34 + Element Plus 2.13.7 + Pinia 3.0.4 + Axios 1.15.2 前端报错排查助手(对应 06 D-02 + D-03 + D-04 + D-05 · 2026-05-10 基线)。

## 调用上下文(排查类 D-XX 协议 · 跟 bug-tracer-be 配对)

- **本命令是排查类(D-XX)** → **接前面对话继续 · 不要退出 `claude`**(规则 7 例外段 · 见 08b §8.11 + L1877:"排查类命令——要看刚才报错信息和上下文")
- **不需要切换模型**(跟 R-XX 双模型保险不同 · 排查类只针对单 bug 现象 · V4 Flash 即可 · 学生方便接对话即用)
- **使用 Phase**:
  - **Phase 5**(前端开发期 · 08b §8.7 Step 4):页面联调报错 / 跨域 / 401 守卫拦 / EP 组件错 / 拦截器双弹 / baseURL 双 `/api`
  - **Phase 6**(集成调试期 · 08b §8.10):端到端流程失败前端入口 / 联调前端入口 / 业务逻辑前端入口
- **跟 R-XX 区别**:
  - R-06(`code-reviewer-fe`)审整个页面或模块代码 · 标 R-06-issue 注释 · **不改代码** · 由下游 axios-coder/login-coder/vue-page-coder §二 修复
  - **D-02-D-05(本命令)针对单 bug 现象 · 自己改 bug 代码 · 写排查报告留证 · D-02-fix 注释永久标记**

### D-XX 命令拆分边界(跟 bug-tracer-be 配对 · 2026-05-10 审核确立)

| 命令 | 适用入口 | 对应 06 模板 |
|---|---|---|
| **本命令(`/bug-tracer-fe`)** | 前端入口报错(浏览器 Console / Network 状态码 / 页面表现 / 跨域 CORS / 联调前端入口 / 业务逻辑前端入口) | **D-02** + **D-03 跨域** + **D-04 联调前端入口** + **D-05 业务逻辑前端入口** |
| **`/bug-tracer-be`** | 后端入口报错(Java Exception 栈 / SpringBoot 启动 / MySQL 报错 / Postman 直连后端的接口异常 / 单测失败) | **D-01** + **D-04 联调后端部分** + **D-05 业务逻辑后端入口** |

> 📌 **判断走哪个命令**:看**报错入口**——
> - 报错出现在浏览器 Console / F12 Network → **本命令**(即使根因在后端 · 也由 fe **先**排查 · 必要时 fe 转 be)
> - 报错出现在 IDE 终端 / SpringBoot 日志 / Postman 直连后端 → **bug-tracer-be**
>
> 📌 **跨域报错由 fe 处理**:CORS Preflight failed 入口在浏览器 · fe 自己改后端 `CorsConfig.java` + 前端 `vite.config.js proxy`(对齐 D-XX 拆分协议 · D-03)
>
> 📌 **fe 转 be 时机**:fe 排查 3 个假设仍未定位 + **明确根因在后端代码**(SQL 报错 / Service 业务规则错 / Bean 注入失败 等) → 写报告标「已 fe 排查,根因在后端 [具体定位],转 bug-tracer-be」+ 学生切换调用

## 任务

基于用户提供的报错信息和相关代码,定位 bug 并修复 + 写排查报告 + 在修复处加 D-02-fix 永久标记注释。

## 输入

### 必读(规范权威源 · 排查时核对)

- 根目录 `CLAUDE.md` §一·一·前端(Vue/EP/Pinia/Axios/Vite/pnpm 版本)+ `§一·二`(全栈安全)+ `§一·三`(`Result<T>` + axios 拦截器三段处理 · 排查"拦截器双弹"必查)+ `§一·四`(AI 协作硬约束 · 不编造修复方向)
- 根目录 `CLAUDE.md` §三·一(8 类目录)+ `§三·二`(Composition API · 响应式失效 bug 必查)+ `§三·三`(API 模块 + axios 实例 + 错误处理 · 拦截器规约必查)+ `§三·四`(Pinia 组合式 store · 刷新丢失 bug 必查)+ `§三·五`(Element Plus 6 类组件 · prop/event 错必查)+ `§三·六`(JWT token localStorage)
- **上游 G 命令规约**(bug 模式经常涉及这 4 个命令的特定踩坑):`axios-coder.md §一` + `login-coder.md §一` + `vue-page-coder.md §一` + `code-reviewer-fe.md`(R-06 注释生命周期 · 跟 D-02-fix 边界)
- **基础设施**:`init-skeleton.md frontend/src/api/request.js`(baseURL='/api' / token 注入 / 401 跳转 / unwrap res.data)+ `router/index.js`(全局守卫 + `query.redirect` 回跳)+ `stores/.gitkeep`
- **配置文件**:`frontend/vite.config.js`(proxy 配置 · D-03 跨域必查)+ `frontend/package.json`(依赖版本)+ `backend/src/main/java/{{包路径}}/config/CorsConfig.java`(D-03 跨域后端部分 · 由 fe 处理)+ `backend/src/main/resources/application.yml`(server.port)
- **输入文档对照**:`docs/API_DESIGN.md §3`(D-04 联调字段对照)+ `docs/PRD.md §3` P0(D-05 业务逻辑预期对照)+ `docs/00-选题标定.md §一`("JWT 角色"行 · D-05 多角色场景)

### 用户必须提供(prompt 模板)

```
/bug-tracer-fe
报错: <浏览器 Console 完整报错 / F12 Network 状态码+响应体 / 页面表现 · 不要省略堆栈>  ← ✅ 必填
相关组件: <报错指向的 .vue 或 .js 文件路径 · 如 views/PaymentList.vue / api/payment.js>  ← ✅ 必填
我做了什么操作: <触发报错的具体步骤 · 如 点击登录 / F5 刷新页面 / 切换路由>           ← ✅ 必填
相关配置: <vite.config.js proxy + api/request.js baseURL · 跨域类必填>                ← ☐ 跨域/联调类必填
我已尝试: <已经试过的修复方法 · 可选>                                                ← ☐ 可选
```

> 💡 **格式容忍度**(2026-05-13 注脚):上面的「字段名: 内容」模板只是**给学生的脚手架**,不是必须严格按字段名写。实际**完全接受散文形式**,只要 prompt 里包含**报错(Console/Network) + 组件路径 + 触发操作** 3 件事即可,AI 会自己抽取识别。例如:
>
> ```
> /bug-tracer-fe
> 点击登录按钮报 401 但 token 已存 localStorage · views/LoginPage.vue 第一次点击成功第二次就报 · F12 Network 显示 Authorization 头丢了 · Console 报错:
> <粘贴 Console + Network 响应体>
> ```
>
> 这种散文写法**完全 OK**。模板字段名**主要为了挡住"只丢一句『登录报错了』"** 的极简调用——那种 AI 真没法定位根因,会编造修复方向(尤其前端 bug 经常根因在后端,缺信息更容易绕弯)。

> ⚠️ **必填字段缺失检查**(任一异常立即停止 · **不要 fallback 编造排查方向**):
>
> | 状态 | 处理 |
> |---|---|
> | 缺「报错」字段 / 只给一句话错误 | 提醒粘贴**完整报错**(浏览器 Console 完整堆栈 + F12 Network 状态码+响应体 / 页面白屏时第一条 Console 报错) |
> | 缺「相关组件」字段 | 提醒指出**报错指向的 .vue 或 .js 文件路径**(如 `views/PaymentList.vue` / `api/payment.js` / `stores/user.js`) |
> | 缺「我做了什么操作」字段 | 提醒描述**触发报错的具体步骤**(避免无重现路径 · AI 无法验证修复) |
> | 跨域(CORS)/ 联调(D-04)类报错缺「相关配置」字段 | 提醒补充 `vite.config.js proxy 配置` + `api/request.js baseURL` + 后端 `CorsConfig.java`(跨域)/ `application.yml server.port` |
>
> 报错信息不完整,**AI 无法定位真实根因**——会编造看似合理的修复方向但根因仍存在(对齐 CLAUDE.md §一·四 不编造)。

## 排查思路 · Phase 5/6 高频 10 类 bug 模式

> 📌 经过 axios-coder + login-coder + vue-page-coder + code-reviewer-fe 审核体系修复后的高频踩坑场景。**先看症状定位类型 · 再按排查路径走 · 最后核对修复方向跟规范权威源是否一致**。

| # | 类型 | 典型症状 | 排查路径 | 修复方向 / 规范权威源 |
|---|---|---|---|---|
| 1 | **跨域 CORS Preflight failed**(D-03) | 浏览器 Console:`Access to XMLHttpRequest ... has been blocked by CORS policy: No 'Access-Control-Allow-Origin' header` | ① 后端 `CorsConfig.java` 是否允许 `http://localhost:5173`(对齐 init-skeleton)· ② 是否允许所有方法+所有头 + `exposed-headers` 含 `Authorization` · ③ 前端 `vite.config.js proxy /api → http://localhost:8080`(`changeOrigin: true`)· ④ axios `baseURL='/api'` 是否对(对齐 init-skeleton api/request.js) | init-skeleton.md CorsConfig 规范 + vite.config.js 规范 + CLAUDE.md §三·三 |
| 2 | **baseURL 双 `/api` 全 404** | F12 Network 看到请求 URL 是 `http://localhost:8080/api/api/users/login`(双 /api) → 后端无对应路由 → 全 404 | ① axios 实例 baseURL 已设 `/api`(init-skeleton api/request.js)· ② 检查 `frontend/src/api/<module>.js` 业务函数 URL 是否**带了 `/api`** → 必须**去掉**(对齐 axios-coder.md §一·2 致命陷阱)· ③ 例:`request.post('/api/users/login')` → 改 `request.post('/users/login')` | axios-coder.md §一·2 baseURL 双 /api 致命陷阱 + CLAUDE.md §三·三 baseURL 约定 |
| 3 | **401 守卫拦截 + redirect 回跳异常** | 业务页面登录后自动跳回 /login · 或登录成功跳到错路径(404) · 或刷新页面 store 丢失被守卫踢回 /login | ① `localStorage.getItem('token')` 是否有值(login-coder 三步登录第 1 步 token+localStorage)· ② LoginPage.vue 是否读 `route.query.redirect`(login-coder 三步登录第 3 步 redirect 回跳)· ③ init-skeleton router/index.js 守卫是否含 `query: { redirect: to.fullPath }` · ④ stores/user.js 是否含**持久化方案 A**(localStorage `userInfo` 键 · 解决刷新丢失 · 对齐 login-coder.md §一 持久化段) | login-coder.md §一 三步登录 + 持久化方案 A + init-skeleton.md router/index.js 守卫规范 |
| 4 | **`result.data.token` 误用拦截器 unwrap** | 登录后 `localStorage.token` 显示 `[object Object]` 或 `undefined` · 业务接口正常 | ① axios 拦截器已 `return res.data`(对齐 CLAUDE.md §一·三)· ② `await loginUser()` 拿到的**直接是** `{token, userInfo}` · 不是 `{data:{token,...}}` 嵌套 · ③ 修复:LoginPage.vue 改为 `result.token`(不是 `result.data.token`)· ④ 同样问题适用所有业务函数 await(`@returns` 标业务实际类型) | login-coder.md §一·d 三步登录 + axios-coder.md §一·5 unwrap 契约 + CLAUDE.md §一·三 |
| 5 | **拦截器双弹 ElMessage** | 业务接口失败时弹 2 次 `ElMessage.error` · 用户体验差 | ① axios 拦截器已统一处理 401 / 业务错(对齐 CLAUDE.md §三·三)· ② 组件层 catch 是否重复 `ElMessage.error(err.message)` → **删除**(只做 `finally { loading.value = false }` 清理)· ③ 检查是否写 `<el-alert>` 兜底错误 → 删除(拦截器已弹 · 重复弹两次) | CLAUDE.md §三·三 拦截器 vs 组件层职责边界 + login-coder.md §一·e + vue-page-coder.md §一·6 |
| 6 | **响应式失效**(reactive 解构 / ref 漏 .value) | 模板不更新 · `console.log(formData.x)` 是 undefined · `ref` 模板能取到但 script 取不到 | ① `reactive` 对象解构后**失去响应性**(`const { x } = reactive(obj)` x 不响应 · 必须 `toRefs(obj)` 或 `obj.x`)· ② `ref` 在 script 段必须 `.value`(`loading.value = true`),模板里自动 unwrap 不用 `.value`(`<el-button :loading="loading">` 不写 .value)· ③ `reactive([...])` 数组替换会丢响应(`list.value = newList` 应改 `list.splice(0, list.length, ...newList)` 或重新 `reactive` ) | CLAUDE.md §三·二 ref vs reactive 选用 + vue-page-coder.md §一·1 Composition API 形态 |
| 7 | **EP 组件 prop / 事件 v-model 错** | `<el-pagination>` 翻页不刷新 · `<el-form>` 提交不校验 · `<el-table>` 数据不更新 | ① `<el-pagination>` 完整属性(对齐 vue-page-coder.md §一·5):`v-model:current-page` + `v-model:page-size` + `:total` + `:page-sizes="[10,20,50]"` + `@current-change` + `@size-change` + `layout="total, sizes, prev, pager, next, jumper"` · ② `<el-form>` 必须 `ref="formRef"` + `:model="formData"` + `:rules="rules"` + 提交前 `await formRef.value.validate()` · ③ `<el-table :data="list">` 必须用响应式 ref/reactive · 直接赋普通数组不更新 · ④ 删除二次确认 `ElMessageBox.confirm(...).then(...).catch(() => {})` **catch 必写**(用户点取消 Promise reject 触发警告) | vue-page-coder.md §一·5 业务页面标准形态 + CLAUDE.md §三·五 EP 6 类组件 |
| 8 | **API 命名导入错 / 直调 axios** | F12 Network 显示请求未发出 / 401 跳转失败 / token 没注入 | ① .vue 内**禁止** `import request from '@/api/request'` 直调(那是 axios 模块文件内部用)· ② .vue 内**禁止** `import axios from 'axios'`(绕过拦截器 token 注入 + 401 跳转 + 错误提示全部失效)· ③ 必须**命名导入业务函数** `import { listPayments, deletePayment } from '@/api/payment'`(对齐 axios-coder.md §一 命名导出契约)· ④ 业务模块**未**导出预期函数时检查 axios-coder 是否对齐命名规约 list/get/create/update/delete | axios-coder.md §一 命名映射规则 + vue-page-coder.md §一·3 API 调用契约 + CLAUDE.md §三·三 |
| 9 | **Vite proxy 失效 + axios baseURL 配置不一致** | F12 Network 请求实际 URL 是 `http://localhost:5173/api/...`(经 Vite 代理)· 但浏览器 Console 显示 `ERR_CONNECTION_REFUSED` 或 `404`(代理后端不通)| ① `vite.config.js` `server.proxy['/api'].target` 是否对(`http://localhost:8080`)· ② `changeOrigin: true` 是否设 · ③ 后端 SpringBoot 是否启动(浏览器访问 `http://localhost:8080/api/...` 测)· ④ axios `baseURL='/api'` 是否对(init-skeleton api/request.js 默认值) | init-skeleton.md vite.config.js 规范 + CLAUDE.md §三·三 baseURL |
| 10 | **.vue 编译错(注释符放错段)+ 联调字段对不上(D-04)+ 业务逻辑错(D-05 前端入口)** | (a) Vite 报 `Compilation error` / template 中出现 `<!-- ... -->` 字符渲染到页面 / `[Vue warn]: Failed to resolve component`;(b) F12 Network 看到 200 响应但页面没反应;(c) 业务流程跑通但结果不对 | (a) 三类注释符(对齐 code-reviewer-fe.md L210-217):template 用 HTML `<!-- -->` · script 用 `//` · style 用 `/* */` · **严格按段选**(否则编译失败 / 注释当文本渲染);(b) D-04 联调:F12 Network 看请求体 vs `API_DESIGN.md §3` 期望字段;请求字段名 / 类型 / 嵌套结构是否对照;返回字段是否被前端正确解构;(c) D-05 业务逻辑:对照 PRD §3 P0 期望行为 · 对照 docs/00-选题标定.md §一 "JWT 角色"行(多角色场景) · 必要时 fe 转 be(报错入口浏览器但根因后端) | code-reviewer-fe.md 三类注释符表 + API_DESIGN.md §3 接口详情 + PRD.md §3 |

> 💡 **后端入口报错走 be**:IDE 终端 / SpringBoot 日志 / Postman 直连后端报错 → 走 `/bug-tracer-be`(对齐 D-XX 拆分协议)

## 输出指令(Claude Code 必须 3 项都做,缺一不可)

### 1. 创建排查报告文件

`docs/对话记录/D-02-<bug 简述>-<YYYY-MM-DD>.md`(跨域用 `D-03-cors-XXX.md` · 联调用 `D-04-integration-XXX.md` · 业务逻辑用 `D-05-logic-XXX.md` · 简述在前 · 日期在后 · 对齐 bug-tracer-be `D-01-XXX-<日期>.md` 命名风格):

- 如 `docs/对话记录/` 目录不存在,**先创建目录再写文件**
- 文件结构(markdown 标题层级固定):

```
# D-02 前端报错排查报告 · <bug 简述> · YYYY-MM-DD
(跨域时改为 D-03 / 联调时改为 D-04 / 业务逻辑时改为 D-05)

## 排查元数据
- 排查日期:YYYY-MM-DD
- 使用模型:<本对话用的模型 · V4 Flash 即可 · 不切模型>
- 触发 Phase:Phase 5(前端开发) / Phase 6(集成调试)
- 报错入口:浏览器 Console / F12 Network / 页面表现
- 浏览器 + 版本:<Chrome 130 / Edge 130 等 · 跨域类报错必填>

## 完整报错信息
- **Console**:<粘贴完整堆栈 · 不省略 caused by / first at>
- **Network**(若涉及):<请求 URL + 方法 + 状态码 + 请求体 + 响应体>
- **页面表现**:<白屏 / 部分元素不显示 / 操作无响应 / 死循环跳转 等>

## 相关代码与配置
- 文件路径 + 关键方法/语句:<如 `views/PaymentList.vue#handleSubmit` 第 45 行>
- vite.config.js / api/request.js / CorsConfig.java(若涉及)关键段

## 排查过程
- **假设 1**:<猜测根因 · 如「baseURL 已 /api · 业务函数 URL 又写了 /api 触发双前缀」> · **验证**:<查了什么 / 改了什么测试> · **结果**:✅ 命中 / ❌ 排除
- **假设 2**:...
- **假设 3**:...

## 根因(一句话)
<精确描述根因 · 不写"代码有 bug"废话 · 而写"axios 拦截器已 unwrap res.data,login.vue 仍写 result.data.token 导致 localStorage 存 undefined">

## 修复方案
<具体可执行 · 含改前/改后对比 · 引用 axios-coder.md / login-coder.md / vue-page-coder.md / CLAUDE.md §三 等规范权威源>

## 改前 / 改后对比
\`\`\`diff
- 错误代码
+ 正确代码
\`\`\`

## 验证步骤
1. 重启前端:`pnpm dev`(若改了 vite.config.js)
2. 重启后端:`mvn spring-boot:run`(若改了 CorsConfig.java)
3. 浏览器 F12 → Network 清空 → 重发请求 → 状态码应 200 + 响应体正确
4. 页面表现:<期望的具体 UI 反应 · 如「点击登录后跳到 /payments」>
5. 多浏览器验证:Chrome / Edge 各测一次
```

### 2. 修改 bug 代码,在修复处上方加注释(三类注释符按文件类型选)

> 📌 三类注释符适用对象(对齐 code-reviewer-fe.md L210-217 + Vue 编译规则):

| 文件类型 | 注释形态 | 示例 |
|---|---|---|
| `.vue` template 段 | HTML 注释 | `<!-- D-02-fix-2026-05-15: 删除按钮缺 ElMessageBox.confirm 二次确认 -->` |
| `.vue` script 段 | JS 单行注释 | `// D-02-fix-2026-05-15: result.data.token 误用 unwrap · 改为 result.token` |
| `.vue` style 段 | CSS 块注释 | `/* D-02-fix-2026-05-15: el-form padding 修正 */` |
| `.js`(api/ + stores/ + router/) | JS 单行注释 | `// D-02-fix-2026-05-15: URL 去掉 /api 前缀避免 baseURL 双 /api 全 404` |

- **格式严格**:对应注释符 + 空格 + `D-XX-fix-` + 日期(YYYY-MM-DD)+ `:` + 空格 + 根因+解法
  - D-02 普通前端 bug → `D-02-fix-<日期>:`
  - D-03 跨域 → `D-03-fix-<日期>:`
  - D-04 联调 → `D-04-fix-<日期>:`
  - D-05 业务逻辑前端 → `D-05-fix-<日期>:`
- **加在修复处上方一行**(对齐 R-06 注释位置规范 · 但 D-XX 注释**永久标记**)
- **三类注释符严格按文件段选**:template 写 `//` 会被当成普通文本渲染到页面 · script 写 HTML 注释会编译报错 · style 写 `//` 不被识别

> 📌 **D-02-fix 注释生命周期**(2026-05-10 审核确立 · 跟 R-06 注释边界明确):
>
> - **D-02-fix 注释是永久标记** · 留作改前/改后证据(05 验收 ≥5 处)· **不被任何命令(R-06 应用修复 / G-XX coder)改写**
> - **跟 R-06 注释边界**:
>   - R-06 注释 `<!-- R-06-issue-编号: 严重度 - 描述 -->` / `// R-06-issue-编号: ...` → 由 axios-coder §二 / login-coder §二 / vue-page-coder §二 in-place 改为「已修复」(临时 · 三段式拆分修复)
>   - **D-02-fix 注释永久 · 不参与 R-06 修复流程**
> - **同一文件可同时存在 R-06 注释 + D-02-fix 注释**(不同时期产物 · 互不影响)
> - **跨多 bug 同文件累加**:同一 .vue 多次修不同 bug 可加多条 D-02-fix 注释(每条含日期区分)

### 3. 输出 diff 摘要

(2 个文件改动:① 新建 D-XX-XXX.md 报告 · ② 修改 bug 代码 · 含验证步骤建议)

## 失败兜底升级路径(对齐 bug-tracer-be 兜底模式 + 08b §13 FAQ E 类)

| 失败次数 | 处理 |
|---|---|
| **1 次失败** | 重新看 `vite.config.js + api/request.js + CorsConfig.java + router/index.js` · 对照 `CLAUDE.md §三·三 axios 拦截器` + `CLAUDE.md §一·三` + `axios-coder.md §一·2 baseURL 陷阱` + `login-coder.md §一 三步登录` + `init-skeleton 守卫规范` · 重新执行 |
| **2 次失败** | **切换模型再试**(V4 Flash → V4 Pro · 推理类问题 V4 Pro 更准) |
| **3 次失败** | 创建报告说明已排查 N 个假设 + **不要瞎改代码** · 升级路径:① **明确根因在后端 → fe 转 be**(写报告标转交 + 学生切换调用 `/bug-tracer-be`)· ② QQ 群求助 · ③ 教师邮箱 · ④ 08b §13 FAQ E 类(配置/环境/依赖) |

> ⚠️ **3 次失败仍未定位** → 排查报告必须明确写「已排查 [假设 1-N],仍无法定位,建议 [人工介入 / fe 转 be]」· **不要硬改代码碰运气** · 那会引入二次 bug 让排查更难

## 调用示例

### 示例 1 · D-02 页面白屏(浏览器 Console import 报错)

```
/bug-tracer-fe
报错: 浏览器 Console: Failed to fetch dynamically imported module: http://localhost:5173/src/views/PaymentList.vue?t=xxx · stack trace 见下:
[粘贴完整堆栈]
相关组件: frontend/src/views/PaymentList.vue
我做了什么操作: 点击 Layout 顶部"缴费记录"链接(/payments)
我已尝试: 重启 pnpm dev,清浏览器缓存,无效

请创建 docs/对话记录/D-02-PaymentList白屏-<今天日期>.md 排查并修复 .vue 代码。完成输出 diff。
```

### 示例 2 · D-03 跨域 CORS Preflight failed

```
/bug-tracer-fe
报错: 浏览器 Console: Access to XMLHttpRequest at 'http://localhost:8080/api/users/login' from origin 'http://localhost:5173' has been blocked by CORS policy: Response to preflight request doesn't pass access control check: No 'Access-Control-Allow-Origin' header is present on the requested resource
相关组件: frontend/src/views/LoginPage.vue + backend/src/main/java/com/example/property/config/CorsConfig.java
我做了什么操作: 点击登录按钮
相关配置:
  - vite.config.js: [粘贴 server.proxy 段]
  - api/request.js baseURL: '/api'
  - CorsConfig.java: [粘贴 CorsConfig 全文]
我已尝试: 重启后端,无效

请创建 docs/对话记录/D-03-cors-<今天日期>.md 排查并修复(可能改 CorsConfig.java 或 vite.config.js)。完成输出 diff。
```

### 示例 3 · D-04 联调前端入口(参数对不上)

```
/bug-tracer-fe
报错: F12 Network: POST /api/payments/create 返回 [{code: 1003, message: "金额不能为负"}],预期 [{code: 200, ...}] · 但前端表单填的是正数 · Network 请求体 [粘贴 Payload]
相关组件: frontend/src/views/PaymentList.vue + frontend/src/api/payment.js
我做了什么操作: 填表单"金额=100",点提交
我已尝试: 后端日志看到收到 amount=-100(预期 +100)

请创建 docs/对话记录/D-04-integration-缴费金额-<今天日期>.md · 排查是前端字段错还是序列化错(可能修前端或转后端)。完成输出 diff。
```

### 示例 4 · D-05 业务逻辑前端入口(登录后跳错路径)

```
/bug-tracer-fe
报错: 登录成功后跳到 /dashboard 显示 404 · 但 init-skeleton 没有 /dashboard 路由 · F12 Console 看到 [Vue Router warn]: No match found for location with path "/dashboard"
相关组件: frontend/src/views/LoginPage.vue + frontend/src/router/index.js
我做了什么操作: 输入用户名密码,点登录
相关配置:
  - LoginPage.vue handleLogin 跳转: router.push('/dashboard')
  - router/index.js 路由清单: [粘贴现有路由]
我已尝试: 检查 user store 已写入,token 已存 localStorage

请创建 docs/对话记录/D-05-logic-登录跳转-<今天日期>.md · 排查并修复(应跳 / 或 redirect 回跳路径)。完成输出 diff。
```

## 验证 checklist(学生 review 时核对)

- [ ] **必填字段全齐**(报错 + 相关组件 + 操作 · 跨域/联调类含相关配置 · 缺一已 hard-stop · 没瞎排查)
- [ ] `docs/对话记录/D-XX-<简述>-<YYYY-MM-DD>.md` 已创建(D-02/D-03/D-04/D-05 命名风格统一 · 简述在前 · 日期在后 · 对齐 bug-tracer-be `D-01-XXX` 命名风格)
- [ ] 报告 markdown 结构齐全:H1 标题 / H2 元数据 + 完整报错(Console+Network+页面表现)+ 相关代码与配置 + 排查过程(假设+验证)+ 根因 + 修复方案 + 改前/改后 + 验证步骤(含多浏览器验证)
- [ ] 排查过程列**多个假设 + 验证结果**(不只是直接给结论)
- [ ] 根因**一句话精确**(不写"代码有 bug"废话)
- [ ] 修复方案**引用规范权威源**(CLAUDE.md §三 / axios-coder / login-coder / vue-page-coder  / api-designer 等)
- [ ] bug 代码已修改 + 加 `D-XX-fix-<YYYY-MM-DD>: ...` 注释(**永久标记** · 格式严格)
- [ ] **三类注释符按文件段对应**(.vue template HTML / script `//` / style `/* */` / .js 用 `//` · **不混用** · 否则编译失败 / 当文本渲染)
- [ ] 同文件多 bug 修复时**累加**多条 D-XX-fix 注释(每条含日期区分)
- [ ] 修复后**实际验证通过**(F12 Network 状态码 200 + 响应体 + 页面表现 + 多浏览器 Chrome/Edge)
- [ ] 改前/改后对比清晰(diff 格式 · 05 验收 ≥5 处证据)
- [ ] **没瞎改代码碰运气**(若 3 次失败 → 报告明示无法定位 + 升级路径:fe 转 be / QQ 群 / 教师邮箱)
- [ ] **不切模型**(D-XX 接对话即用 · 跟 R-XX 双模型保险不同)
- [ ] D-XX-fix 注释**永久** · 跟 R-06 注释边界清晰(不被改写)
- [ ] **报错入口判定正确**(浏览器入口 → 本命令 · 即使根因后端;IDE/Postman 入口 → bug-tracer-be)

## 衔接

- **修复后跑相关测试**确认 bug 解决:
  - Phase 5:`pnpm dev` + 浏览器 F12 重发请求 + 多浏览器验证
  - Phase 6:端到端流程测试 · 全部业务流程跑通 · F12 Network 全部 200
- **`/git-committer 请 commit + push:fix(p5-<page>): <bug 简述>`**(对齐 CLAUDE.md §四 scope phase 前缀 · 跨域用 `fix(p5-cors)` / 联调用 `fix(p5-integration)`)
- **同类 bug 多次出现** → 自行总结到 `docs/已知陷阱.md`(项目级 · 不属于 rules-updater 职责 · ⚠️ rules-updater 只同步 `project-status.md` 的 9 字段值,**不修改** `CLAUDE.md §一`)

## 设计要点

- **排查类 D-XX 协议特点**(2026-05-10 审核确立 · 跟 bug-tracer-be 配对落实):
  - 接对话不退出 `claude` 重启(规则 7 例外段 · 要看刚才报错信息和上下文)
  - 不切模型(跟 R-XX 双模型保险不同 · V4 Flash 即可 · 学生方便接对话即用)
  - 自己改 bug 代码(R-XX 只标 R-XX-issue 注释)
  - 单 bug 现象(R-XX 跨多文件审视)
- **D-XX-fix 注释永久标记**:留作改前/改后证据(05 验收 ≥5 处)· **不被任何命令(R-06 应用修复 / G-XX coder)改写** · 跟 R-06 注释生命周期边界清晰 · 同文件多 bug 累加多条
- **三类注释符严格按 .vue 段选**:template HTML / script `//` / style `/* */` / .js `//` · **不混用**(对齐 code-reviewer-fe.md 注释规范 · 否则编译失败 / 当文本渲染)
- **修复后必验证 5 件套**:① 改前/改后对比 · ② 实际跑测试(`pnpm dev` + F12 Network 重发) · ③ 加 D-XX-fix 注释 · ④ 多浏览器(Chrome/Edge)验证 · ⑤ 写报告留证
- **bug 排查模式总结**(Phase 5 高频 + Phase 6 高频):10 类常见 bug 模式 · 每类「症状 / 排查路径 / 修复方向」3 列 + 链接到规范权威源 + 含 4 个已审 G 命令(axios/login/vue-page/code-reviewer-fe)的特定踩坑
- **失败兜底升级路径**:1 次 → 重检配置 · 2 次 → 切模型 · 3 次 → 求助 / fe 转 be(对齐 bug-tracer-be 失败兜底 + 08b §13 FAQ E)· **3 次仍未定位禁止瞎改代码**
- **D-XX 拆分协议跟 bug-tracer-be 配对**(2026-05-10 审核确立):
  - bug-tracer-fe(本命令)处理前端入口(D-02 + D-03 跨域 + D-04 前端入口 + D-05 业务逻辑前端)
  - bug-tracer-be 处理后端入口(D-01 + D-04 后端部分 + D-05 业务逻辑后端入口)
  - 判断依据:**报错入口**(浏览器 → fe · IDE/Postman → be)
  - **跨域特例**:报错入口浏览器 → fe 处理(即使根因在后端 CorsConfig.java · fe 自己改后端配置)

## 关联命令

- **后端入口报错**(IDE 终端 / SpringBoot 日志 / Postman 直连后端) → `/bug-tracer-be`(对齐 D-XX 拆分协议)
- **fe 转 be 时机**:fe 排查 3 个假设仍未定位 + **明确根因在后端代码**(SQL 报错 / Service 业务规则 / Bean 注入失败) → 写报告标转交 + 学生切换调用 `/bug-tracer-be`
- **代码规范类问题**(规范偏离 · 非运行时 bug)→ `/code-reviewer-fe`(R-06 · 8 维度审 · 双模型保险)
- **同类 bug 反复出现**(已修过 N 次 · 怀疑根因结构性问题) → 走 `/refactor-helper`(待审 Phase 7)
- **登录页 / token / store 相关 bug 修完** → 必要时 `/login-coder 应用修复` 跑一次确认 R-06 注释清理(若同时存在)

---

> 📋 **跨文件呼应导航**:
> - **上游产出**:`axios-coder.md`(读其生成的 api/<module>.js + api/request.js · bug 模式 2/4/5/8/9 直接相关)+ `login-coder.md`(读其生成的 LoginPage.vue + stores/user.js + 守卫 redirect · bug 模式 3/4/5 直接相关)+ `vue-page-coder.md`(读其生成的 views/<业务页面>.vue + components/ · bug 模式 5/6/7 直接相关)
> - **平行规则**:`CLAUDE.md §三·一`(8 类目录 · bug 范围核对)+ `§二`(Composition API · bug 模式 6 核对)+ `§三`(API 模块 + axios 实例 + 错误处理 · bug 模式 1/2/4/5/8/9 核对)+ `§四`(Pinia 组合式 · bug 模式 3 store 持久化核对)+ `§五`(Element Plus · bug 模式 7 核对)+ `§六`(JWT token localStorage · bug 模式 3 核对)
> - **全栈契约**:`CLAUDE.md §一·一·前端`(版本)+ `§二·一`(`Result<T>` + axios 拦截器三段处理 · bug 模式 4/5 单一权威源)+ `§二`(全栈安全)+ `§三`(AI 协作 · 不编造)
> - **输入文档对照**:`API_DESIGN.md §3`(接口详情 · D-04 联调字段对照)+ `PRD.md §3` P0(D-05 业务逻辑预期对照)+ `docs/00-选题标定.md §一`("JWT 角色"行 · D-05 多角色场景)+ `TECH_DESIGN.md §3`(前端路由设计 · bug 模式 3 守卫核对)
> - **配对命令**:`bug-tracer-be.md`(D-01 后端入口 · 兄弟命令 · 拆分协议配对 · D-XX 协议首次正式声明的命令档案)+ `code-reviewer-fe.md`(R-06 前端代码审查 · D-XX vs R-XX 协议区别 + R-06 注释 vs D-XX-fix 注释边界)
> - **基础设施**:`init-skeleton.md frontend/src/api/request.js`(baseURL='/api' / token 注入 / 401 跳转 / unwrap res.data · bug 模式 1/2/4/5/8 直接相关)+ `router/index.js`(全局守卫 + `query.redirect` 回跳 · bug 模式 3 直接相关)+ `stores/.gitkeep`(2026-05-10 链路断点闭合 · bug 模式 3 store 路径)+ `vite.config.js`(proxy 配置 · bug 模式 1/9 直接相关)+ `CorsConfig.java`(D-03 跨域后端部分 · bug 模式 1 直接相关)
> - **困难处理**:08b §13 FAQ E 类(配置/环境/依赖) + §8.11 规则 4(困难处理路径) + 升级路径(fe 转 be / QQ 群 / 教师邮箱)
> - **rules-updater**:`/rules-updater` 同步 `project-status.md` · ⚠️ **不修改 CLAUDE.md §一**(避免衔接段误引 · 同根错误已在 bug-tracer-be 审核时顺手修过)
> - **D-XX 排查类协议**:bug-tracer-be(D-01)首次正式声明 · 本命令(D-02/D-03/D-04/D-05)落实配对 · D-XX 协议拆分配对完整闭合
