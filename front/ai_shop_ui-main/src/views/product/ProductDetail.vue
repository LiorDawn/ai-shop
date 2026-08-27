<template>
  <div class="pd-page">
    <HeaderUser />

    <!-- 骨架屏：加载中 -->
    <div v-if="loading" class="pd-container">
      <div class="pd-skeleton">
        <div class="pd-skeleton-main">
          <div class="pd-skeleton-gallery" />
          <div class="pd-skeleton-info">
            <div class="pd-skeleton-line w-80" />
            <div class="pd-skeleton-line w-60" />
            <div class="pd-skeleton-line w-40" />
            <div class="pd-skeleton-line w-90" />
            <div class="pd-skeleton-line w-50" />
            <div class="pd-skeleton-block" />
          </div>
        </div>
      </div>
    </div>

    <!-- 内容 -->
    <div v-else class="pd-container">
      <!-- 面包屑 -->
      <el-breadcrumb class="pd-breadcrumb" separator="/">
        <el-breadcrumb-item :to="{ path: '/home' }">首页</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/hotsale' }">商城热卖</el-breadcrumb-item>
        <el-breadcrumb-item>{{ product?.categoryName }}</el-breadcrumb-item>
        <el-breadcrumb-item>{{ product?.name }}</el-breadcrumb-item>
      </el-breadcrumb>

      <!-- ========== 商品主区域 ========== -->
      <div class="pd-main">
        <!-- 左侧：图片画廊 -->
        <div class="pd-gallery">
          <div class="pd-main-img" @click="previewVisible = true">
            <img
              :src="currentImage"
              :alt="product?.name"
              @error="onImgError($event, 'main')"
            />
            <div class="pd-main-img-hover"><el-icon style="font-size:28px"><ZoomIn /></el-icon>点击放大</div>
          </div>
          <div class="pd-thumb-list" v-if="imageList.length > 1">
            <div
              v-for="(img, idx) in imageList"
              :key="idx"
              class="pd-thumb"
              :class="{ active: currentImage === img }"
              @click="currentImage = img"
            >
              <img :src="img" @error="onImgError($event)" />
            </div>
          </div>
        </div>

        <!-- 中间：商品信息 -->
        <div class="pd-info">
          <h1 class="pd-name">{{ product?.name }}</h1>
          <div class="pd-price-box">
            <span class="pd-price-label">价格</span>
            <span class="pd-price-current">¥{{ displayPrice(selectedSku?.price ?? product?.price) }}</span>
            <span
              class="pd-price-original"
              v-if="selectedSku?.price && selectedSku.price < (product?.price || 0)"
            >¥{{ displayPrice(product?.price) }}</span>
          </div>
          <div class="pd-meta">
            <span><em>销量</em> {{ product?.sales || 0 }}</span>
            <span><em>库存</em> {{ selectedSku ? selectedSku.stock : product?.stock || 0 }}</span>
            <span><em>分类</em> {{ product?.categoryName }}</span>
          </div>

          <!-- 规格选择 -->
          <div class="pd-sku" v-if="skuList.length > 0">
            <div class="pd-sku-title">选择规格</div>
            <div class="pd-sku-list">
              <span
                v-for="sku in skuList"
                :key="sku.id"
                class="pd-sku-item"
                :class="{
                  active: selectedSku?.id === sku.id,
                  disabled: sku.stock <= 0
                }"
                @click="selectSku(sku)"
              >
                {{ sku.spec }}
                <small v-if="sku.stock <= 0" class="pd-sku-stockout">已售罄</small>
              </span>
            </div>
          </div>

          <!-- 数量 -->
          <div class="pd-qty">
            <span class="pd-qty-label">数量</span>
            <el-input-number
              v-model="quantity"
              :min="1"
              :max="selectedSku ? selectedSku.stock : (product?.stock || 1)"
              :disabled="(selectedSku ? selectedSku.stock : product?.stock || 0) <= 0"
              controls-position="right"
            />
            <span class="pd-qty-stock">库存 {{ selectedSku ? selectedSku.stock : product?.stock || 0 }} 件</span>
          </div>
        </div>

        <!-- 右侧：店铺信息 -->
        <div class="pd-shop">
          <div class="pd-shop-card">
            <div class="pd-shop-name">{{ product?.shopName }}</div>
            <div class="pd-shop-rating">
              <el-rate v-model="shopRating" disabled show-score text-color="#ff9900" score-template="{value}" />
            </div>
            <div class="pd-shop-stats">
              <span>粉丝 {{ shopFans || 0 }}</span>
              <span>商品 {{ shopProductCount || 0 }}</span>
            </div>
            <div class="pd-shop-btns">
              <el-button size="small" @click="goShop">进入店铺</el-button>
              <el-button size="small" @click="contactMerchant">联系商家</el-button>
            </div>
          </div>
        </div>
      </div>

      <!-- 操作按钮（桌面端） -->
      <div class="pd-actions" v-if="product">
        <el-button type="danger" size="large" @click="buyNow" :disabled="!canBuy">
          立即购买
        </el-button>
        <el-button type="warning" size="large" @click="addToCartFn" :disabled="!canBuy">
          加入购物车
        </el-button>
        <el-button size="large" @click="toggleCollect" :type="collected ? 'info' : 'default'">
          <el-icon><StarFilled v-if="collected" /><Star v-else /></el-icon>
          {{ collected ? '已收藏' : '收藏' }}
        </el-button>
      </div>

      <!-- ========== 详情区 Tabs ========== -->
      <div class="pd-detail-section">
        <div class="pd-detail-tabs">
          <span
            :class="{ active: detailTab === 'desc' }"
            @click="detailTab = 'desc'"
          >商品详情</span>
          <span
            :class="{ active: detailTab === 'comment' }"
            @click="detailTab = 'comment'"
          >商品评价 ({{ commentTotal }})</span>
        </div>

        <!-- 商品详情 -->
        <div class="pd-detail-body" v-show="detailTab === 'desc'">
          <div class="pd-desc" v-html="product?.description || '<p style=\'color:#999;text-align:center;padding:40px 0\'>暂无详情</p>'"></div>
        </div>

        <!-- 评价区 -->
        <div class="pd-detail-body" v-show="detailTab === 'comment'">
          <div class="pd-comment-summary" v-if="commentTotal > 0">
            <span class="pd-comment-rate">好评率 {{ commentRate }}%</span>
            <span class="pd-comment-total">共 {{ commentTotal }} 条评价</span>
          </div>
          <div v-if="comments.length === 0 && !commentLoading" class="pd-empty">
            <el-empty description="暂无评价" :image-size="80" />
          </div>
          <div
            v-for="c in comments"
            :key="c.id"
            class="pd-comment-item"
          >
            <div class="pd-comment-user">
              <el-avatar :size="36">{{ c.username?.charAt(0) || '?' }}</el-avatar>
              <div class="pd-comment-userinfo">
                <div class="pd-comment-username">{{ c.username || '匿名用户' }}</div>
                <el-rate v-model="c.score" disabled size="small" />
              </div>
              <span class="pd-comment-time">{{ c.createTime }}</span>
            </div>
            <div class="pd-comment-content">{{ c.content }}</div>
            <div class="pd-comment-images" v-if="c.imageList?.length">
              <img
                v-for="(img, i) in c.imageList"
                :key="i"
                :src="img"
                class="pd-comment-img"
                @error="onImgError($event)"
                @click="previewImage = img; previewVisible = true"
              />
            </div>
            <div class="pd-comment-reply" v-if="c.reply">
              <span class="pd-reply-label">商家回复：</span>{{ c.reply }}
            </div>
          </div>
          <el-pagination
            v-if="commentTotal > 10"
            v-model:current-page="commentPage"
            :page-size="10"
            :total="commentTotal"
            layout="prev, pager, next"
            @current-change="fetchComments"
            class="pd-comment-pager"
            small
          />
        </div>
      </div>

      <!-- 店铺推荐 -->
      <div class="pd-recommend" v-if="recommendProducts.length > 0">
        <div class="pd-recommend-title">本店推荐</div>
        <div class="pd-recommend-list">
          <div
            v-for="p in recommendProducts"
            :key="p.id"
            class="pd-recommend-item"
            @click="goProduct(p.id)"
          >
            <div class="pd-recommend-img">
              <img
                :src="p.image"
                :alt="p.name"
                @error="onImgError($event)"
              />
            </div>
            <div class="pd-recommend-name">{{ p.name }}</div>
            <div class="pd-recommend-price">¥{{ displayPrice(p.price) }}</div>
          </div>
        </div>
      </div>

      <!-- 看了又看 -->
      <div class="pd-recommend" v-if="alsoViewedProducts.length > 0">
        <div class="pd-recommend-title">看了又看</div>
        <div class="pd-recommend-list">
          <div
            v-for="p in alsoViewedProducts"
            :key="p.id"
            class="pd-recommend-item"
            @click="goProduct(p.id)"
          >
            <div class="pd-recommend-img">
              <img :src="p.image" :alt="p.name" @error="onImgError($event)" />
            </div>
            <div class="pd-recommend-name">{{ p.name }}</div>
            <div class="pd-recommend-price">¥{{ displayPrice(p.price) }}</div>
          </div>
        </div>
      </div>

      <!-- 买了又买 -->
      <div class="pd-recommend" v-if="alsoBoughtProducts.length > 0">
        <div class="pd-recommend-title">买了又买</div>
        <div class="pd-recommend-list">
          <div
            v-for="p in alsoBoughtProducts"
            :key="p.id"
            class="pd-recommend-item"
            @click="goProduct(p.id)"
          >
            <div class="pd-recommend-img">
              <img :src="p.image" :alt="p.name" @error="onImgError($event)" />
            </div>
            <div class="pd-recommend-name">{{ p.name }}</div>
            <div class="pd-recommend-price">¥{{ displayPrice(p.price) }}</div>
          </div>
        </div>
      </div>

      <!-- 同类推荐 -->
      <div class="pd-recommend" v-if="similarProductsList.length > 0">
        <div class="pd-recommend-title">同类推荐</div>
        <div class="pd-recommend-list">
          <div
            v-for="p in similarProductsList"
            :key="p.id"
            class="pd-recommend-item"
            @click="goProduct(p.id)"
          >
            <div class="pd-recommend-img">
              <img :src="p.image" :alt="p.name" @error="onImgError($event)" />
            </div>
            <div class="pd-recommend-name">{{ p.name }}</div>
            <div class="pd-recommend-price">¥{{ displayPrice(p.price) }}</div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 手机端底部固定操作栏 ========== -->
    <div v-if="product && !loading" class="pd-mobile-bar">
      <div class="pd-mobile-bar-left">
        <div class="pd-mobile-bar-icon" @click="toggleCollect">
          <el-icon :size="20"><StarFilled v-if="collected" /><Star v-else /></el-icon>
          <span>{{ collected ? '已收藏' : '收藏' }}</span>
        </div>
        <div class="pd-mobile-bar-icon" @click="goShop">
          <el-icon :size="20"><Shop /></el-icon>
          <span>店铺</span>
        </div>
      </div>
      <div class="pd-mobile-bar-right">
        <el-button type="warning" size="small" @click="addToCartFn" :disabled="!canBuy">
          加入购物车
        </el-button>
        <el-button type="danger" size="small" @click="buyNow" :disabled="!canBuy">
          立即购买
        </el-button>
      </div>
    </div>

    <!-- ========== 图片预览弹窗 ========== -->
    <el-dialog v-model="previewVisible" title="商品图片" width="auto" top="5vh" :modal="true" destroy-on-close>
      <img :src="previewImage || currentImage" style="max-width:80vw;max-height:80vh;display:block;margin:0 auto;border-radius:4px;" />
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star, StarFilled, Shop, ZoomIn } from '@element-plus/icons-vue'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import { getProductDetail, getProductComments, getProductsPage, type ProductDTO, type ProductSkuDTO } from '@/api/product'
import { addToCart as addToCartApi } from '@/api/cart'
import { addCollect, removeCollect, checkCollected } from '@/api/collect'
import { alsoViewed, alsoBought, similarProducts } from '@/api/recommend'
import type { CommentDTO } from '@/api/comment'
import { useAuthStore } from '@/stores/auth'

const FALLBACK_IMAGE = 'data:image/svg+xml,%3Csvg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200"%3E%3Crect fill="%23f5f5f5" width="200" height="200"/%3E%3Ctext x="50%25" y="50%25" fill="%23ccc" font-size="14" text-anchor="middle" dy=".3em"%3E图片加载失败%3C/text%3E%3C/svg%3E'

const route = useRoute()
const router = useRouter()

const auth = useAuthStore()

function checkLogin(): boolean {
  const token = auth.token
  if (!token) {
    ElMessage.warning('请先登录')
    router.push('/login')
    return false
  }
  return true
}

// ── 状态 ──
const loading = ref(false)
const product = ref<ProductDTO | null>(null)
const quantity = ref(1)
const selectedSku = ref<ProductSkuDTO | null>(null)
const collected = ref(false)
const shopRating = ref(4.5)
const shopFans = ref(0)
const shopProductCount = ref(0)

// 图片
const imageList = computed(() => {
  if (!product.value) return []
  const imgs: string[] = []
  if (product.value.image) imgs.push(product.value.image)
  if (product.value.imageList) {
    product.value.imageList.forEach((img) => {
      if (img.imageUrl && !imgs.includes(img.imageUrl)) imgs.push(img.imageUrl)
    })
  }
  return imgs.length > 0 ? imgs : ['']
})
const currentImage = ref('')
const previewVisible = ref(false)
const previewImage = ref('')

const previewIndex = computed(() => {
  const idx = imageList.value.indexOf(currentImage.value)
  return idx >= 0 ? idx : 0
})

const displayPrice = (p: number | undefined): string => {
  if (!p && p !== 0) return '0.00'
  return Number(p).toFixed(2)
}

// SKU
const skuList = computed(() => product.value?.skuList || [])

const canBuy = computed(() => {
  if (!product.value) return false
  if (skuList.value.length > 0) {
    return selectedSku.value !== null && selectedSku.value.stock > 0
  }
  return (product.value.stock || 0) > 0
})

// 详情/评价 tab
const detailTab = ref<'desc' | 'comment'>('desc')
const comments = ref<CommentDTO[]>([])
const commentTotal = ref(0)
const commentPage = ref(1)
const commentLoading = ref(false)
const commentRate = computed(() => {
  if (commentTotal.value === 0) return 100
  const good = comments.value.filter((c) => c.score >= 4).length
  return Math.round((good / comments.value.length) * 100)
})

// 推荐
const recommendProducts = ref<ProductDTO[]>([])
const alsoViewedProducts = ref<ProductDTO[]>([])
const alsoBoughtProducts = ref<ProductDTO[]>([])
const similarProductsList = ref<ProductDTO[]>([])

// ── 图片错误兜底 ──
function onImgError(e: Event, type?: string) {
  const img = e.target as HTMLImageElement
  if (img && !img.dataset['fallback']) {
    img.dataset['fallback'] = '1'
    img.src = FALLBACK_IMAGE
    if (type === 'main') {
      img.style.objectFit = 'contain'
      img.style.padding = '20px'
    }
  }
}

// ── 数据加载 ──
async function fetchProduct() {
  const id = Number(route.params.id)
  if (!id) {
    ElMessage.error('商品不存在')
    router.replace('/hotsale')
    return
  }
  loading.value = true
  try {
    const res = await getProductDetail(id)
    product.value = res.data
    currentImage.value = imageList.value[0] || ''
    if (skuList.value.length > 0) {
      const inStock = skuList.value.find((s) => s.stock > 0)
      if (inStock) selectedSku.value = inStock
    }
    fetchComments()
    fetchRecommend()
    checkFav()
  } catch (err: any) {
    // 401 未登录 → 提示请登录
    if (err?.response?.status === 401) {
      ElMessage.warning('请先登录后再查看商品详情')
      router.push('/login')
      return
    }
    ElMessage.error('商品不存在或已下架')
    router.replace('/hotsale')
  } finally {
    loading.value = false
  }
}

async function fetchComments() {
  if (!product.value) return
  commentLoading.value = true
  try {
    const res = await getProductComments(commentPage.value, 10, product.value.id)
    const data = res.data
    comments.value = data.records || []
    commentTotal.value = data.total || 0
  } catch { /* ignore */ }
  finally { commentLoading.value = false }
}

async function fetchRecommend() {
  if (!product.value) return
  try {
    const res = await getProductsPage({ current: 1, size: 6, shopId: product.value.shopId })
    const data = res.data
    recommendProducts.value = (data.records || []).filter((p) => p.id !== product.value!.id)
  } catch { /* ignore */ }

  // 看了又看
  try {
    const res = await alsoViewed(product.value.id, 6)
    alsoViewedProducts.value = (res.data || []).filter((p) => p.id !== product.value!.id)
  } catch { /* ignore */ }

  // 买了又买
  try {
    const res = await alsoBought(product.value.id, 6)
    alsoBoughtProducts.value = (res.data || []).filter((p) => p.id !== product.value!.id)
  } catch { /* ignore */ }

  // 同类推荐
  try {
    const res = await similarProducts(product.value.id, 6)
    similarProductsList.value = (res.data || []).filter((p) => p.id !== product.value!.id)
  } catch { /* ignore */ }
}

async function checkFav() {
  if (!product.value) return
  try {
    const res = await checkCollected(product.value.id)
    collected.value = res.data
  } catch { /* ignore */ }
}

function selectSku(sku: ProductSkuDTO) {
  if (sku.stock <= 0) return
  selectedSku.value = sku
  if (quantity.value > sku.stock) quantity.value = sku.stock
}

async function addToCartFn() {
  if (!checkLogin()) return
  if (!product.value) return
  try {
    await addToCartApi({ productId: product.value.id, skuId: selectedSku.value?.id, num: quantity.value })
    ElMessage.success('已加入购物车')
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '加入购物车失败')
  }
}

async function buyNow() {
  if (!checkLogin()) return
  if (!product.value) return

  const price = selectedSku.value?.price ?? product.value.price
  const spec = selectedSku.value?.spec ?? ''
  const settleItems = [{
    productId: product.value.id,
    productName: product.value.name,
    productImage: product.value.image || '',
    price,
    num: quantity.value,
    spec,
    shopId: product.value.shopId,
    shopName: product.value.shopName,
  }]

  try {
    await addToCartApi({ productId: product.value.id, skuId: selectedSku.value?.id, num: quantity.value })
  } catch { /* ignore */ }

  localStorage.setItem('settle_items', JSON.stringify(settleItems))
  router.push('/order-confirm')
}

async function toggleCollect() {
  if (!checkLogin()) return
  if (!product.value) return
  try {
    if (collected.value) {
      await removeCollect(product.value.id)
      collected.value = false
      ElMessage.success('已取消收藏')
    } else {
      await addCollect(product.value.id)
      collected.value = true
      ElMessage.success('收藏成功')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  }
}

function goShop() {
  if (product.value) router.push(`/shop/${product.value.shopId}`)
}

function contactMerchant() {
  if (!checkLogin()) return
  if (!product.value) return
  router.push({
    path: '/chat/merchant',
    query: {
      shopId: product.value.shopId,
      shopName: product.value.shopName || '店铺客服',
      merchantId: product.value.merchantId || product.value.shopId,
    },
  })
}

function goProduct(id: number) {
  router.push(`/product/${id}`)
}

watch(() => route.params.id, () => {
  if (route.params.id) fetchProduct()
})

onMounted(() => {
  window.scrollTo(0, 0)
  fetchProduct()
})
</script>

<style scoped>
/* ═══════════════ 全局 ═══════════════ */
.pd-page { min-height: 100vh; background: #f5f5f5; padding-bottom: 60px; }
.pd-container { max-width: 1200px; margin: 0 auto; padding: 16px 20px 40px; }
.pd-breadcrumb { margin-bottom: 16px; }

/* ── 骨架屏 ── */
.pd-skeleton { background: #fff; border-radius: 12px; padding: 24px; }
.pd-skeleton-main { display: flex; gap: 24px; }
.pd-skeleton-gallery { width: 400px; height: 400px; background: linear-gradient(90deg, #eee 25%, #f5f5f5 50%, #eee 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: 8px; flex-shrink: 0; }
.pd-skeleton-info { flex: 1; display: flex; flex-direction: column; gap: 16px; padding: 8px 0; }
.pd-skeleton-line { height: 20px; background: linear-gradient(90deg, #eee 25%, #f5f5f5 50%, #eee 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: 4px; }
.pd-skeleton-line.w-80 { width: 80%; }
.pd-skeleton-line.w-60 { width: 60%; }
.pd-skeleton-line.w-40 { width: 40%; }
.pd-skeleton-line.w-90 { width: 90%; }
.pd-skeleton-line.w-50 { width: 50%; }
.pd-skeleton-block { height: 80px; background: linear-gradient(90deg, #eee 25%, #f5f5f5 50%, #eee 75%); background-size: 200% 100%; animation: shimmer 1.5s infinite; border-radius: 4px; }
@keyframes shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }

/* ── 商品主区域 ── */
.pd-main {
  display: flex; gap: 24px;
  background: #fff; border-radius: 12px; padding: 24px;
  margin-bottom: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}

/* ─── 图片画廊 ─── */
.pd-gallery { width: 420px; flex-shrink: 0; }
.pd-main-img {
  position: relative; width: 420px; height: 420px;
  border: 1px solid #f0f0f0; border-radius: 8px; overflow: hidden;
  display: flex; align-items: center; justify-content: center;
  cursor: zoom-in;
}
.pd-main-img img { max-width: 100%; max-height: 100%; object-fit: contain; transition: transform 0.3s; }
.pd-main-img:hover img { transform: scale(1.05); }
.pd-main-img-hover {
  position: absolute; inset: 0; display: flex; align-items: center; justify-content: center;
  gap: 6px; color: #fff; background: rgba(0,0,0,0.25); font-size: 14px;
  opacity: 0; transition: opacity 0.3s;
}
.pd-main-img:hover .pd-main-img-hover { opacity: 1; }

.pd-thumb-list { display: flex; gap: 10px; margin-top: 12px; }
.pd-thumb {
  width: 64px; height: 64px; border: 2px solid transparent; border-radius: 8px;
  cursor: pointer; overflow: hidden; transition: all 0.2s;
  display: flex; align-items: center; justify-content: center; background: #fafafa;
}
.pd-thumb:hover { border-color: #f5a623; }
.pd-thumb.active { border-color: #e4393c; }
.pd-thumb img { width: 100%; height: 100%; object-fit: cover; }

/* ─── 商品信息 ─── */
.pd-info { flex: 1; min-width: 0; }
.pd-name { font-size: 22px; font-weight: 600; color: #1a1a1a; line-height: 1.4; margin: 0 0 16px; }

.pd-price-box {
  background: linear-gradient(135deg, #fff5f5 0%, #fff 100%);
  padding: 18px 20px; border-radius: 8px; margin-bottom: 16px;
}
.pd-price-label { color: #999; font-size: 13px; margin-right: 12px; }
.pd-price-current { font-size: 30px; font-weight: 700; color: #e4393c; letter-spacing: -1px; }
.pd-price-original { font-size: 14px; color: #bbb; text-decoration: line-through; margin-left: 12px; }

.pd-meta { display: flex; gap: 28px; color: #888; font-size: 13px; margin-bottom: 16px; padding-bottom: 16px; border-bottom: 1px solid #f0f0f0; }
.pd-meta em { font-style: normal; color: #666; }

/* 规格 */
.pd-sku { margin-bottom: 20px; }
.pd-sku-title { font-size: 13px; color: #999; margin-bottom: 10px; }
.pd-sku-list { display: flex; flex-wrap: wrap; gap: 10px; }
.pd-sku-item {
  position: relative; padding: 8px 20px; border: 1.5px solid #e0e0e0; border-radius: 6px;
  cursor: pointer; font-size: 13px; transition: all 0.2s; user-select: none;
}
.pd-sku-item:hover { border-color: #f5a623; }
.pd-sku-item.active { border-color: #e4393c; color: #e4393c; background: #fff5f5; box-shadow: 0 0 0 1px #e4393c; }
.pd-sku-item.disabled { color: #ccc; border-color: #eee; cursor: not-allowed; background: #fafafa; }
.pd-sku-stockout { display: block; font-size: 10px; color: #ccc; }

/* 数量 */
.pd-qty { display: flex; align-items: center; gap: 16px; }
.pd-qty-label { font-size: 13px; color: #999; }
.pd-qty-stock { font-size: 12px; color: #bbb; }

/* ─── 操作按钮（桌面） ─── */
.pd-actions {
  display: flex; gap: 12px; margin-bottom: 20px;
  background: #fff; border-radius: 12px; padding: 16px 24px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.04);
}
.pd-actions .el-button--large { padding: 12px 32px; border-radius: 6px; font-size: 15px; font-weight: 500; }

/* ─── 店铺信息 ─── */
.pd-shop { width: 200px; flex-shrink: 0; }
.pd-shop-card {
  border: 1px solid #f0f0f0; border-radius: 12px; padding: 24px 16px;
  text-align: center; background: #fafafa;
}
.pd-shop-name { font-size: 16px; font-weight: 600; color: #333; margin-bottom: 8px; }
.pd-shop-rating { margin-bottom: 10px; display: flex; justify-content: center; }
.pd-shop-stats { display: flex; justify-content: center; gap: 12px; font-size: 12px; color: #999; margin-bottom: 14px; }
.pd-shop-btns { display: flex; flex-direction: row; justify-content: center; gap: 8px; }
.pd-shop-btns .el-button { flex: 1; max-width: 120px; border-radius: 6px; }

/* ─── 详情区 ─── */
.pd-detail-section { background: #fff; border-radius: 12px; margin-bottom: 20px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); overflow: hidden; }
.pd-detail-tabs { display: flex; border-bottom: 1px solid #f0f0f0; }
.pd-detail-tabs span {
  padding: 14px 28px; cursor: pointer; font-size: 15px; font-weight: 500;
  color: #888; border-bottom: 2.5px solid transparent; transition: all 0.2s;
}
.pd-detail-tabs span:hover { color: #e4393c; }
.pd-detail-tabs span.active { color: #e4393c; border-bottom-color: #e4393c; }
.pd-detail-body { padding: 24px; min-height: 200px; }
.pd-desc { line-height: 1.8; color: #333; }
.pd-desc :deep(img) { max-width: 100%; height: auto; }
.pd-empty { text-align: center; padding: 40px 0; }

/* 评价 */
.pd-comment-summary { margin-bottom: 16px; padding-bottom: 12px; border-bottom: 1px solid #f0f0f0; display: flex; align-items: center; gap: 16px; }
.pd-comment-rate { font-size: 18px; font-weight: 600; color: #e4393c; }
.pd-comment-total { color: #999; font-size: 13px; }
.pd-comment-item { padding: 16px 0; border-bottom: 1px solid #f5f5f5; }
.pd-comment-item:last-child { border-bottom: none; }
.pd-comment-user { display: flex; align-items: center; gap: 12px; margin-bottom: 8px; }
.pd-comment-userinfo { flex: 1; min-width: 0; }
.pd-comment-username { font-size: 14px; color: #333; font-weight: 500; }
.pd-comment-time { margin-left: auto; font-size: 12px; color: #bbb; white-space: nowrap; }
.pd-comment-content { font-size: 14px; color: #333; line-height: 1.6; margin: 8px 0; }
.pd-comment-images { display: flex; gap: 10px; flex-wrap: wrap; }
.pd-comment-img { width: 80px; height: 80px; object-fit: cover; border-radius: 6px; cursor: pointer; transition: transform 0.2s; border: 1px solid #f0f0f0; }
.pd-comment-img:hover { transform: scale(1.05); }
.pd-comment-reply { margin-top: 8px; padding: 10px 14px; background: #f8f8f8; border-radius: 6px; font-size: 13px; color: #666; }
.pd-reply-label { color: #e4393c; font-weight: 500; }
.pd-comment-pager { margin-top: 16px; justify-content: center; }

/* ─── 本店推荐 ─── */
.pd-recommend { background: #fff; border-radius: 12px; padding: 20px 24px; box-shadow: 0 1px 4px rgba(0,0,0,0.04); }
.pd-recommend-title { font-size: 18px; font-weight: 600; margin-bottom: 16px; }
.pd-recommend-list { display: grid; grid-template-columns: repeat(6, 1fr); gap: 14px; }
.pd-recommend-item { cursor: pointer; text-align: center; transition: transform 0.2s; }
.pd-recommend-item:hover { transform: translateY(-4px); }
.pd-recommend-img {
  width: 100%; aspect-ratio: 1; border-radius: 8px; overflow: hidden;
  border: 1px solid #f0f0f0; margin-bottom: 8px;
}
.pd-recommend-img img { width: 100%; height: 100%; object-fit: cover; }
.pd-recommend-name { font-size: 13px; color: #333; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; margin-bottom: 4px; }
.pd-recommend-price { font-size: 15px; font-weight: 600; color: #e4393c; }

/* ─── 手机端固定底部栏 ─── */
.pd-mobile-bar {
  display: none;
  position: fixed; bottom: 0; left: 0; right: 0; z-index: 100;
  background: #fff; border-top: 1px solid #f0f0f0;
  padding: 8px 12px; align-items: center;
  box-shadow: 0 -2px 10px rgba(0,0,0,0.06);
}
.pd-mobile-bar-left { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
.pd-mobile-bar-icon {
  display: flex; flex-direction: column; align-items: center; gap: 2px;
  font-size: 10px; color: #888; cursor: pointer; padding: 4px 8px;
}
.pd-mobile-bar-right { flex: 1; display: flex; gap: 8px; justify-content: flex-end; }
.pd-mobile-bar-right .el-button { border-radius: 20px; padding: 8px 18px; font-size: 13px; flex: 0 1 auto; }

/* ═══════════════ 响应式 ═══════════════ */
@media (max-width: 1024px) {
  .pd-main { flex-wrap: wrap; }
  .pd-gallery { width: 100%; }
  .pd-main-img { width: 100%; max-width: 500px; height: auto; aspect-ratio: 1; margin: 0 auto; }
  .pd-thumb-list { justify-content: center; }
  .pd-shop { width: 100%; margin-top: 8px; }
  .pd-shop-card { display: flex; align-items: center; gap: 16px; padding: 16px; text-align: left; }
  .pd-shop-card > * { margin-bottom: 0 !important; }
  .pd-shop-rating { flex-shrink: 0; }
  .pd-shop-stats { flex-shrink: 0; }
  .pd-shop-btns { flex-direction: row; justify-content: center; flex-shrink: 0; }
  .pd-shop-btns .el-button { flex: none; width: auto; }
  .pd-recommend-list { grid-template-columns: repeat(4, 1fr); }
}

@media (max-width: 768px) {
  .pd-container { padding: 10px 12px 80px; }
  .pd-main { padding: 16px; border-radius: 8px; }
  .pd-gallery { width: 100%; }
  .pd-main-img { max-width: 100%; aspect-ratio: 1; }
  .pd-name { font-size: 18px; }
  .pd-price-current { font-size: 24px; }
  .pd-meta { flex-wrap: wrap; gap: 12px; }
  .pd-actions { display: none; }
  .pd-mobile-bar { display: flex; }
  .pd-detail-tabs span { padding: 12px 16px; font-size: 14px; }
  .pd-detail-body { padding: 16px; }
  .pd-recommend-list { grid-template-columns: repeat(3, 1fr); gap: 10px; }
  .pd-shop-card { flex-wrap: wrap; gap: 10px; }
  .pd-shop-btns { margin-left: 0; }
  .pd-breadcrumb { display: none; }
}

@media (max-width: 480px) {
  .pd-recommend-list { grid-template-columns: repeat(2, 1fr); }
  .pd-shop-card { justify-content: center; text-align: center; flex-direction: column; }
  .pd-shop-btns { flex-direction: column; width: 100%; }
  .pd-shop-btns .el-button { width: 100%; }
  }
</style>
