<template>
  <div class="oc-page">
    <HeaderUser />

    <div class="oc-container">
      <div class="oc-title">确认订单</div>

      <div v-loading="loading" class="oc-body">
        <!-- 收货地址 -->
        <div class="oc-section">
          <div class="oc-section-title">收货地址</div>
          <div v-if="addresses.length === 0" class="oc-no-addr">
            暂无地址，请先 <router-link to="/profile/address" style="color: var(--primary)">添加地址</router-link>
          </div>
          <div v-else class="oc-addr-list">
            <div
              v-for="addr in addresses"
              :key="addr.id"
              class="oc-addr-card"
              :class="{ selected: selectedAddrId === addr.id }"
              @click="selectedAddrId = addr.id!"
            >
              <div class="oc-addr-name">
                {{ addr.receiver }}
                <span class="oc-addr-phone">{{ addr.phone }}</span>
                <el-tag v-if="addr.isDefault === 1" size="small" type="danger">默认</el-tag>
              </div>
              <div class="oc-addr-detail">{{ addr.address }}</div>
            </div>
          </div>
        </div>

        <!-- 商品清单 -->
        <div class="oc-section">
          <div class="oc-section-title">商品清单</div>
          <div v-for="(item, idx) in items" :key="idx" class="oc-item">
            <img :src="item.productImage" class="oc-item-img" />
            <div class="oc-item-info">
              <div class="oc-item-name">{{ item.productName }}</div>
              <div class="oc-item-spec" v-if="item.spec">{{ item.spec }}</div>
            </div>
            <div class="oc-item-price">¥{{ Number(item.price).toFixed(2) }}</div>
            <div class="oc-item-num">x{{ item.num }}</div>
            <div class="oc-item-subtotal">¥{{ Number(item.price * item.num).toFixed(2) }}</div>
          </div>
        </div>

        <!-- 优惠券 -->
        <div class="oc-section" v-if="coupons.length > 0">
          <div class="oc-section-title">优惠券</div>
          <div class="oc-coupon-list">
            <div
              v-for="c in coupons"
              :key="c.id"
              class="oc-coupon-card"
              :class="{ selected: selectedCouponId === c.id }"
              @click="selectCoupon(c)"
            >
              <div class="coupon-left">
                <span class="coupon-amount">¥{{ c.discount }}</span>
                <span class="coupon-limit">满{{ c.minPrice }}可用</span>
              </div>
              <div class="coupon-right">
                <div class="coupon-name">{{ c.name }}</div>
                <div class="coupon-date">{{ c.endTime }} 到期</div>
              </div>
              <div v-if="selectedCouponId === c.id" class="coupon-checked">✓</div>
            </div>
          </div>
          <el-button v-if="selectedCouponId" text type="primary" @click="clearCoupon" size="small">不使用优惠券</el-button>
        </div>

        <!-- 订单备注 -->
        <div class="oc-section">
          <div class="oc-section-title">订单备注</div>
          <el-input
            v-model="remark"
            placeholder="选填，给商家的备注"
            maxlength="200"
            show-word-limit
          />
        </div>
      </div>

      <!-- 底部结算栏 -->
      <div class="oc-footer-bar">
        <div class="oc-footer-info">
          <span>共 <em>{{ totalNum }}</em> 件商品，合计</span>
          <template v-if="discountAmount > 0">
            <span class="oc-original-price">¥{{ totalPrice.toFixed(2) }}</span>
            <span class="oc-discount">-¥{{ discountAmount.toFixed(2) }}</span>
          </template>
          <span class="oc-total-price">¥{{ finalPrice.toFixed(2) }}</span>
        </div>
        <el-button type="primary" size="large" :loading="submitting" @click="onSubmit">
          提交订单
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import HeaderUser from '../../components/layout/HeaderUser.vue'
import { listAddresses } from '../../api/profile'
import { createOrder } from '../../api/order'
import { getMyAvailableCoupons } from '../../api/coupon'
import type { CartItemVO } from '../../api/cart'

interface CouponDTO {
  id: number
  name: string
  type: number
  minPrice: number
  discount: number
  stock: number
  remain: number
  startTime: string
  endTime: string
  status: number
}

const router = useRouter()
const route = useRoute()
const loading = ref(false)
const submitting = ref(false)

// 地址
const addresses = ref<any[]>([])
const selectedAddrId = ref<number | null>(null)

// 商品
const items = ref<CartItemVO[]>([])
const totalNum = computed(() => items.value.reduce((s, i) => s + i.num, 0))
const totalPrice = computed(() => items.value.reduce((s, i) => s + i.price * i.num, 0))

// 优惠券
const coupons = ref<CouponDTO[]>([])
const selectedCouponId = ref<number | null>(null)
const discountAmount = ref(0)
const couponLoading = ref(false)
const finalPrice = computed(() => Math.max(0, totalPrice.value - discountAmount.value))

// 备注
const remark = ref('')

function selectCoupon(c: CouponDTO) {
  if (selectedCouponId.value === c.id) {
    clearCoupon()
    return
  }
  selectedCouponId.value = c.id
  discountAmount.value = c.discount
}

function clearCoupon() {
  selectedCouponId.value = null
  discountAmount.value = 0
}

async function loadAddresses() {
  try {
    const res: any = await listAddresses()
    addresses.value = res.data || []
    const def = addresses.value.find((a: any) => a.isDefault === 1)
    if (def) selectedAddrId.value = def.id
    else if (addresses.value.length > 0) selectedAddrId.value = addresses.value[0].id
  } catch {
    addresses.value = []
  }
}

async function loadCoupons() {
  if (totalPrice.value <= 0) return
  couponLoading.value = true
  try {
    const res: any = await getMyAvailableCoupons()
    coupons.value = (res.data || []).filter((c: CouponDTO) => c.minPrice <= totalPrice.value)
  } catch {
    coupons.value = []
  } finally {
    couponLoading.value = false
  }
}

function loadItems() {
  const raw = route.query.items || localStorage.getItem('settle_items')
  if (raw) {
    try {
      items.value = JSON.parse(raw as string)
    } catch {
      items.value = []
    }
  }
}

async function onSubmit() {
  if (!selectedAddrId.value) {
    ElMessage.warning('请选择收货地址')
    return
  }
  if (items.value.length === 0) {
    ElMessage.warning('没有需要结算的商品')
    return
  }
  submitting.value = true
  try {
    const productIds = items.value.map(i => i.productId).join(',')
    const res: any = await createOrder(selectedAddrId.value, productIds, remark.value, selectedCouponId.value || undefined)
    const orderId = res.data
    ElMessage.success('下单成功')
    localStorage.removeItem('settle_items')
    router.push(`/payment/${orderId}`)
  } catch (e: any) {
    ElMessage.error(e.message || '下单失败')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  loading.value = true
  loadItems()
  await loadAddresses()
  if (items.value.length > 0) {
    await loadCoupons()
  }
  loading.value = false
})
</script>

<style scoped>
.oc-page { min-height: 100vh; background: #f5f5f5; }
.oc-container { max-width: 1000px; margin: 0 auto; padding: 20px; }
.oc-title { font-size: 22px; font-weight: 700; margin-bottom: 20px; }

.oc-body { display: flex; flex-direction: column; gap: 16px; }
.oc-section { background: #fff; border-radius: 8px; padding: 20px; }
.oc-section-title { font-size: 16px; font-weight: 600; margin-bottom: 12px; padding-bottom: 12px; border-bottom: 1px solid #f0f0f0; }

.oc-no-addr { color: #999; font-size: 14px; }
.oc-addr-list { display: flex; flex-wrap: wrap; gap: 12px; }
.oc-addr-card {
  width: 280px; padding: 14px; border: 2px solid #e8e8e8; border-radius: 8px;
  cursor: pointer; transition: all 0.2s;
}
.oc-addr-card:hover { border-color: #f56c6c; }
.oc-addr-card.selected { border-color: #e4393c; background: #fff5f5; }
.oc-addr-name { font-weight: 600; margin-bottom: 6px; display: flex; align-items: center; gap: 8px; }
.oc-addr-phone { font-weight: 400; color: #666; }
.oc-addr-detail { font-size: 13px; color: #666; }

.oc-item { display: flex; align-items: center; gap: 16px; padding: 12px 0; border-bottom: 1px solid #f5f5f5; }
.oc-item:last-child { border-bottom: none; }
.oc-item-img { width: 80px; height: 80px; object-fit: cover; border-radius: 6px; }
.oc-item-info { flex: 1; min-width: 0; }
.oc-item-name { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.oc-item-spec { font-size: 12px; color: #999; margin-top: 4px; }
.oc-item-price { width: 80px; text-align: center; color: #666; }
.oc-item-num { width: 60px; text-align: center; color: #666; }
.oc-item-subtotal { width: 100px; text-align: right; font-weight: 600; color: #e4393c; }

.oc-coupon-list { display: flex; flex-direction: column; gap: 10px; }
.oc-coupon-card {
  display: flex; align-items: center; gap: 16px;
  padding: 14px 16px; border: 2px solid #e8e8e8; border-radius: 8px;
  cursor: pointer; transition: all 0.2s; position: relative;
  background: linear-gradient(135deg, #fff8f0 0%, #fff 100%);
}
.oc-coupon-card:hover { border-color: #f5a623; }
.oc-coupon-card.selected { border-color: #f5a623; background: #fff8f0; }
.coupon-left { text-align: center; min-width: 80px; }
.coupon-amount { font-size: 22px; font-weight: 700; color: #e4393c; display: block; }
.coupon-limit { font-size: 12px; color: #999; }
.coupon-right { flex: 1; }
.coupon-name { font-size: 14px; font-weight: 500; }
.coupon-date { font-size: 12px; color: #999; margin-top: 4px; }
.coupon-checked {
  position: absolute; top: 8px; right: 8px;
  width: 20px; height: 20px; background: #f5a623; color: #fff;
  border-radius: 50%; display: flex; align-items: center; justify-content: center;
  font-size: 12px; font-weight: 700;
}

.oc-footer-bar {
  position: sticky; bottom: 0; margin-top: 20px;
  display: flex; align-items: center; justify-content: flex-end; gap: 20px;
  padding: 16px 24px; background: #fff; border-radius: 8px;
  box-shadow: 0 -2px 8px rgba(0,0,0,0.06);
}
.oc-footer-info { font-size: 14px; color: #333; display: flex; align-items: baseline; gap: 8px; }
.oc-footer-info em { font-style: normal; color: #e4393c; font-weight: 600; }
.oc-original-price { text-decoration: line-through; color: #999; font-size: 13px; }
.oc-discount { color: #e4393c; font-size: 13px; }
.oc-total-price { font-size: 22px; font-weight: 700; color: #e4393c; }
</style>