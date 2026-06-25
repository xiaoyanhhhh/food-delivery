<template>
  <div id="app">
    <div class="app-header" v-if="showNav">
      <div class="header-left">
        <span class="logo" @click="goHome">
          <el-icon :size="22"><KnifeFork /></el-icon>
          <span class="logo-text">美味外卖</span>
        </span>
      </div>
      <div class="header-right" v-if="isLogin">
        <!-- Customer nav -->
        <template v-if="role === 'CUSTOMER'">
          <el-button class="nav-btn" @click="router.push('/cart')">
            <el-icon :size="18"><ShoppingCart /></el-icon>
            <span>购物车</span>
          </el-button>
          <el-button class="nav-btn" @click="router.push('/my-orders')">
            <el-icon :size="18"><Tickets /></el-icon>
            <span>我的订单</span>
          </el-button>
          <el-button class="nav-btn" @click="router.push('/favorites')">
            <el-icon :size="18"><Star /></el-icon>
            <span>收藏</span>
          </el-button>
          <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99">
            <el-button class="nav-btn icon-only" @click="router.push('/notifications')">
              <el-icon :size="20"><Bell /></el-icon>
            </el-button>
          </el-badge>
          <el-button class="nav-btn icon-only" @click="router.push('/addresses')">
            <el-icon :size="20"><Location /></el-icon>
          </el-button>
        </template>

        <!-- Merchant nav -->
        <template v-if="role === 'MERCHANT'">
          <el-button class="nav-btn" @click="router.push('/merchant')">
            <el-icon :size="18"><Shop /></el-icon>
            <span>商家中心</span>
          </el-button>
          <el-button class="nav-btn" @click="router.push('/merchant/store')">
            <el-icon :size="18"><Setting /></el-icon>
            <span>店铺管理</span>
          </el-button>
          <el-button class="nav-btn" @click="router.push('/merchant/dishes')">
            <el-icon :size="18"><KnifeFork /></el-icon>
            <span>菜品管理</span>
          </el-button>
          <el-button class="nav-btn" @click="router.push('/merchant/orders')">
            <el-icon :size="18"><Document /></el-icon>
            <span>订单管理</span>
          </el-button>
          <el-button class="nav-btn" @click="router.push('/merchant/sales')">
            <el-icon :size="18"><DataAnalysis /></el-icon>
            <span>销售统计</span>
          </el-button>
        </template>

        <!-- Rider nav -->
        <template v-if="role === 'RIDER'">
          <el-button class="nav-btn" @click="router.push('/rider')">
            <el-icon :size="18"><Van /></el-icon>
            <span>骑手中心</span>
          </el-button>
          <el-button class="nav-btn" @click="router.push('/rider/orders')">
            <el-icon :size="18"><Tickets /></el-icon>
            <span>我的配送</span>
          </el-button>
        </template>

        <!-- User dropdown -->
        <el-dropdown trigger="click" class="user-dropdown">
          <span class="user-trigger">
            <el-icon :size="18"><UserFilled /></el-icon>
            <span class="username">{{ username }}</span>
            <el-icon :size="14"><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item @click="router.push('/profile')">
                <el-icon><UserFilled /></el-icon>个人中心
              </el-dropdown-item>
              <el-dropdown-item v-if="role === 'CUSTOMER'" @click="router.push('/my-orders')">
                <el-icon><Tickets /></el-icon>我的订单
              </el-dropdown-item>
              <el-dropdown-item v-if="role === 'CUSTOMER'" @click="router.push('/cart')">
                <el-icon><ShoppingCart /></el-icon>购物车
              </el-dropdown-item>
              <el-dropdown-item divided @click="doLogout">
                <el-icon><SwitchButton /></el-icon>退出登录
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>

      <!-- Guest nav -->
      <div class="header-right" v-else>
        <el-button class="nav-btn" @click="router.push('/login')">登录</el-button>
        <el-button class="nav-btn-outlined" @click="router.push('/register')">注册</el-button>
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
import {
  ArrowDown,
  Bell,
  DataAnalysis,
  Document,
  KnifeFork,
  Location,
  Setting,
  Shop,
  ShoppingCart,
  Star,
  SwitchButton,
  Tickets,
  UserFilled,
  Van,
} from '@element-plus/icons-vue'

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

function goHome() {
  const r = auth.getRole()
  if (r === 'MERCHANT') router.push('/merchant')
  else if (r === 'RIDER') router.push('/rider')
  else router.push('/')
}

function doLogout() {
  auth.logout()
  router.push('/login')
}

onMounted(() => {
  fetchUnread()
  setInterval(fetchUnread, 30000)
})
</script>

<style>
/* === Global Reset (moved from inline) === */
* { margin: 0; padding: 0; box-sizing: border-box; }
body { font-family: 'Helvetica Neue', Helvetica, 'PingFang SC', 'Microsoft YaHei', sans-serif; background: #F5F0EB; color: #333; }
a { text-decoration: none; }
#app { min-height: 100vh; }

/* === Header === */
.app-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: linear-gradient(135deg, #E67E00 0%, #FF8C00 100%);
  padding: 0 20px;
  height: 52px;
  position: sticky;
  top: 0;
  z-index: 100;
  box-shadow: 0 2px 8px rgba(255, 140, 0, 0.25);
}

.logo {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  color: #fff;
}
.logo-text {
  font-size: 18px;
  font-weight: bold;
  letter-spacing: 1px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 4px;
}

/* Navigation buttons */
.nav-btn {
  --el-button-bg-color: transparent !important;
  --el-button-border-color: transparent !important;
  --el-button-hover-bg-color: transparent !important;
  --el-button-hover-border-color: transparent !important;
  --el-button-active-bg-color: transparent !important;
  --el-button-active-border-color: transparent !important;
  --el-button-text-color: rgba(255, 255, 255, 0.92) !important;
  --el-button-hover-text-color: #fff !important;
  --el-button-active-text-color: #fff !important;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  height: auto;
  color: rgba(255, 255, 255, 0.92) !important;
  font-size: 14px;
  padding: 6px 10px;
  border-radius: 6px;
  transition: background 0.2s;
}
.nav-btn:hover {
  background: rgba(255, 255, 255, 0.15) !important;
  color: #fff !important;
}
.nav-btn:active {
  background: rgba(255, 255, 255, 0.1) !important;
}
.nav-btn .el-icon {
  color: inherit !important;
}
.nav-btn.icon-only {
  width: 34px;
  min-width: 34px;
  height: 32px;
  padding: 0;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

/* Outlined white button for register on orange header */
.nav-btn-outlined {
  color: #fff !important;
  border: 1.5px solid rgba(255, 255, 255, 0.8) !important;
  background: transparent !important;
  font-size: 14px;
  padding: 6px 14px;
  border-radius: 6px;
  transition: all 0.2s;
}
.nav-btn-outlined:hover {
  background: rgba(255, 255, 255, 0.2) !important;
  border-color: #fff !important;
  color: #fff !important;
}

/* User dropdown */
.user-dropdown {
  margin-left: 8px;
}
.user-trigger {
  display: flex;
  align-items: center;
  gap: 4px;
  color: #fff;
  cursor: pointer;
  padding: 4px 10px;
  border-radius: 6px;
  transition: background 0.2s;
  font-size: 14px;
}
.user-trigger:hover {
  background: rgba(255, 255, 255, 0.15);
}
.username {
  max-width: 100px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* === Main Content === */
.main-content {
  /* layout handled by .page-container in style.css */
}
.main-content.full {
  /* full-screen pages (login/register) — no constraints */
}

/* === Page Title === */
.page-title {
  font-size: 22px;
  font-weight: bold;
  margin-bottom: 16px;
  color: #303133;
}
</style>
