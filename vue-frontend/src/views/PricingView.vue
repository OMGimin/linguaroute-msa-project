<template>
  <div>
    <PublicHeader />
    <main class="container pricing">
      <div class="pricing-head">
        <p class="eyebrow">Plans for growing teams</p>
        <h1>팀의 규모에 맞는<br>단순하고 투명한 요금제</h1>
        <p>모든 요금제에는 AI 추천, 학습 현황, 직원 초대 기능이 포함됩니다.</p>
        <div class="billing-toggle">
          <button :class="{active:monthly}" @click="monthly=true">월간 결제</button>
          <button :class="{active:!monthly}" @click="monthly=false">연간 결제</button>
        </div>
      </div>
      <div class="plan-grid">
        <article v-for="plan in plans" :key="plan.name" class="plan-card" :class="{featured:plan.featured}">
          <span v-if="plan.featured" class="recommended">가장 많이 선택</span>
          <div><p>{{ plan.eyebrow }}</p><h2>{{ plan.name }}</h2><span class="seat">최대 {{ plan.seats }}명</span></div>
          <div class="price"><strong>{{ formatPlanPrice(monthly ? plan.prices.MONTHLY : plan.prices.YEARLY) }}</strong><span>원 / {{ monthly?'월':'년' }}</span></div>
          <p class="plan-desc">{{ plan.description }}</p>
          <router-link class="button" :class="plan.featured?'accent':'primary'" to="/signup/company">이 요금제로 시작하기 <ArrowRight :size="16" /></router-link>
          <ul><li v-for="feature in plan.features" :key="feature"><CircleCheck :size="16" />{{ feature }}</li></ul>
        </article>
      </div>
      <p class="pricing-note"><ShieldCheck :size="17" /> {{ sourceMessage }}</p>
    </main>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { ArrowRight, CircleCheck, ShieldCheck } from '@lucide/vue'
import PublicHeader from '@/components/PublicHeader.vue'
import { subscriptionApi } from '@/api/subscription.js'
import { formatPlanPrice, groupPlanPrices, PUBLIC_PLAN_FALLBACK } from '@/domain/pricing.js'
import { useAuthStore } from '@/store/auth.js'

const monthly = ref(true)
const plans = ref(groupPlanPrices(PUBLIC_PLAN_FALLBACK))
const sourceMessage = ref('로그인 전에는 현재 공개 기준 요금제를 표시합니다.')
const auth = useAuthStore()

onMounted(async () => {
  if (!auth.isAuthenticated) return
  try {
    const response = await subscriptionApi.getPlans()
    plans.value = groupPlanPrices(response.data.data)
    sourceMessage.value = '요금제 API에서 조회한 현재 구독 조건입니다.'
  } catch (error) {
    if (error.response?.status !== 401) sourceMessage.value = '요금제 API를 일시적으로 조회하지 못해 현재 공개 기준을 표시합니다.'
  }
})
</script>

<style scoped>
.pricing{padding:80px 0 100px}.pricing-head{text-align:center}.pricing-head .eyebrow{justify-content:center}.pricing-head h1{margin:16px 0;font-family:var(--font-display);font-size:clamp(38px,5vw,58px);line-height:1.08;letter-spacing:-.055em}.pricing-head>p{color:var(--muted);font-size:14px}.billing-toggle{width:max-content;display:flex;margin:30px auto 48px;padding:4px;background:var(--surface-2);border:1px solid var(--line);border-radius:12px}.billing-toggle button{padding:9px 14px;color:var(--muted);background:transparent;border-radius:8px;font-size:11px;font-weight:700}.billing-toggle button.active{color:var(--ink);background:var(--surface);box-shadow:var(--shadow-sm)}.plan-grid{max-width:820px;display:grid;grid-template-columns:repeat(2,1fr);gap:14px;margin:0 auto}.plan-card{position:relative;display:flex;flex-direction:column;padding:28px;background:var(--surface);border:1px solid var(--line);border-radius:20px}.plan-card.featured{color:white;background:var(--forest);border-color:var(--forest);transform:translateY(-12px);box-shadow:var(--shadow-md)}.plan-card>div:first-of-type>p{color:var(--forest-2);font-family:var(--font-display);font-size:9px;font-weight:800;letter-spacing:.12em}.featured>div:first-of-type>p{color:var(--lime)}.plan-card h2{margin:7px 0 4px;font-size:21px}.seat{color:var(--muted);font-size:10px}.featured .seat,.featured .plan-desc{color:#b8cbbf}.price{display:flex;align-items:flex-end;gap:5px;margin:30px 0 10px}.price strong{font-family:var(--font-display);font-size:35px;letter-spacing:-.05em}.price span{margin-bottom:7px;color:var(--muted);font-size:10px}.featured .price span{color:#b8cbbf}.plan-desc{min-height:43px;color:var(--muted);font-size:11px;line-height:1.7}.plan-card .button{width:100%;margin:24px 0}.plan-card ul{display:grid;gap:12px;margin:0;padding:0;list-style:none}.plan-card li{display:flex;align-items:center;gap:8px;font-size:11px}.plan-card li svg{color:var(--forest-2)}.featured li svg{color:var(--lime)}.recommended{position:absolute;top:-11px;right:20px;padding:5px 9px;color:var(--ink);background:var(--lime);border-radius:7px;font-size:9px;font-weight:800}.pricing-note{display:flex;align-items:center;justify-content:center;gap:8px;margin-top:42px;color:var(--muted);font-size:11px}@media(max-width:800px){.plan-grid{grid-template-columns:1fr}.plan-card.featured{transform:none}.plan-desc{min-height:auto}}
</style>
