<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useApprovalStore } from '@/stores/approval'
import { useUserStore } from '@/stores/users'
import LeaveTable from '@/components/LeaveTable.vue'
import { LEAVE_TYPES, STATUS_MAP } from '@/utils/constants'

const router = useRouter()
const store = useApprovalStore()
const userStore = useUserStore()
const typeFilter = ref('')
const statusFilter = ref('')

const filteredRequests = computed(() => {
  let list = store.allRequests
  if (typeFilter.value) list = list.filter(r => r.leaveType === typeFilter.value)
  if (statusFilter.value) list = list.filter(r => r.status === statusFilter.value)
  return list
})

function handleView(id) {
  router.push(`/approval/${id}`)
}

onMounted(async () => {
  await userStore.fetchUsers()
  await store.fetchAllRequests()
})
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>全部请假记录</span>
          <div class="filters">
            <el-select v-model="typeFilter" clearable placeholder="请假类型" style="width: 120px; margin-right: 10px" size="small">
              <el-option v-for="t in LEAVE_TYPES" :key="t" :label="t" :value="t" />
            </el-select>
            <el-select v-model="statusFilter" clearable placeholder="审批状态" style="width: 120px" size="small">
              <el-option v-for="(val, key) in STATUS_MAP" :key="key" :label="val.label" :value="key" />
            </el-select>
          </div>
        </div>
      </template>
      <LeaveTable :data="filteredRequests" :show-actions="true" action-type="view" @action="handleView" />
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 1200px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.filters { display: flex; }
</style>
