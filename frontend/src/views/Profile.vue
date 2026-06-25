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
          <el-descriptions-item label="昵称">{{ profile.nickname || '未设置' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card style="margin-top: 16px">
        <template #header><h3>修改信息</h3></template>
        <el-form :model="form" label-width="80px">
          <el-form-item label="昵称">
            <el-input v-model="form.nickname" placeholder="设置昵称" />
          </el-form-item>
          <el-form-item label="头像链接">
            <el-input v-model="form.avatar" placeholder="https://..." />
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="form.phone" placeholder="手机号" />
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
import { ref, reactive } from 'vue'
import { useAuthStore } from '../stores/auth'
import request from '../api/request'
import { ElMessage } from 'element-plus'

const auth = useAuthStore()
const saving = ref(false)

const profile = reactive({
  phone: auth.user?.phone || '',
  nickname: auth.user?.nickname || '',
  avatar: auth.user?.avatar || '',
})

const form = reactive({
  nickname: profile.nickname,
  avatar: profile.avatar,
  phone: profile.phone,
})

function roleLabel(role) {
  const map = { CUSTOMER: '顾客', MERCHANT: '商家', RIDER: '骑手' }
  return map[role] || role
}

async function handleSave() {
  saving.value = true
  try {
    // Simple profile update via auth/update endpoint - using the request interceptor
    await request.put('/auth/profile', { ...form })
    ElMessage.success('保存成功')
    Object.assign(profile, form)
  } catch { /* handled */ }
  saving.value = false
}
</script>
