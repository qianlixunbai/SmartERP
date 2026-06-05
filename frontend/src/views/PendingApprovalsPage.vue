<script setup>
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useApprovalStore } from '@/stores/approval'
import { useUserStore } from '@/stores/users'
import LeaveTable from '@/components/LeaveTable.vue'
import { repairStuckRequests } from '@/api/leave'
import { ElMessage } from 'element-plus'
import { ref } from 'vue'

const router = useRouter()
const auth = useAuthStore()
const store = useApprovalStore()
const userStore = useUserStore()
const repairing = ref(false)

function handleApprove(id) {
  router.push(`/approval/${id}`)
}

async function handleRepair() {
  repairing.value = true
  try {
    const count = await repairStuckRequests()
    ElMessage.success(`修复完成，共处理 ${count} 条滞留申请`)
    await store.fetchPendingRequests()
  } catch {
    // handled by interceptor
  } finally {
    repairing.value = false
  }
}

onMounted(async () => {
  await userStore.fetchUsers()
  await store.fetchPendingRequests()
})
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>待审批</span>
          <el-button v-if="auth.isManager" size="small" type="warning" :loading="repairing" @click="handleRepair">
            修复滞留申请
          </el-button>
        </div>
      </template>
      <LeaveTable :data="store.pendingRequests" :show-actions="true" action-type="approve" @action="handleApprove" />
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 1200px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
