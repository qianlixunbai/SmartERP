Here is the complete bilingual (Japanese + Chinese) translated markdown:

```markdown
# システムアーキテクチャ設計書

> <span style="color: #888888;"># 系统架构设计文档</span>

## SmartOA 承認フロー管理システム

> <span style="color: #888888;">## SmartOA 审批流管理系统</span>

---

## 1. アーキテクチャ概要

> <span style="color: #888888;">## 1. 架构概览</span>

```
┌─────────────────────────────────────────────────────────┐
│                      ブラウザ (Client)                     │
│              Vue 3 + Element Plus + ECharts              │
└──────────────────────┬──────────────────────────────────┘
                       │ HTTP / JSON
                       │ JWT Bearer Token
┌──────────────────────▼──────────────────────────────────┐
│                  Spring Boot 3.5.14                        │
│  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐  │
│  │ Controller│──│  Service  │──│  Mapper (MyBatis-Plus) │  │
│  │  (REST)  │  │ (Business)│  │     (Data Access)     │  │
│  └──────────┘  └──────────┘  └──────────┬───────────┘  │
│                                         │               │
│  ┌──────────┐  ┌───────────────┐        │               │
│  │ JwtFilter │  │TimeoutScheduler│       │               │
│  │  (Auth)  │  │ (@Scheduled)  │        │               │
│  └──────────┘  └───────────────┘        │               │
└─────────────────────────────────────────┼───────────────┘
                                          │ JDBC
┌─────────────────────────────────────────▼───────────────┐
│                     MySQL 8 (smartoa)                     │
│   sys_user │ approval_template │ approval_node           │
│   leave_request │ approval_record │ template_field       │
│   approval_task (並行承認)                                  │
└─────────────────────────────────────────────────────────┘
```

> <span style="color: #888888;">```
> ┌─────────────────────────────────────────────────────────┐
> │                      浏览器 (Client)                      │
> │              Vue 3 + Element Plus + ECharts              │
> └──────────────────────┬──────────────────────────────────┘
>                        │ HTTP / JSON
>                        │ JWT Bearer Token
> ┌──────────────────────▼──────────────────────────────────┐
> │                  Spring Boot 3.5.14                        │
> │  ┌──────────┐  ┌──────────┐  ┌──────────────────────┐  │
> │  │ Controller│──│  Service  │──│  Mapper (MyBatis-Plus) │  │
> │  │  (REST)  │  │ (Business)│  │     (Data Access)     │  │
> │  └──────────┘  └──────────┘  └──────────┬───────────┘  │
> │                                         │               │
> │  ┌──────────┐  ┌───────────────┐        │               │
> │  │ JwtFilter │  │TimeoutScheduler│       │               │
> │  │  (Auth)  │  │ (@Scheduled)  │        │               │
> │  └──────────┘  └───────────────┘        │               │
> └─────────────────────────────────────────┼───────────────┘
>                                           │ JDBC
> ┌─────────────────────────────────────────▼───────────────┐
> │                     MySQL 8 (smartoa)                     │
> │   sys_user │ approval_template │ approval_node           │
> │   leave_request │ approval_record │ template_field       │
> │   approval_task (并行审批)                                │
> └─────────────────────────────────────────────────────────┘
> ```</span>

---

## 2. 技術選定

> <span style="color: #888888;">## 2. 技术选型</span>

| レイヤー | 技術 | バージョン | 選定理由 |
|---|---|---|---|
| バックエンドフレームワーク | Spring Boot | 3.5.14 | エンタープライズ Java 標準フレームワーク |
| ORM | MyBatis-Plus | 3.5.15 | 柔軟なクエリ + Lambda タイプセーフ + 自動 CRUD |
| データベース | MySQL | 8.x | 成熟安定、utf8mb4 対応 |
| 認証 | JJWT | 0.13.0 | 軽量 JWT 実装 |
| パスワード暗号化 | BCrypt | spring-security-crypto | 一方向ハッシュ、不可逆 |
| フロントエンドフレームワーク | Vue 3 | 3.5 | Composition API + リアクティブ |
| ビルドツール | Vite | 8.x | 高速 HMR、ESM ネイティブ |
| UI ライブラリ | Element Plus | 2.13.7 | エンタープライズ Vue 3 コンポーネントライブラリ |
| 状態管理 | Pinia | 3.x | Vue 3 公式推奨 |
| チャート | ECharts | 5.x | 機能豊富、中国語対応 |
| Excel | Apache POI | 5.3 | Java Excel 読み書き標準ライブラリ |
| パッケージ管理 | pnpm | — | 高速、ディスク容量節約 |

> <span style="color: #888888;">| 层级 | 技术 | 版本 | 选型理由 |
> |---|---|---|---|
> | 后端框架 | Spring Boot | 3.5.14 | 企业级 Java 标准框架 |
> | ORM | MyBatis-Plus | 3.5.15 | 灵活查询 + Lambda 类型安全 + 自动 CRUD |
> | 数据库 | MySQL | 8.x | 成熟稳定，支持 utf8mb4 |
> | 认证 | JJWT | 0.13.0 | 轻量 JWT 实现 |
> | 密码加密 | BCrypt | spring-security-crypto | 单向哈希，不可逆 |
> | 前端框架 | Vue 3 | 3.5 | Composition API + 响应式 |
> | 构建工具 | Vite | 8.x | 极速 HMR，ESM 原生 |
> | UI 库 | Element Plus | 2.13.7 | 企业级 Vue 3 组件库 |
> | 状态管理 | Pinia | 3.x | Vue 3 官方推荐 |
> | 图表 | ECharts | 5.x | 功能全面，中文友好 |
> | Excel | Apache POI | 5.3 | Java Excel 读写标准库 |
> | 包管理 | pnpm | — | 快速、节省磁盘空间 |</span>

---

## 3. パッケージ構造

> <span style="color: #888888;">## 3. 包结构</span>

### 3.1 バックエンド（backend/src/main/java/com/smartoa/）

> <span style="color: #888888;">### 3.1 后端（backend/src/main/java/com/smartoa/）</span>

```
com.smartoa
├── SmartoaApplication.java       # 起動クラス + @MapperScan + @EnableScheduling
├── common/
│   ├── Result.java                # 統一レスポンス {code, message, data}
│   ├── BusinessException.java     # ビジネス例外
│   └── GlobalExceptionHandler.java # @RestControllerAdvice グローバル例外処理
├── config/
│   ├── JwtProperties.java        # JWT 秘密鍵+有効期限設定
│   ├── JwtUtil.java              # Token 生成/検証/解析ユーティリティ
│   ├── JwtFilter.java            # リクエスト認証フィルター
│   ├── UserContextHolder.java    # ThreadLocal で現在のユーザーIDを保持
│   └── WebConfig.java            # CORS 設定 + Filter 登録
├── controller/
│   ├── UserController.java       # ログイン/ログアウト/ユーザー一覧
│   ├── LeaveController.java      # 休暇申請/承認/却下/差し戻し/転送/滞留修復/並行タスク
│   ├── TemplateController.java   # テンプレートCRUD/ノード/フィールド
│   ├── StatsController.java      # 統計データ
│   └── ExportController.java     # Excel エクスポート
├── service/
│   ├── UserService.java          # ユーザービジネスロジック
│   ├── LeaveService.java         # 承認エンジンコア（並行+タイムアウト+条件分岐含む）
│   ├── TemplateService.java      # テンプレート+ノード+フィールド管理
│   ├── StatsService.java         # 統計計算
│   ├── ExportService.java        # Excel 生成
│   └── TimeoutScheduler.java     # タイムアウト定期チェック（@Scheduled 5分）
├── entity/
│   ├── User.java                 # ユーザーエンティティ (sys_user)
│   ├── ApprovalTemplate.java     # テンプレートエンティティ (approval_template)
│   ├── ApprovalNode.java         # ノードエンティティ (approval_node) — 承認モード+条件+タイムアウト含む
│   ├── TemplateField.java        # フィールドエンティティ (template_field)
│   ├── LeaveRequest.java         # 申請エンティティ (leave_request) — timeoutTime 含む
│   ├── ApprovalRecord.java       # レコードエンティティ (approval_record)
│   └── ApprovalTask.java         # 並行承認タスクエンティティ (approval_task)
├── mapper/
│   ├── UserMapper.java
│   ├── LeaveRequestMapper.java
│   ├── ApprovalRecordMapper.java
│   ├── ApprovalTemplateMapper.java
│   ├── ApprovalNodeMapper.java
│   ├── TemplateFieldMapper.java
│   └── ApprovalTaskMapper.java
└── dto/
    ├── LoginDTO.java              # ログインリクエストボディ
    └── LeaveSubmitDTO.java        # 休暇申請リクエストボディ
```

> <span style="color: #888888;">```
> com.smartoa
> ├── SmartoaApplication.java       # 启动类 + @MapperScan + @EnableScheduling
> ├── common/
> │   ├── Result.java                # 统一响应 {code, message, data}
> │   ├── BusinessException.java     # 业务异常
> │   └── GlobalExceptionHandler.java # @RestControllerAdvice 全局异常处理
> ├── config/
> │   ├── JwtProperties.java        # JWT 密钥+过期时间配置
> │   ├── JwtUtil.java              # Token 生成/验证/解析工具
> │   ├── JwtFilter.java            # 请求认证过滤器
> │   ├── UserContextHolder.java    # ThreadLocal 保存当前用户 ID
> │   └── WebConfig.java            # CORS 配置 + Filter 注册
> ├── controller/
> │   ├── UserController.java       # 登录/登出/用户列表
> │   ├── LeaveController.java      # 请假申请/审批/撤回/转派/滞留修复/并行任务
> │   ├── TemplateController.java   # 模板CRUD/节点/字段
> │   ├── StatsController.java      # 统计数据
> │   └── ExportController.java     # Excel 导出
> ├── service/
> │   ├── UserService.java          # 用户业务逻辑
> │   ├── LeaveService.java         # 审批引擎核心（含并行+超时+条件分支）
> │   ├── TemplateService.java      # 模板+节点+字段管理
> │   ├── StatsService.java         # 统计计算
> │   ├── ExportService.java        # Excel 生成
> │   └── TimeoutScheduler.java     # 超时定时检查（@Scheduled 5分钟）
> ├── entity/
> │   ├── User.java                 # 用户实体 (sys_user)
> │   ├── ApprovalTemplate.java     # 模板实体 (approval_template)
> │   ├── ApprovalNode.java         # 节点实体 (approval_node) — 含签批模式+条件+超时
> │   ├── TemplateField.java        # 字段实体 (template_field)
> │   ├── LeaveRequest.java         # 申请实体 (leave_request) — 含 timeoutTime
> │   ├── ApprovalRecord.java       # 记录实体 (approval_record)
> │   └── ApprovalTask.java         # 并行审批任务实体 (approval_task)
> ├── mapper/
> │   ├── UserMapper.java
> │   ├── LeaveRequestMapper.java
> │   ├── ApprovalRecordMapper.java
> │   ├── ApprovalTemplateMapper.java
> │   ├── ApprovalNodeMapper.java
> │   ├── TemplateFieldMapper.java
> │   └── ApprovalTaskMapper.java
> └── dto/
>     ├── LoginDTO.java              # 登录请求体
>     └── LeaveSubmitDTO.java        # 请假提交请求体
> ```</span>

### 3.2 フロントエンド（frontend/src/）

> <span style="color: #888888;">### 3.2 前端（frontend/src/）</span>

```
src
├── App.vue                        # ルートコンポーネント + グローバル provide
├── main.js                        # エントリーポイント：Pinia + Router + Element Plus
├── api/
│   ├── index.js                   # axios インスタンス + インターセプター
│   ├── auth.js                    # 認証インターフェース
│   ├── leave.js                   # 休暇インターフェース（tasks/repair 含む）
│   ├── template.js                # テンプレートインターフェース
│   └── stats.js                   # 統計インターフェース
├── stores/
│   ├── auth.js                    # 認証状態（token/user/role）
│   ├── approval.js                # 承認データ状態（currentTasks 含む）
│   └── users.js                   # ユーザー一覧 + ID→氏名マッピング
├── router/
│   └── index.js                   # ルート設定 + ナビゲーションガード
├── layouts/
│   └── MainLayout.vue             # メインレイアウト（Header + Sidebar + Content）
├── components/
│   ├── AppHeader.vue              # トップバー（ユーザー情報/ログアウト）
│   ├── AppSidebar.vue             # サイドナビゲーション
│   ├── ApprovalTimeline.vue       # 承認タイムライン
│   └── StatusTag.vue              # ステータスタグ
├── views/
│   ├── LoginPage.vue              # ログインページ
│   ├── SubmitApplicationPage.vue  # 申請提出ページ
│   ├── MyApprovalsPage.vue        # 自分の承認ページ
│   ├── ApprovalDetailPage.vue     # 承認詳細ページ（並行承認者タグ+タイムアウト警告含む）
│   ├── TemplateListPage.vue       # テンプレート一覧ページ
│   ├── TemplateEditPage.vue       # テンプレート編集ページ（ビジュアルフローエディタ含む）
│   └── StatsPage.vue              # 統計レポートページ
└── utils/
    └── constants.js               # フロントエンド定数（状態/アクション/承認モード/タイムアウトアクションのマッピング）
```

> <span style="color: #888888;">```
> src
> ├── App.vue                        # 根组件 + 全局 provide
> ├── main.js                        # 入口：Pinia + Router + Element Plus
> ├── api/
> │   ├── index.js                   # axios 实例 + 拦截器
> │   ├── auth.js                    # 认证接口
> │   ├── leave.js                   # 请假接口（含 tasks/repair）
> │   ├── template.js                # 模板接口
> │   └── stats.js                   # 统计接口
> ├── stores/
> │   ├── auth.js                    # 认证状态（token/user/role）
> │   ├── approval.js                # 审批数据状态（含 currentTasks）
> │   └── users.js                   # 用户列表 + ID→姓名映射
> ├── router/
> │   └── index.js                   # 路由配置 + 导航守卫
> ├── layouts/
> │   └── MainLayout.vue             # 主布局（Header + Sidebar + Content）
> ├── components/
> │   ├── AppHeader.vue              # 顶部栏（用户信息/登出）
> │   ├── AppSidebar.vue             # 侧栏导航
> │   ├── ApprovalTimeline.vue       # 审批时间线
> │   └── StatusTag.vue              # 状态标签
> ├── views/
> │   ├── LoginPage.vue              # 登录页
> │   ├── SubmitApplicationPage.vue  # 提交申请页
> │   ├── MyApprovalsPage.vue        # 我的审批页
> │   ├── ApprovalDetailPage.vue     # 审批详情页（含并行审批人标签+超时警告）
> │   ├── TemplateListPage.vue       # 模板列表页
> │   ├── TemplateEditPage.vue       # 模板编辑页（含可视化流程编辑器）
> │   └── StatsPage.vue              # 统计报表页
> └── utils/
>     └── constants.js               # 前端常量（状态/动作/签批模式/超时动作映射）
> ```</span>

---

## 4. コア設計

> <span style="color: #888888;">## 4. 核心设计</span>

### 4.1 承認エンジンフロー

> <span style="color: #888888;">### 4.1 审批引擎流程</span>

```
┌──────────┐     ┌──────────────┐     ┌──────────────────┐
│ 申請提出   │────▶│ テンプレートノード │────▶│ 条件分岐評価(SpEL)  │
│          │     │ 読み込み      │     │                  │
└──────────┘     └──────────────┘     └────────┬─────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                │ 承認者解決 (resolveApprovers) │
                                │ ├─ SINGLE → 単独承認者         │
                                │ ├─ COUNTER_SIGN → 複数人リスト  │
                                │ └─ OR_SIGN → 複数人リスト      │
                                └──────────────┬──────────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                │ SINGLE: currentApproverId 設定 │
                                │ 並行: approval_task を一括挿入   │
                                │ timeoutTime を設定（設定がある場合）│
                                └──────────────┬──────────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                │ 承認者操作                     │
                                │ ├─ APPROVE → 進行/並行チェック   │
                                │ ├─ REJECT → 終了+並行タスクスキップ │
                                │ ├─ WITHDRAW → 終了 (申請者)    │
                                │ └─ TRANSFER → 承認者置換(SINGLE) │
                                └──────────────┬──────────────┘
                                               │
                                ┌──────────────▼──────────────┐
                                │ 次のノードあり？               │
                                │ ├─ はい → 遷移、承認待ち継続    │
                                │ └─ いいえ → APPROVED、フロー終了 │
                                └─────────────────────────────┘
```

> <span style="color: #888888;">```
> ┌──────────┐     ┌──────────────┐     ┌──────────────────┐
> │ 提交申请  │────▶│ 读取模板节点  │────▶│ 评估条件分支(SpEL) │
> └──────────┘     └──────────────┘     └────────┬─────────┘
>                                                │
>                                 ┌──────────────▼──────────────┐
>                                 │ 解析审批人 (resolveApprovers) │
>                                 │ ├─ SINGLE → 单人             │
>                                 │ ├─ COUNTER_SIGN → 多人列表    │
>                                 │ └─ OR_SIGN → 多人列表        │
>                                 └──────────────┬──────────────┘
>                                                │
>                                 ┌──────────────▼──────────────┐
>                                 │ SINGLE: 设 currentApproverId  │
>                                 │ 并行: 批量插入 approval_task   │
>                                 │ 设置 timeoutTime（如有配置）   │
>                                 └──────────────┬──────────────┘
>                                                │
>                                 ┌──────────────▼──────────────┐
>                                 │ 审批人操作                     │
>                                 │ ├─ APPROVE → 推进/检查并行     │
>                                 │ ├─ REJECT → 终止+跳过并行任务  │
>                                 │ ├─ WITHDRAW → 终止 (申请人)   │
>                                 │ └─ TRANSFER → 替换审批人(SINGLE)│
>                                 └──────────────┬──────────────┘
>                                                │
>                                 ┌──────────────▼──────────────┐
>                                 │ 有下一节点？                  │
>                                 │ ├─ 是 → 流转，继续等待审批    │
>                                 │ └─ 否 → APPROVED，流程结束   │
>                                 └─────────────────────────────┘
> ```</span>

**主要メソッド：**

- `advanceToNextNode()` — 次の条件を満たすノードを検索し、承認者を解決し、SINGLE/並行モードを処理する
- `resolveApprovers()` — `approverType` + `signType` に基づいて承認者リストを解決する
  - `DIRECT_LEADER` → `applicant.directLeaderId`
  - `DEPARTMENT_HEAD` → `applicant.departmentHeadId`
  - `SPECIFIC_USER` → SINGLE: `node.approverId`；並行: `node.approverIds` カンマ区切り
- `evaluateCondition()` — SpEL 式評価、`leaveType`、`days`、`startDate`、`endDate` 変数をサポート
- `checkTimeouts()` — `timeoutTime` が期限切れの PENDING 申請をスキャン
- `processTimeout()` — タイムアウトアクションを実行（ESCALATE/AUTO_APPROVE/AUTO_REJECT）

> <span style="color: #888888;">**关键方法：**
> - `advanceToNextNode()` — 找下一个满足条件的节点，解析审批人，处理 SINGLE/并行模式
> - `resolveApprovers()` — 根据 `approverType` + `signType` 解析审批人列表
>   - `DIRECT_LEADER` → `applicant.directLeaderId`
>   - `DEPARTMENT_HEAD` → `applicant.departmentHeadId`
>   - `SPECIFIC_USER` → SINGLE: `node.approverId`；并行: `node.approverIds` 逗号分隔
> - `evaluateCondition()` — SpEL 表达式求值，支持 `leaveType`、`days`、`startDate`、`endDate` 变量
> - `checkTimeouts()` — 扫描 `timeoutTime` 过期的 PENDING 申请
> - `processTimeout()` — 执行超时动作（ESCALATE/AUTO_APPROVE/AUTO_REJECT）</span>

### 4.2 認証フロー

> <span style="color: #888888;">### 4.2 认证流程</span>

```
ユーザーログイン → POST /api/login → BCrypt パスワード検証 → JWT 生成
  │
  ▼
後続リクエスト → JwtFilter がインターセプト
  ├─ /api/login → 通過
  ├─ Authorization ヘッダーなし → 401
  ├─ Token 無効/期限切れ → 401
  ├─ ユーザー不在 → 401
  └─ 検証成功 → UserContextHolder.setUserId() → Controller
```

> <span style="color: #888888;">```
> 用户登录 → POST /api/login → BCrypt 验证密码 → 生成 JWT
>   │
>   ▼
> 后续请求 → JwtFilter 拦截
>   ├─ /api/login → 放行
>   ├─ 无 Authorization 头 → 401
>   ├─ Token 无效/过期 → 401
>   ├─ 用户不存在 → 401
>   └─ 验证通过 → UserContextHolder.setUserId() → Controller
> ```</span>

### 4.3 並行承認モード

> <span style="color: #888888;">### 4.3 并行审批模式</span>

| モード | 進行条件 | 説明 |
|---|---|---|
| SINGLE | 単独承認 | 既存動作を維持、`currentApproverId` が駆動 |
| COUNTER_SIGN | 全員同意 | 各承認者に `approval_task` を作成、全員 COMPLETED 後に進行 |
| OR_SIGN | いずれか同意 | いずれかが APPROVE → 進行、残りのタスクは SKIPPED |

> <span style="color: #888888;">| 模式 | 推进条件 | 说明 |
> |---|---|---|
> | SINGLE | 单人审批 | 保持现有行为，`currentApproverId` 驱动 |
> | COUNTER_SIGN | 全部同意 | 每个审批人创建 `approval_task`，全部 COMPLETED 后推进 |
> | OR_SIGN | 任一同意 | 任一人 APPROVE → 推进，其余任务 SKIPPED |</span>

### 4.4 タイムアウト自動エスカレーション

> <span style="color: #888888;">### 4.4 超时自动升级</span>

```
TimeoutScheduler (@Scheduled 5min)
  → LeaveService.checkTimeouts()
    → 検索 status=PENDING AND timeout_time < NOW()
      → processTimeout()
        ├─ ESCALATE → 転送またはノードスキップ
        ├─ AUTO_APPROVE → 自動承認
        └─ AUTO_REJECT → 自動却下
```

> <span style="color: #888888;">```
> TimeoutScheduler (@Scheduled 5min)
>   → LeaveService.checkTimeouts()
>     → 查询 status=PENDING AND timeout_time < NOW()
>       → processTimeout()
>         ├─ ESCALATE → 转派或跳过节点
>         ├─ AUTO_APPROVE → 自动通过
>         └─ AUTO_REJECT → 自动驳回
> ```</span>

### 4.5 フロントエンド氏名マッピング方式

> <span style="color: #888888;">### 4.5 前端姓名映射方案</span>

```
バックエンドの返却データは ID のみ（applicantId, approverId）
       │
       ▼
App.vue provide('getUserName', id => userStore.getUserName(id))
       │
       ▼
各コンポーネント inject('getUserName') → ID を氏名に変換して表示
```

> <span style="color: #888888;">```
> 后端返回数据只含 ID（applicantId, approverId）
>        │
>        ▼
> App.vue provide('getUserName', id => userStore.getUserName(id))
>        │
>        ▼
> 各组件 inject('getUserName') → ID 转姓名显示
> ```</span>

---

## 5. セキュリティ設計

> <span style="color: #888888;">## 5. 安全设计</span>

| 対策 | 説明 |
|---|---|
| JWT 認証 | すべての /api/* インターフェース（/api/login を除く）に Bearer Token が必要 |
| パスワード暗号化 | BCrypt 一方向ハッシュ、不可逆 |
| パスワード保護 | @JsonIgnore でパスワードがフロントエンドに返却されるのを防止 |
| CORS | http://localhost:5173 のクロスオリジンのみ許可 |
| 権限チェック | テンプレート管理/エクスポートインターフェースは role=MANAGER をチェック |
| 操作検証 | 取り消しは申請者を検証、承認は現在の承認者/並行タスクを検証 |
| 統一レスポンス | `Result<T>` {code, message, data}、グローバル例外処理 |

> <span style="color: #888888;">| 措施 | 说明 |
> |---|---|
> | JWT 认证 | 所有 /api/* 接口（除 /api/login）需 Bearer Token |
> | 密码加密 | BCrypt 单向哈希，不可逆 |
> | 密码保护 | @JsonIgnore 防止密码序列化返回前端 |
> | CORS | 仅允许 http://localhost:5173 跨域 |
> | 权限校验 | 模板管理/导出接口检查 role=MANAGER |
> | 操作校验 | 撤回校验申请人，审批校验当前审批人/并行任务 |
> | 统一响应 | `Result<T>` {code, message, data}，全局异常处理 |</span>

---

## 6. デプロイアーキテクチャ

> <span style="color: #888888;">## 6. 部署架构</span>

```
┌────────────────────────────────────┐
│  開発環境 (単一マシン)                │
│  ┌────────────┐  ┌────────────┐    │
│  │ Vite Dev   │  │ Spring Boot│    │
│  │ :5173      │──│ :8080      │    │
│  └────────────┘  └─────┬──────┘    │
│                        │            │
│                 ┌──────▼──────┐     │
│                 │  MySQL :3306│     │
│                 └─────────────┘     │
└────────────────────────────────────┘
```

> <span style="color: #888888;">```
> ┌────────────────────────────────────┐
> │  开发环境 (单机)                     │
> │  ┌────────────┐  ┌────────────┐    │
> │  │ Vite Dev   │  │ Spring Boot│    │
> │  │ :5173      │──│ :8080      │    │
> │  └────────────┘  └─────┬──────┘    │
> │                        │            │
> │                 ┌──────▼──────┐     │
> │                 │  MySQL :3306│     │
> │                 └─────────────┘     │
> └────────────────────────────────────┘
> ```</span>

| コンポーネント | ポート | 起動コマンド |
|---|---|---|
| フロントエンド | 5173 | `pnpm run dev` |
| バックエンド | 8080 | `cd backend && ./mvnw spring-boot:run` |
| データベース | 3306 | MySQL サービス |

> <span style="color: #888888;">| 组件 | 端口 | 启动命令 |
> |---|---|---|
> | 前端 | 5173 | `pnpm run dev` |
> | 后端 | 8080 | `cd backend && ./mvnw spring-boot:run` |
> | 数据库 | 3306 | MySQL 服务 |</span>

---

> ドキュメントバージョン：v2.0（P2 完了） | 更新日：2026-05-27

> <span style="color: #888888;">> 文档版本：v2.0（P2 完成） | 更新日期：2026-05-27</span>
```