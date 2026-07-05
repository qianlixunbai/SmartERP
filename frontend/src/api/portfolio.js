import api from './index'

// ========== 组合管理 ==========

export function getPortfolios() {
  return api.get('/portfolio/portfolios')
}

export function createPortfolio(data) {
  return api.post('/portfolio/portfolios', data)
}

// ========== 资产标的 ==========

export function getAssets() {
  return api.get('/portfolio/assets')
}

export function createAsset(data) {
  return api.post('/portfolio/assets', data)
}

export function updateAssetPrice(id, data) {
  return api.put(`/portfolio/assets/${id}/price`, data)
}

// ========== 持仓 ==========

export function getHoldings(portfolioId) {
  return api.get('/portfolio/holdings', { params: { portfolioId } })
}

// ========== 交易 ==========

export function getTrades(portfolioId) {
  return api.get('/portfolio/trades', { params: { portfolioId } })
}

export function executeTrade(data) {
  return api.post('/portfolio/trades', data)
}

// ========== 股息 ==========

export function getDividends(portfolioId) {
  return api.get('/portfolio/dividends', { params: { portfolioId } })
}

export function recordDividend(data) {
  return api.post('/portfolio/dividends', data)
}

// ========== 分析 ==========

export function getAllocation(portfolioId) {
  return api.get('/portfolio/allocation', { params: { portfolioId } })
}

export function getPerformance(portfolioId) {
  return api.get('/portfolio/performance', { params: { portfolioId } })
}

export function getDashboard(portfolioId) {
  return api.get('/portfolio/dashboard', { params: { portfolioId } })
}
