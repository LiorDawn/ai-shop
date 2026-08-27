import request from './request'

export interface User {
  id?: number
  username: string
  password?: string
  phone?: string
  email?: string
  avatar?: string
  roleId?: number
  status?: number
  createTime?: string
  updateTime?: string
}

export interface UserDTO {
  id: number
  username: string
  phone: string
  email: string
  roleId: number
  roleName: string
  roleCode: string
  status: number
  createTime: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  current: number
  size: number
  pages: number
}

/**
 * 分页查询用户
 */
export function getUsersPage(params: {
  current?: number
  size?: number
  username?: string
  phone?: string
}) {
  return request.get<PageResult<UserDTO>>('/user/page', { params })
}

/**
 * 查询所有用户
 */
export function listUsers() {
  return request.get<UserDTO[]>('/user/list')
}

/**
 * 根据ID查询用户
 */
export function getUserById(id: number) {
  return request.get<UserDTO>(`/user/${id}`)
}

/**
 * 新增用户
 */
export function addUser(data: User) {
  return request.post('/user', data)
}

/**
 * 修改用户
 */
export function updateUser(data: User) {
  return request.put('/user', data)
}

/**
 * 修改用户状态
 */
export function updateUserStatus(id: number, status: number) {
  return request.put(`/user/status/${id}?status=${status}`)
}

/**
 * 删除用户
 */
export function deleteUser(id: number) {
  return request.delete(`/user/${id}`)
}

/**
 * 批量删除用户
 */
export function deleteBatchUsers(ids: number[]) {
  return request.delete('/user/batch', { data: ids })
}