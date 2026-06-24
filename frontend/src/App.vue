<template>
  <div id="app">
    <div class="app-header" v-if="showNav">
      <div class="header-left">
        <span class="logo" @click="router.push('/')">🍔 美味外卖</span>
      </div>
      <div class="header-right" v-if="isLogin">
        <el-button v-if="role === 'CUSTOMER'" link @click="router.push('/cart')">🛒 购物车</el-button>
        <el-button v-if="role === 'CUSTOMER'" link @click="router.push('/my-orders')">我的订单</el-button>
        <el-button v-if="role === 'CUSTOMER'" link @click="router.push('/favorites')">❤️ 收藏</el-button>
        <el-badge v-if="role === 'CUSTOMER'" :value="unreadCount" :hidden="unreadCount === 0" :max="99">
          <el-button link @click="router.push('/notifications')">🔔</el-button>
        </el-badge>
        <el-button v-if="role === 'CUSTOMER'" link @click="router.push('/addresses')">📍</el-button>
        <el-button v-if="role === 'MERCHANT'" link @click="router.push('/merchant')">商家中心</el-button>
        <el-button v-if="role === 'MERCHANT'" link @click="router.push('/merchant/store')">店铺管理</el-button>
        <el-button v-if="role === 'MERCHANT'" link @click="router.push('/merchant/dishes')">菜品管理</el-button>
        <el-button v-if="role === 'MERCHANT'" link @click="router.push('/merchant/orders')">订单管理</el-button>
        <el-button v-if="role === 'MERCHANT'" link @click="router.push('/merchant/sales')">销售统计</el-button>
        <el-button v-if="role === 'RIDER'" link @click="router.push('/rider')">骑手中心</el-button>
        <el-button v-if="role === 'RIDER'" link @click="router.push('/rider/orders')">我的配送</el-button>
        <el-tag size="small" style="margin:0 8px">{{ username }}</el-tag>
        <el-button @click="doLogout" type="danger" link>退出</el-button>
      </div>
      <div class="header-right" v-else>
        <el-button type="primary" @click="router.push('/login')">登录</el-button>
        <el-button link @click="router.push('/register')">注册</el-button>
      </div>
    </div>
    <div class="main-content" :class="{ full: !showNav }">
      <router-view />
    </div>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { useCartStore } from './stores/cart'
import { getUnreadCount } from './api/notification'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cartStore = useCartStore()

const showNav = computed(() => route.path !== '/login' && route.path !== '/register')
const isLogin = computed(() => auth.isLoggedIn())
const role = computed(() => auth.getRole())
const username = computed(() => auth.user?.username || '')
const unreadCount = ref(0)

if (auth.isLoggedIn() && auth.getRole() === 'CUSTOMER') {
  cartStore.fetchCart()
}

async function fetchUnread() {
  if (!auth.isLoggedIn() || auth.getRole() !== 'CUSTOMER') return
  try { unreadCount.value = await getUnreadCount() } catch { /* ignore */ }
}

function doLogout() {
  auth.logout()
  router.push('/login')
}

onMounted(() => {
  fetchUnread()
  // Poll unread every 30s
  setInterval(fetchUnread, 30000)
})
</script>

<style>
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', sans-serif; background: #f5f7fa; color: #333; }
a { text-decoration: none; }
#app { min-height: 100vh; }
.app-header { display: flex; align-items: center; justify-content: space-between; background: #fff; border-bottom: 1px solid #e4e7ed; padding: 0 24px; height: 60px; position: sticky; top: 0; z-index: 100; }
.logo { font-size: 20px; font-weight: bold; color: #409eff; cursor: pointer; }
.header-right { display: flex; align-items: center; gap: 8px; }
.main-content { max-width: 1200px; margin: 0 auto; padding: 20px; }
.main-content.full { padding: 0; max-width: none; }
.page-title { font-size: 24px; font-weight: bold; margin-bottom: 20px; color: #303133; }
</style>
