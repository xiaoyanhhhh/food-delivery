<template>
  <div class="page-container">
    <h1 class="page-title">🛒 我的购物车</h1>
    <el-empty v-if="cartStore.items.length === 0" description="购物车是空的">
      <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
    </el-empty>
    <template v-else>
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
          <span>合计：<span class="total-price">¥{{ cartStore.totalPrice().toFixed(2) }}</span></span>
          <el-button type="primary" size="large" @click="showOrderDialog = true">
            去结算
          </el-button>
        </div>
      </el-card>
    </template>

    <!-- Place order dialog -->
    <el-dialog v-model="showOrderDialog" title="确认订单" width="480px">
      <el-form :model="orderForm" label-width="80px">
        <el-form-item label="配送地址">
          <el-input v-model="orderForm.deliveryAddress" placeholder="请输入配送地址" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="orderForm.note" type="textarea" placeholder="选填" />
        </el-form-item>
      </el-form>
      <div style="margin: 12px 0">
        <p v-for="item in cartStore.items" :key="item.id">
          {{ item.dish.name }} × {{ item.quantity }} — ¥{{ (item.dish.price * item.quantity).toFixed(2) }}
        </p>
        <el-divider />
        <p style="font-weight:bold;font-size:16px">应付：¥{{ cartStore.totalPrice().toFixed(2) }}</p>
      </div>
      <template #footer>
        <el-button @click="showOrderDialog = false">取消</el-button>
        <el-button type="primary" @click="handleOrder" :loading="ordering">确认下单</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useCartStore } from '../stores/cart'
import { createOrder } from '../api/order'
import { ElMessage } from 'element-plus'

const router = useRouter()
const cartStore = useCartStore()
const showOrderDialog = ref(false)
const ordering = ref(false)

const orderForm = reactive({
  deliveryAddress: '',
  note: '',
})

async function handleUpdate(id, quantity) {
  try {
    await cartStore.update(id, quantity)
  } catch { /* handled */ }
}

async function handleRemove(id) {
  try {
    await cartStore.remove(id)
  } catch { /* handled */ }
}

async function handleOrder() {
  if (!orderForm.deliveryAddress) {
    ElMessage.warning('请输入配送地址')
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
      deliveryAddress: orderForm.deliveryAddress,
      note: orderForm.note,
    })
    ElMessage.success('下单成功！')
    showOrderDialog.value = false
    await cartStore.clear()
    router.push('/my-orders')
  } catch { /* handled */ }
  ordering.value = false
}
</script>

<style scoped>
.cart-item {
  display: flex;
  align-items: center;
  gap: 16px;
}
.item-info {
  flex: 1;
}
.item-info h3 {
  font-size: 16px;
  margin-bottom: 4px;
}
.item-price {
  color: #f56c6c;
}
.item-subtotal {
  font-weight: bold;
  font-size: 16px;
  min-width: 80px;
  text-align: center;
}
.cart-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.total-price {
  font-size: 24px;
  font-weight: bold;
  color: #f56c6c;
}
</style>
