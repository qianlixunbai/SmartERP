# SmartOA — シンプル OA 承認フロー管理システム

> # SmartOA — 简易 OA 审批流管理系统

エンタープライズ級 OA 承認ワークフロー管理システム | Spring Boot 3 + Vue 3 + MyBatis-Plus + JWT

> 企业级 OA 审批流管理系统 | Spring Boot 3 + Vue 3 + MyBatis-Plus + JWT

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange" alt="Java 21"/>
  <img src="https://img.shields.io/badge/Spring_Boot-3.5.14-brightgreen" alt="Spring Boot 3.5"/>
  <img src="https://img.shields.io/badge/Vue-3-4FC08D" alt="Vue 3"/>
  <img src="https://img.shields.io/badge/MySQL-8.0-blue" alt="MySQL 8"/>
  <img src="https://img.shields.io/badge/Tests-24_passed-brightgreen" alt="Tests"/>
  <img src="https://img.shields.io/badge/license-MIT-green" alt="License"/>
</p>

---

## プロジェクト概要

SmartOA は企業の日常業務向けの**シンプルな承認フロー管理システム**で、JWT 認証、承認テンプレート管理、休暇申請と多段階承認フローをサポートします。コア設計は「テンプレート設定 + フローエンジン」を中心に展開し、条件分岐、並行承認（会簽/或簽）、タイムアウト自動エスカレーションなどの高度な機能をサポートします。

> SmartOA 是一个面向企业日常办公的**简易审批流管理系统**，支持 JWT 认证、审批模板管理、请假申请与多级审批流转。核心设计围绕"模板配置 + 流程引擎"展开，支持条件分支、并行审批（会签/或签）、超时自动升级等高级特性。

---

## 技術スタック

| 階層 | 技術 |
|------|------|
| バックエンドフレームワーク | Spring Boot 3.5.14 |
| 永続層 | MyBatis-Plus 3.5.15 |
| データベース | MySQL 8.0 |
| 認証・認可 | JWT（jjwt 0.13.0）+ BCrypt |
| テスト | JUnit 5 + Spring Boot Test |
| フロントエンドフレームワーク | Vue 3.5（Composition API） |
| UI コンポーネントライブラリ | Element Plus 2.13.7 |
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
> | 测试 | JUnit 5 + Spring Boot Test |
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
│   │   ├── common/              # Result<T>、BusinessException、GlobalExceptionHandler
│   │   ├── config/              # セキュリティ設定、CORS、JWT フィルター
│   │   ├── controller/          # REST コントローラー（5つ）
│   │   ├── dto/                 # データ転送オブジェクト
│   │   ├── entity/              # エンティティクラス（7つ）
│   │   ├── mapper/              # MyBatis-Plus Mapper（7つ）
│   │   └── service/             # ビジネスロジック層（5つ）+ TimeoutScheduler
│   ├── src/test/java/com/smartoa/service/
│   │   ├── LeaveServiceTest.java   # 承認フローテスト（18ケース）
│   │   └── UserServiceTest.java    # ユーザーログインテスト（5ケース）
│   ├── src/main/resources/
│   │   └── application.properties
│   └── pom.xml
├── frontend/
│   └── src/
│       ├── api/                 # API ラッパー
│       ├── stores/              # Pinia 状態管理
│       ├── router/              # ルーティング設定
│       ├── views/               # ページコンポーネント（15ページ）
│       ├── components/          # 共有コンポーネント
│       ├── styles/              # グローバル CSS 変数
│       └── layouts/             # レイアウトコンポーネント
├── docs/                        # SQL マイグレーションスクリプト
├── CLAUDE.md
└── README.md
```

> 项目结构同上方日文版。

---

## テスト

プロジェクトには **24 件のユニットテスト**が含まれており、承認フローの主要なシナリオをカバーしています。

```bash
# 全テスト実行
cd backend && ./mvnw test

# 特定のテストクラスのみ実行
./mvnw test -Dtest=LeaveServiceTest
./mvnw test -Dtest=UserServiceTest

# 特定のテストメソッドのみ実行
./mvnw test -Dtest=LeaveServiceTest#testAdminSubmitLeave_ShouldSkipDirectLeaderNode
```

### テストカバレッジ

| テストクラス | ケース数 | カバー機能 |
|-------------|---------|-----------|
| LeaveServiceTest | 18 | 承認フロー全操作 |
| UserServiceTest | 5 | ログイン・ユーザー管理 |
| SmartoaApplicationTests | 1 | アプリケーション起動 |

**LeaveServiceTest 内訳：**
- 申請提出: 4件（adminノードスキップ、社員通常、条件分岐）
- 承認操作: 5件（承認、却下、権限超越、重複、記録保存）
- 取下げ: 3件（正常、非申請者、却下済み）
- 転送: 2件（正常、非承認者）
- 照会: 3件（詳細、記録、保留リスト）
- 滞留修復: 1件

> **测试覆盖：**
>
> | 测试类 | 用例数 | 覆盖功能 |
> |--------|--------|---------|
> | LeaveServiceTest | 18 | 审批流程全部操作 |
> | UserServiceTest | 5 | 登录与用户管理 |
> | SmartoaApplicationTests | 1 | 应用启动 |
>
> **LeaveServiceTest 详情：**
> - 提交申请: 4个（admin跳过节点、员工正常、条件分支）
> - 审批操作: 5个（通过、驳回、越权、重复、记录保存）
> - 撤回: 3个（正常、非申请人、已驳回）
> - 转派: 2个（正常、非审批人）
> - 查询: 3个（详情、记录、待审批列表）
> - 滞留修复: 1个

---

## データベース設計

| テーブル名 | 説明 |
|------|------|
| `sys_user` | ユーザーテーブル（直属上司、部門ディレクター関連を含む） |
| `approval_template` | 承認テンプレートテーブル |
| `approval_node` | 承認ノードテーブル（条件式、承認モード、タイムアウト設定をサポート）⭐ コア |
| `template_field` | テンプレートフィールドテーブル |
| `leave_request` | 休暇申請テーブル（current_node_id + timeout_time でフローを駆動） |
| `approval_record` | 承認記録テーブル |
| `approval_task` | 並行承認タスクテーブル（会簽/或簽モードでの各承認者のステータス） |

> | 表名 | 说明 |
> |------|------|
> | `sys_user` | 用户表（含直属领导、部门总监关联） |
> | `approval_template` | 审批模板表 |
> | `approval_node` | 审批节点表（支持条件表达式、签批模式、超时配置）⭐ 核心 |
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

> ### 环境要求
>
> - Java 21+
> - MySQL 8.0+
> - Node.js 18+ / pnpm

### 1. データベース作成

```sql
CREATE DATABASE smartoa DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

その後、`docs/` 配下の SQL スクリプトを順にインポートします。

> ### 1. 建库
>
> 然后依次导入 `docs/` 下的 SQL 脚本。

### 2. バックエンド起動

```bash
cd backend && ./mvnw spring-boot:run
```

デフォルトポート `8080`。

> ### 2. 启动后端
>
> 默认端口 `8080`。

### 3. フロントエンド起動

```bash
cd frontend
pnpm install
pnpm run dev
```

デフォルトポート `5173`、バックエンドへのプロキシ転送設定済み。

> ### 3. 启动前端
>
> 默认端口 `5173`，已配置代理转发到后端。

### 4. ログイン

ブラウザで `http://localhost:5173` を開き、以下のアカウントでログインします：

| ユーザー名 | パスワード | ロール | 説明 |
|--------|------|------|------|
| admin | 123456 | MANAGER | 技術部マネージャー（直属上司なし） |
| zhangsan | 123456 | EMPLOYEE | 一般社員（直属上司=admin） |
| lisi | 123456 | EMPLOYEE | 製品部社員 |

> ### 4. 登录
>
> | 用户名 | 密码 | 角色 | 说明 |
> |--------|------|------|------|
> | admin | 123456 | MANAGER | 技术部经理（无直属领导） |
> | zhangsan | 123456 | EMPLOYEE | 普通员工（直属领导=admin） |
> | lisi | 123456 | EMPLOYEE | 产品部员工 |

---

## 実装済み機能

### P0 基本機能

- [x] ユーザーログイン（JWT + BCrypt + ロール区別）
- [x] 承認テンプレート CRUD
- [x] 休暇申請の提出
- [x] ハードコードされた二段階承認フロー
- [x] 自分の未処理 / 処理済み / 提出した申請
- [x] 承認詳細ページ

### P1 アップグレード機能

- [x] 8テーブルのデータベース設計
- [x] 設定可能な多段階承認エンジン（approval_node テーブル駆動、動的ノード巡回）
- [x] 承認 / 却下 / 取下げ / 転送の4操作
- [x] 承認ノード設定 UI（テンプレート編集時にノードの追加/削除/ドラッグ＆ドロップ並べ替え）
- [x] フロー進行状況バー（`el-steps`、動的ノードステータス）
- [x] 承認履歴タイムライン（`el-timeline`、操作タイプを色分け表示）
- [x] ECharts 統計グラフ
- [x] バックエンド Excel エクスポート（Apache POI）
- [x] `Result<T>` 統一レスポンス + BusinessException グローバル例外処理

### P2 アップグレード機能

- [x] **フローノードビジュアルエディタ** — ドラッグ＆ドロップ並べ替え、動的ノード追加/削除
- [x] **条件分岐** — SpEL 式駆動（休暇日数 `days`、休暇タイプ `leaveType` などによる条件分岐をサポート）
- [x] **並行承認** — 単人（SINGLE）/ 会簽（COUNTER_SIGN）/ 或簽（OR_SIGN）の3種類の承認モード
- [x] **タイムアウト自動エスカレーション** — ESCALATE（転送）/ AUTO_APPROVE（自動承認）/ AUTO_REJECT（自動却下）、`@Scheduled` で5分ごとにチェック
- [x] **滞留修復** — `repairStuckRequests()` で `currentApproverId` が null の異常滞留申請を修復

### テスト

- [x] **ユニットテスト** — JUnit 5 + Spring Boot Test、24件のテストケース
- [x] **承認フローテスト** — 申請提出、承認、却下、取下げ、転送、照会を網羅
- [x] **異常系テスト** — 権限超越、重複操作、不正パラメータの検証
- [x] **データ整合性** — `@Transactional` によるテスト後の自動ロールバック

> ### P0 基础功能
>
> - [x] 用户登录（JWT + BCrypt + 角色区分）
> - [x] 审批模板 CRUD
> - [x] 请假申请提交
> - [x] 硬编码二级审批流转
> - [x] 我的待办 / 已办 / 我提交的
> - [x] 审批详情页

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

> ### P2 升级功能
>
> - [x] **流程节点可视化编辑器** — 拖拽排序、动态添加/删除节点
> - [x] **条件分支** — SpEL 表达式驱动（支持按请假天数 `days`、请假类型 `leaveType` 等条件分流）
> - [x] **并行审批** — 单人（SINGLE）/ 会签（COUNTER_SIGN）/ 或签（OR_SIGN）三种签批模式
> - [x] **超时自动升级** — ESCALATE（转派）/ AUTO_APPROVE（自动通过）/ AUTO_REJECT（自动驳回），`@Scheduled` 每 5 分钟检查
> - [x] **滞留修复** — `repairStuckRequests()` 修复 `currentApproverId` 为 null 的异常滞留申请

> ### 测试
>
> - [x] **单元测试** — JUnit 5 + Spring Boot Test，24 个测试用例
> - [x] **审批流程测试** — 覆盖提交、审批、驳回、撤回、转派、查询
> - [x] **异常测试** — 验证越权操作、重复操作、非法参数
> - [x] **数据一致性** — 使用 `@Transactional` 测试后自动回滚

---

## API 概要

| メソッド | パス | 説明 |
|------|------|------|
| POST | `/api/login` | ログイン |
| GET | `/api/users` | ユーザー一覧 |
| GET | `/api/templates` | テンプレート一覧 |
| POST | `/api/templates` | テンプレート作成 |
| GET | `/api/templates/{id}/nodes` | 承認ノード取得 |
| POST | `/api/templates/{id}/nodes` | 承認ノード保存 |
| POST | `/api/leave/submit` | 休暇申請提出 |
| POST | `/api/leave/approve` | 休暇承認 |
| POST | `/api/leave/{id}/withdraw` | 取下げ |
| POST | `/api/leave/{id}/transfer` | 転送 |
| GET | `/api/leave/pending` | 未承認一覧 |
| GET | `/api/leave/done` | 承認済み一覧 |
| GET | `/api/leave/my-requests` | 自分の申請 |
| GET | `/api/leave/{id}` | 休暇申請詳細 |
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
