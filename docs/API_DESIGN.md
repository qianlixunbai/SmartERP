# 接口设计文档

## SmartOA 审批流管理系统 API 规范

---

## 1. 一般規約

## 1. 通用约定

### 1.1 基础路径

所有 API 前缀：`/api`

### 1.2 認証方式

### 1.2 认证方式

JWT Bearer Token，请求头：`Authorization: Bearer <token>`

白名单接口（无需认证）：`/api/login`

### 1.3 响应格式

统一使用 `Result<T>` 结构：

```json
// 成功（含数据）
{ "code": 200, "message": "操作成功", "data": { ... } }

// 成功（无数据）
{ "code": 200, "message": "操作成功", "data": null }

// 失败
{ "code": 500, "message": "错误原因", "data": null }
```

### 1.4 状态码

| HTTP 状态码 | code | 含义 |
|---|---|---|
| 200 | 200 | 请求成功 |
| 401 | 401 | 未登录或 Token 过期 |
| 403 | 403 | 无权限 |
| 400 | 400 | 请求参数有误 |
| 500 | 500 | 服务器内部错误 / 业务异常 |

---

## 2. 认证模块

### 2.1 登录

```
POST /api/login
```

**请求体：**

```json
{
  "username": "zhangsan",
  "password": "123456"
}
```

**响应：**

```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOi...",
    "user": {
      "id": 2,
      "username": "zhangsan",
      "realName": "张三",
      "role": "EMPLOYEE",
      "department": "技术部",
      "directLeaderId": 1,
      "departmentHeadId": 4
    }
  }
}
```

### 2.2 获取当前用户

```
GET /api/user/current
```

### 2.3 获取用户列表

```
GET /api/users
```

**响应：** `Result<List<User>>`，password 字段被 `@JsonIgnore` 排除

### 2.4 登出

```
POST /api/logout
```

---

## 3. 审批模板模块

### 3.1 模板列表

```
GET /api/templates
```

**响应：** `Result<List<ApprovalTemplate>>`

### 3.2 模板详情

```
GET /api/templates/{id}
```

### 3.3 创建模板

```
POST /api/templates
```

**権限：** MANAGER

**权限：** MANAGER

**请求体：**

```json
{
  "name": "请假申请",
  "description": "员工请假审批模板",
  "enabled": true
}
```

### 3.4 更新模板

```
PUT /api/templates/{id}
```

**権限：** MANAGER

**权限：** MANAGER

### 3.5 删除模板

```
DELETE /api/templates/{id}
```

**権限：** MANAGER

**权限：** MANAGER
**说明：** 级联删除关联的审批节点和表单字段

### 3.6 模板节点列表

```
GET /api/templates/{id}/nodes
```

**响应：** `Result<List<ApprovalNode>>`，按 `sortOrder` 升序排列

### 3.7 保存模板节点

```
POST /api/templates/{id}/nodes
```

**権限：** MANAGER

**权限：** MANAGER
**请求体：** `List<ApprovalNode>`
**说明：** 先删除旧节点（清理引用），再批量插入新节点，自动分配 `sortOrder`

**节点字段（P2 完整版）：**

```json
{
  "nodeName": "部门总监审批",
  "approverType": "DEPARTMENT_HEAD",
  "signType": "SINGLE",
  "approverIds": "2,3",
  "conditionExpression": "days > 3",
  "timeoutHours": 48,
  "timeoutAction": "ESCALATE",
  "escalateToUserId": 4
}
```

### 3.8 删除节点

```
DELETE /api/templates/{id}/nodes/{nodeId}
```

**権限：** MANAGER

**权限：** MANAGER

### 3.9 模板字段列表

```
GET /api/templates/{id}/fields
```

**响应：** `Result<List<TemplateField>>`

---

## 4. 请假申请模块

### 4.1 申請提出

### 4.1 提交申请

```
POST /api/leave/submit
```

**请求体：**

```json
{
  "templateId": 1,
  "leaveType": "年假",
  "startDate": "2026-06-01",
  "endDate": "2026-06-03",
  "reason": "回家探亲"
}
```

**说明：** 系统自动读取模板节点，评估条件分支，解析审批人（SINGLE/并行），设置 timeoutTime，启动审批流程

### 4.2 承認操作

### 4.2 审批操作

```
POST /api/leave/approve
```

**请求体：**

```json
{
  "requestId": 1,
  "action": "APPROVE",
  "comment": "同意请假"
}
```

**action 取值：** `APPROVE` | `REJECT`

**SINGLE 模式：** 校验 currentApproverId → 推进/终止
**并行模式：** 查 approval_task → COUNTER_SIGN 全部同意后推进 / OR_SIGN 任一人同意即推进
**REJECT：** 终止流程 + 跳过其他并行任务

### 4.3 申請撤回

### 4.3 撤回申请

```
POST /api/leave/{id}/withdraw
```

**权限：** 仅申请人
**前置条件：** 状态为 PENDING
**说明：** 状态 → WITHDRAWN，跳过并行任务，清除 currentApproverId/timeoutTime

### 4.4 转派审批

```
POST /api/leave/{id}/transfer
```

**权限：** 当前审批人（仅 SINGLE 模式）
**约束：** 并行审批节点不支持转派

**请求体：**

```json
{
  "toUserId": 3
}
```

### 4.5 我的申请

```
GET /api/leave/my-requests
```

### 4.6 待审批列表

```
GET /api/leave/pending
```

**说明：** 同时匹配 `currentApproverId = 当前用户` 和 `approval_task` 中 PENDING 任务

### 4.7 已处理列表

```
GET /api/leave/done
```

### 4.8 申請詳細

### 4.8 申请详情

```
GET /api/leave/{id}
```

**说明：** 含 timeoutTime、节点配置等完整信息

### 4.9 承認記録

### 4.9 审批记录

```
GET /api/leave/{id}/records
```

**响应：** `Result<List<ApprovalRecord>>`，含 TIMEOUT_* 系统自动操作记录

### 4.10 并行审批任务查询（P2 新增）

```
GET /api/leave/{id}/tasks
```

**响应：** `Result<List<ApprovalTask>>`，当前节点 PENDING 状态的审批任务

### 4.11 滞留修復（P2 新規）

### 4.11 滞留修复（P2 新增）

```
POST /api/leave/repair
```

**说明：** 修复 `currentApproverId = null` 且无关联 PENDING 任务的滞留申请

---

## 5. 统计模块

### 5.1 统计摘要

```
GET /api/stats/summary
```

**响应：**

```json
{
  "code": 200,
  "data": {
    "avgDurations": [
      { "templateId": 1, "templateName": "请假申请", "avgMinutes": 124.5, "count": 15 }
    ],
    "templateUsages": [
      { "templateId": 1, "templateName": "请假申请", "count": 25 }
    ]
  }
}
```

---

## 6. 导出模块

### 6.1 导出请假单 Excel

```
GET /api/stats/export
```

**権限：** MANAGER

**权限：** MANAGER
**响应：** `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet` 二进制流
**文件名：** `请假单导出_2026-05-27.xlsx`

---

## 7. 数据模型

### 7.1 ApprovalNode（审批节点 — P2 完整版）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| templateId | BIGINT | 关联模板 ID |
| nodeName | VARCHAR(100) | 节点名称 |
| sortOrder | INT | 排序号，0 起 |
| approverType | VARCHAR(30) | 审批人类型 |
| approverId | BIGINT | 指定审批人 ID |
| signType | VARCHAR(20) | 签批模式：SINGLE/COUNTER_SIGN/OR_SIGN |
| approverIds | VARCHAR(1000) | 并行审批人 ID 列表（逗号分隔） |
| conditionExpression | VARCHAR(500) | SpEL 条件表达式 |
| timeoutHours | INT | 超时小时数 |
| timeoutAction | VARCHAR(30) | 超时动作：ESCALATE/AUTO_APPROVE/AUTO_REJECT |
| escalateToUserId | BIGINT | 超时转派目标用户 ID |

### 7.2 ApprovalTask（并行审批任务）

| 字段 | 类型 | 说明 |
|---|---|---|
| id | BIGINT | 主键 |
| leaveRequestId | BIGINT | 关联申请 ID |
| nodeId | BIGINT | 关联节点 ID |
| approverId | BIGINT | 审批人 ID |
| status | VARCHAR(20) | PENDING / COMPLETED / SKIPPED |

---

## 8. 接口汇总

| 模块 | 数量 | 关键路径 |
|---|---|---|
| 认证 | 4 | /login, /user/current, /users, /logout |
| 模板 | 9 | /templates CRUD, /templates/{id}/nodes, /templates/{id}/fields |
| 请假 | 11 | /leave/submit, /leave/approve, /leave/{id}/withdraw, /leave/{id}/transfer, /leave/my-requests, /leave/pending, /leave/done, /leave/{id}, /leave/{id}/records, /leave/{id}/tasks, /leave/repair |
| 统计 | 1 | /stats/summary |
| 导出 | 1 | /stats/export |
| **合计** | **26** | |

---

文档版本：v2.0（P2 完成） | 更新日期：2026-05-27
