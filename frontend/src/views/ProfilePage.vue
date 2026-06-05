<script setup>
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const roleMap = { MANAGER: '管理员', EMPLOYEE: '普通用户' }
</script>

<template>
  <div class="page">
    <el-card shadow="never" header="个人信息">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="姓名">{{ auth.user?.realName }}</el-descriptions-item>
        <el-descriptions-item label="用户名">{{ auth.user?.username }}</el-descriptions-item>
        <el-descriptions-item label="角色">
          <el-tag :type="auth.user?.role === 'MANAGER' ? 'warning' : 'info'" size="small">
            {{ roleMap[auth.user?.role] || auth.user?.role }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="部门">{{ auth.user?.department }}</el-descriptions-item>
      </el-descriptions>
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 800px; margin: 0 auto; }
</style>
