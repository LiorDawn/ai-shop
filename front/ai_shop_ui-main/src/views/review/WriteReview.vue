<template>
  <div class="wr-page">
    <HeaderUser />

    <div class="wr-container">
      <div class="wr-card">
        <h2 class="wr-title">评价晒单</h2>

        <!-- 区块1：商品信息（只读） -->
        <div class="wr-section">
          <div class="wr-product">
            <img :src="query.productImage || 'https://picsum.photos/seed/default/100/100'" class="wr-product-img" />
            <div class="wr-product-info">
              <div class="wr-product-name">{{ query.productName }}</div>
              <div class="wr-product-spec" v-if="query.spec">{{ query.spec }}</div>
              <div class="wr-product-price">实付：¥{{ fmtPrice(Number(query.price)) }}</div>
            </div>
          </div>
        </div>

        <!-- 区块2：评分区 -->
        <div class="wr-section">
          <div class="wr-section-label">商品评分 <span class="wr-required">*</span></div>
          <div class="wr-rate-wrap">
            <el-rate v-model="form.score" :texts="rateTexts" show-text size="large" />
          </div>
        </div>

        <!-- 区块3：评价内容 -->
        <div class="wr-section">
          <div class="wr-section-label">评价内容 <span class="wr-required">*</span></div>
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="5"
            placeholder="分享您的使用体验，帮助其他小伙伴参考..."
            maxlength="500"
            show-word-limit
            class="wr-textarea"
          />
        </div>

        <!-- 区块4：晒单图片 -->
        <div class="wr-section">
          <div class="wr-section-label">晒单图片 <span class="wr-optional">（选填，最多9张）</span></div>
          <div class="wr-upload-wrap">
            <div class="wr-upload-list">
              <div
                v-for="(img, idx) in form.imageList"
                :key="img.uid"
                class="wr-upload-item"
              >
                <img :src="img.localUrl" class="wr-upload-preview" />
                <!-- 上传中遮罩 -->
                <div v-if="img.status === 'uploading'" class="wr-upload-mask">
                  <el-icon class="is-loading" :size="22"><Loading /></el-icon>
                </div>
                <!-- 上传失败提示 -->
                <div v-if="img.status === 'error'" class="wr-upload-mask wr-upload-error" title="点击重试" @click="retryUpload(idx)">
                  <span>上传失败<br/>点击重试</span>
                </div>
                <div class="wr-upload-remove" @click="removeImage(idx)">
                  <el-icon><Close /></el-icon>
                </div>
              </div>
              <div
                v-if="form.imageList.length < 9"
                class="wr-upload-btn"
                @click="triggerUpload"
              >
                <el-icon :size="28"><Plus /></el-icon>
                <span>{{ form.imageList.length }}/9</span>
              </div>
            </div>
            <input
              ref="fileInput"
              type="file"
              accept="image/*"
              multiple
              style="display:none"
              @change="onFileChange"
            />
            <div class="wr-upload-tip">支持 jpg/png/gif/webp 格式，单张不超过10MB</div>
          </div>
        </div>

        <!-- 区块5：匿名评价 -->
        <div class="wr-section wr-section-inline">
          <el-checkbox v-model="form.isAnonymous">匿名评价</el-checkbox>
          <span class="wr-anonymous-tip">勾选后，评价列表将显示为「匿名用户」</span>
        </div>

        <!-- 区块6：提交按钮 -->
        <div class="wr-submit-wrap">
          <el-button size="large" @click="$router.back()">取消</el-button>
          <el-button type="danger" size="large" @click="submitReview" :loading="submitting">
            提交评价
          </el-button>
        </div>
      </div>
    </div>

    <!-- 订单号信息行 -->
    <div class="wr-meta">订单号：{{ query.orderNo }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Close, Plus, Loading } from '@element-plus/icons-vue'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import { addComment } from '@/api/comment'
import { uploadImage } from '@/api/upload'

const router = useRouter()
const route = useRoute()

// 允许的图片 MIME 类型
const ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp']
const MAX_FILE_SIZE = 10 * 1024 * 1024 // 10MB

interface ImageItem {
  uid: number          // 唯一标识
  localUrl: string     // 本地预览 URL（blob:）
  serverUrl: string    // 上传后的服务器 URL
  status: 'pending' | 'uploading' | 'done' | 'error'
  file: File           // 原始文件引用
}

let uidCounter = 0
function genUid() {
  return ++uidCounter
}

// 从 query 中获取订单和商品信息
const query = reactive({
  orderId: 0,
  orderNo: '',
  productId: 0,
  productName: '',
  productImage: '',
  spec: '',
  price: 0,
  shopId: 0,
  shopName: '',
})

const rateTexts = ['极差', '较差', '一般', '满意', '非常好']

const form = reactive({
  score: 5,
  content: '',
  imageList: [] as ImageItem[],
  isAnonymous: false,
})

const fileInput = ref<HTMLInputElement | null>(null)
const submitting = ref(false)
const uploading = ref(false)

function fmtPrice(p: number): string {
  if (isNaN(p)) return '0.00'
  return p.toFixed(2)
}

function triggerUpload() {
  fileInput.value?.click()
}

async function onFileChange(e: Event) {
  const target = e.target as HTMLInputElement
  if (!target.files?.length) return

  const files = Array.from(target.files)
  const remain = 9 - form.imageList.length
  const toUpload = files.slice(0, remain)

  uploading.value = true

  // Step 1: 立即显示本地预览（使用 URL.createObjectURL）
  const newItems: ImageItem[] = []
  for (const file of toUpload) {
    // 客户端校验文件类型
    if (!ALLOWED_MIME_TYPES.includes(file.type)) {
      ElMessage.warning(`"${file.name}" 格式不支持，仅支持 jpg/png/gif/webp`)
      continue
    }
    // 客户端校验文件大小
    if (file.size > MAX_FILE_SIZE) {
      ElMessage.warning(`"${file.name}" 超过10MB限制`)
      continue
    }

    const uid = genUid()
    const localUrl = URL.createObjectURL(file)
    newItems.push({
      uid,
      localUrl,
      serverUrl: '',
      status: 'pending',
      file,
    })
  }

  // 先添加所有本地预览后再开始上传
  form.imageList.push(...newItems)

  // Step 2: 逐个上传到服务器（并行）
  const uploadTasks = newItems.map(async (item) => {
    item.status = 'uploading'
    try {
      const res = await uploadImage(item.file, 'comment')
      item.serverUrl = res.data
      item.status = 'done'
    } catch {
      item.status = 'error'
    }
  })

  await Promise.all(uploadTasks)

  const doneCount = newItems.filter(i => i.status === 'done').length
  const failCount = newItems.filter(i => i.status === 'error').length
  if (doneCount > 0) {
    ElMessage.success(`上传成功 ${doneCount} 张${failCount > 0 ? `，${failCount} 张失败` : ''}`)
  } else if (failCount > 0) {
    ElMessage.error('图片上传失败，可点击图片重试')
  }

  uploading.value = false
  target.value = ''
}

function removeImage(idx: number) {
  const item = form.imageList[idx]
  if (item) {
    // 释放 blob URL，防止内存泄漏
    URL.revokeObjectURL(item.localUrl)
  }
  form.imageList.splice(idx, 1)
}

function retryUpload(idx: number) {
  const item = form.imageList[idx]
  if (!item || item.status !== 'error') return

  item.status = 'uploading'
  uploadImage(item.file, 'comment')
    .then((res) => {
      item.serverUrl = res.data
      item.status = 'done'
      ElMessage.success('重试上传成功')
    })
    .catch(() => {
      item.status = 'error'
      ElMessage.error('重试上传失败')
    })
}

async function submitReview() {
  // 校验
  if (!form.score) {
    ElMessage.warning('请选择商品评分')
    return
  }
  if (!form.content.trim() && form.imageList.length === 0) {
    ElMessage.warning('评价内容和晒单图片至少填一项')
    return
  }

  // 检查是否有正在上传的图片
  const uploadingItems = form.imageList.filter(i => i.status === 'uploading')
  if (uploadingItems.length > 0) {
    ElMessage.warning('请等待图片上传完成')
    return
  }

  // 检查是否有上传失败的图片
  const failedItems = form.imageList.filter(i => i.status === 'error')
  if (failedItems.length > 0) {
    ElMessage.warning('有图片上传失败，请重试或删除后再提交')
    return
  }

  submitting.value = true
  try {
    const serverUrls = form.imageList
      .filter(i => i.status === 'done' && i.serverUrl)
      .map(i => i.serverUrl)
      .join(',')

    await addComment({
      orderId: query.orderId,
      productId: query.productId,
      shopId: query.shopId,
      score: form.score,
      content: form.content.trim() || undefined,
      images: serverUrls || undefined,
    })
    ElMessage.success('评价提交成功！')
    // 跳转到评价晒单页
    router.push('/review/pending')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '提交失败')
  } finally {
    submitting.value = false
  }
}

onMounted(() => {
  // 获取 query 参数
  query.orderId = Number(route.query.orderId) || 0
  query.orderNo = (route.query.orderNo as string) || ''
  query.productId = Number(route.query.productId) || 0
  query.productName = (route.query.productName as string) || ''
  query.productImage = (route.query.productImage as string) || ''
  query.spec = (route.query.spec as string) || ''
  query.price = Number(route.query.price) || 0
  query.shopId = Number(route.query.shopId) || 0
  query.shopName = (route.query.shopName as string) || ''

  if (!query.orderId || !query.productId) {
    ElMessageBox.alert('缺少订单或商品信息，请返回重试', '提示').then(() => {
      router.push('/review/pending')
    })
  }
})
</script>

<style scoped>
.wr-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.wr-container {
  max-width: 720px;
  margin: 0 auto;
  padding: 16px 20px 40px;
}
.wr-card {
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 2px 12px rgba(0,0,0,0.06);
  padding: 28px 32px;
}
.wr-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 24px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.wr-meta {
  max-width: 720px;
  margin: 8px auto 0;
  font-size: 12px;
  color: #999;
  text-align: right;
  padding: 0 20px;
}

/* 区块 */
.wr-section {
  margin-bottom: 24px;
  padding-bottom: 20px;
  border-bottom: 1px solid #f5f5f5;
}
.wr-section:last-of-type {
  border-bottom: none;
}
.wr-section-inline {
  display: flex;
  align-items: center;
  gap: 8px;
  padding-bottom: 20px;
  margin-bottom: 20px;
  border-bottom: 1px solid #f5f5f5;
}
.wr-section-label {
  font-size: 15px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}
.wr-required {
  color: #e4393c;
}
.wr-optional {
  font-weight: 400;
  font-size: 13px;
  color: #999;
}

/* 商品信息 */
.wr-product {
  display: flex;
  gap: 16px;
  align-items: center;
  background: #fafafa;
  padding: 14px;
  border-radius: 8px;
}
.wr-product-img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 6px;
  flex-shrink: 0;
  border: 1px solid #eee;
}
.wr-product-info {
  flex: 1;
  min-width: 0;
}
.wr-product-name {
  font-size: 15px;
  font-weight: 500;
  color: #333;
  margin-bottom: 4px;
}
.wr-product-spec {
  font-size: 13px;
  color: #999;
  margin-bottom: 4px;
}
.wr-product-price {
  font-size: 14px;
  color: #e4393c;
  font-weight: 600;
}

/* 评分 */
.wr-rate-wrap {
  padding: 6px 0;
}
.wr-rate-wrap :deep(.el-rate__item) {
  font-size: 24px;
}
.wr-rate-wrap :deep(.el-rate__text) {
  font-size: 15px;
  margin-left: 12px;
  color: #e4393c;
  font-weight: 500;
}

/* 文本域 */
.wr-textarea :deep(.el-textarea__inner) {
  border-radius: 6px;
  font-size: 14px;
}

/* 图片上传 */
.wr-upload-wrap {
  width: 100%;
}
.wr-upload-list {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}
.wr-upload-item {
  position: relative;
  width: 88px;
  height: 88px;
  border-radius: 6px;
  overflow: hidden;
  border: 1px solid #e0e0e0;
}
.wr-upload-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.wr-upload-mask {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 11px;
  text-align: center;
  line-height: 1.4;
}
.wr-upload-error {
  background: rgba(228,57,60,0.7);
  cursor: pointer;
}
.wr-upload-remove {
  position: absolute;
  top: 2px;
  right: 2px;
  width: 20px;
  height: 20px;
  border-radius: 50%;
  background: rgba(0,0,0,0.5);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  font-size: 12px;
}
.wr-upload-btn {
  width: 88px;
  height: 88px;
  border: 2px dashed #d9d9d9;
  border-radius: 6px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: #999;
  font-size: 12px;
  gap: 2px;
  transition: border-color 0.2s, color 0.2s;
  background: #fafafa;
}
.wr-upload-btn:hover {
  border-color: #e4393c;
  color: #e4393c;
}

/* 匿名 */
.wr-anonymous-tip {
  font-size: 12px;
  color: #999;
}

/* 提交按钮 */
.wr-submit-wrap {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 8px;
}
.wr-submit-wrap .el-button--large {
  padding: 12px 36px;
  border-radius: 6px;
  font-size: 15px;
}
.wr-upload-tip {
  margin-top: 6px;
  font-size: 12px;
  color: #999;
}
</style>