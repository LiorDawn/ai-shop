<template>
  <div class="merchant-manage">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :inline="true" :model="searchForm" size="default">
        <el-form-item label="商家名称">
          <el-input v-model="searchForm.merchantName" placeholder="请输入商家名称" clearable />
        </el-form-item>
        <el-form-item label="审核状态">
          <el-select v-model="searchForm.status" placeholder="全部状态" clearable style="width:140px">
            <el-option label="待审核" :value="0" />
            <el-option label="已通过" :value="1" />
            <el-option label="已驳回" :value="2" />
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
        <el-table-column prop="id" label="商家ID" width="80" />
        <el-table-column prop="merchantName" label="商家名称" min-width="140" />
        <el-table-column label="营业执照" min-width="160">
          <template #default="{ row }">
            <el-tag v-if="!row.licenseNo" type="info" size="small">未提交</el-tag>
            <el-text v-else truncated>{{ row.licenseNo }}</el-text>
          </template>
        </el-table-column>
        <el-table-column prop="contact" label="联系人" width="120" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="审核状态" width="110">
          <template #default="{ row }">
            <el-tag v-if="row.auditStatus === 0" type="warning">待审核</el-tag>
            <el-tag v-else-if="row.auditStatus === 1" type="success">已通过</el-tag>
            <el-tag v-else-if="row.auditStatus === 2" type="danger">已驳回</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="操作" width="250" fixed="right">
          <template #default="{ row }">
            <!-- 待审核：显示审核按钮 -->
            <el-button v-if="row.auditStatus === 0" type="primary" size="small" @click="openAuditDialog(row)">
              审核
            </el-button>
            <!-- 已通过/已驳回：查看详情 -->
            <el-button v-else type="info" size="small" @click="openAuditDialog(row)">
              查看
            </el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- 批量删除 -->
      <div style="margin-top:12px">
        <el-button :disabled="selectedIds.length === 0" type="danger" size="small" @click="handleBatchDelete">
          批量删除 ({{ selectedIds.length }})
        </el-button>
      </div>

      <!-- 分页 -->
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

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" :title="isViewMode ? '申请详情' : '商家审核'" width="560px">
      <el-form label-width="110px">
        <!-- 基础信息（只读） -->
        <el-form-item label="商家名称">
          <span>{{ auditingRow?.merchantName }}</span>
        </el-form-item>
        <el-form-item label="营业执照">
          <span>{{ auditingRow?.licenseNo || '未提交' }}</span>
        </el-form-item>
        <el-form-item label="联系人">
          <span>{{ auditingRow?.contact }}</span>
        </el-form-item>
        <el-form-item label="联系电话">
          <span>{{ auditingRow?.phone }}</span>
        </el-form-item>
        <el-form-item v-if="auditingRow?.auditTime" label="审核时间">
          <span>{{ auditingRow?.auditTime }}</span>
        </el-form-item>
        <el-form-item v-if="auditingRow?.auditRemark" label="审核备注">
          <span>{{ auditingRow?.auditRemark }}</span>
        </el-form-item>
        <el-divider v-if="!isViewMode" />
        <!-- 审核操作（仅待审核状态显示） -->
        <template v-if="!isViewMode">
          <el-form-item label="审核结果" prop="auditStatus">
            <el-radio-group v-model="auditForm.auditStatus">
              <el-radio :value="1">通过</el-radio>
              <el-radio :value="2">驳回</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="审核备注" prop="auditRemark">
            <el-input
              v-model="auditForm.auditRemark"
              type="textarea"
              :rows="3"
              placeholder="请输入审核备注（必填）"
            />
          </el-form-item>
        </template>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button v-if="!isViewMode" type="primary" :loading="auditing" @click="submitAudit">提交审核</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMerchantsPage, auditMerchant, deleteMerchant, deleteBatchMerchants, type MerchantDTO } from '../../api/merchant'

const loading = ref(false)
const tableData = ref<MerchantDTO[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)
const selectedIds = ref<number[]>([])

const searchForm = ref({ merchantName: '', status: undefined as number | undefined })

function handleSelectionChange(rows: MerchantDTO[]) {
  selectedIds.value = rows.map(r => r.id)
}

function resetSearch() {
  searchForm.value = { merchantName: '', status: undefined }
  current.value = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await getMerchantsPage({
      current: current.value,
      size: size.value,
      merchantName: searchForm.value.merchantName || undefined,
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

// ===== 审核弹窗 =====
const auditVisible = ref(false)
const auditing = ref(false)
const auditingRow = ref<MerchantDTO | null>(null)
const isViewMode = computed(() => auditingRow.value?.auditStatus !== 0)
const auditForm = reactive({
  auditStatus: 1,
  auditRemark: '',
})

function openAuditDialog(row: MerchantDTO) {
  
  auditingRow.value = row
  if (row.auditStatus === 0) {
    auditForm.auditStatus = 1
  } else {
    auditForm.auditStatus = row.auditStatus
  }
  auditForm.auditRemark = ''
  auditVisible.value = true
}

async function submitAudit() {
  if (!auditingRow.value) return
  if (!auditForm.auditRemark.trim()) {
    ElMessage.warning('请输入审核备注')
    return
  }
  auditing.value = true
  try {
    await auditMerchant(auditingRow.value.id, auditForm.auditStatus, auditForm.auditRemark)
    const action = auditForm.auditStatus === 1 ? '审核通过' : '已驳回'
    ElMessage.success(`${action}成功`)
    auditVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    auditing.value = false
  }
}

// ===== 删除 =====
async function handleDelete(row: MerchantDTO) {
  try {
    await ElMessageBox.confirm(`确定删除商家「${row.merchantName}」吗？`, '提示', { type: 'warning' })
    await deleteMerchant(row.id)
    ElMessage.success('删除成功')
    fetchData()
  } catch {}
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selectedIds.value.length} 个商家吗？`, '提示', { type: 'warning' })
    await deleteBatchMerchants(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    fetchData()
  } catch {}
}

onMounted(fetchData)
</script>

<style scoped>
.merchant-manage {
  max-width: 1400px;
  margin: 0 auto;
}
.search-card {
  margin-bottom: 16px;
}
</style>