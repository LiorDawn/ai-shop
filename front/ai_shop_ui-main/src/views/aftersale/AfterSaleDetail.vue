<template>
  <div class="asd-page">
    <HeaderUser />

    <div class="asd-container">
      <!-- 顶部返回栏 -->
      <div class="asd-back" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回售后列表</span>
      </div>

      <div v-loading="loading" class="asd-content">
        <div v-if="detail">
          <!-- 状态区 -->
          <div class="asd-status-card">
            <div class="asd-status-left">
              <el-icon :size="32" class="asd-status-icon"><CircleCheck /></el-icon>
              <div>
                <div class="asd-status-text">{{ statusText(detail.auditStatus) }}</div>
                <div class="asd-status-desc">{{ statusDesc(detail.auditStatus) }}</div>
              </div>
            </div>
            <div class="asd-status-actions">
              <template v-if="detail.auditStatus === 0">
                <el-button type="warning" @click="cancelApply">撤销申请</el-button>
              </template>
              <template v-else-if="detail.auditStatus === 1 && detail.type === 1">
                <el-button type="primary" @click="showLogisticsDialog = true">填写退货物流</el-button>
              </template>
            </div>
          </div>

          <!-- 流程时间线 -->
          <div class="asd-card">
            <div class="asd-card-title">售后进度</div>
            <el-timeline class="asd-timeline">
              <el-timeline-item
                v-for="(step, idx) in timelineSteps"
                :key="idx"
                :timestamp="step.time"
                :type="step.type"
                :hollow="step.hollow"
              >
                <div class="asd-timeline-content">
                  <div class="asd-timeline-title">{{ step.title }}</div>
                  <div v-if="step.desc" class="asd-timeline-desc">{{ step.desc }}</div>
                </div>
              </el-timeline-item>
            </el-timeline>
          </div>

          <!-- 基础信息 -->
          <div class="asd-card">
            <div class="asd-card-title">售后信息</div>
            <div class="asd-info-grid">
              <div class="asd-info-item">
                <span class="asd-info-label">售后单号</span>
                <span class="asd-info-value">{{ detail.id }}</span>
              </div>
              <div class="asd-info-item">
                <span class="asd-info-label">订单号</span>
                <span class="asd-info-value">{{ detail.orderNo }}</span>
              </div>
              <div class="asd-info-item">
                <span class="asd-info-label">售后类型</span>
                <span class="asd-info-value">{{ detail.typeText }}</span>
              </div>
              <div class="asd-info-item">
                <span class="asd-info-label">申请金额</span>
                <span class="asd-info-value asd-price">¥{{ fmtPrice(detail.amount) }}</span>
              </div>
              <div class="asd-info-item">
                <span class="asd-info-label">申请时间</span>
                <span class="asd-info-value">{{ detail.createTime }}</span>
              </div>
              <div v-if="detail.auditTime" class="asd-info-item">
                <span class="asd-info-label">处理时间</span>
                <span class="asd-info-value">{{ detail.auditTime }}</span>
              </div>
            </div>
          </div>

          <!-- 商品信息 -->
          <div class="asd-card">
            <div class="asd-card-title">商品信息</div>
            <div v-for="item in detail.items" :key="item.id" class="asd-product">
              <img :src="item.productImage" class="asd-product-img" />
              <div class="asd-product-info">
                <div class="asd-product-name">{{ item.productName }}</div>
                <div class="asd-product-spec" v-if="item.spec">{{ item.spec }}</div>
                <div class="asd-product-meta">
                  <span>单价：¥{{ fmtPrice(item.price) }}</span>
                  <span>数量：x{{ item.num }}</span>
                </div>
              </div>
              <div class="asd-product-total">
                <span class="asd-product-total-label">退款金额</span>
                <span class="asd-product-total-value">¥{{ fmtPrice(item.price * item.num) }}</span>
              </div>
            </div>
          </div>

          <!-- 用户申请信息 -->
          <div class="asd-card">
            <div class="asd-card-title">申请信息</div>
            <div class="asd-info-list">
              <div class="asd-info-row">
                <span class="asd-info-label">售后原因</span>
                <span class="asd-info-value">{{ detail.reason }}</span>
              </div>
              <div v-if="detail.description" class="asd-info-row">
                <span class="asd-info-label">问题描述</span>
                <span class="asd-info-value">{{ detail.description }}</span>
              </div>
              <div v-if="detail.images" class="asd-info-row">
                <span class="asd-info-label">凭证图片</span>
                <div class="asd-info-value">
                  <el-image
                    v-for="(img, idx) in imageList"
                    :key="idx"
                    :src="img"
                    :preview-src-list="imageList"
                    class="asd-evidence-img"
                    fit="cover"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 商家处理结果 -->
          <div v-if="detail.auditStatus === 1 || detail.auditStatus === 2 || detail.auditStatus === 3" class="asd-card">
            <div class="asd-card-title">商家处理</div>
            <div class="asd-info-list">
              <div class="asd-info-row">
                <span class="asd-info-label">处理结果</span>
                <el-tag :type="detail.auditStatus === 2 ? 'danger' : 'success'" size="small">
                  {{ detail.auditStatus === 2 ? '拒绝申请' : '同意申请' }}
                </el-tag>
              </div>
              <div v-if="detail.auditRemark" class="asd-info-row">
                <span class="asd-info-label">商家备注</span>
                <span class="asd-info-value">{{ detail.auditRemark }}</span>
              </div>
              <div v-if="returnAddress" class="asd-info-row">
                <span class="asd-info-label">退货地址</span>
                <span class="asd-info-value">{{ returnAddress }}</span>
              </div>
            </div>
          </div>

          <!-- 退货物流信息 -->
          <div v-if="detail.type === 1 && (expressCompany || expressNo)" class="asd-card">
            <div class="asd-card-title">退货物流</div>
            <div class="asd-info-list">
              <div class="asd-info-row">
                <span class="asd-info-label">快递公司</span>
                <span class="asd-info-value">{{ expressCompany }}</span>
              </div>
              <div class="asd-info-row">
                <span class="asd-info-label">物流单号</span>
                <span class="asd-info-value asd-selectable">{{ expressNo }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 填写物流弹窗 -->
    <el-dialog v-model="showLogisticsDialog" title="填写退货物流" width="480px">
      <el-form :model="logisticsForm" label-width="100px">
        <el-form-item label="快递公司" required>
          <el-input v-model="logisticsForm.expressCompany" placeholder="如：顺丰、圆通、中通等" />
        </el-form-item>
        <el-form-item label="物流单号" required>
          <el-input v-model="logisticsForm.expressNo" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showLogisticsDialog = false">取消</el-button>
        <el-button type="primary" @click="submitLogistics" :loading="submittingLogistics">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, CircleCheck } from '@element-plus/icons-vue'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import {
  getMyAfterSaleDetail,
  cancelAfterSale,
  submitReturnLogistics,
  type AfterSaleDetailDTO,
} from '@/api/afterSale'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const detail = ref<AfterSaleDetailDTO | null>(null)
const showLogisticsDialog = ref(false)
const submittingLogistics = ref(false)
const logisticsForm = ref({
  expressCompany: '',
  expressNo: '',
})
const expressCompany = ref('')
const expressNo = ref('')

const imageList = computed(() => {
  if (!detail.value?.images) return []
  return detail.value.images.split(',').filter((s) => s.trim())
})

const returnAddress = computed(() => {
  const d = detail.value as any
  return d?.returnAddress || d?.auditRemark?.includes('地址') ? '' : ''
})

const timelineSteps = computed(() => {
  const steps: Array<{ title: string; desc?: string; time: string; type: string; hollow: boolean }> = []
  if (!detail.value) return steps

  steps.push({
    title: '提交售后申请',
    desc: `类型：${detail.value.typeText}，金额：¥${fmtPrice(detail.value.amount)}`,
    time: detail.value.createTime || '',
    type: 'primary',
    hollow: false,
  })

  if (detail.value.auditStatus === 0) {
    steps.push({
      title: '等待商家处理',
      desc: '商家将在 48 小时内处理您的申请',
      time: '',
      type: 'warning',
      hollow: true,
    })
  } else if (detail.value.auditStatus === 1) {
    steps.push({
      title: '商家同意申请',
      desc: detail.value.type === 1 ? '请尽快按退货地址寄回商品' : '退款将在 1-3 个工作日内到账',
      time: detail.value.auditTime || '',
      type: 'success',
      hollow: false,
    })
    if (detail.value.type === 1 && !expressCompany.value) {
      steps.push({
        title: '等待您寄回商品',
        desc: '请填写退货物流信息',
        time: '',
        type: 'warning',
        hollow: true,
      })
    }
  } else if (detail.value.auditStatus === 2) {
    steps.push({
      title: '商家拒绝申请',
      desc: detail.value.auditRemark || '商家拒绝了您的售后申请',
      time: detail.value.auditTime || '',
      type: 'danger',
      hollow: false,
    })
  } else if (detail.value.auditStatus === 3) {
    steps.push({
      title: '商家同意申请',
      time: detail.value.auditTime || '',
      type: 'success',
      hollow: false,
    })
    steps.push({
      title: '退款完成',
      desc: '退款已原路返回至您的支付账户',
      time: '',
      type: 'success',
      hollow: false,
    })
  } else if (detail.value.auditStatus === 4) {
    steps.push({
      title: '售后已关闭',
      desc: '您已撤销申请',
      time: detail.value.auditTime || '',
      type: 'info',
      hollow: false,
    })
  }

  return steps
})

function statusText(status: number) {
  const map: Record<number, string> = {
    0: '待商家处理',
    1: '商家已同意',
    2: '已拒绝',
    3: '退款完成',
    4: '已关闭',
  }
  return map[status] || '未知'
}

function statusDesc(status: number) {
  const map: Record<number, string> = {
    0: '商家将在 48 小时内处理您的申请',
    1: '请按商家提供的方式完成后续操作',
    2: '很抱歉，商家拒绝了您的申请',
    3: '退款已原路返回至您的支付账户',
    4: '该售后申请已关闭',
  }
  return map[status] || ''
}

function fmtPrice(p: number | undefined | null): string {
  if (p === undefined || p === null || isNaN(p as number)) return '0.00'
  return Number(p).toFixed(2)
}

function goBack() {
  router.back()
}

async function loadDetail() {
  const id = route.query.id
  if (!id) {
    ElMessage.error('参数错误')
    router.back()
    return
  }
  loading.value = true
  try {
    const res: any = await getMyAfterSaleDetail(Number(id))
    detail.value = res.data || res
    const d = detail.value as any
    expressCompany.value = d?.expressCompany || ''
    expressNo.value = d?.expressNo || ''
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function cancelApply() {
  try {
    await ElMessageBox.confirm('确定撤销该售后申请？撤销后不可恢复。', '提示', { type: 'warning' })
    await cancelAfterSale(detail.value!.id)
    ElMessage.success('已撤销')
    setTimeout(() => {
      router.push('/aftersale/list')
    }, 500)
  } catch {
    // cancelled
  }
}

async function submitLogistics() {
  if (!logisticsForm.value.expressCompany) {
    ElMessage.warning('请填写快递公司')
    return
  }
  if (!logisticsForm.value.expressNo) {
    ElMessage.warning('请填写物流单号')
    return
  }
  submittingLogistics.value = true
  try {
    await submitReturnLogistics(detail.value!.id, logisticsForm.value)
    ElMessage.success('物流信息已提交')
    showLogisticsDialog.value = false
    expressCompany.value = logisticsForm.value.expressCompany
    expressNo.value = logisticsForm.value.expressNo
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '提交失败')
  } finally {
    submittingLogistics.value = false
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.asd-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.asd-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 16px;
}
.asd-back {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 0 16px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: color 0.2s;
  width: fit-content;
}
.asd-back:hover {
  color: #e4393c;
}
.asd-content {
  background: transparent;
}

/* 状态卡 */
.asd-status-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.asd-status-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.asd-status-icon {
  color: #67c23a;
}
.asd-status-text {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}
.asd-status-desc {
  font-size: 13px;
  color: #999;
}

/* 卡片 */
.asd-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 16px;
}
.asd-card-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

/* 时间线 */
.asd-timeline {
  padding-left: 8px;
}
.asd-timeline-content {
  padding: 4px 0;
}
.asd-timeline-title {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}
.asd-timeline-desc {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

/* 信息网格 */
.asd-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px 24px;
}
.asd-info-item {
  display: flex;
  align-items: center;
  font-size: 13px;
}
.asd-info-label {
  color: #999;
  min-width: 80px;
  flex-shrink: 0;
}
.asd-info-value {
  color: #333;
  flex: 1;
}
.asd-price {
  color: #e4393c;
  font-weight: 600;
}
.asd-selectable {
  user-select: text;
}

/* 信息列表 */
.asd-info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.asd-info-row {
  display: flex;
  font-size: 13px;
}
.asd-info-row .asd-info-label {
  min-width: 80px;
  flex-shrink: 0;
  color: #999;
}
.asd-info-row .asd-info-value {
  flex: 1;
  color: #333;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 凭证图片 */
.asd-evidence-img {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  cursor: pointer;
}

/* 商品信息 */
.asd-product {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
}
.asd-product-img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 4px;
}
.asd-product-info {
  flex: 1;
}
.asd-product-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  margin-bottom: 6px;
}
.asd-product-spec {
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
}
.asd-product-meta {
  font-size: 12px;
  color: #666;
  display: flex;
  gap: 16px;
}
.asd-product-total {
  text-align: right;
}
.asd-product-total-label {
  font-size: 12px;
  color: #999;
  display: block;
  margin-bottom: 4px;
}
.asd-product-total-value {
  font-size: 16px;
  color: #e4393c;
  font-weight: 600;
}
</style>
