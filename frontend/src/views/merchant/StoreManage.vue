<template>
  <div class="page-container">
    <h1 class="page-title">
      <el-icon :size="22"><Shop /></el-icon> 店铺管理
    </h1>

    <el-card v-if="store">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px" style="max-width:600px">
        <el-form-item label="店铺名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入店铺名称" />
        </el-form-item>
        <el-form-item label="店铺Logo" prop="logo">
          <div class="image-field">
            <el-upload drag action="" :auto-upload="false" :show-file-list="false"
              accept="image/png,image/jpeg,image/webp,image/gif"
              :on-change="handleLogoChange">
              <el-image v-if="form.logo" :src="assetUrl(form.logo)" fit="cover" class="image-preview" />
              <div v-else class="upload-placeholder">
                <div class="upload-plus">+</div>
                <div>点击或拖拽上传 Logo</div>
                <small>支持 JPG / PNG / WEBP / GIF，5MB 内</small>
              </div>
            </el-upload>
            <el-input v-model="form.logo" placeholder="也可以粘贴图片链接" />
          </div>
        </el-form-item>
        <el-form-item label="店铺描述">
          <el-input v-model="form.description" type="textarea" placeholder="店铺简介" />
        </el-form-item>
        <el-form-item label="店铺分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width:100%">
            <el-option v-for="c in storeCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="起送价">
          <el-input-number v-model="form.minOrderAmount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="营业时间">
          <el-input v-model="form.businessHours" placeholder="例如: 09:00-22:00" />
        </el-form-item>
        <el-form-item label="店铺公告">
          <el-input v-model="form.announcement" type="textarea" placeholder="店铺公告/活动" />
        </el-form-item>
        <el-form-item label="店铺地址" prop="address">
          <el-input v-model="form.address" placeholder="店铺地址" />
        </el-form-item>
        <el-form-item label="地图定位">
          <div class="location-row">
            <el-input-number v-model="form.lat" :precision="6" :step="0.000001" placeholder="纬度" />
            <el-input-number v-model="form.lng" :precision="6" :step="0.000001" placeholder="经度" />
            <el-button @click="useCurrentLocation" :loading="locating">使用当前位置</el-button>
          </div>
        </el-form-item>
        <el-form-item label="联系电话" prop="phone">
          <el-input v-model="form.phone" placeholder="联系电话" />
        </el-form-item>
        <el-form-item label="营业状态">
          <el-switch v-model="form.status" active-text="营业中" inactive-text="休息中" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSave" :loading="saving">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-empty v-else-if="!loading" description="您还没有创建店铺">
      <el-button type="primary" @click="showCreateDialog = true">创建店铺</el-button>
    </el-empty>

    <!-- Create Store Dialog -->
    <el-dialog v-model="showCreateDialog" title="创建店铺" width="500px"
      :close-on-click-modal="false" :close-on-press-escape="false">
      <el-form :model="createForm" label-width="80px">
        <el-form-item label="店铺名称" required>
          <el-input v-model="createForm.name" placeholder="请输入店铺名称" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="createForm.categoryId" placeholder="选择分类" style="width:100%">
            <el-option v-for="c in storeCategories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="createForm.description" type="textarea" />
        </el-form-item>
        <el-form-item label="起送价">
          <el-input-number v-model="createForm.minOrderAmount" :min="0" :precision="2" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" @click="handleCreate" :loading="creating">创建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getMyStore, createStore, updateStore, getStoreCategories } from '../../api/store'
import { uploadImage } from '../../api/upload'
import { assetUrl } from '../../utils/assets'
import { ElMessage } from 'element-plus'

const store = ref(null)
const storeCategories = ref([])
const loading = ref(false)
const saving = ref(false)
const creating = ref(false)
const locating = ref(false)
const showCreateDialog = ref(false)
const formRef = ref(null)

const form = reactive({
  name: '', logo: '', description: '', categoryId: null,
  minOrderAmount: 20, businessHours: '09:00-22:00',
  announcement: '', address: '', phone: '', lat: null, lng: null, status: true,
})

const createForm = reactive({
  name: '', categoryId: null, description: '', minOrderAmount: 20,
})

const rules = {
  name: [{ required: true, message: '请填写店铺名称', trigger: 'blur' }],
  logo: [{ required: true, message: '请上传 Logo 或填写图片链接', trigger: 'change' }],
  address: [{ required: true, message: '请填写店铺地址', trigger: 'blur' }],
  phone: [{ required: true, message: '请填写联系电话', trigger: 'blur' }],
}

async function fetchStore() {
  loading.value = true
  try {
    store.value = await getMyStore()
    form.name = store.value.name
    form.logo = store.value.logo || ''
    form.description = store.value.description || ''
    form.categoryId = store.value.category?.id || null
    form.minOrderAmount = store.value.minOrderAmount || 20
    form.businessHours = store.value.businessHours || '09:00-22:00'
    form.announcement = store.value.announcement || ''
    form.address = store.value.address || ''
    form.phone = store.value.phone || ''
    form.lat = store.value.lat ?? null
    form.lng = store.value.lng ?? null
    form.status = store.value.status
  } catch {
    store.value = null
  }
  loading.value = false
}

async function fetchCategories() {
  try { storeCategories.value = await getStoreCategories() } catch { /* handled */ }
}

async function handleSave() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请先补全店铺信息')
    return
  }
  saving.value = true
  try {
    await updateStore(store.value.id, { ...form })
    ElMessage.success('保存成功')
    fetchStore()
  } catch { /* handled */ }
  saving.value = false
}

async function handleLogoChange(uploadFile) {
  const file = uploadFile.raw
  if (!file) return
  if (!file.type.startsWith('image/')) {
    ElMessage.warning('请选择图片文件')
    return
  }
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.warning('图片不能超过 5MB')
    return
  }
  try {
    saving.value = true
    const data = await uploadImage(file)
    form.logo = data.url
    formRef.value?.validateField('logo')
    ElMessage.success('Logo 上传成功')
  } catch { /* handled */ }
  finally {
    saving.value = false
  }
}

function useCurrentLocation() {
  if (!navigator.geolocation) {
    ElMessage.warning('当前浏览器不支持定位')
    return
  }
  locating.value = true
  navigator.geolocation.getCurrentPosition(
    (pos) => {
      form.lat = Number(pos.coords.latitude.toFixed(6))
      form.lng = Number(pos.coords.longitude.toFixed(6))
      locating.value = false
      ElMessage.success('已获取当前位置')
    },
    () => {
      locating.value = false
      ElMessage.warning('定位失败，请检查浏览器定位权限，或手动填写经纬度')
    },
    { enableHighAccuracy: true, timeout: 10000 },
  )
}

async function handleCreate() {
  creating.value = true
  try {
    await createStore({ ...createForm })
    ElMessage.success('店铺创建成功')
    showCreateDialog.value = false
    fetchStore()
  } catch { /* handled */ }
  creating.value = false
}

onMounted(() => {
  fetchStore()
  fetchCategories()
})
</script>

<style scoped>
.image-field { width: 100%; display: grid; gap: 8px; }
.image-field :deep(.el-upload) { width: 100%; }
.image-field :deep(.el-upload-dragger) { width: 100%; padding: 0; overflow: hidden; }
.image-preview { width: 100%; height: 160px; display: block; }
.upload-placeholder { height: 160px; display: flex; flex-direction: column; align-items: center; justify-content: center; color: #909399; }
.upload-plus { width: 34px; height: 34px; border-radius: 50%; background: #FFF3E0; color: #FF8C00; font-size: 28px; line-height: 30px; margin-bottom: 8px; }
.location-row { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
</style>
