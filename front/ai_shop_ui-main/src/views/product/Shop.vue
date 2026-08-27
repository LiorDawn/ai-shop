<template>
  <div class="sp-page">
    <HeaderUser />

    <!-- 店铺头部 -->
    <div class="sp-header" v-loading="loading">
      <div class="sp-header-inner">
        <div class="sp-header-top">
          <a class="sp-back" @click.prevent="$router.back()">
            <el-icon><ArrowLeft /></el-icon>
            <span>返回</span>
          </a>
          <div class="sp-header-info">
            <img :src="shopLogo" class="sp-logo" />
            <div class="sp-header-text">
              <h1 class="sp-shop-name">{{ shopName }}</h1>
              <div class="sp-shop-tags">
                <el-tag :type="shopStatus === 1 ? 'success' : 'info'" size="small" effect="dark">
                  {{ shopStatus === 1 ? '营业中' : '休息中' }}
                </el-tag>
                <span class="sp-stat">商品 {{ productCount }} 件</span>
                <span class="sp-stat">粉丝 {{ followerCount }} 人</span>
              </div>
            </div>
          </div>
          <div class="sp-header-actions">
            <el-button
              :type="followed ? 'info' : 'danger'"
              :plain="followed"
              size="default"
              @click="toggleFollow"
              :loading="followLoading"
            >
              <el-icon><StarFilled v-if="followed" /><Plus v-else /></el-icon>
              {{ followed ? '已关注' : '关注店铺' }}
            </el-button>
          </div>
        </div>
        <div class="sp-header-desc" v-if="shopIntro">
          <el-icon><InfoFilled /></el-icon>
          <span>{{ shopIntro }}</span>
        </div>
      </div>
    </div>

    <!-- 搜索 + 分类 -->
    <div class="sp-toolbar">
      <div class="sp-toolbar-inner">
        <div class="sp-search">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索本店商品"
            clearable
            size="default"
            @keyup.enter="onSearch"
            @clear="onSearch"
          >
            <template #prefix><el-icon><Search /></el-icon></template>
            <template #suffix>
              <el-button
                type="primary"
                size="small"
                @click="onSearch"
                style="margin-right: -8px; border-radius: 0 4px 4px 0;"
              >搜索</el-button>
            </template>
          </el-input>
        </div>
        <div class="sp-sort">
          <span
            v-for="s in sortOptions"
            :key="s.value"
            class="sp-sort-item"
            :class="{ active: currentSort === s.value }"
            @click="onSortChange(s.value)"
          >{{ s.label }}</span>
        </div>
      </div>
    </div>

    <!-- 分类 Tab -->
    <div class="sp-cat-bar">
      <div class="sp-cat-inner">
        <span
          class="sp-cat-item"
          :class="{ active: activeCatId === null }"
          @click="onCatChange(null)"
        >全部商品</span>
        <span
          v-for="c in categories"
          :key="c.id"
          class="sp-cat-item"
          :class="{ active: activeCatId === c.id }"
          @click="onCatChange(c.id)"
        >{{ c.name }}</span>
      </div>
    </div>

    <!-- 商品列表 -->
    <div class="sp-products" v-loading="productLoading">
      <div v-if="products.length === 0 && !productLoading" class="sp-empty">
        <el-empty :description="searchKeyword ? '未找到相关商品' : '该店铺暂无商品'" />
      </div>

      <div v-else class="sp-product-grid">
        <div
          v-for="p in products"
          :key="p.id"
          class="sp-product-card"
          @click="goProduct(p.id)"
        >
          <div class="sp-product-img-box">
            <img :src="p.image || 'https://picsum.photos/seed/default/300/300'" :alt="p.name" />
          </div>
          <div class="sp-product-info">
            <div class="sp-product-name">{{ p.name }}</div>
            <div class="sp-product-price">
              <span class="sp-price">¥{{ fmtPrice(p.price) }}</span>
            </div>
            <div class="sp-product-sales" v-if="p.sales">已售 {{ p.sales }}</div>
          </div>
        </div>
      </div>

      <el-pagination
        v-if="productTotal > pageSize"
        v-model:current-page="currentPage"
        :page-size="pageSize"
        :total="productTotal"
        layout="prev, pager, next"
        @current-change="fetchProducts"
        class="sp-pager"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowLeft, StarFilled, Plus, Search, InfoFilled } from '@element-plus/icons-vue'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import { getShopDetail, followShop as followShopApi, unfollowShop, getShopCategories, getShopProducts, type ShopDetailDTO, type ShopCategoryDTO } from '@/api/shop'
import type { ProductDTO } from '@/api/product'
import { useAuthStore } from '@/stores/auth'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const shopId = ref(0)
const shopName = ref('')
const shopLogo = ref('')
const shopIntro = ref('')
const shopStatus = ref(1)
const productCount = ref(0)
const followerCount = ref(0)
const followed = ref(false)
const followLoading = ref(false)

// 分类
const categories = ref<ShopCategoryDTO[]>([])
const activeCatId = ref<number | null>(null)

// 搜索排序
const searchKeyword = ref('')
const sortOptions = [
  { label: '综合', value: 0 },
  { label: '价格升序', value: 1 },
  { label: '价格降序', value: 2 },
  { label: '新品', value: 3 },
]
const currentSort = ref(0)

// 商品
const productLoading = ref(false)
const products = ref<ProductDTO[]>([])
const productTotal = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

function fmtPrice(p: number | undefined | null): string {
  if (p === undefined || p === null || isNaN(p as number)) return '0.00'
  return Number(p).toFixed(2)
}

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

async function fetchShopDetail() {
  const id = Number(route.params.id)
  if (!id) {
    ElMessage.error('店铺不存在')
    router.replace('/home')
    return
  }
  shopId.value = id
  loading.value = true
  try {
    const res = await getShopDetail(id)
    const data: ShopDetailDTO = res.data
    shopName.value = data.shop.shopName
    shopLogo.value = data.shop.shopLogo || ''
    shopIntro.value = data.shop.intro || ''
    shopStatus.value = data.shop.status
    productCount.value = data.productCount
    followerCount.value = data.followerCount
    followed.value = data.followed
  } catch {
    ElMessage.error('店铺不存在')
    router.replace('/home')
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    const res = await getShopCategories(shopId.value)
    categories.value = res.data || []
  } catch {
    categories.value = []
  }
}

async function fetchProducts() {
  productLoading.value = true
  try {
    const res = await getShopProducts(shopId.value, {
      current: currentPage.value,
      size: pageSize.value,
      keyword: searchKeyword.value || undefined,
      categoryId: activeCatId.value || undefined,
      sort: currentSort.value,
    })
    const data = res.data
    products.value = data.records || []
    productTotal.value = data.total || 0
  } catch {
    products.value = []
    productTotal.value = 0
  } finally {
    productLoading.value = false
  }
}

async function toggleFollow() {
  if (!checkLogin()) return
  followLoading.value = true
  try {
    if (followed.value) {
      await unfollowShop(shopId.value)
      followed.value = false
      followerCount.value = Math.max(0, followerCount.value - 1)
      ElMessage.success('已取消关注')
    } else {
      await followShopApi(shopId.value)
      followed.value = true
      followerCount.value++
      ElMessage.success('关注成功')
    }
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    followLoading.value = false
  }
}

function onSearch() {
  currentPage.value = 1
  fetchProducts()
}

function onSortChange(sort: number) {
  currentSort.value = sort
  currentPage.value = 1
  fetchProducts()
}

function onCatChange(catId: number | null) {
  activeCatId.value = catId
  currentPage.value = 1
  fetchProducts()
}

function goProduct(id: number) {
  router.push(`/product/${id}`)
}

onMounted(async () => {
  await fetchShopDetail()
  if (shopId.value) {
    await Promise.all([fetchCategories(), fetchProducts()])
  }
})
</script>

<style scoped>
.sp-page {
  min-height: 100vh;
  background: #f5f5f5;
}

/* 店铺头部 */
.sp-header {
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  color: #fff;
}
.sp-header-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 24px;
}
.sp-header-top {
  display: flex;
  align-items: center;
  gap: 20px;
}
.sp-back {
  display: flex;
  align-items: center;
  gap: 4px;
  color: rgba(255,255,255,0.7);
  cursor: pointer;
  text-decoration: none;
  font-size: 14px;
  flex-shrink: 0;
  transition: color 0.2s;
}
.sp-back:hover {
  color: #fff;
}
.sp-header-info {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}
.sp-logo {
  width: 64px;
  height: 64px;
  border-radius: 12px;
  object-fit: cover;
  border: 2px solid rgba(255,255,255,0.3);
  flex-shrink: 0;
  background: #fff;
}
.sp-header-text {
  min-width: 0;
}
.sp-shop-name {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 6px;
  color: #fff;
}
.sp-shop-tags {
  display: flex;
  align-items: center;
  gap: 12px;
  font-size: 13px;
  color: rgba(255,255,255,0.7);
}
.sp-stat {
  white-space: nowrap;
}
.sp-header-actions {
  flex-shrink: 0;
}
.sp-header-desc {
  margin-top: 12px;
  padding: 10px 14px;
  background: rgba(255,255,255,0.1);
  border-radius: 6px;
  font-size: 13px;
  color: rgba(255,255,255,0.8);
  display: flex;
  align-items: flex-start;
  gap: 6px;
  line-height: 1.5;
}

/* 搜索 + 排序 */
.sp-toolbar {
  background: #fff;
  border-bottom: 1px solid #eee;
  position: sticky;
  top: 0;
  z-index: 10;
}
.sp-toolbar-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
}
.sp-search {
  width: 360px;
  flex-shrink: 0;
}
.sp-sort {
  display: flex;
  gap: 0;
}
.sp-sort-item {
  padding: 8px 18px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  border: 1px solid #ddd;
  border-right: none;
  transition: all 0.2s;
  user-select: none;
}
.sp-sort-item:first-child {
  border-radius: 4px 0 0 4px;
}
.sp-sort-item:last-child {
  border-right: 1px solid #ddd;
  border-radius: 0 4px 4px 0;
}
.sp-sort-item:hover {
  color: #e4393c;
}
.sp-sort-item.active {
  background: #e4393c;
  color: #fff;
  border-color: #e4393c;
}

/* 分类 Tab */
.sp-cat-bar {
  background: #fff;
  border-bottom: 1px solid #eee;
}
.sp-cat-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 24px;
  display: flex;
  gap: 0;
  overflow-x: auto;
}
.sp-cat-inner::-webkit-scrollbar {
  display: none;
}
.sp-cat-item {
  padding: 12px 20px;
  font-size: 14px;
  color: #666;
  cursor: pointer;
  white-space: nowrap;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
  user-select: none;
}
.sp-cat-item:hover {
  color: #e4393c;
}
.sp-cat-item.active {
  color: #e4393c;
  font-weight: 600;
  border-bottom-color: #e4393c;
}

/* 商品列表 */
.sp-products {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px 24px 40px;
  min-height: 400px;
}
.sp-empty {
  padding: 60px 0;
  background: #fff;
  border-radius: 8px;
}
.sp-product-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 12px;
}
.sp-product-card {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.2s;
}
.sp-product-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
}
.sp-product-img-box {
  width: 100%;
  aspect-ratio: 1;
  overflow: hidden;
  background: #f8f8f8;
}
.sp-product-img-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  transition: transform 0.3s;
}
.sp-product-card:hover .sp-product-img-box img {
  transform: scale(1.05);
}
.sp-product-info {
  padding: 10px 12px 14px;
}
.sp-product-name {
  font-size: 14px;
  color: #333;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 8px;
  min-height: 39px;
}
.sp-product-price {
  margin-bottom: 4px;
}
.sp-price {
  font-size: 18px;
  font-weight: 700;
  color: #e4393c;
}
.sp-product-sales {
  font-size: 12px;
  color: #999;
}
.sp-pager {
  margin-top: 20px;
  justify-content: center;
}
</style>