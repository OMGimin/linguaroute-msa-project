<template>
  <AppShell>
    <PageHeader eyebrow="People & seats" title="직원과 초대 관리" description="좌석 범위 안에서 직원을 초대하고 계정 상태를 관리하세요.">
      <button class="button accent" @click="inviteModal=true"><UserPlus :size="16"/> 초대코드 생성</button>
    </PageHeader>

    <div v-if="toast" class="toast" role="status"><CircleCheck :size="16"/> {{ toast }}</div>

    <section class="seat-card">
      <div><span class="seat-icon"><Armchair :size="21"/></span><span><strong>{{ seats.used }} / {{ seats.purchased }}</strong><small>사용 중인 좌석</small></span></div>
      <div class="seat-track"><div class="progress"><span :style="`width:${seatUsagePercent}%`"></span></div><p><span>사용 {{ seats.used }}석</span><span>잔여 {{ seats.remaining }}석</span></p></div>
      <router-link to="/company/subscription">좌석 늘리기 <ArrowUpRight :size="15"/></router-link>
    </section>

    <div class="management-tabs">
      <button :class="{active:tab==='employees'}" @click="tab='employees'">직원 목록 <span>{{ employeeRows.length }}</span></button>
      <button :class="{active:tab==='invites'}" @click="tab='invites'">초대코드 <span>{{ invitationRows.length }}</span></button>
    </div>

    <section v-if="tab==='employees'" class="panel data-panel">
      <div class="table-toolbar">
        <div class="table-search"><Search :size="16"/><input v-model="keyword" placeholder="이름 또는 이메일 검색"/></div>
        <select v-model="statusFilter" class="select"><option>전체 상태</option><option value="ACTIVE">활성</option><option value="INACTIVE">비활성</option></select>
      </div>
      <div v-if="filteredEmployees.length" class="table-wrap">
        <table class="data-table">
          <thead><tr><th>직원</th><th>가입일</th><th>상태</th><th>관리</th></tr></thead>
          <tbody><tr v-for="employee in filteredEmployees" :key="employee.userId || employee.email">
            <td><div class="person"><span class="avatar">{{ employee.name[0] }}</span><span><strong>{{ employee.name }}</strong><span>{{ employee.email }}</span></span></div></td>
            <td>{{ employee.joined }}</td><td><span class="tag" :class="employee.status==='ACTIVE'||employee.status==='활성'?'':'amber'">{{ employee.status==='ACTIVE'||employee.status==='활성'?'활성':'비활성' }}</span></td>
            <td><div class="row-actions"><button class="button small" @click="toggleEmployee(employee)">{{ employee.status==='ACTIVE'||employee.status==='활성'?'비활성화':'활성화' }}</button><button class="icon-action danger" aria-label="소속 해제" @click="openEmployeeDialog(employee)"><UserRoundMinus :size="15"/></button></div></td>
          </tr></tbody>
        </table>
      </div>
      <div v-else class="empty-state"><SearchX :size="28"/><h3>조건에 맞는 직원이 없습니다</h3><p>검색어나 상태 필터를 변경해 보세요.</p><button class="button small" @click="keyword='';statusFilter='전체 상태'">필터 초기화</button></div>
    </section>

    <section v-else class="panel data-panel">
      <div class="table-toolbar"><div><h2>일회용 초대코드</h2><p>사용되거나 만료된 코드는 다시 사용할 수 없습니다.</p></div><button class="button small accent" @click="inviteModal=true"><Plus :size="15"/> 새 코드</button></div>
      <div class="invite-list">
        <article v-for="invite in invitationRows" :key="invite.code">
          <span class="invite-icon"><TicketCheck :size="19"/></span>
          <span><strong>{{ invite.code || invite.codeMasked }}</strong><small>{{ invite.created }} 생성 · {{ invite.expires }} 만료</small></span>
          <span class="tag" :class="isUnused(invite)?'':invite.status==='USED'?'gray':'red'">{{ invitationStatusLabel(invite.status) }}</span>
          <button class="button small" :disabled="!invite.code || !isUnused(invite)" @click="copyCode(invite.code)"><Copy :size="14"/> {{ copied===invite.code?'복사됨':'복사' }}</button>
          <button v-if="isUnused(invite)" class="icon-action danger" aria-label="초대코드 폐기" @click="discardInvitation(invite)"><Trash2 :size="15"/></button>
          <button v-else class="button small" @click="reissueInvitation(invite)"><RefreshCw :size="14"/> 재발급</button>
        </article>
      </div>
    </section>

    <div v-if="inviteModal" class="modal-layer" @click.self="inviteModal=false">
      <div class="modal card" role="dialog" aria-modal="true" aria-labelledby="invite-title">
        <button class="modal-close" aria-label="닫기" @click="inviteModal=false"><X :size="18"/></button>
        <span class="modal-icon"><TicketPlus :size="23"/></span><h2 id="invite-title">초대코드 만들기</h2><p>한 명의 직원이 한 번만 사용할 수 있는 코드입니다.</p>
        <div class="field"><label>유효 기간</label><select v-model.number="expiresInDays" class="select"><option :value="7">7일</option><option :value="3">3일</option><option :value="14">14일</option></select></div>
        <div class="modal-note"><ShieldCheck :size="16"/> 활성 구독과 잔여 좌석이 확인되어야 가입할 수 있습니다.</div>
        <div v-if="generatedCode" class="generated-code"><small>새 초대코드</small><strong>{{ generatedCode }}</strong><button @click="copyCode(generatedCode)"><Copy :size="15"/> 복사</button></div>
        <div class="modal-actions"><button class="button" @click="inviteModal=false">닫기</button><button class="button accent" @click="generateInvitation">{{ generatedCode?'코드 하나 더 생성':'코드 생성' }}</button></div>
      </div>
    </div>

    <div v-if="employeeDialog" class="modal-layer" @click.self="employeeDialog=null">
      <div class="modal card confirm-modal" role="alertdialog" aria-modal="true"><span class="modal-icon danger"><UserRoundMinus :size="22"/></span><h2>소속을 해제하시겠어요?</h2><p>{{ employeeDialog.name }}님의 기업 소속과 좌석이 회수됩니다. 계정은 삭제되지 않습니다.</p><div class="modal-actions"><button class="button" @click="employeeDialog=null">취소</button><button class="button danger" @click="releaseEmployee">소속 해제</button></div></div>
    </div>
  </AppShell>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { UserPlus, Armchair, ArrowUpRight, Search, Plus, TicketCheck, Copy, X, TicketPlus, ShieldCheck, CircleCheck, UserRoundMinus, SearchX, Trash2, RefreshCw } from '@lucide/vue'
import AppShell from '@/components/AppShell.vue'
import PageHeader from '@/components/PageHeader.vue'
import { companyApi } from '@/api/company.js'

const tab=ref('employees'), inviteModal=ref(false), copied=ref(''), generatedCode=ref(''), toast=ref(''), keyword=ref(''), statusFilter=ref('전체 상태'), employeeDialog=ref(null), expiresInDays=ref(7)
const employeeRows=ref([])
const invitationRows=ref([])
const seats=ref({purchased:0,used:0,remaining:0})
const seatUsagePercent=computed(()=>seats.value.purchased?Math.min(100,seats.value.used/seats.value.purchased*100):0)
const filteredEmployees=computed(()=>employeeRows.value.filter(item=>(!keyword.value||item.name.includes(keyword.value)||item.email.includes(keyword.value))&&(statusFilter.value==='전체 상태'||item.status===statusFilter.value)))

function notify(message){toast.value=message;window.setTimeout(()=>toast.value='',2200)}
function copyCode(code){copied.value=code;navigator.clipboard?.writeText(code);notify('초대코드를 클립보드에 복사했습니다.')}
async function toggleEmployee(employee){const next=employee.status==='ACTIVE'?'INACTIVE':'ACTIVE';try{await companyApi.updateEmployeeStatus(employee.userId,next);await loadEmployees();notify(`${employee.name}님의 계정을 ${next==='ACTIVE'?'활성':'비활성'} 상태로 변경했습니다.`)}catch(error){notify(error.response?.data?.message||'직원 상태를 변경하지 못했습니다.')}}
function openEmployeeDialog(employee){employeeDialog.value=employee}
async function releaseEmployee(){try{await companyApi.updateEmployeeStatus(employeeDialog.value.userId,'RELEASED');await loadEmployees();notify(`${employeeDialog.value.name}님의 소속을 해제하고 좌석을 회수했습니다.`);employeeDialog.value=null}catch(error){notify(error.response?.data?.message||'직원 소속을 해제하지 못했습니다.')}}
function isUnused(invite){return invite.status==='UNUSED'||invite.status==='미사용'}
function invitationStatusLabel(status){return ({UNUSED:'미사용',USED:'사용됨',EXPIRED:'만료',REVOKED:'폐기'})[status]||status}
function formatDate(value){return value?value.slice(0,10).replaceAll('-','.'):'-'}
function toInvitationRow(invite){return {invitationId:invite.invitationId,code:invite.code,codeMasked:invite.codeMasked,status:invite.status,created:formatDate(invite.createdAt),expires:formatDate(invite.expiresAt)}}
async function loadInvitations(){try{invitationRows.value=(await companyApi.getInvitations()).data.data.map(toInvitationRow)}catch(error){invitationRows.value=[];notify(error.response?.data?.message||'초대코드 목록을 불러오지 못했습니다.')}}
function toEmployeeRow(employee){return {...employee,joined:formatDate(employee.joinedAt)}}
async function loadEmployees(){try{const [employeeResponse,seatResponse]=await Promise.all([companyApi.getEmployees(),companyApi.getSeats()]);employeeRows.value=employeeResponse.data.data.map(toEmployeeRow);seats.value=seatResponse.data.data}catch(error){employeeRows.value=[];seats.value={purchased:0,used:0,remaining:0};notify(error.response?.data?.message||'직원 또는 좌석 정보를 불러오지 못했습니다.')}}
async function discardInvitation(invite){try{await companyApi.revokeInvitation(invite.invitationId);await loadInvitations();notify('초대코드를 폐기했습니다.')}catch(error){notify(error.response?.data?.message||'초대코드를 폐기하지 못했습니다.')}}
async function reissueInvitation(invite){try{const created=toInvitationRow((await companyApi.reissueInvitation(invite.invitationId)).data.data);generatedCode.value=created.code;await loadInvitations();invitationRows.value=invitationRows.value.map(row=>row.invitationId===created.invitationId?created:row);notify('새 초대코드를 발급했습니다.')}catch(error){notify(error.response?.data?.message||'초대코드를 재발급하지 못했습니다.')}}
async function generateInvitation(){try{const created=toInvitationRow((await companyApi.createInvitation(expiresInDays.value)).data.data);generatedCode.value=created.code;await loadInvitations();invitationRows.value=invitationRows.value.map(row=>row.invitationId===created.invitationId?created:row)}catch(error){notify(error.response?.data?.message||'초대코드를 생성하지 못했습니다.')}}
onMounted(()=>{loadInvitations();loadEmployees()})
</script>

<style scoped>
.toast{position:fixed;top:86px;right:28px;z-index:90;display:flex;align-items:center;gap:8px;padding:12px 15px;color:var(--forest);background:var(--mint);border:1px solid #b9d7c4;border-radius:11px;box-shadow:var(--shadow-md);font-size:10px;font-weight:700}.seat-card{display:grid;grid-template-columns:auto minmax(0,1fr) auto;align-items:center;gap:28px;padding:17px 20px;background:var(--surface);border:1px solid var(--line);border-radius:15px}.seat-card>div:first-child{display:flex;align-items:center;gap:12px}.seat-icon{width:40px;height:40px;display:grid;place-items:center;color:var(--forest);background:var(--mint);border-radius:11px}.seat-card strong,.seat-card small{display:block}.seat-card strong{font-family:var(--font-display);font-size:18px}.seat-card small{color:var(--muted);font-size:9px}.seat-track p{display:flex;justify-content:space-between;margin-top:5px;color:var(--muted);font-size:8px}.seat-card>a{display:flex;align-items:center;gap:5px;color:var(--forest-2);font-size:10px;font-weight:700}.management-tabs{display:flex;gap:4px;margin-top:18px;border-bottom:1px solid var(--line)}.management-tabs button{padding:10px 14px;color:var(--muted);background:transparent;border-bottom:2px solid transparent;font-size:11px;font-weight:700}.management-tabs button.active{color:var(--forest);border-color:var(--forest)}.management-tabs span{margin-left:4px}.data-panel{padding:0;overflow:hidden;border-top-left-radius:0}.table-toolbar{min-height:62px;display:flex;align-items:center;gap:8px;padding:12px 16px;border-bottom:1px solid var(--line)}.table-search{width:240px;height:38px;display:flex;align-items:center;gap:8px;padding:0 10px;background:var(--surface-2);border-radius:9px;color:var(--muted)}.table-search input{min-width:0;background:transparent;border:0;outline:0;font-size:10px}.table-toolbar .select{width:120px;min-height:38px;font-size:9px}.table-toolbar>.button{margin-left:auto}.table-progress{display:flex;align-items:center;gap:7px}.table-progress .progress{width:65px}.table-progress b{font-size:8px}.row-actions{display:flex;gap:5px}.row-actions .button{white-space:nowrap}.icon-action{width:34px;height:34px;display:grid;place-items:center;color:var(--muted);background:var(--surface-2);border-radius:8px}.icon-action.danger{color:var(--danger)}.invite-list article{display:grid;grid-template-columns:auto minmax(0,1fr) auto auto auto;align-items:center;gap:12px;padding:14px 18px;border-bottom:1px solid var(--line)}.invite-list article:last-child{border-bottom:0}.invite-icon{width:37px;height:37px;display:grid;place-items:center;color:var(--forest);background:var(--mint);border-radius:10px}.invite-list strong,.invite-list small{display:block}.invite-list strong{font-family:var(--font-display);font-size:13px;letter-spacing:.08em}.invite-list small{margin-top:3px;color:var(--muted);font-size:8px}.invite-list button:disabled{opacity:.4}.empty-state{display:grid;justify-items:center;padding:58px 20px;color:var(--muted);text-align:center}.empty-state>svg{color:var(--subtle)}.empty-state h3{margin:12px 0 4px;color:var(--ink);font-size:14px}.empty-state p{margin-bottom:16px;font-size:9px}.modal-layer{position:fixed;inset:0;z-index:100;display:grid;place-items:center;padding:20px;background:rgba(11,26,18,.55);backdrop-filter:blur(4px)}.modal{position:relative;width:min(410px,100%);padding:30px}.modal-close{position:absolute;top:15px;right:15px;color:var(--muted);background:transparent}.modal-icon{width:48px;height:48px;display:grid;place-items:center;color:var(--forest);background:var(--lime);border-radius:14px}.modal-icon.danger{color:var(--danger);background:var(--danger-soft)}.modal h2{margin:18px 0 5px;font-size:20px}.modal>p{margin-bottom:22px;color:var(--muted);font-size:10px}.modal-note{display:flex;gap:7px;margin-top:14px;padding:11px;color:var(--forest);background:var(--mint);border-radius:9px;font-size:9px}.generated-code{display:grid;grid-template-columns:1fr auto;gap:5px;margin-top:14px;padding:14px;background:var(--surface-2);border-radius:11px}.generated-code small{grid-column:1/-1;color:var(--muted);font-size:8px}.generated-code strong{font-family:var(--font-display);font-size:17px;letter-spacing:.08em}.generated-code button{display:flex;align-items:center;gap:4px;color:var(--forest);background:transparent;font-size:9px;font-weight:700}.modal-actions{display:flex;justify-content:flex-end;gap:7px;margin-top:22px}.confirm-modal{text-align:center}.confirm-modal .modal-icon{margin:auto}.confirm-modal>p{line-height:1.7}
@media(max-width:720px){.seat-card{grid-template-columns:1fr auto}.seat-track{display:none}.table-toolbar{align-items:stretch;flex-wrap:wrap}.table-search{width:100%}.table-toolbar>.button{margin-left:0}.invite-list article{grid-template-columns:auto 1fr auto}.invite-list .button{display:none}.row-actions .button{font-size:0}.toast{right:14px;left:14px}.data-table{min-width:760px}}
</style>
