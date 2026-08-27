<template>
  <div class="mpl-page">
    <div class="mpl-toolbar">
      <el-button type="primary" @click="handleAdd">添加商品</el-button>
      <el-button :disabled="selectedIds.length === 0" @click="handleBatchUp">批量上架</el-button>
      <el-button :disabled="selectedIds.length === 0" @click="handleBatchDown">批量下架</el-button>
      <el-button :disabled="selectedIds.length === 0" type="danger" @click="handleBatchDelete">批量删除</el-button>
    </div>

    <div class="mpl-filter">
      <el-input v-model="filter.name" placeholder="商品名称" clearable style="width:200px" @keyup.enter="fetchList" />
      <el-select v-model="filter.status" placeholder="全部状态" clearable style="width:120px" @change="fetchList">
        <el-option label="上架" :value="1" />
        <el-option label="下架" :value="0" />
      </el-select>
      <el-select v-model="filter.categoryId" placeholder="商品分类" clearable style="width:140px" @change="fetchList">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button type="primary" @click="fetchList">搜索</el-button>
    </div>

    <el-card class="mpl-table-card">
      <el-table
        :data="list"
        stripe
        border
        style="width:100%"
        @selection-change="(rows: any[]) => selectedIds = rows.map(r => r.id)"
      >
        <el-table-column type="selection" width="45" />
        <el-table-column label="商品" min-width="260">
          <template #default="{ row }">
            <div class="mpl-product-cell">
              <img :src="row.image || ''" class="mpl-product-img" />
              <div>
                <div class="mpl-product-name">{{ row.name }}</div>
                <div class="mpl-product-cate">{{ row.categoryName }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="售价" width="100">
          <template #default="{ row }">¥{{ fmtPrice(row.price) }}</template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="80" />
        <el-table-column prop="sales" label="销量" width="80" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button
              size="small"
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleToggle(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-popconfirm title="确定删除该商品？" @confirm="handleDelete(row)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
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
        class="mpl-pager"
      />
    </el-card>

    <!-- 添加/编辑商品对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑商品' : '添加商品'"
      width="900px"
      :close-on-click-modal="false"
      @close="resetForm"
    >
      <el-form
        ref="productFormRef"
        :model="productForm"
        :rules="productRules"
        label-width="100px"
        v-loading="formLoading"
      >
        <el-form-item label="商品名称" prop="name">
          <el-input v-model="productForm.name" maxlength="100" />
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="商品分类" prop="categoryId">
              <el-select v-model="productForm.categoryId" style="width:100%">
                <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="售价" prop="price">
              <el-input-number v-model="productForm.price" :min="0" :precision="2" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <!-- 主图上传 -->
        <el-form-item label="商品主图" prop="image">
          <div class="mp-upload-wrap">
            <el-upload
              :show-file-list="false"
              :before-upload="(file) => beforeMainImageUpload(file)"
              accept="image/*"
            >
              <img v-if="productForm.image" :src="productForm.image" class="mp-upload-preview" />
              <div v-else class="mp-upload-placeholder">
                <el-icon :size="28"><Plus /></el-icon>
                <span>上传主图</span>
              </div>
            </el-upload>
          </div>
        </el-form-item>

        <!-- 副图列表 -->
        <el-form-item label="商品副图">
          <div class="mp-images-wrap">
            <div
              v-for="(img, idx) in productForm.imageList"
              :key="idx"
              class="mp-image-item"
            >
              <img :src="img.imageUrl" class="mp-image-preview" />
              <el-icon class="mp-image-remove" @click="removeImage(idx)"><Close /></el-icon>
            </div>
            <el-upload
              :show-file-list="false"
              :before-upload="(file) => beforeSubImageUpload(file)"
              accept="image/*"
            >
              <div class="mp-image-add">
                <el-icon :size="22"><Plus /></el-icon>
              </div>
            </el-upload>
          </div>
        </el-form-item>

        <!-- SKU 管理 -->
        <el-form-item label="商品规格">
          <div class="mp-sku-wrap">
            <div class="mp-sku-header">
              <span style="width:160px">规格名称</span>
              <span style="width:140px">规格价格</span>
              <span style="width:120px">库存数量</span>
              <span style="width:32px"></span>
            </div>
            <div v-for="(sku, idx) in productForm.skuList" :key="idx" class="mp-sku-row">
              <el-input v-model="sku.spec" placeholder="如：红色/L码/128G" size="small" style="width:160px" />
              <el-input-number v-model="sku.price" :min="0" :precision="2" size="small" style="width:140px" placeholder="规格价" />
              <el-input-number v-model="sku.stock" :min="0" size="small" style="width:120px" placeholder="库存量" />
              <el-button size="small" type="danger" :icon="Delete" @click="removeSku(idx)" />
            </div>
            <el-button size="small" @click="addSku">
              <el-icon><Plus /></el-icon> 添加规格
            </el-button>
          </div>
        </el-form-item>

        <el-form-item label="商品简介">
          <el-input v-model="productForm.description" type="textarea" :rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveProduct">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete, Close } from '@element-plus/icons-vue'
import type { FormInstance, FormRules } from 'element-plus'
import {
  getProductsPage,
  getProductDetail,
  upProduct,
  downProduct,
  deleteProduct,
  deleteBatchProducts,
} from '@/api/product'
import { listCategories } from '@/api/category'
import { uploadImage } from '@/api/upload'
import request from '@/api/request'

const list = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)
const selectedIds = ref<number[]>([])
const categories = ref<any[]>([])
const loading = ref(false)

const filter = reactive({
  name: '',
  status: null as number | null,
  categoryId: null as number | null,
})

async function fetchList() {
  loading.value = true
  try {
    const res = await getProductsPage({
      current: currentPage.value,
      size: pageSize.value,
      name: filter.name || undefined,
      status: filter.status ?? undefined,
      categoryId: filter.categoryId ?? undefined,
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { list.value = []; total.value = 0 }
  finally { loading.value = false }
}

function fmtPrice(v: any) {
  const n = Number(v)
  return isNaN(n) ? '0.00' : n.toFixed(2)
}

async function handleBatchUp() {
  try {
    await Promise.all(selectedIds.value.map(id => upProduct(id)))
    ElMessage.success('批量上架成功')
    fetchList()
  } catch { ElMessage.error('操作失败') }
}

async function handleBatchDown() {
  try {
    await Promise.all(selectedIds.value.map(id => downProduct(id)))
    ElMessage.success('批量下架成功')
    fetchList()
  } catch { ElMessage.error('操作失败') }
}

async function handleBatchDelete() {
  try {
    await ElMessageBox.confirm(`确定删除 ${selectedIds.value.length} 个商品？`, '确认', { type: 'warning' })
  } catch { return }
  try {
    await deleteBatchProducts(selectedIds.value)
    ElMessage.success('批量删除成功')
    selectedIds.value = []
    fetchList()
  } catch { ElMessage.error('删除失败') }
}

async function handleToggle(row: any) {
  try {
    if (row.status === 1) { await downProduct(row.id) }
    else { await upProduct(row.id) }
    ElMessage.success(row.status === 1 ? '已下架' : '已上架')
    fetchList()
  } catch { ElMessage.error('操作失败') }
}

async function handleDelete(row: any) {
  try {
    await deleteProduct(row.id)
    ElMessage.success('删除成功')
    fetchList()
  } catch { ElMessage.error('删除失败') }
}

function handleAdd() {
  editId.value = 0
  resetForm()
  dialogVisible.value = true
}

function handleEdit(row: any) {
  editId.value = row.id
  dialogVisible.value = true
  loadProductDetail(row.id)
}

// ======== 商品表单 ========
const dialogVisible = ref(false)
const editId = ref(0)
const saving = ref(false)
const formLoading = ref(false)
const productFormRef = ref<FormInstance>()

interface ImageItem { imageUrl: string }
interface SkuItem { spec: string; price: number; stock: number }

const productForm = reactive({
  name: '',
  categoryId: null as number | null,
  price: 0,
  image: '',
  description: '',
  imageList: [] as ImageItem[],
  skuList: [] as SkuItem[],
})

const productRules: FormRules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择商品分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入售价', trigger: 'blur' }],
  image: [{ required: true, message: '请上传商品主图', trigger: 'change' }],
}

// 主图上传
async function beforeMainImageUpload(file: File): Promise<boolean> {
  try {
    const res = await uploadImage(file, '产品图')
    productForm.image = res.data
    productFormRef.value?.clearValidate('image')
  } catch { ElMessage.error('主图上传失败') }
  return false
}

// 副图上传
async function beforeSubImageUpload(file: File): Promise<boolean> {
  try {
    const res = await uploadImage(file, '产品图')
    productForm.imageList.push({ imageUrl: res.data })
  } catch { ElMessage.error('副图上传失败') }
  return false
}

function removeImage(idx: number) {
  productForm.imageList.splice(idx, 1)
}

// SKU 管理
function addSku() {
  productForm.skuList.push({ spec: '', price: 0, stock: 0 })
}

function removeSku(idx: number) {
  productForm.skuList.splice(idx, 1)
}

async function loadProductDetail(id: number) {
  formLoading.value = true
  try {
    const res = await getProductDetail(id)
    const d = res.data || {}
    productForm.name = d.name || ''
    productForm.categoryId = d.categoryId || null
    productForm.price = d.price || 0
    productForm.image = d.image || ''
    productForm.description = d.description || ''
    productForm.imageList = (d.imageList || []).map((img: any) => ({
      imageUrl: img.imageUrl || img,
    }))
    productForm.skuList = (d.skuList || []).map((s: any) => ({
      spec: s.spec || '',
      price: s.price || 0,
      stock: s.stock || 0,
    }))
  } catch { ElMessage.error('加载商品信息失败') }
  finally { formLoading.value = false }
}

function resetForm() {
  productForm.name = ''
  productForm.categoryId = null
  productForm.price = 0
  productForm.image = ''
  productForm.description = ''
  productForm.imageList = []
  productForm.skuList = []
  productFormRef.value?.clearValidate()
}

async function handleSaveProduct() {
  const valid = await productFormRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    const body = {
      name: productForm.name,
      categoryId: productForm.categoryId,
      price: productForm.price,
      image: productForm.image,
      description: productForm.description,
      imageList: productForm.imageList.map(img => ({ imageUrl: img.imageUrl })),
      skuList: productForm.skuList.filter(s => s.spec).map(s => ({
        spec: s.spec, price: s.price, stock: s.stock,
      })),
    }
    if (editId.value) {
      await request.put('/product', { id: editId.value, ...body })
      ElMessage.success('修改成功')
    } else {
      await request.post('/product', body)
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchList()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e.message || '保存失败')
  } finally { saving.value = false }
}

onMounted(async () => {
  fetchList()
  try {
    const res = await listCategories()
    categories.value = res.data || []
  } catch { categories.value = [] }
})
</script>

<style scoped>
.mpl-page { min-height: 100%; }
.mpl-toolbar { margin-bottom: 12px; display: flex; gap: 8px; flex-wrap: wrap; }
.mpl-filter { margin-bottom: 12px; display: flex; gap: 10px; flex-wrap: wrap; }
.mpl-table-card { min-height: 300px; }
.mpl-pager { margin-top: 16px; justify-content: flex-end; }
.mpl-product-cell { display: flex; align-items: center; gap: 10px; }
.mpl-product-img { width: 48px; height: 48px; border-radius: 4px; object-fit: cover; }
.mpl-product-name { font-size: 13px; font-weight: 500; color: #333; margin-bottom: 4px; }
.mpl-product-cate { font-size: 12px; color: #999; }

/* 主图上传 */
.mp-upload-wrap { display: flex; gap: 12px; }
.mp-upload-preview { width: 120px; height: 120px; object-fit: cover; border-radius: 6px; border: 1px solid #e0e0e0; }
.mp-upload-placeholder {
  width: 120px; height: 120px; border: 2px dashed #d9d9d9; border-radius: 6px;
  display: flex; flex-direction: column; align-items: center; justify-content: center;
  color: #999; font-size: 12px; gap: 4px; cursor: pointer; transition: all 0.2s; background: #fafafa;
}
.mp-upload-placeholder:hover { border-color: #409eff; color: #409eff; }

/* 副图 */
.mp-images-wrap { display: flex; gap: 10px; flex-wrap: wrap; align-items: center; }
.mp-image-item { position: relative; width: 80px; height: 80px; border-radius: 4px; overflow: hidden; border: 1px solid #eee; }
.mp-image-preview { width: 100%; height: 100%; object-fit: cover; }
.mp-image-remove {
  position: absolute; top: 2px; right: 2px; width: 18px; height: 18px;
  border-radius: 50%; background: rgba(0,0,0,0.5); color: #fff;
  display: flex; align-items: center; justify-content: center; cursor: pointer; font-size: 12px;
}
.mp-image-add {
  width: 80px; height: 80px; border: 2px dashed #d9d9d9; border-radius: 4px;
  display: flex; align-items: center; justify-content: center; cursor: pointer;
  color: #999; transition: all 0.2s; background: #fafafa;
}
.mp-image-add:hover { border-color: #409eff; color: #409eff; }

/* SKU */
.mp-sku-wrap { display: flex; flex-direction: column; gap: 8px; }
.mp-sku-header { display: flex; align-items: center; gap: 8px; font-size: 12px; color: #999; padding: 0 4px; }
.mp-sku-row { display: flex; align-items: center; gap: 8px; }
</style>