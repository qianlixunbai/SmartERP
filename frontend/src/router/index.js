import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '@/stores/auth'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/LoginPage.vue'),
    meta: { layout: 'auth', guest: true, title: '登录' }
  },
  {
    path: '/',
    redirect: '/dashboard'
  },
  {
    path: '/dashboard',
    name: 'Dashboard',
    component: () => import('@/views/DashboardPage.vue'),
    meta: { title: '工作台', requiresAuth: true }
  },
  {
    path: '/submit-application',
    name: 'SubmitApplication',
    component: () => import('@/views/SubmitApplicationPage.vue'),
    meta: { title: '提交申请', requiresAuth: true }
  },
  {
    path: '/my-requests',
    name: 'MyRequests',
    component: () => import('@/views/MyRequestsPage.vue'),
    meta: { title: '我的申请列表', requiresAuth: true }
  },
  {
    path: '/pending-approvals',
    name: 'PendingApprovals',
    component: () => import('@/views/PendingApprovalsPage.vue'),
    meta: { title: '待审批', requiresAuth: true }
  },
  {
    path: '/my-approvals',
    name: 'MyApprovals',
    component: () => import('@/views/MyApprovalsPage.vue'),
    meta: { title: '我的审批', requiresAuth: true }
  },
  {
    path: '/approval/:id',
    name: 'ApprovalDetail',
    component: () => import('@/views/ApprovalDetailPage.vue'),
    meta: { title: '审批详情', requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/ProfilePage.vue'),
    meta: { title: '个人中心', requiresAuth: true }
  },
  {
    path: '/templates',
    name: 'TemplateList',
    component: () => import('@/views/TemplateListPage.vue'),
    meta: { title: '审批模板', requiresAuth: true, role: 'MANAGER' }
  },
  {
    path: '/templates/preview/:id',
    name: 'TemplatePreview',
    component: () => import('@/views/TemplatePreviewPage.vue'),
    meta: { title: '模板预览', requiresAuth: true }
  },
  {
    path: '/templates/edit/:id?',
    name: 'TemplateEdit',
    component: () => import('@/views/TemplateEditPage.vue'),
    meta: { title: '编辑模板', requiresAuth: true, role: 'MANAGER' }
  },
  {
    path: '/stats',
    name: 'Stats',
    component: () => import('@/views/StatsPage.vue'),
    meta: { title: '统计报表', requiresAuth: true }
  },
  {
    path: '/all-requests',
    name: 'AllRequests',
    component: () => import('@/views/AllRequestsPage.vue'),
    meta: { title: '全部记录', requiresAuth: true, role: 'MANAGER' }
  },
  {
    path: '/users',
    name: 'UserList',
    component: () => import('@/views/UserListPage.vue'),
    meta: { title: '用户列表', requiresAuth: true, role: 'MANAGER' }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFoundPage.vue'),
    meta: { title: '404' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to, from, next) => {
  const auth = useAuthStore()

  if (auth.token && !auth.user) {
    try {
      await auth.fetchUser()
    } catch {
      auth.logout()
      return next('/login')
    }
  }

  if (to.meta.requiresAuth && !auth.token) {
    return next('/login')
  }

  if (to.meta.guest && auth.token) {
    return next('/submit-application')
  }

  if (to.meta.role && auth.user?.role !== to.meta.role) {
    return next('/submit-application')
  }

  next()
})

export default router
