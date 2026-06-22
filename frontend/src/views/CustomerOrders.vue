<template>
  <div class="page-container">
    <h1 class="page-title">📦 我的订单</h1>
    <el-empty v-if="orders.length === 0" description="暂无订单">
      <el-button type="primary" @click="$router.push('/')">去下单</el-button>
    </el-empty>
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
        <span>共 {{ order.items.length }} 件商品</span>
        <span class="total">实付：<b>¥{{ order.totalPrice }}</b></span>
      </div>
      <div style="margin-top:12px;color:#909399">
        配送地址：{{ order.deliveryAddress }}
      </div>
      <el-button v-if="order.status === 'PENDING_PAYMENT' || order.status === 'PENDING_ACCEPT'"
        type="danger" size="small" style="margin-top:8px"
        @click="handleCancel(order)">
        取消订单
      </el-button>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getCustomerOrders, updateOrderStatus } from '../api/order'
import { ElMessage } from 'element-plus'

const orders = ref([])

const statusMap = {
  PENDING_PAYMENT: '待支付',
  PENDING_ACCEPT: '待接单',
  PREPARING: '制作中',
  PENDING_PICKUP: '待取餐',
  DELIVERING: '配送中',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}
function statusLabel(status) { return statusMap[status] || status }
function statusType(status) {
  if (status === 'COMPLETED') return 'success'
  if (status === 'CANCELLED') return 'info'
  if (status === 'DELIVERING') return 'warning'
  return ''
}

async function fetchOrders() {
  try { orders.value = await getCustomerOrders() } catch { /* handled */ }
}

async function handleCancel(order) {
  try {
    await updateOrderStatus(order.id, 'CANCELLED', 'CUSTOMER')
    ElMessage.success('已取消')
    fetchOrders()
  } catch { /* handled */ }
}

onMounted(fetchOrders)
</script>

<style scoped>
.order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.order-item {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
}
.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.total b {
  color: #f56c6c;
  font-size: 18px;
}
</style>
