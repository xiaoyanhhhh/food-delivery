<template>
  <div class="page-container">
    <h1 class="page-title">🛵 骑手中心</h1>

    <!-- Earnings Cards -->
    <el-row :gutter="20" style="margin-bottom:20px">
      <el-col :span="6">
        <el-card class="stat-card"><div class="stat-value">¥{{ earnings.todayEarnings }}</div><div class="stat-label">今日收入</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card"><div class="stat-value">¥{{ earnings.totalEarnings }}</div><div class="stat-label">累计收入</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card"><div class="stat-value">{{ earnings.todayCompleted }}</div><div class="stat-label">今日完成</div></el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card"><div class="stat-value">{{ earnings.totalCompleted }}</div><div class="stat-label">累计完成</div></el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <router-link to="/rider/orders" style="text-decoration:none;color:inherit">
          <el-card class="menu-card" shadow="hover">
            <div class="menu-content">
              <span style="font-size:48px">📦</span>
              <h3>我的配送</h3>
              <p>查看正在配送的订单</p>
            </div>
          </el-card>
        </router-link>
      </el-col>
      <el-col :span="12">
        <el-card class="menu-card" shadow="hover" @click="showAvailable = true; fetchAvailable()">
          <div class="menu-content">
            <span style="font-size:48px">🔍</span>
            <h3>可接订单</h3>
            <p>查看待取餐订单并接单</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-dialog v-model="showAvailable" title="可接订单" width="650px">
      <el-empty v-if="availableOrders.length === 0" description="暂无待接订单" />
      <el-card v-for="order in availableOrders" :key="order.id" style="margin-bottom:12px">
        <div class="order-header">
          <span>订单号：{{ order.orderNo }}</span>
          <el-tag>待取餐</el-tag>
        </div>
        <el-divider />
        <div v-for="item in order.items" :key="item.id"><span>{{ item.dishName }} × {{ item.quantity }}</span></div>
        <div style="margin-top:8px;color:#606266">
          <p>商家：{{ order.merchant?.username }}</p>
          <p>地址：{{ order.deliveryAddress }}</p>
          <p v-if="order.deliveryFee">配送费：¥{{ order.deliveryFee }}</p>
        </div>
        <el-button type="primary" style="margin-top:8px" @click="handleAccept(order)">接单</el-button>
      </el-card>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAvailableOrders, acceptOrder } from '../../api/order'
import request from '../../api/request'
import { ElMessage } from 'element-plus'

const showAvailable = ref(false)
const availableOrders = ref([])
const earnings = reactive({ todayEarnings: '0.00', totalEarnings: '0.00', todayCompleted: 0, totalCompleted: 0 })

async function fetchEarnings() {
  try { Object.assign(earnings, await request.get('/rider/earnings')) } catch { /* handled */ }
}

async function fetchAvailable() {
  try { const data = await getAvailableOrders({ page: 0, size: 20 }); availableOrders.value = data.content || data } catch { /* handled */ }
}

async function handleAccept(order) {
  try { await acceptOrder(order.id); ElMessage.success('接单成功'); fetchAvailable(); fetchEarnings() } catch { /* handled */ }
}

onMounted(() => { fetchEarnings() })
</script>

<style scoped>
.stat-card { text-align: center; padding: 10px 0; }
.stat-value { font-size: 28px; font-weight: bold; color: #409eff; }
.stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
.menu-card { cursor: pointer; text-align: center; transition: transform 0.2s; }
.menu-card:hover { transform: translateY(-4px); }
.menu-content { padding: 40px 0; }
.menu-content h3 { margin: 12px 0 6px; font-size: 18px; }
.menu-content p { color: #909399; font-size: 14px; }
.order-header { display: flex; justify-content: space-between; }
</style>
