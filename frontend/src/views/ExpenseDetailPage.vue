<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import StatusTag from '@/components/StatusTag.vue'
import { useAuthStore } from '@/stores/auth'
import { useExpenseStore } from '@/stores/expense'
import { useUserStore } from '@/stores/users'
import { withdrawExpense, reverseExpense } from '@/api/expense'
import { AUDIT_ACTION_MAP } from '@/utils/constants'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const store = useExpenseStore()
const userStore = useUserStore()

const comment = ref('')
const submitting = ref(false)
const reverseReason = ref('')
const showReverse = ref(false)

const id = computed(() => Number(route.params.id))
const detail = computed(() => store.currentDetail)
const applicant = computed(() => userStore.getUser(detail.value?.applicantId))

const canApprove = computed(() => {
  if (!detail.value) return false
  if (detail.value.status !== 'PENDING') return false
  if (detail.value.currentApproverId === auth.user?.id) return true
  return store.currentTasks.some(t => t.approverId === auth.user?.id)
})

const canWithdraw = computed(() => {
  if (!detail.value) return false
  return detail.value.status === 'PENDING' && detail.value.applicantId === auth.user?.id
})

const canReverse = computed(() => {
  if (!detail.value) return false
  return detail.value.status === 'POSTED' && auth.isManager
})

async function handleApprove(action) {
  submitting.value = true
  try {
    const { approveExpense } = await import('@/api/expense')
    await approveExpense(id.value, action, comment.value)
    ElMessage.success(action === 'APPROVE' ? '已通过' : '已驳回')
    router.push('/expense/pending')
  } catch {
  } finally {
    submitting.value = false
  }
}

async function handleWithdraw() {
  submitting.value = true
  try {
    await withdrawExpense(id.value)
    ElMessage.success('已撤回')
    await store.fetchDetail(id.value)
    await store.fetchAuditLogs(id.value)
  } catch {
  } finally {
    submitting.value = false
  }
}

async function handleReverse() {
  if (!reverseReason.value) {
    ElMessage.warning('请输入冲销原因')
    return
  }
  submitting.value = true
  try {
    await reverseExpense(id.value, reverseReason.value)
    ElMessage.success('冲销成功')
    showReverse.value = false
    await store.fetchDetail(id.value)
    await store.fetchAuditLogs(id.value)
  } catch {
  } finally {
    submitting.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(async () => {
  await userStore.fetchUsers()
  await Promise.all([
    store.fetchDetail(id.value),
    store.fetchAuditLogs(id.value),
    store.fetchTasks(id.value)
  ])
})
</script>

<template>
  <div class="page">
    <!-- 基本信息 -->
    <el-card shadow="never" class="mb-20">
      <template #header>
        <div class="card-header">
          <span>经费报销详情</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>
      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请人">{{ applicant?.realName || detail?.applicantId }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ applicant?.department || '' }}</el-descriptions-item>
        <el-descriptions-item label="费用类别">{{ detail?.category }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag v-if="detail" :status="detail.status" />
        </el-descriptions-item>
        <el-descriptions-item label="金额">
          <span class="amount">¥{{ detail?.amount }}</span>
        </el-descriptions-item>
        <el-descriptions-item label="关联分录">
          <el-tag v-if="detail?.transactionId" type="success" size="small">已入账</el-tag>
          <el-tag v-else type="info" size="small">未入账</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="费用说明" :span="2">{{ detail?.description }}</el-descriptions-item>
        <el-descriptions-item label="发票附件" :span="2">
          <a v-if="detail?.receiptUrl" :href="detail.receiptUrl" target="_blank">{{ detail.receiptUrl }}</a>
          <span v-else class="text-muted">无</span>
        </el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ formatDate(detail?.createTime) }}</el-descriptions-item>
        <el-descriptions-item label="更新时间">{{ formatDate(detail?.updateTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 审批操作 -->
    <el-card v-if="canApprove" shadow="never" class="mb-20">
      <template #header><span>审批操作</span></template>
      <el-input v-model="comment" type="textarea" :rows="3" placeholder="审批意见（可选）" maxlength="200" show-word-limit class="mb-16" />
      <div class="action-row">
        <span></span>
        <div class="approve-actions">
          <el-button type="success" :loading="submitting" @click="handleApprove('APPROVE')">通过</el-button>
          <el-button type="danger" :loading="submitting" @click="handleApprove('REJECT')">驳回</el-button>
        </div>
      </div>
    </el-card>

    <!-- 撤回 -->
    <el-card v-if="canWithdraw" shadow="never" class="mb-20">
      <div class="action-row">
        <span>申请正在审批中</span>
        <el-button type="warning" :loading="submitting" @click="handleWithdraw">撤回申请</el-button>
      </div>
    </el-card>

    <!-- 冲销（管理员） -->
    <el-card v-if="canReverse" shadow="never" class="mb-20">
      <div class="action-row">
        <span>已入账，可进行红字冲销</span>
        <el-button type="danger" :loading="submitting" @click="showReverse = true">冲销</el-button>
      </div>
    </el-card>

    <!-- 冲销弹窗 -->
    <el-dialog v-model="showReverse" title="红字冲销" width="400px">
      <el-input v-model="reverseReason" type="textarea" :rows="3" placeholder="请输入冲销原因" />
      <template #footer>
        <el-button @click="showReverse = false">取消</el-button>
        <el-button type="danger" :loading="submitting" @click="handleReverse">确认冲销</el-button>
      </template>
    </el-dialog>

    <!-- 审计日志 -->
    <el-card shadow="never">
      <template #header><span>操作日志</span></template>
      <el-timeline>
        <el-timeline-item
          v-for="log in store.currentAuditLogs"
          :key="log.id"
          :timestamp="formatDate(log.createTime)"
          :type="AUDIT_ACTION_MAP[log.action]?.type || 'info'"
        >
          <div class="log-item">
            <el-tag :type="AUDIT_ACTION_MAP[log.action]?.type || 'info'" size="small">
              {{ AUDIT_ACTION_MAP[log.action]?.label || log.action }}
            </el-tag>
            <span class="log-actor">{{ userStore.getUserName(log.actorId) }}</span>
            <span v-if="log.detail" class="log-detail">{{ log.detail }}</span>
          </div>
        </el-timeline-item>
      </el-timeline>
      <el-empty v-if="store.currentAuditLogs.length === 0" description="暂无操作记录" />
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 800px; margin: 0 auto; }
.mb-20 { margin-bottom: 20px; }
.mb-16 { margin-bottom: 16px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.action-row { display: flex; justify-content: space-between; align-items: center; }
.approve-actions { display: flex; gap: 12px; }
.amount { font-weight: 700; font-size: 18px; color: var(--primary-color); }
.text-muted { color: var(--text-muted); }
.log-item { display: flex; align-items: center; gap: 8px; }
.log-actor { font-weight: 500; }
.log-detail { color: var(--text-secondary); font-size: 13px; }
</style>
