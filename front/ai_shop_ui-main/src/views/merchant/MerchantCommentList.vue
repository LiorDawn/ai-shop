<template>
  <div class="mcl-page">
    <div class="mcl-filter">
      <el-select v-model="filter.score" placeholder="全部评分" clearable style="width:120px" @change="fetchList">
        <el-option label="好评(5分)" :value="5" />
        <el-option label="好评(4分)" :value="4" />
        <el-option label="中评(3分)" :value="3" />
        <el-option label="差评(2分)" :value="2" />
        <el-option label="差评(1分)" :value="1" />
      </el-select>
      <el-select v-model="filter.hasReply" placeholder="回复状态" clearable style="width:120px" @change="fetchList">
        <el-option label="已回复" :value="1" />
        <el-option label="未回复" :value="0" />
      </el-select>
      <el-input v-model="filter.productName" placeholder="商品名称" clearable style="width:180px" @keyup.enter="fetchList" />
      <el-button type="primary" @click="fetchList">搜索</el-button>
    </div>

    <el-card class="mcl-table-card">
      <el-table :data="list" stripe border style="width:100%" v-loading="loading">
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div class="mcl-product">
              <img :src="row.productImage || ''" class="mcl-product-img" />
              <div class="mcl-product-name">{{ row.productName }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="买家" width="120" prop="username" />
        <el-table-column label="评分" width="100">
          <template #default="{ row }">
            <el-rate v-model="row.score" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column label="评价内容" min-width="200">
          <template #default="{ row }">
            <div class="mcl-content">{{ row.content || '暂无内容' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="评价时间" width="160" prop="createTime" />
        <el-table-column label="回复状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.reply" type="success" size="small">已回复</el-tag>
            <el-tag v-else type="info" size="small">未回复</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="handleReply(row)">回复</el-button>
            <el-button
              size="small"
              :type="row.status === 2 ? 'success' : 'warning'"
              @click="handleToggle(row)"
            >
              {{ row.status === 2 ? '显示' : '隐藏' }}
            </el-button>
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
        class="mcl-pager"
      />
    </el-card>

    <!-- 回复弹窗 -->
    <el-dialog v-model="replyVisible" title="回复评价" width="500px">
      <div v-if="replyTarget" class="mcl-reply-info">
        <div class="mcl-reply-meta">
          <span>商品：{{ replyTarget.productName }}</span>
          <el-rate :model-value="replyTarget.score" disabled size="small" style="display:inline-block;margin-left:12px" />
        </div>
        <div class="mcl-reply-content">用户评价：{{ replyTarget.content }}</div>
        <div v-if="replyTarget.reply" class="mcl-reply-old">
          历史回复：{{ replyTarget.reply }}
        </div>
      </div>
      <el-input
        v-model="replyText"
        type="textarea"
        :rows="4"
        placeholder="请输入回复内容"
        maxlength="500"
      />
      <template #footer>
        <el-button @click="replyVisible = false">取消</el-button>
        <el-button type="primary" :loading="replying" @click="confirmReply">确认回复</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMerchantCommentsPage, replyMerchantComment, toggleCommentStatus } from '@/api/merchant'

const loading = ref(false)
const list = ref<any[]>([])
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(10)

const filter = reactive({
  score: null as number | null,
  hasReply: null as number | null,
  productName: '',
})

async function fetchList() {
  loading.value = true
  try {
    const res = await getMerchantCommentsPage({
      current: currentPage.value,
      size: pageSize.value,
      score: filter.score ?? undefined,
      hasReply: filter.hasReply ?? undefined,
      productName: filter.productName || undefined,
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch { list.value = []; total.value = 0 }
  finally { loading.value = false }
}

// 回复
const replyVisible = ref(false)
const replyText = ref('')
const replying = ref(false)
const replyTarget = ref<any>(null)

function handleReply(row: any) {
  replyTarget.value = row
  replyText.value = ''
  replyVisible.value = true
}

async function confirmReply() {
  if (!replyText.value.trim()) {
    ElMessage.warning('请输入回复内容')
    return
  }
  replying.value = true
  try {
    await replyMerchantComment(replyTarget.value.id, replyText.value)
    ElMessage.success('回复成功')
    replyVisible.value = false
    fetchList()
  } catch (e: any) { ElMessage.error(e?.message || '回复失败') }
  finally { replying.value = false }
}

// 切换状态
async function handleToggle(row: any) {
  const action = row.status === 2 ? '显示' : '隐藏'
  try {
    await ElMessageBox.confirm(`确定${action}该评价？`, '确认', { type: 'warning' })
  } catch { return }
  try {
    const newStatus = row.status === 2 ? 1 : 2
    await toggleCommentStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    fetchList()
  } catch { ElMessage.error('操作失败') }
}

onMounted(() => { fetchList() })
</script>

<style scoped>
.mcl-page { min-height: 100%; }
.mcl-filter { margin-bottom: 12px; display: flex; gap: 10px; flex-wrap: wrap; }
.mcl-table-card { min-height: 300px; }
.mcl-pager { margin-top: 16px; justify-content: flex-end; }
.mcl-product { display: flex; align-items: center; gap: 10px; }
.mcl-product-img { width: 40px; height: 40px; border-radius: 4px; object-fit: cover; }
.mcl-product-name { font-size: 13px; color: #333; }
.mcl-content { font-size: 13px; color: #666; max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.mcl-reply-info { margin-bottom: 16px; padding: 12px; background: #f9f9f9; border-radius: 6px; }
.mcl-reply-meta { font-size: 13px; color: #666; margin-bottom: 8px; display: flex; align-items: center; }
.mcl-reply-content { font-size: 13px; color: #333; margin-bottom: 8px; }
.mcl-reply-old { font-size: 13px; color: #999; background: #f0f0f0; padding: 8px; border-radius: 4px; }
</style>