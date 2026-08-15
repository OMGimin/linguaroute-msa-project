<template>
  <AppShell>
    <PageHeader eyebrow="AI learning navigator" title="나에게 맞는 강의 찾기" description="현재 업무와 목표를 알려주시면 실제 등록된 활성 강의 중 가장 적합한 경로를 제안합니다." />
    <div class="recommend-layout">
      <section class="recommend-form panel">
        <div class="step-head"><span>STEP {{ step }} / 3</span><div class="progress"><span :style="`width:${step / 3 * 100}%`"></span></div></div>
        <template v-if="step === 1">
          <h2>어떤 언어를 배우고 싶으세요?</h2><p>현재 수준도 함께 선택해 주세요.</p>
          <div class="choice-grid languages"><button v-for="item in languages" :key="item.label" :class="{ selected: language === item.label }" @click="language = item.label"><span>{{ item.code }}</span><strong>{{ item.label }}</strong><small>{{ item.desc }}</small><CircleCheck v-if="language === item.label" :size="18" /></button></div>
          <h3>현재 수준</h3><div class="segment"><button v-for="item in ['초급','중급','고급']" :key="item" :class="{ active: level === item }" @click="level = item">{{ item }}</button></div>
        </template>
        <template v-else-if="step === 2">
          <h2>업무에서 언제 가장 필요하신가요?</h2><p>직무와 자주 마주치는 비즈니스 상황을 선택해 주세요.</p>
          <div class="field"><label>직무</label><select v-model="job" class="select"><option>글로벌 세일즈</option><option>소프트웨어 개발</option><option>데이터 분석</option><option>프로덕트 관리</option></select></div>
          <div class="choice-grid situations"><button v-for="item in situations" :key="item.label" :class="{ selected: situation === item.label }" @click="situation = item.label"><component :is="item.icon" :size="20" /><strong>{{ item.label }}</strong><small>{{ item.desc }}</small></button></div>
        </template>
        <template v-else>
          <h2>이번 학습으로 이루고 싶은 목표는?</h2><p>업무 상황과 전문용어를 구체적으로 적을수록 더 정확한 추천을 받을 수 있어요.</p>
          <div class="field"><label>학습 목표</label><textarea v-model="goal" maxlength="200" class="textarea"></textarea><small>{{ goal.length }} / 200자</small></div>
          <div class="summary-card"><Sparkles :size="20" /><span><strong>추천 조건 요약</strong>{{ language }} · {{ level }} · {{ job }} · {{ situation }}</span></div>
        </template>
        <div class="step-actions"><button v-if="step > 1" class="button" :disabled="isLoading" @click="previousStep"><ArrowLeft :size="16" /> 이전</button><button class="button accent" :disabled="isLoading" @click="nextStep">{{ step < 3 ? '다음' : isLoading ? '추천 분석 중…' : 'AI 추천 결과 보기' }} <ArrowRight :size="16" /></button></div>
      </section>
      <aside class="guide-card"><Route :size="23" /><h3>좋은 추천을 위한 팁</h3><p>실제로 자주 겪는 상황과 달성하고 싶은 변화를 구체적으로 알려주세요.</p><ul><li>언어와 수준이 일치하는 강의만 추천</li><li>ACTIVE 상태의 실제 강의만 검증</li><li>AI 장애 시 규칙 기반 결과 제공</li></ul></aside>
    </div>

    <section v-if="showResults" class="results">
      <div class="result-title"><div><p class="eyebrow">Your next route</p><h2>이 강의부터 시작해 보세요</h2></div></div>
      <AsyncState v-if="isLoading" type="loading" title="추천 경로를 분석하고 있어요" description="등록된 강의와 현재 학습 이력을 비교하고 있습니다." />
      <AsyncState v-else-if="resultMode === 'error'" type="error" title="추천 결과를 만들지 못했어요" :description="errorMessage" @retry="requestRecommendation" />
      <AsyncState v-else-if="recommendedCourses.length === 0" type="empty" title="추천할 강의가 없어요" description="선택한 언어로 등록된 활성 강의가 있는지 확인해 주세요." />
      <template v-else>
        <div class="result-notice" :class="resultMode"><component :is="resultMode === 'ai' ? Sparkles : ShieldAlert" :size="19" /><span><strong>{{ resultMode === 'ai' ? 'AI 맞춤 추천 결과' : '규칙 기반 추천으로 전환했어요' }}</strong>{{ resultMode === 'ai' ? '입력한 직무·비즈니스 상황·전문용어 목표를 분석해 추천했습니다.' : '외부 AI를 사용하지 못해 언어·수준·상황과 등록 강의 정보를 기준으로 추천했습니다.' }}</span></div>
        <div class="result-grid"><CourseTile v-for="course in recommendedCourses" :key="course.id" :course="course" /></div>
      </template>
    </section>
  </AppShell>
</template>

<script setup>
import { ref } from 'vue'
import { ArrowLeft, ArrowRight, BriefcaseBusiness, CircleCheck, MessagesSquare, Plane, Presentation, Route, ShieldAlert, Sparkles } from '@lucide/vue'
import AppShell from '@/components/AppShell.vue'
import AsyncState from '@/components/AsyncState.vue'
import CourseTile from '@/components/CourseTile.vue'
import PageHeader from '@/components/PageHeader.vue'
import { recommendationApi } from '@/api/recommendation.js'
import { buildRecommendationRequest, mapRecommendedCourses, recommendationMode } from '@/domain/recommendation.js'

const step = ref(1)
const language = ref('영어')
const level = ref('중급')
const situation = ref('고객 미팅')
const job = ref('글로벌 세일즈')
const goal = ref('제품 사양과 기술 용어를 해외 고객에게 자연스럽게 설명하고 질문에 자신 있게 답하고 싶어요.')
const showResults = ref(false)
const resultMode = ref('ai')
const isLoading = ref(false)
const errorMessage = ref('추천 서비스 연결과 등록 강의 검증에 실패했습니다. 잠시 후 다시 시도해 주세요.')
const recommendedCourses = ref([])
const languages = [{ code: 'EN', label: '영어', desc: 'English' }, { code: 'JP', label: '일본어', desc: '日本語' }, { code: 'CN', label: '중국어', desc: '中文' }]
const situations = [{ icon: MessagesSquare, label: '고객 미팅', desc: '고객과 제품·계약 논의' }, { icon: Presentation, label: '업무 발표', desc: '보고와 프레젠테이션' }, { icon: BriefcaseBusiness, label: '협업', desc: '동료·파트너와 협업' }, { icon: Plane, label: '출장', desc: '현지 업무와 네트워킹' }]

async function requestRecommendation() {
  showResults.value = true
  resultMode.value = 'ai'
  errorMessage.value = '추천 서비스 연결과 등록 강의 검증에 실패했습니다. 잠시 후 다시 시도해 주세요.'

  isLoading.value = true
  recommendedCourses.value = []
  try {
    const response = await recommendationApi.create(buildRecommendationRequest({
      language: language.value,
      level: level.value,
      job: job.value,
      situation: situation.value,
      goal: goal.value
    }))
    const data = response?.data?.data
    recommendedCourses.value = mapRecommendedCourses(data?.courses ?? [])
    resultMode.value = recommendationMode(data?.source)
  } catch (error) {
    resultMode.value = 'error'
    errorMessage.value = error?.response?.data?.detail ?? errorMessage.value
  } finally {
    isLoading.value = false
  }
}
function nextStep() { if (step.value < 3) step.value += 1; else requestRecommendation() }
function previousStep() { if (step.value > 1) step.value -= 1 }
</script>

<style scoped>
.recommend-layout{display:grid;grid-template-columns:minmax(0,1fr) 260px;gap:14px}.recommend-form{min-height:500px;padding:26px}.step-head{display:flex;align-items:center;gap:15px;margin-bottom:28px;color:var(--muted);font-family:var(--font-display);font-size:9px;font-weight:800;letter-spacing:.08em}.step-head .progress{flex:1}.recommend-form h2{font-size:23px}.recommend-form>p{margin:7px 0 22px;color:var(--muted);font-size:11px}.choice-grid{display:grid;gap:10px;margin:18px 0 24px}.choice-grid button{position:relative;display:flex;align-items:center;gap:12px;padding:14px;color:var(--ink);background:var(--surface);border:1px solid var(--line);border-radius:12px;text-align:left}.choice-grid button.selected{background:var(--lime-soft);border-color:#a7cb58}.choice-grid button>span{width:34px;height:34px;display:grid;place-items:center;background:var(--surface-2);border-radius:8px;font-family:var(--font-display);font-size:10px;font-weight:800}.choice-grid strong,.choice-grid small{display:block}.choice-grid strong{font-size:12px}.choice-grid small{margin-left:auto;color:var(--muted);font-size:9px}.choice-grid button>svg:last-child{margin-left:auto;color:var(--forest-2)}.languages{grid-template-columns:repeat(3,1fr)}.situations{grid-template-columns:repeat(2,1fr)}.situations button{display:grid;grid-template-columns:auto 1fr}.situations small{grid-column:2;margin-left:0}.recommend-form h3{margin-bottom:10px;font-size:12px}.segment{display:grid;grid-template-columns:repeat(3,1fr);padding:4px;background:var(--surface-2);border-radius:11px}.segment button{padding:9px;color:var(--muted);background:transparent;border-radius:8px;font-size:11px;font-weight:700}.segment button.active{color:var(--forest);background:var(--surface);box-shadow:var(--shadow-sm)}.step-actions{display:flex;justify-content:flex-end;gap:8px;margin-top:26px}.summary-card{display:flex;gap:11px;margin-top:18px;padding:14px;color:var(--purple);background:var(--purple-soft);border-radius:11px;font-size:10px}.summary-card strong,.summary-card span{display:block}.summary-card strong{margin-bottom:2px}.guide-card{height:max-content;padding:22px;color:#bed0c5;background:var(--forest);border-radius:18px}.guide-card>svg{color:var(--lime)}.guide-card h3{margin:16px 0 9px;color:white;font-size:16px}.guide-card p{font-size:10px;line-height:1.7}.guide-card ul{display:grid;gap:10px;margin:21px 0 0;padding:16px 0 0 17px;border-top:1px solid rgba(255,255,255,.1);font-size:9px}.results{margin-top:28px}.result-title{display:flex;align-items:flex-end;justify-content:space-between;margin-bottom:14px}.result-title h2{margin-top:8px;font-size:22px}.result-controls{display:flex;align-items:center;gap:8px;color:var(--muted);font-size:9px}.result-controls select{padding:8px 28px 8px 10px;background:var(--surface);border:1px solid var(--line);border-radius:8px;font-size:9px}.result-notice{display:flex;align-items:flex-start;gap:11px;margin-bottom:12px;padding:13px;color:var(--purple);background:var(--purple-soft);border-radius:12px;font-size:9px}.result-notice.fallback{color:#735315;background:#fff2d1}.result-notice span,.result-notice strong{display:block}.result-notice strong{margin-bottom:3px;font-size:10px}.result-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:12px}@media(max-width:900px){.recommend-layout{grid-template-columns:1fr}.guide-card{display:none}.result-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:620px){.recommend-form{padding:22px}.languages,.situations,.result-grid{grid-template-columns:1fr}.result-title{align-items:flex-start;flex-direction:column;gap:12px}}
</style>
