import api from './index.js'

export const adminApi = {
  getUsers() { return api.get('/api/admin/users') },
  getCompanies() { return api.get('/api/admin/companies') },
  getPayments() { return api.get('/api/admin/payments') },
  getEnrollments(params = {}) { return api.get('/api/admin/enrollments', { params }) },
}
