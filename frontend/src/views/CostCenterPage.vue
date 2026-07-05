<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { useAccountingStore } from '@/stores/accounting'
import * as accountingApi from '@/api/accounting'

const store = useAccountingStore()
const dialogVisible = ref(false)
const editingId = ref(null)
const form = ref({ code: '', name: '', description: '' })

onMounted(() => store.fetchCostCenters())

function openCreate() {
  editingId.value = null
  form.value = { code: '', name: '', description: '' }
  dialogVisible.value = true
}

function openEdit(row) {
  editingId.value = row.id
  form.value = { code: row.code, name: row.name, description: row.description || '' }
  dialogVisible.value = true
}

async function handleSave() {
  if (!form.value.code || !form.value.name) {
    ElMessage.warning('编码和名称不能为空')
    return
  }
  try {
    if (editingId.value) {
      await accountingApi.updateCostCenter(editingId.value, form.value)
      ElMessage.success('更新成功')
    } else {
      await accountingApi.createCostCenter(form.value)
      ElMessage.success('创建成功')
    }
    dialogVisible.value = false
    await store.fetchCostCenters()
  } catch {}
}
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>成本中心</span>
          <el-button type="primary" :icon="Plus" @click="openCreate">新建</el-button>
        </div>
      </template>
      <el-table :data="store.costCenters" stripe>
        <el-table-column prop="code" label="编码" width="120" />
        <el-table-column prop="name" label="名称" width="160" />
        <el-table-column prop="description" label="描述" />
        <el-table-column label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.active ? 'success' : 'info'" size="small">
              {{ row.active ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ row }">
            <el-button type="primary" link @click="openEdit(row)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑成本中心' : '新建成本中心'" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="编码" required>
          <el-input v-model="form.code" placeholder="如 CC001" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.name" placeholder="如 研发部" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSave">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page { max-width: 900px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
