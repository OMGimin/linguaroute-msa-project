<template>
  <AppShell>
    <PageHeader eyebrow="Course library" title="강의 찾기" description="업무 상황과 현재 수준에 맞는 외국어 과정을 찾아보세요.">
      <router-link class="button accent" to="/recommendations"><Sparkles :size="16" /> AI 추천받기</router-link>
    </PageHeader>

    <section class="filter-panel panel">
      <div class="search"><Search :size="18" /><input v-model="keyword" placeholder="강의명 또는 학습 목표로 검색" /></div>
      <div class="filters">
        <select v-model="language" class="select"><option value="">모든 언어</option><option v-for="item in LANGUAGE_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</option></select>
        <select v-model="situation" class="select"><option value="">모든 상황</option><option v-for="item in SITUATION_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</option></select>
        <select v-model="level" class="select"><option value="">모든 난이도</option><option v-for="item in LEVEL_OPTIONS" :key="item.value" :value="item.value">{{ item.label }}</option></select>
        <button class="button small" @click="clearFilters"><SlidersHorizontal :size="15" /> 초기화</button>
      </div>
    </section>

    <div class="result-head">
      <p><strong>{{ filtered.length }}</strong>개의 강의</p>
    </div>

    <AsyncState v-if="viewState === 'loading'" type="loading" title="강의를 불러오고 있어요" description="등록된 ACTIVE 강의를 확인하고 있습니다." />
    <AsyncState v-else-if="viewState === 'error'" type="error" title="강의를 불러오지 못했어요" :description="loadError" @retry="loadCourses" />
    <AsyncState v-else-if="viewState === 'empty' || !filtered.length" type="empty" title="조건에 맞는 강의가 없어요" description="검색어나 필터를 바꾸면 더 많은 강의를 찾을 수 있어요." />
    <template v-else>
      <section class="course-grid"><CourseTile v-for="course in filtered" :key="course.id" :course="course" /></section>
      <nav v-if="totalPages > 1" class="pagination" aria-label="페이지 이동"><button :disabled="page===0" @click="goPage(page-1)"><ChevronLeft :size="16" /></button><button v-for="item in totalPages" :key="item" :class="{active:page===item-1}" @click="goPage(item-1)">{{ item }}</button><button :disabled="page===totalPages-1" @click="goPage(page+1)"><ChevronRight :size="16" /></button></nav>
    </template>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'
import { ChevronLeft, ChevronRight, Search, SlidersHorizontal, Sparkles } from '@lucide/vue'
import AppShell from '@/components/AppShell.vue'
import AsyncState from '@/components/AsyncState.vue'
import CourseTile from '@/components/CourseTile.vue'
import PageHeader from '@/components/PageHeader.vue'
import { LANGUAGE_OPTIONS, LEVEL_OPTIONS, SITUATION_OPTIONS } from '@/constants/domain.js'
import { courseApi } from '@/api/course.js'

const keyword = ref('')
const language = ref('')
const situation = ref('')
const level = ref('')
const viewState = ref('ready')
const liveCourses = ref([])
const loadError = ref('잠시 후 다시 시도해 주세요.')
const page = ref(0)
const totalPages = ref(1)

const filtered = computed(() => liveCourses.value.filter((course) => {
  const query = keyword.value.trim().toLowerCase()
  return (!query || `${course.title} ${course.description}`.toLowerCase().includes(query))
    && (!language.value || course.languageCode === language.value)
    && (!situation.value || course.situationCode === situation.value)
    && (!level.value || course.levelCode === level.value)
}))

async function loadCourses() {
  viewState.value = 'loading'
  try {
    const response = await courseApi.getCourses({ keyword: keyword.value || undefined, language: language.value || undefined, situation: situation.value || undefined, level: level.value || undefined, page: page.value, size: 20 })
    liveCourses.value = response.data.data.content.map((course) => ({
      id: course.courseId, title: course.title, description: '', image: '', duration: '-', students: '-', languageCode: course.language,
      language: course.language, situationCode: course.situation, situation: course.situation,
      levelCode: course.level, level: course.level, status: course.status, tone: 'green'
    }))
    totalPages.value = Math.max(1, Math.ceil(response.data.data.totalElements / response.data.data.size))
    viewState.value = 'ready'
  } catch (error) {
    liveCourses.value = []
    loadError.value = error.response?.data?.message || '잠시 후 다시 시도해 주세요.'
    viewState.value = 'error'
  }
}

onMounted(loadCourses)
watch([keyword, language, situation, level], () => { page.value = 0; loadCourses() })

function goPage(nextPage) {
  page.value = nextPage
  loadCourses()
}

function clearFilters() {
  keyword.value = ''
  language.value = ''
  situation.value = ''
  level.value = ''
  loadCourses()
}
</script>

<style scoped>
.filter-panel{display:flex;gap:12px;margin-bottom:22px;padding:14px}.search{height:44px;display:flex;align-items:center;gap:9px;flex:1;padding:0 12px;background:var(--surface-2);border-radius:10px;color:var(--muted)}.search input{width:100%;background:transparent;border:0;outline:0;font-size:12px}.filters{display:flex;gap:8px}.filters .select{width:130px;min-height:44px;font-size:11px}.result-head{display:flex;align-items:center;justify-content:space-between;margin:0 2px 14px;color:var(--muted);font-size:11px}.result-head strong{color:var(--ink);font-size:13px}.preview-control{display:flex;align-items:center;gap:8px}.preview-control span{font-size:9px}.preview-control select{padding:7px 26px 7px 9px;color:var(--muted);background:var(--surface);border:1px solid var(--line);border-radius:8px;font-size:9px}.course-grid{display:grid;grid-template-columns:repeat(3,1fr);gap:14px}.pagination{display:flex;justify-content:center;gap:4px;margin-top:32px}.pagination button{width:35px;height:35px;display:grid;place-items:center;color:var(--muted);background:transparent;border-radius:8px}.pagination button.active{color:white;background:var(--forest)}@media(max-width:1100px){.filter-panel{align-items:stretch;flex-direction:column}.course-grid{grid-template-columns:repeat(2,1fr)}}@media(max-width:680px){.filters{display:grid;grid-template-columns:1fr 1fr}.filters .select{width:100%}.course-grid{grid-template-columns:1fr}.result-head{align-items:flex-start;flex-direction:column;gap:9px}}
</style>
