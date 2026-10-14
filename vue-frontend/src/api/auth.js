import api from './index.js'

export const authApi = {
  exchangeOAuthCode(code) { return api.post('/api/users/register?action=exchange-oauth-code', { code }) },

  // 내 정보 조회
  getMe() {
    return api.get('/api/users/me')
  },

  updateMe(name) { return api.post('/api/users/me?action=update-profile', { name }) },
  withdrawMe() { return api.delete('/api/users/me') },
  getMyCompany() { return api.get('/api/companies/me') },
  updateMyCompany(name) { return api.post('/api/companies/me?action=update-company', { name }) },
  registerCompany(data) { return api.post('/api/users/register', data) },
  registerEmployee(data) { return api.post('/api/users/register?action=employee-signup', data) },
  validateInvitation(invitationCode) { return api.post('/api/users/register?action=validate-invitation', { invitationCode }) },
  getActiveTerms() { return api.get('/api/users/register?action=active-terms') },
  agreeToTerms(agreementIds) { return api.post('/api/users/me/agreements', { agreementIds }) },
  requestEmailVerification(email) { return api.post('/api/users/register?action=request-email-verification', { email, purpose: 'SIGNUP' }) },
  confirmEmailVerification(email, verificationCode) { return api.post('/api/users/register?action=confirm-email-verification', { email, verificationCode }) },
  requestPasswordReset(email) { return api.post('/api/users/register?action=request-password-reset', { email }) },
  confirmPasswordReset(resetToken, newPassword) { return api.post('/api/users/register?action=confirm-password-reset', { resetToken, newPassword }) },
  requestIdFind(name, businessNumber) { return api.post('/api/users/register?action=request-id-find', { name, businessNumber }) },
  changePassword(currentPassword, newPassword) { return api.put('/api/users/me/password', { currentPassword, newPassword }) }
}
