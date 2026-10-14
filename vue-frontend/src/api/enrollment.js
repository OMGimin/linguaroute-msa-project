import api from './index.js'

export const enrollmentApi = {
  getMyEnrollments() {
    return api.get('/api/enrollments/me')
  },
  enroll(courseId) {
    return api.post('/api/enrollments', { courseId })
  },
  getById(enrollmentId) {
    return api.get(`/api/enrollments/${enrollmentId}`)
  },
  startLesson(enrollmentId, lessonId) {
    return api.post(`/api/enrollments/${enrollmentId}/lessons/${lessonId}/start`)
  },
  completeLesson(enrollmentId, lessonId) {
    return api.post(`/api/enrollments/${enrollmentId}/lessons/${lessonId}/complete`)
  }
}
