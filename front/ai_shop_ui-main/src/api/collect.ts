import request from './request'

/** 收藏商品 */
export function addCollect(productId: number) {
  return request.post(`/collect/add/${productId}`)
}

/** 取消收藏 */
export function removeCollect(productId: number) {
  return request.delete(`/collect/remove/${productId}`)
}

/** 检查是否已收藏 */
export function checkCollected(productId: number) {
  return request.get<boolean>(`/collect/check/${productId}`)
}

/** 获取收藏的商品ID列表 */
export function getCollectedIds() {
  return request.get<number[]>('/collect/ids')
}