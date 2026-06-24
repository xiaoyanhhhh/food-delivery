import request from './request'

export function createOrder(data) {
  return request.post('/orders', data)
}

export function getCustomerOrders(params) {
  return request.get('/orders/customer', { params })
}

export function getMerchantOrders(params) {
  return request.get('/orders/merchant', { params })
}

export function getRiderOrders(params) {
  return request.get('/orders/rider', { params })
}

export function getAvailableOrders(params) {
  return request.get('/orders/available', { params })
}

export function getOrderDetail(id) {
  return request.get(`/orders/${id}`)
}

export function updateOrderStatus(id, status, role) {
  return request.put(`/orders/${id}/status`, { status, role })
}

export function acceptOrder(id) {
  return request.post(`/orders/${id}/accept`)
}

export function payOrder(id, paymentMethod = 'WECHAT') {
  return request.post(`/orders/${id}/pay`, { paymentMethod })
}

export function getDeliveryEstimate(storeId) {
  return request.get('/orders/delivery-estimate', { params: { storeId } })
}

export function reorderOrder(id) {
  return request.post(`/orders/${id}/reorder`)
}
