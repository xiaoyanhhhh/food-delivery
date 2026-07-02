<template>
  <div class="page-container">
    <h1 class="page-title">
      <el-icon :size="22"><Tickets /></el-icon> 我的配送
    </h1>
    <el-empty v-if="orders.length === 0" description="暂无配送订单" />
    <el-card v-for="order in orders" :key="order.id" style="margin-bottom: 16px">
      <div class="order-header">
        <span>订单号：{{ order.orderNo }}</span>
        <div class="tag-row">
          <el-tag v-if="isDispatchOffer(order)" type="danger">系统派单邀请</el-tag>
          <el-tag v-else-if="order.dispatchStatus === 'FORCE_ASSIGNED'" type="danger">强制派单</el-tag>
          <el-tag :type="statusType(order.status)">{{ statusLabel(order.status) }}</el-tag>
        </div>
      </div>
      <el-divider />
      <div v-for="item in order.items" :key="item.id" class="order-item">
        <span>{{ item.dishName }} × {{ item.quantity }}</span>
      </div>
      <el-divider />
      <div class="order-info">
        <p>商家：{{ order.merchant?.username }}</p>
        <p>顾客：{{ order.customer?.username }}</p>
        <p>地址：{{ order.deliveryAddress }}</p>
        <p>金额：<b>¥{{ order.totalPrice }}</b></p>
        <p v-if="order.deliveryFee">配送费：¥{{ order.deliveryFee }}</p>
        <p v-if="order.dispatchSurcharge" class="order-boost">无人派送加价 +¥{{ Number(order.dispatchSurcharge).toFixed(2) }}</p>
        <p v-if="order.estimatedDeliveryTime">预计{{ order.estimatedDeliveryTime }}分钟内送达</p>
      </div>
      <div class="order-actions">
        <template v-if="isDispatchOffer(order)">
          <span class="waiting-text">商家标记无人派送，10秒无人接将强制派单</span>
          <el-button type="primary" @click="handleAcceptDispatch(order)">接受派单</el-button>
          <el-button @click="handleRejectDispatch(order)">拒绝</el-button>
        </template>
        <span v-else-if="order.status === 'PREPARING'" class="waiting-text">
          已接单，等待商家出餐后才能取餐和配送
        </span>
        <el-button v-if="canChat(order)" @click="openChat(order)">顾客私信</el-button>
        <el-button v-if="!isDispatchOffer(order) && ['READY', 'PENDING_PICKUP'].includes(order.status)" type="primary"
          @click="handleAction(order, 'PICKED_UP')">
          确认取餐
        </el-button>
        <el-button v-if="!isDispatchOffer(order) && order.status === 'PICKED_UP'" type="warning"
          @click="handleAction(order, 'DELIVERING')">
          开始配送
        </el-button>
        <el-button v-if="!isDispatchOffer(order) && order.status === 'DELIVERING'" type="success"
          @click="handleAction(order, 'COMPLETED')">
          确认送达
        </el-button>
      </div>
    </el-card>

    <el-dialog v-model="chatDialogVisible" title="顾客私信" width="520px" @closed="stopChatPolling">
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
        <div v-if="selectedOrder && canSendChat(selectedOrder)" class="chat-send">
          <el-input
            v-model="chatInput"
            maxlength="500"
            show-word-limit
            placeholder="给顾客留言，例如：我已到店取餐，马上送达"
            @keyup.enter="sendChat"
          />
          <el-button type="primary" @click="sendChat">发送</el-button>
        </div>
        <div v-else class="chat-closed">订单已结束，不能继续发送私信</div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onBeforeUnmount, ref } from 'vue'
import {
  getOrderMessages,
  getRiderOrders,
  sendOrderMessage,
  updateOrderStatus,
  acceptDispatchOffer,
  rejectDispatchOffer,
} from '../../api/order'
import { ElMessage } from 'element-plus'
import { usePolling } from '../../composables/usePolling'
import { useAuthStore } from '../../stores/auth'

const orders = ref([])
const auth = useAuthStore()
const chatDialogVisible = ref(false)
const chatMessages = ref([])
const chatInput = ref('')
const chatLoading = ref(false)
const selectedOrder = ref(null)
let chatTimer = null

const statusMap = {
  PREPARING: '商家制作中', READY: '待取餐', PENDING_PICKUP: '待取餐', PICKED_UP: '已取餐', DELIVERING: '配送中',
  COMPLETED: '已完成', CANCELLED: '已取消',
}
function statusLabel(s) { return statusMap[s] || s }
function statusType(s) {
  if (s === 'COMPLETED') return 'success'
  if (s === 'PREPARING') return 'warning'
  if (s === 'DELIVERING' || s === 'PICKED_UP') return 'warning'
  return ''
}

function isDispatchOffer(order) {
  return !order.rider
    && ['REQUESTED', 'REJECTED'].includes(order.dispatchStatus)
    && order.dispatchCandidateRider?.id === auth.getUserId()
}

function canChat(order) {
  return order.rider?.id === auth.getUserId()
}

function canSendChat(order) {
  return canChat(order) && !['COMPLETED', 'CANCELLED'].includes(order.status)
}

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

async function fetchOrders() {
  try {
    const data = await getRiderOrders({ page: 0, size: 20 })
    orders.value = data.content || data
  } catch { /* handled */ }
}

async function handleAction(order, status) {
  try {
    await updateOrderStatus(order.id, status, 'RIDER')
    const msg = status === 'COMPLETED' ? '订单已完成' : status === 'DELIVERING' ? '开始配送' : '已取餐'
    ElMessage.success(msg)
    fetchOrders()
  } catch { /* handled */ }
}

async function handleAcceptDispatch(order) {
  try {
    await acceptDispatchOffer(order.id)
    ElMessage.success('已接受派单，订单进入待取餐')
    fetchOrders()
  } catch { /* handled */ }
}

async function handleRejectDispatch(order) {
  try {
    await rejectDispatchOffer(order.id)
    ElMessage.success('已拒绝派单')
    fetchOrders()
  } catch { /* handled */ }
}

async function openChat(order) {
  selectedOrder.value = order
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
  if (!selectedOrder.value?.id || !chatDialogVisible.value) return
  chatLoading.value = true
  try {
    chatMessages.value = await getOrderMessages(selectedOrder.value.id)
  } catch { /* handled */ }
  chatLoading.value = false
}

async function sendChat() {
  const content = chatInput.value.trim()
  if (!content || !selectedOrder.value) return
  try {
    await sendOrderMessage(selectedOrder.value.id, content)
    chatInput.value = ''
    await fetchChatMessages()
  } catch { /* handled */ }
}

usePolling(fetchOrders, 5000)
onBeforeUnmount(stopChatPolling)
</script>

<style scoped>
.order-header { display: flex; justify-content: space-between; align-items: center; }
.order-item { padding: 2px 0; }
.order-info p { margin: 4px 0; color: #606266; }
.order-info b { color: #f56c6c; }
.order-actions { margin-top: 12px; text-align: right; }
.waiting-text { color: #909399; font-size: 13px; }
.order-boost { color: #f56c6c; font-weight: 700; }
.tag-row { display: flex; align-items: center; gap: 6px; }
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
</style>
