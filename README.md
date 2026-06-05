# SmartOA — シンプル OA 承認ワークフロー管理システム

> # SmartOA — 简易 OA 审批流管理系统

> エンタープライズ OA 承認ワークフロー管理システム（P2 完了版） | Spring Boot 3 + Vue 3 + MyBatis-Plus + JWT
>
> 企业级 OA 审批流管理系统（P2 完成版） | Spring Boot 3 + Vue 3 + MyBatis-Plus + JWT

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.14-brightgreen" alt="Spring Boot 4"/>
  <img src="https://img.shields.io/badge/Vue-3-4FC08D" alt="Vue 3"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL 8"/>
  <img src="https://img.shields.io/badge/license-MIT-green" alt="License"/>
</p>

---

## プロジェクト概要

SmartOA は、企業の日常業務向けの**シンプルな承認ワークフロー管理システム**です。JWT 認証、承認テンプレート管理、休暇申請と多段階承認フローをサポートします。コア設計は「テンプレート設定 + フローエンジン」を中心に展開され、条件分岐、並行承認（カウンターサイン/オアサイン）、タイムアウト自動エスカレーションなどの高度な機能を備えています。

> SmartOA 是一个面向企业日常办公的**简易审批流管理系统**，支持 JWT 认证、审批模板管理、请假申请与多级审批流转。核心设计围绕"模板配置 + 流程引擎"展开，支持条件分支、并行审批（会签/或签）、超时自动升级等高级特性。

---

## 技術スタック

| レイヤー | 技術 |
|----------|------|
| バックエンド | Spring Boot 3.5.14 |
| 永続化 | MyBatis-Plus 3.5.15 |
| データベース | MySQL 8.0 |
| 認証 | JWT（jjwt 0.13.0）+ BCrypt |
| フロントエンド | Vue 3.5（Composition API） |
| UI ライブラリ | Element Plus 2.13.7 |
| ビルドツール | Vite 8 |
| パッケージ管理 | pnpm |
| 状態管理 | Pinia |
| ルーティング | Vue Router 5 |

> | 层级 | 技术 |
> |------|------|
> | 后端框架 | Spring Boot 3.5.14 |
> | 持久层 | MyBatis-Plus 3.5.15 |
> | 数据库 | MySQL 8.0 |
> | 认证鉴权 | JWT（jjwt 0.13.0）+ BCrypt |
> | 前端框架 | Vue 3.5（Composition API） |
> | UI 组件库 | Element Plus 2.13.7 |
> | 构建工具 | Vite 8 |
> | 包管理 | pnpm |
> | 状态管理 | Pinia |
> | 路由 | Vue Router 5 |

---

## プロジェクト構成

```
smartoa/
├── backend/
│   ├── src/main/java/com/smartoa/
│   │   ├── common/              # Result<T> 統一レスポンス、BusinessException、GlobalExceptionHandler
│   │   ├── config/              # セキュリティ設定、CORS、JWT フィルター
│   │   ├── controller/          # REST コントローラー（5 ファイル）
│   │   ├── dto/                 # データ転送オブジェクト
│   │   ├── entity/              # エンティティクラス（7 ファイル、ApprovalTask 含む）
│   │   ├── mapper/              # MyBatis-Plus Mapper（7 ファイル）
│   │   └── service/             # ビジネスロジック層（5 ファイル）+ TimeoutScheduler
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
├── frontend/                # Vue 3 フロントエンド
│   └── src/
│       ├── api/             # API クライアント（auth / leave / template）
│       ├── stores/          # Pinia 状態管理（auth / approval / users）
│       ├── router/          # ルーティング設定
│       ├── views/           # ページコンポーネント（15 ページ）
│       ├── components/      # 共有コンポーネント（StatusTag / ApprovalTimeline）
│       └── layouts/         # レイアウトコンポーネント（MainLayout）
├── docs/
│   ├── mysql-p0-upgrade.sql  # テーブル作成 + シードデータ
│   ├── mysql-p3-bcrypt.sql   # BCrypt パスワード移行
│   ├── mysql-p4-parallel.sql # 並行承認
│   └── mysql-p5-timeout.sql  # タイムアウト自動エスカレーション
├── CLAUDE.md
└── README.md
```

> 项目结构同上方日文版。后端 Java 21 + Spring Boot 3，前端 Vue 3 + Element Plus，7 张 MySQL 表。

---

## データベース設計

| テーブル | 説明 |
|----------|------|
| `sys_user` | ユーザーテーブル（直属上司・部門ディレクターの関連付けを含む） |
| `approval_template` | 承認テンプレートテーブル |
| `approval_node` | 承認ノードテーブル（条件式、サイン種別、タイムアウト設定対応） |
| `template_field` | テンプレートフィールドテーブル |
| `leave_request` | 休暇申請テーブル（current_node_id + timeout_time でフロー制御） |
| `approval_record` | 承認記録テーブル |
| `approval_task` | 並行承認タスクテーブル（カウンターサイン/オアサイン時の各承認者状態） |

> | 表名 | 说明 |
> |------|------|
> | `sys_user` | 用户表（含直属领导、部门总监关联） |
> | `approval_template` | 审批模板表 |
> | `approval_node` | 审批节点表（支持条件表达式、签批模式、超时配置） |
> | `template_field` | 模板字段表 |
> | `leave_request` | 请假申请表（current_node_id + timeout_time 驱动流转） |
> | `approval_record` | 审批记录表 |
> | `approval_task` | 并行审批任务表（会签/或签模式下各审批人状态） |

---

## クイックスタート

### 環境要件

- Java 21+
- MySQL 8.0+
- Node.js 18+ / pnpm
- Maven 3.8+

> ### 环境要求
>
> - Java 21+
> - MySQL 8.0+
> - Node.js 18+ / pnpm
> - Maven 3.8+

### 1. データベース作成

```sql
CREATE DATABASE smartoa DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

続いて `docs/` 以下の SQL スクリプトを順にインポートします。

> ### 1. 建库
>
> ```sql
> CREATE DATABASE smartoa DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
> ```
>
> 然后依次导入 `docs/` 下的 SQL 脚本。

### 2. バックエンド起動

```bash
cd backend && ./mvnw spring-boot:run
```

デフォルトポート：`8080`。

> ### 2. 启动后端
>
> ```bash
> cd backend && ./mvnw spring-boot:run
> ```
>
> 默认端口 `8080`。

### 3. フロントエンド起動

```bash
cd frontend
pnpm install
pnpm run dev
```

デフォルトポート：`5173`。バックエンドへのプロキシ転送が設定済みです。

> ### 3. 启动前端
>
> ```bash
> cd frontend
> pnpm install
> pnpm run dev
> ```
>
> 默认端口 `5173`，已配置代理转发到后端。

### 4. ログイン

ブラウザで `http://localhost:5173` を開き、以下のアカウントでログインしてください：

| ユーザー名 | パスワード | 役割 | 備考 |
|------------|------------|------|------|
| admin | 123456 | MANAGER | 技術部マネージャー |
| zhangsan | 123456 | EMPLOYEE | 一般社員 |

> ### 4. 登录
>
> 浏览器打开 `http://localhost:5173`，使用以下账户登录：
>
> | 用户名 | 密码 | 角色 | 说明 |
> |--------|------|------|------|
> | admin | 123456 | MANAGER | 技术部经理 |
> | zhangsan | 123456 | EMPLOYEE | 普通员工 |

---

## 実装済み機能

### P0 基本機能

- [x] ユーザーログイン（JWT + BCrypt + ロール区別）
- [x] 承認テンプレート CRUD
- [x] 休暇申請の提出
- [x] ハードコード二段階承認フロー
- [x] 保留中 / 完了 / 自分の提出タスク管理
- [x] 承認詳細ページ

> ### P0 基础功能
>
> - [x] 用户登录（JWT + BCrypt + 角色区分）
> - [x] 审批模板 CRUD
> - [x] 请假申请提交
> - [x] 硬编码二级审批流转
> - [x] 我的待办 / 已办 / 我提交的
> - [x] 审批详情页

### P1 アップグレード機能

- [x] 8 テーブルデータベース設計
- [x] 設定可能な多段階承認エンジン（approval_node テーブル駆動、動的ノード巡回）
- [x] 承認 / 却下 / 取り消し / 転送の 4 アクション
- [x] 承認ノード設定 UI（テンプレート編集時に追加・削除・ドラッグ＆ドロップ並べ替え可能）
- [x] フロー進捗バー（`el-steps`、動的ノード状態）
- [x] 承認履歴タイムライン（`el-timeline`、操作タイプを色分け）
- [x] ECharts 統計チャート
- [x] バックエンド Excel エクスポート（Apache POI）
- [x] `Result<T>` 統一レスポンス + BusinessException グローバル例外処理

> ### P1 升级功能
>
> - [x] 8 张数据库表设计
> - [x] 可配置多级审批引擎（approval_node 表驱动，动态节点遍历）
> - [x] 同意 / 拒绝 / 撤回 / 转派四种操作
> - [x] 审批节点配置 UI（模板编辑时可添加/删除/拖拽排序节点）
> - [x] 流程进度条（`el-steps`，动态节点状态）
> - [x] 审批历史时间线（`el-timeline`，颜色标注操作类型）
> - [x] ECharts 统计图表
> - [x] 后端 Excel 导出（Apache POI）
> - [x] `Result<T>` 统一响应 + BusinessException 全局异常处理

### P2 アップグレード機能

- [x] **フローノードビジュアルエディタ** — ドラッグ＆ドロップ並べ替え、動的ノード追加/削除
- [x] **条件分岐** — SpEL 式駆動（休暇日数 `days`、休暇種類 `leaveType` などの条件で分岐）
- [x] **並行承認** — 単人（SINGLE）/ カウンターサイン（COUNTER_SIGN）/ オアサイン（OR_SIGN）の 3 種別
- [x] **タイムアウト自動エスカレーション** — ESCALATE（転送）/ AUTO_APPROVE（自動承認）/ AUTO_REJECT（自動却下）、`@Scheduled` で 5 分毎にチェック
- [x] **滞留修復** — `repairStuckRequests()` で `currentApproverId` が null の異常滞留申請を修復

> ### P2 升级功能
>
> - [x] **流程节点可视化编辑器** — 拖拽排序、动态添加/删除节点
> - [x] **条件分支** — SpEL 表达式驱动（支持按请假天数 `days`、请假类型 `leaveType` 等条件分流）
> - [x] **并行审批** — 单人（SINGLE）/ 会签（COUNTER_SIGN）/ 或签（OR_SIGN）三种签批模式
> - [x] **超时自动升级** — ESCALATE（转派）/ AUTO_APPROVE（自动通过）/ AUTO_REJECT（自动驳回），`@Scheduled` 每 5 分钟检查
> - [x] **滞留修复** — `repairStuckRequests()` 修复 `currentApproverId` 为 null 的异常滞留申请

---

## API 概要

| メソッド | パス | 説明 |
|----------|------|------|
| POST | `/api/login` | ログイン |
| GET | `/api/users` | ユーザー一覧 |
| GET | `/api/templates` | テンプレート一覧 |
| POST | `/api/templates` | テンプレート作成 |
| GET | `/api/templates/{id}/nodes` | 承認ノード取得 |
| POST | `/api/templates/{id}/nodes` | 承認ノード保存 |
| POST | `/api/leave/submit` | 休暇申請提出 |
| POST | `/api/leave/approve` | 休暇承認 |
| POST | `/api/leave/{id}/withdraw` | 申請取り消し |
| POST | `/api/leave/{id}/transfer` | 承認転送 |
| GET | `/api/leave/pending` | 保留中一覧 |
| GET | `/api/leave/done` | 処理済み一覧 |
| GET | `/api/leave/my-requests` | 自分の申請 |
| GET | `/api/leave/{id}` | 申請詳細 |
| GET | `/api/leave/{id}/records` | 承認記録 |
| GET | `/api/leave/{id}/tasks` | 並行承認タスク |
| POST | `/api/leave/repair` | 滞留修復 |
| GET | `/api/stats/summary` | 統計サマリー |
| GET | `/api/stats/export` | Excel エクスポート |

> | 方法 | 路径 | 说明 |
> |------|------|------|
> | POST | `/api/login` | 登录 |
> | GET | `/api/users` | 用户列表 |
> | GET | `/api/templates` | 模板列表 |
> | POST | `/api/templates` | 创建模板 |
> | GET | `/api/templates/{id}/nodes` | 获取审批节点 |
> | POST | `/api/templates/{id}/nodes` | 保存审批节点 |
> | POST | `/api/leave/submit` | 提交请假 |
> | POST | `/api/leave/approve` | 审批请假 |
> | POST | `/api/leave/{id}/withdraw` | 撤回 |
> | POST | `/api/leave/{id}/transfer` | 转派 |
> | GET | `/api/leave/pending` | 待审批列表 |
> | GET | `/api/leave/done` | 已审批列表 |
> | GET | `/api/leave/my-requests` | 我的申请 |
> | GET | `/api/leave/{id}` | 请假单详情 |
> | GET | `/api/leave/{id}/records` | 审批记录 |
> | GET | `/api/leave/{id}/tasks` | 并行审批任务 |
> | POST | `/api/leave/repair` | 滞留修复 |
> | GET | `/api/stats/summary` | 统计摘要 |
> | GET | `/api/stats/export` | Excel 导出 |

---

## ライセンス

MIT License

> MIT 许可证
