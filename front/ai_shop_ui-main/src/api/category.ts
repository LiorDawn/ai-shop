import request from './request'

export interface Category {
  id: number
  name: string
  parentId: number
  sort: number
}

export function listCategories() {
  return request.get('/category/list', { skipAuthRedirect: true } as any)
}

export function addCategory(data: { name: string; parentId?: number; sort?: number }) {
  return request.post('/category', data)
}

export function updateCategory(data: Category) {
  return request.put('/category', data)
}

export function deleteCategory(id: number) {
  return request.delete(`/category/${id}`)
}