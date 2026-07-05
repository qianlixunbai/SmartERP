<script setup>
import { ref, onMounted, watch } from 'vue'
import * as accountingApi from '@/api/accounting'
import { useAccountingStore } from '@/stores/accounting'

const store = useAccountingStore()
const loading = ref(false)
const dashboard = ref(null)
const filters = ref({ periodId: null, costCenterId: null, profitCenterId: null })

async function fetchDashboard() {
  loading.value = true
  try {
    dashboard.value = await accountingApi.getAccountingDashboard(filters.value)
  } catch {
    dashboard.value = null
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await Promise.all([
    store.fetchPeriods(),
    store.fetchCostCenters(),
    store.fetchProfitCenters()
  ])
  fetchDashboard()
})

watch(filters, fetchDashboard, { deep: true })
</script>

<template>
  <div class="page" v-loading="loading">
    <el-card header="财务看板" shadow="never">
      <el-row :gutter="16" class="filter-row">
        <el-col :span="8">
          <el-select v-model="filters.periodId" placeholder="选择期间" clearable class="w-full">
            <el-option v-for="p in store.periods" :key="p.id"
              :label="`${p.year}-${String(p.month).padStart(2,'0')} (${p.status})`" :value="p.id" />
          </el-select>
        </el-col>
        <el-col :span="8">
          <el-select v-model="filters.costCenterId" placeholder="成本中心" clearable class="w-full">
            <el-option v-for="c in store.costCenters" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-col>
        <el-col :span="8">
          <el-select v-model="filters.profitCenterId" placeholder="利润中心" clearable class="w-full">
            <el-option v-for="p in store.profitCenters" :key="p.id" :label="p.name" :value="p.id" />
          </el-select>
        </el-col>
      </el-row>

      <template v-if="dashboard">
        <el-row :gutter="20" class="stats-row">
          <el-col :span="8">
            <div class="stat-card debit">
              <div class="stat-label">借方合计</div>
              <div class="stat-value">{{ dashboard.totalDebit }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-card credit">
              <div class="stat-label">贷方合计</div>
              <div class="stat-value">{{ dashboard.totalCredit }}</div>
            </div>
          </el-col>
          <el-col :span="8">
            <div class="stat-card count">
              <div class="stat-label">余额条数</div>
              <div class="stat-value">{{ dashboard.balanceCount }}</div>
            </div>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-card header="按科目类型" shadow="never" class="inner-card">
              <div v-if="dashboard.byAccountType && Object.keys(dashboard.byAccountType).length">
                <div v-for="(val, key) in dashboard.byAccountType" :key="key" class="data-row">
                  <span class="data-label">{{ key }}</span>
                  <span class="data-value">{{ val }}</span>
                </div>
              </div>
              <div v-else class="empty-hint">暂无数据</div>
            </el-card>
          </el-col>
          <el-col :span="12">
            <el-card header="按成本中心" shadow="never" class="inner-card">
              <div v-if="dashboard.byCostCenter && Object.keys(dashboard.byCostCenter).length">
                <div v-for="(val, key) in dashboard.byCostCenter" :key="key" class="data-row">
                  <span class="data-label">{{ key }}</span>
                  <span class="data-value">{{ val }}</span>
                </div>
              </div>
              <div v-else class="empty-hint">暂无数据（月结后生成）</div>
            </el-card>
          </el-col>
        </el-row>
      </template>

      <div v-else class="empty-hint">请选择期间查看财务数据</div>
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 1200px; margin: 0 auto; }
.w-full { width: 100%; }
.filter-row { margin-bottom: 20px; }
.stats-row { margin-bottom: 20px; }
.stat-card {
  padding: 20px;
  border-radius: 8px;
  text-align: center;
}
.stat-card.debit { background: linear-gradient(135deg, #e8f5e9, #c8e6c9); }
.stat-card.credit { background: linear-gradient(135deg, #e3f2fd, #bbdefb); }
.stat-card.count { background: linear-gradient(135deg, #fff3e0, #ffe0b2); }
.stat-label { color: #666; font-size: 13px; margin-bottom: 8px; }
.stat-value { font-size: 24px; font-weight: 700; color: #333; }
.inner-card { min-height: 200px; }
.data-row {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  border-bottom: 1px solid #f0f0f0;
}
.data-label { color: #666; }
.data-value { font-weight: 600; color: #333; }
.empty-hint { color: #999; text-align: center; padding: 40px 0; }
</style>
