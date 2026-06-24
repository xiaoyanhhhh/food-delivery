import request from './request'

export function getStoreReviews(storeId, params) {
  return request.get(`/reviews/store/${storeId}`, { params })
}

export function getDishReviews(dishId, params) {
  return request.get(`/reviews/dish/${dishId}`, { params })
}

export function getMyReviews(params) {
  return request.get('/reviews/my', { params })
}

export function createReview(data) {
  return request.post('/reviews', data)
}

export function replyReview(id, reply) {
  return request.put(`/reviews/${id}/reply`, { reply })
}
