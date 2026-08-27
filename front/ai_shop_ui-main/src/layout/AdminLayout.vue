<template>
  <el-container class="admin-layout">
    <el-header class="admin-header">
      <div class="header-left">
        <span class="header-title">AI 智能商城后台管理</span>
      </div>
      <div class="header-right">
        <span class="user-info">欢迎，{{ user?.username || '管理员' }}</span>
        <el-button type="danger" size="small" @click="handleLogout">退出登录</el-button>
      </div>
    </el-header>
    <el-container>
      <el-aside width="220px" class="admin-aside">
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#304156"
          text-color="#bfcbd9"
          active-text-color="#409eff"
        >
          <el-menu-item index="/admin/user">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/merchant">
            <el-icon><Shop /></el-icon>
            <span>商家管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/shop">
            <el-icon><HomeFilled /></el-icon>
            <span>店铺管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/product">
            <el-icon><Goods /></el-icon>
            <span>商品管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/category">
            <el-icon><CollectionTag /></el-icon>
            <span>分类管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/order">
            <el-icon><List /></el-icon>
            <span>订单管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/coupon">
            <el-icon><Ticket /></el-icon>
            <span>优惠券管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/after-sale">
            <el-icon><WarningFilled /></el-icon>
            <span>售后管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/comment">
            <el-icon><ChatDotSquare /></el-icon>
            <span>评价管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/stats">
            <el-icon><DataAnalysis /></el-icon>
            <span>数据统计</span>
          </el-menu-item>
          <el-menu-item index="/admin/customer-service">
            <el-icon><Headset /></el-icon>
            <span>客服工作台</span>
            <el-badge
              :value="unreadCount"
              :hidden="unreadCount === 0"
              :max="99"
              class="admin-badge"
            />
          </el-menu-item>
          <el-menu-item index="/admin/system-config">
            <el-icon><Setting /></el-icon>
            <span>系统配置</span>
          </el-menu-item>
          <!-- 超级管理员专属菜单 -->
          <template v-if="isSuperAdmin">
            <el-divider style="border-color:#4a5a72;margin:8px 0" />
          </template>
        </el-menu>
      </el-aside>
      <el-main class="admin-main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { User, Shop, HomeFilled, Goods, CollectionTag, List, Ticket, WarningFilled, ChatDotSquare, DataAnalysis, Headset, Setting } from '@element-plus/icons-vue'
import request from '../api/request'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const user = auth.user
const isSuperAdmin = computed(() => user?.roleCode === 'SUPER_ADMIN')
const activeMenu = computed(() => route.path)
const unreadCount = ref(0)
let pollTimer: ReturnType<typeof setInterval> | null = null

onMounted(() => { fetchUnreadCount(); pollTimer = setInterval(fetchUnreadCount, 15000) })
onUnmounted(() => { if (pollTimer) clearInterval(pollTimer) })

async function fetchUnreadCount() {
  try {
    const res = await request.get<any>('/admin/chat/unread-count')
    if (res.data) unreadCount.value = res.data.unreadCount || 0
  } catch { /* ignore */ }
}

function handleLogout() {
  ElMessageBox.confirm('确定要退出登录吗？', '提示', {
    type: 'warning',
  }).then(() => auth.logout()).catch(() => {})
}
</script>

<style scoped>
.admin-layout {
  height: 100vh;
}

.admin-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #242f42;
  color: #fff;
  padding: 0 20px;
  height: 60px !important;
}

.header-left {
  display: flex;
  align-items: center;
}

.header-title {
  font-size: 18px;
  font-weight: bold;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 12px;
}

.user-info {
  font-size: 14px;
  color: #bfcbd9;
}

.admin-aside {
  background: #304156;
  overflow-y: auto;
}

.admin-main {
  background: #f0f2f5;
  padding: 20px;
}
.admin-badge { margin-left: 8px; }
.admin-badge :deep(.el-badge__content) { position: static; transform: none; }
</style>