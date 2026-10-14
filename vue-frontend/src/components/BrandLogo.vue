<template>
  <router-link to="/" class="brand" :class="{ compact }" aria-label="LinguaRoute 홈">
    <span class="brand-mark" aria-hidden="true"><Route :size="compact ? 18 : 21" :stroke-width="2.4" /></span>
    <span v-if="!compact" class="brand-name">Lingua<span>Route</span></span>
  </router-link>
</template>

<script setup>
import { Route } from '@lucide/vue'
defineProps({ compact: Boolean })
</script>

<style scoped>
.brand { display: inline-flex; align-items: center; gap: 10px; font-family: var(--font-display); font-size: 19px; font-weight: 700; letter-spacing: -.04em; }
.brand-mark { width: 38px; height: 38px; display: grid; place-items: center; color: var(--ink); background: var(--lime); border-radius: 11px 11px 11px 4px; }
.brand-name span { color: var(--forest-2); }
.compact .brand-mark { width: 34px; height: 34px; }
</style>
