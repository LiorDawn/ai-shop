import axios from 'axios'
import { useAuthStore } from '../stores/auth'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

// 请求拦截器 - 添加 token
request.interceptors.request.use(
  (config) => {
    const auth = useAuthStore()
    if (auth.token) {
      config.headers.token = auth.token
    }
    return config
  },
  (error) => Promise.reject(error)
)

// 响应拦截器 - 统一处理
request.interceptors.response.use(
  (response) => {
    // 保存后端返回的新 token（LoginInterceptor 会在每次请求时刷新）
    const newToken = response.headers['newtoken']
    if (newToken) {
      const auth = useAuthStore()
      auth.token = newToken
    }

    const res = response.data
    if (res.code !== 200) {
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res
  },
  (error) => {
    if (error.response?.status === 401) {
      if ((error.config as any)?.skipAuthRedirect) {
        return Promise.reject(error)
      }
      const auth = useAuthStore()
      auth.logout()  // 内部已做重复调用保护
      // 已登录页不再强制刷新，避免 AIChat 等组件产生无限循环
      if (window.location.hash !== '#/login') {
        window.location.href = '/#/login'
      }
    }
    return Promise.reject(error)
  }
)

export default request