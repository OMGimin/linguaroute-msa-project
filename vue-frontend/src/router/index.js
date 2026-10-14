import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  { path: '/', name: 'Landing', component: () => import('@/views/LandingView.vue') },
  { path: '/pricing', name: 'Pricing', component: () => import('@/views/PricingView.vue') },
  { path: '/login', name: 'Login', component: () => import('@/views/LoginView.vue') },
  { path: '/callback', name: 'OAuthCallback', component: () => import('@/views/OAuthCallbackView.vue') },
  { path: '/signup/company', name: 'CompanySignup', component: () => import('@/views/AuthFlowView.vue'), props: { mode: 'company-signup' } },
  { path: '/signup/employee', name: 'EmployeeSignup', component: () => import('@/views/AuthFlowView.vue'), props: { mode: 'employee-signup' } },
  { path: '/account/recovery', name: 'AccountRecovery', component: () => import('@/views/AuthFlowView.vue'), props: { mode: 'recovery' } },
  { path: '/account/reset-password', name: 'ResetPassword', component: () => import('@/views/AuthFlowView.vue'), props: { mode: 'reset-confirm' } },
  { path: '/app', name: 'EmployeeHome', component: () => import('@/views/EmployeeDashboardLiveView.vue'), meta: { role: 'employee' } },
  { path: '/courses', name: 'CourseList', component: () => import('@/views/CourseListView.vue'), meta: { role: 'employee' } },
  { path: '/courses/:id(\\d+)', name: 'CourseDetail', component: () => import('@/views/CourseDetailLiveView.vue'), meta: { role: 'employee' } },
  { path: '/recommendations', name: 'Recommendations', component: () => import('@/views/RecommendationView.vue'), meta: { role: 'employee' } },
  { path: '/learning', alias: '/enrollments', name: 'MyLearning', component: () => import('@/views/EnrollmentView.vue'), meta: { role: 'employee' } },
  { path: '/learning/:enrollmentId/lessons/:lessonId', name: 'LearningPlayer', component: () => import('@/views/LearningPlayerLiveView.vue'), meta: { role: 'employee' } },
  { path: '/profile', alias: '/mypage', name: 'Profile', component: () => import('@/views/MyPageView.vue'), meta: { requiresAuth: true } },

  { path: '/company', name: 'CompanyDashboard', component: () => import('@/views/CompanyDashboardView.vue'), meta: { role: 'company' } },
  { path: '/company/employees', name: 'CompanyEmployees', component: () => import('@/views/EmployeeManagementView.vue'), meta: { role: 'company' } },
  { path: '/company/progress', name: 'CompanyProgress', component: () => import('@/views/CompanyProgressView.vue'), meta: { role: 'company' } },
  { path: '/company/subscription', name: 'CompanySubscription', component: () => import('@/views/SubscriptionView.vue'), meta: { role: 'company' } },
  { path: '/company/checkout', name: 'CompanyCheckout', component: () => import('@/views/CheckoutView.vue'), meta: { role: 'company' } },
  { path: '/company/settings', name: 'CompanySettings', component: () => import('@/views/CompanySettingsView.vue'), meta: { role: 'company' } },

  { path: '/admin', name: 'AdminDashboard', component: () => import('@/views/AdminOperationsView.vue'), props: { type: 'dashboard' }, meta: { role: 'admin' } },
  { path: '/admin/companies', name: 'AdminCompanies', component: () => import('@/views/AdminOperationsView.vue'), props: { type: 'companies' }, meta: { role: 'admin' } },
  { path: '/admin/users', name: 'AdminUsers', component: () => import('@/views/AdminOperationsView.vue'), props: { type: 'users' }, meta: { role: 'admin' } },
  { path: '/admin/payments', name: 'AdminPayments', component: () => import('@/views/AdminOperationsView.vue'), props: { type: 'payments' }, meta: { role: 'admin' } },
  { path: '/admin/enrollments', name: 'AdminEnrollments', component: () => import('@/views/AdminOperationsView.vue'), props: { type: 'enrollments' }, meta: { role: 'admin' } },
  { path: '/admin/courses', name: 'AdminCourses', component: () => import('@/views/AdminCourseView.vue'), meta: { role: 'admin' } },
  { path: '/admin/courses/new', name: 'AdminCourseCreate', component: () => import('@/views/CourseCreateView.vue'), meta: { role: 'admin' } },
  { path: '/admin/courses/:id/edit', name: 'AdminCourseEdit', component: () => import('@/views/CourseCreateView.vue'), meta: { role: 'admin' } },
  { path: '/:pathMatch(.*)*', redirect: '/' }
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() { return { top: 0 } }
})

const routeByBusinessRole = {
  PLATFORM_ADMIN: { role: 'admin', home: '/admin' },
  COMPANY_ADMIN: { role: 'company', home: '/company' },
  EMPLOYEE: { role: 'employee', home: '/app' }
}

router.beforeEach((to) => {
  const accessToken = sessionStorage.getItem('access_token')
  const protectedRoute = Boolean(to.meta.role || to.meta.requiresAuth)
  if (!protectedRoute) return true
  if (!accessToken) return '/login?reason=auth-required'

  let user = null
  try {
    user = JSON.parse(sessionStorage.getItem('user') || 'null')
  } catch (_) {
    sessionStorage.removeItem('access_token')
    sessionStorage.removeItem('user')
    return '/login?reason=session-expired'
  }
  if (!user?.businessRole) {
    sessionStorage.removeItem('access_token')
    sessionStorage.removeItem('user')
    return '/login?reason=session-expired'
  }
  if (user?.status && user.status !== 'ACTIVE') {
    sessionStorage.removeItem('access_token')
    sessionStorage.removeItem('user')
    return '/login?reason=user-inactive'
  }

  const access = routeByBusinessRole[user?.businessRole]
  if (!access) return '/login?reason=session-expired'
  if (to.meta.role && access.role !== to.meta.role) return access.home
  return true
})

export default router
