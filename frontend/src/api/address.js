import request from './request'

export function getAddresses() {
  return request.get('/addresses')
}

export function getAddress(id) {
  return request.get(`/addresses/${id}`)
}

export function createAddress(data) {
  return request.post('/addresses', data)
}

export function updateAddress(id, data) {
  return request.put(`/addresses/${id}`, data)
}

export function deleteAddress(id) {
  return request.delete(`/addresses/${id}`)
}
