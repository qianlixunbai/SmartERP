<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { DocumentAdd, List, Setting, Plus, DataAnalysis, Download, HomeFilled, Clock, Files, Avatar, Money, WalletFilled, Coin } from '@element-plus/icons-vue'

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
    <div class="sidebar-logo">
      <div class="logo-icon">S</div>
      <span class="logo-text">SmartERP</span>
    </div>

    <el-menu
      :default-active="activeMenu"
      router
      background-color="transparent"
      text-color="rgba(255,255,255,0.65)"
      active-text-color="#ffffff"
      class="sidebar-menu"
    >
      <el-menu-item v-for="item in menuItems" :key="item.path" :index="item.path" class="menu-item">
        <el-icon><component :is="item.icon" /></el-icon>
        <span>{{ item.title }}</span>
      </el-menu-item>

      <el-sub-menu index="/expense-group" class="sub-menu">
        <template #title>
          <el-icon><Money /></el-icon>
          <span>经费报销</span>
        </template>
        <el-menu-item index="/expense/submit">
          <el-icon><Plus /></el-icon>
          <span>提交报销</span>
        </el-menu-item>
        <el-menu-item index="/expense/my">
          <el-icon><Files /></el-icon>
          <span>我的报销</span>
        </el-menu-item>
        <el-menu-item index="/expense/pending">
          <el-icon><Clock /></el-icon>
          <span>待审批经费</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu v-if="auth.isManager" index="/accounting-group" class="sub-menu">
        <template #title>
          <el-icon><Coin /></el-icon>
          <span>记账管理</span>
        </template>
        <el-menu-item index="/accounting/accounts">
          <el-icon><List /></el-icon>
          <span>会计科目</span>
        </el-menu-item>
        <el-menu-item index="/accounting/trial-balance">
          <el-icon><DataAnalysis /></el-icon>
          <span>试算平衡</span>
        </el-menu-item>
        <el-menu-item index="/accounting/cost-centers">
          <el-icon><Coin /></el-icon>
          <span>成本中心</span>
        </el-menu-item>
        <el-menu-item index="/accounting/profit-centers">
          <el-icon><Money /></el-icon>
          <span>利润中心</span>
        </el-menu-item>
        <el-menu-item index="/accounting/periods">
          <el-icon><Clock /></el-icon>
          <span>财务期间</span>
        </el-menu-item>
        <el-menu-item index="/accounting/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>财务看板</span>
        </el-menu-item>
      </el-sub-menu>

      <el-sub-menu v-if="auth.isManager" index="/templates-group" class="sub-menu">
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

      <el-menu-item v-if="auth.isManager" index="/all-requests" class="menu-item">
        <el-icon><List /></el-icon>
        <span>全部记录</span>
      </el-menu-item>

      <el-menu-item v-if="auth.isManager" index="/users" class="menu-item">
        <el-icon><Avatar /></el-icon>
        <span>用户列表</span>
      </el-menu-item>

      <el-menu-item v-if="auth.isManager" @click="handleExport" class="menu-item">
        <el-icon><Download /></el-icon>
        <span>导出Excel</span>
      </el-menu-item>
    </el-menu>

    <div class="sidebar-footer">
      <div class="version-badge">v2.0</div>
    </div>
  </div>
</template>

<style scoped>
.sidebar {
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  overflow-x: hidden;
}

.sidebar-logo {
  height: 64px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.logo-icon {
  width: 32px;
  height: 32px;
  border-radius: 8px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 16px;
  letter-spacing: -1px;
}

.logo-text {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
  letter-spacing: 1px;
}

.sidebar-menu {
  flex: 1;
  border-right: none !important;
  padding: 8px 0;
}

.menu-item {
  margin: 2px 12px;
  border-radius: 8px;
  height: 44px;
  line-height: 44px;
  transition: all 0.2s ease;
}

.menu-item:hover {
  background: rgba(255, 255, 255, 0.08) !important;
}

.menu-item.is-active {
  background: rgba(102, 126, 234, 0.2) !important;
  color: #fff !important;
  font-weight: 500;
}

.menu-item.is-active::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  background: linear-gradient(180deg, #667eea 0%, #764ba2 100%);
  border-radius: 0 3px 3px 0;
}

.sub-menu {
  margin: 2px 12px;
}

.sidebar-footer {
  padding: 16px;
  border-top: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;
}

.version-badge {
  text-align: center;
  color: rgba(255, 255, 255, 0.3);
  font-size: 11px;
  letter-spacing: 1px;
}
</style>
