<template>
  <div class="page-container">
    <div style="max-width: 500px; margin: 0 auto">
      <h1 class="page-title">
        <el-icon :size="22"><UserFilled /></el-icon> 个人中心
      </h1>

      <el-card>
        <el-descriptions title="账号信息" :column="1" border>
          <el-descriptions-item label="用户名">{{ auth.user?.username }}</el-descriptions-item>
          <el-descriptions-item label="角色">
            <el-tag>{{ roleLabel(auth.user?.role) }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="手机号">{{ profile.phone || '未设置' }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ profile.address || '未设置' }}</el-descriptions-item>
          <el-descriptions-item v-if="auth.user?.role === 'RIDER'" label="骑手类型">
            <el-tag :type="profile.riderType === 'FULL_TIME' ? 'success' : 'info'">
              {{ riderTypeLabel(profile.riderType) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card style="margin-top: 16px">
        <template #header><h3>修改信息</h3></template>
        <el-form :model="form" label-width="80px">
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="手机号" />
          </el-form-item>
          <el-form-item label="地址">
            <el-input v-model="form.address" placeholder="常用地址" />
          </el-form-item>
          <el-form-item v-if="auth.user?.role === 'RIDER'" label="骑手类型">
            <el-segmented v-model="form.riderType" :options="riderTypeOptions" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSave" :loading="saving">保存</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useAuthStore } from '../stores/auth'
import request from '../api/request'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()
const saving = ref(false)

const profile = reactive({
  phone: auth.user?.phone || '',
  address: auth.user?.address || '',
  riderType: auth.user?.riderType || 'PART_TIME',
})

const form = reactive({
  phone: profile.phone,
  address: profile.address,
  riderType: profile.riderType,
})

const riderTypeOptions = [
  { label: '全职', value: 'FULL_TIME' },
  { label: '兼职', value: 'PART_TIME' },
]

function roleLabel(role) {
  const map = { CUSTOMER: '顾客', MERCHANT: '商家', RIDER: '骑手' }
  return map[role] || role
}

function riderTypeLabel(type) {
  return type === 'FULL_TIME' ? '全职骑手' : '兼职骑手'
}

async function fetchProfile() {
  try {
    const data = await request.get('/auth/profile')
    Object.assign(profile, data)
    Object.assign(form, {
      phone: data.phone || '',
      address: data.address || '',
      riderType: data.riderType || 'PART_TIME',
    })
    auth.setProfile(data)
  } catch { /* handled */ }
}

async function handleSave() {
  saving.value = true
  try {
    await request.put('/auth/profile', { ...form })
    ElMessage.success('保存成功')
    Object.assign(profile, form)
    auth.setProfile(form)
  } catch { /* handled */ }
  saving.value = false
}

onMounted(fetchProfile)
</script>
