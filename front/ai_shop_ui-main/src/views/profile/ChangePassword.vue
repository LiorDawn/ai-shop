<template>
  <div class="cp-page">
    <HeaderUser />

    <div class="cp-wrapper">
      <div class="cp-layout">
        <ProfileSidebar :user="user" :stats="stats" />

        <div class="cp-main">
          <div class="cp-card">
            <div class="cp-card-header">
              <h3>修改密码</h3>
              <span class="cp-card-desc">定期修改密码可以有效保障账户安全</span>
            </div>

            <el-form
              ref="formRef"
              :model="form"
              :rules="rules"
              label-width="120px"
              class="cp-form"
            >
              <el-form-item label="原密码" prop="oldPassword">
                <el-input
                  v-model="form.oldPassword"
                  type="password"
                  show-password
                  placeholder="请输入原密码"
                  style="width:320px"
                />
              </el-form-item>
              <el-form-item label="新密码" prop="newPassword">
                <el-input
                  v-model="form.newPassword"
                  type="password"
                  show-password
                  placeholder="请输入新密码（6-20位）"
                  style="width:320px"
                />
              </el-form-item>
              <el-form-item label="确认新密码" prop="confirmPassword">
                <el-input
                  v-model="form.confirmPassword"
                  type="password"
                  show-password
                  placeholder="请再次输入新密码"
                  style="width:320px"
                />
              </el-form-item>
              <el-form-item>
                <el-button type="primary" size="large" @click="save" :loading="saving">确认修改</el-button>
                <el-button size="large" @click="$router.back()">取消</el-button>
              </el-form-item>
            </el-form>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import ProfileSidebar from '@/views/profile/ProfileSidebar.vue'
import { changePassword, getProfile, getProfileStats, type UserProfileDTO, type ProfileStats } from '@/api/profile'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const saving = ref(false)
const formRef = ref<any>(null)
const user = ref<UserProfileDTO | null>(null)
const stats = ref<ProfileStats | null>(null)

const form = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: '',
})

const validateConfirm = (_rule: any, value: string, callback: any) => {
  if (value !== form.newPassword) callback(new Error('两次密码不一致'))
  else callback()
}

const rules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20位', trigger: 'blur' },
  ],
  confirmPassword: [
    { required: true, message: '请确认新密码', trigger: 'blur' },
    { validator: validateConfirm, trigger: 'blur' },
  ],
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    await changePassword({
      oldPassword: form.oldPassword,
      newPassword: form.newPassword,
      confirmPassword: form.confirmPassword,
    })
    ElMessage.success('密码修改成功，请重新登录')
    auth.logout()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '修改失败')
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  try {
    const [profileRes, statsRes] = await Promise.all([getProfile(), getProfileStats()])
    user.value = profileRes.data
    stats.value = statsRes.data
  } catch { /* ignore */ }
})
</script>

<style scoped>
.cp-page { min-height: 100vh; background: #f5f5f5; }
.cp-wrapper { max-width: 1600px; margin: 0 auto; padding: 16px; }
.cp-layout { display: flex; gap: 16px; align-items: flex-start; }
.cp-main { flex: 1; min-width: 0; }
.cp-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.cp-card-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}
.cp-card-header h3 { margin: 0 0 4px; font-size: 18px; font-weight: 600; color: #333; }
.cp-card-desc { font-size: 13px; color: #999; }
.cp-form { padding: 32px 24px; }
</style>