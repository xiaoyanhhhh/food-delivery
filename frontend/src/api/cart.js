import request from './request'

export function getCart() {
  return request.get('/cart')
}

export function addToCart(dishId, quantity = 1) {
  return request.post('/cart', { dishId, quantity })
}

export function updateCartItem(id, quantity) {
  return request.put(`/cart/${id}`, { quantity })
}

export function removeCartItem(id) {
  return request.delete(`/cart/${id}`)
}

export function clearCart() {
  return request.delete('/cart')
}
