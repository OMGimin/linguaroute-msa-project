<template>
  <AppShell>
    <PageHeader eyebrow="Account & access" title="내 정보" description="프로필, 비밀번호와 약관 동의를 관리하세요." />
    <div class="profile-layout">
      <aside class="profile-nav panel">
        <div class="identity"><span class="avatar">{{ user.name?.[0] || '?' }}</span><span><strong>{{ user.name }}</strong><small>{{ user.email }}</small></span></div>
        <button v-for="item in tabs" :key="item.id" :class="{ active: tab === item.id }" @click="tab = item.id"><component :is="item.icon" :size="16" />{{ item.label }}</button>
      </aside>

      <section class="panel profile-panel">
        <template v-if="tab === 'profile'">
          <div class="section-title"><div><h2>기본 정보</h2><p>현재 백엔드 계약에서는 이름만 수정할 수 있습니다.</p></div><span class="tag">{{ user.status }}</span></div>
          <form class="form-stack" @submit.prevent="saveProfile">
            <div class="field"><label>이름</label><input v-model.trim="profileName" class="input" maxlength="100" required /></div>
            <div class="field"><label>로그인 이메일</label><input class="input" :value="user.email" disabled /><small>이메일 변경은 MVP 범위에 포함되지 않습니다.</small></div>
            <div class="account-grid"><div><small>비즈니스 역할</small><strong>{{ user.businessRole }}</strong></div><div><small>Auth 호환 역할</small><strong>{{ user.role }}</strong></div><div><small>소속 기업 ID</small><strong>{{ user.companyId ?? '없음' }}</strong></div><div><small>가입일</small><strong>{{ user.createdAt.slice(0, 10) }}</strong></div></div>
            <p v-if="profileMessage" class="message"><CircleCheck :size="15" />{{ profileMessage }}</p><button class="button primary">이름 저장</button>
          </form>
        </template>

        <template v-else-if="tab === 'security'">
          <div class="section-title"><div><h2>비밀번호 변경</h2><p>현재 비밀번호를 확인한 후 새 비밀번호로 변경합니다.</p></div></div>
          <form class="form-stack narrow" @submit.prevent="savePassword"><div class="field"><label>현재 비밀번호</label><input v-model="currentPassword" class="input" type="password" required /></div><div class="field"><label>새 비밀번호</label><input v-model="newPassword" class="input" type="password" minlength="8" maxlength="72" required /><small>8자 이상 72자 이하</small></div><div class="field"><label>새 비밀번호 확인</label><input v-model="newPasswordConfirm" class="input" type="password" minlength="8" maxlength="72" required /></div><p v-if="passwordSaved" class="message"><CircleCheck :size="15" />비밀번호가 변경되었습니다.</p><button class="button primary">비밀번호 변경</button></form>
        </template>

        <template v-else-if="tab === 'agreements'">
          <div class="section-title"><div><h2>약관 및 동의</h2><p>활성 약관의 버전과 동의 상태를 확인합니다.</p></div></div>
          <div class="term-list"><label v-for="term in terms" :key="term.id"><input v-model="term.agreed" type="checkbox" :disabled="term.required" /><span><strong>{{ term.required ? '[필수]' : '[선택]' }} {{ term.title }}</strong><small>버전 {{ term.version }} · 시행 {{ term.effectiveAt }}</small></span><span class="tag" :class="term.required ? '' : 'gray'">ID {{ term.id }}</span></label></div>
          <p class="contract-help"><Info :size="15" />선택 약관은 <code>POST /api/users/me/agreements</code>의 agreementIds로 저장됩니다.</p>
        </template>

        <template v-else>
          <div class="danger-zone"><span><UserRoundX :size="24" /></span><h2>회원 탈퇴</h2><p>탈퇴하면 상태가 <b>WITHDRAWN</b>으로 변경되고 기존 Access Token으로도 보호 API를 사용할 수 없습니다.</p><label class="confirm"><input v-model="withdrawConfirmed" type="checkbox" />탈퇴 후 학습 기록과 서비스 접근이 제한되는 것을 확인했습니다.</label><button class="button danger" :disabled="!withdrawConfirmed" @click="withdrawMe">회원 탈퇴 요청</button><div v-if="withdrawn" class="withdraw-result"><TriangleAlert :size="16" />탈퇴 처리 후 저장된 Access Token을 삭제하고 로그인 화면으로 이동합니다.</div></div>
        </template>
      </section>
    </div>
  </AppShell>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { CircleCheck, FileCheck2, Info, LockKeyhole, TriangleAlert, UserRound, UserRoundX } from '@lucide/vue'
import AppShell from '@/components/AppShell.vue'
import PageHeader from '@/components/PageHeader.vue'
import { authApi } from '@/api/auth.js'
import { unwrapApiData } from '@/constants/domain.js'

const tab = ref('profile')
const profileName = ref('')
const profileMessage = ref('')
const passwordSaved = ref(false)
const withdrawConfirmed = ref(false)
const withdrawn = ref(false)
const currentPassword = ref('')
const newPassword = ref('')
const newPasswordConfirm = ref('')
const user = reactive({ id: null, email: '', name: '', role: '', businessRole: '', companyId: null, status: '', createdAt: '' })
const terms = reactive([])
const tabs = [{ id: 'profile', label: '기본 정보', icon: UserRound }, { id: 'security', label: '비밀번호 변경', icon: LockKeyhole }, { id: 'agreements', label: '약관 및 동의', icon: FileCheck2 }, { id: 'withdraw', label: '회원 탈퇴', icon: UserRoundX }]

onMounted(async () => {
  try {
    const userData = unwrapApiData(await authApi.getMe())
    Object.assign(user, userData)
    profileName.value = user.name
    const activeTerms = unwrapApiData(await authApi.getActiveTerms())
    terms.splice(0, terms.length, ...activeTerms.map((term) => ({ ...term, title: term.content, effectiveAt: term.effectiveAt?.slice(0, 10), agreed: term.required })))
  } catch (error) {
    profileMessage.value = error.response?.data?.message || '내 정보를 불러오지 못했습니다.'
  }
})

async function saveProfile() {
  Object.assign(user, unwrapApiData(await authApi.updateMe(profileName.value)))
  profileMessage.value = '이름이 저장되었습니다.'
}

async function savePassword() {
  if (newPassword.value !== newPasswordConfirm.value) return
  await authApi.changePassword(currentPassword.value, newPassword.value)
  passwordSaved.value = true
}

async function withdrawMe() {
  await authApi.withdrawMe()
  withdrawn.value = true
}
</script>

<style scoped>
.profile-layout{display:grid;grid-template-columns:230px 1fr;gap:16px}.profile-nav{height:max-content;padding:14px}.identity{display:flex;align-items:center;gap:10px;padding:10px 7px 20px;margin-bottom:8px;border-bottom:1px solid var(--line)}.identity .avatar{width:42px;height:42px}.identity strong,.identity small{display:block}.identity strong{font-size:12px}.identity small{margin-top:2px;color:var(--muted);font-size:8px}.profile-nav button{width:100%;display:flex;align-items:center;gap:9px;padding:11px;color:var(--muted);background:transparent;border-radius:9px;font-size:10px;font-weight:700;text-align:left}.profile-nav button.active{color:var(--forest);background:var(--mint)}.profile-panel{min-height:550px;padding:30px}.section-title{display:flex;align-items:flex-start;justify-content:space-between;margin-bottom:26px}.section-title h2{font-size:19px}.section-title p{margin-top:5px;color:var(--muted);font-size:9px}.profile-panel form{max-width:680px}.narrow{max-width:520px!important}.field small{color:var(--muted);font-size:8px}.account-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:10px}.account-grid div{padding:14px;background:var(--surface-2);border-radius:10px}.account-grid small,.account-grid strong{display:block}.account-grid small{margin-bottom:4px;color:var(--muted);font-size:8px}.account-grid strong{font-size:10px}.message{display:flex;align-items:center;gap:6px;color:var(--forest-2);font-size:9px}.term-list{border:1px solid var(--line);border-radius:13px}.term-list label{display:flex;align-items:center;gap:11px;padding:16px;border-bottom:1px solid var(--line)}.term-list label:last-child{border-bottom:0}.term-list label>span:nth-child(2){flex:1}.term-list strong,.term-list small{display:block}.term-list strong{font-size:10px}.term-list small{margin-top:4px;color:var(--muted);font-size:8px}.contract-help{display:flex;align-items:center;gap:7px;margin-top:15px;color:var(--muted);font-size:9px}.contract-help code{color:var(--forest-2)}.danger-zone{max-width:620px;padding:26px;color:var(--danger);background:var(--danger-soft);border-radius:16px}.danger-zone>span{width:50px;height:50px;display:grid;place-items:center;background:white;border-radius:14px}.danger-zone h2{margin:17px 0 8px;font-size:19px}.danger-zone>p{color:#825852;font-size:10px;line-height:1.7}.confirm{display:flex;gap:8px;margin:20px 0;color:#825852;font-size:9px}.danger-zone .button:disabled{opacity:.4}.withdraw-result{display:flex;gap:7px;margin-top:14px;font-size:9px}@media(max-width:760px){.profile-layout{grid-template-columns:1fr}.profile-nav{display:flex;overflow-x:auto}.identity{display:none}.profile-nav button{width:auto;white-space:nowrap}.account-grid{grid-template-columns:1fr}.profile-panel{padding:22px}}
</style>
