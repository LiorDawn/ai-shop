<template>
  <div class="map-page">
    <HeaderUser />

    <div class="map-container">
      <!-- 返回栏 -->
      <div class="map-back" @click="goBack">
        <el-icon><ArrowLeft /></el-icon>
        <span>返回售后列表</span>
      </div>

      <div v-loading="loading" class="map-content">
        <div v-if="detail">
          <!-- 状态卡 -->
          <div class="map-status-card">
            <div class="map-status-left">
              <el-icon :size="32" class="map-status-icon"><CircleCheck /></el-icon>
              <div>
                <div class="map-status-text">{{ statusText(detail.auditStatus) }}</div>
                <div class="map-status-desc">{{ statusDesc(detail.auditStatus) }}</div>
              </div>
            </div>
          </div>

          <!-- 基础信息 -->
          <div class="map-card">
            <div class="map-card-title">售后信息</div>
            <div class="map-info-grid">
              <div class="map-info-item">
                <span class="map-info-label">售后单号</span>
                <span class="map-info-value">{{ detail.id }}</span>
              </div>
              <div class="map-info-item">
                <span class="map-info-label">订单号</span>
                <span class="map-info-value">{{ detail.orderNo }}</span>
              </div>
              <div class="map-info-item">
                <span class="map-info-label">用户</span>
                <span class="map-info-value">{{ detail.username }}</span>
              </div>
              <div class="map-info-item">
                <span class="map-info-label">售后类型</span>
                <span class="map-info-value">{{ detail.typeText }}</span>
              </div>
              <div class="map-info-item">
                <span class="map-info-label">申请金额</span>
                <span class="map-info-value map-price">¥{{ fmtPrice(detail.amount) }}</span>
              </div>
              <div class="map-info-item">
                <span class="map-info-label">申请时间</span>
                <span class="map-info-value">{{ detail.createTime }}</span>
              </div>
            </div>
          </div>

          <!-- 商品信息 -->
          <div class="map-card">
            <div class="map-card-title">商品信息</div>
            <div v-for="item in detail.items" :key="item.id" class="map-product">
              <img :src="item.productImage" class="map-product-img" />
              <div class="map-product-info">
                <div class="map-product-name">{{ item.productName }}</div>
                <div class="map-product-spec" v-if="item.spec">{{ item.spec }}</div>
                <div class="map-product-meta">
                  <span>单价：¥{{ fmtPrice(item.price) }}</span>
                  <span>数量：x{{ item.num }}</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 用户申请信息 -->
          <div class="map-card">
            <div class="map-card-title">用户申请</div>
            <div class="map-info-list">
              <div class="map-info-row">
                <span class="map-info-label">售后原因</span>
                <span class="map-info-value">{{ detail.reason }}</span>
              </div>
              <div v-if="detail.description" class="map-info-row">
                <span class="map-info-label">问题描述</span>
                <span class="map-info-value">{{ detail.description }}</span>
              </div>
              <div v-if="detail.images" class="map-info-row">
                <span class="map-info-label">凭证图片</span>
                <div class="map-info-value">
                  <el-image
                    v-for="(img, idx) in imageList"
                    :key="idx"
                    :src="img"
                    :preview-src-list="imageList"
                    class="map-evidence-img"
                    fit="cover"
                  />
                </div>
              </div>
            </div>
          </div>

          <!-- 处理区（仅待处理状态） -->
          <div v-if="detail.auditStatus === 0 && !isViewMode" class="map-card">
            <div class="map-card-title">处理售后</div>

            <!-- 退货退款 - 需填写退货地址 -->
            <el-form v-if="detail.type === 1" :model="processForm" label-width="100px" class="map-process-form">
              <el-form-item label="退货地址" required>
                <el-input
                  v-model="processForm.returnAddress"
                  type="textarea"
                  :rows="2"
                  placeholder="请填写详细的退货地址，包括收件人、电话、地址"
                />
              </el-form-item>
            </el-form>

            <el-form :model="processForm" label-width="100px" class="map-process-form">
              <el-form-item label="处理备注">
                <el-input
                  v-model="processForm.auditRemark"
                  type="textarea"
                  :rows="2"
                  placeholder="请填写处理说明（可选，拒绝时必填）"
                />
              </el-form-item>
            </el-form>

            <div class="map-process-actions">
              <el-button type="success" :loading="processing" @click="handleApprove">
                {{ detail.type === 1 ? '同意退货' : '同意退款' }}
              </el-button>
              <el-button type="danger" :loading="processing" @click="handleReject">拒绝申请</el-button>
            </div>
          </div>

          <!-- 已处理 - 查看处理结果 -->
          <div v-if="detail.auditStatus !== 0" class="map-card">
            <div class="map-card-title">处理结果</div>
            <div class="map-info-list">
              <div class="map-info-row">
                <span class="map-info-label">处理人</span>
                <span class="map-info-value">{{ detail.auditBy || '-' }}</span>
              </div>
              <div class="map-info-row">
                <span class="map-info-label">处理时间</span>
                <span class="map-info-value">{{ detail.auditTime || '-' }}</span>
              </div>
              <div class="map-info-row">
                <span class="map-info-label">处理结果</span>
                <el-tag :type="detail.auditStatus === 2 ? 'danger' : 'success'" size="small">
                  {{ detail.auditStatus === 2 ? '拒绝申请' : detail.auditStatus === 3 ? '已退款' : '同意申请' }}
                </el-tag>
              </div>
              <div v-if="detail.auditRemark" class="map-info-row">
                <span class="map-info-label">处理备注</span>
                <span class="map-info-value">{{ detail.auditRemark }}</span>
              </div>
              <div v-if="returnAddress" class="map-info-row">
                <span class="map-info-label">退货地址</span>
                <span class="map-info-value">{{ returnAddress }}</span>
              </div>
            </div>
          </div>

          <!-- 退货物流信息 -->
          <div v-if="detail.type === 1 && (expressCompany || expressNo)" class="map-card">
            <div class="map-card-title">退货物流</div>
            <div class="map-info-list">
              <div class="map-info-row">
                <span class="map-info-label">快递公司</span>
                <span class="map-info-value">{{ expressCompany }}</span>
              </div>
              <div class="map-info-row">
                <span class="map-info-label">物流单号</span>
                <span class="map-info-value map-selectable">{{ expressNo }}</span>
              </div>
            </div>

            <!-- 确认收货按钮 -->
            <div v-if="detail.auditStatus === 1 && !isViewMode" class="map-process-actions">
              <el-button type="success" :loading="processing" @click="handleFinish">
                确认收货并退款
              </el-button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { ArrowLeft, CircleCheck } from '@element-plus/icons-vue'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import {
  getMerchantAfterSaleDetail,
  merchantAuditAfterSale,
  merchantFinishAfterSale,
  type AfterSaleDetailDTO,
} from '@/api/afterSale'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const processing = ref(false)
const detail = ref<AfterSaleDetailDTO | null>(null)
const isViewMode = ref(false)
const expressCompany = ref('')
const expressNo = ref('')

const processForm = ref({
  auditRemark: '',
  returnAddress: '',
})

const imageList = computed(() => {
  if (!detail.value?.images) return []
  return detail.value.images.split(',').filter((s) => s.trim())
})

const returnAddress = computed(() => {
  const d = detail.value as any
  return d?.returnAddress || ''
})

function statusText(status: number) {
  const map: Record<number, string> = {
    0: '待处理',
    1: '已同意',
    2: '已拒绝',
    3: '退款完成',
    4: '已关闭',
  }
  return map[status] || '未知'
}

function statusDesc(status: number) {
  const map: Record<number, string> = {
    0: '请及时处理用户的售后申请',
    1: '已同意申请，等待用户寄回商品或退款到账',
    2: '已拒绝该售后申请',
    3: '退款已完成',
    4: '该售后已关闭',
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
  isViewMode.value = route.query.view === '1'
  if (!id) {
    ElMessage.error('参数错误')
    router.back()
    return
  }
  loading.value = true
  try {
    const res: any = await getMerchantAfterSaleDetail(Number(id))
    detail.value = res.data || res
    const d = detail.value as any
    expressCompany.value = d?.expressCompany || ''
    expressNo.value = d?.expressNo || ''
    if (d?.returnAddress) {
      processForm.value.returnAddress = d.returnAddress
    }
    if (d?.auditRemark) {
      processForm.value.auditRemark = d.auditRemark
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

async function handleApprove() {
  if (detail.value!.type === 1 && !processForm.value.returnAddress.trim()) {
    ElMessage.warning('请填写退货地址')
    return
  }
  try {
    await ElMessageBox.confirm('确认同意该售后申请？', '确认', { type: 'warning' })
  } catch {
    return
  }
  processing.value = true
  try {
    await merchantAuditAfterSale({
      id: detail.value!.id,
      auditStatus: 1,
      auditRemark: processForm.value.auditRemark,
      returnAddress: processForm.value.returnAddress,
    })
    ElMessage.success('处理成功')
    setTimeout(() => {
      loadDetail()
    }, 500)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '处理失败')
  } finally {
    processing.value = false
  }
}

async function handleReject() {
  if (!processForm.value.auditRemark.trim()) {
    ElMessage.warning('请填写拒绝原因')
    return
  }
  try {
    await ElMessageBox.confirm('确认拒绝该售后申请？', '确认', { type: 'warning' })
  } catch {
    return
  }
  processing.value = true
  try {
    await merchantAuditAfterSale({
      id: detail.value!.id,
      auditStatus: 2,
      auditRemark: processForm.value.auditRemark,
      returnAddress: '',
    })
    ElMessage.success('处理成功')
    setTimeout(() => {
      loadDetail()
    }, 500)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '处理失败')
  } finally {
    processing.value = false
  }
}

async function handleFinish() {
  try {
    await ElMessageBox.confirm('确认已收到退货商品，将执行退款操作？', '确认', { type: 'warning' })
  } catch {
    return
  }
  processing.value = true
  try {
    await merchantFinishAfterSale(detail.value!.id)
    ElMessage.success('退款成功')
    setTimeout(() => {
      loadDetail()
    }, 500)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '处理失败')
  } finally {
    processing.value = false
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.map-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.map-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 16px;
}
.map-back {
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
.map-back:hover {
  color: #e4393c;
}

/* 状态卡 */
.map-status-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.map-status-left {
  display: flex;
  align-items: center;
  gap: 16px;
}
.map-status-icon {
  color: #67c23a;
}
.map-status-text {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}
.map-status-desc {
  font-size: 13px;
  color: #999;
}

/* 卡片 */
.map-card {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px;
  margin-bottom: 16px;
}
.map-card-title {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}

/* 信息网格 */
.map-info-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px 24px;
}
.map-info-item {
  display: flex;
  align-items: center;
  font-size: 13px;
}
.map-info-label {
  color: #999;
  min-width: 80px;
  flex-shrink: 0;
}
.map-info-value {
  color: #333;
  flex: 1;
}
.map-price {
  color: #e4393c;
  font-weight: 600;
}
.map-selectable {
  user-select: text;
}

/* 信息列表 */
.map-info-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.map-info-row {
  display: flex;
  font-size: 13px;
}
.map-info-row .map-info-label {
  min-width: 80px;
  flex-shrink: 0;
  color: #999;
}
.map-info-row .map-info-value {
  flex: 1;
  color: #333;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

/* 凭证图片 */
.map-evidence-img {
  width: 80px;
  height: 80px;
  border-radius: 4px;
  cursor: pointer;
}

/* 商品信息 */
.map-product {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
}
.map-product-img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 4px;
}
.map-product-info {
  flex: 1;
}
.map-product-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  margin-bottom: 6px;
}
.map-product-spec {
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
}
.map-product-meta {
  font-size: 12px;
  color: #666;
  display: flex;
  gap: 16px;
}

/* 处理表单 */
.map-process-form {
  margin-bottom: 16px;
}
.map-process-actions {
  display: flex;
  justify-content: center;
  gap: 24px;
  padding-top: 16px;
  border-top: 1px dashed #eee;
}
.map-process-actions .el-button {
  min-width: 140px;
}
</style>
