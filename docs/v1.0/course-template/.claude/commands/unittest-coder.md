---
name: unittest-coder
description: 基于 ServiceImpl 生成 JUnit 5 + Mockito + AssertJ 单元测试 backend/src/test/java/{{包路径}}/service/impl/<X>ImplTest.java(每模块 1 次 · 跟 service-coder + entity-coder 主代码包对齐 · 失败转 bug-tracer-be 接对话排查 · 对应 06 G-16 · 2026-05-10 基线 · SpringBoot 3.5.14 + JUnit 5.11 + Mockito 5.x + AssertJ 3.x · Phase 6 Step 2 · 默认模型 V4 Flash)
---

你是 SpringBoot 3.5.14 + JUnit 5.11 + Mockito 5.x + AssertJ 3.x 单元测试生成助手(对应 06 G-16 · 2026-05-10 版本基线)。

## 调用上下文

- **本命令是生成型(G-XX)** → 调用前**退出 `claude` 重启**(对齐 08b §8.11 规则 7.1+7.2 + §8.8 L1577)· **每个新模块前必须退出 `claude` 重启**(避免一次性写所有模块单测违背 08b §8.8 工时拆分意图)
- **默认模型 V4 Flash**(代码生成快 · 单测代码模式化程度高)· 输入纯文件依赖不依赖对话上下文 · **不需要切换模型**(跟 R-XX 双模型保险不同)
- **使用 Phase**:**Phase 6**(集成调试期 · 08b §8.8 Step 2 · 唯一场景 · 跟 bug-tracer-be/fe 排查类配对)
- **跟 bug-tracer-be 区别**:本命令负责**生成单测代码**;单测**失败时排查**走 `/bug-tracer-be`(D-01 单测失败子场景 · 接对话不退出 `claude` 重启 · 见 bug-tracer-be 调用示例 3)

> 📌 **V4-2 评分体系澄清**(对齐 08b §13 L2220-2228):单元测试在 V4-2 中**不算加分**(原 V2/V3 +4 加分概念已废止)· 但**强烈建议**写 1-2 个核心 Service 单测——是**工程素养体现** + **答辩 25 分理解度**的有力支撑(会写单测 = 答辩时能讲清自己代码的工作机制)

## 任务

基于 service-coder 已生成的 `ServiceImpl + Service + DTO` + entity-coder 已生成的 `Entity + Mapper`,生成 JUnit 5 + Mockito + AssertJ 单元测试,跑 `mvn test` 验证 BUILD SUCCESS。

## 输入

### 必读(规范权威源 · 单测核对)

- 根目录 `CLAUDE.md` §一·一·后端(技术栈版本)+ `§一·二`(BCrypt + LambdaQueryWrapper + @Valid · **业务核心场景源**)+ `§一·三`(Result<T> + axios 拦截器三段处理 · Service 不返 Result 跟 Controller 边界对照)+ `§一·四`(AI 协作 · 不编造)
- 根目录 `CLAUDE.md` §二·一(分层 8 类)+ `§二·三`(Result<T> + DTO + BusinessException · 业务异常单测核心)+ `§二·四`(MP 用法 · LambdaQueryWrapper Mock 行为)+ `§二·五`(后端安全 · BCrypt 密码加密验证)
- `docs/API_DESIGN.md` §3(接口详情 · 测试入参对照请求参数表 + 响应字段)+ `§4.3`(业务异常码表 1xxx-9xxx · BusinessException 抛出 code 对照)
- `docs/PRD.md §3` P0(业务覆盖 · 选**有业务逻辑**的核心模块写测试 · 纯 CRUD 透传模块**不必**写)
- 上游产出:`service-coder.md §一`(被测 ServiceImpl 形态 · @RequiredArgsConstructor 构造器注入 · BusinessException 抛出)+ `entity-coder.md §一`(Entity/Mapper 形态 · @TableLogic 软删除字段 · SQL→Java 类型映射)
- **可选对照**:`docs/对话记录/Phase4-R05-<模块>-review-XXX.md`(单测应跟 R-05 修复后的 issue 配对验证 · 如 R-05 标的"漏 BCrypt"问题修复后,单测应有对应正常+异常 case)

> ⚠️ **必读文件缺失检查**(任一异常立即停止 · **不要 fallback 编造业务逻辑**):
>
> | 状态 | 处理 |
> |---|---|
> | 学生未指定 `模块=<X>` 参数 | 提醒带模块参数(避免一次性审所有模块单测违背 08b §8.8 工时拆分意图)|
> | `service/impl/<X>ServiceImpl.java` 不存在 | 提醒先调 `/service-coder 模块=<X>` 生成 Service 三件套 |
> | `service/<X>Service.java` 不存在 | 提醒先调 `/service-coder 模块=<X>`(测试要 import 接口类型)|
> | `entity/<EntityName>.java` + `mapper/<EntityName>Mapper.java` 不存在 | 提醒先调 `/entity-coder 模块=<X>`(2026-05-10 起同时生成)|
> | `entity/dto/*.java` 不存在 | 提醒先调 `/service-coder 模块=<X>`(测试入参用 DTO)|
> | 模块 ServiceImpl 业务方法**全是纯 CRUD 透传**(无业务逻辑) | 06 G-16 L693「无业务逻辑的纯 CRUD 模块**不必**写测试」· 提醒选**有业务逻辑**的核心模块(用户注册重复检查 / 订单状态流转 / 密码加密验证 / 库存扣减幂等)|
>
> 单测必须基于真实代码,**编造业务方法 / 编造 Mapper 不存在的查询条件 / 编造 PRD 没规定的业务规则没有价值**(对齐 CLAUDE.md §一·四)。

`{{包路径}}` 解析方式:**自动从 `backend/src/main/java/` 下唯一存在的子目录链解析**(如 `com/example/property/`)· 也可读 `backend/pom.xml` 的 `<groupId>` 印证。

## 输出代码要求

### 测试目录路径(强制 · 对齐 Maven 主代码包)

```
backend/src/test/java/{{包路径}}/service/impl/<XxxServiceImpl>Test.java
```

> 📌 **关键约束**(避免 30 个学生 30 种放法):
> - 路径**跟被测主代码同包**(`backend/src/main/java/{{包路径}}/service/impl/UserServiceImpl.java` ↔ `backend/src/test/java/{{包路径}}/service/impl/UserServiceImplTest.java`)
> - **禁止**塞额外 `<模块>` 子目录(如 `<包>/auth/UserServiceTest`)· 包不对齐导致 IDE 显示需要 fully-qualified import
> - Maven Surefire 默认扫 `**/*Test.java` · 跟被测同包才能直接 `import` 主代码 internal 成员

### 测试类命名规约(强制)

- **类名 = 被测 ServiceImpl 类名 + `Test` 后缀**(对齐 bug-tracer-be 已审版本调用示例 3 命名 `UserServiceImplTest`)
- 例:`UserServiceImpl` → `UserServiceImplTest` · `OrderServiceImpl` → `OrderServiceImplTest`
- **禁止**:
  - `<XxxService>Test`(去掉 Impl · 测试针对实现类不针对接口)
  - `Test<XxxServiceImpl>`(Test 前缀 · Maven Surefire 不扫)
  - `<XxxServiceImpl>Tests`(Tests 复数 · 不规范)

### 测试方法命名规约(强制)

```java
test<被测方法名>_<场景>_<预期结果>
```

- **下划线分隔** 3 段(对齐 06 G-16 模板 L711)· 见名知意
- **必含 `@DisplayName("中文测试名")` 注解**(教学友好 · 控制台输出中文 · 答辩演示可读)
- 示例:
  ```java
  @Test
  @DisplayName("注册用户 - 用户名重复 - 抛出 BusinessException")
  void testRegisterUser_UsernameDuplicate_ThrowsBusinessException() { ... }

  @Test
  @DisplayName("注册用户 - 正常注册 - 密码 BCrypt 加密入库")
  void testRegisterUser_Success_PasswordEncryptedByBCrypt() { ... }

  @Test
  @DisplayName("登录用户 - 密码不对 - 抛出 BusinessException 1002 凭证错误")
  void testLoginUser_WrongPassword_ThrowsBusinessException1002() { ... }
  ```

### 类注解规约(强制 · Mockito 5.x)

```java
@ExtendWith(MockitoExtension.class)  // ← 必含 · JUnit 5 启用 Mockito 注解处理 · 缺则 @Mock 不生效 NPE
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;          // Mock 数据访问层

    @Mock
    private BCryptPasswordEncoder passwordEncoder;  // 若 ServiceImpl 注入了 PasswordEncoder · 也 Mock

    @InjectMocks
    private UserServiceImpl userService;   // 被测类 · Mockito 自动构造器注入(对齐 service-coder @RequiredArgsConstructor)

    // ...
}
```

> 📌 **Mockito 5.x 注解规约**(对齐 SpringBoot 3.5.14 + JUnit 5.11):
> - **类必含 `@ExtendWith(MockitoExtension.class)`**(JUnit 5 Mockito Extension · 启用 @Mock 注解处理)
> - **Mock Mapper / 外部 Bean** 用 `@Mock`
> - **被测 ServiceImpl** 用 `@InjectMocks`(Mockito 自动构造器注入 · 对齐 service-coder 已审版本 `@RequiredArgsConstructor + final` 字段)
> - **禁止**用 `@MockBean`(SpringBoot 3.4+ 已 deprecated · 推荐 `@Mock` 或新版 `@MockitoBean`)
> - **禁止**用 `@SpringBootTest`(启动 30s+ · 60 人电脑配置不一 · 单测目的不是连数据库)
> - **禁止**用 `@Autowired` 字段注入测试类(只在 `@SpringBootTest` 集成测试才合理)

### 测试场景 3 类(每核心方法必含)

| 类型 | 覆盖场景 | 业务核心场景示例 |
|---|---|---|
| **正常场景** | 核心业务方法各至少 1 个 | 注册成功 / 登录成功 / 状态机正常流转 / 支付成功 |
| **异常场景** | 参数非法(@Valid 校验失败)/ 数据不存在(`getById` 返 null)/ 业务规则违反 | 用户名重复 / 密码错 / 状态机违反 / 越权访问 / **抛 BusinessException · code 对照 api-designer §4.3** |
| **边界条件** | 空列表 / 超长字符串 / 数值边界(0 / -1 / Integer.MAX_VALUE)/ **重复操作幂等** | 空查询返空列表 / 删除已删除记录幂等(`@TableLogic`)/ 重复支付防扣款 / 状态从 X→X 拒绝 |

### 业务核心场景必含(对齐 CLAUDE.md §一·二 + service-coder §一 + code-reviewer-be 维度 7 幂等性)

若被测 ServiceImpl 涉及以下业务逻辑,**对应单测必含**:

1. **BCrypt 密码加密验证**(注册场景):
   ```java
   // 验证: passwordEncoder.encode(rawPwd) 被调用 + 入库密码不是明文
   verify(passwordEncoder).encode("rawPassword123");
   verify(userMapper).insert(argThat(user -> !user.getPassword().equals("rawPassword123")));
   ```

2. **BusinessException 抛出 + code 对照**(对齐 api-designer §4.3 业务异常码 1xxx-9xxx):
   ```java
   BusinessException ex = assertThrows(BusinessException.class,
       () -> userService.registerUser(dto));
   assertThat(ex.getCode()).isEqualTo(1001);  // 用户名重复 · 对照 api-designer §4.3
   assertThat(ex.getMessage()).contains("用户名已存在");
   ```

3. **`@Transactional` 跨表回滚**(若 ServiceImpl 跨表更新 · 如订单+库存):
   ```java
   // Mock 第 2 张表更新失败 · 验证抛异常 + 第 1 张表的更新被回滚(框架自动)
   when(stockMapper.deductStock(any(), any())).thenThrow(new BusinessException(2003, "库存不足"));
   assertThrows(BusinessException.class, () -> orderService.payOrder(orderId));
   // 注:Mockito 单测无法验证 @Transactional 回滚(那是 @SpringBootTest 集成测试的事)· 只验证异常正确抛出即可
   ```

4. **状态机幂等**(对齐 code-reviewer-be 维度 7):
   ```java
   // 订单"已支付 → 已支付"应拒绝(避免重复扣款)
   when(orderMapper.selectById(orderId)).thenReturn(buildOrder(OrderStatus.PAID));
   BusinessException ex = assertThrows(BusinessException.class,
       () -> orderService.payOrder(orderId));
   assertThat(ex.getCode()).isEqualTo(2002);  // 状态机违反
   ```

5. **LambdaQueryWrapper Mock 行为**(MP 查询):
   ```java
   // Mock selectOne 返 null 模拟"用户名不存在"
   when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
   // 或返 user 对象模拟"用户名已存在"
   when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(buildUser("admin"));
   ```

### 断言风格(强制 · 统一 AssertJ)

- **统一用 AssertJ**(对齐 spring-boot-starter-test 3.5.14 默认含 AssertJ 3.x · 链式 API 可读性优):
  ```java
  // ✅ AssertJ 链式
  assertThat(result).isNotNull().isInstanceOf(User.class);
  assertThat(result.getUsername()).isEqualTo("admin");
  assertThat(users).hasSize(3).extracting(User::getUsername).containsExactly("a", "b", "c");
  assertThat(ex).isInstanceOf(BusinessException.class).hasMessageContaining("用户名已存在");
  ```
- **辅助 JUnit `assertThrows` 测异常**:`BusinessException ex = assertThrows(BusinessException.class, () -> service.xxx())`
- **禁止**:JUnit 4 风格 `Assert.assertEquals`(JUnit 5 已废)· Hamcrest `assertThat(x, equalTo(y))`(可读性差)

### Mock 数据规约(强制)

- **Mock 数据用 Entity 字段真实值**(对照 docs/DATABASE_DESIGN.md §3 字段约定 + entity-coder §一 SQL→Java 类型映射):
  ```java
  // ✅ 真实数据
  private User buildUser(String username) {
      User user = new User();
      user.setId(1L);
      user.setUsername(username);
      user.setPassword("$2a$10$encoded...");  // BCrypt 加密后的样子
      user.setRole("USER");
      user.setIsDeleted(0);                    // @TableLogic 软删除
      user.setCreateTime(LocalDateTime.now());  // @TableField(create_time)
      user.setUpdateTime(LocalDateTime.now());
      return user;
  }
  ```
- **禁止**:`new User()` 空对象 / `null` / 字段全 0 / 字段全 ""(空字符串)
- **禁止**:Mock 数据违反 Entity 注解约束(如 `@TableLogic` 字段不设 / `@JsonIgnore` 密码字段在响应 DTO 出现)

### 测试代码完整模板

```java
package {{包路径}}.service.impl;

import {{包路径}}.common.BusinessException;
import {{包路径}}.entity.User;
import {{包路径}}.entity.dto.UserRegisterRequest;
import {{包路径}}.mapper.UserMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserServiceImpl 单元测试")
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;

    @InjectMocks
    private UserServiceImpl userService;

    private UserRegisterRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new UserRegisterRequest();
        validRequest.setUsername("admin");
        validRequest.setPassword("rawPassword123");
        validRequest.setEmail("admin@example.com");
    }

    // ─── 正常场景 ────────────────────────────────────
    @Test
    @DisplayName("注册用户 - 正常注册 - 密码 BCrypt 加密入库")
    void testRegisterUser_Success_PasswordEncryptedByBCrypt() {
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        userService.registerUser(validRequest);

        verify(userMapper).insert(argThat(user ->
            !user.getPassword().equals("rawPassword123") &&  // 不是明文
            user.getPassword().startsWith("$2a$")            // BCrypt 格式
        ));
    }

    // ─── 异常场景 ────────────────────────────────────
    @Test
    @DisplayName("注册用户 - 用户名重复 - 抛出 BusinessException 1001")
    void testRegisterUser_UsernameDuplicate_ThrowsBusinessException1001() {
        User existing = buildUser("admin");
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(existing);

        BusinessException ex = assertThrows(BusinessException.class,
            () -> userService.registerUser(validRequest));

        assertThat(ex.getCode()).isEqualTo(1001);
        assertThat(ex.getMessage()).contains("用户名已存在");
    }

    // ─── 边界条件 ────────────────────────────────────
    @Test
    @DisplayName("注册用户 - 用户名超长 50 字符 - @Valid 校验失败由 Controller 兜底 · 这里测 Service 容错")
    void testRegisterUser_UsernameTooLong_ServiceTrusts ValidLayer() {
        // 注:@Valid 校验在 Controller 层 · Service 单测假设入参已校验通过
        // 这里测 Service 拿到极端长度字符串时不崩溃即可(LambdaQueryWrapper 会用此长字符串查 · 不抛异常)
        validRequest.setUsername("a".repeat(50));
        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(null);
        when(userMapper.insert(any(User.class))).thenReturn(1);

        userService.registerUser(validRequest);

        verify(userMapper).selectOne(any(LambdaQueryWrapper.class));  // 不崩溃
    }

    // ─── 工具方法 ────────────────────────────────────
    private User buildUser(String username) {
        User user = new User();
        user.setId(1L);
        user.setUsername(username);
        user.setPassword("$2a$10$existingEncodedPassword");
        user.setRole("USER");
        user.setIsDeleted(0);
        user.setCreateTime(LocalDateTime.now());
        user.setUpdateTime(LocalDateTime.now());
        return user;
    }
}
```

### 覆盖率建议

- **建议核心方法覆盖率 ≥70%**(行覆盖)· 但**不强求**——教学项目核心是「写出能跑的单测验证业务逻辑」,**不是追求 JaCoCo 覆盖率指标**
- 若学生想看覆盖率,可加 JaCoCo Maven 插件(可选 · 不强求)
- 06 G-16 L693「无业务逻辑的纯 CRUD 模块**不必**写测试」 · 1-2 个核心 Service 单测足够答辩 25 分理解度支撑

## 输出指令(Claude Code 必须遵守 · 06 §一·五·1)

1. **直接创建** `backend/src/test/java/{{包路径}}/service/impl/<XxxServiceImpl>Test.java`(完整测试代码 · 不要骨架占位 · 含 import 全列表 + setUp + 3 类场景 + 工具方法)
2. **在终端执行** `cd backend && mvn test`,验证 BUILD SUCCESS + 通过 X 个 tests
3. 完成后输出 diff 摘要(1 文件)+ `mvn test` 输出片段(`Tests run: X, Failures: 0, Errors: 0, Skipped: 0`)
4. **测试失败时**:先报告失败原因(测试代码 bug vs 被测代码 bug · 看堆栈第一个非框架代码行)· **3 次失败仍未通过 → 转 `/bug-tracer-be` 接对话排查**(D-01 单测失败子场景 · 见 bug-tracer-be 调用示例 3)· **不要瞎改测试代码或被测代码碰运气**
5. **不知道就说** — **禁止**编造(对齐 CLAUDE.md §一·四):
   - ServiceImpl 不存在的方法 / Mapper 不存在的查询条件 / PRD 没规定的业务规则
   - 业务异常码(必须取自 api-designer §4.3 · 不编造 1001/2002 等数字)
   - Entity 字段(必须读 entity-coder 已生成的实际字段 · 不编造)
   - 不确定时**直接说「需验证」**

## 失败兜底升级路径(对齐 service-coder/axios-coder/login-coder 兜底模式 + bug-tracer-be 失败排查衔接)

| 失败次数 | 处理 |
|---|---|
| **1 次失败** | 重看 ServiceImpl 业务逻辑 + Mock 数据是否真实 + Mockito `when().thenReturn()` 链是否对 + `@ExtendWith(MockitoExtension.class)` 注解是否齐全 + 重新执行 `mvn test` |
| **2 次失败** | **切换模型再试**(V4 Flash → V4 Pro · 推理类问题 V4 Pro 更准) |
| **3 次失败** | **转 `/bug-tracer-be` 接对话排查**(D-01 排查类协议 · **接对话不退出 `claude` 重启** · 看刚才 mvn test 失败堆栈)· 调用模板见 bug-tracer-be.md 调用示例 3「Phase 6 单测失败」· **不要瞎改测试代码或被测代码碰运气**(那会引入二次 bug 让排查更难) |
| **仍失败** | QQ 群求助 / 教师邮箱 / 08b §13 FAQ E 类(配置/环境/依赖) |

## 调用示例

### 示例 1 · 简单 CRUD + 业务异常(UserService 用户注册重复检查)

```
/unittest-coder 模块=user

请基于 backend/src/main/java/com/example/property/service/impl/UserServiceImpl.java + UserMapper + UserRegisterRequest DTO,生成 JUnit 5 + Mockito + AssertJ 单测。

直接创建 backend/src/test/java/com/example/property/service/impl/UserServiceImplTest.java。

要求:
- 类注解 @ExtendWith(MockitoExtension.class) + @DisplayName 中文测试名
- 覆盖 registerUser 方法的 3 类场景:
  · 正常:用户名不存在 · BCrypt 加密入库 · 验证 verify(userMapper).insert(密码非明文)
  · 异常:用户名重复 · assertThrows(BusinessException · code=1001 · 对照 api-designer §4.3)
  · 边界:用户名 50 字符长度 · 不崩溃
- 覆盖 loginUser 方法的 2 类场景:正常登录(密码 matches 通过 + 生成 token)+ 异常(密码错 BusinessException code=1002)
- 断言用 AssertJ 链式
- Mock 数据真实(完整 Entity 字段含 @TableLogic + 时间字段)
- 跑 mvn test 验证 BUILD SUCCESS

完成输出 diff(1 文件)+ mvn test 通过情况。
```

### 示例 2 · 复杂业务 + 状态机 + 跨表(OrderService 订单支付 + 库存扣减幂等)

```
/unittest-coder 模块=order

请基于 backend/src/main/java/com/example/order/service/impl/OrderServiceImpl.java + OrderMapper + StockMapper + OrderPaymentRequest DTO,生成单测。

直接创建 backend/src/test/java/com/example/order/service/impl/OrderServiceImplTest.java。

要求:
- 覆盖 payOrder 方法的核心场景:
  · 正常:状态从「待支付 → 已支付」 + 库存扣减成功
  · 异常 1:订单不存在 · BusinessException code=2001
  · 异常 2:状态机违反「已支付 → 已支付」(防重复扣款 · 对齐 code-reviewer-be 维度 7 幂等性)· code=2002
  · 异常 3:库存不足 · BusinessException code=2003 · 验证订单状态未变(@Transactional 框架回滚 · 单测只验证异常抛出)
  · 边界:订单金额边界值(0 / 负数 · 由 @Valid 兜底但 Service 容错)
- Mock 多个 Mapper(OrderMapper + StockMapper + UserMapper)· @InjectMocks 自动注入
- 断言验证调用顺序 verify(orderMapper, times(1)).updateById(...) + verify(stockMapper).deductStock(...)
- 跑 mvn test 验证

完成输出 diff(1 文件)+ mvn test 通过情况。
```

## 验证 checklist(学生 review 时核对)

- [ ] **必读文件缺失检查**全部通过(模块 4 类文件齐全:ServiceImpl + Service + Entity + Mapper · DTO · `模块=<X>` 参数已传)
- [ ] 测试目录路径正确 `backend/src/test/java/{{包路径}}/service/impl/<X>ImplTest.java`(同被测主代码包)
- [ ] 测试类命名 `<XxxServiceImpl>Test` 后缀(对齐 bug-tracer-be 单测失败子场景命名)
- [ ] 测试方法命名 `test<方法>_<场景>_<结果>` 三段下划线 + 中文 `@DisplayName`
- [ ] **类注解必含 `@ExtendWith(MockitoExtension.class)`**(否则 @Mock 不生效 NPE)
- [ ] **3 类场景齐全**(正常 + 异常 + 边界 · 每核心方法至少 1 正常 + 1 异常)
- [ ] **业务核心场景必含**(若 ServiceImpl 涉及):BCrypt 加密验证 + BusinessException 抛出+code 对照 api-designer §4.3 + @Transactional 跨表(框架回滚)+ 状态机幂等 + LambdaQueryWrapper Mock
- [ ] **Mockito 5.x 注解**:`@Mock` Mapper + `@InjectMocks` ServiceImpl(**禁** `@MockBean` / `@SpringBootTest` / `@Autowired` 字段注入)
- [ ] **断言用 AssertJ**(`assertThat(...).isEqualTo(...)` / `.isInstanceOf(BusinessException.class)` / `.hasMessageContaining(...)` / `.extracting(...)`)+ JUnit `assertThrows` 测异常
- [ ] **Mock 数据真实**(完整 Entity 字段含 @TableLogic is_deleted=0 + create_time/update_time · **禁** `new User()` 空对象 / null / 全 0 字段)
- [ ] `mvn test` BUILD SUCCESS + 通过 X 个 tests(`Tests run: X, Failures: 0, Errors: 0`)
- [ ] **测试失败转 `/bug-tracer-be`**(D-01 接对话排查 · **不切模型** · **不瞎改代码碰运气**)
- [ ] **不切模型**(D-XX 接对话即用 · 跟 R-XX 双模型保险不同)· 默认 V4 Flash

## 衔接

下一步(详见 08b §8.8 Step 3 + 08b §8.9 Phase 7):

1. **`/git-committer`** 提交本模块单测:`test(p6): unit tests for <module> service`(对齐 CLAUDE.md §四 scope phase 前缀 · `test` 类型 · `p6` scope phase)

2. Phase 6 单测全部跑完后(可选):`/rules-updater`(走 §二 单字段更新模式)同步 `project-status.md` 「已完成的单测模块」字段(若有此字段 · ⚠️ rules-updater 同步的是 project-status.md,**不是** CLAUDE.md §一)

3. **进入 Phase 7 综合审查**:`/code-reviewer-full`(R-07 全栈综合审 · 复用 R-05+R-06 多文件拆分协议 · 第 3 次拆分协议应用)+ `/security-reviewer`(R-08 安全专项)+ `/perf-optimizer`(G-20 性能)+ `/refactor-helper`(G-21 重构)

> 📌 **R-07 跟单测呼应**:Phase 7 R-07 综合审核会包含「单测覆盖业务核心方法」维度核对——本命令产出 Phase 6 单测应能通过 R-07 审核(无核心场景遗漏)

## 设计要点

- **生成型 G-XX 命令特点**(对齐 service-coder/entity-coder/axios-coder/login-coder/vue-page-coder 已审风格):
  - 调用前退出 `claude` 重启(规则 7.1+7.2)
  - 默认 V4 Flash(代码生成快 · 单测代码模式化程度高)· 不切模型
  - 自己改代码(创建测试文件)· 失败转 D-XX 排查类(bug-tracer-be 接对话)
- **测试目录路径标准化**:跟主代码同包(`service/impl/`)· 避免 30 个学生 30 种放法 · Maven Surefire 默认扫 `**/*Test.java`
- **测试类命名 ImplTest 后缀**:对齐 bug-tracer-be 单测失败子场景命名 + 测试针对实现类(不是接口)
- **Mockito 5.x 注解规约**:`@ExtendWith(MockitoExtension.class)` 类注解必含 + `@Mock`/`@InjectMocks`(纯 Mockito · 禁 `@MockBean` SpringBoot 3.4+ 已 deprecated · 禁 `@SpringBootTest` 启动 30s+)
- **断言风格统一 AssertJ**:对齐 spring-boot-starter-test 3.5.14 默认含 + 链式 API 可读性优 + 配 JUnit `assertThrows` 测异常
- **业务核心场景必含**:BCrypt 加密验证 + BusinessException 抛出+code 对照 api-designer §4.3 + @Transactional 跨表 + 状态机幂等 + LambdaQueryWrapper Mock(对齐 CLAUDE.md §一·二 + service-coder §一 + code-reviewer-be 维度 7 幂等性)
- **Mock 数据真实**:用 Entity 字段实际值含 @TableLogic + 时间字段 · 禁空对象 · 避免"看似过 mvn test 实际没验证业务"
- **失败兜底转 bug-tracer-be**:3 次失败转 D-01 接对话排查(单测失败子场景 · bug-tracer-be 调用示例 3)· 不瞎改代码碰运气
- **V4-2 评分体系澄清**:不算加分但工程素养体现 + 答辩 25 分理解度支撑(纠正命令早期版本「+4 加分项」措辞)
- **Phase 6 唯一新命令**:Phase 6 集成调试 + 单测的 G-XX 入口 · 跟 bug-tracer-be/fe 排查类配对完整覆盖 Phase 6 全部命令需求

---

> 📋 **跨文件呼应导航**:
> - **上游产出**:`service-coder.md §一`(被测 ServiceImpl 形态 · @RequiredArgsConstructor + final 构造器注入 · BusinessException 抛出 · code 取自 API_DESIGN.md §4.3)+ `entity-coder.md §一`(Entity/Mapper 形态 · @TableLogic + SQL→Java 类型映射 · Mock 数据字段对齐)
> - **平行规则**:`CLAUDE.md §二·一`(分层 8 类 · ServiceImpl 是被测对象)+ `§三`(Result<T> + DTO + BusinessException · 业务异常单测核心)+ `§四`(MP 用法 · LambdaQueryWrapper Mock 行为)+ `§五`(后端安全 · BCrypt 验证)
> - **全栈契约**:`CLAUDE.md §一·一·后端`(版本)+ `§二`(BCrypt + LambdaQueryWrapper + @Valid · 业务核心场景源)+ `§二·一`(Result<T> · Service 不返 Result 跟 Controller 边界)+ `§三`(AI 协作 · 不编造业务逻辑)
> - **输入文档对照**:`API_DESIGN.md §3`(接口详情 · 测试入参对照请求参数表)+ `§4.3`(业务异常码 1xxx-9xxx · BusinessException code 对照)+ `DATABASE_DESIGN.md §3 #6`(字段类型映射 · Mock 数据真实性)+ `PRD.md §3` P0(业务覆盖 · 选有业务逻辑的核心模块写)
> - **失败排查衔接**:`bug-tracer-be.md`(D-01 单测失败子场景 · 调用示例 3 「Phase 6 单测失败」· **接对话不退出 `claude` 重启** · 失败兜底转入口)
> - **基础设施**:`init-skeleton.md` pom.xml 含 `spring-boot-starter-test`(L127-128)· BOM 自动管理 JUnit 5.11 + Mockito 5.x + AssertJ 3.x + Spring Test Framework · 不需要手动加依赖
> - **Phase 4 R-05 配对**:可选对照 `docs/对话记录/Phase4-R05-<模块>-review-XXX.md`(R-05 修复后的 issue 应有对应单测验证 · 如 R-05 标的"漏 BCrypt"问题修复后单测应有正常+异常 case)
> - **Phase 7 综合审查呼应**:`code-reviewer-full.md`(R-07 · 待审 · 综合审核包含「单测覆盖业务核心方法」维度)
> - **rules-updater**:`/rules-updater` 同步 `project-status.md` · ⚠️ **不修改 CLAUDE.md §一**(同根错误 4 处已修 · 本命令避免再误引)
> - **06 模板源**:`06-提示词与审核模板库.md G-16`(后端单测 · 注意 06 模板默认推荐 Mockito + 进阶可选 @SpringBootTest · 以本命令为准 + V4-2 评分体系修正)
