<template>
  <div class="page-container">
    <h1 class="page-title">
      <el-icon :size="22"><Document /></el-icon> 订单管理
    </h1>

    <el-empty v-if="orders.length === 0" description="暂无订单" />

    <el-card v-for="order in orders" :key="order.id" class="order-card">
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

      <div class="order-info">
        <p>顾客：{{ order.customer?.username }}</p>
        <p>{{ isPickupOrder(order) ? '取餐方式' : '地址' }}：{{ order.deliveryAddress }}</p>
        <p>商品小计：{{ formatMoney(order.totalPrice) }}</p>
        <p>{{ isPickupOrder(order) ? '自取服务费' : '配送费' }}：{{ formatMoney(orderDeliveryFee(order)) }}</p>
        <p v-if="order.dispatchSurcharge" class="dispatch-price">无人派送加价：+{{ formatMoney(order.dispatchSurcharge) }}</p>
        <p v-if="order.dispatchStatus && order.dispatchStatus !== 'NONE'" class="dispatch-info">
          派单状态：{{ dispatchLabel(order.dispatchStatus) }}
          <span v-if="order.dispatchCandidateRider">，候选骑手：{{ order.dispatchCandidateRider.username }}</span>
        </p>
        <p class="payable">应付：{{ formatMoney(orderPayable(order)) }}</p>
        <p v-if="isPickupOrder(order)">顾客到店自取，无需骑手配送</p>
        <p v-else-if="order.estimatedDeliveryTime">预计 {{ order.estimatedDeliveryTime }} 分钟内送达</p>
      </div>

      <div v-if="order.note" class="note">备注：{{ order.note }}</div>

      <div class="order-actions">
        <el-button v-if="order.status === 'PENDING_ACCEPT' || order.status === 'PAID'" type="success"
          @click="handleAction(order, 'PREPARING')">
          接单并制作
        </el-button>
        <el-button v-if="order.status === 'PREPARING'" type="primary"
          @click="handleAction(order, 'READY')">
          制作完成
        </el-button>
        <el-button v-if="canRequestNoRider(order)" type="warning"
          @click="handleNoRider(order)">
          无人派送
        </el-button>
        <el-button v-if="canConfirmPickup(order)" type="success"
          @click="handleConfirmPickup(order)">
          确认顾客已取餐
        </el-button>
        <div v-if="order.status === 'COMPLETED'" class="done-text">订单已完成</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getMerchantOrders, updateOrderStatus, requestNoRiderDispatch } from '../../api/order'
import { ElMessage, ElMessageBox } from 'element-plus'
import { usePolling } from '../../composables/usePolling'

const orders = ref([])

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
  if (status === 'PENDING_ACCEPT' || status === 'PAID') return 'warning'
  return ''
}

function dispatchLabel(status) {
  const map = {
    REQUESTED: '加价派单中',
    REJECTED: '骑手已拒绝，等待强制派单',
    ACCEPTED: '骑手已接派单',
    FORCE_ASSIGNED: '系统已强制派单',
  }
  return map[status] || status
}

function formatMoney(value) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function isPickupOrder(order) {
  return (order.deliveryAddress || '').includes('到店自取') || (order.note || '').includes('到店自取')
}

function orderDeliveryFee(order) {
  return isPickupOrder(order) ? 0 : Number(order.deliveryFee || 0)
}

function orderPayable(order) {
  return Number(order.totalPrice || 0) + orderDeliveryFee(order)
}

async function fetchOrders() {
  try {
    const data = await getMerchantOrders({ page: 0, size: 20 })
    orders.value = data.content || data
  } catch { /* handled */ }
}

async function handleAction(order, status) {
  try {
    await updateOrderStatus(order.id, status, 'MERCHANT')
    ElMessage.success('操作成功')
    fetchOrders()
  } catch { /* handled */ }
}

function canRequestNoRider(order) {
  return !isPickupOrder(order)
    && !order.rider
    && ['PREPARING', 'READY', 'PENDING_PICKUP'].includes(order.status)
    && !['REQUESTED', 'ACCEPTED', 'FORCE_ASSIGNED'].includes(order.dispatchStatus)
}

async function handleNoRider(order) {
  try {
    await ElMessageBox.confirm('确认当前无人派送？系统将加价并优先派给全职骑手，10秒无人接单后会强制派单。', '无人派送', { type: 'warning' })
    await requestNoRiderDispatch(order.id)
    ElMessage.success('已发起无人派送，配送费已加价')
    fetchOrders()
  } catch { /* cancelled or handled */ }
}

function canConfirmPickup(order) {
  return isPickupOrder(order) && ['READY', 'PENDING_PICKUP'].includes(order.status)
}

async function handleConfirmPickup(order) {
  try {
    await ElMessageBox.confirm('确认顾客已经取餐了吗？确认后订单将完成。', '确认取餐', { type: 'success' })
    await updateOrderStatus(order.id, 'COMPLETED', 'MERCHANT')
    ElMessage.success('已确认取餐，订单完成')
    fetchOrders()
  } catch { /* cancelled or handled */ }
}

usePolling(fetchOrders, 15000)
</script>

<style scoped>
.order-card { margin-bottom: 16px; }
.order-header { display: flex; justify-content: space-between; align-items: center; }
.order-item { display: flex; justify-content: space-between; padding: 4px 0; }
.order-info { color: #606266; line-height: 1.8; }
.order-info p { margin: 0; }
.payable { color: #f56c6c; font-weight: 700; }
.dispatch-price { color: #f56c6c; font-weight: 700; }
.dispatch-info { color: #e6a23c; }
.note { margin-top: 8px; color: #e6a23c; font-size: 13px; }
.order-actions { margin-top: 12px; text-align: right; }
.done-text { color:#909399;font-size:12px; }
</style>
