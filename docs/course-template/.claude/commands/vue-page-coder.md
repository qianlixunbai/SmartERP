---
name: vue-page-coder
description: 基于页面原型 + axios API 模块生成 Vue 业务页面 .vue + 路由注册(对应 06 G-11 · 2026-05-10 基线 · Vue 3.5.34 + Element Plus 2.13.7 + Pinia 3.0.4)
---

> ⚠️ **生成型命令** — 调用前请先 退出 `claude` 后重新运行 `claude`(新会话清空上下文)(对齐 08b §8.11 规则 7.1+7.2 · 规则 7.2 生成型命令清单含本命令)。

你是 Vue 3.5.34 + Element Plus 2.13.7 业务页面生成助手(对应 06 G-11 · 2026-05-10 版本基线)。

## 任务

基于 docs/TECH_DESIGN.md §6 页面原型描述 + 对应 axios API 模块,生成业务页面 .vue + 注册路由。

## 输入

- 用户用 `/vue-page-coder 页面=<PageName>` 调用(可选 `路径=/xxx` 显式指定 path)
- 读 **docs/TECH_DESIGN.md §6** — 该页面的原型描述(由 page-prototyper.md G-04 产出)
  - 应含 7 项必填:① 页面标题 / ② 路由 path / ③ 顶部操作区按钮 / ④ 主区控件类型(表格/表单)/ ⑤ 字段清单(列名+类型+校验)/ ⑥ 行内/底部操作按钮 / ⑦ 弹窗清单
  - **若 §6 中本页面缺以上任一项,先问学生补全,禁止编造字段或操作**
- 读 **frontend/src/api/<module>.js** — 对应业务 API 模块(由 axios-coder G-13 产出 · 命名导出业务函数)
- 读 **frontend/src/router/index.js** — 已有路由配置(在 routes 数组**末尾追加**新路由,**禁止**改已有路由)

## 命名映射规则(强制 · 2026-05-11 升级:强制 Page 后缀)

学生输入 `/vue-page-coder 页面=PaymentList` 后,AI 按以下规则推导(避免 30 个学生 30 种命名 · 与 login-coder 生成 `LoginPage.vue` + init-skeleton 占位 `HomePage.vue` + rules-updater 扫描期望对齐):

| 项 | 规则 | 示例(`PaymentList`) | 示例(`UserProfilePage`) | 示例(`Home`) |
|---|---|---|---|---|
| .vue 文件名 | **PageName 大驼峰 + 自动追加 `Page` 后缀**(若学生传的已含 `Page` 后缀则原样不重复)| `PaymentListPage.vue` | `UserProfilePage.vue` | `HomePage.vue` |
| 路由 path | `/` + 去掉末尾 `List`/`Page` 后转 kebab-case | `/payments` | `/user-profile` | `/` |
| 路由 name | 跟 .vue 文件名一致(含 `Page` 后缀) | `PaymentListPage` | `UserProfilePage` | `HomePage` |

> ⚠️ **2026-05-11 升级**(强制 Page 后缀 · 链路断点修复):
> - **统一命名规约**:所有 views/ 下的 .vue 都用 **`大驼峰 + Page 后缀`**(如 `LoginPage.vue` / `HomePage.vue` / `PaymentListPage.vue`)
> - **跟下游 4 个权威源对齐**:`login-coder` 生成 `LoginPage.vue` + `init-skeleton` 占位 `LoginPage.vue` / `HomePage.vue` + `rules-updater` 扫描期望「大驼峰 + Page 后缀」+ `08b 节奏表 commit message` `feat(p5-LoginPage)` 风格
> - **覆盖式重写**:若 init-skeleton 已生成同名占位文件(如 `views/HomePage.vue`),本命令**直接覆盖**(不要先手删占位文件,也不要追加新文件名)
> - **历史踩坑**:此前命名规则不强制 Page 后缀 · 学生跑 `/vue-page-coder 页面=PaymentList` 生成 `PaymentList.vue`(无 Page)· 跟 init-skeleton 占位 `HomePage.vue`(有 Page)及 login-coder `LoginPage.vue`(有 Page)风格分裂 · rules-updater 扫描时统计错乱

**例外**:学生在调用时显式传 `路径=/xxx`(如详情页用动态参数 `路径=/payments/:id`),按学生显式值覆盖默认推导。**但 .vue 文件名仍强制 Page 后缀**(动态路由不影响命名规约)。

## 输出代码要求

### `frontend/src/views/<PageName>.vue`

#### 1. Composition API 形态(对齐 CLAUDE.md §三·二)

- 用 `<script setup>` 语法,**禁止** `export default { setup() {} }` 旧写法
- `ref` 用基本类型(`loading` / `dialogVisible` / `submitting` / `total` / `pageNum` / `pageSize` / `editingId`)
- `reactive` 用复杂对象(`formData` / `queryParams` / `rules`)
- `computed` 用派生(如 `dialogTitle` 区分新增/编辑)
- `onMounted` 内调 `loadList()` 首次加载
- **import 顺序**(集中在 `<script setup>` 顶部):`vue` → `vue-router` → `element-plus` → `@/api/<module>` → `@/stores/<store>`

#### 2. Element Plus 引入策略

- EP **全注册**已由 init-skeleton 在 `main.js` 完成,业务页面**直接用** `<el-table>` `<el-button>` 等标签
- **禁止**在 .vue 内重复 `import { ElXxx } from 'element-plus'` 注册单组件
- **例外**:`ElMessage` `ElMessageBox` `ElLoading` 三类**函数式 API** 必须显式 `import { ElMessage, ElMessageBox } from 'element-plus'`

#### 3. API 调用契约(对齐 CLAUDE.md §三·三)

- 业务函数**命名导入**:`import { listPayments, deletePayment, createPayment, updatePayment } from '@/api/payment'`(由 axios-coder 命名导出)
- **禁止**在 .vue 内 `import request from '@/api/request'` 直调(`request` 仅在 axios-coder 模块文件内部使用)
- **禁止**在 .vue 内写 `if (res.code !== 200) ...` 硬编码(响应码处理是 axios 拦截器职责 · 见 CLAUDE.md §一·三)

#### 4. Pinia 状态管理(对齐 CLAUDE.md §三·四)

- 读当前登录用户(userId / role / username 等)统一通过:
  ```js
  import { useUserStore } from '@/stores/user'
  const userStore = useUserStore()
  // 用 userStore.userId / userStore.role
  ```
- **禁止**在业务页面 `localStorage.getItem('userId')` 直读
- **禁止**在每个页面单独发 `/api/user/info` 请求

#### 5. 业务页面标准形态(4 类 · 按页面原型实际需要选取)

##### (a) 列表查询

- 数据获取函数 `loadList(pageNum, pageSize, queryParams)` — 内部 `loading.value = true` 起 / `finally { loading.value = false }` 止
- `<el-table v-loading="loading" :data="list">` + `<el-table-column prop label width align>` 按字段清单一一列出(列宽 80-200px,操作列固定 right)
- `<el-pagination>` 完整属性(分页字段 `pageNum`/`pageSize` 对齐 API_DESIGN.md §1 接口约定):
  ```html
  <el-pagination
    v-model:current-page="pageNum"
    v-model:page-size="pageSize"
    :total="total"
    :page-sizes="[10, 20, 50]"
    layout="total, sizes, prev, pager, next, jumper"
    @current-change="loadList(pageNum, pageSize, queryParams)"
    @size-change="loadList(1, pageSize, queryParams)"
  />
  ```
- 列表为空(`list.length === 0` 且 `!loading`)显示 `<el-empty description="暂无数据" />` 替代默认"暂无数据"提示

##### (b) 新增/编辑弹窗

- `<el-dialog v-model="dialogVisible" :title="dialogTitle" width="600px" @closed="resetForm">` — `dialogTitle` 计算属性(`editingId.value ? '编辑XX' : '新增XX'`)
- 表单 `<el-form ref="formRef" :model="formData" :rules="rules" label-width="100px">`:
  - `formRef = ref()`(类型 `FormInstance`)
  - `formData = reactive({...})`
  - `rules = reactive({...})` 顶层定义(必填 / 长度 / 正则等 · `trigger: 'blur'`)
- 提交按钮 `<el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>`
- submit 流程:
  ```js
  await formRef.value.validate();
  submitting.value = true;
  try {
    if (editingId.value) {
      await updateXxx(editingId.value, formData);
    } else {
      await createXxx(formData);
    }
    ElMessage.success('保存成功');
    dialogVisible.value = false;
    loadList(pageNum.value, pageSize.value, queryParams);
  } finally {
    submitting.value = false;
  }
  ```

##### (c) 删除二次确认

```js
ElMessageBox.confirm('确认删除该记录?', '提示', {
  type: 'warning',
  confirmButtonText: '删除',
  cancelButtonText: '取消',
})
  .then(async () => {
    await deleteXxx(id);  // 业务错由 axios 拦截器统一 ElMessage.error
    ElMessage.success('删除成功');
    loadList(pageNum.value, pageSize.value, queryParams);
  })
  .catch(() => {});  // 必须写 catch · 用户点取消时 Promise reject 会触发未捕获错误警告
```

##### (d) 查询条件区(若原型有)

- `<el-form :inline="true" :model="queryParams">` + 「查询」「重置」按钮
- 查询: `loadList(1, pageSize.value, queryParams)`(回到第 1 页)
- 重置: `formRef.value.resetFields()` + `loadList(1, pageSize.value, queryParams)`

#### 6. 错误处理三态(对齐 CLAUDE.md §三·三 axios 拦截器规范)

| 状态 | 实现 |
|---|---|
| 加载中 | `v-loading="loading"` 指令绑表格/弹窗 |
| 空数据 | `<el-empty description="暂无数据" />` 替代默认提示 |
| 错误 | **网络错 / HTTP 错 / 全局业务错由 axios 拦截器统一 `ElMessage.error(msg)`**(组件层不处理);组件层**只**对**业务关联错**(如表单字段冲突需在字段上特殊提示)做 try-catch + `ElMessage.warning` 或 `el-form-item :error` |

- **禁止**在组件层写 `<el-alert>` 兜底错误(拦截器已统一 · 重复弹)
- **禁止**在组件层写 `if (res.code !== 200) ...`

#### 7. 页面文件大小

- 单个 .vue 超过 **300 行**考虑拆分子组件到 `frontend/src/components/`(对齐 CLAUDE.md §三·一末尾)

### 路由注册

修改 `frontend/src/router/index.js`,在 routes 数组**末尾追加**(禁止改已有路由):

```js
{
  path: '/<page-path>',          // 由 PageName 推导(详见上方"命名映射规则")
  name: '<PageName>',
  component: () => import('@/views/<PageName>.vue'),
  meta: {
    title: '<页面标题>',         // 浏览器 tab 标题(从原型描述取)· 路由守卫可用此设 document.title
    requiresAuth: true,          // 需登录访问;若公开页面(登录/注册/帮助/找回密码)显式置 false
    roles: ['<角色>']            // 可选 · 多角色项目限制访问 · 角色名对齐 docs/00-选题标定.md §一 "JWT 角色"行 + CLAUDE.md 起手段 {{角色列表}}
  }
}
```

#### 默认主张(简化教学,90 分钟课跑得通)

- **列表 + 详情 + 新增 + 编辑全在同一个页面用 `el-dialog` 切换**,**不开多路由**
- 若学生明确需要详情页/编辑页**独立路由**,显式调用 `路径=/payments/:id`(动态参数),并在 setup 内用:
  ```js
  import { useRoute, useRouter } from 'vue-router'
  const route = useRoute()
  const router = useRouter()
  // 取参数: route.params.id  跳转: router.push({ name: 'PaymentList' })
  ```

## 输出指令(Claude Code 必须遵守 · 06 §一·五·1)

1. **直接创建** `frontend/src/views/<PageName>.vue`(完整业务形态,不要骨架占位)
2. **修改** `frontend/src/router/index.js`(末尾追加路由配置 · 不改已有路由)
3. 完成后输出 diff 摘要(2 文件)
4. **不知道就说** — **禁止**编造(对齐 CLAUDE.md §一·四):
   - Element Plus 2.13.7 的 props / events / 方法签名 / 函数式 API
   - Vue 3.5.34 的 Composition API 用法
   - axios-coder 模块未导出的函数名 / 后端字段名
   - 不确定时直接说「需验证」

## 调用示例

**示例 1 · 简单列表页**:

```
/vue-page-coder 页面=PaymentList

请基于 docs/TECH_DESIGN.md §6 中"缴费记录页"原型 + frontend/src/api/payment.js,生成 frontend/src/views/PaymentList.vue + 注册路由(默认推导 /payments)。完成输出 diff。
```

**示例 2 · 多角色管理页**:

```
/vue-page-coder 页面=UserManage

请基于 docs/TECH_DESIGN.md §6 中"用户管理页"原型(含查询条件区 + 列表表格 + 新增/编辑弹窗)+ frontend/src/api/user.js,生成 frontend/src/views/UserManage.vue + 注册路由(/users · meta.roles=['admin'])。完成输出 diff。
```

## 验证 checklist

- [ ] .vue 文件已创建,用 `<script setup>` + Composition API 形态(ref/reactive/computed/onMounted)
- [ ] 命名映射对齐(PageName 大驼峰 / path kebab-case / name 大驼峰)
- [ ] router/index.js 末尾追加路由(name/path/component/meta 四字段完整 · 已有路由未改)
- [ ] 业务形态完整(按原型实际取舍):列表分页 / 表单校验 / 弹窗新增编辑 / 删除二次确认(含 catch)/ 查询条件区
- [ ] 三态处理:loading / empty / 错误由拦截器(组件层无 `<el-alert>` 兜底 · 无 `if (res.code !== 200)`)
- [ ] API 调用走**命名导入业务函数**(.vue 内**无** `import request`)
- [ ] 状态管理用 `useUserStore`(.vue 内**无** `localStorage.getItem('userId')` 直读)
- [ ] EP 直接用标签(无重复 `import { ElXxx } from 'element-plus'` · 仅 ElMessage/ElMessageBox/ElLoading 例外)
- [ ] meta.requiresAuth 配置正确,多角色项目 meta.roles 对齐 docs/00-选题标定.md §一 "JWT 角色"行
- [ ] .vue 文件未超 300 行(否则拆子组件到 components/)

## 衔接

**Phase 5 单页面 7 步循环**(对齐 08b §8.7):

| Step | 命令 | 说明 |
|:----:|---|---|
| 前置 | `/axios-coder` + `/login-coder` | 已完成 axios 模块 + 登录页 + token 持久化 + user store |
| **Step 2-3** | **`/vue-page-coder`**(本命令) | 生成业务页面 .vue + 路由注册 |
| Step 4 | `pnpm dev` 联调 | 浏览器 F12 看 Network 请求/响应 · 报错用 `/bug-tracer-fe`(D-02) |
| Step 5 | `/code-reviewer-fe <PageName>` | R-06 审核(切 V4 Pro · 页面切片 · 位置参数 PascalCase=页面)+ 标 issue 注释 |
| Step 6 | `/vue-page-coder` + 应用修复 prompt | 详见下方「应用修复模式」段(R-06 拆分协议) |
| Step 7 | `/git-committer` | 提交 + push |

## 应用修复模式

**R-06 拆分协议**(对齐 CLAUDE.md §三·一 8 类目录边界 · 复用 R-05 entity+service 拆分模式):

| R-06-issue 所在文件 | 负责命令 |
|---|---|
| `frontend/src/views/<业务页面>.vue` + `frontend/src/components/` | **本命令** `/vue-page-coder` |
| `frontend/src/api/<module>.js` + `frontend/src/api/request.js` | `/axios-coder` |
| `frontend/src/views/LoginPage.vue` + `frontend/src/router/index.js` 守卫 | `/login-coder` |

**调用模板**:

```
/vue-page-coder 请扫描 frontend/src/views/<PageName>.vue 中所有 R-06-issue 注释,逐条修复并改为"已修复"。完成输出 diff。
```

### ✅ views/&lt;业务页面&gt;.vue + components 段 R-06 闭环后 · 下一步硬指令(防 builder 跨命令幻觉)

**当前位置**:Phase 5 单页面 7 步循环 Step 6(R-06 拆分协议 · views + components 段已修)→ **R-06 拆分协议要求 3 个命令各管一段** · 同一页面的 axios + login + vue-page 修复必须**全部跑完**才能 commit。

**完成提示模板**(builder 在 views 段闭环后必须输出 · 一字不漏):
> ✅ 页面 `<PageName>.vue` + components/ 下 R-06 注释已闭环(N 条修复)。**下一步根据剩余 R-06 注释位置选择**:
> - 若 `api/<module>.js` / `api/request.js` 还有 R-06 注释 → 调用 `/axios-coder 应用修复 模块=<X>`(接对话不新建)
> - 若 `LoginPage.vue` / `router/index.js 守卫` / `stores/user.js` 还有 R-06 注释 → 调用 `/login-coder 应用修复`(接对话不新建)
> - 若以上两类都已修完 → 调用 `/git-committer` 提交本页面:`feat(p5-<page>): <页面名> + R-06 review and fix`

**⛔ 禁止下列幻觉**:
- ⛔ **不要**直接 `/git-committer`——除非确认 axios + login 段 R-06 也已修完(R-06 多文件拆分协议)
- ⛔ **不要**抢答 `/code-reviewer-fe`——R-06 已审完,正在应用修复
- ⛔ **不要**抢答下一个页面 `/vue-page-coder 页面=<Y>`——本页面 R-06 还没修完
- ⛔ **不要**抢答 Phase 6 `/unittest-coder`——所有页面跑完再进 Phase 6

## 失败兜底

3 次失败 → 按以下顺序救火(对齐 init-skeleton/service-coder 兜底升级路径):

1. **退出 `claude` 重启 清空对话上下文**(让 Claude Code 重新加载命令模板)再试一次
2. **切换模型**到 V4 Pro 等更强模型再试
3. **模式 B 手动**:从 06 G-11 找整段 prompt,粘贴到 Claude Code,末尾追加「请直接创建上述 .vue + 修改 router/index.js,完成输出 diff」
4. 仍失败 → QQ 群求助 / 教师邮箱

## 📋 跨文件呼应导航

| 引用方向 | 目标文件 / 段落 | 引用内容 |
|---|---|---|
| 上游产出 | `TECH_DESIGN.md §3` | 前端路由设计(meta 字段 / 动态路由约定) |
| 上游产出 | `TECH_DESIGN.md §6` | 页面原型描述 7 项必填字段(由 page-prototyper 生成) |
| 上游产出 | `axios-coder.md §一` | API 模块命名导出契约(业务函数) |
| 上游产出 | `login-coder.md §一` | user store(`useUserStore`)形态 + token 持久化 |
| 全局规范 | `CLAUDE.md §一·一/§一·三/§一·四` | 技术栈版本 / `Result<T>` + axios 拦截器契约 / AI 协作硬约束 |
| 前端规范 | `CLAUDE.md §三·一/§三·二/§三·三/§三·四/§三·五` | 8 类目录 / Composition API / API 模块 / Pinia / Element Plus |
| 接口对齐 | `API_DESIGN.md §1` | 分页字段 `pageNum`/`pageSize` + 接口前缀 `/api` |
| 骨架对齐 | `init-skeleton.md` | `stores/.gitkeep`(2026-05-10 stores/store 链路断点闭合 · 方案 B)+ EP 全注册 + axios 实例 |
| 下游审核 | `code-reviewer-fe.md`(R-06) | 8 维度审核 + R-06 拆分协议(本命令负责 views/+components/) |
| 排查类 | `bug-tracer-fe.md`(D-02) | 接对话不退出 `claude` 重启 · 浏览器入口 bug |
| 同步对象 | `rules-updater.md` | Phase 5 末同步 `project-status.md` 的「已完成的前端页面」字段 |
| 项目流程 | `08b-项目实施操作流程.md §8.7` | Phase 5 单页面 7 步循环 |
| 06 模板源 | `06-提示词与审核模板库.md G-11` | 提示词原始模板 |
