<template>
  <div class="page-container order-detail-page">
    <div class="detail-shell" v-if="order">
      <header class="detail-hero">
        <el-button class="back-link" @click="$router.back()" :icon="ArrowLeft" circle />
        <div class="hero-actions">
          <el-button :icon="Refresh" circle @click="fetchOrder" />
          <el-button v-if="order.status === 'COMPLETED'" @click="router.push('/my-orders')">再来一单</el-button>
        </div>
        <h1>{{ heroTitle }}</h1>
        <p>{{ heroSubtitle }}</p>
        <div class="hero-buttons">
          <el-button v-if="order.rider">联系骑手</el-button>
          <el-button>联系商家</el-button>
          <el-button v-if="order.status === 'PENDING_PAYMENT'" type="primary" @click="handlePay">立即支付</el-button>
        </div>
      </header>

      <section class="status-card" v-if="order.status !== 'COMPLETED' && order.status !== 'CANCELLED'">
        <div class="status-head">
          <strong>{{ statusLabel(order.status) }}</strong>
          <span>预计 {{ order.estimatedDeliveryTime || 30 }} 分钟内送达</span>
        </div>
        <el-steps :active="activeStep" align-center>
          <el-step title="下单" />
          <el-step title="支付/接单" />
          <el-step title="制作中" />
          <el-step title="配送中" />
          <el-step title="完成" />
        </el-steps>
      </section>

      <section v-if="['DELIVERING','PICKED_UP'].includes(order.status)" class="map-card">
        <OrderMap
          :store-lat="storeLat"
          :store-lng="storeLng"
          :store-name="order.merchant?.username"
          :rider-lat="riderLat"
          :rider-lng="riderLng"
          :rider-name="order.rider?.username"
          :customer-lat="order.customerLat || 39.9542"
          :customer-lng="order.customerLng || 116.4074"
          :static="false"
          :height="320"
        />
      </section>

      <section class="info-card">
        <h2>订单信息</h2>
        <div class="info-grid">
          <span>期望时间</span><strong>立即配送</strong>
          <span>配送地址</span><strong>{{ order.deliveryAddress || '-' }}</strong>
          <span>餐具数量</span><strong>商家按餐量提供</strong>
          <span>配送服务</span><strong>美味校园送</strong>
          <span>配送骑手</span><strong>{{ order.rider?.username || '待分配' }}</strong>
          <span>订单号码</span><strong>{{ order.orderNo }}</strong>
          <span>下单时间</span><strong>{{ formatTime(order.createdAt) }}</strong>
          <span>支付方式</span><strong>{{ order.status === 'PENDING_PAYMENT' ? '待支付' : '在线支付' }}</strong>
        </div>
      </section>

      <section class="fee-detail-card">
        <div class="section-title">
          <h2>商品费用</h2>
          <span>{{ order.merchant?.username || '商家' }}</span>
        </div>
        <div class="order-items">
          <article v-for="item in order.items" :key="item.id" class="order-item">
            <div>
              <h3>{{ item.dishName }}</h3>
              <p>x{{ item.quantity }}</p>
            </div>
            <strong>¥{{ money(item.dishPrice * item.quantity) }}</strong>
          </article>
        </div>
        <div class="price-breakdown">
          <div><span>商品小计</span><strong>¥{{ money(order.totalPrice) }}</strong></div>
          <div><span>配送费</span><strong>¥{{ money(order.deliveryFee) }}</strong></div>
          <div class="total"><span>合计</span><strong>¥{{ total() }}</strong></div>
        </div>
      </section>

      <section class="note-card" v-if="order.note">
        <h2>备注</h2>
        <p>{{ order.note }}</p>
      </section>
    </div>
    <el-skeleton v-else :rows="10" animated />
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, payOrder } from '../api/order'
import request from '../api/request'
import { ElMessage } from 'element-plus'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import OrderMap from '../components/OrderMap.vue'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const riderLat = ref(39.9087)
const riderLng = ref(116.4605)
const storeLat = ref(39.9087)
const storeLng = ref(116.4605)
let pollTimer = null

const statusMap = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  PENDING_ACCEPT: '待接单',
  PREPARING: '制作中',
  READY: '待取餐',
  PENDING_PICKUP: '待取餐',
  PICKED_UP: '已取餐',
  DELIVERING: '配送中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

const statusStepMap = {
  PENDING_PAYMENT: 0,
  PAID: 1,
  PENDING_ACCEPT: 1,
  PREPARING: 2,
  READY: 2,
  PENDING_PICKUP: 2,
  PICKED_UP: 3,
  DELIVERING: 3,
  COMPLETED: 4,
}

const activeStep = computed(() => statusStepMap[order.value?.status] ?? 0)

const heroTitle = computed(() => {
  const status = order.value?.status
  if (status === 'COMPLETED') return '订单已完成'
  if (status === 'CANCELLED') return '订单已取消'
  if (status === 'PENDING_PAYMENT') return '等待支付'
  if (status === 'PENDING_ACCEPT') return '等待商家接单'
  if (status === 'PREPARING') return '商家正在制作'
  if (status === 'DELIVERING' || status === 'PICKED_UP') return '订单配送中'
  return '订单进行中'
})

const heroSubtitle = computed(() => {
  const status = order.value?.status
  if (status === 'COMPLETED') return '感谢您的信任，请节约用餐，美味不浪费。'
  if (status === 'CANCELLED') return '订单已关闭，可重新下单。'
  if (status === 'PENDING_PAYMENT') return '请尽快完成支付，商家接单后会开始制作。'
  if (status === 'PENDING_ACCEPT') return '支付成功，正在等待商家确认订单。'
  if (status === 'PREPARING') return '商家已接单，正在准备您的餐品。'
  return `预计 ${order.value?.estimatedDeliveryTime || 30} 分钟内送达，请留意骑手电话。`
})

function statusLabel(s) { return statusMap[s] || s }

function money(value) {
  return Number(value || 0).toFixed(2)
}

function total() {
  if (!order.value) return '0.00'
  return money(Number(order.value.totalPrice || 0) + Number(order.value.deliveryFee || 0))
}

function formatTime(time) {
  if (!time) return '-'
  return time.replace('T', ' ').substring(0, 19)
}

async function fetchOrder() {
  try {
    order.value = await getOrderDetail(route.params.id)
    if (order.value.storeLat) storeLat.value = Number(order.value.storeLat)
    if (order.value.storeLng) storeLng.value = Number(order.value.storeLng)
  } catch { /* handled */ }
}

async function pollRiderLocation() {
  if (!order.value?.rider) return
  try {
    const loc = await request.get('/rider/location', { params: { riderId: order.value.rider.id } })
    if (loc) {
      riderLat.value = Number(loc.lat)
      riderLng.value = Number(loc.lng)
    }
  } catch { /* best effort */ }
}

async function handlePay() {
  try {
    await payOrder(order.value.id)
    ElMessage.success('支付成功')
    fetchOrder()
  } catch { /* handled */ }
}

onMounted(() => {
  fetchOrder()
  pollTimer = setInterval(() => {
    fetchOrder()
    pollRiderLocation()
  }, 5000)
})

onBeforeUnmount(() => {
  if (pollTimer) clearInterval(pollTimer)
})
</script>

<style scoped>
.order-detail-page { background: #f7f2ec; }
.detail-shell { max-width: 920px; margin: 0 auto; }
.detail-hero { position: relative; padding: 22px 8px 30px; }
.back-link { margin-bottom: 28px; border: none; background: #fff; }
.hero-actions { position: absolute; top: 22px; right: 8px; display: flex; gap: 10px; }
.detail-hero h1 { margin: 0 0 8px; font-size: 44px; line-height: 1.1; letter-spacing: 0; }
.detail-hero p { margin: 0; color: #606266; font-size: 18px; }
.hero-buttons { display: flex; gap: 14px; margin-top: 24px; flex-wrap: wrap; }
.hero-buttons :deep(.el-button) { min-width: 118px; border-radius: 999px; font-weight: 800; }
.status-card, .info-card, .fee-detail-card, .note-card, .map-card { margin-bottom: 16px; padding: 24px; border-radius: 18px; background: #fff; box-shadow: 0 8px 24px rgba(31, 35, 41, 0.06); }
.status-head { display: flex; justify-content: space-between; align-items: center; margin-bottom: 22px; }
.status-head strong { font-size: 20px; }
.status-head span { color: #ff8c00; font-weight: 700; }
.info-card h2, .fee-detail-card h2, .note-card h2 { margin: 0 0 18px; font-size: 24px; }
.info-grid { display: grid; grid-template-columns: 120px 1fr; gap: 18px 22px; color: #73777f; }
.info-grid strong { color: #303133; font-weight: 600; line-height: 1.7; }
.section-title { display: flex; align-items: baseline; justify-content: space-between; gap: 16px; margin-bottom: 12px; }
.section-title span { color: #909399; }
.order-items { display: grid; gap: 0; border-top: 1px solid #f0f1f3; border-bottom: 1px solid #f0f1f3; }
.order-item { display: flex; justify-content: space-between; align-items: center; padding: 14px 0; }
.order-item + .order-item { border-top: 1px solid #f0f1f3; }
.order-item h3 { margin: 0 0 5px; font-size: 17px; }
.order-item p { margin: 0; color: #909399; }
.order-item strong { font-size: 18px; }
.price-breakdown { display: grid; gap: 10px; margin-top: 18px; }
.price-breakdown div { display: flex; justify-content: space-between; align-items: center; }
.price-breakdown span { color: #606266; }
.price-breakdown strong { font-size: 18px; }
.price-breakdown .total strong { color: #ff4d4f; font-size: 24px; }
.note-card p { margin: 0; color: #606266; line-height: 1.7; }
@media (max-width: 720px) {
  .detail-hero h1 { font-size: 36px; }
  .hero-actions { position: static; margin-bottom: 14px; }
  .info-grid { grid-template-columns: 88px 1fr; }
  .status-card, .info-card, .fee-detail-card, .note-card, .map-card { padding: 18px; }
}
</style>
