<template>
  <section class="async-state" :class="type" role="status">
    <span class="state-icon">
      <LoaderCircle v-if="type === 'loading'" :size="24" />
      <SearchX v-else-if="type === 'empty'" :size="24" />
      <TriangleAlert v-else :size="24" />
    </span>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <button v-if="type === 'error'" class="button" @click="$emit('retry')">
      <RefreshCw :size="15" /> 다시 시도
    </button>
  </section>
</template>

<script setup>
import { LoaderCircle, RefreshCw, SearchX, TriangleAlert } from '@lucide/vue'

defineProps({
  type: { type: String, default: 'empty' },
  title: { type: String, required: true },
  description: { type: String, required: true },
})

defineEmits(['retry'])
</script>

<style scoped>
.async-state{min-height:260px;display:grid;place-content:center;justify-items:center;padding:36px;color:var(--muted);background:var(--surface);border:1px dashed var(--line-strong);border-radius:18px;text-align:center}.state-icon{width:52px;height:52px;display:grid;place-items:center;color:var(--forest);background:var(--mint);border-radius:15px}.loading .state-icon svg{animation:spin 1s linear infinite}.error .state-icon{color:var(--danger);background:var(--danger-soft)}h3{margin:15px 0 6px;color:var(--ink);font-size:14px}p{max-width:360px;font-size:10px;line-height:1.7}.button{margin-top:16px}@keyframes spin{to{transform:rotate(360deg)}}
</style>
