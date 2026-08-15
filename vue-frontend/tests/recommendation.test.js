import test from 'node:test'
import assert from 'node:assert/strict'

import {
  buildRecommendationRequest,
  mapRecommendedCourses,
  recommendationMode
} from '../src/domain/recommendation.js'

test('화면의 한글 선택값을 추천 API enum 요청으로 변환한다', () => {
  assert.deepEqual(
    buildRecommendationRequest({
      language: '영어',
      level: '중급',
      job: '글로벌 세일즈',
      situation: '고객 미팅',
      goal: '제품 사양과 기술 용어를 해외 고객에게 자연스럽게 설명하고 싶어요.'
    }),
    {
      language: 'ENGLISH',
      level: 'INTERMEDIATE',
      job: 'GLOBAL_SALES',
      situation: 'CUSTOMER_MEETING',
      goal: '제품 사양과 기술 용어를 해외 고객에게 자연스럽게 설명하고 싶어요.'
    }
  )
})

test('추천 API 결과를 CourseTile 표시 구조로 변환한다', () => {
  assert.deepEqual(
    mapRecommendedCourses([
      {
        courseId: 9105,
        title: '임원 보고 영어 프레젠테이션',
        language: 'ENGLISH',
        level: 'ADVANCED',
        reason: '업무 발표 목표에 적합합니다.'
      }
    ]),
    [
      {
        id: 9105,
        title: '임원 보고 영어 프레젠테이션',
        language: '영어',
        level: '고급',
        situation: 'AI 추천',
        description: '업무 발표 목표에 적합합니다.',
        duration: '추천 강의',
        students: 0,
        image: '',
        tone: 'green'
      }
    ]
  )
})

test('서버 source를 화면 결과 상태로 일관되게 변환한다', () => {
  assert.equal(recommendationMode('AI'), 'ai')
  assert.equal(recommendationMode('RULE_BASED_FALLBACK'), 'fallback')
})
