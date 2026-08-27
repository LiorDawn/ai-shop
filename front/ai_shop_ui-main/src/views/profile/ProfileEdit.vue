<template>
  <div class="pe-page">
    <HeaderUser />

    <div class="pe-wrapper">
      <div class="pe-layout">
        <ProfileSidebar :user="user" :stats="stats" />

        <div class="pe-main">
          <!-- 主表单卡片 -->
          <div class="pe-card">
            <div class="pe-card-header">
              <div class="pe-card-header-left">
                <h3>个人资料</h3>
              </div>
              <el-tag v-if="profile" type="success" effect="plain" size="small">
                <el-icon style="margin-right:4px"><CircleCheck /></el-icon>已认证
              </el-tag>
            </div>

            <el-form
              label-width="120px"
              class="pe-form"
              v-loading="loading"
            >
              <!-- 头像行 - 加宽 -->
              <el-form-item label="头像">
                <div class="pe-avatar-wrap">
                  <div class="pe-avatar-img" @click="triggerUpload">
                    <img :src="form.avatar || 'https://picsum.photos/seed/default/80/80'" />
                    <div class="pe-avatar-overlay"><el-icon :size="20"><Camera /></el-icon></div>
                  </div>
                  <input ref="fileInput" type="file" accept="image/*" style="display:none" @change="onFileChange" />
                </div>
              </el-form-item>

              <!-- 昵称 + 手机号 两列 -->
              <div class="pe-form-row">
                <el-form-item label="昵称" class="pe-form-item-half">
                  <el-input v-model="form.nickname" placeholder="请输入昵称" maxlength="20" />
                </el-form-item>
                <el-form-item label="手机号" class="pe-form-item-half">
                  <el-input :model-value="profile?.phone" disabled>
                    <template #suffix>
                      <el-tag type="info" size="small" effect="plain">已验证</el-tag>
                    </template>
                  </el-input>
                </el-form-item>
              </div>

              <!-- 邮箱 -->
              <el-form-item label="邮箱">
                <el-input v-model="form.email" placeholder="请输入邮箱（选填）" maxlength="50" />
              </el-form-item>

              <!-- 性别 + 生日 两列 -->
              <div class="pe-form-row">
                <el-form-item label="性别" class="pe-form-item-half">
                  <el-radio-group v-model="form.gender">
                    <el-radio :value="0">保密</el-radio>
                    <el-radio :value="1">男</el-radio>
                    <el-radio :value="2">女</el-radio>
                  </el-radio-group>
                </el-form-item>
                <el-form-item label="注册时间" class="pe-form-item-half">
                  <el-input :model-value="profile?.createTime || ''" disabled />
                </el-form-item>
              </div>

              <!-- 个性签名 - 全宽 -->
              <el-form-item label="个性签名">
                <el-input
                  v-model="form.signature"
                  type="textarea"
                  :rows="3"
                  placeholder="介绍一下自己，让其他用户更好地认识你..."
                  maxlength="100"
                  show-word-limit
                />
              </el-form-item>

              <!-- 提交按钮 -->
              <el-form-item>
                <div class="pe-form-actions">
                  <el-button type="primary" size="large" @click="save" :loading="saving">
                    <el-icon style="margin-right:4px"><Check /></el-icon>保存修改
                  </el-button>
                  <el-button size="large" @click="$router.back()">取消</el-button>
                </div>
              </el-form-item>
            </el-form>
          </div>

          <!-- 辅助信息卡片：账号安全 -->
          <div class="pe-card pe-card-secondary">
            <div class="pe-card-header">
              <div class="pe-card-header-left">
                <h3>账号安全</h3>
                <span class="pe-card-desc">保障你的账户安全</span>
              </div>
            </div>
            <div class="pe-security">
              <div class="pe-security-item">
                <div class="pe-security-left">
                  <el-icon color="#67C23A" :size="18"><CircleCheck /></el-icon>
                  <div>
                    <div class="pe-security-label">登录密码</div>
                    <div class="pe-security-desc">已设置</div>
                  </div>
                </div>
                <el-button text type="primary" @click="$router.push('/profile/password')">修改</el-button>
              </div>
              <div class="pe-security-item">
                <div class="pe-security-left">
                  <el-icon color="#67C23A" :size="18"><CircleCheck /></el-icon>
                  <div>
                    <div class="pe-security-label">手机绑定</div>
                    <div class="pe-security-desc">{{ profile?.phone || '未绑定' }}</div>
                  </div>
                </div>
                <el-tag v-if="profile?.phone" type="success" size="small" effect="plain">已绑定</el-tag>
                <el-button v-else text type="primary">绑定</el-button>
              </div>
            </div>
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
import { CircleCheck, Camera, Check } from '@element-plus/icons-vue'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import ProfileSidebar from '@/views/profile/ProfileSidebar.vue'
import { getProfile, getProfileStats, updateProfile, type UserProfileDTO, type ProfileStats } from '@/api/profile'
import { uploadImage } from '@/api/upload'

const router = useRouter()
const loading = ref(false)
const saving = ref(false)
const profile = ref<UserProfileDTO | null>(null)
const user = ref<UserProfileDTO | null>(null)
const stats = ref<ProfileStats | null>(null)
const fileInput = ref<HTMLInputElement | null>(null)

const form = reactive({
  nickname: '',
  gender: 0,
  signature: '',
  avatar: '',
  email: '',
})

function triggerUpload() {
  fileInput.value?.click()
}

async function onFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  if (!target.files?.length) return
  try {
    const res = await uploadImage(target.files[0])
    form.avatar = res.data
    ElMessage.success('头像上传成功')
  } catch {
    ElMessage.error('头像上传失败')
  }
}

async function fetchProfile() {
  loading.value = true
  try {
    const [profileRes, statsRes] = await Promise.all([
      getProfile(),
      getProfileStats(),
    ])
    profile.value = profileRes.data
    user.value = profileRes.data
    stats.value = statsRes.data
    form.nickname = profileRes.data.nickname || profileRes.data.username || ''
    form.gender = profileRes.data.gender ?? 0
    form.signature = profileRes.data.signature || ''
    form.avatar = profileRes.data.avatar || ''
    form.email = profileRes.data.email || ''
  } catch {
    ElMessage.error('获取用户信息失败')
  } finally {
    loading.value = false
  }
}

async function save() {
  if (!form.nickname.trim()) {
    ElMessage.warning('昵称不能为空')
    return
  }
  saving.value = true
  try {
    await updateProfile({
      nickname: form.nickname.trim(),
      gender: form.gender,
      signature: form.signature.trim(),
      avatar: form.avatar || undefined,
    })
    ElMessage.success('保存成功')
    router.back()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  fetchProfile()
})
</script>

<style scoped>
.pe-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.pe-wrapper {
  max-width: 1600px;
  margin: 0 auto;
  padding: 16px;
}
.pe-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.pe-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

/* 主卡片 */
.pe-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.pe-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
}
.pe-card-header-left h3 {
  margin: 0 0 4px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}
.pe-card-desc {
  font-size: 13px;
  color: #999;
}
.pe-form {
  padding: 20px 24px 24px;
}
.pe-form :deep(.el-form-item) {
  margin-bottom: 18px;
}
.pe-form :deep(.el-form-item__label) {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}
.pe-form :deep(.el-input__wrapper),
.pe-form :deep(.el-textarea__wrapper) {
  border-radius: 6px;
}
.pe-form :deep(.el-input),
.pe-form :deep(.el-textarea) {
  max-width: 420px;
}

/* 两列布局 */
.pe-form-row {
  display: flex;
  gap: 40px;
}
.pe-form-item-half {
  flex: 1;
  min-width: 0;
}
.pe-form-item-half :deep(.el-input),
.pe-form-item-half :deep(.el-textarea) {
  max-width: 100%;
}

/* 头像 */
.pe-avatar-wrap {
  display: flex;
  align-items: center;
  gap: 16px;
}
.pe-avatar-img {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  border: 2px solid #e8e8e8;
}
.pe-avatar-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.pe-avatar-overlay {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.45);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}
.pe-avatar-img:hover .pe-avatar-overlay {
  opacity: 1;
}
.pe-avatar-info {
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.pe-avatar-tip {
  margin: 0;
  font-size: 12px;
  color: #999;
  line-height: 1.6;
}
.pe-form-tip {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
  line-height: 1.4;
}

/* 按钮 */
.pe-form-actions {
  display: flex;
  gap: 12px;
  padding-top: 8px;
}
.pe-form-actions .el-button--large {
  padding: 12px 32px;
  border-radius: 6px;
}

/* 辅助卡片 */
.pe-card-secondary {
  border: 1px solid #f0f0f0;
}
.pe-security {
  padding: 8px 32px;
}
.pe-security-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 0;
  border-bottom: 1px solid #f5f5f5;
}
.pe-security-item:last-child {
  border-bottom: none;
}
.pe-security-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.pe-security-label {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}
.pe-security-desc {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
</style>