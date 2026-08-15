<template>
  <AppShell>
    <nav class="breadcrumbs"><router-link to="/admin/courses">강의 관리</router-link><ChevronRight :size="13" /><span>{{ isEdit ? '강의 수정' : '새 강의 등록' }}</span></nav>
    <PageHeader :eyebrow="isEdit ? 'Edit course' : 'Create course'" :title="isEdit ? '강의 수정' : '새 강의 등록'" description="최신 강의 enum과 차시 계약을 기준으로 콘텐츠를 구성하세요."><button class="button accent" :disabled="saving" @click="saveCourse">{{ saving ? '저장 중' : saved ? '저장 완료' : '강의 저장' }} <CircleCheck v-if="saved" :size="16" /><Save v-else :size="16" /></button></PageHeader>
    <p v-if="feedback" class="save-feedback">{{ feedback }}</p>

    <div class="editor-layout">
      <main>
        <section class="panel editor-section"><div class="section-number">01</div><div class="section-content"><h2>기본 정보</h2><p>등록 요청은 title, description, language, situation, level만 전송합니다.</p><div class="field"><label>강의명</label><input v-model="form.title" class="input" required /></div><div class="field"><label>강의 설명</label><textarea v-model="form.description" class="textarea" maxlength="1000"></textarea><small>{{ form.description.length }}자</small></div><div class="form-grid triple"><div class="field"><label>언어</label><select v-model="form.language" class="select"><option v-for="item in LANGUAGE_OPTIONS" :key="item.value" :value="item.value">{{ item.label }} · {{ item.value }}</option></select></div><div class="field"><label>난이도</label><select v-model="form.level" class="select"><option v-for="item in LEVEL_OPTIONS" :key="item.value" :value="item.value">{{ item.label }} · {{ item.value }}</option></select></div><div class="field"><label>상황</label><select v-model="form.situation" class="select"><option v-for="item in SITUATION_OPTIONS" :key="item.value" :value="item.value">{{ item.label }} · {{ item.value }}</option></select></div></div><div class="payload"><code>{{ JSON.stringify(form) }}</code></div></div></section>

        <section class="panel editor-section"><div class="section-number">02</div><div class="section-content"><div class="section-head"><div><h2>강의 차시</h2><p>차시 등록은 강의 생성 후 별도 API로 순서대로 처리합니다.</p></div><button class="button small accent" @click="addLesson"><Plus :size="14" /> 차시 추가</button></div><div class="lesson-editor"><article v-for="(lesson,index) in lessons" :key="lesson.sequence"><GripVertical :size="17" /><span class="lesson-num">{{ String(lesson.sequence).padStart(2,'0') }}</span><span><input v-model="lesson.title" class="inline-input" /><small>{{ lesson.durationSeconds }}초 · {{ lesson.required ? '필수' : '선택' }} 차시</small></span><button aria-label="차시 삭제" @click="removeLesson(index)"><Trash2 :size="15" /></button></article></div></div></section>
      </main>

      <aside><section class="panel status-card"><h3>강의 상태</h3><p>생성과 상태 변경은 서로 다른 API입니다.</p><div><span>노출 상태</span><button class="status-toggle" :class="{ active: status === 'ACTIVE' }" @click="status = status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE'"><i></i>{{ status }}</button></div><small><code>POST /api/admin/courses/{id}/status?action=update-status</code></small></section><section class="panel contract-card"><ShieldCheck :size="19" /><h3>문서 계약 기준</h3><ul><li>언어 3종</li><li>난이도 4단계</li><li>상황 5종</li><li>ACTIVE / INACTIVE</li></ul></section></aside>
    </div>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ChevronRight, CircleCheck, GripVertical, Plus, Save, ShieldCheck, Trash2 } from '@lucide/vue'
import AppShell from '@/components/AppShell.vue'
import PageHeader from '@/components/PageHeader.vue'
import { courseApi } from '@/api/course.js'
import { LANGUAGE_OPTIONS, LEVEL_OPTIONS, SITUATION_OPTIONS } from '@/constants/domain.js'

const route = useRoute()
const isEdit = computed(() => Boolean(route.params.id))
const saved = ref(false)
const saving = ref(false)
const feedback = ref('')
const status = ref('ACTIVE')
const form = reactive({ title: '', description: '', language: 'ENGLISH', situation: 'CUSTOMER_MEETING', level: 'INTERMEDIATE' })
const lessons = reactive([{ title: '미팅 전, 관계를 여는 스몰토크', contentUrl: 'https://example.com/lessons/1', sequence: 1, required: true, durationSeconds: 1104 }, { title: '자연스럽게 안건 소개하기', contentUrl: 'https://example.com/lessons/2', sequence: 2, required: true, durationSeconds: 1330 }])

function addLesson() { lessons.push({ title: '새 차시', contentUrl: '', sequence: lessons.length + 1, required: true, durationSeconds: 600 }) }
function removeLesson(index) { lessons.splice(index, 1); lessons.forEach((lesson, i) => { lesson.sequence = i + 1 }) }
onMounted(async () => {
  if (!isEdit.value) return
  try {
    const [courseResponse, lessonResponse] = await Promise.all([courseApi.getAdminById(route.params.id), courseApi.getAdminLessons(route.params.id)])
    const course = courseResponse.data.data
    Object.assign(form, { title: course.title, description: course.description, language: course.language, situation: course.situation, level: course.level })
    status.value = course.status
    lessons.splice(0, lessons.length, ...lessonResponse.data.data.map((lesson) => ({ title: lesson.title, contentUrl: lesson.contentUrl, sequence: lesson.sequence, required: lesson.required, durationSeconds: lesson.durationSeconds })))
  } catch (error) { feedback.value = error.response?.data?.message || '강의 정보를 불러오지 못했습니다.' }
})

async function saveCourse() {
  feedback.value = ''
  saving.value = true
  try {
    const course = isEdit.value
      ? (await courseApi.update(route.params.id, form)).data.data
      : (await courseApi.create(form)).data.data
    if (!isEdit.value) {
      for (const lesson of lessons) await courseApi.createLesson(course.id, lesson)
    }
    if (course.status !== status.value) await courseApi.updateStatus(course.id, status.value)
    saved.value = true
    feedback.value = isEdit.value ? '강의 기본 정보가 저장되었습니다. 기존 차시는 별도 수정 API가 없어 변경하지 않았습니다.' : '강의와 차시가 서버에 등록되었습니다.'
  } catch (error) { feedback.value = error.response?.data?.message || '강의 저장에 실패했습니다.' }
  finally { saving.value = false }
}
</script>

<style scoped>
.save-feedback{margin:-8px 0 14px;color:var(--forest-2);font-size:10px}
.breadcrumbs{display:flex;align-items:center;gap:7px;margin-bottom:18px;color:var(--muted);font-size:9px}.editor-layout{display:grid;grid-template-columns:1fr 290px;gap:16px}.editor-layout main{display:grid;gap:14px}.editor-section{display:grid;grid-template-columns:45px 1fr;padding:27px}.section-number{color:var(--forest-2);font-family:var(--font-display);font-size:10px;font-weight:800}.section-content h2{font-size:17px}.section-content>p,.section-head p{margin:5px 0 22px;color:var(--muted);font-size:9px}.triple{grid-template-columns:repeat(3,1fr)}.field small{color:var(--muted);font-size:8px}.payload{padding:11px;overflow:auto;color:var(--forest-2);background:var(--mint);border-radius:9px;font-size:8px}.section-head{display:flex;align-items:flex-start;justify-content:space-between}.lesson-editor{border-top:1px solid var(--line)}.lesson-editor article{display:grid;grid-template-columns:auto 30px 1fr auto;align-items:center;gap:10px;padding:13px 4px;border-bottom:1px solid var(--line)}.lesson-editor>article>svg{color:var(--subtle)}.lesson-num{color:var(--forest-2);font-family:var(--font-display);font-size:9px}.inline-input{width:100%;padding:3px 0;background:transparent;border:0;font-size:10px;font-weight:700}.lesson-editor small{display:block;color:var(--muted);font-size:8px}.lesson-editor button{color:var(--muted);background:transparent}aside{display:grid;align-content:start;gap:14px}.status-card,.contract-card{padding:22px}.status-card h3,.contract-card h3{font-size:13px}.status-card>p{margin:5px 0 17px;color:var(--muted);font-size:8px}.status-card>div{display:flex;align-items:center;justify-content:space-between;padding:12px 0;border-top:1px solid var(--line);font-size:9px}.status-toggle{display:flex;align-items:center;gap:6px;padding:5px 8px;color:var(--muted);background:var(--surface-2);border-radius:7px;font-size:8px;font-weight:700}.status-toggle i{width:7px;height:7px;background:var(--subtle);border-radius:50%}.status-toggle.active{color:var(--forest);background:var(--mint)}.status-toggle.active i{background:var(--forest-2)}.status-card>small{color:var(--muted);font-size:7px}.contract-card>svg{color:var(--forest-2)}.contract-card h3{margin:12px 0}.contract-card ul{display:grid;gap:8px;padding-left:15px;color:var(--muted);font-size:9px}@media(max-width:900px){.editor-layout{grid-template-columns:1fr}.editor-layout aside{grid-template-columns:1fr 1fr}}@media(max-width:620px){.editor-section{grid-template-columns:1fr;padding:20px}.section-number{margin-bottom:10px}.triple,.editor-layout aside{grid-template-columns:1fr}.section-head{align-items:flex-start;flex-direction:column;gap:10px}}
</style>
