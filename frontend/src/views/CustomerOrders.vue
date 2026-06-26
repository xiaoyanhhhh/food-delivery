<template>
  <div class="page-container">
    <h1 class="page-title">
      <el-icon :size="22"><Tickets /></el-icon> 我的订单
      <el-tag v-if="polling" size="small" type="success" style="margin-left:8px">自动刷新中</el-tag>
    </h1>

    <el-empty v-if="orders.length === 0" description="暂无订单">
      <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
    </el-empty>

    <el-card v-for="order in orders" :key="order.id" class="order-card" shadow="hover"
      :class="{ updated: order._updated }">
      <div class="order-header">
        <span>订单号：{{ order.orderNo }}</span>
        <el-tag :type="statusType(order.status)">{{ statusLabel(order.status) }}</el-tag>
      </div>

      <el-divider />

      <div v-for="item in order.items" :key="item.id" class="order-item">
        <span>{{ item.dishName }} × {{ item.quantity }}</span>
        <span>{{ formatMoney(Number(item.dishPrice || 0) * Number(item.quantity || 0)) }}</span>
      </div>

      <el-divider />

      <div class="order-summary">
        <div><span>共 {{ itemsCount(order) }} 件</span></div>
        <div class="summary-lines">
          <span>商品小计：{{ formatMoney(orderSubtotal(order)) }}</span>
          <span v-if="Number(order.deliveryFee || 0) > 0">配送费：{{ formatMoney(order.deliveryFee) }}</span>
          <b>应付：{{ formatMoney(orderPayable(order)) }}</b>
        </div>
      </div>

      <div class="order-actions">
        <el-button v-if="order.status === 'PENDING_PAYMENT'" type="primary" size="small" @click="handlePay(order)">立即支付</el-button>
        <el-button v-if="['PENDING_PAYMENT','PAID','PENDING_ACCEPT'].includes(order.status)" type="danger" size="small" @click="handleCancel(order)">取消</el-button>
        <el-button v-if="!['PENDING_PAYMENT','COMPLETED','CANCELLED'].includes(order.status)" type="warning" size="small" @click="$router.push(`/orders/${order.id}/track`)">追踪</el-button>
        <el-button v-if="order.status === 'COMPLETED'" type="success" size="small"
          :loading="reorderingId === order.id" @click="handleReorder(order)">再来一单</el-button>
        <el-button v-if="order.status === 'COMPLETED'" type="primary" size="small" @click="showReview(order)">评价</el-button>
      </div>
    </el-card>

    <el-dialog v-model="reviewDialog.visible" title="评价" width="420px">
      <el-form>
        <el-form-item label="评分"><el-rate v-model="reviewDialog.rating" /></el-form-item>
        <el-form-item label="内容"><el-input v-model="reviewDialog.content" type="textarea" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="reviewDialog.visible = false">取消</el-button>
        <el-button type="primary" @click="submitReview" :loading="reviewing">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { getCustomerOrders, updateOrderStatus, payOrder, reorderOrder } from '../api/order'
import { createReview } from '../api/review'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePolling } from '../composables/usePolling'

const router = useRouter()
const orders = ref([])
const reviewing = ref(false)
const polling = ref(false)
const prevStatuses = ref({})
const reorderingId = ref(null)

const reviewDialog = reactive({
  visible: false,
  rating: 5,
  content: '',
  orderId: null,
  storeId: null,
})

const statusMap = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  PENDING_ACCEPT: '待接单',
  PREPARING: '制作中',
  READY: '已备好',
  PENDING_PICKUP: '待取餐',
  PICKED_UP: '已取餐',
  DELIVERING: '配送中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

function statusLabel(status) {
  return statusMap[status] || status
}

function statusType(status) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'CANCELLED') return 'info'
  if (status === 'DELIVERING' || status === 'PICKED_UP') return 'warning'
  return ''
}

function formatMoney(value) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function orderSubtotal(order) {
  return Number(order.totalPrice || 0)
}

function orderPayable(order) {
  return orderSubtotal(order) + Number(order.deliveryFee || 0)
}

function itemsCount(order) {
  return (order.items || []).reduce((sum, item) => sum + Number(item.quantity || 0), 0)
}

async function fetchOrders() {
  try {
    const data = await getCustomerOrders({ page: 0, size: 20 })
    const newOrders = data.content || data || []
    newOrders.forEach(order => {
      order._updated = prevStatuses.value[order.id] && prevStatuses.value[order.id] !== order.status
      prevStatuses.value[order.id] = order.status
    })
    orders.value = newOrders
    polling.value = true
  } catch { /* handled */ }
}

usePolling(fetchOrders, 15000)

async function handlePay(order) {
  try {
    await payOrder(order.id)
    ElMessage.success('支付成功')
    fetchOrders()
  } catch { /* handled */ }
}

async function handleCancel(order) {
  try {
    await updateOrderStatus(order.id, 'CANCELLED', 'CUSTOMER')
    ElMessage.success('已取消')
    fetchOrders()
  } catch { /* handled */ }
}

async function handleReorder(order) {
  try {
    await ElMessageBox.confirm('确定再来一单吗？将使用相同菜品创建新订单。', '提示', { type: 'info' })
    reorderingId.value = order.id
    const newOrder = await reorderOrder(order.id)
    ElMessage.success('已创建新订单，请继续支付')
    await fetchOrders()
    if (newOrder?.id) {
      router.push('/my-orders')
    }
  } catch { /* cancelled or handled */ }
  finally {
    reorderingId.value = null
  }
}

function showReview(order) {
  reviewDialog.orderId = order.id
  reviewDialog.storeId = order.merchant?.id
  reviewDialog.rating = 5
  reviewDialog.content = ''
  reviewDialog.visible = true
}

async function submitReview() {
  reviewing.value = true
  try {
    await createReview({
      rating: reviewDialog.rating,
      content: reviewDialog.content,
      storeId: reviewDialog.storeId,
      orderId: reviewDialog.orderId,
    })
    ElMessage.success('评价成功')
    reviewDialog.visible = false
    fetchOrders()
  } catch { /* handled */ }
  reviewing.value = false
}

onBeforeUnmount(() => {
  polling.value = false
})
</script>

<style scoped>
.order-card { margin-bottom: 16px; transition: border-color 0.3s; }
.order-card.updated { border-left: 4px solid #FF8C00; }
.order-header { display: flex; justify-content: space-between; align-items: center; }
.order-item { display: flex; justify-content: space-between; padding: 4px 0; }
.order-summary { display: flex; justify-content: space-between; align-items: flex-start; gap: 16px; }
.summary-lines { display: flex; flex-direction: column; align-items: flex-end; gap: 4px; color: #606266; }
.summary-lines b { color: #f56c6c; font-size: 18px; }
.order-actions { display: flex; gap: 8px; margin-top: 12px; justify-content: flex-end; }
</style>
