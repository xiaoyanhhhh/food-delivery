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
      </div>
      <div class="order-actions">
        <el-button v-if="order.status === 'PENDING_PICKUP'" type="primary"
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
import { ref, onMounted } from 'vue'
import { getRiderOrders, updateOrderStatus } from '../../api/order'
import { ElMessage } from 'element-plus'

const orders = ref([])

const statusMap = {
  PENDING_PICKUP: '待取餐',
  DELIVERING: '配送中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}
function statusLabel(s) { return statusMap[s] || s }
function statusType(s) {
  if (s === 'COMPLETED') return 'success'
  if (s === 'DELIVERING') return 'warning'
  return ''
}

async function fetchOrders() { try { orders.value = await getRiderOrders() } catch { /* handled */ } }

async function handleAction(order, status) {
  try {
    await updateOrderStatus(order.id, status, 'RIDER')
    ElMessage.success(status === 'COMPLETED' ? '订单已完成' : '开始配送')
    fetchOrders()
  } catch { /* handled */ }
}

onMounted(fetchOrders)
</script>

<style scoped>
.order-header { display: flex; justify-content: space-between; align-items: center; }
.order-item { padding: 2px 0; }
.order-info p { margin: 4px 0; color: #606266; }
.order-info b { color: #f56c6c; }
.order-actions { margin-top: 12px; text-align: right; }
</style>
