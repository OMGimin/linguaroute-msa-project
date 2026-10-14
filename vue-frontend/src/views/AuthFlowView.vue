<template>
  <div class="flow-page">
    <header><BrandLogo /><router-link to="/login">이미 계정이 있나요? <strong>로그인</strong></router-link></header>
    <main>
      <div class="flow-intro">
        <p class="eyebrow">{{ config.eyebrow }}</p><h1>{{ config.title }}</h1><p>{{ config.description }}</p>
        <div v-if="mode.includes('signup')" class="steps"><span v-for="n in 3" :key="n" :class="{ active:n===step, done:n<step }"><i>{{ n<step?'✓':n }}</i>{{ config.steps[n-1] }}</span></div>
      </div>

      <section class="form-card card">
        <template v-if="mode==='company-signup'">
          <form class="form-stack" @submit.prevent="handleCompanyNext">
            <template v-if="step===1"><div class="form-grid"><div class="field"><label>기업명</label><input v-model.trim="companyForm.company.name" class="input" maxlength="100" required placeholder="예: 스칼라테크" /></div><div class="field"><label>사업자등록번호</label><input v-model="companyForm.company.businessNumber" class="input" required pattern="\d{3}-?\d{2}-?\d{5}" placeholder="000-00-00000" /></div></div><div class="field"><label>관리자 이름</label><input v-model.trim="companyForm.admin.name" class="input" maxlength="100" required placeholder="이름을 입력하세요" /></div><div class="field"><label>업무용 이메일</label><div class="verify-row"><input v-model.trim="companyForm.admin.email" class="input" type="email" required placeholder="name@company.com" /><button class="button small" type="button" @click="requestCompanyVerification">인증번호 발송</button></div></div></template>
            <template v-else-if="step===2"><div class="notice"><MailCheck :size="20" /><span><strong>이메일을 확인해 주세요</strong>{{ companyForm.admin.email }}로 6자리 인증번호를 보냈습니다.</span></div><div class="field"><label>인증번호</label><div class="verify-row"><input v-model="verificationCode" class="input verification-code" maxlength="6" inputmode="numeric" required placeholder="6자리 숫자" /><button class="button small" type="button" :disabled="!verificationSent" @click="confirmCompanyVerification">인증 확인</button></div><small v-if="companyForm.emailVerificationToken" class="success-text"><CircleCheck :size="14" /> 이메일 인증이 완료되었습니다.</small><small v-else class="timer">15분 동안 한 번만 사용할 수 있습니다.</small></div><div class="form-grid"><div class="field"><label>비밀번호</label><input v-model="companyForm.admin.password" class="input" type="password" minlength="8" maxlength="72" required /></div><div class="field"><label>비밀번호 확인</label><input v-model="passwordConfirm" class="input" type="password" minlength="8" maxlength="72" required /></div></div></template>
            <template v-else><div class="agreement-box"><label v-for="item in agreements" :key="item.id" class="agreement"><input v-model="companyForm.agreementIds" type="checkbox" :value="item.id" /><span><strong>{{ item.required?'[필수]':'[선택]' }} {{ item.label }}</strong><small>약관 ID {{ item.id }} · 버전 {{ item.version }}</small></span><ChevronRight :size="17" /></label></div><div class="payload-preview"><strong>가입 요청 계약</strong><code>company + admin + emailVerificationToken + agreementIds</code></div></template>
            <p v-if="formError" class="form-error"><AlertTriangle :size="15" />{{ formError }}</p>
            <div class="form-actions"><button v-if="step>1" class="button" type="button" @click="previousStep"><ArrowLeft :size="16" /> 이전</button><button class="button primary" type="submit">{{ step===3?'기업 계정 만들기':'다음 단계' }} <ArrowRight :size="16" /></button></div>
          </form>
        </template>

        <template v-else-if="mode==='employee-signup'">
          <form class="form-stack" @submit.prevent="handleEmployeeNext">
            <template v-if="step===1">
              <div class="invite-check"><TicketCheck :size="24" /><div><strong>초대코드를 먼저 확인할게요</strong><p>기업 관리자가 전달한 8자리 코드를 입력하세요.</p></div></div>
              <div class="field"><label>초대코드</label><div class="verify-row"><input v-model="invitationCode" class="input invite-input" /><button type="button" class="button small accent" @click="validateInvitation">코드 확인</button></div></div>
              <div v-if="inviteStatus==='valid'" class="notice success"><CircleCheck :size="18" /><span><strong>초대코드를 입력했습니다</strong>가입 요청 시 코드의 유효 기간, 사용 여부와 잔여 좌석을 다시 확인합니다.</span></div>
              <div v-else-if="inviteStatus" class="notice error"><AlertTriangle :size="18" /><span><strong>{{ inviteError.title }}</strong>{{ inviteError.description }}</span></div>
            </template>
            <template v-else-if="step===2">
              <div class="form-grid"><div class="field"><label>이름</label><input v-model.trim="employeeForm.name" class="input" required maxlength="100" placeholder="이름" /></div><div class="field"><label>업무용 이메일</label><div class="verify-row"><input v-model.trim="employeeForm.email" class="input" type="email" required placeholder="name@company.com" /><button type="button" class="button small" @click="requestEmployeeVerification">인증번호 발송</button></div></div></div>
              <div v-if="verificationSent" class="notice"><MailCheck :size="18" /><span><strong>인증번호를 발송했습니다</strong>15분 안에 아래 6자리 인증번호를 입력해 주세요.</span></div>
              <div class="field"><label>이메일 인증번호</label><div class="verify-row"><input v-model.trim="employeeVerificationCode" class="input verification-code" maxlength="6" inputmode="numeric" required placeholder="6자리 숫자" /><button type="button" class="button small" :disabled="!verificationSent" @click="confirmEmployeeVerification">인증 확인</button></div><small v-if="employeeForm.emailVerificationToken" class="success-text"><CircleCheck :size="14" /> 이메일 인증이 완료되었습니다.</small></div>
            </template>
            <template v-else>
              <div class="form-grid"><div class="field"><label>비밀번호</label><input v-model="employeeForm.password" class="input" type="password" minlength="8" maxlength="72" required placeholder="8자 이상" /></div><div class="field"><label>비밀번호 확인</label><input v-model="employeePasswordConfirm" class="input" type="password" minlength="8" maxlength="72" required placeholder="비밀번호를 다시 입력하세요" /></div></div>
              <div class="agreement-box"><label v-for="item in agreements" :key="item.id" class="agreement"><input v-model="employeeForm.agreementIds" type="checkbox" :value="item.id" /><span><strong>{{ item.required?'[필수]':'[선택]' }} {{ item.label }}</strong><small>약관 ID {{ item.id }} · 버전 {{ item.version }}</small></span><ChevronRight :size="17" /></label></div>
              <p v-if="formError" class="form-error"><AlertTriangle :size="15" />{{ formError }}</p>
            </template>
            <div class="form-actions"><button v-if="step>1" class="button" type="button" @click="previousStep"><ArrowLeft :size="16" /> 이전</button><button class="button primary" type="submit" :disabled="step===1 && inviteStatus!=='valid'">{{ step===3?'가입 완료':'다음 단계' }} <ArrowRight :size="16" /></button></div>
          </form>
        </template>

        <template v-else-if="mode==='reset-confirm'">
          <form v-if="!resetDone" class="form-stack" @submit.prevent="confirmPasswordReset">
            <div class="notice success"><ShieldCheck :size="19" /><span><strong>새 비밀번호를 설정해 주세요</strong>이 링크는 15분 동안 한 번만 사용할 수 있습니다.</span></div>
            <div class="field"><label>새 비밀번호</label><input v-model="resetPassword" class="input" type="password" minlength="8" maxlength="72" required /><small class="helper">8자 이상 72자 이하</small></div>
            <div class="field"><label>새 비밀번호 확인</label><input v-model="resetPasswordConfirm" class="input" type="password" minlength="8" maxlength="72" required /></div>
            <p v-if="formError" class="form-error"><AlertTriangle :size="15" />{{ formError }}</p>
            <button class="button primary" type="submit">새 비밀번호 저장 <ArrowRight :size="16" /></button>
          </form>
          <div v-else class="completion-state"><span><CircleCheck :size="28" /></span><h2>비밀번호가 변경되었습니다</h2><p>새 비밀번호로 다시 로그인해 주세요. 기존 로그인 정보는 더 이상 사용할 수 없습니다.</p><router-link class="button primary" to="/login">로그인으로 이동 <ArrowRight :size="16" /></router-link></div>
        </template>

        <template v-else-if="mode==='recovery'">
          <div class="recovery-tabs"><button :class="{active:recoveryTab==='password'}" @click="recoveryTab='password'">비밀번호 재설정</button><button :class="{active:recoveryTab==='id'}" @click="recoveryTab='id'">아이디 찾기</button></div>
          <form class="form-stack" @submit.prevent="requestRecovery"><div v-if="submitted" class="notice success"><MailCheck :size="20" /><span><strong>안내 메일을 발송했습니다</strong>입력한 정보와 일치하는 계정이 있다면 등록된 이메일로 안내가 전송됩니다.</span></div><template v-else-if="recoveryTab==='password'"><div class="field"><label>로그인 이메일</label><input v-model.trim="recoveryEmail" class="input" type="email" required placeholder="name@company.com" /></div><p class="helper">가입된 계정인지 여부는 보안을 위해 화면에 표시하지 않습니다.</p></template><template v-else><div class="field"><label>이름</label><input v-model.trim="recoveryName" class="input" required placeholder="가입 시 입력한 이름" /></div><div class="field"><label>기업 사업자등록번호</label><input v-model.trim="recoveryBusinessNumber" class="input" required placeholder="000-00-00000" /></div><p class="helper">계정 정보는 등록된 로그인 이메일로만 안내됩니다.</p></template><p v-if="formError" class="form-error"><AlertTriangle :size="15" />{{ formError }}</p><button class="button primary" type="submit">{{ submitted?'로그인으로 돌아가기':'안내 메일 받기' }} <ArrowRight :size="16" /></button></form>
        </template>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ArrowLeft, ArrowRight, MailCheck, ChevronRight, TicketCheck, CircleCheck, AlertTriangle, Info, ShieldCheck } from '@lucide/vue'
import BrandLogo from '@/components/BrandLogo.vue'
import { authApi } from '@/api/auth.js'
const props=defineProps({ mode:{type:String,default:'company-signup'} })
const route=useRoute(); const step=ref(1); const submitted=ref(false); const recoveryTab=ref(route.query.tab==='id'?'id':'password'); const invitationCode=ref(''); const inviteStatus=ref(''); const verificationSent=ref(false); const resetDone=ref(false)
const verificationCode=ref(''); const passwordConfirm=ref(''); const formError=ref('')
const companyForm=reactive({company:{name:'',businessNumber:''},admin:{email:'',password:'',name:''},emailVerificationToken:'',agreementIds:[]})
const employeeForm=reactive({invitationCode:'',email:'',password:'',name:'',emailVerificationToken:'',agreementIds:[]}); const employeeVerificationCode=ref(''); const employeePasswordConfirm=ref('')
const recoveryEmail=ref(''); const recoveryName=ref(''); const recoveryBusinessNumber=ref(''); const resetPassword=ref(''); const resetPasswordConfirm=ref('')
const configs={
  'company-signup':{eyebrow:'Company onboarding',title:'기업 학습 공간 만들기',description:'기업 정보와 관리자 계정을 등록하면 바로 요금제를 선택하고 직원을 초대할 수 있습니다.',steps:['기업 정보','이메일 인증','약관 동의']},
  'employee-signup':{eyebrow:'Join your team',title:'초대코드로 참여하기',description:'회사에서 받은 초대코드를 확인하고 나만의 학습 경로를 시작하세요.',steps:['초대 확인','계정 정보','가입 완료']},
  recovery:{eyebrow:'Account recovery',title:'계정 찾기',description:'보안을 지키면서 계정에 다시 접근할 수 있도록 도와드릴게요.',steps:[]},
  'reset-confirm':{eyebrow:'Create new password',title:'새 비밀번호 설정',description:'메일로 받은 링크가 확인되었습니다. 안전한 새 비밀번호를 설정하세요.',steps:[]}
}
const mode=computed(()=>props.mode); const config=computed(()=>configs[mode.value]||configs.recovery)
const agreements=ref([])
async function requestCompanyVerification(){formError.value='';try{if(!companyForm.admin.email){formError.value='이메일을 입력해 주세요.';return}await authApi.requestEmailVerification(companyForm.admin.email);verificationSent.value=true}catch(error){formError.value=error.response?.data?.message||'인증번호를 발송하지 못했습니다.'}}
async function confirmCompanyVerification(){formError.value='';try{if(!verificationCode.value){formError.value='인증번호를 입력해 주세요.';return}const response=await authApi.confirmEmailVerification(companyForm.admin.email,verificationCode.value);companyForm.emailVerificationToken=response.data.data.emailVerificationToken}catch(error){formError.value=error.response?.data?.message||'이메일 인증에 실패했습니다.'}}
async function handleCompanyNext(){ formError.value=''; if(step.value===1&&!verificationSent.value){formError.value='이메일 인증번호를 먼저 발송해 주세요.';return} if(step.value===2){if(!companyForm.emailVerificationToken){formError.value='이메일 인증을 완료해 주세요.';return}if(companyForm.admin.password!==passwordConfirm.value){formError.value='비밀번호가 일치하지 않습니다.';return}} if(step.value<3){step.value+=1;return} if(!agreements.value.filter(item=>item.required).every(item=>companyForm.agreementIds.includes(item.id))){formError.value='필수 약관에 모두 동의해 주세요.';return} try{await authApi.registerCompany(companyForm);window.location.href='/login?reason=signup-complete&next=/company/checkout'}catch(error){formError.value=error.response?.data?.message||'기업 계정을 만들지 못했습니다.'} }
function previousStep(){ if(step.value>1) step.value-=1 }
const inviteErrors={notFound:{title:'초대코드를 찾을 수 없습니다',description:'입력한 코드를 다시 확인해 주세요.'},expired:{title:'초대코드가 만료되었습니다',description:'기업 관리자에게 새로운 초대코드를 요청해 주세요.'},used:{title:'이미 사용된 초대코드입니다',description:'일회용 코드는 한 명만 사용할 수 있습니다.'},seat:{title:'사용 가능한 좌석이 없습니다',description:'기업 관리자가 구독 좌석을 확보한 후 다시 시도해 주세요.'},inactive:{title:'기업 구독이 활성 상태가 아닙니다',description:'구독 상태가 복구된 후 직원 가입을 진행할 수 있습니다.'}}
const inviteError=computed(()=>inviteErrors[inviteStatus.value]||inviteErrors.expired)
async function validateInvitation(){formError.value='';inviteStatus.value='';if(!invitationCode.value.trim()){formError.value='초대코드를 입력해 주세요.';return}employeeForm.invitationCode=invitationCode.value.trim().toUpperCase();try{await authApi.validateInvitation(employeeForm.invitationCode);inviteStatus.value='valid'}catch(error){inviteStatus.value=({INVITATION_NOT_FOUND:'notFound',INVITATION_EXPIRED:'expired',INVITATION_ALREADY_USED:'used',SEAT_LIMIT_EXCEEDED:'seat',SUBSCRIPTION_INACTIVE:'inactive'})[error.response?.data?.code]||'notFound';formError.value=error.response?.data?.message||'초대코드를 확인하지 못했습니다.'}}
async function requestEmployeeVerification(){formError.value='';try{if(!employeeForm.email){formError.value='이메일을 입력해 주세요.';return}await authApi.requestEmailVerification(employeeForm.email);verificationSent.value=true}catch(error){formError.value=error.response?.data?.message||'인증번호를 발송하지 못했습니다.'}}
async function confirmEmployeeVerification(){formError.value='';try{if(!employeeVerificationCode.value){formError.value='인증번호를 입력해 주세요.';return}const response=await authApi.confirmEmailVerification(employeeForm.email,employeeVerificationCode.value);employeeForm.emailVerificationToken=response.data.data.emailVerificationToken}catch(error){formError.value=error.response?.data?.message||'이메일 인증에 실패했습니다.'}}
async function handleEmployeeNext(){formError.value='';if(step.value===1&&inviteStatus.value!=='valid')return;if(step.value===2&&!employeeForm.emailVerificationToken){formError.value='이메일 인증을 완료해 주세요.';return}if(step.value<3){step.value+=1;return}if(employeeForm.password!==employeePasswordConfirm.value){formError.value='비밀번호가 일치하지 않습니다.';return}if(!agreements.value.filter(item=>item.required).every(item=>employeeForm.agreementIds.includes(item.id))){formError.value='필수 약관에 모두 동의해 주세요.';return}try{await authApi.registerEmployee(employeeForm);window.location.href='/login'}catch(error){formError.value=error.response?.data?.message||'직원 계정을 만들지 못했습니다.'}}
async function requestRecovery(){ formError.value=''; try{if(recoveryTab.value==='password')await authApi.requestPasswordReset(recoveryEmail.value);else await authApi.requestIdFind(recoveryName.value,recoveryBusinessNumber.value);submitted.value=true}catch(error){formError.value=error.response?.data?.message||'요청을 처리하지 못했습니다.'} }
async function confirmPasswordReset(){ formError.value=''; if(resetPassword.value!==resetPasswordConfirm.value){formError.value='비밀번호가 일치하지 않습니다.';return} if(!route.query.token){formError.value='재설정 토큰이 없습니다.';return} try{await authApi.confirmPasswordReset(route.query.token,resetPassword.value);resetDone.value=true}catch(error){formError.value=error.response?.data?.message||'비밀번호를 변경하지 못했습니다.'} }
onMounted(async()=>{if(!mode.value.includes('signup'))return;try{const active=(await authApi.getActiveTerms()).data.data;agreements.value=active.map(term=>({id:term.id,required:term.required,label:term.content,version:term.version}))}catch(error){formError.value=error.response?.data?.message||'약관을 불러오지 못했습니다.'}})
</script>

<style scoped>
.flow-page{min-height:100vh;background:var(--paper)}header{height:72px;display:flex;align-items:center;justify-content:space-between;padding:0 max(24px,5vw);background:var(--surface);border-bottom:1px solid var(--line)}header>a:last-child{color:var(--muted);font-size:12px}header>a strong{margin-left:5px;color:var(--forest-2)}main{width:min(760px,calc(100% - 32px));margin:0 auto;padding:62px 0 90px}.flow-intro{text-align:center}.flow-intro .eyebrow{justify-content:center}.flow-intro h1{margin:15px 0 10px;font-family:var(--font-display);font-size:40px;letter-spacing:-.05em}.flow-intro>p:last-of-type{color:var(--muted);font-size:13px}.steps{display:flex;align-items:center;justify-content:center;gap:0;margin:34px auto;width:min(580px,100%)}.steps span{position:relative;display:flex;flex:1;align-items:center;gap:8px;color:var(--subtle);font-size:10px;font-weight:700}.steps span:not(:last-child)::after{height:1px;flex:1;margin:0 10px;content:'';background:var(--line-strong)}.steps i{width:26px;height:26px;display:grid;place-items:center;flex:none;background:var(--surface-2);border:1px solid var(--line);border-radius:50%;font-style:normal}.steps .active{color:var(--forest)}.steps .active i,.steps .done i{color:var(--ink);background:var(--lime);border-color:var(--lime)}.form-card{padding:34px}.verify-row{display:flex;gap:8px}.verify-row .input{flex:1}.form-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:8px}.form-actions .primary{min-width:150px}.notice{display:flex;align-items:flex-start;gap:12px;padding:15px;color:var(--blue);background:var(--blue-soft);border-radius:12px;font-size:11px}.notice strong,.notice span{display:block}.notice strong{margin-bottom:3px;font-size:12px}.notice.success{color:var(--forest);background:var(--mint)}.code-inputs{display:flex;justify-content:center;gap:10px}.code-inputs input{width:48px;height:55px;border:1px solid var(--line-strong);border-radius:11px;font-size:20px;font-weight:700;text-align:center}.timer{display:block;margin-top:7px;color:var(--danger);text-align:center}.agreement-box{border:1px solid var(--line);border-radius:13px}.agreement{display:flex;align-items:center;gap:11px;padding:16px;border-bottom:1px solid var(--line);font-size:11px}.agreement:last-child{border-bottom:0}.agreement span{flex:1}.agreement strong,.agreement small{display:block}.agreement small{margin-top:3px;color:var(--muted)}.agreement.simple{padding:0;border:0}.legal-note,.helper{color:var(--muted);font-size:10px}.invite-check{display:flex;align-items:center;gap:14px;padding:17px;color:var(--forest);background:var(--mint);border-radius:13px}.invite-check strong{font-size:13px}.invite-check p{color:var(--muted);font-size:10px}.invite-input{font-family:var(--font-display);font-size:16px;letter-spacing:.12em;text-transform:uppercase}.success-text{display:flex;align-items:center;gap:5px;margin-top:8px;color:var(--forest-2)}.recovery-tabs{display:grid;grid-template-columns:1fr 1fr;margin:-10px 0 28px;border-bottom:1px solid var(--line)}.recovery-tabs button{padding:13px;color:var(--muted);background:transparent;border-bottom:2px solid transparent;font-size:12px;font-weight:700}.recovery-tabs button.active{color:var(--forest);border-color:var(--forest)}
.notice.error{color:var(--danger);background:var(--danger-soft)}
.state-hint{display:flex;align-items:center;gap:6px;color:var(--subtle);font-size:9px}
.form-actions .button:disabled{cursor:not-allowed;opacity:.45}
.completion-state{display:grid;justify-items:center;padding:10px 0;text-align:center}.completion-state>span{width:62px;height:62px;display:grid;place-items:center;color:var(--forest);background:var(--lime);border-radius:18px}.completion-state h2{margin:19px 0 7px;font-size:21px}.completion-state p{max-width:410px;margin-bottom:22px;color:var(--muted);font-size:10px;line-height:1.7}
.verification-code{font-family:var(--font-display);font-size:19px;letter-spacing:.35em;text-align:center}.payload-preview{display:flex;justify-content:space-between;gap:10px;padding:12px;color:var(--muted);background:var(--surface-2);border-radius:9px;font-size:8px}.payload-preview code{color:var(--forest-2)}.form-error{display:flex;align-items:center;gap:6px;color:var(--danger);font-size:9px}
@media(max-width:600px){main{padding-top:42px}.flow-intro h1{font-size:32px}.steps span{justify-content:center;font-size:0}.steps span:not(:last-child)::after{margin:0 5px}.form-card{padding:22px}.form-grid{grid-template-columns:1fr}.verify-row{align-items:stretch;flex-direction:column}.form-actions .button{flex:1}.code-inputs{gap:5px}.code-inputs input{width:40px;height:48px}}
</style>
