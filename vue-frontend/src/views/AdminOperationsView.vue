<template>
  <AppShell>
    <PageHeader eyebrow="Platform operations" :title="config.title" :description="config.description" />
    <AsyncState v-if="state === 'loading'" type="loading" title="운영 데이터를 불러오고 있어요" description="각 소유 서비스의 관리자 API를 확인하고 있습니다." />
    <AsyncState v-else-if="state === 'error'" type="error" title="운영 데이터를 불러오지 못했어요" :description="errorMessage" @retry="load" />
    <template v-else>
      <section v-if="type === 'dashboard'" class="metric-grid">
        <article v-for="item in dashboardMetrics" :key="item.label" class="metric card"><div class="metric-head"><span>{{ item.label }}</span></div><strong class="metric-value">{{ item.value }}</strong><p class="trend">실제 API 조회 결과</p></article>
      </section>
      <section v-else class="panel data-panel">
        <div class="table-toolbar"><strong>총 {{ rows.length }}건</strong><button class="button small" @click="load">새로고침</button></div>
        <div v-if="rows.length" class="table-wrap">
          <table class="data-table">
            <thead><tr><th v-for="column in config.columns" :key="column.key">{{ column.label }}</th></tr></thead>
            <tbody><tr v-for="(row, index) in rows" :key="row.id || row.userId || row.paymentId || row.enrollmentId || index"><td v-for="column in config.columns" :key="column.key">{{ display(row, column.key) }}</td></tr></tbody>
          </table>
        </div>
        <p v-else class="empty-message">조회된 데이터가 없습니다.</p>
      </section>
    </template>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import AppShell from '@/components/AppShell.vue'
import PageHeader from '@/components/PageHeader.vue'
import AsyncState from '@/components/AsyncState.vue'
import { adminApi } from '@/api/admin.js'

const props = defineProps({ type: { type: String, default: 'dashboard' } })
const state = ref('loading')
const errorMessage = ref('잠시 후 다시 시도해 주세요.')
const data = ref({ users: [], companies: [], payments: [], enrollments: [] })
const configs = {
  dashboard: { title: '운영 대시보드', description: '사용자·기업·결제·수강 상태를 서비스별 실제 데이터로 확인합니다.', columns: [] },
  users: { title: '사용자 관리', description: '계정의 역할, 소속과 상태를 확인합니다.', columns: [{ key: 'userId', label: 'ID' }, { key: 'name', label: '이름' }, { key: 'email', label: '이메일' }, { key: 'businessRole', label: '역할' }, { key: 'companyId', label: '기업 ID' }, { key: 'status', label: '상태' }] },
  companies: { title: '기업 관리', description: '가입 기업과 계정 상태를 확인합니다.', columns: [{ key: 'companyId', label: 'ID' }, { key: 'name', label: '기업명' }, { key: 'businessNumber', label: '사업자번호' }, { key: 'status', label: '상태' }, { key: 'createdAt', label: '등록일' }] },
  payments: { title: '결제 관리', description: '전체 기업의 결제 처리 결과를 확인합니다.', columns: [{ key: 'paymentId', label: '결제 ID' }, { key: 'companyId', label: '기업 ID' }, { key: 'amount', label: '금액' }, { key: 'currency', label: '통화' }, { key: 'status', label: '상태' }, { key: 'requestedAt', label: '요청일' }] },
  enrollments: { title: '수강 관리', description: '전체 기업의 수강 상태와 서버 계산 진도율을 확인합니다.', columns: [{ key: 'enrollmentId', label: '수강 ID' }, { key: 'companyId', label: '기업 ID' }, { key: 'userId', label: '사용자 ID' }, { key: 'courseId', label: '강의 ID' }, { key: 'progressRate', label: '진도율' }, { key: 'status', label: '상태' }] },
}
const config = computed(() => configs[props.type] || configs.dashboard)
const rows = computed(() => data.value[props.type] || [])
const dashboardMetrics = computed(() => [
  { label: '전체 기업', value: `${data.value.companies.length}개` },
  { label: '전체 사용자', value: `${data.value.users.length}명` },
  { label: '전체 결제', value: `${data.value.payments.length}건` },
  { label: '전체 수강', value: `${data.value.enrollments.length}건` },
])

function unwrap(response) { return response?.data?.data ?? response?.data ?? [] }
function rowsFrom(response) {
  const payload = unwrap(response)
  return Array.isArray(payload) ? payload : (payload?.content || [])
}
function display(row, key) {
  const value = row[key]
  if (value == null || value === '') return '-'
  if (key === 'amount') return Number(value).toLocaleString()
  if (key === 'progressRate') return `${Number(value)}%`
  if (key.endsWith('At')) return String(value).slice(0, 10)
  return value
}
async function load() {
  state.value = 'loading'
  try {
    if (props.type === 'dashboard') {
      const [users, companies, payments, enrollments] = await Promise.all([adminApi.getUsers(), adminApi.getCompanies(), adminApi.getPayments(), adminApi.getEnrollments({ page: 0, size: 100 })])
      data.value = { users: rowsFrom(users), companies: rowsFrom(companies), payments: rowsFrom(payments), enrollments: rowsFrom(enrollments) }
    } else {
      const response = props.type === 'users' ? await adminApi.getUsers() : props.type === 'companies' ? await adminApi.getCompanies() : props.type === 'payments' ? await adminApi.getPayments() : await adminApi.getEnrollments({ page: 0, size: 100 })
      data.value[props.type] = rowsFrom(response)
    }
    state.value = 'ready'
  } catch (error) {
    errorMessage.value = error.response?.data?.message || '운영 조회 API 요청에 실패했습니다.'
    state.value = 'error'
  }
}
onMounted(load)
watch(() => props.type, load)
</script>

<style scoped>
.data-panel{padding:0;overflow:hidden}.table-toolbar{display:flex;align-items:center;justify-content:space-between;padding:14px 18px;border-bottom:1px solid var(--line)}.empty-message{padding:44px;color:var(--muted);text-align:center}.metric-grid{margin-bottom:18px}
</style>
