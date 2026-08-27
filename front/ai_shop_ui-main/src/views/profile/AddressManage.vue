<template>
  <div class="am-page">
    <HeaderUser />

    <div class="am-wrapper">
      <div class="am-layout">
        <ProfileSidebar :user="user" :stats="stats" />

        <div class="am-main">
          <div class="am-card">
            <div class="am-card-header">
              <div>
                <h3>收货地址管理</h3>
                <span class="am-card-desc">管理你的收货地址，方便快速下单</span>
              </div>
              <el-button type="primary" @click="showForm(null)">+ 新增地址</el-button>
            </div>

            <div class="am-body" v-loading="loading">
              <el-empty v-if="addresses.length === 0 && !loading" description="暂无收货地址" />

              <div v-for="addr in addresses" :key="addr.id" class="am-addr-item">
                <div class="am-addr-left">
                  <div class="am-addr-top">
                    <span class="am-addr-receiver">{{ addr.receiver }}</span>
                    <span class="am-addr-phone">{{ addr.phone }}</span>
                    <el-tag v-if="addr.isDefault === 1" size="small" type="danger" effect="plain">默认</el-tag>
                  </div>
                  <div class="am-addr-address">{{ addr.address }}</div>
                </div>
                <div class="am-addr-right">
                  <el-button text type="primary" @click="editAddress(addr)">编辑</el-button>
                  <el-button text type="danger" @click="removeAddress(addr)">删除</el-button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 弹窗 -->
    <el-dialog
      v-model="formVisible"
      :title="formMode === 'add' ? '新增地址' : '编辑地址'"
      width="520px"
    >
      <el-form :model="form" label-width="80px" :rules="formRules" ref="formRef">
        <el-form-item label="收件人" prop="receiver">
          <el-input v-model="form.receiver" placeholder="请输入收件人姓名" />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" maxlength="11" />
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="form.address" type="textarea" :rows="2" placeholder="省市区 + 详细地址" />
        </el-form-item>
        <el-form-item label="设为默认">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="saveAddress" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import HeaderUser from '@/components/layout/HeaderUser.vue'
import ProfileSidebar from '@/views/profile/ProfileSidebar.vue'
import { listAddresses, addAddress, updateAddress, deleteAddress, getProfile, getProfileStats, type Address, type UserProfileDTO, type ProfileStats } from '@/api/profile'

const loading = ref(false)
const saving = ref(false)
const addresses = ref<Address[]>([])
const user = ref<UserProfileDTO | null>(null)
const stats = ref<ProfileStats | null>(null)

const formVisible = ref(false)
const formMode = ref<'add' | 'edit'>('add')
const formRef = ref<any>(null)
const form = reactive<Address>({ receiver: '', phone: '', address: '', isDefault: 0 })
const editingId = ref<number | null>(null)

const formRules = {
  receiver: [{ required: true, message: '请输入收件人', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式不正确', trigger: 'blur' },
  ],
  address: [{ required: true, message: '请输入详细地址', trigger: 'blur' }],
}

async function fetchData() {
  loading.value = true
  try {
    const [profileRes, addrRes, statsRes] = await Promise.all([getProfile(), listAddresses(), getProfileStats()])
    user.value = profileRes.data
    stats.value = statsRes.data
    addresses.value = addrRes.data || []
  } catch {
    addresses.value = []
  } finally {
    loading.value = false
  }
}

function showForm(addr: Address | null) {
  if (addr) {
    formMode.value = 'edit'
    editingId.value = addr.id ?? null
    form.receiver = addr.receiver
    form.phone = addr.phone
    form.address = addr.address
    form.isDefault = addr.isDefault ?? 0
  } else {
    formMode.value = 'add'
    editingId.value = null
    form.receiver = ''
    form.phone = ''
    form.address = ''
    form.isDefault = 0
  }
  formVisible.value = true
}

function editAddress(addr: Address) { showForm(addr) }

async function saveAddress() {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return
  saving.value = true
  try {
    if (formMode.value === 'add') {
      await addAddress({ receiver: form.receiver, phone: form.phone, address: form.address, isDefault: form.isDefault })
      ElMessage.success('新增成功')
    } else {
      await updateAddress({ id: editingId.value!, receiver: form.receiver, phone: form.phone, address: form.address, isDefault: form.isDefault })
      ElMessage.success('修改成功')
    }
    formVisible.value = false
    fetchData()
  } catch (e: any) {
    ElMessage.error(e?.response?.data?.message || '操作失败')
  } finally {
    saving.value = false
  }
}

async function removeAddress(addr: Address) {
  try {
    await ElMessageBox.confirm(`确定删除收件人「${addr.receiver}」的地址吗？`, '提示', { type: 'warning' })
    await deleteAddress(addr.id!)
    ElMessage.success('已删除')
    fetchData()
  } catch { /* cancel */ }
}

onMounted(() => { fetchData() })
</script>

<style scoped>
.am-page { min-height: 100vh; background: #f5f5f5; }
.am-wrapper { max-width: 1600px; margin: 0 auto; padding: 16px; }
.am-layout { display: flex; gap: 16px; align-items: flex-start; }
.am-main { flex: 1; min-width: 0; }
.am-card {
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.06);
}
.am-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 20px 24px;
  border-bottom: 1px solid #f0f0f0;
}
.am-card-header h3 { margin: 0 0 4px; font-size: 18px; font-weight: 600; color: #333; }
.am-card-desc { font-size: 13px; color: #999; }
.am-body { padding: 8px 0; }
.am-addr-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 24px;
  border-bottom: 1px solid #f5f5f5;
  transition: background 0.2s;
}
.am-addr-item:hover { background: #fafafa; }
.am-addr-item:last-child { border-bottom: none; }
.am-addr-left { flex: 1; min-width: 0; }
.am-addr-top { display: flex; align-items: center; gap: 12px; margin-bottom: 6px; }
.am-addr-receiver { font-size: 15px; font-weight: 600; color: #333; }
.am-addr-phone { font-size: 14px; color: #666; }
.am-addr-address { font-size: 14px; color: #999; line-height: 1.5; }
.am-addr-right { flex-shrink: 0; display: flex; gap: 8px; }
</style>