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
        <p>地址：{{ order.deliveryAddress }}</p>
        <p>商品小计：{{ formatMoney(order.totalPrice) }}</p>
        <p>配送费：{{ formatMoney(order.deliveryFee) }}</p>
        <p class="payable">应付：{{ formatMoney(orderPayable(order)) }}</p>
        <p v-if="order.estimatedDeliveryTime">预计 {{ order.estimatedDeliveryTime }} 分钟内送达</p>
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
        <div v-if="order.status === 'COMPLETED'" class="done-text">订单已完成</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getMerchantOrders, updateOrderStatus } from '../../api/order'
import { ElMessage } from 'element-plus'
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

function formatMoney(value) {
  return `¥${Number(value || 0).toFixed(2)}`
}

function orderPayable(order) {
  return Number(order.totalPrice || 0) + Number(order.deliveryFee || 0)
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

usePolling(fetchOrders, 15000)
</script>

<style scoped>
.order-card { margin-bottom: 16px; }
.order-header { display: flex; justify-content: space-between; align-items: center; }
.order-item { display: flex; justify-content: space-between; padding: 4px 0; }
.order-info { color: #606266; line-height: 1.8; }
.order-info p { margin: 0; }
.payable { color: #f56c6c; font-weight: 700; }
.note { margin-top: 8px; color: #e6a23c; font-size: 13px; }
.order-actions { margin-top: 12px; text-align: right; }
.done-text { color:#909399;font-size:12px; }
</style>
