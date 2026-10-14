<template>
  <AppShell>
    <PageHeader eyebrow="Company profile" title="기업 정보" description="로그인한 기업 관리자의 소속 기업 정보를 조회하고 수정하세요." />
    <div class="settings-grid">
      <section class="panel company-profile">
        <div class="company-head"><span class="company-logo">S</span><span><h2>{{ form.name }}</h2><p>기업 ID {{ company.id }} · {{ company.status }}</p></span><span class="tag" :class="company.status === 'ACTIVE' ? '' : 'gray'">{{ company.status }}</span></div>
        <div class="contract-note"><ShieldCheck :size="18" /><span><strong>최신 user-service 계약 반영</strong><code>POST /api/companies/me?action=update-company</code>는 기업명만 수정하며 사업자번호는 변경하지 않습니다.</span></div>
        <form class="form-stack" @submit.prevent="saveCompany">
          <div class="field"><label>기업명</label><input v-model.trim="form.name" class="input" maxlength="100" required /><small>최대 100자</small></div>
          <div class="field"><label>사업자등록번호</label><input class="input" :value="formattedBusinessNumber" disabled /><small>가입 후에는 변경할 수 없습니다.</small></div>
          <div v-if="error" class="form-message error"><TriangleAlert :size="15" />{{ error }}</div>
          <div class="save-row"><span v-if="saved"><CircleCheck :size="15" /> 기업명이 저장되었습니다.</span><button class="button primary" :disabled="saving">{{ saving ? '저장 중' : '기업 정보 저장' }}</button></div>
        </form>
      </section>

      <aside>
        <section class="panel admin-card"><span class="tag">대표 관리자</span><div class="person-large"><span class="avatar">{{ admin.name?.[0] || '?' }}</span><span><strong>{{ admin.name || '-' }}</strong><small>{{ admin.email || '-' }}</small></span></div><div class="contact-row"><ShieldCheck :size="15" /><span><small>비즈니스 역할</small><strong>{{ admin.businessRole || '-' }}</strong></span></div><div class="contact-row"><Building2 :size="15" /><span><small>기업 소속</small><strong>companyId {{ admin.companyId ?? company.id }}</strong></span></div><router-link class="button" to="/profile">관리자 계정 설정</router-link></section>
        <section class="panel scope-card"><h3>관리 가능한 정보</h3><p><CircleCheck :size="14" /> 기업명</p><p><LockKeyhole :size="14" /> 사업자번호는 읽기 전용</p><p><LockKeyhole :size="14" /> 구독·좌석은 각 소유 API에서 관리</p></section>
      </aside>
    </div>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Building2, CircleCheck, LockKeyhole, ShieldCheck, TriangleAlert } from '@lucide/vue'
import AppShell from '@/components/AppShell.vue'
import PageHeader from '@/components/PageHeader.vue'
import { authApi } from '@/api/auth.js'
import { formatBusinessNumber, unwrapApiData } from '@/constants/domain.js'

const company = reactive({ id: null, name: '', businessNumber: '', status: '' })
const admin = reactive({ name: '', email: '', businessRole: '', companyId: null })
const form = reactive({ name: company.name })
const saving = ref(false)
const saved = ref(false)
const error = ref('')
const formattedBusinessNumber = computed(() => formatBusinessNumber(company.businessNumber))

onMounted(async () => {
  try {
    const [companyResponse, userResponse] = await Promise.all([authApi.getMyCompany(), authApi.getMe()])
    Object.assign(company, unwrapApiData(companyResponse))
    Object.assign(admin, unwrapApiData(userResponse))
    form.name = company.name
  } catch (requestError) {
    error.value = requestError.response?.data?.message || '기업 정보를 불러오지 못했습니다.'
  }
})

async function saveCompany() {
  saved.value = false
  error.value = ''
  saving.value = true
  try {
    Object.assign(company, unwrapApiData(await authApi.updateMyCompany(form.name)))
    saved.value = true
  } catch (requestError) {
    error.value = requestError.response?.data?.message || '기업 정보를 저장하지 못했습니다.'
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.settings-grid{display:grid;grid-template-columns:1fr 300px;gap:16px}.company-profile{padding:28px}.company-head{display:flex;align-items:center;gap:13px}.company-head>span:nth-child(2){flex:1}.company-logo{width:54px;height:54px;display:grid;place-items:center;color:var(--ink);background:var(--lime);border-radius:15px;font-family:var(--font-display);font-size:18px;font-weight:800}.company-head h2{font-size:19px}.company-head p{margin-top:4px;color:var(--muted);font-size:9px}.contract-note{display:flex;gap:10px;margin:24px 0;padding:14px;color:var(--forest);background:var(--mint);border-radius:11px;font-size:9px}.contract-note span,.contract-note strong,.contract-note code{display:block}.contract-note strong{margin-bottom:4px;font-size:10px}.contract-note code{color:var(--forest-2)}.company-profile form{max-width:680px}.field small{color:var(--muted);font-size:8px}.save-row{display:flex;align-items:center;justify-content:flex-end;gap:14px;margin-top:8px}.save-row span,.form-message{display:flex;align-items:center;gap:5px;color:var(--forest-2);font-size:9px}.form-message.error{justify-content:flex-start;color:var(--danger)}aside{display:grid;gap:14px;align-content:start}.admin-card,.scope-card{padding:22px}.person-large{display:flex;align-items:center;gap:11px;margin:18px 0}.person-large .avatar{width:44px;height:44px}.person-large strong,.person-large small{display:block}.person-large strong{font-size:12px}.person-large small{color:var(--muted);font-size:8px}.contact-row{display:flex;align-items:center;gap:9px;padding:11px 0;border-top:1px solid var(--line)}.contact-row svg{color:var(--forest-2)}.contact-row small,.contact-row strong{display:block}.contact-row small{color:var(--muted);font-size:7px}.contact-row strong{font-size:9px}.admin-card>.button{width:100%;margin-top:15px}.scope-card h3{margin-bottom:12px;font-size:13px}.scope-card p{display:flex;align-items:center;gap:7px;padding:7px 0;color:var(--muted);font-size:9px}.scope-card svg{color:var(--forest-2)}@media(max-width:850px){.settings-grid{grid-template-columns:1fr}}@media(max-width:560px){.company-profile{padding:20px}.save-row{align-items:stretch;flex-direction:column}.save-row .button{width:100%}}
</style>
