import request from './request'

export interface UserDTO {
  id: number
  username: string
  phone: string
  email: string
  roleId: number
  roleName: string
  roleCode: string
  shopId?: number
}

export interface LoginVO {
  token: string
  user: UserDTO
}

export interface SendCodeDTO {
  account: string
  type: number  // 1=手机 2=邮箱
}

export interface LoginByPwdDTO {
  account: string
  password: string
  type: number
}

export interface RegisterByCodeDTO {
  account: string
  code: string
  password: string
  type: number
}

export interface ResetPwdDTO {
  account: string
  code: string
  newPwd: string
  type: number
}

/**
 * 发送验证码（手机/邮箱）
 */
export function sendCode(data: SendCodeDTO) {
  return request.post('/user/sendCode', data)
}

/**
 * 账号密码登录（手机/邮箱）
 */
export function loginByPwd(data: LoginByPwdDTO) {
  return request.post<LoginVO>('/user/loginByPwd', data)
}

/**
 * 验证码注册（手机/邮箱）
 */
export function registerByCode(data: RegisterByCodeDTO) {
  return request.post<LoginVO>('/user/register', data)
}

/**
 * 重置密码
 */
export function resetPwd(data: ResetPwdDTO) {
  return request.post('/user/resetPwd', data)
}

/** 获取当前登录用户最新信息（角色变更后立即生效） */
export function getCurrentUser() {
  return request.get<UserDTO>('/user/current')
}