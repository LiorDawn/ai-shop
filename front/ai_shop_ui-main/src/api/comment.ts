import request from './request'

export interface CommentDTO {
  id: number
  orderId: number
  productId: number
  productName: string
  productImage: string
  shopId: number
  shopName: string
  userId: number
  username: string
  userAvatar: string
  content: string
  images: string
  imageList: string[]
  score: number
  status: number
  reply: string
  replyTime: string
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

// ===== 管理员 API =====
export function getCommentPage(params: {
  current?: number
  size?: number
  productName?: string
  score?: number
  status?: number
  startTime?: string
  endTime?: string
}) {
  return request.get<PageResult<CommentDTO>>('/admin/comment/list', { params })
}

export function getCommentDetail(id: number) {
  return request.get<CommentDTO>(`/admin/comment/${id}`)
}

export function replyComment(id: number, reply: string) {
  return request.post('/admin/comment/reply', null, { params: { id, reply } })
}

export function toggleCommentStatus(id: number, status: number) {
  return request.put(`/admin/comment/status/${id}`, null, { params: { status } })
}

export function deleteComment(id: number) {
  return request.delete(`/admin/comment/${id}`)
}

// ===== 用户端 API =====
export function getProductComments(current: number, size: number, productId: number) {
  return request.get<PageResult<CommentDTO>>(`/comment/product/${productId}`, { params: { current, size } })
}

export function addComment(data: {
  orderId: number
  productId: number
  shopId: number
  score: number
  content?: string
  images?: string
}) {
  return request.post('/comment/add', null, { params: data })
}

/** 我的评论 */
export function getMyComments(current: number, size: number) {
  return request.get<PageResult<CommentDTO>>('/comment/self', { params: { current, size } })
}