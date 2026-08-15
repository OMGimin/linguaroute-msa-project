<template>
  <AppShell>
    <PageHeader eyebrow="Company overview" title="기업 학습 현황" description="좌석과 직원 학습 현황을 실제 저장 데이터로 확인하세요.">
      <router-link class="button accent" to="/company/employees"><UserPlus :size="16"/> 직원 초대</router-link>
    </PageHeader>
    <p v-if="loadError" class="panel load-error" role="alert">{{ loadError }}</p>
    <section class="metric-grid">
      <article v-for="item in metrics" :key="item.label" class="metric card"><div class="metric-head"><span>{{ item.label }}</span><span class="metric-icon"><component :is="item.icon" :size="17"/></span></div><div><strong class="metric-value">{{ item.value }}</strong><p class="trend">{{ item.trend }}</p></div></article>
    </section>
    <section class="panel employee-preview">
      <div class="section-head"><div><h2>직원별 학습 현황</h2><p>서버에서 계산한 수강 수와 평균 진도율입니다.</p></div><router-link to="/company/progress">전체 현황 보기 <ArrowRight :size="15"/></router-link></div>
      <div v-if="employees.length" class="table-wrap"><table class="data-table"><thead><tr><th>직원</th><th>수강 강의</th><th>평균 진도</th><th>최근 학습</th><th>상태</th></tr></thead><tbody><tr v-for="employee in employees.slice(0,5)" :key="employee.userId || employee.email"><td><div class="person"><span class="avatar">{{ employee.name[0] }}</span><span><strong>{{ employee.name }}</strong><span>{{ employee.email }}</span></span></div></td><td>{{ employee.courses }}개</td><td><div class="table-progress"><div class="progress"><span :style="`width:${employee.progress}%`"></span></div><b>{{ employee.progress }}%</b></div></td><td>{{ employee.joined || '-' }}</td><td><span class="tag" :class="employee.status==='ACTIVE'?'':'amber'">{{ employee.status }}</span></td></tr></tbody></table></div>
      <p v-else-if="!loadError" class="empty-message">등록된 직원 또는 수강 데이터가 없습니다.</p>
    </section>
  </AppShell>
</template>
<script setup>
import { onMounted, ref } from 'vue'
import { UserPlus,ArrowRight,UsersRound,Armchair,GraduationCap,ChartNoAxesCombined } from '@lucide/vue'
import AppShell from '@/components/AppShell.vue'
import PageHeader from '@/components/PageHeader.vue'
import { companyApi } from '@/api/company.js'
import { averageProgress } from '@/domain/progress.js'

const employees = ref([])
const metrics = ref([])
const loadError = ref('')

onMounted(async () => {
  try {
    const [employeeResponse, seatResponse, progressResponse] = await Promise.all([
      companyApi.getEmployees(), companyApi.getSeats(), companyApi.getEnrollmentProgress()
    ])
    const employeeList = employeeResponse.data.data
    const enrollments = progressResponse.data.data.content
    employees.value = employeeList.map((employee) => ({
      ...employee, team: '-', courses: enrollments.filter((item) => item.userId === employee.userId).length,
      progress: averageProgress(enrollments.filter((item) => item.userId === employee.userId)), joined: employee.joinedAt
    }))
    const seats = seatResponse.data.data
    const activeEmployees = employeeList.filter((employee) => employee.status === 'ACTIVE').length
    const average = enrollments.length ? Math.round(enrollments.reduce((sum, item) => sum + Number(item.progressRate), 0) / enrollments.length) : 0
    metrics.value = [
      { label: '활성 직원', value: `${activeEmployees}명`, trend: '현재 소속 직원 기준', icon: UsersRound },
      { label: '잔여 좌석', value: `${seats.remaining}석`, trend: `${seats.used} / ${seats.purchased}석 사용`, icon: Armchair },
      { label: '수강 중 강의', value: `${enrollments.filter((item) => item.status !== 'COMPLETED').length}건`, trend: '서버 수강 상태 기준', icon: GraduationCap },
      { label: '평균 진도율', value: `${average}%`, trend: '서버 계산 진도율', icon: ChartNoAxesCombined }
    ]
  } catch (error) {
    employees.value = []
    metrics.value = []
    loadError.value = error.response?.data?.message || '기업 학습 현황을 불러오지 못했습니다.'
  }
})

</script>
<style scoped>.subscription-strip{display:grid;grid-template-columns:minmax(0,1fr) 250px auto;align-items:center;gap:24px;margin-bottom:14px;padding:17px 20px;color:white;background:var(--forest);border-radius:16px}.subscription-strip>div:first-child{display:flex;align-items:center;gap:11px}.subscription-strip strong{font-size:12px}.subscription-strip small{color:#93ad9d;font-size:9px}.seat-usage span{display:block;margin-bottom:6px;color:#a9c0b2;font-size:9px}.seat-usage b{color:white}.seat-usage .progress{background:rgba(255,255,255,.12)}.seat-usage .progress span{background:var(--lime)}.subscription-strip>a{display:flex;align-items:center;gap:6px;color:var(--lime);font-size:10px;font-weight:700}.company-grid{display:grid;grid-template-columns:minmax(0,1.5fr) minmax(280px,1fr);gap:14px;margin:14px 0}.section-head{display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:14px}.section-head h2{font-size:15px}.section-head p{margin-top:3px;color:var(--muted);font-size:9px}.section-head select{padding:6px 9px;background:var(--surface);border:1px solid var(--line);border-radius:7px;color:var(--muted);font-size:9px}.section-head button{color:var(--muted);background:transparent}.section-head a{display:flex;align-items:center;gap:5px;color:var(--forest-2);font-size:9px;font-weight:700}.chart{height:190px;display:flex;align-items:flex-end;gap:18px;padding:18px 10px 24px;border-bottom:1px solid var(--line)}.chart>span{position:relative;flex:1;background:#dfe8e0;border-radius:5px 5px 0 0}.chart>span:last-child{background:var(--forest-2)}.chart i{position:absolute;top:-19px;width:100%;font-style:normal;font-size:8px;text-align:center}.chart small{position:absolute;bottom:-21px;width:100%;color:var(--muted);font-size:8px;text-align:center}.activity-list{display:grid;gap:2px}.activity-list>div{display:flex;align-items:center;gap:10px;padding:9px;border-radius:9px}.activity-list>div:hover{background:var(--surface-2)}.activity-list>div>span{width:30px;height:30px;display:grid;place-items:center;color:var(--forest);background:var(--mint);border-radius:9px}.activity-list>div>span.blue{color:var(--blue);background:var(--blue-soft)}.activity-list>div>span.amber{color:#8b6419;background:var(--amber-soft)}.activity-list>div>span.purple{color:var(--purple);background:var(--purple-soft)}.activity-list p{flex:1}.activity-list strong,.activity-list small{display:block}.activity-list strong{font-size:9px}.activity-list small{margin-top:3px;color:var(--muted);font-size:8px}.employee-preview{padding:0;overflow:hidden}.employee-preview .section-head{padding:18px 20px 5px}.table-progress{display:flex;align-items:center;gap:8px}.table-progress .progress{width:75px}.table-progress b{font-size:9px}@media(max-width:1050px){.company-grid{grid-template-columns:1fr}.subscription-strip{grid-template-columns:1fr auto}.seat-usage{display:none}}@media(max-width:620px){.subscription-strip{grid-template-columns:1fr}.subscription-strip>div:first-child{align-items:flex-start;flex-direction:column}.chart{gap:8px}}</style>
