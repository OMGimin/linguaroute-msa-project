export function buildLearningCourse(course, enrollment, lessons, detail) {
  const requiredLessons = lessons.filter((lesson) => lesson.required)
  const progressLessons = detail.lessons || []
  const completedLessonIds = new Set(
    progressLessons
      .filter((lesson) => lesson.status === 'COMPLETED')
      .map((lesson) => String(lesson.lessonId))
  )
  const matchedLessons = requiredLessons.filter((lesson) =>
    progressLessons.some((progress) => String(progress.lessonId) === String(lesson.lessonId))
  )
  const useProgressFallback = progressLessons.length > 0 && matchedLessons.length === 0
  const totalLessons = requiredLessons.length || progressLessons.length
  const completedLessons = useProgressFallback
    ? Math.min(progressLessons.filter((lesson) => lesson.status === 'COMPLETED').length, totalLessons)
    : requiredLessons.filter((lesson) => completedLessonIds.has(String(lesson.lessonId))).length
  const nextLesson = useProgressFallback
    ? requiredLessons[completedLessons]
    : requiredLessons.find((lesson) => !completedLessonIds.has(String(lesson.lessonId)))

  return {
    ...course,
    id: enrollment.courseId,
    enrollmentId: enrollment.enrollmentId,
    progress: Number(enrollment.progressRate),
    completedLessons,
    totalLessons,
    nextLessonId: nextLesson?.lessonId,
    enrolledAt: enrollment.enrolledAt,
    startedAt: enrollment.startedAt,
    completedAt: enrollment.completedAt,
    image: '',
    duration: '등록된 차시 기준',
    students: '-',
    tone: 'green'
  }
}
