<template>
  <div id="app">
    <el-container>
      <!-- Top Navigation -->
      <el-header v-if="showNav" class="app-header">
        <div class="header-left">
          <router-link to="/" class="logo">🍔 美味外卖</router-link>
        </div>
        <div class="header-right" v-if="auth.isLoggedIn()">
          <template v-if="auth.getRole() === 'CUSTOMER'">
            <router-link to="/cart">
              <el-badge :value="cartItemsCount" :hidden="cartItemsCount === 0">
                <el-button circle :icon="ShoppingCart" />
              </el-badge>
            </router-link>
            <router-link to="/my-orders">
              <el-button type="primary" link>我的订单</el-button>
            </router-link>
          </template>
          <template v-if="auth.getRole() === 'MERCHANT'">
            <router-link to="/merchant">
              <el-button type="primary" link>商家中心</el-button>
            </router-link>
          </template>
          <template v-if="auth.getRole() === 'RIDER'">
            <router-link to="/rider">
              <el-button type="primary" link>骑手中心</el-button>
            </router-link>
          </template>
          <el-tag>{{ auth.user?.username }}</el-tag>
          <el-button @click="handleLogout" type="danger" link>退出</el-button>
        </div>
        <div class="header-right" v-else>
          <router-link to="/login">
            <el-button type="primary">登录</el-button>
          </router-link>
        </div>
      </el-header>

      <!-- Main Content -->
      <el-main :class="{ 'no-header': !showNav }">
        <router-view />
      </el-main>
    </el-container>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from './stores/auth'
import { useCartStore } from './stores/cart'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const cartStore = useCartStore()

const showNav = computed(() => !['/login', '/register'].includes(route.path))

const cartItemsCount = computed(() =>
  cartStore.items.reduce((sum, item) => sum + item.quantity, 0)
)

if (auth.isLoggedIn() && auth.getRole() === 'CUSTOMER') {
  cartStore.fetchCart()
}

function handleLogout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e4e7ed;
  padding: 0 24px;
  height: 60px;
}
.logo {
  font-size: 20px;
  font-weight: bold;
  color: #409eff;
  text-decoration: none;
}
.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.no-header {
  padding: 0;
}
</style>
