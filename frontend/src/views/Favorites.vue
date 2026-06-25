<template>
  <div class="page-container">
    <h1 class="page-title">
      <el-icon :size="22"><StarFilled /></el-icon> 我的收藏
    </h1>
    <el-empty v-if="stores.length === 0" description="暂无收藏">
      <el-button type="primary" @click="$router.push('/')">去逛逛</el-button>
    </el-empty>
    <div v-else class="store-grid">
      <el-card v-for="f in stores" :key="f.store?.id" class="store-card" shadow="hover"
        @click="$router.push(`/store/${f.store?.id}`)">
        <h3>{{ f.store?.name }}</h3>
        <div class="store-meta">
          <el-rate v-model="f.store.rating" disabled size="small" />
          <span>月售{{ f.store?.monthlySales || 0 }}</span>
        </div>
        <el-button link type="danger" @click.stop="handleRemove(f.store.id)">取消收藏</el-button>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getFavorites, removeFavorite } from '../api/favorite'
import { ElMessage } from 'element-plus'

const stores = ref([])

async function fetchFavorites() {
  try {
    const data = await getFavorites({ page: 0, size: 50 })
    stores.value = data.content || data || []
  } catch { stores.value = [] }
}

async function handleRemove(storeId) {
  try {
    await removeFavorite(storeId)
    ElMessage.success('已取消收藏')
    fetchFavorites()
  } catch { /* handled */ }
}

onMounted(fetchFavorites)
</script>

<style scoped>
.store-grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 16px; }
.store-card { cursor: pointer; transition: transform 0.2s; }
.store-card:hover { transform: translateY(-4px); }
.store-meta { display: flex; align-items: center; gap: 8px; margin: 8px 0; }
</style>
