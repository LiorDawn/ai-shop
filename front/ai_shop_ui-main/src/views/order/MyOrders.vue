<template>
  <div class="mo-page">
    <!-- 顶部导航 -->
    <HeaderUser />

    <div class="mo-container">
      <h2 class="mo-title">我的订单</h2>

      <!-- 状态筛选 -->
      <div class="mo-filter">
        <span
          v-for="s in statusTabs"
          :key="s.value"
          class="mo-filter-item"
          :class="{ active: activeStatus === s.value }"
          @click="onStatusChange(s.value)"
        >{{ s.label }}</span>
        <div class="mo-search">
          <el-input
            v-model="searchKey"
            placeholder="订单号 / 商品名"
            clearable
            size="small"
            style="width: 200px"
            @keyup.enter="fetchOrders"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
      </div>

      <!-- 订单列表 -->
      <div v-loading="loading">
        <div v-if="orders.length === 0 && !loading" class="mo-empty">
          <el-empty description="暂无订单" />
        </div>

        <div
          v-for="order in orders"
          :key="order.id"
          class="mo-order-card"
        >
          <!-- 订单头部 -->
          <div class="mo-order-header">
            <div class="mo-order-shop">
              <span class="mo-shop-name">{{ order.shopName || '官方自营' }}</span>
              <el-tag :type="statusTagType(order.orderStatus)" size="small">
                {{ statusText(order.orderStatus) }}
              </el-tag>
            </div>
            <div class="mo-order-no">
              订单号：{{ order.orderNo }}
              <span class="mo-order-time">{{ order.createTime }}</span>
            </div>
          </div>

          <!-- 商品列表 -->
          <div
            v-for="item in getOrderItems(order)"
            :key="item.id"
            class="mo-order-item"
          >
            <img :src="item.productImage" class="mo-item-img" @click="showDetail(order)" />
            <div class="mo-item-info" @click="showDetail(order)">
              <div class="mo-item-name">{{ item.productName }}</div>
              <div class="mo-item-spec" v-if="item.spec">{{ item.spec }}</div>
            </div>
            <div class="mo-item-price" @click="showDetail(order)">¥{{ fmtPrice(item.price) }}</div>
            <div class="mo-item-num" @click="showDetail(order)">x{{ item.num }}</div>
            <!-- 操作区 -->
            <div class="mo-item-action">
              <!-- 已完成 && 未售后 && 未评价 -->
              <template v-if="order.orderStatus === 3 && !item.afterSaleId && !item.hasComment">
                <el-button size="small" type="danger" @click.stop="goAfterSaleItem(order, item)">申请售后</el-button>
              </template>
              <!-- 已售后 -->
              <template v-if="item.afterSaleId">
                <el-tag size="small" type="warning">已申请售后</el-tag>
              </template>
              <!-- 已评价且未售后 -->
              <template v-if="item.hasComment && !item.afterSaleId">
                <el-tag size="small" type="success">已评价</el-tag>
              </template>
            </div>
          </div>

          <!-- 订单底部 -->
          <div class="mo-order-footer">
            <div class="mo-order-total">
              共 {{ getTotalNum(order) }} 件，实付
              <span class="mo-total-price">¥{{ fmtPrice(order.actualPrice) }}</span>
            </div>
            <div class="mo-order-actions">
              <!-- 待付款 -->
              <template v-if="order.orderStatus === 0">
                <el-button size="small" @click="cancelOrder(order)">取消订单</el-button>
                <el-button size="small" type="danger" @click="payOrder(order)">去付款</el-button>
              </template>
              <!-- 待发货 -->
              <template v-if="order.orderStatus === 1">
                <el-button size="small" @click="applyRefund(order)">申请退款</el-button>
              </template>
              <!-- 待收货 -->
              <template v-if="order.orderStatus === 2">
                <el-button size="small" @click="showLogistics(order)">查看物流</el-button>
                <el-button size="small" type="primary" @click="handleConfirmReceive(order)">确认收货</el-button>
              </template>
              <!-- 已完成 -->
              <template v-if="order.orderStatus === 3">
                <el-button size="small" @click="buyAgain(order)">再次购买</el-button>
                <el-button size="small" type="warning" @click="goComment(order)">去评价</el-button>
              </template>
              <!-- 已取消/已退款 -->
              <template v-if="order.orderStatus === 4 || order.orderStatus === 5">
                <el-button size="small" @click="showDetail(order)">查看详情</el-button>
              </template>
            </div>
          </div>
        </div>

        <el-pagination
          v-if="total > pageSize"
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchOrders"
          class="mo-pager"
        />
      </div>
    </div>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="700px">
      <div v-if="detailOrder" class="mo-detail">
        <div class="mo-detail-header">
          <span>订单号：{{ detailOrder.orderNo }}</span>
          <el-tag :type="statusTagType(detailOrder.orderStatus)">
            {{ statusText(detailOrder.orderStatus) }}
          </el-tag>
        </div>
        <!-- 收货地址 -->
        <div class="mo-detail-section">
          <div class="mo-detail-label">收货信息</div>
          <div>{{ detailOrder.receiver }} {{ detailOrder.receiverPhone }}</div>
          <div>{{ detailOrder.address }}</div>
        </div>
        <!-- 商品明细 -->
        <div class="mo-detail-section">
          <div class="mo-detail-label">商品明细</div>
          <div v-for="item in detailOrder.items" :key="item.id" class="mo-detail-item">
            <img :src="item.productImage" class="mo-detail-img" />
            <div class="mo-detail-item-info">
              <div>{{ item.productName }}</div>
              <div class="mo-detail-spec" v-if="item.spec">{{ item.spec }}</div>
            </div>
            <div class="mo-detail-item-price">¥{{ fmtPrice(item.price) }} x{{ item.num }}</div>
          </div>
        </div>
        <!-- 金额明细 -->
        <div class="mo-detail-section">
          <div class="mo-detail-label">金额明细</div>
          <div class="mo-amount-row">
            <span>商品金额</span><span>¥{{ fmtPrice(detailOrder.totalPrice) }}</span>
          </div>
          <div class="mo-amount-row" v-if="detailOrder.couponPrice > 0">
            <span>优惠金额</span><span class="mo-amount-discount">-¥{{ fmtPrice(detailOrder.couponPrice) }}</span>
          </div>
          <div class="mo-amount-row">
            <span>运费</span><span>¥0.00</span>
          </div>
          <div class="mo-amount-row mo-amount-total">
            <span>实付金额</span><span class="mo-total-price">¥{{ fmtPrice(detailOrder.actualPrice) }}</span>
          </div>
        </div>
        <!-- 物流信息 -->
        <div class="mo-detail-section" v-if="detailOrder.logistics">
          <div class="mo-detail-label">物流信息</div>
          <div>快递单号：{{ detailOrder.logistics }}</div>
        </div>
      </div>
    </el-dialog>

    <!-- 物流弹窗 -->
    <el-dialog v-model="logisticsVisible" title="物流追踪" width="500px">
      <el-timeline v-if="logisticsOrder">
        <el-timeline-item
          v-for="(t, idx) in mockLogistics"
          :key="idx"
          :timestamp="t.time"
          :color="idx === 0 ? '#e4393c' : '#ccc'"
        >{{ t.text }}</el-timeline-item>
      </el-timeline>
    </el-dialog>

    <!-- 售后申请弹窗 -->
    <el-dialog v-model="refundVisible" title="申请售后" width="480px">
      <el-form :model="refundForm" label-width="80px">
        <el-form-item label="售后类型">
          <el-radio-group v-model="refundForm.type">
            <el-radio :value="0">仅退款</el-radio>
            <el-radio :value="1">退货退款</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="退款金额">
          <el-input-number v-model="refundForm.amount" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="售后原因">
          <el-input v-model="refundForm.reason" placeholder="请描述售后原因" />
        </el-form-item>
        <el-form-item label="问题描述">
          <el-input v-model="refundForm.description" type="textarea" :rows="3" placeholder="详细描述问题" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRefund" :loading="refunding">提交申请</el-button>
      </template>
    </el-dialog>

  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import {
  getMyOrders, getMyOrderDetail, cancelMyOrder, confirmReceive,
  type OrderDTO, type OrderDetailDTO, type PageResult
} from '@/api/order'
import { applyAfterSale } from '@/api/afterSale'
import { addToCart } from '@/api/cart'

const router = useRouter()

const statusTabs = [
  { label: '全部', value: -1 },
  { label: '待付款', value: 0 },
  { label: '待发货', value: 1 },
  { label: '待收货', value: 2 },
  { label: '已完成', value: 3 },
  { label: '已取消', value: 4 },
]

const loading = ref(false)
const orders = ref<OrderDTO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const activeStatus = ref(-1)
const searchKey = ref('')

// 订单详情缓存
const orderItemsCache = ref<Map<number, any[]>>(new Map())

function statusText(status: number) {
  const map: Record<number, string> = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消' }
  return map[status] || '未知'
}
function statusTagType(status: number) {
  const map: Record<number, string> = { 0: 'warning', 1: '', 2: '', 3: 'success', 4: 'info' }
  return map[status] || 'info'
}
function fmtPrice(p: number | undefined | null): string {
  if (p === undefined || p === null || isNaN(p as number)) return '0.00'
  return Number(p).toFixed(2)
}

async function fetchOrders() {
  loading.value = true
  try {
    const fetchSize = activeStatus.value === -1 ? pageSize.value : 200
    const params: any = { current: currentPage.value, size: fetchSize }
    if (searchKey.value) params.orderNo = searchKey.value
    const res = await getMyOrders(params)
    const data: PageResult<OrderDTO> = res.data
    orders.value = data.records || []
    total.value = data.total || 0

    // 客户端筛选（保证各 tab 数据量与"全部"一致）
    if (activeStatus.value !== -1) {
      orders.value = orders.value.filter(o => o.orderStatus === activeStatus.value)
      total.value = orders.value.length
    }

    // 预加载订单商品明细
    for (const o of orders.value) {
      try {
        const detail = await getMyOrderDetail(o.id)
        orderItemsCache.value.set(o.id, detail.data.items || [])
      } catch {
        orderItemsCache.value.set(o.id, [])
      }
    }
  } catch {
    ElMessage.error('加载订单失败')
  } finally {
    loading.value = false
  }
}

function getOrderItems(order: OrderDTO) {
  return orderItemsCache.value.get(order.id) || []
}

function getTotalNum(order: OrderDTO) {
  const items = orderItemsCache.value.get(order.id) || []
  return items.reduce((sum: number, i: any) => sum + i.num, 0)
}

function onStatusChange(status: number) {
  activeStatus.value = status
  currentPage.value = 1
  fetchOrders()
}

// === 弹窗状态 ===
const detailVisible = ref(false)
const detailOrder = ref<OrderDetailDTO | null>(null)
const logisticsVisible = ref(false)
const logisticsOrder = ref<OrderDTO | null>(null)
const refundVisible = ref(false)
const refunding = ref(false)
const refundForm = ref({ type: 0, amount: 0, reason: '', description: '', orderId: 0, orderItemId: 0 })

// 模拟物流
const mockLogistics = [
  { time: '快递员派送中', text: '您的快递正在派送中' },
  { time: '到达目的地网点', text: '快递已到达【目的地网点】' },
  { time: '运输中', text: '快递正在运输中' },
  { time: '已揽收', text: '快递已被揽收' },
]

async function showDetail(order: OrderDTO) {
  try {
    const res = await getMyOrderDetail(order.id)
    detailOrder.value = res.data
    detailVisible.value = true
  } catch {
    ElMessage.error('加载订单详情失败')
  }
}

function showLogistics(order: OrderDTO) {
  logisticsOrder.value = order
  logisticsVisible.value = true
}

async function cancelOrder(order: OrderDTO) {
  try {
    await ElMessageBox.confirm('确定取消该订单？', '提示', { type: 'warning' })
    await cancelMyOrder(order.id)
    ElMessage.success('订单已取消')
    fetchOrders()
  } catch {
    // cancelled
  }
}

async function handleConfirmReceive(order: OrderDTO) {
  try {
    await ElMessageBox.confirm('确认已收到商品？', '提示', { type: 'warning' })
    await confirmReceive(order.id)
    ElMessage.success('确认收货成功')
    fetchOrders()
  } catch {
    // cancelled
  }
}

function payOrder(order: OrderDTO) {
  router.push('/payment/' + order.id)
}

function applyRefund(order: OrderDTO) {
  const items = orderItemsCache.value.get(order.id) || []
  if (items.length === 0) {
    ElMessage.warning('暂无商品信息')
    return
  }
  refundForm.value = {
    type: 0,
    amount: order.actualPrice as number,
    reason: '',
    description: '',
    orderId: order.id as number,
    orderItemId: (items[0] as any).id || 0,
  }
  refundVisible.value = true
}

async function submitRefund() {
  if (!refundForm.value.reason) {
    ElMessage.warning('请填写售后原因')
    return
  }
  refunding.value = true
  try {
    await applyAfterSale(refundForm.value)
    ElMessage.success('售后申请已提交')
    refundVisible.value = false
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '申请失败')
  } finally {
    refunding.value = false
  }
}

function buyAgain(order: OrderDTO) {
  const items = orderItemsCache.value.get(order.id) || []
  items.forEach(async (item: any) => {
    try {
      await addToCart({ productId: item.productId, num: item.num })
    } catch { /* ignore */ }
  })
  ElMessage.success('已加入购物车')
  router.push('/cart')
}

function goComment(order: OrderDTO) {
  const items = orderItemsCache.value.get(order.id) || []
  if (items.length === 0) {
    ElMessage.warning('暂无商品信息')
    return
  }
  const item = items[0] as any
  router.push({
    path: '/review/write',
    query: {
      orderId: order.id,
      orderNo: order.orderNo,
      productId: item.productId || 0,
      productName: item.productName || '',
      productImage: item.productImage || '',
      spec: item.spec || '',
      price: item.price || 0,
      shopId: item.shopId || 0,
      shopName: item.shopName || '',
    },
  })
}

function goAfterSale(order: OrderDTO) {
  const items = orderItemsCache.value.get(order.id) || []
  if (items.length === 0) {
    ElMessage.warning('暂无商品信息')
    return
  }
  router.push({
    path: '/aftersale/apply',
    query: {
      orderId: order.id as number,
      orderItemId: (items[0] as any).id || 0,
    },
  })
}

function goAfterSaleItem(order: OrderDTO, item: any) {
  router.push({
    path: '/aftersale/apply',
    query: {
      orderId: order.id as number,
      orderItemId: item.id || 0,
      productId: item.productId,
      productName: item.productName,
      productImage: item.productImage,
      spec: item.spec,
      price: item.price,
      num: item.num,
    },
  })
}

onMounted(() => {
  fetchOrders()
})
</script>

<style scoped>
.mo-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.mo-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px;
}
.mo-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 16px;
}
.mo-filter {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #fff;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
}
.mo-filter-item {
  padding: 6px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
}
.mo-filter-item.active {
  background: #e4393c;
  color: #fff;
}
.mo-search {
  margin-left: auto;
}
.mo-empty {
  background: #fff;
  border-radius: 8px;
  padding: 40px;
}
.mo-order-card {
  background: #fff;
  border-radius: 8px;
  margin-bottom: 16px;
  overflow: hidden;
}
.mo-order-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}
.mo-order-shop {
  display: flex;
  align-items: center;
  gap: 12px;
}
.mo-shop-name {
  font-weight: 600;
  font-size: 14px;
}
.mo-order-no {
  font-size: 12px;
  color: #999;
}
.mo-order-time {
  margin-left: 16px;
}
.mo-order-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 16px;
  border-bottom: 1px solid #f5f5f5;
  cursor: pointer;
}
.mo-order-item:hover {
  background: #fafafa;
}
.mo-item-img {
  width: 64px;
  height: 64px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}
.mo-item-info {
  flex: 1;
  min-width: 0;
}
.mo-item-name {
  font-size: 14px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mo-item-spec {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}
.mo-item-price {
  font-size: 14px;
  color: #333;
  min-width: 60px;
  text-align: right;
}
.mo-item-num {
  font-size: 13px;
  color: #999;
  min-width: 40px;
  text-align: right;
}
.mo-item-action {
  min-width: 100px;
  text-align: center;
  flex-shrink: 0;
}
.mo-order-footer {
  display: flex;
  justify-content: flex-end;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
}
.mo-order-total {
  font-size: 14px;
  color: #666;
}
.mo-total-price {
  font-size: 16px;
  font-weight: 700;
  color: #e4393c;
}
.mo-order-actions {
  display: flex;
  gap: 8px;
}
.mo-pager {
  margin-top: 16px;
  justify-content: center;
}

/* 详情弹窗 */
.mo-detail-section {
  margin-bottom: 16px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}
.mo-detail-label {
  font-weight: 600;
  font-size: 14px;
  color: #333;
  margin-bottom: 8px;
}
.mo-detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 12px;
  border-bottom: 1px solid #eee;
  margin-bottom: 16px;
}
.mo-detail-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}
.mo-detail-img {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
}
.mo-detail-item-info {
  flex: 1;
  font-size: 14px;
}
.mo-detail-spec {
  font-size: 12px;
  color: #999;
}
.mo-detail-item-price {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}
.mo-amount-row {
  display: flex;
  justify-content: space-between;
  padding: 4px 0;
  font-size: 14px;
  color: #666;
}
.mo-amount-discount {
  color: #e4393c;
}
.mo-amount-total {
  font-weight: 600;
  font-size: 15px;
  color: #333;
  padding-top: 8px;
  border-top: 1px solid #eee;
  margin-top: 4px;
}
</style>