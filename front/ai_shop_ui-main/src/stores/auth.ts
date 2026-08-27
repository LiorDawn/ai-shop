import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import router from '../router'
import {
  sendCode as sendCodeApi,
  loginByPwd,
  registerByCode,
  resetPwd,
  type SendCodeDTO,
  type LoginByPwdDTO,
  type RegisterByCodeDTO,
  type ResetPwdDTO,
  type UserDTO,
} from '../api/auth'

export const useAuthStore = defineStore('auth', () => {
  const token = ref<string>('')
  const user = ref<UserDTO | null>(null)

  const isLoggedIn = computed(() => !!token.value && !!user.value)
  const isMerchant = computed(() => user.value?.roleCode === 'MERCHANT')
  const isAdmin = computed(() => user.value?.roleCode === 'SUPER_ADMIN' || user.value?.roleCode === 'ADMIN')
  const userName = computed(() => user.value?.username || user.value?.phone || '')

  /**
   * 账号密码登录（手机/邮箱）
   */
  async function loginByAccount(data: LoginByPwdDTO) {
    const res: any = await loginByPwd(data)
    const loginUser = res.data.user
    token.value = res.data.token
    user.value = loginUser
    return loginUser
  }

  /**
   * 验证码注册（手机/邮箱）
   */
  async function registerByAccount(data: RegisterByCodeDTO) {
    const res: any = await registerByCode(data)
    return res
  }

  /**
   * 发送验证码（手机/邮箱）
   */
  async function sendVerifyCode(data: SendCodeDTO) {
    await sendCodeApi(data)
  }

  /**
   * 重置密码
   */
  async function resetPassword(data: ResetPwdDTO) {
    const res: any = await resetPwd(data)
    return res
  }

  function logout() {
    if (!token.value && !user.value) return  // 已退出，避免重复
    token.value = ''
    user.value = null
    ElMessage.success('已退出登录')
    router.push('/login')
  }

  return {
    token,
    user,
    isLoggedIn,
    isMerchant,
    isAdmin,
    userName,
    loginByAccount,
    registerByAccount,
    sendVerifyCode,
    resetPassword,
    logout,
  }
}, {
  persist: {
    storage: sessionStorage,
    pick: ['token', 'user'],
  },
})