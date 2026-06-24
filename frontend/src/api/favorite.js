import request from './request'
export const getFavorites = (p) => request.get('/favorites', { params: p })
export const checkFavorite = (storeId) => request.get(`/favorites/check/${storeId}`)
export const addFavorite = (storeId) => request.post(`/favorites/${storeId}`)
export const removeFavorite = (storeId) => request.delete(`/favorites/${storeId}`)
