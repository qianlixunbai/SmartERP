<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/users'
import { getTemplateById, getTemplateNodes, getTemplateFields } from '@/api/template'
import { APPROVER_TYPES, SIGN_TYPES, TIMEOUT_ACTIONS } from '@/utils/constants'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const template = ref(null)
const nodes = ref([])
const fields = ref([])
const loading = ref(true)

const approverColors = {
  DIRECT_LEADER: '#409eff',
  DEPARTMENT_HEAD: '#e6a23c',
  SPECIFIC_USER: '#67c23a'
}

const approverLabels = {
  DIRECT_LEADER: '直属领导',
  DEPARTMENT_HEAD: '部门总监'
}

function getSignLabel(type) {
  return SIGN_TYPES.find(s => s.value === type)?.label || type
}

function getTimeoutLabel(node) {
  if (!node.timeoutHours) return null
  const action = TIMEOUT_ACTIONS.find(a => a.value === node.timeoutAction)
  let label = `${node.timeoutHours}小时后${action?.label || node.timeoutAction}`
  if (node.timeoutAction === 'ESCALATE' && node.escalateToUserId) {
    label += ` → ${userStore.getUserName(node.escalateToUserId)}`
  }
  return label
}

onMounted(async () => {
  try {
    const id = Number(route.params.id)
    await userStore.fetchUsers()
    const [t, n, f] = await Promise.all([
      getTemplateById(id),
      getTemplateNodes(id),
      getTemplateFields(id)
    ])
    template.value = t
    nodes.value = n
    fields.value = f
  } catch {
    ElMessage.error('加载模板失败')
    router.back()
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div class="page">
    <el-button style="margin-bottom: 16px" @click="router.back()">← 返回</el-button>

    <el-card shadow="never" v-loading="loading" header="模板信息" class="mb-20">
      <el-descriptions v-if="template" :column="2" border>
        <el-descriptions-item label="模板名称">{{ template.name }}</el-descriptions-item>
        <el-descriptions-item label="ID">{{ template.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="template.enabled ? 'success' : 'info'" size="small">
            {{ template.enabled ? '启用' : '禁用' }}
          </el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="描述">{{ template.description || '—' }}</el-descriptions-item>
      </el-descriptions>
    </el-card>

    <el-card v-if="nodes.length" shadow="never" header="审批流程" class="mb-20">
      <div class="flow-container">
        <div v-for="(node, idx) in nodes" :key="node.id || idx" class="flow-node-wrapper">
          <div class="flow-card" :style="{ borderLeftColor: approverColors[node.approverType] }">
            <div class="step-badge" :style="{ background: approverColors[node.approverType] }">
              {{ idx + 1 }}
            </div>
            <div class="node-body">
              <div class="node-row">
                <strong>{{ node.nodeName }}</strong>
                <el-tag size="small" type="info">{{ getSignLabel(node.signType) }}</el-tag>
                <span class="approver-label">
                  {{ approverLabels[node.approverType] || userStore.getUserName(node.approverId) }}
                </span>
                <span v-if="node.signType !== 'SINGLE' && node.approverIds" class="approver-label">
                  ({{ node.approverIds.split(',').map(id => userStore.getUserName(Number(id))).join('、') }})
                </span>
              </div>
              <div v-if="node.conditionExpression" class="info-line">
                📋 条件：{{ node.conditionExpression }}
              </div>
              <div v-if="getTimeoutLabel(node)" class="info-line">
                ⏱ {{ getTimeoutLabel(node) }}
              </div>
            </div>
          </div>
          <div v-if="idx < nodes.length - 1" class="flow-connector">
            <div class="connector-line" />
            <div class="connector-arrow">▼</div>
          </div>
        </div>
      </div>
    </el-card>
    <el-card v-else shadow="never" header="审批流程" class="mb-20">
      <el-empty description="暂无审批节点" />
    </el-card>

    <el-card v-if="fields.length" shadow="never" header="模板字段">
      <el-table :data="fields" stripe>
        <el-table-column prop="fieldLabel" label="字段标签" width="150" />
        <el-table-column prop="fieldName" label="字段名" width="150" />
        <el-table-column prop="fieldType" label="类型" width="100" />
        <el-table-column prop="sortOrder" label="排序" width="80" />
        <el-table-column prop="required" label="必填" width="80">
          <template #default="{ row }">
            <el-tag :type="row.required ? 'danger' : 'info'" size="small">{{ row.required ? '是' : '否' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 800px; margin: 0 auto; }
.mb-20 { margin-bottom: 20px; }
.flow-container { padding: 10px 0; }
.flow-node-wrapper { position: relative; }
.flow-card {
  display: flex;
  align-items: flex-start;
  padding: 16px 20px;
  background: #fafafa;
  border: 1px solid #e4e7ed;
  border-left: 4px solid #409eff;
  border-radius: 6px;
  gap: 14px;
}
.step-badge {
  width: 28px; height: 28px;
  border-radius: 50%;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
  flex-shrink: 0;
}
.node-body { flex: 1; }
.node-row { display: flex; align-items: center; gap: 8px; }
.approver-label { color: #909399; font-size: 13px; }
.info-line { margin-top: 6px; font-size: 13px; color: #606266; }
.flow-connector {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 4px 0 4px 32px;
}
.connector-line { width: 2px; height: 20px; background: #c0c4cc; }
.connector-arrow { color: #c0c4cc; font-size: 10px; line-height: 1; }
</style>
