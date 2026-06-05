<script setup>
import { onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import LeaveForm from '@/components/LeaveForm.vue'
import LeaveTable from '@/components/LeaveTable.vue'
import { useApprovalStore } from '@/stores/approval'
import { useUserStore } from '@/stores/users'

const store = useApprovalStore()
const userStore = useUserStore()

async function handleSubmit(formData) {
  try {
    await store.submitLeave(formData)
    ElMessage.success('提交成功')
  } catch {
    // handled by interceptor
  }
}

onMounted(async () => {
  await userStore.fetchUsers()
  store.fetchMyRequests()
})
</script>

<template>
  <div class="page">
    <el-row :gutter="20">
      <el-col :span="10">
        <el-card header="提交请假申请" shadow="never">
          <LeaveForm @submit="handleSubmit" />
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card header="我的请假记录" shadow="never">
          <LeaveTable :data="store.myRequests" :show-actions="true" action-type="view" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page { max-width: 1200px; margin: 0 auto; }
</style>
