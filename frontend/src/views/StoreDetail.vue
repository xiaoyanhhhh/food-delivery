<template>
  <div class="page-container">
    <el-button @click="$router.back()" :icon="ArrowLeft" link style="margin-bottom: 16px">返回</el-button>

    <!-- Store Header -->
    <el-card v-if="store" class="store-header-card">
      <div class="store-header">
        <el-image :src="store.logo || 'https://placehold.co/120x120/409eff/white'"
          fit="cover" style="width:100px;height:100px;border-radius:8px" />
        <div class="store-header-info">
          <h1>{{ store.name }}</h1>
          <div class="store-stats">
            <el-rate v-model="store.rating" disabled size="small" show-score :score-template="'{value}'" />
            <span>月售{{ store.monthlySales || 0 }}</span>
          </div>
          <div class="store-details">
            <span>配送费 ¥{{ store.deliveryFee || 3 }}</span>
            <el-divider direction="vertical" />
            <span>起送 ¥{{ store.minOrderAmount || 20 }}</span>
            <el-divider direction="vertical" />
            <span>{{ store.businessHours || '09:00-22:00' }}</span>
          </div>
          <p class="store-announcement" v-if="store.announcement">
            <el-icon :size="16"><Bell /></el-icon> {{ store.announcement }}
          </p>
        </div>
      </div>
    </el-card>

    <!-- Dish Menu -->
    <el-card style="margin-top: 16px">
      <el-tabs v-model="activeCategory" @tab-change="handleCategoryChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane v-for="cat in categories" :key="cat.id"
          :label="cat.name" :name="String(cat.id)" />
      </el-tabs>

      <div v-if="dishes.length > 0" class="dish-list">
        <div v-for="dish in dishes" :key="dish.id" class="dish-item">
          <el-image :src="dish.image || 'https://placehold.co/100x100/409eff/white?text=' + dish.name"
            fit="cover" style="width:100px;height:100px;border-radius:8px" />
          <div class="dish-info">
            <h3>{{ dish.name }}</h3>
            <p class="dish-desc">{{ dish.description || '暂无描述' }}</p>
            <div class="dish-meta">
              <span class="dish-price">¥{{ dish.price }}</span>
              <span class="dish-sales" v-if="dish.monthlySales">月售{{ dish.monthlySales }}</span>
            </div>
          </div>
          <el-button type="primary" size="small" :icon="Plus" circle
            :loading="isAdding(dish.id)" :disabled="isAdding(dish.id)"
            @click="handleAddToCart(dish)" />
        </div>
      </div>
      <el-empty v-else description="暂无菜品" />

      <div class="pagination-wrapper" v-if="totalPages > 1">
        <el-pagination v-model:current-page="currentPage" :page-size="pageSize"
          :total="totalElements" layout="prev, pager, next" @current-change="handlePageChange" />
      </div>
    </el-card>

    <!-- Store Map -->
    <el-card style="margin-top: 16px" v-if="store?.lat && store?.lng">
      <template #header>
        <h3><el-icon :size="18"><Location /></el-icon> 店铺位置</h3>
      </template>
      <OrderMap :store-lat="Number(store.lat)" :store-lng="Number(store.lng)"
        :store-name="store.name" :static="true" :height="300" />
      <p style="margin-top:8px;color:#909399">{{ store.address }}</p>
    </el-card>

    <!-- Reviews Section -->
    <el-card style="margin-top: 16px">
      <template #header>
        <h3>店铺评价 ({{ reviewTotal }})</h3>
      </template>
      <div v-if="reviews.length > 0">
        <div v-for="review in reviews" :key="review.id" class="review-item">
          <div class="review-header">
            <el-rate v-model="review.rating" disabled size="small" />
            <span class="review-user">{{ review.user?.username }}</span>
            <span class="review-time">{{ formatTime(review.createdAt) }}</span>
          </div>
          <div v-if="reviewPurchasedText(review)" class="review-purchased">
            已购 {{ reviewPurchasedText(review) }}
          </div>
          <p class="review-content">{{ review.content }}</p>
          <p class="review-reply" v-if="review.merchantReply">
            <b>商家回复：</b>{{ review.merchantReply }}
          </p>
        </div>
      </div>
      <el-empty v-else description="暂无评价" :image-size="60" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'
import { getStoreDetail } from '../api/store'
import { getDishes } from '../api/dish'
import { getCategories } from '../api/category'
import { getStoreReviews } from '../api/review'
import { ElMessage, ElNotification } from 'element-plus'
import { ArrowLeft, Plus } from '@element-plus/icons-vue'
import OrderMap from '../components/OrderMap.vue'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cartStore = useCartStore()

const store = ref(null)
const dishes = ref([])
const categories = ref([])
const activeCategory = ref('all')
const reviews = ref([])
const reviewTotal = ref(0)
const currentPage = ref(0)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)
const addingDishIds = ref(new Set())

async function fetchStore() {
  try {
    store.value = await getStoreDetail(route.params.id)
  } catch { /* handled */ }
}

async function fetchDishes() {
  try {
    const params = {
      storeId: route.params.id,
      page: currentPage.value,
      size: pageSize.value,
    }
    if (activeCategory.value !== 'all') params.categoryId = activeCategory.value
    const data = await getDishes(params)
    dishes.value = data.content || data
    totalElements.value = data.totalElements || 0
    totalPages.value = data.totalPages || 0
  } catch { /* handled */ }
}

async function fetchCategories() {
  try {
    categories.value = await getCategories({ storeId: route.params.id })
  } catch { /* handled */ }
}

async function fetchReviews() {
  try {
    const data = await getStoreReviews(route.params.id, { page: 0, size: 5 })
    reviews.value = data.content || []
    reviewTotal.value = data.totalElements || 0
  } catch { /* handled */ }
}

function handleCategoryChange() {
  currentPage.value = 0
  fetchDishes()
}

function handlePageChange(page) {
  currentPage.value = page - 1
  fetchDishes()
  window.scrollTo({ top: 400, behavior: 'smooth' })
}

async function handleAddToCart(dish) {
  if (!auth.isLoggedIn()) { router.push('/login'); return }
  if (auth.getRole() !== 'CUSTOMER') { ElMessage.warning('\u4ec5\u987e\u5ba2\u53ef\u52a0\u8d2d'); return }
  try {
    setAdding(dish.id, true)
    await cartStore.add(dish.id)
    ElNotification({
      title: '\u5df2\u52a0\u5165\u8d2d\u7269\u8f66',
      type: 'success',
      message: `${dish.name}\uff0c\u53ef\u5728\u53f3\u4e0a\u89d2\u8d2d\u7269\u8f66\u67e5\u770b`,
      position: 'bottom-left',
      duration: 2600,
      customClass: 'cart-add-notification',
    })
  } catch { /* handled */ }
  finally {
    setAdding(dish.id, false)
  }
}

function isAdding(dishId) {
  return addingDishIds.value.has(dishId)
}

function setAdding(dishId, value) {
  const next = new Set(addingDishIds.value)
  if (value) next.add(dishId)
  else next.delete(dishId)
  addingDishIds.value = next
}

function formatTime(time) {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 16)
}

function reviewPurchasedText(review) {
  const items = review.order?.items || []
  if (items.length === 0) return ''
  return items
    .map(item => `${item.dishName} × ${item.quantity}`)
    .join('，')
}

onMounted(() => {
  fetchStore()
  fetchDishes()
  fetchCategories()
  fetchReviews()
})
</script>

<style scoped>
.store-header-card { margin-bottom: 0; border-top: 3px solid #FF8C00; }
.store-header { display: flex; gap: 20px; }
.store-header-info { flex: 1; }
.store-header-info h1 { font-size: 24px; margin-bottom: 8px; }
.store-stats { display: flex; align-items: center; gap: 16px; margin-bottom: 8px; color: #606266; }
.store-details { display: flex; align-items: center; gap: 4px; font-size: 14px; color: #909399; margin-bottom: 4px; }
.store-announcement { font-size: 13px; color: #e6a23c; }
.dish-list { display: flex; flex-direction: column; gap: 0; }
.dish-item { display: flex; align-items: center; gap: 16px; padding: 16px 0; border-bottom: 1px solid #f0f0f0; }
.dish-item:last-child { border-bottom: none; }
.dish-info { flex: 1; }
.dish-info h3 { font-size: 16px; margin-bottom: 4px; }
.dish-desc { font-size: 13px; color: #999; margin-bottom: 6px; }
.dish-meta { display: flex; align-items: center; gap: 12px; }
.dish-price { font-size: 18px; font-weight: bold; color: #f56c6c; }
.dish-sales { font-size: 12px; color: #909399; }
.review-item { padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.review-item:last-child { border-bottom: none; }
.review-header { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.review-user { font-weight: bold; font-size: 14px; }
.review-time { color: #c0c4cc; font-size: 12px; }
.review-purchased {
  display: inline-flex;
  align-items: center;
  max-width: 100%;
  margin-bottom: 6px;
  padding: 3px 8px;
  border-radius: 4px;
  background: #f5f7fa;
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
}
.review-content { color: #606266; line-height: 1.6; }
.review-reply { background: #f5f7fa; padding: 8px 12px; border-radius: 4px; margin-top: 6px; font-size: 13px; color: #909399; }
.pagination-wrapper { display: flex; justify-content: center; margin-top: 20px; }
</style>
