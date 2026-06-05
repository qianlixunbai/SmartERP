<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { LEAVE_TYPES } from '@/utils/constants'
import { getTemplateList } from '@/api/template'

const emit = defineEmits(['submit'])
const formRef = ref(null)
const submitting = ref(false)
const templates = ref([])

const form = reactive({
  templateId: null,
  leaveType: '年假',
  startDate: '',
  endDate: '',
  reason: ''
})

const rules = {
  templateId: [{ required: true, message: '请选择审批模板', trigger: 'change' }],
  leaveType: [{ required: true, message: '请选择请假类型', trigger: 'change' }],
  startDate: [{ required: true, message: '请选择开始日期', trigger: 'change' }],
  endDate: [{ required: true, message: '请选择结束日期', trigger: 'change' }],
  reason: [{ required: true, message: '请填写请假原因', trigger: 'blur' }]
}

onMounted(async () => {
  templates.value = await getTemplateList()
  if (templates.value.length > 0) {
    form.templateId = templates.value[0].id
  }
})

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  if (form.endDate < form.startDate) {
    ElMessage.warning('结束日期不能早于开始日期')
    return
  }
  submitting.value = true
  try {
    emit('submit', { ...form })
    formRef.value.resetFields()
    form.leaveType = '年假'
    if (templates.value.length > 0) {
      form.templateId = templates.value[0].id
    }
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
    <el-form-item label="审批模板" prop="templateId">
      <el-select v-model="form.templateId" class="w-full">
        <el-option v-for="t in templates" :key="t.id" :label="t.name" :value="t.id" />
      </el-select>
    </el-form-item>
    <el-form-item label="请假类型" prop="leaveType">
      <el-select v-model="form.leaveType" class="w-full">
        <el-option v-for="t in LEAVE_TYPES" :key="t" :label="t" :value="t" />
      </el-select>
    </el-form-item>
    <el-form-item label="开始日期" prop="startDate">
      <el-date-picker v-model="form.startDate" type="date" placeholder="请选择" class="w-full" />
    </el-form-item>
    <el-form-item label="结束日期" prop="endDate">
      <el-date-picker v-model="form.endDate" type="date" placeholder="请选择" class="w-full" />
    </el-form-item>
    <el-form-item label="请假原因" prop="reason">
      <el-input v-model="form.reason" type="textarea" :rows="3" placeholder="请填写请假原因" />
    </el-form-item>
    <el-form-item>
      <el-button type="primary" :loading="submitting" class="w-full" @click="handleSubmit">
        提交申请
      </el-button>
    </el-form-item>
  </el-form>
</template>

<style scoped>
.w-full { width: 100%; }
</style>
