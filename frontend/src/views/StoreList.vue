<template>
  <div class="page-container">
    <!-- Search Bar with dual mode -->
    <div class="search-section">
      <div class="search-bar">
        <el-input v-model="keyword" :placeholder="searchMode === 'store' ? '搜索店铺...' : '搜索菜品...'"
          size="large" :prefix-icon="Search" clearable @keyup.enter="handleSearch" style="width: 400px" />
        <el-button type="primary" size="large" @click="handleSearch">搜索</el-button>
        <el-radio-group v-model="searchMode" size="small" style="margin-left:12px">
          <el-radio-button value="store">搜店铺</el-radio-button>
          <el-radio-button value="dish">搜菜品</el-radio-button>
        </el-radio-group>
      </div>
    </div>

    <!-- Controls: category tabs + favorite filter -->
    <div class="controls">
      <el-tabs v-model="activeCategory" @tab-change="handleCategoryChange">
        <el-tab-pane label="全部" name="all" />
        <el-tab-pane v-for="cat in storeCategories" :key="cat.id" :label="cat.name" :name="String(cat.id)" />
      </el-tabs>
      <el-checkbox v-if="isLogin && role === 'CUSTOMER'" v-model="favOnly" @change="fetchStores" style="margin-left:auto">
        仅看收藏
      </el-checkbox>
    </div>

    <!-- Dish Search Results (cross-store) -->
    <div v-if="searchMode === 'dish' && keyword && dishResults.length > 0" class="dish-results">
      <h3 style="margin-bottom:12px">搜索结果：</h3>
      <el-card v-for="dish in dishResults" :key="dish.id" class="dish-result-item" @click="$router.push(`/dishes/${dish.id}`)">
        <div class="dish-row">
          <span style="font-weight:bold">{{ dish.name }}</span>
          <span style="color:#f56c6c">¥{{ dish.price }}</span>
          <span style="color:#909399;font-size:12px">{{ '\u70b9\u51fb\u67e5\u770b\u8be6\u60c5' }}</span>
          <el-tag size="small">{{ dish.monthlySales || 0 }}份</el-tag>
        </div>
      </el-card>
    </div>

    <!-- Store Grid -->
    <div v-else-if="stores.length > 0" class="store-grid">
      <el-card v-for="store in stores" :key="store.id" class="store-card" shadow="hover" @click="$router.push(`/store/${store.id}`)">
        <div class="store-logo">
          <el-image :src="store.logo || 'https://placehold.co/280x160/409eff/white?text=' + store.name"
            fit="cover" style="width:100%;height:160px;border-radius:8px" />
          <!-- Favorite button -->
          <el-button v-if="isLogin && role === 'CUSTOMER'" class="fav-btn"
            :type="favIds.includes(store.id) ? 'danger' : 'default'"
            size="small" circle @click.stop="toggleFav(store)">
            <el-icon :size="16"><component :is="favIds.includes(store.id) ? 'StarFilled' : 'Star'" /></el-icon>
          </el-button>
        </div>
        <div class="store-info">
          <h3 class="store-name">{{ store.name }}</h3>
          <div class="store-meta">
            <el-rate v-model="store.rating" disabled size="small" show-score :score-template="'{value}'" />
            <span class="monthly-sales">月售{{ store.monthlySales || 0 }}</span>
            <el-tag v-if="!store.status" type="danger" size="small">休息中</el-tag>
          </div>
          <div class="store-bottom">
            <span>配送费 ¥{{ store.deliveryFee || 3 }}</span>
            <span>起送 ¥{{ store.minOrderAmount || 20 }}</span>
            <el-tag v-if="store.category" size="small" type="info">{{ store.category.name }}</el-tag>
          </div>
        </div>
      </el-card>
    </div>
    <el-empty v-else description="暂无店铺" />

    <div class="pagination-wrapper" v-if="totalPages > 1 && searchMode !== 'dish'">
      <el-pagination v-model:current-page="currentPage" :page-size="pageSize" :total="totalElements"
        layout="prev, pager, next, total" @current-change="(p) => { currentPage = p - 1; fetchStores() }" />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, watch } from 'vue'
import { getStores, getStoreCategories } from '../api/store'
import { getDishes } from '../api/dish'
import { getFavorites, addFavorite, removeFavorite } from '../api/favorite'
import { Search } from '@element-plus/icons-vue'
import { useAuthStore } from '../stores/auth'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()
const isLogin = ref(auth.isLoggedIn())
const role = ref(auth.getRole())

const stores = ref([])
const storeCategories = ref([])
const activeCategory = ref('all')
const keyword = ref('')
const searchMode = ref('store')
const favOnly = ref(false)
const favIds = ref([])
const dishResults = ref([])
const currentPage = ref(0)
const pageSize = ref(12)
const totalElements = ref(0)
const totalPages = ref(0)

async function fetchStores() {
  try {
    const params = { page: currentPage.value, size: pageSize.value }
    if (activeCategory.value !== 'all') params.categoryId = activeCategory.value
    if (keyword.value) params.keyword = keyword.value
    if (favOnly.value) {
      // 仅看收藏：只显示已收藏的店铺
      if (favIds.value.length > 0) {
        const data = await getStores(params)
        stores.value = data.content.filter(s => favIds.value.includes(s.id))
        totalElements.value = data.totalElements
        totalPages.value = data.totalPages
      } else {
        stores.value = []
        totalElements.value = 0
        totalPages.value = 0
      }
    } else {
      const data = await getStores(params)
      stores.value = data.content
      totalElements.value = data.totalElements
      totalPages.value = data.totalPages
    }
  } catch { stores.value = [] }
}

async function fetchDishSearch() {
  try {
    const data = await getDishes({ keyword: keyword.value, size: 50 })
    dishResults.value = data.content || []
  } catch { dishResults.value = [] }
}

async function fetchCategories() {
  try { storeCategories.value = await getStoreCategories() } catch { /* handled */ }
}

async function fetchFavIds() {
  if (!isLogin.value || role.value !== 'CUSTOMER') return
  try {
    const data = await getFavorites({ page: 0, size: 100 })
    favIds.value = (data.content || data || []).map(f => f.store?.id).filter(Boolean)
  } catch { favIds.value = [] }
}

function handleSearch() {
  const term = keyword.value.trim()
  if (searchMode.value === 'dish') {
    if (!term) {
      ElMessage.warning('请输入菜品名称')
      dishResults.value = []
      return
    }
    fetchDishSearch()
  } else {
    currentPage.value = 0
    fetchStores()
  }
}

function handleCategoryChange() { currentPage.value = 0; fetchStores() }

watch(searchMode, () => {
  currentPage.value = 0
  dishResults.value = []
  if (searchMode.value === 'store') fetchStores()
})

watch(keyword, (value) => {
  if (!value) {
    dishResults.value = []
    if (searchMode.value === 'store') fetchStores()
  }
})

async function toggleFav(store) {
  try {
    if (favIds.value.includes(store.id)) {
      await removeFavorite(store.id)
      favIds.value = favIds.value.filter(id => id !== store.id)
    } else {
      await addFavorite(store.id)
      favIds.value.push(store.id)
    }
  } catch (e) { ElMessage.error('操作失败') }
}

onMounted(() => {
  fetchStores()
  fetchCategories()
  fetchFavIds()
})
</script>

<style scoped>
.search-section { background: linear-gradient(135deg, #FF8C00 0%, #FFA500 100%); padding: 30px 0; margin: 0 -16px 20px -16px; display: flex; justify-content: center; }
.search-bar { display: flex; align-items: center; gap: 10px; }
.controls { display: flex; align-items: center; margin-bottom: 16px; }
.store-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 20px; }
.store-card { cursor: pointer; transition: transform 0.2s, box-shadow 0.2s; position: relative; border-radius: 12px; border: 1px solid #EBE8E2; }
.store-card:hover { box-shadow: 0 4px 20px rgba(255, 140, 0, 0.15); }
.store-card:hover { transform: translateY(-4px); }
.store-logo { position: relative; }
.fav-btn { position: absolute; top: 8px; right: 8px; z-index: 10; }
.store-info { padding: 12px 0 0; }
.store-name { font-size: 18px; font-weight: bold; margin-bottom: 8px; }
.store-meta { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.monthly-sales { font-size: 13px; color: #FF8C00; font-weight: 500; }
.store-bottom { display: flex; gap: 12px; font-size: 13px; color: #909399; }
.dish-results { margin-bottom: 16px; }
.dish-result-item { margin-bottom: 8px; cursor: pointer; }
.dish-row { display: flex; align-items: center; gap: 16px; }
.pagination-wrapper { display: flex; justify-content: center; margin-top: 30px; }
@media (max-width: 1200px) { .store-grid { grid-template-columns: repeat(2, 1fr); } }
@media (max-width: 768px) { .store-grid { grid-template-columns: 1fr; } }
</style>
