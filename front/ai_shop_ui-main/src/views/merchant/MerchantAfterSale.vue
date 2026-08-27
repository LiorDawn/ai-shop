<template>
  <div class="mas-page">
    <HeaderUser />

    <div class="mas-container">
      <h2 class="mas-title">售后订单管理</h2>

      <!-- 筛选区 -->
      <div class="mas-filter">
        <el-select v-model="filterForm.auditStatus" placeholder="全部状态" style="width: 140px" clearable @change="onFilterChange">
          <el-option label="待处理" :value="0" />
          <el-option label="已同意" :value="1" />
          <el-option label="已拒绝" :value="2" />
          <el-option label="退款完成" :value="3" />
          <el-option label="已关闭" :value="4" />
        </el-select>
        <el-select v-model="filterForm.type" placeholder="全部类型" style="width: 140px" clearable @change="onFilterChange">
          <el-option label="仅退款" :value="0" />
          <el-option label="退货退款" :value="1" />
        </el-select>
        <el-input
          v-model="filterForm.orderNo"
          placeholder="订单号搜索"
          style="width: 220px"
          clearable
          @keyup.enter="onFilterChange"
        >
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" @click="onFilterChange">搜索</el-button>
      </div>

      <!-- 列表 -->
      <div v-loading="loading" class="mas-list">
        <el-table :data="list" border stripe style="width: 100%">
          <el-table-column prop="id" label="售后单号" width="110" />
          <el-table-column prop="orderNo" label="订单号" width="160" />
          <el-table-column prop="username" label="用户" width="100" />
          <el-table-column label="商品" min-width="200">
            <template #default="{ row }">
              <div class="mas-product-cell">
                <img :src="row.productImage" class="mas-product-img" />
                <div class="mas-product-info">
                  <div class="mas-product-name">{{ row.productName }}</div>
                  <div class="mas-product-spec" v-if="row.spec">{{ row.spec }}</div>
                </div>
              </div>
            </template>
          </el-table-column>
          <el-table-column label="类型" width="90">
            <template #default="{ row }">
              <el-tag size="small" :type="row.type === 1 ? 'warning' : 'info'">
                {{ row.type === 1 ? '退货退款' : '仅退款' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申请金额" width="100">
            <template #default="{ row }">
              <span class="mas-price">¥{{ fmtPrice(row.amount) }}</span>
            </template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusTagType(row.auditStatus)" size="small">
                {{ statusText(row.auditStatus) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="createTime" label="申请时间" width="170" />
          <el-table-column label="操作" width="140" fixed="right">
            <template #default="{ row }">
              <el-button size="small" type="primary" @click="goProcess(row)">处理</el-button>
              <el-button size="small" @click="viewDetail(row)">详情</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination
          v-if="total > pageSize"
          v-model:current-page="currentPage"
          :page-size="pageSize"
          :total="total"
          layout="prev, pager, next, total"
          @current-change="fetchList"
          class="mas-pager"
        />
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import { getMerchantAfterSales, type AfterSaleDTO, type PageResult } from '@/api/afterSale'

const router = useRouter()

const loading = ref(false)
const list = ref<AfterSaleDTO[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const filterForm = ref({
  auditStatus: null as number | null,
  type: null as number | null,
  orderNo: '',
})

function statusText(status: number) {
  const map: Record<number, string> = {
    0: '待处理',
    1: '已同意',
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
    if (filterForm.value.auditStatus !== null && filterForm.value.auditStatus !== undefined) {
      params.auditStatus = filterForm.value.auditStatus
    }
    if (filterForm.value.type !== null && filterForm.value.type !== undefined) {
      params.type = filterForm.value.type
    }
    if (filterForm.value.orderNo) {
      params.orderNo = filterForm.value.orderNo
    }
    const res = await getMerchantAfterSales(params)
    const data: PageResult<AfterSaleDTO> = res.data
    list.value = data.records || []
    total.value = data.total || 0
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '加载失败')
  } finally {
    loading.value = false
  }
}

function onFilterChange() {
  currentPage.value = 1
  fetchList()
}

function goProcess(row: AfterSaleDTO) {
  router.push({ path: '/merchant/aftersale/process', query: { id: row.id } })
}

function viewDetail(row: AfterSaleDTO) {
  router.push({ path: '/merchant/aftersale/process', query: { id: row.id, view: '1' } })
}

onMounted(() => {
  fetchList()
})
</script>

<style scoped>
.mas-page {
  min-height: 100vh;
  background: #f5f5f5;
}
.mas-container {
  max-width: 1400px;
  margin: 0 auto;
  padding: 16px;
}
.mas-title {
  font-size: 22px;
  font-weight: 600;
  margin: 0 0 16px;
  color: #333;
}
.mas-filter {
  display: flex;
  align-items: center;
  gap: 12px;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  margin-bottom: 16px;
}
.mas-list {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}
.mas-product-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}
.mas-product-img {
  width: 48px;
  height: 48px;
  object-fit: cover;
  border-radius: 4px;
  flex-shrink: 0;
}
.mas-product-info {
  min-width: 0;
}
.mas-product-name {
  font-size: 13px;
  color: #333;
  margin-bottom: 4px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.mas-product-spec {
  font-size: 12px;
  color: #999;
}
.mas-price {
  color: #e4393c;
  font-weight: 600;
}
.mas-pager {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
