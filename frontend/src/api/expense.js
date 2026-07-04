import api from './index'

export function submitExpense(data) {
  return api.post('/expense/submit', data)
}

export function approveExpense(requestId, action, comment) {
  return api.post('/expense/approve', { requestId, action, comment })
}

export function withdrawExpense(id) {
  return api.post(`/expense/${id}/withdraw`)
}

export function reverseExpense(id, reason) {
  return api.post(`/expense/${id}/reverse`, { reason })
}

export function getMyExpenses() {
  return api.get('/expense/my-expenses')
}

export function getPendingExpenses() {
  return api.get('/expense/pending')
}

export function getAllExpenses() {
  return api.get('/expense/all')
}

export function getExpenseDetail(id) {
  return api.get(`/expense/${id}`)
}

export function getAuditLogs(id) {
  return api.get(`/expense/${id}/audit-logs`)
}

export function getApprovalTasks(id) {
  return api.get(`/expense/${id}/tasks`)
}

export function getTrialBalance() {
  return api.get('/accounting/trial-balance')
}

export function getAccountBalances() {
  return api.get('/accounting/balances')
}
