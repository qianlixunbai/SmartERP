<script setup>
import { ref, onMounted } from 'vue'
import { usePortfolioStore } from '@/stores/portfolio'

const store = usePortfolioStore()
const loading = ref(false)
const selectedPortfolio = ref(null)

onMounted(async () => {
  loading.value = true
  await store.fetchPortfolios()
  if (store.portfolios.length > 0) {
    selectedPortfolio.value = store.portfolios[0].id
    await store.fetchHoldings(selectedPortfolio.value)
  }
  loading.value = false
})

async function onPortfolioChange(id) {
  if (!id) return
  loading.value = true
  await store.fetchHoldings(id)
  loading.value = false
}

function formatMoney(val) {
  if (val == null) return '--'
  return '$' + Number(val).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

function pnlClass(val) {
  if (val > 0) return 'pnl-up'
  if (val < 0) return 'pnl-down'
  return ''
}
</script>

<template>
  <div class="page" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>持仓明细</span>
          <el-select v-model="selectedPortfolio" placeholder="选择组合" style="width: 240px" @change="onPortfolioChange">
            <el-option v-for="p in store.portfolios" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </div>
      </template>

      <el-table :data="store.holdings" stripe show-summary :summary-method="({ columns, data }) => {
        const sums = []
        columns.forEach((col, i) => {
          if (i === 0) { sums[i] = '合计'; return }
          if (['totalCost', 'marketValue', 'unrealizedPnl'].includes(col.property)) {
            sums[i] = formatMoney(data.reduce((s, r) => s + Number(r[col.property] || 0), 0))
          } else if (col.property === 'allocation') {
            sums[i] = '100%'
          } else {
            sums[i] = ''
          }
        })
        return sums
      }">
        <el-table-column prop="symbol" label="代码" width="90" />
        <el-table-column prop="assetName" label="名称" min-width="180" />
        <el-table-column prop="assetType" label="类型" width="70" />
        <el-table-column prop="quantity" label="数量" width="100" align="right">
          <template #default="{ row }">{{ Number(row.quantity).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="avgCost" label="均价" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.avgCost) }}</template>
        </el-table-column>
        <el-table-column prop="totalCost" label="总成本" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.totalCost) }}</template>
        </el-table-column>
        <el-table-column prop="currentPrice" label="现价" width="100" align="right">
          <template #default="{ row }">{{ formatMoney(row.currentPrice) }}</template>
        </el-table-column>
        <el-table-column prop="marketValue" label="市值" width="120" align="right">
          <template #default="{ row }">{{ formatMoney(row.marketValue) }}</template>
        </el-table-column>
        <el-table-column prop="unrealizedPnl" label="浮动盈亏" width="120" align="right">
          <template #default="{ row }">
            <span :class="pnlClass(row.unrealizedPnl)">{{ formatMoney(row.unrealizedPnl) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="pnlPercent" label="盈亏%" width="90" align="right">
          <template #default="{ row }">
            <span :class="pnlClass(row.pnlPercent)">{{ row.pnlPercent }}%</span>
          </template>
        </el-table-column>
        <el-table-column prop="allocation" label="占比" width="80" align="right">
          <template #default="{ row }">{{ row.allocation }}%</template>
        </el-table-column>
      </el-table>

      <div v-if="!store.holdings.length && !loading" class="empty-hint">暂无持仓数据</div>
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 1200px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.pnl-up { color: #f56c6c; font-weight: 600; }
.pnl-down { color: #67c23a; font-weight: 600; }
.empty-hint { color: #999; text-align: center; padding: 40px 0; }
</style>
