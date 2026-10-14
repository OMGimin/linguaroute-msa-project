<template>
  <div class="page-header">
    <div>
      <p v-if="eyebrow" class="eyebrow">{{ eyebrow }}</p>
      <h1 class="page-title">{{ title }}</h1>
      <p v-if="description" class="description">{{ description }}</p>
    </div>
    <div class="actions"><slot /></div>
  </div>
</template>
<script setup>
defineProps({ title: String, description: String, eyebrow: String })
</script>
<style scoped>
.page-header { display: flex; align-items: flex-end; justify-content: space-between; gap: 24px; margin-bottom: 28px; }
.eyebrow { margin-bottom: 8px; }
.description { max-width: 660px; margin-top: 9px; color: var(--muted); font-size: 14px; }
.actions { display: flex; flex: none; gap: 8px; }
@media (max-width: 720px) { .page-header { align-items: flex-start; flex-direction: column; } .actions { width: 100%; } .actions :deep(.button) { flex: 1; } }
</style>
