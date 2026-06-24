<template>
  <div class="page-container">
    <h1 class="page-title">📦 我的配送</h1>
    <el-empty v-if="orders.length === 0" description="暂无配送订单" />
    <el-card v-for="order in orders" :key="order.id" style="margin-bottom: 16px">
      <div class="order-header">
        <span>订单号：{{ order.orderNo }}</span>
        <el-tag :type="statusType(order.status)">{{ statusLabel(order.status) }}</el-tag>
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
        <p v-if="order.estimatedDeliveryTime">预计{{ order.estimatedDeliveryTime }}分钟内送达</p>
      </div>
      <div class="order-actions">
        <el-button v-if="order.status === 'READY'" type="primary"
          @click="handleAction(order, 'PICKED_UP')">
          确认取餐
        </el-button>
        <el-button v-if="order.status === 'PICKED_UP'" type="warning"
          @click="handleAction(order, 'DELIVERING')">
          开始配送
        </el-button>
        <el-button v-if="order.status === 'DELIVERING'" type="success"
          @click="handleAction(order, 'COMPLETED')">
          确认送达
        </el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getRiderOrders, updateOrderStatus } from '../../api/order'
import { ElMessage } from 'element-plus'
import { usePolling } from '../../composables/usePolling'

const orders = ref([])

const statusMap = {
  READY: '待取餐', PICKED_UP: '已取餐', DELIVERING: '配送中',
  COMPLETED: '已完成', CANCELLED: '已取消',
}
function statusLabel(s) { return statusMap[s] || s }
function statusType(s) {
  if (s === 'COMPLETED') return 'success'
  if (s === 'DELIVERING' || s === 'PICKED_UP') return 'warning'
  return ''
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

usePolling(fetchOrders, 15000)
</script>

<style scoped>
.order-header { display: flex; justify-content: space-between; align-items: center; }
.order-item { padding: 2px 0; }
.order-info p { margin: 4px 0; color: #606266; }
.order-info b { color: #f56c6c; }
.order-actions { margin-top: 12px; text-align: right; }
</style>
