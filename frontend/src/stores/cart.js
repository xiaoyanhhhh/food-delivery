import { defineStore } from 'pinia'
import { ref } from 'vue'
import {
  getCart as getCartApi,
  addToCart as addToCartApi,
  updateCartItem as updateCartItemApi,
  removeCartItem as removeCartItemApi,
  clearCart as clearCartApi,
} from '../api/cart'

export const useCartStore = defineStore('cart', () => {
  const items = ref([])

  async function fetchCart() {
    try {
      items.value = await getCartApi()
    } catch {
      items.value = []
    }
  }

  async function add(dishId, quantity = 1) {
    await addToCartApi(dishId, quantity)
    await fetchCart()
  }

  async function update(id, quantity) {
    await updateCartItemApi(id, quantity)
    await fetchCart()
  }

  async function remove(id) {
    await removeCartItemApi(id)
    await fetchCart()
  }

  async function clear() {
    await clearCartApi()
    items.value = []
  }

  function totalPrice() {
    return items.value.reduce((sum, item) => {
      return sum + item.dish.price * item.quantity
    }, 0)
  }

  return { items, fetchCart, add, update, remove, clear, totalPrice }
})
