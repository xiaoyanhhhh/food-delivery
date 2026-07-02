import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as loginApi, register as registerApi } from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref(localStorage.getItem('token') || '')
  const user = ref(JSON.parse(localStorage.getItem('user') || 'null'))
  const profile = ref(JSON.parse(localStorage.getItem('profile') || 'null') || {})

  function setAuth(data) {
    token.value = data.token
    user.value = {
      username: data.username,
      role: data.role,
      userId: data.userId,
      phone: data.phone,
      address: data.address,
      riderType: data.riderType,
    }
    localStorage.setItem('token', data.token)
    localStorage.setItem('user', JSON.stringify(user.value))
  }

  function setProfile(data) {
    profile.value = { ...profile.value, ...data }
    localStorage.setItem('profile', JSON.stringify(profile.value))
  }

  async function login(username, password) {
    const data = await loginApi(username, password)
    setAuth(data)
    return data
  }

  async function register(form) {
    const data = await registerApi(form)
    setAuth(data)
    return data
  }

  function logout() {
    token.value = ''
    user.value = null
    profile.value = {}
    localStorage.removeItem('token')
    localStorage.removeItem('user')
    localStorage.removeItem('profile')
  }

  function isLoggedIn() {
    return !!token.value
  }

  function getRole() {
    return user.value?.role || ''
  }

  function getUserId() {
    return user.value?.userId || null
  }

  return { token, user, profile, setProfile, login, register, logout, isLoggedIn, getRole, getUserId }
})
