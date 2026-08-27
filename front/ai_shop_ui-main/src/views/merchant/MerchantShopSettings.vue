<template>
  <div class="mss-page">
    <div v-loading="loading" class="mss-content">
      <el-card class="mss-card">
        <template #header><span>店铺基本信息</span></template>
        <el-form :model="form" label-width="100px" class="mss-form">
          <el-form-item label="店铺名称">
            <el-input v-model="form.shopName" maxlength="50" />
          </el-form-item>
          <el-form-item label="店铺LOGO">
            <div class="mss-logo-upload">
              <el-upload
                :show-file-list="false"
                :before-upload="beforeLogoUpload"
                accept="image/*"
              >
                <img v-if="form.shopLogo" :src="form.shopLogo" class="mss-logo-preview" />
                <div v-else class="mss-logo-placeholder">
                  <el-icon :size="24"><Plus /></el-icon>
                  <span>上传LOGO</span>
                </div>
              </el-upload>
            </div>
          </el-form-item>
          <el-form-item label="店铺简介">
            <el-input v-model="form.intro" type="textarea" :rows="3" maxlength="200" />
          </el-form-item>
          <el-form-item label="营业状态">
            <el-switch
              v-model="form.status"
              :active-value="1"
              :inactive-value="0"
              active-text="营业中"
              inactive-text="暂停营业"
              @change="handleStatusChange"
            />
          </el-form-item>
          <el-form-item label="商家账号">
            <el-input :model-value="profile.username" disabled>
              <template #prepend>登录账号</template>
            </el-input>
          </el-form-item>
          <el-form-item label="手机号">
            <el-input :model-value="profile.phone" disabled />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" @click="handleSave" :loading="saving">保存修改</el-button>
          </el-form-item>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { getMerchantShopInfo, updateMerchantShopInfo, toggleShopStatus } from '@/api/merchant'
import { uploadImage } from '@/api/upload'

const loading = ref(false)
const saving = ref(false)
const form = ref({
  shopName: '',
  shopLogo: '',
  intro: '',
  status: 1 as number,
})
const profile = ref({
  username: '',
  phone: '',
})

async function fetchData() {
  loading.value = true
  try {
    const res = await getMerchantShopInfo()
    const data = res.data
    const shop = data.shop || {}
    form.value.shopName = shop.shopName || ''
    form.value.shopLogo = shop.shopLogo || ''
    form.value.intro = shop.intro || ''
    form.value.status = shop.status ?? 1
    profile.value.username = data.username || ''
    profile.value.phone = data.phone || ''
  } catch (e: any) {
    ElMessage.error(e?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function beforeLogoUpload(file: File): Promise<boolean> {
  try {
    const res = await uploadImage(file, 'logo')
    form.value.shopLogo = res.data
  } catch { ElMessage.error('LOGO上传失败') }
  return false
}

async function handleSave() {
  if (!form.value.shopName.trim()) {
    ElMessage.warning('店铺名称不能为空')
    return
  }
  saving.value = true
  try {
    await updateMerchantShopInfo({
      shopName: form.value.shopName,
      shopLogo: form.value.shopLogo,
      intro: form.value.intro,
    })
    ElMessage.success('保存成功')
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  } finally {
    saving.value = false
  }
}

async function handleStatusChange(val: number) {
  try {
    await toggleShopStatus(val)
    ElMessage.success(val === 1 ? '已切换为营业中' : '已暂停营业')
  } catch {
    form.value.status = val === 1 ? 0 : 1
  }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.mss-page { min-height: 100%; }
.mss-content { max-width: 800px; }
.mss-form { max-width: 600px; }
.mss-logo-upload { display: flex; align-items: center; gap: 12px; }
.mss-logo-preview { width: 80px; height: 80px; border-radius: 8px; object-fit: cover; border: 1px solid #eee; cursor: pointer; }
.mss-logo-placeholder {
  width: 80px; height: 80px; border: 2px dashed #d9d9d9; border-radius: 8px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: #999; font-size: 12px; gap: 4px; cursor: pointer; transition: all 0.2s; background: #fafafa;
}
.mss-logo-placeholder:hover { border-color: #409eff; color: #409eff; }
</style>