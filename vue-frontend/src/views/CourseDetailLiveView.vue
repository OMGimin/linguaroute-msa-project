<template>
  <AppShell>
    <AsyncState v-if="state==='loading'" type="loading" title="강의 정보를 불러오고 있어요" description="강의와 차시를 확인하고 있습니다."/>
    <AsyncState v-else-if="state==='error'" type="error" title="강의를 불러오지 못했어요" :description="errorMessage" @retry="load"/>
    <template v-else>
      <PageHeader eyebrow="Course detail" :title="course.title" :description="course.description||'등록된 강의 상세 정보입니다.'"><button class="button accent" :disabled="submitting" @click="enroll">{{ submitting?'신청 중':'수강 신청' }}</button></PageHeader>
      <p v-if="feedback" class="panel feedback" role="status">{{ feedback }}</p>
      <section class="panel"><div class="tags"><span class="tag">{{ course.language }}</span><span class="tag gray">{{ course.level }}</span><span class="tag gray">{{ course.situation }}</span><span class="tag" :class="course.status==='ACTIVE'?'':'gray'">{{ course.status }}</span></div><h2>차시 목록</h2><div v-if="lessons.length" class="lesson-list"><article v-for="lesson in lessons" :key="lesson.lessonId"><span><strong>{{ lesson.orderNo }}. {{ lesson.title }}</strong><small>{{ lesson.required?'필수':'선택' }}</small></span></article></div><p v-else class="empty-message">등록된 차시가 없습니다.</p></section>
    </template>
  </AppShell>
</template>
<script setup>
import { onMounted, ref } from 'vue';import { useRoute } from 'vue-router';import AppShell from '@/components/AppShell.vue';import PageHeader from '@/components/PageHeader.vue';import AsyncState from '@/components/AsyncState.vue';import { courseApi } from '@/api/course.js';import { enrollmentApi } from '@/api/enrollment.js'
const route=useRoute(),state=ref('loading'),errorMessage=ref(''),course=ref({}),lessons=ref([]),submitting=ref(false),feedback=ref('')
async function load(){state.value='loading';try{const [courseResponse,lessonResponse]=await Promise.all([courseApi.getById(route.params.id),courseApi.getLessons(route.params.id)]);course.value=courseResponse.data.data;lessons.value=lessonResponse.data.data;state.value='ready'}catch(error){errorMessage.value=error.response?.data?.message||'강의 정보를 불러오지 못했습니다.';state.value='error'}}
async function enroll(){submitting.value=true;feedback.value='';try{const response=await enrollmentApi.enroll(Number(route.params.id));feedback.value=`수강 신청이 완료되었습니다. 수강 ID: ${response.data.data.enrollmentId}`}catch(error){feedback.value=error.response?.data?.message||'수강 신청에 실패했습니다.'}finally{submitting.value=false}}
onMounted(load)
</script>
<style scoped>.feedback{margin-bottom:14px;color:var(--forest-2)}.tags{display:flex;gap:6px;margin-bottom:20px}.lesson-list article{padding:14px 0;border-top:1px solid var(--line)}.lesson-list strong,.lesson-list small{display:block}.lesson-list small{margin-top:4px;color:var(--muted)}.empty-message{padding:28px;color:var(--muted);text-align:center}</style>
