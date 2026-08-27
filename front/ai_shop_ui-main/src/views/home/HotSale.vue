<template>
  <div class="hot-page">
    <HeaderUser />

    <SearchBar
      v-model:keyword="keyword"
      :hot-words="hotWords"
      logo-text="商品热卖"
      logo-sub="HOT SALE"
      logo-short="热"
      @search="onSearch"
      @logo-click="goHome"
    />

    <!-- ========== 主体 ========== -->
    <div class="hot-main">
      <div class="container">
        <!-- 京东风格筛选栏 -->
        <div class="jd-filter">
          <!-- 第一行：筛选标题 + 展开按钮 -->
          <div class="jd-filter-top">
            <div class="jd-filter-title">
              <span class="jd-filter-bar"></span>
              <span class="jd-filter-text">{{ activeCategoryName || '热卖商品' }}</span>
              <span class="jd-filter-sub" v-if="!activeCategoryName">热门推荐 · 限时特惠</span>
              <span class="jd-filter-sub" v-else>分类精选 · 好物推荐</span>
            </div>
            <div class="jd-filter-actions">
              <el-button
                text
                class="jd-expand-btn"
                @click="filterExpand = !filterExpand"
              >
                <template #icon>
                  <el-icon><component :is="filterExpand ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
                </template>
                {{ filterExpand ? '收起筛选' : '展开筛选' }}
              </el-button>
            </div>
          </div>

          <!-- 第二行：排序 tabs -->
          <div class="jd-filter-row">
            <span class="jd-filter-label">排序：</span>
            <div class="jd-sort">
              <span
                class="jd-sort-tab"
                :class="{ active: sortBy === 'default' }"
                @click="setSort('default')"
              >综合</span>
              <span
                class="jd-sort-tab"
                :class="{ active: sortBy === 'sales' }"
                @click="setSort('sales')"
              >销量</span>
              <span
                class="jd-sort-tab jd-sort-price"
                :class="{ active: sortBy === 'price_asc' || sortBy === 'price_desc' }"
                @click="togglePriceSort"
              >
                价格
                <span class="jd-price-arrows">
                  <span class="jd-arrow-up" :class="{ on: sortBy === 'price_asc' }">&#9650;</span>
                  <span class="jd-arrow-down" :class="{ on: sortBy === 'price_desc' }">&#9660;</span>
                </span>
              </span>
            </div>
          </div>

          <!-- 展开区域：分类 + 价格区间 -->
          <Transition name="jd-expand">
            <div v-if="filterExpand" class="jd-expand-panel">
              <!-- 分类：层级结构，先一级分类下面再二级分类 -->
              <div class="jd-expand-block">
                <span class="jd-filter-label">分类：</span>
                <div class="jd-cat-tree">
                  <div class="jd-cat-tree-item">
                    <span
                      class="jd-chip"
                      :class="{ active: activeCategoryId === null && activeParentCategoryId === null }"
                      @click="onSelectAllCategories"
                    >全部分类</span>
                  </div>
                  <div v-for="top in topCategories" :key="top.id" class="jd-cat-tree-item">
                    <span
                      class="jd-cat-parent"
                      :class="{ active: activeParentCategoryId === top.id }"
                      @click="onSelectParentCategory(top)"
                    >{{ top.name }}</span>
                    <span
                      v-for="sub in getSubCategories(top.id)"
                      :key="sub.id"
                      class="jd-chip"
                      :class="{ active: activeCategoryId === sub.id }"
                      @click="onSelectSubCategory(sub)"
                    >{{ sub.name }}</span>
                  </div>
                </div>
              </div>
              <!-- 价格区间 -->
              <div class="jd-expand-block">
                <span class="jd-filter-label">价格：</span>
                <div class="jd-price-range">
                  <input
                    v-model.number="minPrice"
                    type="number"
                    class="jd-price-input"
                    placeholder="最低价"
                    @input="onPriceChange"
                  />
                  <span class="jd-price-sep">-</span>
                  <input
                    v-model.number="maxPrice"
                    type="number"
                    class="jd-price-input"
                    placeholder="最高价"
                    @input="onPriceChange"
                  />
                  <el-button size="small" type="primary" plain @click="onPriceConfirm">确定</el-button>
                </div>
              </div>
              <!-- 重置 -->
              <div class="jd-expand-block jd-expand-reset">
                <el-button size="small" @click="resetFilter">重置筛选</el-button>
              </div>
            </div>
          </Transition>
        </div>

        <!-- 商品列表 -->
        <el-empty
          v-if="!loading && filteredProducts.length === 0"
          description="暂无匹配商品，请调整筛选条件"
          class="jd-empty"
        />

        <div v-else class="jd-grid">
          <div
            v-for="p in filteredProducts"
            :key="p.id"
            class="jd-card"
            @click="goToProduct(p)"
          >
            <div class="jd-cover">
              <img :src="p.image || defaultImg" :alt="p.name" loading="lazy" />
            </div>
            <div class="jd-info">
              <div class="jd-name">{{ p.name }}</div>
              <div class="jd-shop">{{ p.shopName }}</div>
              <div class="jd-price-row">
                <span class="jd-price-sym">¥</span>
                <span class="jd-price">{{ Number(p.price).toFixed(2) }}</span>
              </div>
              <div class="jd-actions">
                <el-button
                  type="primary"
                  size="small"
                  class="jd-cart-btn"
                  @click.stop="onAddCart(p)"
                >加入购物车</el-button>
              </div>
            </div>
          </div>
        </div>

        <div v-if="loading" class="jd-loading">
          <el-icon class="is-loading" :size="22"><Loading /></el-icon>
          <span style="margin-left: 8px">正在加载商品...</span>
        </div>

        <div v-if="!loading && totalPages > 1" class="jd-pager">
          <el-pagination
            v-model:current-page="currentPage"
            :page-size="pageSize"
            :total="total"
            layout="prev, pager, next, total"
            background
            @current-change="onPageChange"
          />
        </div>
      </div>
    </div>

    <!-- ========== 页脚 ========== -->
    <div class="footer">
      <div class="container footer-inner">
        <div>© 2026 AI 智能商城系统 · All Rights Reserved</div>
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
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, onBeforeUnmount } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  ArrowDown,
  ArrowUp,
  Loading,
} from '@element-plus/icons-vue'
import HeaderUser from '../../components/layout/HeaderUser.vue'
import SearchBar from '../../components/layout/SearchBar.vue'
import { getProductsPage, type ProductDTO } from '../../api/product'
import { listCategories, type Category } from '../../api/category'
import { addToCart } from '../../api/cart'

const router = useRouter()
const route = useRoute()
const defaultImg = 'https://picsum.photos/seed/default/300/300'

/* ===== 搜索与路由参数 ===== */
const keyword = ref('')
const activeCategoryId = ref<number | null>(null)
const activeParentCategoryId = ref<number | null>(null)
const minPrice = ref<number | null>(null)
const maxPrice = ref<number | null>(null)
const sortBy = ref<string>('default')
const currentPage = ref(1)
const pageSize = 20
const filterExpand = ref(false)

/* ===== 分类 ===== */
const allCategories = ref<Category[]>([])
const topCategories = computed(() => allCategories.value.filter(c => c.parentId === 0))

const hotWords = computed(() => {
  const list: string[] = []
  const pool = topCategories.value
  pool.slice(0, 8).forEach(c => list.push(c.name))
  return list
})

const activeCategoryName = computed(() => {
  if (activeParentCategoryId.value !== null) {
    const c = allCategories.value.find(x => x.id === activeParentCategoryId.value)
    return c ? c.name : ''
  }
  if (activeCategoryId.value === null) return ''
  const c = allCategories.value.find(x => x.id === activeCategoryId.value)
  return c ? c.name : ''
})

function getSubCategories(parentId: number) {
  return allCategories.value.filter(c => c.parentId === parentId)
}

/* ===== 筛选操作 ===== */
function onSelectAllCategories() {
  activeCategoryId.value = null
  activeParentCategoryId.value = null
  currentPage.value = 1
  fetchProducts()
}

function onSelectParentCategory(top: Category) {
  activeParentCategoryId.value = top.id
  activeCategoryId.value = null
  currentPage.value = 1
  fetchProducts()
}

function onSelectSubCategory(sub: Category) {
  activeCategoryId.value = sub.id
  activeParentCategoryId.value = null
  currentPage.value = 1
  fetchProducts()
}

function setSort(s: string) {
  sortBy.value = s
  currentPage.value = 1
  fetchProducts()
}

function togglePriceSort() {
  if (sortBy.value === 'price_asc') {
    sortBy.value = 'price_desc'
  } else {
    sortBy.value = 'price_asc'
  }
  currentPage.value = 1
  fetchProducts()
}

let priceTimer: any = null
function onPriceChange() {
  if (priceTimer) clearTimeout(priceTimer)
}

function onPriceConfirm() {
  currentPage.value = 1
  fetchProducts()
}

function resetFilter() {
  activeCategoryId.value = null
  activeParentCategoryId.value = null
  minPrice.value = null
  maxPrice.value = null
  sortBy.value = 'default'
  keyword.value = ''
  currentPage.value = 1
  fetchProducts()
}

/* ===== 商品列表 ===== */
const productList = ref<ProductDTO[]>([])
const total = ref(0)
const loading = ref(false)
let requestId = 0

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))

const filteredProducts = computed(() => {
  let list = [...productList.value]

  if (keyword.value && keyword.value.trim() !== '') {
    const kw = keyword.value.trim().toLowerCase()
    list = list.filter(p => p.name.toLowerCase().includes(kw))
  }

  if (minPrice.value !== null && minPrice.value !== undefined && !isNaN(minPrice.value)) {
    list = list.filter(p => Number(p.price) >= minPrice.value!)
  }
  if (maxPrice.value !== null && maxPrice.value !== undefined && !isNaN(maxPrice.value)) {
    list = list.filter(p => Number(p.price) <= maxPrice.value!)
  }

  if (sortBy.value === 'price_asc') {
    list.sort((a, b) => Number(a.price) - Number(b.price))
  } else if (sortBy.value === 'price_desc') {
    list.sort((a, b) => Number(b.price) - Number(a.price))
  } else if (sortBy.value === 'sales') {
    list.sort((a, b) => (b.stock || 0) - (a.stock || 0))
  }

  return list
})

function onSearch() {
  currentPage.value = 1
  fetchProducts()
}

function goHome() {
  router.push('/home')
}

function openPlatformChat() {
  router.push('/platform-chat')
}

function goToProduct(p: ProductDTO) {
  router.push(`/product/${p.id}`)
}

async function onAddCart(p: ProductDTO) {
  try {
    await addToCart({ productId: p.id, num: 1 })
    ElMessage.success('已加入购物车')
  } catch (e: any) {
    ElMessage.error(e?.message || '加入购物车失败')
  }
}

function onPageChange(page: number) {
  currentPage.value = page
  fetchProducts()
  window.scrollTo({ top: 0, behavior: 'smooth' })
}

async function fetchProducts() {
  loading.value = true
  const myReq = ++requestId
  try {
    const params: any = {
      current: currentPage.value,
      size: pageSize,
      status: 1,
    }
    if (activeCategoryId.value !== null) params.categoryId = activeCategoryId.value
    else if (activeParentCategoryId.value !== null) params.parentCategoryId = activeParentCategoryId.value
    if (keyword.value && keyword.value.trim() !== '') params.name = keyword.value.trim()

    const res: any = await getProductsPage(params)
    if (myReq !== requestId) return
    const data = res?.data
    const list = Array.isArray(data?.records) ? data.records : Array.isArray(data?.list) ? data.list : Array.isArray(data?.rows) ? data.rows : Array.isArray(data) ? data : []
    productList.value = list
    total.value = data?.total != null ? data.total : list.length
  } catch {
    productList.value = []
    total.value = 0
  } finally {
    if (myReq === requestId) loading.value = false
  }
}

async function loadCategories() {
  try {
    const res: any = await listCategories()
    if (res?.code === 200 || res?.code === 0) {
      allCategories.value = res.data || []
    } else if (Array.isArray(res)) {
      allCategories.value = res
    } else {
      allCategories.value = []
    }
  } catch {
    allCategories.value = []
  }
}

/* ===== 初始化 ===== */
onMounted(async () => {
  await loadCategories()

  const qCatId = route.query.category
  const qKw = route.query.keyword
  if (qCatId) {
    const id = Number(qCatId)
    if (!isNaN(id) && allCategories.value.some(c => c.id === id)) {
      const matchedCat = allCategories.value.find(c => c.id === id)
      if (matchedCat) {
        if (matchedCat.parentId === 0 || matchedCat.parentId == null) {
          // 一级分类：用 parentCategoryId 过滤，查询该一级分类下所有子分类商品
          activeParentCategoryId.value = id
          activeCategoryId.value = null
        } else {
          // 二级分类：用 categoryId 过滤
          activeCategoryId.value = id
          activeParentCategoryId.value = null
        }
      }
    }
  }
  if (typeof qKw === 'string' && qKw) keyword.value = qKw

  await fetchProducts()
  if (productList.value.length === 0 && (activeCategoryId.value !== null || activeParentCategoryId.value !== null)) {
    activeCategoryId.value = null
    activeParentCategoryId.value = null
    fetchProducts()
  }
})

watch(
  () => route.query.category,
  (newCat) => {
    if (newCat) {
      const id = Number(newCat)
      if (!isNaN(id) && allCategories.value.some(c => c.id === id)) {
        const matchedCat = allCategories.value.find(c => c.id === id)
        if (matchedCat) {
          if (matchedCat.parentId === 0 || matchedCat.parentId == null) {
            activeParentCategoryId.value = id
            activeCategoryId.value = null
          } else {
            activeCategoryId.value = id
            activeParentCategoryId.value = null
          }
        }
        currentPage.value = 1
        fetchProducts().then(() => {
          if (productList.value.length === 0) {
            activeCategoryId.value = null
            activeParentCategoryId.value = null
            fetchProducts()
          }
        })
      }
    }
  }
)

onBeforeUnmount(() => {
  if (priceTimer) clearTimeout(priceTimer)
})
</script>

<style scoped>
.hot-page {
  min-height: 100vh;
  background-color: #f5f5f5;
}

/* ===== 主体 ===== */
.hot-main {
  padding: 16px 0 32px;
}

/* ===== 京东风格筛选栏 ===== */
.jd-filter {
  background: #fff;
  border-radius: 12px;
  padding: 14px 20px;
  margin-bottom: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.jd-filter-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.jd-filter-title {
  display: flex;
  align-items: center;
  gap: 10px;
}
.jd-filter-bar {
  width: 4px;
  height: 18px;
  background: linear-gradient(180deg, #ff0f23 0%, #ff6a00 100%);
  border-radius: 2px;
}
.jd-filter-text {
  font-size: 17px;
  font-weight: 700;
  color: #333;
}
.jd-filter-sub {
  font-size: 13px;
  color: #999;
  margin-left: 4px;
}
.jd-expand-btn {
  font-size: 13px;
  color: #666;
}
.jd-expand-btn:hover {
  color: #ff0f23;
}

.jd-filter-row {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
  border-top: 1px solid #f0f0f0;
}
.jd-filter-label {
  font-size: 13px;
  color: #888;
  white-space: nowrap;
}
.jd-sort {
  display: flex;
  align-items: center;
  gap: 4px;
}
.jd-sort-tab {
  padding: 6px 14px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  border-radius: 6px;
  transition: all 0.15s;
  user-select: none;
}
.jd-sort-tab:hover {
  color: #ff0f23;
  background: #fff1eb;
}
.jd-sort-tab.active {
  color: #fff;
  background: #ff0f23;
  font-weight: 600;
}
.jd-sort-price {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}
.jd-price-arrows {
  display: inline-flex;
  flex-direction: column;
  line-height: 1;
  font-size: 8px;
  gap: 1px;
}
.jd-price-arrows span {
  color: #bbb;
  transition: color 0.15s;
}
.jd-price-arrows span.on {
  color: #fff;
}

/* 展开面板 */
.jd-expand-panel {
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.jd-expand-block {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}
.jd-expand-reset {
  justify-content: flex-end;
  border-top: 1px solid #f0f0f0;
  padding-top: 12px;
}
.jd-cat-chips,
.jd-cat-tree {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  flex: 1;
}
.jd-cat-tree {
  flex-direction: column;
  gap: 8px;
}
.jd-cat-tree-item {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 6px;
}
.jd-cat-parent {
  font-size: 13px;
  font-weight: 700;
  color: #333;
  min-width: 50px;
  padding: 5px 12px;
  cursor: pointer;
  border-radius: 14px;
  transition: all 0.15s;
  user-select: none;
}
.jd-cat-parent:hover {
  background: #fff1eb;
  color: #ff0f23;
}
.jd-cat-parent.active {
  background: #ff0f23;
  color: #fff;
}
.jd-chip {
  padding: 5px 14px;
  font-size: 12px;
  color: #666;
  background: #f5f5f5;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.15s;
  user-select: none;
}
.jd-chip:hover {
  background: #fff1eb;
  color: #ff0f23;
}
.jd-chip.active {
  background: #ff0f23;
  color: #fff;
  font-weight: 600;
}
.jd-price-range {
  display: flex;
  align-items: center;
  gap: 8px;
}
.jd-price-input {
  width: 100px;
  height: 30px;
  padding: 0 10px;
  border: 1px solid #ddd;
  border-radius: 6px;
  font-size: 13px;
  outline: none;
  transition: border-color 0.15s;
}
.jd-price-input:focus {
  border-color: #ff0f23;
}
.jd-price-sep {
  color: #999;
  font-size: 12px;
}

/* 展开动画 */
.jd-expand-enter-active,
.jd-expand-leave-active {
  transition: all 0.25s ease;
}
.jd-expand-enter-from,
.jd-expand-leave-to {
  opacity: 0;
  max-height: 0;
  padding-top: 0;
  overflow: hidden;
}

/* ===== 商品网格 ===== */
.jd-grid {
  display: grid;
  grid-template-columns: repeat(6, minmax(0, 1fr));
  gap: 8px;
}
.jd-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  position: relative;
  transition: transform 0.2s, box-shadow 0.2s;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
}
.jd-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
}
.jd-cover {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #fafafa;
  overflow: hidden;
}
.jd-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.jd-card:hover .jd-cover img {
  transform: scale(1.06);
}
.jd-info {
  padding: 6px;
  display: flex;
  flex-direction: column;
  gap: 3px;
}
.jd-name {
  font-size: 12px;
  color: #333;
  line-height: 1.35;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  min-height: 32px;
}
.jd-shop {
  font-size: 11px;
  color: #999;
}
.jd-price-row {
  display: flex;
  align-items: baseline;
  color: #ff0f23;
}
.jd-price-sym {
  font-size: 11px;
  font-weight: 700;
}
.jd-price {
  font-size: 15px;
  font-weight: 900;
  margin-left: 1px;
  letter-spacing: -0.5px;
}
.jd-actions {
  margin-top: 2px;
}
.jd-cart-btn {
  width: 100%;
  font-size: 11px;
  padding: 4px 0;
  height: auto;
}
.jd-cart-btn :deep(.el-button__inner) {
  padding: 0;
}

/* Loading / Empty / Pager */
.jd-empty {
  background: #fff;
  border-radius: 12px;
  padding: 60px 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.jd-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 30px;
  color: #999;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}
.jd-pager {
  display: flex;
  justify-content: center;
  padding: 16px;
  background: #fff;
  border-radius: 12px;
  margin-top: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

/* ===== 页脚 ===== */
.footer {
  background: #fff;
  border-top: 1px solid #eee;
  padding: 20px 0;
  margin-top: 20px;
}
.footer-inner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 12px;
  color: #999;
}
.footer-links {
  display: flex;
  align-items: center;
  gap: 8px;
}
.footer-links span {
  cursor: pointer;
}
.footer-links .dot {
  cursor: default;
  color: #ddd;
}

/* ===== 响应式 ===== */
@media (max-width: 1280px) {
  .jd-grid {
    grid-template-columns: repeat(5, minmax(0, 1fr));
  }
}
@media (max-width: 1024px) {
  .jd-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }
}
@media (max-width: 768px) {
  .jd-filter-top {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .jd-grid {
    grid-template-columns: repeat(3, minmax(0, 1fr));
    gap: 6px;
  }
}
@media (max-width: 540px) {
  .jd-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 6px;
  }
}
</style>