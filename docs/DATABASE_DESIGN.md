# データベース設計書

## SmartOA 承認フロー管理システム

---

| 項目 | 値 |
|---|---|
| 照合順序 | utf8mb4_unicode_ci |

## 1. データベース概要
| 項目 | 値 |
|---|---|
| データベース名 | smartoa |
| 文字セット | utf8mb4 |
| 照合順序 | utf8mb4_unicode_ci |
| ストレージエンジン | InnoDB |
| テーブル数 | 13 |

---

## 2. ER 図（テキスト版）
```
sys_user (ユーザー)
  │
  ├─(applicant_id)── leave_request (休暇申請)
  │                      │
  │                      ├─(template_id)── approval_template (承認テンプレート)
  │                      │                      │
  │                      │                      ├─(1:N)── approval_node (承認ノード)
  │                      │                      └─(1:N)── template_field (テンプレートフィールド)
  │                      │
  │                      ├─(1:N)── approval_record (承認記録)
  │                      │              │
  │                      │              └─(node_id)── approval_node
  │                      │
  │                      └─(1:N)── approval_task (並行承認タスク)
  │                                      │
  │                                      └─(node_id)── approval_node
  │
  └─(direct_leader_id / department_head_id)── sys_user (組織階層)
```

---

## 3. テーブル定義

### 3.1 sys_user（ユーザーテーブル）
| フィールド | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | ユーザー ID |
| username | VARCHAR(50) | NOT NULL, UNIQUE | ログインユーザー名 |
| password | VARCHAR(100) | NOT NULL | ログインパスワード（BCrypt 暗号化） |
| real_name | VARCHAR(50) | NOT NULL | 氏名 |
| role | VARCHAR(20) | NOT NULL | ロール：EMPLOYEE / MANAGER |
| department | VARCHAR(50) | | 所属部門 |
| direct_leader_id | BIGINT | FK → sys_user.id | 直属上司 ID |
| department_head_id | BIGINT | FK → sys_user.id | 部門責任者 ID |

**シードデータ：**
| id | username | real_name | role | department | direct_leader_id | department_head_id |
|---|---|---|---|---|---|---|
| 1 | admin | 王经理 | MANAGER | 技術部 | NULL | 4 |
| 2 | zhangsan | 張三 | EMPLOYEE | 技術部 | 1 | 4 |
| 3 | lisi | 李四 | EMPLOYEE | 製品部 | 1 | 5 |
| 4 | zongjian1 | 張総監 | MANAGER | 技術部 | NULL | NULL |
| 5 | zongjian2 | 李総監 | MANAGER | 製品部 | NULL | NULL |

**組織階層イメージ：**
```
張総監(4)                   李総監(5)
  └─ 王经理(1) ─┐             └─ 李四(3)
       ├─ 張三(2)
```

---

### 3.2 approval_template（承認テンプレートテーブル）
| フィールド | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | テンプレート ID |
| name | VARCHAR(100) | NOT NULL | テンプレート名 |
| description | VARCHAR(500) | | テンプレート説明 |
| enabled | BIT | NOT NULL, DEFAULT 1 | 有効フラグ |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新日時 |

---

### 3.3 approval_node（承認ノードテーブル）⭐ コア
| フィールド | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | ノード ID |
| template_id | BIGINT | NOT NULL, FK → approval_template.id | 所属テンプレート |
| node_name | VARCHAR(100) | NOT NULL | ノード名 |
| sort_order | INT | NOT NULL, DEFAULT 0 | 並び順（0 起点） |
| approver_type | VARCHAR(30) | NOT NULL | 承認者タイプ |
| approver_id | BIGINT | | 指定承認者 ID（SPECIFIC_USER + SINGLE） |
| condition_expression | VARCHAR(500) | | SpEL 条件式（P2） |
| sign_type | VARCHAR(20) | NOT NULL, DEFAULT 'SINGLE' | 署名モード（P2） |
| approver_ids | VARCHAR(1000) | | 並行承認者 ID リスト、カンマ区切り（P2） |
| timeout_hours | INT | | タイムアウト時間数（P2） |
| timeout_action | VARCHAR(30) | | タイムアウトアクション（P2） |
| escalate_to_user_id | BIGINT | | タイムアウト時エスカレーション先ユーザー ID（P2） |
| create_time | DATETIME | NOT NULL | 作成日時 |
| update_time | DATETIME | NOT NULL | 更新日時 |

**一意制約：** `(template_id, sort_order)`

**承認者タイプ列挙：**
| 値 | 説明 |
|---|---|
| DIRECT_LEADER | 申請者の直属上司 |
| DEPARTMENT_HEAD | 申請者の所属部門の責任者 |
| SPECIFIC_USER | 指定ユーザー |

**署名モード列挙（P2）：**
| 値 | 説明 |
|---|---|
| SINGLE | 単独承認（デフォルト） |
| COUNTER_SIGN | 会签 — 全承認者の同意が必要 |
| OR_SIGN | 或签 — いずれかの承認者の同意で進行 |

**タイムアウトアクション列挙（P2）：**
| 値 | 説明 |
|---|---|
| ESCALATE | escalate_to_user_id にエスカレーション、またはノードをスキップ |
| AUTO_APPROVE | 現在のノードを自動承認 |
| AUTO_REJECT | 自動却下、フロー終了 |

---

### 3.4 template_field（テンプレートフォームフィールドテーブル）
| フィールド | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | フィールド ID |
| template_id | BIGINT | NOT NULL, FK → approval_template.id | 所属テンプレート |
| field_name | VARCHAR(50) | NOT NULL | フィールド名（英字） |
| field_label | VARCHAR(100) | NOT NULL | 表示名（中国語） |
| field_type | VARCHAR(30) | NOT NULL | フィールドタイプ |
| required | BIT | NOT NULL, DEFAULT 1 | 必須フラグ |
| sort_order | INT | NOT NULL, DEFAULT 0 | 並び順 |
| options | VARCHAR(500) | | SELECT タイプの JSON オプション |
| create_time | DATETIME | NOT NULL | 作成日時 |

**一意制約：** `(template_id, field_name)`

**フィールドタイプ列挙：** TEXT / DATE / SELECT / TEXTAREA / NUMBER

---

### 3.5 leave_request（休暇申請テーブル）
| フィールド | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 申請 ID |
| applicant_id | BIGINT | NOT NULL, FK → sys_user.id | 申請者 ID |
| template_id | BIGINT | FK → approval_template.id | 関連承認テンプレート |
| leave_type | VARCHAR(20) | NOT NULL | 休暇タイプ |
| start_date | DATE | NOT NULL | 開始日 |
| end_date | DATE | NOT NULL | 終了日 |
| reason | VARCHAR(500) | | 休暇理由 |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | ステータス |
| current_node_id | BIGINT | FK → approval_node.id | 現在の承認ノード ID |
| current_approver_id | BIGINT | FK → sys_user.id | 現在の承認者 ID（SINGLE モード） |
| timeout_time | DATETIME | | タイムアウト期限（P2） |
| create_time | DATETIME | NOT NULL | 提出日時 |
| update_time | DATETIME | NOT NULL | 更新日時 |

**ステータス列挙：**
| 値 | 説明 |
|---|---|
| PENDING | 承認中 |
| APPROVED | 承認済み |
| REJECTED | 却下済み |
| WITHDRAWN | 取下げ済み |

---

### 3.6 approval_record（承認記録テーブル）
| フィールド | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | 記録 ID |
| leave_request_id | BIGINT | NOT NULL, FK → leave_request.id | 関連申請 ID |
| approver_id | BIGINT | NOT NULL, FK → sys_user.id | 承認者 ID（0=システム自動操作） |
| action | VARCHAR(20) | NOT NULL | 操作タイプ |
| comment | VARCHAR(500) | | 承認コメント |
| node_id | BIGINT | FK → approval_node.id | 操作時のノード ID |
| create_time | DATETIME | NOT NULL | 操作日時 |

**操作タイプ列挙：**
| 値 | 説明 |
|---|---|
| APPROVE | 承認 |
| REJECT | 却下 |
| WITHDRAW | 取下げ |
| TRANSFER | 転送 |
| TIMEOUT_ESCALATE | タイムアウト時エスカレーション |
| TIMEOUT_APPROVE | タイムアウト時自動承認 |
| TIMEOUT_REJECT | タイムアウト時自動却下 |

---

### 3.7 approval_task（並行承認タスクテーブル — P2 新規追加）
| フィールド | 型 | 制約 | 説明 |
|---|---|---|---|
| id | BIGINT | PK, AUTO_INCREMENT | タスク ID |
| leave_request_id | BIGINT | NOT NULL, FK → leave_request.id | 関連申請 ID |
| node_id | BIGINT | NOT NULL, FK → approval_node.id | 関連ノード ID |
| approver_id | BIGINT | NOT NULL, FK → sys_user.id | 承認者 ID |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING' | タスクステータス |
| create_time | DATETIME | NOT NULL, DEFAULT CURRENT_TIMESTAMP | 作成日時 |
| update_time | DATETIME | NOT NULL, ON UPDATE CURRENT_TIMESTAMP | 更新日時 |

**一意制約：** `(leave_request_id, node_id, approver_id)`

**タスクステータス列挙：**
| 値 | 説明 |
|---|---|
| PENDING | 承認待ち |
| COMPLETED | 承認済み |
| SKIPPED | スキップ済み（他者の操作による） |

---

## 4. インデックス設計
| テーブル | インデックスタイプ | フィールド |
|---|---|---|
| sys_user | UNIQUE | username |
| approval_node | UNIQUE | (template_id, sort_order) |
| approval_node | FOREIGN KEY | template_id |
| template_field | UNIQUE | (template_id, field_name) |
| template_field | FOREIGN KEY | template_id |
| leave_request | FOREIGN KEY | applicant_id |
| leave_request | FOREIGN KEY | template_id |
| leave_request | FOREIGN KEY | current_node_id |
| leave_request | INDEX | (status, timeout_time) |
| approval_record | FOREIGN KEY | leave_request_id |
| approval_record | FOREIGN KEY | approver_id |
| approval_record | FOREIGN KEY | node_id |
| approval_task | UNIQUE | (leave_request_id, node_id, approver_id) |
| approval_task | FOREIGN KEY | leave_request_id |
| approval_task | FOREIGN KEY | node_id |

---

## 5. マイグレーションスクリプト
| スクリプト | 説明 |
|---|---|
| `docs/mysql-p0-upgrade.sql` | P0 初期化：DB作成 + 4テーブル + シードデータ |
| `docs/mysql-p1-upgrade.sql` | P1 アップグレード：approval_node + template_field + 新規フィールド |
| `docs/mysql-p2a-bcrypt.sql` | BCrypt パスワード移行 |
| `docs/mysql-p2b-parallel.sql` | P2 並行承認：sign_type + approver_ids + approval_task テーブル |
| `docs/mysql-p2c-timeout.sql` | P2 タイムアウトアップグレード：timeout_hours + timeout_action + escalate_to_user_id + timeout_time |
| `docs/mysql-p3-expense.sql` | v2.0 経費精算+複式簿記：account/journal_entry/expense_request/expense_approval_task/audit_log |

---

文書バージョン：v2.0（P2 完了） | 更新日：2026-05-27
