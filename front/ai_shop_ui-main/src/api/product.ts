import request from './request'

export interface ProductImageDTO {
  id: number
  imageUrl: string
}

export interface ProductSkuDTO {
  id: number
  spec: string
  price: number
  stock: number
}

export interface ProductDTO {
  id: number
  name: string
  categoryId: number
  categoryName: string
  shopId: number
  merchantId?: number
  shopName: string
  price: number
  image: string // 主图
  status: number // 0下架 1上架
  description: string
  stock: number
  createTime: string
  sales?: number
  imageList?: ProductImageDTO[]
  skuList?: ProductSkuDTO[]
}

export interface ProductQueryParams {
  current: number
  size: number
  name?: string
  categoryId?: number
  shopId?: number
  status?: number
}

export interface RecommendParams {
  current: number
  size: number
  categoryId?: number
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

export function getProductsPage(params: ProductQueryParams): Promise<PageResult<ProductDTO>> {
  return request.get('/product/page', { params, skipAuthRedirect: true } as any)
}

export function upProduct(id: number): Promise<void> {
  return request.put(`/product/up/${id}`)
}

export function downProduct(id: number): Promise<void> {
  return request.put(`/product/down/${id}`)
}

export function deleteProduct(id: number): Promise<void> {
  return request.delete(`/product/${id}`)
}

export function deleteBatchProducts(ids: number[]): Promise<void> {
  return request.delete('/product/batch', { data: ids })
}

/** 获取商品详情 */
export function getProductDetail(id: number): Promise<ProductDTO> {
  return request.get<ProductDTO>(`/product/${id}`, { skipAuthRedirect: true } as any)
}

/** 获取商品评价 */
export function getProductComments(current: number, size: number, productId: number): Promise<any> {
  return request.get('/comment/product/' + productId, { params: { current, size }, skipAuthRedirect: true } as any)
}

/** 个性化推荐（销量×新鲜度评分） */
export function getRecommendProducts(params: RecommendParams): Promise<any> {
  return request.get('/product/recommend', { params, skipAuthRedirect: true } as any)
}

/** 热门商品排行（销量排行，Redis ZSet） */
export function getHotProducts(limit: number = 10): Promise<any> {
  return request.get('/product/hot', { params: { limit }, skipAuthRedirect: true } as any)
}