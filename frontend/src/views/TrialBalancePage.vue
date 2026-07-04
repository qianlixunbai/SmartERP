<script setup>
import { ref, onMounted, computed } from 'vue'
import { getTrialBalance } from '@/api/expense'

const data = ref(null)
const loading = ref(false)

const balanced = computed(() => data.value?.balanced ?? false)

onMounted(async () => {
  loading.value = true
  try {
    data.value = await getTrialBalance()
  } catch {
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <el-card shadow="never" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>试算平衡表</span>
          <el-tag v-if="data" :type="balanced ? 'success' : 'danger'" size="large">
            {{ balanced ? '已平衡' : '不平衡' }}
          </el-tag>
        </div>
      </template>

      <div v-if="data" class="balance-cards">
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">借方合计</div>
          <div class="stat-value">¥{{ data.totalDebit }}</div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">贷方合计</div>
          <div class="stat-value">¥{{ data.totalCredit }}</div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">分录条数</div>
          <div class="stat-value">{{ data.entryCount }}</div>
        </el-card>
        <el-card shadow="hover" class="stat-card">
          <div class="stat-label">差额</div>
          <div class="stat-value" :class="{ 'text-danger': !balanced, 'text-success': balanced }">
            ¥{{ (data.totalDebit - data.totalCredit).toFixed(2) }}
          </div>
        </el-card>
      </div>

      <el-empty v-if="!data && !loading" description="暂无分录数据" />
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 800px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.balance-cards { display: grid; grid-template-columns: repeat(2, 1fr); gap: 16px; }
.stat-card { text-align: center; }
.stat-label { color: var(--text-secondary); font-size: 14px; margin-bottom: 8px; }
.stat-value { font-size: 28px; font-weight: 700; }
.text-danger { color: #F56C6C; }
.text-success { color: #67C23A; }
</style>
