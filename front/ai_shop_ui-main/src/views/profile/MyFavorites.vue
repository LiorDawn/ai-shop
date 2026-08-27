<template>
  <div class="mf-page">
    <HeaderUser />

    <div class="mf-wrapper">
      <div class="mf-layout">
        <ProfileSidebar :user="user" :stats="stats" />

        <div class="mf-main">
          <div class="mf-card">
            <div class="mf-card-header">
              <div>
                <h3>我的收藏</h3>
                <span class="mf-card-desc">共 {{ total }} 件收藏商品</span>
              </div>
            </div>

            <div class="mf-body" v-loading="loading">
              <el-empty v-if="products.length === 0 && !loading" description="暂无收藏商品，去逛逛吧~" />

              <div v-else class="mf-grid">
                <div v-for="p in products" :key="p.id" class="mf-product-card" @click="goProduct(p.id)">
                  <div class="mf-img-box">
                    <img :src="p.image || 'https://picsum.photos/seed/default/300/300'" />
                    <div class="mf-del-btn" @click.stop="removeCollect(p.id)">
                      <el-icon><Delete /></el-icon>
                    </div>
                  </div>
                  <div class="mf-info">
                    <div class="mf-name">{{ p.name }}</div>
                    <div class="mf-price">¥{{ fmtPrice(p.price) }}</div>
                  </div>
                </div>
              </div>

              <div class="mf-pager" v-if="total > pageSize">
                <el-pagination
                  v-model:current-page="currentPage"
                  :page-size="pageSize"
                  :total="total"
                  layout="prev, pager, next"
                  @current-change="fetchCollects"
                />
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import ProfileSidebar from '@/views/profile/ProfileSidebar.vue'
import { getProfile, getProfileStats, listCollectProducts, type UserProfileDTO, type ProfileStats } from '@/api/profile'
import { removeCollect } from '@/api/collect'

const router = useRouter()
const loading = ref(false)
const user = ref<UserProfileDTO | null>(null)
const stats = ref<ProfileStats | null>(null)
const products = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)

function fmtPrice(p: number | undefined | null): string {
  if (p === undefined || p === null || isNaN(p as number)) return '0.00'
  return Number(p).toFixed(2)
}

async function fetchData() {
  loading.value = true
  try {
    const [profileRes, statsRes, collectRes] = await Promise.all([
      getProfile(),
      getProfileStats(),
      listCollectProducts(currentPage.value, pageSize.value),
    ])
    user.value = profileRes.data
    stats.value = statsRes.data
    const data = collectRes.data
    products.value = data.records || []
    total.value = data.total || 0
  } catch {
    products.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function fetchCollects() {
  loading.value = true
  try {
    const res = await listCollectProducts(currentPage.value, pageSize.value)
    const data = res.data
    products.value = data.records || []
    total.value = data.total || 0
  } catch {
    products.value = []
    total.value = 0
  } finally {
    loading.value = false
  }
}

async function removeCollect(productId: number) {
  try {
    await ElMessageBox.confirm('确定取消收藏该商品？', '提示', { type: 'warning' })
    await removeCollect(productId)
    ElMessage.success('已取消收藏')
    fetchCollects()
  } catch { /* cancel */ }
}

function goProduct(id: number) {
  router.push(`/product/${id}`)
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.mf-page { min-height: 100vh; background: #f5f5f5; }
.mf-wrapper { max-width: 1600px; margin: 0 auto; padding: 16px; }
.mf-layout { display: flex; gap: 16px; align-items: flex-start; }
.mf-main { flex: 1; min-width: 0; }
.mf-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.mf-card-header {
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}
.mf-card-header h3 { margin: 0 0 4px; font-size: 18px; font-weight: 600; color: #333; }
.mf-card-desc { font-size: 13px; color: #999; }
.mf-body { padding: 20px 24px; min-height: 200px; }
.mf-grid {
  display: grid;
  grid-template-columns: repeat(5, 1fr);
  gap: 16px;
}
@media (max-width: 1200px) {
  .mf-grid { grid-template-columns: repeat(4, 1fr); }
}
@media (max-width: 900px) {
  .mf-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 700px) {
  .mf-grid { grid-template-columns: repeat(2, 1fr); }
}
.mf-product-card {
  border: 1px solid #f0f0f0;
  border-radius: 8px;
  overflow: hidden;
  cursor: pointer;
  transition: box-shadow 0.2s;
}
.mf-product-card:hover {
  box-shadow: 0 2px 12px rgba(0,0,0,0.08);
}
.mf-img-box {
  position: relative;
  width: 100%;
  aspect-ratio: 1;
  background: #f8f8f8;
}
.mf-img-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.mf-del-btn {
  position: absolute;
  top: 6px; right: 6px;
  width: 30px; height: 30px;
  background: rgba(0,0,0,0.4);
  color: #fff;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  opacity: 0;
  transition: opacity 0.2s;
}
.mf-product-card:hover .mf-del-btn { opacity: 1; }
.mf-info { padding: 10px 12px 14px; }
.mf-name {
  font-size: 14px; color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 6px;
  min-height: 39px;
}
.mf-price { font-size: 16px; font-weight: 700; color: #e4393c; }
.mf-pager { margin-top: 20px; display: flex; justify-content: center; }
</style>