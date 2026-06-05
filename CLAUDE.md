# SmartOA — シンプル OA 承認ワークフロー管理システム
> # SmartOA — 简易 OA 审批流管理系统

Spring Boot + Vue 3 + MyBatis-Plus、休暇承認フローに特化。

> Spring Boot + Vue 3 + MyBatis-Plus，聚焦请假审批流程。

---

## 技術スタック
> ## 技术栈

- バックエンド：Spring Boot 3.5.14 + Java 21 + MyBatis-Plus 3.5.15 + MySQL
- フロントエンド：Vue 3.5.34 + Vite 8.0 + Element Plus 2.13.7 + Pinia 3.0.4 + Axios 1.15.2 + pnpm
- 認証：JWT (jjwt 0.13.0) + BCrypt (spring-security-crypto)

> - 后端：Spring Boot 3.5.14 + Java 21 + MyBatis-Plus 3.5.15 + MySQL
> - 前端：Vue 3.5.34 + Vite 8.0 + Element Plus 2.13.7 + Pinia 3.0.4 + Axios 1.15.2 + pnpm
> - 认证：JWT (jjwt 0.13.0) + BCrypt (spring-security-crypto)

## 起動方法
> ## 启动方式

```bash
# バックエンド
cd backend && ./mvnw spring-boot:run          # → localhost:8080

# フロントエンド
cd frontend && pnpm run dev      # → localhost:5173（proxy /api → 8080）
```

> ```bash
> # 后端
> cd backend && ./mvnw spring-boot:run          # → localhost:8080
> 
> # 前端
> cd frontend && pnpm run dev      # → localhost:5173（proxy /api → 8080）
> ```

## 現在の進捗
> ## 当前进度

- **P0/P1 完了**：JWT 認証、設定可能な承認エンジン（approval_node テーブル駆動）、6 テーブルアーキテクチャ、4 承認アクション（承認/却下/取り消し/転送）、Excel エクスポート、ECharts 統計
- **P2 完了**：
  - フローノードビジュアルエディタ（ドラッグ＆ドロップ並べ替え、動的追加/削除）
  - 条件分岐（SpEL 式、休暇日数/種類などの条件で分岐）
  - 並行承認 — カウンターサイン（COUNTER_SIGN）+ オアサイン（OR_SIGN）+ 単人（SINGLE）
  - タイムアウト自動エスカレーション（ESCALATE/AUTO_APPROVE/AUTO_REJECT、@Scheduled で 5 分毎チェック）
- **P3 未対応**：動的フォームレンダリング、フロントエンド Excel エクスポート、モバイル対応

> - **P0/P1 已完成**：JWT 认证、可配置审批引擎（approval_node 表驱动）、6 表架构、4 审批动作（同意/拒绝/撤回/转派）、Excel 导出、ECharts 统计
> - **P2 已完成**：
>   - 流程节点可视化编辑器（拖拽排序、动态添加/删除）
>   - 条件分支（SpEL 表达式，支持按请假天数/类型等条件分流）
>   - 并行审批 — 会签（COUNTER_SIGN）+ 或签（OR_SIGN）+ 单人（SINGLE）
>   - 超时自动升级（ESCALATE/AUTO_APPROVE/AUTO_REJECT，@Scheduled 每 5 分钟检查）
> - **P3 待做**：动态表单渲染、前端 Excel 导出、移动端适配

## データベース移行
> ## 数据库迁移

| スクリプト | 説明 |
|------|------|
| `docs/mysql-p0-upgrade.sql` | データベース作成 + テーブル作成 + シードデータ |
| `docs/mysql-p3-bcrypt.sql` | パスワードを BCrypt にアップグレード |
| `docs/mysql-p4-parallel.sql` | 並行承認（sign_type + approval_task テーブル） |
| `docs/mysql-p5-timeout.sql` | タイムアウト自動エスカレーション（timeout_hours + timeout_time） |

> | 脚本 | 说明 |
> |------|------|
> | `docs/mysql-p0-upgrade.sql` | 建库建表 + 种子数据 |
> | `docs/mysql-p3-bcrypt.sql` | 密码升级为 BCrypt |
> | `docs/mysql-p4-parallel.sql` | 并行审批（sign_type + approval_task 表） |
> | `docs/mysql-p5-timeout.sql` | 超时自动升级（timeout_hours + timeout_time） |

## テストアカウント
> ## 测试账号

| ユーザー名 | パスワード | 役割 | 説明 |
|--------|------|------|------|
| admin | 123456 | MANAGER | 技術部マネージャー |
| zhangsan | 123456 | EMPLOYEE | 一般社員（直属上司=admin） |

> | 用户名 | 密码 | 角色 | 说明 |
> |--------|------|------|------|
> | admin | 123456 | MANAGER | 技术部经理 |
> | zhangsan | 123456 | EMPLOYEE | 普通员工（直属领导=admin） |

## グローバル設定
> ## 全局配置

Claude Code は DeepSeek API バックエンドを使用（`C:\Users\28421\.claude\settings.json`）：
- `ANTHROPIC_BASE_URL` = `https://api.deepseek.com/anthropic`
- デフォルトモデル：`deepseek-v4-pro`、Haiku：`deepseek-v4-flash`

> Claude Code 使用 DeepSeek API 后端（`C:\Users\28421\.claude\settings.json`）：
> - `ANTHROPIC_BASE_URL` = `https://api.deepseek.com/anthropic`
> - 默认模型：`deepseek-v4-pro`、Haiku：`deepseek-v4-flash`
