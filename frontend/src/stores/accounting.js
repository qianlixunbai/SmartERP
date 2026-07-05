import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as accountingApi from '@/api/accounting'

export const useAccountingStore = defineStore('accounting', () => {
  const costCenters = ref([])
  const profitCenters = ref([])
  const periods = ref([])
  const periodBalances = ref([])
  const dashboardData = ref(null)

  async function fetchCostCenters() {
    costCenters.value = await accountingApi.getCostCenters()
  }

  async function fetchProfitCenters() {
    profitCenters.value = await accountingApi.getProfitCenters()
  }

  async function fetchPeriods() {
    periods.value = await accountingApi.getPeriods()
  }

  async function fetchPeriodBalances(periodId) {
    periodBalances.value = await accountingApi.getPeriodBalances(periodId)
  }

  async function fetchDashboard(params) {
    dashboardData.value = await accountingApi.getAccountingDashboard(params)
  }

  return {
    costCenters, profitCenters, periods, periodBalances, dashboardData,
    fetchCostCenters, fetchProfitCenters, fetchPeriods,
    fetchPeriodBalances, fetchDashboard
  }
})
