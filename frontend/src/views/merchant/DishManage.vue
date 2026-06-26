<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-title">
        <el-icon :size="22"><KnifeFork /></el-icon> 菜品管理
      </h1>
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
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="菜品名称" prop="name"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="描述" prop="description"><el-input v-model="form.description" type="textarea" /></el-form-item>
        <el-form-item label="价格" prop="price"><el-input-number v-model="form.price" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="图片" prop="image">
          <div class="image-field">
            <el-upload drag action="" :auto-upload="false" :show-file-list="false"
              accept="image/png,image/jpeg,image/webp,image/gif"
              :on-change="handleImageChange">
              <el-image v-if="form.image" :src="assetUrl(form.image)" fit="cover" class="image-preview" />
              <div v-else class="upload-placeholder">
                <div class="upload-plus">+</div>
                <div>点击或拖拽上传图片</div>
                <small>支持 JPG / PNG / WEBP / GIF，5MB 内</small>
              </div>
            </el-upload>
            <el-input v-model="form.image" placeholder="也可以粘贴图片链接" />
          </div>
        </el-form-item>
        <el-form-item label="分类" prop="categoryId">
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
import { uploadImage } from '../../api/upload'
import { assetUrl } from '../../utils/assets'
import { ElMessage, ElMessageBox } from 'element-plus'

const dishes = ref([])
const categories = ref([])
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const editingId = ref(null)
const formRef = ref(null)

const form = reactive({
  name: '', description: '', price: 0, image: '', categoryId: null, status: true
})

const rules = {
  name: [{ required: true, message: '请填写菜品名称', trigger: 'blur' }],
  description: [{ required: true, message: '请填写菜品描述', trigger: 'blur' }],
  price: [
    { required: true, message: '请填写价格', trigger: 'change' },
    {
      validator: (_rule, value, callback) => {
        Number(value) > 0 ? callback() : callback(new Error('价格必须大于 0'))
      },
      trigger: 'change',
    },
  ],
  image: [{ required: true, message: '请上传图片或填写图片链接', trigger: 'change' }],
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
}

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
  formRef.value?.clearValidate()
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('\u8bf7\u5148\u8865\u5168\u83dc\u54c1\u4fe1\u606f')
    return
  }
  saving.value = true
  try {
    if (editingId.value) {
      await request.put(`/merchant/dishes/${editingId.value}`, form)
    } else {
      await request.post('/merchant/dishes', form)
    }
    ElMessage.success(editingId.value ? '\u4fee\u6539\u6210\u529f' : '\u6dfb\u52a0\u6210\u529f')
    showDialog.value = false
    fetchDishes()
  } catch (e) {
    // interceptor shows error
  }
  saving.value = false
}

async function handleImageChange(uploadFile) {
  const file = uploadFile.raw
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('\u8bf7\u9009\u62e9\u56fe\u7247\u6587\u4ef6')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('\u56fe\u7247\u4e0d\u80fd\u8d85\u8fc7 5MB')
    return
  }
  try {
    saving.value = true
    const data = await uploadImage(file)
    form.image = data.url
    formRef.value?.validateField('image')
    ElMessage.success('\u56fe\u7247\u4e0a\u4f20\u6210\u529f')
  } catch { /* handled */ }
  finally {
    saving.value = false
  }
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
.image-field { width: 100%; display: grid; gap: 8px; }
.image-field :deep(.el-upload) { width: 100%; }
.image-field :deep(.el-upload-dragger) { width: 100%; padding: 0; overflow: hidden; }
.image-preview { width: 100%; height: 160px; display: block; }
.upload-placeholder { height: 160px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #909399; }
.upload-plus { width: 34px; height: 34px; border-radius: 50%; background: #FFF3E0; color: #FF8C00; font-size: 28px; line-height: 30px; margin-bottom: 8px; }
</style>
