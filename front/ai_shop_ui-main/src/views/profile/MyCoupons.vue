<template>
  <div class="mc-page">
    <HeaderUser />

    <div class="mc-wrapper">
      <div class="mc-layout">
        <ProfileSidebar :user="user" :stats="stats" />

        <div class="mc-main">
          <!-- Tab 切换栏 -->
          <div class="mc-tabs">
            <div
              v-for="tab in tabs"
              :key="tab.key"
              class="mc-tab"
              :class="{ active: activeTab === tab.key }"
              @click="switchTab(tab.key)"
            >
              <span>{{ tab.label }}</span>
              <span v-if="tab.key === 0 && counts.unused > 0" class="mc-tab-badge">{{ counts.unused > 99 ? '99+' : counts.unused }}</span>
            </div>
          </div>

          <!-- 优惠券列表区域 -->
          <div class="mc-list-wrap" ref="listWrapRef" @scroll="onScroll">
            <!-- 加载中 -->
            <div v-if="loading && list.length === 0" class="mc-loading">
              <el-skeleton :rows="4" animated />
            </div>

            <!-- 空状态 -->
            <div v-else-if="list.length === 0" class="mc-empty">
              <svg width="80" height="80" viewBox="0 0 120 120" fill="none">
                <rect x="15" y="25" width="90" height="70" rx="6" stroke="#d9d9d9" stroke-width="2" fill="#fafafa" />
                <path d="M25 50 L60 65 L95 50" stroke="#d9d9d9" stroke-width="2" stroke-linecap="round" />
                <circle cx="60" cy="42" r="8" stroke="#d9d9d9" stroke-width="2" fill="#fafafa" />
              </svg>
              <p class="mc-empty-text">{{ emptyText }}</p>
              <el-button v-if="activeTab === 0" type="primary" round @click="goToCouponCenter">去领券</el-button>
            </div>

            <!-- 优惠券卡片列表 -->
            <div v-else class="mc-coupon-list">
              <div
                v-for="item in list"
                :key="item.id"
                class="mc-coupon-card"
                :class="{
                  'is-used': item.status === 1,
                  'is-expired': item.status === 2,
                  'is-expiring': isExpiringSoon(item)
                }"
              >
                <!-- 左侧金额区 -->
                <div
                  class="mc-card-left"
                  :class="item.type === 2 ? 'type-discount' : 'type-cash'"
                >
                  <div class="mc-discount">
                    <span class="mc-d-symbol" v-if="item.type === 1">¥</span>
                    <span class="mc-d-num">{{ formatDiscount(item) }}</span>
                    <span class="mc-d-unit" v-if="item.type === 2">折</span>
                  </div>
                  <div class="mc-d-cond">{{ item.minPrice > 0 ? `满¥${item.minPrice}可用` : '无门槛' }}</div>
                  <!-- 圆孔装饰 -->
                  <div class="mc-hole mc-hole-top"></div>
                  <div class="mc-hole mc-hole-bottom"></div>
                </div>

                <!-- 右侧信息区 -->
                <div class="mc-card-right">
                  <div class="mc-card-top">
                    <div class="mc-name-row">
                      <span class="mc-name">{{ item.couponName }}</span>
                      <span class="mc-tag">平台通用</span>
                    </div>
                    <div class="mc-scope">平台全品类可用</div>
                  </div>
                  <div class="mc-card-bottom">
                    <div class="mc-time">
                      {{ formatDate(item.startTime) }} ~ {{ formatDate(item.endTime) }}
                    </div>
                    <div class="mc-card-actions">
                      <template v-if="item.status === 0">
                        <el-button size="small" round @click="showRuleDetail(item)">使用规则</el-button>
                        <el-button size="small" type="primary" round @click="goUse(item)">立即使用</el-button>
                      </template>
                      <template v-else-if="item.status === 1">
                        <span class="mc-used-label">已使用</span>
                        <el-button size="small" text @click="viewOrder(item)">查看订单</el-button>
                      </template>
                    </div>
                  </div>
                </div>
              </div>

              <!-- 加载更多 -->
              <div v-if="loadingMore" class="mc-loading-more">
                <el-icon class="is-loading"><Loading /></el-icon>
                加载中...
              </div>
              <div v-else-if="!hasMore && list.length > 0" class="mc-no-more">没有更多优惠券</div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 优惠券使用规则弹窗 -->
    <el-dialog v-model="showRules" title="优惠券使用规则" width="90%" class="mc-rules-dialog" destroy-on-close>
      <div class="mc-rules-content">
        <h4>适用范围</h4>
        <p>优惠券仅限在平台购买指定商品或指定店铺使用，具体适用范围请查看每张优惠券的详细说明。</p>
        <h4>使用门槛</h4>
        <p>满减券需满足最低消费金额方可使用，折扣券按折扣比例减免金额，具体门槛请查看券面说明。</p>
        <h4>不可叠加规则</h4>
        <p>单笔订单仅可使用一张优惠券，不可与其他优惠券叠加使用。</p>
        <h4>退换货规则</h4>
        <p>使用优惠券的订单发生退款时，若订单部分退款，优惠券不予退回；若整单取消/全额退款，优惠券将退回至您的账户，可在有效期内继续使用。</p>
        <h4>有效期说明</h4>
        <p>请在有效期内使用，过期不予补发，敬请谅解。</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="showRules = false">我知道了</el-button>
      </template>
    </el-dialog>

    <!-- 使用规则详情弹窗 -->
    <el-dialog v-model="showRuleDetailDialog" title="使用规则" width="90%" class="mc-rules-dialog" destroy-on-close>
      <div class="mc-rules-content">
        <h4>优惠券名称</h4>
        <p>{{ ruleDetailItem?.couponName || '' }}</p>
        <h4>优惠内容</h4>
        <p v-if="ruleDetailItem?.type === 1">满¥{{ ruleDetailItem?.minPrice }} 减 ¥{{ ruleDetailItem?.discount }}</p>
        <p v-else-if="ruleDetailItem?.type === 2">{{ ruleDetailItem?.discount }}折，最高减¥{{ ruleDetailItem?.minPrice || '不限' }}</p>
        <h4>适用范围</h4>
        <p>平台全品类通用</p>
        <h4>有效期</h4>
        <p>{{ formatDate(ruleDetailItem?.startTime) }} ~ {{ formatDate(ruleDetailItem?.endTime) }}</p>
        <h4>使用限制</h4>
        <p>每单仅限使用一张优惠券，不可与其它优惠叠加使用。</p>
      </div>
      <template #footer>
        <el-button type="primary" @click="showRuleDetailDialog = false">我知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Loading } from '@element-plus/icons-vue'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import ProfileSidebar from '@/views/profile/ProfileSidebar.vue'
import { getProfile, getProfileStats, type UserProfileDTO, type ProfileStats } from '@/api/profile'
import { getMyCoupons, type CouponRecordVO } from '@/api/coupon'

const router = useRouter()

// ==================== 个人中心 ====================
const user = ref<UserProfileDTO | null>(null)
const stats = ref<ProfileStats | null>(null)

// ==================== 常量 ====================
const tabs = [
  { key: 0, label: '未使用' },
  { key: 1, label: '已使用' },
  { key: 2, label: '已过期' },
]

// ==================== 状态 ====================
const activeTab = ref(0)
const list = ref<CouponRecordVO[]>([])
const loading = ref(false)
const loadingMore = ref(false)
const hasMore = ref(true)
const currentPage = ref(1)
const pageSize = 10
const showRules = ref(false)
const showRuleDetailDialog = ref(false)
const ruleDetailItem = ref<CouponRecordVO | null>(null)
const counts = reactive({ unused: 0, used: 0, expired: 0 })

const listWrapRef = ref<HTMLElement | null>(null)

// ==================== 计算属性 ====================
const emptyText = computed(() => {
  switch (activeTab.value) {
    case 0: return '暂无可用优惠券'
    case 1: return '暂无已使用优惠券'
    case 2: return '暂无过期优惠券'
    default: return ''
  }
})

// ==================== 方法 ====================
function switchTab(tabKey: number) {
  activeTab.value = tabKey
  currentPage.value = 1
  list.value = []
  hasMore.value = true
  fetchCoupons(true)
}

async function fetchCoupons(resetCounts = false) {
  const isFirstPage = currentPage.value === 1
  if (isFirstPage) {
    loading.value = true
  } else {
    loadingMore.value = true
  }

  try {
    const statusFilter = activeTab.value === 0 ? undefined : activeTab.value
    const res = await getMyCoupons({
      current: currentPage.value,
      size: pageSize,
      status: statusFilter,
    })
    const records = res.data.records || []
    if (currentPage.value === 1) {
      list.value = records
    } else {
      list.value = [...list.value, ...records]
    }
    hasMore.value = records.length >= pageSize
    if (resetCounts) {
      await fetchCounts()
    }
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
    loadingMore.value = false
  }
}

async function fetchCounts() {
  try {
    const [unusedRes, usedRes, expiredRes] = await Promise.all([
      getMyCoupons({ current: 1, size: 1, status: 0 }),
      getMyCoupons({ current: 1, size: 1, status: 1 }),
      getMyCoupons({ current: 1, size: 1, status: 2 }),
    ])
    counts.unused = unusedRes.data.total || 0
    counts.used = usedRes.data.total || 0
    counts.expired = expiredRes.data.total || 0
  } catch { /* ignore */ }
}

function onScroll() {
  const el = listWrapRef.value
  if (!el || !hasMore.value || loadingMore.value) return
  const scrollBottom = el.scrollHeight - el.scrollTop - el.clientHeight
  if (scrollBottom < 60) {
    currentPage.value++
    fetchCoupons()
  }
}

function formatDiscount(item: CouponRecordVO) {
  return item.discount
}

function formatDate(dateStr: string | undefined) {
  if (!dateStr) return ''
  const d = new Date(dateStr)
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${m}-${day}`
}

function getRemainingDays(item: CouponRecordVO): string | null {
  if (!item.endTime) return null
  const now = Date.now()
  const end = new Date(item.endTime).getTime()
  const diffMs = end - now
  if (diffMs <= 0) return null
  const days = Math.floor(diffMs / 86400000)
  const hours = Math.floor((diffMs % 86400000) / 3600000)
  if (days > 0) return `${days}天${hours}小时`
  if (hours > 0) return `${hours}小时`
  return '不足1小时'
}

function isExpiringSoon(item: CouponRecordVO): boolean {
  if (item.status !== 0 || !item.endTime) return false
  const end = new Date(item.endTime).getTime()
  const diffMs = end - Date.now()
  return diffMs > 0 && diffMs <= 3 * 86400000
}

function goUse(item: CouponRecordVO) {
  router.push('/home')
}

function viewOrder(item: CouponRecordVO) {
  router.push('/orders')
}

function showRuleDetail(item: CouponRecordVO) {
  ruleDetailItem.value = item
  showRuleDetailDialog.value = true
}

function goToCouponCenter() {
  router.push('/coupons')
}

// ==================== 生命周期 ====================
onMounted(async () => {
  try {
    const [profileRes, statsRes] = await Promise.all([getProfile(), getProfileStats()])
    user.value = profileRes.data
    stats.value = statsRes.data
  } catch { /* ignore */ }
  await fetchCounts()
  await fetchCoupons(false)
})
</script>

<style scoped>
.mc-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.mc-wrapper {
  width: 100%;
  padding: 16px 24px;
}
.mc-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.mc-main {
  flex: 1;
  min-width: 0;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  display: flex;
  flex-direction: column;
}

/* ===== Tab 栏 ===== */
.mc-tabs {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
  flex-shrink: 0;
}
.mc-tab {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  height: 44px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  position: relative;
  transition: color 0.2s;
}
.mc-tab.active {
  color: #e4393c;
  font-weight: 600;
}
.mc-tab.active::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 50%;
  transform: translateX(-50%);
  width: 24px;
  height: 3px;
  background: #e4393c;
  border-radius: 2px;
}
.mc-tab-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: #e4393c;
  color: #fff;
  font-size: 11px;
  font-weight: 500;
  border-radius: 9px;
  line-height: 1;
}

/* ===== 列表区域 ===== */
.mc-list-wrap {
  flex: 1;
  overflow-y: auto;
  -webkit-overflow-scrolling: touch;
  padding: 16px;
  min-height: 400px;
}

/* 空状态 */
.mc-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  gap: 12px;
}
.mc-empty-text {
  color: #bbb;
  font-size: 14px;
  margin: 0;
}

/* 加载中 */
.mc-loading {
  padding: 20px;
}

/* ===== 优惠券卡片 ===== */
.mc-coupon-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}
.mc-coupon-card {
  display: flex;
  position: relative;
  height: 92px;
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0,0,0,0.04);
  border: 1px solid #f0f0f0;
  transition: box-shadow 0.2s;
  min-width: 0;
}
.mc-coupon-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
}
.mc-coupon-card.is-used {
  opacity: 0.75;
}
.mc-coupon-card.is-expired {
  opacity: 0.5;
}

/* 左侧色块 ———— 仿淘宝券样式 */
.mc-card-left {
  width: 130px;
  min-width: 130px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: #fff;
  position: relative;
  flex-shrink: 0;
}
.mc-card-left.type-cash {
  background: linear-gradient(135deg, #ff6a33, #ff5000);
}
.mc-card-left.type-discount {
  background: linear-gradient(135deg, #4a9eff, #3178f6);
}
.mc-coupon-card.is-used .mc-card-left.type-cash,
.mc-coupon-card.is-expired .mc-card-left.type-cash {
  background: #c0c0c0;
}
.mc-coupon-card.is-used .mc-card-left.type-discount,
.mc-coupon-card.is-expired .mc-card-left.type-discount {
  background: #b0b8c0;
}

.mc-discount {
  display: flex;
  align-items: baseline;
  line-height: 1;
}
.mc-d-symbol {
  font-size: 14px;
  font-weight: 500;
}
.mc-d-num {
  font-size: 30px;
  font-weight: 700;
  letter-spacing: -1px;
}
.mc-d-unit {
  font-size: 14px;
  font-weight: 500;
}
.mc-d-cond {
  font-size: 11px;
  opacity: 0.85;
  margin-top: 4px;
  white-space: nowrap;
}

/* 圆孔装饰 - 模拟真实券的虚线孔 */
.mc-hole {
  position: absolute;
  width: 14px;
  height: 14px;
  border-radius: 50%;
  background: #f5f5f5;
  right: -7px;
  z-index: 1;
}
.mc-hole-top {
  top: -7px;
}
.mc-hole-bottom {
  bottom: -7px;
}

/* 右侧信息 */
.mc-card-right {
  flex: 1;
  padding: 0 16px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  position: relative;
}
.mc-card-right::before {
  content: '';
  position: absolute;
  left: 0;
  top: 8px;
  bottom: 8px;
  width: 1px;
  border-left: 1px dashed #e0e0e0;
}

.mc-card-top {
  padding-top: 12px;
}
.mc-name-row {
  display: flex;
  align-items: center;
  gap: 8px;
}
.mc-name {
  font-size: 14px;
  font-weight: 600;
  color: #222;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 200px;
}
.mc-tag {
  display: inline-flex;
  align-items: center;
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 10px;
  font-weight: 500;
  line-height: 1.6;
  color: #ff5000;
  background: #fff2f5;
}
.mc-scope {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}

.mc-card-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 10px;
}
.mc-time {
  font-size: 11px;
  color: #bbb;
}
.mc-card-actions {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-shrink: 0;
}
.mc-card-actions .el-button--small {
  height: 28px;
  padding: 0 14px;
  font-size: 12px;
}
.mc-card-actions .el-button--small.is-round {
  padding: 0 14px;
}
.mc-card-actions .el-button--primary {
  --el-button-bg-color: #ff5000;
  --el-button-border-color: #ff5000;
  --el-button-hover-bg-color: #ff6a33;
  --el-button-hover-border-color: #ff6a33;
  --el-button-active-bg-color: #e64700;
  --el-button-active-border-color: #e64700;
}

.mc-used-label {
  font-size: 12px;
  color: #999;
}

/* 加载更多 */
.mc-loading-more,
.mc-no-more {
  text-align: center;
  padding: 16px;
  color: #999;
  font-size: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

/* 弹窗 */
.mc-rules-dialog :deep(.el-dialog__body) {
  max-height: 60vh;
  overflow-y: auto;
}
.mc-rules-content h4 {
  margin: 16px 0 6px;
  color: #333;
  font-size: 14px;
}
.mc-rules-content h4:first-child {
  margin-top: 0;
}
.mc-rules-content p {
  color: #666;
  font-size: 13px;
  line-height: 1.6;
  margin: 0;
}
</style>