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
      <div class="order-actions" v-if="order.status === 'PENDING_ACCEPT'">
        <el-button type="success" @click="handleAction(order, 'PREPARING')">接单并开始制作</el-button>
      </div>
      <div class="order-actions" v-if="order.status === 'PREPARING'">
        <el-button type="primary" @click="handleAction(order, 'PENDING_PICKUP')">已完成制作</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getMerchantOrders, updateOrderStatus } from '../../api/order'
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
function statusLabel(s) { return statusMap[s] || s }
function statusType(s) {
  if (s === 'COMPLETED') return 'success'
  if (s === 'CANCELLED') return 'info'
  return ''
}

async function fetchOrders() { try { orders.value = await getMerchantOrders() } catch { /* handled */ } }

async function handleAction(order, status) {
  try {
    await updateOrderStatus(order.id, status, 'MERCHANT')
    ElMessage.success('操作成功')
    fetchOrders()
  } catch { /* handled */ }
}

onMounted(fetchOrders)
</script>

<style scoped>
.order-header { display: flex; justify-content: space-between; align-items: center; }
.order-item { display: flex; justify-content: space-between; padding: 4px 0; }
.order-footer { display: flex; justify-content: space-between; align-items: center; }
.order-footer b { color: #f56c6c; font-size: 18px; }
.order-actions { margin-top: 12px; text-align: right; }
</style>
