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
          <el-button v-if="canChatWithRider" type="primary" @click="openRiderChat">骑手私信</el-button>
          <el-button @click="openMerchantContact">联系商家</el-button>
          <el-button v-if="canConfirmPickup" type="success" @click="handleConfirmPickup">确认取餐</el-button>
          <el-button v-if="order.status === 'PENDING_PAYMENT'" type="primary" @click="handlePay">立即支付</el-button>
        </div>
      </header>

      <section class="status-card" v-if="order.status !== 'COMPLETED' && order.status !== 'CANCELLED'">
        <div class="status-head">
          <strong>{{ statusLabel(order.status) }}</strong>
          <span>{{ statusHint }}</span>
        </div>
        <el-steps :active="activeStep" align-center>
          <el-step v-for="step in stepTitles" :key="step" :title="step" />
        </el-steps>
      </section>

      <section v-if="!isPickupOrder && ['DELIVERING','PICKED_UP'].includes(order.status)" class="map-card">
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
          <span>期望时间</span><strong>{{ expectedTimeText }}</strong>
          <span>{{ isPickupOrder ? '取餐方式' : '配送地址' }}</span><strong>{{ order.deliveryAddress || '-' }}</strong>
          <span>餐具数量</span><strong>商家按餐量提供</strong>
          <span>{{ isPickupOrder ? '取餐服务' : '配送服务' }}</span><strong>{{ isPickupOrder ? '到店自取' : '校园送' }}</strong>
          <template v-if="!isPickupOrder">
            <span>配送骑手</span><strong>{{ order.rider?.username || '待分配' }}</strong>
          </template>
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
          <div><span>{{ isPickupOrder ? '自取服务费' : '配送费' }}</span><strong>¥{{ displayDeliveryFee }}</strong></div>
          <div class="total"><span>合计</span><strong>¥{{ total() }}</strong></div>
        </div>
      </section>

      <section class="note-card" v-if="order.note">
        <h2>备注</h2>
        <p>{{ order.note }}</p>
      </section>

      <el-dialog v-model="contactDialogVisible" :title="contactDialog.title" width="420px">
        <div class="contact-card">
          <p><span>联系人</span><strong>{{ contactDialog.name }}</strong></p>
          <p>
            <span>联系电话</span>
            <strong v-if="contactDialog.phone">
              <a :href="`tel:${contactDialog.phone}`">{{ contactDialog.phone }}</a>
            </strong>
            <strong v-else>暂无联系电话</strong>
          </p>
          <p v-if="contactDialog.tip" class="contact-tip">{{ contactDialog.tip }}</p>
        </div>
        <template #footer>
          <el-button @click="contactDialogVisible = false">关闭</el-button>
          <el-button v-if="contactDialog.phone" type="primary" @click="callPhone(contactDialog.phone)">拨打电话</el-button>
        </template>
      </el-dialog>

      <el-dialog v-model="chatDialogVisible" title="骑手私信" width="520px" @closed="stopChatPolling">
        <div class="chat-panel" v-loading="chatLoading">
          <el-empty v-if="chatMessages.length === 0" description="暂无消息" />
          <div v-else class="chat-list">
            <div v-for="message in chatMessages" :key="message.id" class="chat-row" :class="{ mine: message.mine }">
              <div class="chat-bubble">
                <div class="chat-meta">
                  <span>{{ message.mine ? '我' : message.senderName }}</span>
                  <time>{{ formatTime(message.createdAt) }}</time>
                </div>
                <p>{{ message.content }}</p>
              </div>
            </div>
          </div>
        </div>
        <template #footer>
          <div v-if="canSendChat" class="chat-send">
            <el-input
              v-model="chatInput"
              maxlength="500"
              show-word-limit
              placeholder="给骑手留言，例如：麻烦送到宿舍楼下"
              @keyup.enter="sendChat"
            />
            <el-button type="primary" @click="sendChat">发送</el-button>
          </div>
          <div v-else class="chat-closed">订单已结束，不能继续发送私信</div>
        </template>
      </el-dialog>
    </div>
    <el-skeleton v-else :rows="10" animated />
  </div>
</template>

<script setup>
import { ref, computed, reactive, onBeforeUnmount, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, getOrderMessages, payOrder, sendOrderMessage, updateOrderStatus } from '../api/order'
import request from '../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, Refresh } from '@element-plus/icons-vue'
import OrderMap from '../components/OrderMap.vue'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const riderLat = ref(39.9087)
const riderLng = ref(116.4605)
const storeLat = ref(39.9087)
const storeLng = ref(116.4605)
const contactDialogVisible = ref(false)
const chatDialogVisible = ref(false)
const chatMessages = ref([])
const chatInput = ref('')
const chatLoading = ref(false)
const contactDialog = reactive({
  title: '',
  name: '',
  phone: '',
  tip: '',
})
let pollTimer = null
let chatTimer = null

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

const pickupStepMap = {
  PENDING_PAYMENT: 0,
  PAID: 1,
  PENDING_ACCEPT: 1,
  PREPARING: 2,
  READY: 3,
  PENDING_PICKUP: 3,
  PICKED_UP: 3,
  DELIVERING: 3,
  COMPLETED: 4,
}

const isPickupOrder = computed(() => {
  const address = order.value?.deliveryAddress || ''
  const note = order.value?.note || ''
  return address.includes('到店自取') || note.includes('到店自取')
})

const activeStep = computed(() => {
  const status = order.value?.status
  return isPickupOrder.value ? (pickupStepMap[status] ?? 0) : (statusStepMap[status] ?? 0)
})

const stepTitles = computed(() => (
  isPickupOrder.value
    ? ['下单', '支付/接单', '制作中', '待自取', '完成']
    : ['下单', '支付/接单', '制作中', '配送中', '完成']
))

const statusHint = computed(() => {
  if (!order.value) return ''
  if (isPickupOrder.value) {
    if (order.value.status === 'READY' || order.value.status === 'PENDING_PICKUP') return '餐品已备好，请到店自取'
    if (order.value.status === 'PREPARING') return '商家正在备餐，请稍后到店取餐'
    return '到店自取订单，无需等待骑手配送'
  }
  return `预计 ${order.value.estimatedDeliveryTime || 30} 分钟内送达`
})

const expectedTimeText = computed(() => {
  if (isPickupOrder.value) return '到店自取'
  const note = order.value?.note || ''
  const match = note.match(/预约配送：([^；]+)/)
  return match?.[1] ? `预约配送 ${match[1]}` : '立即配送'
})

const displayDeliveryFee = computed(() => (
  isPickupOrder.value ? '0.00' : money(order.value?.deliveryFee)
))

const canConfirmPickup = computed(() => (
  isPickupOrder.value && ['READY', 'PENDING_PICKUP'].includes(order.value?.status)
))

const canChatWithRider = computed(() => (
  !!order.value?.rider && !isPickupOrder.value
))

const canSendChat = computed(() => (
  canChatWithRider.value && !['COMPLETED', 'CANCELLED'].includes(order.value?.status)
))

const heroTitle = computed(() => {
  const status = order.value?.status
  if (status === 'COMPLETED') return '订单已完成'
  if (status === 'CANCELLED') return '订单已取消'
  if (status === 'PENDING_PAYMENT') return '等待支付'
  if (status === 'PENDING_ACCEPT') return '等待商家接单'
  if (status === 'PREPARING') return isPickupOrder.value ? '商家正在备餐' : '商家正在制作'
  if (isPickupOrder.value && (status === 'READY' || status === 'PENDING_PICKUP')) return '餐品待自取'
  if (status === 'DELIVERING' || status === 'PICKED_UP') return '订单配送中'
  return '订单进行中'
})

const heroSubtitle = computed(() => {
  const status = order.value?.status
  if (status === 'COMPLETED') return '感谢您的信任，请节约用餐，不浪费。'
  if (status === 'CANCELLED') return '订单已关闭，可重新下单。'
  if (status === 'PENDING_PAYMENT') return '请尽快完成支付，商家接单后会开始制作。'
  if (status === 'PENDING_ACCEPT') return '支付成功，正在等待商家确认订单。'
  if (status === 'PREPARING') return isPickupOrder.value ? '商家已接单，正在准备您的餐品，请稍后到店取餐。' : '商家已接单，正在准备您的餐品。'
  if (isPickupOrder.value && (status === 'READY' || status === 'PENDING_PICKUP')) return '餐品已备好，请前往商家处自取。'
  if (isPickupOrder.value) return '这是到店自取订单，无需等待骑手配送。'
  return `预计 ${order.value?.estimatedDeliveryTime || 30} 分钟内送达，请留意骑手电话。`
})

function statusLabel(s) { return statusMap[s] || s }

function money(value) {
  return Number(value || 0).toFixed(2)
}

function total() {
  if (!order.value) return '0.00'
  return money(Number(order.value.totalPrice || 0) + Number(isPickupOrder.value ? 0 : (order.value.deliveryFee || 0)))
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

function openMerchantContact() {
  contactDialog.title = '联系商家'
  contactDialog.name = order.value?.merchant?.username || '商家'
  contactDialog.phone = order.value?.merchant?.phone || ''
  contactDialog.tip = contactDialog.phone ? '' : '商家暂未公开手机号，可凭订单号向平台或店铺核对。'
  contactDialogVisible.value = true
}

async function openRiderChat() {
  chatDialogVisible.value = true
  await fetchChatMessages()
  stopChatPolling()
  chatTimer = setInterval(fetchChatMessages, 3000)
}

function stopChatPolling() {
  if (chatTimer) {
    clearInterval(chatTimer)
    chatTimer = null
  }
}

async function fetchChatMessages() {
  if (!order.value?.id || !chatDialogVisible.value) return
  chatLoading.value = true
  try {
    chatMessages.value = await getOrderMessages(order.value.id)
  } catch { /* handled */ }
  chatLoading.value = false
}

async function sendChat() {
  const content = chatInput.value.trim()
  if (!content) return
  try {
    await sendOrderMessage(order.value.id, content)
    chatInput.value = ''
    await fetchChatMessages()
  } catch { /* handled */ }
}

function callPhone(phone) {
  window.location.href = `tel:${phone}`
}

async function handleConfirmPickup() {
  try {
    await ElMessageBox.confirm('确认您已到店取到餐了吗？确认后订单将完成。', '确认取餐', { type: 'success' })
    await updateOrderStatus(order.value.id, 'COMPLETED', 'CUSTOMER')
    ElMessage.success('取餐成功，订单已完成')
    fetchOrder()
  } catch { /* cancelled or handled */ }
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
  stopChatPolling()
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
.contact-card { display: grid; gap: 14px; }
.contact-card p { display: flex; justify-content: space-between; gap: 16px; margin: 0; line-height: 1.7; }
.contact-card span { color: #73777f; }
.contact-card strong { color: #303133; text-align: right; }
.contact-card a { color: #ff8c00; font-weight: 800; }
.contact-tip { display: block !important; color: #909399; font-size: 13px; }
.chat-panel { min-height: 280px; max-height: 420px; overflow-y: auto; padding: 4px; background: #f7f2ec; border-radius: 10px; }
.chat-list { display: grid; gap: 10px; }
.chat-row { display: flex; justify-content: flex-start; }
.chat-row.mine { justify-content: flex-end; }
.chat-bubble { max-width: 76%; padding: 10px 12px; border-radius: 10px; background: #fff; box-shadow: 0 2px 8px rgba(31, 35, 41, 0.06); }
.chat-row.mine .chat-bubble { background: #fff4e5; }
.chat-meta { display: flex; justify-content: space-between; gap: 12px; margin-bottom: 6px; font-size: 12px; color: #909399; }
.chat-bubble p { margin: 0; line-height: 1.6; color: #303133; white-space: pre-wrap; word-break: break-word; }
.chat-send { display: grid; grid-template-columns: 1fr auto; gap: 10px; width: 100%; }
.chat-closed { width: 100%; color: #909399; text-align: center; }
@media (max-width: 720px) {
  .detail-hero h1 { font-size: 36px; }
  .hero-actions { position: static; margin-bottom: 14px; }
  .info-grid { grid-template-columns: 88px 1fr; }
  .status-card, .info-card, .fee-detail-card, .note-card, .map-card { padding: 18px; }
}
</style>
