<template>
  <div class="aas-page">
    <HeaderUser />

    <div class="aas-container">
      <h2 class="aas-title">申请售后</h2>

      <div class="aas-card">
        <!-- 商品信息区 -->
        <div class="aas-section">
          <div class="aas-section-title">商品信息</div>
          <div v-if="orderItem" class="aas-product">
            <img :src="orderItem.productImage" class="aas-product-img" />
            <div class="aas-product-info">
              <div class="aas-product-name">{{ orderItem.productName }}</div>
              <div class="aas-product-spec" v-if="orderItem.spec">{{ orderItem.spec }}</div>
              <div class="aas-product-price">
                实付单价：<span class="aas-price">¥{{ fmtPrice(orderItem.price) }}</span>
                <span class="aas-num">x{{ orderItem.num }}</span>
              </div>
            </div>
          </div>
          <div v-else class="aas-loading-product">
            <el-skeleton :rows="2" animated />
          </div>
        </div>

        <!-- 售后类型 -->
        <div class="aas-section">
          <div class="aas-section-title"><span class="aas-required">*</span>售后类型</div>
          <div class="aas-type-group">
            <label class="aas-type-option" :class="{ active: form.type === 0 }">
              <input type="radio" :value="0" v-model="form.type" />
              <div class="aas-type-content">
                <div class="aas-type-name">仅退款</div>
                <div class="aas-type-desc">未收到货或与商家协商一致，无需退回商品</div>
              </div>
            </label>
            <label class="aas-type-option" :class="{ active: form.type === 1 }">
              <input type="radio" :value="1" v-model="form.type" />
              <div class="aas-type-content">
                <div class="aas-type-name">退货退款</div>
                <div class="aas-type-desc">已收到货，需退回商品后退款</div>
              </div>
            </label>
          </div>
        </div>

        <!-- 退款金额 -->
        <div class="aas-section">
          <div class="aas-section-title">
            <span class="aas-required">*</span>退款金额
            <span class="aas-hint">（最多 ¥{{ maxAmount.toFixed(2) }}）</span>
          </div>
          <el-input-number
            v-model="form.amount"
            :min="0.01"
            :max="maxAmount"
            :precision="2"
            :step="1"
            :controls-position="'right'"
            style="width: 220px"
          />
        </div>

        <!-- 售后原因 -->
        <div class="aas-section">
          <div class="aas-section-title"><span class="aas-required">*</span>售后原因</div>
          <el-select v-model="form.reason" placeholder="请选择售后原因" style="width: 300px">
            <el-option label="质量问题" value="质量问题" />
            <el-option label="发错货" value="发错货" />
            <el-option label="七天无理由" value="七天无理由" />
            <el-option label="不想要了" value="不想要了" />
            <el-option label="商品描述不符" value="商品描述不符" />
            <el-option label="物流问题" value="物流问题" />
            <el-option label="其他" value="其他" />
          </el-select>
        </div>

        <!-- 问题描述 -->
        <div class="aas-section">
          <div class="aas-section-title">问题描述</div>
          <el-input
            v-model="form.description"
            type="textarea"
            :rows="4"
            placeholder="请详细描述问题，以便商家更好地处理您的申请"
            maxlength="500"
            show-word-limit
          />
        </div>

        <!-- 凭证图片上传 -->
        <div class="aas-section">
          <div class="aas-section-title">
            上传凭证
            <span class="aas-hint">（可选，建议上传清晰的商品问题照片）</span>
          </div>
          <el-upload
            v-model:file-list="fileList"
            :action="uploadUrl"
            :headers="uploadHeaders"
            list-type="picture-card"
            :limit="6"
            :on-success="handleUploadSuccess"
            :on-remove="handleUploadRemove"
            accept="image/*"
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
          <div class="aas-upload-tip">最多上传 6 张图片，单张不超过 5MB</div>
        </div>

        <!-- 底部按钮 -->
        <div class="aas-footer">
          <el-button @click="goBack">返回</el-button>
          <el-button type="primary" @click="submitApply" :loading="submitting">
            提交申请
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import { applyAfterSale } from '@/api/afterSale'
import { getMyOrderDetail, type OrderItemDTO } from '@/api/order'
import { useAuthStore } from '@/stores/auth'

const router = useRouter()
const route = useRoute()

const orderId = ref<number>(0)
const orderItemId = ref<number>(0)
const orderItem = ref<OrderItemDTO | null>(null)
const loading = ref(false)
const submitting = ref(false)

const form = ref({
  type: 0,
  amount: 0,
  reason: '',
  description: '',
})

const fileList = ref<any[]>([])
const uploadUrl = computed(() => {
  // 同源相对路径，由 Nginx 反代到后端，避免写死开发地址
  return `/api/upload/image`
})
const auth = useAuthStore()

const uploadHeaders = computed(() => {
  const token = auth.token
  return token ? { token } : {}
})

const maxAmount = computed(() => {
  if (!orderItem.value) return 0
  return Number(orderItem.value.price) * Number(orderItem.value.num)
})

function fmtPrice(p: number | undefined | null): string {
  if (p === undefined || p === null || isNaN(p as number)) return '0.00'
  return Number(p).toFixed(2)
}

function handleUploadSuccess(response: any, uploadFile: any) {
  if (response?.url || response?.data?.url) {
    uploadFile.url = response.url || response.data.url
  }
}

function handleUploadRemove() {
  // 文件已自动从 fileList 移除
}

function goBack() {
  router.back()
}

async function submitApply() {
  if (!form.value.reason) {
    ElMessage.warning('请选择售后原因')
    return
  }
  if (!form.value.amount || form.value.amount <= 0) {
    ElMessage.warning('请填写有效的退款金额')
    return
  }
  if (form.value.amount > maxAmount.value) {
    ElMessage.warning('退款金额不能超过实付金额')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认提交售后申请？\n类型：${form.value.type === 0 ? '仅退款' : '退货退款'}\n金额：¥${form.value.amount.toFixed(2)}`,
      '确认提交',
      { type: 'warning' }
    )
  } catch {
    return
  }

  submitting.value = true
  try {
    const images = fileList.value.map((f) => f.url).filter(Boolean).join(',')
    await applyAfterSale({
      orderId: orderId.value,
      orderItemId: orderItemId.value,
      type: form.value.type,
      amount: form.value.amount,
      reason: form.value.reason,
      description: form.value.description,
      images: images || undefined,
    })
    ElMessage.success('售后申请已提交，请等待商家处理')
    setTimeout(() => {
      router.push('/aftersale/list')
    }, 800)
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function loadOrderInfo() {
  const oid = route.query.orderId
  const itemId = route.query.orderItemId
  if (!oid || !itemId) {
    ElMessage.error('参数错误')
    router.back()
    return
  }
  orderId.value = Number(oid)
  orderItemId.value = Number(itemId)

  loading.value = true
  try {
    const res: any = await getMyOrderDetail(orderId.value)
    const detail = res.data || res
    const items = detail.items || []
    const item = items.find((i: OrderItemDTO) => i.id === orderItemId.value)
    if (item) {
      orderItem.value = item
      form.value.amount = Number((item.price * item.num).toFixed(2))
    } else {
      ElMessage.error('未找到商品信息')
      router.back()
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载订单信息失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadOrderInfo()
})
</script>

<style scoped>
.aas-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.aas-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
}
.aas-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 16px;
  color: #333;
}
.aas-card {
  background: #fff;
  border-radius: 8px;
  padding: 24px 32px;
}
.aas-section {
  padding: 16px 0;
  border-bottom: 1px dashed #eee;
}
.aas-section:last-of-type {
  border-bottom: none;
}
.aas-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
  display: flex;
  align-items: center;
  gap: 4px;
}
.aas-required {
  color: #e4393c;
}
.aas-hint {
  font-size: 12px;
  color: #999;
  font-weight: normal;
}

/* 商品信息 */
.aas-product {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
}
.aas-product-img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
}
.aas-product-info {
  flex: 1;
}
.aas-product-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
  margin-bottom: 6px;
}
.aas-product-spec {
  font-size: 12px;
  color: #999;
  margin-bottom: 6px;
}
.aas-product-price {
  font-size: 13px;
  color: #666;
}
.aas-price {
  color: #e4393c;
  font-weight: 600;
}
.aas-num {
  margin-left: 16px;
  color: #999;
}
.aas-loading-product {
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
}

/* 售后类型 */
.aas-type-group {
  display: flex;
  gap: 16px;
}
.aas-type-option {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px 20px;
  border: 2px solid #e5e5e5;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
}
.aas-type-option:hover {
  border-color: #ff9999;
}
.aas-type-option.active {
  border-color: #e4393c;
  background: #fff5f5;
}
.aas-type-option input[type="radio"] {
  width: 18px;
  height: 18px;
  accent-color: #e4393c;
  cursor: pointer;
}
.aas-type-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 4px;
}
.aas-type-desc {
  font-size: 12px;
  color: #999;
}

/* 上传 */
.aas-upload-tip {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

/* 底部 */
.aas-footer {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 24px;
  margin-top: 8px;
}
.aas-footer .el-button {
  min-width: 140px;
}
</style>
