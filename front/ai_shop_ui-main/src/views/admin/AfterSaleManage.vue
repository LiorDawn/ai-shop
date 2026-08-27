<template>
  <div class="after-sale-manage">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="订单号">
          <el-input v-model="searchForm.orderNo" placeholder="请输入订单号" clearable style="width:180px" />
        </el-form-item>
        <el-form-item label="售后状态">
          <el-select v-model="searchForm.auditStatus" placeholder="全部" clearable style="width:130px">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
            <el-option label="待退货" :value="3" />
            <el-option label="已完成" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="申请时间">
          <el-date-picker
            v-model="searchForm.startTime"
            type="datetime"
            placeholder="开始时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:170px"
          />
          <span style="margin:0 6px">~</span>
          <el-date-picker
            v-model="searchForm.endTime"
            type="datetime"
            placeholder="结束时间"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width:170px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="售后单号" width="100" />
        <el-table-column prop="orderNo" label="关联订单号" width="180" />
        <el-table-column label="用户信息" width="120">
          <template #default="{ row }">{{ row.username }}</template>
        </el-table-column>
        <el-table-column label="售后类型" width="100">
          <template #default="{ row }">{{ row.typeText }}</template>
        </el-table-column>
        <el-table-column label="申请金额" width="110">
          <template #default="{ row }">¥{{ row.amount }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.auditStatus)" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleDetail(row)">查看</el-button>
            <el-button
              v-if="row.auditStatus === 0"
              type="warning"
              link
              size="small"
              @click="handleAudit(row)"
            >审核</el-button>
            <el-button
              v-if="row.auditStatus === 1 || row.auditStatus === 3"
              type="success"
              link
              size="small"
              @click="handleFinish(row)"
            >标记完成</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="current"
          v-model:page-size="size"
          :page-sizes="[10, 20, 50]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="fetchData"
          @current-change="fetchData"
        />
      </div>
    </el-card>

    <!-- 详情弹窗 -->
    <el-dialog v-model="detailVisible" title="售后详情" width="750px">
      <template v-if="detailData">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="售后单号" :span="2">{{ detailData.id }}</el-descriptions-item>
          <el-descriptions-item label="关联订单号">{{ detailData.orderNo }}</el-descriptions-item>
          <el-descriptions-item label="下单用户">{{ detailData.username }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ detailData.orderCreateTime }}</el-descriptions-item>
          <el-descriptions-item label="订单总金额">¥{{ detailData.orderTotalPrice }}</el-descriptions-item>
          <el-descriptions-item label="售后类型">{{ detailData.typeText }}</el-descriptions-item>
          <el-descriptions-item label="申请金额">¥{{ detailData.amount }}</el-descriptions-item>
          <el-descriptions-item label="售后原因" :span="2">{{ detailData.reason }}</el-descriptions-item>
          <el-descriptions-item label="问题描述" :span="2">{{ detailData.description || '无' }}</el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ detailData.createTime }}</el-descriptions-item>
          <el-descriptions-item label="当前状态">
            <el-tag :type="statusType(detailData.auditStatus)" size="small">{{ detailData.statusText }}</el-tag>
          </el-descriptions-item>
          <el-descriptions-item v-if="detailData.auditBy" label="审核人">{{ detailData.auditBy }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.auditTime" label="审核时间">{{ detailData.auditTime }}</el-descriptions-item>
          <el-descriptions-item v-if="detailData.auditRemark" label="审核备注" :span="2">{{ detailData.auditRemark }}</el-descriptions-item>
        </el-descriptions>

        <h4 style="margin:16px 0 8px">商品明细</h4>
        <el-table :data="detailData.items" stripe size="small">
          <el-table-column label="商品名称" min-width="160">
            <template #default="{ row }">
              <div style="display:flex;align-items:center;gap:8px">
                <el-image :src="row.productImage" style="width:40px;height:40px" fit="cover" />
                <span>{{ row.productName }}</span>
              </div>
            </template>
          </el-table-column>
          <el-table-column prop="spec" label="规格" width="100" />
          <el-table-column label="单价" width="100">
            <template #default="{ row }">¥{{ row.price }}</template>
          </el-table-column>
          <el-table-column prop="num" label="数量" width="80" />
        </el-table>
      </template>
      <div v-else style="text-align:center;padding:40px;color:#999">暂无数据</div>
      <template #footer>
        <el-button v-if="detailData && detailData.auditStatus === 0" type="warning" @click="handleAuditFromDetail">审核</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" title="审核售后单" width="500px">
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="100px">
        <el-form-item label="售后单号">{{ auditingRow?.id }}</el-form-item>
        <el-form-item label="订单号">{{ auditingRow?.orderNo }}</el-form-item>
        <el-form-item label="申请金额">¥{{ auditingRow?.amount }}</el-form-item>
        <el-form-item label="审核结果" prop="auditStatus">
          <el-radio-group v-model="auditForm.auditStatus">
            <el-radio :value="1">通过</el-radio>
            <el-radio :value="2">驳回</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="审核备注" prop="auditRemark">
          <el-input v-model="auditForm.auditRemark" type="textarea" :rows="3" placeholder="请输入审核备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="primary" :loading="auditing" @click="submitAudit">提交</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getAfterSalePage,
  getAfterSaleDetail,
  adminAuditAfterSale,
  finishAfterSale,
  type AfterSaleDTO,
  type AfterSaleDetailDTO,
} from '../../api/afterSale'

const loading = ref(false)
const tableData = ref<AfterSaleDTO[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)

const searchForm = reactive({
  orderNo: '',
  auditStatus: undefined as number | undefined,
  startTime: undefined as string | undefined,
  endTime: undefined as string | undefined,
})

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const params: any = { current: current.value, size: size.value }
    if (searchForm.orderNo) params.orderNo = searchForm.orderNo
    if (searchForm.auditStatus !== undefined) params.auditStatus = searchForm.auditStatus
    if (searchForm.startTime) params.startTime = searchForm.startTime
    if (searchForm.endTime) params.endTime = searchForm.endTime
    const res: any = await getAfterSalePage(params)
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
  searchForm.auditStatus = undefined
  searchForm.startTime = undefined
  searchForm.endTime = undefined
  current.value = 1
  fetchData()
}

function statusType(st: number) {
  if (st === 0) return 'warning'
  if (st === 1 || st === 4) return 'success'
  if (st === 2) return 'danger'
  return 'info'
}

// 详情
const detailVisible = ref(false)
const detailData = ref<AfterSaleDetailDTO | null>(null)

async function handleDetail(row: AfterSaleDTO) {
  detailVisible.value = true
  detailData.value = null
  try {
    const res: any = await getAfterSaleDetail(row.id)
    detailData.value = res.data
  } catch (e: any) {
    ElMessage.error(e.message || '查询失败')
  }
}

// 审核
const auditVisible = ref(false)
const auditing = ref(false)
const auditingRow = ref<AfterSaleDTO | null>(null)
const auditFormRef = ref<FormInstance>()
const auditForm = reactive({
  auditStatus: 1,
  auditRemark: '',
})

const auditRules: FormRules = {
  auditStatus: [{ required: true, message: '请选择审核结果' }],
  auditRemark: [{ required: true, message: '请输入审核备注', trigger: 'blur' }],
}

function handleAudit(row: AfterSaleDTO) {
  auditingRow.value = row
  auditForm.auditStatus = 1
  auditForm.auditRemark = ''
  auditVisible.value = true
}

function handleAuditFromDetail() {
  if (detailData.value) {
    auditingRow.value = {
      id: detailData.value.id,
      orderNo: detailData.value.orderNo,
      amount: detailData.value.amount,
    } as AfterSaleDTO
    auditForm.auditStatus = 1
    auditForm.auditRemark = ''
    auditVisible.value = true
    detailVisible.value = false
  }
}

async function submitAudit() {
  const valid = await auditFormRef.value?.validate().catch(() => false)
  if (!valid || !auditingRow.value) return
  auditing.value = true
  try {
    await adminAuditAfterSale({
      id: auditingRow.value.id,
      auditStatus: auditForm.auditStatus,
      auditRemark: auditForm.auditRemark,
    })
    ElMessage.success('审核完成')
    auditVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e.message || '审核失败')
  } finally {
    auditing.value = false
  }
}

// 标记完成
async function handleFinish(row: AfterSaleDTO) {
  ElMessageBox.confirm(`确定标记售后单 #${row.id} 为已完成？`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await finishAfterSale(row.id)
        ElMessage.success('操作成功')
        fetchData()
      } catch (e: any) {
        ElMessage.error(e.message || '操作失败')
      }
    })
    .catch(() => {})
}
</script>

<style scoped>
.after-sale-manage {
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