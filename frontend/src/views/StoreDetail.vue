<template>
  <div class="page-container store-order-page">
    <div class="order-topbar">
      <el-button class="icon-link" @click="$router.back()" :icon="ArrowLeft" circle />
      <el-segmented v-model="orderMode" :options="orderModeOptions" class="mode-switch" />
      <div class="topbar-spacer" />
      <el-input v-model="keyword" class="dish-search" :prefix-icon="Search" clearable placeholder="搜索本店菜品" />
      <el-button class="soft-action" :icon="Star" circle />
    </div>

    <section v-if="store" class="store-hero">
      <el-image
        class="store-cover"
        :src="assetUrl(store.logo, 'https://placehold.co/220x160/409eff/white?text=' + store.name)"
        fit="cover"
      />
      <div class="store-copy">
        <h1>{{ store.name }}</h1>
        <div class="store-stats">
          <el-rate v-model="store.rating" disabled size="small" show-score :score-template="'{value}'" />
          <span>月售{{ store.monthlySales || 0 }}</span>
        </div>
        <div class="store-details">
          <span>配送费 ¥{{ store.deliveryFee || 3 }}</span>
          <span>起送 ¥{{ store.minOrderAmount || 20 }}</span>
          <span>{{ store.businessHours || '09:00-22:00' }}</span>
        </div>
        <p class="store-announcement" v-if="store.announcement">
          <el-icon :size="16"><Bell /></el-icon> {{ store.announcement }}
        </p>
      </div>
    </section>

    <nav class="order-tabs">
      <button class="active">点菜</button>
      <button @click="scrollToReviews">评价 {{ reviewTotal }}</button>
      <button @click="scrollToMerchant">商家</button>
      <span class="order-tip">温馨提示：请适量点餐</span>
    </nav>

    <section class="menu-workbench">
      <aside class="category-rail">
        <button :class="{ active: activeCategory === 'all' }" @click="setCategory('all')">
          <span>全部</span>
        </button>
        <button
          v-for="cat in categories"
          :key="cat.id"
          :class="{ active: activeCategory === String(cat.id) }"
          @click="setCategory(String(cat.id))"
        >
          <span>{{ cat.name }}</span>
        </button>
      </aside>

      <main class="dish-panel">
        <div class="dish-panel-head">
          <div>
            <h2>{{ activeCategoryName }}</h2>
            <p>近 30 天 {{ categorySales }} 人下单，热门菜品优先推荐</p>
          </div>
          <el-tag type="warning" effect="light">单点可送</el-tag>
        </div>

        <div v-if="filteredDishes.length > 0" class="dish-list">
          <article v-for="dish in filteredDishes" :key="dish.id" class="dish-item">
            <el-image
              class="dish-image"
              :src="assetUrl(dish.image, 'https://placehold.co/140x140/409eff/white?text=' + dish.name)"
              fit="cover"
            />
            <div class="dish-info">
              <h3>{{ dish.name }}</h3>
              <p class="dish-desc">{{ dish.description || '暂无描述' }}</p>
              <div class="dish-tags">
                <span>月售{{ dish.monthlySales || 0 }}</span>
                <span v-if="Number(dish.monthlySales || 0) > 20">{{ Number(dish.monthlySales) }}人觉得分量足</span>
                <span>单点不送</span>
              </div>
              <div class="dish-price-row">
                <div>
                  <span class="dish-price">¥{{ dish.price }}</span>
                  <span class="coupon-text">已含券</span>
                </div>
                <el-button
                  class="add-pill"
                  type="primary"
                  :loading="isAdding(dish.id)"
                  :disabled="isAdding(dish.id)"
                  @click="handleAddToCart(dish)"
                >
                  {{ currentCartQty(dish.id) ? `已加 ${currentCartQty(dish.id)}` : '加入购物车' }}
                </el-button>
              </div>
            </div>
          </article>
        </div>
        <el-empty v-else description="暂无菜品" />

        <div class="pagination-wrapper" v-if="totalPages > 1">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="totalElements"
            layout="prev, pager, next"
            @current-change="handlePageChange"
          />
        </div>
      </main>
    </section>

    <section ref="reviewsRef" class="info-section">
      <div class="section-title">
        <h2>店铺评价 ({{ reviewTotal }})</h2>
        <span>真实购买后评价</span>
      </div>
      <div v-if="reviews.length > 0" class="review-list">
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
    </section>

    <section ref="merchantRef" class="info-section" v-if="store?.lat && store?.lng">
      <div class="section-title">
        <h2>商家信息</h2>
        <span>{{ store.address }}</span>
      </div>
      <OrderMap
        :store-lat="Number(store.lat)"
        :store-lng="Number(store.lng)"
        :store-name="store.name"
        :static="true"
        :height="260"
      />
    </section>

    <div class="floating-cart" :class="{ show: cartCount > 0 }">
      <div class="cart-badge">
        <el-icon><ShoppingCart /></el-icon>
        <span>{{ cartCount }}</span>
      </div>
      <div class="cart-summary">
        <strong>¥{{ cartTotal }}</strong>
        <span>已选 {{ cartCount }} 件，配送费另计</span>
      </div>
      <el-button type="primary" size="large" @click="router.push('/cart')">去结算</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'
import { getStoreDetail } from '../api/store'
import { getDishes } from '../api/dish'
import { getCategories } from '../api/category'
import { getStoreReviews } from '../api/review'
import { ElMessage, ElNotification } from 'element-plus'
import { ArrowLeft, Search, ShoppingCart, Star } from '@element-plus/icons-vue'
import OrderMap from '../components/OrderMap.vue'
import { assetUrl } from '../utils/assets'

const route = useRoute()
const router = useRouter()
const auth = useAuthStore()
const cartStore = useCartStore()

const store = ref(null)
const dishes = ref([])
const categories = ref([])
const activeCategory = ref('all')
const keyword = ref('')
const orderMode = ref('delivery')
const orderModeOptions = [
  { label: '外卖', value: 'delivery' },
  { label: '自取', value: 'pickup' },
]
const reviews = ref([])
const reviewTotal = ref(0)
const currentPage = ref(0)
const pageSize = ref(10)
const totalPages = ref(0)
const totalElements = ref(0)
const addingDishIds = ref(new Set())
const reviewsRef = ref(null)
const merchantRef = ref(null)

const filteredDishes = computed(() => {
  const term = keyword.value.trim().toLowerCase()
  if (!term) return dishes.value
  return dishes.value.filter(dish => `${dish.name || ''} ${dish.description || ''}`.toLowerCase().includes(term))
})

const activeCategoryName = computed(() => {
  if (activeCategory.value === 'all') return '全部菜品'
  return categories.value.find(cat => String(cat.id) === activeCategory.value)?.name || '菜品'
})

const categorySales = computed(() => filteredDishes.value.reduce((sum, dish) => sum + Number(dish.monthlySales || 0), 0))
const cartCount = computed(() => cartStore.items.reduce((sum, item) => sum + Number(item.quantity || 0), 0))
const cartTotal = computed(() => cartStore.totalPrice().toFixed(2))

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

function setCategory(value) {
  activeCategory.value = value
  handleCategoryChange()
}

function handlePageChange(page) {
  currentPage.value = page - 1
  fetchDishes()
  window.scrollTo({ top: 360, behavior: 'smooth' })
}

async function handleAddToCart(dish) {
  if (!auth.isLoggedIn()) { router.push('/login'); return }
  if (auth.getRole() !== 'CUSTOMER') { ElMessage.warning('仅顾客可加购'); return }
  try {
    setAdding(dish.id, true)
    await cartStore.add(dish.id)
    ElNotification({
      title: '已加入购物车',
      type: 'success',
      message: `${dish.name}，可继续点餐或去结算`,
      position: 'bottom-left',
      duration: 2200,
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

function currentCartQty(dishId) {
  const item = cartStore.items.find(i => i.dish?.id === dishId)
  return item?.quantity || 0
}

function scrollToReviews() {
  reviewsRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

function scrollToMerchant() {
  merchantRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
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
  if (auth.isLoggedIn() && auth.getRole() === 'CUSTOMER') cartStore.fetchCart()
})
</script>

<style scoped>
.store-order-page { padding-bottom: 96px; }
.order-topbar { position: sticky; top: 64px; z-index: 20; display: flex; align-items: center; gap: 12px; margin: -4px 0 16px; padding: 10px 0; background: #f7f2ec; }
.icon-link, .soft-action { border: none; background: #fff; box-shadow: 0 4px 14px rgba(31, 35, 41, 0.08); }
.mode-switch { --el-segmented-item-selected-bg-color: #ffe24a; --el-segmented-item-selected-color: #1f2329; font-weight: 700; }
.topbar-spacer { flex: 1; }
.dish-search { width: min(320px, 35vw); }
.store-hero { display: grid; grid-template-columns: 180px 1fr; gap: 24px; padding: 24px; border-radius: 18px; background: #fff; box-shadow: 0 8px 24px rgba(31, 35, 41, 0.08); }
.store-cover { width: 180px; height: 140px; border-radius: 14px; background: #f2f4f7; }
.store-copy h1 { margin: 0 0 10px; font-size: 30px; line-height: 1.15; }
.store-stats { display: flex; align-items: center; gap: 14px; margin-bottom: 10px; color: #606266; }
.store-details { display: flex; flex-wrap: wrap; gap: 8px; color: #73777f; }
.store-details span { padding: 5px 10px; border-radius: 999px; background: #f6f7f9; font-size: 13px; }
.store-announcement { display: inline-flex; align-items: center; gap: 6px; margin-top: 12px; color: #ff7a00; font-size: 13px; }
.order-tabs { display: flex; align-items: center; gap: 28px; margin: 18px 0; border-bottom: 1px solid #ebe3db; }
.order-tabs button { position: relative; padding: 14px 0; border: 0; background: transparent; color: #606266; font-size: 20px; font-weight: 700; cursor: pointer; }
.order-tabs button.active { color: #1f2329; }
.order-tabs button.active::after { content: ""; position: absolute; left: 0; right: 0; bottom: -1px; height: 4px; border-radius: 999px; background: #ffe24a; }
.order-tip { margin-left: auto; color: #a0a4ad; font-size: 14px; }
.menu-workbench { display: grid; grid-template-columns: 164px minmax(0, 1fr); gap: 18px; align-items: start; }
.category-rail { position: sticky; top: 142px; display: flex; flex-direction: column; overflow: hidden; border-radius: 16px; background: #fff; box-shadow: 0 8px 24px rgba(31, 35, 41, 0.06); }
.category-rail button { min-height: 54px; padding: 10px 14px; border: 0; border-left: 4px solid transparent; background: #fff; color: #606266; text-align: left; font-size: 15px; cursor: pointer; }
.category-rail button.active { border-left-color: #ff8c00; background: #fff8ea; color: #1f2329; font-weight: 800; }
.dish-panel { min-width: 0; padding: 22px 24px; border-radius: 18px; background: #fff; box-shadow: 0 8px 24px rgba(31, 35, 41, 0.06); }
.dish-panel-head { display: flex; justify-content: space-between; gap: 16px; align-items: flex-start; margin-bottom: 6px; }
.dish-panel-head h2 { margin: 0 0 4px; font-size: 22px; }
.dish-panel-head p { margin: 0; color: #909399; font-size: 13px; }
.dish-list { display: flex; flex-direction: column; }
.dish-item { display: grid; grid-template-columns: 140px minmax(0, 1fr); gap: 18px; padding: 20px 0; border-bottom: 1px solid #f0f1f3; }
.dish-item:last-child { border-bottom: none; }
.dish-image { width: 140px; height: 140px; border-radius: 14px; background: #f2f4f7; }
.dish-info { min-width: 0; }
.dish-info h3 { margin: 0 0 8px; font-size: 21px; line-height: 1.25; }
.dish-desc { margin: 0 0 8px; color: #8b9099; font-size: 14px; line-height: 1.5; }
.dish-tags { display: flex; flex-wrap: wrap; gap: 6px; margin-bottom: 12px; }
.dish-tags span { padding: 3px 7px; border-radius: 6px; background: #fff6ed; color: #bf6a1a; font-size: 12px; }
.dish-price-row { display: flex; align-items: center; justify-content: space-between; gap: 12px; }
.dish-price { color: #f04438; font-size: 26px; font-weight: 900; }
.coupon-text { margin-left: 8px; padding: 2px 7px; border: 1px solid #ffd4c9; border-radius: 6px; color: #f04438; font-size: 12px; }
.add-pill { min-width: 112px; border: none; border-radius: 999px; background: #ffe24a; color: #1f2329; font-weight: 800; }
.add-pill:hover { background: #ffd31a; color: #1f2329; }
.info-section { margin-top: 18px; padding: 24px; border-radius: 18px; background: #fff; box-shadow: 0 8px 24px rgba(31, 35, 41, 0.06); scroll-margin-top: 130px; }
.section-title { display: flex; align-items: baseline; gap: 12px; margin-bottom: 12px; }
.section-title h2 { margin: 0; font-size: 22px; }
.section-title span { color: #909399; }
.review-list { display: grid; gap: 8px; }
.review-item { padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.review-item:last-child { border-bottom: none; }
.review-header { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.review-user { font-weight: bold; font-size: 14px; }
.review-time { color: #c0c4cc; font-size: 12px; }
.review-purchased { display: inline-flex; align-items: center; max-width: 100%; margin-bottom: 6px; padding: 3px 8px; border-radius: 4px; background: #f5f7fa; color: #909399; font-size: 12px; line-height: 1.4; }
.review-content { color: #606266; line-height: 1.6; }
.review-reply { background: #f5f7fa; padding: 8px 12px; border-radius: 4px; margin-top: 6px; font-size: 13px; color: #909399; }
.pagination-wrapper { display: flex; justify-content: center; margin-top: 20px; }
.floating-cart { position: fixed; left: 50%; bottom: 22px; z-index: 40; display: flex; align-items: center; gap: 16px; width: min(760px, calc(100vw - 32px)); padding: 12px 14px; border-radius: 999px; background: #1f2329; color: #fff; box-shadow: 0 18px 45px rgba(31, 35, 41, 0.28); transform: translate(-50%, 120px); opacity: 0; pointer-events: none; transition: transform .2s ease, opacity .2s ease; }
.floating-cart.show { transform: translate(-50%, 0); opacity: 1; pointer-events: auto; }
.cart-badge { position: relative; display: grid; place-items: center; width: 48px; height: 48px; border-radius: 50%; background: #ff8c00; color: #fff; }
.cart-badge span { position: absolute; top: -5px; right: -5px; min-width: 20px; height: 20px; padding: 0 5px; border-radius: 999px; background: #ff4d4f; font-size: 12px; line-height: 20px; text-align: center; }
.cart-summary { flex: 1; display: flex; flex-direction: column; gap: 2px; }
.cart-summary strong { font-size: 24px; }
.cart-summary span { color: #c7ccd6; font-size: 13px; }
@media (max-width: 900px) {
  .order-topbar { top: 56px; flex-wrap: wrap; }
  .topbar-spacer { display: none; }
  .dish-search { width: 100%; order: 3; }
  .store-hero { grid-template-columns: 96px 1fr; padding: 16px; }
  .store-cover { width: 96px; height: 96px; }
  .store-copy h1 { font-size: 22px; }
  .menu-workbench { grid-template-columns: 104px minmax(0, 1fr); gap: 10px; }
  .category-rail { top: 174px; }
  .dish-panel { padding: 16px; }
  .dish-item { grid-template-columns: 96px minmax(0, 1fr); gap: 12px; }
  .dish-image { width: 96px; height: 96px; }
  .dish-info h3 { font-size: 17px; }
  .dish-price-row { align-items: flex-end; }
  .add-pill { min-width: 42px; width: 42px; height: 42px; padding: 0; font-size: 0; }
  .add-pill::after { content: "+"; font-size: 24px; line-height: 1; }
  .order-tip { display: none; }
}
</style>
