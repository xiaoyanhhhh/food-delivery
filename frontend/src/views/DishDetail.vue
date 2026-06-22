<template>
  <div class="page-container">
    <el-button @click="$router.back()" :icon="ArrowLeft" link style="margin-bottom: 16px">
      返回
    </el-button>
    <el-card v-if="dish" style="max-width: 800px; margin: 0 auto">
      <el-row :gutter="24">
        <el-col :span="10">
          <el-image :src="dish.image || 'https://placehold.co/400x300/409eff/white?text=' + dish.name"
            fit="cover" style="width:100%;border-radius:8px" />
        </el-col>
        <el-col :span="14">
          <h1 class="dish-title">{{ dish.name }}</h1>
          <el-tag v-if="dish.category" style="margin: 8px 0">{{ dish.category.name }}</el-tag>
          <p class="dish-price">¥{{ dish.price }}</p>
          <p class="dish-desc">{{ dish.description || '暂无描述' }}</p>
          <el-input-number v-model="quantity" :min="1" :max="99" style="margin: 16px 0" />
          <el-button type="primary" size="large" :icon="ShoppingCart"
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
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cartStore = useCartStore()
const dish = ref(null)
const quantity = ref(1)

async function fetchDish() {
  try {
    dish.value = await getDishDetail(route.params.id)
  } catch { /* handled */ }
}

async function handleAddToCart() {
  if (!auth.isLoggedIn()) {
    router.push('/login')
    return
  }
  try {
    await cartStore.add(dish.value.id, quantity.value)
    ElMessage.success(`已添加 ${quantity.value} 份 ${dish.value.name}`)
  } catch { /* handled */ }
}

onMounted(fetchDish)
</script>

<style scoped>
.dish-title {
  font-size: 28px;
  margin-bottom: 8px;
}
.dish-price {
  font-size: 24px;
  font-weight: bold;
  color: #f56c6c;
  margin: 12px 0;
}
.dish-desc {
  color: #666;
  line-height: 1.8;
}
</style>
