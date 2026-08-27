import request from './request'

export interface OrderDTO {
  id: number
  orderNo: string
  username: string
  shopName: string
  totalPrice: number
  couponPrice: number
  actualPrice: number
  payType: number
  payStatus: number
  orderStatus: number
  logistics: string
  createTime: string
}

export interface OrderItemDTO {
  id: number
  productId: number
  productName: string
  productImage: string
  spec: string
  shopId: number
  shopName: string
  num: number
  price: number
  subtotal: number
  /** 售后单ID，null表示未申请 */
  afterSaleId?: number
  /** 是否已评价 */
  hasComment?: boolean
}

export interface OrderDetailDTO {
  id: number
  orderNo: string
  username: string
  totalPrice: number
  couponPrice: number
  actualPrice: number
  payType: number
  payStatus: number
  orderStatus: number
  logistics: string
  createTime: string
  receiver: string
  receiverPhone: string
  address: string
  items: OrderItemDTO[]
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export interface OrderQueryParams {
  current?: number
  size?: number
  orderNo?: string
  orderStatus?: number
  shopId?: number
  startTime?: string
  endTime?: string
}

export interface MyOrderQueryParams {
  current?: number
  size?: number
  orderNo?: string
  orderStatus?: number
}

export interface CreateOrderParams {
  addressId: number
  productIds: string
  remark?: string
  couponId?: number
}

export function getOrdersPage(params: OrderQueryParams): Promise<PageResult<OrderDTO>> {
  return request.get<PageResult<OrderDTO>>('/admin/order/page', { params })
}

export function getOrderDetail(id: number): Promise<OrderDetailDTO> {
  return request.get<OrderDetailDTO>(`/admin/order/${id}`)
}

export function deliverOrder(id: number, logistics: string): Promise<void> {
  return request.put(`/admin/order/deliver/${id}?logistics=${encodeURIComponent(logistics)}`)
}

export function cancelOrder(id: number): Promise<void> {
  return request.put(`/admin/order/cancel/${id}`)
}

// ===== 用户端 API =====

/** 我的订单列表 */
export function getMyOrders(params: MyOrderQueryParams): Promise<PageResult<OrderDTO>> {
  return request.get<PageResult<OrderDTO>>('/order/page', { params })
}

/** 我的订单详情 */
export function getMyOrderDetail(id: number): Promise<OrderDetailDTO> {
  return request.get<OrderDetailDTO>(`/order/${id}`)
}

/** 取消订单（用户端） */
export function cancelMyOrder(id: number): Promise<void> {
  return request.put(`/order/cancel/${id}`)
}

/** 确认收货 */
export function confirmReceive(id: number): Promise<void> {
  return request.put(`/order/confirm/${id}`)
}

/** 创建订单（从购物车结算） */
export function createOrder(addressId: number, productIds: string, remark?: string, couponId?: number): Promise<number> {
  const params: CreateOrderParams = { addressId, productIds, remark }
  if (couponId) params.couponId = couponId
  return request.post<number>('/order/create', null, { params })
}

export function completeOrder(id: number): Promise<void> {
  return request.put(`/admin/order/complete/${id}`)
}

// ===== 支付宝支付 API =====

/** 生成支付宝支付表单（自动提交会跳转支付宝） */
export function alipayCreatePay(orderId: number): Promise<string> {
  return request.post<string>(`/pay/create/${orderId}`)
}

/** 模拟支付成功（开发环境用，跳过支付宝异步通知） */
export function simulatePay(orderId: number): Promise<void> {
  return request.post(`/pay/simulate/${orderId}`)
}

/** 主动查询支付宝支付状态（支付宝同步跳回后确认支付） */
export function queryPayStatus(orderId: number): Promise<boolean> {
  return request.post<boolean>(`/pay/query/${orderId}`)
}