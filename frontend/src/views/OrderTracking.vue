<template>
  <div class="page-container">
    <div style="max-width: 800px; margin: 0 auto">
      <el-button @click="$router.back()" link style="margin-bottom: 16px">
        <el-icon :size="16"><ArrowLeft /></el-icon> 返回
      </el-button>

      <el-card v-if="order">
        <template #header>
          <div class="card-header">
            <span>订单号：{{ order.orderNo }}</span>
            <el-tag :type="statusType(order.status)" size="large">{{ statusLabel(order.status) }}</el-tag>
          </div>
        </template>

        <!-- Steps -->
        <el-steps :active="activeStep" align-center style="margin: 24px 0">
          <el-step title="已下单" />
          <el-step title="已支付" />
          <el-step title="制作中" />
          <el-step title="配送中" />
          <el-step title="已送达" />
        </el-steps>

        <!-- ETA -->
        <div v-if="['DELIVERING','PICKED_UP'].includes(order.status)" class="eta">
          <span v-if="order.estimatedDeliveryTime">
            <el-icon :size="16"><Van /></el-icon> 预计 {{ order.estimatedDeliveryTime }} 分钟内送达
          </span>
          <span v-if="order.rider"> | 骑手：{{ order.rider.username }}</span>
        </div>

        <!-- Map (only when delivering) -->
        <div v-if="['DELIVERING','PICKED_UP'].includes(order.status)" style="margin: 16px 0">
          <OrderMap
            :store-lat="storeLat" :store-lng="storeLng" :store-name="order.merchant?.username"
            :rider-lat="riderLat" :rider-lng="riderLng" :rider-name="order.rider?.username"
            :customer-lat="order.customerLat || 39.9542"
            :customer-lng="order.customerLng || 116.4074"
            :static="false" :height="350"
          />
        </div>

        <el-divider />
        <div v-for="item in order.items" :key="item.id" class="order-item">
          <span>{{ item.dishName }} × {{ item.quantity }}</span>
          <span>¥{{ (item.dishPrice * item.quantity).toFixed(2) }}</span>
        </div>
        <el-divider />
        <div class="price-breakdown">
          <div class="row"><span>商品小计</span><span>¥{{ order.totalPrice }}</span></div>
          <div class="row" v-if="order.deliveryFee"><span>配送费</span><span>¥{{ order.deliveryFee }}</span></div>
          <div class="row total"><span>合计</span><span class="red">¥{{ total() }}</span></div>
        </div>
        <el-divider />
        <p><b>配送地址：</b>{{ order.deliveryAddress }}</p>
        <p v-if="order.note"><b>备注：</b>{{ order.note }}</p>

        <div class="actions" v-if="actions.length">
          <el-button v-for="a in actions" :key="a.label" :type="a.type" @click="a.handler">{{ a.label }}</el-button>
        </div>
      </el-card>
      <el-skeleton v-else :rows="10" animated />
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getOrderDetail, payOrder } from '../api/order'
import request from '../api/request'
import { ElMessage } from 'element-plus'
import OrderMap from '../components/OrderMap.vue'

const route = useRoute()
const router = useRouter()
const order = ref(null)
const riderLat = ref(39.9087)
const riderLng = ref(116.4605)
const storeLat = ref(39.9087)
const storeLng = ref(116.4605)
let pollTimer = null

const statusMap = {
  PENDING_PAYMENT:'待支付',PAID:'已支付',PENDING_ACCEPT:'待接单',
  PREPARING:'制作中',READY:'已备好',PICKED_UP:'已取餐',
  DELIVERING:'配送中',COMPLETED:'已完成',CANCELLED:'已取消',
}
function statusLabel(s){return statusMap[s]||s}
function statusType(s){
  if(s==='COMPLETED')return'success';if(s==='CANCELLED')return'info'
  if(s==='DELIVERING'||s==='PICKED_UP')return'warning';return''
}
const statusStepMap = {
  PENDING_PAYMENT: 0,
  PAID: 1,
  PENDING_ACCEPT: 1,
  PREPARING: 2,
  READY: 2,
  PENDING_PICKUP: 2,
  PICKED_UP: 3,
  DELIVERING: 3,
  COMPLETED: 4,
}
const activeStep = computed(() => {
  if(!order.value)return 0
  return statusStepMap[order.value.status] ?? 0
})
function total(){
  if(!order.value)return'0.00'
  return(Number(order.value.totalPrice||0)+Number(order.value.deliveryFee||0)).toFixed(2)
}

const actions = computed(()=>{
  if(!order.value)return[]
  const s=order.value.status
  const list=[]
  if(s==='PENDING_PAYMENT')list.push({label:'去支付',type:'primary',handler:handlePay})
  return list
})

async function fetchOrder(){
  try{
    order.value = await getOrderDetail(route.params.id)
    if(order.value.storeLat) storeLat.value = order.value.storeLat
    if(order.value.storeLng) storeLng.value = order.value.storeLng
  }catch{/* handled */}
}

async function pollRiderLocation(){
  if(!order.value?.rider)return
  try{
    const loc = await request.get('/rider/location',{params:{riderId:order.value.rider.id}})
    if(loc){riderLat.value=Number(loc.lat);riderLng.value=Number(loc.lng)}
  }catch{/* */}
}

async function handlePay(){
  try{await payOrder(order.value.id);ElMessage.success('支付成功');fetchOrder()}catch{/* */}
}

// Start polling when rider is active
import { onMounted } from 'vue'
onMounted(()=>{
  fetchOrder()
  pollTimer = setInterval(()=>{fetchOrder();pollRiderLocation()},5000)
})
onBeforeUnmount(()=>{if(pollTimer)clearInterval(pollTimer)})
</script>

<style scoped>
.card-header{display:flex;justify-content:space-between;align-items:center}
.eta{background:#FFF3E0;padding:12px;border-radius:8px;color:#FF8C00;font-weight:bold}
.order-item{display:flex;justify-content:space-between;padding:4px 0}
.row{display:flex;justify-content:space-between;padding:4px 0}
.total{font-weight:bold;font-size:16px}
.red{color:#f56c6c;font-size:20px}
.actions{margin-top:16px;text-align:center}
</style>
