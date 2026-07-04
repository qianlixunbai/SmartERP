<script setup>
import AppHeader from '@/components/AppHeader.vue'
import AppSidebar from '@/components/AppSidebar.vue'
</script>

<template>
  <el-container class="main-layout">
    <el-aside width="240px">
      <AppSidebar />
    </el-aside>
    <el-container>
      <el-header height="64px">
        <AppHeader />
      </el-header>
      <el-main>
        <slot />
      </el-main>
    </el-container>
  </el-container>
</template>

<style scoped>
.main-layout {
  min-height: 100vh;
}

.el-aside {
  background: var(--bg-sidebar);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid rgba(255, 255, 255, 0.06);
  overflow: hidden;
}

.el-header {
  background: var(--bg-header);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-bottom: 1px solid rgba(0, 0, 0, 0.04);
  display: flex;
  align-items: center;
  padding: 0 28px;
  position: sticky;
  top: 0;
  z-index: 10;
}

.el-main {
  background: var(--bg-page);
  padding: 24px;
  min-height: calc(100vh - 64px);
}
</style>
