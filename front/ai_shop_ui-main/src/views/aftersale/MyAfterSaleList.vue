<template>
  <div class="asl-page">
    <HeaderUser />

    <div class="asl-container">
      <h2 class="asl-title">我的售后</h2>

      <!-- 状态筛选 -->
      <div class="asl-filter">
        <span
          v-for="s in statusTabs"
          :key="s.value"
          class="asl-filter-item"
          :class="{ active: activeStatus === s.value }"
          @click="onStatusChange(s.value)"
        >{{ s.label }}</span>
      </div>

      <!-- 售后列表 -->
      <div v-loading="loading">
        <div v-if="list.length === 0 && !loading" class="asl-empty">
          <el-empty description="暂无售后记录" />
        </div>

        <div
          v-for="item in list"
          :key="item.id"
          class="asl-card"
        >
          <!-- 头部 -->
          <div class="asl-card-header">
            <div class="asl-card-order">
              售后单号：<span class="asl-order-no">{{ item.id }}</span>
              <span class="asl-time">申请时间：{{ item.createTime }}</span>
            </div>
            <el-tag :type="statusTagType(item.auditStatus)" effect="light" size="small">
              {{ statusText(item.auditStatus) }}
            </el-tag>
          </div>

          <!-- 商品信息 -->
          <div class="asl-card-body" @click="goDetail(item)">
            <img :src="item.productImage" class="asl-item-img" />
            <div class="asl-item-info">
              <div class="asl-item-name">{{ item.productName }}</div>
              <div class="asl-item-spec" v-if="item.spec">{{ item.spec }}</div>
              <div class="asl-item-meta">
                <span class="asl-item-type">{{ item.typeText }}</span>
                <span class="asl-item-amount">退款金额：<span class="asl-price">¥{{ fmtPrice(item.amount) }}</span></span>
              </div>
            </div>

            <!-- 操作按钮 -->
            <div class="asl-item-actions">
              <el-button size="small" @click.stop="goDetail(item)">查看详情</el-button>
              <el-button
                v-if="item.auditStatus === 0"
                size="small"
                type="warning"
                @click.stop="cancelApply(item)"
              >撤销申请</el-button>
            </div>
          </div>
        </div>

        <el-pagination
          v-if="total > pageSize"
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next"
          @current-change="fetchList"
          class="asl-pager"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import { getMyAfterSales, cancelAfterSale, type AfterSaleDTO, type PageResult } from '@/api/afterSale'

const router = useRouter()

const statusTabs = [
  { label: '全部', value: -1 },
  { label: '待商家处理', value: 0 },
  { label: '商家已同意', value: 1 },
  { label: '已拒绝', value: 2 },
  { label: '退款完成', value: 3 },
  { label: '已关闭', value: 4 },
]

const loading = ref(false)
const list = ref<AfterSaleDTO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const activeStatus = ref(-1)

function statusText(status: number) {
  const map: Record<number, string> = {
    0: '待商家处理',
    1: '商家已同意',
    2: '已拒绝',
    3: '退款完成',
    4: '已关闭',
  }
  return map[status] || '未知'
}

function statusTagType(status: number) {
  const map: Record<number, string> = {
    0: 'warning',
    1: 'primary',
    2: 'danger',
    3: 'success',
    4: 'info',
  }
  return map[status] || 'info'
}

function fmtPrice(p: number | undefined | null): string {
  if (p === undefined || p === null || isNaN(p as number)) return '0.00'
  return Number(p).toFixed(2)
}

async function fetchList() {
  loading.value = true
  try {
    const params: any = { current: currentPage.value, size: pageSize.value }
    if (activeStatus.value !== -1) params.auditStatus = activeStatus.value
    const res = await getMyAfterSales(params)
    const data: PageResult<AfterSaleDTO> = res.data
    list.value = data.records || []
    total.value = data.total || 0
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onStatusChange(status: number) {
  activeStatus.value = status
  currentPage.value = 1
  fetchList()
}

function goDetail(item: AfterSaleDTO) {
  router.push(`/aftersale/detail?id=${item.id}`)
}

async function cancelApply(item: AfterSaleDTO) {
  try {
    await ElMessageBox.confirm('确定撤销该售后申请？撤销后不可恢复。', '提示', { type: 'warning' })
    await cancelAfterSale(item.id)
    ElMessage.success('已撤销')
    fetchList()
  } catch {
    // cancelled
  }
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.asl-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.asl-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 16px;
}
.asl-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 16px;
  color: #333;
}
.asl-filter {
  display: flex;
  align-items: center;
  gap: 4px;
  background: #fff;
  border-radius: 8px;
  padding: 12px 16px;
  margin-bottom: 16px;
}
.asl-filter-item {
  padding: 6px 16px;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
  color: #666;
  transition: all 0.2s;
}
.asl-filter-item.active {
  background: #e4393c;
  color: #fff;
}
.asl-empty {
  background: #fff;
  border-radius: 8px;
  padding: 40px;
}
.asl-card {
  background: #fff;
  border-radius: 8px;
  margin-bottom: 12px;
  overflow: hidden;
}
.asl-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  background: #fafafa;
}
.asl-card-order {
  font-size: 13px;
  color: #666;
}
.asl-order-no {
  color: #333;
  font-weight: 500;
  margin-right: 16px;
}
.asl-time {
  color: #999;
  margin-left: 8px;
}
.asl-card-body {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 16px;
  cursor: pointer;
  transition: background 0.2s;
}
.asl-card-body:hover {
  background: #fafafa;
}
.asl-item-img {
  width: 80px;
  height: 80px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}
.asl-item-info {
  flex: 1;
  min-width: 0;
}
.asl-item-name {
  font-size: 14px;
  color: #333;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.asl-item-spec {
  font-size: 12px;
  color: #999;
  margin-bottom: 8px;
}
.asl-item-meta {
  font-size: 13px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 16px;
}
.asl-item-type {
  background: #f0f0f0;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}
.asl-price {
  color: #e4393c;
  font-weight: 600;
}
.asl-item-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.asl-pager {
  margin-top: 16px;
  justify-content: center;
}
</style>
