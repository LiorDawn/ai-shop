import request from './request'

export interface FlashSaleItem {
  id: number
  productId: number
  productName: string
  productImage: string
  flashPrice: number
  originalPrice: number
  stock: number
  startTime: string
  endTime: string
  limitPerUser: number
}

export interface FlashSaleDetail extends FlashSaleItem {
  status: number
}

export interface SeckillUrlResult {
  url: string
  expireIn: string
}

export interface SeckillResult {
  requestId: string
  message: string
}

export interface PollResult {
  status: string
  orderId?: string
}

/** 秒杀活动列表 */
export function getFlashSaleList() {
  return request.get<FlashSaleItem[]>('/flash-sale/list', { skipAuthRedirect: true } as any)
}

/** 秒杀商品详情 */
export function getFlashSaleDetail(id: number) {
  return request.get<FlashSaleDetail>(`/flash-sale/detail/${id}`)
}

/** 获取秒杀地址（签名验证） */
export function getSeckillUrl(id: number, verifyCode: string, sign: string) {
  return request.post<SeckillUrlResult>(`/flash-sale/url/${id}`, null, {
    params: { verifyCode, sign },
  })
}

/** 执行秒杀 */
export function executeSeckill(id: number, token: string) {
  return request.post<SeckillResult>(`/flash-sale/execute/${id}`, null, {
    params: { token },
  })
}

/** 轮询秒杀结果 */
export function pollSeckillResult(requestId: string) {
  return request.get<PollResult>(`/flash-sale/result/${requestId}`)
}