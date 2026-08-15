<template>
  <AppShell>
    <PageHeader eyebrow="Course operations" title="강의 관리" description="강의를 등록·수정하고 활성 상태를 관리합니다.">
      <router-link class="button accent" to="/admin/courses/new"><Plus :size="16" /> 새 강의 등록</router-link>
    </PageHeader>
    <section class="metric-grid summary">
      <article v-for="item in stats" :key="item.label" class="metric card">
        <div class="metric-head"><span>{{ item.label }}</span><span class="metric-icon"><component :is="item.icon" :size="17" /></span></div>
        <div><strong class="metric-value">{{ item.value }}</strong><p class="trend">{{ item.desc }}</p></div>
      </article>
    </section>
    <section class="panel course-panel">
      <div class="table-toolbar">
        <div class="table-search"><Search :size="16" /><input v-model="keyword" placeholder="강의명 검색" /></div>
        <select v-model="language" class="select"><option value="">모든 언어</option><option value="ENGLISH">영어</option><option value="JAPANESE">일본어</option><option value="CHINESE">중국어</option></select>
        <select v-model="status" class="select"><option value="">모든 상태</option><option value="ACTIVE">ACTIVE</option><option value="INACTIVE">INACTIVE</option></select>
        <button class="button small" @click="loadCourses"><SlidersHorizontal :size="14" /> 필터</button>
      </div>
      <p v-if="loadError" class="empty-message">{{ loadError }}</p>
      <div v-else class="course-admin-list">
        <article v-for="course in courses" :key="course.id">
          <div class="course-info"><div><span class="tag" :class="course.status === 'INACTIVE' ? 'gray' : ''">{{ course.status }}</span><span>{{ course.language }} · {{ course.level }} · {{ course.situation }}</span></div><h2>{{ course.title }}</h2><p>COURSE-{{ course.id }}</p></div>
          <div class="course-actions"><router-link class="button small" :to="`/admin/courses/${course.id}/edit`"><Pencil :size="14" /> 수정</router-link><button class="button small" @click="toggle(course)"><Power :size="14" /> {{ course.status === 'ACTIVE' ? '비활성화' : '활성화' }}</button></div>
        </article>
        <p v-if="!courses.length" class="empty-message">조건에 맞는 강의가 없습니다.</p>
      </div>
    </section>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { BadgeCheck, BookX, LibraryBig, Pencil, Plus, Power, Search, SlidersHorizontal } from '@lucide/vue'
import AppShell from '@/components/AppShell.vue'
import PageHeader from '@/components/PageHeader.vue'
import { courseApi } from '@/api/course.js'

const courses = ref([])
const loadError = ref('')
const keyword = ref('')
const language = ref('')
const status = ref('')
const stats = computed(() => [
  { label: '조회 강의', value: `${courses.value.length}`, desc: '현재 필터 결과', icon: LibraryBig },
  { label: '활성 강의', value: `${courses.value.filter((course) => course.status === 'ACTIVE').length}`, desc: '현재 결과 중 노출', icon: BadgeCheck },
  { label: '비활성 강의', value: `${courses.value.filter((course) => course.status === 'INACTIVE').length}`, desc: '현재 결과 중 비노출', icon: BookX }
])

async function loadCourses() {
  loadError.value = ''
  try {
    const response = await courseApi.getAdminCourses({ keyword: keyword.value || undefined, language: language.value || undefined, status: status.value || undefined, page: 0, size: 100 })
    courses.value = response.data.data.content.map((course) => ({ id: course.courseId, ...course }))
  } catch (error) {
    courses.value = []
    loadError.value = error.response?.data?.message || '강의 목록을 불러오지 못했습니다.'
  }
}

async function toggle(course) {
  await courseApi.updateStatus(course.id, course.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE')
  await loadCourses()
}

onMounted(loadCourses)
</script>

<style scoped>
.summary{margin-bottom:14px}.course-panel{padding:0;overflow:hidden}.table-toolbar{display:flex;gap:8px;padding:13px 16px;border-bottom:1px solid var(--line)}.table-search{width:270px;height:38px;display:flex;align-items:center;gap:8px;padding:0 10px;background:var(--surface-2);border-radius:9px;color:var(--muted)}.table-search input{background:transparent;border:0;outline:0;font-size:10px}.table-toolbar .select{width:130px;min-height:38px;font-size:9px}.table-toolbar .button{margin-left:auto}.course-admin-list article{display:flex;align-items:center;justify-content:space-between;gap:18px;padding:18px 20px;border-bottom:1px solid var(--line)}.course-info>div{display:flex;align-items:center;gap:8px;color:var(--muted);font-size:8px}.course-info h2{margin:7px 0 3px;font-size:13px}.course-info p{color:var(--muted);font-size:8px}.course-actions{display:flex;gap:6px}.empty-message{padding:36px;color:var(--muted);text-align:center}@media(max-width:620px){.table-toolbar{flex-wrap:wrap}.table-search{width:100%}.table-toolbar .button{margin-left:0}.course-admin-list article{align-items:flex-start;flex-direction:column}.course-actions{width:100%}}
</style>
