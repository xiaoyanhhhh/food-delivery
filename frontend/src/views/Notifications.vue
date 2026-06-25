<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-title">
        <el-icon :size="22"><BellFilled /></el-icon> 消息通知
      </h1>
      <el-button v-if="notifications.length > 0" size="small" @click="handleMarkAll">全部已读</el-button>
    </div>
    <el-empty v-if="notifications.length === 0" description="暂无通知" />
    <el-card v-for="n in notifications" :key="n.id" class="notif-card" :class="{ unread: !n.isRead }">
      <div class="notif-header">
        <h3 :style="{ fontWeight: n.isRead ? 'normal' : 'bold' }">{{ n.title }}</h3>
        <el-tag v-if="!n.isRead" type="danger" size="small">未读</el-tag>
        <span class="time">{{ formatTime(n.createdAt) }}</span>
      </div>
      <p class="content">{{ n.content }}</p>
      <el-button v-if="!n.isRead" link type="primary" size="small" @click="handleRead(n.id)">标为已读</el-button>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getNotifications, markRead, markAllRead } from '../api/notification'
import { ElMessage } from 'element-plus'

const notifications = ref([])

async function fetch() {
  try {
    const data = await getNotifications({ page: 0, size: 50 })
    notifications.value = data.content || data || []
  } catch { notifications.value = [] }
}

async function handleRead(id) {
  try { await markRead(id); fetch() } catch { /* handled */ }
}

async function handleMarkAll() {
  try { await markAllRead(); ElMessage.success('全部已读'); fetch() } catch { /* handled */ }
}

function formatTime(t) {
  return t ? t.replace('T', ' ').substring(0, 16) : ''
}

onMounted(fetch)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header .page-title { margin-bottom: 0; }
.notif-card { margin-bottom: 12px; }
.notif-card.unread { border-left: 3px solid #f56c6c; }
.notif-header { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.time { color: #c0c4cc; font-size: 12px; margin-left: auto; }
.content { color: #606266; line-height: 1.6; }
</style>
