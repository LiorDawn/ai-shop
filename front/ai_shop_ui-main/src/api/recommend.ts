import request from './request'
import type { ProductDTO } from './product'

/** 猜你喜欢 */
export function guessYouLike(limit: number = 12) {
  return request.get<ProductDTO[]>('/recommend/guess', { params: { limit }, skipAuthRedirect: true } as any)
}

/** 看了又看 */
export function alsoViewed(productId: number, limit: number = 8) {
  return request.get<ProductDTO[]>(`/recommend/also-viewed/${productId}`, { params: { limit }, skipAuthRedirect: true } as any)
}

/** 买了又买 */
export function alsoBought(productId: number, limit: number = 8) {
  return request.get<ProductDTO[]>(`/recommend/also-bought/${productId}`, { params: { limit }, skipAuthRedirect: true } as any)
}

/** 同类商品 */
export function similarProducts(productId: number, limit: number = 8) {
  return request.get<ProductDTO[]>(`/recommend/similar/${productId}`, { params: { limit }, skipAuthRedirect: true } as any)
}