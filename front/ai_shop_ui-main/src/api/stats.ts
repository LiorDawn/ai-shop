import request from './request'

export interface ProductSalesRankVO {
  productId: number
  productName: string
  productImage: string
  totalSales: number
}

export interface StatsDTO {
  totalSales: number
  totalOrders: number
  totalUsers: number
  couponUsageRate: string
  afterSaleRate: string
  productSalesRank: ProductSalesRankVO[]
}

export function getStatsSummary() {
  return request.get('/admin/stats/summary')
}