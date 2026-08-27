<template>
  <div class="product-manage">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" size="default">
        <el-form-item label="商品名称">
          <el-input v-model="searchForm.name" placeholder="请输入商品名称" clearable />
        </el-form-item>
        <el-form-item label="店铺">
          <el-input v-model="searchForm.shopName" placeholder="店铺名称" disabled style="width:150px" v-if="searchForm.shopId" />
          <el-input v-model="searchForm.shopName" placeholder="所有店铺" disabled style="width:150px" v-else />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width:130px">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
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
        <el-table-column prop="id" label="商品ID" width="80" />
        <el-table-column prop="shopName" label="所属店铺" min-width="130" />
        <el-table-column prop="name" label="商品名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="categoryName" label="分类" width="120" show-overflow-tooltip />
        <el-table-column prop="price" label="价格" width="100" align="right">
          <template #default="{ row }">
            ¥{{ row.price?.toFixed(2) }}
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" align="center" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" type="success" size="small" @click="handleUp(row)">上架</el-button>
            <el-button v-else type="warning" size="small" @click="handleDown(row)">下架</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 批量操作 -->
      <div style="margin-top:12px;display:flex;gap:8px">
        <el-button :disabled="selectedIds.length === 0" type="success" size="small" @click="handleBatchUp">
          批量上架 ({{ selectedIds.length }})
        </el-button>
        <el-button :disabled="selectedIds.length === 0" type="warning" size="small" @click="handleBatchDown">
          批量下架 ({{ selectedIds.length }})
        </el-button>
        <el-button :disabled="selectedIds.length === 0" type="danger" size="small" @click="handleBatchDelete">
          批量删除 ({{ selectedIds.length }})
        </el-button>
      </div>

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
import { ref, onMounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getProductsPage, upProduct, downProduct, deleteProduct, deleteBatchProducts, type ProductDTO } from '../../api/product'

const route = useRoute()
const loading = ref(false)
const tableData = ref<ProductDTO[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const selectedIds = ref<number[]>([])

const searchForm = ref({
  name: '',
  shopId: undefined as number | undefined,
  shopName: '',
  status: undefined as number | undefined,
})

// 监听路由参数变化（从店铺管理跳转过来时 shopId 会变化）
watch(() => route.query.shopId, (newShopId) => {
  if (newShopId) {
    searchForm.value.shopId = Number(newShopId)
    searchForm.value.shopName = (route.query.shopName as string) || '指定店铺'
  } else {
    searchForm.value.shopId = undefined
    searchForm.value.shopName = ''
  }
  current.value = 1
  fetchData()
})

onMounted(() => {
  // 初始加载时如果有 shopId 参数，直接使用
  if (route.query.shopId) {
    searchForm.value.shopId = Number(route.query.shopId)
    searchForm.value.shopName = (route.query.shopName as string) || '指定店铺'
  }
  fetchData()
})

function handleSelectionChange(rows: ProductDTO[]) {
  selectedIds.value = rows.map(r => r.id)
}

function resetSearch() {
  searchForm.value.name = ''
  // 不清除 shopId（可能来自跳转过滤）
  searchForm.value.status = undefined
  if (!route.query.shopId) {
    searchForm.value.shopId = undefined
    searchForm.value.shopName = ''
  }
  current.value = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await getProductsPage({
      current: current.value,
      size: size.value,
      name: searchForm.value.name || undefined,
      shopId: searchForm.value.shopId || undefined,
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

async function handleUp(row: ProductDTO) {
  await upProduct(row.id)
  ElMessage.success('上架成功')
  fetchData()
}

async function handleDown(row: ProductDTO) {
  await downProduct(row.id)
  ElMessage.success('下架成功')
  fetchData()
}

async function handleDelete(row: ProductDTO) {
  try {
    await ElMessageBox.confirm(`确定删除商品「${row.name}」吗？`, '提示', { type: 'warning' })
    await deleteProduct(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {}
}

async function handleBatchUp() {
  try {
    await ElMessageBox.confirm(`确定上架选中的 ${selectedIds.value.length} 个商品吗？`, '提示', { type: 'warning' })
    for (const id of selectedIds.value) await upProduct(id)
    ElMessage.success('批量上架成功')
    selectedIds.value = []
    fetchData()
  } catch {}
}

async function handleBatchDown() {
  try {
    await ElMessageBox.confirm(`确定下架选中的 ${selectedIds.value.length} 个商品吗？`, '提示', { type: 'warning' })
    for (const id of selectedIds.value) await downProduct(id)
    ElMessage.success('批量下架成功')
    selectedIds.value = []
    fetchData()
  } catch {}
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个商品吗？`, '提示', { type: 'warning' })
    await deleteBatchProducts(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    fetchData()
  } catch {}
}

</script>