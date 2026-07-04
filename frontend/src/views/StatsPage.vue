<script setup>
import { ref, onMounted, watchEffect } from 'vue'
import * as echarts from 'echarts'
import { getAvgDuration, getTemplateUsage } from '@/api/stats'

const durationChart = ref(null)
const usageChart = ref(null)

onMounted(async () => {
  const [durationData, usageData] = await Promise.all([
    getAvgDuration(),
    getTemplateUsage()
  ])

  // 平均审批时长 — 柱状图
  if (durationChart.value) {
    const chart = echarts.init(durationChart.value)
    chart.setOption({
      title: { text: '各模板平均审批时长（分钟）', left: 'center' },
      tooltip: {},
      xAxis: { data: durationData.map(d => d.templateName) },
      yAxis: { name: '分钟' },
      series: [{
        type: 'bar',
        data: durationData.map(d => d.avgMinutes),
        itemStyle: { color: '#409eff' },
        barMaxWidth: 60
      }]
    })
  }

  // 模板使用量 — 饼图
  if (usageChart.value) {
    const chart = echarts.init(usageChart.value)
    chart.setOption({
      title: { text: '各模板使用量', left: 'center' },
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['40%', '70%'],
        data: usageData.map(d => ({ name: d.templateName, value: d.count })),
        emphasis: { itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0, 0, 0, 0.5)' } }
      }]
    })
  }
})
</script>

<template>
  <div class="page">
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card shadow="never">
          <div ref="durationChart" style="height:400px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card shadow="never">
          <div ref="usageChart" style="height:400px"></div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page { max-width: 1200px; margin: 0 auto; }
</style>
