import request from './request'

export interface CartItemVO {
  id: number
  userId: number
  productId: number
  skuId?: number
  spec?: string
  productName: string
  productImage: string
  price: number
  stock: number
  num: number
  checked: number      // 0未选中 1选中
  productStatus: number // 1上架 0下架
  shopId: number
  shopName: string
  subtotal: number
  createTime: string
}

export interface ShopCartGroup {
  shopId: number
  shopName: string
  items: CartItemVO[]
}

export interface CartSettleVO {
  shopGroups: ShopCartGroup[]
  totalNum: number
  totalPrice: number
  crossShop: boolean
}

export interface AddCartDTO {
  productId: number
  skuId?: number
  num: number
}

/** 加入购物车 */
export function addToCart(data: AddCartDTO): Promise<void> {
  return request.post('/cart/add', data)
}

/** 查询购物车列表 */
export function getCartList(): Promise<CartItemVO[]> {
  return request.get<CartItemVO[]>('/cart/list')
}

/** 修改数量 */
export function updateCartNum(id: number, num: number): Promise<void> {
  return request.put(`/cart/${id}/num?num=${num}`)
}

/** 选中/取消选中 */
export function toggleCartCheck(id: number, checked: number): Promise<void> {
  return request.put(`/cart/${id}/check?checked=${checked}`)
}

/** 全选/全不选 */
export function checkAllCart(checked: number): Promise<void> {
  return request.put(`/cart/check-all?checked=${checked}`)
}

/** 删除单个 */
export function deleteCartItem(id: number): Promise<void> {
  return request.delete(`/cart/${id}`)
}

/** 批量删除 */
export function deleteCartBatch(ids: number[]): Promise<void> {
  return request.delete('/cart/batch', { data: ids })
}

/** 结算前置校验 */
export function settleCheck(): Promise<CartSettleVO> {
  return request.post<CartSettleVO>('/cart/settle-check')
}