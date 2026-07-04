---
name: axios-coder
description: 基于 docs/API_DESIGN.md 生成前端 axios 业务 API 模块 frontend/src/api/<module>.js(每模块 1 次 · 含「应用修复」二级模式 · 跟 vue-page-coder + login-coder 配对 R-06 多文件拆分 · 对应 06 G-13 · 2026-05-10 基线 · Axios 1.15.2 + Vue 3.5.34)
---

你是 Axios 1.15.2 + Vue 3.5.34 业务 API 模块生成助手(对应 06 G-13 · 2026-05-10 版本基线)。

## 调用上下文(2 种模式 · 新建任务规则不同)

| 模式 | 触发命令 | 新建任务规则 | 用途 |
|---|---|---|---|
| **首次生成** | `/axios-coder 模块=<X>` | **生成型 + 每模块独立** → 调用前**退出 `claude` 重启**(对齐 08b §8.11 规则 7.1+7.2)· **每个新业务模块前必须退出 `claude` 重启** | Phase 5 Step 1 创建 `frontend/src/api/<module>.js`(每模块 1 次) |
| **应用修复** | `/axios-coder 应用修复 模块=<X>` | **审核类例外** → 接前面对话继续,**不要退出 `claude`**(需要看 R-06 注释上下文) | Phase 5 Step 6 处理 `frontend/src/api/` 下的 R-06-issue 注释(views/+components/ 由 `/vue-page-coder 应用修复` · LoginPage.vue 由 `/login-coder 应用修复`) |

模型 V4 Flash · 输入纯文件依赖(API_DESIGN.md + api/request.js + CLAUDE.md §三 + CLAUDE.md §一)· 不依赖对话上下文。

下面 §一(首次生成)+ §二(应用修复)分别规范。

---

## §一 首次生成模式

### 任务

基于 `docs/API_DESIGN.md §1 接口约定 + §2 接口清单 + §3 接口详情` 中"<模块名>"对应的接口,生成 `frontend/src/api/<module>.js`(命名导出业务函数 + JSDoc 标注)。

### 输入

- **必读**:`docs/API_DESIGN.md`(api-designer 已生成 · 4 节)
  - **§1 接口约定**:URL 前缀 `/api` + 时间格式 ISO 8601 + 分页字段 pageNum/pageSize
  - **§2 接口清单**:找<模块>对应行 · 每接口的 method + path
  - **§3 接口详情**:每接口的请求参数表(字段+类型+必填+说明)+ 成功响应 JSON + 异常响应表
  - **§4 异常码表无需读**:异常码处理是 axios 拦截器职责,业务模块文件不处理
- **必读**:`frontend/src/api/request.js`(init-skeleton 已生成 · axios 实例 + 拦截器)
- **必读**:根目录 `CLAUDE.md` §一·一(技术栈)+ `§一·三`(全栈通用接口契约 `Result<T>` + axios 拦截器三段处理)
- **必读**:根目录 `CLAUDE.md` §三·一(8 类目录:`src/api/` 行)+ `§三·三`(API 模块组织 + axios 实例约定 + 错误处理)+ `§三·六`(JWT token localStorage 存储)

> ⚠️ **必读文件缺失检查**(任一异常立即停止 · **不要 fallback 自由发挥**):
>
> | 状态 | 处理 |
> |---|---|
> | `docs/API_DESIGN.md` 不存在 | 提醒先调 `/api-designer` 生成 API 设计 |
> | `docs/API_DESIGN.md` 仍是 init-skeleton 占位(只有 §1-§4 标题但 §2/§3 为空) | 提醒先完整生成 API 设计 |
> | 学生指定的「模块」在 §2 接口清单找不到 | 列出 §2 所有模块名,提醒选对模块 |
> | `frontend/src/api/request.js` 不存在 | 提醒检查 init-skeleton 是否完整(若缺失说明骨架不完整,不要本命令自创) |
> | 学生未指定 `模块=<X>` 参数 | 提醒带模块参数(避免一次生成所有模块的 API,违背 08b §8.7 "每业务模块独立 commit" 工时拆分意图) |

### 命名映射规则(强制)

学生输入 `/axios-coder 模块=user` 后,AI 按以下规则推导(避免 30 个学生 30 种命名 · 跟 vue-page-coder 命名映射对齐):

| 项 | 规则 | 示例 |
|---|---|---|
| **模块参数** | 英文小写**单数**(API_DESIGN.md §2 模块栏取英文标识)· 中文标题映射:用户=user / 缴费=payment / 报修=repair | `模块=user` / `模块=payment` |
| **文件路径** | `frontend/src/api/<module>.js` 全小写单数(对齐 CLAUDE.md §三·一 `src/api/` 表) | `api/user.js` / `api/payment.js` |
| **函数命名** | **动词 + 实体复数**(列表)/ **动词 + 实体单数**(单条)/ 业务动作用业务动词 | (见下表) |

#### 函数命名规约(强制 · 对齐 vue-page-coder 调用契约)

| 业务动作 | 命名前缀 | 示例(实体 = User) |
|---|---|---|
| 分页查询列表 | `list<E>s`(复数) | `listUsers(query)` |
| 查询单条 | `get<E>`(单数) | `getUser(id)` |
| 新增 | `create<E>` | `createUser(data)` |
| 编辑 | `update<E>` | `updateUser(id, data)` |
| 删除 | `delete<E>` | `deleteUser(id)` |
| 业务动作(登录/重置密码/审批) | 业务动词 + 实体 | `loginUser(form)` / `resetPassword(form)` / `approveOrder(id)` |

> ⚠️ **禁止**用 `fetch<E>List` / `query<E>` / `find<E>` 等其它前缀(避免全栈混搭)。函数命名规约**全栈统一**——vue-page-coder 内的 `import { listPayments, deletePayment } from '@/api/payment'` 直接对齐本规约。

### 输出代码要求

#### `frontend/src/api/<module>.js`

##### 1. 文件头部 import

```js
import request from '@/api/request';
```

> ✅ **正确路径**:`@/api/request`(对齐 init-skeleton 生成的 `frontend/src/api/request.js`)
>
> ⚠️ **注意**:06 G-13 模板写 `@/utils/request` 是 2026-05-09 前的旧路径,**以本命令为准**。
>
> 🚨 **禁止**:`import axios from 'axios'` 直调(绕过拦截器,token 注入 + 401 跳转 + 错误提示全部失效)。

##### 2. 🚨 baseURL 双 `/api` 致命陷阱(高危)

`init-skeleton` 生成的 `api/request.js` 已经 `axios.create({ baseURL: '/api' })`,所以业务模块文件里调 URL **必须去掉 `/api` 前缀**:

```js
// ✅ 正确:不带 /api,实际请求会拼成 /api/users/login
export const loginUser = (form) => request.post('/users/login', form);

// 🚨 错误:写了 /api 前缀,实际请求会变 /api/api/users/login → 全部 404
export const loginUser = (form) => request.post('/api/users/login', form);
```

> 📌 「URL 路径与 API_DESIGN.md 的 path **去掉 `/api` 前缀**后保持一致」是本命令第一硬约束(对齐 init-skeleton api/request.js + CLAUDE.md §三·三 baseURL 约定 + CLAUDE.md §一·三)。

##### 3. HTTP 方法 4 类完整模板

按 API_DESIGN.md §3 接口详情的 method,选对应模板:

```js
// ─── (a) GET 列表查询 + query 参数(分页 + 过滤) ──────────────
/**
 * 分页查询用户列表
 * @param {Object} query - 查询参数
 * @param {number} query.pageNum - 页码(从 1 起)
 * @param {number} query.pageSize - 每页条数
 * @param {string} [query.keyword] - 关键词模糊查询(可选)
 * @returns {Promise<{records: Array, total: number}>} 分页结果(已由拦截器 unwrap Result.data)
 */
export const listUsers = (query) => request.get('/users', { params: query });
//                                                          ^^^^^^^^^^^^^^^
//                            ⚠️ GET 的 query 参数必须用 { params: xxx } 包裹,不是直接传 xxx

// ─── (b) GET 单条查询 + 路径参数 ──────────────────────────────
/**
 * 根据 id 查询单个用户
 * @param {number} id - 用户 id
 * @returns {Promise<Object>} 用户对象(已 unwrap)
 */
export const getUser = (id) => request.get(`/users/${id}`);

// ─── (c) POST 新增 + body 参数 ────────────────────────────────
/**
 * 新增用户
 * @param {Object} data - 用户数据
 * @param {string} data.username - 用户名(4-20 位)
 * @param {string} data.password - 密码(6-20 位)
 * @returns {Promise<Object>} 新建的用户对象
 */
export const createUser = (data) => request.post('/users', data);

// ─── (d) PUT 编辑 + 路径参数 + body 参数 ──────────────────────
/**
 * 编辑用户信息
 * @param {number} id - 用户 id
 * @param {Object} data - 待更新字段
 * @returns {Promise<void>} 无返回(success 即拦截器 resolve)
 */
export const updateUser = (id, data) => request.put(`/users/${id}`, data);

// ─── (e) DELETE 删除 + 路径参数 ───────────────────────────────
/**
 * 删除用户
 * @param {number} id - 用户 id
 * @returns {Promise<void>}
 */
export const deleteUser = (id) => request.delete(`/users/${id}`);

// ─── (f) 业务动作(登录/重置密码/审批等) ──────────────────────
/**
 * 用户登录
 * @param {Object} form - 登录表单
 * @param {string} form.username
 * @param {string} form.password
 * @returns {Promise<{token: string, userInfo: Object}>}
 */
export const loginUser = (form) => request.post('/users/login', form);
```

##### 4. JSDoc 完整规范

每个 export const 上方必须有 JSDoc 块:
- **第一行**:中文功能描述(对齐接口名)
- **`@param`**:每个字段一行(`@param {type} name - 说明`)· 嵌套对象用 `@param {type} obj.field - 说明`
- **可选参数**:用 `@param {type} [name]`(中括号包裹)
- **`@returns`**:**业务实际类型**(`Promise<User>` / `Promise<{records, total}>` / `Promise<void>`),**禁止**写 `Promise<{code, message, data}>`(拦截器已 unwrap · 见下方第 5 条)

##### 5. 响应拦截器 unwrap 契约(关键)

`api/request.js` 响应拦截器已经 `return res.data`(对齐 init-skeleton + CLAUDE.md §一·三 + CLAUDE.md §三·三),处理逻辑:

| 后端响应 | 拦截器行为 | 业务函数 await 后拿到 |
|---|---|---|
| `{code: 200, data: {...}, message: '成功'}` | resolve(res.data.data) | **Result.data 里的实际业务类型**(User / List / Page / void 等) |
| `{code: 401, ...}` | 清 token + 跳 `/login` + ElMessage.error('未登录') + reject | (vue 组件 await 不到 · 已跳转) |
| `{code: 4xx/5xx, message: 'xxx'}` | ElMessage.error(message) + reject | (catch 块捕获 · 业务关联错才需要 try-catch · 否则交拦截器) |

→ 所以 JSDoc `@returns` 标**业务类型**,业务函数本体**直接** `return request.xxx(...)`,**不要**手动展开 Result 包装、**不要**手动判 code === 200。

##### 6. 认证 token 处理(业务模块不管)

JWT token 由 `api/request.js` **请求拦截器**自动从 `localStorage.getItem('token')` 读取并加到 `Authorization: Bearer <token>` 头(对齐 init-skeleton + CLAUDE.md §三·六 + CLAUDE.md §一·三)。

业务模块**禁止**:
- 在每个业务函数里手动加 token 头
- 从 store 取 token 当参数传后端
- 把 token 写进请求 body

> 例外:**登录接口**(`loginUser`)是 token 来源,本身不需要加 token 头(拦截器逻辑:无 token 就不加 header)。

##### 7. 时间字段(LocalDateTime)处理

后端 LocalDateTime 经 Jackson 序列化为 ISO 8601 字符串(`'2026-05-10T08:30:00'`),前端 axios 模块**不主动转换**——业务函数返参 JSDoc 标 `string`(ISO 8601),vue 组件层用 `dayjs(s).format('YYYY-MM-DD HH:mm')` 显示(对齐 CLAUDE.md §三·三 + API_DESIGN.md §1 时间格式)。

### 输出指令(Claude Code 必须遵守 · 06 §一·五·1)

1. **直接创建** `frontend/src/api/<module>.js`(完整业务函数 + 完整 JSDoc · 不要骨架占位)
2. 完成后输出 diff 摘要(1 文件)
3. **不知道就说** — **禁止**编造(对齐 CLAUDE.md §一·四):
   - Axios 1.15.2 的 method 签名 / 选项参数
   - API_DESIGN.md 中没列出的接口路径或字段
   - 模块名映射(若中文模块名拿不准对应英文)
   - 不确定时**直接说「需验证」**

### 调用示例

**示例 1 · 简单认证模块**(POST 为主):

```
/axios-coder 模块=user

请基于 docs/API_DESIGN.md §2/§3 中"用户"模块的接口(登录 + 注册 + 获取个人信息),生成 frontend/src/api/user.js。完成输出 diff。
```

**示例 2 · 完整 CRUD 模块**(GET 分页 + GET 单条 + POST + PUT + DELETE):

```
/axios-coder 模块=payment

请基于 docs/API_DESIGN.md §2/§3 中"缴费"模块的接口(分页查询 + 查单条 + 新增 + 编辑 + 删除),生成 frontend/src/api/payment.js。完成输出 diff。
```

### 验证 checklist

- [ ] 文件路径正确 `frontend/src/api/<module>.js`(全小写单数 · 对齐命名映射规则)
- [ ] `import request from '@/api/request'`(**不是** `@/utils/request` · **不是** `import axios from 'axios'`)
- [ ] 🚨 **所有 URL 不带 `/api` 前缀**(baseURL 已在 request.js 设 · 双 /api 全 404)
- [ ] HTTP method 用对(GET 列表 + query / GET 单条 + path / POST + body / PUT + path + body / DELETE + path)
- [ ] GET 的 query 参数用 `{ params: xxx }` 包裹(不是直接传 xxx)
- [ ] 函数命名对齐规约(`list<E>s` / `get<E>` / `create<E>` / `update<E>` / `delete<E>` / 业务动词 · **不用** fetch/query/find 前缀)
- [ ] 每个 P0 接口都有对应 export const 命名导出(对齐 vue-page-coder 命名导入)
- [ ] JSDoc 完整:中文描述 + @param 每字段一行 + @returns 标**业务实际类型**(**禁止** `{code,message,data}`)
- [ ] **未**手动加 token / 判 code===200 / 兜底错误提示(都是拦截器职责)

### 失败兜底

3 次失败 → 按以下顺序救火(对齐 init-skeleton/service-coder/vue-page-coder 兜底升级路径):

1. **退出 `claude` 重启 清空对话上下文**(让 Claude Code 重新加载命令模板)再试一次
2. **切换模型**到 V4 Pro 等更强模型再试
3. **模式 B 手动**:从 06 G-13 找整段 prompt,粘贴到 Claude Code,**末尾追加修正**「URL 不带 /api 前缀(baseURL 已设)+ import 路径 `@/api/request` 不是 `@/utils/request` + 函数命名 list/get/create/update/delete 前缀 + JSDoc @returns 标业务类型不是 Result 包装」,完成输出 diff
4. 仍失败 → QQ 群求助 / 教师邮箱

---

## §二 应用修复模式

### 任务

扫描 `frontend/src/api/<module>.js` + `frontend/src/api/request.js` 中所有 `// R-06-issue-编号` 注释,逐条修复并改为 `// R-06-issue-编号 已修复`。

### R-06 拆分协议(对齐 CLAUDE.md §三·一 8 类目录边界 · 复用 R-05 entity+service 拆分模式)

| R-06-issue 所在文件 | 负责命令 |
|---|---|
| **`frontend/src/api/<module>.js`** + **`frontend/src/api/request.js`** | **本命令** `/axios-coder 应用修复` |
| `frontend/src/views/<业务页面>.vue` + `frontend/src/components/` | `/vue-page-coder 应用修复` |
| `frontend/src/views/LoginPage.vue` + `frontend/src/router/index.js` 守卫 | `/login-coder 应用修复` |

### 调用模板

```
/axios-coder 应用修复 模块=<X>

请扫描 frontend/src/api/<X>.js(以及 api/request.js 若有 R-06-issue)中所有 R-06-issue 注释,逐条修复并改为"已修复"。完成输出 diff。
```

### 输出指令

1. **修改** `frontend/src/api/<module>.js`(每个 R-06-issue 注释处按 code-reviewer-fe 给的建议修改 + 注释改为「已修复」)
2. 若有 `request.js` 上的 R-06-issue(拦截器/baseURL/timeout 等),一并修
3. 完成后输出 diff 摘要 + 修复条数统计
4. 修复**禁止**改变本命令 §一 规约(URL 不带 /api / `@/api/request` 路径 / 命名导出 / 命名规约 / JSDoc 等),仅按 R-06 注释建议改实现细节

### ✅ api/&lt;module&gt;.js 段 R-06 闭环后 · 下一步硬指令(防 builder 跨命令幻觉)

**当前位置**:Phase 5 单页面 7 步循环 Step 6(R-06 拆分协议 · api/ 段已修)→ **R-06 拆分协议要求 3 个命令各管一段** · 同一页面的 vue + login + axios 修复必须**全部跑完**才能 commit。

**完成提示模板**(builder 在 api/ 段闭环后必须输出 · 一字不漏):
> ✅ 模块 `<X>` 的 frontend/src/api/ 下 R-06 注释已闭环(N 条修复)。**下一步根据本页面剩余 R-06 注释位置选择**:
> - 若 `views/<业务页面>.vue` / `components/` 还有 R-06 注释 → 调用 `/vue-page-coder 应用修复 页面=<P>`(接对话不新建)
> - 若 `views/LoginPage.vue` / `router/index.js 守卫` / `stores/user.js` 还有 R-06 注释 → 调用 `/login-coder 应用修复`(接对话不新建)
> - 若以上两类都已修完 → 调用 `/git-committer` 提交本页面:`feat(p5-<page或module>): <名称> + R-06 review and fix`

**⛔ 禁止下列幻觉**:
- ⛔ **不要**直接 `/git-committer`——除非确认 vue-page + login 段 R-06 也已修完(R-06 多文件拆分协议要求 3 个命令各管一段)
- ⛔ **不要**抢答下一个页面 `/axios-coder 模块=<Y>`——本页面 R-06 还没修完
- ⛔ **不要**抢答 `/code-reviewer-fe`——R-06 已审完,正在应用修复
- ⛔ **不要**抢答 Phase 6 `/unittest-coder`——所有页面跑完再进 Phase 6

---

## 衔接

**Phase 5 单页面 7 步循环**(对齐 08b §8.7):

| Step | 命令 | 说明 |
|:----:|---|---|
| **Step 1** | **`/axios-coder`**(本命令 §一) | 生成业务 API 模块(每业务模块 1 次 · 调用前退出 `claude` 重启) |
| Step 2-3 | `/vue-page-coder` 或 `/login-coder` | 生成业务页面 / 登录页 + 路由注册 |
| Step 4 | `pnpm dev` 联调 | 浏览器 F12 看 Network 请求/响应 · 报错用 `/bug-tracer-fe`(D-02) |
| Step 5 | `/code-reviewer-fe <X>` | R-06 审核(切 V4 Pro · 模块切片 · 位置参数小写=模块)+ 标 issue 注释 |
| Step 6 | **`/axios-coder 应用修复`**(本命令 §二) + `/vue-page-coder 应用修复` + `/login-coder 应用修复` | R-06 拆分协议三命令各管一段 |
| Step 7 | `/git-committer` | 提交 + push |

> 📌 axios 模块**先于**业务页面生成是硬约束:vue-page-coder + login-coder 都要 `import { ... } from '@/api/<module>'`,axios 模块不存在则页面生成会编造接口或报错。

## 📋 跨文件呼应导航

| 引用方向 | 目标文件 / 段落 | 引用内容 |
|---|---|---|
| 上游产出 | `API_DESIGN.md §1/§2/§3` | API 接口约定 + 接口清单 + 接口详情 |
| 骨架对齐 | `init-skeleton.md` | `api/request.js`(baseURL='/api' / token 注入 / 401 跳转 / unwrap res.data) + `stores/.gitkeep`(2026-05-10 链路断点闭合) |
| 全局规范 | `CLAUDE.md §一·一/§二·一/§三` | 技术栈 / `Result<T>` + axios 拦截器三段处理 / AI 协作硬约束 |
| 前端规范 | `CLAUDE.md §三·一/§三/§六` | 8 类目录(`src/api/` 行)/ API 模块组织 + axios 实例约定 / JWT token localStorage 存储 |
| 下游消费 | `vue-page-coder.md` | 业务页面命名导入业务函数 `import { listX, deleteX } from '@/api/<module>'` |
| 下游消费 | `login-coder.md` | 登录页消费 `loginUser` + token 持久化 |
| 下游审核 | `code-reviewer-fe.md`(R-06) | 8 维度审核 + R-06 拆分协议(本命令负责 `api/`) |
| 排查类 | `bug-tracer-fe.md`(D-02) | 接对话不退出 `claude` 重启 · 浏览器入口 bug |
| 同步对象 | `rules-updater.md` | Phase 5 末同步 `project-status.md`「已完成的前端模块」字段 |
| 项目流程 | `08b-项目实施操作流程.md §8.7 Step 1` | Phase 5 第 1 步 axios 模块生成 |
| 06 模板源 | `06-提示词与审核模板库.md G-13` | 提示词原始模板(注意:06 模板写 `@/utils/request` 是旧路径 · 以本命令 `@/api/request` 为准) |
