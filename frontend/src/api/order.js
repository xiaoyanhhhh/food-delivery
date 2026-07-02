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

export function getOrderMessages(id) {
  return request.get(`/orders/${id}/messages`)
}

export function sendOrderMessage(id, content) {
  return request.post(`/orders/${id}/messages`, { content })
}

export function updateOrderStatus(id, status, role) {
  return request.put(`/orders/${id}/status`, { status, role })
}

export function acceptOrder(id) {
  return request.post(`/orders/${id}/accept`)
}

export function requestNoRiderDispatch(id) {
  return request.post(`/orders/${id}/no-rider`)
}

export function acceptDispatchOffer(id) {
  return request.post(`/orders/${id}/dispatch/accept`)
}

export function rejectDispatchOffer(id) {
  return request.post(`/orders/${id}/dispatch/reject`)
}

export function payOrder(id, paymentMethod = 'WECHAT') {
  return request.post(`/orders/${id}/pay`, { paymentMethod })
}

export function getDeliveryEstimate(storeId, address = '') {
  return request.get('/orders/delivery-estimate', { params: { storeId, address } })
}

export function reorderOrder(id) {
  return request.post(`/orders/${id}/reorder`)
}
