<template>
  <div class="coupon-manage">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="优惠券名称">
          <el-input v-model="searchForm.name" placeholder="请输入名称" clearable style="width:200px" />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="searchForm.status" placeholder="全部" clearable style="width:120px">
            <el-option label="启用" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">搜索</el-button>
          <el-button @click="handleReset">重置</el-button>
          <el-button type="primary" @click="handleAdd">新增优惠券</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 表格 -->
    <el-card class="table-card">
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="name" label="优惠券名称" min-width="160" />
        <el-table-column label="满减门槛" width="120">
          <template #default="{ row }">满 ¥{{ row.minPrice }} 可用</template>
        </el-table-column>
        <el-table-column label="优惠金额" width="110">
          <template #default="{ row }">¥{{ row.discount }}</template>
        </el-table-column>
        <el-table-column label="总发放数量" width="110">
          <template #default="{ row }">{{ row.stock }} 张</template>
        </el-table-column>
        <el-table-column label="剩余数量" width="110">
          <template #default="{ row }">{{ row.remain }} 张</template>
        </el-table-column>
        <el-table-column label="领取时间" min-width="170">
          <template #default="{ row }">{{ row.startTime }} ~ {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column label="使用时间" min-width="170">
          <template #default="{ row }">{{ row.startTime }} ~ {{ row.endTime }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              @change="handleToggle(row)"
              :loading="togglingId === row.id"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
            <el-button type="info" link size="small" @click="handleViewRecords(row)">领取记录</el-button>
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

    <!-- 新增/编辑弹窗 -->
    <el-dialog v-model="formVisible" :title="isEdit ? '编辑优惠券' : '新增优惠券'" width="600px" @closed="resetForm">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="120px">
        <el-form-item label="优惠券名称" prop="name">
          <el-input v-model="form.name" placeholder="如：满100减10" />
        </el-form-item>
        <el-form-item label="满减金额(门槛)" prop="minPrice">
          <el-input-number v-model="form.minPrice" :min="0" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="优惠抵扣金额" prop="discount">
          <el-input-number v-model="form.discount" :min="0.01" :precision="2" style="width:100%" />
        </el-form-item>
        <el-form-item label="发放总数量" prop="stock">
          <el-input-number v-model="form.stock" :min="1" style="width:100%" />
        </el-form-item>
        <el-form-item label="领取开始时间" prop="startTime">
          <el-date-picker v-model="form.startTime" type="datetime" placeholder="选择日期时间" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="领取结束时间" prop="endTime">
          <el-date-picker v-model="form.endTime" type="datetime" placeholder="选择日期时间" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-switch v-model="form.status" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>

    <!-- 领取记录弹窗 -->
    <el-dialog v-model="recordVisible" title="领取记录" width="700px">
      <el-table :data="recordData" v-loading="recordLoading" stripe>
        <el-table-column prop="username" label="用户" width="120" />
        <el-table-column prop="couponName" label="优惠券名称" min-width="160" />
        <el-table-column prop="createTime" label="领取时间" width="170" />
        <el-table-column prop="statusText" label="使用状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : row.status === 2 ? 'info' : 'warning'" size="small">
              {{ row.statusText }}
            </el-tag>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination-wrapper">
        <el-pagination
          v-model:current-page="recordPage"
          v-model:page-size="recordSize"
          :total="recordTotal"
          layout="total, prev, pager, next"
          @current-change="fetchRecords"
          small
        />
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getCouponPage,
  addCoupon,
  updateCoupon,
  deleteCoupon,
  toggleCouponStatus,
  getCouponRecords,
  type CouponDTO,
  type CouponRecordVO,
} from '../../api/coupon'

const loading = ref(false)
const tableData = ref<CouponDTO[]>([])
const total = ref(0)
const current = ref(1)
const size = ref(10)

const searchForm = reactive({
  name: '',
  status: undefined as number | undefined,
})

// 表单弹窗
const formVisible = ref(false)
const isEdit = ref(false)
const submitting = ref(false)
const formRef = ref<FormInstance>()
const form = reactive({
  id: 0,
  name: '',
  minPrice: 0,
  discount: 0,
  stock: 1,
  startTime: '',
  endTime: '',
  status: 1,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入优惠券名称', trigger: 'blur' }],
  minPrice: [{ required: true, message: '请输入满减门槛', trigger: 'blur' }],
  discount: [{ required: true, message: '请输入优惠金额', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入发放数量', trigger: 'blur' }],
  startTime: [{ required: true, message: '请选择领取开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择领取结束时间', trigger: 'change' }],
}

// 切换状态
const togglingId = ref<number | null>(null)
async function handleToggle(row: CouponDTO) {
  togglingId.value = row.id
  try {
    await toggleCouponStatus(row.id)
    ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
    fetchData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    togglingId.value = null
  }
}

onMounted(() => fetchData())

async function fetchData() {
  loading.value = true
  try {
    const params: any = { current: current.value, size: size.value }
    if (searchForm.name) params.name = searchForm.name
    if (searchForm.status !== undefined) params.status = searchForm.status
    const res: any = await getCouponPage(params)
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
  searchForm.name = ''
  searchForm.status = undefined
  current.value = 1
  fetchData()
}

function resetForm() {
  form.id = 0
  form.name = ''
  form.minPrice = 0
  form.discount = 0
  form.stock = 1
  form.startTime = ''
  form.endTime = ''
  form.status = 1
  formRef.value?.clearValidate()
}

function handleAdd() {
  isEdit.value = false
  resetForm()
  formVisible.value = true
}

function handleEdit(row: CouponDTO) {
  isEdit.value = true
  form.id = row.id
  form.name = row.name
  form.minPrice = row.minPrice
  form.discount = row.discount
  form.stock = row.stock
  form.startTime = row.startTime
  form.endTime = row.endTime
  form.status = row.status
  formVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCoupon({
        id: form.id,
        name: form.name,
        minPrice: form.minPrice,
        discount: form.discount,
        stock: form.stock,
        startTime: form.startTime,
        endTime: form.endTime,
        status: form.status,
      })
      ElMessage.success('修改成功')
    } else {
      await addCoupon({
        name: form.name,
        minPrice: form.minPrice,
        discount: form.discount,
        stock: form.stock,
        startTime: form.startTime,
        endTime: form.endTime,
      })
      ElMessage.success('新增成功')
    }
    formVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

function handleDelete(row: CouponDTO) {
  ElMessageBox.confirm(`确定删除优惠券「${row.name}」吗？已领取的券无法删除。`, '提示', { type: 'warning' })
    .then(async () => {
      try {
        await deleteCoupon(row.id)
        ElMessage.success('删除成功')
        fetchData()
      } catch (e: any) {
        ElMessage.error(e.message || '删除失败')
      }
    })
    .catch(() => {})
}

// 领取记录
const recordVisible = ref(false)
const recordLoading = ref(false)
const recordData = ref<CouponRecordVO[]>([])
const recordTotal = ref(0)
const recordPage = ref(1)
const recordSize = ref(10)
const currentCouponId = ref<number>(0)

function handleViewRecords(row: CouponDTO) {
  currentCouponId.value = row.id
  recordPage.value = 1
  recordVisible.value = true
  fetchRecords()
}

async function fetchRecords() {
  recordLoading.value = true
  try {
    const res: any = await getCouponRecords({
      current: recordPage.value,
      size: recordSize.value,
      couponId: currentCouponId.value,
    })
    recordData.value = res.data.records
    recordTotal.value = res.data.total
  } catch (e: any) {
    ElMessage.error(e.message || '查询失败')
  } finally {
    recordLoading.value = false
  }
}
</script>

<style scoped>
.coupon-manage {
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