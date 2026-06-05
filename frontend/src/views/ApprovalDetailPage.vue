<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Clock } from '@element-plus/icons-vue'
import StatusTag from '@/components/StatusTag.vue'
import ApprovalTimeline from '@/components/ApprovalTimeline.vue'
import { useAuthStore } from '@/stores/auth'
import { useApprovalStore } from '@/stores/approval'
import { useUserStore } from '@/stores/users'
import { getTemplateNodes } from '@/api/template'
import { transferLeave, getPendingTasks } from '@/api/leave'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const store = useApprovalStore()
const userStore = useUserStore()

const comment = ref('')
const submitting = ref(false)
const nodes = ref([])
const tasks = ref([])
const transferUserId = ref(null)
const showTransfer = ref(false)

const id = computed(() => Number(route.params.id))

const detail = computed(() => store.currentDetail)
const applicant = computed(() => userStore.getUser(detail.value?.applicantId))

const canApprove = computed(() => {
  if (!detail.value) return false
  if (detail.value.status !== 'PENDING') return false
  if (detail.value.currentApproverId === auth.user?.id) return true
  return tasks.value.some(t => t.approverId === auth.user?.id)
})

const pendingApproverNames = computed(() => {
  return tasks.value.map(t => userStore.getUser(t.approverId)?.realName || `用户${t.approverId}`)
})

const canWithdraw = computed(() => {
  if (!detail.value) return false
  return detail.value.status === 'PENDING' && detail.value.applicantId === auth.user?.id
})

const canTransfer = computed(() => {
  return canApprove.value
})

const timeoutWarning = computed(() => {
  if (!detail.value || !detail.value.timeoutTime) return null
  if (detail.value.status !== 'PENDING') return null
  return formatDate(detail.value.timeoutTime)
})

const activeStep = computed(() => {
  if (!detail.value) return 0
  if (detail.value.status === 'REJECTED' || detail.value.status === 'WITHDRAWN') return detail.value.approvalStep
  if (detail.value.status === 'APPROVED') return nodes.value.length
  const idx = nodes.value.findIndex(n => n.id === detail.value.currentNodeId)
  return idx >= 0 ? idx : 0
})

const stepStatus = (idx) => {
  if (!detail.value || nodes.value.length === 0) return 'wait'
  const currentIdx = nodes.value.findIndex(n => n.id === detail.value.currentNodeId)
  if (detail.value.status === 'REJECTED') {
    if (idx < detail.value.approvalStep) return 'finish'
    if (idx === detail.value.approvalStep) return 'error'
    return 'wait'
  }
  if (detail.value.status === 'WITHDRAWN') {
    return idx < detail.value.approvalStep ? 'finish' : 'wait'
  }
  if (detail.value.status === 'APPROVED') return 'finish'
  // PENDING
  if (currentIdx >= 0) {
    if (idx < currentIdx) return 'finish'
    if (idx === currentIdx) return 'process'
  }
  return 'wait'
}

async function handleApprove(action) {
  submitting.value = true
  try {
    await store.approve(id.value, action, comment.value)
    ElMessage.success(action === 'APPROVE' ? '已通过' : '已驳回')
    router.push('/my-approvals')
  } catch {
    // handled by interceptor
  } finally {
    submitting.value = false
  }
}

async function handleWithdraw() {
  submitting.value = true
  try {
    const { withdrawLeave } = await import('@/api/leave')
    await withdrawLeave(id.value)
    ElMessage.success('已撤回')
    await store.fetchDetail(id.value)
  } catch {
  } finally {
    submitting.value = false
  }
}

async function handleTransfer() {
  if (!transferUserId.value) {
    ElMessage.warning('请选择转派目标')
    return
  }
  submitting.value = true
  try {
    await transferLeave(id.value, transferUserId.value)
    ElMessage.success('转派成功')
    showTransfer.value = false
    await store.fetchDetail(id.value)
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
  await Promise.all([store.fetchDetail(id.value), store.fetchRecords(id.value)])
  tasks.value = await getPendingTasks(id.value).catch(() => [])
  if (store.currentDetail?.templateId) {
    nodes.value = await getTemplateNodes(store.currentDetail.templateId)
  }
})
</script>

<template>
  <div class="page">
    <el-card shadow="never" class="mb-20">
      <template #header>
        <div class="card-header">
          <span>请假单详情</span>
          <el-button @click="router.back()">返回</el-button>
        </div>
      </template>

      <!-- 审批进度 -- 动态节点 -->
      <el-steps v-if="nodes.length > 0" :active="activeStep" finish-status="success" align-center class="mb-20">
        <el-step
          v-for="(node, idx) in nodes"
          :key="node.id"
          :title="node.nodeName"
          :status="stepStatus(idx)"
        />
        <el-step title="完成" :status="detail?.status === 'APPROVED' ? 'finish' : 'wait'" />
      </el-steps>

      <el-descriptions :column="2" border>
        <el-descriptions-item label="申请人">{{ applicant?.realName || detail?.applicantId }}</el-descriptions-item>
        <el-descriptions-item label="部门">{{ applicant?.department || '' }}</el-descriptions-item>
        <el-descriptions-item label="请假类型">{{ detail?.leaveType }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <StatusTag v-if="detail" :status="detail.status" />
        </el-descriptions-item>
        <el-descriptions-item label="开始日期">{{ detail?.startDate }}</el-descriptions-item>
        <el-descriptions-item label="结束日期">{{ detail?.endDate }}</el-descriptions-item>
        <el-descriptions-item label="请假原因" :span="2">{{ detail?.reason }}</el-descriptions-item>
        <el-descriptions-item label="提交时间" :span="2">{{ formatDate(detail?.createTime) }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <!-- 并行审批待办提示 -->
    <el-card v-if="pendingApproverNames.length > 0" shadow="never" class="mb-20">
      <template #header><span>当前审批人</span></template>
      <div class="approver-tags">
        <el-tag v-for="name in pendingApproverNames" :key="name" type="warning" class="mr-8">{{ name }}</el-tag>
      </div>
    </el-card>

    <!-- 超时提醒 -->
    <el-card v-if="timeoutWarning" shadow="never" class="mb-20 timeout-card">
      <div class="timeout-warning">
        <el-icon :size="18"><Clock /></el-icon>
        <span>当前节点将于 <strong>{{ timeoutWarning }}</strong> 超时，超时后将自动处理</span>
      </div>
    </el-card>

    <!-- 审批操作 -->
    <el-card v-if="canApprove" shadow="never" class="mb-20">
      <template #header><span>审批操作</span></template>
      <el-input
        v-model="comment"
        type="textarea"
        :rows="3"
        placeholder="请输入审批意见（可选）"
        maxlength="200"
        show-word-limit
        class="mb-16"
      />
      <div class="action-row">
        <div>
          <el-button type="warning" @click="showTransfer = true">转派</el-button>
        </div>
        <div class="approve-actions">
          <el-button type="success" :loading="submitting" @click="handleApprove('APPROVE')">通过</el-button>
          <el-button type="danger" :loading="submitting" @click="handleApprove('REJECT')">驳回</el-button>
        </div>
      </div>
    </el-card>

    <!-- 转派弹窗 -->
    <el-dialog v-model="showTransfer" title="转派审批" width="400px">
      <el-select v-model="transferUserId" placeholder="请选择转派目标" class="w-full">
        <el-option
          v-for="u in userStore.users"
          :key="u.id"
          :label="`${u.realName} (${u.department || ''})`"
          :value="u.id"
        />
      </el-select>
      <template #footer>
        <el-button @click="showTransfer = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleTransfer">确定转派</el-button>
      </template>
    </el-dialog>

    <!-- 撤回按钮 -->
    <el-card v-if="canWithdraw" shadow="never" class="mb-20">
      <div class="action-row">
        <span>该申请正在审批中，你可以撤回</span>
        <el-button type="warning" :loading="submitting" @click="handleWithdraw">撤回申请</el-button>
      </div>
    </el-card>

    <!-- 审批记录 -->
    <el-card shadow="never">
      <template #header><span>审批记录</span></template>
      <ApprovalTimeline :records="store.currentRecords" :nodes="nodes" />
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 800px; margin: 0 auto; }
.mb-20 { margin-bottom: 20px; }
.mb-16 { margin-bottom: 16px; }
.w-full { width: 100%; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.action-row { display: flex; justify-content: space-between; align-items: center; }
.approve-actions { display: flex; gap: 12px; }
.approver-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.timeout-card { border-left: 4px solid #E6A23C; }
.timeout-warning { display: flex; align-items: center; gap: 8px; color: #E6A23C; }
</style>
