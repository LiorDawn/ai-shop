<template>
  <div class="fs-page">
    <HeaderUser />

    <!-- 秒杀 Banner 通栏 -->
    <div class="fs-banner">
      <div class="container">
        <div class="fs-banner-inner">
          <div class="fs-banner-title">
            <svg class="fs-banner-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <polygon points="13 2 3 14 12 14 11 22 21 10 12 10 13 2"/>
            </svg>
            <span class="fs-banner-text">限时秒杀</span>
          </div>
          <div v-if="countdown > 0" class="fs-banner-countdown">
            <span class="fs-banner-cd-label">距结束</span>
            <span
              v-for="(ch, i) in countdownText.split('')"
              :key="i"
              class="fs-banner-cd-box"
              :class="{ 'fs-banner-cd-colon': ch === ':' }"
            >{{ ch }}</span>
          </div>
          <div v-else-if="nearestStartTime" class="fs-banner-countdown">
            <span class="fs-banner-cd-label">距开始</span>
            <span
              v-for="(ch, i) in countdownText.split('')"
              :key="i"
              class="fs-banner-cd-box"
              :class="{ 'fs-banner-cd-colon': ch === ':' }"
            >{{ ch }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 商品列表 -->
    <div class="container">
      <div class="fs-list" v-if="items.length > 0">
        <div
          v-for="item in items"
          :key="item.id"
          class="fs-card"
          @click="showDetail(item)"
        >
          <div class="fs-card-img">
            <el-image
              :src="item.productImage"
              :alt="item.productName"
              fit="cover"
            />
            <div class="fs-card-badge">秒杀</div>
          </div>
          <div class="fs-card-body">
            <div class="fs-card-name">{{ item.productName }}</div>
            <div class="fs-card-price-row">
              <span class="fs-card-flash-price">
                <span class="fs-card-price-sym">¥</span>
                {{ Number(item.flashPrice).toFixed(2) }}
                <span class="fs-card-original-price">¥{{ Number(item.originalPrice).toFixed(2) }}</span>
              </span>
            </div>
            <div class="fs-card-info">
              <span class="fs-card-stock" :class="{ 'fs-card-stock-low': item.stock <= 30, 'fs-card-stock-urgent': item.stock <= 10 }">
                剩余 {{ item.stock }} 件
                <span v-if="item.stock <= 10 && item.stock > 0" class="fs-card-stock-tag">即将售罄</span>
              </span>
              <el-button
                class="fs-card-btn"
                :disabled="item.stock <= 0"
                @click.stop="buyNow(item)"
              >
                {{ item.stock > 0 ? '立即抢购' : '已抢完' }}
              </el-button>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-else description="暂无秒杀活动" />
    </div>

    <!-- 秒杀详情弹窗 -->
    <el-dialog
      v-model="detailVisible"
      :title="detailItem?.productName || '秒杀详情'"
      width="680px"
      class="fs-dialog"
    >
      <div v-if="detailItem" class="fs-detail">
        <div class="fs-detail-left">
          <el-image :src="detailItem.productImage" fit="cover" class="fs-detail-img" />
        </div>
        <div class="fs-detail-right">
          <h3 class="fs-detail-name">{{ detailItem.productName }}</h3>
          <div class="fs-detail-price-row">
            <span class="fs-detail-flash-price">
              <span class="fs-detail-price-sym">¥</span>
              {{ Number(detailItem.flashPrice).toFixed(2) }}
              <span class="fs-detail-original-price">¥{{ Number(detailItem.originalPrice).toFixed(2) }}</span>
            </span>
          </div>
          <div class="fs-detail-stock" :class="{ 'fs-detail-stock-urgent': detailItem.stock <= 10 }">
            剩余库存：<span class="fs-detail-stock-num">{{ detailItem.stock }}</span> 件
            <span v-if="detailItem.stock <= 10 && detailItem.stock > 0" class="fs-detail-stock-tag">即将售罄</span>
          </div>
          <div class="fs-detail-limit">每人限购 {{ detailItem.limitPerUser }} 件</div>
          <div class="fs-detail-time">
            活动时间：{{ formatTime(detailItem.startTime) }} ~ {{ formatTime(detailItem.endTime) }}
          </div>
          <el-button
            class="fs-detail-btn"
            size="large"
            :disabled="detailItem.stock <= 0"
            @click="buyNow(detailItem)"
          >
            {{ detailItem.stock > 0 ? '立即抢购' : '已抢完' }}
          </el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import HeaderUser from '../../components/layout/HeaderUser.vue'
import { getFlashSaleList, executeSeckill, pollSeckillResult, type FlashSaleItem } from '../../api/flashSale'

const router = useRouter()

const items = ref<FlashSaleItem[]>([])
const countdown = ref(0)
const nearestEndTime = ref<number>(0)
const nearestStartTime = ref<number>(0)
let timer: number | null = null

const detailVisible = ref(false)
const detailItem = ref<FlashSaleItem | null>(null)

const countdownText = computed(() => {
  const t = countdown.value
  if (t <= 0) return '00:00:00'
  const h = Math.floor(t / 3600)
  const m = Math.floor((t % 3600) / 60)
  const s = t % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

function formatTime(t: string) {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

async function fetchList() {
  try {
    const res = await getFlashSaleList()
    items.value = res.data || []
    updateCountdown()
  } catch { /* ignore */ }
}

function updateCountdown() {
  const now = Date.now()
  let nearestEnd = Infinity
  let nearestStart = Infinity

  for (const item of items.value) {
    const end = new Date(item.endTime).getTime()
    const start = new Date(item.startTime).getTime()
    if (end > now && end < nearestEnd) nearestEnd = end
    if (start > now && start < nearestStart) nearestStart = start
  }

  nearestEndTime.value = nearestEnd
  nearestStartTime.value = nearestStart

  if (nearestEnd < Infinity) {
    countdown.value = Math.max(0, Math.floor((nearestEnd - now) / 1000))
  } else if (nearestStart < Infinity) {
    countdown.value = Math.max(0, Math.floor((nearestStart - now) / 1000))
  } else {
    countdown.value = 0
  }
}

function startTimer() {
  timer = window.setInterval(() => {
    if (countdown.value > 0) {
      countdown.value--
    } else {
      updateCountdown()
    }
  }, 1000)
}

function showDetail(item: FlashSaleItem) {
  detailItem.value = item
  detailVisible.value = true
}

async function buyNow(item: FlashSaleItem) {
  if (item.stock <= 0) {
    ElMessage.warning('商品已售罄')
    return
  }

  try {
    await ElMessageBox.confirm(
      `确认以 ¥${Number(item.flashPrice).toFixed(2)} 的价格抢购「${item.productName}」？`,
      '抢购确认',
      { confirmButtonText: '立即抢购', cancelButtonText: '再想想', type: 'warning' }
    )
  } catch {
    return
  }

  const sign = generateSign(item.id)
  try {
    const executeRes = await executeSeckill(item.id, sign)
    const data = executeRes.data
    ElMessage.success('抢购请求已提交，正在处理中...')

    const requestId = data.requestId
    pollResult(requestId)
  } catch (err: any) {
    ElMessage.error(err.message || '抢购失败')
  }
}

function generateSign(id: number): string {
  return 'temp-' + Date.now() + '-' + id
}

async function pollResult(requestId: string) {
  const maxRetries = 30
  let retries = 0

  const poll = () => {
    return new Promise<void>((resolve) => {
      const t = window.setTimeout(async () => {
        if (retries >= maxRetries) {
          ElMessage.error('抢购结果查询超时，请到订单中心查看')
          resolve()
          return
        }
        retries++
        try {
          const res = await pollSeckillResult(requestId)
          const data = res.data
          if (data.status === 'SUCCESS') {
            ElMessage.success('抢购成功！即将跳转支付...')
            fetchList()
            detailVisible.value = false
            // 跳转到支付页面，使用模拟支付
            const orderId = data.orderId
            if (orderId) {
              router.push(`/payment/${orderId}`)
            } else {
              ElMessage.warning('订单创建成功，但未获取到订单ID，请到订单中心查看')
              router.push('/orders')
            }
            resolve()
            return
          } else if (data.status === 'PENDING') {
            poll()
          } else {
            ElMessage.error('抢购失败')
            fetchList()
            resolve()
            return
          }
        } catch {
          poll()
          return
        }
        resolve()
      }, 1000)
    })
  }

  await poll()
}

onMounted(() => {
  fetchList()
  startTimer()
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
/* ===== 全局 ===== */
.fs-page {
  min-height: 100vh;
  background: #f7f8fa;
  font-family: 'PingFang SC', 'Microsoft YaHei', sans-serif;
  padding-bottom: 60px;
}

/* ===== 版心 ===== */
.container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 16px;
}

/* ===== Banner 通栏 ===== */
.fs-banner {
  background: linear-gradient(90deg, #b80c1a, #d11020);
  padding: 32px 0;
}

.fs-banner-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.fs-banner-title {
  display: flex;
  align-items: center;
  gap: 14px;
}

.fs-banner-icon {
  width: 40px;
  height: 40px;
  color: #fff;
  filter: drop-shadow(0 2px 6px rgba(0, 0, 0, 0.2));
}

.fs-banner-text {
  font-size: 30px;
  font-weight: 700;
  color: #fff;
  letter-spacing: 3px;
}

/* 倒计时 */
.fs-banner-countdown {
  display: flex;
  align-items: center;
  gap: 6px;
}

.fs-banner-cd-label {
  color: rgba(255, 255, 255, 0.55);
  font-size: 13px;
  margin-right: 6px;
  letter-spacing: 1px;
}

.fs-banner-cd-box {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 36px;
  height: 42px;
  padding: 0 4px;
  background: rgba(0, 0, 0, 0.35);
  color: #fff;
  font-size: 22px;
  font-weight: 700;
  border-radius: 6px;
  font-family: 'Courier New', 'PingFang SC', monospace;
  letter-spacing: 1px;
}

.fs-banner-cd-colon {
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  min-width: 10px;
  padding: 0;
  font-size: 20px;
}

/* ===== 商品列表 ===== */
.fs-list {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px 20px;
  padding: 24px 0;
}

/* ===== 商品卡片 ===== */
.fs-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  transition: transform 0.25s ease, box-shadow 0.25s ease;
}

.fs-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}

/* 图片区 */
.fs-card-img {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  background: #fafafa;
}

.fs-card-img .el-image {
  width: 100%;
  height: 100%;
}

.fs-card-img :deep(.el-image__inner) {
  object-fit: cover;
  object-position: center;
}

/* 斜角秒杀角标 — 右上角裁切斜边 */
.fs-card-badge {
  position: absolute;
  top: 0;
  right: 0;
  background: linear-gradient(135deg, #c81623, #e02020);
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  padding: 5px 10px 5px 18px;
  clip-path: polygon(20% 0, 100% 0, 100% 100%, 0 100%);
  letter-spacing: 1px;
  border-radius: 0 12px 0 0;
}

/* 卡片内容区 */
.fs-card-body {
  padding: 16px 16px 16px 16px;
}

.fs-card-name {
  font-size: 15px;
  color: #222;
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 10px;
}

/* 价格区 — 原价浮动在秒杀价右上角 */
.fs-card-price-row {
  margin-bottom: 10px;
}

.fs-card-flash-price {
  position: relative;
  display: inline-block;
  color: #c81623;
  font-size: 22px;
  font-weight: 700;
  line-height: 1;
}

.fs-card-price-sym {
  font-size: 14px;
  font-weight: 700;
}

.fs-card-original-price {
  position: absolute;
  top: -2px;
  right: -56px;
  color: #999;
  font-size: 12px;
  font-weight: 400;
  text-decoration: line-through;
  white-space: nowrap;
}

/* 底部信息栏 */
.fs-card-info {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.fs-card-stock {
  font-size: 12px;
  color: #999;
  transition: color 0.2s;
}

.fs-card-stock-low {
  color: #c81623;
}

.fs-card-stock-urgent {
  color: #c81623;
  font-weight: 600;
}

.fs-card-stock-tag {
  display: inline-block;
  margin-left: 6px;
  background: #ffeaea;
  color: #c81623;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 11px;
  font-weight: 600;
}

/* 抢购按钮 */
.fs-card-btn {
  width: 120px;
  height: 34px;
  border-radius: 8px;
  background: #c81623;
  border: none;
  color: #fff;
  font-weight: 600;
  font-size: 13px;
  transition: background 0.2s, transform 0.2s;
  padding: 0;
  flex-shrink: 0;
}

.fs-card-btn:hover:not(:disabled) {
  background: #a8101d;
  transform: translateY(-1px) scale(1.03);
  color: #fff;
}

.fs-card-btn:disabled {
  background: #e8e8e8;
  color: #bbb;
  cursor: not-allowed;
}

/* ===== 详情弹窗 ===== */
.fs-detail {
  display: flex;
  gap: 24px;
}

.fs-detail-left {
  flex: 0 0 300px;
}

.fs-detail-img {
  width: 100%;
  aspect-ratio: 1;
  border-radius: 8px;
}

.fs-detail-right {
  flex: 1;
}

.fs-detail-name {
  font-size: 20px;
  font-weight: 600;
  color: #333;
  margin: 0 0 16px 0;
}

.fs-detail-price-row {
  margin-bottom: 12px;
}

.fs-detail-flash-price {
  position: relative;
  display: inline-block;
  color: #c81623;
  font-size: 32px;
  font-weight: 700;
}

.fs-detail-price-sym {
  font-size: 16px;
  font-weight: 700;
}

.fs-detail-original-price {
  position: absolute;
  top: 2px;
  right: -72px;
  color: #999;
  font-size: 14px;
  text-decoration: line-through;
  white-space: nowrap;
}

.fs-detail-stock,
.fs-detail-limit,
.fs-detail-time {
  font-size: 14px;
  color: #666;
  margin-bottom: 8px;
}

.fs-detail-stock-num {
  color: #c81623;
  font-weight: 600;
}

.fs-detail-stock-urgent .fs-detail-stock-num {
  color: #c81623;
}

.fs-detail-stock-tag {
  display: inline-block;
  margin-left: 8px;
  background: #ffeaea;
  color: #c81623;
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
}

.fs-detail-btn {
  margin-top: 20px;
  width: 100%;
  border-radius: 8px;
  background: #c81623;
  border: none;
  color: #fff;
  font-weight: 600;
  font-size: 16px;
  height: 44px;
  transition: background 0.2s, transform 0.2s;
}

.fs-detail-btn:hover:not(:disabled) {
  background: #a8101d;
  transform: translateY(-1px);
  color: #fff;
}

.fs-detail-btn:disabled {
  background: #e8e8e8;
  color: #bbb;
}

/* ===== 响应式 ===== */
@media (max-width: 768px) {
  .fs-list {
    grid-template-columns: repeat(2, 1fr);
    gap: 12px;
    padding: 12px;
  }
  .fs-detail {
    flex-direction: column;
  }
  .fs-detail-left {
    flex: none;
  }
  .fs-banner-text {
    font-size: 22px;
  }
  .fs-banner-cd-box {
    min-width: 28px;
    height: 34px;
    font-size: 18px;
  }
}
</style>