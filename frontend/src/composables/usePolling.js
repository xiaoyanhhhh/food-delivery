import { onMounted, onUnmounted } from 'vue'
export function usePolling(fn, intervalMs = 15000) {
  let timer = null
  onMounted(() => { fn(); timer = setInterval(fn, intervalMs) })
  onUnmounted(() => { if (timer) clearInterval(timer) })
}
