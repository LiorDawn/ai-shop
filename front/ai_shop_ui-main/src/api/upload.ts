import request from './request'

/**
 * 上传图片
 * @param file 图片文件
 * @param imageType 图片类型（如 avatar, product, banner）
 */
export function uploadImage(file: File, imageType: string = 'avatar') {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('imageType', imageType)
  return request.post('/upload/image', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  })
}