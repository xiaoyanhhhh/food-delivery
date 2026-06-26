export function assetUrl(value, fallback = '') {
  if (!value) return fallback

  const url = String(value).trim()
  if (!url) return fallback
  if (/^(https?:)?\/\//i.test(url) || url.startsWith('data:') || url.startsWith('blob:')) {
    return url
  }

  if (url.startsWith('/uploads/')) {
    const uploadOrigin = import.meta.env.VITE_UPLOAD_ORIGIN || import.meta.env.VITE_API_ORIGIN
    if (import.meta.env.DEV) {
      return `${uploadOrigin || 'http://localhost:8080'}${url}`
    }
    return url
  }

  return url
}
