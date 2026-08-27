<template>
  <div class="page">
    <HeaderUser />
<div class="all">
    <!-- ========== 顶部区域：搜索栏 + 轮播（统一背景） ========== -->
    <div class="top-section">
      <SearchBar
        v-model:keyword="keyword"
        :hot-words="hotWords"
        @search="onSearch"
        @logo-click="onLogoClick"
      />

      <!-- 主体：左分类 / 中轮播 -->
      <div class="banner-section">
      <div class="main">
        <div class="container">
          <div class="main-card">
          <!-- 左侧：分类 -->
          <aside class="sidebar" @mouseleave="onLeaveCategory">
            <div class="sidebar-head" @click="onSelectAllCategories">
              <el-icon><Grid /></el-icon>
              <span>全部分类</span>
            </div>
            <ul class="cat-list">
              <li
                v-for="c in topCategories"
                :key="c.id"
                class="cat-item"
                :class="{ active: activeCategoryId === c.id || hoverCategoryId === c.id }"
                @mouseenter="onHoverCategory(c)"
                @click="onSelectCategory(c)"
              >
                <span class="cat-icon" :style="{ color: getCategoryColor(c.id) }">
                  <component :is="getCategoryIcon(c.id)" :size="18" />
                </span>
                <span class="cat-content">
                  <span class="cat-main">{{ c.name }}</span>
                  <span class="cat-subs">
                    <span
                      v-for="(sub, idx) in getSubCategoryList(c.id)"
                      :key="sub.id"
                      class="cat-sub-tag"
                      @click.stop="onSelectSubCategory(sub)"
                    >{{ sub.name }}<span v-if="idx < getSubCategoryList(c.id).length - 1" class="cat-sub-sep">/</span></span>
                  </span>
                </span>
                <el-icon class="cat-arrow"><ArrowRight /></el-icon>
              </li>
            </ul>
          </aside>

          <!-- 二级分类面板（hover 时显示） -->
          <div
            v-if="hoverCategoryId !== null && currentSubCategories.length > 0"
            class="subcat-panel"
            @mouseenter="currentTopCategory && onHoverCategory(currentTopCategory)"
            @mouseleave="onLeaveCategory"
          >
            <div class="subcat-title">{{ hoveredCategoryName }}</div>
            <div class="subcat-list">
              <div class="subcat-group">
                <span
                  v-for="sub in currentSubCategories"
                  :key="sub.id"
                  class="subcat-item"
                  :class="{ active: activeCategoryId === sub.id }"
                  @click="onSelectSubCategory(sub)"
                >{{ sub.name }}</span>
              </div>
            </div>
          </div>

          <!-- 中间：Banner 轮播 -->
          <section class="banner">
            <el-carousel :interval="4000" height="100%" arrow="hover">
              <el-carousel-item v-for="b in banners" :key="b.id">
                <div class="banner-item" :style="bannerStyle(b)" @click="onClickBanner(b)">
                  <div class="banner-mask"></div>
                  <div class="banner-text">
                    <div class="banner-eyebrow">{{ b.eyebrow }}</div>
                    <h2 class="banner-title">{{ b.title }}</h2>
                    <p class="banner-desc">{{ b.desc }}</p>
                    <el-button type="primary" class="banner-btn" @click.stop="onClickBanner(b)">
                      去逛逛<el-icon><ArrowRight /></el-icon>
                    </el-button>
                  </div>
                </div>
              </el-carousel-item>
            </el-carousel>
          </section>

          <!-- 右侧：分类卡片 -->
          <aside class="right-banner-container">
            <div
              v-for="item in rightCards"
              :key="item.id"
              class="right-banner-item"
              :style="{ backgroundImage: 'url(' + item.img + ')', backgroundColor: item.bg }"
              @click="goCategory(item)"
            >
              <div class="right-banner-mask"></div>
              <div class="right-banner-text">
                <div class="right-banner-title" :style="{ color: item.color }">{{ item.title }}</div>
                <div class="right-banner-sub">{{ item.sub }}</div>
              </div>
            </div>
          </aside>
        </div>
      </div>
    </div>
    </div><!-- 结束 banner-section -->
    </div><!-- 结束 top-section -->
    <!-- ========== 限时秒杀 + 热门商品 合并展示 ========== -->
    <div class="promo-section">
      <div class="container">
        <div class="promo-box">
          <div class="promo-inner">
            <!-- 左侧：限时秒杀（前 3 件）— 主推区 -->
            <div class="promo-col flash">
              <div class="promo-head">
                <div class="promo-title">
                  <span class="promo-title-icon">⚡</span>
                  <span class="promo-title-text flash">限时秒杀</span>
                  <span class="promo-title-sub">整点开抢 · 手慢无</span>
                </div>
                <div class="promo-head-actions">
                  <div class="flash-countdown" v-if="flashCountdown > 0">
                    <span class="flash-countdown-label">距结束</span>
                    <span class="flash-countdown-num">{{ flashCountdownText }}</span>
                  </div>
                  <span class="promo-more" @click="goFlashSale">查看更多 ›</span>
                </div>
              </div>
              <div class="promo-cards">
                <div
                  v-for="item in flashSaleItems.slice(0, 4)"
                  :key="item.id"
                  class="promo-card"
                  @click="goFlashSale"
                >
                  <div class="promo-img-wrap">
                    <el-image :src="item.productImage" fit="cover" class="promo-img" />
                    <div class="promo-badge">秒杀</div>
                  </div>
                  <div class="promo-info">
                    <div class="promo-name">{{ item.productName }}</div>
                    <div class="promo-price-row">
                      <span class="promo-flash-price">
                        <span class="promo-price-sym">¥</span>
                        {{ Number(item.flashPrice).toFixed(2) }}
                      </span>
                      <span class="promo-original-price">¥{{ Number(item.originalPrice).toFixed(2) }}</span>
                    </div>
                    <div class="promo-stock-row">
                      <span class="promo-stock">剩余 {{ item.stock }} 件</span>
                      <el-button size="small" class="promo-btn" @click.stop="goFlashSale">抢购</el-button>
                    </div>
                  </div>
                </div>
              </div>
            </div>

            <!-- 右侧：热门商品（前 2 件）— 辅助区 -->
            <div class="promo-col hot">
              <div class="promo-head">
                <div class="promo-title">
                  <span class="promo-title-icon">🔥</span>
                  <span class="promo-title-text hot">热门商品</span>
                  <span class="promo-title-sub">大家都在买</span>
                </div>
                <div class="promo-head-actions">
                  <span class="promo-more" @click="goHotSale">查看更多 ›</span>
                </div>
              </div>
              <div class="promo-cards">
                <div
                  v-for="(p, idx) in hotProducts.slice(0, 3)"
                  :key="p.id"
                  class="promo-card"
                  @click="onClickProduct(p)"
                >
                  <div class="promo-img-wrap">
                    <el-image :src="p.image || defaultImg" fit="cover" class="promo-img" />
                    <span class="promo-rank" :class="'rank-' + (idx + 1)">{{ idx + 1 }}</span>
                  </div>
                  <div class="promo-info">
                    <div class="promo-name">{{ p.name }}</div>
                    <div class="promo-price-row">
                      <span class="promo-price-sym">¥</span>
                      <span class="promo-hot-price">{{ Number(p.price).toFixed(2) }}</span>
                    </div>
                    <div class="promo-hot-sales">热度 {{ (p as any).viewPoint || Math.floor(Math.random() * 9000 + 1000) }}</div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 为你推荐 ========== -->
    <div class="goods-wrap">
      <div class="container goods-inner">
        <div class="goods-card-wrap">
          <div class="goods-head">
            <div class="goods-title">
              <span class="goods-title-bar"></span>
              <span class="goods-title-text">{{ activeCategoryName || '为你推荐' }}</span>
              <span class="goods-title-sub" v-if="!activeCategoryName">猜你喜欢 · 好物精选</span>
            </div>
            <div class="goods-sort">
              <span
                class="sort-tab"
                :class="{ active: sortBy === 'default' }"
                @click="setSort('default')"
              >综合</span>
              <span
                class="sort-tab"
                :class="{ active: sortBy === 'sales' }"
                @click="setSort('sales')"
              >销量</span>
              <span
                class="sort-tab"
                :class="{ active: sortBy === 'price_asc' }"
                @click="setSort('price_asc')"
              >价格 ↑</span>
              <span
                class="sort-tab"
                :class="{ active: sortBy === 'price_desc' }"
                @click="setSort('price_desc')"
              >价格 ↓</span>
            </div>
          </div>

          <el-empty
            v-if="!loading && productList.length === 0"
            description="暂无商品"
            class="goods-empty"
          />

          <div v-else class="goods-grid">
            <div
              v-for="p in sortedProducts"
              :key="p.id"
              class="goods-card"
              @click="onClickProduct(p)"
            >
              <span class="hover-border"></span>
              <div class="goods-cover">
                <el-image
                  :src="p.image || defaultImg"
                  fit="cover"
                  :preview-src-list="[p.image || defaultImg]"
                />
                <span v-if="isHot(p)" class="goods-tag">热销</span>
                <span v-if="(p as any).discount" class="goods-tag goods-tag-discount">特价</span>
              </div>
              <div class="goods-body">
                <div class="goods-name">{{ p.name }}</div>
                <div class="goods-promos">
                  <span class="promo-tag">券</span>
                  <span class="promo-text">满{{ Math.floor(Number(p.price) * 2) }}减{{ Math.floor(Number(p.price) * 0.1) }}</span>
                </div>
                <div class="goods-sales-row">
                  <span class="goods-sales-label">{{ (p as any).sales || Math.floor(Math.random() * 9000 + 1000) }}人已购买</span>
                  <span class="goods-quality">品质保障</span>
                </div>
                <div class="goods-price-wrap">
                    <div class="goods-price-row">
                      <span class="goods-price-sym">¥</span>
                      <span class="goods-price">{{ Number(p.price).toFixed(2) }}</span>
                    </div>
                  </div>
              </div>
            </div>
          </div>

          <div v-if="loading && currentPage > 1" class="goods-loading">
            <div class="loading-spinner">
              <el-icon class="is-loading" :size="28"><Loading /></el-icon>
              <span>正在加载更多商品...</span>
            </div>
          </div>

          <div v-else-if="loading" class="goods-loading">
            <div class="loading-spinner">
              <el-icon class="is-loading" :size="28"><Loading /></el-icon>
              <span>正在加载商品...</span>
            </div>
          </div>

          <!-- 滚轮滑动加载哨兵：始终存在，当进入视口时触发 loadMore -->
          <div ref="sentinelRef" class="goods-sentinel" v-if="currentPage < totalPages"></div>

          <!-- 没有更多了 -->
          <div v-if="!loading && currentPage >= totalPages && productList.length > 0" class="goods-no-more">
            <span>— 没有更多了 —</span>
          </div>
        </div>
      </div>
    </div>

    <!-- ========== 页脚 ========== -->
    <div class="footer">
      <div class="container footer-inner">
        <div>© 2026 智慧购 · All Rights Reserved</div>
        <div class="footer-links">
          <span>关于我们</span>
          <span class="dot">·</span>
          <span>商家入驻</span>
          <span class="dot">·</span>
          <span @click="openPlatformChat" style="cursor:pointer">联系管理员</span>
          <span class="dot">·</span>
          <span>帮助中心</span>
          <span class="dot">·</span>
          <span>用户协议</span>
        </div>
      </div>
    </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import {
  ArrowRight,
  Grid,
  Loading,
  Refrigerator,
  Monitor,
  Iphone,
  Bell,
  Apple,
  CoffeeCup,
  Tools,
  House,
  Dish,
  Van,
  Management,
} from '@element-plus/icons-vue'
import HeaderUser from '../../components/layout/HeaderUser.vue'
import SearchBar from '../../components/layout/SearchBar.vue'
import { getProductsPage, getRecommendProducts, getHotProducts, type ProductDTO } from '../../api/product'
import { getFlashSaleList, type FlashSaleItem } from '../../api/flashSale'
import { guessYouLike } from '../../api/recommend'
import { useCategoryStore, type Category } from '../../stores/category'

const router = useRouter()
const defaultImg = 'https://picsum.photos/seed/default/300/300'
const categoryStore = useCategoryStore()
const categories = computed(() => categoryStore.categories)
const topCategories = computed(() => categoryStore.topCategories)
const subCategoryMap = computed(() => categoryStore.subCategoryMap)

/* ===== 右侧卡片数据 ===== */
interface RightCardItem {
  id: number
  title: string
  sub: string
  color: string
  bg: string
  img: string
  categoryId?: number
}
const rightCards: RightCardItem[] = [
  {
    id: 1,
    title: '国家补贴',
    sub: '惠享正品',
    color: '#52c41a',
    bg: '#f6ffed',
    img: 'https://picsum.photos/seed/card1/200/200',
    categoryId: 1,
  },
  {
    id: 2,
    title: '精致美妆',
    sub: '品质之选',
    color: '#eb2f96',
    bg: '#fff0f6',
    img: 'https://picsum.photos/seed/card2/200/200',
    categoryId: 6,
  },
  {
    id: 3,
    title: '超值百货',
    sub: '省心省钱',
    color: '#1890ff',
    bg: '#e6f7ff',
    img: 'https://picsum.photos/seed/card3/200/200',
    categoryId: 9,
  },
  {
    id: 4,
    title: '品质五金',
    sub: '超值特惠',
    color: '#fa8c16',
    bg: '#fff7e6',
    img: 'https://picsum.photos/seed/card4/200/200',
    categoryId: 7,
  },
]

function goCategory(item: RightCardItem) {
  if (item.categoryId) {
    router.push({ path: '/hotsale', query: { category: item.categoryId } })
  } else {
    router.push('/hotsale')
  }
}

function openPlatformChat() {
  router.push('/platform-chat')
}

function goHotSale() {
  router.push('/hotsale')
}

/* ===== 搜索 ===== */
const keyword = ref('')
const defaultHotWords = ['夏日T恤', '连衣裙', '运动鞋', '蓝牙耳机', '护肤品', '家居好物', '男士护肤', '休闲鞋靴', '手机配件']
const hotWords = computed(() => {
  const subCats = categories.value.filter((c) => c.parentId && c.parentId > 0)
  if (subCats.length === 0) return defaultHotWords
  const names = subCats.map((c) => c.name).filter((n) => n && n.trim())
  const unique = [...new Set(names)]
  return unique.length > 0 ? unique.slice(0, 12) : defaultHotWords
})

/* ===== 轮播图 ===== */
interface BannerItem {
  id: number
  eyebrow: string
  title: string
  desc: string
  img: string
  bg: string
  link?: string
}
const banners = ref<BannerItem[]>([])
const bannerLoading = ref(false)

const defaultBanners: BannerItem[] = [
  {
    id: 1,
    eyebrow: '新品首发',
    title: '618 品质盛典',
    desc: '爆款清单 · 低至 5 折起 · 限时抢购',
    img: 'https://picsum.photos/seed/banner1/800/400',
    bg: 'linear-gradient(135deg, #ff5000 0%, #ff8c42 100%)',
  },
  {
    id: 2,
    eyebrow: '特惠专区',
    title: '百亿补贴',
    desc: '大牌好货 · 全网低价 · 别错过',
    img: 'https://picsum.photos/seed/banner2/800/400',
    bg: 'linear-gradient(135deg, #ff8c42 0%, #ffb88c 100%)',
  },
  {
    id: 3,
    eyebrow: '新人专享',
    title: '首单立减',
    desc: '新用户注册即享 · 满 100 减 30',
    img: 'https://picsum.photos/seed/banner3/800/400',
    bg: 'linear-gradient(135deg, #e65c00 0%, #ffb347 100%)',
  },
  {
    id: 4,
    eyebrow: '数码盛典',
    title: '数码狂欢节',
    desc: '爆款数码好物 · 最高直降 2000',
    img: 'https://picsum.photos/seed/banner4/800/400',
    bg: 'linear-gradient(135deg, #ff5000 0%, #fa8c16 100%)',
  },
]

function bannerStyle(b: BannerItem) {
  return {
    backgroundColor: b.bg,
    backgroundImage: 'url(' + b.img + ')',
    backgroundPosition: '80% center',
    backgroundRepeat: 'no-repeat',
    backgroundSize: 'cover',
  }
}

async function loadBanners() {
  bannerLoading.value = true
  try {
    // 可扩展为从后端获取轮播图配置
    // const res: any = await getBannerList()
    // if (res?.data?.length) { banners.value = res.data; return }
    banners.value = defaultBanners
  } catch {
    banners.value = defaultBanners
  } finally {
    bannerLoading.value = false
  }
}

function onClickBanner(_b: BannerItem) {
  router.push('/hotsale')
}

/* ===== 分类 & 商品 ===== */
const activeCategoryId = ref<number | null>(null)
const activeCategoryName = ref<string>('')
const hoverCategoryId = ref<number | null>(null)
const hoveredCategoryName = ref<string>('')
let leaveTimer: number | null = null
const productList = ref<ProductDTO[]>([])
const loading = ref(false)
const currentPage = ref(1)
const totalPages = ref(1)
const totalItems = ref(0)
const sortBy = ref<'default' | 'price_asc' | 'price_desc' | 'sales'>('default')
const pageSize = 12
const hotProducts = ref<ProductDTO[]>([])

// 秒杀专区
const flashSaleItems = ref<FlashSaleItem[]>([])
const flashCountdown = ref(0)
let flashTimer: number | null = null
const flashCountdownText = computed(() => {
  const t = flashCountdown.value
  if (t <= 0) return '00:00:00'
  const h = Math.floor(t / 3600)
  const m = Math.floor((t % 3600) / 60)
  const s = t % 60
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

async function fetchFlashSale() {
  try {
    const res = await getFlashSaleList()
    flashSaleItems.value = res.data || []
    updateFlashCountdown()
  } catch { /* ignore */ }
}

function updateFlashCountdown() {
  const now = Date.now()
  let nearestEnd = Infinity
  for (const item of flashSaleItems.value) {
    const end = new Date(item.endTime).getTime()
    if (end > now && end < nearestEnd) nearestEnd = end
  }
  if (nearestEnd < Infinity) {
    flashCountdown.value = Math.max(0, Math.floor((nearestEnd - now) / 1000))
  } else {
    flashCountdown.value = 0
  }
}

function goFlashSale() {
  router.push('/flash-sale')
}

const currentTopCategory = computed(() => {
  if (hoverCategoryId.value === null) return null
  const top = topCategories.value.find((c) => c.id === hoverCategoryId.value)
  if (top) return top
  const sub = categories.value.find((c) => c.id === hoverCategoryId.value)
  if (sub) return topCategories.value.find((c) => c.id === sub.parentId) || null
  return null
})

const currentSubCategories = computed(() =>
  hoverCategoryId.value !== null ? subCategoryMap.value.get(hoverCategoryId.value) || [] : [],
)

const sortedProducts = computed(() => {
  const list = [...productList.value]
  switch (sortBy.value) {
    case 'price_asc': return list.sort((a, b) => a.price - b.price)
    case 'price_desc': return list.sort((a, b) => b.price - a.price)
    case 'sales': return list.sort((a, b) => (b as any).sales - (a as any).sales)
    default: return list
  }
})

/** 个性化推荐算法：优先使用 Redis 缓存的推荐接口，降级到普通商品分页 */
async function loadProducts(categoryId?: number) {
  loading.value = true
  currentPage.value = 1
  try {
    // 首页「为你推荐」：优先使用猜你喜欢接口（Redis 缓存 + 协同过滤）
    if (!categoryId && !keyword.value.trim()) {
      try {
        const res = await guessYouLike(pageSize)
        if (res.data && Array.isArray(res.data) && res.data.length > 0) {
          productList.value = res.data
          totalItems.value = res.data.length
          // 设置一个较大的总页数，让用户可以继续滚动加载更多
          totalPages.value = 999
          loading.value = false
          setupObserver()
          return
        }
      } catch {
        // 推荐接口失败，降级到普通商品列表
      }
    }

    // 降级方案：使用普通商品分页
    const params: any = { current: 1, size: pageSize }
    if (keyword.value.trim()) params.name = keyword.value.trim()
    if (categoryId) params.categoryId = categoryId
    const res: any = await getRecommendProducts(params)
    const data = res?.data
    const list = data?.records || data?.list || data || []
    productList.value = Array.isArray(list) ? list : getFallbackProducts()
    totalItems.value = data?.total || productList.value.length
    totalPages.value = data?.pages || Math.ceil(totalItems.value / pageSize)
    if (!productList.value.length) productList.value = getFallbackProducts()
  } catch {
    productList.value = getFallbackProducts()
    totalPages.value = 1
    totalItems.value = productList.value.length
  } finally {
    loading.value = false
    setupObserver()
  }
}

/** 无限滚动加载更多：使用 getProductsPage 分页加载 */
async function loadMore() {
  if (currentPage.value >= totalPages.value || loading.value) return
  loading.value = true
  currentPage.value++
  try {
    const params: any = { current: currentPage.value, size: pageSize }
    if (keyword.value.trim()) params.name = keyword.value.trim()
    if (activeCategoryId.value) params.categoryId = activeCategoryId.value
    const res: any = await getProductsPage(params)
    const data = res?.data
    const list = data?.records || data?.list || data || []
    if (Array.isArray(list) && list.length > 0) {
      productList.value = [...productList.value, ...list]
    }
    // 更新总页数
    if (data?.pages) totalPages.value = data.pages
    if (data?.total) totalItems.value = data.total
  } catch {
    // 静默失败
  } finally {
    loading.value = false
  }
}

async function loadHotProducts() {
  try {
    const res: any = await getHotProducts(8)
    const list = res?.data?.data || res?.data || []
    if (Array.isArray(list) && list.length > 0) {
      hotProducts.value = list.slice(0, 8)
    } else {
      hotProducts.value = getFallbackProducts().slice(0, 8)
    }
  } catch {
    hotProducts.value = getFallbackProducts().slice(0, 8)
  }
}

function setSort(by: 'default' | 'price_asc' | 'price_desc' | 'sales') {
  sortBy.value = by
}

function onLogoClick() {
  keyword.value = ''
  activeCategoryId.value = null
  activeCategoryName.value = ''
  loadProducts()
}

function onSearch() {
  const kw = keyword.value?.trim() || ''
  if (kw) {
    router.push({ path: '/hotsale', query: { keyword: kw } })
  } else {
    router.push('/hotsale')
  }
}

function onHoverCategory(c: Category) {
  if (leaveTimer !== null) {
    window.clearTimeout(leaveTimer)
    leaveTimer = null
  }
  if (c && c.id) {
    hoverCategoryId.value = c.id
    hoveredCategoryName.value = c.name
  } else if (currentTopCategory.value) {
    hoverCategoryId.value = currentTopCategory.value.id
    hoveredCategoryName.value = currentTopCategory.value.name
  }
}

function onLeaveCategory() {
  if (leaveTimer !== null) {
    window.clearTimeout(leaveTimer)
  }
  leaveTimer = window.setTimeout(() => {
    hoverCategoryId.value = null
    leaveTimer = null
  }, 200)
}

function onSelectAllCategories() {
  router.push('/hotsale')
}

function onSelectCategory(c: Category) {
  router.push({ path: '/hotsale', query: { category: c.id } })
}

function onSelectSubCategory(sub: Category) {
  router.push({ path: '/hotsale', query: { category: sub.id } })
}

function onClickProduct(p: ProductDTO) {
  router.push(`/product/${p.id}`)
}

function isHot(p: ProductDTO) {
  return p.id % 3 === 0
}

/* ===== 分类图标和颜色映射 ===== */
function getCategoryIcon(id: number) {
  const iconMap: Record<number, any> = {
    1: Refrigerator,
    2: Monitor,
    3: Iphone,
    4: Bell,
    5: Apple,
    6: CoffeeCup,
    7: Tools,
    8: House,
    9: Dish,
    10: Van,
    11: Management,
  }
  return iconMap[id] || Grid
}

function getCategoryColor(id: number) {
  const colorMap: Record<number, string> = {
    1: '#1890ff',
    2: '#722ed1',
    3: '#ff0f23',
    4: '#fa8c16',
    5: '#52c41a',
    6: '#eb2f96',
    7: '#f5222d',
    8: '#13c2c2',
    9: '#2f54eb',
    10: '#fa541c',
    11: '#faad14',
  }
  return colorMap[id] || '#666'
}

function getSubCategoryList(parentId: number) {
  return categoryStore.getSubCategories(parentId).slice(0, 3)
}
function getFallbackProducts(): any[] {
  return [
    { id: 1, name: '蒸汽眼罩助眠热敷眼罩', categoryId: 1, categoryName: '个护', shopId: 1, shopName: '家居旗舰店', price: 22.7, image: 'https://picsum.photos/seed/p1/400/400', status: 1, description: '', stock: 100, createTime: '', sales: 3520, discount: true },
    { id: 2, name: '洁丽雅一次性洗脸巾', categoryId: 2, categoryName: '家居', shopId: 1, shopName: '时尚女装', price: 27.9, image: 'https://picsum.photos/seed/p2/400/400', status: 1, description: '', stock: 120, createTime: '', sales: 8840 },
    { id: 3, name: '可复美胶原棒精华液', categoryId: 3, categoryName: '美妆', shopId: 2, shopName: '美妆优选', price: 818, image: 'https://picsum.photos/seed/p3/400/400', status: 1, description: '', stock: 200, createTime: '', sales: 1260, discount: true },
    { id: 4, name: 'BKT 护腰坐垫人体工学椅', categoryId: 4, categoryName: '家具', shopId: 3, shopName: '家居百货', price: 198, image: 'https://picsum.photos/seed/p4/400/400', status: 1, description: '', stock: 500, createTime: '', sales: 5630 },
    { id: 5, name: 'BKT 护腰坐垫靠垫一体', categoryId: 5, categoryName: '家具', shopId: 4, shopName: '潮流男装', price: 198, image: 'https://picsum.photos/seed/p5/400/400', status: 1, description: '', stock: 90, createTime: '', sales: 4210 },
    { id: 6, name: 'XTOOL 激光雕刻机小型便携', categoryId: 6, categoryName: '五金', shopId: 5, shopName: '五金优选', price: 12697.62, image: 'https://picsum.photos/seed/p6/400/400', status: 1, description: '', stock: 999, createTime: '', sales: 870 },
    { id: 7, name: '男士冰丝内裤', categoryId: 7, categoryName: '内衣', shopId: 6, shopName: '运动潮流', price: 46.55, image: 'https://picsum.photos/seed/p7/400/400', status: 1, description: '', stock: 80, createTime: '', sales: 12500, discount: true },
    { id: 8, name: '休闲裤男士夏季薄款', categoryId: 8, categoryName: '男装', shopId: 7, shopName: '夏日潮流', price: 79.9, image: 'https://picsum.photos/seed/p8/400/400', status: 1, description: '', stock: 160, createTime: '', sales: 7230 },
    { id: 9, name: '彪马男士内裤', categoryId: 9, categoryName: '内衣', shopId: 8, shopName: '鞋类优选', price: 159, image: 'https://picsum.photos/seed/p9/400/400', status: 1, description: '', stock: 70, createTime: '', sales: 3890 },
    { id: 10, name: '女士内裤', categoryId: 10, categoryName: '内衣', shopId: 9, shopName: '配饰潮流', price: 49.5, image: 'https://picsum.photos/seed/p10/400/400', status: 1, description: '', stock: 110, createTime: '', sales: 16100, discount: true },
    { id: 11, name: '华硕 ROG 全家桶台式电脑', categoryId: 11, categoryName: '数码', shopId: 10, shopName: '数码优选', price: 5272, image: 'https://picsum.photos/seed/p11/400/400', status: 1, description: '', stock: 300, createTime: '', sales: 2450 },
    { id: 12, name: '松野湃男士速干透气健身', categoryId: 12, categoryName: '男装', shopId: 11, shopName: '夏季T恤', price: 449, image: 'https://picsum.photos/seed/p12/400/400', status: 1, description: '', stock: 220, createTime: '', sales: 6780 },
    { id: 13, name: '多功能料理机家用', categoryId: 13, categoryName: '家电', shopId: 12, shopName: '厨房电器', price: 459, image: 'https://picsum.photos/seed/p13/400/400', status: 1, description: '', stock: 95, createTime: '', sales: 4320, discount: true },
    { id: 14, name: '智能蓝牙音箱', categoryId: 14, categoryName: '数码', shopId: 13, shopName: '数码优选', price: 259, image: 'https://picsum.photos/seed/p14/400/400', status: 1, description: '', stock: 150, createTime: '', sales: 9870 },
    { id: 15, name: '真无线蓝牙耳机', categoryId: 15, categoryName: '数码', shopId: 14, shopName: '蓝牙耳机馆', price: 199, image: 'https://picsum.photos/seed/p15/400/400', status: 1, description: '', stock: 200, createTime: '', sales: 15320, discount: true },
  ]
}

let observer: IntersectionObserver | null = null
const sentinelRef = ref<HTMLElement | null>(null)

/** 设置无限滚动 IntersectionObserver */
function setupObserver() {
  if (observer) {
    observer.disconnect()
    observer = null
  }
  nextTick(() => {
    const sentinel = sentinelRef.value
    if (sentinel) {
      observer = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting && !loading.value && currentPage.value < totalPages.value) {
          loadMore()
        }
      }, { rootMargin: '0px 0px 100px 0px' })
      observer.observe(sentinel)
    }
  })
}

onMounted(async () => {
  await categoryStore.fetchCategories()
  await Promise.all([loadBanners(), loadProducts(), loadHotProducts(), fetchFlashSale()])
  // 秒杀倒计时
  if (flashCountdown.value > 0) {
    flashTimer = window.setInterval(() => {
      if (flashCountdown.value > 0) flashCountdown.value--
      else updateFlashCountdown()
    }, 1000)
  }
})
onUnmounted(() => {
  if (observer) observer.disconnect()
  if (flashTimer) clearInterval(flashTimer)
})
</script>

<style scoped>
/* ===============================================================
   全局变量
   =============================================================== */
.page {
  min-height: 100vh;
  background: #fff;
  color: #333;
  font-family: 'PingFang SC', 'Microsoft YaHei', 'Helvetica Neue', Arial, sans-serif;
  width: 100%;
}

.container {
  width: 90%;
  max-width: 1900px;
  margin: 0 auto;
  padding: 0 16px;
  box-sizing: border-box;
}

/* ===============================================================
   顶部区域：搜索栏 + 轮播（统一背景）
   =============================================================== */
.top-section {
  background: transparent;
}
/* 搜索栏白色背景，logo 清晰可见 */
.top-section :deep(.search-bar) {
  background: #fff;
}
.all {
  width: 100%;
  background: #fff;
}

/* ===============================================================
   主体：左分类 / 中轮播
   =============================================================== */
.main {
  padding: 16px 0 0;
  background: transparent;
}
.main-card {
  display: grid;
  grid-template-columns: 1fr 3fr 1fr;
  gap: 12px;
  align-items: stretch;
  aspect-ratio: 16 / 5.5;
  background: #fff;
  border-radius: var(--radius-lg);
  position: relative;
  overflow: hidden;
}

/* 左侧分类 */
.sidebar {
  background: #f7f7f7;
  border-radius: 12px;
  overflow: visible;
  display: flex;
  flex-direction: column;
  position: relative;
  height: 100%;
}
.sidebar-head {
  background: var(--primary);
  color: #fff;
  padding: 10px 16px;
  font-size: 17px;
  font-weight: 700;
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  user-select: none;
  border-radius: 12px 12px 0 0;
}
.sidebar-head:hover {
  opacity: 0.9;
}
.cat-list {
  list-style: none;
  margin: 0;
  padding: 4px 0;
  flex: 1;
  background: #f7f7f7;
  border-radius: 0 0 12px 12px;
}
.cat-item {
  display: flex;
  align-items: center;
  padding: 9px 14px;
  font-size: 16px;
  color: #444;
  cursor: pointer;
  transition: all 0.15s;
}
.cat-icon {
  width: 24px;
  height: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  margin-right: 8px;
}
.cat-content {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 2px;
}
.cat-main {
  font-weight: 600;
  color: #222;
  font-size: 16px;
}
.cat-subs {
  font-size: 14px;
  color: #999;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  width: 100%;
}
.cat-sub-tag {
  font-size: 14px;
  color: #999;
  cursor: pointer;
  transition: color 0.15s;
}
.cat-sub-tag:hover {
  color: var(--primary);
}
.cat-sub-sep {
  margin: 0 3px;
  color: #ddd;
}
.cat-item:hover {
  background: #fff;
}
.cat-item:hover .cat-main,
.cat-item:hover .cat-arrow {
  color: var(--primary);
}
.cat-item.active {
  background: #fff;
}
.cat-item.active .cat-main,
.cat-item.active .cat-arrow {
  color: var(--primary);
}
.cat-arrow {
  color: #bbb;
  font-size: 12px;
  flex-shrink: 0;
  margin-left: 8px;
}

/* 二级分类面板 - 紧贴灰色分类容器右侧，完全覆盖轮播图 */
.subcat-panel {
  position: absolute;
  left: calc(20% + 12px);
  right: calc(20% + 12px);
  top: 16px;
  bottom: 0px;
  background: #fff;
  border-radius: 0 var(--radius-lg) var(--radius-lg) 0;
  padding: 24px;
  box-shadow: 2px 2px 8px rgba(0, 0, 0, 0.08);
  z-index: 50;
  animation: fadeSlide 0.18s ease-out;
}
@keyframes fadeSlide {
  from {
    opacity: 0;
    transform: translateX(-8px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}
.subcat-title {
  font-size: 15px;
  font-weight: 700;
  color: var(--primary);
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f0f0;
}
.subcat-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.subcat-group {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.subcat-item {
  display: inline-block;
  padding: 6px 14px;
  font-size: 13px;
  color: var(--text-1);
  background: #f7f7f7;
  border-radius: 6px;
  cursor: pointer;
  transition: all 0.15s;
}
.subcat-item:hover {
  background: var(--primary);
  color: #fff;
  transform: translateY(-1px);
}
.subcat-item.active {
  background: var(--primary);
  color: #fff;
  font-weight: 600;
}

/* 中间 Banner - 京东风格 */
.banner {
  background-color: #f7f7f7;
  border-radius: 12px;
  overflow: hidden;
  height: 100%;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.banner :deep(.el-carousel),
.banner :deep(.el-carousel__container),
.banner :deep(.el-carousel__item) {
  height: 100%;
  width: 100%;
}
.banner-item {
  width: 100%;
  height: 100%;
  display: block;
  position: relative;
  cursor: pointer;
  overflow: hidden;
}
.banner-mask {
  position: absolute;
  left: 0;
  top: 0;
  width: 55%;
  height: 100%;
  background: linear-gradient(to right, rgba(0,0,0,0.28), rgba(0,0,0,0));
  pointer-events: none;
}
.banner-text {
  position: absolute;
  left: 48px;
  top: 50%;
  transform: translateY(-50%);
  max-width: 420px;
  z-index: 2;
  color: #fff;
}
.banner-eyebrow {
  font-size: 12px;
  letter-spacing: 2px;
  opacity: 0.95;
  margin-bottom: 8px;
  background: rgba(255,255,255,0.22);
  display: inline-block;
  padding: 2px 12px;
  border-radius: 999px;
}
.banner-title {
  font-size: 32px;
  font-weight: 900;
  margin: 0 0 8px;
  letter-spacing: 1px;
  text-shadow: 0 2px 8px rgba(0,0,0,0.15);
}
.banner-desc {
  font-size: 14px;
  opacity: 0.95;
  margin: 0 0 20px;
}
.banner-btn {
  background: #fff;
  color: var(--primary);
  border-color: #fff;
  font-weight: 700;
  padding: 8px 22px;
  border-radius: 999px;
  transition: all 0.15s;
  font-size: 14px;
}
.banner-btn:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

/* 右侧：分类卡片 */
.right-banner-container {
  height: 100%;
  width: 100%;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.right-banner-item {
  background-color: #f7f7f7;
  background-position: 92% 50%;
  background-repeat: no-repeat;
  background-size: contain;
  border-radius: 12px;
  overflow: hidden;
  position: relative;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  padding: 0 18px;
  height: calc((100% - 18px) / 4);
  width: 100%;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.04);
}
.right-banner-item:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 14px rgba(0, 0, 0, 0.1);
}
.right-banner-mask {
  position: absolute;
  right: 0;
  top: 0;
  bottom: 0;
  width: 50%;
  background: linear-gradient(to right, transparent 0%, transparent 50%, rgba(255,255,255,0.3) 100%);
  pointer-events: none;
}
.right-banner-text {
  position: relative;
  z-index: 2;
  display: flex;
  flex-direction: column;
  gap: 4px;
}
.right-banner-title {
  font-size: 16px;
  font-weight: 700;
  line-height: 1.2;
}
.right-banner-sub {
  font-size: 13px;
  color: #666;
}

/* ===============================================================
   为你推荐
   =============================================================== */
.goods-wrap {
  padding: 10px 0 30px;
}
.goods-inner {
  display: flex;
  flex-direction: column;
}
.goods-card-wrap {
  background: #fff;
  border-radius: var(--radius-lg);
  overflow: hidden;
}
.goods-head {
  padding: 20px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #f0f0f0;
}
.goods-title {
  display: flex;
  align-items: center;
  gap: 12px;
}
.goods-title-bar {
  display: inline-block;
  width: 4px;
  height: 18px;
  background: var(--primary);
  border-radius: 2px;
}
.goods-title-text {
  font-size: 20px;
  font-weight: 800;
  color: var(--text-1);
}
.goods-title-sub {
  font-size: 12px;
  color: var(--text-3);
}
.goods-sort {
  display: flex;
  align-items: center;
  gap: 6px;
}
.sort-tab {
  font-size: 13px;
  color: var(--text-2);
  cursor: pointer;
  padding: 4px 12px;
  border-radius: 6px;
  transition: all 0.15s;
  background: transparent;
  user-select: none;
}
.sort-tab:hover {
  background: #fff6f0;
  color: var(--primary);
}
.sort-tab.active {
  background: var(--primary);
  color: #fff;
  font-weight: 600;
}
.sort-count {
  font-size: 12px;
  color: var(--text-4);
  margin-left: 8px;
}

.goods-sales {
  font-size: 11px;
  color: var(--text-4);
  margin-left: auto;
}

.goods-load-more {
  text-align: center;
  padding: 20px 0 10px;
}
.goods-load-more .el-button {
  padding: 10px 36px;
  border-radius: 999px;
  font-size: 14px;
}

/* 无限滚动哨兵元素 */
.goods-sentinel {
  height: 1px;
  width: 100%;
}

/* 没有更多提示 */
.goods-no-more {
  text-align: center;
  padding: 30px 0 20px;
  color: #999;
  font-size: 13px;
}
.goods-no-more span {
  display: inline-block;
  padding: 0 16px;
  position: relative;
}
.goods-no-more span::before,
.goods-no-more span::after {
  content: '';
  display: block;
  position: absolute;
  top: 50%;
  width: 40px;
  height: 1px;
  background: #e0e0e0;
}
.goods-no-more span::before {
  right: 100%;
  margin-right: 8px;
}
.goods-no-more span::after {
  left: 100%;
  margin-left: 8px;
}

.goods-grid {
  padding: 20px;
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}
.goods-card {
  background-color: #fff;
  border: 1px solid #f0f0f0;
  border-radius: 12px;
  box-sizing: border-box;
  cursor: pointer;
  height: auto;
  position: relative;
  transition: all 0.3s;
  width: 100%;
  overflow: hidden;
  outline: none;
}
.goods-card:focus,
.goods-card:focus-visible,
.goods-card:active {
  outline: none;
}
.goods-card:hover {
  transform: translateY(-3px);
  border-color: transparent;
  box-shadow: 0 6px 16px rgba(200, 210, 230, 0.5);
}
.goods-card:hover .hover-border {
  border-color: #4e8cf2;
  border-width: 2px;
}
.hover-border {
  border: 1px solid transparent;
  border-radius: 12px;
  box-sizing: border-box;
  height: calc(100% + 8px);
  left: -4px;
  position: absolute;
  top: -4px;
  width: calc(100% + 8px);
  pointer-events: none;
  z-index: 2;
  transition: all 0.3s;
  outline: none;
}
.goods-card *:focus,
.goods-card *:focus-visible {
  outline: none;
}
.goods-cover {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  background: #fafafa;
  overflow: hidden;
  border-radius: 12px 12px 0 0;
}
.goods-cover :deep(.el-image) {
  width: 100%;
  height: 100%;
}
.goods-tag {
  position: absolute;
  top: 10px;
  left: 10px;
  padding: 2px 10px;
  background: var(--primary);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 4px;
}
.goods-tag-discount {
  left: auto;
  right: 10px;
  background: #f5222d;
}
.goods-body {
  padding: 10px 12px 14px;
}
.goods-name {
  font-size: 14px;
  color: #333;
  line-height: 1.5;
  height: 42px;
  overflow: hidden;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 8px;
}
.goods-promos {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 4px;
  margin-bottom: 8px;
}
.promo-tag {
  display: inline-block;
  padding: 1px 6px;
  background: var(--primary);
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 2px;
  line-height: 1.4;
}
.promo-tag-blue {
  background: #2b6cff;
}
.promo-text {
  font-size: 12px;
  color: #666;
  margin-right: 4px;
}
.goods-sales-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  font-size: 12px;
  color: #999;
}
.goods-sales-label {
  color: #ff4400;
  font-weight: 500;
}
.goods-quality {
  color: #999;
}
.goods-price-wrap {
  height: 40px;
}
.goods-price-row {
  display: flex;
  align-items: flex-end;
  height: 100%;
  line-height: 1;
}
.goods-price-sym {
  font-size: 14px;
  color: var(--primary);
  margin-right: 2px;
  font-weight: 600;
}
.goods-price {
  font-size: 26px;
  font-weight: 900;
  color: var(--primary);
  letter-spacing: -0.5px;
}

.goods-empty {
  padding: 60px 0;
}
.goods-loading {
  padding: 40px 30px;
  text-align: center;
  color: var(--text-3);
  font-size: 13px;
}

/* 加载旋转动画 */
.loading-spinner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  color: #999;
  font-size: 14px;
}
.loading-spinner .el-icon {
  animation: spin 1s linear infinite;
}
@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* ===============================================================
   页脚
   =============================================================== */
.footer {
  margin-top: 10px;
  background: #fff;
  border-top: 1px solid var(--border);
  padding: 20px 0;
  font-size: 12px;
  color: var(--text-3);
}
.footer-inner {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}
.footer-links {
  display: flex;
  align-items: center;
  gap: 6px;
}
.dot {
  color: #d9d9d9;
}

/* ===============================================================
   响应式
   =============================================================== */
@media (max-width: 1440px) {
  .main-card {
    grid-template-columns: 1fr 3fr 1fr;
  }
  .right-banner-container {
    width: 100%;
    height: 100%;
  }
  .right-banner-item {
    background-size: contain;
    background-position: 90% 50%;
    height: calc((100% - 24px) / 4);
  }
  .subcat-panel {
    left: calc(20% + 12px);
    right: calc(20% + 12px);
  }
}

@media (max-width: 1280px) {
  .container {
    padding-left: 12px;
    padding-right: 12px;
  }
  .main-card {
    grid-template-columns: 200px 1fr;
    gap: 12px;
    aspect-ratio: 16 / 4.5;
  }
  .right-banner-container {
    grid-column: 1 / -1;
    flex-direction: row;
    width: 100%;
    height: auto;
  }
  .right-banner-item {
    background-position: 85% 50%;
    background-size: contain;
    height: 80px;
    padding: 12px 14px;
    flex: 1;
  }
  .goods-grid {
    grid-template-columns: repeat(5, 1fr);
  }
  .subcat-panel {
    left: 212px;
    right: 16px;
    top: 16px;
    bottom: 16px;
  }
}

@media (max-width: 1024px) {
  .main-card {
    grid-template-columns: 1fr 3fr;
    aspect-ratio: 16 / 4.5;
  }
  .banner {
    height: 300px;
  }
  .goods-grid {
    grid-template-columns: repeat(4, 1fr);
  }
  .subcat-panel {
    left: calc(25% + 12px);
    right: 16px;
    top: 16px;
    bottom: 16px;
  }
  .banner-title {
    font-size: 26px;
  }
  .promo-box {
    padding: 28px 28px 32px;
    border-radius: 18px;
  }
  .promo-cards {
    gap: 14px;
  }
  .promo-col.flash .promo-cards {
    overflow-x: auto;
    flex-wrap: nowrap;
  }
  .promo-col.flash .promo-card {
    flex: 0 0 168px;
  }
  .promo-col.hot .promo-card {
    flex: 0 0 calc((100% - 28px) / 3);
  }
  .promo-head {
    margin-bottom: 22px;
  }
}

@media (max-width: 768px) {
  .main-card {
    grid-template-columns: 1fr;
    overflow: visible;
    aspect-ratio: auto;
  }
  .sidebar,
  .banner,
  .right-banner-container {
    grid-column: 1 / -1;
  }
  .sidebar {
    height: auto;
  }
  .right-banner-container {
    flex-direction: column;
    width: 100%;
    height: auto;
  }
  .right-banner-item {
    background-size: contain;
    padding: 14px;
    height: 76px;
  }
  .banner {
    height: 200px;
  }
  .banner-title {
    font-size: 22px;
  }
  .goods-grid {
    grid-template-columns: repeat(2, 1fr);
  }
  .subcat-panel {
    position: static;
    min-width: auto;
    border-radius: 0;
    box-shadow: none;
    background: #fafafa;
    padding: 12px 18px;
    border-top: 1px solid #eaeaea;
    animation: none;
  }
  .subcat-title {
    font-size: 13px;
    margin-bottom: 8px;
    padding-bottom: 6px;
  }
  .subcat-item {
    padding: 5px 10px;
    font-size: 12px;
  }
  .promo-box {
    padding: 20px 18px 24px;
    border-radius: 14px;
  }
  .promo-inner {
    flex-direction: column;
    gap: 24px;
  }
  .promo-col.hot {
    padding-left: 0;
    border-left: none;
  }
  .promo-cards {
    gap: 10px;
    flex-wrap: wrap;
    overflow-x: visible;
  }
  .promo-col.flash .promo-card,
  .promo-col.hot .promo-card {
    flex: 0 0 calc(50% - 5px);
    min-width: 0;
  }
  .promo-head {
    flex-wrap: wrap;
    gap: 10px;
    margin-bottom: 18px;
  }
  .promo-title-text {
    font-size: 18px;
  }
  .promo-more {
    margin-left: 0;
  }
}

/* ===============================================================
   限时秒杀 + 热门商品 — 合并展示
   =============================================================== */
.promo-section {
  background: #fff;
  padding: 16px 0;
}

.promo-box {
  background-color: #FFF9EF;
  border-radius: 24px;
  padding: 40px 44px 44px;
  position: relative;
  overflow: hidden;
  border: 1.5px solid #E8E2D6;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  background-image: url("data:image/svg+xml,%3Csvg width='100' height='100' xmlns='http://www.w3.org/2000/svg'%3E%3Ccircle cx='50' cy='50' r='16' fill='none' stroke='%23D4A853' stroke-width='0.6' opacity='0.12'/%3E%3Ccircle cx='50' cy='50' r='9' fill='none' stroke='%23D4A853' stroke-width='0.4' opacity='0.10'/%3E%3Crect x='49' y='46' width='2' height='8' rx='1' fill='%23D4A853' opacity='0.10'/%3E%3C/svg%3E");
  background-repeat: repeat;
  background-size: 100px 100px;
  background-position: 0 0;
}

/* 左右两栏 — 60/40 比例，无分隔线，宽间距自然分区 */
.promo-inner {
  display: flex;
  gap: 32px;
  position: relative;
  z-index: 1;
}

.promo-col {
  min-width: 0;
  position: relative;
}

/* 左侧秒杀主推区 — 4 张卡片，与热门卡片同尺寸 */
.promo-col.flash {
  flex: 1.35;
}

/* 右侧热门辅助区 — 3 张卡片 */
.promo-col.hot {
  flex: 1;
  padding-left: 32px;
  border-left: 1px solid #E8E2D6;
}

/* 标题栏 */
.promo-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
  min-height: 36px;
}

.promo-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.promo-title-icon {
  font-size: 24px;
  line-height: 1;
}

.promo-title-text {
  font-size: 20px;
  font-weight: 800;
  letter-spacing: 1px;
}

.promo-title-text.flash {
  color: #D4380D;
}

.promo-title-text.hot {
  color: #333;
}

.promo-title-sub {
  font-size: 12px;
  color: #BF8C4A;
  font-weight: 500;
  white-space: nowrap;
}

.promo-head-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.promo-more {
  font-size: 13px;
  color: #BF8C4A;
  cursor: pointer;
  font-weight: 500;
  transition: color 0.2s;
  white-space: nowrap;
}

.promo-more:hover {
  color: #D4380D;
}

/* 卡片行 */
.promo-cards {
  display: flex;
  gap: 20px;
}

/* 双列卡片 — 秒杀 4 张 / 热门 3 张，实际像素尺寸一致 */
.promo-col.flash .promo-card {
  flex: 0 0 calc((100% - 60px) / 4);
  min-width: 0;
}

.promo-col.hot .promo-card {
  flex: 0 0 calc((100% - 40px) / 3);
  min-width: 0;
}

.promo-card {
  background: #fff;
  border-radius: 14px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.25s, box-shadow 0.25s;
  border: 1px solid rgba(212, 168, 83, 0.15);
  box-shadow: 0 2px 8px rgba(212, 168, 83, 0.06);
}

.promo-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 8px 24px rgba(212, 168, 83, 0.18);
}

/* 卡片图片 */
.promo-img-wrap {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
}

.promo-img {
  width: 100%;
  height: 100%;
  transition: transform 0.3s;
}

.promo-card:hover .promo-img {
  transform: scale(1.06);
}

.promo-badge {
  position: absolute;
  top: 8px;
  left: 8px;
  background: linear-gradient(135deg, #D4380D, #FA8C16);
  color: #fff;
  padding: 2px 10px;
  border-radius: 4px;
  font-size: 11px;
  font-weight: 700;
}

.promo-rank {
  position: absolute;
  top: 8px;
  left: 8px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  font-weight: 700;
  color: #fff;
  background: #999;
}

.promo-rank.rank-1 {
  background: linear-gradient(135deg, #ff4d4f, #ff7a45);
  width: 28px;
  height: 28px;
  font-size: 14px;
}

.promo-rank.rank-2 {
  background: linear-gradient(135deg, #fa8c16, #ffa940);
}

.promo-rank.rank-3 {
  background: linear-gradient(135deg, #fadb14, #ffec3d);
  color: #333;
}

/* 卡片信息区 */
.promo-info {
  padding: 12px 12px 14px;
}

.promo-name {
  font-size: 13px;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  margin-bottom: 6px;
  font-weight: 500;
}

.promo-price-row {
  display: flex;
  align-items: baseline;
  gap: 6px;
  margin-bottom: 8px;
}

.promo-flash-price {
  color: #D4380D;
  font-size: 17px;
  font-weight: 700;
}

.promo-price-sym {
  font-size: 12px;
}

.promo-original-price {
  color: #bbb;
  font-size: 11px;
  text-decoration: line-through;
}

.promo-stock-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.promo-stock {
  font-size: 11px;
  color: #999;
}

.promo-btn {
  flex-shrink: 0;
  background: linear-gradient(135deg, #D4380D, #FA8C16);
  border: none;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 6px;
  color: #fff;
  cursor: pointer;
}

.promo-btn:hover {
  opacity: 0.9;
}

.promo-hot-price {
  font-size: 16px;
  font-weight: 700;
  color: var(--primary);
}

.promo-hot-sales {
  font-size: 11px;
  color: #999;
  margin-top: 2px;
}
</style>
