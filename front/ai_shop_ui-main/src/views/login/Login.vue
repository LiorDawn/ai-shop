<template>
  <div class="login-page">
    <!-- 通栏透明顶部导航 -->
    <div class="login-nav">
      <div class="login-nav-left">
        <div class="nav-menu">
          <a href="javascript:void(0)" @click="goHome">首页</a>
          <a href="javascript:void(0)">关于我们</a>
          <a href="javascript:void(0)">项目介绍</a>
          <a href="javascript:void(0)">加入我们</a>
        </div>
      </div>
      <div class="login-nav-right">
      </div>
    </div>

    <!-- 页面居中登录卡片 -->
    <div class="login-body">
      <div class="login-card">
        <!-- 头部：商城名称 + 标语 -->
        <div class="card-header">
          <div class="card-title">智汇购</div>
          <div class="card-subtitle">AI 赋能 · 开启智慧购物新体验</div>
        </div>

        <!-- Tab 切换 -->
        <el-tabs v-model="loginTab" class="login-tabs" @tab-change="handleLoginTabChange">
          <el-tab-pane label="手机号登录" name="phone" />
          <el-tab-pane label="邮箱登录" name="email" />
        </el-tabs>

        <el-form
          ref="formRef"
          :model="loginForm"
          :rules="loginRules"
          label-width="0"
          size="default"
          class="login-form"
        >
          <!-- 手机号输入 -->
          <el-form-item v-if="loginTab === 'phone'" prop="phone">
            <el-input
              v-model="loginForm.phone"
              placeholder="手机号"
              maxlength="11"
              clearable
            >
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 24 24" width="18" height="18" fill="#999"><path d="M17 1.01L7 1c-1.1 0-2 .9-2 2v18c0 1.1.9 2 2 2h10c1.1 0 2-.9 2-2V3c0-1.1-.9-1.99-2-1.99zM17 19H7V5h10v14z"/></svg>
              </template>
            </el-input>
          </el-form-item>

          <!-- 邮箱输入 -->
          <el-form-item v-if="loginTab === 'email'" prop="email">
            <el-input
              v-model="loginForm.email"
              placeholder="邮箱地址"
              clearable
            >
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 24 24" width="18" height="18" fill="#999"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"/></svg>
              </template>
            </el-input>
          </el-form-item>

          <!-- 密码输入 -->
          <el-form-item prop="password">
            <el-input
              v-model="loginForm.password"
              :type="showPassword ? 'text' : 'password'"
              placeholder="登录密码"
            >
              <template #prefix>
                <svg class="input-icon" viewBox="0 0 24 24" width="18" height="18" fill="#999"><path d="M18 8h-1V6c0-2.76-2.24-5-5-5S7 3.24 7 6v2H6c-1.1 0-2 .9-2 2v10c0 1.1.9 2 2 2h12c1.1 0 2-.9 2-2V10c0-1.1-.9-2-2-2zm-6 9c-1.1 0-2-.9-2-2s.9-2 2-2 2 .9 2 2-.9 2-2 2zm3.1-9H8.9V6c0-1.71 1.39-3.1 3.1-3.1 1.71 0 3.1 1.39 3.1 3.1v2z"/></svg>
              </template>
              <template #suffix>
                <span class="pwd-icon" @click="showPassword = !showPassword">
                  <el-icon><template v-if="showPassword"><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg></template><template v-else><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/></svg></template></el-icon>
                </span>
              </template>
            </el-input>
          </el-form-item>

          <el-form-item>
            <el-button :loading="loginLoading" class="login-btn" @click="handleLogin">
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <!-- 测试账号提示（仅手机号登录时显示） -->
        <div v-if="loginTab === 'phone'" class="test-account-tip">
          测试账号：<b>13800138005</b>　密码：<b>123456</b>
        </div>

        <div class="card-footer">
          <span class="card-link" @click="showRegister = true">立即注册</span>
          <span class="card-link" @click="showResetPwd = true">忘记密码？</span>
        </div>
      </div>
    </div>

    <!-- ===== 注册对话框 ===== -->
    <el-dialog
      v-model="showRegister"
      title="用户注册"
      width="420px"
      :close-on-click-modal="false"
      class="register-dialog"
    >
      <el-tabs v-model="registerTab" class="register-tabs">
        <el-tab-pane label="手机注册" name="phone" />
        <el-tab-pane label="邮箱注册" name="email" />
      </el-tabs>

      <el-form
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-width="0"
        size="default"
        class="register-form"
      >
        <!-- 手机号 -->
        <el-form-item v-if="registerTab === 'phone'" prop="phone">
          <el-input
            v-model="registerForm.phone"
            placeholder="手机号"
            maxlength="11"
            clearable
          />
        </el-form-item>

        <!-- 邮箱 -->
        <el-form-item v-if="registerTab === 'email'" prop="email">
          <el-input
            v-model="registerForm.email"
            placeholder="邮箱地址"
            clearable
          />
        </el-form-item>

        <!-- 验证码行 -->
        <el-form-item prop="code">
          <div class="code-row">
            <el-input v-model="registerForm.code" placeholder="验证码" maxlength="6" />
            <el-button
              :disabled="registerCodeBtnDisabled"
              :loading="registerCodeSending"
              class="code-btn"
              @click="handleRegisterSendCode"
            >
              {{ registerCodeBtnText }}
            </el-button>
          </div>
        </el-form-item>

        <!-- 密码 -->
        <el-form-item prop="password">
          <el-input
            v-model="registerForm.password"
            :type="registerShowPwd ? 'text' : 'password'"
            placeholder="密码"
          >
            <template #suffix>
              <span class="pwd-icon" @click="registerShowPwd = !registerShowPwd">
                <el-icon><template v-if="registerShowPwd"><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg></template><template v-else><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/></svg></template></el-icon>
              </span>
            </template>
          </el-input>
        </el-form-item>

        <!-- 确认密码 -->
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="registerForm.confirmPassword"
            :type="registerShowConfirmPwd ? 'text' : 'password'"
            placeholder="确认密码"
          >
            <template #suffix>
              <span class="pwd-icon" @click="registerShowConfirmPwd = !registerShowConfirmPwd">
                <el-icon><template v-if="registerShowConfirmPwd"><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg></template><template v-else><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/></svg></template></el-icon>
              </span>
            </template>
          </el-input>
        </el-form-item>

        <!-- 协议勾选框 -->
        <el-form-item prop="agreed">
          <el-checkbox v-model="registerForm.agreed" size="default">
            我已阅读并同意
            <el-link type="primary" :underline="false" href="javascript:void(0)">《用户服务协议》</el-link>
            和
            <el-link type="primary" :underline="false" href="javascript:void(0)">《隐私政策》</el-link>
          </el-checkbox>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showRegister = false">取消</el-button>
        <el-button type="primary" :loading="registerLoading" :disabled="!registerForm.agreed" @click="handleRegister">
          注册
        </el-button>
      </template>
    </el-dialog>

    <!-- ===== 重置密码对话框 ===== -->
    <el-dialog
      v-model="showResetPwd"
      title="重置密码"
      width="420px"
      :close-on-click-modal="false"
      class="reset-dialog"
    >
      <el-tabs v-model="resetTab" class="reset-tabs">
        <el-tab-pane label="手机找回" name="phone" />
        <el-tab-pane label="邮箱找回" name="email" />
      </el-tabs>

      <el-form
        ref="resetFormRef"
        :model="resetForm"
        :rules="resetRules"
        label-width="0"
        size="default"
        class="reset-form"
      >
        <!-- 账号输入 -->
        <el-form-item prop="account">
          <el-input
            v-model="resetForm.account"
            :placeholder="resetTab === 'phone' ? '请输入注册手机号' : '请输入注册邮箱'"
            clearable
          />
        </el-form-item>

        <!-- 验证码行 -->
        <el-form-item prop="code">
          <div class="code-row">
            <el-input v-model="resetForm.code" placeholder="验证码" maxlength="6" />
            <el-button
              :disabled="resetCodeBtnDisabled"
              :loading="resetCodeSending"
              class="code-btn"
              @click="handleResetSendCode"
            >
              {{ resetCodeBtnText }}
            </el-button>
          </div>
        </el-form-item>

        <!-- 新密码 -->
        <el-form-item prop="newPwd">
          <el-input
            v-model="resetForm.newPwd"
            :type="resetShowPwd ? 'text' : 'password'"
            placeholder="新密码"
          >
            <template #suffix>
              <span class="pwd-icon" @click="resetShowPwd = !resetShowPwd">
                <el-icon><template v-if="resetShowPwd"><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg></template><template v-else><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/></svg></template></el-icon>
              </span>
            </template>
          </el-input>
        </el-form-item>

        <!-- 确认新密码 -->
        <el-form-item prop="confirmPwd">
          <el-input
            v-model="resetForm.confirmPwd"
            :type="resetShowConfirmPwd ? 'text' : 'password'"
            placeholder="确认新密码"
          >
            <template #suffix>
              <span class="pwd-icon" @click="resetShowConfirmPwd = !resetShowConfirmPwd">
                <el-icon><template v-if="resetShowConfirmPwd"><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 4.5C7 4.5 2.73 7.61 1 12c1.73 4.39 6 7.5 11 7.5s9.27-3.11 11-7.5c-1.73-4.39-6-7.5-11-7.5zM12 17c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/></svg></template><template v-else><svg viewBox="0 0 24 24" width="16" height="16" fill="currentColor"><path d="M12 7c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l2.92 2.92c1.51-1.26 2.7-2.89 3.43-4.75-1.73-4.39-6-7.5-11-7.5-1.4 0-2.74.25-3.98.7l2.16 2.16C10.74 7.13 11.35 7 12 7zM2 4.27l2.28 2.28.46.46C3.08 8.3 1.78 10.02 1 12c1.73 4.39 6 7.5 11 7.5 1.55 0 3.03-.3 4.38-.84l.42.42L19.73 22 21 20.73 3.27 3 2 4.27zM7.53 9.8l1.55 1.55c-.05.21-.08.43-.08.65 0 1.66 1.34 3 3 3 .22 0 .44-.03.65-.08l1.55 1.55c-.67.33-1.41.53-2.2.53-2.76 0-5-2.24-5-5 0-.79.2-1.53.53-2.2zm4.31-.78l3.15 3.15.02-.16c0-1.66-1.34-3-3-3l-.17.01z"/></svg></template></el-icon>
              </span>
            </template>
          </el-input>
        </el-form-item>
      </el-form>

      <template #footer>
        <el-button @click="showResetPwd = false">取消</el-button>
        <el-button type="primary" :loading="resetLoading" @click="handleResetPwd">
          重置密码
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
// 使用内联 SVG 替代图标组件
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const authStore = useAuthStore()

// ==================== 登录 ====================
const loginTab = ref('phone')
const formRef = ref<FormInstance>()
const loginLoading = ref(false)
const showPassword = ref(false)

const loginForm = reactive({
  phone: '',
  email: '',
  password: '',
})

const loginRules: FormRules = {
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/, message: '邮箱格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码长度为6~16位', trigger: 'blur' },
  ],
}

function handleLoginTabChange() {
  // 清空校验
  formRef.value?.clearValidate()
}

/** 游客浏览首页 */
function goHome() {
  router.push('/home')
}

async function handleLogin() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loginLoading.value = true
  try {
    const account = loginTab.value === 'phone' ? loginForm.phone : loginForm.email
    const type = loginTab.value === 'phone' ? 1 : 2

    const loginUser = await authStore.loginByAccount({ account, password: loginForm.password, type })
    ElMessage.success('登录成功')

    const roleCode = loginUser.roleCode
    if (roleCode === 'SUPER_ADMIN' || roleCode === 'ADMIN') {
      router.push('/admin')
    } else if (roleCode === 'MERCHANT') {
      router.push('/merchant/dashboard')
    } else {
      router.push('/home')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '登录失败')
  } finally {
    loginLoading.value = false
  }
}

// ==================== 注册 ====================
const showRegister = ref(false)
const registerTab = ref('phone')
const registerFormRef = ref<FormInstance>()
const registerLoading = ref(false)
const registerShowPwd = ref(false)
const registerShowConfirmPwd = ref(false)

const registerForm = reactive({
  phone: '',
  email: '',
  code: '',
  password: '',
  confirmPassword: '',
  agreed: false,
})

// 注册验证码倒计时
const registerCodeSending = ref(false)
const registerCodeCountdown = ref(0)
const registerCodeBtnDisabled = computed(() => registerCodeCountdown.value > 0 || registerCodeSending.value)
const registerCodeBtnText = computed(() => {
  if (registerCodeSending.value) return '发送中...'
  if (registerCodeCountdown.value > 0) return `${registerCodeCountdown.value}s后重发`
  return '获取验证码'
})

let registerCountdownTimer: ReturnType<typeof setInterval> | null = null
function startRegisterCountdown() {
  registerCodeCountdown.value = 60
  if (registerCountdownTimer) clearInterval(registerCountdownTimer)
  registerCountdownTimer = setInterval(() => {
    registerCodeCountdown.value--
    if (registerCodeCountdown.value <= 0) {
      if (registerCountdownTimer) clearInterval(registerCountdownTimer)
      registerCountdownTimer = null
    }
  }, 1000)
}

// 注册校验规则
const validateRegisterPhone = (_rule: any, value: string, callback: any) => {
  if (!value) return callback(new Error('请输入手机号'))
  if (!/^1[3-9]\d{9}$/.test(value)) return callback(new Error('手机号格式不正确'))
  callback()
}
const validateRegisterEmail = (_rule: any, value: string, callback: any) => {
  if (!value) return callback(new Error('请输入邮箱'))
  if (!/^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/.test(value)) return callback(new Error('邮箱格式不正确'))
  callback()
}
const validateRegisterAgreed = (_rule: any, value: boolean, callback: any) => {
  if (!value) return callback(new Error('请同意用户协议'))
  callback()
}
const validateRegisterConfirmPwd = (_rule: any, value: string, callback: any) => {
  if (!value) return callback(new Error('请确认密码'))
  if (value !== registerForm.password) return callback(new Error('两次输入的密码不一致'))
  callback()
}

const registerRules: FormRules = {
  phone: [{ validator: validateRegisterPhone, trigger: 'blur' }],
  email: [{ validator: validateRegisterEmail, trigger: 'blur' }],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码长度为6~16位', trigger: 'blur' },
  ],
  confirmPassword: [{ validator: validateRegisterConfirmPwd, trigger: 'blur' }],
  agreed: [{ validator: validateRegisterAgreed, trigger: 'change' }],
}

async function handleRegisterSendCode() {
  const account = registerTab.value === 'phone' ? registerForm.phone : registerForm.email
  const type = registerTab.value === 'phone' ? 1 : 2

  // 前端校验账号格式
  if (type === 1 && !/^1[3-9]\d{9}$/.test(account)) {
    ElMessage.warning('请先输入正确的手机号')
    return
  }
  if (type === 2 && !/^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/.test(account)) {
    ElMessage.warning('请先输入正确的邮箱地址')
    return
  }

  registerCodeSending.value = true
  try {
    await authStore.sendVerifyCode({ account, type })
    ElMessage.success('验证码已发送')
    startRegisterCountdown()
  } catch (error: any) {
    ElMessage.error(error.message || '验证码发送失败')
  } finally {
    registerCodeSending.value = false
  }
}

async function handleRegister() {
  const valid = await registerFormRef.value?.validate().catch(() => false)
  if (!valid) return
  if (!registerForm.agreed) {
    ElMessage.warning('请同意用户协议')
    return
  }

  registerLoading.value = true
  try {
    const account = registerTab.value === 'phone' ? registerForm.phone : registerForm.email
    const type = registerTab.value === 'phone' ? 1 : 2

    await authStore.registerByAccount({
      account,
      code: registerForm.code,
      password: registerForm.password,
      type,
    })
    ElMessage.success('注册成功')
    showRegister.value = false

    // 填充登录表单
    if (type === 1) {
      loginForm.phone = account
      loginTab.value = 'phone'
    } else {
      loginForm.email = account
      loginTab.value = 'email'
    }

    // 清空注册表单
    registerForm.phone = ''
    registerForm.email = ''
    registerForm.code = ''
    registerForm.password = ''
    registerForm.confirmPassword = ''
    registerForm.agreed = false
  } catch (error: any) {
    ElMessage.error(error.message || '注册失败')
  } finally {
    registerLoading.value = false
  }
}

// 切换注册 Tab 时清空表单
watch(registerTab, () => {
  registerForm.code = ''
  registerForm.password = ''
  registerForm.confirmPassword = ''
  registerFormRef.value?.clearValidate()
})

// ==================== 重置密码 ====================
const showResetPwd = ref(false)
const resetTab = ref('phone')
const resetFormRef = ref<FormInstance>()
const resetLoading = ref(false)
const resetShowPwd = ref(false)
const resetShowConfirmPwd = ref(false)

const resetForm = reactive({
  account: '',
  code: '',
  newPwd: '',
  confirmPwd: '',
})

// 重置密码验证码倒计时
const resetCodeSending = ref(false)
const resetCodeCountdown = ref(0)
const resetCodeBtnDisabled = computed(() => resetCodeCountdown.value > 0 || resetCodeSending.value)
const resetCodeBtnText = computed(() => {
  if (resetCodeSending.value) return '发送中...'
  if (resetCodeCountdown.value > 0) return `${resetCodeCountdown.value}s后重发`
  return '获取验证码'
})

let resetCountdownTimer: ReturnType<typeof setInterval> | null = null
function startResetCountdown() {
  resetCodeCountdown.value = 60
  if (resetCountdownTimer) clearInterval(resetCountdownTimer)
  resetCountdownTimer = setInterval(() => {
    resetCodeCountdown.value--
    if (resetCodeCountdown.value <= 0) {
      if (resetCountdownTimer) clearInterval(resetCountdownTimer)
      resetCountdownTimer = null
    }
  }, 1000)
}

// 重置密码校验规则
const validateResetAccount = (_rule: any, value: string, callback: any) => {
  if (!value) return callback(new Error('请输入账号'))
  if (resetTab.value === 'phone') {
    if (!/^1[3-9]\d{9}$/.test(value)) return callback(new Error('手机号格式不正确'))
  } else {
    if (!/^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/.test(value)) return callback(new Error('邮箱格式不正确'))
  }
  callback()
}
const validateResetConfirmPwd = (_rule: any, value: string, callback: any) => {
  if (!value) return callback(new Error('请确认密码'))
  if (value !== resetForm.newPwd) return callback(new Error('两次输入的密码不一致'))
  callback()
}

const resetRules: FormRules = {
  account: [{ validator: validateResetAccount, trigger: 'blur' }],
  code: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' },
  ],
  newPwd: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码长度为6~16位', trigger: 'blur' },
  ],
  confirmPwd: [{ validator: validateResetConfirmPwd, trigger: 'blur' }],
}

async function handleResetSendCode() {
  const account = resetForm.account
  const type = resetTab.value === 'phone' ? 1 : 2

  if (!account) {
    ElMessage.warning('请先输入账号')
    return
  }
  if (type === 1 && !/^1[3-9]\d{9}$/.test(account)) {
    ElMessage.warning('手机号格式不正确')
    return
  }
  if (type === 2 && !/^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\.[a-zA-Z0-9_-]+)+$/.test(account)) {
    ElMessage.warning('邮箱格式不正确')
    return
  }

  resetCodeSending.value = true
  try {
    await authStore.sendVerifyCode({ account, type })
    ElMessage.success('验证码已发送')
    startResetCountdown()
  } catch (error: any) {
    ElMessage.error(error.message || '验证码发送失败')
  } finally {
    resetCodeSending.value = false
  }
}

async function handleResetPwd() {
  const valid = await resetFormRef.value?.validate().catch(() => false)
  if (!valid) return

  resetLoading.value = true
  try {
    const type = resetTab.value === 'phone' ? 1 : 2
    await authStore.resetPassword({
      account: resetForm.account,
      code: resetForm.code,
      newPwd: resetForm.newPwd,
      type,
    })
    ElMessage.success('密码重置成功，请重新登录')

    // 关闭重置弹窗，回到登录
    showResetPwd.value = false
    resetForm.account = ''
    resetForm.code = ''
    resetForm.newPwd = ''
    resetForm.confirmPwd = ''

    // 清空登录表单
    loginForm.phone = ''
    loginForm.email = ''
    loginForm.password = ''
  } catch (error: any) {
    ElMessage.error(error.message || '重置密码失败')
  } finally {
    resetLoading.value = false
  }
}

// 切换重置 Tab 时清空
watch(resetTab, () => {
  resetForm.code = ''
  resetForm.newPwd = ''
  resetForm.confirmPwd = ''
  resetFormRef.value?.clearValidate()
})

// 进入页面时：清除 body 背景色，防止背景色从固定定位层下方透出
onMounted(() => {
  document.body.classList.add('login-page-active')
})

// 退出页面时：恢复 body 背景色，清除倒计时定时器
onUnmounted(() => {
  document.body.classList.remove('login-page-active')
  if (registerCountdownTimer) {
    clearInterval(registerCountdownTimer)
    registerCountdownTimer = null
  }
  if (resetCountdownTimer) {
    clearInterval(resetCountdownTimer)
    resetCountdownTimer = null
  }
})
</script>

<style scoped>
/* ===========================
   底层：全屏背景 + 渐变遮罩
   =========================== */
.login-page {
  position: fixed;
  inset: 0;
  background-image: url('@/assets/background.jpg');
  background-size: cover;
  background-position: center;
  background-repeat: no-repeat;
  overflow: hidden;
}

/* 整张山水图叠加 15% 透明度浅蓝遮罩，压暗背景突出卡片 */
.login-page::before {
  content: '';
  position: absolute;
  inset: 0;
  background: linear-gradient(
    180deg,
    rgba(90, 140, 190, 0.15) 0%,
    rgba(70, 120, 170, 0.15) 100%
  );
  z-index: 1;
  pointer-events: none;
}

/* ===========================
   顶层：通栏透明导航
   =========================== */
.login-nav {
  position: relative;
  z-index: 3;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 56px;
  height: 72px;
  background: transparent;
}

.login-nav-left {
  display: flex;
  align-items: center;
  gap: 32px;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 28px;
}

.nav-menu a {
  color: rgba(255, 255, 255, 0.92);
  font-size: 15px;
  font-weight: 400;
  text-decoration: none;
  transition: color 0.25s;
  cursor: pointer;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.30);
  letter-spacing: 0.5px;
}

.nav-menu a:hover {
  color: #87CEEB;
}

.nav-logo {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  object-fit: cover;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
}

.nav-brand {
  display: flex;
  flex-direction: column;
}

.nav-brand-name {
  color: #fff;
  font-size: 20px;
  font-weight: 600;
  letter-spacing: 2px;
  text-shadow: 0 1px 4px rgba(0, 0, 0, 0.2);
}

.nav-brand-slogan {
  color: rgba(255, 255, 255, 0.75);
  font-size: 11px;
  letter-spacing: 3px;
  text-shadow: 0 1px 3px rgba(0, 0, 0, 0.15);
}

.login-nav-right {
  display: flex;
  align-items: center;
  gap: 32px;
}

.login-nav-right a {
  color: rgba(255, 255, 255, 0.92);
  font-size: 15px;
  font-weight: 400;
  text-decoration: none;
  transition: color 0.25s;
  cursor: pointer;
  text-shadow: 0 1px 6px rgba(0, 0, 0, 0.30);
}

.login-nav-right a:hover {
  color: #87CEEB;
}

.login-nav-right a.nav-active {
  color: #fff;
  font-weight: 600;
  position: relative;
}

.login-nav-right a.nav-active::after {
  content: '';
  position: absolute;
  bottom: -4px;
  left: 0;
  right: 0;
  height: 3px;
  background: #87CEEB;
  border-radius: 2px;
}

/* ===========================
   中层：页面居中登录卡片
   =========================== */
.login-body {
  position: relative;
  z-index: 3;
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: calc(100vh - 72px);
  padding: 40px 20px;
}

.login-card {
  width: 100%;
  max-width: 420px;
  padding: 44px 40px 36px;
  background: rgba(255, 255, 255, 0.78);
  border-radius: 20px;
  box-shadow:
    0 12px 48px rgba(0, 0, 0, 0.12),
    0 4px 16px rgba(0, 0, 0, 0.06);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
}

/* ===========================
   卡片内部排版
   =========================== */

/* 头部 — 标题组与下方 Tab 留出 24px 间距 */
.card-header {
  text-align: center;
  margin-bottom: 24px;
}

.card-title {
  font-size: 32px;
  font-weight: 800;
  color: #1a1a2e;
  letter-spacing: 4px;
  margin-bottom: 8px;
}

.card-subtitle {
  font-size: 13px;
  color: #999;
  letter-spacing: 2px;
}

/* 登录标签 */
.login-tabs {
  margin-bottom: 28px;
}

/* 输入框前缀图标 */
.input-icon {
  vertical-align: middle;
}

/* 登录按钮 — 蓝 + 浅紫渐变 */
.login-btn {
  width: 100%;
  height: 46px;
  font-size: 16px;
  font-weight: 500;
  letter-spacing: 6px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #4A90D9 0%, #7B5EA7 100%);
  color: #fff;
  transition: all 0.3s;
  cursor: pointer;
}

.login-btn:hover {
  background: linear-gradient(135deg, #5BA0E9 0%, #8B6EB7 100%);
  box-shadow: 0 6px 20px rgba(74, 144, 217, 0.35);
  transform: translateY(-1px);
}

.login-btn:active {
  transform: translateY(0);
}

/* 底部链接左右分布 — 缩小与按钮间距 */
.card-footer {
  display: flex;
  justify-content: space-between;
  margin-top: 8px;
}

/* 测试账号提示条 */
.test-account-tip {
  margin: 4px 0 14px;
  padding: 8px 12px;
  font-size: 13px;
  line-height: 1.5;
  color: #666;
  background: #f4f6fb;
  border: 1px dashed #b8a9d4;
  border-radius: 8px;
  text-align: center;
}
.test-account-tip b {
  color: #4A2E75;
  font-weight: 700;
}

.card-link {
  color: #4A2E75;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.2s;
  user-select: none;
}

.card-link:hover {
  color: #4A90D9;
  text-decoration: underline;
}

/* ===========================
   通用组件样式覆盖
   =========================== */

/* 验证码行布局 */
.code-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.code-row .el-input {
  flex: 1;
}

.code-btn {
  white-space: nowrap;
  flex-shrink: 0;
}

/* 密码显隐图标 */
.pwd-icon {
  cursor: pointer;
  display: flex;
  align-items: center;
  color: #999;
  font-size: 16px;
}
.pwd-icon:hover {
  color: #666;
}

/* 表单间距 — 拉大输入框与按钮距离 */
:deep(.login-form .el-form-item),
:deep(.register-form .el-form-item),
:deep(.reset-form .el-form-item) {
  margin-bottom: 26px;
}

/* 输入框样式覆盖 — 极浅灰白底色，弱化冷蓝 */
:deep(.el-input__wrapper) {
  background: #ffffff !important;
  border-radius: 10px !important;
  box-shadow: none !important;
  border: 1px solid #e8e8e8;
  transition: border-color 0.25s, box-shadow 0.25s;
}

:deep(.el-input__wrapper:hover) {
  border-color: #b8a9d4;
}

:deep(.el-input__wrapper.is-focus) {
  border-color: #7B5EA7;
  box-shadow: 0 0 0 3px rgba(123, 94, 167, 0.10) !important;
}

:deep(.el-input__inner) {
  height: 42px;
  font-size: 14px;
  background: transparent !important;
}

:deep(.el-input__prefix) {
  margin-right: 6px;
}

/* 确保前缀图标区域背景与输入框一致 */
:deep(.el-input__prefix-inner) {
  background: transparent !important;
}

/* Tab 样式 */
:deep(.el-tabs__header) {
  margin-bottom: 20px;
}

:deep(.el-tabs__nav-wrap) {
  justify-content: center;
}

:deep(.el-tabs__nav-scroll) {
  display: flex;
  justify-content: center;
}

:deep(.el-tabs__item) {
  font-size: 15px;
  color: #888;
  transition: color 0.2s;
  padding: 0 24px;
}

/* 选中 Tab 文字加粗加深 */
:deep(.el-tabs__item.is-active) {
  color: #4A90D9;
  font-weight: 700;
  font-size: 16px;
}

:deep(.el-tabs__active-bar) {
  background: linear-gradient(90deg, #4A90D9, #7B5EA7);
  height: 3px;
  border-radius: 2px;
  transition: width 0.3s, transform 0.3s;
}

/* 复选框 */
:deep(.register-form .el-checkbox) {
  margin-top: 8px;
}

/* 错误提示 */
:deep(.el-form-item__error) {
  padding-top: 6px;
  font-size: 13px;
}

/* 对话框 */
:deep(.register-dialog .el-dialog__body),
:deep(.reset-dialog .el-dialog__body) {
  padding: 24px 28px;
}

:deep(.register-dialog .el-dialog__footer),
:deep(.reset-dialog .el-dialog__footer) {
  padding: 0 28px 20px;
}

/* 对话框标题居中 */
:deep(.el-dialog__header) {
  text-align: center;
  padding-top: 24px;
}

/* ===========================
   响应式适配
   =========================== */
@media (max-width: 768px) {
  .login-nav {
    padding: 0 20px;
    height: 60px;
    flex-wrap: nowrap;
    gap: 8px;
  }

  .login-nav-left {
    gap: 14px;
  }

  .nav-menu {
    gap: 12px;
  }

  .nav-menu a {
    font-size: 13px;
  }

  .nav-logo {
    width: 36px;
    height: 36px;
  }

  .login-body {
    min-height: calc(100vh - 60px);
    padding: 20px 16px;
  }

  .login-card {
    max-width: 100%;
    padding: 32px 24px 28px;
    border-radius: 16px;
  }

  .card-title {
    font-size: 22px;
  }

  .card-subtitle {
    font-size: 12px;
  }
}

/* ===== 登录页激活时：清除 body 背景色防止透出 ===== */
body.login-page-active {
  background: transparent !important;
}
</style>