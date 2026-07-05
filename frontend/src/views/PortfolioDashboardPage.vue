<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import { usePortfolioStore } from '@/stores/portfolio'
import * as echarts from 'echarts'

const store = usePortfolioStore()
const loading = ref(false)
const selectedPortfolio = ref(null)
const dashboard = ref(null)
const pieRef = ref(null)
let pieChart = null

onMounted(async () => {
  loading.value = true
  await store.fetchPortfolios()
  if (store.portfolios.length > 0) {
    selectedPortfolio.value = store.portfolios[0].id
    await loadDashboard(selectedPortfolio.value)
  }
  loading.value = false
})

async function loadDashboard(portfolioId) {
  if (!portfolioId) return
  loading.value = true
  await store.fetchDashboard(portfolioId)
  dashboard.value = store.dashboardData
  loading.value = false
  await nextTick()
  renderPie()
}

async function onPortfolioChange(id) {
  await loadDashboard(id)
}

function renderPie() {
  if (!dashboard.value?.allocation?.length || !pieRef.value) return
  if (pieChart) pieChart.dispose()
  pieChart = echarts.init(pieRef.value)
  pieChart.setOption({
    tooltip: { trigger: 'item', formatter: '{b}: ${c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      label: { formatter: '{b}\n{d}%' },
      data: dashboard.value.allocation.map(a => ({
        name: a.symbol, value: Number(a.marketValue)
      }))
    }]
  })
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
          <span>投资看板</span>
          <el-select v-model="selectedPortfolio" placeholder="选择组合" style="width: 240px" @change="onPortfolioChange">
            <el-option v-for="p in store.portfolios" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </div>
      </template>

      <template v-if="dashboard">
        <!-- 统计卡片 -->
        <el-row :gutter="16" class="stats-row">
          <el-col :span="6">
            <div class="stat-card value">
              <div class="stat-label">总市值</div>
              <div class="stat-value">{{ formatMoney(dashboard.totalMarketValue) }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card cost">
              <div class="stat-label">总成本</div>
              <div class="stat-value">{{ formatMoney(dashboard.totalCost) }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card" :class="dashboard.totalPnl >= 0 ? 'up' : 'down'">
              <div class="stat-label">浮动盈亏</div>
              <div class="stat-value" :class="pnlClass(dashboard.totalPnl)">{{ formatMoney(dashboard.totalPnl) }}</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="stat-card dividend">
              <div class="stat-label">股息收入</div>
              <div class="stat-value">{{ formatMoney(dashboard.totalDividends) }}</div>
            </div>
          </el-col>
        </el-row>

        <!-- 绩效指标 -->
        <el-row :gutter="16" class="stats-row">
          <el-col :span="6">
            <div class="metric-card">
              <div class="metric-label">总收益率</div>
              <div class="metric-value" :class="pnlClass(dashboard.performance?.totalReturnPercent)">
                {{ dashboard.performance?.totalReturnPercent }}%
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="metric-card">
              <div class="metric-label">年化收益率</div>
              <div class="metric-value" :class="pnlClass(dashboard.performance?.annualizedReturnPercent)">
                {{ dashboard.performance?.annualizedReturnPercent }}%
              </div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="metric-card">
              <div class="metric-label">最大回撤</div>
              <div class="metric-value warn">{{ dashboard.maxDrawdown?.maxDrawdownPercent }}%</div>
            </div>
          </el-col>
          <el-col :span="6">
            <div class="metric-card">
              <div class="metric-label">夏普比率</div>
              <div class="metric-value">{{ dashboard.sharpeRatio?.sharpeRatio }}</div>
            </div>
          </el-col>
        </el-row>

        <!-- 资产配置饼图 + 持仓表 -->
        <el-row :gutter="20">
          <el-col :span="10">
            <el-card header="资产配置" shadow="never" class="inner-card">
              <div ref="pieRef" class="pie-chart"></div>
            </el-card>
          </el-col>
          <el-col :span="14">
            <el-card header="持仓明细" shadow="never" class="inner-card">
              <el-table :data="dashboard.holdings" stripe size="small">
                <el-table-column prop="symbol" label="代码" width="70" />
                <el-table-column prop="assetName" label="名称" min-width="130" show-overflow-tooltip />
                <el-table-column label="市值" width="110" align="right">
                  <template #default="{ row }">{{ formatMoney(row.marketValue) }}</template>
                </el-table-column>
                <el-table-column label="盈亏" width="100" align="right">
                  <template #default="{ row }">
                    <span :class="pnlClass(row.unrealizedPnl)">{{ formatMoney(row.unrealizedPnl) }}</span>
                  </template>
                </el-table-column>
                <el-table-column label="占比" width="70" align="right">
                  <template #default="{ row }">{{ row.allocation }}%</template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <div v-else-if="!loading" class="empty-hint">选择组合查看投资看板</div>
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 1200px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.stats-row { margin-bottom: 16px; }
.stat-card {
  padding: 20px;
  border-radius: 8px;
  text-align: center;
  background: #f5f7fa;
}
.stat-card.value { background: linear-gradient(135deg, #e3f2fd, #bbdefb); }
.stat-card.cost { background: linear-gradient(135deg, #f5f5f5, #e0e0e0); }
.stat-card.up { background: linear-gradient(135deg, #ffebee, #ffcdd2); }
.stat-card.down { background: linear-gradient(135deg, #e8f5e9, #c8e6c9); }
.stat-card.dividend { background: linear-gradient(135deg, #fff3e0, #ffe0b2); }
.stat-label { color: #666; font-size: 13px; margin-bottom: 8px; }
.stat-value { font-size: 22px; font-weight: 700; color: #333; }
.metric-card {
  padding: 16px;
  border-radius: 8px;
  text-align: center;
  background: #fafafa;
  border: 1px solid #ebeef5;
}
.metric-label { color: #999; font-size: 12px; margin-bottom: 6px; }
.metric-value { font-size: 20px; font-weight: 700; color: #333; }
.metric-value.warn { color: #e6a23c; }
.pnl-up { color: #f56c6c; }
.pnl-down { color: #67c23a; }
.inner-card { min-height: 360px; }
.pie-chart { width: 100%; height: 320px; }
.empty-hint { color: #999; text-align: center; padding: 40px 0; }
</style>
