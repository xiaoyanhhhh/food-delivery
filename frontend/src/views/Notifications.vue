<template>
  <div class="page-container">
    <div class="page-header">
      <h1 class="page-title">
        <el-icon :size="22"><BellFilled /></el-icon> 消息通知
      </h1>
      <el-button v-if="notifications.length > 0" size="small" plain @click="handleMarkAll">全部已读</el-button>
    </div>

    <el-empty v-if="notifications.length === 0" description="暂无通知" />

    <el-card v-for="notification in notifications" :key="notification.id"
      class="notif-card" :class="{ unread: !notification.isRead }">
      <div class="notif-header">
        <div class="title-row">
          <h3 :class="{ normal: notification.isRead }">{{ notification.title }}</h3>
          <el-tag v-if="!notification.isRead" type="danger" size="small">未读</el-tag>
        </div>
        <span class="time">{{ formatTime(notification.createdAt) }}</span>
      </div>

      <p class="content">{{ notification.content }}</p>

      <div v-if="!notification.isRead" class="notif-actions">
        <el-button type="primary" size="small" plain @click="handleRead(notification.id)">标为已读</el-button>
      </div>
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
  try {
    await markRead(id)
    fetch()
  } catch { /* handled */ }
}

async function handleMarkAll() {
  try {
    await markAllRead()
    ElMessage.success('全部已读')
    fetch()
  } catch { /* handled */ }
}

function formatTime(time) {
  return time ? time.replace('T', ' ').substring(0, 16) : ''
}

onMounted(fetch)
</script>

<style scoped>
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header .page-title { margin-bottom: 0; }
.notif-card { margin-bottom: 12px; }
.notif-card.unread { border-left: 3px solid #FF8C00; }
.notif-header { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; margin-bottom: 8px; }
.title-row { display: flex; align-items: center; gap: 10px; min-width: 0; }
.title-row h3 { margin: 0; font-size: 18px; font-weight: 700; color: #1f2937; }
.title-row h3.normal { font-weight: 500; }
.time { color: #c0c4cc; font-size: 12px; white-space: nowrap; }
.content { color: #606266; line-height: 1.6; margin: 0; }
.notif-actions { margin-top: 10px; }
.notif-actions :deep(.el-button) {
  min-width: auto;
  height: 28px;
  padding: 4px 12px;
  border-radius: 6px;
  background: #fff7ed;
  border-color: #FF8C00;
  color: #d97706;
}
</style>
