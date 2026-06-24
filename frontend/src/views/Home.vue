<template>
  <div class="page-container">
    <!-- Search bar -->
    <div class="search-bar">
      <el-input v-model="keyword" placeholder="搜索菜品..." @keyup.enter="handleSearch"
        :prefix-icon="Search" clearable style="width: 400px" />
      <el-button type="primary" @click="handleSearch">搜索</el-button>
    </div>

    <!-- Category tabs -->
    <el-tabs v-model="activeCategory" @tab-change="handleCategoryChange" style="margin-bottom: 20px">
      <el-tab-pane label="全部" name="all" />
      <el-tab-pane v-for="cat in categories" :key="cat.id"
        :label="cat.name" :name="String(cat.id)" />
    </el-tabs>

    <!-- Dish Grid -->
    <div v-if="dishes.length > 0" class="dish-grid">
      <el-card v-for="dish in dishes" :key="dish.id" class="dish-card" shadow="hover"
        @click="$router.push(`/dishes/${dish.id}`)">
        <el-image :src="dish.image || 'https://placehold.co/260x180/409eff/white?text=' + dish.name"
          fit="cover" style="width:100%;height:180px;border-radius:8px" />
        <div class="dish-info">
          <h3>{{ dish.name }}</h3>
          <p class="dish-desc">{{ dish.description || '暂无描述' }}</p>
          <div class="dish-bottom">
            <span class="price">¥{{ dish.price }}</span>
            <el-button type="primary" size="small" :icon="Plus" circle
              @click.stop="handleAddToCart(dish)" />
          </div>
        </div>
      </el-card>
    </div>
    <el-empty v-else description="暂无菜品" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { useCartStore } from '../stores/cart'
import { getDishes, searchDishes } from '../api/dish'
import { getCategories } from '../api/category'
import { ElMessage } from 'element-plus'
import { Search, Plus } from '@element-plus/icons-vue'

const router = useRouter()
const auth = useAuthStore()
const cartStore = useCartStore()

const dishes = ref([])
const categories = ref([])
const activeCategory = ref('all')
const keyword = ref('')

async function fetchDishes() {
  try {
    const params = {}
    if (activeCategory.value !== 'all') params.categoryId = activeCategory.value
    if (keyword.value) params.keyword = keyword.value
    dishes.value = await getDishes(params)
  } catch { /* handled by interceptor */ }
}

async function fetchCategories() {
  try {
    categories.value = await getCategories()
  } catch { /* handled by interceptor */ }
}

function handleCategoryChange(val) {
  activeCategory.value = val
  fetchDishes()
}

function handleSearch() {
  fetchDishes()
}

async function handleAddToCart(dish) {
  if (!auth.isLoggedIn()) {
    router.push('/login')
    return
  }
  if (auth.getRole() !== 'CUSTOMER') {
    ElMessage.warning('仅顾客可加购')
    return
  }
  try {
    await cartStore.add(dish.id)
    ElMessage.success(`已添加 ${dish.name} 到购物车`)
  } catch { /* handled */ }
}

onMounted(() => {
  fetchDishes()
  fetchCategories()
})
</script>

<style scoped>
.search-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 20px;
}
.dish-card {
  cursor: pointer;
  transition: transform 0.2s;
}
.dish-card:hover {
  transform: translateY(-4px);
}
.dish-info {
  padding: 12px 0 0;
}
.dish-info h3 {
  font-size: 16px;
  margin-bottom: 6px;
}
.dish-desc {
  font-size: 13px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 8px;
}
.dish-bottom {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.price {
  font-size: 18px;
  font-weight: bold;
  color: #f56c6c;
}
</style>
