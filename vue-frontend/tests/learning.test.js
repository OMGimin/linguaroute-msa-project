import test from 'node:test'
import assert from 'node:assert/strict'

import { buildLearningCourse } from '../src/domain/learning.js'

test('완료 차시를 진도율 추정값이 아닌 실제 필수 차시 상태로 계산한다', () => {
  const result = buildLearningCourse(
    { id: 9101, title: '미팅 영어' },
    { enrollmentId: 1, courseId: 9101, progressRate: 50 },
    [
      { lessonId: 11, required: true },
      { lessonId: 12, required: true },
      { lessonId: 13, required: false }
    ],
    { lessons: [{ lessonId: 11, status: 'COMPLETED' }, { lessonId: 13, status: 'COMPLETED' }] }
  )

  assert.equal(result.completedLessons, 1)
  assert.equal(result.totalLessons, 2)
  assert.equal(result.nextLessonId, 12)
})

test('기존 진도 데이터의 차시 ID가 강의 차시 ID와 다르면 저장된 완료 상태 개수를 사용한다', () => {
  const result = buildLearningCourse(
    { id: 9101, title: '미팅 영어' },
    { enrollmentId: 1, courseId: 9101, progressRate: 50 },
    [{ lessonId: 910101, required: true }, { lessonId: 910102, required: true }, { lessonId: 910103, required: true }],
    { lessons: [{ lessonId: 1, status: 'COMPLETED' }, { lessonId: 2, status: 'LEARNING' }] }
  )

  assert.equal(result.completedLessons, 1)
  assert.equal(result.totalLessons, 3)
  assert.equal(result.nextLessonId, 910102)
})
