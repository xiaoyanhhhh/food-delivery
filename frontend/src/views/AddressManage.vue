<template>
  <div class="page-container">
    <div style="max-width: 600px; margin: 0 auto">
      <div class="page-header">
        <h1 class="page-title">
          <el-icon :size="22"><LocationFilled /></el-icon> 地址管理
        </h1>
        <el-button type="primary" :icon="Plus" @click="openDialog(null)">添加地址</el-button>
      </div>

      <el-empty v-if="addresses.length === 0" description="暂无地址" />
      <el-card v-for="addr in addresses" :key="addr.id" class="address-card">
        <div class="address-info">
          <div class="address-top">
            <el-tag size="small" :type="addr.isDefault ? 'success' : 'info'">{{ addr.label }}</el-tag>
            <span class="contact">{{ addr.contactName }} {{ addr.contactPhone }}</span>
            <el-tag v-if="addr.isDefault" size="small" type="danger">默认</el-tag>
          </div>
          <p class="detail">{{ addr.detail }}</p>
        </div>
        <div class="address-actions">
          <el-button link type="primary" @click="openDialog(addr)">编辑</el-button>
          <el-button link type="danger" @click="handleDelete(addr.id)">删除</el-button>
        </div>
      </el-card>

      <!-- Add/Edit Dialog -->
      <el-dialog v-model="showDialog" :title="editing ? '编辑地址' : '添加地址'" width="480px">
        <el-form :model="form" label-width="80px">
          <el-form-item label="标签">
            <el-select v-model="form.label" style="width:100%">
              <el-option value="家" /><el-option value="公司" /><el-option value="学校" /><el-option value="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="联系人">
            <el-input v-model="form.contactName" placeholder="收件人姓名" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="form.contactPhone" placeholder="手机号" />
          </el-form-item>
          <el-form-item label="详细地址">
            <el-input v-model="form.detail" type="textarea" placeholder="详细地址" />
          </el-form-item>
          <el-form-item label="设为默认">
            <el-switch v-model="form.isDefault" />
          </el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="showDialog = false">取消</el-button>
          <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { getAddresses, createAddress, updateAddress, deleteAddress } from '../api/address'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const addresses = ref([])
const showDialog = ref(false)
const editing = ref(null)
const saving = ref(false)

const form = reactive({
  label: '家',
  contactName: '',
  contactPhone: '',
  detail: '',
  isDefault: false,
})

async function fetchAddresses() {
  try { addresses.value = await getAddresses() } catch { /* handled */ }
}

function openDialog(addr) {
  if (addr) {
    editing.value = addr
    form.label = addr.label
    form.contactName = addr.contactName
    form.contactPhone = addr.contactPhone
    form.detail = addr.detail
    form.isDefault = addr.isDefault
  } else {
    editing.value = null
    form.label = '家'
    form.contactName = ''
    form.contactPhone = ''
    form.detail = ''
    form.isDefault = false
  }
  showDialog.value = true
}

async function handleSave() {
  saving.value = true
  try {
    if (editing.value) {
      await updateAddress(editing.value.id, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createAddress({ ...form })
      ElMessage.success('添加成功')
    }
    showDialog.value = false
    fetchAddresses()
  } catch { /* handled */ }
  saving.value = false
}

async function handleDelete(id) {
  try {
    await ElMessageBox.confirm('确定删除此地址吗？', '提示', { type: 'warning' })
    await deleteAddress(id)
    ElMessage.success('已删除')
    fetchAddresses()
  } catch { /* cancelled */ }
}

onMounted(fetchAddresses)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header .page-title { margin-bottom: 0; }
.address-card { margin-bottom: 12px; display: flex; justify-content: space-between; align-items: center; }
.address-top { display: flex; align-items: center; gap: 8px; margin-bottom: 6px; }
.contact { font-weight: bold; font-size: 15px; }
.detail { color: #606266; font-size: 14px; }
.address-actions { display: flex; gap: 4px; flex-shrink: 0; margin-left: 12px; }
.address-actions :deep(.el-button.is-link) {
  min-width: auto;
  height: auto;
  padding: 2px 6px;
  background: transparent !important;
}
.address-actions :deep(.el-button.is-link.el-button--primary) {
  color: #FF8C00;
}
.address-actions :deep(.el-button.is-link.el-button--danger) {
  color: #f56c6c;
}
</style>
