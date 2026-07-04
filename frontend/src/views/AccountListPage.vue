<script setup>
import { ref, onMounted } from 'vue'
import { getAccountBalances } from '@/api/expense'

const balances = ref([])
const loading = ref(false)

onMounted(async () => {
  loading.value = true
  try {
    balances.value = await getAccountBalances()
  } catch {
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header><span>会计科目表</span></template>
      <el-table :data="balances" stripe v-loading="loading">
        <el-table-column prop="code" label="编码" width="100" />
        <el-table-column prop="name" label="科目名称" width="150" />
        <el-table-column prop="type" label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 'EXPENSE' ? 'warning' : row.type === 'ASSET' ? '' : 'success'">
              {{ row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="借方合计" width="120" align="right">
          <template #default="{ row }">
            <span :class="{ 'has-value': row.debit > 0 }">{{ row.debit > 0 ? '¥' + row.debit : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="贷方合计" width="120" align="right">
          <template #default="{ row }">
            <span :class="{ 'has-value': row.credit > 0 }">{{ row.credit > 0 ? '¥' + row.credit : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="120" align="right">
          <template #default="{ row }">
            <span class="balance" :class="{ negative: row.balance < 0 }">
              ¥{{ row.balance }}
            </span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 1000px; margin: 0 auto; }
.has-value { font-weight: 500; }
.balance { font-weight: 600; }
.negative { color: #F56C6C; }
</style>
