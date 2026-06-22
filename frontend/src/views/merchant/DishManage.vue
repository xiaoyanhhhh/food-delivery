<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-title">🍽 菜品管理</h1>
      <el-button type="primary" :icon="Plus" @click="showDialog = true; editingDish = null">
        添加菜品
      </el-button>
    </div>

    <el-table :data="dishes" style="width:100%" v-loading="loading">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="菜品名称" />
      <el-table-column label="分类">
        <template #default="{ row }">
          {{ row.category?.name || '-' }}
        </template>
      </el-table-column>
      <el-table-column prop="price" label="价格" width="100">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column prop="status" label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'info'">
            {{ row.status ? '上架' : '下架' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="editDish(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">下架</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- Add/Edit dialog -->
    <el-dialog v-model="showDialog" :title="editingDish ? '编辑菜品' : '添加菜品'" width="500px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="菜品名称">
          <el-input v-model="form.name" placeholder="请输入菜品名称" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" placeholder="菜品描述" />
        </el-form-item>
        <el-form-item label="价格">
          <el-input-number v-model="form.price" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="图片链接">
          <el-input v-model="form.image" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" active-text="上架" inactive-text="下架" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getCategories } from '../../api/category'
import request from '../../api/request'
import { ElMessage, ElMessageBox } from 'element-plus'

const dishes = ref([])
const categories = ref([])
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const editingDish = ref(null)

const form = reactive({
  name: '',
  description: '',
  price: 0,
  image: '',
  categoryId: null,
  status: true,
})

async function fetchDishes() {
  loading.value = true
  try { dishes.value = await request.get('/merchant/dishes') } catch { /* handled */ }
  loading.value = false
}

async function fetchCategories() {
  try { categories.value = await getCategories() } catch { /* handled */ }
}

function editDish(dish) {
  editingDish.value = dish
  form.name = dish.name
  form.description = dish.description || ''
  form.price = dish.price
  form.image = dish.image || ''
  form.categoryId = dish.category?.id || null
  form.status = dish.status
  showDialog.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (editingDish.value) {
      await request.put(`/merchant/dishes/${editingDish.value.id}`, form)
      ElMessage.success('修改成功')
    } else {
      await request.post('/merchant/dishes', form)
      ElMessage.success('添加成功')
    }
    showDialog.value = false
    resetForm()
    fetchDishes()
  } catch { /* handled */ }
  saving.value = false
}

function resetForm() {
  editingDish.value = null
  form.name = ''
  form.description = ''
  form.price = 0
  form.image = ''
  form.categoryId = null
  form.status = true
}

async function handleDelete(dish) {
  try {
    await ElMessageBox.confirm(`确定下架 "${dish.name}" 吗？`, '提示', { type: 'warning' })
    await request.delete(`/merchant/dishes/${dish.id}`)
    ElMessage.success('已下架')
    fetchDishes()
  } catch { /* cancelled */ }
}

onMounted(() => {
  fetchDishes()
  fetchCategories()
})
</script>

<style scoped>
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-header .page-title {
  margin-bottom: 0;
}
</style>
