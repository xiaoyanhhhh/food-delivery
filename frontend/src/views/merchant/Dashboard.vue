<template>
  <div class="page-container">
    <h1 class="page-title">🏪 商家中心</h1>
    <el-row :gutter="20">
      <el-col :span="8">
        <el-card @click="$router.push('/merchant/dishes')" class="menu-card" shadow="hover">
          <div class="menu-content">
            <el-icon :size="48"><Dish /></el-icon>
            <h3>菜品管理</h3>
            <p>添加、编辑、下架菜品</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card @click="$router.push('/merchant/orders')" class="menu-card" shadow="hover">
          <div class="menu-content">
            <el-icon :size="48"><Document /></el-icon>
            <h3>订单管理</h3>
            <p>查看和处理顾客订单</p>
          </div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card @click="showCategoryDialog = true" class="menu-card" shadow="hover">
          <div class="menu-content">
            <el-icon :size="48"><List /></el-icon>
            <h3>分类管理</h3>
            <p>管理菜品分类</p>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- Category add dialog -->
    <el-dialog v-model="showCategoryDialog" title="添加分类" width="400px">
      <el-form>
        <el-form-item label="分类名称">
          <el-input v-model="categoryName" placeholder="例如：主食、饮品" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="categorySort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCategoryDialog = false">取消</el-button>
        <el-button type="primary" @click="handleAddCategory">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { getCategories } from '../../api/category'
import request from '../../api/request'
import { ElMessage } from 'element-plus'

const showCategoryDialog = ref(false)
const categoryName = ref('')
const categorySort = ref(0)

async function handleAddCategory() {
  if (!categoryName.value) {
    ElMessage.warning('请输入分类名称')
    return
  }
  try {
    await request.post('/categories', { name: categoryName.value, sortOrder: categorySort.value })
    ElMessage.success('分类添加成功')
    showCategoryDialog.value = false
    categoryName.value = ''
    categorySort.value = 0
  } catch { /* handled */ }
}
</script>

<style scoped>
.menu-card {
  cursor: pointer;
  text-align: center;
  transition: transform 0.2s;
}
.menu-card:hover {
  transform: translateY(-4px);
}
.menu-content {
  padding: 30px 0;
}
.menu-content h3 {
  margin: 12px 0 6px;
  font-size: 18px;
}
.menu-content p {
  color: #909399;
  font-size: 14px;
}
</style>
