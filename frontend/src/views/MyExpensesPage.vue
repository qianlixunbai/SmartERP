<script setup>
import { onMounted } from 'vue'
import { useExpenseStore } from '@/stores/expense'
import { useUserStore } from '@/stores/users'
import StatusTag from '@/components/StatusTag.vue'

const store = useExpenseStore()
const userStore = useUserStore()

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(async () => {
  await userStore.fetchUsers()
  await store.fetchMyExpenses()
})
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header><span>我的报销记录</span></template>
      <el-table :data="store.myExpenses" stripe>
        <el-table-column type="index" width="50" />
        <el-table-column prop="category" label="类别" width="80" />
        <el-table-column label="金额" width="120">
          <template #default="{ row }">
            <span class="amount">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="说明" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <StatusTag :status="row.status" />
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="140">
          <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <router-link :to="`/expense/${row.id}`">
              <el-button type="primary" link>详情</el-button>
            </router-link>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 1200px; margin: 0 auto; }
.amount { font-weight: 600; color: var(--primary-color); }
</style>
