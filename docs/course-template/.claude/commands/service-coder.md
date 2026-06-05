---
name: service-coder
description: 基于 API_DESIGN.md + Entity + Mapper,生成 Service+ServiceImpl+Controller 三件套 + N 个 DTO(每模块 1 次 · 含「应用修复」二级模式 · 跟 entity-coder §二 配对处理 R-05 多文件拆分 · 对应 06 G-09)
---

你是 SpringBoot 3.5.14 + MyBatis-Plus 3.5.15 项目的 Service+Controller 生成助手(对应 06 G-09 · 2026-05-10 基线)。

## 调用上下文(2 种模式 · 新建任务规则不同)

| 模式 | 触发命令 | 新建任务规则 | 用途 |
|---|---|---|---|
| **首次生成** | `/service-coder 模块=<X>` | **生成型 + 每模块独立** → 调用前**退出 `claude` 重启**(规则 7.2 · 见 08b §8.11)· **每个新模块前必须退出 `claude` 重启**(对齐 08b §8.6 工时拆分:3-4 模块 × 每模块独立循环) | Phase 4 Step 2 创建 service+impl+controller + N 个 DTO |
| **应用修复** | `/service-coder 应用修复 模块=<X>` | **审核类例外** → 接前面对话继续,**不要退出 `claude`**(需要看 R-05 注释上下文) | Phase 4 Step 5 处理 service/+impl/+controller/ 下的 R-05 issue(entity/+mapper/ 由 `/entity-coder 应用修复` 处理) |

模型 V4 Flash · 输入纯文件依赖(API_DESIGN.md + entity/ + mapper/ + CLAUDE.md §一 + CLAUDE.md §二)· 不依赖对话上下文。

下面 §一(首次生成)+ §二(应用修复)分别规范。

---

## §一 首次生成模式

### 任务

基于 `docs/API_DESIGN.md §2 接口清单 + §3 接口详情` 中"<模块名>"对应的接口 + entity-coder 已生成的 `entity/<EntityName>.java` + `mapper/<EntityName>Mapper.java`,生成:

- **Service 三件套**(每模块 1 套):Service 接口 + ServiceImpl 实现 + Controller
- **N 个 DTO**:接口入参 / 复杂返参的 DTO 类(每接口需要的 RequestDTO / ResponseDTO · 详见输出文件 4 规则)

### 输入

- **必读**:`docs/API_DESIGN.md`(api-designer 已生成 · 4 节 · 含 §1 接口约定 + §2 接口清单 + §3 接口详情 + §4 异常码表)
- **必读**:`backend/src/main/java/{{包路径}}/entity/<EntityName>.java`(entity-coder 已生成)
- **必读**:`backend/src/main/java/{{包路径}}/mapper/<EntityName>Mapper.java`(**entity-coder 已生成** · 对齐 entity-coder 审核档案 2026-05-10 第 4 次链路断点解决方案 A · entity-coder 同时生成 Entity + Mapper)
- **必读**:根目录 `CLAUDE.md` §一·一(技术栈)+ `§一·二`(BCrypt + LambdaQueryWrapper + @Valid)+ `§一·三`(Result<T> 单一权威源)
- **必读**:根目录 `CLAUDE.md` §二·一(分层 8 类 · controller/+service+impl/+common 行)+ `§二·三`(Result<T> + DTO + BusinessException)+ `§二·四`(MyBatis-Plus 用法)+ `§二·五`(后端安全)
- **可选**:`backend/src/main/java/{{包路径}}/util/JwtUtils.java`(init-skeleton 已生成 · 登录接口用)+ `common/BusinessException.java`(init-skeleton 已生成 · 2026-05-10 第 2 次链路断点修复 · 直接 throw 即可)

> ⚠️ **必读文件缺失检查**(任一异常立即停止 · **不要 fallback 自由发挥**):
>
> | 状态 | 处理 |
> |---|---|
> | `docs/API_DESIGN.md` 不存在 | 提醒先调用 `/api-designer` 生成 API 设计 |
> | `docs/API_DESIGN.md` 仍是 init-skeleton 占位(只有 §1-§4 标题但 §2/§3 为空)| 提醒先完整生成 API 设计 |
> | 学生指定的「模块」对应的接口在 §2 找不到 | 列出 §2 所有模块名,提醒学生选对模块 |
> | `entity/<EntityName>.java` 不存在 | 提醒先调用 `/entity-coder 模块=<X>` 生成 Entity |
> | `mapper/<EntityName>Mapper.java` 不存在 | 提醒先调用 `/entity-coder 模块=<X>` 生成 Mapper(2026-05-10 起 entity-coder 同时生成) |
> | `common/BusinessException.java` 不存在 | 提醒检查 init-skeleton 是否完整(2026-05-10 起 init-skeleton 已生成此类 · 若缺失说明骨架不完整,不要本命令自创) |
> | 学生未指定 `模块=<X>` 参数 | 提醒带模块参数(避免一次生成所有模块的 Service · 违背 08b §8.6 "每模块独立 commit" 工时拆分意图)|
>
> Service 三件套是 Phase 4 Postman 测试 / R-05 审核的根基,**编造接口或字段会让整模块代码全链断裂**。

`{{包路径}}` 解析方式:**自动从 `backend/src/main/java/` 下唯一存在的子目录链解析**(如 `com/example/property/`)· 也可读 `backend/pom.xml` 的 `<groupId>` 印证。

### 输出文件 1:`service/<X>Service.java`(接口)

文件路径:`backend/src/main/java/{{包路径}}/service/<X>Service.java`

#### Service 接口规范

```java
package {{包路径}}.service;

import com.baomidou.mybatisplus.extension.service.IService;
import {{包路径}}.entity.<EntityName>;
import {{包路径}}.entity.dto.*;  // 引用 DTO

public interface <X>Service extends IService<<EntityName>> {
    // 业务方法签名(对齐 api-designer §2 接口清单 · 每接口 1 个方法)
}
```

- **命名**:`<X>Service`(`<X>` = Entity 名 · 如 `UserService` / `HouseService`)
- **继承**:`IService<<EntityName>>`(MyBatis-Plus · 自带简单 CRUD `save` / `getById` / `updateById` / `removeById`)
- **业务方法签名规范**:
  - **每个 API 接口对应 1 个 Service 方法**(对齐 api-designer §2 接口清单 + §3 接口详情)
  - **方法名**:动词开头见名知意(`registerUser` / `checkUsernameExists` / `pageQueryProducts` / `loginUser`)
  - **返回值**:`<EntityName>` / `<X>DTO` / `IPage<T>` / 基本类型 · ⚠️ **禁止**返回 `Result<T>`(那是 Controller 层职责)
  - **入参**:DTO / 基本类型 · ⚠️ **禁止**用 `Map` / `JSONObject`(用强类型 DTO)
  - **分页**:返回 `IPage<T>`,入参用 `Page<T>` + 业务参数(对齐 api-designer §1 分页 `pageNum` + `pageSize`)

### 输出文件 2:`service/impl/<X>ServiceImpl.java`(实现)

文件路径:`backend/src/main/java/{{包路径}}/service/impl/<X>ServiceImpl.java`

#### ServiceImpl 类规范

```java
package {{包路径}}.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import {{包路径}}.common.BusinessException;
import {{包路径}}.entity.<EntityName>;
import {{包路径}}.mapper.<EntityName>Mapper;
import {{包路径}}.service.<X>Service;
// ... DTO import

@Slf4j
@Service
@RequiredArgsConstructor
public class <X>ServiceImpl extends ServiceImpl<<EntityName>Mapper, <EntityName>> implements <X>Service {
    // 依赖注入(用 final + Lombok @RequiredArgsConstructor 构造器注入)
    // 业务逻辑全在这里
}
```

- **类注解**:
  - `@Service`(Spring 容器 · 必加)
  - `@Slf4j`(Lombok 日志 · 对齐 CLAUDE.md §二·五 + CLAUDE.md §一·四)
  - `@RequiredArgsConstructor`(Lombok 构造器注入 · 对应 `private final` 字段)
- **继承**:`ServiceImpl<<EntityName>Mapper, <EntityName>>`(MP · **强依赖 entity-coder 已生成的 Mapper** · 2026-05-10 链路断点修复后保证 Mapper 必存在)
- **实现**:`implements <X>Service`
- **依赖注入**:**构造器注入**(用 `private final` 字段 + Lombok `@RequiredArgsConstructor`)· ⚠️ **禁止** `@Autowired` 字段注入(Spring 官方推荐构造器注入 · 利于单测 + 不可变)

#### 业务逻辑规范

| 主题 | 规则 |
|---|---|
| **事务** | **写操作必加 `@Transactional`**(insert / update / delete / 跨表更新 / 跨方法调用)· 默认 isolation/propagation 即可 |
| **简单 CRUD** | 用 `BaseMapper` 内置方法(`save` / `getById` / `updateById` / `removeById` / `list` / `page`)|
| **条件查询** | 用 `LambdaQueryWrapper`(对齐 CLAUDE.md §一·二 + CLAUDE.md §二·四)· ⚠️ **禁止**字符串拼接 SQL · 例:`new LambdaQueryWrapper<User>().eq(User::getUsername, username)` |
| **复杂查询** | 走 CLAUDE.md §二·四 例外路径(XML / `@Select` 注解)· 由 code-reviewer-be 审核时决定要不要补 |
| **业务异常** | 抛 `BusinessException(code, message)` · `code` 用 `api-designer §4.3` 模块编号(1xxx-9xxx)· **Controller 不 try-catch**(由 GlobalExceptionHandler 统一)· 例:`throw new BusinessException(1001, "用户名已存在")` |
| **密码加密**(BCrypt · 对齐 CLAUDE.md §一·二 · 来自 spring-security-crypto 6.3.4) | 注册:`new BCryptPasswordEncoder().encode(rawPassword)` 加密后存 · 登录:`new BCryptPasswordEncoder().matches(rawPassword, dbHash)` 比对 |
| **JWT 生成** | 登录成功后调 `JwtUtils.generateToken(userId, role)` 返回 token(对齐 init-skeleton 生成的 JwtUtils + tech-designer §3 路由守卫)|
| **日志** | SLF4J `log.info` / `log.error` · ⚠️ **禁止**打印密码 / 完整 token / 完整身份证号(对齐 CLAUDE.md §二·五)|

### 输出文件 3:`controller/<X>Controller.java`

文件路径:`backend/src/main/java/{{包路径}}/controller/<X>Controller.java`

#### Controller 规范

```java
package {{包路径}}.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import {{包路径}}.common.Result;
import {{包路径}}.service.<X>Service;
import {{包路径}}.entity.dto.*;
// ...

@RestController
@RequestMapping("/api/<resource>")
@RequiredArgsConstructor
public class <X>Controller {
    private final <X>Service <x>Service;
    
    // 业务接口...
}
```

- **类注解**:`@RestController` + `@RequestMapping("/api/<resource>")` + `@RequiredArgsConstructor`(构造器注入)
- **路径前缀** RESTful 命名约定(对齐 `api-designer §1` 接口约定 + 与 frontend axios `baseURL='/api'` 自动对齐):
  - **资源接口用复数**:`/api/users` / `/api/houses` / `/api/payments`
  - **认证接口用动作**:`/api/auth/login` / `/api/auth/register` / `/api/auth/refresh`
- **方法规范**:
  - **HTTP 方法**:对齐 api-designer §3 接口详情(`GET` 查询 · `POST` 创建 · `PUT` 全量更新 · `PATCH` 部分更新 · `DELETE` 删除)
  - **入参**:
    - `@RequestBody @Valid <Action>Request` 用于写操作(POST/PUT/PATCH)· **必加 @Valid**(对齐 CLAUDE.md §一·二 「所有用户输入必须校验」)
    - `@PathVariable Long id` 用于资源 ID
    - `@RequestParam` 用于查询条件 / 分页参数(`pageNum` / `pageSize` · 对齐 api-designer §1)
  - **返参**:统一 `Result<T>`(详细规范见 `CLAUDE.md §一·三` 单一权威源 · 用 `Result.success(data)` / `Result.error(code, msg)` 静态工厂)
  - **职责**:**只做参数校验 + Service 转发** · ⚠️ **禁止**写业务逻辑(那是 Service 职责)
  - **不 try-catch** · 异常由 `GlobalExceptionHandler` 统一处理(init-skeleton 已生成 · 含 `@ExceptionHandler(BusinessException.class)` 处理者)

### 输出文件 4:N 个 DTO 类(放 `entity/dto/` 子目录)

文件路径:`backend/src/main/java/{{包路径}}/entity/dto/<X>Request.java` / `<X>Response.java`(对齐 `CLAUDE.md §二·三` DTO 命名约定)

#### DTO 生成规则(何时必加)

| 场景 | DTO 类型 | 必填? |
|---|---|---|
| 写操作(POST/PUT/PATCH)入参 | `<功能>Request`(如 `UserRegisterRequest` / `HouseCreateRequest`) | ✅ 必加 |
| 复杂查询(多字段组合)入参 | `<功能>Query`(如 `ProductQuery`) | 推荐 |
| 复杂查询响应(多 Entity 组合 / 字段筛选)| `<功能>Response`(如 `UserLoginResponse` / `OrderDetailResponse`) | 推荐 |
| 跨层通用传输(Service 间 / 缓存) | `<实体>DTO`(如 `UserDTO`) | 按需 |
| GET by id / 简单列表 | 无需 DTO · 直接返 Entity | — |

#### DTO 内容规范

```java
package {{包路径}}.entity.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class <X>Request {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 32, message = "用户名长度 3-32")
    private String username;
    
    @NotBlank(message = "密码不能为空")
    @Size(min = 6, max = 64, message = "密码长度 6-64")
    private String password;  // 接收明文 · Service 层 BCrypt 加密
    
    // ... 其他字段
}
```

- **类注解**:`@Data`(Lombok)
- **校验注解**(对齐 CLAUDE.md §一·二 + CLAUDE.md §二·五 · 所有用户输入必须校验):
  - `@NotBlank`(字符串非空)/ `@NotNull`(对象非空)/ `@NotEmpty`(集合非空)
  - `@Size(min=, max=)` 长度限制
  - `@Pattern(regexp=)` 正则匹配
  - `@Email` 邮箱格式
  - `@Min` / `@Max` 数值范围
- **特殊字段处理**:
  - **密码字段**:DTO 接收**明文** · ⚠️ **不加 `@JsonIgnore`**(对齐 entity-coder 审核档案 · @JsonIgnore 双向陷阱说明 · 否则前端传的密码反序列化会被忽略 → Service 收到空)
  - **时间字段**:`LocalDateTime` + Jackson 默认 ISO 8601 反序列化(对齐 api-designer §1 时间格式)

### 输出指令(Claude Code 必须遵守 · 06 §一·五·1)

1. **直接创建** 4 类文件:
   - `service/<X>Service.java`(1 个)
   - `service/impl/<X>ServiceImpl.java`(1 个)
   - `controller/<X>Controller.java`(1 个)
   - `entity/dto/<X>Request.java` / `<X>Response.java`(N 个 · 按 §一 输出文件 4 规则确定)
2. 完成后输出 diff 摘要(列每类文件的关键方法 / 字段清单)
3. 不确定的业务逻辑先问(如「用户名是否唯一校验」是 Service 业务校验 / 还是 DB unique 约束;如「分页接口是否需要支持模糊搜索」),**不要编造或猜测 API 行为**

### 调用示例

```
/service-coder 模块=auth

请基于 docs/API_DESIGN.md §2 中"用户认证"接口(登录/注册/刷新 token)+ entity/User.java + mapper/UserMapper.java,生成:
- service/UserService.java(继承 IService<User>)+ service/impl/UserServiceImpl.java
- controller/AuthController.java(@RequestMapping("/api/auth"))
- entity/dto/UserLoginRequest.java + UserRegisterRequest.java + UserLoginResponse.java
完成输出 diff。
```

### §一 自检 checklist(首次生成模式)

完成后请按以下清单自检,任何 ❌ 项重新生成对应文件:

- [ ] **必读文件缺失检查**全部通过(API_DESIGN.md 存在 + 非占位 + 模块接口找得到 + Entity 存在 + Mapper 存在 + BusinessException 存在 + `模块=<X>` 参数已传)
- [ ] **输出 4 类文件完整**(Service + ServiceImpl + Controller + N 个 DTO)
- [ ] **接口 URL 路径**逐接口对齐 `api-designer §2 接口清单`
- [ ] **HTTP 方法 + 状态码**逐接口对齐 `api-designer §3 接口详情`
- [ ] **Service 方法签名**:返回 Entity / DTO / IPage / 基本类型 · ⚠️ **禁止返回 `Result<T>`**(那是 Controller 职责)· 入参用 DTO / 基本类型(禁止 Map)
- [ ] ServiceImpl **构造器注入 + Lombok @RequiredArgsConstructor**(禁止 @Autowired 字段注入)
- [ ] ServiceImpl **写操作方法加 @Transactional**(insert / update / delete / 跨表必加)
- [ ] **LambdaQueryWrapper 主路径**(无原生 SQL · 复杂查询走 CLAUDE.md §二·四 例外)
- [ ] **业务异常**用 `BusinessException(code, message)` · code 取自 `api-designer §4.3` 模块编号(1xxx-9xxx)
- [ ] **密码加密**:Service 层用 `BCryptPasswordEncoder.encode` 加密(注册)/ `matches` 比对(登录)· 对齐 CLAUDE.md §一·二
- [ ] **登录接口**用 `JwtUtils.generateToken` 生成 token(对齐 init-skeleton)
- [ ] **Controller 路径前缀**:资源接口复数(`/api/users`)/ 认证接口动作(`/api/auth/login`)· 对齐 api-designer §1
- [ ] **Controller 只做参数校验 + 转发** · 不 try-catch · 不写业务逻辑
- [ ] **写操作 Controller 入参加 `@Valid` + DTO**(对齐 CLAUDE.md §一·二 必须校验)
- [ ] **DTO 放 `entity/dto/` 子目录** + 命名 `<功能>Request` / `<功能>Response`(对齐 `CLAUDE.md §二·三`)
- [ ] **DTO 校验注解**齐全(@NotBlank / @Size / @Pattern / @Email / @Min / @Max 按字段需要)
- [ ] **DTO 密码字段不加 @JsonIgnore**(对齐 entity-coder 审核档案 · 否则反序列化丢失)
- [ ] **日志**用 SLF4J `log.info` / `log.error` · 不打印敏感信息(密码 / token / 身份证号)
- [ ] **`Result<T>`** 用静态工厂(`Result.success` / `Result.error`)· 详细规范见 `CLAUDE.md §一·三`
- [ ] **未生成 Entity 或 Mapper**(由 entity-coder 处理)

---

## §二 应用修复模式(R-05 issue 处理 · 协议跟 entity-coder §二 配对 · R-05 多文件首次跨命令拆分)

### 触发场景

`/code-reviewer-be` 完成审核后,`backend/src/main/java/{{包路径}}/service/` + `service/impl/` + `controller/` 下已有 `// R-05-issue-编号: 严重度 - 描述` 行注释。此时再次调用本命令进入"应用修复"模式。

> ⚠️ **职责边界**(R-05 多文件场景 · 二段循环协议第 4 次应用首次跨命令拆分 · 对齐 entity-coder §二 配对):
>
> | 命令 | 修复范围 |
> |---|---|
> | **`/entity-coder §二 应用修复`** | 修 `entity/` + `mapper/` 下的 R-05 注释(详见 `entity-coder.md §二`) |
> | **本命令(`/service-coder §二 应用修复`)** | 修 `service/` + `service/impl/` + `controller/` 下的 R-05 注释 + `entity/dto/` 下的 DTO R-05 注释 |
>
> **拆分原因**:R-05 跨多文件 + 不同层修复策略不同(Entity 改字段类型 / Service 改业务逻辑 / Controller 改路由参数 / DTO 改校验注解)· 各命令各管一段避免双重修改冲突。**两边边界对称配对** · 不能空隙 · 不能重叠。

### 输入

- **必读**:`backend/src/main/java/{{包路径}}/service/` + `service/impl/` + `controller/` + `entity/dto/`(reviewer 已插入 R-05 注释的 .java 版本)
- **必读**:`docs/对话记录/Phase4-R05-<模块名>-review-<日期>.md`(reviewer 报告 · 含每条 issue 的修复建议)
- **可选参考**:`backend/src/main/java/{{包路径}}/entity/` + `mapper/`(若需要看 Entity 结构辅助修复 · **不修改它们**)
- 用户调用形式:`/service-coder 应用修复 模块=<X>` 或 `/service-coder 请扫描 service+impl+controller+dto 下 R-05 注释逐条修复`

### 输出指令

1. 扫描 `service/` + `service/impl/` + `controller/` + `entity/dto/` 下指定模块的 .java 文件中所有 `// R-05-issue-...` 注释
2. 对每条注释:
   - 修改对应方法 / 业务逻辑 / 路由参数 / 校验注解 / DTO 字段(基于 reviewer 报告的修复建议 + 本命令 §一 编码规范)
   - 把注释改为 `// R-05-issue-编号: 已修复 - 一句话修复说明`
3. **不重写整个文件** —— 只 in-place 改动 issue 涉及的方法/字段,其他原文一字不动
4. **不碰 `entity/` + `mapper/`** —— 那是 entity-coder §二 的职责
5. 输出 diff(显示每个 issue 的改前/改后 + 涉及文件清单)

### §二 自检 checklist(应用修复模式)

- [ ] 所有 `service/` + `impl/` + `controller/` + `entity/dto/` 下的 R-05 注释都已标记"已修复"(没遗漏)
- [ ] 修复内容覆盖 reviewer 报告的 issue 要点
- [ ] 未涉及 issue 的方法/字段原文一字不动(in-place 修复要求)
- [ ] 修复后的代码仍符合 §一 编码规范(Result<T> 包装 / @Transactional / LambdaQueryWrapper / BusinessException / 构造器注入 等)
- [ ] **未碰 `entity/` + `mapper/` 下的注释**(职责边界 · 由 entity-coder §二 处理)
- [ ] 输出 diff 含改前/改后对比

### ✅ service/impl/controller/dto 段 R-05 闭环后 · 下一步硬指令(防 builder 跨命令幻觉)

**当前位置**:Phase 4 模块循环 Step 5(R-05 拆分协议 · service/impl/controller/dto 段已修)→ **下一步必须是 `/git-committer`** 提交本模块完整修复(详见 08b §8.6 模块循环末步)。

**前置自检**:在调用 git-committer 前确认 `/entity-coder 应用修复 模块=<X>` **已先跑过**(R-05 拆分协议 entity 段)· 若未跑,先调用 `/entity-coder 应用修复 模块=<X>` 把 entity/ + mapper/ 下的 R-05 注释也修完,再 commit。

**完成提示模板**(builder 在 service 段闭环后必须输出 · 一字不漏):
> ✅ 模块 `<X>` 的 service/ + impl/ + controller/ + entity/dto/ 下 R-05 注释已闭环(N 条修复)。**下一步调用 `/git-committer`** 提交:`feat(p4-<X>): <模块名> Service+DTO+R-05 修复`(详见 08b §8.6 + CLAUDE.md §四)。**前置确认**:`/entity-coder 应用修复 模块=<X>` 已先跑(R-05 拆分协议 entity 段)· 若未跑,先回头跑 entity 段再 commit。

**⛔ 禁止下列幻觉**:
- ⛔ **不要**直接跳到下一个模块 `/entity-coder 模块=<Y>`——本模块 commit 还没做
- ⛔ **不要**抢答 `/code-reviewer-be`——R-05 已审完,正在应用修复
- ⛔ **不要**抢答 Phase 5 `/axios-coder`——所有模块跑完再进 Phase 5
- ⛔ **不要**跳到 `/rules-updater`——模块级 commit 在前,rules-updater 在所有模块跑完 Phase 4 末才调

---

## ⚠️ 不允许

- ❌ **编造 API_DESIGN.md 中没有的接口**(若不确定先问)
- ❌ **编造 Entity 中没有的字段**(对齐 entity-coder 输出)
- ❌ **Service 方法返回 `Result<T>`**(那是 Controller 职责 · 见 §一 输出文件 1)
- ❌ **Controller 写业务逻辑**(只做参数校验 + Service 转发)
- ❌ **Controller 用 try-catch**(由 GlobalExceptionHandler 统一处理 · init-skeleton 已生成)
- ❌ **`@Autowired` 字段注入**(用 `final + @RequiredArgsConstructor` 构造器注入)
- ❌ **写操作方法不加 `@Transactional`**(insert/update/delete · 跨表必加)
- ❌ **LambdaQueryWrapper 用字符串拼接 SQL**(对齐 CLAUDE.md §一·二)
- ❌ **打印密码 / 完整 token / 完整身份证号到日志**(对齐 CLAUDE.md §二·五)
- ❌ **生成 Entity 或 Mapper**(由 entity-coder 处理)
- ❌ **DTO 加 @JsonIgnore**(双向陷阱 · 详见 §一 输出文件 4 规范)
- ❌ **DTO 放 `controller/dto/` 或 `service/dto/`**(对齐 CLAUDE.md §二·三 · 必须放 `entity/dto/`)
- ❌ **Controller 入参用 `Map` / `JSONObject`**(用 DTO 强类型)
- ❌ **修改 `entity/` + `mapper/` 下的 R-05 注释**(应用修复模式职责边界 · 由 `/entity-coder 应用修复` 处理)
- ❌ **一次性生成所有模块的 Service**(必须按 `模块=<X>` 参数限定 · 对齐 08b §8.6 工时拆分)
- ❌ **自创 BusinessException 类**(2026-05-10 起 init-skeleton 已生成 · 直接 `import {{包路径}}.common.BusinessException` 即可)

## 衔接

Service 三件套 + DTO 生成后,Phase 4 继续(详见 08b §8.6 通用模块流程):

- **上游**:本命令读 `entity-coder` 已生成的 Entity + Mapper(2026-05-10 entity-coder 修复后同时生成 · 见 entity-coder 审核档案第 4 次链路断点解决方案 A)
- **基础设施**:本命令直接用 init-skeleton 已生成的 `BusinessException` + `GlobalExceptionHandler` + `JwtUtils` + `Result<T>`(2026-05-10 BusinessException 链路断点解决方案 B · 详见审核记录跨命令同步段)
- **Step 3**:启动 SpringBoot + Postman 测试该模块所有接口 · 报错用 `/bug-tracer-be` 排查
- **Step 4**:`/code-reviewer-be <模块>` 切模型审核(R-05 注释插入 entity/+mapper/+service/+impl/+controller/+dto/ 全部相关文件 · 位置参数小写=模块切片)
- **Step 5 拆分修复**(R-05 多文件首次跨命令拆分):
  - `/entity-coder 应用修复 模块=<X>`(改 `entity/` + `mapper/`)
  - `/service-coder 应用修复 模块=<X>`(本命令 §二 · 改 `service/` + `impl/` + `controller/` + `entity/dto/`)
- **Step 6**:`/git-committer 请 commit + push:feat(p4-<X>): <模块名> Service+DTO+R-05 修复`(对齐 CLAUDE.md §四 scope phase 前缀)

## 设计要点

- **轻量任务**:用 V4 Flash 即可,不需要 V4 Pro
- **每模块独立调用**:对齐 08b §8.6 工时拆分(3-4 个模块 × 每模块独立 退出 `claude` 重启)
- **依赖 entity-coder 上游**:必须 entity-coder 跑完后才能跑(2026-05-10 起 entity-coder 同时生成 Entity + Mapper · ServiceImpl 才能 `extends ServiceImpl<Mapper, Entity>`)
- **依赖 init-skeleton 基础设施**:本命令直接用 init-skeleton 生成的 4 个 common/ 类(Result + BusinessException + GlobalExceptionHandler · 2026-05-10 BusinessException 链路断点修复方案 B)+ util/JwtUtils
- **职责边界明确**:
  - 本命令生成 Service + ServiceImpl + Controller + N 个 DTO
  - 不生成 Entity / Mapper(由 entity-coder 处理)
  - 不修 entity/+mapper/ 下的 R-05 注释(由 entity-coder §二 处理)
  - 不自创 BusinessException(由 init-skeleton 处理)
- **二段循环协议第 4 次应用 · R-05 多文件跨命令拆分配对**:
  - 本命令 §二 修 service/+impl/+controller/+entity/dto/
  - entity-coder §二 修 entity/+mapper/
  - 边界对称(不空隙 / 不重叠)· 跟 srs-writer §二 / db-designer §二 / api-designer §二 协议一致
- **基于事实**:扫描 API_DESIGN.md + Entity + Mapper 真实结构,不依赖学生记忆,不编造接口或字段

---

> 📋 **跨文件呼应导航**:
> - **上游产出**:`entity-coder.md`(读其生成的 Entity + Mapper · R-05 修复跟 §二 配对)
> - **平行规则**:`CLAUDE.md §二·一`(分层 8 类 · controller/+service+impl/+common 行)+ `§三`(Result<T> + DTO + BusinessException)+ `§四`(MyBatis-Plus 用法 · 复杂查询例外)+ `§五`(后端安全)
> - **全栈契约**:`CLAUDE.md §一·一·后端`(版本 + spring-security-crypto 6.3.4)+ `§二·一`(Result<T> 单一权威源)+ `§二`(BCrypt + LambdaQueryWrapper + @Valid)
> - **输入文档**:`API_DESIGN.md §1`(接口约定 · 路径前缀 / 分页 / RESTful)+ `§2`(接口清单 · 模块分组)+ `§3`(接口详情 5 字段)+ `§4`(异常码 1xxx-9xxx)
> - **下游消费**:Postman 测试 · `code-reviewer-be.md`(R-05 · 待审 Phase 4 第 3)+ 二段循环协议跟 `entity-coder §二` 配对
> - **基础设施**:`init-skeleton.md backend/src/main/java/{{包路径}}/common/`(Result + BusinessException + GlobalExceptionHandler · 2026-05-10 第 2 次链路断点修复)+ `util/JwtUtils`(登录接口用)
> - **骨架占位**:`init-skeleton.md backend/src/main/java/{{包路径}}/service+impl+controller/`(本命令填充)
