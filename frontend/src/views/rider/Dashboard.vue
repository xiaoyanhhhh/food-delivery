<template>
  <div class="page-container">
    <h1 class="page-title">🛵 骑手中心</h1>
    <el-row :gutter="20">
      <el-col :span="12">
        <el-card @click="$router.push('/rider/orders')" class="menu-card" shadow="hover">
          <div class="menu-content">
            <el-icon :size="48"><List /></el-icon>
            <h3>我的配送</h3>
            <p>查看正在配送的订单</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card @click="fetchAvailable(); showAvailable = true" class="menu-card" shadow="hover">
          <div class="menu-content">
            <el-icon :size="48"><Plus /></el-icon>
            <h3>可接订单</h3>
            <p>查看待取餐订单并接单</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Available orders dialog -->
    <el-dialog v-model="showAvailable" title="可接订单" width="600px">
      <el-empty v-if="availableOrders.length === 0" description="暂无待接订单" />
      <el-card v-for="order in availableOrders" :key="order.id" style="margin-bottom:12px">
        <div class="order-header">
          <span>订单号：{{ order.orderNo }}</span>
          <el-tag>待取餐</el-tag>
        </div>
        <el-divider />
        <div v-for="item in order.items" :key="item.id" class="order-item">
          <span>{{ item.dishName }} × {{ item.quantity }}</span>
        </div>
        <div style="margin-top:8px">
          商家：{{ order.merchant?.username }} | 配送地址：{{ order.deliveryAddress }}
        </div>
        <el-button type="primary" style="margin-top:8px" @click="handleAccept(order)">
          接单
        </el-button>
      </el-card>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getAvailableOrders, acceptOrder } from '../../api/order'
import { ElMessage } from 'element-plus'

const showAvailable = ref(false)
const availableOrders = ref([])

async function fetchAvailable() {
  try { availableOrders.value = await getAvailableOrders() } catch { /* handled */ }
}

async function handleAccept(order) {
  try {
    await acceptOrder(order.id)
    ElMessage.success('接单成功')
    fetchAvailable()
  } catch { /* handled */ }
}
</script>

<style scoped>
.menu-card {
  cursor: pointer;
  text-align: center;
  transition: transform 0.2s;
}
.menu-card:hover { transform: translateY(-4px); }
.menu-content { padding: 40px 0; }
.menu-content h3 { margin: 12px 0 6px; font-size: 18px; }
.menu-content p { color: #909399; font-size: 14px; }
.order-header { display: flex; justify-content: space-between; }
.order-item { padding: 2px 0; }
</style>
