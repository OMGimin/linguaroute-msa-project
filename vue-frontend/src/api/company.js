import api from './index.js'

export const companyApi = {
  getInvitations() { return api.get('/api/companies/me/invitations') },
  createInvitation(expiresInDays = 7) { return api.post('/api/companies/me/invitations', { expiresInDays }) },
  revokeInvitation(invitationId) { return api.delete(`/api/companies/me/invitations/${invitationId}`) },
  reissueInvitation(invitationId) { return api.post(`/api/companies/me/invitations/${invitationId}/reissue`) },
  getEmployees() { return api.get('/api/companies/me/employees') },
  updateEmployeeStatus(userId, status) { return api.post(`/api/companies/me/employees/${userId}/status?action=update-status`, { status }) },
  getSeats() { return api.get('/api/companies/me/seats') },
  getEnrollments() { return api.get('/api/companies/me/enrollments') },
  getEnrollmentProgress() { return api.get('/api/companies/me/enrollments/progress') },
}
