# SmartOA — シンプルOA承認フロー管理システム

Spring Boot + Vue 3 + MyBatis-Plus、休暇承認フローに焦点を当てたシステム。

---

## 技術スタック

- バックエンド：Spring Boot 3.5.14 + Java 21 + MyBatis-Plus 3.5.15 + MySQL
- フロントエンド：Vue 3.5.34 + Vite 8.0 + Element Plus 2.13.7 + Pinia 3.0.4 + Axios 1.15.2 + pnpm
- 認証：JWT (jjwt 0.13.0) + BCrypt (spring-security-crypto)

## 起動方法

```bash
# バックエンド
cd backend && ./mvnw spring-boot:run          # → localhost:8080

# フロントエンド
cd frontend && pnpm run dev      # → localhost:5173（proxy /api → 8080）
```

## 現在の進捗

- **P0/P1 完了**：JWT認証、設定可能な承認エンジン（approval_node テーブル駆動）、6テーブルアーキテクチャ、4承認アクション（同意/却下/撤回/転送）、Excelエクスポート、ECharts統計
- **P2 完了**：
  - フローノード可視化エディタ（ドラッグ&ドロップ並べ替え、動的追加/削除）
  - 条件分岐（SpEL式、休暇日数/種類などの条件によるルーティングをサポート）
  - 並行承認 — 全員承認（COUNTER_SIGN）+ いずれか承認（OR_SIGN）+ 単独承認（SINGLE）
  - タイムアウト自動エスカレーション（ESCALATE/AUTO_APPROVE/AUTO_REJECT、@Scheduled で5分ごとにチェック）
- **P3 未着手**：動的フォームレンダリング、フロントエンドExcelエクスポート、モバイル対応

## データベースマイグレーション

| スクリプト | 説明 |
|------|------|
| `docs/mysql-p0-upgrade.sql` | データベース・テーブル作成 + シードデータ |
| `docs/mysql-p3-bcrypt.sql` | パスワードをBCryptにアップグレード |
| `docs/mysql-p4-parallel.sql` | 並行承認（sign_type + approval_task テーブル） |
| `docs/mysql-p5-timeout.sql` | タイムアウト自動エスカレーション（timeout_hours + timeout_time） |

## テストアカウント

| ユーザー名 | パスワード | 役割 | 説明 |
|--------|------|------|------|
| admin | 123456 | MANAGER | 技術部マネージャー |
| zhangsan | 123456 | EMPLOYEE | 一般社員（直属上司=admin） |

## ブランチ説明

| ブランチ | 言語 | 用途 |
|------|------|------|
| `master` | 中国語 | Gitee |
| `github` | 日中バイリンガル | GitHub、日本面接用 |
| `github-cn` | 中国語のみ | GitHub、中国国内面接/学校用 |