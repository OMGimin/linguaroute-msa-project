import test from 'node:test'
import assert from 'node:assert/strict'

import { averageProgress } from '../src/domain/progress.js'

test('직원의 여러 수강 진도율을 마지막 값으로 덮어쓰지 않고 평균한다', () => {
  assert.equal(averageProgress([{ progressRate: 50 }, { progressRate: 100 }]), 75)
  assert.equal(averageProgress([]), 0)
})
