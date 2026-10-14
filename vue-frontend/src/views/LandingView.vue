<template>
  <div class="landing">
    <PublicHeader />
    <main>
      <section class="hero">
        <div class="container hero-grid">
          <div class="hero-copy animate-in">
            <p class="eyebrow">Language learning, routed right</p>
            <h1>우리 팀의 성장을<br><em>하나의 경로</em>로 잇다</h1>
            <p class="hero-description">기업의 언어 교육 운영부터 직원의 비즈니스 외국어 학습까지. LinguaRoute가 직무·업무 상황·전문용어 목표를 AI 추천과 데이터로 연결합니다.</p>
            <div class="hero-actions"><router-link class="button accent" to="/signup/company">기업 무료 체험 <ArrowRight :size="17" /></router-link><router-link class="button" to="/signup/employee">초대코드로 참여</router-link></div>
            <div class="trust-row"><span>이미 함께 성장하는 팀</span><strong>SCALA</strong><strong>NOVA</strong><strong>BRIDGE</strong><strong>EDEN</strong></div>
          </div>
          <div class="route-visual animate-in" aria-label="개인화 학습 경로 미리보기">
            <div class="route-caption"><span class="live-dot"></span> 김민지님의 이번 주 학습 경로 <MoreHorizontal :size="17" /></div>
            <div class="route-card active-card">
              <div class="route-number">01</div><div><span class="tag">지금 학습 중</span><h3>해외 고객 미팅 영어</h3><p>제품 사양과 기술 용어를 설명하는 법</p></div><div class="progress-ring"><span>68%</span></div>
            </div>
            <div class="route-line"><span></span></div>
            <div class="route-card next-card"><div class="route-number">02</div><div><span class="tag blue">AI 추천</span><h3>영어 프레젠테이션</h3><p>현재 목표에 가장 가까운 다음 과정</p></div><ArrowUpRight :size="20" /></div>
            <div class="route-note"><Sparkles :size="17" /><span><strong>이번 달 4.2시간 절약</strong>AI가 탐색 시간을 줄여드렸어요.</span></div>
          </div>
        </div>
      </section>

      <section id="solution" class="value-section container">
        <div class="section-intro"><p class="eyebrow">One platform, three perspectives</p><h2 class="section-title">각자의 화면은 달라도<br>성장은 같은 방향으로</h2></div>
        <div class="value-grid">
          <article v-for="(item, index) in values" :key="item.title" class="value-card">
            <span class="value-index">0{{ index + 1 }}</span><component :is="item.icon" :size="24" /><h3>{{ item.title }}</h3><p>{{ item.description }}</p><router-link :to="item.to">화면 미리보기 <ArrowRight :size="15" /></router-link>
          </article>
        </div>
      </section>

      <section id="learning" class="experience-section">
        <div class="container experience-grid">
          <div class="insight-panel">
            <div class="panel-header"><span><strong>팀 학습 온도</strong>2026년 8월</span><span class="tag">좋은 흐름</span></div>
            <div class="big-stat"><strong>78<small>%</small></strong><span>평균 진도율<br><b>전월 대비 +12%</b></span></div>
            <div class="bars"><i style="height:35%"></i><i style="height:48%"></i><i style="height:42%"></i><i style="height:62%"></i><i style="height:55%"></i><i style="height:76%"></i><i class="current" style="height:88%"></i></div>
            <div class="panel-footer"><span>7월 1주</span><span>8월 2주</span></div>
          </div>
          <div class="experience-copy"><p class="eyebrow">Visible progress</p><h2 class="section-title">감이 아닌 데이터로<br>학습의 변화를 확인하세요</h2><p>좌석 사용량, 직원별 진도, 수강 완료율을 한 화면에서 파악하고 필요한 순간에 바로 지원할 수 있습니다.</p><ul><li><Check :size="17" /> 기업별 좌석과 구독 상태 실시간 확인</li><li><Check :size="17" /> 직원별 수강 상태와 진도율 비교</li><li><Check :size="17" /> 결제·초대·학습 흐름을 역할별로 분리</li></ul><router-link class="text-link" to="/company">기업 관리자 화면 보기 <ArrowRight :size="16" /></router-link></div>
        </div>
      </section>

      <section class="cta container"><div><p class="eyebrow">Ready for the next route?</p><h2>팀의 다음 성장을<br>오늘 연결해 보세요.</h2></div><div><p>복잡한 도입 과정 없이 기업 계정을 만들고<br>바로 학습 공간을 준비할 수 있습니다.</p><router-link class="button accent" to="/signup/company">LinguaRoute 시작하기 <ArrowUpRight :size="17" /></router-link></div></section>
    </main>
    <footer><div class="container footer-inner"><BrandLogo /><p>© 2026 LinguaRoute. 기업 외국어교육의 새로운 경로.</p><div><a href="#">이용약관</a><a href="#">개인정보처리방침</a></div></div></footer>
  </div>
</template>

<script setup>
import { ArrowRight, ArrowUpRight, MoreHorizontal, Sparkles, Check, UsersRound, GraduationCap, Gauge } from '@lucide/vue'
import PublicHeader from '@/components/PublicHeader.vue'
import BrandLogo from '@/components/BrandLogo.vue'
const values = [
  { icon: GraduationCap, title: '직원에게는 나만의 학습 경로', description: '언어, 수준, 직무와 목표를 분석해 지금 필요한 강의를 더 빠르게 찾습니다.', to: '/app' },
  { icon: UsersRound, title: '관리자에게는 선명한 운영 현황', description: '초대부터 좌석, 진도와 결제까지 흩어진 교육 운영을 한곳에 모읍니다.', to: '/company' },
  { icon: Gauge, title: '플랫폼에는 안정적인 통제', description: '기업, 사용자, 강의와 거래 상태를 역할에 맞게 안전하게 관리합니다.', to: '/admin' }
]
</script>

<style scoped>
.hero { position: relative; overflow: hidden; padding: 64px 0 68px; }.hero::before { position:absolute; inset:0; z-index:-1; content:''; background-image: radial-gradient(#d6dcd4 1px, transparent 1px); background-size: 28px 28px; mask-image: linear-gradient(to right, black, transparent 80%); opacity:.65; }
.hero-grid { display:grid; grid-template-columns: 1.02fr .98fr; align-items:center; gap:52px; }.hero-copy h1 { margin:16px 0 18px; font-family:var(--font-display); font-size:clamp(44px,5vw,64px); line-height:1.04; letter-spacing:0; }.hero-copy h1 em { position:relative; color:var(--forest-2); font-style:normal; }.hero-copy h1 em::after { position:absolute; right:0; bottom:2px; left:0; z-index:-1; height:13px; content:''; background:var(--lime); transform:rotate(-1deg); }.hero-description { max-width:560px; color:var(--muted); font-size:16px; line-height:1.75; }.hero-actions { display:flex; gap:10px; margin-top:26px; }.trust-row { display:flex; align-items:center; gap:18px; margin-top:34px; color:var(--subtle); font-family:var(--font-display); font-size:11px; }.trust-row span { font-family:var(--font); }.trust-row strong { letter-spacing:.08em; }
.route-visual { position:relative; padding:20px; background:#153e2d; border:1px solid rgba(255,255,255,.1); border-radius:24px; box-shadow:0 24px 64px rgba(16,45,33,.2); animation-delay:.12s; }.route-caption { display:flex; align-items:center; gap:8px; margin-bottom:16px; color:#b9cec1; font-size:11px; }.route-caption svg { margin-left:auto; }.live-dot { width:7px; height:7px; background:var(--lime); border-radius:50%; box-shadow:0 0 0 4px rgba(200,243,107,.13); }.route-card { min-height:112px; display:grid; grid-template-columns:auto 1fr auto; align-items:center; gap:14px; padding:17px; background:#fff; border-radius:15px; }.route-number { align-self:start; color:#9ba69e; font-family:var(--font-display); font-size:13px; font-weight:700; }.route-card h3 { margin:9px 0 4px; font-size:16px; }.route-card p { color:var(--muted); font-size:11px; }.progress-ring { --value:68%; width:52px; height:52px; display:grid; place-items:center; background:conic-gradient(var(--lime) var(--value), #e9ede7 0); border-radius:50%; }.progress-ring::before { position:absolute; width:40px; height:40px; content:''; background:#fff; border-radius:50%; }.progress-ring span { z-index:1; font-family:var(--font-display); font-size:10px; font-weight:700; }.route-line { height:30px; padding-left:32px; }.route-line span { display:block; width:2px; height:100%; background:repeating-linear-gradient(to bottom,var(--lime) 0 4px,transparent 4px 8px); }.next-card { min-height:98px; color:#d9e7de; background:#214c38; border:1px solid rgba(255,255,255,.08); }.next-card p { color:#9eb5a7; }.next-card svg { color:var(--lime); }.route-note { display:flex; align-items:center; gap:11px; margin:14px 4px 1px; padding:10px 12px; color:#bcd0c3; background:rgba(255,255,255,.06); border-radius:11px; font-size:10px; }.route-note svg { color:var(--lime); }.route-note span, .route-note strong { display:block; }.route-note strong { margin-bottom:2px; color:white; font-size:11px; }
.value-section { padding:78px 0; }.section-intro { display:flex; align-items:flex-end; justify-content:space-between; margin-bottom:32px; }.value-grid { display:grid; grid-template-columns:repeat(3,1fr); border-top:1px solid var(--line-strong); }.value-card { position:relative; min-height:260px; padding:26px 24px; border-right:1px solid var(--line); transition:background .25s ease; }.value-card:last-child { border-right:0; }.value-card:hover { background:var(--surface); }.value-index { position:absolute; top:26px; right:24px; color:var(--subtle); font-family:var(--font-display); font-size:11px; }.value-card>svg { margin:34px 0 26px; color:var(--forest-2); }.value-card h3 { font-size:18px; }.value-card p { margin:12px 0 22px; color:var(--muted); font-size:13px; line-height:1.7; }.value-card a,.text-link { display:inline-flex; align-items:center; gap:7px; color:var(--forest-2); font-size:12px; font-weight:700; }
.experience-section { padding:105px 0; background:#eaf0e8; }.experience-grid { display:grid; grid-template-columns:1fr 1fr; align-items:center; gap:95px; }.insight-panel { padding:26px; background:var(--surface); border:1px solid #d9dfd7; border-radius:22px; box-shadow:var(--shadow-md); }.panel-header,.panel-footer { display:flex; align-items:center; justify-content:space-between; }.panel-header span:first-child strong,.panel-header span:first-child { display:block; }.panel-header span:first-child { color:var(--muted); font-size:10px; }.panel-header strong { margin-bottom:2px; color:var(--ink); font-size:13px; }.big-stat { display:flex; align-items:flex-end; gap:20px; margin:35px 0 30px; }.big-stat>strong { font-family:var(--font-display); font-size:65px; line-height:.9; letter-spacing:-.06em; }.big-stat small { font-size:25px; }.big-stat span { color:var(--muted); font-size:11px; }.big-stat b { color:var(--forest-2); }.bars { height:140px; display:flex; align-items:flex-end; gap:13px; padding:0 8px; border-bottom:1px solid var(--line); }.bars i { flex:1; background:#dbe4da; border-radius:5px 5px 0 0; }.bars i.current { background:var(--forest-2); }.panel-footer { margin-top:9px; color:var(--subtle); font-size:9px; }.experience-copy .section-title { margin:17px 0; }.experience-copy>p { color:var(--muted); font-size:14px; line-height:1.8; }.experience-copy ul { display:grid; gap:12px; margin:27px 0; padding:0; list-style:none; }.experience-copy li { display:flex; align-items:center; gap:10px; font-size:13px; }.experience-copy li svg { color:var(--forest-2); }
.cta { margin-top:100px; margin-bottom:100px; padding:58px; display:flex; align-items:flex-end; justify-content:space-between; color:white; background:var(--forest); border-radius:26px 26px 26px 8px; }.cta h2 { margin-top:15px; font-family:var(--font-display); font-size:44px; line-height:1.12; letter-spacing:-.05em; }.cta .eyebrow { color:var(--lime); }.cta>div:last-child { display:grid; gap:20px; justify-items:start; }.cta>div:last-child p { color:#bdd0c4; font-size:13px; }footer { padding:28px 0; border-top:1px solid var(--line); }.footer-inner { display:flex; align-items:center; gap:28px; }.footer-inner p { margin-left:auto; color:var(--muted); font-size:11px; }.footer-inner>div { display:flex; gap:18px; color:var(--muted); font-size:11px; }
@media(max-width:900px){.hero-grid,.experience-grid{grid-template-columns:1fr;}.hero{padding-top:60px}.route-visual{max-width:600px}.value-grid{grid-template-columns:1fr}.value-card{min-height:auto;border-right:0;border-bottom:1px solid var(--line)}.cta{align-items:flex-start;flex-direction:column;gap:35px}.experience-grid{gap:50px}}
@media(max-width:620px){.hero-copy h1{font-size:44px}.hero-actions{align-items:stretch;flex-direction:column}.trust-row{flex-wrap:wrap}.route-visual{padding:13px;border-radius:20px}.route-card{grid-template-columns:auto 1fr}.route-card>*:last-child{display:none}.value-section{padding:75px 0}.experience-section{padding:75px 0}.cta{margin:70px 14px;padding:34px 25px}.cta h2{font-size:34px}.footer-inner{align-items:flex-start;flex-direction:column}.footer-inner p{margin-left:0}}
</style>
