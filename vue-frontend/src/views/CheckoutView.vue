<template>
  <AppShell>
    <PageHeader eyebrow="Subscription checkout" title="구독 결제" description="요금제와 좌석을 확인하고 안전한 모의 결제로 구독을 시작하세요." />
    <div class="checkout-steps"><span :class="{active:step>=1}"><i>1</i>요금제 확인</span><b></b><span :class="{active:step>=2}"><i>2</i>결제 정보</span><b></b><span :class="{active:step>=3}"><i>3</i>결제 결과</span></div>

    <div class="checkout-layout">
      <main class="panel checkout-main">
        <template v-if="step===1">
          <p class="eyebrow">Select plan</p><h2>구독할 요금제를 확인하세요</h2><p class="lead">월간 또는 연간 주기를 선택할 수 있으며 결제 완료 후 즉시 좌석이 활성화됩니다.</p>
          <div class="cycle-tabs"><button :class="{active:cycle==='MONTHLY'}" @click="cycle='MONTHLY'">월간 결제</button><button :class="{active:cycle==='YEARLY'}" @click="cycle='YEARLY'">연간 결제 <span>2개월 절약</span></button></div>
          <div class="plan-select"><button v-for="plan in plans" :key="plan.name" :class="{selected:selectedPlan.name===plan.name}" @click="selectedPlan=plan"><span><strong>{{ plan.name }}</strong><small>최대 {{ plan.seats }}명 · 모든 강의와 AI 추천</small></span><b>₩{{ cycle==='MONTHLY'?plan.monthly:plan.yearly }}</b><CircleCheck v-if="selectedPlan.name===plan.name" :size="18"/></button></div>
        </template>

        <template v-else-if="step===2">
          <p class="eyebrow">Test payment</p><h2>결제 정보를 입력하세요</h2><p class="lead">MVP에서는 실제 카드정보를 받지 않고 테스트용 결제 토큰으로 결제 API를 호출합니다.</p>
          <div class="mock-banner"><FlaskConical :size="20"/><span><strong>모의 결제 환경</strong>결제 토큰은 mock-success 또는 mock-failure만 사용하며, 중복 요청은 서버의 멱등성 키로 보호합니다.</span></div>
          <div class="field"><label>결제 결과 미리보기</label><div class="payment-options"><button v-for="option in paymentOptions" :key="option.value" :class="{selected:paymentToken===option.value}" @click="paymentToken=option.value"><component :is="option.icon" :size="18"/><span><strong>{{ option.label }}</strong><small>{{ option.description }}</small></span></button></div></div>
          <label class="agreement"><input v-model="reuseIdempotencyKey" type="checkbox"/><span>동일한 Idempotency-Key를 재사용해 중복 요청 보호 상태 확인</span></label>
          <div class="form-grid"><div class="field"><label>결제 담당자</label><input class="input" :value="payer.name" readonly/></div><div class="field"><label>결제 안내 이메일</label><input class="input" :value="payer.email" readonly/></div></div>
          <label class="agreement"><input type="checkbox" checked/><span>선택한 결제 주기에 따라 자동 갱신되는 것에 동의합니다.</span></label>
        </template>

        <template v-else>
          <div class="result-state" :class="paymentResult">
            <span><component :is="resultContent.icon" :size="31"/></span><p class="eyebrow">{{ resultContent.eyebrow }}</p><h2>{{ resultContent.title }}</h2><p>{{ resultContent.description }}</p>
            <div class="result-details"><div><small>planPriceId</small><strong>{{ selectedPlan.priceIds[cycle] }}</strong></div><div><small>결제 금액</small><strong>₩{{ price }}</strong></div><div><small>멱등성 키</small><strong>{{ idempotencyKey }}</strong></div><div><small>상태</small><span class="tag" :class="paymentResult==='success'?'':paymentResult==='failed'?'red':'amber'">{{ resultContent.status }}</span></div></div>
            <div v-if="paymentResult==='success'" class="event-route"><span class="done">PaymentCompleted</span><i></i><span class="done">구독 ACTIVE</span><i></i><span class="done">좌석 {{ selectedPlan.seats }}석</span></div>
          <div v-else class="error-help"><Info :size="17"/> {{ paymentFeedback || (paymentResult==='failed'?'결제수단을 확인한 후 다시 시도할 수 있습니다.':'동일 요청으로 새 결제를 만들지 않고 기존 결과를 반환했습니다.') }}</div>
          </div>
        </template>

        <div class="checkout-actions"><button v-if="step>1&&step<3" class="button" @click="step--"><ArrowLeft :size="16"/> 이전</button><button v-if="step<2" class="button accent" @click="step=2">결제 정보 입력 <ArrowRight :size="16"/></button><button v-else-if="step===2" class="button accent" :disabled="submitting" @click="completePayment">{{ submitting ? '결제 처리 중' : `₩${price} 결제하기` }} <LockKeyhole :size="15"/></button><router-link v-else-if="paymentResult==='success'" class="button accent" to="/company/subscription">구독 현황 보기 <ArrowRight :size="16"/></router-link><button v-else class="button primary" @click="step=2">다시 시도</button></div>
      </main>

      <aside class="order-summary card"><p>결제 요약</p><div class="summary-plan"><span class="plan-mark">LR</span><span><strong>{{ selectedPlan.name }}</strong><small>{{ cycle==='MONTHLY'?'월간':'연간' }} · {{ selectedPlan.seats }}석</small></span></div><div class="summary-lines"><span>구독 금액 <b>₩{{ price }}</b></span><span>부가세 <b>포함</b></span><span>오늘 결제 <strong>₩{{ price }}</strong></span></div><div class="entitlement"><ShieldCheck :size="18"/><span><strong>결제 완료 즉시 활성화</strong>초대와 수강신청 권한이 바로 열립니다.</span></div><small>같은 멱등성 키로 중복 결제되지 않습니다.</small></aside>
    </div>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { CircleCheck, FlaskConical, CircleX, CopyCheck, ArrowLeft, ArrowRight, LockKeyhole, Info, ShieldCheck } from '@lucide/vue'
import AppShell from '@/components/AppShell.vue'
import PageHeader from '@/components/PageHeader.vue'
import { authApi } from '@/api/auth.js'
import { subscriptionApi } from '@/api/subscription.js'

const plans=ref([])
const payer=ref({name:'로그인 사용자',email:'-'})
const step=ref(1),cycle=ref('MONTHLY'),selectedPlan=ref(plans.value[0]||{name:'요금제 불러오는 중',seats:0,monthly:'-',yearly:'-',priceIds:{}}),paymentToken=ref('mock-success'),paymentResult=ref('success'),reuseIdempotencyKey=ref(false),idempotencyKey=ref(''),submitting=ref(false),paymentFeedback=ref('')
const paymentOptions=[{value:'mock-success',label:'결제 성공',description:'PaymentCompleted 상태 확인',icon:CircleCheck},{value:'mock-failure',label:'결제 실패',description:'PaymentFailed 상태 확인',icon:CircleX}]
const price=computed(()=>cycle.value==='MONTHLY'?selectedPlan.value.monthly:selectedPlan.value.yearly)
const resultContent=computed(()=>paymentResult.value==='success'?{icon:CircleCheck,eyebrow:'Payment completed',title:'구독 결제가 완료되었습니다',description:'기업 학습 공간과 좌석이 활성화되었습니다. 이제 직원을 초대할 수 있습니다.',status:'ACTIVE'}:paymentResult.value==='failed'?{icon:CircleX,eyebrow:'Payment failed',title:'결제를 완료하지 못했습니다',description:'결제 승인에 실패했습니다. 구독 권한과 좌석은 활성화되지 않았습니다.',status:'PAYMENT_FAILED'}:{icon:CopyCheck,eyebrow:'Duplicate protected',title:'이미 처리된 결제 요청입니다',description:'중복 결제를 만들지 않고 이전에 완료된 결제 결과를 불러왔습니다.',status:'DUPLICATE'} )
onMounted(async()=>{
  try {
    const me=(await authApi.getMe()).data.data
    payer.value={name:me.name,email:me.email}
  } catch (_) { paymentFeedback.value='결제 담당자 정보를 불러오지 못했습니다.' }
  try {
    const prices=(await subscriptionApi.getPlans()).data.data
    const grouped=new Map()
    prices.forEach((item)=>{
      const plan=grouped.get(item.planName)||{name:item.planName,seats:item.seatLimit,monthly:'-',yearly:'-',priceIds:{}}
      plan.seats=item.seatLimit
      plan.priceIds[item.billingCycle]=item.planPriceId
      plan[item.billingCycle==='MONTHLY'?'monthly':'yearly']=Number(item.price).toLocaleString()
      grouped.set(item.planName,plan)
    })
    plans.value=[...grouped.values()]
    selectedPlan.value=plans.value[0]||selectedPlan.value
  } catch (_) { plans.value=[];paymentFeedback.value='요금제 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.' }
})
async function completePayment(){
  if(!selectedPlan.value.priceIds[cycle.value]){paymentFeedback.value='결제 가능한 요금제를 먼저 불러와 주세요.';return}
  idempotencyKey.value=reuseIdempotencyKey.value&&idempotencyKey.value?idempotencyKey.value:crypto.randomUUID()
  submitting.value=true;paymentFeedback.value=''
  try {
    const result=(await subscriptionApi.subscribe(selectedPlan.value.priceIds[cycle.value],paymentToken.value,idempotencyKey.value)).data.data
    paymentResult.value=result.status==='ACTIVE'?'success':'failed'
    paymentFeedback.value=result.status==='ACTIVE'?'결제와 구독 상태가 서버에 저장되었습니다.':'구독 상태를 확인해 주세요.'
  } catch(error) { paymentResult.value='failed'; paymentFeedback.value=error.response?.data?.message||'결제 요청을 처리하지 못했습니다.' }
  finally { submitting.value=false;step.value=3 }
}
</script>

<style scoped>
.checkout-steps{width:min(560px,100%);display:flex;align-items:center;margin:0 auto 20px}.checkout-steps span{display:flex;align-items:center;gap:7px;color:var(--subtle);font-size:9px;font-weight:700;white-space:nowrap}.checkout-steps span.active{color:var(--forest)}.checkout-steps i{width:25px;height:25px;display:grid;place-items:center;background:var(--surface-2);border:1px solid var(--line);border-radius:50%;font-style:normal}.checkout-steps .active i{color:var(--ink);background:var(--lime);border-color:var(--lime)}.checkout-steps b{height:1px;flex:1;margin:0 10px;background:var(--line)}.checkout-layout{display:grid;grid-template-columns:minmax(0,1fr) 280px;gap:14px;max-width:980px;margin:auto}.checkout-main{padding:26px}.checkout-main h2{margin:10px 0 6px;font-size:22px}.lead{margin-bottom:20px;color:var(--muted);font-size:10px}.cycle-tabs{display:grid;grid-template-columns:1fr 1fr;padding:4px;background:var(--surface-2);border-radius:11px}.cycle-tabs button{padding:9px;color:var(--muted);background:transparent;border-radius:8px;font-size:10px;font-weight:700}.cycle-tabs button.active{color:var(--forest);background:var(--surface);box-shadow:var(--shadow-sm)}.cycle-tabs span{margin-left:5px;color:var(--forest-2)}.plan-select{display:grid;gap:8px;margin-top:13px}.plan-select button{display:grid;grid-template-columns:1fr auto auto;align-items:center;gap:12px;padding:14px;background:var(--surface);border:1px solid var(--line);border-radius:12px;text-align:left}.plan-select button.selected{background:var(--lime-soft);border-color:#a8ca62}.plan-select strong,.plan-select small{display:block}.plan-select small{margin-top:3px;color:var(--muted);font-size:8px}.plan-select button>b{font-size:11px}.plan-select button>svg{color:var(--forest)}.mock-banner{display:flex;gap:11px;margin-bottom:18px;padding:13px;color:var(--purple);background:var(--purple-soft);border-radius:11px;font-size:9px}.mock-banner span,.mock-banner strong{display:block}.payment-options{display:grid;grid-template-columns:repeat(2,1fr);gap:8px}.payment-options button{display:flex;align-items:flex-start;gap:8px;padding:12px;color:var(--muted);background:var(--surface);border:1px solid var(--line);border-radius:10px;text-align:left}.payment-options button.selected{color:var(--forest);background:var(--mint);border-color:#a9cdb7}.payment-options strong,.payment-options small{display:block}.payment-options strong{font-size:9px}.payment-options small{margin-top:3px;font-size:7px}.agreement{display:flex;align-items:center;gap:8px;color:var(--muted);font-size:9px}.checkout-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:24px}.order-summary{height:max-content;padding:21px}.order-summary>p{margin-bottom:16px;font-size:11px;font-weight:700}.summary-plan{display:flex;align-items:center;gap:10px}.plan-mark{width:38px;height:38px;display:grid;place-items:center;color:var(--ink);background:var(--lime);border-radius:10px;font-family:var(--font-display);font-weight:800}.summary-plan strong,.summary-plan small{display:block}.summary-plan strong{font-size:11px}.summary-plan small{color:var(--muted);font-size:8px}.summary-lines{display:grid;gap:10px;margin:18px 0;padding:15px 0;border-top:1px solid var(--line);border-bottom:1px solid var(--line)}.summary-lines span{display:flex;justify-content:space-between;color:var(--muted);font-size:9px}.summary-lines b{color:var(--ink)}.summary-lines span:last-child{color:var(--ink);font-weight:700}.summary-lines strong{font-size:13px}.entitlement{display:flex;gap:9px;padding:11px;color:var(--forest);background:var(--mint);border-radius:10px;font-size:8px}.entitlement span,.entitlement strong{display:block}.order-summary>small{display:block;margin-top:12px;color:var(--subtle);font-size:7px;text-align:center}.result-state{display:grid;justify-items:center;padding:12px 0;text-align:center}.result-state>span:first-child{width:60px;height:60px;display:grid;place-items:center;color:var(--forest);background:var(--lime);border-radius:18px}.result-state.failed>span:first-child{color:var(--danger);background:var(--danger-soft)}.result-state.duplicate>span:first-child{color:#806018;background:var(--amber-soft)}.result-state .eyebrow{margin-top:16px}.result-state h2{font-size:23px}.result-state>p:not(.eyebrow){max-width:480px;color:var(--muted);font-size:10px}.result-details{width:100%;display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin:22px 0;text-align:left}.result-details div{padding:11px;background:var(--surface-2);border-radius:9px}.result-details small,.result-details strong{display:block}.result-details small{margin-bottom:4px;color:var(--muted);font-size:7px}.result-details strong{font-size:9px;overflow-wrap:anywhere}.event-route{display:flex;align-items:center;width:100%;margin-bottom:6px}.event-route span{padding:6px 9px;color:var(--forest);background:var(--mint);border-radius:7px;font-size:8px;font-weight:700}.event-route i{height:1px;flex:1;background:#afd1bb}.error-help{display:flex;align-items:center;gap:8px;padding:11px;color:var(--muted);background:var(--surface-2);border-radius:9px;font-size:9px}
@media(max-width:820px){.checkout-layout{grid-template-columns:1fr}.order-summary{order:-1}.payment-options{grid-template-columns:1fr}.result-details{grid-template-columns:1fr 1fr}}@media(max-width:520px){.checkout-steps span{font-size:0}.checkout-main{padding:22px}.checkout-actions{align-items:stretch;flex-direction:column}.event-route{align-items:stretch;flex-direction:column;gap:5px}.event-route i{width:1px;height:10px}.result-details{grid-template-columns:1fr}}
</style>
