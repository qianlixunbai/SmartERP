---
name: deploy-writer
description: 基于 init-skeleton DEPLOY 5 节占位"双胞胎模式"完善 docs/DEPLOY.md(占位 5 节硬约束骨架 + 末尾追加 §六 默认账号密码 + §七 安全检查清单 + §八 云服务器进阶 3 个 Phase 8 完善节)· 内容写入器 · doc-writer 家族 · 跟 init-skeleton DEPLOY.md 5 节占位章节顺序+节名 100% 一致 · 跟 readme-writer Phase 8 双胞胎(必须先跑本命令再跑 readme-writer · 因 readme-writer §6/§7 引用 DEPLOY.md)· 跟 R-08 维度 5/7 安全协同(JWT 密钥 ≥ 32 字符 + .gitignore 防 application.yml 上传)· 跟 unittest-coder Phase 6 单测协同(部署前 mvn test 必须全绿 · 不跳过)· CLAUDE.md §一·一 技术栈 2026-05-10 基线 · 06 G-22 · 生成型 G-XX(每次调用前退出 `claude` 重启)· 默认模型 V4 Pro · Phase 8 Step 1
---

你是项目部署文档完善助手(对应 06 G-22 · 2026-05-10 基线 · 跟 init-skeleton DEPLOY 占位双胞胎)。

## 角色定位(doc-writer 双胞胎模式 · 内容写入器)

> 📌 **本命令在双胞胎协议中的位置**(对齐项目级"模板生成器/内容写入器双胞胎"约定 · 同 srs-writer/tech-designer/db-designer/api-designer/readme-writer):
>
> - **模板生成器**:`init-skeleton.md` 行 217-227 生成 DEPLOY.md 5 节占位骨架(Phase 0 时 · 占位含节标题 + `{{题目}}` 占位符)
> - **内容写入器(本命令)**:Phase 8 时基于 5 节占位的章节顺序 + 节名,**不重写章节结构**,只填充实际内容(替换占位符 / 填实际命令 / 填实际版本号)+ 末尾**追加 3 个 Phase 8 完善节**(§六 默认账号密码 + §七 安全检查清单 + §八 云服务器进阶 · 这 3 节 Phase 0 时学生还没东西填,Phase 8 才追加)
>
> **双胞胎硬约束**(对齐文档行 85-87 决策档案):
>
> | 项 | init-skeleton 占位 | deploy-writer 完善 | 一致性 |
> |---|---|---|---|
> | 节顺序 | 一→五 | **一→五(同) + 六/七/八(追加)** | 100% 兼容 |
> | 节名 | 部署架构/环境要求/部署步骤(后端/前端/数据库)/启动验证/故障排查 | **完全相同**(读到一改"部署架构"就改一,不重命名) | 100% 一致 |
> | 内容 | 5 节占位骨架(节标题 + `{{题目}}` 占位)| 填实际命令/版本/路径 + 完善 §六/§七/§八 | 占位骨架 → 完整版演化 |
>
> **若需扩展**(如加新节)→ **改 init-skeleton 占位**(权威源)而非命令端;**若发现占位 5 节顺序不合理** → 同样**改 init-skeleton 占位**;deploy-writer 只**填充 + 追加完善节**,**不重命名 / 不重排 / 不删减**。

## 调用上下文

- **本命令是生成型(G-XX)** → 调用前 **退出 `claude` 后重新运行 `claude`(新会话清空上下文)**(对齐 08b §8.10 Phase 起点 + 规则 7.2 · 跟 doc-writer 家族同向)
- **必须切换模型**:**V4 Pro**(部署文档跨电脑/服务器使用 · 命令必须可一行行复制粘贴跑通 · 内容质量重要 · 跟 doc-writer 家族 V4 Pro 同向)
- **对接 Phase**:**Phase 8 Step 1**(在 readme-writer Step 3 之前 · 因 readme-writer §6 快速开始 + §7 文档索引引用 docs/DEPLOY.md · **必须先跑本命令再跑 readme-writer**)
- **审什么**:**重写**`docs/DEPLOY.md`(替换 init-skeleton Phase 0 生成的占位 · 章节骨架不变 · 内容填实)
- **不做什么**:
  - **不重命名** init-skeleton 占位 5 节(对齐双胞胎硬约束)
  - **不重排** 5 节顺序(同上)
  - **不删减** 5 节中任意一节(若占位不合理 → 改 init-skeleton 占位)
  - **不编造** 命令(若实际项目结构不存在 / 版本号未替换 → 提醒先跑对应命令)
  - **不在 readme-writer 之后跑**(违反 Phase 8 顺序 · readme-writer 引用本命令产出)

## 任务

基于 Phase 0-7 已生成的全部产出文档 + 实际项目结构,**完善** docs/DEPLOY.md(替换 init-skeleton Phase 0 生成的占位):① 占位 5 节填实际命令/版本/路径(替换 `{{题目}}` 占位符 + 填实际 jar 包名 + 填实际 nginx 配置等)② 末尾追加 §六 默认账号密码 + §七 安全检查清单 + §八 云服务器进阶 3 个 Phase 8 完善节 ③ 输出 git diff 摘要。

## 输入

- **必读**(占位 5 节填实际内容的源):
  - `docs/00-选题标定.md § 一`(题目 → 替换 `{{题目}}` 占位符)
  - `backend/pom.xml`(实际 jar 包名 + finalName + 依赖版本号 → 填 §三 部署步骤后端命令)
  - `frontend/package.json`(实际 dist 输出路径 + scripts → 填 §三 部署步骤前端命令)
  - `backend/src/main/resources/application.yml`(端口 + 数据库连接 + JWT 配置占位 → 填 §五 故障排查 + §七 安全检查)
  - `sql/01-init.sql`(实际 SQL 文件路径 + 数据库名 → 填 §三 数据库初始化命令)
  - 实际项目目录(verify backend/target/ + frontend/dist/ 是否能产出 → 填 §三 构建步骤命令实际可跑)

- **必读**(Phase 8 追加完善节的源):
  - `docs/PRD.md §3 P0`(默认账号场景 → 填 §六 默认账号密码 · 教学演示规约)
  - `init-skeleton.md` `.gitignore` 段(行 379-410 · 已防 application-local.yml + .env 上传 → 印证 §七 安全检查)
  - `根目录 CLAUDE.md §一·一 后端 + §一·二 全栈安全`(JJWT 0.13.0 模块化 + BCrypt + 不硬编码密钥 → 填 §七 安全检查清单)
  - 06 G-22 模板第 901-940 行(云服务器场景 · nohup / systemd / 防火墙 → 填 §八 云服务器进阶)

- **必读**(规范权威源):
  - `init-skeleton.md` 行 217-227 DEPLOY.md 5 节占位 · **本命令章节顺序 + 节名严格对齐此** · **双胞胎权威源**
  - `CLAUDE.md §一·一` 技术栈基线 · **§二 环境要求对齐源**(版本号 100% 一致)
  - `CLAUDE.md §二·六` 配置规范 · "application.yml **禁止**放生产密码"(§七 安全检查源)
  - `06-提示词与审核模板库.md` G-22 段(行 895-944)· 默认账号 / nohup / systemd / 防火墙等教学规约
  - `08b-项目实施操作流程.md` §8.10 Step 1 · Phase 8 调用上下文

- **必读**(横向协同核对):
  - `readme-writer.md`(刚审完 · §6 快速开始 + §7 文档索引引用 docs/DEPLOY.md · deploy-writer 必须先跑)
  - `security-reviewer.md` R-08 维度 5 JWT 深度 + 维度 7 敏感信息深度(JWT 密钥 ≥ 32 字符 + .gitignore 防密码上传 · §七 安全检查清单参照)
  - `unittest-coder.md`(Phase 6 单测产出 · §三 部署步骤构建命令 mvn package 不跳过单测)

> ⚠️ **必读文件缺失检查**(任一异常立即停止 · **不要 fallback 编造内容**):
>
> | 状态 | 处理 |
> |---|---|
> | `backend/pom.xml` 不存在 / 仍是 init-skeleton 占位(`finalName` 未替换 / 学生没编译过) | 提醒先跑 `cd backend && mvn clean compile` 验证依赖完整 · §三 后端构建命令无基线 |
> | `frontend/package.json` 不存在 / pnpm 未装 | 提醒先按 CLAUDE.md §一·一 装 pnpm 10.33.4(`npm install -g pnpm`)+ `cd frontend && pnpm install` |
> | `sql/01-init.sql` 不存在 / 仍是占位 | 提醒先跑 `/db-designer`(Phase 2)· §三 数据库初始化命令无基线 |
> | `application.yml` 占位未替换(`<请填写>` / `<请填写至少32字符的密钥>` 仍存在)| 提醒先按 08b §3 改密码 + 改 JWT 密钥(否则部署直接启动失败)|
> | `docs/00-选题标定.md` 不存在 / `{{题目}}` 占位未替换 | 提醒检查 init-skeleton §1.4.3 + CLAUDE.md 起手段 ⚠️ Phase 0 必改 banner |
> | 当前不在 Phase 8(项目状态显示 Phase 0-7)| 提醒"deploy-writer 是 Phase 8 第 1 命令 · 早期 Phase 用 init-skeleton 占位即可" |
> | readme-writer 已经跑过(根目录 README.md 已是完整版) | 警告"readme-writer 应在 deploy-writer 之后跑 · readme-writer §6/§7 引用本命令产出 · 检查是否已经按错顺序跑过"|
>
> 部署文档必须基于真实项目,**编造命令没有价值**(对齐 CLAUDE.md §一·四)。

## 输入文档对照(DEPLOY 8 节 vs 各节源文档)

| DEPLOY 节 | 类型 | 内容来源 | 占位字段 / 完善节内容 |
|---|---|---|---|
| **§一 部署架构** | 占位 5 节(对齐 init-skeleton)| `docs/00-选题标定.md § 一` + `docs/TECH_DESIGN.md §1` | 部署目标(本机 / 云服务器)+ 部署模式(前后端分离 vs 前后端合一)+ 部署拓扑图(可选 mermaid)|
| **§二 环境要求** | 占位 5 节 | `CLAUDE.md §一·一` + `backend/pom.xml` + `frontend/package.json` | 跟 CLAUDE.md §一 2026-05-10 基线 100% 一致(JDK 21 + SpringBoot 3.5.14 + MyBatis-Plus 3.5.15 + MySQL 8.4 LTS + Maven 3.9 + Node.js 24 LTS + pnpm 10.33.4 + Vue 3.5.34 + Vite 8.0.0 + Git 2.x)+ 操作系统(Windows / macOS / Linux 兼容性)|
| **§三 部署步骤(后端/前端/数据库)** | 占位 5 节 | `backend/pom.xml` + `frontend/package.json` + `sql/01-init.sql` + `application.yml` | **3.1 后端构建**:`cd backend && mvn clean package`(**不**用 `-DskipTests` · Phase 6 单测必须通过) → 产出 `backend/target/<jar>` · **3.2 前端构建**:`cd frontend && pnpm install && pnpm build` → 产出 `frontend/dist/` · **3.3 数据库初始化**:`CREATE DATABASE <数据库名>` + `mysql -u root -p <数据库名> < sql/01-init.sql` + `SHOW TABLES` 验证 · **3.4 配置修改**:改 application.yml 数据库密码 + JWT 密钥(≥ 32 字符) |
| **§四 启动验证** | 占位 5 节 | `application.yml` + `vite.config.js` + 实际端口 | 启动后端:`java -jar backend/target/<jar>`(默认 8080)/ 启动前端:`cd frontend && pnpm preview`(本机调试 4173)或 nginx 反代 dist(正式部署)/ 浏览器访问 + 测试登录 + 主要业务流程 · 默认账号:见 §六 |
| **§五 故障排查** | 占位 5 节 | `application.yml` + R-08 维度 5/7 + bug-tracer-be/fe 高频踩坑 | **8 项常见问题**(详见下方「§五 故障排查清单」)|
| **§六 默认账号密码**(Phase 8 追加)| 完善节 | `docs/PRD.md §3 P0 功能`(P0-N 编号 · 登录注册场景) + `CLAUDE.md §一·二`(BCrypt) | 教学项目演示用账号 · 数据库存 BCrypt 哈希(不存明文)· 上线必改强密码 |
| **§七 安全检查清单**(Phase 8 追加)| 完善节 | `R-08 维度 5/7 + CLAUDE.md §一·二 + CLAUDE.md §二·六` | JWT 密钥 ≥ 32 字符 + application.yml 不上传 Gitee + .gitignore 已防 application-local.yml + 数据库密码不提交 + 所有密码 BCrypt 加密(数据库哈希存储) |
| **§八 云服务器进阶**(Phase 8 追加 · 可选)| 完善节 | 06 G-22 模板行 901-940 | 前后端合一部署(dist 复制到 static/)/ nginx 反代 + SPA fallback / nohup 后台 / systemd 自启 / 防火墙开端口 / 查日志 |

## 输出文档结构(DEPLOY.md 8 节 = 占位 5 + 完善 3)

> 📌 **占位 5 节顺序 + 节名严格对齐 init-skeleton.md 行 217-227**(双胞胎硬约束)· 末尾追加 3 个 Phase 8 完善节。

```markdown
# {{题目}} - 部署文档

## 1. 部署架构

- **部署目标**:本机部署(学生电脑全新目录从头部署一遍 · 教师验收用)/(可选)云服务器部署(华为云/阿里云学生免费实例 · 公网访问演示)
- **部署模式**:前后端分离(后端 jar + 前端 dist + nginx 反代 · 推荐)/ 前后端合一(dist 复制到 backend/src/main/resources/static/ · 单 jar 部署 · 进阶可选)
- **架构图**(可选 mermaid):浏览器 → nginx(80) → 前端 dist(/) + 后端 SpringBoot(:8080/api/) → MySQL(:3306)

## 2. 环境要求

| 类型 | 软件 | 版本 | 说明 |
|---|---|---|---|
| 后端 | JDK | 21 | 必须 21 LTS · `java -version` 验证 |
| 后端 | Maven | 3.9 | `mvn -version` 验证 · **禁止换 Gradle** |
| 后端 | MySQL | 8.4 LTS | 数据库 · 驱动 mysql-connector-j 8.4.0 |
| 前端 | Node.js | 24 LTS(代号 Krypton) | `node -v` 验证 |
| 前端 | pnpm | 10.33.4 LTS | `pnpm -v` 验证 · **禁止换 npm/yarn** · 没装跑 `npm install -g pnpm` |
| 工具 | Git | 2.x | clone 代码用 |
| 操作系统 | Windows / macOS / Linux | - | 全兼容(教学项目本机部署默认 Windows / macOS · 云服务器默认 Linux Ubuntu 22.04+)|

## 3. 部署步骤(后端/前端/数据库)

### 3.1 后端构建

```bash
cd backend
mvn clean package    # ⚠️ 不要加 -DskipTests · Phase 6 单测必须通过
# 产出 backend/target/<projectName>-<version>.jar
```

> ⚠️ **不跳过单测**(对齐 unittest-coder + refactor-helper 重构铁律 #3):若 Phase 6 已写单测,部署前最后一次单测验证;若 mvn test 失败 → 先用 `/bug-tracer-be` 排查修复再打包。
> ⚠️ 若环境无 MySQL 单测连不上数据库 → 临时 `mvn clean package -DskipTests` + **必须**在 README §八 验收清单标注"单测因环境问题未跑"(教师验收时减分但不直接扣 P0)

### 3.2 前端构建

```bash
cd frontend
pnpm install          # 用 pnpm 不要用 npm/yarn(详见 CLAUDE.md §一·一)
pnpm build            # 产出 frontend/dist/
```

> 💡 没装 pnpm? 跑 `npm install -g pnpm`(全局安装一次即可)

### 3.3 数据库初始化

```bash
# 1. 创建数据库
mysql -u root -p
> CREATE DATABASE <数据库名> CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
> EXIT;

# 2. 跑建表脚本
mysql -u root -p <数据库名> < sql/01-init.sql

# 3. 验证表已创建
mysql -u root -p <数据库名>
> SHOW TABLES;
> SELECT COUNT(*) FROM <核心表>;    # 验证测试数据已导入
> EXIT;
```

### 3.4 配置修改(部署前必做)

打开 `backend/src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/<数据库名>?useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai
    username: root
    password: <你的 MySQL root 密码>    # ⚠️ 必填 · 占位 <请填写> 替换

jwt:
  secret: <随机 32+ 字符密钥>            # ⚠️ 必填 · 详见 §七 安全检查
  expiration: 7200                       # 单位:秒(2 小时)
```

> ⚠️ **JWT 密钥生成**(≥ 32 字符 · 对齐 R-08 维度 5):
> - Linux/macOS:`openssl rand -base64 32`
> - Windows PowerShell:`[Convert]::ToBase64String((1..32 | %{ Get-Random -Maximum 256 }))`
> - Java 一次性生成:`UUID.randomUUID().toString().replace("-","") + UUID.randomUUID().toString().replace("-","")`(64 字符)
> ⚠️ **不要把密码 / JWT 密钥 commit 到 Git**(对齐 CLAUDE.md §一·二)· 详见 §七 安全检查清单

## 4. 启动验证

### 4.1 启动后端

```bash
# 方式 1:开发模式(推荐 · 看实时日志)
cd backend
mvn spring-boot:run    # 默认 http://localhost:8080

# 方式 2:生产模式(jar 包)
java -jar backend/target/<projectName>-<version>.jar
```

成功启动标志:控制台看到 `Started Application in X seconds` + `Tomcat started on port 8080`。

### 4.2 启动前端

```bash
# 方式 1:本机调试(简单 · 但仅本机用)
cd frontend
pnpm preview          # 默认 http://localhost:4173

# 方式 2:正式部署(用 nginx 反代 dist · 详见 §八 云服务器进阶)
```

> ⚠️ **`pnpm preview` 仅本机调试用**(Vite 内置预览服务器 · 性能差 · 单连接 · 无 SPA fallback)· 正式部署用 nginx 反代 dist(详见 §八)

### 4.3 浏览器访问 + 业务流程验证

```
1. 打开 http://localhost:4173(本机)或 http://<公网IP>(云服务器)
2. 看到登录页 · 用 §六 默认账号登录(admin / admin123)
3. 跑通主流程:登录 → <P0 业务流程 1> → <P0 业务流程 2> → 退出
4. F12 Network 面板验证 /api/* 请求状态码 200 + Result.code == 200
```

成功验证标志:5 项硬地基全 ✅(对齐 README §八 验收清单)。

## 5. 故障排查

### 5.1 后端启动失败

| 错误 | 诊断 | 修复 |
|---|---|---|
| `Access denied for user 'root'@'localhost'` | 数据库密码错 | 改 application.yml `spring.datasource.password` |
| `Communications link failure` | 数据库未启动 / 端口不对 | 启动 MySQL 服务 + 验证 `netstat -an | findstr 3306` |
| `Unknown database '<数据库名>'` | 数据库未创建 | 跑 §3.3 创建数据库 + 跑 SQL |
| `Port 8080 was already in use` | 端口被占用 | `netstat -ano | findstr 8080` 查 PID + `taskkill /F /PID <pid>` 杀进程 · 或改 application.yml `server.port: 8090` |
| `JwtUtils: secret too short` / `WeakKeyException` | JWT 密钥 < 32 字符 | 改 application.yml `jwt.secret` 为 ≥ 32 字符随机串(详见 §3.4 生成命令)|
| `application.yml not found` | jar 启动找不到配置 | `java -jar -Dspring.config.location=classpath:application.yml,file:./application.yml ...` |

### 5.2 前端启动失败

| 错误 | 诊断 | 修复 |
|---|---|---|
| `pnpm: command not found` | 没装 pnpm | `npm install -g pnpm` |
| `Cannot find module ...` | 依赖未装 | `cd frontend && pnpm install` |
| `Port 4173/5173 in use` | 端口被占用 | 改 vite.config.js `server.port` |
| 浏览器 `404 Not Found` 刷新页面 | nginx 配置缺 SPA fallback | nginx 加 `try_files $uri $uri/ /index.html`(详见 §八)|
| 前端 `Network Error` 调用 /api | 后端未启动 / vite proxy 配错 / CORS 配错 | 验证后端 8080 在跑 + 验证 vite.config.js proxy `/api → http://localhost:8080` + 验证 CorsConfig.java 含 `http://localhost:5173` |

### 5.3 联调常见问题

| 问题 | 诊断 | 修复 |
|---|---|---|
| 登录后所有接口 401 | JWT 密钥前后端不匹配 / token 没传 | F12 Network 看 Authorization Header · 对照 application.yml `jwt.secret` |
| 跨域 CORS 错误 | CorsConfig 未含前端域名 | 改 CorsConfig.java `allowedOrigins` 加前端域名 |
| Windows 防火墙阻断 | 局域网访问失败 | Windows 防火墙允许 Java + Node 入站 · 或临时关防火墙 |
| MySQL 客户端版本不兼容 | mysql 客户端 < 8.0 报错 | 升级 mysql 客户端到 8.4 LTS · 或用 mysqlsh |
| 中文乱码 | 数据库字符集错 | 创建数据库时 `CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci`(详见 §3.3)|

> 💡 **更详细排查**:用 `/bug-tracer-be` / `/bug-tracer-fe`(对齐 D-XX 协议)生成 `docs/对话记录/D-XX-<简述>-<日期>.md` 排查报告。

## 6. 默认账号密码(Phase 8 追加 · 教学演示用)

> ⚠️ **教学项目演示用 · 上线必改强密码 · BCrypt 哈希存数据库不存明文**(对齐 CLAUDE.md §一·二 + R-08 维度 4)

| 角色 | 账号 | 密码 | 说明 |
|---|---|---|---|
| 管理员 | `admin` | `admin123` | BCrypt 加密哈希存 user 表 · 教师验收用 |
| 普通用户 | `<示例用户>` | `<示例密码>` | 注册场景演示用(若 P0 含注册流程)|

> 📌 **如何确认默认账号已创建**:
> 1. 跑 `sql/01-init.sql` 时若含 `INSERT INTO user`,默认账号已自动创建
> 2. 若没有,首次部署后用 `/api/register` 接口注册管理员(对齐 docs/PRD.md §3 P0)
> 3. 验证:登录后看到管理员权限菜单 → 默认账号生效

## 7. 安全检查清单(Phase 8 追加 · 上线前必查 · 对齐 R-08 维度 5/7)

### 7.1 JWT 密钥安全

- [ ] **JWT 密钥 ≥ 32 字符**(application.yml `jwt.secret` · 对齐 R-08 维度 5)
- [ ] **签名算法 HS256 或更强**(`JwtUtils.generateToken` 用 `Jwts.SIG.HS256` · **严禁** `none`)
- [ ] **JWT 密钥不硬编码到 .java 代码**(从 application.yml 读 `${jwt.secret}`)
- [ ] **Token 过期时间合理**(2-7200 秒 · 不要"永不过期")

### 7.2 配置文件安全

- [ ] **`.gitignore` 已防 `application-local.yml` + `.env` + `*.log` 上传**(对齐 init-skeleton .gitignore 行 379-410)
- [ ] **`application.yml` 中无生产密码**(教学占位用 `<请填写>` · 真实密码放环境变量或 `application-local.yml` · 对齐 CLAUDE.md §二·六)
- [ ] **Git 提交历史无明文密码**(`git log --all -- application.yml` 验证 · 若曾经 commit 过 → 用 `git filter-branch` 清理 · 学生项目通常未发生)

### 7.3 密码安全

- [ ] **数据库存 BCrypt 哈希**(对齐 CLAUDE.md §一·二 + R-08 维度 4 · `BCryptPasswordEncoder.encode`)
- [ ] **登录接口用 `matches` 比对**(不直接 `equals`)
- [ ] **响应字段过滤**:`Result<T>` 不返回密码 hash(对齐 R-08 维度 7 · Entity 加 `@JsonIgnore` 或 DTO 投影)
- [ ] **日志脱敏**:`log.info` 不打印整个 user 对象含密码(对齐 CLAUDE.md §二·五)

### 7.4 端口与防火墙

- [ ] 仅开放必要端口:80(nginx)/ 443(HTTPS · 可选)/ 8080(后端 · 内部)/ 3306(MySQL · 内部 · 不对外)
- [ ] **MySQL 不对外开放 3306**(只允许 localhost 或后端服务器 IP 连接 · 防外部直连数据库)

## 8. 云服务器进阶(Phase 8 追加 · 可选 · 教学项目本机部署即可)

> 📌 **教学项目通常本机部署即可达成 5 项硬地基** · 云服务器是加分项(对齐 05 验收方案 P2)。如不做云部署,可删除本节或保留参考。

### 8.1 服务器准备

- 系统:Ubuntu 22.04+ / CentOS 8+(教学项目用阿里云/华为云学生免费实例)
- 配置:1 核 2G(教学项目数据量小够用)
- 装环境:`sudo apt install openjdk-21-jdk maven mysql-server-8.0 nginx`(Ubuntu)+ `npm install -g pnpm`

### 8.2 前后端合一部署(单 jar · 最简方案)

```bash
# 1. 本机打包
cd frontend && pnpm build
cp -r dist/* backend/src/main/resources/static/   # 前端 dist 复制到后端 static
cd backend && mvn clean package

# 2. 上传 jar 到服务器
scp backend/target/<jar> ubuntu@<服务器 IP>:/home/ubuntu/

# 3. 服务器跑
java -jar <jar>    # 浏览器访问 http://<服务器 IP>:8080 看到前端
```

> ⚠️ **前端 axios baseURL 改 `''`**(空字符串 · 因前后端同源 · `vite.config.js` 中 `base: '/'`)· 否则双 `/api` 全 404

### 8.3 nohup 后台 / systemd 自启

```bash
# 方式 1:nohup(简单)
nohup java -jar <jar> > app.log 2>&1 &
# 查日志:tail -f app.log
# 停止:ps -ef | grep java + kill <pid>

# 方式 2:systemd(推荐 · 服务器重启自启)
sudo vim /etc/systemd/system/myapp.service
# 内容:
[Unit]
Description=My App
After=network.target

[Service]
ExecStart=/usr/bin/java -jar /home/ubuntu/<jar>
Restart=on-failure
User=ubuntu

[Install]
WantedBy=multi-user.target

sudo systemctl daemon-reload
sudo systemctl enable myapp
sudo systemctl start myapp
sudo systemctl status myapp    # 看运行状态
sudo journalctl -u myapp -f    # 看日志
```

### 8.4 nginx 反代(前后端分离 · 进阶)

```nginx
server {
    listen 80;
    server_name <服务器 IP 或域名>;

    # 前端 dist
    location / {
        root /home/ubuntu/dist;
        try_files $uri $uri/ /index.html;    # SPA fallback · 避免 F5 刷新 404
    }

    # 后端 /api 反代
    location /api/ {
        proxy_pass http://localhost:8080/;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }
}
```

部署 dist:`scp -r frontend/dist/* ubuntu@<服务器 IP>:/home/ubuntu/dist/`

### 8.5 防火墙开端口

```bash
# Ubuntu UFW
sudo ufw allow 80/tcp
sudo ufw allow 443/tcp    # 可选(HTTPS)
sudo ufw enable
sudo ufw status

# 阿里云/华为云控制台:安全组 入方向规则 加 80 + 443
```

### 8.6 启动失败看日志

```bash
# nohup 模式
tail -f app.log
tail -100 app.log | grep -i error

# systemd 模式
sudo journalctl -u myapp -f
sudo journalctl -u myapp --since "5 minutes ago"
```
```

## 输出指令(Claude Code 必须 4 项都做,缺一不可)

1. **直接重写** `docs/DEPLOY.md`(替换 init-skeleton Phase 0 生成的占位 · **章节顺序 + 节名严格对齐 init-skeleton.md 行 217-227 · 不重命名 / 不重排 / 不删减 5 节** · 仅在末尾追加 §六 默认账号密码 + §七 安全检查清单 + §八 云服务器进阶 3 个 Phase 8 完善节)
2. **8 节齐全 · 命令必须可一行行复制粘贴跑通**:**严禁**伪代码 / 占位命令(如 `<TODO>` / `# 待补充`);若任一引用文档不存在 → **立即停止**(参照「必读文件缺失检查」表)· 不要编造命令
3. **学生填占位用 ✍**:实际项目特有的字段(`<数据库名>` / `<jar 包名>` / `<服务器 IP>` / 默认密码示例 / P0 业务流程名 等)用 ✍ 占位提醒;**绝不**编造默认值
4. **输出 git diff 摘要**(改前 vs 改后 · git diff 风格 + 含文件路径)

## ⚠️ 不允许的部署内容

- ❌ **重命名 / 重排 / 删减 init-skeleton 占位 5 节**(违反双胞胎硬约束)
- ❌ **mvn package 加 `-DskipTests`**(违反 unittest-coder + 重构铁律 · 单测必须通过 · 仅环境无 MySQL 时临时跳过 + README 标注)
- ❌ **JWT 密钥示例 < 32 字符**(违反 R-08 维度 5)
- ❌ **将真实密码 / JWT 密钥写入 DEPLOY.md 让学生 commit**(违反 R-08 维度 7 · 应用占位 `<请填写>` · 提醒学生放 application-local.yml)
- ❌ **跳过 §六 默认账号密码节**(教师验收 + 演示用 · 教学项目必有)
- ❌ **跳过 §七 安全检查清单**(对齐 R-08 安全闭环 · 教学项目 + 上线前都需查)
- ❌ **deploy-writer 在 readme-writer 之后跑**(违反 Phase 8 顺序 · readme-writer §6/§7 引用本命令产出)

## 调用示例

### 示例 1:本机部署(默认 · 教学项目通常用此)

```
/deploy-writer 目标=本机

请基于:
- docs/00-选题标定.md § 一(题目)
- backend/pom.xml + frontend/package.json(实际版本)
- sql/01-init.sql(实际数据库结构)
- application.yml(端口/数据库连接/JWT 配置)
- 根目录 CLAUDE.md §一·一(技术栈基线 2026-05-10)
- 06-提示词与审核模板库.md G-22 段(教学规约)

完善 docs/DEPLOY.md(占位 5 节填实际命令 + 末尾追加 §六 默认账号密码 + §七 安全检查清单 + §八 云服务器进阶),严格对齐 init-skeleton.md 行 217-227 占位 5 节顺序 + 节名(双胞胎硬约束)。

学生填占位字段(数据库名 / jar 包名 / 默认密码 等)用 ✍。

输出 diff。

⚠️ 调用前 会话内**切换模型**(用 `/model` 命令)到 V4 Pro(部署文档命令必须可复制粘贴跑通 · 内容质量重要)。
⚠️ 调用前 **退出 `claude` 后重新运行 `claude`(新会话清空上下文)**(对齐 08b §8.10 Phase 起点 + 规则 7.2)。
⚠️ **必须在 readme-writer 之前跑**(因 readme-writer §6/§7 引用本命令产出)。
```

### 示例 2:云服务器部署(可选 · P2 加分项)

```
/deploy-writer 目标=云服务器

服务器:Ubuntu 22.04 + 1 核 2G(阿里云学生免费实例)
公网 IP:<待补充>
部署模式:前后端合一(单 jar · 最简方案)

请生成完整部署文档,占位 5 节 + §六/§七/§八 完善节齐全 · §八 云服务器进阶详细写(含 systemd 自启 + nginx 反代 + 防火墙)。

输出 diff。
```

### 示例 3:实测验证后修订(部署遇到问题反馈)

```
/deploy-writer

我按 docs/DEPLOY.md §3.3 数据库初始化跑到第 2 步报错:`ERROR 1045 (28000): Access denied for user 'root'@'localhost'`。

请修订 DEPLOY.md §五 故障排查段(加该错的诊断 + 修复)+ §3.4 配置修改段(加密码权限说明)。完成输出 diff。
```

## 验证 checklist(学生 review 时核对)

- [ ] **必读文件缺失检查**全部通过(`backend/pom.xml / frontend/package.json / sql/01-init.sql / application.yml / docs/00-选题标定.md` 全在 + 当前 Phase 8)
- [ ] **退出 `claude` 重启确认**(`/deploy-writer` 调用前退出 `claude` 重启 · 对齐 08b §8.10 Phase 起点)
- [ ] **切换模型确认**(V4 Pro · 跟 doc-writer 家族同向)
- [ ] **deploy-writer 在 readme-writer 之前跑**(Phase 8 Step 1 vs Step 3 · 双胞胎硬约束)
- [ ] **章节顺序 + 节名对齐 init-skeleton.md 行 217-227 占位 5 节**(双胞胎硬约束 · 不重命名 / 不重排 / 不删减)
- [ ] **占位 5 节齐全**:① 部署架构 ② 环境要求 ③ 部署步骤(后端/前端/数据库) ④ 启动验证 ⑤ 故障排查
- [ ] **追加 3 个完善节**:§六 默认账号密码 + §七 安全检查清单 + §八 云服务器进阶(Phase 8 新增)
- [ ] **§一 部署架构**:含部署目标 + 部署模式 + (可选)架构图
- [ ] **§二 环境要求**:跟 CLAUDE.md §一·一 2026-05-10 基线 **100% 一致**(JDK 21 / SpringBoot 3.5.14 / MyBatis-Plus 3.5.15 / MySQL 8.4 LTS / Maven 3.9 / Node 24 / pnpm 10.33.4 / Vite 8.0.0 等)+ 操作系统兼容性
- [ ] **§三 部署步骤**:4 个子段齐全(3.1 后端构建 + 3.2 前端构建 + 3.3 数据库初始化 + 3.4 配置修改)· **不用 `-DskipTests`**(单测必须通过 · 对齐 unittest-coder + 重构铁律)
- [ ] **§四 启动验证**:后端启动 + 前端启动 + 浏览器访问 + 业务流程验证 4 步齐全 · `pnpm preview` 仅本机调试用
- [ ] **§五 故障排查**:8+ 项常见问题(后端启动失败 + 前端启动失败 + 联调问题)+ 转 `/bug-tracer-be/fe` 提示
- [ ] **§六 默认账号密码**:教学演示规约 + BCrypt 哈希存数据库不存明文 + 上线必改提醒
- [ ] **§七 安全检查清单**(对齐 R-08 维度 5/7):JWT 密钥 ≥ 32 字符 + 签名算法 HS256 + .gitignore 防 application-local.yml + 数据库 BCrypt 加密 + Git 历史无明文密码 + 端口防火墙
- [ ] **§八 云服务器进阶**(可选):服务器准备 + 前后端合一 / nginx 反代 / nohup / systemd / 防火墙 / 看日志
- [ ] **命令可一行行复制粘贴跑通**(不是伪代码 · 不含 `<TODO>`)
- [ ] **学生填占位用 ✍**(数据库名 / jar 包名 / 服务器 IP / 默认密码示例 等)
- [ ] **未编造命令**(命令基于实际 pom.xml / package.json / sql/01-init.sql 取值)
- [ ] **JWT 密钥示例 ≥ 32 字符 + 生成命令齐全**(openssl / PowerShell / Java UUID)
- [ ] **`-DskipTests` 仅作为环境无 MySQL 临时跳过提及 + 强制 README 标注**

## 衔接

下一步(对齐 08b §8.10):

1. **实测部署流程**(08b §8.10 Step 2):按 docs/DEPLOY.md §3 部署步骤,**从一个全新目录**跑部署(如 `D:\code\<项目>-deploy-test\`):
   - 拷贝 build 产物
   - 跑 §3.3 数据库初始化
   - 启动后端 jar
   - 启动前端 dist
   - §4 浏览器访问验证
   
   如有问题:`/deploy-writer 跑部署到 Step X 失败,报错: [...]。请修订 docs/DEPLOY.md。完成输出 diff。`

2. **`/git-committer`** 提交本命令产出:
   ```
   /git-committer 请 commit + push:docs(p8): 完善 docs/DEPLOY.md 5 节(对齐 init-skeleton 占位)+ 追加默认账号/安全检查/云服务器进阶
   ```

3. **`/readme-writer`**(Phase 8 Step 3 · readme-writer §6/§7 引用 docs/DEPLOY.md):
   ```
   /readme-writer
   ```
   详见 `readme-writer.md` 调用上下文(必须 退出 `claude` 重启 + 切 V4 Pro)。

4. **08b §8.10 Step 4 最终验收基准 commit**(在 readme-writer 之后):
   ```
   /git-committer 请 commit + push:docs(p8): 完善 README + DEPLOY 部署指南
   git commit --allow-empty -m "chore: final submission for course assessment"
   git push
   ```

5. **(收尾)** Phase 8 全部跑完后:`/rules-updater` 同步 `project-status.md` 「Phase 8 完成」(对齐 rules-updater §二 单字段更新模式 · ⚠️ 同步的是 project-status.md,不动 CLAUDE.md)

## 设计要点

- **双胞胎模式 · 内容写入器**(对齐文档行 85-87 决策档案 · doc-writer 家族标配 · 跟 readme-writer 方案 C 同模式):deploy-writer 是 init-skeleton DEPLOY.md 5 节占位的"内容写入器" · 章节结构 100% 一致(顺序 + 节名 + 占位 5 节)· 末尾仅追加 §六/§七/§八 3 个 Phase 8 完善节(因 Phase 0 时这 3 节学生还没东西填)· **若需扩展占位 5 节,改 init-skeleton 占位**(权威源)而非命令端
- **必须切换模型**(V4 Pro · 跟 doc-writer 家族同向):部署文档跨电脑/服务器使用 · 命令必须可一行行复制粘贴跑通 · 内容质量重要 · 不切模型(V4 Flash 命令格式细节易错)
- **从 Phase 0 占位 → Phase 8 完整版的"演化"**:占位 5 节填字段(`{{题目}}` / 节标题占位)· deploy-writer 替换为实际命令/版本/路径;**不重写章节结构**(双胞胎硬约束)· 仅追加 3 个完善节
- **跟 readme-writer 横向协同**(Phase 8 双胞胎 · 同 doc-writer 家族 · 同 V4 Pro):readme-writer §6 快速开始 + §7 文档索引引用 docs/DEPLOY.md · **deploy-writer 必须在 readme-writer 之前跑**(Phase 8 Step 1 vs Step 3)· **顺序硬约束**
- **跟 R-08 维度 5/7 安全协同**:§七 安全检查清单全面对应 R-08 安全维度(JWT 密钥 ≥ 32 字符 + .gitignore + BCrypt + 日志脱敏 + 端口防火墙)· R-08 命令是审核 / deploy-writer 是部署落地 · 双向打通安全闭环
- **跟 unittest-coder Phase 6 单测协同**:§3.1 后端构建命令 `mvn clean package` 不加 `-DskipTests`(单测必须通过)· 仅环境无 MySQL 时临时跳过 + 强制 README 标注 · 对齐 refactor-helper / perf-optimizer 重构铁律 #3
- **04/05 验收方案对齐**:§六 默认账号密码 + §四 启动验证 + §七 安全检查清单 是教师验收第一对照点(教师跑 README §六 快速开始 → 用 §六 默认账号登录 → 验证 P0 跑通)
- **学生项目典型踩坑场景**(已加到验证 checklist 和 §五 故障排查):
  - 部署前未跑单测(用 -DskipTests 掩盖单测损坏)→ 删 -DskipTests 强制跑测
  - JWT 密钥太短(`<请填写>` 占位填了 8 字符)→ 启动 WeakKeyException → §3.4 加生成命令
  - application.yml 误上传 Gitee 泄漏密码 → §七 .gitignore 检查 + Git 历史扫描
  - pnpm preview 误用作生产部署(单连接性能差)→ §4.2 明示仅本机调试 + §八 nginx 反代
  - 默认账号 admin 密码明文存数据库(BCrypt 漏)→ §六 教学规约 + R-08 维度 4 协同
  - Windows 防火墙阻断局域网 → §五 故障排查 5.3
  - mysql 客户端版本不兼容 → §五 故障排查 5.3

---

> 📋 **跨文件呼应导航**:
> - **上游产出**(占位 5 节填实际内容的源):`init-skeleton.md` 行 217-227 DEPLOY.md 5 节占位(**双胞胎权威源**) + `docs/00-选题标定.md § 一`(题目)+ `backend/pom.xml`(实际 jar / 版本)+ `frontend/package.json`(实际 dist 路径)+ `sql/01-init.sql`(实际数据库)+ `application.yml`(端口/密码/JWT 占位)
> - **平行规则**:`CLAUDE.md §一·一`(技术栈基线 · §二 环境要求对齐源 · 2026-05-10)+ `§一·二`(全栈安全 · BCrypt + 不硬编码密钥 · §七 对齐源)+ `§一·四`(AI 硬约束 · 中文注释 · DEPLOY.md 中文化)+ `CLAUDE.md §二·六`(配置规范 · application.yml 禁放生产密码 · §七 对齐源)+ `CLAUDE.md §三·一` 8 类(项目结构印证)
> - **横向协同**(Phase 8 doc-writer 双胞胎 · 顺序硬约束):`readme-writer.md`(同 Phase 8 · 同 V4 Pro · 同 doc-writer 家族 · readme-writer §6/§7 引用 docs/DEPLOY.md)· **必须在 readme-writer 之前跑**(08b §8.10 Step 1 vs Step 3)
> - **安全协同**(R-08 安全闭环):`security-reviewer.md` R-08 维度 5 JWT 深度(JWT 密钥 ≥ 32 字符 + 签名算法 HS256 + Token 过期 · §七 对齐源)+ R-08 维度 7 敏感信息深度(application.yml 不上传 + .gitignore 防泄漏 · §七 对齐源)
> - **单测协同**:`unittest-coder.md`(Phase 6 单测产出 · §3.1 部署前 mvn test 必须全绿 · 不跳过)+ `refactor-helper.md` / `perf-optimizer.md` 重构铁律 #3(测试达标再 commit)
> - **故障排查协同**:`bug-tracer-be.md` / `bug-tracer-fe.md`(D-01/D-02 · §五 故障排查指引学生用 D-XX 协议生成排查报告)
> - **下游消费**:实测验证(08b §8.10 Step 2 · 全新目录跑一遍部署)+ `git-committer.md`(commit message `docs(p8): 完善 docs/DEPLOY.md 5 节 + 追加完善 3 节`)+ `readme-writer.md`(Phase 8 Step 3 · readme-writer §6/§7 引用本命令产出)
> - **教学源头**:`06-提示词与审核模板库.md` G-22 段(行 895-944 · 前后端合一 / nohup / systemd / 防火墙 / 看日志 · 命令引用即可,无需修改源头)+ `04-课件大纲.md / 05-验收方案 V4-2.md`(Phase 8 部署 + 5 项硬地基对齐)
> - **doc-writer 家族标杆**:`srs-writer.md`(Phase 1 · PRD)+ `tech-designer.md`(Phase 1 · TECH_DESIGN)+ `db-designer.md`(Phase 2 · DATABASE_DESIGN)+ `api-designer.md`(Phase 3 · API_DESIGN)+ `readme-writer.md`(Phase 8 · README · 刚审完确立方案 C)+ **本命令(Phase 8 · DEPLOY · doc-writer 家族第 7 个 + 双胞胎模式第 7 次应用 · 主任务最终闭合命令)**
> - **rules-updater**:Phase 8 全部完成后 `/rules-updater` 同步 `project-status.md` 「Phase 8 完成」(对齐 rules-updater §二 单字段更新模式 · ⚠️ 同步的是 project-status.md,不动 CLAUDE.md)
