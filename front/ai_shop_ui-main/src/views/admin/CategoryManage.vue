<template>
  <div class="category-manage">
    <el-card>
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>分类管理</span>
          <el-button type="primary" @click="handleAdd">添加分类</el-button>
        </div>
      </template>

      <el-table :data="tableData" border stripe v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="name" label="分类名称" min-width="200" />
        <el-table-column prop="sort" label="排序" width="100" align="center" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 添加/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑分类' : '添加分类'"
      width="450px"
      :close-on-click-modal="false"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="分类名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入分类名称" maxlength="50" />
        </el-form-item>
        <el-form-item label="排序" prop="sort">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { listCategories, addCategory, updateCategory, deleteCategory, type Category } from '../../api/category'

const loading = ref(false)
const submitting = ref(false)
const tableData = ref<Category[]>([])
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const form = ref({
  id: 0,
  name: '',
  sort: 0,
})

const rules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

async function fetchData() {
  loading.value = true
  try {
    const res: any = await listCategories()
    tableData.value = res.data
  } catch {
    tableData.value = []
  } finally {
    loading.value = false
  }
}

function handleAdd() {
  isEdit.value = false
  form.value = { id: 0, name: '', sort: 0 }
  dialogVisible.value = true
}

function handleEdit(row: Category) {
  isEdit.value = true
  form.value = { id: row.id, name: row.name, sort: row.sort }
  dialogVisible.value = true
}

async function submitForm() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (isEdit.value) {
      await updateCategory(form.value)
      ElMessage.success('修改成功')
    } else {
      // 添加时不传 sort，后端自动计算排序值
      await addCategory({ name: form.value.name })
      ElMessage.success('添加成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || e?.message || '操作失败')
  } finally {
    submitting.value = false
  }
}

function handleDelete(row: Category) {
  ElMessageBox.confirm(`确定删除分类「${row.name}」吗？`, '提示', {
    type: 'warning',
  }).then(async () => {
    try {
      await deleteCategory(row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (e: any) {
      ElMessage.error(e?.response?.data?.message || '删除失败')
    }
  }).catch(() => {})
}

onMounted(fetchData)
</script>