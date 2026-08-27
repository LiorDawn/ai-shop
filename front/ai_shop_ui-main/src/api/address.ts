import request from './request'

export interface AddressVO {
  id: number
  consignee: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: number
  createTime?: string
}

/** 获取地址列表 */
export function getAddressList() {
  return request.get<AddressVO[]>('/profile/addresses')
}

/** 新增地址 */
export function addAddress(data: {
  consignee: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: number
}) {
  return request.post('/profile/addresses', data)
}