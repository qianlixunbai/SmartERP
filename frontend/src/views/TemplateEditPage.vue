<script setup>
import { reactive, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTemplateById, createTemplate, updateTemplate, getTemplateNodes, saveTemplateNodes } from '@/api/template'
import { useUserStore } from '@/stores/users'
import { APPROVER_TYPES, SIGN_TYPES, TIMEOUT_ACTIONS } from '@/utils/constants'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const isEdit = ref(false)
const saving = ref(false)
const formRef = ref(null)

const form = reactive({
  name: '',
  description: '',
  enabled: true
})

const nodes = ref([])
const dragIndex = ref(null)

const rules = {
  name: [{ required: true, message: '请输入模板名称', trigger: 'blur' }]
}

const approverColors = {
  DIRECT_LEADER: '#409EFF',
  DEPARTMENT_HEAD: '#A855F7',
  SPECIFIC_USER: '#67C23A'
}

const approverLabels = {
  DIRECT_LEADER: '直属领导',
  DEPARTMENT_HEAD: '部门总监',
  SPECIFIC_USER: '指定用户'
}

onMounted(async () => {
  await userStore.fetchUsers()
  const id = route.params.id
  if (id) {
    isEdit.value = true
    const data = await getTemplateById(Number(id))
    form.name = data.name
    form.description = data.description
    form.enabled = data.enabled
    const serverNodes = await getTemplateNodes(Number(id))
    nodes.value = serverNodes.map((n, i) => ({
      ...n,
      _uid: ++i,
      _showCondition: !!n.conditionExpression,
      _showTimeout: !!(n.timeoutHours && n.timeoutHours > 0),
      _approverIdsArr: n.approverIds ? n.approverIds.split(',').map(Number) : []
    }))
    uidCounter = nodes.value.length
  }
})

let uidCounter = 0

function addNode() {
  nodes.value.push({
    _uid: ++uidCounter,
    nodeName: '新节点',
    sortOrder: nodes.value.length,
    approverType: 'DIRECT_LEADER',
    approverId: null,
    signType: 'SINGLE',
    approverIds: null,
    conditionExpression: null,
    timeoutHours: null,
    timeoutAction: 'ESCALATE',
    escalateToUserId: null,
    _showCondition: false,
    _showTimeout: false,
    _approverIdsArr: []
  })
}

function removeNode(index) {
  nodes.value.splice(index, 1)
}

// -- drag & drop --
function onDragStart(index) {
  dragIndex.value = index
}

function onDragOver(e, index) {
  e.preventDefault()
  if (dragIndex.value === null || dragIndex.value === index) return
  const items = [...nodes.value]
  const dragged = items.splice(dragIndex.value, 1)[0]
  items.splice(index, 0, dragged)
  nodes.value = items
  dragIndex.value = index
}

function onDragEnd() {
  dragIndex.value = null
}

async function handleSave() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const data = { ...form }
    if (isEdit.value) {
      await updateTemplate(Number(route.params.id), data)
      // re-assign sortOrder before saving
      const ordered = nodes.value.map(({ _uid, _showCondition, _showTimeout, _approverIdsArr, ...n }, i) => ({
        ...n,
        sortOrder: i,
        approverIds: Array.isArray(_approverIdsArr) && _approverIdsArr.length > 0 ? _approverIdsArr.join(',') : null,
        timeoutHours: n.timeoutHours && n.timeoutHours > 0 ? n.timeoutHours : null
      }))
      await saveTemplateNodes(Number(route.params.id), ordered)
      ElMessage.success('更新成功')
    } else {
      await createTemplate(data)
      ElMessage.success('创建成功，请编辑模板来配置审批节点')
    }
    router.push('/templates')
  } catch {
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <div class="template-editor">
    <!-- 基本信息 -->
    <el-card shadow="never" class="mb-20">
      <template #header>
        <div class="card-header">
          <span>{{ isEdit ? '编辑模板' : '新建模板' }}</span>
          <el-button @click="router.push('/templates')">返回列表</el-button>
        </div>
      </template>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px" class="form">
        <el-form-item label="模板名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入模板名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入模板描述" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.enabled" active-text="启用" inactive-text="禁用" />
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 审批流程可视化编辑器 -->
    <el-card v-if="isEdit" shadow="never" class="mb-20">
      <template #header>
        <div class="card-header">
          <span>审批流程（{{ nodes.length }} 级）</span>
          <span class="header-hint">拖拽左侧手柄可调整顺序</span>
        </div>
      </template>

      <el-empty v-if="nodes.length === 0" description="暂无审批节点，请点击下方按钮添加" />

      <div v-else class="flow-container">
        <TransitionGroup name="flow">
          <div
            v-for="(node, idx) in nodes"
            :key="node._uid"
            class="flow-node-wrapper"
            :class="{ 'is-last': idx === nodes.length - 1 }"
          >
            <!-- 节点卡片 -->
            <div
              class="flow-card"
              :style="{ borderLeftColor: approverColors[node.approverType] }"
              @dragover="onDragOver($event, idx)"
            >
              <!-- 拖拽手柄 -->
              <div
                class="drag-handle"
                draggable="true"
                @dragstart="onDragStart(idx)"
                @dragend="onDragEnd"
                title="拖拽排序"
              >
                <span class="drag-dots">::</span>
              </div>

              <!-- 步骤序号 -->
              <div
                class="step-badge"
                :style="{ background: approverColors[node.approverType] }"
              >
                {{ idx + 1 }}
              </div>

              <!-- 节点内容 -->
              <div class="node-body">
                <div class="node-row">
                  <el-input
                    v-model="node.nodeName"
                    placeholder="节点名称"
                    class="node-name-input"
                  />
                  <el-select
                    v-model="node.signType"
                    class="sign-type-select"
                  >
                    <el-option
                      v-for="st in SIGN_TYPES"
                      :key="st.value"
                      :label="st.label"
                      :value="st.value"
                    />
                  </el-select>
                  <template v-if="node.signType === 'SINGLE'">
                    <el-select
                      v-model="node.approverType"
                      class="approver-type-select"
                    >
                      <el-option
                        v-for="at in APPROVER_TYPES"
                        :key="at.value"
                        :label="at.label"
                        :value="at.value"
                      />
                    </el-select>
                    <el-select
                      v-if="node.approverType === 'SPECIFIC_USER'"
                      v-model="node.approverId"
                      placeholder="选择用户"
                      class="user-select"
                    >
                      <el-option
                        v-for="u in userStore.users"
                        :key="u.id"
                        :label="u.realName"
                        :value="u.id"
                      />
                    </el-select>
                    <span class="approver-label" :style="{ color: approverColors[node.approverType] }">
                      <span class="type-dot" :style="{ background: approverColors[node.approverType] }"></span>
                      {{ approverLabels[node.approverType] }}
                    </span>
                  </template>
                  <el-select
                    v-else
                    v-model="node._approverIdsArr"
                    multiple
                    placeholder="选择审批人"
                    class="user-select-multi"
                  >
                    <el-option
                      v-for="u in userStore.users"
                      :key="u.id"
                      :label="u.realName"
                      :value="u.id"
                    />
                  </el-select>
                </div>
              </div>

              <!-- 条件表达式 -->
              <div class="condition-row">
                <el-button
                  v-if="!node._showCondition"
                  link
                  type="info"
                  size="small"
                  @click="node._showCondition = true"
                >
                  + 条件
                </el-button>
                <div v-else class="condition-input-area">
                  <el-input
                    v-model="node.conditionExpression"
                    placeholder="如: days > 3 或 leaveType == '病假'"
                    size="small"
                    class="condition-input"
                    clearable
                  />
                  <el-button
                    link
                    type="info"
                    size="small"
                    @click="node._showCondition = false; node.conditionExpression = null"
                  >
                    移除
                  </el-button>
                </div>
              </div>

              <!-- 超时设置 -->
              <div class="condition-row">
                <el-button
                  v-if="!node._showTimeout"
                  link
                  type="warning"
                  size="small"
                  @click="node._showTimeout = true"
                >
                  + 超时
                </el-button>
                <div v-else class="timeout-area">
                  <div class="timeout-row">
                    <el-input-number
                      v-model="node.timeoutHours"
                      :min="1"
                      :max="720"
                      placeholder="小时"
                      size="small"
                      class="timeout-hours-input"
                    />
                    <span class="timeout-label">小时未处理则</span>
                    <el-select
                      v-model="node.timeoutAction"
                      size="small"
                      class="timeout-action-select"
                    >
                      <el-option
                        v-for="ta in TIMEOUT_ACTIONS"
                        :key="ta.value"
                        :label="ta.label"
                        :value="ta.value"
                      />
                    </el-select>
                    <el-select
                      v-if="node.timeoutAction === 'ESCALATE'"
                      v-model="node.escalateToUserId"
                      placeholder="选择转派目标"
                      size="small"
                      class="timeout-user-select"
                      clearable
                    >
                      <el-option
                        v-for="u in userStore.users"
                        :key="u.id"
                        :label="u.realName"
                        :value="u.id"
                      />
                    </el-select>
                    <el-button
                      link
                      type="info"
                      size="small"
                      @click="node._showTimeout = false; node.timeoutHours = null"
                    >
                      移除
                    </el-button>
                  </div>
                </div>
              </div>

              <!-- 删除按钮 -->
              <el-button
                class="delete-btn"
                type="danger"
                circle
                size="small"
                @click="removeNode(idx)"
              >
                <el-icon><Delete /></el-icon>
              </el-button>
            </div>

            <!-- 连接线 -->
            <div v-if="idx < nodes.length - 1" class="flow-connector">
              <div class="connector-line"></div>
              <div class="connector-arrow"></div>
            </div>
          </div>
        </TransitionGroup>

        <!-- 添加节点按钮 -->
        <div class="add-node-area">
          <div class="connector-line is-dimmed"></div>
          <el-button
            type="primary"
            @click="addNode"
            plain
            round
          >
            <el-icon style="margin-right:6px"><Plus /></el-icon>
            添加审批节点
          </el-button>
        </div>
      </div>
    </el-card>

    <!-- 保存/取消 -->
    <el-card shadow="never">
      <el-button type="primary" :loading="saving" @click="handleSave">保存</el-button>
      <el-button @click="router.push('/templates')">取消</el-button>
    </el-card>
  </div>
</template>

<style scoped>
.template-editor { max-width: 800px; margin: 0 auto; }
.mb-20 { margin-bottom: 20px; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
.header-hint { font-size: 13px; color: #909399; }
.form { margin-top: 10px; }

/* -- 流程图容器 -- */
.flow-container {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.flow-node-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  max-width: 680px;
  transition: all 0.3s ease;
}

/* -- 节点卡片 -- */
.flow-card {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  padding: 14px 16px;
  background: #fff;
  border: 1px solid #e4e7ed;
  border-left: 4px solid #409EFF;
  border-radius: 10px;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  transition: transform 0.2s, box-shadow 0.2s, opacity 0.2s;
  cursor: default;
}

.flow-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.1);
  transform: translateY(-1px);
}

.flow-card:active {
  cursor: default;
}

/* -- 拖拽手柄 -- */
.drag-handle {
  flex-shrink: 0;
  cursor: grab;
  padding: 4px 6px;
  border-radius: 4px;
  color: #c0c4cc;
  font-size: 18px;
  letter-spacing: 2px;
  user-select: none;
  transition: color 0.2s, background 0.2s;
  line-height: 1;
}

.drag-handle:hover {
  color: #409EFF;
  background: #ecf5ff;
}

.drag-handle:active {
  cursor: grabbing;
}

/* -- 步骤序号 badge -- */
.step-badge {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 700;
  font-size: 15px;
  box-shadow: 0 2px 6px rgba(0,0,0,0.15);
}

/* -- 节点内容 -- */
.node-body {
  flex: 1;
  min-width: 0;
}

.node-row {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.node-name-input {
  width: 160px;
}

.approver-type-select {
  width: 130px;
}

.sign-type-select {
  width: 170px;
}

.user-select {
  width: 150px;
}

.user-select-multi {
  width: 250px;
}

.approver-label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  white-space: nowrap;
}

.type-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  flex-shrink: 0;
}

/* -- 条件表达式 -- */
.condition-row {
  margin-top: 6px;
}

.condition-input-area {
  display: flex;
  align-items: center;
  gap: 6px;
}

.condition-input {
  width: 280px;
}

/* -- 超时设置 -- */
.timeout-area {
  padding: 8px 0 0 0;
}

.timeout-row {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-wrap: wrap;
}

.timeout-hours-input {
  width: 100px;
}

.timeout-label {
  font-size: 13px;
  color: #909399;
  white-space: nowrap;
}

.timeout-action-select {
  width: 140px;
}

.timeout-user-select {
  width: 160px;
}

/* -- 删除按钮 -- */
.delete-btn {
  flex-shrink: 0;
  opacity: 0;
  transition: opacity 0.2s;
}

.flow-card:hover .delete-btn {
  opacity: 1;
}

/* -- 连接线 -- */
.flow-connector {
  display: flex;
  flex-direction: column;
  align-items: center;
  height: 36px;
}

.connector-line {
  width: 2px;
  flex: 1;
  background: #c0c4cc;
}

.connector-line.is-dimmed {
  background: #dcdfe6;
  height: 20px;
  flex: none;
}

.connector-arrow {
  width: 0;
  height: 0;
  border-left: 6px solid transparent;
  border-right: 6px solid transparent;
  border-top: 8px solid #c0c4cc;
}

/* -- 添加节点区域 -- */
.add-node-area {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
}

.add-node-area .el-button {
  margin-top: 4px;
}

/* -- 过渡动画 -- */
.flow-enter-active,
.flow-leave-active {
  transition: all 0.4s ease;
}

.flow-enter-from {
  opacity: 0;
  transform: translateY(-20px);
}

.flow-leave-to {
  opacity: 0;
  transform: translateX(40px);
}

.flow-leave-active {
  position: absolute;
}

.flow-move {
  transition: transform 0.3s ease;
}
</style>
