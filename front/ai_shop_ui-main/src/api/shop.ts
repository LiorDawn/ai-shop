import request from './request'
import type { ProductDTO } from './product'

export interface ShopDTO {
  id: number
  merchantId: number
  merchantName: string
  shopName: string
  shopLogo: string
  intro: string
  status: number // 0关闭 1营业
  createTime: string
}

export interface ShopDetailDTO {
  shop: ShopDTO
  productCount: number
  followerCount: number
  followed: boolean
}

export interface ShopCategoryDTO {
  id: number
  name: string
}

// ===== 管理端 API =====
export function getShopsPage(params: {
  current: number
  size: number
  shopName?: string
  status?: number
}) {
  return request.get('/admin/shop/page', { params })
}

export function listShops() {
  return request.get('/admin/shop/list')
}

export function updateShopStatus(id: number, status: number) {
  return request.put(`/admin/shop/status/${id}?status=${status}`)
}

export function deleteShop(id: number) {
  return request.delete(`/admin/shop/${id}`)
}

export function deleteBatchShops(ids: number[]) {
  return request.delete('/admin/shop/batch', { data: ids })
}

// ===== 用户端店铺页 API =====

/** 获取店铺详情（含商品数、关注数、关注状态） */
export function getShopDetail(id: number) {
  return request.get<ShopDetailDTO>(`/shop/${id}`)
}

/** 关注店铺 */
export function followShop(id: number) {
  return request.post(`/shop/follow/${id}`)
}

/** 取消关注 */
export function unfollowShop(id: number) {
  return request.delete(`/shop/follow/${id}`)
}

/** 检查是否已关注 */
export function checkFollowed(id: number) {
  return request.get<boolean>(`/shop/follow/check/${id}`)
}

/** 获取店铺内商品分类 */
export function getShopCategories(id: number) {
  return request.get<ShopCategoryDTO[]>(`/shop/${id}/categories`)
}

/** 店铺内商品搜索 */
export function getShopProducts(id: number, params: {
  current: number
  size: number
  keyword?: string
  categoryId?: number
  sort?: number
}) {
  return request.get(`/shop/${id}/products`, { params })
}