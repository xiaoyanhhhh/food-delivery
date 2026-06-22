import request from './request'

export function createOrder(data) {
  return request.post('/orders', data)
}

export function getCustomerOrders() {
  return request.get('/orders/customer')
}

export function getMerchantOrders() {
  return request.get('/orders/merchant')
}

export function getRiderOrders() {
  return request.get('/orders/rider')
}

export function getAvailableOrders() {
  return request.get('/orders/available')
}

export function updateOrderStatus(id, status, role) {
  return request.put(`/orders/${id}/status`, { status, role })
}

export function acceptOrder(id) {
  return request.post(`/orders/${id}/accept`)
}
