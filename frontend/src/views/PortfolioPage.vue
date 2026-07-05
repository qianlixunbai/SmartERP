<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { usePortfolioStore } from '@/stores/portfolio'
import * as portfolioApi from '@/api/portfolio'

const store = usePortfolioStore()
const activeTab = ref('portfolios')

// 组合表单
const portfolioDialog = ref(false)
const portfolioForm = ref({ name: '', description: '', baseCurrency: 'USD' })

// 资产标的表单
const assetDialog = ref(false)
const assetForm = ref({ symbol: '', name: '', assetType: 'ETF', currency: 'USD' })

// 更新价格
const priceDialog = ref(false)
const priceForm = ref({ id: null, symbol: '', currentPrice: '' })

onMounted(async () => {
  await Promise.all([store.fetchPortfolios(), store.fetchAssets()])
})

async function handleCreatePortfolio() {
  if (!portfolioForm.value.name) { ElMessage.warning('名称不能为空'); return }
  await portfolioApi.createPortfolio(portfolioForm.value)
  ElMessage.success('创建成功')
  portfolioDialog.value = false
  await store.fetchPortfolios()
}

async function handleCreateAsset() {
  if (!assetForm.value.symbol || !assetForm.value.name) { ElMessage.warning('代码和名称不能为空'); return }
  await portfolioApi.createAsset(assetForm.value)
  ElMessage.success('创建成功')
  assetDialog.value = false
  await store.fetchAssets()
}

function openPriceUpdate(row) {
  priceForm.value = { id: row.id, symbol: row.symbol, currentPrice: '' }
  priceDialog.value = true
}

async function handleUpdatePrice() {
  if (!priceForm.value.currentPrice) { ElMessage.warning('请输入价格'); return }
  await portfolioApi.updateAssetPrice(priceForm.value.id, {
    symbol: priceForm.value.symbol,
    currentPrice: Number(priceForm.value.currentPrice)
  })
  ElMessage.success('价格已更新')
  priceDialog.value = false
  await store.fetchAssets()
}
</script>

<template>
  <div class="page">
    <el-tabs v-model="activeTab">
      <el-tab-pane label="投资组合" name="portfolios">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>投资组合列表</span>
              <el-button type="primary" :icon="Plus" @click="portfolioForm = { name: '', description: '', baseCurrency: 'USD' }; portfolioDialog = true">新建</el-button>
            </div>
          </template>
          <el-table :data="store.portfolios" stripe>
            <el-table-column prop="id" label="ID" width="60" />
            <el-table-column prop="name" label="组合名称" width="200" />
            <el-table-column prop="description" label="描述" />
            <el-table-column prop="baseCurrency" label="币种" width="80" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.active ? 'success' : 'info'" size="small">
                  {{ row.active ? '启用' : '停用' }}
                </el-tag>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="资产标的" name="assets">
        <el-card shadow="never">
          <template #header>
            <div class="card-header">
              <span>资产标的列表</span>
              <el-button type="primary" :icon="Plus" @click="assetForm = { symbol: '', name: '', assetType: 'ETF', currency: 'USD' }; assetDialog = true">新建</el-button>
            </div>
          </template>
          <el-table :data="store.assets" stripe>
            <el-table-column prop="symbol" label="代码" width="100" />
            <el-table-column prop="name" label="名称" width="240" />
            <el-table-column prop="assetType" label="类型" width="80" />
            <el-table-column prop="currency" label="币种" width="70" />
            <el-table-column label="最新价格" width="120">
              <template #default="{ row }">
                <span v-if="row.currentPrice" class="price">${{ row.currentPrice }}</span>
                <span v-else class="no-price">--</span>
              </template>
            </el-table-column>
            <el-table-column prop="priceDate" label="价格日期" width="110" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button type="primary" link @click="openPriceUpdate(row)">更新价格</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <!-- 新建组合 -->
    <el-dialog v-model="portfolioDialog" title="新建投资组合" width="480px">
      <el-form :model="portfolioForm" label-width="80px">
        <el-form-item label="名称" required>
          <el-input v-model="portfolioForm.name" placeholder="如 主投资组合 (JPY)" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="portfolioForm.description" type="textarea" :rows="2" />
        </el-form-item>
        <el-form-item label="基准币种">
          <el-select v-model="portfolioForm.baseCurrency" class="w-full">
            <el-option label="USD - 美元" value="USD" />
            <el-option label="JPY - 日元" value="JPY" />
            <el-option label="CNY - 人民币" value="CNY" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="portfolioDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreatePortfolio">创建</el-button>
      </template>
    </el-dialog>

    <!-- 新建资产 -->
    <el-dialog v-model="assetDialog" title="新建资产标的" width="480px">
      <el-form :model="assetForm" label-width="80px">
        <el-form-item label="代码" required>
          <el-input v-model="assetForm.symbol" placeholder="如 AAPL" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="assetForm.name" placeholder="如 Apple Inc." />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="assetForm.assetType" class="w-full">
            <el-option label="ETF" value="ETF" />
            <el-option label="STOCK" value="STOCK" />
            <el-option label="BOND" value="BOND" />
            <el-option label="CASH" value="CASH" />
          </el-select>
        </el-form-item>
        <el-form-item label="币种">
          <el-select v-model="assetForm.currency" class="w-full">
            <el-option label="USD" value="USD" />
            <el-option label="JPY" value="JPY" />
            <el-option label="CNY" value="CNY" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="assetDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreateAsset">创建</el-button>
      </template>
    </el-dialog>

    <!-- 更新价格 -->
    <el-dialog v-model="priceDialog" title="更新资产价格" width="400px">
      <el-form :model="priceForm" label-width="80px">
        <el-form-item label="标的">
          <el-input :model-value="priceForm.symbol" disabled />
        </el-form-item>
        <el-form-item label="最新价格" required>
          <el-input-number v-model="priceForm.currentPrice" :precision="4" :min="0" class="w-full" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priceDialog = false">取消</el-button>
        <el-button type="primary" @click="handleUpdatePrice">更新</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page { max-width: 1100px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.w-full { width: 100%; }
.price { color: #409eff; font-weight: 600; }
.no-price { color: #999; }
</style>
