import request from './request'

export function getDishes(params) {
  return request.get('/dishes', { params })
}

export function getDishDetail(id) {
  return request.get(`/dishes/${id}`)
}

export function searchDishes(keyword) {
  return request.get('/dishes', { params: { keyword } })
}
