<template>
  <div class="app-shell" :class="{ collapsed, 'mobile-open': mobileOpen }">
    <aside class="sidebar">
      <div class="sidebar-top">
        <BrandLogo :compact="collapsed" />
        <button class="collapse-button" :aria-label="collapsed ? '사이드바 펼치기' : '사이드바 접기'" @click="collapsed = !collapsed">
          <PanelLeftClose v-if="!collapsed" :size="17" /><PanelLeftOpen v-else :size="17" />
        </button>
      </div>

      <div v-if="!collapsed" class="workspace-switcher">
        <span class="company-symbol">S</span>
        <span><strong>{{ roleLabel }}</strong><small>{{ roleDescription }}</small></span>
        <ChevronsUpDown :size="14" />
      </div>

      <nav class="sidebar-nav" :aria-label="`${roleLabel} 메뉴`">
        <template v-for="group in navigation" :key="group.label">
          <p v-if="!collapsed" class="nav-group">{{ group.label }}</p>
          <router-link v-for="item in group.items" :key="item.to" :to="item.to" class="nav-item" :title="collapsed ? item.label : undefined">
            <component :is="item.icon" :size="18" :stroke-width="1.9" /><span v-if="!collapsed">{{ item.label }}</span>
            <span v-if="item.count && !collapsed" class="nav-count">{{ item.count }}</span>
          </router-link>
        </template>
      </nav>

      <div class="sidebar-bottom">
        <router-link to="/profile" class="profile-chip">
          <span class="avatar">{{ userInitial }}</span><span v-if="!collapsed"><strong>{{ userName }}</strong><small>{{ roleLabel }}</small></span><MoreHorizontal v-if="!collapsed" :size="17" />
        </router-link>
        <button class="logout-button" :title="collapsed ? '로그아웃' : undefined" @click="logoutModal = true">
          <LogOut :size="17" /><span v-if="!collapsed">로그아웃</span>
        </button>
      </div>
    </aside>

    <div class="workspace">
      <header class="topbar">
        <button class="mobile-trigger icon-button" aria-label="사이드바 열기" @click="mobileOpen = !mobileOpen"><Menu :size="19" /></button>
        <div class="search-box"><Search :size="17" /><input aria-label="전체 검색" placeholder="강의, 직원, 기업 검색" /><kbd>⌘ K</kbd></div>
        <div class="topbar-actions"><button class="icon-button" aria-label="알림"><Bell :size="18" /></button></div>
      </header>
      <main class="workspace-main"><slot /></main>
    </div>
    <button v-if="mobileOpen" class="scrim" aria-label="메뉴 닫기" @click="mobileOpen = false"></button>
    <div v-if="logoutModal" class="modal-layer" @click.self="logoutModal = false">
      <div class="logout-modal card" role="dialog" aria-modal="true" aria-labelledby="logout-title">
        <span class="logout-icon"><LogOut :size="22" /></span>
        <h2 id="logout-title">로그아웃하시겠어요?</h2>
        <p>현재 기기의 Access Token과 인증 세션을 정리하고 첫 화면으로 이동합니다.</p>
        <div><button class="button" @click="logoutModal = false">취소</button><button class="button primary" @click="confirmLogout">로그아웃</button></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { LayoutDashboard, BookOpen, Sparkles, GraduationCap, UserRound, Building2, UsersRound, CreditCard, ChartNoAxesCombined, ReceiptText, LibraryBig, Bell, Search, Menu, MoreHorizontal, ChevronsUpDown, PanelLeftClose, PanelLeftOpen, LogOut } from '@lucide/vue'
import BrandLogo from './BrandLogo.vue'
import { useAuthStore } from '@/store/auth.js'

const route = useRoute()
const auth = useAuthStore()
const collapsed = ref(false)
const mobileOpen = ref(false)
const logoutModal = ref(false)
watch(() => route.path, () => { mobileOpen.value = false })

const configs = {
  employee: {
    label: '직원 학습 공간',
    groups: [
      { label: '학습', items: [
        { label: '홈', to: '/app', icon: LayoutDashboard }, { label: '강의 찾기', to: '/courses', icon: BookOpen }, { label: 'AI 강의 추천', to: '/recommendations', icon: Sparkles }, { label: '내 학습', to: '/learning', icon: GraduationCap }
      ]},
      { label: '계정', items: [{ label: '내 정보', to: '/profile', icon: UserRound }] }
    ]
  },
  company: {
    label: '기업 관리자',
    groups: [
      { label: '운영', items: [
        { label: '대시보드', to: '/company', icon: LayoutDashboard }, { label: '직원 · 초대', to: '/company/employees', icon: UsersRound }, { label: '학습 현황', to: '/company/progress', icon: ChartNoAxesCombined }
      ]},
      { label: '구독', items: [{ label: '요금제 · 결제', to: '/company/subscription', icon: CreditCard }, { label: '구독 결제 신청', to: '/company/checkout', icon: ReceiptText }, { label: '기업 정보', to: '/company/settings', icon: Building2 }] }
    ]
  },
  admin: {
    label: '플랫폼 운영',
    groups: [
      { label: '모니터링', items: [{ label: '운영 대시보드', to: '/admin', icon: LayoutDashboard }, { label: '기업', to: '/admin/companies', icon: Building2 }, { label: '사용자', to: '/admin/users', icon: UsersRound }] },
      { label: '콘텐츠 · 거래', items: [{ label: '강의 관리', to: '/admin/courses', icon: LibraryBig }, { label: '결제', to: '/admin/payments', icon: ReceiptText }, { label: '수강', to: '/admin/enrollments', icon: GraduationCap }] },
    ]
  }
}

const roleKey = computed(() => ({ EMPLOYEE: 'employee', COMPANY_ADMIN: 'company', PLATFORM_ADMIN: 'admin' }[auth.businessRole] || 'employee'))
const current = computed(() => configs[roleKey.value])
const navigation = computed(() => current.value.groups)
const roleLabel = computed(() => current.value.label)
const roleDescription = computed(() => roleKey.value === 'admin' ? 'LinguaRoute' : auth.user?.companyName || '소속 기업')
const userName = computed(() => auth.user?.name || auth.user?.email || '사용자')
const userInitial = computed(() => userName.value.trim().charAt(0) || 'U')

function confirmLogout() {
  logoutModal.value = false
  auth.logout()
}
</script>

<style scoped>
.app-shell { --sidebar-width: 248px; min-height: 100vh; display: grid; grid-template-columns: var(--sidebar-width) 1fr; background: var(--paper); transition: grid-template-columns .25s ease; }
.app-shell.collapsed { --sidebar-width: 78px; }
.sidebar { position: sticky; top: 0; z-index: 40; height: 100vh; display: flex; flex-direction: column; padding: 18px 14px 14px; overflow: hidden; color: #dce9e1; background: #102d21; border-right: 1px solid rgba(255,255,255,.08); }
.sidebar-top { min-height: 42px; display: flex; align-items: center; justify-content: space-between; padding: 0 4px; }
.sidebar :deep(.brand-name) { color: white; }.sidebar :deep(.brand-name span) { color: var(--lime); }
.collapse-button { width: 30px; height: 30px; display: grid; flex: none; place-items: center; color: #98afa2; background: transparent; border-radius: 8px; }.collapse-button:hover { color: white; background: rgba(255,255,255,.08); }
.workspace-switcher { display: grid; grid-template-columns: auto 1fr auto; align-items: center; gap: 9px; margin: 22px 0 16px; padding: 10px; background: rgba(255,255,255,.07); border: 1px solid rgba(255,255,255,.08); border-radius: 12px; }
.company-symbol { width: 29px; height: 29px; display: grid; place-items: center; color: var(--ink); background: var(--lime); border-radius: 8px; font-weight: 800; }
.workspace-switcher strong, .workspace-switcher small { display: block; }.workspace-switcher strong { color: white; font-size: 12px; }.workspace-switcher small { color: #8fa99a; font-size: 10px; }
.sidebar-nav { display: grid; gap: 4px; overflow-y: auto; }
.nav-group { margin: 17px 10px 5px; color: #6f8e7d; font-size: 9px; font-weight: 800; letter-spacing: .12em; text-transform: uppercase; }
.nav-item { min-height: 42px; display: flex; align-items: center; gap: 12px; padding: 0 11px; color: #a9beb2; border-radius: 10px; font-size: 12px; font-weight: 600; transition: background .18s ease, color .18s ease; }
.nav-item:hover { color: white; background: rgba(255,255,255,.06); }.nav-item.router-link-active { color: var(--lime); background: rgba(200,243,107,.1); }.nav-item span { overflow: hidden; white-space: nowrap; }.nav-count { min-width: 20px; margin-left: auto; padding: 2px 6px; color: var(--ink); background: var(--lime); border-radius: 6px; font-size: 9px; text-align: center; }
.sidebar-bottom { display: grid; gap: 10px; margin-top: auto; }
.role-preview { padding: 11px; background: rgba(255,255,255,.05); border-radius: 11px; }.role-preview span { display: block; margin-bottom: 6px; color: #789485; font-size: 9px; font-weight: 700; }.role-preview select { width: 100%; color: white; background: #193b2d; border: 1px solid rgba(255,255,255,.1); border-radius: 7px; font-size: 11px; padding: 5px; }
.profile-chip { min-height: 50px; display: flex; align-items: center; gap: 9px; padding: 7px; border-top: 1px solid rgba(255,255,255,.08); }.profile-chip .avatar { color: var(--ink); background: var(--lime); }.profile-chip span:not(.avatar) { flex: 1; overflow: hidden; }.profile-chip strong, .profile-chip small { display: block; white-space: nowrap; }.profile-chip strong { color: white; font-size: 11px; }.profile-chip small { color: #789485; font-size: 9px; }
.logout-button { min-height: 36px; display: flex; align-items: center; gap: 10px; padding: 0 11px; color: #8fa99a; background: transparent; border-radius: 9px; font-size: 10px; font-weight: 700; }.logout-button:hover { color: white; background: rgba(255,255,255,.06); }
.workspace { min-width: 0; }.topbar { position: sticky; top: 0; z-index: 25; height: 72px; display: flex; align-items: center; gap: 18px; padding: 0 34px; background: rgba(251,250,246,.9); border-bottom: 1px solid var(--line); backdrop-filter: blur(14px); }
.search-box { width: min(420px, 46vw); height: 40px; display: flex; align-items: center; gap: 9px; padding: 0 12px; color: var(--muted); background: var(--surface); border: 1px solid var(--line); border-radius: 11px; }.search-box input { min-width: 0; flex: 1; background: transparent; border: 0; outline: 0; font-size: 12px; }.search-box kbd { padding: 2px 6px; color: var(--subtle); background: var(--surface-2); border: 1px solid var(--line); border-radius: 5px; font-size: 9px; }
.topbar-actions { display: flex; align-items: center; gap: 10px; margin-left: auto; }.prototype-label { display: inline-flex; align-items: center; gap: 5px; color: var(--forest-2); font-size: 9px; font-weight: 800; letter-spacing: .08em; }.topbar .icon-button { position: relative; }.topbar .icon-button i { position: absolute; top: 9px; right: 9px; width: 6px; height: 6px; background: var(--danger); border: 1px solid white; border-radius: 50%; }
.workspace-main { max-width: 1180px; margin: 0 auto; padding: 28px; }.mobile-trigger { display: none; }.scrim { display: none; }
.modal-layer { position: fixed; inset: 0; z-index: 100; display: grid; place-items: center; padding: 20px; background: rgba(11,26,18,.58); backdrop-filter: blur(5px); }.logout-modal { width: min(390px,100%); padding: 30px; text-align: center; }.logout-icon { width: 50px; height: 50px; display: grid; place-items: center; margin: 0 auto; color: var(--danger); background: var(--danger-soft); border-radius: 15px; }.logout-modal h2 { margin: 17px 0 7px; font-size: 19px; }.logout-modal p { color: var(--muted); font-size: 10px; line-height: 1.7; }.logout-modal>div { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 22px; }
@media (max-width: 1180px) { .workspace-main { padding: 24px; } .topbar { padding: 0 24px; } }
@media (max-width: 900px) { .app-shell, .app-shell.collapsed { --sidebar-width: 0px; grid-template-columns: 1fr; }.sidebar { position: fixed; left: -260px; width: 248px; transition: left .25s ease; }.mobile-open .sidebar { left: 0; }.collapse-button { display: none; }.mobile-trigger { display: grid; }.scrim { position: fixed; inset: 0; z-index: 35; display: block; background: rgba(10,25,18,.45); }.workspace-main { padding: 26px 20px; }.topbar { padding: 0 20px; } }
@media (max-width: 600px) { .search-box { display: none; }.prototype-label { display: none; }.workspace-main { padding: 24px 14px; } }
</style>
