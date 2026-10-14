<template>
  <AppShell>
    <PageHeader eyebrow="My learning overview" :title="`${userName}님의 학습 현황`" description="서버에 저장된 수강 및 진도 상태를 확인하세요.">
      <router-link class="button accent" to="/courses">강의 찾기</router-link>
    </PageHeader>
    <AsyncState v-if="state==='loading'" type="loading" title="학습 현황을 불러오고 있어요" description="수강 데이터와 강의 정보를 확인하고 있습니다."/>
    <AsyncState v-else-if="state==='error'" type="error" title="학습 현황을 불러오지 못했어요" :description="errorMessage" @retry="load"/>
    <template v-else>
      <section class="metric-grid">
        <article class="metric card"><div class="metric-head"><span>전체 수강</span></div><strong class="metric-value">{{ enrollments.length }}개</strong><p class="trend">실제 신청 강의</p></article>
        <article class="metric card"><div class="metric-head"><span>학습 중</span></div><strong class="metric-value">{{ learningCount }}개</strong><p class="trend">ENROLLED·LEARNING</p></article>
        <article class="metric card"><div class="metric-head"><span>완료</span></div><strong class="metric-value">{{ completedCount }}개</strong><p class="trend">COMPLETED</p></article>
        <article class="metric card"><div class="metric-head"><span>평균 진도율</span></div><strong class="metric-value">{{ averageProgress }}%</strong><p class="trend">서버 계산값</p></article>
      </section>
      <section class="panel recent-panel"><div><h2>최근 수강 강의</h2><router-link to="/learning">전체 내 학습 보기</router-link></div><div v-if="enrollments.length" class="course-list"><article v-for="item in enrollments.slice(0,3)" :key="item.enrollmentId"><span><strong>{{ item.title }}</strong><small>{{ item.status }} · {{ item.progressRate }}%</small></span><router-link class="button small" :to="`/courses/${item.courseId}`">강의 보기</router-link></article></div><p v-else class="empty-message">아직 신청한 강의가 없습니다.</p></section>
    </template>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import AppShell from '@/components/AppShell.vue'
import PageHeader from '@/components/PageHeader.vue'
import AsyncState from '@/components/AsyncState.vue'
import { enrollmentApi } from '@/api/enrollment.js'
import { courseApi } from '@/api/course.js'
import { useAuthStore } from '@/store/auth.js'

const auth=useAuthStore(),state=ref('loading'),errorMessage=ref('잠시 후 다시 시도해 주세요.'),enrollments=ref([])
const userName=computed(()=>auth.user?.name||'사용자')
const learningCount=computed(()=>enrollments.value.filter(item=>item.status!=='COMPLETED').length)
const completedCount=computed(()=>enrollments.value.filter(item=>item.status==='COMPLETED').length)
const averageProgress=computed(()=>enrollments.value.length?Math.round(enrollments.value.reduce((sum,item)=>sum+Number(item.progressRate),0)/enrollments.value.length):0)
async function load(){state.value='loading';try{const list=(await enrollmentApi.getMyEnrollments()).data.data;enrollments.value=await Promise.all(list.map(async item=>{const course=(await courseApi.getById(item.courseId)).data.data;return{...item,title:course.title}}));state.value='ready'}catch(error){enrollments.value=[];errorMessage.value=error.response?.data?.message||'학습 현황을 불러오지 못했습니다.';state.value='error'}}
onMounted(load)
</script>

<style scoped>.recent-panel{margin-top:16px}.recent-panel>div:first-child,.course-list article{display:flex;align-items:center;justify-content:space-between}.recent-panel h2{font-size:16px}.recent-panel a{color:var(--forest-2);font-size:10px}.course-list{margin-top:14px}.course-list article{padding:14px 0;border-top:1px solid var(--line)}.course-list strong,.course-list small{display:block}.course-list small{margin-top:4px;color:var(--muted);font-size:9px}.empty-message{padding:34px 0;color:var(--muted);text-align:center}</style>
