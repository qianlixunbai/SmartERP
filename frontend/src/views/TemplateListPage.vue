<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getTemplateList, deleteTemplate } from '@/api/template'

const router = useRouter()
const templates = ref([])

async function fetchData() {
  templates.value = await getTemplateList()
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除该模板？', '提示', { type: 'warning' })
    await deleteTemplate(id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {
    // cancelled or error
  }
}

onMounted(fetchData)
</script>

<template>
  <div class="page">
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <span>审批模板</span>
          <el-button type="primary" @click="router.push('/templates/edit')">新建模板</el-button>
        </div>
      </template>
      <el-table :data="templates" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="模板名称" width="200" />
        <el-table-column prop="description" label="描述" minWidth="200" show-overflow-tooltip />
        <el-table-column prop="enabled" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.enabled ? 'success' : 'info'" size="small">
              {{ row.enabled ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="210" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="router.push(`/templates/preview/${row.id}`)">预览</el-button>
            <el-button size="small" @click="router.push(`/templates/edit/${row.id}`)">编辑</el-button>
            <el-button size="small" type="danger" @click="handleDelete(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page { max-width: 1000px; margin: 0 auto; }
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
