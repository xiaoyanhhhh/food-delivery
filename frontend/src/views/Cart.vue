<template>
  <div class="page-container">
    <h1 class="page-title">
      <el-icon :size="22"><ShoppingCart /></el-icon> 我的购物车
    </h1>
    <el-empty v-if="cartStore.items.length === 0" description="购物车是空的">
      <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
    </el-empty>
    <template v-else>
      <!-- Store Info -->
      <el-alert v-if="cartStore.items[0]?.dish?.merchant" type="info" :closable="false" style="margin-bottom: 16px">
        <template #title>
          商家：{{ cartStore.items[0].dish.merchant?.username || '未知' }}
        </template>
      </el-alert>

      <el-card v-for="item in cartStore.items" :key="item.id" style="margin-bottom: 12px">
        <div class="cart-item">
          <el-image :src="item.dish.image || 'https://placehold.co/80x80/409eff/white'"
            fit="cover" style="width:80px;height:80px;border-radius:8px" />
          <div class="item-info">
            <h3>{{ item.dish.name }}</h3>
            <p class="item-price">¥{{ item.dish.price }}</p>
          </div>
          <el-input-number v-model="item.quantity" :min="1" :max="99" size="small"
            @change="(val) => handleUpdate(item.id, val)" />
          <span class="item-subtotal">¥{{ (item.dish.price * item.quantity).toFixed(2) }}</span>
          <el-button :icon="Delete" circle type="danger" plain size="small"
            @click="handleRemove(item.id)" />
        </div>
      </el-card>

      <el-card style="margin-top: 16px">
        <div class="cart-footer">
          <span>合计：<span class="total-price">¥{{ (cartStore.totalPrice() + Number(deliveryFee)).toFixed(2) }}</span></span>
          <el-button type="primary" size="large" @click="openOrderDialog">
            去结算
          </el-button>
        </div>
      </el-card>
    </template>

    <!-- Place order dialog -->
    <el-dialog v-model="showOrderDialog" title="确认订单" width="500px">
      <el-form :model="orderForm" label-width="80px">
        <el-form-item label="配送地址" required>
          <el-input v-model="orderForm.deliveryAddress" placeholder="请输入收货地址，例如：北京市朝阳区望京SOHO T1 3层" />
        </el-form-item>
        <el-form-item label="快速选择">
          <el-select v-model="selectedAddressId" placeholder="从地址簿选择..."
            @change="onAddressSelect" clearable filterable :loading="addressLoading"
            :empty-text="addressLoading ? 'Loading...' : 'No data'" style="width:100%">
            <el-option v-for="addr in savedAddresses" :key="addr.id"
              :label="formatAddressOption(addr)" :value="addr.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderForm.note" type="textarea" placeholder="口味要求、配送时间等（选填）" :rows="2" />
        </el-form-item>
      </el-form>
      <div style="margin: 12px 0; background: #f5f7fa; padding: 12px; border-radius: 8px">
        <p v-for="item in cartStore.items" :key="item.id" style="line-height: 2">
          {{ item.dish.name }} × {{ item.quantity }} — ¥{{ (item.dish.price * item.quantity).toFixed(2) }}
        </p>
        <el-divider style="margin: 8px 0" />
        <p>商品小计：¥{{ cartStore.totalPrice().toFixed(2) }}</p>
        <p>配送费：¥{{ deliveryFee }}</p>
        <p style="font-weight:bold;font-size:18px;margin-top:4px">
          应付：<span style="color:#f56c6c">¥{{ (cartStore.totalPrice() + Number(deliveryFee)).toFixed(2) }}</span>
        </p>
      </div>
      <template #footer>
        <el-button @click="showOrderDialog = false">取消</el-button>
        <el-button type="primary" @click="handleOrder" :loading="ordering">确认下单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { createOrder, getDeliveryEstimate } from '../api/order'
import { getAddresses } from '../api/address'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'

const router = useRouter()
const cartStore = useCartStore()
const showOrderDialog = ref(false)
const ordering = ref(false)
const estimating = ref(false)
const deliveryFee = ref('3.00')
const deliveryDistance = ref(null)
const estimatedMinutes = ref(30)
const savedAddresses = ref([])
const selectedAddressId = ref(null)
const addressLoading = ref(false)

const orderForm = reactive({
  deliveryAddress: '',
  note: '',
})

async function handleUpdate(id, quantity) {
  try { await cartStore.update(id, quantity) } catch (e) { ElMessage.error(e.message || '更新失败') }
}

async function handleRemove(id) {
  try { await cartStore.remove(id) } catch (e) { ElMessage.error(e.message || '删除失败') }
}

function openOrderDialog() {
  showOrderDialog.value = true
  fetchSavedAddresses()
}

async function fetchDeliveryEstimate() {
  try {
    const info = await getDeliveryEstimate(1)
    deliveryFee.value = Number(info.deliveryFee || 3).toFixed(2)
    deliveryDistance.value = info.distance
    estimatedMinutes.value = info.estimatedMinutes
  } catch { /* 配送预估不影响下单 */ }
}

async function fetchSavedAddresses() {
  addressLoading.value = true
  try {
    const data = await getAddresses()
    savedAddresses.value = Array.isArray(data) ? data : (data?.content || [])
    const defaultAddr = savedAddresses.value.find(a => a.isDefault)
    if (defaultAddr && !orderForm.deliveryAddress) {
      orderForm.deliveryAddress = defaultAddr.detail
      selectedAddressId.value = defaultAddr.id
    }
  } catch { /* address loading failure should not block manual input */ }
  addressLoading.value = false
}

function onAddressSelect(addrId) {
  const addr = savedAddresses.value.find(a => a.id === addrId)
  if (addr) {
    orderForm.deliveryAddress = addr.detail
  }
}

function formatAddressOption(addr) {
  return [addr.label, addr.contactName, addr.contactPhone, addr.detail]
    .filter(Boolean)
    .join(' - ')
}

async function handleOrder() {
  if (!orderForm.deliveryAddress || !orderForm.deliveryAddress.trim()) {
    ElMessage.warning('请填写配送地址（任意文字即可，如：北京市朝阳区XX路XX号）')
    return
  }
  ordering.value = true
  try {
    const items = cartStore.items.map(i => ({
      dishId: i.dish.id,
      quantity: i.quantity,
    }))
    await createOrder({
      items,
      deliveryAddress: orderForm.deliveryAddress.trim(),
      note: orderForm.note || '',
    })
    ElMessage.success('下单成功！请尽快支付')
    showOrderDialog.value = false
    await cartStore.clear()
    router.push('/my-orders')
  } catch (e) {
    // request 拦截器已经显示了错误消息
  }
  ordering.value = false
}

onMounted(() => {
  fetchDeliveryEstimate()
  fetchSavedAddresses()
})
</script>

<style scoped>
.cart-item { display: flex; align-items: center; gap: 16px; }
.item-info { flex: 1; }
.item-info h3 { font-size: 16px; margin-bottom: 4px; }
.item-price { color: #f56c6c; font-weight: bold; }
.item-subtotal { font-weight: bold; font-size: 16px; min-width: 80px; text-align: center; }
.cart-footer { display: flex; justify-content: space-between; align-items: center; }
.total-price { font-size: 24px; font-weight: bold; color: #f56c6c; }
</style>
