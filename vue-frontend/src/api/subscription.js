import api from './index.js'

export const subscriptionApi = {
  getPlans() { return api.get('/api/plans') },
  subscribe(planPriceId, paymentMethodToken, idempotencyKey) {
    return api.post('/api/subscriptions', { planPriceId, paymentMethodToken }, { headers: { 'Idempotency-Key': idempotencyKey } })
  },
  getMine() { return api.get('/api/subscriptions/me') },
  cancel(reason) { return api.post('/api/subscriptions/me/cancel', { reason }) },
  getPayments() { return api.get('/api/payments') },
}
