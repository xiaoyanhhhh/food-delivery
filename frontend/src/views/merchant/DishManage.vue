<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-title">🍽 菜品管理</h1>
      <el-button type="primary" @click="openAdd">添加菜品</el-button>
    </div>

    <el-table :data="dishes" style="width:100%" v-loading="loading" empty-text="暂无菜品">
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="name" label="菜品名称" />
      <el-table-column label="分类" width="100">
        <template #default="{ row }">{{ row.category?.name || '-' }}</template>
      </el-table-column>
      <el-table-column label="价格" width="100">
        <template #default="{ row }">¥{{ row.price }}</template>
      </el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.status ? 'success' : 'info'">{{ row.status ? '上架' : '下架' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(row)">下架</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="showDialog" :title="editingId ? '编辑菜品' : '添加菜品'" width="500px" @closed="resetForm">
      <el-form :model="form" label-width="80px">
        <el-form-item label="菜品名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="价格"><el-input-number v-model="form.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="图片链接"><el-input v-model="form.image" placeholder="https://..." /></el-form-item>
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
const editingId = ref(null)

const form = reactive({
  name: '', description: '', price: 0, image: '', categoryId: null, status: true
})

async function fetchDishes() {
  loading.value = true
  try {
    const data = await request.get('/merchant/dishes', { params: { page: 0, size: 100 } })
    dishes.value = Array.isArray(data) ? data : (data.content || [])
  } catch {
    dishes.value = []
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    const data = await getCategories()
    categories.value = Array.isArray(data) ? data : []
  } catch {
    categories.value = []
  }
}

function openAdd() {
  editingId.value = null
  Object.assign(form, { name: '', description: '', price: 0, image: '', categoryId: null, status: true })
  showDialog.value = true
}

function openEdit(dish) {
  editingId.value = dish.id
  Object.assign(form, {
    name: dish.name,
    description: dish.description || '',
    price: dish.price,
    image: dish.image || '',
    categoryId: dish.category?.id || null,
    status: dish.status
  })
  showDialog.value = true
}

function resetForm() {
  editingId.value = null
}

async function handleSave() {
  if (!form.name.trim()) { ElMessage.warning('请输入菜品名称'); return }
  if (!form.categoryId) { ElMessage.warning('请选择分类'); return }
  saving.value = true
  try {
    if (editingId.value) {
      await request.put(`/merchant/dishes/${editingId.value}`, form)
    } else {
      await request.post('/merchant/dishes', form)
    }
    ElMessage.success(editingId.value ? '修改成功' : '添加成功')
    showDialog.value = false
    fetchDishes()
  } catch (e) {
    // interceptor shows error
  }
  saving.value = false
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
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header .page-title { margin-bottom: 0; }
</style>
