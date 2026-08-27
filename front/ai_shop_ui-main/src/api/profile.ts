import request from './request'
import type { PageResult } from './user'

// ===== 用户信息接口 =====

export interface UserProfileDTO {
  id: number
  username: string
  nickname: string
  gender: number
  signature: string
  phone: string
  email: string
  avatar: string
  roleId: number
  roleName: string
  roleCode: string
  status: number
  createTime: string
}

export interface UserProfileUpdateDTO {
  nickname?: string
  gender?: number
  signature?: string
  avatar?: string
}

export interface PasswordChangeDTO {
  oldPassword: string
  newPassword: string
  confirmPassword: string
}

// ===== 地址接口 =====

export interface Address {
  id?: number
  userId?: number
  receiver: string
  phone: string
  address: string
  isDefault?: number
}

// ===== 收藏接口 =====

export interface CollectProductDTO {
  id: number
  productId: number
  productName: string
  productImage: string
  price: number
  shopId: number
  shopName: string
  status: number
  createTime: string
}

// ===== 用户统计 =====

export interface ProfileStats {
  orderCount: number
  pendingPayCount: number
  pendingShipCount: number
  pendingReceiveCount: number
  pendingReviewCount: number
  collectCount: number
  followShopCount: number
}

// ===== 用户个人资料 =====

/** 获取当前用户信息 */
export function getProfile(): Promise<UserProfileDTO> {
  return request.get<UserProfileDTO>('/profile')
}

/** 更新个人资料 */
export function updateProfile(data: UserProfileUpdateDTO): Promise<void> {
  return request.put('/profile', data)
}

/** 修改密码 */
export function changePassword(data: PasswordChangeDTO): Promise<void> {
  return request.put('/profile/password', data)
}

// ===== 收货地址 =====

/** 地址列表 */
export function listAddresses(): Promise<Address[]> {
  return request.get<Address[]>('/profile/addresses')
}

/** 新增地址 */
export function addAddress(data: Address): Promise<void> {
  return request.post('/profile/addresses', data)
}

/** 修改地址 */
export function updateAddress(data: Address): Promise<void> {
  return request.put('/profile/addresses', data)
}

/** 删除地址 */
export function deleteAddress(id: number): Promise<void> {
  return request.delete(`/profile/addresses/${id}`)
}

/** 获取默认地址 */
export function getDefaultAddress(): Promise<Address> {
  return request.get<Address>('/profile/addresses/default')
}

// ===== 我的收藏 =====

/** 收藏商品列表（分页） */
export function listCollectProducts(current: number = 1, size: number = 20): Promise<PageResult<CollectProductDTO>> {
  return request.get<PageResult<any>>('/profile/collects', {
    params: { current, size },
  })
}

/** 收藏商品ID列表 */
export function getCollectIds(): Promise<number[]> {
  return request.get<number[]>('/profile/collects/ids')
}

/** 获取用户统计数据 */
export function getProfileStats(): Promise<ProfileStats> {
  return request.get<ProfileStats>('/profile/stats')
}