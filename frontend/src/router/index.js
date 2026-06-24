import { createRouter, createWebHashHistory } from 'vue-router'

import Login from '../views/Login.vue'
import Register from '../views/Register.vue'
import StoreList from '../views/StoreList.vue'
import StoreDetail from '../views/StoreDetail.vue'
import DishDetail from '../views/DishDetail.vue'
import Cart from '../views/Cart.vue'
import CustomerOrders from '../views/CustomerOrders.vue'
import OrderTracking from '../views/OrderTracking.vue'
import Profile from '../views/Profile.vue'
import AddressManage from '../views/AddressManage.vue'
import MerchantDashboard from '../views/merchant/Dashboard.vue'
import DishManage from '../views/merchant/DishManage.vue'
import OrderManage from '../views/merchant/OrderManage.vue'
import StoreManage from '../views/merchant/StoreManage.vue'
import SalesAnalytics from '../views/merchant/SalesAnalytics.vue'
import RiderDashboard from '../views/rider/Dashboard.vue'
import RiderOrders from '../views/rider/OrderList.vue'

const routes = [
  { path: '/login', name: 'Login', component: Login },
  { path: '/register', name: 'Register', component: Register },
  { path: '/', name: 'StoreList', component: StoreList },
  { path: '/store/:id', name: 'StoreDetail', component: StoreDetail },
  { path: '/dishes/:id', name: 'DishDetail', component: DishDetail },
  { path: '/cart', name: 'Cart', component: Cart, meta: { auth: true, role: 'CUSTOMER' } },
  { path: '/my-orders', name: 'MyOrders', component: CustomerOrders, meta: { auth: true, role: 'CUSTOMER' } },
  { path: '/orders/:id/track', name: 'OrderTracking', component: OrderTracking, meta: { auth: true } },
  { path: '/profile', name: 'Profile', component: Profile, meta: { auth: true } },
  { path: '/addresses', name: 'AddressManage', component: AddressManage, meta: { auth: true } },
  { path: '/favorites', name: 'Favorites', component: () => import('../views/Favorites.vue'), meta: { auth: true } },
  { path: '/notifications', name: 'Notifications', component: () => import('../views/Notifications.vue'), meta: { auth: true } },
  { path: '/merchant', name: 'MerchantDashboard', component: MerchantDashboard, meta: { auth: true, role: 'MERCHANT' } },
  { path: '/merchant/dishes', name: 'MerchantDishes', component: DishManage, meta: { auth: true, role: 'MERCHANT' } },
  { path: '/merchant/orders', name: 'MerchantOrders', component: OrderManage, meta: { auth: true, role: 'MERCHANT' } },
  { path: '/merchant/store', name: 'StoreManage', component: StoreManage, meta: { auth: true, role: 'MERCHANT' } },
  { path: '/merchant/sales', name: 'SalesAnalytics', component: SalesAnalytics, meta: { auth: true, role: 'MERCHANT' } },
  { path: '/rider', name: 'RiderDashboard', component: RiderDashboard, meta: { auth: true, role: 'RIDER' } },
  { path: '/rider/orders', name: 'RiderOrders', component: RiderOrders, meta: { auth: true, role: 'RIDER' } },
]

const router = createRouter({
  history: createWebHashHistory(),
  routes,
})

router.beforeEach((to, from) => {
  const token = localStorage.getItem('token')
  const userStr = localStorage.getItem('user')

  if (to.meta.auth) {
    if (!token || !userStr) return '/login'
    try {
      const user = JSON.parse(userStr)
      if (to.meta.role && to.meta.role !== user.role) return '/'
    } catch { return '/login' }
  }
})

export default router
