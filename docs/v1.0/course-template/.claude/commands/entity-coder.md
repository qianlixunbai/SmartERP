---
name: entity-coder
description: 基于 docs/DATABASE_DESIGN.md 中模块对应的表,生成 MP Entity + Mapper(每模块 N 张表 → N 个 Entity + N 个 Mapper · 含「应用修复」二级模式 · 跟 code-reviewer-be R-05 形成「审核 ↔ 修复」二段循环 · 对应 06 G-08)
---

你是 SpringBoot 3.5 + MyBatis-Plus 3.5.15 项目的实体类生成助手(对应 06 G-08 · 2026-05-10 基线)。

## 调用上下文(2 种模式 · 新建任务规则不同)

| 模式 | 触发命令 | 新建任务规则 | 用途 |
|---|---|---|---|
| **首次生成** | `/entity-coder 模块=<X>` | **生成型 + 每模块独立** → 调用前**退出 `claude` 重启**(规则 7.2 · 见 08b §8.11)· **每个新模块前必须退出 `claude` 重启**(对齐 08b §8.6 工时拆分:3-4 模块 × 每模块独立循环) | Phase 4 Step 1 创建 entity/ + mapper/ |
| **应用修复** | `/entity-coder 应用修复 模块=<X>` | **审核类例外** → 接前面对话继续,**不要退出 `claude`**(需要看 R-05 注释上下文) | Phase 4 Step 5 处理 entity/ + mapper/ 下的 R-05 issue(service/ + impl/ + controller/ 由 `/service-coder 应用修复` 处理) |

模型 V4 Flash · 输入纯文件依赖(DATABASE_DESIGN.md + CLAUDE.md §一 + CLAUDE.md §二)· 不依赖对话上下文。

下面 §一(首次生成)+ §二(应用修复)分别规范。

---

## §一 首次生成模式

### 任务

基于 `docs/DATABASE_DESIGN.md §3` 中"<模块名>"对应的表(N 张),生成 **N 个 Entity 类 + N 个 Mapper 接口**(每表 1 个 Entity + 1 个 Mapper)。

### 输入

- **必读**:`docs/DATABASE_DESIGN.md`(db-designer 已生成 · 4 节 · 含 §3 CREATE TABLE 完整 SQL)
- **必读**:根目录 `CLAUDE.md` §一·一·后端(技术栈版本 + JDK 21 + MP 3.5.15 + Lombok 1.18.46)+ `§一·二`(BCrypt + LambdaQueryWrapper)
- **必读**:根目录 `CLAUDE.md` §二·一(分层 8 类 · `entity/` + `mapper/` 行 + 关键类示例)+ `§二·二`(Entity 规范 · `@TableName` / `@TableId` / `LocalDateTime` / `@TableLogic`)
- **可选**:`backend/src/main/java/{{包路径}}/entity/`(已生成的其他 Entity · 避免命名冲突)

> ⚠️ **必读文件缺失检查**(任一异常立即停止 · **不要 fallback 自由发挥**):
>
> | 状态 | 处理 |
> |---|---|
> | `docs/DATABASE_DESIGN.md` 不存在 | 提醒先调用 `/db-designer` 生成数据库设计 |
> | `docs/DATABASE_DESIGN.md` 仍是 init-skeleton 占位(只有 §1-§4 标题但 §3 内容为空)| 提醒先完整生成 §3 SQL,再来生成 Entity |
> | 学生指定的「模块」对应的表名在 §3 找不到 | 列出 §3 所有 CREATE TABLE 表名,提醒学生选对模块名 |
> | 学生未指定 `模块=<X>` 参数 | 提醒带模块参数(避免一次生成所有表的 Entity · 违背 08b §8.6 "每模块独立 commit" 工时拆分意图)|
>
> Entity + Mapper 是 Phase 4 service-coder / Postman 测试 / 全模块编译的根基 —— **编造字段 / 漏 Mapper 会让整模块代码全链断裂**。

`{{包路径}}` 解析方式:**自动从 `backend/src/main/java/` 下唯一存在的子目录链解析**(如 `com/example/property/`)· 也可读 `backend/pom.xml` 的 `<groupId>` 印证。

### 输出文件 1:N 个 Entity 类(每表 1 个 · 扁平 entity/ 目录)

文件路径:`backend/src/main/java/{{包路径}}/entity/<EntityName>.java`(**扁平 · 不分模块子目录** · 对齐 init-skeleton + CLAUDE.md §二·一 entity/ 行)

`<EntityName>` 命名规则:**单数 · 大驼峰 · 下划线转驼峰**

| 表名(SQL) | 实体类名(Java) |
|---|---|
| `user` | `User.java` |
| `user_role` | `UserRole.java` |
| `house` | `House.java` |
| `payment_record` | `PaymentRecord.java` |

#### Entity 类内容规范(必符合)

1. **包声明 + import**:`package {{包路径}}.entity;` + 必要的 import(`LocalDateTime` / `BigDecimal` / `@Data` / `@TableName` / `@TableId` / `@TableField` / `@TableLogic` / `@JsonIgnore` / `IdType` 等)
2. **类注解**:
   - `@Data`(Lombok · getter/setter/equals/hashCode/toString)
   - `@TableName("<表名>")`(**显式声明** · 避免 MP 默认下划线↔驼峰策略对缩写表名/复数差异时误映射 · 对齐 CLAUDE.md §二·二)
3. **字段顺序对齐 db-designer §3 字段顺序约定**:`id` → 业务字段(逻辑顺序) → `is_deleted`(若有) → `create_time` → `update_time`
4. **字段注解**:
   - **主键**:`@TableId(type = IdType.AUTO)`(数据库自增 · 对齐 db-designer §3 #4)
   - **普通字段**:`@TableField` 默认 MP 自动下划线↔驼峰映射;**字段名差异**(缩写/特殊命名)时**必须**显式标注 `@TableField("col_name")`(对齐 CLAUDE.md §二·二)
   - **软删除字段**(若 db-designer §3 该表含 `is_deleted TINYINT(1)`):**`@TableLogic`** + `@TableField("is_deleted")`(对齐 CLAUDE.md §二·二 + db-designer §3 #6)
   - **密码字段**:`@JsonIgnore`(防响应序列化泄漏)
     - ⚠️ **`@JsonIgnore` 双向都生效** —— 注册接口**禁止**用 Entity 直接接收前端密码(密码会被反序列化忽略 → 数据库存空)
     - **注册/改密接口必须用单独的 `RegisterDTO` / `ChangePasswordDTO`** 接收明文密码(详见 `CLAUDE.md §二·三` DTO 命名约定 · 由 `/service-coder` 生成)
     - Service 层用 `BCryptPasswordEncoder` 加密后赋值给 Entity(对齐 `CLAUDE.md §一·二`)

#### SQL → Java 字段类型映射表(对齐 db-designer §3 #6 字段约定)

| SQL 类型 | Java 类型 | 备注 |
|---|---|---|
| `BIGINT`(主键 / 外键) | `Long` | 主键加 `@TableId(IdType.AUTO)` |
| `VARCHAR(N)` | `String` | 用户名 / 标题 / 状态码等 |
| `TEXT` | `String` | 描述 / 备注字段 |
| **`DECIMAL(M,N)`** | **`BigDecimal`** | ⚠️ **禁止映射 `Double` / `Float`**(精度丢失 · CLAUDE.md §二·二 明示)· import `java.math.BigDecimal` |
| `TINYINT(1)`(布尔语义) | `Boolean` | MP 默认 0/1 ↔ false/true 自动转换 |
| `TINYINT(1)`(`is_deleted`) | `Integer` | 加 `@TableLogic`(逻辑删除字段不参与序列化) |
| `INT` | `Integer` | — |
| **`DATETIME`**(`create_time` / `update_time` / 业务时间) | **`LocalDateTime`** | ⚠️ **禁止映射 `Date`**(对齐 CLAUDE.md §二·二)· import `java.time.LocalDateTime` · Jackson ISO 8601 自动序列化(对齐 api-designer §1 时间格式) |
| `DATE` | `LocalDate` | — |
| `TIME` | `LocalTime` | — |
| `JSON` | `String`(简单场景)/ 自定义 TypeHandler(复杂场景) | — |

### 输出文件 2:N 个 Mapper 接口(每表 1 个 · 扁平 mapper/ 目录)

文件路径:`backend/src/main/java/{{包路径}}/mapper/<EntityName>Mapper.java`(对齐 init-skeleton 生成的 `mapper/.gitkeep` 占位 + CLAUDE.md §二·一 mapper/ 行「关键类示例 UserMapper extends BaseMapper」)

> 📌 **2026-05-10 审核确立**:Mapper 接口由本命令(entity-coder)同时生成,**不再由 service-coder 兜底**(对齐 README.md L30 表格描述「Entity + Mapper」+ 06 G-08 原始设计意图)。这解决了之前 entity-coder + service-coder 都未生成 Mapper 导致 ServiceImpl 编译失败「找不到符号 UserMapper」的链路断点(详见审核记录跨命令同步段第 4 次链路断点决策)。

#### Mapper 接口内容规范

```java
package {{包路径}}.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import {{包路径}}.entity.<EntityName>;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface <EntityName>Mapper extends BaseMapper<<EntityName>> {
    // 简单 CRUD 全交给 BaseMapper(selectById / insert / updateById / 等)
    // 复杂查询(多表 join / 统计 / 子查询)有限例外才允许 @Select 或 XML
    // 详见 CLAUDE.md §二·四 MyBatis-Plus 用法
}
```

- **命名**:`<EntityName>Mapper.java`(对齐 CLAUDE.md §二·一 mapper/ 行 + 跟 ServiceImpl `extends ServiceImpl<Mapper, Entity>` 强一致)
- **`@Mapper` 注解**:加在接口上(双重保险 · 即使 init-skeleton Application.java 上的 `@MapperScan` 失效也不影响)
- **空方法体**(简单 CRUD 全走 `BaseMapper`,Service 层用 `LambdaQueryWrapper`;复杂查询走 CLAUDE.md §二·四 例外路径,Service 审核时再决定要不要补)

### 输出指令(Claude Code 必须遵守 · 06 §一·五·1)

1. **直接创建** N 个 Entity 文件 + N 个 Mapper 文件(扁平 `entity/` + `mapper/` 目录 · 不分模块子目录)
2. 完成后输出 diff 摘要(列每个 Entity 的字段清单 + 每个 Mapper 的文件路径)
3. 不确定的字段映射先问(如 `TINYINT(1)` 是布尔还是软删除标志 / DECIMAL 精度位数),**不要编造数据库字段**

### 调用示例

#### 单表模块(如「auth」模块只有 user 表)

```
/entity-coder 模块=auth

请基于 docs/DATABASE_DESIGN.md §3 中"用户"相关表(user 表),生成 Entity + Mapper:
- backend/src/main/java/com/example/property/entity/User.java
- backend/src/main/java/com/example/property/mapper/UserMapper.java
完成输出 diff。
```

#### 多表模块(如「auth」模块有 user + user_role 2 张表)

```
/entity-coder 模块=auth

请基于 docs/DATABASE_DESIGN.md §3 中"用户"相关表(user 表 + user_role 表),生成 2 个 Entity + 2 个 Mapper:
- entity/User.java + mapper/UserMapper.java
- entity/UserRole.java + mapper/UserRoleMapper.java
完成输出 diff。
```

### §一 自检 checklist(首次生成模式)

完成后请按以下清单自检,任何 ❌ 项重新生成对应文件:

- [ ] 必读文件缺失检查全部通过(DATABASE_DESIGN.md 存在 + 非占位 + 模块表名找得到 + `模块=<X>` 参数已传)
- [ ] **N 张表生成 N 个 Entity + N 个 Mapper**(每表 1 个 · 不合并字段)
- [ ] Entity 命名:**单数 + 大驼峰 + 下划线转驼峰**(`user_role` → `UserRole`)· Mapper 命名:`<EntityName>Mapper`
- [ ] Entity 文件**扁平**放 `entity/`,Mapper **扁平**放 `mapper/`(都不分模块子目录)
- [ ] **字段顺序与 CREATE TABLE 字段顺序一致**(`id` → 业务 → `is_deleted` → `create_time` → `update_time`)
- [ ] `@TableName("<表名>")` **显式声明**(对齐 CLAUDE.md §二·二)
- [ ] 主键加 `@TableId(type = IdType.AUTO)`
- [ ] 时间字段映射 `LocalDateTime`(**未用 `Date`**)· import `java.time.LocalDateTime`
- [ ] **`DECIMAL` 字段映射 `BigDecimal`**(**未用 `Double` / `Float`**)· import `java.math.BigDecimal`
- [ ] **`is_deleted` 字段加 `@TableLogic`**(若 db-designer §3 该表含此字段)
- [ ] 密码字段加 `@JsonIgnore` + 命令文件已提示用 `RegisterDTO` 接收(由 service-coder 生成)
- [ ] `@Data` Lombok 注解
- [ ] **Mapper 接口 `extends BaseMapper<Entity>` + `@Mapper` 注解 + 空方法体**(简单 CRUD 走 BaseMapper)
- [ ] 字段类型逐字段符合「SQL → Java 字段类型映射表」
- [ ] **未生成 DTO**(DTO 由 service-coder 生成 · 对齐 CLAUDE.md §二·三 DTO 命名约定)

---

## §二 应用修复模式(R-05 issue 处理 · 协议跟 srs-writer §二 / db-designer §二 / api-designer §二 一致)

### 触发场景

`/code-reviewer-be` 完成审核后,`backend/src/main/java/{{包路径}}/entity/<X>.java` 或 `mapper/<X>Mapper.java` 中已有 `// R-05-issue-编号: 严重度 - 描述` 行注释。此时再次调用本命令进入"应用修复"模式。

> ⚠️ **职责边界**(R-05 多文件场景 · 二段循环协议第 4 次应用首次拆分):
>
> | 命令 | 修复范围 |
> |---|---|
> | **本命令(entity-coder §二)** | 只修 `entity/` + `mapper/` 下的 R-05 注释 |
> | **`/service-coder 应用修复`** | 修 `service/` + `service/impl/` + `controller/` 下的 R-05 注释 |
>
> 拆分原因:R-05 跨多文件 · 不同层修复策略不同(Entity 改字段类型 / Service 改业务逻辑 / Controller 改路由参数)· 各命令各管一段,避免双重修改冲突。

### 输入

- **必读**:`backend/src/main/java/{{包路径}}/entity/`(reviewer 已插入 R-05 注释的 .java 版本)
- **必读**:`backend/src/main/java/{{包路径}}/mapper/`(若 reviewer 注释也涉及 Mapper)
- **必读**:`docs/对话记录/Phase4-R05-<模块名>-review-<日期>.md`(reviewer 报告 · 含每条 issue 的修复建议)
- 用户调用形式:`/entity-coder 应用修复 模块=<X>` 或 `/entity-coder 请扫描 entity/ + mapper/ 下 R-05 注释逐条修复`

### 输出指令

1. 扫描 `entity/` + `mapper/` 下指定模块的 .java 文件中所有 `// R-05-issue-...` 注释
2. 对每条注释:
   - 修改对应字段 / 注解 / import / 类型映射(基于 reviewer 报告的修复建议 + 本命令 §一 字段类型映射表)
   - 把注释改为 `// R-05-issue-编号: 已修复 - 一句话修复说明`
3. **不重写整个文件** —— 只 in-place 改动 issue 涉及的字段/注解,其他原文一字不动
4. **不碰 service/ + service/impl/ + controller/** —— 那是 service-coder §应用修复 的职责
5. 输出 diff(显示每个 issue 的改前/改后 + 涉及文件清单)

### §二 自检 checklist(应用修复模式)

- [ ] 所有 entity/ + mapper/ 下的 R-05 注释都已标记"已修复"(没遗漏)
- [ ] 修复内容覆盖 reviewer 报告的 issue 要点
- [ ] 未涉及 issue 的字段/注解原文一字不动(in-place 修复要求)
- [ ] 字段顺序未破坏(对齐 §一 字段顺序约定)
- [ ] 字段类型仍符合「SQL → Java 字段类型映射表」(若 reviewer 改过 SQL,需先确认 db-designer §二 应用修复已同步 sql/01-init.sql,再来改 Entity)
- [ ] **未碰 `service/` + `service/impl/` + `controller/` 下的注释**(职责边界)
- [ ] 输出 diff 含改前/改后对比

### ✅ entity/mapper 段 R-05 闭环后 · 下一步硬指令(防 builder 跨命令幻觉)

**当前位置**:Phase 4 模块循环 Step 5(R-05 拆分协议 · entity/mapper 段已修)→ **下一步必须是 `/service-coder 应用修复 模块=<X>`**(同模块 · 接对话不新建 · 详见 service-coder §二)。

**完成提示模板**(builder 在 entity/mapper 段闭环后必须输出 · 一字不漏):
> ✅ 模块 `<X>` 的 entity/ + mapper/ 下 R-05 注释已闭环(N 条修复)。**下一步必须调用 `/service-coder 应用修复 模块=<X>`**(R-05 拆分协议 · 修 service/ + impl/ + controller/ + dto/ 下剩余 R-05 注释 · 接对话不退出 `claude` 重启)。**只有两段都跑完才能进 git-committer**。

**⛔ 禁止下列幻觉**:
- ⛔ **不要**直接 `/git-committer`——R-05 多文件拆分协议要求 entity 段 + service 段**都跑完**才能 commit,否则 commit 不完整
- ⛔ **不要**跳到下一个模块 `/entity-coder 模块=<Y>`——本模块 R-05 还没修完
- ⛔ **不要**抢答 `/code-reviewer-be`——R-05 已经审完了,正在应用修复
- ⛔ **不要**跳到 Phase 5 `/axios-coder`——本模块还在 Phase 4

---

## ⚠️ 不允许

- ❌ **编造 DATABASE_DESIGN.md 中没有的字段**(若不确定先问)
- ❌ **DECIMAL 字段映射 `Double` / `Float`**(精度丢失 · CLAUDE.md §二·二 明示禁止)
- ❌ **时间字段映射 `Date`**(必用 `LocalDateTime` · CLAUDE.md §二·二)
- ❌ **软删除字段不加 `@TableLogic`**(MP 默认行为下软删数据仍出现在查询结果)
- ❌ **一次性生成所有模块的 Entity**(必须按 `模块=<X>` 参数限定 · 对齐 08b §8.6 工时拆分)
- ❌ **Entity 文件分模块子目录**(扁平 `entity/` · 对齐 init-skeleton)
- ❌ **生成 DTO 类**(DTO 由 service-coder 生成 · 对齐 CLAUDE.md §二·三)
- ❌ **修改 service/ + impl/ + controller/ 下的 R-05 注释**(应用修复模式职责边界 · 由 service-coder §应用修复 处理)
- ❌ **在 Mapper 接口里写复杂查询**(简单 CRUD 走 BaseMapper · 复杂查询走 CLAUDE.md §二·四 例外路径)

## 衔接

Entity + Mapper 生成后,Phase 4 继续(详见 08b §8.6 通用模块流程):

- **Step 2**:`/service-coder 模块=<X>` 生成 Service+ServiceImpl+Controller 三件套(读 API_DESIGN.md + 本命令生成的 Entity + Mapper)
- **Step 3**:启动 SpringBoot + Postman 测试 · 报错用 `/bug-tracer-be` 排查
- **Step 4**:`/code-reviewer-be <模块>` 切模型审核(R-05 注释插入 entity/ + mapper/ + service/ + impl/ + controller/ 全部相关文件 · 位置参数小写=模块切片)
- **Step 5 拆分修复**:
  - `/entity-coder 应用修复 模块=<X>`(本命令 §二 · 修 `entity/` + `mapper/`)
  - `/service-coder 应用修复 模块=<X>`(改 `service/` + `impl/` + `controller/`)
- **Step 6**:`/git-committer 请 commit + push:feat(p4-<X>): <模块名> Entity+Mapper+Service+R-05 修复`(对齐 CLAUDE.md §四 scope phase 前缀)

## 设计要点

- **轻量任务**:用 V4 Flash 即可,不需要 V4 Pro
- **每模块独立调用**:对齐 08b §8.6 工时拆分(3-4 个模块 × 每模块独立 退出 `claude` 重启)
- **同时生成 Entity + Mapper · 解决第 4 次链路断点**(2026-05-10 entity-coder 审核确立 · 对齐 README L30 + 06 G-08 原始设计意图 · 详见审核记录跨命令同步段)
- **基于事实**:扫描 DATABASE_DESIGN.md §3 真实 SQL,不依赖学生记忆,不编造字段
- **职责边界明确**:本命令只生成 entity/ + mapper/ · DTO 由 service-coder 生成(详见 CLAUDE.md §二·三 DTO 命名约定)
- **二段循环协议第 4 次应用 · 多文件场景首次拆分**:R-05 entity/ + mapper/ 修复由 §二 处理 · service+controller 由 service-coder §应用修复 处理(协议跟 R-01/R-03/R-04 一致 · 但 R-05 跨多文件,首次按目录边界拆分到 2 个命令)

---

> 📋 **跨文件呼应导航**:
> - **上游产出**:`DATABASE_DESIGN.md §3`(CREATE TABLE 9 项字段约定 + 字段顺序)+ `§3 #6`(SQL 字段类型规则)
> - **平行规则**:`CLAUDE.md §二·一`(分层 8 类 · entity/ + mapper/ 行 + 关键类示例)+ `§二`(Entity 规范)+ `§三`(DTO 命名 · 由 service-coder 应用)+ `§四`(MyBatis-Plus 用法 · 复杂查询例外)
> - **全栈契约**:`CLAUDE.md §一·一·后端`(JDK 21 + MP 3.5.15 + Lombok 1.18.46 版本)+ `§二`(BCrypt + LambdaQueryWrapper)
> - **下游消费**:`service-coder.md`(读本命令生成的 Entity + Mapper · 生成 Service 三件套)
> - **API 序列化**:`API_DESIGN.md §1`(时间 ISO 8601 · 字段命名小驼峰)+ `§4`(Result<T> 序列化时 Entity 字段)
> - **骨架占位**:`init-skeleton.md backend/src/main/java/{{包路径}}/entity/ + mapper/`(本命令填充)
> - **审核协议**:`code-reviewer-be.md`(R-05 · 待审 Phase 4 第 3)+ 二段循环协议跟 `srs-reviewer / db-reviewer / api-reviewer` 一致
