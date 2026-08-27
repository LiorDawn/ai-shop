<template>
  <div class="ma-page">
    <HeaderUser />
    <div class="ma-wrapper">
      <div class="ma-container">
        <div class="ma-header">
          <h2>商家入驻申请</h2>
          <p class="ma-header-desc">填写以下资料，提交后等待平台管理员审核，审核通过后即可开启您的店铺</p>
        </div>

        <div class="ma-content">
          <!-- 入驻须知 -->
          <el-alert
            title="入驻须知"
            type="info"
            :closable="false"
            show-icon
            class="ma-notice"
          >
            <ul class="ma-notice-list">
              <li>提交申请后，请耐心等待平台管理员审核，审核周期一般为1-3个工作日</li>
              <li>审核通过后，您将自动成为平台商家，可登录商家中心管理您的店铺</li>
              <li>审核期间请勿重复提交申请</li>
              <li>请确保填写的资质信息真实有效</li>
              <li>店铺名称和简介可在商家中心的店铺设置中自行配置</li>
            </ul>
          </el-alert>

          <!-- 表单 -->
          <el-form
            ref="formRef"
            :model="form"
            :rules="rules"
            label-width="120px"
            class="ma-form"
            v-loading="submitting"
          >
            <!-- 基础信息区 -->
            <el-divider content-position="left">
              <el-icon><User /></el-icon> 基础信息
            </el-divider>
            <el-form-item label="联系人姓名" prop="contact">
              <el-input v-model="form.contact" placeholder="请输入联系人姓名" maxlength="20" />
            </el-form-item>
            <el-form-item label="联系手机号" prop="phone">
              <el-input v-model="form.phone" placeholder="请输入联系手机号" maxlength="11" />
            </el-form-item>

            <!-- 商家资质区 -->
            <el-divider content-position="left">
              <el-icon><Document /></el-icon> 商家资质
            </el-divider>
            <el-form-item label="商家名称" prop="merchantName">
              <el-input v-model="form.merchantName" placeholder="请输入商家名称" maxlength="50" />
            </el-form-item>
            <el-form-item label="营业执照编号" prop="licenseNo">
              <el-input v-model="form.licenseNo" placeholder="请输入营业执照编号" maxlength="30" />
            </el-form-item>

            <!-- 协议区 -->
            <el-divider content-position="left">
              <el-icon><Checked /></el-icon> 入驻协议
            </el-divider>
            <el-form-item prop="agreed">
              <el-checkbox v-model="form.agreed">
                我已阅读并同意
                <el-link type="primary" :underline="false">《平台入驻服务协议》</el-link>
              </el-checkbox>
            </el-form-item>

            <!-- 操作按钮 -->
            <el-form-item>
              <el-button type="primary" size="large" :loading="submitting" @click="handleSubmit">
                <el-icon style="margin-right:4px"><Upload /></el-icon>提交申请
              </el-button>
              <el-button size="large" @click="handleReset">重置表单</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User, Document, Checked, Upload,
} from '@element-plus/icons-vue'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import { submitMerchantApply, getMerchantApplyStatus } from '@/api/merchant'

const router = useRouter()
const formRef = ref()
const submitting = ref(false)

const form = reactive({
  contact: '',
  phone: '',
  merchantName: '',
  licenseNo: '',
  agreed: false,
})

const rules = {
  contact: [
    { required: true, message: '请输入联系人姓名', trigger: 'blur' },
    { min: 2, max: 20, message: '联系人姓名长度在2-20个字符', trigger: 'blur' },
  ],
  phone: [
    { required: true, message: '请输入联系手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '请输入正确的手机号', trigger: 'blur' },
  ],
  merchantName: [
    { required: true, message: '请输入商家名称', trigger: 'blur' },
    { min: 2, max: 50, message: '商家名称长度在2-50个字符', trigger: 'blur' },
  ],
  licenseNo: [
    { required: true, message: '请输入营业执照编号', trigger: 'blur' },
  ],
  agreed: [
    {
      validator: (_rule: any, value: boolean, callback: any) => {
        if (!value) {
          callback(new Error('请先阅读并同意入驻服务协议'))
        } else {
          callback()
        }
      },
      trigger: 'change',
    },
  ],
}

// 页面加载时检查是否已有申请
onMounted(async () => {
  try {
    const res: any = await getMerchantApplyStatus()
    if (res.data) {
      // 已有申请，跳转到状态页
      router.replace('/merchant/apply/status')
    }
  } catch {
    // 未申请，正常显示表单
  }
})

async function handleSubmit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    await submitMerchantApply({
      contact: form.contact,
      phone: form.phone,
      merchantName: form.merchantName,
      licenseNo: form.licenseNo,
    })
    ElMessage.success('入驻申请已提交，请等待审核')
    router.push('/merchant/apply/status')
  } catch (e: any) {
    ElMessage.error(e.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

function handleReset() {
  formRef.value?.resetFields()
}
</script>

<style scoped>
.ma-page {
  min-height: 100vh;
  background: #f5f7fa;
}
.ma-wrapper {
  padding: 24px 0;
}
.ma-container {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  overflow: hidden;
}
.ma-header {
  background: linear-gradient(135deg, #ff0f23, #e4393c);
  color: #fff;
  padding: 32px 40px;
}
.ma-header h2 {
  margin: 0 0 8px;
  font-size: 22px;
}
.ma-header-desc {
  margin: 0;
  font-size: 14px;
  opacity: 0.9;
}
.ma-content {
  padding: 32px 40px;
}
.ma-notice {
  margin-bottom: 28px;
}
.ma-notice-list {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  line-height: 2;
}
.ma-form {
  margin-top: 8px;
}
.ma-form :deep(.el-divider__text) {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  background: #fff;
  padding: 0 12px;
}
.ma-form :deep(.el-divider__text .el-icon) {
  margin-right: 4px;
}
</style>