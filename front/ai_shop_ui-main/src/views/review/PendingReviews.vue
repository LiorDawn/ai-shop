<template>
  <div class="pr-page">
    <HeaderUser />

    <div class="pr-container">
      <h2 class="pr-title">评价晒单</h2>

      <!-- 筛选栏 -->
      <div class="pr-tabs">
        <span
          v-for="t in tabs"
          :key="t.value"
          class="pr-tab"
          :class="{ active: activeTab === t.value }"
          @click="switchTab(t.value)"
        >{{ t.label }}<span v-if="t.count !== undefined" class="pr-tab-count">{{ t.count }}</span></span>
      </div>

      <div v-loading="loading">
        <!-- 待评价列表 -->
        <template v-if="activeTab === 'pending'">
          <div v-if="pendingOrders.length === 0" class="pr-empty">
            <el-empty description="暂无待评价订单" />
          </div>
          <div v-for="order in pendingOrders" :key="order.id" class="pr-order-card">
            <div class="pr-order-header">
              <span>订单号：{{ order.orderNo }}</span>
              <span class="pr-order-time">{{ order.createTime }}</span>
            </div>
            <div
              v-for="item in getOrderItems(order)"
              :key="item.id"
              class="pr-order-item"
            >
              <img :src="item.productImage || 'https://picsum.photos/seed/default/80/80'" class="pr-item-img" />
              <div class="pr-item-info">
                <div class="pr-item-name">{{ item.productName }}</div>
                <div class="pr-item-spec" v-if="item.spec">{{ item.spec }}</div>
              </div>
              <div class="pr-item-price">¥{{ fmtPrice(item.price) }}</div>
              <div class="pr-item-num">x{{ item.num }}</div>
              <div class="pr-item-action">
                <template v-if="isReviewed(order.id, item.productId)">
                  <el-tag type="success" size="small">已评价</el-tag>
                </template>
                <template v-else>
                  <el-button
                    type="danger"
                    size="small"
                    @click="goWrite(order, item)"
                  >去评价</el-button>
                </template>
              </div>
            </div>
            <div class="pr-order-footer">
              实付：<span class="pr-price">¥{{ fmtPrice(order.actualPrice) }}</span>
            </div>
          </div>
        </template>

        <!-- 已评价列表 -->
        <template v-if="activeTab === 'reviewed'">
          <div v-if="myReviews.length === 0" class="pr-empty">
            <el-empty description="暂无评价记录" />
          </div>
          <div v-for="r in myReviews" :key="r.id" class="pr-review-card">
            <div class="pr-review-top">
              <img :src="r.productImage || 'https://picsum.photos/seed/default/80/80'" class="pr-review-img" />
              <div class="pr-review-info">
                <div class="pr-review-name">{{ r.productName }}</div>
                <el-rate v-model="r.score" disabled size="small" />
              </div>
              <span class="pr-review-time">{{ r.createTime }}</span>
            </div>
            <div class="pr-review-content">{{ r.content }}</div>
            <div v-if="r.imageList && r.imageList.length" class="pr-review-imgs">
              <img
                v-for="(img, idx) in r.imageList"
                :key="idx"
                :src="img"
                class="pr-review-img-item"
                @click="previewImg(img)"
              />
            </div>
            <div v-if="r.reply" class="pr-review-reply">
              <span class="pr-reply-label">商家回复：</span>{{ r.reply }}
            </div>
          </div>
          <el-pagination
            v-if="reviewTotal > reviewPageSize"
            v-model:current-page="reviewPage"
            :page-size="reviewPageSize"
            :total="reviewTotal"
            layout="prev, pager, next"
            @current-change="fetchMyReviews"
            class="pr-pager"
          />
        </template>

        <!-- 全部 -->
        <template v-if="activeTab === 'all'">
          <div v-if="allItems.length === 0" class="pr-empty">
            <el-empty description="暂无记录" />
          </div>
          <div v-for="item in allItems" :key="'a' + item.orderId + '_' + item.productId" class="pr-all-card">
            <img :src="item.productImage || 'https://picsum.photos/seed/default/80/80'" class="pr-all-img" />
            <div class="pr-all-info">
              <div class="pr-all-name">{{ item.productName }}</div>
              <div class="pr-all-spec" v-if="item.spec">{{ item.spec }}</div>
            </div>
            <div class="pr-all-price">¥{{ fmtPrice(item.price) }}</div>
            <div class="pr-all-status">
              <el-tag v-if="item.reviewed" type="success" size="small">已评价</el-tag>
              <el-tag v-else-if="item.orderData && item.orderData.orderStatus === 3" type="warning" size="small">待评价</el-tag>
              <el-tag v-else type="info" size="small">{{ item.orderData ? statusText(item.orderData.orderStatus) : '未知' }}</el-tag>
            </div>
            <div class="pr-all-action">
              <el-button
                v-if="!item.reviewed && item.orderData && item.orderData.orderStatus === 3"
                type="danger"
                size="small"
                @click="goWrite(item.orderData, item)"
              >去评价</el-button>
            </div>
            <!-- 已评价显示评价内容 -->
            <div v-if="item.reviewed && item.reviewData" class="pr-all-review">
              <div class="pr-all-review-rate">
                <el-rate v-model="item.reviewData.score" disabled size="small" />
              </div>
              <div class="pr-all-review-content">{{ item.reviewData.content }}</div>
              <div v-if="item.reviewData.imageList?.length" class="pr-all-review-imgs">
                <img
                  v-for="(img, idx) in item.reviewData.imageList"
                  :key="idx"
                  :src="img"
                  class="pr-all-review-img"
                  @click="previewImg(img)"
                />
              </div>
            </div>
          </div>
          <el-pagination
            v-if="allTotal > allPageSize"
            v-model:current-page="allPage"
            :page-size="allPageSize"
            :total="allTotal"
            layout="prev, pager, next"
            @current-change="fetchAll"
            class="pr-pager"
          />
        </template>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import { getMyOrders, getMyOrderDetail, type OrderDTO } from '@/api/order'
import { getMyComments, type CommentDTO } from '@/api/comment'

const router = useRouter()

const tabs = [
  { label: '全部', value: 'all' },
  { label: '待评价', value: 'pending' },
  { label: '已评价', value: 'reviewed' },
]
const activeTab = ref('all')

const loading = ref(false)
const pendingOrders = ref<OrderDTO[]>([])
const orderItemsCache = ref<Map<number, any[]>>(new Map())

// 已评价
const myReviews = ref<CommentDTO[]>([])
const reviewPage = ref(1)
const reviewPageSize = ref(10)
const reviewTotal = ref(0)

// 全部
const allItems = ref<any[]>([])
const allPage = ref(1)
const allPageSize = ref(10)
const allTotal = ref(0)

// 已评价的 orderId_productId 集合
const reviewedSet = ref<Set<string>>(new Set())

function fmtPrice(p: number | undefined | null): string {
  if (p === undefined || p === null) return '0.00'
  return Number(p).toFixed(2)
}

function statusText(status: number): string {
  const map: Record<number, string> = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消' }
  return map[status] || '未知'
}

function getOrderItems(order: OrderDTO) {
  return orderItemsCache.value.get(order.id) || []
}

function isReviewed(orderId: number, productId: number) {
  return reviewedSet.value.has(`${orderId}_${productId}`)
}

async function fetchPendingOrders() {
  try {
    // 同时拉取订单和已评价集合
    const [ordersRes, reviewsRes] = await Promise.all([
      getMyOrders({ current: 1, size: 200 }),
      getMyComments(1, 999),
    ])
    const allOrders: OrderDTO[] = ordersRes.data.records || []

    // 构建已评价集合
    const reviewSet = new Set<string>()
    ;(reviewsRes.data.records || []).forEach((r: CommentDTO) => {
      reviewSet.add(`${r.orderId}_${r.productId}`)
    })
    reviewedSet.value = reviewSet

    // 筛选已完成订单（可评价）
    let completedOrders = allOrders.filter(o => o.orderStatus === 3)

    // 加载订单明细，过滤掉全部已评价的订单
    const result: OrderDTO[] = []
    for (const o of completedOrders) {
      let items: any[]
      try {
        const detail = await getMyOrderDetail(o.id)
        items = detail.data.items || []
      } catch {
        items = []
      }
      orderItemsCache.value.set(o.id, items)
      // 只要还有至少一个商品未评价，就保留该订单
      const hasUnreviewed = items.some((item: any) => !reviewSet.has(`${o.id}_${item.productId}`))
      if (hasUnreviewed) {
        result.push(o)
      }
    }
    pendingOrders.value = result
  } catch {
    ElMessage.error('加载订单失败')
  }
}

async function fetchMyReviews(page = 1) {
  try {
    const res = await getMyComments(page, reviewPageSize.value)
    const data = res.data
    myReviews.value = data.records || []
    reviewTotal.value = data.total || 0
    reviewPage.value = page
  } catch {
    myReviews.value = []
  }
}

async function fetchReviewedSet() {
  // 获取所有已评价记录，构建 Set
  const allReviews: CommentDTO[] = []
  try {
    const res = await getMyComments(1, 999)
    const records = res.data.records || []
    allReviews.push(...records)
  } catch { /* ignore */ }
  allReviews.forEach((r) => {
    reviewedSet.value.add(`${r.orderId}_${r.productId}`)
  })
}

async function fetchAll(page = 1) {
  loading.value = true
  try {
    const [ordersRes, reviewsRes] = await Promise.all([
      getMyOrders({ current: page, size: allPageSize.value }),
      getMyComments(1, 999),
    ])
    const orders: OrderDTO[] = (ordersRes.data.records || []).filter(o => o.orderStatus !== 4)
    allTotal.value = ordersRes.data.total || 0
    allPage.value = page

    // 获取已评价集合及评价详情
    const reviewSet = new Set<string>()
    const reviewMap = new Map<string, CommentDTO>()
    ;(reviewsRes.data.records || []).forEach((r: CommentDTO) => {
      const key = `${r.orderId}_${r.productId}`
      reviewSet.add(key)
      reviewMap.set(key, r)
    })

    // 展开为商品条目
    const items: any[] = []
    for (const o of orders) {
      let orderItems: any[]
      try {
        const detail = await getMyOrderDetail(o.id)
        orderItems = detail.data.items || []
      } catch {
        orderItems = []
      }
      orderItems.forEach((item) => {
        const key = `${o.id}_${item.productId}`
        items.push({
          ...item,
          orderId: o.id,
          orderData: o,
          reviewed: reviewSet.has(key),
          reviewData: reviewMap.get(key) || null,
        })
      })
    }
    allItems.value = items
  } catch {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

function switchTab(tab: string) {
  activeTab.value = tab
  if (tab === 'pending') {
    loading.value = true
    fetchPendingOrders().finally(() => { loading.value = false })
  } else if (tab === 'reviewed') {
    loading.value = true
    fetchMyReviews().finally(() => { loading.value = false })
  } else if (tab === 'all') {
    fetchAll()
  }
}

function goWrite(order: OrderDTO, item: any) {
  router.push({
    path: '/review/write',
    query: {
      orderId: order.id,
      orderNo: order.orderNo,
      productId: item.productId,
      productName: item.productName,
      productImage: item.productImage,
      spec: item.spec || '',
      price: item.price,
      shopId: item.shopId,
      shopName: item.shopName || '',
    },
  })
}

function previewImg(url: string) {
  window.open(url, '_blank')
}

onMounted(() => {
  fetchAll()
})
</script>

<style scoped>
.pr-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.pr-container {
  max-width: 1000px;
  margin: 0 auto;
  padding: 16px 20px 40px;
}
.pr-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 16px;
}

/* Tabs */
.pr-tabs {
  display: flex;
  gap: 4px;
  background: #fff;
  border-radius: 8px;
  padding: 8px 16px;
  margin-bottom: 16px;
}
.pr-tab {
  padding: 6px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 4px;
}
.pr-tab.active {
  background: #e4393c;
  color: #fff;
}
.pr-tab-count {
  font-size: 11px;
  background: rgba(255,255,255,0.3);
  padding: 0 6px;
  border-radius: 8px;
}

/* Empty */
.pr-empty {
  background: #fff;
  border-radius: 8px;
  padding: 40px;
}

/* 待评价订单卡片 */
.pr-order-card {
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
  overflow: hidden;
}
.pr-order-header {
  display: flex;
  justify-content: space-between;
  padding: 10px 16px;
  background: #fafafa;
  border-bottom: 1px solid #f0f0f0;
  font-size: 12px;
  color: #999;
}
.pr-order-time {
  margin-left: 12px;
}
.pr-order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  border-bottom: 1px solid #f5f5f5;
}
.pr-item-img {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}
.pr-item-info {
  flex: 1;
  min-width: 0;
}
.pr-item-name {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.pr-item-spec {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.pr-item-price {
  font-size: 14px;
  color: #333;
  min-width: 60px;
  text-align: right;
}
.pr-item-num {
  font-size: 13px;
  color: #999;
  min-width: 30px;
  text-align: right;
}
.pr-item-action {
  min-width: 70px;
  text-align: center;
}
.pr-order-footer {
  padding: 10px 16px;
  text-align: right;
  font-size: 13px;
  color: #666;
}
.pr-price {
  font-size: 16px;
  font-weight: 700;
  color: #e4393c;
}

/* 已评价卡片 */
.pr-review-card {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 12px;
}
.pr-review-top {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 10px;
}
.pr-review-img {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}
.pr-review-info {
  flex: 1;
  min-width: 0;
}
.pr-review-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 4px;
  font-weight: 500;
}
.pr-review-time {
  font-size: 12px;
  color: #999;
  flex-shrink: 0;
}
.pr-review-content {
  font-size: 14px;
  color: #555;
  line-height: 1.6;
  margin-bottom: 8px;
}
.pr-review-imgs {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 8px;
}
.pr-review-img-item {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #eee;
}
.pr-review-reply {
  font-size: 13px;
  color: #888;
  background: #f9f9f9;
  padding: 8px 12px;
  border-radius: 4px;
  margin-top: 6px;
}
.pr-reply-label {
  color: #e4393c;
  font-weight: 500;
}

/* 全部 */
.pr-all-card {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 12px;
  background: #fff;
  padding: 12px 16px;
  border-radius: 8px;
  margin-bottom: 8px;
}
.pr-all-img {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}
.pr-all-info {
  flex: 1;
  min-width: 0;
}
.pr-all-name {
  font-size: 14px;
  color: #333;
}
.pr-all-spec {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
.pr-all-price {
  font-size: 14px;
  color: #333;
  min-width: 60px;
  text-align: right;
}
.pr-all-status {
  min-width: 60px;
  text-align: center;
}
.pr-all-action {
  min-width: 70px;
}
.pr-all-review {
  width: 100%;
  padding: 10px 0 0 0;
  margin-top: 6px;
  border-top: 1px dashed #eee;
}
.pr-all-review-rate {
  margin-bottom: 4px;
}
.pr-all-review-content {
  font-size: 13px;
  color: #555;
  line-height: 1.5;
  margin-bottom: 6px;
}
.pr-all-review-imgs {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}
.pr-all-review-img {
  width: 60px;
  height: 60px;
  object-fit: cover;
  border-radius: 4px;
  cursor: pointer;
  border: 1px solid #eee;
}
.pr-pager {
  margin-top: 16px;
  justify-content: center;
}
</style>