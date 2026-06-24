<template>
  <div class="page-container">
    <h1 class="page-title">🏪 店铺管理</h1>

    <el-card v-if="store">
      <el-form :model="form" label-width="100px" style="max-width:600px">
        <el-form-item label="店铺名称">
          <el-input v-model="form.name" placeholder="请输入店铺名称" />
        </el-form-item>
        <el-form-item label="店铺Logo">
          <el-input v-model="form.logo" placeholder="https://..." />
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
        <el-form-item label="店铺地址">
          <el-input v-model="form.address" placeholder="店铺地址" />
        </el-form-item>
        <el-form-item label="联系电话">
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
import { ElMessage } from 'element-plus'

const store = ref(null)
const storeCategories = ref([])
const loading = ref(false)
const saving = ref(false)
const creating = ref(false)
const showCreateDialog = ref(false)

const form = reactive({
  name: '', logo: '', description: '', categoryId: null,
  minOrderAmount: 20, businessHours: '09:00-22:00',
  announcement: '', address: '', phone: '', status: true,
})

const createForm = reactive({
  name: '', categoryId: null, description: '', minOrderAmount: 20,
})

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
  saving.value = true
  try {
    await updateStore(store.value.id, { ...form })
    ElMessage.success('保存成功')
    fetchStore()
  } catch { /* handled */ }
  saving.value = false
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
