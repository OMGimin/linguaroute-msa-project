const LANGUAGE_CODES = { 영어: 'ENGLISH', 일본어: 'JAPANESE', 중국어: 'CHINESE' }
const LEVEL_CODES = { 초급: 'ELEMENTARY', 중급: 'INTERMEDIATE', 고급: 'ADVANCED' }
const JOB_CODES = {
  '글로벌 세일즈': 'GLOBAL_SALES',
  '소프트웨어 개발': 'SOFTWARE_DEVELOPMENT',
  '데이터 분석': 'DATA_ANALYSIS',
  '프로덕트 관리': 'PRODUCT_MANAGEMENT'
}
const SITUATION_CODES = {
  '고객 미팅': 'CUSTOMER_MEETING',
  '업무 발표': 'PRESENTATION',
  협업: 'DAILY_CONVERSATION',
  출장: 'BUSINESS_TRIP'
}

const LANGUAGE_LABELS = { ENGLISH: '영어', JAPANESE: '일본어', CHINESE: '중국어' }
const LEVEL_LABELS = {
  BEGINNER: '입문',
  ELEMENTARY: '초급',
  INTERMEDIATE: '중급',
  ADVANCED: '고급'
}

export function buildRecommendationRequest(input) {
  return {
    language: LANGUAGE_CODES[input.language],
    level: LEVEL_CODES[input.level],
    job: JOB_CODES[input.job],
    situation: SITUATION_CODES[input.situation],
    goal: input.goal.trim()
  }
}

export function mapRecommendedCourses(courses, defaults = {}) {
  return courses.map((course) => ({
    id: course.courseId,
    title: course.title,
    language: LANGUAGE_LABELS[course.language] ?? course.language,
    level: LEVEL_LABELS[course.level] ?? course.level,
    situation: 'AI 추천',
    description: course.reason,
    duration: '추천 강의',
    students: 0,
    image: defaults.image ?? '',
    tone: defaults.tone ?? 'green'
  }))
}

export function recommendationMode(source) {
  return source === 'RULE_BASED_FALLBACK' ? 'fallback' : 'ai'
}
