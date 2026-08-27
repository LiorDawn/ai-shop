<template>
  <div class="mpw-page">
    <el-card class="mpw-card">
      <template #header><span>修改登录密码</span></template>
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="120px"
        class="mpw-form"
      >
        <el-form-item label="登录账号">
          <el-input :model-value="username" disabled />
        </el-form-item>
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="确认新密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" type="password" show-password />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSubmit" :loading="submitting">确认修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import { getMerchantProfile, changeMerchantPassword } from '@/api/merchant'
import { useAuthStore } from '@/stores/auth'

const formRef = ref<FormInstance>()
const auth = useAuthStore()
const username = ref('')
const submitting = ref(false)
const form = ref({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const rules: FormRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '新密码长度不能少于6位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    {
      validator: (_rule: any, value: string, callback: Function) => {
        if (value !== form.value.newPassword) {
          callback(new Error('两次输入密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    await changeMerchantPassword({
      oldPassword: form.value.oldPassword,
      newPassword: form.value.newPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    auth.logout()
  } catch (e: any) {
    ElMessage.error(e?.message || '修改失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    const res = await getMerchantProfile()
    username.value = res.data?.username || ''
  } catch { /* ignore */ }
})
</script>

<style scoped>
.mpw-page { min-height: 100%; }
.mpw-card { max-width: 600px; }
.mpw-form { max-width: 450px; }
</style>