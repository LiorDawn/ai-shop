<template>
  <div class="coupon-page">
    <div class="page-header">
      <div class="container header-inner">
        <h2>领券中心</h2>
        <router-link to="/profile/coupons" class="my-link">我的优惠券 →</router-link>
      </div>
    </div>

    <div class="container coupon-body">
      <el-tabs v-model="activeTab" class="coupon-tabs">
        <el-tab-pane label="可领取" name="available">
          <div v-if="couponList.length === 0" class="empty-state">
            <p>暂无可用优惠券</p>
          </div>
          <div v-else class="coupon-list">
            <div v-for="coupon in couponList" :key="coupon.id" class="coupon-card">
              <div class="coupon-left">
                <div class="coupon-amount" v-if="coupon.type === 1">
                  <span class="amount-symbol">¥</span>
                  <span class="amount-value">{{ coupon.discount }}</span>
                </div>
                <div class="coupon-amount" v-else-if="coupon.type === 2">
                  <span class="amount-value">{{ coupon.discount }}</span>
                  <span class="amount-symbol">折</span>
                </div>
                <div class="coupon-amount" v-else>
                  <span class="amount-value">{{ coupon.discount }}</span>
                </div>
                <div class="coupon-min">满{{ coupon.minPrice }}元可用</div>
              </div>
              <div class="coupon-center">
                <div class="coupon-name">{{ coupon.name }}</div>
                <div class="coupon-time">
                  有效期：{{ formatDate(coupon.startTime) }} ~ {{ formatDate(coupon.endTime) }}
                </div>
                <div class="coupon-stock" v-if="coupon.remain !== undefined">
                  剩余 <b>{{ coupon.remain }}</b> / {{ coupon.stock }}
                </div>
              </div>
              <div class="coupon-right">
                <el-button
                  type="primary"
                  size="small"
                  :disabled="coupon.remain <= 0 || coupon.claimed"
                  :loading="receivingIds.has(coupon.id)"
                  @click="handleReceive(coupon.id)"
                >
                  {{ coupon.claimed ? '已领取' : coupon.remain <= 0 ? '已领完' : '立即领取' }}
                </el-button>
              </div>
            </div>
          </div>

          <div v-if="totalPages > 1" class="pagination-wrap">
            <el-pagination
              v-model:current-page="currentPage"
              :page-size="pageSize"
              :total="total"
              layout="prev, pager, next"
              @current-change="fetchCoupons"
            />
          </div>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAvailableCoupons, receiveCoupon, type CouponDTO } from '../../api/coupon'

const activeTab = ref('available')
const couponList = ref<CouponDTO[]>([])
const receivingIds = ref(new Set<number>())
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

onMounted(() => {
  fetchCoupons()
})

async function fetchCoupons() {
  try {
    const res: any = await getAvailableCoupons({ current: currentPage.value, size: pageSize.value })
    const data = res.data
    couponList.value = data?.records || []
    total.value = data?.total || 0
  } catch {
    couponList.value = []
  }
}

async function handleReceive(couponId: number) {
  receivingIds.value.add(couponId)
  try {
    await receiveCoupon(couponId)
    ElMessage.success('领取成功！')
    await fetchCoupons()
  } catch (e: any) {
    ElMessage.error(e.message || '领取失败')
  } finally {
    receivingIds.value.delete(couponId)
  }
}

function formatDate(dateStr: string): string {
  if (!dateStr) return ''
  return dateStr.slice(0, 10)
}

function statusType(status: number): string {
  switch (status) {
    case 0: return 'info'
    case 1: return 'success'
    case 2: return 'danger'
    default: return 'info'
  }
}
</script>

<style scoped>
.container { width: 90%; max-width: 1900px; padding: 0 16px; box-sizing: border-box; }
.coupon-page { min-height: 100vh; background: #f5f5f5; padding-bottom: 60px; }
.page-header { background: #fff; border-bottom: 1px solid #e8e8e8; }
.header-inner { display: flex; align-items: center; justify-content: space-between; height: 56px; }
.header-inner h2 { margin: 0; font-size: 18px; }
.my-link { color: var(--primary); font-size: 14px; text-decoration: none; }
.coupon-body { margin-top: 20px; }
.coupon-tabs { background: #fff; border-radius: 8px; padding: 16px 20px; }
.coupon-list { display: flex; flex-direction: column; gap: 12px; }
.empty-state { text-align: center; padding: 60px 0; color: #999; }
.coupon-card {
  display: flex;
  align-items: center;
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
  background: linear-gradient(135deg, #fafafa 0%, #fff 100%);
  transition: all 0.2s;
}
.coupon-card:hover { border-color: #ff0f23; box-shadow: 0 2px 8px rgba(255,15,35,0.08); }
.coupon-left {
  width: 140px;
  text-align: center;
  padding: 20px 12px;
  background: linear-gradient(135deg, #fff0f0, #ffe8e8);
  flex-shrink: 0;
}
.coupon-amount { margin-bottom: 4px; }
.amount-symbol { font-size: 16px; color: #ff0f23; vertical-align: super; }
.amount-value { font-size: 32px; font-weight: 800; color: #ff0f23; line-height: 1; }
.coupon-min { font-size: 12px; color: #999; }
.coupon-center { flex: 1; padding: 16px 20px; min-width: 0; }
.coupon-name { font-size: 15px; font-weight: 600; color: #333; margin-bottom: 6px; }
.coupon-time { font-size: 12px; color: #999; margin-bottom: 4px; }
.coupon-stock { font-size: 12px; color: #999; }
.coupon-right { padding: 16px 20px; flex-shrink: 0; }
.pagination-wrap { margin-top: 20px; display: flex; justify-content: center; }
</style>