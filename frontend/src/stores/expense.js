import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as expenseApi from '@/api/expense'

export const useExpenseStore = defineStore('expense', () => {
  const myExpenses = ref([])
  const pendingExpenses = ref([])
  const allExpenses = ref([])
  const currentDetail = ref(null)
  const currentAuditLogs = ref([])
  const currentTasks = ref([])

  async function fetchMyExpenses() {
    myExpenses.value = await expenseApi.getMyExpenses()
  }

  async function fetchPendingExpenses() {
    pendingExpenses.value = await expenseApi.getPendingExpenses()
  }

  async function fetchAllExpenses() {
    allExpenses.value = await expenseApi.getAllExpenses()
  }

  async function fetchDetail(id) {
    currentDetail.value = await expenseApi.getExpenseDetail(id)
  }

  async function fetchAuditLogs(id) {
    currentAuditLogs.value = await expenseApi.getAuditLogs(id)
  }

  async function fetchTasks(id) {
    currentTasks.value = await expenseApi.getApprovalTasks(id)
  }

  async function submitExpense(formData) {
    await expenseApi.submitExpense(formData)
    await fetchMyExpenses()
  }

  async function approve(requestId, action, comment) {
    await expenseApi.approveExpense(requestId, action, comment)
  }

  return {
    myExpenses, pendingExpenses, allExpenses,
    currentDetail, currentAuditLogs, currentTasks,
    fetchMyExpenses, fetchPendingExpenses, fetchAllExpenses,
    fetchDetail, fetchAuditLogs, fetchTasks,
    submitExpense, approve
  }
})
