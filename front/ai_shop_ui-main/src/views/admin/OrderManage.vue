<template>
  <div class="order-manage">
    <!-- 搜索筛选栏 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="订单编号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单编号" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="订单状态">
          <el-select v-model="searchForm.orderStatus" placeholder="全部" clearable style="width:140px">
            <el-option label="待付款" :value="0" />
            <el-option label="待发货" :value="1" />
            <el-option label="待收货" :value="2" />
            <el-option label="已完成" :value="3" />
            <el-option label="已取消" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="店铺">
          <el-select v-model="searchForm.shopId" placeholder="全部店铺" clearable style="width:160px">
            <el-option v-for="shop in shopOptions" :key="shop.id" :label="shop.shopName" :value="shop.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="下单时间">
          <el-date-picker
            v-model="timeRange"
            type="datetimerange"
            range-separator="至"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 数据表格 -->
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="orderNo" label="订单编号" min-width="160" />
        <el-table-column prop="username" label="下单用户" width="120" />
        <el-table-column prop="shopName" label="所属店铺" min-width="140" />
        <el-table-column label="订单金额" width="180">
          <template #default="{ row }">
            <div>
              <span style="color:#f56c6c;font-weight:bold;font-size:16px">¥{{ row.actualPrice }}</span>
              <span v-if="row.couponPrice > 0" style="color:#999;font-size:12px;margin-left:4px">
                <del>¥{{ row.totalPrice }}</del>
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="订单状态" width="100">
          <template #default="{ row }">
            <el-tag :type="orderStatusTag(row.orderStatus)" size="small">
              {{ orderStatusText(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.payStatus === 1 ? 'success' : 'info'" size="small">
              {{ row.payStatus === 1 ? '已支付' : '未支付' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170" />
        <el-table-column label="操作" width="280" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleViewDetail(row)">查看详情</el-button>
            <el-button
              v-if="row.orderStatus === 1"
              type="success"
              link
              size="small"
              @click="handleDeliver(row)"
            >发货</el-button>
            <el-button
              v-if="row.orderStatus === 0"
              type="warning"
              link
              size="small"
              @click="handleCancel(row)"
            >取消订单</el-button>
            <el-button
              v-if="row.orderStatus === 2"
              type="primary"
              link
              size="small"
              @click="handleComplete(row)"
            >完成订单</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50, 100]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="800px" top="5vh">
      <template v-if="detailData">
        <!-- 订单基本信息 -->
        <el-descriptions title="基本信息" :column="2" border size="small">
          <el-descriptions-item label="订单编号">{{ detailData.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="下单用户">{{ detailData.username }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ detailData.createTime }}</el-descriptions-item>
          <el-descriptions-item label="支付方式">{{ detailData.payType === 1 ? '支付宝' : '其他' }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">
            <el-tag :type="detailData.payStatus === 1 ? 'success' : 'info'" size="small">
              {{ detailData.payStatus === 1 ? '已支付' : '未支付' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="orderStatusTag(detailData.orderStatus)" size="small">
              {{ orderStatusText(detailData.orderStatus) }}
            </el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 收货地址 -->
        <el-descriptions title="收货地址" :column="1" border size="small" style="margin-top:16px">
          <el-descriptions-item label="收件人">{{ detailData.receiver || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detailData.receiverPhone || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="详细地址">{{ detailData.address || '暂无' }}</el-descriptions-item>
        </el-descriptions>

        <!-- 物流信息 -->
        <el-descriptions title="物流信息" :column="1" border size="small" style="margin-top:16px">
          <el-descriptions-item label="物流单号">
            <template v-if="detailData.orderStatus === 1">
              <el-input v-model="logisticsForm.logistics" placeholder="请输入物流单号" size="small" style="width:260px" />
              <el-button type="primary" size="small" style="margin-left:8px" @click="submitDeliver(detailData.id)">确认发货</el-button>
            </template>
            <template v-else>
              {{ detailData.logistics || '未发货' }}
            </template>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 金额信息 -->
        <el-descriptions title="金额信息" :column="3" border size="small" style="margin-top:16px">
          <el-descriptions-item label="商品原价">¥{{ detailData.totalPrice }}</el-descriptions-item>
          <el-descriptions-item label="优惠券抵扣">¥{{ detailData.couponPrice }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">
            <span style="color:#f56c6c;font-weight:bold">¥{{ detailData.actualPrice }}</span>
          </el-descriptions-item>
        </el-descriptions>

        <!-- 商品明细 -->
        <div style="margin-top:16px">
          <div style="font-size:14px;font-weight:bold;margin-bottom:8px">商品明细</div>
          <el-table :data="detailData.items" border size="small">
            <el-table-column label="商品" min-width="200">
              <template #default="{ row }">
                <div style="display:flex;align-items:center;gap:8px">
                  <el-image v-if="row.productImage" :src="row.productImage" style="width:50px;height:50px;border-radius:4px" fit="cover" />
                  <span>{{ row.productName }}</span>
                </div>
              </template>
            </el-table-column>
            <el-table-column prop="spec" label="规格" width="100" />
            <el-table-column label="单价" width="100">
              <template #default="{ row }">¥{{ row.price }}</template>
            </el-table-column>
            <el-table-column prop="num" label="数量" width="70" />
            <el-table-column label="小计" width="100">
              <template #default="{ row }">
                <span style="color:#f56c6c">¥{{ row.subtotal }}</span>
              </template>
            </el-table-column>
            <el-table-column prop="shopName" label="所属店铺" width="120" />
          </el-table>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getOrdersPage,
  getOrderDetail,
  deliverOrder,
  cancelOrder,
  completeOrder,
  type OrderDTO,
  type OrderDetailDTO,
} from '../../api/order'
import { listShops, type ShopDTO } from '../../api/shop'

// 订单状态映射
const orderStatusMap: Record<number, string> = {
  0: '待付款',
  1: '待发货',
  2: '待收货',
  3: '已完成',
  4: '已取消',
}

function orderStatusText(status: number): string {
  return orderStatusMap[status] || '未知'
}

function orderStatusTag(status: number): string {
  const map: Record<number, string> = { 0: 'warning', 1: 'danger', 2: 'primary', 3: 'success', 4: 'info' }
  return map[status] || 'info'
}

const loading = ref(false)
const tableData = ref<OrderDTO[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)

// 店铺选项
const shopOptions = ref<ShopDTO[]>([])

// 搜索表单
const searchForm = reactive({
  orderNo: '',
  orderStatus: undefined as number | undefined,
  shopId: undefined as number | undefined,
})
const timeRange = ref<string[] | null>(null)

// 详情弹窗
const detailVisible = ref(false)
const detailData = ref<OrderDetailDTO | null>(null)
const logisticsForm = reactive({ logistics: '' })

onMounted(async () => {
  try {
    const res: any = await listShops()
    shopOptions.value = res.data
  } catch {}
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const params: any = {
      current: current.value,
      size: size.value,
    }
    if (searchForm.orderNo) params.orderNo = searchForm.orderNo
    if (searchForm.orderStatus !== undefined) params.orderStatus = searchForm.orderStatus
    if (searchForm.shopId !== undefined) params.shopId = searchForm.shopId
    if (timeRange.value && timeRange.value.length === 2) {
      params.startTime = timeRange.value[0]
      params.endTime = timeRange.value[1]
    }
    const res: any = await getOrdersPage(params)
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (e: any) {
    ElMessage.error(e.message || '查询失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  current.value = 1
  fetchData()
}

function handleReset() {
  searchForm.orderNo = ''
  searchForm.orderStatus = undefined
  searchForm.shopId = undefined
  timeRange.value = null
  current.value = 1
  fetchData()
}

// 查看详情
async function handleViewDetail(row: OrderDTO) {
  logisticsForm.logistics = ''
  try {
    const res: any = await getOrderDetail(row.id)
    detailData.value = res.data
    if (res.data.logistics) {
      logisticsForm.logistics = res.data.logistics
    }
    detailVisible.value = true
  } catch (e: any) {
    ElMessage.error(e.message || '查询失败')
  }
}

// 在详情弹窗中确认发货
async function submitDeliver(orderId: number) {
  if (!logisticsForm.logistics.trim()) {
    ElMessage.warning('请输入物流单号')
    return
  }
  try {
    await deliverOrder(orderId, logisticsForm.logistics.trim())
    ElMessage.success('发货成功')
    detailVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e.message || '发货失败')
  }
}

// 表格中直接发货
function handleDeliver(row: OrderDTO) {
  ElMessageBox.prompt('请输入物流单号', '发货', {
    inputPlaceholder: '请输入物流单号',
    confirmButtonText: '确认发货',
    cancelButtonText: '取消',
  }).then(async ({ value }) => {
    if (!value || !value.trim()) {
      ElMessage.warning('请输入物流单号')
      return
    }
    try {
      await deliverOrder(row.id, value.trim())
      ElMessage.success('发货成功')
      fetchData()
    } catch (e: any) {
      ElMessage.error(e.message || '发货失败')
    }
  }).catch(() => {})
}

// 取消订单
function handleCancel(row: OrderDTO) {
  ElMessageBox.confirm(`确定取消订单「${row.orderNo}」吗？`, '提示', {
    type: 'warning',
  }).then(async () => {
    try {
      await cancelOrder(row.id)
      ElMessage.success('取消成功')
      fetchData()
    } catch (e: any) {
      ElMessage.error(e.message || '取消失败')
    }
  }).catch(() => {})
}

// 完成订单
function handleComplete(row: OrderDTO) {
  ElMessageBox.confirm(`确定将订单「${row.orderNo}」标记为已完成吗？`, '提示', {
    type: 'warning',
  }).then(async () => {
    try {
      await completeOrder(row.id)
      ElMessage.success('操作成功')
      fetchData()
    } catch (e: any) {
      ElMessage.error(e.message || '操作失败')
    }
  }).catch(() => {})
}
</script>

<style scoped>
.order-manage {
  max-width: 1400px;
  margin: 0 auto;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  margin-bottom: 16px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>