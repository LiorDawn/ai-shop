import request from './request'

export interface AfterSaleDTO {
  id: number
  orderId: number
  orderItemId: number
  type: number
  typeText: string
  amount: number
  reason: string
  auditStatus: number
  statusText: string
  createTime: string
  orderNo: string
  username: string
  productName: string
  productImage: string
  spec: string
  num: number
}

export interface AfterSaleDetailDTO {
  id: number
  orderId: number
  orderNo: string
  username: string
  orderCreateTime: string
  orderTotalPrice: number
  type: number
  typeText: string
  amount: number
  reason: string
  description: string
  images: string
  auditStatus: number
  statusText: string
  createTime: string
  auditBy: string
  auditTime: string
  auditRemark: string
  items: AfterSaleItemDTO[]
}

export interface AfterSaleItemDTO {
  id: number
  productId: number
  productName: string
  productImage: string
  spec: string
  price: number
  num: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export function getAfterSalePage(params: {
  current?: number
  size?: number
  orderNo?: string
  auditStatus?: number
  startTime?: string
  endTime?: string
}) {
  return request.get<PageResult<AfterSaleDTO>>('/admin/after-sale/list', { params })
}

export function getAfterSaleDetail(id: number) {
  return request.get<AfterSaleDetailDTO>(`/admin/after-sale/${id}`)
}

export function adminAuditAfterSale(data: { id: number; auditStatus: number; auditRemark: string }) {
  return request.post('/admin/after-sale/audit', data)
}

// ===== 用户端 API =====

/** 申请售后 */
export function applyAfterSale(data: {
  orderId: number
  orderItemId: number
  type: number
  amount: number
  reason: string
  description?: string
  images?: string
}) {
  return request.post('/after-sale/apply', null, { params: data })
}

/** 我的售后列表 */
export function getMyAfterSales(params: {
  current?: number
  size?: number
  auditStatus?: number
}) {
  return request.get<PageResult<AfterSaleDTO>>('/after-sale/list', { params })
}

/** 用户端获取售后详情 */
export function getMyAfterSaleDetail(id: number) {
  return request.get<AfterSaleDetailDTO>(`/after-sale/${id}`)
}

/** 撤销售后申请（用户端） */
export function cancelAfterSale(id: number) {
  return request.post(`/after-sale/cancel/${id}`)
}

/** 填写退货物流信息 */
export function submitReturnLogistics(id: number, data: { expressCompany: string; expressNo: string }) {
  return request.post(`/after-sale/return-logistics/${id}`, null, { params: data })
}

/** 确认收货完成退款 */
export function finishAfterSale(id: number) {
  return request.post(`/admin/after-sale/finish/${id}`)
}

// ===== 商家端 API =====

/** 商家获取售后列表 */
export function getMerchantAfterSales(params: {
  current?: number
  size?: number
  auditStatus?: number
  type?: number
  orderNo?: string
  startTime?: string
  endTime?: string
}) {
  return request.get<PageResult<AfterSaleDTO>>('/merchant/after-sale/list', { params })
}

/** 商家获取售后详情 */
export function getMerchantAfterSaleDetail(id: number) {
  return request.get<AfterSaleDetailDTO>(`/merchant/after-sale/${id}`)
}

/** 商家处理售后 - 同意/拒绝 */
export function merchantAuditAfterSale(data: { id: number; auditStatus: number; auditRemark: string; returnAddress?: string }) {
  return request.post('/merchant/after-sale/audit', null, { params: data })
}

/** 商家确认收货完成退款 */
export function merchantFinishAfterSale(id: number) {
  return request.post(`/merchant/after-sale/finish/${id}`)
}