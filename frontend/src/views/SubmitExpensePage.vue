<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { useExpenseStore } from '@/stores/expense'
import { useUserStore } from '@/stores/users'
import { useAccountingStore } from '@/stores/accounting'
import { EXPENSE_CATEGORIES } from '@/utils/constants'
import StatusTag from '@/components/StatusTag.vue'

const store = useExpenseStore()
const userStore = useUserStore()
const accountingStore = useAccountingStore()

const formRef = ref(null)
const submitting = ref(false)

const form = ref({
  category: '',
  costCenterId: null,
  amount: null,
  description: '',
  receiptUrl: ''
})

const rules = {
  category: [{ required: true, message: '请选择费用类别', trigger: 'change' }],
  amount: [{ required: true, message: '请输入金额', trigger: 'blur' }],
  description: [{ required: true, message: '请输入费用说明', trigger: 'blur' }]
}

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await store.submitExpense(form.value)
    ElMessage.success('提交成功')
    form.value = { category: '', costCenterId: null, amount: null, description: '', receiptUrl: '' }
  } catch {
  } finally {
    submitting.value = false
  }
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

onMounted(async () => {
  await userStore.fetchUsers()
  await store.fetchMyExpenses()
  await accountingStore.fetchCostCenters()
})
</script>

<template>
  <div class="page">
    <el-row :gutter="20">
      <el-col :span="10">
        <el-card header="提交经费报销" shadow="never">
          <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
            <el-form-item label="费用类别" prop="category">
              <el-select v-model="form.category" placeholder="请选择" class="w-full">
                <el-option v-for="c in EXPENSE_CATEGORIES" :key="c.value" :label="c.label" :value="c.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="成本中心">
              <el-select v-model="form.costCenterId" placeholder="选择成本中心" clearable class="w-full">
                <el-option v-for="cc in accountingStore.costCenters" :key="cc.id" :label="`${cc.code} - ${cc.name}`" :value="cc.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="金额" prop="amount">
              <el-input-number v-model="form.amount" :min="0.01" :precision="2" :step="100" class="w-full" />
            </el-form-item>
            <el-form-item label="费用说明" prop="description">
              <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请描述费用用途" />
            </el-form-item>
            <el-form-item label="发票附件">
              <el-input v-model="form.receiptUrl" placeholder="附件URL（可选）" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" :loading="submitting" @click="handleSubmit">提交申请</el-button>
            </el-form-item>
          </el-form>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card header="我的报销记录" shadow="never">
          <el-table :data="store.myExpenses" stripe>
            <el-table-column prop="category" label="类别" width="80" />
            <el-table-column label="金额" width="100">
              <template #default="{ row }">¥{{ row.amount }}</template>
            </el-table-column>
            <el-table-column prop="description" label="说明" show-overflow-tooltip />
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <StatusTag :status="row.status" />
              </template>
            </el-table-column>
            <el-table-column label="时间" width="130">
              <template #default="{ row }">{{ formatDate(row.createTime) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="80">
              <template #default="{ row }">
                <router-link :to="`/expense/${row.id}`">
                  <el-button type="primary" link>详情</el-button>
                </router-link>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page { max-width: 1200px; margin: 0 auto; }
.w-full { width: 100%; }
</style>
