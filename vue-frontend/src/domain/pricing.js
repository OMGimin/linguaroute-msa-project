export const PUBLIC_PLAN_FALLBACK = [
  { planPriceId: 1, planName: 'BUSINESS_50', billingCycle: 'MONTHLY', seatLimit: 50, price: 299000, currency: 'KRW' },
  { planPriceId: 2, planName: 'BUSINESS_50', billingCycle: 'YEARLY', seatLimit: 50, price: 2990000, currency: 'KRW' },
  { planPriceId: 3, planName: 'STARTUP_20', billingCycle: 'MONTHLY', seatLimit: 20, price: 129000, currency: 'KRW' },
  { planPriceId: 4, planName: 'STARTUP_20', billingCycle: 'YEARLY', seatLimit: 20, price: 1290000, currency: 'KRW' }
]

const descriptions = {
  STARTUP_20: '처음 기업 교육을 시작하는 작은 팀을 위한 구성',
  BUSINESS_50: '활발하게 성장하는 조직을 위한 균형 있는 구성'
}

export function groupPlanPrices(items) {
  const grouped = new Map()
  for (const item of items) {
    const plan = grouped.get(item.planName) || {
      name: item.planName,
      seats: item.seatLimit,
      prices: {},
      description: descriptions[item.planName] || `${item.seatLimit}명까지 이용 가능한 기업 구독 요금제`,
      features: ['전체 외국어 강의 이용', 'AI 맞춤 강의 추천', '직원 초대와 좌석 관리', '직원별 학습 현황']
    }
    plan.prices[item.billingCycle] = Number(item.price)
    grouped.set(item.planName, plan)
  }

  return [...grouped.values()]
    .sort((a, b) => a.seats - b.seats)
    .map((plan, index, plans) => ({ ...plan, eyebrow: index === 0 ? 'START' : 'GROW', featured: index === plans.length - 1 }))
}

export function formatPlanPrice(value) {
  return Number.isFinite(value) ? value.toLocaleString('ko-KR') : '문의'
}
