<template>
  <header class="public-header">
    <div class="container header-inner">
      <BrandLogo />
      <nav class="desktop-nav" aria-label="주요 메뉴">
        <a href="#solution">서비스</a><a href="#learning">학습 경험</a><router-link to="/pricing">요금제</router-link>
      </nav>
      <div class="header-actions">
        <router-link class="button ghost" to="/login">로그인</router-link>
        <router-link class="button primary" to="/signup/company">기업 시작하기 <ArrowUpRight :size="16" /></router-link>
      </div>
      <button class="mobile-menu icon-button" aria-label="메뉴 열기"><Menu :size="19" /></button>
    </div>
  </header>
</template>

<script setup>
import { ArrowUpRight, Menu } from '@lucide/vue'
import BrandLogo from './BrandLogo.vue'
</script>

<style scoped>
.public-header { position: sticky; top: 0; z-index: 30; background: rgba(251, 250, 246, .88); border-bottom: 1px solid rgba(226, 229, 221, .8); backdrop-filter: blur(14px); }
.header-inner { height: 72px; display: flex; align-items: center; gap: 36px; }
.desktop-nav { display: flex; align-items: center; gap: 28px; margin-left: auto; color: var(--muted); font-size: 13px; font-weight: 600; }
.desktop-nav a:hover { color: var(--ink); }
.header-actions { display: flex; gap: 8px; }
.mobile-menu { display: none; margin-left: auto; }
@media (max-width: 760px) { .desktop-nav, .header-actions { display: none; } .mobile-menu { display: grid; } }
</style>
