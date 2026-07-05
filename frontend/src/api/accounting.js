import api from './index'

// ========== 成本中心 ==========

export function getCostCenters() {
  return api.get('/accounting/cost-centers')
}

export function createCostCenter(data) {
  return api.post('/accounting/cost-centers', data)
}

export function updateCostCenter(id, data) {
  return api.put(`/accounting/cost-centers/${id}`, data)
}

// ========== 利润中心 ==========

export function getProfitCenters() {
  return api.get('/accounting/profit-centers')
}

export function createProfitCenter(data) {
  return api.post('/accounting/profit-centers', data)
}

export function updateProfitCenter(id, data) {
  return api.put(`/accounting/profit-centers/${id}`, data)
}

// ========== 财务期间 ==========

export function getPeriods() {
  return api.get('/accounting/periods')
}

export function closePeriod(year, month) {
  return api.post('/accounting/periods/close', { year, month })
}

export function reopenPeriod(year, month) {
  return api.post('/accounting/periods/reopen', { year, month })
}

// ========== 期间余额快照 ==========

export function getPeriodBalances(periodId) {
  return api.get(`/accounting/period-balances/${periodId}`)
}

// ========== 财务看板 ==========

export function getAccountingDashboard(params) {
  return api.get('/accounting/dashboard', { params })
}
