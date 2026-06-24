<template>
  <div class="page-container">
    <h1 class="page-title">📋 订单管理</h1>
    <el-empty v-if="orders.length === 0" description="暂无订单" />
    <el-card v-for="order in orders" :key="order.id" style="margin-bottom: 16px">
      <div class="order-header">
        <span>订单号：{{ order.orderNo }}</span>
        <el-tag :type="statusType(order.status)">{{ statusLabel(order.status) }}</el-tag>
      </div>
      <el-divider />
      <div v-for="item in order.items" :key="item.id" class="order-item">
        <span>{{ item.dishName }} × {{ item.quantity }}</span>
        <span>¥{{ (item.dishPrice * item.quantity).toFixed(2) }}</span>
      </div>
      <el-divider />
      <div class="order-footer">
        <span>顾客：{{ order.customer?.username }}</span>
        <span>总计：<b>¥{{ order.totalPrice }}</b></span>
      </div>
      <div class="order-actions">
        <el-button v-if="order.status === 'PENDING_ACCEPT' || order.status === 'PAID'" type="success"
          @click="handleAction(order, 'PREPARING')">
          接单并制作
        </el-button>
        <el-button v-if="order.status === 'PREPARING'" type="primary"
          @click="handleAction(order, 'READY')">
          制作完成
        </el-button>
        <!-- Reply review if needed -->
        <div v-if="order.status === 'COMPLETED'" style="color:#909399;font-size:12px">
          订单已完成
        </div>
      </div>
      <div v-if="order.note" style="margin-top:8px;color:#e6a23c;font-size:13px">
        备注：{{ order.note }}
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
  PENDING_PAYMENT: '待支付', PAID: '已支付', PENDING_ACCEPT: '待接单',
  PREPARING: '制作中', READY: '已备好', PENDING_PICKUP: '待取餐',
  PICKED_UP: '已取餐', DELIVERING: '配送中', COMPLETED: '已完成', CANCELLED: '已取消',
}
function statusLabel(s) { return statusMap[s] || s }
function statusType(s) {
  if (s === 'COMPLETED') return 'success'
  if (s === 'CANCELLED') return 'info'
  return ''
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
.order-header { display: flex; justify-content: space-between; align-items: center; }
.order-item { display: flex; justify-content: space-between; padding: 4px 0; }
.order-footer { display: flex; justify-content: space-between; align-items: center; }
.order-footer b { color: #f56c6c; font-size: 18px; }
.order-actions { margin-top: 12px; text-align: right; }
</style>
