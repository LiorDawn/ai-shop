<template>
  <div class="up-page">
    <HeaderUser />

    <div class="up-wrapper">
      <div class="up-layout">
        <!-- 左侧导航 -->
        <ProfileSidebar :user="user" :stats="stats" />

        <!-- 主内容 - 只保留订单模块 -->
        <div class="up-main">
          <div class="up-order-card">
            <div class="up-order-title">我的订单</div>
            <div class="up-order-icons">
              <div class="up-order-icon-item" @click="goOrders(0)">
                <div class="up-order-icon-box">
                  <el-icon :size="26"><Wallet /></el-icon>
                  <span v-if="(stats?.pendingPayCount ?? 0) > 0" class="up-order-badge">{{ stats?.pendingPayCount }}</span>
                </div>
                <span class="up-order-label">待付款</span>
              </div>
              <div class="up-order-icon-item" @click="goOrders(1)">
                <div class="up-order-icon-box">
                  <el-icon :size="26"><Goods /></el-icon>
                  <span v-if="(stats?.pendingShipCount ?? 0) > 0" class="up-order-badge">{{ stats?.pendingShipCount }}</span>
                </div>
                <span class="up-order-label">待发货</span>
              </div>
              <div class="up-order-icon-item" @click="goOrders(2)">
                <div class="up-order-icon-box">
                  <el-icon :size="26"><TakeawayBox /></el-icon>
                  <span v-if="(stats?.pendingReceiveCount ?? 0) > 0" class="up-order-badge">{{ stats?.pendingReceiveCount }}</span>
                </div>
                <span class="up-order-label">待收货</span>
              </div>
              <div class="up-order-icon-item" @click="goOrders(3)">
                <div class="up-order-icon-box">
                  <el-icon :size="26"><ChatDotSquare /></el-icon>
                  <span v-if="(stats?.pendingReviewCount ?? 0) > 0" class="up-order-badge">{{ stats?.pendingReviewCount }}</span>
                </div>
                <span class="up-order-label">待评价</span>
              </div>
              <!-- 查看全部作为最后一个图标项 -->
              <div class="up-order-icon-item" @click="$router.push('/orders')">
                <div class="up-order-icon-box up-order-all-box">
                  <el-icon :size="26"><List /></el-icon>
                </div>
                <span class="up-order-label">全部订单</span>
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
import { Wallet, Goods, TakeawayBox, ChatDotSquare, List } from '@element-plus/icons-vue'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import ProfileSidebar from '@/views/profile/ProfileSidebar.vue'
import { getProfile, getProfileStats, type UserProfileDTO, type ProfileStats } from '@/api/profile'

const router = useRouter()
const user = ref<UserProfileDTO | null>(null)
const stats = ref<ProfileStats | null>(null)

async function fetchData() {
  try {
    const [profileRes, statsRes] = await Promise.all([
      getProfile(),
      getProfileStats(),
    ])
    user.value = profileRes.data
    stats.value = statsRes.data
  } catch {
    // 忽略
  }
}

function goOrders(status: number) {
  router.push(status === -1 ? '/orders' : `/orders?status=${status}`)
}

onMounted(() => {
  fetchData()
})
</script>

<style scoped>
.up-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.up-wrapper {
  max-width: 1600px;
  margin: 0 auto;
  padding: 16px;
}
.up-layout {
  display: flex;
  gap: 16px;
  align-items: flex-start;
}
.up-main {
  flex: 1;
  min-width: 0;
}

/* 订单卡片 */
.up-order-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  padding: 24px 32px;
}
.up-order-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  padding-bottom: 12px;
  border-bottom: 1px solid #f0f0f0;
}
.up-order-icons {
  display: flex;
  justify-content: space-around;
  gap: 0;
}
.up-order-icon-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  transition: color 0.2s;
  color: #666;
  font-size: 13px;
  padding: 0 20px;
  flex: 1;
  border-right: 1px solid #f0f0f0;
}
.up-order-icon-item:last-child {
  border-right: none;
}
.up-order-icon-item:hover {
  color: #e4393c;
}
.up-order-icon-box {
  position: relative;
  width: 52px;
  height: 52px;
  border-radius: 50%;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: background 0.2s;
}
.up-order-icon-item:hover .up-order-icon-box {
  background: #fef0f0;
}
.up-order-all-box {
  background: #fef8f0;
}
.up-order-icon-item:hover .up-order-all-box {
  background: #fdf0db;
}
.up-order-label {
  font-size: 13px;
  white-space: nowrap;
}
.up-order-badge {
  position: absolute;
  top: -3px;
  right: -3px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background: #e4393c;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  line-height: 18px;
  text-align: center;
  box-shadow: 0 2px 4px rgba(228,57,60,0.25);
}
</style>