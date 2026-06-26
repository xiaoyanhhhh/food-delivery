<template>
  <div class="page-container cart-page">
    <div class="cart-head">
      <h1>
        <el-icon :size="24"><ShoppingCart /></el-icon>
        我的购物车
      </h1>
      <p v-if="cartStore.items.length">已选 {{ itemCount }} 件，确认信息后即可下单</p>
    </div>

    <el-empty v-if="cartStore.items.length === 0" description="购物车是空的">
      <el-button type="primary" @click="$router.push('/')">去点餐</el-button>
    </el-empty>

    <template v-else>
      <section class="cart-list">
        <article v-for="item in cartStore.items" :key="item.id" class="cart-item">
          <el-image
            class="item-image"
            :src="assetUrl(item.dish.image, 'https://placehold.co/96x96/409eff/white')"
            fit="cover"
          />
          <div class="item-info">
            <h3>{{ item.dish.name }}</h3>
            <p>单价 ¥{{ money(item.dish.price) }}</p>
          </div>
          <el-input-number
            v-model="item.quantity"
            :min="1"
            :max="99"
            size="small"
            @change="(val) => handleUpdate(item.id, val)"
          />
          <strong class="item-subtotal">¥{{ money(item.dish.price * item.quantity) }}</strong>
          <el-button :icon="Delete" circle type="danger" plain size="small" @click="handleRemove(item.id)" />
        </article>
      </section>

      <section class="cart-total-card">
        <div>
          <span>商品小计</span>
          <strong>¥{{ money(subtotal) }}</strong>
        </div>
        <div>
          <span>预估配送费</span>
          <strong>¥{{ deliveryFee }}</strong>
        </div>
        <div class="payable">
          <span>应付</span>
          <strong>¥{{ money(payable) }}</strong>
        </div>
        <el-button type="primary" size="large" @click="openOrderDialog">去结算</el-button>
      </section>
    </template>

    <el-dialog
      v-model="showOrderDialog"
      title="确认订单"
      width="760px"
      class="checkout-dialog"
      :close-on-click-modal="false"
    >
      <div class="checkout-sheet">
        <section class="delivery-card">
          <div class="delivery-tabs">
            <button class="active">外卖配送</button>
            <button type="button">到店自取</button>
          </div>
          <div class="address-row">
            <div>
              <span class="section-kicker">选择您的收货地址</span>
              <el-input
                v-model="orderForm.deliveryAddress"
                placeholder="请输入收货地址，例如：宿舍楼/教学楼/校区地址"
                @blur="fetchDeliveryEstimate"
              />
            </div>
            <el-select
              v-model="selectedAddressId"
              placeholder="从地址簿选择"
              @change="onAddressSelect"
              clearable
              filterable
              :loading="addressLoading"
            >
              <el-option v-for="addr in savedAddresses" :key="addr.id" :label="formatAddressOption(addr)" :value="addr.id" />
            </el-select>
          </div>
          <div class="time-options">
            <div class="time-option active">
              <span>标准配送</span>
              <strong>{{ estimatedMinutes || 30 }} 分钟内送达</strong>
            </div>
            <div class="time-option">
              <span>预约配送</span>
              <strong>稍后可选</strong>
            </div>
          </div>
        </section>

        <section class="checkout-items">
          <div class="store-line">本次点餐</div>
          <article v-for="item in cartStore.items" :key="item.id" class="checkout-item">
            <el-image
              class="checkout-image"
              :src="assetUrl(item.dish.image, 'https://placehold.co/58x58/409eff/white')"
              fit="cover"
            />
            <div>
              <h4>{{ item.dish.name }}</h4>
              <p>x{{ item.quantity }}</p>
            </div>
            <strong>¥{{ money(item.dish.price * item.quantity) }}</strong>
          </article>
        </section>

        <section class="fee-card">
          <div><span>商品原价</span><strong>¥{{ money(subtotal) }}</strong></div>
          <div><span>用户配送费</span><strong>¥{{ deliveryFee }}</strong></div>
          <div><span>美味红包</span><em>暂无可用红包</em></div>
          <div><span>商家优惠</span><strong class="discount">- ¥0.00</strong></div>
        </section>

        <section class="note-card">
          <span>备注</span>
          <el-input v-model="orderForm.note" type="textarea" :rows="2" placeholder="口味要求、配送时间等（选填）" />
        </section>
      </div>

      <template #footer>
        <div class="checkout-footer">
          <div>
            <span>券前</span>
            <strong>¥{{ money(payable) }}</strong>
          </div>
          <el-button type="primary" size="large" :loading="ordering" @click="handleOrder">立即支付</el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { createOrder, getDeliveryEstimate } from '../api/order'
import { getAddresses } from '../api/address'
import { ElMessage } from 'element-plus'
import { Delete, ShoppingCart } from '@element-plus/icons-vue'
import { assetUrl } from '../utils/assets'

const router = useRouter()
const cartStore = useCartStore()
const showOrderDialog = ref(false)
const ordering = ref(false)
const deliveryFee = ref('3.00')
const deliveryDistance = ref(null)
const estimatedMinutes = ref(30)
const savedAddresses = ref([])
const selectedAddressId = ref(null)
const addressLoading = ref(false)
let estimateTimer = null

const orderForm = reactive({
  deliveryAddress: '',
  note: '',
})

const subtotal = computed(() => cartStore.totalPrice())
const payable = computed(() => subtotal.value + Number(deliveryFee.value || 0))
const itemCount = computed(() => cartStore.items.reduce((sum, item) => sum + Number(item.quantity || 0), 0))

function money(value) {
  return Number(value || 0).toFixed(2)
}

async function handleUpdate(id, quantity) {
  try { await cartStore.update(id, quantity) } catch (e) { ElMessage.error(e.message || '更新失败') }
}

async function handleRemove(id) {
  try { await cartStore.remove(id) } catch (e) { ElMessage.error(e.message || '删除失败') }
}

function openOrderDialog() {
  showOrderDialog.value = true
  fetchSavedAddresses()
  fetchDeliveryEstimate()
}

async function fetchDeliveryEstimate() {
  try {
    const info = await getDeliveryEstimate(1, orderForm.deliveryAddress || '')
    deliveryFee.value = Number(info.deliveryFee || 3).toFixed(2)
    deliveryDistance.value = info.distance
    estimatedMinutes.value = info.estimatedMinutes
  } catch { /* delivery estimate should not block order creation */ }
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
      fetchDeliveryEstimate()
    }
  } catch { /* address loading failure should not block manual input */ }
  addressLoading.value = false
}

function onAddressSelect(addrId) {
  const addr = savedAddresses.value.find(a => a.id === addrId)
  if (addr) {
    orderForm.deliveryAddress = addr.detail
    fetchDeliveryEstimate()
  }
}

function formatAddressOption(addr) {
  return [addr.label, addr.contactName, addr.contactPhone, addr.detail]
    .filter(Boolean)
    .join(' - ')
}

async function handleOrder() {
  if (!orderForm.deliveryAddress || !orderForm.deliveryAddress.trim()) {
    ElMessage.warning('请填写配送地址')
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
    ElMessage.success('下单成功，请尽快支付')
    showOrderDialog.value = false
    await cartStore.clear()
    router.push('/my-orders')
  } catch {
    // request interceptor shows the error
  }
  ordering.value = false
}

watch(() => orderForm.deliveryAddress, () => {
  clearTimeout(estimateTimer)
  estimateTimer = setTimeout(fetchDeliveryEstimate, 400)
})

onMounted(() => {
  cartStore.fetchCart()
  fetchDeliveryEstimate()
  fetchSavedAddresses()
})
</script>

<style scoped>
.cart-page { padding-bottom: 40px; }
.cart-head { display: flex; align-items: baseline; justify-content: space-between; margin-bottom: 18px; }
.cart-head h1 { display: inline-flex; align-items: center; gap: 8px; margin: 0; font-size: 28px; }
.cart-head p { color: #909399; }
.cart-list { display: grid; gap: 12px; }
.cart-item { display: grid; grid-template-columns: 96px 1fr auto 108px auto; align-items: center; gap: 16px; padding: 18px; border-radius: 16px; background: #fff; box-shadow: 0 8px 24px rgba(31, 35, 41, 0.06); }
.item-image { width: 96px; height: 96px; border-radius: 12px; background: #f2f4f7; }
.item-info h3 { margin: 0 0 6px; font-size: 18px; }
.item-info p { margin: 0; color: #909399; }
.item-subtotal { color: #1f2329; font-size: 18px; text-align: right; }
.cart-total-card { position: sticky; bottom: 18px; z-index: 15; display: grid; grid-template-columns: 1fr 1fr 1.2fr auto; align-items: center; gap: 16px; margin-top: 18px; padding: 16px 18px; border-radius: 18px; background: #1f2329; color: #fff; box-shadow: 0 16px 40px rgba(31, 35, 41, 0.22); }
.cart-total-card div { display: flex; flex-direction: column; gap: 2px; }
.cart-total-card span { color: #c7ccd6; font-size: 13px; }
.cart-total-card strong { font-size: 18px; }
.cart-total-card .payable strong { color: #ffdd3b; font-size: 28px; }
.checkout-sheet { display: grid; gap: 16px; }
.delivery-card, .checkout-items, .fee-card, .note-card { padding: 18px; border-radius: 16px; background: #fff; border: 1px solid #edf0f4; }
.delivery-tabs { display: grid; grid-template-columns: 1fr 1fr; margin: -18px -18px 18px; overflow: hidden; border-radius: 16px 16px 0 0; background: #f2f3f5; }
.delivery-tabs button { min-height: 58px; border: 0; background: transparent; color: #73777f; font-size: 18px; font-weight: 800; }
.delivery-tabs button.active { background: #fff; color: #1f2329; }
.address-row { display: grid; grid-template-columns: 1fr 260px; gap: 12px; align-items: end; }
.section-kicker { display: block; margin-bottom: 8px; color: #ff7a00; font-size: 22px; font-weight: 900; }
.time-options { display: grid; grid-template-columns: 1fr 1fr; gap: 12px; margin-top: 14px; }
.time-option { padding: 14px; border: 1px solid #e5e7eb; border-radius: 12px; }
.time-option.active { border-color: #ffe24a; background: #fffdf0; box-shadow: inset 0 0 0 2px #ffe24a; }
.time-option span { display: block; color: #606266; margin-bottom: 6px; }
.time-option strong { font-size: 20px; }
.store-line { margin-bottom: 12px; color: #606266; font-weight: 700; }
.checkout-item { display: grid; grid-template-columns: 58px 1fr auto; gap: 12px; align-items: center; padding: 10px 0; }
.checkout-item + .checkout-item { border-top: 1px solid #f0f1f3; }
.checkout-image { width: 58px; height: 58px; border-radius: 10px; background: #f2f4f7; }
.checkout-item h4 { margin: 0 0 4px; font-size: 16px; }
.checkout-item p { margin: 0; color: #909399; }
.fee-card { display: grid; gap: 12px; }
.fee-card div { display: flex; justify-content: space-between; align-items: center; }
.fee-card span { color: #606266; }
.fee-card strong { font-size: 18px; }
.fee-card em { color: #ff4d4f; font-style: normal; }
.discount { color: #ff4d4f; }
.note-card { display: grid; grid-template-columns: 80px 1fr; gap: 12px; align-items: start; }
.note-card span { padding-top: 8px; color: #606266; font-weight: 700; }
.checkout-footer { display: flex; justify-content: space-between; align-items: center; width: 100%; }
.checkout-footer div { display: flex; align-items: baseline; gap: 8px; color: #909399; }
.checkout-footer strong { color: #ff4d4f; font-size: 30px; }
.checkout-footer :deep(.el-button) { min-width: 180px; border-radius: 999px; background: #ffe24a; border-color: #ffe24a; color: #1f2329; font-weight: 900; }
@media (max-width: 760px) {
  .cart-head { display: block; }
  .cart-item { grid-template-columns: 76px 1fr auto; }
  .item-image { width: 76px; height: 76px; }
  .item-subtotal { grid-column: 2; text-align: left; }
  .cart-total-card { grid-template-columns: 1fr auto; }
  .cart-total-card div:not(.payable) { display: none; }
  .address-row, .time-options { grid-template-columns: 1fr; }
  .note-card { grid-template-columns: 1fr; }
}
</style>
