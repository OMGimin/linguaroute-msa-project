<template><AppShell><PageHeader eyebrow="My learning" title="내 학습" description="신청한 강의와 진행 상황을 한눈에 확인하고 이어서 학습하세요."/><div class="learning-tabs"><button v-for="item in tabs" :key="item" :class="{active:tab===item}" @click="tab=item">{{ item }} <span>{{ counts[item] }}</span></button></div><section class="learning-grid"><article v-for="course in visibleCourses" :key="course.enrollmentId" class="learning-card card"><div class="learning-thumb" :class="`tone-${course.tone}`"><img :src="course.image" :alt="course.title"/><span class="tag" :class="course.progress===100?'blue':''">{{ course.progress===100?'수강 완료':'학습 중' }}</span></div><div class="learning-body"><div class="learning-title"><span><small>{{ course.language }} · {{ course.level }}</small><h2>{{ course.title }}</h2></span><button aria-label="더보기"><MoreHorizontal :size="18"/></button></div><div class="learning-stats"><span><b>{{ course.progress }}%</b>진도율</span><span><b>{{ course.completedLessons }} / {{ course.totalLessons }}</b>완료 필수 차시</span><span><b>{{ course.completedAt || course.startedAt || course.enrolledAt || '-' }}</b>최근 학습</span></div><div class="progress"><span :style="`width:${course.progress}%`"></span></div><div class="next-lesson"><span class="route-dot"><Play :size="14" fill="currentColor"/></span><span><small>다음 차시</small><strong>{{ course.progress===100?'모든 차시를 완료했어요':'다음 필수 차시를 학습하세요.' }}</strong></span></div><router-link class="button" :class="course.progress===100?'':'primary'" :to="course.progress===100 || !course.nextLessonId ? `/courses/${course.id}` : `/learning/${course.enrollmentId}/lessons/${course.nextLessonId}`">{{ course.progress===100?'과정 다시 보기':'이어서 학습하기' }} <ArrowRight :size="16"/></router-link></div></article><router-link to="/courses" class="empty-card"><span><Plus :size="23"/></span><h3>새로운 강의 시작하기</h3><p>목표에 맞는 다음 강의를 찾아보세요.</p></router-link></section></AppShell></template>
<script setup>
import { computed, onMounted, ref } from 'vue'
import { MoreHorizontal,Play,ArrowRight,Plus } from '@lucide/vue'
import AppShell from '@/components/AppShell.vue'
import PageHeader from '@/components/PageHeader.vue'
import { courseApi } from '@/api/course.js'
import { enrollmentApi } from '@/api/enrollment.js'
import { buildLearningCourse } from '@/domain/learning.js'

const tab = ref('전체')
const liveCourses = ref([])
const activeCourses = computed(() => liveCourses.value)
const counts = computed(() => ({
  전체: activeCourses.value.length,
  '학습 중': activeCourses.value.filter((course) => course.progress < 100).length,
  완료: activeCourses.value.filter((course) => course.progress === 100).length
}))
const tabs = ['전체', '학습 중', '완료']
const visibleCourses = computed(() => tab.value === '전체' ? activeCourses.value : tab.value === '완료' ? activeCourses.value.filter((course) => course.progress === 100) : activeCourses.value.filter((course) => course.progress < 100))

onMounted(async () => {
  try {
    const response = await enrollmentApi.getMyEnrollments()
    liveCourses.value = await Promise.all(response.data.data.map(async (enrollment) => {
      const [courseResponse, lessonResponse, detailResponse] = await Promise.all([
        courseApi.getById(enrollment.courseId),
        courseApi.getLessons(enrollment.courseId),
        enrollmentApi.getById(enrollment.enrollmentId)
      ])
      return buildLearningCourse(courseResponse.data.data, enrollment, lessonResponse.data.data, detailResponse.data.data)
    }))
  } catch (_) {
    liveCourses.value = []
  }
})
</script>
<style scoped>.learning-tabs{display:flex;gap:4px;margin-bottom:18px;border-bottom:1px solid var(--line)}.learning-tabs button{padding:10px 15px;color:var(--muted);background:transparent;border-bottom:2px solid transparent;font-size:11px;font-weight:700}.learning-tabs button.active{color:var(--forest);border-color:var(--forest)}.learning-tabs span{margin-left:4px;color:var(--subtle);font-size:9px}.learning-grid{display:grid;grid-template-columns:repeat(2,1fr);gap:14px}.learning-card{display:grid;grid-template-columns:190px 1fr;overflow:hidden}.learning-thumb{position:relative;display:grid;place-items:center;background:#dce9df}.learning-thumb img{width:80%;height:80%;object-fit:contain;mix-blend-mode:multiply}.learning-thumb>.tag{position:absolute;top:12px;left:12px}.tone-blue{background:#dce7f3}.tone-purple{background:#e8e1f0}.learning-body{padding:20px}.learning-title{display:flex;justify-content:space-between}.learning-title small{color:var(--forest-2);font-size:9px;font-weight:700}.learning-title h2{margin-top:5px;font-size:15px}.learning-title button{align-self:flex-start;color:var(--subtle);background:transparent}.learning-stats{display:flex;gap:24px;margin:19px 0 11px}.learning-stats span,.learning-stats b{display:block}.learning-stats span{color:var(--muted);font-size:8px}.learning-stats b{margin-bottom:2px;color:var(--ink);font-size:10px}.next-lesson{display:flex;align-items:center;gap:10px;margin:16px 0;padding:10px;background:var(--surface-2);border-radius:10px}.route-dot{width:27px;height:27px;display:grid;place-items:center;color:var(--forest);background:var(--lime);border-radius:50%}.next-lesson small,.next-lesson strong{display:block}.next-lesson small{color:var(--muted);font-size:8px}.next-lesson strong{font-size:9px}.learning-body>.button{width:100%;min-height:38px;font-size:10px}.empty-card{min-height:265px;display:grid;place-content:center;justify-items:center;padding:25px;color:var(--muted);border:1px dashed var(--line-strong);border-radius:18px;text-align:center}.empty-card>span{width:48px;height:48px;display:grid;place-items:center;color:var(--forest);background:var(--mint);border-radius:50%}.empty-card h3{margin:13px 0 5px;color:var(--ink);font-size:13px}.empty-card p{font-size:10px}@media(max-width:1150px){.learning-grid{grid-template-columns:1fr}}@media(max-width:620px){.learning-card{grid-template-columns:1fr}.learning-thumb{height:170px}.learning-stats{gap:13px}}</style>
