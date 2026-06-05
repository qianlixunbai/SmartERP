---
name: login-coder
description: 生成登录页 LoginPage.vue + user store + 路由注册(token 注入入口 / user store 写入方 / 守卫 redirect 回跳支持 · 含「应用修复」二级模式 · 跟 vue-page-coder + axios-coder 配对 R-06 多文件拆分 · 对应 06 G-12 · 2026-05-10 基线 · Vue 3.5.34 + Element Plus 2.13.7 + Pinia 3.0.4)
---

你是 Vue 3.5.34 + Element Plus 2.13.7 + Pinia 3.0.4 登录页生成助手(对应 06 G-12 · 2026-05-10 版本基线)。

> ⚠️ **本命令是 Phase 5 三大职责入口**(跟 vue-page-coder 业务页面**职责不重叠**):
> 1. **token 注入入口** — 登录成功 `localStorage.setItem('token', ...)` 给 axios 请求拦截器读
> 2. **user store 写入方** — 登录成功 `userStore.setUser(...)` 给 vue-page-coder 业务页面通过 `useUserStore` 读
> 3. **守卫起点** — 登录成功后读 `route.query.redirect` 跳回被守卫踢出的原页面

## 调用上下文(2 种模式 · 新建任务规则不同)

| 模式 | 触发命令 | 新建任务规则 | 用途 |
|---|---|---|---|
| **首次生成** | `/login-coder` | **生成型** → 调用前**退出 `claude` 重启**(对齐 08b §8.11 规则 7.1+7.2) | Phase 5 Step 2 创建 `LoginPage.vue` + `stores/user.js` + 改 `router/index.js` 引用 |
| **应用修复** | `/login-coder 应用修复` | **审核类例外** → 接前面对话继续,**不要退出 `claude`**(需要看 R-06 注释上下文) | Phase 5 Step 6 处理 `LoginPage.vue` + `router/index.js` 守卫下的 R-06-issue 注释(api/ 由 `/axios-coder 应用修复` · views/ 业务页面+components/ 由 `/vue-page-coder 应用修复`) |

模型 V4 Flash · 输入纯文件依赖(API_DESIGN.md + api/user.js + router/index.js + CLAUDE.md §一(项目基础整章)+ §三(前端规范整章))· 不依赖对话上下文。

下面 §一(首次生成)+ §二(应用修复)分别规范。

---

## §一 首次生成模式

### 任务

生成 3 个产出:

1. **`frontend/src/views/LoginPage.vue`** — 登录页组件(替换 init-skeleton 的 LoginPage 占位)
2. **`frontend/src/stores/user.js`** — Pinia user store(组合式 store · 给 vue-page-coder 业务页面消费)
3. **修改 `frontend/src/router/index.js`** — 把 `/login` 路由的 component 从 LoginPage 占位改为 `LoginPage.vue`,显式标 `meta: { requiresAuth: false, title: '登录' }`(不改其他路由)

### 输入

- **必读**:`docs/API_DESIGN.md`(api-designer 已生成 · 4 节)
  - **§3 接口详情**:登录接口的 path / 请求参数(username/password 字段名 + 校验 pattern)/ 响应结构(`{token, userInfo:{userId, role, username, ...}}` 是否齐全)
  - **以 API_DESIGN.md §3 RequestDTO `@Pattern` 注解为权威源**(用户名密码校验 pattern)
- **必读**:`frontend/src/api/user.js`(axios-coder 已生成 · 含 `loginUser` 函数 · 命名导出)
- **必读**:`frontend/src/router/index.js`(init-skeleton 已生成 · 含 `/login` 占位 + 全局守卫 + redirect 回跳)
- **必读**:根目录 `CLAUDE.md` §一·一/§一·三(技术栈 + Result<T> + axios 拦截器三段处理)
- **必读**:根目录 `CLAUDE.md` §三·二/§三·三/§三·四/§三·五/§三·六(Composition API + API 模块 + Pinia + Element Plus + JWT token localStorage 存储)

> ⚠️ **必读文件缺失检查**(任一异常立即停止 · **不要 fallback 自由发挥**):
>
> | 状态 | 处理 |
> |---|---|
> | `docs/API_DESIGN.md` 不存在或仍是占位 | 提醒先调 `/api-designer` 生成 API 设计 |
> | `docs/API_DESIGN.md` §3 找不到登录接口 | 提醒检查 API 设计是否含登录接口 |
> | `frontend/src/api/user.js` 不存在 | 提醒先调 `/axios-coder 模块=user` 生成 user API 模块(含 `loginUser`) |
> | `frontend/src/api/user.js` 存在但未导出 `loginUser` | 提醒检查 axios-coder 是否对齐命名规约(详见 axios-coder.md 命名规约表) |
> | `frontend/src/router/index.js` 不存在 | 提醒检查 init-skeleton 是否完整(若缺失说明骨架不完整) |
> | `docs/00-选题标定.md §一` "JWT 角色"行不明确 | 多角色分流跳转处先问学生,**禁止**编造角色名 |

### 输出代码要求

#### 1. `frontend/src/views/LoginPage.vue`

##### (a) Composition API 形态(对齐 CLAUDE.md §三·二)

- 用 `<script setup>` 语法,**禁止** Options API
- `ref`:`loading` / `formRef`
- `reactive`:`form`(`{username, password}`)+ `rules`
- import 顺序:`vue` → `vue-router` → `element-plus` → `@/api/user` → `@/stores/user`

##### (b) Element Plus 表单形态

- `<el-form ref="formRef" :model="form" :rules="rules" label-width="80px">` + 两个 `<el-form-item>`(用户名 + 密码)
- 用户名 `<el-input v-model="form.username" placeholder="请输入用户名">`
- 密码 `<el-input v-model="form.password" type="password" placeholder="请输入密码" show-password>`
- 登录按钮 `<el-button type="primary" :loading="loading" @click="handleLogin">登录</el-button>`(loading 防重复)

##### (c) 校验规则(强制 · pattern 取自后端权威源)

```js
const rules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 4, max: 20, message: '用户名长度 4-20 位', trigger: 'blur' },
    // ⚠️ 具体 pattern 从 API_DESIGN.md §3 接口详情的 RequestDTO @Pattern 注解读取
    // 若 api-designer 未规定具体 pattern,先问学生,禁止编造 /^[A-Za-z0-9]+$/ 等正则
    // { pattern: <从 api-designer 取>, message: '<跟后端一致的提示>', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度 6-20 位', trigger: 'blur' },
    // ⚠️ 同上,具体 pattern 取自后端权威源
  ],
})
```

> 📌 前端校验**只是 UX 友好提示,不是安全边界**(后端 `@Valid` + `@Pattern` 才是 · 对齐 CLAUDE.md §一·二)。前端 pattern 跟后端不一致会导致"后端通过的用户名前端报错",**必须**取自 API_DESIGN.md §3。

##### (d) 登录提交逻辑(关键 · 三步)

```js
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { loginUser } from '@/api/user'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const formRef = ref()

async function handleLogin() {
  await formRef.value.validate()  // 校验失败抛错 · 中断流程
  loading.value = true
  try {
    // ✅ axios 拦截器已 unwrap(对齐 axios-coder.md §一·5 + CLAUDE.md §一·三)
    // await 拿到的直接是 {token, userInfo} · 不是 {data:{token,...}} 嵌套
    const result = await loginUser(form)

    // 第 1 步:token 进 localStorage(给 axios 请求拦截器读 · 对齐 init-skeleton api/request.js)
    localStorage.setItem('token', result.token)

    // 第 2 步:userInfo 写入 user store(给 vue-page-coder 业务页面 useUserStore 读)
    userStore.setUser(result.userInfo)

    ElMessage.success('登录成功')

    // 第 3 步:redirect 回跳(对齐 init-skeleton router/index.js 守卫 query.redirect)
    // 多角色项目按 role 分流(单角色项目 fallback 直接 '/')
    const homeByRole = { admin: '/admin', user: '/' }  // ⚠️ 角色名以 docs/00-选题标定.md §一 "JWT 角色"行为准
    const fallback = homeByRole[result.userInfo.role] || '/'
    router.push(route.query.redirect || fallback)
  } finally {
    loading.value = false
  }
}
```

##### (e) ElMessage 使用规范(对齐 CLAUDE.md §三·三 拦截器规范)

- **登录失败**(密码错 / 账号不存在 / 5xx 等):**axios 拦截器已统一** `ElMessage.error(message)` · 组件层**不要再手动** `ElMessage.error`(重复弹两次)
- 组件层 try-catch **只**做清理(`finally { loading.value = false }`)+ ElMessage.success('登录成功')
- **例外**:登录失败需在表单字段上特殊提示(如「密码错误次数过多 · 5 分钟后重试」)时,组件层可 try-catch + `el-form-item :error`,但**禁止**在 catch 内重复 `ElMessage.error`

##### (f) 多角色分流跳转(若项目含多角色)

- 角色名映射表 `homeByRole = { admin: '/admin', user: '/', ... }` — 角色名**必须**对齐 `docs/00-选题标定.md §一` "JWT 角色"行 + CLAUDE.md 起手段 `{{角色列表}}`
- 单角色项目省略此判断,默认 `fallback = '/'`
- **不确定项目角色清单时,先问学生,禁止编造 admin/user/teacher 等角色名**

##### (g) UI 布局参考

```
┌──────────────────────────────────┐
│           [系统标题]              │
│                                   │
│   ┌───────────────────────────┐  │
│   │ 用户名 [_______________]  │  │
│   │ 密  码 [_______________]  │  │
│   │       [    登录    ]      │  │
│   └───────────────────────────┘  │
└──────────────────────────────────┘
```

CSS 居中布局(简化版 · `<el-card>` 包裹即可):
```html
<div style="display: flex; min-height: 100vh; justify-content: center; align-items: center; background: #f5f7fa;">
  <el-card style="width: 400px;">
    <h2 style="text-align: center; margin-bottom: 24px;">{{ siteTitle }}</h2>
    <el-form ...>...</el-form>
  </el-card>
</div>
```

> 📌 系统标题在 `<script setup>` 中 `const siteTitle = ref('<实际题名>')`,题名从 `docs/00-选题标定.md §一` 提取(对齐 CLAUDE.md 起手段 `{{题目}}`),**禁止**编造。

#### 2. `frontend/src/stores/user.js`(Pinia 组合式 store · 对齐 CLAUDE.md §三·四)

```js
import { defineStore } from 'pinia'
import { ref } from 'vue'

// ⚠️ 持久化方案 A(默认 · 推荐 · 简单):userInfo 同时存 localStorage,store 初始化时从中恢复 · 解决"刷新页面 store 丢失"问题
const STORAGE_KEY = 'userInfo'

export const useUserStore = defineStore('user', () => {
  // 初始化时从 localStorage 恢复(避免刷新后 vue-page-coder 取不到值)
  const stored = JSON.parse(localStorage.getItem(STORAGE_KEY) || 'null')
  const userId = ref(stored?.userId || null)
  const role = ref(stored?.role || null)
  const username = ref(stored?.username || null)

  function setUser(info) {
    userId.value = info.userId
    role.value = info.role
    username.value = info.username
    localStorage.setItem(STORAGE_KEY, JSON.stringify(info))  // 同步到 localStorage
  }

  function clearUser() {
    userId.value = null
    role.value = null
    username.value = null
    localStorage.removeItem(STORAGE_KEY)
  }

  return { userId, role, username, setUser, clearUser }
})
```

> 📌 **登出(logout)逻辑**(虽不属于本命令主职,但应同步在业务页面顶部「退出登录」按钮调用):
> ```js
> userStore.clearUser()
> localStorage.removeItem('token')
> router.push('/login')
> ```
> 业务页面右上角的退出登录按钮 由 vue-page-coder 在生成业务页面时按需添加。

#### 3. 修改 `frontend/src/router/index.js`

把 init-skeleton 生成的 `/login` 路由 component 从占位改为本命令生成的 `LoginPage.vue`,补 meta:

```js
// 找到现有的 /login 路由,改为:
{
  path: '/login',
  name: 'LoginPage',
  component: () => import('@/views/LoginPage.vue'),
  meta: {
    requiresAuth: false,  // 显式公开 · 守卫不拦
    title: '登录',
  },
},
```

> 🚨 **禁止**(对齐 init-skeleton 守卫规约):
> - 改其他已有路由(`/` HomePage 等)
> - 改全局守卫 `beforeEach` 逻辑(init-skeleton 已实现 redirect 回跳支持 · 若守卫缺 `query: { redirect: to.fullPath }` 用 `/bug-tracer-fe` 排查)
> - 把 redirect 回跳逻辑写在路由守卫里(应在本命令生成的 LoginPage.vue `handleLogin` 里读 `route.query.redirect`)

### 输出指令(Claude Code 必须遵守 · 06 §一·五·1)

1. **直接创建** `frontend/src/views/LoginPage.vue`(完整登录页 · 不要骨架占位)
2. **直接创建** `frontend/src/stores/user.js`(Pinia 组合式 store · 含持久化方案 A)
3. **修改** `frontend/src/router/index.js`(只改 `/login` 路由 component · 补 meta · 不改其他路由)
4. 完成后输出 diff 摘要(3 文件)
5. **不知道就说** — **禁止**编造(对齐 CLAUDE.md §一·四):
   - Element Plus 2.13.7 的 props/events / 函数式 API
   - api/user.js 未导出的函数名
   - 后端 `userInfo` 嵌套字段(`userId`/`role`/`username` 是否齐全)
   - 用户名密码校验 pattern(必须取自 API_DESIGN.md §3 · 不取自经验)
   - 项目角色清单(必须取自 docs/00-选题标定.md §一 "JWT 角色"行 · 不编造 admin/user/teacher)
   - 不确定时**直接说「需验证」**

### 调用示例

**示例 1 · 单角色登录(简化版)**:

```
/login-coder

请基于 docs/API_DESIGN.md §3 中登录接口 + frontend/src/api/user.js 中的 loginUser,生成 frontend/src/views/LoginPage.vue + frontend/src/stores/user.js,并修改 router/index.js 的 /login 路由 component。系统标题从 docs/00-选题标定.md §一 取。完成输出 diff。
```

**示例 2 · 多角色项目(含分流跳转)**:

```
/login-coder

请生成登录页 + user store · 多角色场景:docs/00-选题标定.md §一 "JWT 角色"行为「学生 / 维修员 / 管理员」(第 5 题示例),登录后学生跳 /,维修员跳 /worker,管理员跳 /admin · 用户名密码校验 pattern 取自 API_DESIGN.md §3 RequestDTO @Pattern。完成输出 diff(3 文件)。
```

### 验证 checklist

- [ ] `LoginPage.vue` 用 `<script setup>` + Composition API · `<el-form>` + `<el-input>` · `el-button` 含 loading
- [ ] 校验规则 pattern **取自 API_DESIGN.md §3 RequestDTO @Pattern**(不写死 `^[A-Za-z0-9]+$`)
- [ ] 登录提交三步全:(1) `localStorage.setItem('token', result.token)` (2) `userStore.setUser(result.userInfo)` (3) `router.push(route.query.redirect || fallback)`
- [ ] **未**写 `result.data.token`(拦截器已 unwrap · 直接 `result.token`)
- [ ] **未**在 catch 内重复 `ElMessage.error`(拦截器已统一)
- [ ] 多角色项目含 `homeByRole` 映射表(角色名对齐 docs/00-选题标定.md §一 "JWT 角色"行 · 单角色项目省略)
- [ ] `stores/user.js` 含 `userId / role / username` + `setUser` + `clearUser` + 持久化方案 A(localStorage `userInfo` 键)
- [ ] `router/index.js` `/login` 路由 component 已改 + meta 含 `requiresAuth: false` + `title: '登录'`
- [ ] `router/index.js` **未改**其他路由 + **未改**全局守卫
- [ ] 系统标题取自 `docs/00-选题标定.md §一`(不编造)

### 失败兜底

3 次失败 → 按以下顺序救火(对齐 init-skeleton/service-coder/axios-coder/vue-page-coder 兜底升级路径):

1. **退出 `claude` 重启 清空对话上下文**(让 Claude Code 重新加载命令模板)再试一次
2. **切换模型**到 V4 Pro 等更强模型再试
3. **模式 B 手动**:从 06 G-12 找整段 prompt,粘贴到 Claude Code,**末尾追加修正**「(1) 三步登录:token+localStorage、userStore.setUser、redirect 回跳;(2) `await loginUser()` 已 unwrap,直接用 `result.token` 不是 `result.data.token`;(3) 不在 catch 重复 ElMessage.error;(4) 同时生成 stores/user.js 含持久化方案 A;(5) router 只改 /login 路由 component,不改其他;(6) 校验 pattern 取自 api-designer.md」,完成输出 diff
4. 仍失败 → QQ 群求助 / 教师邮箱

---

## §二 应用修复模式

### 任务

扫描 `frontend/src/views/LoginPage.vue` + `frontend/src/router/index.js`(含守卫)+ `frontend/src/stores/user.js` 中所有 `// R-06-issue-编号` 注释,逐条修复并改为 `// R-06-issue-编号 已修复`。

### R-06 拆分协议(对齐 CLAUDE.md §三·一 8 类目录边界 · 复用 R-05 entity+service 拆分模式)

| R-06-issue 所在文件 | 负责命令 |
|---|---|
| **`frontend/src/views/LoginPage.vue`** + **`frontend/src/router/index.js` 守卫/路由** + **`frontend/src/stores/user.js`** | **本命令** `/login-coder 应用修复` |
| `frontend/src/api/<module>.js` + `frontend/src/api/request.js` | `/axios-coder 应用修复` |
| `frontend/src/views/<业务页面>.vue` + `frontend/src/components/` | `/vue-page-coder 应用修复` |

### 调用模板

```
/login-coder 应用修复

请扫描 frontend/src/views/LoginPage.vue + frontend/src/router/index.js(守卫部分)+ frontend/src/stores/user.js 中所有 R-06-issue 注释,逐条修复并改为"已修复"。完成输出 diff。
```

### 输出指令

1. **修改** LoginPage.vue / stores/user.js / router/index.js(每个 R-06-issue 注释处按 code-reviewer-fe 给的建议修改 + 注释改为「已修复」)
2. 完成后输出 diff 摘要 + 修复条数统计
3. 修复**禁止**改变本命令 §一 规约(三步登录 / unwrap 用法 / 拦截器不重复 / 持久化方案 A / 路由限改 /login),仅按 R-06 注释建议改实现细节

### ✅ LoginPage.vue / router 守卫 / user store 段 R-06 闭环后 · 下一步硬指令(防 builder 跨命令幻觉)

**当前位置**:Phase 5 单页面 7 步循环 Step 6(R-06 拆分协议 · login 段已修)→ **R-06 拆分协议要求 3 个命令各管一段** · 同一登录流程的 axios + vue-page + login 修复必须**全部跑完**才能 commit。

**完成提示模板**(builder 在 login 段闭环后必须输出 · 一字不漏):
> ✅ LoginPage.vue + router/index.js 守卫 + stores/user.js 下 R-06 注释已闭环(N 条修复)。**下一步根据剩余 R-06 注释位置选择**:
> - 若 `api/user.js` / `api/request.js` 还有 R-06 注释 → 调用 `/axios-coder 应用修复 模块=user`(接对话不新建)
> - 若 `views/<业务页面>.vue` / `components/` 还有 R-06 注释 → 调用 `/vue-page-coder 应用修复 页面=<P>`(接对话不新建)
> - 若以上两类都已修完 → 调用 `/git-committer` 提交:`feat(p5-login): Login + auth flow + R-06 review and fix`

**⛔ 禁止下列幻觉**:
- ⛔ **不要**直接 `/git-committer`——除非确认 axios + vue-page 段 R-06 也已修完(R-06 多文件拆分协议)
- ⛔ **不要**抢答 `/code-reviewer-fe`——R-06 已审完,正在应用修复
- ⛔ **不要**抢答下一个页面 `/vue-page-coder 页面=<X>`——登录流程 R-06 还没修完(login 是 Phase 5 首页)
- ⛔ **不要**抢答 Phase 6 `/unittest-coder`——所有页面跑完再进 Phase 6

---

## 衔接

**Phase 5 单页面 7 步循环**(对齐 08b §8.7):

| Step | 命令 | 说明 |
|:----:|---|---|
| Step 1 | `/axios-coder 模块=user` | 先生成 user API 模块(含 `loginUser`)· 本命令依赖此 |
| **Step 2** | **`/login-coder`**(本命令 §一) | 生成 LoginPage.vue + stores/user.js + 改 router /login 路由(调用前退出 `claude` 重启) |
| Step 4 | `pnpm dev` 联调登录流程 | 浏览器 F12 看 token 是否进 localStorage / store 是否写入 / redirect 是否回跳;报错用 `/bug-tracer-fe`(D-02) |
| Step 5 | `/code-reviewer-fe LoginPage` | R-06 审核(切 V4 Pro · 页面切片 · 位置参数 PascalCase=页面)+ 标 issue 注释 |
| Step 6 | **`/login-coder 应用修复`**(本命令 §二)+ `/axios-coder 应用修复` + `/vue-page-coder 应用修复` | R-06 拆分协议三命令各管一段 |
| Step 7 | `/git-committer` | 提交 + push |

> 📌 登录页**先于**业务页面生成是硬约束:vue-page-coder 业务页面通过 `useUserStore` 取 userInfo,user store 由本命令首次创建。若 vue-page-coder 早于本命令运行,store 不存在 → 业务页面报错。

## 📋 跨文件呼应导航

| 引用方向 | 目标文件 / 段落 | 引用内容 |
|---|---|---|
| 上游产出 | `API_DESIGN.md §3` | 登录接口 path + RequestDTO `@Pattern` 校验权威源 + `userInfo` 响应结构 |
| 上游产出 | `axios-coder.md §一` | `loginUser` 命名导出契约 + JSDoc `@returns {Promise<{token, userInfo}>}` + unwrap 契约 |
| 上游产出 | `TECH_DESIGN.md §3` | 路由设计 + 全局守卫规则(`requiresAuth` + `redirect` 回跳) |
| 上游产出 | `docs/00-选题标定.md §一` | 系统标题 + "JWT 角色"行(多角色分流跳转用) |
| 骨架对齐 | `init-skeleton.md` | `router/index.js`(`/login` 占位 + 全局守卫含 `query.redirect`)+ `api/request.js`(从 localStorage 读 token + 401 跳 /login)+ `stores/.gitkeep`(2026-05-10 链路断点闭合) |
| 全局规范 | `CLAUDE.md §一·一/§一·三/§一·二/起手段` | 技术栈 / `Result<T>` + axios 拦截器三段处理(全栈接口契约)/ 全栈安全(BCrypt 密码) / `{{题目}}` `{{角色列表}}` 占位 |
| 前端规范 | `CLAUDE.md §三·二/§三·三/§三·四/§三·五/§三·六` | Composition API / API 模块 / Pinia 组合式 store / Element Plus / JWT token localStorage |
| 下游消费 | `vue-page-coder.md` | 业务页面 `useUserStore` 取 `userId/role/username`(本命令是写入方,vue-page-coder 是消费方) |
| 下游审核 | `code-reviewer-fe.md`(R-06) | 8 维度审核 + R-06 拆分协议(本命令负责 `LoginPage.vue + router 守卫 + stores/user.js`) |
| 排查类 | `bug-tracer-fe.md`(D-02) | 接对话不退出 `claude` 重启 · 浏览器入口 bug(redirect 回跳异常 / store 刷新丢失 等) |
| 同步对象 | `rules-updater.md` | Phase 5 末同步 `project-status.md`「已完成的前端模块」字段 |
| 项目流程 | `08b-项目实施操作流程.md §8.7 Step 2` | Phase 5 第 2 步登录页生成 |
| 06 模板源 | `06-提示词与审核模板库.md G-12` | 提示词原始模板(注意:06 模板未含 user store + redirect 回跳 · 以本命令为准) |
