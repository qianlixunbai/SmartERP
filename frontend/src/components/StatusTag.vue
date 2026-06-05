<script setup>
import { STATUS_MAP } from '@/utils/constants'

const props = defineProps({
  status: { type: String, required: true }
})

const config = STATUS_MAP[props.status] || { label: props.status, type: 'info' }
</script>

<template>
  <el-tag :type="config.type" size="small">{{ config.label }}</el-tag>
</template>
