import request from './request'

export interface MerchantApplyDTO {
  merchantName: string
  licenseNo: string
  contact: string
  phone: string
}

export interface ShopInfoUpdateDTO {
  shopName?: string
  shopLogo?: string
  intro?: string
}

export interface MerchantPasswordChangeDTO {
  oldPassword: string
  newPassword: string
}

export interface CommentQueryParams {
  current: number
  size: number
  score?: number
  productName?: string
  hasReply?: number
}

export interface MerchantQueryParams {
  current: number
  size: number
  status?: number
  merchantName?: string
}

export interface AuditDTO {
  status: number
  auditRemark: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export interface StatsOverviewDTO {
  totalSales: number
  totalOrders: number
  totalProducts: number
  todaySales: number
  todayOrders: number
}

export interface SalesRankingDTO {
  productId: number
  productName: string
  productImage: string
  salesCount: number
  salesAmount: number
}

export interface OrderTrendDTO {
  date: string
  orderCount: number
  salesAmount: number
}

export interface CommentDTO {
  id: number
  userId: number
  username: string
  userAvatar: string
  productId: number
  productName: string
  productImage: string
  orderId: number
  score: number
  content: string
  images: string
  reply: string
  replyTime: string
  status: number
  createTime: string
}

export interface MerchantDTO {
  id: number
  userId: number
  username: string
  phone: string
  merchantName: string
  licenseNo: string
  contact: string
  auditStatus: number
  auditRemark: string
  auditTime: string
  status: number
  createTime: string
  shop?: {
    id: number
    shopName: string
    shopLogo: string
    intro: string
    status: number
  }
}

// ===== 用户端：入驻申请 =====
export function submitMerchantApply(data: MerchantApplyDTO): Promise<void> {
  return request.post('/merchant/apply', data)
}

export function getMerchantApplyStatus(): Promise<any> {
  return request.get('/merchant/apply/status')
}

// ===== 店铺设置 =====
export function getMerchantShopInfo(): Promise<any> {
  return request.get('/merchant/shop/info')
}

export function updateMerchantShopInfo(data: ShopInfoUpdateDTO): Promise<void> {
  return request.put('/merchant/shop/info', data)
}

export function toggleShopStatus(status: number): Promise<void> {
  return request.put('/merchant/shop/status', { status })
}

export function getMerchantProfile(): Promise<any> {
  return request.get('/merchant/shop/profile')
}

export function changeMerchantPassword(data: MerchantPasswordChangeDTO): Promise<void> {
  return request.put('/merchant/shop/password', data)
}

// ===== 数据统计 =====
export function getMerchantStatsOverview(): Promise<StatsOverviewDTO> {
  return request.get('/merchant/stats/overview')
}

export function getMerchantSalesRanking(): Promise<SalesRankingDTO[]> {
  return request.get('/merchant/stats/sales-ranking')
}

export function getMerchantOrderTrend(): Promise<OrderTrendDTO[]> {
  return request.get('/merchant/stats/order-trend')
}

// ===== 评价管理 =====
export function getMerchantCommentsPage(params: CommentQueryParams): Promise<PageResult<CommentDTO>> {
  return request.get('/merchant/comment/page', { params })
}

export function getMerchantCommentDetail(id: number): Promise<CommentDTO> {
  return request.get(`/merchant/comment/${id}`)
}

export function replyMerchantComment(id: number, reply: string): Promise<void> {
  return request.put(`/merchant/comment/reply/${id}`, { reply })
}

export function toggleCommentStatus(id: number, status: number): Promise<void> {
  return request.put(`/merchant/comment/toggle-status/${id}`, { status })
}

// ===== 管理员端：商家管理 =====
export function getMerchantsPage(params: MerchantQueryParams): Promise<PageResult<MerchantDTO>> {
  return request.get('/admin/merchant/page', { params })
}

export function getMerchantDetail(id: number): Promise<MerchantDTO> {
  return request.get(`/admin/merchant/${id}`)
}

export function auditMerchant(id: number, status: number, auditRemark: string): Promise<void> {
  return request.put(`/admin/merchant/audit/${id}`, { status, auditRemark })
}

export function deleteMerchant(id: number): Promise<void> {
  return request.delete(`/admin/merchant/${id}`)
}

export function deleteBatchMerchants(ids: number[]): Promise<void> {
  return request.delete('/admin/merchant/batch', { data: { ids } })
}