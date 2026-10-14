export const LANGUAGE_OPTIONS = [
  { value: 'ENGLISH', label: '영어' },
  { value: 'JAPANESE', label: '일본어' },
  { value: 'CHINESE', label: '중국어' },
]

export const LEVEL_OPTIONS = [
  { value: 'BEGINNER', label: '입문' },
  { value: 'ELEMENTARY', label: '초급' },
  { value: 'INTERMEDIATE', label: '중급' },
  { value: 'ADVANCED', label: '고급' },
]

export const SITUATION_OPTIONS = [
  { value: 'CUSTOMER_MEETING', label: '고객 미팅' },
  { value: 'PRESENTATION', label: '업무 발표' },
  { value: 'EMAIL', label: '이메일' },
  { value: 'BUSINESS_TRIP', label: '출장' },
  { value: 'DAILY_CONVERSATION', label: '일상 회화' },
]

export function unwrapApiData(response) {
  return response?.data?.data ?? response?.data ?? response
}

export function formatBusinessNumber(value = '') {
  const digits = String(value).replace(/\D/g, '').slice(0, 10)
  return digits.replace(/^(\d{3})(\d{2})(\d{0,5}).*$/, (_, a, b, c) => [a, b, c].filter(Boolean).join('-'))
}
