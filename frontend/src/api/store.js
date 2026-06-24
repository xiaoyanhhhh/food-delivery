import request from './request'

export function getStores(params) {
  return request.get('/stores', { params })
}

export function getStoreDetail(id) {
  return request.get(`/stores/${id}`)
}

export function getMyStore() {
  return request.get('/merchant/store')
}

export function createStore(data) {
  return request.post('/merchant/store', data)
}

export function updateStore(id, data) {
  return request.put(`/merchant/store/${id}`, data)
}

export function getStoreCategories() {
  return request.get('/store-categories')
}
