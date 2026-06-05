<script setup>
import { reactive, ref } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { User, Lock } from '@element-plus/icons-vue'

const auth = useAuthStore()
const formRef = ref(null)
const form = reactive({ username: '', password: '' })
const loading = ref(false)

async function handleLogin() {
  if (!form.username || !form.password) return
  loading.value = true
  try {
    await auth.login(form.username, form.password)
  } catch {
    // error handled by axios interceptor
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <el-card class="login-card" shadow="always">
    <h2 class="login-title">SmartOA 审批系统</h2>
    <el-form ref="formRef" :model="form" size="large" @keyup.enter="handleLogin">
      <el-form-item>
        <el-input v-model="form.username" placeholder="请输入用户名" :prefix-icon="User" />
      </el-form-item>
      <el-form-item>
        <el-input v-model="form.password" type="password" placeholder="请输入密码" :prefix-icon="Lock" show-password />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" :loading="loading" class="login-btn" @click="handleLogin">
          登 录
        </el-button>
      </el-form-item>
    </el-form>
    <p class="login-hint">测试账号: admin / zhangsan / lisi &nbsp; 密码: 123456</p>
  </el-card>
</template>

<style scoped>
.login-card {
  width: 400px;
}
.login-title {
  text-align: center;
  margin-bottom: 24px;
  color: #303133;
}
.login-btn {
  width: 100%;
}
.login-hint {
  text-align: center;
  color: #999;
  font-size: 13px;
  margin: 0;
}
</style>
