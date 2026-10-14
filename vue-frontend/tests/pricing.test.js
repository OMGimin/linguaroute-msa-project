import test from 'node:test'
import assert from 'node:assert/strict'

import { groupPlanPrices } from '../src/domain/pricing.js'

test('요금제 API의 월간과 연간 가격을 같은 요금제 카드로 묶는다', () => {
  const plans = groupPlanPrices([
    { planPriceId: 3, planName: 'STARTUP_20', billingCycle: 'MONTHLY', seatLimit: 20, price: 129000 },
    { planPriceId: 4, planName: 'STARTUP_20', billingCycle: 'YEARLY', seatLimit: 20, price: 1290000 },
    { planPriceId: 1, planName: 'BUSINESS_50', billingCycle: 'MONTHLY', seatLimit: 50, price: 299000 }
  ])

  assert.deepEqual(plans.map((plan) => plan.name), ['STARTUP_20', 'BUSINESS_50'])
  assert.equal(plans[0].prices.MONTHLY, 129000)
  assert.equal(plans[0].prices.YEARLY, 1290000)
  assert.equal(plans[1].seats, 50)
})
