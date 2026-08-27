import request from './request'

export interface CouponDTO {
  id: number
  name: string
  type: number
  minPrice: number
  discount: number
  stock: number
  remain: number
  claimed: boolean
  startTime: string
  endTime: string
  status: number
}

export interface CouponRecordVO {
  id: number
  userId: number
  username: string
  couponId: number
  couponName: string
  type: number       // 1满减 2折扣
  minPrice: number   // 满多少可用
  discount: number   // 减免金额/折扣
  startTime: string  // 生效时间
  endTime: string    // 过期时间
  status: number
  statusText: string
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export function getCouponPage(params: {
  current?: number
  size?: number
  name?: string
  status?: number
}) {
  return request.get<PageResult<CouponDTO>>('/admin/coupon/page', { params })
}

export function getCoupon(id: number) {
  return request.get<CouponDTO>(`/admin/coupon/${id}`)
}

export function addCoupon(data: any) {
  return request.post('/admin/coupon', data)
}

export function updateCoupon(data: any) {
  return request.put('/admin/coupon', data)
}

export function deleteCoupon(id: number) {
  return request.delete(`/admin/coupon/${id}`)
}

export function toggleCouponStatus(id: number) {
  return request.put(`/admin/coupon/toggle/${id}`)
}

export function getCouponRecords(params: {
  current?: number
  size?: number
  couponId?: number
}) {
  return request.get<PageResult<CouponRecordVO>>('/admin/coupon/records', { params })
}

// ===== 用户端优惠券 API =====

/** 获取可领取的优惠券列表 */
export function getAvailableCoupons(params: {
  current?: number
  size?: number
}) {
  return request.get<PageResult<CouponDTO>>('/coupon/available', { params })
}

/** 领取优惠券 */
export function receiveCoupon(couponId: number) {
  return request.post(`/coupon/receive/${couponId}`)
}

/** 我的优惠券列表 */
export function getMyCoupons(params: {
  current?: number
  size?: number
  status?: number  // 0未使用 1已使用 2已过期
}) {
  return request.get<PageResult<CouponRecordVO>>('/coupon/my', { params })
}

/** 我的可用优惠券 */
export function getMyAvailableCoupons() {
  return request.get<CouponDTO[]>('/coupon/my-available')
}