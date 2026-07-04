<script setup>
import { useAuthStore } from '@/stores/auth'
import { ArrowDown, User, SwitchButton } from '@element-plus/icons-vue'
import { useRouter } from 'vue-router'

const auth = useAuthStore()
const router = useRouter()

function handleCommand(cmd) {
  if (cmd === 'profile') {
    router.push('/profile')
  } else if (cmd === 'logout') {
    auth.logout()
  }
}
</script>

<template>
  <div class="header">
    <div class="header-left">
      <span class="header-title">审批系统</span>
    </div>
    <div class="header-right">
      <el-dropdown @command="handleCommand" trigger="click">
        <div class="user-info">
          <div class="user-avatar">
            {{ auth.user?.realName?.charAt(0) || 'U' }}
          </div>
          <div class="user-meta">
            <span class="user-name">{{ auth.user?.realName }}</span>
            <span class="user-dept">{{ auth.user?.department }}</span>
          </div>
          <el-icon class="arrow-icon"><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="profile">
              <el-icon><User /></el-icon>
              个人中心
            </el-dropdown-item>
            <el-dropdown-item command="logout" divided>
              <el-icon><SwitchButton /></el-icon>
              退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </div>
</template>

<style scoped>
.header {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-primary);
  letter-spacing: 0.5px;
}

.user-info {
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 6px 12px;
  border-radius: 10px;
  transition: all 0.2s ease;
}

.user-info:hover {
  background: rgba(0, 0, 0, 0.04);
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 10px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-weight: 600;
  font-size: 14px;
}

.user-meta {
  display: flex;
  flex-direction: column;
  line-height: 1.3;
}

.user-name {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-primary);
}

.user-dept {
  font-size: 11px;
  color: var(--text-muted);
}

.arrow-icon {
  color: var(--text-muted);
  font-size: 12px;
  transition: transform 0.2s ease;
}

.user-info:hover .arrow-icon {
  transform: rotate(180deg);
}
</style>
