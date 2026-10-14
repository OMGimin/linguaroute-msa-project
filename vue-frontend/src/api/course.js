import api from './index.js'

export const courseApi = {
  getCourses(params) {
    return api.get('/api/courses', { params })
  },

  getAdminCourses(params) {
    return api.get('/api/admin/courses', { params })
  },

  getAdminById(id) {
    return api.get(`/api/admin/courses/${id}`)
  },

  getAdminLessons(courseId) {
    return api.get(`/api/admin/courses/${courseId}/lessons`)
  },

  getById(id) {
    return api.get(`/api/courses/${id}`)
  },

  getLessons(courseId) {
    return api.get(`/api/courses/${courseId}/lessons`)
  },

  create(data) {
    return api.post('/api/admin/courses', data)
  },

  update(id, data) {
    return api.post(`/api/admin/courses/${id}?action=update-course`, data)
  },

  updateStatus(id, status) {
    return api.post(`/api/admin/courses/${id}/status?action=update-status`, { status })
  },

  createLesson(courseId, data) {
    return api.post(`/api/admin/courses/${courseId}/lessons`, data)
  },

  recommend(data) {
    return api.post('/api/courses/recommendations', data)
  }
}
