<script setup>
import { onMounted } from 'vue'
import { useUserStore } from '@/stores/users'

const userStore = useUserStore()

const roleMap = { MANAGER: '管理员', EMPLOYEE: '普通用户' }

onMounted(async () => {
  await userStore.fetchUsers()
})
</script>

<template>
  <div class="page">
    <el-card shadow="never" header="用户列表">
      <el-table :data="userStore.users" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column prop="username" label="用户名" width="120" />
        <el-table-column prop="department" label="部门" width="150" />
        <el-table-column prop="role" label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="row.role === 'MANAGER' ? 'warning' : 'info'" size="small">
              {{ roleMap[row.role] || row.role }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 1000px; margin: 0 auto; }
</style>
