import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

const service = axios.create({
  baseURL: '/api',
  timeout: 15000,
  headers: { 'Content-Type': 'application/json' }
})

service.interceptors.request.use(
  config => {
    const token = localStorage.getItem('token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => Promise.reject(error)
)

service.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code === 200) {
      return res.data
    }
    // 业务码非 200（如 body.code=1002 但 HTTP 仍是 200 的旧场景兜底）
    ElMessage.error(res.message || '请求失败')
    return Promise.reject(new Error(res.message))
  },
  error => {
    const status = error.response?.status
    const backendMsg = error.response?.data?.message

    if (status === 401) {
      // 登录接口自身返回 401：显示后端消息，不跳转、不清除 token
      const url = error.config?.url || ''
      if (url.includes('/login')) {
        ElMessage.error(backendMsg || '用户名或密码错误')
      } else {
        // 其他接口 401：清除登录状态并跳转
        localStorage.removeItem('token')
        localStorage.removeItem('user')
        router.push('/login')
        ElMessage.error(backendMsg || '登录已过期，请重新登录')
      }
    } else {
      ElMessage.error(backendMsg || error.message || '请求失败')
    }
    return Promise.reject(error)
  }
)

export default service
