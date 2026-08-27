<template>
  <div class="pay-page">
    <HeaderUser />
    <div class="pay-container" v-loading="loading">
      <div class="pay-card">
        <!-- 支付成功 -->
        <div v-if="paid" class="pay-success">
          <el-icon style="font-size: 64px; color: #67c23a"><CircleCheckFilled /></el-icon>
          <h2>支付成功</h2>
          <p>订单号：{{ order?.orderNo }}</p>
          <p style="margin-top: 8px">实付金额：<span class="pay-amount">¥{{ fmtPrice(order?.actualPrice) }}</span></p>
          <p class="pay-redirect-tip">{{ countdown }} 秒后自动返回订单页面</p>
          <el-button type="primary" style="margin-top: 20px" @click="goOrders">查看订单</el-button>
        </div>

        <!-- 未支付：展示订单信息 + 支付按钮 -->
        <template v-else>
          <h2 class="pay-title">订单支付</h2>
          <div class="pay-info">
            <p>订单号：{{ order?.orderNo }}</p>
            <p>实付金额：<span class="pay-amount">¥{{ fmtPrice(order?.actualPrice) }}</span></p>
          </div>

          <div class="pay-status">
            <el-tag :type="statusType">{{ statusText }}</el-tag>
          </div>

          <div class="pay-actions">
            <el-button
              type="primary"
              size="large"
              :loading="paying"
              @click="goAlipay"
            >
              <el-icon style="margin-right: 4px"><Wallet /></el-icon>
              支付宝支付
            </el-button>
            <el-button
              type="warning"
              size="large"
              :loading="simulating"
              @click="simulatePayClick"
            >
              模拟支付成功
            </el-button>
            <el-button size="large" @click="goOrders">稍后支付</el-button>
          </div>

          <div class="pay-tips">
            <el-icon style="margin-right: 4px"><InfoFilled /></el-icon>
            点击「支付宝支付」将跳转到支付宝官方支付页面。请使用「沙箱版支付宝」账号完成支付。
          </div>
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import HeaderUser from '../../components/layout/HeaderUser.vue'
import { getMyOrderDetail, alipayCreatePay, simulatePay, queryPayStatus } from '../../api/order'
import { ElMessage } from 'element-plus'
import { CircleCheckFilled, Wallet, InfoFilled } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const paying = ref(false)
const simulating = ref(false)
const order = ref<any>(null)
const paid = ref(false)
const pollTimer = ref<number>(0)
const redirectTimer = ref<number>(0)
const countdown = ref(5)
const statusText = ref('等待支付')
const statusType = ref<string>('warning')

/** sessionStorage key 前缀，用于标记哪个订单已发起支付宝支付 */
const PAYING_KEY = 'paying_order_id'

function fmtPrice(v: any) {
  if (v == null) return '0.00'
  return Number(v).toFixed(2)
}

async function loadOrder() {
  const orderId = route.params.id
  if (!orderId) return
  try {
    const res: any = await getMyOrderDetail(Number(orderId))
    order.value = res.data
    if (order.value.payStatus === 1) {
      paid.value = true
      statusText.value = '支付成功'
      statusType.value = 'success'
    }
  } catch (e: any) {
    ElMessage.error('订单不存在')
    router.push('/orders')
  }
}

async function goAlipay() {
  if (!order.value) return
  paying.value = true
  statusText.value = '正在生成支付请求…'
  try {
    const res: any = await alipayCreatePay(Number(order.value.id))
    const formHtml = res.data
    if (!formHtml) {
      ElMessage.error('支付请求失败')
      paying.value = false
      return
    }

    // 标记此订单已发起支付宝支付
    sessionStorage.setItem(PAYING_KEY, String(order.value.id))
    statusText.value = '正在跳转支付宝…'

    // ★ 关键修复：在 document.body 上创建临时容器（不在 Vue 组件内）
    // 避免 Vue 响应式重渲染时清空 innerHTML 导致表单导航中断
    const tempDiv = document.createElement('div')
    tempDiv.style.display = 'none'
    document.body.appendChild(tempDiv)
    tempDiv.innerHTML = formHtml
    const form = tempDiv.querySelector('form') as HTMLFormElement | null

    if (form) {
      form.target = '_self'
      form.submit()
      // 不重置 paying —— 浏览器导航离开后 Vue 状态无关紧要
      // 兜底：5 秒后如果页面还在（导航失败），重置按钮状态
      setTimeout(() => { paying.value = false }, 5000)
    } else {
      document.body.removeChild(tempDiv)
      ElMessage.error('支付表单加载失败')
      paying.value = false
    }
  } catch (e: any) {
    ElMessage.error(e.message || '支付请求失败')
    paying.value = false
  }
}

async function simulatePayClick() {
  if (!order.value) return
  simulating.value = true
  try {
    await simulatePay(Number(order.value.id))
    paid.value = true
    statusText.value = '支付成功'
    statusType.value = 'success'
    stopPolling()
    sessionStorage.removeItem(PAYING_KEY)
    startRedirectCountdown()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '模拟支付失败')
  } finally {
    simulating.value = false
  }
}

function startPolling() {
  if (pollTimer.value) return // 防止重复启动
  pollTimer.value = window.setInterval(async () => {
    const orderId = route.params.id
    if (!orderId) return
    try {
      // 主动向支付宝查询支付状态（后端自动更新订单）
      const res: any = await queryPayStatus(Number(orderId))
      const paidResult = res.data
      if (paidResult === true) {
        // 重新加载订单获取最新状态
        const detail: any = await getMyOrderDetail(Number(orderId))
        order.value = detail.data
        paid.value = true
        statusText.value = '支付成功'
        statusType.value = 'success'
        stopPolling()
        sessionStorage.removeItem(PAYING_KEY)
        startRedirectCountdown()
      }
    } catch { /* 轮询中网络异常静默忽略 */ }
  }, 3000)
}

function stopPolling() {
  if (pollTimer.value) {
    clearInterval(pollTimer.value)
    pollTimer.value = 0
  }
}

function startRedirectCountdown() {
  countdown.value = 5
  redirectTimer.value = window.setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) {
      stopRedirectTimer()
      router.push('/orders')
    }
  }, 1000)
}

function stopRedirectTimer() {
  if (redirectTimer.value) {
    clearInterval(redirectTimer.value)
    redirectTimer.value = 0
  }
}

function goOrders() {
  stopRedirectTimer()
  router.push('/orders')
}

onMounted(async () => {
  loading.value = true
  await loadOrder()
  if (!paid.value) {
    // 只有当用户已发起支付（sessionStorage 有标记）时才启动轮询
    // 避免用户首次进入页面时就向支付宝发起无效查询
    const payingOrderId = sessionStorage.getItem(PAYING_KEY)
    if (payingOrderId === String(order.value?.id)) {
      statusText.value = '等待支付完成…'
      startPolling()
    }
  }
  loading.value = false
})

onUnmounted(() => {
  stopPolling()
  stopRedirectTimer()
})
</script>

<style scoped>
.pay-page { min-height: 100vh; background: #f5f5f5; }
.pay-container { max-width: 600px; margin: 0 auto; padding: 40px 20px; }
.pay-card { background: #fff; border-radius: 12px; padding: 40px; text-align: center; box-shadow: 0 2px 12px rgba(0,0,0,0.06); }

.pay-title { font-size: 20px; font-weight: 700; margin-bottom: 20px; }
.pay-info { margin-bottom: 24px; color: #666; }
.pay-info p { margin: 6px 0; }
.pay-amount { font-size: 24px; font-weight: 700; color: #e4393c; }

.pay-status { margin: 16px 0; }
.pay-actions { margin-top: 24px; display: flex; gap: 12px; justify-content: center; }

.pay-tips { margin-top: 32px; padding: 12px 16px; background: #fdf6ec; border-radius: 6px; color: #e6a23c; font-size: 13px; display: flex; align-items: center; justify-content: center; }

.pay-success { padding: 20px 0; }
.pay-success h2 { margin: 12px 0; font-size: 22px; color: #333; }
.pay-success p { color: #666; }
.pay-redirect-tip { margin-top: 16px; font-size: 14px; color: #67c23a; }
</style>