import request from './request'

export interface Role {
  id: number
  name: string
  code: string
}

/**
 * 查询所有角色
 */
export function listRoles() {
  return request.get<Role[]>('/role/list')
}