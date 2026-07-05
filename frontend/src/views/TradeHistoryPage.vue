<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { usePortfolioStore } from '@/stores/portfolio'
import * as portfolioApi from '@/api/portfolio'

const store = usePortfolioStore()
const loading = ref(false)
const selectedPortfolio = ref(null)
const tradeDialog = ref(false)
const tradeForm = ref({
  portfolioId: null, assetId: null, tradeType: 'BUY',
  quantity: '', price: '', fee: 0, tradeDate: '', memo: ''
})

onMounted(async () => {
  loading.value = true
  await Promise.all([store.fetchPortfolios(), store.fetchAssets()])
  if (store.portfolios.length > 0) {
    selectedPortfolio.value = store.portfolios[0].id
    await store.fetchTrades(selectedPortfolio.value)
  }
  loading.value = false
})

async function onPortfolioChange(id) {
  if (!id) return
  loading.value = true
  await store.fetchTrades(id)
  loading.value = false
}

function openCreate() {
  tradeForm.value = {
    portfolioId: selectedPortfolio.value, assetId: null, tradeType: 'BUY',
    quantity: '', price: '', fee: 0, tradeDate: '', memo: ''
  }
  tradeDialog.value = true
}

async function handleExecute() {
  if (!tradeForm.value.assetId || !tradeForm.value.quantity || !tradeForm.value.price) {
    ElMessage.warning('请填写完整交易信息')
    return
  }
  try {
    await portfolioApi.executeTrade({
      ...tradeForm.value,
      quantity: Number(tradeForm.value.quantity),
      price: Number(tradeForm.value.price),
      fee: Number(tradeForm.value.fee) || 0
    })
    ElMessage.success('交易执行成功')
    tradeDialog.value = false
    await store.fetchTrades(selectedPortfolio.value)
  } catch {}
}

function getSymbol(assetId) {
  const asset = store.assets.find(a => a.id === assetId)
  return asset ? asset.symbol : assetId
}
</script>

<template>
  <div class="page" v-loading="loading">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>交易记录</span>
          <div class="header-actions">
            <el-select v-model="selectedPortfolio" placeholder="选择组合" style="width: 240px; margin-right: 12px" @change="onPortfolioChange">
              <el-option v-for="p in store.portfolios" :key="p.id" :label="p.name" :value="p.id" />
            </el-select>
            <el-button type="primary" :icon="Plus" @click="openCreate">执行交易</el-button>
          </div>
        </div>
      </template>

      <el-table :data="store.trades" stripe>
        <el-table-column prop="tradeDate" label="交易日期" width="110" />
        <el-table-column label="标的" width="100">
          <template #default="{ row }">{{ getSymbol(row.assetId) }}</template>
        </el-table-column>
        <el-table-column label="方向" width="80">
          <template #default="{ row }">
            <el-tag :type="row.tradeType === 'BUY' ? 'danger' : 'success'" size="small">
              {{ row.tradeType === 'BUY' ? '买入' : '卖出' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="100" align="right">
          <template #default="{ row }">{{ Number(row.quantity).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="price" label="成交价" width="110" align="right">
          <template #default="{ row }">${{ Number(row.price).toFixed(4) }}</template>
        </el-table-column>
        <el-table-column prop="totalAmount" label="成交金额" width="120" align="right">
          <template #default="{ row }">${{ Number(row.totalAmount).toLocaleString('en-US', { minimumFractionDigits: 2 }) }}</template>
        </el-table-column>
        <el-table-column prop="fee" label="手续费" width="90" align="right">
          <template #default="{ row }">${{ Number(row.fee).toFixed(2) }}</template>
        </el-table-column>
        <el-table-column prop="memo" label="备注" min-width="160" />
      </el-table>

      <div v-if="!store.trades.length && !loading" class="empty-hint">暂无交易记录</div>
    </el-card>

    <el-dialog v-model="tradeDialog" title="执行交易" width="520px">
      <el-form :model="tradeForm" label-width="80px">
        <el-form-item label="组合">
          <el-input :model-value="store.portfolios.find(p => p.id === tradeForm.portfolioId)?.name || ''" disabled />
        </el-form-item>
        <el-form-item label="标的" required>
          <el-select v-model="tradeForm.assetId" placeholder="选择资产标的" class="w-full">
            <el-option v-for="a in store.assets" :key="a.id" :label="`${a.symbol} - ${a.name}`" :value="a.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="方向" required>
          <el-radio-group v-model="tradeForm.tradeType">
            <el-radio value="BUY">买入</el-radio>
            <el-radio value="SELL">卖出</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="数量" required>
          <el-input-number v-model="tradeForm.quantity" :precision="6" :min="0" class="w-full" />
        </el-form-item>
        <el-form-item label="成交价" required>
          <el-input-number v-model="tradeForm.price" :precision="4" :min="0" class="w-full" />
        </el-form-item>
        <el-form-item label="手续费">
          <el-input-number v-model="tradeForm.fee" :precision="2" :min="0" class="w-full" />
        </el-form-item>
        <el-form-item label="交易日期">
          <el-date-picker v-model="tradeForm.tradeDate" type="date" value-format="YYYY-MM-DD" placeholder="默认今天" class="w-full" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="tradeForm.memo" placeholder="如 定投买入" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="tradeDialog = false">取消</el-button>
        <el-button type="primary" @click="handleExecute">执行</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page { max-width: 1100px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-actions { display: flex; align-items: center; }
.w-full { width: 100%; }
.empty-hint { color: #999; text-align: center; padding: 40px 0; }
</style>
