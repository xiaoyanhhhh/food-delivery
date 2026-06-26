<template>
  <div class="page-container">
    <el-button @click="$router.back()" :icon="ArrowLeft" link style="margin-bottom: 16px">返回</el-button>
    <el-card v-if="dish" style="max-width: 800px; margin: 0 auto">
      <el-row :gutter="24">
        <el-col :span="10">
          <el-image :src="assetUrl(dish.image, 'https://placehold.co/400x300/409eff/white?text=' + dish.name)"
            fit="cover" style="width:100%;border-radius:8px" />
        </el-col>
        <el-col :span="14">
          <h1 class="dish-title">{{ dish.name }}</h1>
          <el-tag v-if="dish.category" style="margin: 8px 0">{{ dish.category.name }}</el-tag>
          <p class="dish-price">¥{{ dish.price }}</p>
          <p class="dish-sales" v-if="dish.monthlySales">月售{{ dish.monthlySales }}份</p>
          <p class="dish-desc">{{ dish.description || '暂无描述' }}</p>

          <!-- Specifications (placeholder for future spec support) -->
          <div v-if="specs.length > 0" style="margin: 16px 0">
            <div v-for="spec in specs" :key="spec.id" style="margin-bottom: 8px">
              <span style="font-size:14px;color:#606266">{{ spec.specGroup }}：</span>
              <el-tag size="small">{{ spec.specName }}</el-tag>
              <span v-if="spec.priceDelta > 0" style="color:#f56c6c;font-size:12px">
                +¥{{ spec.priceDelta }}
              </span>
            </div>
          </div>

          <el-input-number v-model="quantity" :min="1" :max="99" style="margin: 16px 0" />
          <el-button type="primary" size="large" :icon="ShoppingCart" :loading="adding"
            :disabled="adding"
            @click="handleAddToCart" style="display: block">
            加入购物车
          </el-button>
        </el-col>
      </el-row>
    </el-card>
    <el-skeleton v-else :rows="6" animated style="max-width: 800px; margin: 0 auto" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'
import { getDishDetail } from '../api/dish'
import { ElNotification } from 'element-plus'
import { ArrowLeft, ShoppingCart } from '@element-plus/icons-vue'
import { assetUrl } from '../utils/assets'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cartStore = useCartStore()
const dish = ref(null)
const quantity = ref(1)
const specs = ref([])
const adding = ref(false)

async function fetchDish() {
  try {
    dish.value = await getDishDetail(route.params.id)
    // specs would be fetched if available
  } catch { /* handled */ }
}

async function handleAddToCart() {
  if (!auth.isLoggedIn()) { router.push('/login'); return }
  try {
    adding.value = true
    await cartStore.add(dish.value.id, quantity.value)
    ElNotification({
      title: '\u5df2\u52a0\u5165\u8d2d\u7269\u8f66',
      type: 'success',
      message: `${dish.value.name} x ${quantity.value}\uff0c\u53ef\u5728\u53f3\u4e0a\u89d2\u8d2d\u7269\u8f66\u67e5\u770b`,
      position: 'bottom-left',
      duration: 2600,
      customClass: 'cart-add-notification',
    })
  } catch { /* handled */ }
  finally {
    adding.value = false
  }
}

onMounted(fetchDish)
</script>

<style scoped>
.dish-title { font-size: 28px; margin-bottom: 8px; }
.dish-price { font-size: 24px; font-weight: bold; color: #f56c6c; margin: 12px 0; }
.dish-sales { font-size: 13px; color: #909399; margin-bottom: 8px; }
.dish-desc { color: #666; line-height: 1.8; }
</style>
