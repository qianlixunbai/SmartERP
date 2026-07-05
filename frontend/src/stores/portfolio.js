import { defineStore } from 'pinia'
import { ref } from 'vue'
import * as portfolioApi from '@/api/portfolio'

export const usePortfolioStore = defineStore('portfolio', () => {
  const portfolios = ref([])
  const assets = ref([])
  const holdings = ref([])
  const trades = ref([])
  const dividends = ref([])
  const allocation = ref([])
  const dashboardData = ref(null)

  async function fetchPortfolios() {
    portfolios.value = await portfolioApi.getPortfolios()
  }

  async function fetchAssets() {
    assets.value = await portfolioApi.getAssets()
  }

  async function fetchHoldings(portfolioId) {
    holdings.value = await portfolioApi.getHoldings(portfolioId)
  }

  async function fetchTrades(portfolioId) {
    trades.value = await portfolioApi.getTrades(portfolioId)
  }

  async function fetchDividends(portfolioId) {
    dividends.value = await portfolioApi.getDividends(portfolioId)
  }

  async function fetchAllocation(portfolioId) {
    allocation.value = await portfolioApi.getAllocation(portfolioId)
  }

  async function fetchDashboard(portfolioId) {
    dashboardData.value = await portfolioApi.getDashboard(portfolioId)
  }

  return {
    portfolios, assets, holdings, trades, dividends, allocation, dashboardData,
    fetchPortfolios, fetchAssets, fetchHoldings, fetchTrades,
    fetchDividends, fetchAllocation, fetchDashboard
  }
})
