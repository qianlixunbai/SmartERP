<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useApprovalStore } from '@/stores/approval'
import { useUserStore } from '@/stores/users'
import { DocumentAdd, Files, Clock, List, SuccessFilled, Bell } from '@element-plus/icons-vue'

const router = useRouter()
const auth = useAuthStore()
const store = useApprovalStore()
const userStore = useUserStore()

const today = new Date().toLocaleDateString('zh-CN', { year: 'numeric', month: 'long', day: 'numeric', weekday: 'long' })

const stats = computed(() => {
  const myRequests = store.myRequests
  return {
    total: myRequests.length,
    pending: myRequests.filter(r => r.status === 'PENDING').length,
    approved: myRequests.filter(r => r.status === 'APPROVED').length,
    rejected: myRequests.filter(r => r.status === 'REJECTED').length,
    pendingApproval: auth.isManager ? store.pendingRequests.length : 0
  }
})

function go(path) {
  router.push(path)
}

onMounted(async () => {
  await userStore.fetchUsers()
  try { await store.fetchMyRequests() } catch { /* ok */ }
  if (auth.isManager) {
    try { await store.fetchPendingRequests() } catch { /* ok */ }
  }
})
</script>

<template>
  <div class="page">
    <!-- 欢迎卡片 -->
    <div class="welcome-card">
      <div class="welcome-content">
        <div class="welcome-text">
          <h2>欢迎回来，{{ auth.user?.realName }}</h2>
          <p class="date-text">{{ today }}</p>
        </div>
        <div class="welcome-stats">
          <div class="mini-stat">
            <span class="mini-number">{{ stats.total }}</span>
            <span class="mini-label">我的申请</span>
          </div>
          <div class="mini-stat" v-if="auth.isManager && stats.pendingApproval > 0">
            <span class="mini-number pending">{{ stats.pendingApproval }}</span>
            <span class="mini-label">待审批</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 统计卡片 -->
    <div class="stats-grid">
      <div class="stat-card" @click="go('/my-requests')">
        <div class="stat-icon blue">
          <el-icon><Files /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ stats.total }}</span>
          <span class="stat-label">我的申请</span>
        </div>
      </div>

      <div class="stat-card" @click="go('/my-requests')">
        <div class="stat-icon orange">
          <el-icon><Clock /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ stats.pending }}</span>
          <span class="stat-label">审批中</span>
        </div>
      </div>

      <div class="stat-card">
        <div class="stat-icon green">
          <el-icon><SuccessFilled /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ stats.approved }}</span>
          <span class="stat-label">已通过</span>
        </div>
      </div>

      <div v-if="auth.isManager" class="stat-card" @click="go('/pending-approvals')">
        <div class="stat-icon red">
          <el-icon><Bell /></el-icon>
        </div>
        <div class="stat-info">
          <span class="stat-number">{{ stats.pendingApproval }}</span>
          <span class="stat-label">待我审批</span>
        </div>
      </div>
    </div>

    <!-- 快捷操作 -->
    <el-card shadow="never" class="action-card">
      <template #header>
        <span class="card-title">快捷操作</span>
      </template>
      <div class="action-grid">
        <el-button type="primary" :icon="DocumentAdd" size="large" class="action-btn" @click="go('/submit-application')">
          提交请假申请
        </el-button>
        <el-button :icon="Files" size="large" class="action-btn" @click="go('/my-requests')">
          我的申请
        </el-button>
        <el-button type="warning" :icon="Clock" size="large" class="action-btn" @click="go('/pending-approvals')">
          待我审批
        </el-button>
        <el-button v-if="auth.isManager" :icon="List" size="large" class="action-btn" @click="go('/all-requests')">
          全部记录
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.page {
  max-width: 1200px;
  margin: 0 auto;
}

/* 欢迎卡片 */
.welcome-card {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 16px;
  padding: 28px 32px;
  margin-bottom: 24px;
  color: #fff;
  box-shadow: 0 10px 40px -10px rgba(102, 126, 234, 0.3);
}

.welcome-content {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.welcome-text h2 {
  margin: 0 0 6px 0;
  font-size: 24px;
  font-weight: 600;
  color: #fff;
}

.date-text {
  margin: 0;
  color: rgba(255, 255, 255, 0.8);
  font-size: 14px;
}

.welcome-stats {
  display: flex;
  gap: 24px;
}

.mini-stat {
  text-align: center;
}

.mini-number {
  display: block;
  font-size: 28px;
  font-weight: 700;
  color: #fff;
}

.mini-number.pending {
  color: #fbbf24;
}

.mini-label {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

/* 统计卡片网格 */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
  gap: 16px;
  margin-bottom: 24px;
}

.stat-card {
  background: #fff;
  border-radius: 12px;
  padding: 20px;
  display: flex;
  align-items: center;
  gap: 16px;
  cursor: pointer;
  transition: all 0.25s ease;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  border: 1px solid rgba(0, 0, 0, 0.04);
}

.stat-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 10px 40px -10px rgba(102, 126, 234, 0.15);
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}

.stat-icon.blue {
  background: rgba(102, 126, 234, 0.1);
  color: #667eea;
}

.stat-icon.orange {
  background: rgba(251, 191, 36, 0.1);
  color: #f59e0b;
}

.stat-icon.green {
  background: rgba(52, 211, 153, 0.1);
  color: #10b981;
}

.stat-icon.red {
  background: rgba(248, 113, 113, 0.1);
  color: #ef4444;
}

.stat-info {
  display: flex;
  flex-direction: column;
}

.stat-number {
  font-size: 28px;
  font-weight: 700;
  color: #1e293b;
  line-height: 1.2;
}

.stat-label {
  font-size: 13px;
  color: #94a3b8;
  margin-top: 2px;
}

/* 快捷操作 */
.action-card {
  border-radius: 12px;
}

.card-title {
  font-weight: 600;
  color: #1e293b;
}

.action-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.action-btn {
  flex: 1;
  min-width: 140px;
  height: 48px;
  border-radius: 10px;
  font-weight: 500;
}

.action-btn:not(.el-button--primary):not(.el-button--warning) {
  border-color: #e2e8f0;
  color: #475569;
}

.action-btn:not(.el-button--primary):not(.el-button--warning):hover {
  border-color: #667eea;
  color: #667eea;
  background: rgba(102, 126, 234, 0.04);
}
</style>
