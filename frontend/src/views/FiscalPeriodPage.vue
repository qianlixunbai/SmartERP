<script setup>
import { ref, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useAccountingStore } from '@/stores/accounting'
import * as accountingApi from '@/api/accounting'

const store = useAccountingStore()
const selectedPeriod = ref(null)
const balances = ref([])
const loadingBalances = ref(false)

const openPeriods = computed(() => store.periods.filter(p => p.status === 'OPEN'))
const closedPeriods = computed(() => store.periods.filter(p => p.status === 'CLOSED'))

onMounted(() => store.fetchPeriods())

async function handleClose(period) {
  try {
    await ElMessageBox.confirm(
      `确认关闭 ${period.year}年${period.month}月 的财务期间？关闭后将生成余额快照，期间内无法再记账。`,
      '月结确认',
      { type: 'warning' }
    )
    await accountingApi.closePeriod(period.year, period.month)
    ElMessage.success('月结完成')
    await store.fetchPeriods()
  } catch {}
}

async function handleReopen(period) {
  try {
    await ElMessageBox.confirm(
      `确认重新打开 ${period.year}年${period.month}月？余额快照将被删除。`,
      '反月结确认',
      { type: 'warning' }
    )
    await accountingApi.reopenPeriod(period.year, period.month)
    ElMessage.success('反月结完成')
    await store.fetchPeriods()
  } catch {}
}

async function viewBalances(period) {
  selectedPeriod.value = period
  loadingBalances.value = true
  try {
    balances.value = await accountingApi.getPeriodBalances(period.id)
  } catch {
    balances.value = []
  } finally {
    loadingBalances.value = false
  }
}

function statusType(status) {
  return status === 'OPEN' ? 'success' : 'info'
}
</script>

<template>
  <div class="page">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card header="财务期间管理" shadow="never">
          <el-table :data="store.periods" stripe size="small">
            <el-table-column label="期间" width="120">
              <template #default="{ row }">{{ row.year }}-{{ String(row.month).padStart(2, '0') }}</template>
            </el-table-column>
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="statusType(row.status)" size="small">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button v-if="row.status === 'OPEN'" type="warning" link size="small" @click="handleClose(row)">月结</el-button>
                <el-button v-if="row.status === 'CLOSED'" type="success" link size="small" @click="handleReopen(row)">反月结</el-button>
                <el-button v-if="row.status === 'CLOSED'" type="primary" link size="small" @click="viewBalances(row)">查看快照</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <span v-if="selectedPeriod">
              {{ selectedPeriod.year }}-{{ String(selectedPeriod.month).padStart(2, '0') }} 余额快照
            </span>
            <span v-else>余额快照</span>
          </template>
          <div v-if="!selectedPeriod" class="empty-hint">点击左侧「查看快照」查看期间余额</div>
          <el-table v-else :data="balances" stripe size="small" v-loading="loadingBalances">
            <el-table-column prop="accountId" label="科目ID" width="80" />
            <el-table-column label="成本中心" width="90">
              <template #default="{ row }">{{ row.costCenterId || '-' }}</template>
            </el-table-column>
            <el-table-column label="借方" width="100">
              <template #default="{ row }">{{ row.debitTotal }}</template>
            </el-table-column>
            <el-table-column label="贷方" width="100">
              <template #default="{ row }">{{ row.creditTotal }}</template>
            </el-table-column>
            <el-table-column label="期末余额" width="100">
              <template #default="{ row }">{{ row.closingBalance }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page { max-width: 1200px; margin: 0 auto; }
.empty-hint { color: #999; text-align: center; padding: 40px 0; }
</style>
