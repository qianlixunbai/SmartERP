# SmartERP 版本路线图

> **项目愿景：** SmartERP 正在持续演进为一体化企业管理平台，融合办公自动化、财务核算、资金管理和投资组合分析。

---

## 模块架构

```
SmartERP
├── HR
│   ├── 员工
│   ├── 请假
│   └── 考勤
│
├── OA
│   ├── 审批
│   ├── 公告
│   └── 工作流
│
├── Finance
│   ├── 报销
│   ├── 总账
│   ├── 复式记账
│   └── 财务报表
│
├── Treasury
│   ├── 银行账户
│   ├── 企业资金
│   └── 现金流
│
├── Portfolio
│   ├── ETF
│   ├── 股票
│   ├── 收益率
│   ├── 夏普比率
│   └── 最大回撤
│
└── AI Assistant
```

---

## v1.0 OA 模块 ✅ 已完成

**目的：** 验证「模板配置 + 流程引擎」架构，建立 ERP 系统的审批与权限基础。

- JWT 认证 + BCrypt 密码加密 + 角色区分
- 多级审批引擎：条件分支（SpEL）/ 并行审批（会签/或签）/ 超时自动升级
- 请假管理 + 经费报销
- 复式记账引擎（借/贷自动生成、会计恒等式保证）
- BigDecimal 精度控制（DECIMAL(19,2)，不使用 double/float）
- 乐观锁（@Version）防并发审批冲突
- 红字冲销（借贷互换的反向分录）
- 审计日志（只追加，不可修改/删除）
- 状态机约束：DRAFT → PENDING → APPROVED → POSTED
- 12 张表 / 41 个单元测试 / ~8,500 行代码

---

## v2.0 Finance — Accounting（财务核算）✅ 已完成

**目的：** 解决企业财务核算问题，实现复式记账与凭证管理。

- Chart of Accounts（会计科目体系）
- Journal Entry（凭证管理）
- Double-Entry Bookkeeping（复式记账）
- Reversal（红字冲销）
- Cost Center（成本中心）
- Profit Center（利润中心）
- Fiscal Period（财务期间 / 月结）
- Audit Trail（审计日志）
- Financial Dashboard

---

## v3.0 Finance — Treasury（资金管理）

**目的：** 统一管理企业现金流、银行账户和资金调拨，解决资金分散、不透明的问题。

- Corporate Account（企业账户）
- Bank Account Management（银行账户管理）
- Cash Position（现金余额）
- Accounts Receivable / Payable（应收/应付）
- Cash Flow Management（企业现金流）
- Intercompany Transfer（内部资金调拨）
- Bank Reconciliation（银行对账）
- Cash Flow Forecasting（资金预测）
- Treasury Investment（资金投资 — 货币基金等短期理财）
- Treasury Dashboard

---

## v4.0 Finance — Portfolio（投资组合管理）✅ 已完成

**目的：** 帮助企业管理闲置资金投资，提供收益率、风险分析及资产配置能力。

- Investment Portfolio（ETF / 股票 / 债券[预留] / 现金）
- Holdings Management（持仓管理）
- Trade & Settlement（交易记录与结算）
- Dividend Management（股息管理）
- Asset Allocation（资产配置）
- Portfolio Performance（组合绩效：收益率 / 年化收益率）
- Time-Weighted Return / Money-Weighted Return
- Max Drawdown（最大回撤）
- Sharpe Ratio（夏普比率）
- Volatility Analysis（波动率分析）
- Risk Analysis（风险分析）
- Portfolio Dashboard

**演示数据：** QQQM / VOO / VTI

---

## v5.0 Analytics（BI 分析）

**目的：** 将 OA、财务、资金、投资数据统一汇总，提供可视化经营洞察。

- Business Intelligence Dashboard
- Financial Statements（财务报表）
- Profitability Analysis（利润分析）
- Budget vs Actual（预算执行率）
- Investment Return Trends（投资收益趋势）
- Cash Flow Forecast（现金流预测）
- KPI Dashboard / ECharts / Data Export

---

## v6.0 AI（AI Assistant）

**目的：** 引入 AI 辅助决策，降低人工审批和分析负担。

- AI Approval Recommendation（AI 审批建议）
- AI Financial Analysis（AI 财务分析）
- AI Cash Flow Prediction（AI 资金预测）
- AI Investment Analysis（AI 投资分析）
- AI Risk Alert（AI 风险提示）
- Natural Language Query（自然语言查询）

---

## Dashboard 核心指标

- 企业现金余额 / 月度现金流 / 资金预测
- Investment Portfolio（QQQM、VOO、VTI 演示数据）
- Portfolio Performance（收益率）/ Max Drawdown（最大回撤）
- Sharpe Ratio（夏普比率）/ Risk Analysis（风险分析）

---

## 命名规范

- ❌ 股票交易 → ✅ Investment Portfolio
- ❌ 买股票 → ✅ Asset Allocation
- ❌ 盈亏 → ✅ Portfolio Performance
- ❌ 我的股票 → ✅ Treasury Investment

---

> 更新日期：2026-07-05
