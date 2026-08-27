<template>
  <div class="mol-page">
    <div class="mol-filter">
      <el-input v-model="filter.orderNo" placeholder="订单号" clearable style="width:200px" @keyup.enter="fetchList" />
      <el-select v-model="filter.orderStatus" placeholder="订单状态" clearable style="width:130px" @change="fetchList">
        <el-option label="待付款" :value="0" />
        <el-option label="待发货" :value="1" />
        <el-option label="待收货" :value="2" />
        <el-option label="已完成" :value="3" />
        <el-option label="已取消" :value="4" />
      </el-select>
      <el-button type="primary" @click="fetchList">搜索</el-button>
    </div>

    <el-card class="mol-table-card">
      <el-table :data="list" stripe border style="width:100%" v-loading="loading">
        <el-table-column label="订单号" width="180">
          <template #default="{ row }">
            <span class="mol-order-no">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="买家" width="120" prop="username" />
        <el-table-column label="实付金额" width="120">
          <template #default="{ row }">¥{{ fmtPrice(row.actualPrice) }}</template>
        </el-table-column>
        <el-table-column label="订单状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusType(row.orderStatus)" size="small">
              {{ statusText(row.orderStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="下单时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="viewDetail(row)">详情</el-button>
            <el-button
              v-if="row.orderStatus === 1"
              size="small"
              type="success"
              @click="handleDeliver(row)"
            >发货</el-button>
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
        class="mol-pager"
      />
    </el-card>

    <!-- 发货弹窗 -->
    <el-dialog v-model="deliverVisible" title="发货" width="400px">
      <el-form :model="deliverForm" label-width="100px">
        <el-form-item label="快递公司">
          <el-select v-model="deliverForm.company" style="width:100%">
            <el-option label="顺丰速运" value="顺丰速运" />
            <el-option label="中通快递" value="中通快递" />
            <el-option label="圆通速递" value="圆通速递" />
            <el-option label="韵达快递" value="韵达快递" />
            <el-option label="极兔速递" value="极兔速递" />
            <el-option label="邮政EMS" value="邮政EMS" />
          </el-select>
        </el-form-item>
        <el-form-item label="物流单号">
          <el-input v-model="deliverForm.trackingNo" placeholder="请输入物流单号" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="deliverVisible = false">取消</el-button>
        <el-button type="primary" :loading="delivering" @click="confirmDeliver">确认发货</el-button>
      </template>
    </el-dialog>

    <!-- 订单详情弹窗 -->
    <el-dialog v-model="detailVisible" title="订单详情" width="800px" :close-on-click-modal="false">
      <div v-if="orderDetail" v-loading="detailLoading">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="订单号">{{ orderDetail.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="订单状态">
            <el-tag :type="statusType(orderDetail.orderStatus)" size="small">
              {{ statusText(orderDetail.orderStatus) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="买家">{{ orderDetail.username }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ orderDetail.createTime }}</el-descriptions-item>
          <el-descriptions-item label="实付金额">¥{{ fmtPrice(orderDetail.actualPrice) }}</el-descriptions-item>
          <el-descriptions-item label="物流信息">{{ orderDetail.logistics || '暂无' }}</el-descriptions-item>
          <el-descriptions-item label="收货人">{{ orderDetail.receiver }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ orderDetail.receiverPhone }}</el-descriptions-item>
          <el-descriptions-item label="收货地址" :span="2">{{ orderDetail.address }}</el-descriptions-item>
        </el-descriptions>

        <el-divider />

        <div class="mol-detail-items">
          <div class="mol-detail-items-title">商品明细</div>
          <div v-for="item in orderDetail.items" :key="item.id" class="mol-detail-item">
            <img :src="item.productImage || ''" class="mol-detail-img" />
            <div class="mol-detail-info">
              <div class="mol-detail-name">{{ item.productName }}</div>
              <div class="mol-detail-spec">{{ item.spec || '默认规格' }}</div>
              <div class="mol-detail-meta">¥{{ fmtPrice(item.price) }} × {{ item.num }}</div>
            </div>
          </div>
        </div>

        <!-- 订单备注 -->
        <el-divider />
        <div class="mol-remark">
          <div class="mol-remark-title">订单备注（仅商家可见）</div>
          <el-input
            v-model="remarkText"
            type="textarea"
            :rows="2"
            placeholder="添加内部备注"
            style="margin-bottom: 8px"
          />
          <el-button size="small" type="primary" @click="saveRemark">保存备注</el-button>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getOrdersPage, getOrderDetail, deliverOrder, type OrderDTO } from '@/api/order'
import request from '@/api/request'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const filter = reactive({
  orderNo: '',
  orderStatus: null as number | null,
})

function statusType(s: number) {
  const map: Record<number, string> = { 0: 'primary', 1: 'warning', 2: '', 3: 'success', 4: 'info' }
  return map[s] || 'info'
}

function statusText(s: number) {
  const map: Record<number, string> = { 0: '待付款', 1: '待发货', 2: '待收货', 3: '已完成', 4: '已取消' }
  return map[s] || '未知'
}

function fmtPrice(v: any) {
  const n = Number(v)
  return isNaN(n) ? '0.00' : n.toFixed(2)
}

async function fetchList() {
  loading.value = true
  try {
    const res = await request.get('/merchant/order/page', {
      params: {
        current: currentPage.value,
        size: pageSize.value,
        orderNo: filter.orderNo || undefined,
        orderStatus: filter.orderStatus ?? undefined,
      },
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { list.value = []; total.value = 0 }
  finally { loading.value = false }
}

// 详情
const detailVisible = ref(false)
const detailLoading = ref(false)
const orderDetail = ref<any>(null)
const remarkText = ref('')

async function viewDetail(row: any) {
  detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await request.get(`/merchant/order/${row.id}`)
    orderDetail.value = res.data
    remarkText.value = res.data?.remark || ''
  } catch { ElMessage.error('加载订单详情失败') }
  finally { detailLoading.value = false }
}

async function saveRemark() {
  if (!orderDetail.value?.id) return
  try {
    await request.put(`/merchant/order/remark/${orderDetail.value.id}`, { remark: remarkText.value })
    ElMessage.success('备注已保存')
  } catch { ElMessage.error('保存失败') }
}

// 发货
const deliverVisible = ref(false)
const delivering = ref(false)
const deliverForm = reactive({ company: '顺丰速运', trackingNo: '' })
let deliverOrderId = 0

function handleDeliver(row: any) {
  deliverOrderId = row.id
  deliverForm.company = '顺丰速运'
  deliverForm.trackingNo = ''
  deliverVisible.value = true
}

async function confirmDeliver() {
  if (!deliverForm.trackingNo.trim()) {
    ElMessage.warning('请输入物流单号')
    return
  }
  const logistics = `${deliverForm.company}:${deliverForm.trackingNo}`
  delivering.value = true
  try {
    await request.put(`/merchant/order/deliver/${deliverOrderId}`, { logistics })
    ElMessage.success('发货成功')
    deliverVisible.value = false
    fetchList()
  } catch (e: any) { ElMessage.error(e?.message || '发货失败') }
  finally { delivering.value = false }
}

onMounted(() => { fetchList() })
</script>

<style scoped>
.mol-page { min-height: 100%; }
.mol-filter { margin-bottom: 12px; display: flex; gap: 10px; }
.mol-table-card { min-height: 300px; }
.mol-pager { margin-top: 16px; justify-content: flex-end; }
.mol-order-no { font-family: monospace; font-size: 13px; color: #409eff; }
.mol-detail-items-title { font-weight: 600; font-size: 14px; margin-bottom: 12px; color: #333; }
.mol-detail-item { display: flex; align-items: center; gap: 12px; padding: 8px 0; border-bottom: 1px solid #f0f0f0; }
.mol-detail-item:last-child { border-bottom: none; }
.mol-detail-img { width: 56px; height: 56px; object-fit: cover; border-radius: 4px; }
.mol-detail-name { font-size: 14px; color: #333; }
.mol-detail-spec { font-size: 12px; color: #999; }
.mol-detail-meta { font-size: 13px; color: #e4393c; margin-top: 4px; }
.mol-remark-title { font-weight: 600; font-size: 14px; margin-bottom: 8px; color: #333; }
</style>