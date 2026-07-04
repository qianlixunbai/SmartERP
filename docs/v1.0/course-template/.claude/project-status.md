# 项目当前进度状态

> 📌 **本文件由 `/rules-updater` 命令每 Phase 末自动重写**(动态状态 · 单独存放在 `.claude/` 目录 · 与根 `CLAUDE.md` 静态规则解耦)。

> 📋 **Phase 编号(0-8 共 9 阶段)**
>
> `0 项目初始化` · `1 SRS + 概要设计` · `2 数据库设计` · `3 API 设计` · `4 后端开发` · `5 前端开发` · `6 集成调试 + 单测` · `7 重构 + 代码审查` · `8 部署 + 文档`

> 📌 **维护分工**
>
> - **本文件结构(字段名 + 顺序)**:由教师项目模板维护(2026-05-12 基线 · 与 `/rules-updater` 输出 100% 对齐)。学生**不要**新增 / 删除 / 重命名字段——改字段会让 `/rules-updater` 输出对不上,后续命令读到错误状态。
> - **字段值更新**:
>   - **Phase 0 → 1 切换**:**手动**改第一行"当前 Phase"为 `Phase 1`(此时 docs/sql/views 还为空,无需扫描;详见 `08b-项目实施操作流程.md §7「必改 2」`)
>   - **Phase 1+ 各 Phase 末**:跑 `/rules-updater` **自动**扫描重写整个文件(详见 `08b-项目实施操作流程.md §8.11 规则 1`)

## 当前状态字段(9 个)

- **当前 Phase**:Phase 0(项目初始化) <!-- Phase 0 → 1 切换时手动改本行 -->
- **上次更新**:2026-05-12 <!-- 由 /rules-updater 自动写入 YYYY-MM-DD;手动切换 Phase 时不必改本字段 -->
- **已完成文档**:无 <!-- 完成 SRS 后由 /rules-updater 自动列出,如:PRD.md, TECH_DESIGN.md, DATABASE_DESIGN.md -->
- **数据库表**:无 <!-- 完成 Phase 2 后自动列表名,如:user, house, payment, repair -->
- **已有接口**:无 <!-- 完成 Phase 3 后自动列接口数 + 关键功能,如:8 个接口:登录注册 / 房屋 CRUD / 缴费列表 / 报修工单 -->
- **已完成的后端模块**:无 <!-- 完成 Phase 4 后自动列 ServiceImpl,如:UserServiceImpl, HouseServiceImpl, PaymentServiceImpl -->
- **已完成的前端页面**:无 <!-- 完成 Phase 5 后自动列 .vue 文件名(大驼峰 + Page 后缀,对齐 CLAUDE.md §三·一 + init-skeleton 路由占位),如:LoginPage.vue, DashboardPage.vue, PaymentListPage.vue -->
- **已完成的对话记录数**:0 <!-- /rules-updater 统计 docs/对话记录/ 下 .md 文件数 · 05 验收要求 ≥21 -->
- **已完成的 commit 数(估)**:1 <!-- /rules-updater 估值(基于 git log --oneline | wc -l) · 05 验收要求 ≥30 -->

## 每 Phase 末该做的事

1. **Phase 0 → 1 切换**:不调用 `/rules-updater`(扫描结果会全为空),只手动改"当前 Phase"为 `Phase 1`,其他字段仍为"无"/"0"
2. **Phase 1+ 各 Phase 末**:跑 `/rules-updater` 自动重写本文件(扫描 docs/ + sql/ + backend/ + frontend/ 重新填字段)
3. 跑 `/git-committer` 把本文件改动 commit + push(commit message 形如 `chore(rules): Phase X 末状态同步`)

> ⚠️ **不要把"技术栈/编码规范/AI 协作约束"等内容写到这里**——那些都在根目录 `CLAUDE.md`(对应单一权威源)。本文件只装"项目当前到哪儿了"的动态状态值。
