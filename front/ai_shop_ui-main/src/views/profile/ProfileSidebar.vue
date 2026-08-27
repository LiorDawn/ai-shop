<template>
  <div class="ps-sidebar">
    <!-- 用户信息卡片 -->
    <div class="ps-user-card">
      <div class="ps-avatar-row">
        <div class="ps-avatar">
          <img :src="user?.avatar || 'https://picsum.photos/seed/default/80/80'" />
        </div>
        <div class="ps-user-info">
          <div class="ps-user-name">{{ user?.nickname || user?.username || '未设置昵称' }}</div>
          <div class="ps-user-phone">{{ user?.phone || '' }}</div>
          <span class="ps-edit-btn" @click="goEditProfile">编辑资料 <svg width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></span>
        </div>
      </div>

      <!-- 数据统计（可点击） -->
      <div class="ps-stats">
        <div class="ps-stat-item" @click="goOrders">
          <span class="ps-stat-num">{{ stats?.orderCount ?? '-' }}</span>
          <span class="ps-stat-label">订单</span>
        </div>
        <div class="ps-stat-divider"></div>
        <div class="ps-stat-item" @click="goFavorites">
          <span class="ps-stat-num">{{ stats?.collectCount ?? '-' }}</span>
          <span class="ps-stat-label">收藏</span>
        </div>
        <div class="ps-stat-divider"></div>
        <div class="ps-stat-item" @click="goFollows">
          <span class="ps-stat-num">{{ stats?.followShopCount ?? '-' }}</span>
          <span class="ps-stat-label">关注</span>
        </div>
      </div>

      <!-- 优惠券过期提醒 -->
      <div v-if="expiringCouponCount > 0" class="ps-coupon-tip" @click="goCoupons">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 12V6H4v6m16 0a2 2 0 0 1-2 2 2 2 0 1 0 4 0m-16 0a2 2 0 0 0 2 2 2 2 0 1 1-4 0m4-4h12"/></svg>
        你有 <strong>{{ expiringCouponCount }}</strong> 张优惠券即将过期 →
        <span class="ps-coupon-tip-action">去使用</span>
      </div>
    </div>

    <!-- 导航菜单 - 分组 -->
    <el-menu
      :default-active="activeMenu"
      class="ps-menu"
      @select="handleSelect"
    >
      <!-- 模块 1：账户设置 -->
      <div class="ps-group-label">账户设置</div>
      <el-menu-item index="/profile">
        <el-icon><User /></el-icon>
        <span>个人资料</span>
      </el-menu-item>
      <el-menu-item index="/profile/address">
        <el-icon><Location /></el-icon>
        <span>收货地址管理</span>
      </el-menu-item>
      <el-menu-item index="/profile/password">
        <el-icon><Lock /></el-icon>
        <span>修改密码</span>
      </el-menu-item>

      <div class="ps-group-label">订单与优惠</div>
      <el-menu-item index="/orders">
        <el-icon><List /></el-icon>
        <span>我的订单</span>
      </el-menu-item>
      <el-menu-item index="/aftersale/list">
        <el-icon><WarningFilled /></el-icon>
        <span>我的售后</span>
        <sup v-if="hasPendingAfterSale" class="ps-badge-dot"></sup>
      </el-menu-item>
      <el-menu-item index="/review/pending">
        <el-icon><ChatDotSquare /></el-icon>
        <span>评价晒单</span>
      </el-menu-item>
      <el-menu-item index="/profile/coupons">
        <el-icon>
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M20 12V6H4v6m16 0a2 2 0 0 1-2 2 2 2 0 1 0 4 0m-16 0a2 2 0 0 0 2 2 2 2 0 1 1-4 0m4-4h12"/></svg>
        </el-icon>
        <span>我的优惠券</span>
        <sup v-if="expiringCouponCount > 0" class="ps-badge-num">{{ expiringCouponCount }}</sup>
      </el-menu-item>

      <div class="ps-group-label">收藏与聊天</div>
      <el-menu-item index="/profile/favorites">
        <el-icon><StarFilled /></el-icon>
        <span>我的收藏</span>
      </el-menu-item>
      <el-menu-item index="/profile/follows">
        <el-icon><Shop /></el-icon>
        <span>我的关注店铺</span>
      </el-menu-item>
      <el-menu-item index="/profile/chat-history">
        <el-icon><ChatLineSquare /></el-icon>
        <span>聊天记录</span>
        <sup v-if="unreadChatCount > 0" class="ps-badge-dot"></sup>
      </el-menu-item>

      <div class="ps-group-label">服务与开店</div>
      <el-menu-item index="/platform-chat">
        <el-icon><Headset /></el-icon>
        <span>平台客服</span>
      </el-menu-item>
      <el-menu-item v-if="!isMerchant" index="/merchant/apply" class="ps-menu-item-light">
        <el-icon><Goods /></el-icon>
        <span>商家入驻</span>
      </el-menu-item>
    </el-menu>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import {
  User, Location, StarFilled, Shop, Lock, List, ChatDotSquare, WarningFilled, Goods, Headset, ChatLineSquare,
} from '@element-plus/icons-vue'
import type { UserProfileDTO, ProfileStats } from '@/api/profile'
import { useAuthStore } from '@/stores/auth'

const props = defineProps<{
  user: UserProfileDTO | null
  stats: ProfileStats | null
}>()

const router = useRouter()
const route = useRoute()

const activeMenu = computed(() => route.path)

const auth = useAuthStore()

const isMerchant = computed(() => {
  const u = auth.user
  return u?.roleCode === 'MERCHANT'
})

// 红点/徽标数据（可扩展为从接口获取）
const hasPendingAfterSale = computed(() => false)
const unreadChatCount = computed(() => 0)

// 模拟过期优惠券数量（可从接口获取）
const expiringCouponCount = computed(() => {
  // TODO: 从接口获取即将过期的优惠券数量
  return 0
})

function handleSelect(index: string) {
  router.push(index)
}

function goEditProfile() { router.push('/profile') }
function goOrders() { router.push('/orders') }
function goFavorites() { router.push('/profile/favorites') }
function goFollows() { router.push('/profile/follows') }
function goCoupons() { router.push('/profile/coupons') }
</script>

<style scoped>
.ps-sidebar {
  width: 240px;
  flex-shrink: 0;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

/* ===== 用户卡片 ===== */
.ps-user-card {
  padding: 24px 20px 0;
}
.ps-avatar-row {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ps-avatar img {
  width: 60px;
  height: 60px;
  border-radius: 50%;
  object-fit: cover;
  border: 3px solid #e8e8e8;
}
.ps-user-info {
  flex: 1;
  min-width: 0;
}
.ps-user-name {
  font-size: 15px;
  font-weight: 600;
  color: #333;
}
.ps-user-phone {
  font-size: 12px;
  color: #999;
  margin-top: 2px;
}
.ps-edit-btn {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: #999;
  cursor: pointer;
  transition: color 0.15s;
  margin-top: 4px;
}
.ps-edit-btn:hover {
  color: #e4393c;
}

/* 统计数据行（可点击） */
.ps-stats {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0;
  margin-top: 16px;
  padding: 14px 0;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}
.ps-stat-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 3px;
  cursor: pointer;
  transition: background 0.15s;
  padding: 4px 0;
  border-radius: 6px;
}
.ps-stat-item:hover {
  background: #fef0f0;
}
.ps-stat-num {
  font-size: 20px;
  font-weight: 700;
  color: #e4393c;
  line-height: 1.2;
}
.ps-stat-label {
  font-size: 12px;
  color: #999;
  letter-spacing: 0.5px;
}
.ps-stat-divider {
  width: 1px;
  height: 30px;
  background: #eee;
  flex-shrink: 0;
}

/* 优惠券过期提醒 */
.ps-coupon-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin: 12px 0;
  padding: 8px 12px;
  background: #fff2f5;
  border-radius: 6px;
  font-size: 12px;
  color: #e4393c;
  cursor: pointer;
  transition: background 0.15s;
}
.ps-coupon-tip:hover {
  background: #ffe8ec;
}
.ps-coupon-tip strong {
  font-size: 13px;
}
.ps-coupon-tip-action {
  font-weight: 600;
  text-decoration: underline;
}

/* ===== 菜单分组 ===== */
.ps-menu {
  border-right: none !important;
  padding: 0 0 8px !important;
}

.ps-group-label {
  padding: 16px 20px 4px;
  font-size: 12px;
  font-weight: 600;
  color: #bbb;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.ps-menu .el-menu-item {
  height: 40px;
  line-height: 40px;
  font-size: 13px;
  margin: 1px 8px;
  border-radius: 6px;
  position: relative;
}
.ps-menu .el-menu-item.is-active {
  color: #e4393c;
  background: #fef0f0;
  font-weight: 600;
}
.ps-menu .el-menu-item:hover {
  background: #f5f5f5;
}

/* 低频菜单弱化 */
.ps-menu-item-light {
  opacity: 0.6;
}
.ps-menu-item-light:hover {
  opacity: 1;
}

/* 徽标 */
.ps-badge-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #ff0f23;
  position: absolute;
  right: 14px;
  top: 50%;
  margin-top: -3px;
}
.ps-badge-num {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  background: #ff0f23;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  border-radius: 9px;
  line-height: 1;
  position: absolute;
  right: 10px;
  top: 50%;
  margin-top: -9px;
}
</style>