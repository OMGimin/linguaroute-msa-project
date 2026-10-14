import api from './index.js'

export const recommendationApi = {
  create(data) {
    return api.post('/api/courses/recommendations', data)
  }
}
