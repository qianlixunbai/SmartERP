<script setup>
import { computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { useApprovalStore } from '@/stores/approval'
import { useUserStore } from '@/stores/users'
import { DocumentAdd, Files, Clock, List } from '@element-plus/icons-vue'

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
    <el-card shadow="never" class="welcome-card">
      <div class="welcome-text">
        <h2>欢迎回来，{{ auth.user?.realName }}</h2>
        <p class="date-text">{{ today }}</p>
      </div>
    </el-card>

    <el-row :gutter="20" class="stats-row">
      <el-col :span="6">
        <div class="stat-card" @click="go('/my-requests')">
          <div class="stat-number" style="color: #409eff">{{ stats.total }}</div>
          <div class="stat-label">我的申请</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card" @click="go('/my-requests')">
          <div class="stat-number" style="color: #e6a23c">{{ stats.pending }}</div>
          <div class="stat-label">审批中</div>
        </div>
      </el-col>
      <el-col :span="6">
        <div class="stat-card">
          <div class="stat-number" style="color: #67c23a">{{ stats.approved }}</div>
          <div class="stat-label">已通过</div>
        </div>
      </el-col>
      <el-col v-if="auth.isManager" :span="6">
        <div class="stat-card" @click="go('/pending-approvals')">
          <div class="stat-number" style="color: #f56c6c">{{ stats.pendingApproval }}</div>
          <div class="stat-label">待我审批</div>
        </div>
      </el-col>
    </el-row>

    <el-card shadow="never" header="快捷操作">
      <el-row :gutter="12">
        <el-col :span="4">
          <el-button type="primary" :icon="DocumentAdd" size="large" class="action-btn" @click="go('/submit-application')">
            提交请假申请
          </el-button>
        </el-col>
        <el-col :span="4">
          <el-button :icon="Files" size="large" class="action-btn" @click="go('/my-requests')">
            我的申请
          </el-button>
        </el-col>
        <el-col :span="4">
          <el-button type="warning" :icon="Clock" size="large" class="action-btn" @click="go('/pending-approvals')">
            待我审批
          </el-button>
        </el-col>
        <el-col v-if="auth.isManager" :span="4">
          <el-button :icon="List" size="large" class="action-btn" @click="go('/all-requests')">
            全部记录
          </el-button>
        </el-col>
      </el-row>
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 1200px; margin: 0 auto; }
.welcome-card { margin-bottom: 20px; }
.welcome-text h2 { margin: 0 0 4px 0; font-size: 22px; }
.date-text { margin: 0; color: #909399; font-size: 14px; }
.stats-row { margin-bottom: 20px; }
.stat-card {
  text-align: center;
  padding: 24px 16px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  cursor: pointer;
  transition: transform .2s, box-shadow .2s;
}
.stat-card:hover { transform: translateY(-2px); box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.stat-number { font-size: 32px; font-weight: 700; margin-bottom: 8px; }
.stat-label { font-size: 14px; color: #606266; }
.action-btn { width: 100%; }
</style>
