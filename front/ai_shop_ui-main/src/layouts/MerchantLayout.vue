<template>
  <div class="ml-container">
    <el-container class="ml-layout">
      <!-- 侧边栏 -->
      <el-aside class="ml-sidebar" :width="collapse ? '64px' : '220px'">
        <div class="ml-logo" @click="router.push('/merchant/dashboard')">
          <span v-if="!collapse" class="ml-logo-text">AI 商家中心</span>
          <span v-else class="ml-logo-mini">S</span>
        </div>
        <el-scrollbar>
          <el-menu
            :default-active="activeMenu"
            :collapse="collapse"
            background-color="#1d2a3a"
            text-color="#bfcbd9"
            active-text-color="#409eff"
            router
          >
            <el-menu-item index="/merchant/dashboard">
              <el-icon><DataAnalysis /></el-icon>
              <template #title>数据看板</template>
            </el-menu-item>

            <el-sub-menu index="shop">
              <template #title>
                <el-icon><Setting /></el-icon>
                <span>店铺设置</span>
              </template>
              <el-menu-item index="/merchant/shop/settings">基本信息</el-menu-item>
              <el-menu-item index="/merchant/shop/password">修改密码</el-menu-item>
            </el-sub-menu>

            <el-menu-item index="/merchant/product">
              <el-icon><Goods /></el-icon>
              <template #title>商品管理</template>
            </el-menu-item>

            <el-menu-item index="/merchant/order">
              <el-icon><List /></el-icon>
              <template #title>订单管理</template>
            </el-menu-item>

            <el-menu-item index="/merchant/aftersale">
              <el-icon><ChatDotRound /></el-icon>
              <template #title>售后管理</template>
            </el-menu-item>

            <el-menu-item index="/merchant/comment">
              <el-icon><Star /></el-icon>
              <template #title>评价管理</template>
            </el-menu-item>

            <el-menu-item index="/merchant/customer-service">
              <el-icon><Service /></el-icon>
              <template #title>
                <span>客户咨询</span>
                <el-badge
                  :value="unreadCount"
                  :hidden="unreadCount === 0"
                  :max="99"
                  class="ml-badge"
                />
              </template>
            </el-menu-item>
          </el-menu>
        </el-scrollbar>
      </el-aside>

      <!-- 主区域 -->
      <el-container>
        <el-header class="ml-header">
          <div class="ml-header-left">
            <el-icon class="ml-collapse-btn" @click="collapse = !collapse">
              <Fold v-if="!collapse" />
              <Expand v-else />
            </el-icon>
            <el-breadcrumb separator="/">
              <el-breadcrumb-item :to="{ path: '/merchant/dashboard' }">首页</el-breadcrumb-item>
              <el-breadcrumb-item v-if="breadcrumb">{{ breadcrumb }}</el-breadcrumb-item>
            </el-breadcrumb>
          </div>
          <div class="ml-header-right">
            <el-button text @click="goHome">返回前台</el-button>
            <span class="ml-user-name">{{ user?.username }}</span>
            <el-button type="danger" size="small" @click="handleLogout">退出</el-button>
          </div>
        </el-header>

        <el-main class="ml-main">
          <router-view />
        </el-main>
      </el-container>
    </el-container>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import {
  DataAnalysis, Setting, Goods, List, ChatDotRound, Star,
  Fold, Expand, Service,
} from '@element-plus/icons-vue'
import request from '../../api/request'
import { useMerchantWebSocket } from '../../composables/useWebSocket'
import { useAuthStore } from '../../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()
const collapse = ref(false)
const unreadCount = ref(0)
let pollTimer: ReturnType<typeof setInterval> | null = null

const { connect, disconnect } = useMerchantWebSocket()

function getMerchantWsUrl() {
  if (!auth.user) return ''
  const baseUrl = import.meta.env.VITE_WS_URL || `ws://${location.host}`
  return `${baseUrl}/api/ws/merchant?uid=${auth.user.id}&type=merchant`
}

onMounted(() => {
  console.log('[MerchantLayout] 已挂载，初始化 WebSocket')
  fetchUnreadCount()
  pollTimer = setInterval(fetchUnreadCount, 15000)
  connect(getMerchantWsUrl())
})
onUnmounted(() => {
  console.log('[MerchantLayout] 已卸载，断开 WebSocket')
  if (pollTimer) clearInterval(pollTimer)
  disconnect()
})

async function fetchUnreadCount() {
  try {
    const res = await request.get<any>('/merchant/chat/unread-count')
    if (res.data) unreadCount.value = res.data.unreadCount || 0
  } catch { /* ignore */ }
}

const user = auth.user

const activeMenu = computed(() => {
  return route.path
})

const breadcrumbMap: Record<string, string> = {
  '/merchant/dashboard': '数据看板',
  '/merchant/shop/settings': '店铺基本信息',
  '/merchant/shop/password': '修改密码',
  '/merchant/product': '商品管理',
  '/merchant/product/edit': '添加商品',
  '/merchant/order': '订单管理',
  '/merchant/aftersale': '售后管理',
  '/merchant/aftersale/process': '售后处理',
  '/merchant/comment': '评价管理',
}

const breadcrumb = computed(() => {
  const path = route.path
  if (breadcrumbMap[path]) return breadcrumbMap[path]
  const matched = Object.entries(breadcrumbMap).find(([key]) => path.startsWith(key))
  return matched ? matched[1] : ''
})

function goHome() {
  router.push('/home')
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', { type: 'warning' })
    .then(() => auth.logout())
    .catch(() => {})
}
</script>

<style scoped>
.ml-container { height: 100vh; }
.ml-layout { height: 100%; }
.ml-sidebar {
  background: #1d2a3a;
  transition: width 0.3s;
  overflow: hidden;
}
.ml-logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: bold;
  cursor: pointer;
  border-bottom: 1px solid rgba(255,255,255,0.1);
}
.ml-logo-text { white-space: nowrap; }
.ml-logo-mini { font-size: 22px; }
.ml-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-bottom: 1px solid #e6e6e6;
  padding: 0 20px;
  height: 60px !important;
}
.ml-header-left {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ml-collapse-btn {
  font-size: 20px;
  cursor: pointer;
  color: #666;
}
.ml-collapse-btn:hover { color: #409eff; }
.ml-header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}
.ml-user-name { font-size: 14px; color: #666; }
.ml-main {
  background: #f0f2f5;
  padding: 16px;
  overflow-y: auto;
}
/* 覆盖 el-menu 样式 */
.ml-sidebar .el-menu { border-right: none; }
.ml-sidebar .el-sub-menu .el-menu { background-color: #162436; }
.ml-badge { margin-left: 8px; }
.ml-badge :deep(.el-badge__content) { position: static; transform: none; }
</style>