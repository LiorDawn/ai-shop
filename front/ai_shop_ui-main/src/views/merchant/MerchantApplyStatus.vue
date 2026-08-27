<template>
  <div class="mas-page">
    <HeaderUser />
    <div class="mas-wrapper">
      <div class="mas-container">
        <div class="mas-header">
          <h2>入驻申请状态</h2>
        </div>

        <div class="mas-content" v-loading="loading">
          <!-- 无申请记录 -->
          <el-empty
            v-if="!application"
            description="暂无入驻申请"
            :image-size="120"
          >
            <el-button type="primary" @click="goApply">去申请入驻</el-button>
          </el-empty>

          <!-- 申请信息 -->
          <template v-else>
            <!-- 状态标签 -->
            <div class="mas-status-bar">
              <div class="mas-status-item">
                <span class="mas-status-label">当前状态</span>
                <el-tag v-if="application.auditStatus === 0" type="warning" size="large" effect="dark">
                  <el-icon><Clock /></el-icon> 审核中
                </el-tag>
                <el-tag v-else-if="application.auditStatus === 1" type="success" size="large" effect="dark">
                  <el-icon><CircleCheck /></el-icon> 审核通过
                </el-tag>
                <el-tag v-else-if="application.auditStatus === 2" type="danger" size="large" effect="dark">
                  <el-icon><CircleClose /></el-icon> 审核驳回
                </el-tag>
              </div>
              <div class="mas-status-item">
                <span class="mas-status-label">提交时间</span>
                <span class="mas-status-value">{{ application.createTime }}</span>
              </div>
            </div>

            <!-- 基础信息 -->
            <el-divider content-position="left">
              <el-icon><User /></el-icon> 基础信息
            </el-divider>
            <el-descriptions :column="2" border size="default">
              <el-descriptions-item label="联系人姓名">{{ application.contact }}</el-descriptions-item>
              <el-descriptions-item label="联系手机号">{{ application.phone }}</el-descriptions-item>
            </el-descriptions>

            <!-- 商家资质 -->
            <el-divider content-position="left">
              <el-icon><Document /></el-icon> 商家资质
            </el-divider>
            <el-descriptions :column="2" border size="default">
              <el-descriptions-item label="商家名称">{{ application.merchantName }}</el-descriptions-item>
              <el-descriptions-item label="营业执照编号">{{ application.licenseNo }}</el-descriptions-item>
            </el-descriptions>

            <!-- 审核结果区域 -->
            <div class="mas-audit-result">
              <!-- 审核中：无额外内容 -->
              <template v-if="application.auditStatus === 0">
                <el-alert
                  title="申请正在审核中，请耐心等待"
                  type="warning"
                  :closable="false"
                  show-icon
                >
                  <p>平台管理员正在审核您的入驻申请，审核周期一般为1-3个工作日，请耐心等待。</p>
                </el-alert>
              </template>

              <!-- 审核通过 -->
              <template v-if="application.auditStatus === 1">
                <el-alert
                  title="恭喜您，入驻申请已审核通过！"
                  type="success"
                  :closable="false"
                  show-icon
                >
                  <p>您已正式成为平台商家，现在可以进入商家中心管理您的店铺了。</p>
                </el-alert>
                <div class="mas-actions">
                  <el-button type="primary" size="large" @click="goMerchant">
                    <el-icon><Shop /></el-icon> 进入商家中心
                  </el-button>
                </div>
              </template>

              <!-- 审核驳回 -->
              <template v-if="application.auditStatus === 2">
                <el-alert
                  title="您的入驻申请已被驳回"
                  type="error"
                  :closable="false"
                  show-icon
                >
                  <template #default>
                    <div class="mas-reject-info">
                      <p class="mas-reject-reason">
                        <strong>驳回原因：</strong>{{ application.auditRemark }}
                      </p>
                      <p v-if="application.auditTime" class="mas-reject-time">
                        <strong>审核时间：</strong>{{ application.auditTime }}
                      </p>
                    </div>
                  </template>
                </el-alert>
                <div class="mas-actions">
                  <el-button type="primary" size="large" @click="goReapply">
                    <el-icon><Edit /></el-icon> 重新申请
                  </el-button>
                </div>
              </template>
            </div>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  User, Document, Shop, Clock, CircleCheck, CircleClose, Edit,
} from '@element-plus/icons-vue'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import { getMerchantApplyStatus } from '@/api/merchant'
import { getCurrentUser } from '@/api/auth'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const application = ref<any>(null)

onMounted(async () => {
  loading.value = true
  try {
    const res: any = await getMerchantApplyStatus()
    application.value = res.data
    // 审核通过 → 刷新本地用户信息，让角色（MERCHANT）和 shopId 立即生效
    if (res.data?.auditStatus === 1) {
      await refreshLocalUser()
    }
  } catch (e: any) {
    ElMessage.error(e.message || '查询失败')
  } finally {
    loading.value = false
  }
})

/** 从后端重新获取当前用户信息，更新到 Pinia store（无需重新登录） */
async function refreshLocalUser() {
  try {
    const res: any = await getCurrentUser()
    const userData = res.data
    if (userData) {
      auth.user = userData
    }
  } catch {
    // 刷新失败不影响页面展示
  }
}

function goApply() {
  router.push('/merchant/apply')
}

function goMerchant() {
  // 先刷新本地用户信息再跳转（确保路由守卫读到最新角色）
  refreshLocalUser().then(() => {
    router.push('/merchant')
  })
}

function goReapply() {
  router.push('/merchant/apply')
}
</script>

<style scoped>
.mas-page {
  min-height: 100vh;
  background: #f5f7fa;
}
.mas-wrapper {
  padding: 24px 0;
}
.mas-container {
  max-width: 800px;
  margin: 0 auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  overflow: hidden;
}
.mas-header {
  background: linear-gradient(135deg, #ff0f23, #e4393c);
  color: #fff;
  padding: 28px 40px;
}
.mas-header h2 {
  margin: 0;
  font-size: 20px;
}
.mas-content {
  padding: 32px 40px;
  min-height: 300px;
}

/* 状态栏 */
.mas-status-bar {
  display: flex;
  align-items: center;
  gap: 48px;
  padding: 20px 24px;
  background: #fafafa;
  border-radius: 8px;
  margin-bottom: 24px;
}
.mas-status-item {
  display: flex;
  align-items: center;
  gap: 12px;
}
.mas-status-label {
  font-size: 14px;
  color: #666;
}
.mas-status-value {
  font-size: 14px;
  color: #333;
}

/* 分割线 */
.mas-content :deep(.el-divider__text) {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  background: #fff;
  padding: 0 12px;
}
.mas-content :deep(.el-divider__text .el-icon) {
  margin-right: 4px;
}

/* 审核结果 */
.mas-audit-result {
  margin-top: 24px;
}
.mas-audit-result p {
  margin: 6px 0;
  font-size: 14px;
}
.mas-reject-info {
  margin-top: 8px;
}
.mas-reject-reason {
  color: #e4393c;
}
.mas-reject-time {
  color: #999;
  font-size: 13px;
}
.mas-actions {
  margin-top: 20px;
  text-align: center;
}
</style>