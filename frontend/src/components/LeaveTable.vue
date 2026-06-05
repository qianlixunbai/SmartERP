<script setup>
import { computed, inject } from 'vue'
import StatusTag from './StatusTag.vue'

const props = defineProps({
  data: { type: Array, default: () => [] },
  columns: { type: Array, default: () => [] },
  showActions: { type: Boolean, default: true },
  actionType: { type: String, default: 'view' }
})

const emit = defineEmits(['action'])

const getUserName = inject('getUserName', (id) => `用户${id}`)

const leaveColumns = [
  { prop: 'applicantId', label: '申请人', width: 100, isUser: true },
  { prop: 'leaveType', label: '类型', width: 80 },
  { prop: 'dates', label: '日期范围', width: 200 },
  { prop: 'reason', label: '原因', minWidth: 150 },
  { prop: 'status', label: '状态', width: 100 },
  { prop: 'createTime', label: '提交时间', width: 160 }
]

const finalColumns = computed(() => props.columns.length > 0 ? props.columns : leaveColumns)

function getValue(row, prop) {
  if (prop === 'applicantId' || prop === 'currentApproverId') {
    return getUserName(row[prop])
  }
  return row[prop]
}

function formatDate(dateStr) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const pad = n => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}

function formatCell(row, col) {
  const val = getValue(row, col.prop)
  if (col.prop === 'dates') return row.startDate && row.endDate ? `${row.startDate} ~ ${row.endDate}` : ''
  if (col.prop === 'status') return ''
  if (col.prop === 'createTime') return formatDate(val)
  if (col.prop === 'reason' && val && val.length > 20) return val.substring(0, 20) + '...'
  return val ?? ''
}

function handleAction(id) {
  emit('action', id)
}
</script>

<template>
  <el-table :data="data" stripe>
    <el-table-column
      v-for="col in finalColumns"
      :key="col.prop"
      :prop="col.prop"
      :label="col.label"
      :width="col.width"
      :minWidth="col.minWidth"
      show-overflow-tooltip
    >
      <template #default="{ row }">
        <slot :name="col.prop" :row="row">
          <StatusTag v-if="col.prop === 'status'" :status="row.status" />
          <span v-else>{{ formatCell(row, col) }}</span>
        </slot>
      </template>
    </el-table-column>
    <el-table-column v-if="showActions" label="操作" width="100" fixed="right">
      <template #default="{ row }">
        <el-button v-if="actionType === 'approve'" type="primary" size="small" @click="handleAction(row.id)">
          审批
        </el-button>
        <el-button v-else size="small" @click="handleAction(row.id)">
          详情
        </el-button>
      </template>
    </el-table-column>
  </el-table>
</template>
