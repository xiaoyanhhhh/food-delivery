import request from './request'
export const getNotifications = (p) => request.get('/notifications', { params: p })
export const getUnreadCount = () => request.get('/notifications/unread-count')
export const markRead = (id) => request.put(`/notifications/${id}/read`)
export const markAllRead = () => request.put('/notifications/read-all')
