<template>
  <div class="user-manage">
    <!-- 搜索栏 -->
    <el-card class="search-card">
      <el-form :model="searchForm" inline>
        <el-form-item label="用户名">
          <el-input v-model="searchForm.username" placeholder="请输入用户名" clearable />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="searchForm.phone" placeholder="请输入手机号" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 操作栏 -->
    <el-card class="table-card">
      <div class="table-toolbar">
        <el-button type="primary" @click="handleAdd">新增用户</el-button>
        <el-button
          type="danger"
          :disabled="selectedIds.length === 0"
          @click="handleBatchDelete"
        >
          批量删除
        </el-button>
      </div>

      <!-- 数据表格 -->
      <el-table
        :data="tableData"
        v-loading="loading"
        stripe
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="55" />
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column prop="email" label="邮箱" min-width="160" />
        <el-table-column prop="roleName" label="角色" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="row._statusLoading"
              @change="(val: boolean) => handleStatusChange(row, val)"
            />
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button type="primary" link size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button type="danger" link size="small" @click="handleDelete(row)">删除</el-button>
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

    <!-- 新增/编辑对话框 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑用户' : '新增用户'"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="formRules"
        label-width="80px"
      >
        <el-form-item label="用户名" prop="username">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="邮箱" prop="email">
          <el-input v-model="form.email" placeholder="请输入邮箱" />
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="请输入密码"
            show-password
          />
        </el-form-item>
        <el-form-item label="角色" prop="roleId">
          <el-select v-model="form.roleId" placeholder="请选择角色" style="width: 100%">
            <el-option
              v-for="role in roleOptions"
              :key="role.id"
              :label="role.name"
              :value="role.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'
import {
  getUsersPage,
  addUser,
  updateUser,
  updateUserStatus,
  deleteUser,
  deleteBatchUsers,
  type User,
  type UserDTO,
} from '../../api/user'
import { listRoles, type Role } from '../../api/role'
import { useAuthStore } from '@/stores/auth'

const loading = ref(false)
const tableData = ref<UserDTO[]>([])
const selectedIds = ref<number[]>([])
const allRoleOptions = ref<Role[]>([])

const auth = useAuthStore()

// 根据当前用户角色过滤下拉选项（永远不出现超级管理员选项）
const currentUserRole = auth.user?.roleCode
const roleOptions = computed(() => {
  if (currentUserRole === 'SUPER_ADMIN') {
    // 超级管理员可以看到：管理员、商家、普通用户（不能选超级管理员自己）
    return allRoleOptions.value.filter(r => r.code !== 'SUPER_ADMIN')
  }
  // 管理员能看到：普通用户、商家
  return allRoleOptions.value.filter(r => r.code === 'CUSTOMER' || r.code === 'MERCHANT')
})

// 搜索表单
const searchForm = reactive({
  username: '',
  phone: '',
})

// 分页
const current = ref(1)
const size = ref(10)
const total = ref(0)

// 对话框
const dialogVisible = ref(false)
const isEdit = ref(false)
const submitLoading = ref(false)
const formRef = ref<FormInstance>()

const form = reactive<User>({
  username: '',
  phone: '',
  email: '',
  password: '',
})

const formRules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1[3-9]\d{9}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' },
  ],
}

onMounted(async () => {
  try {
    const res: any = await listRoles()
    allRoleOptions.value = res.data
  } catch {}
  fetchData()
})

async function fetchData() {
  loading.value = true
  try {
    const res: any = await getUsersPage({
      current: current.value,
      size: size.value,
      username: searchForm.username || undefined,
      phone: searchForm.phone || undefined,
    })
    tableData.value = res.data.records
    total.value = res.data.total
  } catch (error: any) {
    ElMessage.error(error.message || '查询失败')
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  current.value = 1
  fetchData()
}

function handleReset() {
  searchForm.username = ''
  searchForm.phone = ''
  current.value = 1
  fetchData()
}

function handleSelectionChange(rows: UserDTO[]) {
  selectedIds.value = rows.map((r) => r.id)
}

// 新增
function handleAdd() {
  isEdit.value = false
  form.id = undefined
  form.username = ''
  form.phone = ''
  form.email = ''
  form.password = ''
  form.roleId = undefined
  dialogVisible.value = true
}

// 编辑
function handleEdit(row: UserDTO) {
  isEdit.value = true
  form.id = row.id
  form.username = row.username
  form.phone = row.phone
  form.email = row.email
  form.roleId = row.roleId
  form.password = ''
  dialogVisible.value = true
}

// 提交表单
async function handleSubmit() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await updateUser({
        id: form.id,
        username: form.username,
        phone: form.phone,
        email: form.email,
        roleId: form.roleId,
      })
      ElMessage.success('修改成功')
    } else {
      await addUser({
        username: form.username,
        phone: form.phone,
        email: form.email,
        password: form.password,
        roleId: form.roleId,
      })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 状态切换
async function handleStatusChange(row: UserDTO, val: boolean) {
  const newStatus = val ? 1 : 0
  try {
    const target = tableData.value.find((r) => r.id === row.id)
    if (target) {
      ;(target as any)._statusLoading = true
    }
    await updateUserStatus(row.id, newStatus)
    ElMessage.success('状态更新成功')
    fetchData()
  } catch (error: any) {
    ElMessage.error(error.message || '状态更新失败')
  } finally {
    const target = tableData.value.find((r) => r.id === row.id)
    if (target) {
      ;(target as any)._statusLoading = false
    }
  }
}

// 删除
function handleDelete(row: UserDTO) {
  ElMessageBox.confirm(`确定要删除用户「${row.username}」吗？`, '提示', {
    type: 'warning',
  }).then(async () => {
    try {
      await deleteUser(row.id)
      ElMessage.success('删除成功')
      fetchData()
    } catch (error: any) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}

// 批量删除
function handleBatchDelete() {
  ElMessageBox.confirm(`确定要删除选中的 ${selectedIds.value.length} 个用户吗？`, '提示', {
    type: 'warning',
  }).then(async () => {
    try {
      await deleteBatchUsers(selectedIds.value)
      ElMessage.success('批量删除成功')
      selectedIds.value = []
      fetchData()
    } catch (error: any) {
      ElMessage.error(error.message || '删除失败')
    }
  }).catch(() => {})
}
</script>

<style scoped>
.user-manage {
  max-width: 1400px;
  margin: 0 auto;
}

.search-card {
  margin-bottom: 16px;
}

.table-card {
  margin-bottom: 16px;
}

.table-toolbar {
  margin-bottom: 16px;
}

.pagination-wrapper {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>