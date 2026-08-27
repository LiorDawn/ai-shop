<template>
  <div class="cart-page">
    <!-- 顶部导航 -->
    <header class="cart-header">
      <div class="container header-inner">
        <router-link to="/home" class="cart-logo">
          <img src="../../assets/AiShop.jpg" alt="AiShop" class="cart-logo-img" />
          <span class="logo-text">AI 智能商城</span>
        </router-link>
        <div class="cart-title">我的购物车</div>
        <div class="header-right">
          <span class="user-name">{{ userName }}</span>
          <span class="header-link" @click="logout">退出登录</span>
        </div>
      </div>
    </header>

    <!-- 主体 -->
    <main class="cart-main">
      <div class="container" v-loading="loading">
        <!-- 空购物车 -->
        <div v-if="!loading && cartItems.length === 0" class="cart-empty">
          <el-empty description="购物车还是空的，快去逛逛吧">
            <router-link to="/home">
              <el-button type="primary" size="large">去逛逛</el-button>
            </router-link>
          </el-empty>
        </div>

        <template v-else>
          <!-- 按店铺分组渲染 -->
          <div
            v-for="group in shopGroups"
            :key="group.shopId"
            class="shop-group"
          >
            <!-- 店铺头 -->
            <div class="shop-group-head">
              <label class="shop-check-all">
                <el-checkbox
                  :model-value="isShopAllChecked(group.shopId)"
                  :indeterminate="isShopIndeterminate(group.shopId)"
                  :disabled="getShopValidItems(group).length === 0"
                  @change="(val: boolean) => toggleShopAll(group.shopId, val)"
                />
                <span class="shop-name">{{ group.shopName }}</span>
              </label>
            </div>

            <!-- 商品列表 -->
            <div
              v-for="item in group.items"
              :key="item.id"
              class="cart-item"
              :class="{ invalid: item.productStatus !== 1 }"
            >
              <!-- 勾选框 -->
              <div class="col-check">
                <el-checkbox
                  :model-value="item.checked === 1"
                  :disabled="item.productStatus !== 1"
                  @change="(val: boolean) => onToggleItem(item, val)"
                />
              </div>

              <!-- 商品图 -->
              <div class="col-img">
                <el-image
                  :src="item.productImage || defaultImg"
                  fit="cover"
                  class="product-img"
                />
                <div v-if="item.productStatus !== 1" class="invalid-mask">
                  <span>商品已失效</span>
                </div>
              </div>

              <!-- 商品名 -->
              <div class="col-name">
                <div class="product-name" :class="{ 'name-invalid': item.productStatus !== 1 }">
                  {{ item.productName }}
                </div>
                <div v-if="item.productStatus !== 1" class="invalid-tip">商品已失效</div>
              </div>

              <!-- 单价 -->
              <div class="col-price">¥{{ Number(item.price).toFixed(2) }}</div>

              <!-- 数量器 -->
              <div class="col-num">
                <div class="num-stepper" :class="{ disabled: item.productStatus !== 1 }">
                  <span class="num-btn" @click="item.productStatus === 1 && onMinus(item)">-</span>
                  <input
                    class="num-input"
                    type="text"
                    :value="item.num"
                    :disabled="item.productStatus !== 1"
                    @blur="(e: any) => onInputNum(item, e.target.value)"
                  />
                  <span class="num-btn" @click="item.productStatus === 1 && onPlus(item)">+</span>
                </div>
              </div>

              <!-- 小计 -->
              <div class="col-subtotal">
                <template v-if="item.productStatus === 1">
                  ¥{{ subtotal(item).toFixed(2) }}
                </template>
                <template v-else>
                  <span class="invalid-price">--</span>
                </template>
              </div>

              <!-- 操作 -->
              <div class="col-action">
                <el-button link type="danger" class="btn-del" @click="onDeleteSingle(item)">删除</el-button>
              </div>
            </div>
          </div>
        </template>
      </div>
    </main>

    <!-- 底部固定栏 -->
    <footer v-if="cartItems.length > 0" class="cart-footer">
      <div class="container footer-inner">
        <div class="footer-left">
          <el-checkbox
            :model-value="isAllChecked"
            :indeterminate="isIndeterminate"
            @change="(val: boolean) => onCheckAll(val)"
          >全选</el-checkbox>
          <span class="footer-link" @click="onDeleteBatch">删除选中</span>
        </div>
        <div class="footer-right">
          <span class="footer-info">
            已选 <em class="highlight">{{ selectedNum }}</em> 件商品
          </span>
          <span class="footer-total">
            合计：<em class="total-price">¥{{ totalPrice.toFixed(2) }}</em>
          </span>
          <el-button
            type="primary"
            size="large"
            class="btn-settle"
            :disabled="selectedNum === 0"
            @click="onSettle"
          >去结算</el-button>
        </div>
      </div>
    </footer>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { CartItemVO, ShopCartGroup } from '../../api/cart'
import {
  getCartList,
  updateCartNum,
  toggleCartCheck,
  checkAllCart,
  deleteCartItem,
  deleteCartBatch,
  settleCheck,
} from '../../api/cart'

import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const auth = useAuthStore()
const loading = ref(false)
const cartItems = ref<CartItemVO[]>([])
const defaultImg = 'https://picsum.photos/seed/default/200/200'

const userName = computed(() => auth.user?.username || '')

function logout() {
  auth.logout()
}

// ---- 数据加载 ----
async function fetchCart() {
  loading.value = true
  try {
    const res = await getCartList()
    cartItems.value = (res.data || []).map((item: CartItemVO) => ({
      ...item,
      subtotal: item.price != null ? Number(item.price) * item.num : 0,
    }))
  } catch {
    cartItems.value = []
  } finally {
    loading.value = false
  }
}

// ---- 按店铺分组（computed） ----
const shopGroups = computed<ShopCartGroup[]>(() => {
  const map = new Map<number, CartItemVO[]>()
  cartItems.value.forEach((item) => {
    const key = item.shopId || 0
    if (!map.has(key)) map.set(key, [])
    map.get(key)!.push(item)
  })
  const groups: ShopCartGroup[] = []
  map.forEach((items, shopId) => {
    groups.push({
      shopId,
      shopName: items[0].shopName || '未知店铺',
      items,
    })
  })
  return groups
})

// ---- 选中状态 ----
const selectedItems = computed(() =>
  cartItems.value.filter((item) => item.checked === 1 && item.productStatus === 1),
)

const selectedNum = computed(() => selectedItems.value.reduce((sum, item) => sum + item.num, 0))

const totalPrice = computed(() =>
  selectedItems.value.reduce((sum, item) => sum + subtotal(item), 0),
)

const validItems = computed(() => cartItems.value.filter((item) => item.productStatus === 1))

const isAllChecked = computed(() =>
  validItems.value.length > 0 && validItems.value.every((item) => item.checked === 1),
)

const isIndeterminate = computed(() => {
  const checkedCount = validItems.value.filter((item) => item.checked === 1).length
  return checkedCount > 0 && checkedCount < validItems.value.length
})

function subtotal(item: CartItemVO): number {
  return Number(item.price) * item.num
}

function getShopValidItems(group: ShopCartGroup): CartItemVO[] {
  return group.items.filter((item) => item.productStatus === 1)
}

function isShopAllChecked(shopId: number): boolean {
  const group = shopGroups.value.find((g) => g.shopId === shopId)
  if (!group) return false
  const valid = getShopValidItems(group)
  return valid.length > 0 && valid.every((item) => item.checked === 1)
}

function isShopIndeterminate(shopId: number): boolean {
  const group = shopGroups.value.find((g) => g.shopId === shopId)
  if (!group) return false
  const valid = getShopValidItems(group)
  const checked = valid.filter((item) => item.checked === 1).length
  return checked > 0 && checked < valid.length
}

// ---- 勾选操作 ----
async function onToggleItem(item: CartItemVO, val: boolean) {
  const checked = val ? 1 : 0
  try {
    await toggleCartCheck(item.id, checked)
    item.checked = checked
  } catch {
    ElMessage.error('操作失败')
  }
}

async function onCheckAll(val: boolean) {
  const checked = val ? 1 : 0
  try {
    await checkAllCart(checked)
    cartItems.value.forEach((item) => {
      if (item.productStatus === 1) item.checked = checked
    })
  } catch {
    ElMessage.error('操作失败')
  }
}

async function toggleShopAll(shopId: number, val: boolean) {
  const group = shopGroups.value.find((g) => g.shopId === shopId)
  if (!group) return
  const checked = val ? 1 : 0
  const validItems = getShopValidItems(group)
  for (const item of validItems) {
    try {
      await toggleCartCheck(item.id, checked)
      item.checked = checked
    } catch {
      // continue
    }
  }
}

// ---- 数量操作 ----
async function onMinus(item: CartItemVO) {
  if (item.num <= 1) return
  const newNum = item.num - 1
  try {
    await updateCartNum(item.id, newNum)
    item.num = newNum
  } catch {
    ElMessage.error('修改失败')
  }
}

async function onPlus(item: CartItemVO) {
  const newNum = item.num + 1
  try {
    await updateCartNum(item.id, newNum)
    item.num = newNum
  } catch {
    ElMessage.error('修改失败')
  }
}

async function onInputNum(item: CartItemVO, val: string) {
  let n = parseInt(val, 10)
  if (isNaN(n) || n < 1) n = 1
  if (n === item.num) return
  try {
    await updateCartNum(item.id, n)
    item.num = n
  } catch {
    ElMessage.error('修改失败')
  }
}

// ---- 删除操作 ----
async function onDeleteSingle(item: CartItemVO) {
  try {
    await ElMessageBox.confirm('确定要从购物车中删除该商品吗？', '确认删除', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await deleteCartItem(item.id)
    cartItems.value = cartItems.value.filter((c) => c.id !== item.id)
    ElMessage.success('已删除')
  } catch {
    ElMessage.error('删除失败')
  }
}

async function onDeleteBatch() {
  const ids = selectedItems.value.map((item) => item.id)
  if (ids.length === 0) {
    ElMessage.warning('请先勾选要删除的商品')
    return
  }
  try {
    await ElMessageBox.confirm(`确定要删除选中的 ${ids.length} 件商品吗？`, '批量删除', {
      type: 'warning',
    })
  } catch {
    return
  }
  try {
    await deleteCartBatch(ids)
    cartItems.value = cartItems.value.filter((c) => !ids.includes(c.id))
    ElMessage.success('删除成功')
  } catch {
    ElMessage.error('删除失败')
  }
}

// ---- 结算 ----
async function onSettle() {
  if (selectedNum.value === 0) {
    ElMessage.warning('请至少勾选一件有效商品')
    return
  }
  try {
    const res = await settleCheck()
    const settleVO = res.data
    if (!settleVO) return
    // 跨店铺提示
    if (settleVO.crossShop) {
      await ElMessageBox.confirm(
        '您选择的商品来自不同店铺，将分开下单。确定继续吗？',
        '跨店铺结算',
        { type: 'info' },
      )
    }
    // 跳转订单确认页（携带选中的购物车项）
    const settleItems = selectedItems.value.map(item => ({
      productId: item.productId,
      skuId: item.skuId,
      productName: item.productName,
      productImage: item.productImage,
      price: item.price,
      num: item.num,
      spec: item.spec || '',
      shopId: item.shopId,
      shopName: item.shopName,
    }))
    localStorage.setItem('settle_items', JSON.stringify(settleItems))
    router.push(`/order-confirm`)
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '结算校验失败')
  }
}

onMounted(() => {
  fetchCart()
})
</script>

<style scoped>
/* 页面容器 */
.cart-page {
  min-height: 100vh;
  background: #f5f5f5;
  display: flex;
  flex-direction: column;
  padding-bottom: 80px;
}

/* 顶部 */
.cart-header {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
}
.header-inner {
  display: flex;
  align-items: center;
  height: 56px;
  gap: 24px;
}
.cart-logo {
  display: flex;
  align-items: center;
  gap: 8px;
  text-decoration: none;
  color: var(--text-1);
  flex-shrink: 0;
}
.cart-logo-img {
  width: 48px;
  height: 48px;
  border-radius: 6px;
  object-fit: contain;
}
.logo-text { font-size: 16px; font-weight: 700; }
.cart-title {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-1);
}
.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: var(--text-3);
}
.user-name { color: var(--text-2); }
.header-link { color: var(--primary); cursor: pointer; }

/* 主体 */
.cart-main {
  flex: 1;
  padding: 20px 0;
}

/* 空购物车 */
.cart-empty {
  background: #fff;
  border-radius: 8px;
  padding: 80px 0;
}

/* 店铺分组 */
.shop-group {
  background: #fff;
  border-radius: 8px;
  margin-bottom: 16px;
}
.shop-group-head {
  padding: 12px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
}
.shop-check-all {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}
.shop-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--text-1);
}

/* 购物车行 */
.cart-item {
  display: flex;
  align-items: center;
  padding: 16px 20px;
  gap: 16px;
  border-bottom: 1px solid #f5f5f5;
}
.cart-item.invalid {
  background: #fafafa;
  opacity: 0.7;
}
.cart-item:last-child { border-bottom: none; }

.col-check { flex-shrink: 0; }

.col-img {
  width: 80px;
  height: 80px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
  background: #f5f5f5;
}
.product-img {
  width: 100%;
  height: 100%;
}
.invalid-mask {
  position: absolute;
  inset: 0;
  background: rgba(0,0,0,0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 12px;
}

.col-name {
  flex: 1;
  min-width: 0;
}
.product-name {
  font-size: 13px;
  color: var(--text-1);
  line-height: 1.5;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}
.name-invalid { color: var(--text-4); }
.invalid-tip {
  font-size: 11px;
  color: var(--text-4);
  margin-top: 4px;
}

.col-price {
  width: 100px;
  text-align: center;
  font-size: 13px;
  color: var(--text-2);
  flex-shrink: 0;
}

.col-num {
  flex-shrink: 0;
}
.num-stepper {
  display: flex;
  align-items: center;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  overflow: hidden;
}
.num-stepper.disabled { opacity: 0.5; }
.num-btn {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  user-select: none;
  font-size: 14px;
  color: var(--text-2);
  background: #fafafa;
  transition: background 0.15s;
}
.num-btn:hover { background: #e8e8e8; }
.num-input {
  width: 44px;
  height: 28px;
  border: none;
  border-left: 1px solid #d9d9d9;
  border-right: 1px solid #d9d9d9;
  text-align: center;
  font-size: 13px;
  outline: none;
  background: #fff;
}

.col-subtotal {
  width: 100px;
  text-align: center;
  font-size: 14px;
  font-weight: 700;
  color: var(--primary);
  flex-shrink: 0;
}
.invalid-price { color: var(--text-4); font-weight: 400; }

.col-action {
  width: 60px;
  text-align: center;
  flex-shrink: 0;
}
.btn-del { font-size: 12px; }

/* 底部固定栏 */
.cart-footer {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: #fff;
  border-top: 2px solid var(--primary);
  z-index: 100;
  box-shadow: 0 -2px 8px rgba(0,0,0,0.06);
}
.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 56px;
}
.footer-left {
  display: flex;
  align-items: center;
  gap: 24px;
}
.footer-link {
  font-size: 13px;
  color: var(--text-3);
  cursor: pointer;
}
.footer-link:hover { color: var(--primary); }
.footer-right {
  display: flex;
  align-items: center;
  gap: 20px;
}
.footer-info {
  font-size: 13px;
  color: var(--text-2);
}
.highlight {
  font-style: normal;
  color: var(--primary);
  font-weight: 700;
}
.footer-total {
  font-size: 14px;
  color: var(--text-1);
}
.total-price {
  font-style: normal;
  font-size: 22px;
  font-weight: 900;
  color: var(--primary);
}
.btn-settle {
  padding: 12px 32px;
  font-size: 16px;
  font-weight: 700;
  border-radius: 999px;
}</style>