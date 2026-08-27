<template>
  <div class="shop-manage">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" size="default">
        <el-form-item label="店铺名称">
          <el-input v-model="searchForm.shopName" placeholder="请输入店铺名称" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width:130px">
            <el-option label="营业" :value="1" />
            <el-option label="关闭" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">搜索</el-button>
          <el-button @click="resetSearch">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card style="margin-top:16px">
      <el-table :data="tableData" border stripe v-loading="loading" @selection-change="handleSelectionChange">
        <el-table-column type="selection" width="50" />
        <el-table-column prop="id" label="店铺ID" width="80" />
        <el-table-column prop="merchantId" label="商家ID" width="80" />
        <el-table-column prop="merchantName" label="所属商家" min-width="130" />
        <el-table-column prop="shopName" label="店铺名称" min-width="160" />
        <el-table-column label="Logo" width="70" align="center">
          <template #default="{ row }">
            <el-image v-if="row.shopLogo" :src="row.shopLogo" style="width:36px;height:36px;border-radius:4px" fit="cover" />
            <el-text v-else type="info">-</el-text>
          </template>
        </el-table-column>
        <el-table-column prop="intro" label="简介" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '营业' : '关闭' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" type="warning" size="small" @click="handleToggleStatus(row, 0)">关闭</el-button>
            <el-button v-else type="success" size="small" @click="handleToggleStatus(row, 1)">营业</el-button>
            <el-button type="primary" size="small" @click="handleViewProducts(row)">查看商品</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="current"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        style="margin-top:16px;justify-content:flex-end"
        @change="fetchData"
      />
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getShopsPage, updateShopStatus, type ShopDTO } from '../../api/shop'

const router = useRouter()
const loading = ref(false)
const tableData = ref<ShopDTO[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const selectedIds = ref<number[]>([])

const searchForm = ref({ shopName: '', status: undefined as number | undefined })

function handleSelectionChange(rows: ShopDTO[]) {
  selectedIds.value = rows.map(r => r.id)
}

function resetSearch() {
  searchForm.value = { shopName: '', status: undefined }
  current.value = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await getShopsPage({
      current: current.value,
      size: size.value,
      shopName: searchForm.value.shopName || undefined,
      status: searchForm.value.status,
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e: any) {
    ElMessage.error(e.message || '查询失败')
  } finally {
    loading.value = false
  }
}

async function handleToggleStatus(row: ShopDTO, status: number) {
  const label = status === 1 ? '营业' : '关闭'
  try {
    await ElMessageBox.confirm(`确定${label}店铺「${row.shopName}」吗？`, '提示', { type: 'warning' })
    await updateShopStatus(row.id, status)
    ElMessage.success(`${label}成功`)
    fetchData()
  } catch {}
}

function handleViewProducts(row: ShopDTO) {
  router.push({ path: '/admin/product', query: { shopId: String(row.id), shopName: row.shopName } })
}

onMounted(fetchData)
</script>