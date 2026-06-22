import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/Login.vue'),
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/Register.vue'),
  },
  {
    path: '/',
    name: 'Home',
    component: () => import('../views/Home.vue'),
  },
  {
    path: '/dishes/:id',
    name: 'DishDetail',
    component: () => import('../views/DishDetail.vue'),
  },
  {
    path: '/cart',
    name: 'Cart',
    component: () => import('../views/Cart.vue'),
    meta: { requiresAuth: true, role: 'CUSTOMER' },
  },
  {
    path: '/my-orders',
    name: 'MyOrders',
    component: () => import('../views/CustomerOrders.vue'),
    meta: { requiresAuth: true, role: 'CUSTOMER' },
  },
  // 商家端
  {
    path: '/merchant',
    name: 'MerchantDashboard',
    component: () => import('../views/merchant/Dashboard.vue'),
    meta: { requiresAuth: true, role: 'MERCHANT' },
  },
  {
    path: '/merchant/dishes',
    name: 'MerchantDishes',
    component: () => import('../views/merchant/DishManage.vue'),
    meta: { requiresAuth: true, role: 'MERCHANT' },
  },
  {
    path: '/merchant/orders',
    name: 'MerchantOrders',
    component: () => import('../views/merchant/OrderManage.vue'),
    meta: { requiresAuth: true, role: 'MERCHANT' },
  },
  // 骑手端
  {
    path: '/rider',
    name: 'RiderDashboard',
    component: () => import('../views/rider/Dashboard.vue'),
    meta: { requiresAuth: true, role: 'RIDER' },
  },
  {
    path: '/rider/orders',
    name: 'RiderOrders',
    component: () => import('../views/rider/OrderList.vue'),
    meta: { requiresAuth: true, role: 'RIDER' },
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

// Navigation guard
router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  const userStr = localStorage.getItem('user')

  if (to.meta.requiresAuth) {
    if (!token || !userStr) {
      return next('/login')
    }
    const user = JSON.parse(userStr)
    if (to.meta.role && to.meta.role !== user.role) {
      return next('/')
    }
  }
  next()
})

export default router
