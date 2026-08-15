<template>
  <div class="login-page">
    <header class="login-header">
      <BrandLogo />
      <router-link to="/" class="home-link"><ArrowLeft :size="16" /> 홈으로</router-link>
    </header>

    <main class="login-main">
      <section class="login-intro animate-in">
        <p class="eyebrow">Continue your route</p>
        <h1>다시, 나의 학습 경로로</h1>
        <p>회사 계정으로 로그인하고 오늘의 학습과 팀의 성장을 이어가세요.</p>
      </section>

      <section class="login-card card animate-in" aria-labelledby="login-title">
        <div class="route-preview" aria-label="로그인 진행 과정">
          <div class="route-step active">
            <span><LogIn :size="16" /></span>
            <small>01</small>
            <strong>로그인</strong>
          </div>
          <i aria-hidden="true"></i>
          <div class="route-step">
            <span><ShieldCheck :size="16" /></span>
            <small>02</small>
            <strong>역할 확인</strong>
          </div>
          <i aria-hidden="true"></i>
          <div class="route-step">
            <span><Route :size="16" /></span>
            <small>03</small>
            <strong>맞춤 화면</strong>
          </div>
        </div>

        <div class="card-heading">
          <p class="eyebrow">Sign in</p>
          <h2 id="login-title">LinguaRoute 로그인</h2>
          <p class="muted">계정 정보는 안전한 Auth Server 로그인 화면에서 입력합니다.</p>
        </div>

        <div v-if="reasonMessage" class="login-notice" role="status">
          <TriangleAlert :size="18" />
          <span>{{ reasonMessage }}</span>
        </div>

        <form class="form-stack" @submit.prevent="login">
          <div v-if="loginError" class="login-notice error" role="alert">
            <TriangleAlert :size="18" />
            <span>{{ loginError }}</span>
          </div>
          <button class="button primary login-button" type="submit" :disabled="isSubmitting" :aria-busy="isSubmitting">
            <span>{{ isSubmitting ? '로그인 화면으로 이동 중...' : '회사 계정으로 로그인' }}</span>
            <ArrowRight :size="18" />
          </button>
          <p class="security-note"><LockKeyhole :size="14" /> LinguaRoute는 이 화면에서 비밀번호를 직접 저장하지 않습니다.</p>
          <p class="login-help">로그인에 실패하면 Auth Server에 인증 실패 안내가 표시됩니다. 업무용 이메일과 비밀번호를 다시 확인해 주세요.</p>
        </form>

        <div class="auth-divider"><span>처음이신가요?</span></div>

        <div class="signup-links">
          <router-link to="/signup/company">
            <span class="signup-icon"><Building2 :size="19" /></span>
            <span><strong>기업 관리자 가입</strong><small>새로운 기업 학습 공간 만들기</small></span>
            <ChevronRight :size="17" />
          </router-link>
          <router-link to="/signup/employee">
            <span class="signup-icon"><TicketCheck :size="19" /></span>
            <span><strong>직원 가입</strong><small>초대코드로 학습 공간 참여하기</small></span>
            <ChevronRight :size="17" />
          </router-link>
        </div>

        <router-link class="find-id" to="/account/recovery?tab=id">아이디를 잊으셨나요?</router-link>
      </section>
    </main>
  </div>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute } from 'vue-router'
import {
  ArrowLeft,
  ArrowRight,
  Building2,
  ChevronRight,
  LockKeyhole,
  LogIn,
  Route,
  ShieldCheck,
  TicketCheck,
  TriangleAlert
} from '@lucide/vue'
import BrandLogo from '@/components/BrandLogo.vue'
import { useAuthStore } from '@/store/auth.js'

const loginError = ref('')
const isSubmitting = ref(false)
const route = useRoute()
const auth = useAuthStore()

const reasonMessage = computed(() => route.query.reason === 'signup-complete'
  ? '기업 계정이 생성되었습니다. 로그인하면 구독 결제를 계속할 수 있습니다.'
  : route.query.reason === 'session-expired'
    ? 'Access Token이 만료되었습니다. 다시 로그인해 주세요.'
    : route.query.reason === 'user-inactive'
      ? '비활성 또는 탈퇴 계정은 서비스를 이용할 수 없습니다.'
      : route.query.reason === 'auth-required'
        ? '로그인이 필요한 화면입니다. 로그인 후 다시 이용해 주세요.'
        : '')

async function login() {
  loginError.value = ''
  isSubmitting.value = true
  try {
    const nextPath = String(route.query.next || '')
    if (nextPath.startsWith('/') && !nextPath.startsWith('//')) sessionStorage.setItem('post_login_redirect', nextPath)
    else sessionStorage.removeItem('post_login_redirect')
    auth.startOAuthLogin()
  } catch (error) {
    loginError.value = error.response?.data?.message || '로그인 화면으로 이동하지 못했습니다. 잠시 후 다시 시도해 주세요.'
  } finally {
    isSubmitting.value = false
  }
}
</script>

<style scoped>
.login-page { min-height: 100vh; background: var(--paper); }
.login-header { height: 72px; display: flex; align-items: center; justify-content: space-between; padding: 0 max(24px, 5vw); background: rgba(251, 250, 246, .9); border-bottom: 1px solid var(--line); backdrop-filter: blur(14px); }
.home-link { display: inline-flex; align-items: center; gap: 7px; color: var(--muted); font-size: 12px; font-weight: 600; }
.home-link:hover { color: var(--forest-2); }
.login-main { position: relative; width: min(680px, calc(100% - 32px)); margin: 0 auto; padding: 58px 0 90px; }
.login-main::before { position: absolute; top: 0; left: 50%; z-index: -1; width: min(980px, 100vw); height: 390px; content: ''; background-image: radial-gradient(#d6dcd4 1px, transparent 1px); background-size: 28px 28px; mask-image: linear-gradient(to bottom, black, transparent 85%); opacity: .65; transform: translateX(-50%); }
.login-intro { text-align: center; }
.login-intro .eyebrow { justify-content: center; }
.login-intro h1 { margin: 15px 0 11px; font-family: var(--font-display); font-size: clamp(34px, 5vw, 46px); line-height: 1.12; letter-spacing: -.045em; }
.login-intro > p:last-child { color: var(--muted); font-size: 14px; }
.login-card { margin-top: 34px; padding: 34px; animation-delay: .08s; box-shadow: var(--shadow-md); }
.route-preview { display: grid; grid-template-columns: auto 1fr auto 1fr auto; align-items: center; padding: 16px 18px; background: var(--forest); border-radius: 15px 15px 15px 5px; }
.route-preview > i { height: 1px; margin: 0 12px; background: repeating-linear-gradient(to right, rgba(200, 243, 107, .7) 0 5px, transparent 5px 9px); }
.route-step { display: grid; grid-template-columns: 30px auto; align-items: center; column-gap: 8px; color: #a9bdb0; }
.route-step > span { width: 30px; height: 30px; grid-row: 1 / 3; display: grid; place-items: center; color: var(--lime); background: rgba(200, 243, 107, .1); border-radius: 9px 9px 9px 3px; }
.route-step small { font-family: var(--font-display); font-size: 8px; line-height: 1; }
.route-step strong { color: #d9e7de; font-size: 10px; white-space: nowrap; }
.route-step.active > span { color: var(--ink); background: var(--lime); }
.route-step.active strong { color: white; }
.card-heading { margin: 30px 0 22px; }
.card-heading h2 { margin: 10px 0 6px; font-family: var(--font-display); font-size: 27px; letter-spacing: -.035em; }
.card-heading .muted { font-size: 12px; }
.login-notice { display: flex; align-items: flex-start; gap: 9px; margin-bottom: 18px; padding: 13px 14px; color: var(--blue); background: var(--blue-soft); border-radius: 11px; font-size: 11px; }
.login-notice svg { flex: none; margin-top: 1px; }
.login-notice.error { color: var(--danger); background: var(--danger-soft); }
.login-button { width: 100%; min-height: 50px; justify-content: space-between; padding: 0 18px; }
.login-button:disabled { cursor: wait; opacity: .65; }
.security-note { display: flex; align-items: center; justify-content: center; gap: 6px; color: var(--subtle); font-size: 10px; }
.login-help { color: var(--muted); font-size: 10px; line-height: 1.6; text-align: center; }
.auth-divider { display: flex; align-items: center; gap: 12px; margin: 25px 0 17px; color: var(--subtle); font-size: 10px; }
.auth-divider::before, .auth-divider::after { height: 1px; flex: 1; content: ''; background: var(--line); }
.signup-links { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
.signup-links > a { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 10px; min-height: 72px; padding: 12px; background: var(--surface-2); border: 1px solid transparent; border-radius: 13px; transition: background .2s ease, border-color .2s ease, transform .2s ease; }
.signup-links > a:hover { background: var(--surface); border-color: var(--line-strong); transform: translateY(-1px); }
.signup-icon { width: 34px; height: 34px; display: grid; place-items: center; color: var(--forest-2); background: var(--mint); border-radius: 10px 10px 10px 4px; }
.signup-links strong, .signup-links small { display: block; }
.signup-links strong { color: var(--ink); font-size: 11px; }
.signup-links small { margin-top: 2px; color: var(--muted); font-size: 9px; line-height: 1.4; }
.signup-links > a > svg { color: var(--subtle); }
.find-id { display: block; width: max-content; margin: 18px auto 0; color: var(--forest-2); font-size: 11px; font-weight: 700; text-decoration: underline; text-underline-offset: 3px; }

@media (max-width: 600px) {
  .login-header { padding: 0 20px; }
  .login-main { padding: 42px 0 64px; }
  .login-intro h1 { font-size: 32px; }
  .login-intro > p:last-child { max-width: 310px; margin: 0 auto; font-size: 12px; }
  .login-card { margin-top: 28px; padding: 22px; }
  .route-preview { grid-template-columns: repeat(3, 1fr); gap: 8px; padding: 13px; }
  .route-preview > i { display: none; }
  .route-step { grid-template-columns: 28px 1fr; column-gap: 6px; }
  .route-step > span { width: 28px; height: 28px; }
  .route-step strong { font-size: 9px; }
  .signup-links { grid-template-columns: 1fr; }
}

@media (max-width: 390px) {
  .route-step { display: flex; flex-direction: column; gap: 5px; text-align: center; }
  .route-step > span { flex: none; }
  .route-step small { display: none; }
}
</style>
