<template>
  <div class="mf-page">
    <HeaderUser />

    <div class="mf-wrapper">
      <div class="mf-layout">
        <ProfileSidebar :user="user" :stats="stats" />

        <div class="mf-main">
          <div class="mf-card">
            <div class="mf-card-header">
              <h3>我的关注店铺</h3>
            </div>
            <div class="mf-body" v-loading="loading">
              <el-empty v-if="shops.length === 0 && !loading" description="暂未关注任何店铺，去逛逛吧~" />
              <div v-else class="mf-list">
                <div v-for="shop in shops" :key="shop.id" class="mf-shop-item">
                  <img :src="shop.shopLogo || 'https://picsum.photos/seed/default/60/60'" class="mf-shop-logo" />
                  <div class="mf-shop-info">
                    <div class="mf-shop-name">{{ shop.shopName }}</div>
                    <div class="mf-shop-desc">{{ shop.intro || '' }}</div>
                  </div>
                  <el-button text type="primary" @click="goShop(shop.id)">进店看看</el-button>
                </div>
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
import { ElMessage } from 'element-plus'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import ProfileSidebar from '@/views/profile/ProfileSidebar.vue'
import { getProfile, getProfileStats, type UserProfileDTO, type ProfileStats } from '@/api/profile'
import request from '@/api/request'

const router = useRouter()
const loading = ref(false)
const user = ref<UserProfileDTO | null>(null)
const stats = ref<ProfileStats | null>(null)
const shops = ref<any[]>([])

async function fetchData() {
  loading.value = true
  try {
    const [profileRes, statsRes, shopRes] = await Promise.all([
      getProfile(),
      getProfileStats(),
      request.get<any[]>('/shop/follows'),
    ])
    user.value = profileRes.data
    stats.value = statsRes.data
    shops.value = shopRes.data || []
  } catch {
    shops.value = []
  } finally {
    loading.value = false
  }
}

function goShop(shopId: number) {
  router.push(`/shop/${shopId}`)
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.mf-page { min-height: 100vh; background: #f5f5f5; }
.mf-wrapper { max-width: 1600px; margin: 0 auto; padding: 16px; }
.mf-layout { display: flex; gap: 16px; align-items: flex-start; }
.mf-main { flex: 1; min-width: 0; }
.mf-card { background: #fff; border-radius: 8px; box-shadow: 0 1px 4px rgba(0,0,0,0.06); }
.mf-card-header { padding: 20px 24px; border-bottom: 1px solid #f0f0f0; }
.mf-card-header h3 { margin: 0; font-size: 18px; font-weight: 600; color: #333; }
.mf-body { padding: 8px 0; min-height: 200px; }
.mf-list { padding: 0; }
.mf-shop-item {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px 24px;
  border-bottom: 1px solid #f5f5f5;
}
.mf-shop-item:last-child { border-bottom: none; }
.mf-shop-logo { width: 56px; height: 56px; border-radius: 8px; object-fit: cover; }
.mf-shop-info { flex: 1; min-width: 0; }
.mf-shop-name { font-size: 15px; font-weight: 600; color: #333; margin-bottom: 4px; }
.mf-shop-desc { font-size: 13px; color: #999; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
</style>