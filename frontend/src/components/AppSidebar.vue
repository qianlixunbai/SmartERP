<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { DocumentAdd, List, Setting, Plus, DataAnalysis, Download, HomeFilled, Clock, Files, Avatar } from '@element-plus/icons-vue'

const route = useRoute()
const auth = useAuthStore()

const activeMenu = computed(() => route.path)

function handleExport() {
  const token = localStorage.getItem('token')
  const a = document.createElement('a')
  a.href = `/api/export/leaves`
  a.download = ''
  a.click()
}

const menuItems = [
  { path: '/dashboard', title: '工作台', icon: HomeFilled },
  { path: '/submit-application', title: '提交申请', icon: DocumentAdd },
  { path: '/my-requests', title: '我的申请', icon: Files },
  { path: '/pending-approvals', title: '待审批', icon: Clock },
  { path: '/stats', title: '统计报表', icon: DataAnalysis }
]
</script>

<template>
  <div class="sidebar">
    <div class="sidebar-logo">SmartOA</div>
    <el-menu
      :default-active="activeMenu"
      router
      background-color="#304156"
      text-color="#bfcbd9"
      active-text-color="#409eff"
    >
      <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.title }}</span>
      </el-menu-item>

      <el-sub-menu v-if="auth.isManager" index="/templates-group">
        <template #title>
          <el-icon><Setting /></el-icon>
          <span>模板管理</span>
        </template>
        <el-menu-item index="/templates">
          <el-icon><List /></el-icon>
          <span>模板列表</span>
        </el-menu-item>
        <el-menu-item index="/templates/edit">
          <el-icon><Plus /></el-icon>
          <span>新建模板</span>
        </el-menu-item>
      </el-sub-menu>

      <el-menu-item v-if="auth.isManager" index="/all-requests">
        <el-icon><List /></el-icon>
        <span>全部记录</span>
      </el-menu-item>

      <el-menu-item v-if="auth.isManager" index="/users">
        <el-icon><Avatar /></el-icon>
        <span>用户列表</span>
      </el-menu-item>

      <el-menu-item v-if="auth.isManager" @click="handleExport">
        <el-icon><Download /></el-icon>
        <span>导出Excel</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<style scoped>
.sidebar-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 2px;
  border-bottom: 1px solid rgba(255,255,255,.1);
}
.el-menu {
  border-right: none;
}
</style>
