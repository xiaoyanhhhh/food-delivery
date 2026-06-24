<template>
  <div class="page-container">
    <h1 class="page-title">📊 销售统计</h1>
    <el-row :gutter="20" style="margin-bottom:20px">
      <el-col :span="6"><el-card class="stat-card"><div class="stat-value">{{ stats.todayOrders }}</div><div class="stat-label">今日订单</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="stat-value">¥{{ stats.todayRevenue }}</div><div class="stat-label">今日收入</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="stat-value">{{ stats.totalOrders }}</div><div class="stat-label">累计订单</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="stat-value">{{ stats.avgRating }}</div><div class="stat-label">店铺评分</div></el-card></el-col>
    </el-row>

    <el-row :gutter="20">
      <el-col :span="12">
        <el-card><template #header><h3>📈 7日收入趋势</h3></template>
          <Line v-if="trendReady" :data="trendData" :options="chartOpts" />
          <el-empty v-else description="暂无数据" :image-size="80" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card><template #header><h3>🔥 菜品销量 Top 5</h3></template>
          <Bar v-if="topReady" :data="topData" :options="chartOpts" />
          <el-empty v-else description="暂无数据" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top:20px">
      <el-col :span="12">
        <el-card><template #header><h3>📊 订单状态分布</h3></template>
          <Pie v-if="pieReady" :data="pieData" :options="chartOpts" />
          <el-empty v-else description="暂无数据" :image-size="80" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card><template #header><h3>📋 状态汇总</h3></template>
          <div class="status-summary">
            <div v-for="s in statusSummary" :key="s.label" class="status-item">
              <span>{{ s.label }}</span>
              <el-tag :type="s.type" size="large">{{ s.count }}</el-tag>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMyStore } from '../../api/store'
import { getMerchantOrders } from '../../api/order'
import { Line, Bar, Pie } from 'vue-chartjs'
import { Chart as ChartJS, Title, Tooltip, Legend, PointElement, LineElement, CategoryScale, LinearScale, BarElement, ArcElement } from 'chart.js'

ChartJS.register(Title, Tooltip, Legend, PointElement, LineElement, CategoryScale, LinearScale, BarElement, ArcElement)

const stats = reactive({ todayOrders: 0, todayRevenue: '0.00', totalOrders: 0, avgRating: '5.0' })
const trendReady = ref(false), topReady = ref(false), pieReady = ref(false)
const chartOpts = { responsive: true, maintainAspectRatio: false }

const trendData = ref({ labels: [], datasets: [{ label: '收入(元)', data: [], borderColor: '#409eff', backgroundColor: 'rgba(64,158,255,0.1)', fill: true }] })
const topData = ref({ labels: [], datasets: [{ label: '销量', data: [], backgroundColor: ['#f56c6c','#e6a23c','#409eff','#67c23a','#909399'] }] })
const pieData = ref({ labels: [], datasets: [{ data: [], backgroundColor: ['#f56c6c','#e6a23c','#409eff','#67c23a','#909399','#ff9800','#9c27b0'] }] })
const statusSummary = ref([{ label: '待接单', type: 'warning', count: 0 }, { label: '制作中', type: '', count: 0 }, { label: '待取餐', type: 'info', count: 0 }, { label: '已完成', type: 'success', count: 0 }])

async function fetchData() {
  try {
    const store = await getMyStore()
    stats.avgRating = store.rating || '5.0'
    const data = await getMerchantOrders({ page: 0, size: 200 })
    const orders = data.content || []
    const today = new Date().toISOString().substring(0, 10)
    let todayO = 0, todayR = 0

    // Trend: last 7 days
    const days = []
    for (let i = 6; i >= 0; i--) {
      const d = new Date(); d.setDate(d.getDate() - i)
      days.push(d.toISOString().substring(0, 10))
    }
    const dayRevenue = days.map(d => orders.filter(o => o.createdAt?.substring(0,10) === d && o.status !== 'CANCELLED').reduce((s,o) => s + Number(o.totalPrice||0), 0))
    trendData.value = { labels: days.map(d => d.substring(5)), datasets: [{ label: '收入(元)', data: dayRevenue, borderColor: '#409eff', backgroundColor: 'rgba(64,158,255,0.1)', fill: true }] }
    trendReady.value = true

    // Top dishes
    const dishMap = new Map()
    orders.forEach(o => { if (o.items) o.items.forEach(i => { const k = i.dishName; dishMap.set(k, (dishMap.get(k)||0) + i.quantity) }) })
    const top = [...dishMap.entries()].sort((a,b) => b[1]-a[1]).slice(0,5)
    topData.value = { labels: top.map(t => t[0]), datasets: [{ label: '销量', data: top.map(t => t[1]), backgroundColor: ['#f56c6c','#e6a23c','#409eff','#67c23a','#909399'] }] }
    topReady.value = true

    // Status pie
    const statusCount = { PENDING_ACCEPT: 0, PREPARING: 0, READY: 0, DELIVERING: 0, COMPLETED: 0, CANCELLED: 0 }
    orders.forEach(o => { if (statusCount.hasOwnProperty(o.status)) statusCount[o.status]++; else statusCount[o.status] = 1 })
    const sl = ['待接单','制作中','待取餐','配送中','已完成','已取消']
    const sc = Object.values(statusCount)
    pieData.value = { labels: sl, datasets: [{ data: sc, backgroundColor: ['#f56c6c','#e6a23c','#409eff','#67c23a','#909399','#ff9800'] }] }
    pieReady.value = true

    // Status summary
    statusSummary.value[0].count = (statusCount.PENDING_ACCEPT || 0) + (statusCount.READY || 0)
    statusSummary.value[1].count = statusCount.PREPARING || 0
    statusSummary.value[2].count = statusCount.READY || 0
    statusSummary.value[3].count = statusCount.COMPLETED || 0

    // Today
    orders.forEach(o => { if (o.createdAt?.substring(0,10) === today && o.status !== 'CANCELLED') { todayO++; todayR += Number(o.totalPrice||0) } })
    stats.todayOrders = todayO; stats.todayRevenue = todayR.toFixed(2); stats.totalOrders = orders.length
  } catch { /* handled */ }
}

onMounted(fetchData)
</script>

<style scoped>
.stat-card { text-align: center; padding: 10px 0; }
.stat-value { font-size: 28px; font-weight: bold; color: #409eff; }
.stat-label { font-size: 14px; color: #909399; margin-top: 4px; }
.status-summary { display: flex; gap: 24px; justify-content: center; }
.status-item { display: flex; flex-direction: column; align-items: center; gap: 6px; }
</style>
