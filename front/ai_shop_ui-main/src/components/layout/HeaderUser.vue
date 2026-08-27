<template>
  <div class="header-user">
    <div class="container header-user-inner">
      <!-- 左侧 -->
      <div class="hu-left">
        <span class="hu-link hu-link-home" @click="goHome" :title="'返回商城首页'">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12l2-2m0 0l7-7 7 7M5 10v10a1 1 0 001 1h3m10-11l2 2m-2-2v10a1 1 0 01-1 1h-3m-6 0a1 1 0 001-1v-4a1 1 0 011-1h2a1 1 0 011 1v4a1 1 0 001 1m-6 0h6"/></svg>
          首页
        </span>
        <span v-if="user" class="hu-divider"></span>
        <span v-if="user" class="hu-user" @click="goProfile">{{ user.username }}</span>
        <span v-else class="hu-link" @click="goLogin">
          <el-icon><User /></el-icon>请登录
        </span>
        <span v-if="!user" class="hu-link" @click="goLogin">免费注册</span>
      </div>

      <!-- 右侧 -->
      <div class="hu-right" ref="navRef">
        <!-- 移动端汉堡按钮 -->
         
        <button class="hu-mobile-toggle" @click="mobileOpen = !mobileOpen">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 12h18M3 6h18M3 18h18"/></svg>
        </button>

        <!-- 导航链接 -->
        <div class="hu-nav-links" :class="{ 'mobile-open': mobileOpen }">

          <!-- ===== 游客模式：仅显示登录按钮 ===== -->
          <template v-if="!user">
            <span class="hu-link hu-link-login-guest" @click="goLogin" title="登录账号">
              <el-icon><User /></el-icon>登录
            </span>
          </template>

          <!-- ===== 已登录用户：完整导航 ===== -->
          <template v-else>
            <!-- 核心消费入口 -->
            <span class="hu-link hu-link-cart" @click="goCart" title="查看购物车商品">
              <el-icon><ShoppingCart /></el-icon>购物车
              <sup v-if="cartCount > 0" class="hu-badge">{{ cartCount > 99 ? '99+' : cartCount }}</sup>
            </span>
            <span class="hu-link hu-link-coupon" @click="goCoupons" title="免费领取平台/店铺优惠券">
              <el-icon><Ticket /></el-icon>领券中心
              <sup v-if="hasNewCoupon" class="hu-badge hu-badge-dot"></sup>
            </span>
            <span class="hu-link hu-link-profile" @click="goProfile" title="个人中心">
              <el-icon><User /></el-icon>个人中心
            </span>

            <!-- 分隔 -->
            <span class="hu-nav-divider"></span>

            <!-- 个人业务入口 -->
            <span class="hu-link hu-link-aftersale" @click="goAfterSale" title="查看售后进度">
              <el-icon><WarningFilled /></el-icon>我的售后
              <sup v-if="hasPendingAfterSale" class="hu-badge hu-badge-dot hu-badge-orange"></sup>
            </span>
            <span class="hu-link hu-link-service" @click="goCustomerService" title="联系平台客服">
              <el-icon><Headset /></el-icon>平台客服
            </span>
            <span class="hu-link hu-link-merchant-chat" @click="goMerchantChat" title="与商家沟通记录">
              <el-icon><ChatDotRound /></el-icon>商家客服
            </span>
            <span class="hu-link hu-link-help" @click="goHelp" title="帮助中心">
              <el-icon><QuestionFilled /></el-icon>帮助中心
            </span>

            <!-- 分隔 -->
            <span class="hu-nav-divider"></span>

            <!-- 低频入口 -->
            <span v-if="isMerchant" class="hu-link hu-link-merchant" @click="goMerchant" title="商家管理中心">
              <el-icon><Shop /></el-icon>商家中心
            </span>
            <span v-else-if="!isMerchant" class="hu-link hu-link-become" @click="goBecomeMerchant" title="开通店铺，成为商家">
              <el-icon><Shop /></el-icon>商家入驻
            </span>
            <span class="hu-link hu-link-logout" @click="handleLogout" title="退出当前账号">
              <el-icon><SwitchButton /></el-icon>退出登录
            </span>
          </template>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  User,
  ShoppingCart,
  QuestionFilled,
  Shop,
  SwitchButton,
  WarningFilled,
  Ticket,
  Headset,
  ChatDotRound,
} from '@element-plus/icons-vue'
import { useAuthStore } from '../../stores/auth'
import { useCartStore } from '../../stores/cart'

const router = useRouter()
const authStore = useAuthStore()
const cartStore = useCartStore()

const mobileOpen = ref(false)
const navRef = ref<HTMLElement | null>(null)

const user = computed(() => authStore.user)
const isMerchant = computed(() => authStore.user?.roleCode === 'MERCHANT')
const cartCount = computed(() => cartStore.totalNum || 0)

// 红点状态（可扩展为从接口获取）
const hasNewCoupon = ref(false)
const hasPendingAfterSale = ref(false)

function closeMobile(e: MouseEvent) {
  if (navRef.value && !navRef.value.contains(e.target as Node)) {
    mobileOpen.value = false
  }
}

onMounted(async () => {
  document.addEventListener('click', closeMobile)
  if (authStore.token) {
    try {
      await cartStore.fetchCart()
    } catch { /* ignore */ }
  }
})

onBeforeUnmount(() => {
  document.removeEventListener('click', closeMobile)
})

function goLogin() { router.push('/login') }
function goHome() { router.push('/home') }
function goCart() { router.push('/cart'); mobileOpen.value = false }
function goProfile() { router.push('/profile'); mobileOpen.value = false }
function goCoupons() { router.push('/coupons'); mobileOpen.value = false }
function goCustomerService() { router.push('/platform-chat'); mobileOpen.value = false }
function goMerchantChat() { router.push('/chat/merchant'); mobileOpen.value = false }
function goAfterSale() { router.push('/aftersale/list'); mobileOpen.value = false }
function goHelp() { router.push('/help'); mobileOpen.value = false }
function goMerchant() { router.push('/merchant'); mobileOpen.value = false }
function goBecomeMerchant() { router.push('/merchant/apply'); mobileOpen.value = false }

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    .then(() => {
      authStore.logout()
      setTimeout(() => window.location.reload(), 300)
    })
    .catch(() => {})
}
</script>

<style scoped>
.container {
  width: 90%;
  max-width: 1900px;
  margin: 0 auto;
  padding: 0 16px;
  box-sizing: border-box;
}

.header-user {
  width: 100%;
  background: #f5f5f5;
  font-size: 12px;
  color: #666;
  position: relative;
  z-index: 100;
}

.header-user-inner {
  height: 34px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.hu-left,
.hu-right {
  display: flex;
  align-items: center;
}

/* ===== 左侧 ===== */
.hu-link {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  color: #666;
  padding: 0 10px;
  cursor: pointer;
  height: 20px;
  line-height: 20px;
  transition: color 0.15s;
  white-space: nowrap;
  position: relative;
  text-decoration: none;
}
.hu-link:hover {
  color: #ff0f23;
}

.hu-link-home {
  color: #333;
  font-weight: 500;
}
.hu-link-home:hover {
  color: #ff0f23;
}

.hu-user {
  color: #ff0f23;
  font-weight: 600;
  cursor: pointer;
  transition: color 0.15s;
  margin-left: 4px;
}
.hu-user:hover {
  color: #cc0011;
}

.hu-divider {
  display: inline-block;
  width: 1px;
  height: 10px;
  background: #ddd;
  margin: 0 6px;
}

/* ===== 右侧导航 ===== */
.hu-nav-links {
  display: flex;
  align-items: center;
}

.hu-nav-divider {
  display: inline-block;
  width: 1px;
  height: 12px;
  background: #e0e0e0;
  margin: 0 2px;
  flex-shrink: 0;
}

/* 红点徽标 */
.hu-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 5px;
  background: #ff0f23;
  color: #fff;
  font-size: 10px;
  font-weight: 600;
  border-radius: 8px;
  line-height: 1;
  position: absolute;
  top: -6px;
  right: 2px;
}
.hu-badge-dot {
  min-width: 8px;
  width: 8px;
  height: 8px;
  padding: 0;
  top: -2px;
  right: 4px;
}
.hu-badge-orange {
  background: #ff8a00;
}

/* 功能色系分层 */
/* 消费类 - 品牌红色 */
.hu-link-cart,
.hu-link-coupon {
  color: #e4393c;
}
.hu-link-cart:hover,
.hu-link-coupon:hover {
  color: #cc0011;
}

/* 售后客服 - 橙色警示 */
.hu-link-aftersale {
  color: #ff8a00;
}
.hu-link-aftersale:hover {
  color: #e67a00;
}

/* 商家客服 */
.hu-link-merchant-chat {
  color: #409eff;
}
.hu-link-merchant-chat:hover {
  color: #66b1ff;
}

/* 商家入驻 - 弱化 */
.hu-link-become {
  color: #bbb;
}
.hu-link-become:hover {
  color: #999;
}

/* 退出登录 - 弱化防误触 */
.hu-link-logout {
  color: #ccc;
  font-size: 11px;
}
.hu-link-logout:hover {
  color: #ff0f23;
}

/* 游客登录按钮 */
.hu-link-login-guest {
  color: #e4393c;
  font-weight: 600;
  font-size: 13px;
}
.hu-link-login-guest:hover {
  color: #cc0011;
}

/* 移动端汉堡按钮 */
.hu-mobile-toggle {
  display: none;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: #666;
  padding: 0;
}
.hu-mobile-toggle:hover {
  color: #ff0f23;
}

/* ===== 响应式：移动端折叠 ===== */
@media (max-width: 960px) {
  .container {
    padding: 0 12px;
  }

  .hu-mobile-toggle {
    display: flex;
  }

  .hu-nav-links {
    display: none;
    position: absolute;
    top: 100%;
    right: 0;
    left: 0;
    background: #fff;
    flex-direction: column;
    align-items: stretch;
    padding: 8px 0;
    box-shadow: 0 4px 12px rgba(0,0,0,0.1);
    border-radius: 0 0 8px 8px;
    max-height: 80vh;
    overflow-y: auto;
  }

  .hu-nav-links.mobile-open {
    display: flex;
  }

  .hu-nav-links .hu-link {
    height: 40px;
    line-height: 40px;
    padding: 0 16px;
    justify-content: space-between;
    border-bottom: 1px solid #f5f5f5;
  }
  .hu-nav-links .hu-link:last-child {
    border-bottom: none;
  }

  .hu-nav-divider {
    display: none;
  }

  .hu-badge {
    position: static;
    margin-left: auto;
  }
  .hu-badge-dot {
    margin-left: 4px;
    min-width: 6px;
    width: 6px;
    height: 6px;
  }
}
</style>