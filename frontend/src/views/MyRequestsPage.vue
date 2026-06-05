<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useApprovalStore } from '@/stores/approval'
import { useUserStore } from '@/stores/users'
import LeaveTable from '@/components/LeaveTable.vue'
import { STATUS_MAP } from '@/utils/constants'

const router = useRouter()
const store = useApprovalStore()
const userStore = useUserStore()
const statusFilter = ref('')

const filteredRequests = computed(() =>
  statusFilter.value
    ? store.myRequests.filter(r => r.status === statusFilter.value)
    : store.myRequests
)

function handleView(id) {
  router.push(`/approval/${id}`)
}

onMounted(async () => {
  await userStore.fetchUsers()
  await store.fetchMyRequests()
})
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>我的申请</span>
          <el-select v-model="statusFilter" clearable placeholder="筛选状态" style="width: 140px" size="small">
            <el-option
              v-for="(val, key) in STATUS_MAP"
              :key="key"
              :label="val.label"
              :value="key"
            />
          </el-select>
        </div>
      </template>
      <LeaveTable :data="filteredRequests" :show-actions="true" action-type="view" @action="handleView" />
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 1200px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
