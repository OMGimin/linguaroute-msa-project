<template>
  <AppShell>
    <PageHeader eyebrow="Learning analytics" title="직원별 학습 현황" description="직원별 수강 상태와 서버에서 계산된 진도율을 확인하세요." />
    <section class="metric-grid">
      <article v-for="item in stats" :key="item.label" class="metric card">
        <div class="metric-head"><span>{{ item.label }}</span><span class="metric-icon"><component :is="item.icon" :size="17" /></span></div>
        <div><strong class="metric-value">{{ item.value }}</strong><p class="trend">{{ item.desc }}</p></div>
      </article>
    </section>
    <section class="panel data-panel">
      <div class="table-head"><h2>직원별 수강 현황</h2><p>직원 정보와 수강 진도 API를 사용자 ID로 결합한 결과입니다.</p></div>
      <div class="table-wrap">
        <table class="data-table">
          <thead><tr><th>직원</th><th>전체 수강</th><th>진행 중</th><th>수료</th><th>평균 진도율</th><th>최근 학습</th></tr></thead>
          <tbody>
            <tr v-for="employee in employees" :key="employee.userId">
              <td><div class="person"><span class="avatar">{{ employee.name[0] }}</span><span><strong>{{ employee.name }}</strong><span>{{ employee.email }}</span></span></div></td>
              <td>{{ employee.total }}건</td><td>{{ employee.inProgress }}건</td><td>{{ employee.completed }}건</td>
              <td><div class="progress-cell"><div class="progress"><span :style="`width:${employee.progress}%`"></span></div><b>{{ employee.progress }}%</b></div></td>
              <td>{{ employee.latestActivity }}</td>
            </tr>
            <tr v-if="!employees.length"><td colspan="6" class="empty-message">수강 기록이 있는 직원이 없습니다.</td></tr>
          </tbody>
        </table>
      </div>
    </section>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { ChartNoAxesCombined, GraduationCap, Trophy, UsersRound } from '@lucide/vue'
import AppShell from '@/components/AppShell.vue'
import PageHeader from '@/components/PageHeader.vue'
import { companyApi } from '@/api/company.js'

const employees = ref([])
const stats = computed(() => {
  const items = employees.value
  const totalEnrollments = items.reduce((sum, item) => sum + item.total, 0)
  const totalCompleted = items.reduce((sum, item) => sum + item.completed, 0)
  const average = items.length ? Math.round(items.reduce((sum, item) => sum + item.progress, 0) / items.length) : 0
  return [
    { label: '학습 참여 직원', value: `${items.length}명`, desc: '수강 기록 기준', icon: UsersRound },
    { label: '전체 수강', value: `${totalEnrollments}건`, desc: '서버 수강 기록', icon: GraduationCap },
    { label: '평균 진도율', value: `${average}%`, desc: '직원별 평균의 평균', icon: ChartNoAxesCombined },
    { label: '수료 강의', value: `${totalCompleted}건`, desc: 'COMPLETED 상태', icon: Trophy }
  ]
})

function latestActivityOf(enrollments) {
  const values = enrollments.flatMap((item) => [item.completedAt, item.startedAt, item.enrolledAt]).filter(Boolean).sort()
  return values.at(-1)?.replace('T', ' ').slice(0, 16) || '-'
}

onMounted(async () => {
  try {
    const [progressResponse, employeeResponse] = await Promise.all([companyApi.getEnrollmentProgress(), companyApi.getEmployees()])
    const enrollments = progressResponse.data.data.content || []
    const employeeById = new Map((employeeResponse.data.data || []).map((employee) => [employee.userId, employee]))
    const grouped = new Map()
    enrollments.forEach((enrollment) => grouped.set(enrollment.userId, [...(grouped.get(enrollment.userId) || []), enrollment]))
    employees.value = [...grouped.entries()].map(([userId, items]) => {
      const employee = employeeById.get(userId) || { name: `직원 #${userId}`, email: '-' }
      const completed = items.filter((item) => item.status === 'COMPLETED').length
      return {
        userId, name: employee.name, email: employee.email, total: items.length,
        inProgress: items.length - completed, completed,
        progress: Math.round(items.reduce((sum, item) => sum + Number(item.progressRate), 0) / items.length),
        latestActivity: latestActivityOf(items)
      }
    })
  } catch (_) { employees.value = [] }
})
</script>

<style scoped>
.data-panel{margin-top:14px;padding:0;overflow:hidden}.table-head{padding:18px 20px;border-bottom:1px solid var(--line)}.table-head h2{font-size:15px}.table-head p{margin-top:4px;color:var(--muted);font-size:9px}.progress-cell{display:flex;align-items:center;gap:8px}.progress-cell .progress{width:90px}.progress-cell b{font-size:8px}.empty-message{padding:42px!important;color:var(--muted);text-align:center}
</style>
